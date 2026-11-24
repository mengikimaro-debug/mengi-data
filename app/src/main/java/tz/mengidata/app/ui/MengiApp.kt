package tz.mengidata.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.components.GlobalSearchSheet
import tz.mengidata.app.ui.components.MengiSidebarSheet
import tz.mengidata.app.ui.components.OneUiFloatingNavBar
import tz.mengidata.app.ui.components.OneUiNavTab
import tz.mengidata.app.ui.components.PartyDirectorySheet
import tz.mengidata.app.ui.components.PinLockScreen
import tz.mengidata.app.ui.navigation.BillsRoute
import tz.mengidata.app.ui.navigation.DashboardRoute
import tz.mengidata.app.ui.navigation.HubRoute
import tz.mengidata.app.ui.navigation.InvoicesRoute
import tz.mengidata.app.ui.navigation.SettingsRoute
import tz.mengidata.app.ui.navigation.TransactionsRoute
import tz.mengidata.app.ui.screens.BillsScreen
import tz.mengidata.app.ui.screens.DashboardScreen
import tz.mengidata.app.ui.screens.HubScreen
import tz.mengidata.app.ui.screens.InvoicesScreen
import tz.mengidata.app.ui.screens.SettingsScreen
import tz.mengidata.app.ui.screens.TransactionsScreen
import tz.mengidata.app.ui.theme.LocalMengiColors
import tz.mengidata.app.ui.theme.MengidataTheme
import tz.mengidata.app.viewmodel.MengiViewModel
import tz.mengidata.app.viewmodel.PendingFileExport

private data class TopLevelDestination(
    val route: Any,
    val labelSw: String,
    val labelEn: String,
    val icon: ImageVector,
    val tag: String
)

