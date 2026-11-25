package tz.mengidata.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
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
import tz.mengidata.app.data.db.DeletedRecordEntity
import tz.mengidata.app.data.db.InvoiceEntity
import tz.mengidata.app.data.db.InvoiceItemEntity
import tz.mengidata.app.data.db.InvoiceWithItems
import tz.mengidata.app.data.db.MengiDatabase
import tz.mengidata.app.data.db.PhoneStorageStatus
import tz.mengidata.app.data.db.SavingsGoalEntity
import tz.mengidata.app.data.db.SavingsLogEntity
import tz.mengidata.app.data.db.TransactionCategoryEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.data.pdf.FinancialStatementPdfGenerator
import tz.mengidata.app.data.pdf.InvoicePdfGenerator
import java.io.File
import tz.mengidata.app.data.prefs.AppPreferences
import tz.mengidata.app.data.repository.FinanceRepository
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.PeriodFilter
import tz.mengidata.app.i18n.ThemeMode
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.components.MonthlyChartPoint
import tz.mengidata.app.ui.components.MonthlyTrendPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import tz.mengidata.app.notifications.LargeBillWarningPopup
import tz.mengidata.app.notifications.MengiNotificationManager
import tz.mengidata.app.notifications.PhonePermissionsSnapshot
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
    val periodHouseholdBills: Double = 0.0,
    val periodSavingsOutflow: Double = 0.0,
    val periodTotalOutflow: Double = 0.0,
    val todayHouseholdBills: Double = 0.0,
    val netProfit: Double = 0.0,
    val receivablesBalance: Double = 0.0,
    val unpaidBillsTotal: Double = 0.0,
    val overdueBillsCount: Int = 0,
    val totalSavings: Double = 0.0,
    val totalSavingsTarget: Double = 0.0,
    val chartPoints: List<MonthlyChartPoint> = emptyList(),
    val spendingTrendPoints: List<MonthlyTrendPoint> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList()
)

class MengiViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPreferences(application)
    private val repository = FinanceRepository(application, MengiDatabase.getInstance(application), prefs)
    val updateManager = AppUpdateManager(application)
    val notificationManager = MengiNotificationManager(application)

    val themeMode: StateFlow<ThemeMode> = prefs.themeMode
    val dynamicColorEnabled: StateFlow<Boolean> = prefs.dynamicColorEnabled
    val highContrastEnabled: StateFlow<Boolean> = prefs.highContrastEnabled
    val language: StateFlow<AppLanguage> = prefs.language
    val autoUpdateEnabled: StateFlow<Boolean> = prefs.autoUpdateEnabled
    val privacyMaskEnabled: StateFlow<Boolean> = prefs.privacyMaskEnabled
    val pinLockEnabled: StateFlow<Boolean> = prefs.pinLockEnabled
    val notificationsEnabled: StateFlow<Boolean> = prefs.notificationsEnabled
    val billAlertsEnabled: StateFlow<Boolean> = prefs.billAlertsEnabled
    val transactionAlertsEnabled: StateFlow<Boolean> = prefs.transactionAlertsEnabled
    val savingsAlertsEnabled: StateFlow<Boolean> = prefs.savingsAlertsEnabled
    val systemAlertsEnabled: StateFlow<Boolean> = prefs.systemAlertsEnabled
    val largeBillThreshold: StateFlow<Double> = prefs.largeBillThreshold

    private val _isAppUnlocked = MutableStateFlow(!prefs.pinLockEnabled.value)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _phonePermissions = MutableStateFlow(notificationManager.getPermissionsSnapshot())
    val phonePermissions: StateFlow<PhonePermissionsSnapshot> = _phonePermissions.asStateFlow()

    private val _largeBillWarningPopup = MutableStateFlow<LargeBillWarningPopup?>(null)
    val largeBillWarningPopup: StateFlow<LargeBillWarningPopup?> = _largeBillWarningPopup.asStateFlow()
    private var warningDismissJob: Job? = null

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

    val deletedRecords: StateFlow<List<DeletedRecordEntity>> = repository.deletedRecordsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
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
        // Personal/system expenses, Household Bills, and Savings (Akiba)
        // are ALL deducted normally from system balance, while each stays clearly identified!
        val personalExpense = periodTxns
            .filter {
                !it.isIncome &&
                    !AutoAccountingEngine.isHouseholdTransaction(it) &&
                    !AutoAccountingEngine.isSavingsTransaction(it)
            }
            .sumOf { it.amount }
        val periodHousehold = periodTxns
            .filter { AutoAccountingEngine.isHouseholdTransaction(it) }
            .sumOf { it.amount }
        val periodSavingsOutflow = periodTxns
            .filter { !it.isIncome && AutoAccountingEngine.isSavingsTransaction(it) }
            .sumOf { it.amount }
        val totalOutflow = personalExpense + periodHousehold + periodSavingsOutflow

        val receivables = invs.filter { !it.isDraft }.sumOf { it.balance }
        val unpaidBills = bls.filter { !it.paid }
        val today = FormatUtils.todayIso()
        val todayHousehold = txns
            .filter { it.date == today && AutoAccountingEngine.isHouseholdTransaction(it) }
            .sumOf { it.amount }
        val chart = (-5..0).map { offset ->
            val key = FormatUtils.monthKey(FormatUtils.addMonthsIso(today, offset))
            val mTxns = txns.filter { FormatUtils.monthKey(it.date) == key }
            MonthlyChartPoint(
                label = FormatUtils.shortMonthLabel(key, lang),
                income = mTxns.filter { it.isIncome }.sumOf { it.amount },
                expense = mTxns
                    .filter { !it.isIncome }
                    .sumOf { it.amount }
            )
        }

        // Monthly spending trends comparing Household Bills, Personal Expenses, and Savings Progress
        val totalVaultSavings = goals.sumOf { it.currentAmount }
        val monthOffsets = (-5..0).toList()
        val rawMonthlyTrends = monthOffsets.map { offset ->
            val key = FormatUtils.monthKey(FormatUtils.addMonthsIso(today, offset))
            val mTxns = txns.filter { FormatUtils.monthKey(it.date) == key }
            val mHousehold = mTxns
                .filter { AutoAccountingEngine.isHouseholdTransaction(it) }
                .sumOf { it.amount }
            val mPersonal = mTxns
                .filter {
                    !it.isIncome &&
                        !AutoAccountingEngine.isHouseholdTransaction(it) &&
                        !AutoAccountingEngine.isSavingsTransaction(it)
                }
                .sumOf { it.amount }
            val mSavingsDeposit = mTxns
                .filter { !it.isIncome && AutoAccountingEngine.isSavingsTransaction(it) }
                .sumOf { it.amount }
            val mSavingsWithdraw = mTxns
                .filter { it.isIncome && AutoAccountingEngine.isSavingsTransaction(it) }
                .sumOf { it.amount }
            Triple(key, Triple(mHousehold, mPersonal, mSavingsDeposit), mSavingsDeposit - mSavingsWithdraw)
        }
        val runningCumulative = DoubleArray(rawMonthlyTrends.size)
        var cursorSavings = totalVaultSavings
        for (i in rawMonthlyTrends.indices.reversed()) {
            runningCumulative[i] = cursorSavings.coerceAtLeast(0.0)
            cursorSavings = (cursorSavings - rawMonthlyTrends[i].third).coerceAtLeast(0.0)
        }
        val spendingTrends = rawMonthlyTrends.mapIndexed { idx, (key, buckets, _) ->
            MonthlyTrendPoint(
                monthKey = key,
                label = FormatUtils.shortMonthLabel(key, lang),
                householdBills = buckets.first,
                personalExpenses = buckets.second,
                savingsDeposited = buckets.third,
                cumulativeSavings = runningCumulative[idx]
            )
        }

        DashboardMetrics(
            periodIncome = income,
            periodExpense = personalExpense,
            periodHouseholdBills = periodHousehold,
            periodSavingsOutflow = periodSavingsOutflow,
            periodTotalOutflow = totalOutflow,
            todayHouseholdBills = todayHousehold,
            netProfit = income - totalOutflow,
            receivablesBalance = receivables,
            unpaidBillsTotal = unpaidBills.sumOf { it.amount },
            overdueBillsCount = unpaidBills.count { it.isOverdue },
            totalSavings = totalVaultSavings,
            totalSavingsTarget = goals.sumOf { it.targetAmount },
            chartPoints = chart,
            spendingTrendPoints = spendingTrends,
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
        bills,
        savingsGoals,
        savingsLogs
    ) { txns, invs, bls, goals, logs ->
        PartyDirectoryEngine.buildDirectory(txns, invs, bls, goals, logs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _hasSafetySnapshot = MutableStateFlow(repository.hasSafetySnapshot())
    val hasSafetySnapshot: StateFlow<Boolean> = _hasSafetySnapshot.asStateFlow()

    private val _phoneStorageStatus = MutableStateFlow(repository.getPhoneStorageStatus())
    val phoneStorageStatus: StateFlow<PhoneStorageStatus> = _phoneStorageStatus.asStateFlow()

    private fun refreshStorageStatus() {
        val status = repository.getPhoneStorageStatus()
        _phoneStorageStatus.value = status
        _hasSafetySnapshot.value = status.hasAnyBackup
        _phonePermissions.value = notificationManager.getPermissionsSnapshot(status.hasAnyBackup)
    }

    fun refreshPhonePermissions() {
        _phonePermissions.value = notificationManager.getPermissionsSnapshot(_phoneStorageStatus.value.hasAnyBackup)
    }

    fun dismissLargeBillWarningPopup() {
        warningDismissJob?.cancel()
        _largeBillWarningPopup.value = null
    }

    fun triggerLargeBillWarning(
        billName: String,
        amount: Double,
        category: String = "Bili",
        forceForTest: Boolean = false
    ) {
        val threshold = prefs.largeBillThreshold.value
        val cur = settings.value.currency.ifBlank { "TZS" }
        val cleanName = billName.ifBlank { tr(language.value, "Bili Kubwa", "Large Bill") }
        val cleanCategory = category.ifBlank { tr(language.value, "Bili", "Bill") }
        refreshPhonePermissions()
        if (forceForTest || (prefs.notificationsEnabled.value && prefs.billAlertsEnabled.value)) {
            val posted = notificationManager.notifyLargeBillWarning(
                billName = cleanName,
                amount = amount,
                threshold = threshold,
                category = cleanCategory,
                currency = cur,
                language = language.value
            )
            if (forceForTest && !posted) {
                emitNotice(
                    tr(
                        language.value,
                        "Washa ruhusa ya Taarifa za Simu (POST_NOTIFICATIONS) kwanza.",
                        "Please grant POST_NOTIFICATIONS permission first."
                    )
                )
            }
        }
    }

    private var lastUpdateInfo: UpdateInfo? = null

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
            refreshStorageStatus()
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
                        emitNotice(tr(language.value, "Sasisho limesakinishwa kikamilifu.", "Update installed successfully."))
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
            emitNotice(tr(language.value, "Namba ya siri ya ulinzi imewekwa salama.", "Security PIN enabled."))
        }
        return saved
    }

    fun disableAppPin() {
        prefs.clearAppPin()
        _isAppUnlocked.value = true
        emitNotice(tr(language.value, "Kufunga kwa namba ya siri kumezimwa.", "PIN lock disabled."))
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
            refreshStorageStatus()
            val label = bucket.label(language.value)
            val threshold = prefs.largeBillThreshold.value
            if ((bucket == AutoBucket.HOUSEHOLD_BILL || bucket == AutoBucket.EXPENSE) && amount >= threshold) {
                triggerLargeBillWarning(
                    billName = description.ifBlank { label },
                    amount = amount,
                    category = label
                )
            }
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
            refreshStorageStatus()
            val cur = settings.value.currency.ifBlank { "TZS" }
            if (!txn.isIncome && txn.amount >= prefs.largeBillThreshold.value) {
                triggerLargeBillWarning(
                    billName = txn.description.ifBlank { txn.category },
                    amount = txn.amount,
                    category = txn.category
                )
            } else if (prefs.notificationsEnabled.value && prefs.transactionAlertsEnabled.value) {
                notificationManager.notifyTransactionSaved(txn, cur, language.value)
            }
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
            refreshStorageStatus()
            if (prefs.notificationsEnabled.value && prefs.billAlertsEnabled.value) {
                val total = items.sumOf { it.lineTotal }
                notificationManager.notifyInvoiceEvent(
                    invoiceNumber = invoice.number,
                    clientName = invoice.customerName,
                    amount = total,
                    currency = settings.value.currency.ifBlank { "TZS" },
                    language = language.value,
                    isPayment = false
                )
            }
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
            refreshStorageStatus()
            emitNotice(
                tr(
                    language.value,
                    "Ankara imehamishwa kwenye Kikapu cha Urejeshaji (unaweza kuirejesha).",
                    "Invoice moved to Recovery Vault (can be restored anytime)."
                )
            )
        }
    }

    fun exportInvoicePdf(invoiceWithItems: InvoiceWithItems) {
        viewModelScope.launch {
            val lang = language.value
            val bytes = InvoicePdfGenerator.generatePdfBytes(invoiceWithItems, settings.value, lang)
            val safeNumber = invoiceWithItems.invoice.number.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val prefix = if (lang == AppLanguage.EN) "Invoice" else "Ankara"
            _exportRequests.emit(
                PendingFileExport(
                    filename = "${prefix}_$safeNumber.pdf",
                    mimeType = "application/pdf",
                    bytes = bytes
                )
            )
        }
    }

    fun printInvoicePdf(invoiceWithItems: InvoiceWithItems) {
        viewModelScope.launch {
            val lang = language.value
            val bytes = InvoicePdfGenerator.generatePdfBytes(invoiceWithItems, settings.value, lang)
            val titlePrefix = if (lang == AppLanguage.EN) "Invoice" else "Ankara"
            val ok = InvoicePdfGenerator.printInvoicePdf(
                getApplication(),
                "$titlePrefix ${invoiceWithItems.invoice.number}",
                bytes
            )
            if (!ok) {
                emitNotice(tr(lang, "Chapishaji haupatikani.", "Print service unavailable."))
            }
        }
    }

    // --- Bills ---

    fun saveBill(bill: BillEntity) {
        viewModelScope.launch {
            repository.saveBill(bill)
            refreshStorageStatus()
            val cur = settings.value.currency.ifBlank { "TZS" }
            if (bill.amount >= prefs.largeBillThreshold.value) {
                triggerLargeBillWarning(
                    billName = bill.name,
                    amount = bill.amount,
                    category = bill.category
                )
            } else if (prefs.notificationsEnabled.value && prefs.billAlertsEnabled.value) {
                notificationManager.notifyBillEvent(
                    bill = bill,
                    currency = cur,
                    language = language.value,
                    justPaid = bill.paid
                )
            }
            emitNotice(tr(language.value, "Bili imehifadhiwa.", "Bill saved."))
        }
    }

    fun toggleBillPaid(billId: String) {
        viewModelScope.launch {
            val before = repository.toggleBillPaid(billId) ?: return@launch
            refreshStorageStatus()
            if (!before.paid) {
                if (before.amount >= prefs.largeBillThreshold.value) {
                    triggerLargeBillWarning(
                        billName = before.name,
                        amount = before.amount,
                        category = before.category
                    )
                } else if (prefs.notificationsEnabled.value && prefs.billAlertsEnabled.value) {
                    notificationManager.notifyBillEvent(
                        bill = before.copy(paid = true),
                        currency = settings.value.currency.ifBlank { "TZS" },
                        language = language.value,
                        justPaid = true
                    )
                }
            }
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
            refreshStorageStatus()
            emitNotice(
                tr(
                    language.value,
                    "Bili imehamishwa kwenye Kikapu cha Urejeshaji.",
                    "Bill moved to Recovery Vault."
                )
            )
        }
    }

    fun duplicateBill(bill: BillEntity) {
        viewModelScope.launch {
            val nextDate = FormatUtils.addMonthsIso(bill.dueDate.ifBlank { FormatUtils.todayIso() }, 1)
            repository.saveBill(
                bill.copy(
                    id = "",
                    paid = false,
                    paidDate = "",
                    linkedTxnId = "",
                    dueDate = nextDate
                )
            )
            refreshStorageStatus()
            emitNotice(
                tr(
                    language.value,
                    "Nakala ya bili (${bill.name}) imeongezwa kwa $nextDate.",
                    "Bill duplicated (${bill.name}) for $nextDate."
                )
            )
        }
    }

    fun payAllDueBills() {
        viewModelScope.launch {
            val today = FormatUtils.todayIso()
            val targets = bills.value.filter { !it.paid && (it.isOverdue || it.dueDate <= today) }
            if (targets.isEmpty()) {
                emitNotice(
                    tr(
                        language.value,
                        "Hakuna bili zilizochelewa au zinazodaiwa leo.",
                        "No overdue or due-today bills to pay."
                    )
                )
                return@launch
            }
            targets.forEach { bill ->
                repository.toggleBillPaid(bill.id)
            }
            refreshStorageStatus()
            emitNotice(
                tr(
                    language.value,
                    "Bili ${targets.size} zimelipwa na kukatwa kwenye mfumo.",
                    "${targets.size} due bills marked paid and deducted."
                )
            )
        }
    }

    fun duplicateTransaction(txn: TransactionEntity) {
        viewModelScope.launch {
            repository.saveTransaction(
                txn.copy(
                    id = "",
                    date = FormatUtils.todayIso(),
                    linkedId = "",
                    source = if (txn.source == "bill" || txn.source == "invoice") "manual" else txn.source
                )
            )
            refreshStorageStatus()
            emitNotice(
                tr(
                    language.value,
                    "Nakala ya muamala imehifadhiwa kwa leo.",
                    "Transaction duplicated for today."
                )
            )
        }
    }

    fun buildShareTransactionReceiptIntent(txn: TransactionEntity): Intent {
        val s = settings.value
        val lang = language.value
        val currency = s.currency.ifBlank { "TZS" }
        val bucket = AutoAccountingEngine.classifyTransaction(txn)
        val business = s.businessName.ifBlank { "Mengidata" }
        val text = buildString {
            appendLine("$business — ${tr(lang, "RISITI YA MUAMALA", "TRANSACTION RECEIPT")}")
            appendLine("────────────────────────")
            appendLine("${tr(lang, "Aina", "Category")}: ${bucket.label(lang)}")
            appendLine("${tr(lang, "Maelezo", "Description")}: ${txn.description}")
            if (txn.party.isNotBlank()) {
                appendLine("${tr(lang, "Mhusika", "Contact")}: ${txn.party}")
            }
            appendLine("${tr(lang, "Kiasi", "Amount")}: ${FormatUtils.formatMoney(txn.amount, currency)}")
            appendLine("${tr(lang, "Njia ya Malipo", "Payment Method")}: ${FormatUtils.localizedMethod(txn.method, lang)}")
            appendLine("${tr(lang, "Tarehe", "Date")}: ${txn.date}")
            if (txn.reference.isNotBlank()) {
                appendLine("${tr(lang, "Kumbukumbu", "Reference")}: ${txn.reference}")
            }
            if (txn.notes.isNotBlank()) {
                appendLine("${tr(lang, "Maelezo ya ziada", "Notes")}: ${txn.notes}")
            }
        }
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "$business · ${txn.description}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }

    fun buildShareInvoicePdfIntent(invoiceWithItems: InvoiceWithItems): Intent? {
        val app = getApplication<Application>()
        val lang = language.value
        val pdfBytes = InvoicePdfGenerator.generatePdfBytes(invoiceWithItems, settings.value, lang)
        val filePrefix = tr(lang, "Ankara", "Invoice")
        val filename = "${filePrefix}_${invoiceWithItems.invoice.number.replace(" ", "_")}.pdf"
        return buildSharePdfFileIntent(
            app = app,
            filename = filename,
            pdfBytes = pdfBytes,
            subject = "${settings.value.businessName.ifBlank { "Mengidata" }} · $filePrefix ${invoiceWithItems.invoice.number}",
            bodyText = tr(
                lang,
                "Tafadhali pokea ankara ya PDF namba ${invoiceWithItems.invoice.number} kwa ${invoiceWithItems.invoice.customerName}.",
                "Please find attached invoice PDF ${invoiceWithItems.invoice.number} for ${invoiceWithItems.invoice.customerName}."
            )
        )
    }

    fun exportFinancialStatementPdf(period: PeriodFilter = selectedPeriod.value) {
        viewModelScope.launch {
            val lang = language.value
            val pdfBytes = FinancialStatementPdfGenerator.generateStatementPdfBytes(
                settings = settings.value,
                transactions = transactions.value,
                bills = bills.value,
                invoices = invoices.value,
                savingsGoals = savingsGoals.value,
                period = period,
                language = lang
            )
            val prefix = tr(lang, "Mengidata_Taarifa_Fedha", "Mengidata_Financial_Statement")
            _exportRequests.emit(
                PendingFileExport(
                    filename = "${prefix}_${period.name.lowercase()}_${FormatUtils.todayIso()}.pdf",
                    mimeType = "application/pdf",
                    bytes = pdfBytes
                )
            )
        }
    }

    fun buildShareStatementPdfIntent(period: PeriodFilter = selectedPeriod.value): Intent? {
        val app = getApplication<Application>()
        val lang = language.value
        val pdfBytes = FinancialStatementPdfGenerator.generateStatementPdfBytes(
            settings = settings.value,
            transactions = transactions.value,
            bills = bills.value,
            invoices = invoices.value,
            savingsGoals = savingsGoals.value,
            period = period,
            language = lang
        )
        val prefix = tr(lang, "Mengidata_Taarifa_Fedha", "Mengidata_Financial_Statement")
        val filename = "${prefix}_${period.name.lowercase()}_${FormatUtils.todayIso()}.pdf"
        return buildSharePdfFileIntent(
            app = app,
            filename = filename,
            pdfBytes = pdfBytes,
            subject = "${settings.value.businessName.ifBlank { "Mengidata" }} · ${tr(lang, "Taarifa ya Fedha", "Financial Statement")}",
            bodyText = tr(
                lang,
                "Taarifa ya fedha ya PDF (${period.labelSw}) kutoka ${settings.value.businessName.ifBlank { "Mengidata" }}.",
                "Financial statement PDF (${period.labelEn}) from ${settings.value.businessName.ifBlank { "Mengidata" }}."
            )
        )
    }

    private fun buildSharePdfFileIntent(
        app: Application,
        filename: String,
        pdfBytes: ByteArray,
        subject: String,
        bodyText: String
    ): Intent? = runCatching {
        val dir = File(app.cacheDir, "shared_pdfs").apply { if (!exists()) mkdirs() }
        val file = File(dir, filename)
        file.writeBytes(pdfBytes)
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, bodyText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }.getOrNull()

    // --- Savings (Akiba) ---

    fun saveSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.saveSavingsGoal(goal)
            refreshStorageStatus()
            emitNotice(tr(language.value, "Lengo la akiba limehifadhiwa.", "Savings goal saved."))
        }
    }

    fun deleteSavingsGoal(goalId: String) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goalId)
            refreshStorageStatus()
            emitNotice(
                tr(
                    language.value,
                    "Lengo la akiba limehamishwa kwenye Kikapu cha Urejeshaji.",
                    "Savings goal moved to Recovery Vault."
                )
            )
        }
    }

    fun recordSavingsTransaction(
        goalId: String,
        type: String,
        amount: Double,
        note: String,
        partyName: String = ""
    ) {
        viewModelScope.launch {
            repository.recordSavingsTransaction(goalId, type, amount, note, partyName)
            refreshStorageStatus()
            val goal = savingsGoals.value.firstOrNull { it.id == goalId }
            if (goal != null && prefs.notificationsEnabled.value && prefs.savingsAlertsEnabled.value) {
                val newTotal = if (type == "deposit") goal.currentAmount + amount else (goal.currentAmount - amount).coerceAtLeast(0.0)
                notificationManager.notifySavingsActivity(
                    goalName = goal.title,
                    amount = amount,
                    newTotal = newTotal,
                    targetAmount = goal.targetAmount,
                    isDeposit = type == "deposit",
                    currency = settings.value.currency.ifBlank { "TZS" },
                    language = language.value
                )
            }
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

    fun syncDatabaseToPhoneStorage() {
        viewModelScope.launch {
            val ok = repository.syncToPhoneStorageNow()
            refreshStorageStatus()
            if (ok) {
                emitNotice(
                    tr(
                        language.value,
                        "Hifadhidata imesawazishwa salama kwenye hifadhi ya simu!",
                        "Database safely synced to phone storage!"
                    )
                )
            } else {
                emitNotice(
                    tr(
                        language.value,
                        "Imeshindwa kuhifadhi nakala kwenye hifadhi ya simu.",
                        "Failed to sync backup to phone storage."
                    )
                )
            }
        }
    }

    fun restoreDeletedRecord(trashId: String) {
        viewModelScope.launch {
            val ok = repository.restoreDeletedRecord(trashId)
            refreshStorageStatus()
            if (ok) {
                emitNotice(
                    tr(
                        language.value,
                        "Rekodi imerejeshwa kikamilifu kwenye mfumo!",
                        "Record restored to the database!"
                    )
                )
            }
        }
    }

    fun permanentlyDeleteTrashRecord(trashId: String) {
        viewModelScope.launch {
            repository.permanentlyDeleteTrashRecord(trashId)
            emitNotice(tr(language.value, "Rekodi imeondolewa kabisa.", "Record permanently removed."))
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            emitNotice(tr(language.value, "Kikapu cha urejeshaji kimesafishwa.", "Recovery Vault emptied."))
        }
    }

    fun restoreLatestSafetySnapshot() {
        viewModelScope.launch {
            val ok = repository.restoreLatestSafetySnapshot()
            refreshStorageStatus()
            if (ok) {
                emitNotice(tr(language.value, "Hesabu zote zimerejeshwa kutoka hifadhi ya simu!", "All records restored from phone storage backup!"))
            } else {
                emitNotice(tr(language.value, "Hakuna hifadhi ya usalama ya kurejesha bado.", "No safety snapshot available yet."))
            }
        }
    }

    fun importBackupJson(jsonText: String) {
        viewModelScope.launch {
            runCatching { repository.importBackupJson(jsonText) }
                .onSuccess {
                    repository.reconcileLedgerIntegrity()
                    repository.persistSafetySnapshot()
                    refreshStorageStatus()
                    emitNotice(tr(language.value, "Nakala rudufu imerejeshwa kikamilifu.", "Backup restored successfully."))
                }
                .onFailure {
                    emitNotice(tr(language.value, "Faili la nakala rudufu si sahihi.", "Invalid Mengidata backup JSON file."))
                }
        }
    }

    fun loadDemoData() {
        viewModelScope.launch {
            repository.loadDemoData()
            repository.persistSafetySnapshot()
            refreshStorageStatus()
            emitNotice(tr(language.value, "Data ya mfano imepakiwa.", "Sample business data loaded."))
        }
    }

    fun clearAllFinancialData() {
        viewModelScope.launch {
            repository.clearFinancialData()
            refreshStorageStatus()
            emitNotice(
                tr(
                    language.value,
                    "Data zimefutwa — nakala ya dharura imehifadhiwa kwenye hifadhi ya simu.",
                    "Financial data cleared — an emergency backup was saved to phone storage."
                )
            )
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
                            err.message ?: tr(language.value, "Imeshindwa kukagua sasisho.", "Failed to check for updates.")
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
            // Proactively sync the database to phone storage (Documents/Mengidata) before updating
            runCatching { repository.syncToPhoneStorageNow() }
            refreshStorageStatus()

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
        refreshPhonePermissions()
        val state = _updateState.value
        if (state is UpdateUiState.PermissionRequired && updateManager.canRequestPackageInstalls()) {
            _updateState.value = UpdateUiState.Available(state.info)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.setNotificationsEnabled(enabled)
        refreshPhonePermissions()
    }

    fun setBillAlertsEnabled(enabled: Boolean) = prefs.setBillAlertsEnabled(enabled)

    fun setTransactionAlertsEnabled(enabled: Boolean) = prefs.setTransactionAlertsEnabled(enabled)

    fun setSavingsAlertsEnabled(enabled: Boolean) = prefs.setSavingsAlertsEnabled(enabled)

    fun setSystemAlertsEnabled(enabled: Boolean) = prefs.setSystemAlertsEnabled(enabled)

    fun setLargeBillThreshold(amount: Double) {
        prefs.setLargeBillThreshold(amount)
        val cur = settings.value.currency.ifBlank { "TZS" }
        emitNotice(
            tr(
                language.value,
                "Kiwango cha Onyo la Bili Kubwa kimewekwa: ${FormatUtils.formatMoney(prefs.largeBillThreshold.value, cur)}",
                "Large Bill Warning threshold set to: ${FormatUtils.formatMoney(prefs.largeBillThreshold.value, cur)}"
            )
        )
    }

    fun sendTestLargeBillWarning() {
        val threshold = prefs.largeBillThreshold.value
        triggerLargeBillWarning(
            billName = tr(language.value, "Bili ya Mfano (Kodi / Umeme)", "Sample Large Bill (Rent / Power)"),
            amount = threshold * 1.25,
            category = tr(language.value, "Bili ya Nyumbani", "Household Bill"),
            forceForTest = true
        )
    }

    fun sendTestSystemNotification() {
        refreshPhonePermissions()
        val ok = notificationManager.sendTestNotification(
            language = language.value,
            businessName = settings.value.businessName.ifBlank { "Mengidata" }
        )
        if (ok) {
            emitNotice(
                tr(
                    language.value,
                    "Taarifa ya majaribio imetumwa kwenye simu yako!",
                    "Test notification sent to your phone!"
                )
            )
        } else {
            emitNotice(
                tr(
                    language.value,
                    "Washa ruhusa ya Taarifa za Simu (POST_NOTIFICATIONS) kwanza.",
                    "Please grant POST_NOTIFICATIONS permission first."
                )
            )
        }
    }

    fun runDueBillsAndInvoicesReminderNow() {
        refreshPhonePermissions()
        val count = notificationManager.runSmartDueReminders(
            bills = bills.value,
            invoices = invoices.value,
            currency = settings.value.currency.ifBlank { "TZS" },
            language = language.value
        )
        if (count > 0) {
            emitNotice(
                tr(
                    language.value,
                    "Ukumbusho wa malipo ($count) umetumwa kwenye taarifa za simu.",
                    "Payment reminder ($count due items) posted to phone notifications."
                )
            )
        } else {
            emitNotice(
                tr(
                    language.value,
                    "Hakuna bili au ankara zilizochelewa leo.",
                    "No overdue bills or unpaid due invoices today."
                )
            )
        }
    }

    fun openInstallPermissionSettings(): Intent = updateManager.buildInstallPermissionIntent()

    fun openAppNotificationSettings(): Intent = notificationManager.buildAppNotificationSettingsIntent()

    fun openAppDetailsSettings(): Intent = notificationManager.buildAppDetailsSettingsIntent()

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
