package tz.mengidata.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tz.mengidata.app.data.accounting.AutoAccountingEngine
import tz.mengidata.app.data.accounting.AutoBucket
import tz.mengidata.app.data.accounting.BusinessHealthEngine
import tz.mengidata.app.data.accounting.BusinessHealthReport
import tz.mengidata.app.data.accounting.CategoryAnalyticsEngine
import tz.mengidata.app.data.accounting.CategoryAnalyticsReport
import tz.mengidata.app.data.accounting.PartyDirectoryEngine
import tz.mengidata.app.data.accounting.PartySummary
import tz.mengidata.app.data.accounting.ZeroLossLedgerSummary
import tz.mengidata.app.data.db.BillEntity
import tz.mengidata.app.data.db.BusinessSettingsEntity
import tz.mengidata.app.data.db.InvoiceEntity
import tz.mengidata.app.data.db.InvoiceItemEntity
import tz.mengidata.app.data.db.InvoiceWithItems
import tz.mengidata.app.data.db.MengiDatabase
import tz.mengidata.app.data.db.SavingsGoalEntity
import tz.mengidata.app.data.db.SavingsLogEntity
import tz.mengidata.app.data.db.TransactionCategoryEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.data.pdf.InvoicePdfGenerator
import tz.mengidata.app.data.prefs.AppPreferences
import tz.mengidata.app.data.repository.FinanceRepository
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.PeriodFilter
import tz.mengidata.app.i18n.ThemeMode
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.components.MonthlyChartPoint
import tz.mengidata.app.updater.AppUpdateManager
import tz.mengidata.app.updater.InstallResultEvent
import tz.mengidata.app.updater.UpdateInfo
import tz.mengidata.app.updater.UpdateInstallEventBus
import tz.mengidata.app.updater.UpdateUiState

data class SnackbarNotice(
    val message: String,
    val undoActionLabel: String? = null,
    val onUndo: (() -> Unit)? = null
)

data class PendingFileExport(
    val filename: String,
    val mimeType: String,
    val bytes: ByteArray
)

data class DashboardMetrics(
    val periodIncome: Double = 0.0,
    val periodExpense: Double = 0.0,
    val netProfit: Double = 0.0,
    val receivablesBalance: Double = 0.0,
    val unpaidBillsTotal: Double = 0.0,
    val overdueBillsCount: Int = 0,
    val totalSavings: Double = 0.0,
    val totalSavingsTarget: Double = 0.0,
    val chartPoints: List<MonthlyChartPoint> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList()
)

class MengiViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPreferences(application)
    private val repository = FinanceRepository(application, MengiDatabase.getInstance(application), prefs)
    val updateManager = AppUpdateManager(application)

    val themeMode: StateFlow<ThemeMode> = prefs.themeMode
    val dynamicColorEnabled: StateFlow<Boolean> = prefs.dynamicColorEnabled
    val highContrastEnabled: StateFlow<Boolean> = prefs.highContrastEnabled
    val language: StateFlow<AppLanguage> = prefs.language
    val autoUpdateEnabled: StateFlow<Boolean> = prefs.autoUpdateEnabled
    val privacyMaskEnabled: StateFlow<Boolean> = prefs.privacyMaskEnabled
    val pinLockEnabled: StateFlow<Boolean> = prefs.pinLockEnabled

    private val _isAppUnlocked = MutableStateFlow(!prefs.pinLockEnabled.value)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    val settings: StateFlow<BusinessSettingsEntity> = repository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        BusinessSettingsEntity()
    )

    val transactions: StateFlow<List<TransactionEntity>> = repository.transactionsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val invoices: StateFlow<List<InvoiceWithItems>> = repository.invoicesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val bills: StateFlow<List<BillEntity>> = repository.billsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val savingsGoals: StateFlow<List<SavingsGoalEntity>> = repository.savingsGoalsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val savingsLogs: StateFlow<List<SavingsLogEntity>> = repository.savingsLogsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val categories: StateFlow<List<TransactionCategoryEntity>> = repository.categoriesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CategoryAnalyticsEngine.defaultCategories()
    )

    private val _selectedPeriod = MutableStateFlow(PeriodFilter.MONTH)
    val selectedPeriod: StateFlow<PeriodFilter> = _selectedPeriod.asStateFlow()

    val categoryAnalytics: StateFlow<CategoryAnalyticsReport> = combine(
        transactions,
        categories,
        selectedPeriod
    ) { txns, cats, period ->
        CategoryAnalyticsEngine.analyze(txns, cats, period)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryAnalyticsReport())

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    private val _notices = MutableSharedFlow<SnackbarNotice>(extraBufferCapacity = 4)
    val notices: SharedFlow<SnackbarNotice> = _notices.asSharedFlow()

    private val _exportRequests = MutableSharedFlow<PendingFileExport>(extraBufferCapacity = 2)
    val exportRequests: SharedFlow<PendingFileExport> = _exportRequests.asSharedFlow()

    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        transactions,
        invoices,
        bills,
        savingsGoals,
        selectedPeriod,
        language
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val txns = values[0] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val invs = values[1] as List<InvoiceWithItems>
        @Suppress("UNCHECKED_CAST")
        val bls = values[2] as List<BillEntity>
        @Suppress("UNCHECKED_CAST")
        val goals = values[3] as List<SavingsGoalEntity>
        val period = values[4] as PeriodFilter
        val lang = values[5] as AppLanguage

        val periodTxns = txns.filter { FormatUtils.isDateInPeriod(it.date, period) }
        val income = periodTxns.filter { it.isIncome }.sumOf { it.amount }
        val expense = periodTxns.filter { !it.isIncome }.sumOf { it.amount }

        val receivables = invs.filter { !it.isDraft }.sumOf { it.balance }
        val unpaidBills = bls.filter { !it.paid }
        val today = FormatUtils.todayIso()
        val chart = (-5..0).map { offset ->
            val key = FormatUtils.monthKey(FormatUtils.addMonthsIso(today, offset))
            val mTxns = txns.filter { FormatUtils.monthKey(it.date) == key }
            MonthlyChartPoint(
                label = FormatUtils.shortMonthLabel(key, lang),
                income = mTxns.filter { it.isIncome }.sumOf { it.amount },
                expense = mTxns.filter { !it.isIncome }.sumOf { it.amount }
            )
        }

        DashboardMetrics(
            periodIncome = income,
            periodExpense = expense,
            netProfit = income - expense,
            receivablesBalance = receivables,
            unpaidBillsTotal = unpaidBills.sumOf { it.amount },
            overdueBillsCount = unpaidBills.count { it.isOverdue },
            totalSavings = goals.sumOf { it.currentAmount },
            totalSavingsTarget = goals.sumOf { it.targetAmount },
            chartPoints = chart,
            recentTransactions = txns.take(5)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardMetrics())

    val zeroLossLedger: StateFlow<ZeroLossLedgerSummary> = combine(
        transactions,
        bills,
        invoices,
        savingsGoals,
        savingsLogs,
        settings
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val txns = values[0] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val bls = values[1] as List<BillEntity>
        @Suppress("UNCHECKED_CAST")
        val invs = values[2] as List<InvoiceWithItems>
        @Suppress("UNCHECKED_CAST")
        val goals = values[3] as List<SavingsGoalEntity>
        @Suppress("UNCHECKED_CAST")
        val logs = values[4] as List<SavingsLogEntity>
        val s = values[5] as BusinessSettingsEntity
        AutoAccountingEngine.computeZeroLossSummary(
            transactions = txns,
            bills = bls,
            invoices = invs,
            savingsGoals = goals,
            savingsLogs = logs,
            currency = s.currency.ifBlank { "TZS" }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ZeroLossLedgerSummary())

    val businessHealth: StateFlow<BusinessHealthReport> = combine(
        transactions,
        invoices,
        bills,
        savingsGoals,
        categoryAnalytics,
        settings
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val txns = values[0] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val invs = values[1] as List<InvoiceWithItems>
        @Suppress("UNCHECKED_CAST")
        val bls = values[2] as List<BillEntity>
        @Suppress("UNCHECKED_CAST")
        val goals = values[3] as List<SavingsGoalEntity>
        val catReport = values[4] as CategoryAnalyticsReport
        val s = values[5] as BusinessSettingsEntity
        BusinessHealthEngine.evaluate(
            transactions = txns,
            invoices = invs,
            bills = bls,
            savingsGoals = goals,
            categoryAnalytics = catReport,
            currency = s.currency.ifBlank { "TZS" }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BusinessHealthReport())

    val partyDirectory: StateFlow<List<PartySummary>> = combine(
        transactions,
        invoices,
        bills
    ) { txns, invs, bls ->
        PartyDirectoryEngine.buildDirectory(txns, invs, bls)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _hasSafetySnapshot = MutableStateFlow(repository.hasSafetySnapshot())
    val hasSafetySnapshot: StateFlow<Boolean> = _hasSafetySnapshot.asStateFlow()

    private var lastUpdateInfo: UpdateInfo? = null

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
            _hasSafetySnapshot.value = repository.hasSafetySnapshot()
            if (prefs.shouldAutoCheckUpdates()) {
                checkForUpdates(silentIfUpToDate = true)
            }
        }
        viewModelScope.launch {
            UpdateInstallEventBus.events.collect { event ->
                when (event) {
                    is InstallResultEvent.PendingUserAction -> {
                        val info = lastUpdateInfo
                        if (info != null) {
                            _updateState.value = UpdateUiState.Installing(info)
                        }
                    }
                    is InstallResultEvent.Success -> {
                        val version = lastUpdateInfo?.version ?: updateManager.currentVersionName
                        _updateState.value = UpdateUiState.Success(version)
                        emitNotice(tr(language.value, "Update imesakinishwa kikamilifu.", "Update installed successfully."))
                    }
                    is InstallResultEvent.Failure -> {
                        _updateState.value = UpdateUiState.Error(event.reason, lastUpdateInfo)
                    }
                }
            }
        }
    }

    // --- UI Preferences ---

    fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode)

    fun setDynamicColorEnabled(enabled: Boolean) = prefs.setDynamicColorEnabled(enabled)

    fun setHighContrastEnabled(enabled: Boolean) = prefs.setHighContrastEnabled(enabled)

    fun setLanguage(lang: AppLanguage) = prefs.setLanguage(lang)

    fun toggleLanguage() = prefs.toggleLanguage()

    fun setSelectedPeriod(period: PeriodFilter) {
        _selectedPeriod.value = period
    }

    fun setPrivacyMaskEnabled(enabled: Boolean) = prefs.setPrivacyMaskEnabled(enabled)

    fun togglePrivacyMask() = prefs.togglePrivacyMask()

    fun unlockWithPin(pin: String): Boolean {
        val ok = prefs.verifyAppPin(pin)
        if (ok) _isAppUnlocked.value = true
        return ok
    }

    fun lockAppNow() {
        if (prefs.pinLockEnabled.value) {
            _isAppUnlocked.value = false
        }
    }

    fun setupAppPin(pin: String): Boolean {
        val saved = prefs.saveAppPin(pin)
        if (saved) {
            _isAppUnlocked.value = true
            emitNotice(tr(language.value, "PIN ya ulinzi imewekwa salama.", "Security PIN enabled."))
        }
        return saved
    }

    fun disableAppPin() {
        prefs.clearAppPin()
        _isAppUnlocked.value = true
        emitNotice(tr(language.value, "Kufunga kwa PIN kumezimwa.", "PIN lock disabled."))
    }

    // --- Smart Auto-Entry & Transactions ---

    fun saveSmartAutoEntry(
        description: String,
        amount: Double,
        party: String,
        method: String,
        date: String,
        overrideBucket: AutoBucket? = null,
        markBillPaid: Boolean = true
    ) {
        viewModelScope.launch {
            val bucket = repository.saveSmartAutoEntry(
                description = description,
                amount = amount,
                party = party,
                method = method,
                date = date,
                overrideBucket = overrideBucket,
                markBillPaid = markBillPaid
            )
            _hasSafetySnapshot.value = repository.hasSafetySnapshot()
            val label = bucket.label(language.value)
            emitNotice(
                tr(
                    language.value,
                    "Imehifadhiwa kiotomatiki kama: $label",
                    "Auto-saved under: $label"
                )
            )
        }
    }

    fun reconcileLedgerNow() {
        viewModelScope.launch {
            val repaired = repository.reconcileLedgerIntegrity()
            repository.persistSafetySnapshot()
            _hasSafetySnapshot.value = repository.hasSafetySnapshot()
            val msg = if (repaired > 0) {
                tr(
                    language.value,
                    "Hesabu $repaired zimesawazishwa! 0% upotevu.",
                    "Reconciled $repaired entries! 0% loss verified."
                )
            } else {
                tr(
                    language.value,
                    "Hesabu zote zipo sawa — hakuna pesa iliyopotea!",
                    "All accounts balanced — zero missing entries!"
                )
            }
            emitNotice(msg)
        }
    }

    fun saveTransaction(txn: TransactionEntity) {
        viewModelScope.launch {
            repository.saveTransaction(txn)
            _hasSafetySnapshot.value = repository.hasSafetySnapshot()
            emitNotice(tr(language.value, "Muamala umehifadhiwa.", "Transaction saved."))
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            val deleted = repository.deleteTransaction(id) ?: return@launch
            emitNotice(
                message = tr(language.value, "Muamala umefutwa.", "Transaction deleted."),
                undoLabel = tr(language.value, "Tendua", "Undo"),
                onUndo = {
                    viewModelScope.launch {
                        repository.saveTransaction(deleted)
                        emitNotice(tr(language.value, "Imetenduliwa.", "Restored."))
                    }
                }
            )
        }
    }

    // --- Invoices ---

    suspend fun generateNextInvoiceNumber(): String = repository.nextInvoiceNumber()

    fun saveInvoice(invoice: InvoiceEntity, items: List<InvoiceItemEntity>) {
        viewModelScope.launch {
            repository.saveInvoice(invoice, items)
            emitNotice(tr(language.value, "Ankara imehifadhiwa.", "Invoice saved."))
        }
    }

    fun markInvoicePaid(invoiceId: String) {
        viewModelScope.launch {
            val before = repository.markInvoicePaid(invoiceId) ?: return@launch
            emitNotice(
                message = tr(
                    language.value,
                    "Ankara imewekwa imelipwa na kuingizwa kwenye mapato.",
                    "Invoice marked paid and synced to income."
                ),
                undoLabel = tr(language.value, "Tendua", "Undo"),
                onUndo = {
                    viewModelScope.launch {
                        repository.saveInvoice(before.invoice, before.orderedItems)
                        emitNotice(tr(language.value, "Imetenduliwa.", "Restored."))
                    }
                }
            )
        }
    }

    fun markInvoiceUnpaid(invoiceId: String) {
        viewModelScope.launch {
            repository.markInvoiceUnpaid(invoiceId)
            emitNotice(tr(language.value, "Ankara imerudishwa inadaiwa.", "Invoice marked unpaid."))
        }
    }

    fun recordInvoicePayment(invoiceId: String, addedAmount: Double) {
        viewModelScope.launch {
            val before = repository.recordInvoicePayment(invoiceId, addedAmount) ?: return@launch
            emitNotice(
                message = tr(
                    language.value,
                    "Malipo ya awamu yamerekodiwa na kuingizwa kwenye mapato.",
                    "Partial payment recorded and synced to income."
                ),
                undoLabel = tr(language.value, "Tendua", "Undo"),
                onUndo = {
                    viewModelScope.launch {
                        repository.saveInvoice(before.invoice, before.orderedItems)
                        emitNotice(tr(language.value, "Imetenduliwa.", "Restored."))
                    }
                }
            )
        }
    }

    fun duplicateInvoice(invoiceId: String) {
        viewModelScope.launch {
            val copy = repository.duplicateInvoice(invoiceId) ?: return@launch
            emitNotice(
                tr(
                    language.value,
                    "Nakala mpya ya ankara (${copy.number}) imeundwa.",
                    "Duplicated invoice as ${copy.number}."
                )
            )
        }
    }

    fun buildInvoiceShareText(item: InvoiceWithItems): String {
        val s = settings.value
        val cur = s.currency.ifBlank { "TZS" }
        val biz = s.businessName.ifBlank { "Mengidata" }
        val lang = language.value
        return buildString {
            appendLine("*$biz — ${tr(lang, "ANKARA", "INVOICE")} ${item.invoice.number}*")
            appendLine("${tr(lang, "Mteja", "Customer")}: ${item.invoice.customerName}")
            appendLine("${tr(lang, "Tarehe", "Date")}: ${item.invoice.issueDate} | ${tr(lang, "Mwisho", "Due")}: ${item.invoice.dueDate}")
            appendLine()
            item.orderedItems.forEach { line ->
                appendLine("• ${line.description} (${line.qty.toInt()} × ${FormatUtils.formatMoney(line.price, cur)}) = ${FormatUtils.formatMoney(line.lineTotal, cur)}")
            }
            appendLine()
            appendLine("${tr(lang, "Jumla Kuu", "Total")}: ${FormatUtils.formatMoney(item.total, cur)}")
            if (item.invoice.amountPaid > 0.005) {
                appendLine("${tr(lang, "Imelipwa", "Paid")}: ${FormatUtils.formatMoney(item.invoice.amountPaid, cur)}")
            }
            appendLine("*${tr(lang, "Salio", "Balance Due")}: ${FormatUtils.formatMoney(item.balance, cur)}*")
            if (s.paymentDetails.isNotBlank()) {
                appendLine()
                appendLine(tr(lang, "Maelezo ya Malipo:", "Payment Details:"))
                appendLine(s.paymentDetails)
            }
        }.trim()
    }

    fun deleteInvoice(invoiceId: String) {
        viewModelScope.launch {
            repository.deleteInvoice(invoiceId)
            emitNotice(tr(language.value, "Ankara imefutwa.", "Invoice deleted."))
        }
    }

    fun exportInvoicePdf(invoiceWithItems: InvoiceWithItems) {
        viewModelScope.launch {
            val bytes = InvoicePdfGenerator.generatePdfBytes(invoiceWithItems, settings.value)
            val safeNumber = invoiceWithItems.invoice.number.replace(Regex("[^A-Za-z0-9_-]"), "_")
            _exportRequests.emit(
                PendingFileExport(
                    filename = "Ankara_$safeNumber.pdf",
                    mimeType = "application/pdf",
                    bytes = bytes
                )
            )
        }
    }

    fun printInvoicePdf(invoiceWithItems: InvoiceWithItems) {
        viewModelScope.launch {
            val bytes = InvoicePdfGenerator.generatePdfBytes(invoiceWithItems, settings.value)
            val ok = InvoicePdfGenerator.printInvoicePdf(
                getApplication(),
                "Ankara ${invoiceWithItems.invoice.number}",
                bytes
            )
            if (!ok) {
                emitNotice(tr(language.value, "Chapishaji haupatikani.", "Print service unavailable."))
            }
        }
    }

    // --- Bills ---

    fun saveBill(bill: BillEntity) {
        viewModelScope.launch {
            repository.saveBill(bill)
            emitNotice(tr(language.value, "Bili imehifadhiwa.", "Bill saved."))
        }
    }

    fun toggleBillPaid(billId: String) {
        viewModelScope.launch {
            val before = repository.toggleBillPaid(billId) ?: return@launch
            emitNotice(
                message = if (before.paid) {
                    tr(language.value, "Malipo ya bili yametenduliwa.", "Bill marked unpaid.")
                } else {
                    tr(language.value, "Bili imelipwa na kurekodiwa kwenye matumizi.", "Bill marked paid and logged as expense.")
                },
                undoLabel = tr(language.value, "Tendua", "Undo"),
                onUndo = {
                    viewModelScope.launch {
                        repository.saveBill(before)
                        emitNotice(tr(language.value, "Imetenduliwa.", "Restored."))
                    }
                }
            )
        }
    }

    fun scheduleNextRecurringBill(billId: String) {
        viewModelScope.launch {
            val next = repository.scheduleNextRecurringBill(billId) ?: return@launch
            emitNotice(
                tr(
                    language.value,
                    "Bili ya mwezi ujao (${next.dueDate}) imepangwa!",
                    "Next month's recurring bill (${next.dueDate}) scheduled!"
                )
            )
        }
    }

    fun deleteBill(billId: String) {
        viewModelScope.launch {
            repository.deleteBill(billId)
            emitNotice(tr(language.value, "Bili imefutwa.", "Bill deleted."))
        }
    }

    // --- Savings (Akiba) ---

    fun saveSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.saveSavingsGoal(goal)
            emitNotice(tr(language.value, "Lengo la akiba limehifadhiwa.", "Savings goal saved."))
        }
    }

    fun deleteSavingsGoal(goalId: String) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goalId)
            emitNotice(tr(language.value, "Lengo la akiba limefutwa.", "Savings goal deleted."))
        }
    }

    fun recordSavingsTransaction(goalId: String, type: String, amount: Double, note: String) {
        viewModelScope.launch {
            repository.recordSavingsTransaction(goalId, type, amount, note)
            val msg = if (type == "deposit") {
                tr(language.value, "Fedha zimeongezwa kwenye akiba.", "Deposit recorded.")
            } else {
                tr(language.value, "Fedha zimetolewa kwenye akiba.", "Withdrawal recorded.")
            }
            emitNotice(msg)
        }
    }

    // --- Settings & Backups ---

    fun saveBusinessSettings(updated: BusinessSettingsEntity) {
        viewModelScope.launch {
            repository.saveSettings(updated)
            emitNotice(tr(language.value, "Mipangilio imehifadhiwa.", "Settings saved."))
        }
    }

    fun exportBackupJson() {
        viewModelScope.launch {
            val json = repository.exportBackupJson()
            _exportRequests.emit(
                PendingFileExport(
                    filename = "Mengidata_Backup_${FormatUtils.todayIso()}.json",
                    mimeType = "application/json",
                    bytes = json.toByteArray(Charsets.UTF_8)
                )
            )
        }
    }

    fun exportColorExcelReport() {
        viewModelScope.launch {
            val xml = repository.exportColorExcelReport(language.value)
            _exportRequests.emit(
                PendingFileExport(
                    filename = "Mengidata_Ripoti_Rangi_${FormatUtils.todayIso()}.xls",
                    mimeType = "application/vnd.ms-excel",
                    bytes = xml.toByteArray(Charsets.UTF_8)
                )
            )
        }
    }

    fun exportTransactionsCsv() {
        viewModelScope.launch {
            val csv = repository.exportTransactionsCsv(language.value)
            _exportRequests.emit(
                PendingFileExport(
                    filename = "Mengidata_Miamala_Pekee_${FormatUtils.todayIso()}.csv",
                    mimeType = "text/csv",
                    bytes = csv.toByteArray(Charsets.UTF_8)
                )
            )
        }
    }

    fun exportBillsCsv() {
        viewModelScope.launch {
            val csv = repository.exportBillsCsv(language.value)
            _exportRequests.emit(
                PendingFileExport(
                    filename = "Mengidata_Bili_Pekee_${FormatUtils.todayIso()}.csv",
                    mimeType = "text/csv",
                    bytes = csv.toByteArray(Charsets.UTF_8)
                )
            )
        }
    }

    fun exportSavingsCsv() {
        viewModelScope.launch {
            val csv = repository.exportSavingsCsv(language.value)
            _exportRequests.emit(
                PendingFileExport(
                    filename = "Mengidata_Akiba_Pekee_${FormatUtils.todayIso()}.csv",
                    mimeType = "text/csv",
                    bytes = csv.toByteArray(Charsets.UTF_8)
                )
            )
        }
    }

    fun exportMasterSeparatedCsv() {
        viewModelScope.launch {
            val csv = repository.exportMasterSeparatedCsv(language.value)
            _exportRequests.emit(
                PendingFileExport(
                    filename = "Mengidata_Ripoti_Tenganishwa_${FormatUtils.todayIso()}.csv",
                    mimeType = "text/csv",
                    bytes = csv.toByteArray(Charsets.UTF_8)
                )
            )
        }
    }

    fun exportCategoryAnalyticsCsv(period: PeriodFilter = selectedPeriod.value) {
        viewModelScope.launch {
            val csv = repository.exportCategoryAnalyticsCsv(period, language.value)
            _exportRequests.emit(
                PendingFileExport(
                    filename = "Mengidata_Uchambuzi_Makundi_${FormatUtils.todayIso()}.csv",
                    mimeType = "text/csv",
                    bytes = csv.toByteArray(Charsets.UTF_8)
                )
            )
        }
    }

    // --- Transaction Categories CRUD ---

    fun saveCategory(category: TransactionCategoryEntity, previousName: String? = null) {
        viewModelScope.launch {
            runCatching {
                repository.saveCategory(category, previousName)
            }.onSuccess { saved ->
                emitNotice(
                    tr(
                        language.value,
                        "Kundi \"${saved.name}\" limehifadhiwa.",
                        "Category \"${saved.name}\" saved."
                    )
                )
            }.onFailure {
                emitNotice(
                    tr(
                        language.value,
                        "Kundi lenye jina hili tayari lipo.",
                        "A category with this name already exists."
                    )
                )
            }
        }
    }

    fun deleteCategory(categoryId: String, reassignToCategory: String? = null) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId, reassignToCategory)
            emitNotice(
                tr(
                    language.value,
                    "Kundi limeondolewa na miamala imehamishwa salama.",
                    "Category removed and transactions reassigned safely."
                )
            )
        }
    }

    fun restoreLatestSafetySnapshot() {
        viewModelScope.launch {
            val ok = repository.restoreLatestSafetySnapshot()
            if (ok) {
                emitNotice(tr(language.value, "Hesabu zote zimerejeshwa kutoka Auto-Snapshot!", "All records restored from Auto-Snapshot!"))
            } else {
                emitNotice(tr(language.value, "Hakuna Auto-Snapshot ya kurejesha bado.", "No Auto-Snapshot available yet."))
            }
        }
    }

    fun importBackupJson(jsonText: String) {
        viewModelScope.launch {
            runCatching { repository.importBackupJson(jsonText) }
                .onSuccess {
                    repository.reconcileLedgerIntegrity()
                    repository.persistSafetySnapshot()
                    _hasSafetySnapshot.value = repository.hasSafetySnapshot()
                    emitNotice(tr(language.value, "Nakala rudufu imerejeshwa kikamilifu.", "Backup restored successfully."))
                }
                .onFailure {
                    emitNotice(tr(language.value, "Faili la backup si sahihi.", "Invalid Mengidata backup JSON file."))
                }
        }
    }

    fun loadDemoData() {
        viewModelScope.launch {
            repository.loadDemoData()
            emitNotice(tr(language.value, "Data ya mfano imepakiwa.", "Sample business data loaded."))
        }
    }

    fun clearAllFinancialData() {
        viewModelScope.launch {
            repository.clearFinancialData()
            emitNotice(tr(language.value, "Data zote za kifedha zimefutwa.", "All financial records cleared."))
        }
    }

    // --- Hardened Android 12+ Update System ---

    fun setAutoUpdateEnabled(enabled: Boolean) {
        prefs.setAutoUpdateEnabled(enabled)
        if (enabled) {
            checkForUpdates(silentIfUpToDate = false)
        }
    }

    fun checkForUpdates(silentIfUpToDate: Boolean = false) {
        viewModelScope.launch {
            _updateState.value = UpdateUiState.Checking
            updateManager.checkForUpdate()
                .onSuccess { info ->
                    if (info == null) {
                        lastUpdateInfo = null
                        _updateState.value = if (silentIfUpToDate) {
                            UpdateUiState.Idle
                        } else {
                            UpdateUiState.UpToDate(updateManager.currentVersionName)
                        }
                    } else {
                        lastUpdateInfo = info
                        _updateState.value = UpdateUiState.Available(info)
                    }
                }
                .onFailure { err ->
                    if (silentIfUpToDate) {
                        _updateState.value = UpdateUiState.Idle
                    } else {
                        _updateState.value = UpdateUiState.Error(
                            err.message ?: tr(language.value, "Imeshindwa kuangalia update.", "Failed to check for updates.")
                        )
                    }
                }
        }
    }

    fun startUpdateDownloadAndInstall(info: UpdateInfo) {
        lastUpdateInfo = info
        if (!updateManager.canRequestPackageInstalls()) {
            _updateState.value = UpdateUiState.PermissionRequired(info)
            return
        }
        viewModelScope.launch {
            _updateState.value = UpdateUiState.Downloading(info, 0, 0L, info.expectedSizeBytes)
            updateManager.downloadAndVerifyApk(
                info = info,
                onProgress = { pct, downloaded, total ->
                    _updateState.value = UpdateUiState.Downloading(info, pct, downloaded, total)
                },
                onVerifying = {
                    _updateState.value = UpdateUiState.Verifying(info)
                }
            ).onSuccess { verifiedApk ->
                _updateState.value = UpdateUiState.Installing(info)
                updateManager.installVerifiedApk(verifiedApk)
                    .onFailure { err ->
                        _updateState.value = UpdateUiState.Error(
                            err.message ?: tr(language.value, "Imeshindwa kuanzisha usakinishaji.", "Install failed."),
                            retryInfo = info
                        )
                    }
            }.onFailure { err ->
                _updateState.value = UpdateUiState.Error(
                    err.message ?: tr(language.value, "Imeshindwa kupakua au kuthibitisha APK.", "Download or signature verification failed."),
                    retryInfo = info
                )
            }
        }
    }

    fun onResumeCheckInstallPermission() {
        val state = _updateState.value
        if (state is UpdateUiState.PermissionRequired && updateManager.canRequestPackageInstalls()) {
            _updateState.value = UpdateUiState.Available(state.info)
        }
    }

    fun openInstallPermissionSettings(): Intent = updateManager.buildInstallPermissionIntent()

    fun openReleasesInBrowser(): Intent = updateManager.buildReleasesBrowserIntent()

    fun notifyFileSaved() {
        emitNotice(tr(language.value, "Faili limehifadhiwa.", "File saved."))
    }

    fun notifyFileError() {
        emitNotice(tr(language.value, "Imeshindwa kuhifadhi faili.", "Failed to save file."))
    }

    private fun emitNotice(message: String, undoLabel: String? = null, onUndo: (() -> Unit)? = null) {
        _notices.tryEmit(SnackbarNotice(message, undoLabel, onUndo))
    }
}