private val PrimaryDestinations = listOf(
    TopLevelDestination(DashboardRoute, "Dashibodi", "Home", Icons.Rounded.Home, "nav_dashboard"),
    TopLevelDestination(TransactionsRoute, "Miamala", "Ledger", Icons.Rounded.AccountBalanceWallet, "nav_transactions"),
    TopLevelDestination(InvoicesRoute, "Ankara", "Invoices", Icons.Rounded.Description, "nav_invoices"),
    TopLevelDestination(BillsRoute, "Bili", "Bills", Icons.AutoMirrored.Rounded.ReceiptLong, "nav_bills"),
    TopLevelDestination(HubRoute, "Akiba", "Savings", Icons.Rounded.Savings, "nav_hub")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MengiApp(viewModel: MengiViewModel) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColorEnabled by viewModel.dynamicColorEnabled.collectAsStateWithLifecycle()
    val highContrastEnabled by viewModel.highContrastEnabled.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val autoUpdateEnabled by viewModel.autoUpdateEnabled.collectAsStateWithLifecycle()
    val privacyMaskEnabled by viewModel.privacyMaskEnabled.collectAsStateWithLifecycle()
    val pinLockEnabled by viewModel.pinLockEnabled.collectAsStateWithLifecycle()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val zeroLossLedger by viewModel.zeroLossLedger.collectAsStateWithLifecycle()
    val businessHealth by viewModel.businessHealth.collectAsStateWithLifecycle()
    val partyDirectory by viewModel.partyDirectory.collectAsStateWithLifecycle()
    val hasSafetySnapshot by viewModel.hasSafetySnapshot.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val savingsGoals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    val savingsLogs by viewModel.savingsLogs.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val categoryAnalytics by viewModel.categoryAnalytics.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingExport by remember { mutableStateOf<PendingFileExport?>(null) }
    var quickAddTxnTrigger by remember { mutableIntStateOf(0) }
    var showGlobalSearch by remember { mutableStateOf(false) }
    var showPartyDirectory by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val export = pendingExport
        pendingExport = null
        if (uri != null && export != null) {
            val saved = writeBytesToUri(context, uri, export.bytes)
            if (saved) viewModel.notifyFileSaved() else viewModel.notifyFileError()
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val text = readTextFromUri(context, uri)
            if (text != null) {
                viewModel.importBackupJson(text)
            } else {
                viewModel.notifyFileError()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.notices.collect { notice ->
            val result = snackbarHostState.showSnackbar(
                message = notice.message,
                actionLabel = notice.undoActionLabel,
                duration = if (notice.undoActionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                notice.onUndo?.invoke()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.exportRequests.collect { export ->
            pendingExport = export
            createDocumentLauncher.launch(export.filename)
        }
    }

    MengidataTheme(
        themeMode = themeMode,
        dynamicColorEnabled = dynamicColorEnabled,
        highContrastEnabled = highContrastEnabled,
        onSetThemeMode = viewModel::setThemeMode,
        onSetDynamicColor = viewModel::setDynamicColorEnabled,
        onSetHighContrast = viewModel::setHighContrastEnabled
    ) {
        val colors = LocalMengiColors.current
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val isSettings = currentDestination?.hasRoute(SettingsRoute::class) == true

        fun navigateTopLevel(route: Any) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        fun openSettings() {
            if (!isSettings) {
                navController.navigate(SettingsRoute) {
                    launchSingleTop = true
                }
            }
        }

        BackHandler(enabled = drawerState.isOpen) {
            scope.launch { drawerState.close() }
        }

        if (pinLockEnabled && !isAppUnlocked) {
            PinLockScreen(
                businessName = settings.businessName,
                language = language,
                onVerifyPin = viewModel::unlockWithPin
            )
            return@MengidataTheme
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                MengiSidebarSheet(
                    settings = settings,
                    zeroLossLedger = zeroLossLedger,
                    language = language,
                    updateState = updateState,
                    privacyMaskEnabled = privacyMaskEnabled,
                    partyCount = partyDirectory.size,
                    selectedRouteMatch = { route -> currentDestination?.hasRoute(route::class) == true },
                    isSettingsSelected = isSettings,
                    onSelectDestination = { route ->
                        scope.launch { drawerState.close() }
                        navigateTopLevel(route)
                    },
                    onOpenPartyDirectory = {
                        scope.launch { drawerState.close() }
                        showPartyDirectory = true
                    },
                    onOpenSettings = {
                        scope.launch { drawerState.close() }
                        openSettings()
                    },
                    onCloseDrawer = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavRail = maxWidth >= 680.dp
                val showFloatingDock = !useNavRail && !isSettings

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                        containerColor = MaterialTheme.colorScheme.background,
                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier.padding(
                                    bottom = if (showFloatingDock) 84.dp else 12.dp
                                )
                            ) { data ->
                                Snackbar(
                                    snackbarData = data,
                                    shape = CircleShape,
                                    containerColor = if (colors.isDark) Color(0xFF262832) else Color(0xFF141B2D),
                                    contentColor = Color.White,
                                    actionColor = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        topBar = {
                            CenterAlignedTopAppBar(
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    titleContentColor = MaterialTheme.colorScheme.onBackground
                                ),
                                navigationIcon = {
                                    Box(modifier = Modifier.padding(start = 10.dp)) {
                                        Surface(
                                            onClick = {
                                                if (isSettings) {
                                                    navController.popBackStack()
                                                } else {
                                                    scope.launch { drawerState.open() }
                                                }
                                            },
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(
                                                0.75.dp,
                                                if (colors.isDark) Color.White.copy(alpha = 0.08f) else colors.cardBorder.copy(alpha = 0.55f)
                                            ),
                                            modifier = Modifier
                                                .size(44.dp)
                                                .testTag(if (isSettings) "settings_back_btn" else "open_sidebar_btn")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isSettings) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.Menu,
                                                    contentDescription = if (isSettings) {
                                                        tr(language, "Rudi", "Back")
                                                    } else {
                                                        tr(language, "Fungua menyu", "Open menu")
                                                    },
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(21.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                title = {
                                    Text(
                                        text = if (isSettings) {
                                            tr(language, "Mipangilio", "Settings")
                                        } else {
                                            settings.businessName.ifBlank { "Mengidata" }
                                        },
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                },
                                actions = {
                                    if (!isSettings) {
                                        Row(
                                            modifier = Modifier.padding(end = 10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                onClick = { viewModel.togglePrivacyMask() },
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(
                                                    0.75.dp,
                                                    if (colors.isDark) Color.White.copy(alpha = 0.08f) else colors.cardBorder.copy(alpha = 0.55f)
                                                ),
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .testTag("topbar_privacy_toggle_btn")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (privacyMaskEnabled) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                        contentDescription = tr(language, "Ficha au onyesha salio", "Toggle balance visibility"),
                                                        tint = if (privacyMaskEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Surface(
                                                onClick = { showGlobalSearch = true },
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(
                                                    0.75.dp,
                                                    if (colors.isDark) Color.White.copy(alpha = 0.08f) else colors.cardBorder.copy(alpha = 0.55f)
                                                ),
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .testTag("topbar_global_search_btn")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Search,
                                                        contentDescription = tr(language, "Utafutaji wa haraka", "Global search"),
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (useNavRail && !isSettings) {
                                NavigationRail(
                                    modifier = Modifier.fillMaxHeight(),
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    PrimaryDestinations.forEach { dest ->
                                        val selected = currentDestination?.hasRoute(dest.route::class) == true
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { navigateTopLevel(dest.route) },
                                            icon = { Icon(dest.icon, contentDescription = dest.labelEn) },
                                            label = {
                                                Text(if (language == AppLanguage.EN) dest.labelEn else dest.labelSw)
                                            },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                            ),
                                            modifier = Modifier.testTag(dest.tag)
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    NavigationRailItem(
                                        selected = isSettings,
                                        onClick = { openSettings() },
                                        icon = {
                                            Icon(
                                                Icons.Rounded.Settings,
                                                contentDescription = tr(language, "Mipangilio", "Settings")
                                            )
                                        },
                                        label = {
                                            Text(tr(language, "Mipangilio", "Settings"))
                                        },
                                        modifier = Modifier.testTag("rail_settings_btn")
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                NavHost(
                                    navController = navController,
                                    startDestination = DashboardRoute,
                                    enterTransition = {
                                        fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                                            scaleIn(initialScale = 0.985f, animationSpec = tween(220, easing = FastOutSlowInEasing))
                                    },
                                    exitTransition = {
                                        fadeOut(tween(160, easing = FastOutSlowInEasing))
                                    },
                                    popEnterTransition = {
                                        fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                                            slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { -it / 16 }
                                    },
                                    popExitTransition = {
                                        fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                                            slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) { it / 16 }
                                    }
                                ) {
                                    composable<DashboardRoute> {
                                        DashboardScreen(
                                            metrics = metrics,
                                            zeroLossLedger = zeroLossLedger,
                                            categoryAnalytics = categoryAnalytics,
                                            businessHealth = businessHealth,
                                            partyCount = partyDirectory.size,
                                            privacyMaskEnabled = privacyMaskEnabled,
                                            settings = settings,
                                            selectedPeriod = selectedPeriod,
                                            language = language,
                                            updateState = updateState,
                                            onSelectPeriod = viewModel::setSelectedPeriod,
                                            onOpenTransactions = { navigateTopLevel(TransactionsRoute) },
                                            onOpenInvoices = { navigateTopLevel(InvoicesRoute) },
                                            onOpenBills = { navigateTopLevel(BillsRoute) },
                                            onOpenHub = { navigateTopLevel(HubRoute) },
                                            onOpenSettings = { navController.navigate(SettingsRoute) },
                                            onOpenPartyDirectory = { showPartyDirectory = true },
                                            onQuickAddTransaction = {
                                                quickAddTxnTrigger++
                                                navigateTopLevel(TransactionsRoute)
                                            },
                                            onSaveSmartAutoEntry = viewModel::saveSmartAutoEntry,
                                            onReconcileLedger = viewModel::reconcileLedgerNow,
                                            onExportColorExcel = viewModel::exportColorExcelReport,
                                            onExportCategoryCsv = { viewModel.exportCategoryAnalyticsCsv(selectedPeriod) }
                                        )
                                    }

                                    composable<TransactionsRoute> {
                                        TransactionsScreen(
                                            transactions = transactions,
                                            categories = categories,
                                            selectedPeriod = selectedPeriod,
                                            currency = settings.currency.ifBlank { "TZS" },
                                            language = language,
                                            openEditorTrigger = quickAddTxnTrigger,
                                            onSelectPeriod = viewModel::setSelectedPeriod,
                                            onSaveTransaction = viewModel::saveTransaction,
                                            onDeleteTransaction = viewModel::deleteTransaction,
                                            onSaveCategory = viewModel::saveCategory,
                                            onDeleteCategory = { viewModel.deleteCategory(it) },
                                            onExportTransactionsCsv = viewModel::exportTransactionsCsv,
                                            onExportCategoryAnalyticsCsv = viewModel::exportCategoryAnalyticsCsv
                                        )
                                    }

                                    composable<InvoicesRoute> {
                                        InvoicesScreen(
                                            invoices = invoices,
                                            settings = settings,
                                            language = language,
                                            generateNextNumber = viewModel::generateNextInvoiceNumber,
                                            onSaveInvoice = viewModel::saveInvoice,
                                            onMarkPaid = viewModel::markInvoicePaid,
                                            onRecordPayment = viewModel::recordInvoicePayment,
                                            onDuplicateInvoice = viewModel::duplicateInvoice,
                                            onShareInvoice = { inv ->
                                                val text = viewModel.buildInvoiceShareText(inv)
                                                val send = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_SUBJECT, "Ankara ${inv.invoice.number}")
                                                    putExtra(Intent.EXTRA_TEXT, text)
                                                }
                                                runCatching { context.startActivity(Intent.createChooser(send, null)) }
                                            },
                                            onMarkUnpaid = viewModel::markInvoiceUnpaid,
                                            onDeleteInvoice = viewModel::deleteInvoice,
                                            onExportPdf = viewModel::exportInvoicePdf,
                                            onPrintPdf = viewModel::printInvoicePdf
                                        )
                                    }

                                    composable<BillsRoute> {
                                        BillsScreen(
                                            bills = bills,
                                            zeroLossLedger = zeroLossLedger,
                                            currency = settings.currency.ifBlank { "TZS" },
                                            language = language,
                                            onSaveBill = viewModel::saveBill,
                                            onTogglePaid = viewModel::toggleBillPaid,
                                            onScheduleNextMonth = viewModel::scheduleNextRecurringBill,
                                            onDeleteBill = viewModel::deleteBill,
                                            onExportBillsCsv = viewModel::exportBillsCsv
                                        )
                                    }

                                    composable<HubRoute> {
                                        HubScreen(
                                            savingsGoals = savingsGoals,
                                            savingsLogs = savingsLogs,
                                            currency = settings.currency.ifBlank { "TZS" },
                                            language = language,
                                            onSaveGoal = viewModel::saveSavingsGoal,
                                            onDeleteGoal = viewModel::deleteSavingsGoal,
                                            onRecordSavingsTxn = viewModel::recordSavingsTransaction,
                                            onExportSavingsCsv = viewModel::exportSavingsCsv
                                        )
                                    }

                                    composable<SettingsRoute> {
                                        val summary = tr(
                                            language,
                                            "${transactions.size} miamala · ${invoices.size} ankara · ${bills.size} bili · ${savingsGoals.size} akiba",
                                            "${transactions.size} txns · ${invoices.size} invoices · ${bills.size} bills · ${savingsGoals.size} savings"
                                        )
                                        SettingsScreen(
                                            settings = settings,
                                            themeMode = themeMode,
                                            language = language,
                                            autoUpdateEnabled = autoUpdateEnabled,
                                            privacyMaskEnabled = privacyMaskEnabled,
                                            pinLockEnabled = pinLockEnabled,
                                            updateState = updateState,
                                            statsSummary = summary,
                                            hasSafetySnapshot = hasSafetySnapshot,
                                            onSetThemeMode = viewModel::setThemeMode,
                                            onSetLanguage = viewModel::setLanguage,
                                            onSetAutoUpdate = viewModel::setAutoUpdateEnabled,
                                            onSetPrivacyMask = viewModel::setPrivacyMaskEnabled,
                                            onSavePin = { pin -> viewModel.setupAppPin(pin) },
                                            onDisablePin = viewModel::disableAppPin,
                                            onLockNow = viewModel::lockAppNow,
                                            onCheckUpdates = { viewModel.checkForUpdates(silentIfUpToDate = false) },
                                            onDownloadUpdate = viewModel::startUpdateDownloadAndInstall,
                                            onOpenInstallPermissionSettings = {
                                                runCatching { context.startActivity(viewModel.openInstallPermissionSettings()) }
                                            },
                                            onOpenReleasesBrowser = {
                                                runCatching { context.startActivity(viewModel.openReleasesInBrowser()) }
                                            },
                                            onSaveSettings = viewModel::saveBusinessSettings,
                                            onExportColorExcel = viewModel::exportColorExcelReport,
                                            onExportTransactionsCsv = viewModel::exportTransactionsCsv,
                                            onExportBillsCsv = viewModel::exportBillsCsv,
                                            onExportSavingsCsv = viewModel::exportSavingsCsv,
                                            onExportMasterCsv = viewModel::exportMasterSeparatedCsv,
                                            onExportJson = viewModel::exportBackupJson,
                                            onImportJsonRequest = {
                                                openBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                            },
                                            onReconcileLedger = viewModel::reconcileLedgerNow,
                                            onRestoreSafetySnapshot = viewModel::restoreLatestSafetySnapshot,
                                            onLoadDemoData = viewModel::loadDemoData,
                                            onClearAllData = viewModel::clearAllFinancialData
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Samsung One UI 8.5 Floating Frosted-Glass Capsule Bottom Dock
                    if (showFloatingDock) {
                        OneUiFloatingNavBar(
                            tabs = PrimaryDestinations.map { dest ->
                                OneUiNavTab(
                                    label = if (language == AppLanguage.EN) dest.labelEn else dest.labelSw,
                                    icon = dest.icon,
                                    testTag = dest.tag,
                                    selected = currentDestination?.hasRoute(dest.route::class) == true,
                                    onClick = { navigateTopLevel(dest.route) }
                                )
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }

        if (showGlobalSearch) {
            GlobalSearchSheet(
                transactions = transactions,
                invoices = invoices,
                bills = bills,
                savingsGoals = savingsGoals,
                currency = settings.currency.ifBlank { "TZS" },
                language = language,
                onNavigate = { route -> navigateTopLevel(route) },
                onDismiss = { showGlobalSearch = false }
            )
        }

        if (showPartyDirectory) {
            PartyDirectorySheet(
                parties = partyDirectory,
                businessName = settings.businessName,
                currency = settings.currency.ifBlank { "TZS" },
                language = language,
                onDismiss = { showPartyDirectory = false }
            )
        }
    }
}

private fun writeBytesToUri(context: Context, uri: Uri, bytes: ByteArray): Boolean {
    return runCatching {
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return false
        true
    }.getOrDefault(false)
}

private fun readTextFromUri(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    }.getOrNull()
}
