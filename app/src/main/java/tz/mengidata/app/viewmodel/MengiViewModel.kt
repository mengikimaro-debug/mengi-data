package tz.mengidata.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tz.mengidata.app.system.DevicePerformanceEngine
import tz.mengidata.app.system.DevicePerformanceProfile
import tz.mengidata.app.system.PerformanceModePreference
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
import tz.mengidata.app.data.db.PendingTransactionEntity
import tz.mengidata.app.data.db.PhoneStorageStatus
import tz.mengidata.app.data.db.SavedPublicPdf
import tz.mengidata.app.data.db.SavingsGoalEntity
import tz.mengidata.app.data.db.SavingsLogEntity
import tz.mengidata.app.data.db.TransactionCategoryEntity
import tz.mengidata.app.data.db.TransactionEntity
import tz.mengidata.app.data.pdf.FinancialStatementPdfGenerator
import tz.mengidata.app.data.pdf.InvoicePdfGenerator
import java.io.File
import tz.mengidata.app.data.currency.CurrencyExchangeService
import tz.mengidata.app.data.prefs.AppPreferences
import tz.mengidata.app.data.repository.FinanceRepository
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.i18n.PeriodFilter
import tz.mengidata.app.i18n.ThemeMode
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.components.MonthlyChartPoint
import tz.mengidata.app.ui.components.MonthlyTrendPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import tz.mengidata.app.notifications.EveningReminderScheduler
import tz.mengidata.app.notifications.LargeBillWarningPopup
import tz.mengidata.app.notifications.MengiNotificationManager
import tz.mengidata.app.notifications.PhonePermissionsSnapshot
import tz.mengidata.app.security.BiometricAvailability
import tz.mengidata.app.security.UninstallGuardSession
import tz.mengidata.app.security.UninstallGuardStatus
import tz.mengidata.app.security.UninstallProtectionManager
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
    val performanceMode: StateFlow<PerformanceModePreference> = prefs.performanceMode
    val language: StateFlow<AppLanguage> = prefs.language
    val autoUpdateEnabled: StateFlow<Boolean> = prefs.autoUpdateEnabled
    val autoPhoneBackupEnabled: StateFlow<Boolean> = prefs.autoPhoneBackupEnabled
    val pendingReinstallRestore: StateFlow<Boolean> = prefs.pendingReinstallRestore
    val restorePromptDismissed: StateFlow<Boolean> = prefs.restorePromptDismissed
    val privacyMaskEnabled: StateFlow<Boolean> = prefs.privacyMaskEnabled
    val pinLockEnabled: StateFlow<Boolean> = prefs.pinLockEnabled
    val notificationsEnabled: StateFlow<Boolean> = prefs.notificationsEnabled
    val billAlertsEnabled: StateFlow<Boolean> = prefs.billAlertsEnabled
    val transactionAlertsEnabled: StateFlow<Boolean> = prefs.transactionAlertsEnabled
    val savingsAlertsEnabled: StateFlow<Boolean> = prefs.savingsAlertsEnabled
    val systemAlertsEnabled: StateFlow<Boolean> = prefs.systemAlertsEnabled
    val largeBillThreshold: StateFlow<Double> = prefs.largeBillThreshold

    private val _deviceHardware = MutableStateFlow(
        DevicePerformanceEngine.captureHardwareSnapshot(application)
    )
    val devicePerformance: StateFlow<DevicePerformanceProfile> = combine(
        _deviceHardware,
        performanceMode
    ) { hw, mode ->
        DevicePerformanceEngine.resolveProfile(hw, mode)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        DevicePerformanceEngine.resolveProfile(_deviceHardware.value, performanceMode.value)
    )

    private val _isAppUnlocked = MutableStateFlow(!prefs.pinLockEnabled.value)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _uninstallGuardStatus = MutableStateFlow(
        UninstallProtectionManager.getStatus(application)
    )
    val uninstallGuardStatus: StateFlow<UninstallGuardStatus> = _uninstallGuardStatus.asStateFlow()

    private val _uninstallProtectionActive = MutableStateFlow(_uninstallGuardStatus.value.isProtected)
    val uninstallProtectionActive: StateFlow<Boolean> = _uninstallProtectionActive.asStateFlow()

    private val _biometricAvailability = MutableStateFlow(_uninstallGuardStatus.value.biometricAvailability)
    val biometricAvailability: StateFlow<BiometricAvailability> = _biometricAvailability.asStateFlow()

    private val _phonePermissions = MutableStateFlow(PhonePermissionsSnapshot())
    val phonePermissions: StateFlow<PhonePermissionsSnapshot> = _phonePermissions.asStateFlow()

    private val _savedPublicPdfs = MutableStateFlow<List<SavedPublicPdf>>(emptyList())
    val savedPublicPdfs: StateFlow<List<SavedPublicPdf>> = _savedPublicPdfs.asStateFlow()

    private val _largeBillWarningPopup = MutableStateFlow<LargeBillWarningPopup?>(null)
    val largeBillWarningPopup: StateFlow<LargeBillWarningPopup?> = _largeBillWarningPopup.asStateFlow()
    private var warningDismissJob: Job? = null

    private val _isConvertingCurrency = MutableStateFlow(false)
    val isConvertingCurrency: StateFlow<Boolean> = _isConvertingCurrency.asStateFlow()

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

    val pendingTransactions: StateFlow<List<PendingTransactionEntity>> = repository.pendingTransactionsFlow.stateIn(
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
    }
        .conflate()
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryAnalyticsReport())

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()
    private var updateDownloadJob: Job? = null

    private val _installLaunchRequests = MutableSharedFlow<Intent>(extraBufferCapacity = 4)
    val installLaunchRequests: SharedFlow<Intent> = _installLaunchRequests.asSharedFlow()

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

        val today = FormatUtils.todayIso()
        var income = 0.0
        var personalExpense = 0.0
        var periodHousehold = 0.0
        var periodSavingsOutflow = 0.0
        var todayHousehold = 0.0

        // Single-pass O(N) grouping by monthKey for fast 6-month charts & trends
        val monthIncome = HashMap<String, Double>(12)
        val monthExpense = HashMap<String, Double>(12)
        val monthHousehold = HashMap<String, Double>(12)
        val monthPersonal = HashMap<String, Double>(12)
        val monthSavingsDep = HashMap<String, Double>(12)
        val monthSavingsWdr = HashMap<String, Double>(12)

        for (t in txns) {
            val isHouse = AutoAccountingEngine.isHouseholdTransaction(t)
            val isSav = !isHouse && AutoAccountingEngine.isSavingsTransaction(t)
            val mKey = FormatUtils.monthKey(t.date)

            if (t.isIncome) {
                monthIncome[mKey] = (monthIncome[mKey] ?: 0.0) + t.amount
                if (isSav) monthSavingsWdr[mKey] = (monthSavingsWdr[mKey] ?: 0.0) + t.amount
            } else {
                monthExpense[mKey] = (monthExpense[mKey] ?: 0.0) + t.amount
                when {
                    isHouse -> monthHousehold[mKey] = (monthHousehold[mKey] ?: 0.0) + t.amount
                    isSav -> monthSavingsDep[mKey] = (monthSavingsDep[mKey] ?: 0.0) + t.amount
                    else -> monthPersonal[mKey] = (monthPersonal[mKey] ?: 0.0) + t.amount
                }
            }

            if (t.date == today && isHouse) {
                todayHousehold += t.amount
            }

            if (FormatUtils.isDateInPeriod(t.date, period)) {
                if (t.isIncome) {
                    income += t.amount
                } else {
                    when {
                        isHouse -> periodHousehold += t.amount
                        isSav -> periodSavingsOutflow += t.amount
                        else -> personalExpense += t.amount
                    }
                }
            }
        }

        val totalOutflow = personalExpense + periodHousehold + periodSavingsOutflow
        val receivables = invs.sumOf { if (!it.isDraft) it.balance else 0.0 }
        var unpaidBillsTotal = 0.0
        var overdueBillsCount = 0
        for (b in bls) {
            if (!b.paid) {
                unpaidBillsTotal += b.amount
                if (b.isOverdue) overdueBillsCount++
            }
        }

        val monthKeys = (-5..0).map { offset ->
            FormatUtils.monthKey(FormatUtils.addMonthsIso(today, offset))
        }
        val chart = monthKeys.map { key ->
            MonthlyChartPoint(
                label = FormatUtils.shortMonthLabel(key, lang),
                income = monthIncome[key] ?: 0.0,
                expense = monthExpense[key] ?: 0.0
            )
        }

        val totalVaultSavings = goals.sumOf { it.currentAmount }
        val totalSavingsTarget = goals.sumOf { it.targetAmount }
        val runningCumulative = DoubleArray(monthKeys.size)
        var cursorSavings = totalVaultSavings
        for (i in monthKeys.indices.reversed()) {
            val key = monthKeys[i]
            runningCumulative[i] = cursorSavings.coerceAtLeast(0.0)
            val netMonthSavings = (monthSavingsDep[key] ?: 0.0) - (monthSavingsWdr[key] ?: 0.0)
            cursorSavings = (cursorSavings - netMonthSavings).coerceAtLeast(0.0)
        }
        val spendingTrends = monthKeys.mapIndexed { idx, key ->
            MonthlyTrendPoint(
                monthKey = key,
                label = FormatUtils.shortMonthLabel(key, lang),
                householdBills = monthHousehold[key] ?: 0.0,
                personalExpenses = monthPersonal[key] ?: 0.0,
                savingsDeposited = monthSavingsDep[key] ?: 0.0,
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
            unpaidBillsTotal = unpaidBillsTotal,
            overdueBillsCount = overdueBillsCount,
            totalSavings = totalVaultSavings,
            totalSavingsTarget = totalSavingsTarget,
            chartPoints = chart,
            spendingTrendPoints = spendingTrends,
            recentTransactions = txns.take(devicePerformance.value.recentItemsLimit)
        )
    }
        .conflate()
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardMetrics())

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
    }
        .conflate()
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ZeroLossLedgerSummary())

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
    }
        .conflate()
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BusinessHealthReport())

    val partyDirectory: StateFlow<List<PartySummary>> = combine(
        transactions,
        invoices,
        bills,
        savingsGoals,
        savingsLogs
    ) { txns, invs, bls, goals, logs ->
        PartyDirectoryEngine.buildDirectory(txns, invs, bls, goals, logs)
    }
        .conflate()
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val phoneStorageStatus: StateFlow<PhoneStorageStatus> = repository.phoneStorageStatusFlow

    val hasSafetySnapshot: StateFlow<Boolean> = phoneStorageStatus
        .map { it.hasAnyBackup }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun refreshStorageStatus() {
        viewModelScope.launch {
            val status = repository.refreshStorageStatusAsync()
            val snapshot = withContext(Dispatchers.IO) {
                notificationManager.getPermissionsSnapshot(status.hasAnyBackup)
            }
            _phonePermissions.value = snapshot
        }
    }

    fun refreshPhonePermissions() {
        val hasBackup = phoneStorageStatus.value.hasAnyBackup
        viewModelScope.launch(Dispatchers.IO) {
            _phonePermissions.value = notificationManager.getPermissionsSnapshot(hasBackup)
        }
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

    private var lastUpdateInfo: UpdateInfo? = null
    private var lastVerifiedApkPath: String? = null
    private var directFallbackAttempted: Boolean = false

    private var lastResumeRefreshMs: Long = 0L

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                notificationManager.ensureNotificationChannels()
                EveningReminderScheduler.scheduleDailyEveningReminder(getApplication())
            }
            // Silently refresh live market exchange rates in background if cache is stale
            if (prefs.isExchangeRatesStale()) {
                viewModelScope.launch(Dispatchers.IO) {
                    CurrencyExchangeService.fetchLiveRatesPerUsd()?.let { liveSnapshot ->
                        prefs.saveCachedExchangeRates(liveSnapshot)
                    }
                }
            }
            val autoRestored = repository.ensureInitialized()
            refreshStorageStatus()
            refreshSavedPublicPdfs()
            if (autoRestored) {
                emitNotice(
                    tr(
                        language.value,
                        "Data zako zote zimerejeshwa kiotomatiki kutoka hifadhi ya simu!",
                        "All your records were automatically restored from phone storage!"
                    )
                )
            }
            // Automatically check due bills, unpaid bills & pending transactions once per day
            val todayIso = FormatUtils.todayIso()
            if (prefs.shouldRunDailyDueReminder(todayIso)) {
                withContext(Dispatchers.IO) {
                    val currentBills = runCatching { repository.billsFlow.first() }.getOrDefault(emptyList())
                    val currentInvoices = runCatching { repository.invoicesFlow.first() }.getOrDefault(emptyList())
                    val currentPending = runCatching { repository.pendingTransactionsFlow.first() }.getOrDefault(emptyList())
                    val currentSettings = runCatching { repository.settingsFlow.first() }.getOrDefault(BusinessSettingsEntity())
                    val cur = currentSettings.currency.ifBlank { "TZS" }
                    notificationManager.runSmartDueReminders(
                        bills = currentBills,
                        invoices = currentInvoices,
                        currency = cur,
                        language = language.value
                    )
                    if (currentPending.isNotEmpty()) {
                        notificationManager.notifyPendingTransactionsReminder(
                            pendingItems = currentPending,
                            currency = cur,
                            language = language.value
                        )
                    }
                }
            }
            if (prefs.shouldAutoCheckUpdates()) {
                checkForUpdates(silentIfUpToDate = true)
            }
        }
        viewModelScope.launch {
            UpdateInstallEventBus.events.collect { event ->
                when (event) {
                    is InstallResultEvent.PendingUserAction -> {
                        val info = lastUpdateInfo
                        val apkPath = event.cachedApkPath ?: lastVerifiedApkPath
                        if (apkPath != null) lastVerifiedApkPath = apkPath
                        if (info != null) {
                            _updateState.value = UpdateUiState.Installing(info, apkPath)
                        }
                        // Launch Android's confirmation sheet directly from the foreground Activity
                        // so Android 14/15/16 Background Activity Launch (BAL) never drops it.
                        _installLaunchRequests.tryEmit(event.confirmIntent)
                    }
                    is InstallResultEvent.Success -> {
                        val version = lastUpdateInfo?.version ?: updateManager.currentVersionName
                        lastVerifiedApkPath = null
                        _updateState.value = UpdateUiState.Success(version)
                        emitNotice(tr(language.value, "Sasisho limesakinishwa kikamilifu.", "Update installed successfully."))
                    }
                    is InstallResultEvent.Failure -> {
                        val apkPath = event.cachedApkPath ?: lastVerifiedApkPath
                        if (apkPath != null) lastVerifiedApkPath = apkPath

                        if (event.shouldFallbackToDirectInstaller &&
                            !directFallbackAttempted &&
                            !apkPath.isNullOrBlank() &&
                            File(apkPath).exists()
                        ) {
                            directFallbackAttempted = true
                            val info = lastUpdateInfo
                            if (info != null) {
                                _updateState.value = UpdateUiState.Installing(info, apkPath)
                            }
                            runCatching {
                                _installLaunchRequests.tryEmit(updateManager.buildDirectApkInstallIntent(File(apkPath)))
                            }.onFailure {
                                _updateState.value = UpdateUiState.Error(
                                    message = event.reason,
                                    retryInfo = lastUpdateInfo,
                                    cachedApkPath = apkPath
                                )
                            }
                        } else if (event.isSignatureConflict) {
                            handleSignatureConflictRecovery(
                                reason = event.reason,
                                apkPath = apkPath,
                                info = lastUpdateInfo
                            )
                        } else {
                            _updateState.value = UpdateUiState.Error(
                                message = event.reason,
                                retryInfo = lastUpdateInfo,
                                cachedApkPath = apkPath?.takeIf { File(it).exists() }
                            )
                        }
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

    fun refreshUninstallProtectionStatus() {
        val app = getApplication<Application>()
        var status = UninstallProtectionManager.getStatus(app)
        if (status.isDeviceOwner && !status.isDeviceOwnerUninstallBlocked && prefs.uninstallProtectionPreferred.value) {
            UninstallProtectionManager.enforceDeviceOwnerUninstallBlock(app, blocked = true)
            status = UninstallProtectionManager.getStatus(app)
        }
        // Automatically arm preference if user turned on Android System Accessibility Guard or Device Admin
        if (status.isAccessibilityGuardActive || status.isDeviceAdminActive || status.isDeviceOwnerUninstallBlocked) {
            if (!prefs.uninstallProtectionPreferred.value) {
                prefs.setUninstallProtectionPreferred(true)
            }
        }
        _uninstallGuardStatus.value = status
        _uninstallProtectionActive.value = status.isProtected
        _biometricAvailability.value = status.biometricAvailability
    }

    fun markUninstallProtectionArmed() {
        prefs.setUninstallProtectionPreferred(true)
        UninstallGuardSession.revokeTemporaryUnlock()
        refreshUninstallProtectionStatus()
    }

    fun enableDeviceOwnerUninstallBlockAfterFingerprint(): Boolean {
        val app = getApplication<Application>()
        val locked = UninstallProtectionManager.enforceDeviceOwnerUninstallBlock(app, blocked = true)
        if (locked) {
            prefs.setUninstallProtectionPreferred(true)
            UninstallGuardSession.revokeTemporaryUnlock()
            refreshUninstallProtectionStatus()
            emitNotice(
                tr(
                    language.value,
                    "Device Owner: Kuondoa app kumezuiwa kabisa kwenye mfumo wa Android.",
                    "Device Owner: App uninstallation is now permanently blocked by Android."
                )
            )
        }
        return locked
    }

    fun deactivateUninstallProtectionAfterFingerprint(
        notifyUser: Boolean = true,
        clearDeviceOwnerForUninstall: Boolean = false
    ): Boolean {
        val app = getApplication<Application>()
        UninstallGuardSession.grantTemporaryUnlock(45_000L)
        val removed = UninstallProtectionManager.releaseProtectionAfterFingerprint(
            context = app,
            clearDeviceOwnerForUninstall = clearDeviceOwnerForUninstall
        )
        prefs.setUninstallProtectionPreferred(false)
        refreshUninstallProtectionStatus()
        if (notifyUser) {
            emitNotice(
                tr(
                    language.value,
                    "Alama ya kidole imethibitishwa. Ulinzi wa kuondoa app umezimwa kwa sekunde 45.",
                    "Fingerprint verified. Uninstall guard unlocked."
                )
            )
        }
        return removed
    }

    fun emitSecurityNotice(message: String) {
        emitNotice(message)
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
            withContext(Dispatchers.IO) {
                repository.persistSafetySnapshot()
            }
            refreshStorageStatus()
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
            } else {
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
            val total = items.sumOf { it.lineTotal }
            notificationManager.notifyInvoiceEvent(
                invoiceNumber = invoice.number,
                clientName = invoice.customerName,
                amount = total,
                currency = settings.value.currency.ifBlank { "TZS" },
                language = language.value,
                isPayment = false
            )
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
            val currentSettings = settings.value
            val safeNumber = invoiceWithItems.invoice.number.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val prefix = if (lang == AppLanguage.EN) "Invoice" else "Ankara"
            val filename = "${prefix}_$safeNumber.pdf"
            val bytes = withContext(Dispatchers.IO) {
                val generated = InvoicePdfGenerator.generatePdfBytes(invoiceWithItems, currentSettings, lang)
                // Always mirror directly into public Documents/Mengidata/Ankara/ so it is immediately retrievable
                runCatching { repository.savePdfToPublicDocuments(filename, generated) }
                generated
            }
            refreshSavedPublicPdfs()
            _exportRequests.emit(
                PendingFileExport(
                    filename = filename,
                    mimeType = "application/pdf",
                    bytes = bytes
                )
            )
        }
    }

    fun printInvoicePdf(invoiceWithItems: InvoiceWithItems) {
        viewModelScope.launch {
            val lang = language.value
            val currentSettings = settings.value
            val bytes = withContext(Dispatchers.IO) {
                InvoicePdfGenerator.generatePdfBytes(invoiceWithItems, currentSettings, lang)
            }
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
            } else {
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
                } else {
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

    fun buildViewInvoicePdfIntent(invoiceWithItems: InvoiceWithItems): Intent? {
        val app = getApplication<Application>()
        val lang = language.value
        val pdfBytes = InvoicePdfGenerator.generatePdfBytes(invoiceWithItems, settings.value, lang)
        val safeNumber = invoiceWithItems.invoice.number.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val filePrefix = if (lang == AppLanguage.EN) "Invoice" else "Ankara"
        val filename = "${filePrefix}_$safeNumber.pdf"
        // Also mirror a clean application/pdf copy into Documents/Mengidata/Ankara/
        runCatching { repository.savePdfToPublicDocuments(filename, pdfBytes) }
        return buildViewPdfFileIntent(filename, pdfBytes)
    }

    fun buildViewPdfFileIntent(filename: String, pdfBytes: ByteArray): Intent? {
        val app = getApplication<Application>()
        val safeName = if (filename.endsWith(".pdf", ignoreCase = true)) filename else "$filename.pdf"
        return runCatching {
            val dir = File(app.cacheDir, "shared_pdfs").apply { if (!exists()) mkdirs() }
            val file = File(dir, safeName)
            file.writeBytes(pdfBytes)
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            }
        }.getOrNull()
    }

    fun savePublicPdfMirror(filename: String, pdfBytes: ByteArray) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { repository.savePdfToPublicDocuments(filename, pdfBytes) }
            }
            refreshSavedPublicPdfs()
        }
    }

    fun refreshSavedPublicPdfs() {
        viewModelScope.launch {
            val items = repository.listSavedPublicInvoicePdfs()
            _savedPublicPdfs.value = items
        }
    }

    fun deleteSavedPublicPdf(pdf: SavedPublicPdf) {
        viewModelScope.launch {
            val deleted = repository.deleteSavedPublicPdf(pdf)
            refreshSavedPublicPdfs()
            if (deleted) {
                emitNotice(tr(language.value, "PDF imefutwa kwenye hifadhi ya simu.", "PDF deleted from phone storage."))
            }
        }
    }

    fun buildViewSavedPublicPdfIntent(pdf: SavedPublicPdf): Intent? {
        val app = getApplication<Application>()
        val uri = resolvePublicPdfUri(app, pdf) ?: return null
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildShareSavedPublicPdfIntent(pdf: SavedPublicPdf): Intent? {
        val app = getApplication<Application>()
        val uri = resolvePublicPdfUri(app, pdf) ?: return null
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, pdf.displayName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun resolvePublicPdfUri(app: Application, pdf: SavedPublicPdf): android.net.Uri? {
        if (!pdf.absolutePath.isNullOrBlank()) {
            val file = File(pdf.absolutePath)
            if (file.exists() && file.length() > 0L) {
                val fpUri = runCatching {
                    FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
                }.getOrNull()
                if (fpUri != null) return fpUri
            }
        }
        return pdf.contentUri
    }

    private fun buildSharePdfFileIntent(
        app: Application,
        filename: String,
        pdfBytes: ByteArray,
        subject: String,
        bodyText: String
    ): Intent? = runCatching {
        val dir = File(app.cacheDir, "shared_pdfs").apply { if (!exists()) mkdirs() }
        val safeName = if (filename.endsWith(".pdf", ignoreCase = true)) filename else "$filename.pdf"
        val file = File(dir, safeName)
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
            if (goal != null) {
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

    fun setCurrency(currencyCode: String) {
        val targetCode = FormatUtils.normalizeCurrencyCode(currencyCode)
        val currentCode = FormatUtils.normalizeCurrencyCode(settings.value.currency)
        if (currentCode.equals(targetCode, ignoreCase = true)) return
        if (_isConvertingCurrency.value) return
        val option = FormatUtils.findCurrency(targetCode)
        viewModelScope.launch {
            _isConvertingCurrency.value = true
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    val live = CurrencyExchangeService.fetchLiveRatesPerUsd()
                    if (live != null) {
                        prefs.saveCachedExchangeRates(live)
                        live
                    } else {
                        prefs.getCachedExchangeRates()
                    }
                }
                val factor = CurrencyExchangeService.conversionFactor(
                    fromCode = currentCode,
                    toCode = targetCode,
                    ratesPerUsd = snapshot.ratesPerUsd
                )
                repository.convertAllMoneyToCurrency(
                    targetCurrencyCode = targetCode,
                    factor = factor
                )
                refreshStorageStatus()
                val ratePair = CurrencyExchangeService.formatRatePair(
                    fromCode = currentCode,
                    toCode = targetCode,
                    ratesPerUsd = snapshot.ratesPerUsd
                )
                emitNotice(
                    tr(
                        language.value,
                        "Sarafu sasa ni ${option.symbol} · Pesa zimebadilishwa ($ratePair)",
                        "Currency set to ${option.symbol} · Amounts converted ($ratePair)"
                    )
                )
            } finally {
                _isConvertingCurrency.value = false
            }
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

    fun setAutoPhoneBackupEnabled(enabled: Boolean) {
        prefs.setAutoPhoneBackupEnabled(enabled)
        if (enabled) {
            viewModelScope.launch {
                val ok = repository.persistSafetySnapshot(force = true)
                refreshStorageStatus()
                if (ok) {
                    emitNotice(
                        tr(
                            language.value,
                            "Hifadhi ya kiotomatiki kwenye simu imewashwa na data zimehifadhiwa!",
                            "Auto phone storage backup enabled and synced!"
                        )
                    )
                }
            }
        } else {
            emitNotice(
                tr(
                    language.value,
                    "Hifadhi ya kiotomatiki kwenye simu imesimamishwa.",
                    "Auto phone storage backup paused."
                )
            )
        }
    }

    fun setCustomBackupFolderUri(uriString: String) {
        repository.setCustomBackupFolderUri(uriString)
        viewModelScope.launch {
            val ok = repository.syncToPhoneStorageNow()
            refreshStorageStatus()
            if (ok) {
                emitNotice(
                    tr(
                        language.value,
                        "Folda ya hifadhi ya simu imeunganishwa na data zimehifadhiwa kiotomatiki!",
                        "Phone storage folder linked and synced automatically!"
                    )
                )
            }
        }
    }

    fun autoBackupOnBackground() {
        if (!prefs.autoPhoneBackupEnabled.value) return
        viewModelScope.launch {
            runCatching { repository.persistSafetySnapshot(force = false) }
            refreshStorageStatus()
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

    fun dismissRestorePrompt() {
        prefs.setRestorePromptDismissed(true)
    }

    fun restoreLatestSafetySnapshot() {
        viewModelScope.launch {
            val ok = repository.restoreLatestSafetySnapshot()
            refreshStorageStatus()
            if (ok) {
                emitNotice(tr(language.value, "Hesabu zote zimerejeshwa kutoka hifadhi ya simu!", "All records restored from phone storage backup!"))
            } else {
                emitNotice(
                    tr(
                        language.value,
                        "Hakuna faili la nakala rudufu lililopatikana bado. Hakikisha umeruhusu 'All Files Access' (Storage Manager).",
                        "No backup file found yet. Ensure 'All Files Access' (Storage Manager) is granted."
                    )
                )
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
        directFallbackAttempted = false
        updateDownloadJob?.cancel()
        updateDownloadJob = viewModelScope.launch {
            try {
                // Proactively sync the database to phone storage (Documents/Mengidata) before updating
                runCatching { repository.syncToPhoneStorageNow() }
                refreshStorageStatus()

                // Reuse already-downloaded & verified APK if present so retries & permission returns are instant
                val cachedApk = updateManager.findValidCachedApk(info)
                val verifiedApk: File = if (cachedApk != null) {
                    cachedApk
                } else {
                    val perf = devicePerformance.value
                    _updateState.value = UpdateUiState.Downloading(
                        info = info,
                        progressPercent = 0,
                        downloadedBytes = 0L,
                        totalBytes = info.expectedSizeBytes,
                        speedBytesPerSec = 0L,
                        etaSeconds = -1L,
                        activeStreams = perf.downloadParallelStreams
                    )
                    val downloadResult = updateManager.downloadAndVerifyApk(
                        info = info,
                        maxParallelStreams = perf.downloadParallelStreams,
                        ioBufferBytes = perf.downloadBufferBytes,
                        onProgress = { progress ->
                            _updateState.value = UpdateUiState.Downloading(
                                info = info,
                                progressPercent = progress.percent,
                                downloadedBytes = progress.downloadedBytes,
                                totalBytes = progress.totalBytes,
                                speedBytesPerSec = progress.speedBytesPerSec,
                                etaSeconds = progress.etaSeconds,
                                activeStreams = progress.activeStreams
                            )
                        },
                        onVerifying = {
                            _updateState.value = UpdateUiState.Verifying(info)
                        }
                    )
                    downloadResult.getOrElse { err ->
                        _updateState.value = UpdateUiState.Error(
                            message = err.message ?: tr(language.value, "Imeshindwa kupakua au kuthibitisha APK.", "Download or signature verification failed."),
                            retryInfo = info
                        )
                        return@launch
                    }
                }

                lastVerifiedApkPath = verifiedApk.absolutePath

                // Pre-check signing certificate match before asking Android PackageInstaller
                val certMatches = updateManager.hasMatchingSigningCertificate(verifiedApk)
                if (!certMatches) {
                    handleSignatureConflictRecovery(
                        reason = tr(
                            language.value,
                            "Saini ya toleo lililopo simuni ni tofauti na toleo jipya v${info.version}.",
                            "The installed build uses a different signing key than v${info.version}."
                        ),
                        apkPath = verifiedApk.absolutePath,
                        info = info
                    )
                    return@launch
                }

                _updateState.value = UpdateUiState.Installing(info, verifiedApk.absolutePath)
                updateManager.installVerifiedApk(verifiedApk)
                    .onFailure {
                        // Automatically fall back to native FileProvider ACTION_VIEW APK installer
                        directFallbackAttempted = true
                        launchDirectApkInstall(verifiedApk.absolutePath)
                    }
            } catch (ce: CancellationException) {
                // Restore Available state cleanly when user taps Cancel
                _updateState.value = UpdateUiState.Available(info)
                throw ce
            }
        }
    }

    private suspend fun handleSignatureConflictRecovery(
        reason: String,
        apkPath: String?,
        info: UpdateInfo?
    ) {
        val version = info?.version ?: "latest"
        // Guarantee user's financial data is backed up AND copy the verified APK to public Downloads/Mengidata/
        runCatching { repository.syncToPhoneStorageNow() }
        val savedName = if (!apkPath.isNullOrBlank()) {
            updateManager.saveApkToPublicDownloads(File(apkPath), version)
        } else {
            null
        }
        val guidance = if (savedName != null) {
            tr(
                language.value,
                "$reason Data zako zimehifadhiwa salama kwenye Documents/Mengidata na faili la $savedName limehifadhiwa kwenye Downloads/Mengidata. Bonyeza 'Ondoa Toleo la Zamani' kisha fungua $savedName kwenye Downloads — data zako zitarudi zenyewe!",
                "$reason Your data is safely backed up in Documents/Mengidata and $savedName has been saved to Downloads/Mengidata. Tap 'Uninstall Old Version' then open $savedName from Downloads — your data will auto-restore!"
            )
        } else {
            tr(
                language.value,
                "$reason Data zako zimehifadhiwa salama kwenye Documents/Mengidata — ondoa toleo la zamani kisha sakinisha Mengidata.apk mpya.",
                "$reason Your data is safely backed up in Documents/Mengidata — uninstall the old version then install the new Mengidata.apk."
            )
        }
        _updateState.value = UpdateUiState.Error(
            message = guidance,
            retryInfo = info,
            cachedApkPath = apkPath?.takeIf { File(it).exists() },
            isSignatureConflict = true,
            savedDownloadsName = savedName
        )
    }

    fun launchDirectApkInstall(apkPath: String) {
        val file = File(apkPath)
        val info = lastUpdateInfo
        if (!updateManager.canRequestPackageInstalls()) {
            if (info != null) {
                _updateState.value = UpdateUiState.PermissionRequired(info)
            }
            return
        }
        if (!file.exists()) {
            if (info != null) {
                startUpdateDownloadAndInstall(info)
            }
            return
        }
        if (info != null) {
            _updateState.value = UpdateUiState.Installing(info, apkPath)
        }
        runCatching {
            _installLaunchRequests.tryEmit(updateManager.buildDirectApkInstallIntent(file))
        }.onFailure { err ->
            _updateState.value = UpdateUiState.Error(
                message = err.message ?: tr(language.value, "Imeshindwa kufungua kisakinishi cha Android.", "Failed to open Android APK installer."),
                retryInfo = info,
                cachedApkPath = apkPath
            )
        }
    }

    fun fallbackToDirectInstallIfAvailable() {
        val apkPath = lastVerifiedApkPath
        if (!apkPath.isNullOrBlank() && File(apkPath).exists() && !directFallbackAttempted) {
            directFallbackAttempted = true
            launchDirectApkInstall(apkPath)
        }
    }

    fun openUninstallOldApp(): Intent = updateManager.buildUninstallSelfIntent()

    fun cancelUpdateDownload() {
        updateDownloadJob?.cancel()
        updateDownloadJob = null
        val info = lastUpdateInfo
        if (info != null) {
            _updateState.value = UpdateUiState.Available(info)
        } else {
            _updateState.value = UpdateUiState.Idle
        }
    }

    fun onResumeCheckInstallPermission() {
        refreshUninstallProtectionStatus()
        val now = System.currentTimeMillis()
        val prevStorageGranted = _phonePermissions.value.storageManagerGranted
        val hasBackup = phoneStorageStatus.value.hasAnyBackup
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = notificationManager.getPermissionsSnapshot(hasBackup)
            _phonePermissions.value = snapshot
            val permissionChanged = snapshot.storageManagerGranted != prevStorageGranted
            if (snapshot.storageManagerGranted && (permissionChanged || prefs.pendingReinstallRestore.value)) {
                val restored = repository.tryAutoRestoreAfterStorageAccessGranted()
                if (restored) {
                    emitNotice(
                        tr(
                            language.value,
                            "Data zako zote zimerejeshwa kiotomatiki kutoka Documents/Mengidata!",
                            "All your records were automatically restored from Documents/Mengidata!"
                        )
                    )
                } else if (permissionChanged && !repository.isDatabaseOnlyDemoOrEmpty()) {
                    // User just enabled All Files Access on an active install -> mirror real data to public storage immediately!
                    repository.syncToPhoneStorageNow()
                }
            }
            if (permissionChanged || now - lastResumeRefreshMs >= 4_000L) {
                lastResumeRefreshMs = now
                refreshStorageStatus()
                refreshSavedPublicPdfs()
            }
            val state = _updateState.value
            if (state is UpdateUiState.PermissionRequired && updateManager.canRequestPackageInstalls()) {
                withContext(Dispatchers.Main) {
                    // Automatically continue download/install as soon as the user enables "Allow from this source"
                    startUpdateDownloadAndInstall(state.info)
                }
            }
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

    fun savePendingTransaction(item: PendingTransactionEntity) {
        viewModelScope.launch {
            val saved = repository.savePendingTransaction(item)
            EveningReminderScheduler.scheduleDailyEveningReminder(getApplication())
            notificationManager.notifyPendingTransactionSaved(
                item = saved,
                currency = settings.value.currency.ifBlank { "TZS" },
                language = language.value
            )
        }
    }

    fun recordPendingPayment(id: String, addedAmount: Double, markFull: Boolean = false) {
        viewModelScope.launch {
            val updated = repository.recordPendingPayment(id, addedAmount, markFull) ?: return@launch
            val cur = settings.value.currency.ifBlank { "TZS" }
            val lang = language.value
            EveningReminderScheduler.scheduleDailyEveningReminder(getApplication())
            notificationManager.notifyPendingTransactionSaved(
                item = updated,
                currency = cur,
                language = lang
            )
        }
    }

    fun completePendingTransaction(id: String) {
        viewModelScope.launch {
            val posted = repository.completePendingTransaction(id)
            if (posted != null) {
                if (prefs.notificationsEnabled.value && prefs.transactionAlertsEnabled.value) {
                    notificationManager.notifyTransactionSaved(
                        txn = posted,
                        currency = settings.value.currency.ifBlank { "TZS" },
                        language = language.value
                    )
                }
                emitNotice(
                    tr(
                        language.value,
                        "Muamala umekamilika na kuingizwa kwenye Ledger.",
                        "Pending transaction completed and posted to Ledger."
                    )
                )
            }
        }
    }

    fun deletePendingTransaction(id: String) {
        viewModelScope.launch {
            if (repository.deletePendingTransaction(id)) {
                emitNotice(
                    tr(
                        language.value,
                        "Muamala unaosubiri umeondolewa.",
                        "Pending transaction removed."
                    )
                )
            }
        }
    }

    fun sendEveningRemindersNow() {
        refreshPhonePermissions()
        EveningReminderScheduler.scheduleDailyEveningReminder(getApplication())
        val cur = settings.value.currency.ifBlank { "TZS" }
        val lang = language.value
        val pendingCount = notificationManager.notifyPendingTransactionsReminder(
            pendingItems = pendingTransactions.value,
            currency = cur,
            language = lang
        )
        val unpaidBillsCount = notificationManager.notifyUnpaidBillsEveningReminder(
            bills = bills.value,
            currency = cur,
            language = lang
        )
        if (pendingCount > 0 || unpaidBillsCount > 0) {
            emitNotice(
                tr(
                    lang,
                    "Ukumbusho umetumwa ($pendingCount miamala inayosubiri · $unpaidBillsCount bili hazijalipwa). Pia utatuma kila siku saa 1:00 jioni.",
                    "Reminder sent ($pendingCount pending transactions · $unpaidBillsCount unpaid bills). Daily 7:00 PM alert active."
                )
            )
        } else {
            emitNotice(
                tr(
                    lang,
                    "Hakuna muamala unaosubiri wala bili isiyolipwa kwa sasa.",
                    "No pending transactions or unpaid bills right now."
                )
            )
        }
    }

    fun runDueBillsAndInvoicesReminderNow() {
        refreshPhonePermissions()
        EveningReminderScheduler.scheduleDailyEveningReminder(getApplication())
        val cur = settings.value.currency.ifBlank { "TZS" }
        val lang = language.value
        val dueCount = notificationManager.runSmartDueReminders(
            bills = bills.value,
            invoices = invoices.value,
            currency = cur,
            language = lang
        )
        val unpaidBillsCount = notificationManager.notifyUnpaidBillsEveningReminder(
            bills = bills.value,
            currency = cur,
            language = lang
        )
        val pendingCount = notificationManager.notifyPendingTransactionsReminder(
            pendingItems = pendingTransactions.value,
            currency = cur,
            language = lang
        )
        val totalSent = dueCount + unpaidBillsCount + pendingCount
        if (totalSent > 0) {
            emitNotice(
                tr(
                    lang,
                    "Ukumbusho wa malipo, bili hazijalipwa na miamala inayosubiri umetumwa.",
                    "Reminders for unpaid bills, due items, and pending transactions sent."
                )
            )
        } else {
            emitNotice(
                tr(
                    lang,
                    "Hakuna bili isiyolipwa wala muamala unaosubiri leo.",
                    "No unpaid bills or pending transactions right now."
                )
            )
        }
    }

    fun openInstallPermissionSettings(): Intent = updateManager.buildInstallPermissionIntent()

    fun openManageStorageSettings(): Intent = notificationManager.buildManageStoragePermissionIntent()

    fun openManageStorageFallbackSettings(): Intent = notificationManager.buildManageAllFilesFallbackIntent()

    fun openAppNotificationSettings(): Intent = notificationManager.buildAppNotificationSettingsIntent()

    fun openAppDetailsSettings(): Intent = notificationManager.buildAppDetailsSettingsIntent()

    fun openReleasesInBrowser(): Intent = updateManager.buildReleasesBrowserIntent()

    fun notifyFileSaved() {
        emitNotice(tr(language.value, "Faili limehifadhiwa.", "File saved."))
    }

    fun notifyPdfSaved(onOpenPdf: () -> Unit) {
        emitNotice(
            message = tr(
                language.value,
                "PDF imehifadhiwa kikamilifu! Gusa kufungua.",
                "PDF saved successfully! Tap to open."
            ),
            undoLabel = tr(language.value, "Fungua PDF", "Open PDF"),
            onUndo = onOpenPdf
        )
    }

    fun notifyNoPdfViewer() {
        emitNotice(
            tr(
                language.value,
                "Hakuna programu ya kufungua PDF kwenye simu hii — unaweza kutuma au kuchapisha.",
                "No PDF viewer app found — you can share or print instead."
            )
        )
    }

    fun setPerformanceMode(mode: PerformanceModePreference) {
        prefs.setPerformanceMode(mode)
        refreshDevicePerformance()
        val resolved = devicePerformance.value.resolvedTier.shortLabel(language.value)
        emitNotice(
            tr(
                language.value,
                "Hali ya utendaji imewekwa: ${mode.label(language.value)} ($resolved).",
                "Performance mode set: ${mode.label(language.value)} ($resolved)."
            )
        )
    }

    fun refreshDevicePerformance() {
        viewModelScope.launch(Dispatchers.Default) {
            _deviceHardware.value = DevicePerformanceEngine.captureHardwareSnapshot(
                context = getApplication(),
                memoryPressureActive = false
            )
        }
    }

    fun onSystemMemoryPressure(level: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            DevicePerformanceEngine.clearSafeTempCaches(getApplication())
            _deviceHardware.value = DevicePerformanceEngine.captureHardwareSnapshot(
                context = getApplication(),
                memoryPressureActive = level > 0
            )
        }
    }

    fun optimizeSystemNow() {
        viewModelScope.launch {
            val freedBytes = withContext(Dispatchers.IO) {
                DevicePerformanceEngine.clearSafeTempCaches(getApplication())
            }
            val freshHw = withContext(Dispatchers.Default) {
                DevicePerformanceEngine.captureHardwareSnapshot(
                    context = getApplication(),
                    memoryPressureActive = false
                )
            }
            _deviceHardware.value = freshHw
            val resolved = DevicePerformanceEngine.resolveProfile(freshHw, performanceMode.value)
            val freedKb = (freedBytes / 1024L).coerceAtLeast(0L)
            val freedLabel = if (freedKb >= 1024L) {
                String.format(java.util.Locale.US, "%.1f MB", freedKb / 1024.0)
            } else {
                "$freedKb KB"
            }
            emitNotice(
                tr(
                    language.value,
                    "Mfumo umeboreshwa kwa ${freshHw.deviceDisplayName} (${resolved.resolvedTier.shortLabel(language.value)}) · $freedLabel cache imesafishwa.",
                    "System optimized for ${freshHw.deviceDisplayName} (${resolved.resolvedTier.shortLabel(language.value)}) · $freedLabel cache cleared."
                )
            )
        }
    }

    fun notifyFileError() {
        emitNotice(tr(language.value, "Imeshindwa kuhifadhi faili.", "Failed to save file."))
    }

    private fun emitNotice(message: String, undoLabel: String? = null, onUndo: (() -> Unit)? = null) {
        _notices.tryEmit(SnackbarNotice(message, undoLabel, onUndo))
    }
}
