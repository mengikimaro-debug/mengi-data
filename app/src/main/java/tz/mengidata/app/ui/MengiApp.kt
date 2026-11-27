package tz.mengidata.app.ui

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import tz.mengidata.app.R
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.components.AboutAppSheet
import tz.mengidata.app.ui.components.AutoAccountingSheet
import tz.mengidata.app.ui.components.GlobalSearchSheet
import tz.mengidata.app.ui.components.MengiSidebarSheet
import tz.mengidata.app.ui.components.OneUiFloatingNavBar
import tz.mengidata.app.ui.components.OneUiNavTab
import tz.mengidata.app.ui.components.PartyDirectorySheet
import tz.mengidata.app.ui.components.PinLockScreen
import tz.mengidata.app.ui.components.RecoveryVaultSheet
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
import tz.mengidata.app.security.BiometricAvailability
import tz.mengidata.app.security.UninstallProtectionManager
import tz.mengidata.app.system.LocalDevicePerformance
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
    val privacyMaskEnabled by viewModel.privacyMaskEnabled.collectAsStateWithLifecycle()
    val pinLockEnabled by viewModel.pinLockEnabled.collectAsStateWithLifecycle()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val devicePerformance by viewModel.devicePerformance.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingExport by remember { mutableStateOf<PendingFileExport?>(null) }
    var quickAddTxnTrigger by remember { mutableIntStateOf(0) }
    var showGlobalSearch by remember { mutableStateOf(false) }
    var showAutoAccountingSheet by remember { mutableStateOf(false) }
    var showPartyDirectory by remember { mutableStateOf(false) }
    var showGlobalRecoveryVault by remember { mutableStateOf(false) }
    var showAboutApp by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.refreshPhonePermissions()
    }

    val legacyWriteStoragePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.refreshPhonePermissions()
        viewModel.refreshStorageStatus()
        viewModel.refreshSavedPublicPdfs()
    }

    val deviceAdminActivationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        viewModel.refreshUninstallProtectionStatus()
        if (UninstallProtectionManager.isUninstallProtectionActive(context)) {
            viewModel.emitSecurityNotice(
                tr(
                    language,
                    "Ulinzi wa Alama ya Kidole umewashwa kwenye mfumo wa Android. App haiwezi ku-uninstalliwa bila Fingerprint.",
                    "Android System Fingerprint uninstall lock is now active."
                )
            )
        } else {
            viewModel.emitSecurityNotice(
                tr(
                    language,
                    "Ulinzi wa mfumo wa Android haukuwashwa. Thibitisha 'Activate' ili kuzuia kuondoa app.",
                    "Android System guard was not activated. Tap Activate to block uninstallation."
                )
            )
        }
    }

    val openEnrollFingerprintSettings: () -> Unit = {
        val openedEnroll = runCatching {
            context.startActivity(UninstallProtectionManager.buildEnrollFingerprintIntent())
        }.isSuccess
        if (!openedEnroll) {
            runCatching {
                context.startActivity(UninstallProtectionManager.buildSecuritySettingsFallbackIntent())
            }
        }
    }

    val handleToggleUninstallProtection: (Boolean) -> Unit = { enable ->
        val biometricStatus = UninstallProtectionManager.getBiometricStatus(context)
        if (enable) {
            if (biometricStatus == BiometricAvailability.NONE_ENROLLED) {
                viewModel.emitSecurityNotice(
                    tr(
                        language,
                        "Sajili Alama ya Kidole kwanza kwenye mipangilio ya Android.",
                        "Please enroll a Fingerprint in Android Settings first."
                    )
                )
                openEnrollFingerprintSettings()
            } else if (biometricStatus == BiometricAvailability.UNAVAILABLE) {
                viewModel.emitSecurityNotice(
                    tr(
                        language,
                        "Kifaa hiki hakina sensa ya Alama ya Kidole.",
                        "Fingerprint sensor is not available on this device."
                    )
                )
            } else {
                UninstallProtectionManager.authenticateFingerprint(
                    context = context,
                    title = tr(language, "Washa Ulinzi wa Kuondoa App", "Enable Uninstall Protection"),
                    subtitle = tr(language, "Thibitisha Alama ya Kidole (Android System)", "Verify Fingerprint (Android System)"),
                    description = tr(
                        language,
                        "Weka kidole chako ili kuwasha ulinzi wa mfumo wa Android wa kuzuia kuondoa VaultLedger.",
                        "Touch the fingerprint sensor to activate Android System uninstall blocking."
                    ),
                    cancelLabel = tr(language, "Ghairi", "Cancel"),
                    allowCredentialFallbackIfNoBiometric = false,
                    onSuccess = {
                        if (UninstallProtectionManager.isDeviceOwner(context)) {
                            viewModel.enableDeviceOwnerUninstallBlockAfterFingerprint()
                        } else {
                            runCatching {
                                deviceAdminActivationLauncher.launch(
                                    UninstallProtectionManager.buildActivateDeviceAdminIntent(context, language)
                                )
                            }.onFailure {
                                viewModel.emitSecurityNotice(
                                    tr(
                                        language,
                                        "Imeshindwa kufungua dirisha la ulinzi la Android.",
                                        "Could not open Android Device Admin prompt."
                                    )
                                )
                            }
                        }
                    },
                    onFailure = { reason ->
                        if (reason != "CANCELLED") {
                            viewModel.emitSecurityNotice(
                                tr(
                                    language,
                                    "Uthibitisho wa Alama ya Kidole umeshindikana.",
                                    "Fingerprint verification failed."
                                )
                            )
                        }
                    }
                )
            }
        } else {
            // Strictly require Android System Fingerprint before disabling Device Admin / Device Owner uninstall lock
            UninstallProtectionManager.authenticateFingerprint(
                context = context,
                title = tr(language, "Zima Ulinzi wa Kuondoa App", "Disable Uninstall Protection"),
                subtitle = tr(language, "Alama ya Kidole inahitajika", "Fingerprint Required"),
                description = tr(
                    language,
                    "Weka kidole chako ili kuruhusu app iweze kuondolewa (uninstall).",
                    "Verify your Fingerprint to unlock app uninstallation."
                ),
                cancelLabel = tr(language, "Ghairi", "Cancel"),
                allowCredentialFallbackIfNoBiometric = true,
                onSuccess = {
                    viewModel.deactivateUninstallProtectionAfterFingerprint(
                        notifyUser = true,
                        clearDeviceOwnerForUninstall = false
                    )
                },
                onFailure = { reason ->
                    if (reason != "CANCELLED") {
                        viewModel.emitSecurityNotice(
                            tr(
                                language,
                                "Alama ya kidole haikubaliwi — ulinzi wa kuondoa app bado upo wazi.",
                                "Fingerprint not verified — uninstall protection remains active."
                            )
                        )
                    }
                }
            )
        }
    }

    val handleFingerprintGuidedUninstall: () -> Unit = {
        val isActive = UninstallProtectionManager.isUninstallProtectionActive(context)
        if (!isActive) {
            runCatching { context.startActivity(viewModel.openUninstallOldApp()) }
        } else {
            UninstallProtectionManager.authenticateFingerprint(
                context = context,
                title = tr(language, "Ondoa VaultLedger (Uninstall)", "Uninstall VaultLedger"),
                subtitle = tr(language, "Thibitisha Alama ya Kidole", "Fingerprint Verification Required"),
                description = tr(
                    language,
                    "Weka kidole chako ili mfumo wa Android ukuruhusu kuondoa app hii.",
                    "Scan your Fingerprint so Android System unlocks and allows uninstalling VaultLedger."
                ),
                cancelLabel = tr(language, "Ghairi", "Cancel"),
                allowCredentialFallbackIfNoBiometric = true,
                onSuccess = {
                    viewModel.deactivateUninstallProtectionAfterFingerprint(
                        notifyUser = false,
                        clearDeviceOwnerForUninstall = true
                    )
                    scope.launch {
                        repeat(10) {
                            if (!UninstallProtectionManager.isUninstallProtectionActive(context)) return@repeat
                            delay(150L)
                        }
                        viewModel.refreshUninstallProtectionStatus()
                        runCatching {
                            context.startActivity(UninstallProtectionManager.buildUninstallSelfIntent(context))
                        }
                    }
                },
                onFailure = { reason ->
                    if (reason != "CANCELLED") {
                        viewModel.emitSecurityNotice(
                            tr(
                                language,
                                "Kuondoa app kumezuiwa — Alama ya Kidole haijathibitishwa.",
                                "Uninstall blocked — Fingerprint was not verified."
                            )
                        )
                    }
                }
            )
        }
    }

    val adbDeviceOwnerCommand = remember(context) {
        UninstallProtectionManager.buildAdbDeviceOwnerCommand(context)
    }

    val handleCopyDeviceOwnerCommand: () -> Unit = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("ADB Device Owner Command", adbDeviceOwnerCommand))
        viewModel.emitSecurityNotice(
            tr(
                language,
                "Amri ya ADB Device Owner imenakiliwa.",
                "ADB Device Owner command copied to clipboard."
            )
        )
    }

    val requestManageStoragePermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val openedPerApp = runCatching {
                context.startActivity(viewModel.openManageStorageSettings())
            }.isSuccess
            if (!openedPerApp) {
                runCatching {
                    context.startActivity(viewModel.openManageStorageFallbackSettings())
                }.onFailure {
                    runCatching { context.startActivity(viewModel.openAppDetailsSettings()) }
                }
            }
        } else {
            runCatching {
                legacyWriteStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPhonePermissions()
        val needsPostNotifications = withContext(Dispatchers.IO) {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !viewModel.notificationManager.isPostNotificationsGranted()
        }
        if (needsPostNotifications) {
            runCatching {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Launch PackageInstaller confirmation & FileProvider APK install intents directly from the
    // foreground Activity so Android 14/15/16 Background Activity Launch (BAL) never blocks them.
    LaunchedEffect(Unit) {
        viewModel.installLaunchRequests.collect { installIntent ->
            val activity = context as? Activity
            val launched = runCatching {
                if (activity != null) {
                    activity.startActivity(installIntent)
                } else {
                    installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(installIntent)
                }
            }.isSuccess
            if (!launched) {
                viewModel.fallbackToDirectInstallIfAvailable()
            }
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = CreateDocumentWithMimeContract()
    ) { uri: Uri? ->
        val export = pendingExport
        pendingExport = null
        if (uri != null && export != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    writeBytesToUri(context, uri, export.bytes)
                }
                if (saved) {
                    if (export.mimeType == "application/pdf") {
                        viewModel.savePublicPdfMirror(export.filename, export.bytes)
                        viewModel.notifyPdfSaved {
                            openPdfSafely(
                                context = context,
                                safUri = uri,
                                filename = export.filename,
                                pdfBytes = export.bytes,
                                viewModel = viewModel
                            )
                        }
                    } else {
                        viewModel.notifyFileSaved()
                    }
                } else {
                    viewModel.notifyFileError()
                }
            }
        }
    }

    val openBackupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            runCatching {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(treeUri, flags)
            }
            viewModel.setCustomBackupFolderUri(treeUri.toString())
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    readTextFromUri(context, uri)
                }
                if (text != null) {
                    viewModel.importBackupJson(text)
                } else {
                    viewModel.notifyFileError()
                }
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
            createDocumentLauncher.launch(export)
        }
    }

    CompositionLocalProvider(LocalDevicePerformance provides devicePerformance) {
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
                val drawerLedger by viewModel.zeroLossLedger.collectAsStateWithLifecycle()
                val drawerUpdateState by viewModel.updateState.collectAsStateWithLifecycle()
                val drawerParties by viewModel.partyDirectory.collectAsStateWithLifecycle()
                val isConvertingCurrency by viewModel.isConvertingCurrency.collectAsStateWithLifecycle()
                MengiSidebarSheet(
                    settings = settings,
                    zeroLossLedger = drawerLedger,
                    language = language,
                    updateState = drawerUpdateState,
                    privacyMaskEnabled = privacyMaskEnabled,
                    isConvertingCurrency = isConvertingCurrency,
                    partyCount = drawerParties.size,
                    selectedRouteMatch = { route -> currentDestination?.hasRoute(route::class) == true },
                    isSettingsSelected = isSettings,
                    onSelectDestination = { route ->
                        scope.launch { drawerState.close() }
                        navigateTopLevel(route)
                    },
                    onSelectCurrency = { code ->
                        viewModel.setCurrency(code)
                    },
                    onOpenAutoAccounting = {
                        scope.launch { drawerState.close() }
                        showAutoAccountingSheet = true
                    },
                    onOpenPartyDirectory = {
                        scope.launch { drawerState.close() }
                        showPartyDirectory = true
                    },
                    onOpenSettings = {
                        scope.launch { drawerState.close() }
                        openSettings()
                    },
                    onOpenAboutApp = {
                        scope.launch { drawerState.close() }
                        showAboutApp = true
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
                                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                                    actionColor = MaterialTheme.colorScheme.inversePrimary
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
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            tonalElevation = 2.dp,
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
                                    if (isSettings) {
                                        Text(
                                            text = tr(language, "Mipangilio", "Settings"),
                                            style = MaterialTheme.typography.titleLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Image(
                                                    painter = painterResource(id = R.drawable.mengidata_oneui_icon_1795525888393),
                                                    contentDescription = "VaultLedger",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Text(
                                                text = settings.businessName.ifBlank { "VaultLedger" },
                                                style = MaterialTheme.typography.titleLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
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
                                                color = if (privacyMaskEnabled) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                                },
                                                tonalElevation = 2.dp,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .testTag("topbar_privacy_toggle_btn")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (privacyMaskEnabled) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                        contentDescription = tr(language, "Ficha au onyesha salio", "Toggle balance visibility"),
                                                        tint = if (privacyMaskEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Surface(
                                                onClick = { showGlobalSearch = true },
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                tonalElevation = 2.dp,
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
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
                                val navMs = devicePerformance.navTransitionMs
                                val navExitMs = (navMs * 0.75f).toInt()
                                NavHost(
                                    navController = navController,
                                    startDestination = DashboardRoute,
                                    enterTransition = { if (navMs <= 0) EnterTransition.None else fadeIn(tween(navMs)) },
                                    exitTransition = { if (navExitMs <= 0) ExitTransition.None else fadeOut(tween(navExitMs)) },
                                    popEnterTransition = { if (navMs <= 0) EnterTransition.None else fadeIn(tween(navMs)) },
                                    popExitTransition = { if (navExitMs <= 0) ExitTransition.None else fadeOut(tween(navExitMs)) }
                                ) {
                                    composable<DashboardRoute> {
                                        val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
                                        val zeroLossLedger by viewModel.zeroLossLedger.collectAsStateWithLifecycle()
                                        val categoryAnalytics by viewModel.categoryAnalytics.collectAsStateWithLifecycle()
                                        val businessHealth by viewModel.businessHealth.collectAsStateWithLifecycle()
                                        val partyDirectory by viewModel.partyDirectory.collectAsStateWithLifecycle()
                                        val deletedRecords by viewModel.deletedRecords.collectAsStateWithLifecycle()
                                        val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
                                        val updateState by viewModel.updateState.collectAsStateWithLifecycle()
                                        DashboardScreen(
                                            metrics = metrics,
                                            zeroLossLedger = zeroLossLedger,
                                            categoryAnalytics = categoryAnalytics,
                                            businessHealth = businessHealth,
                                            partyCount = partyDirectory.size,
                                            deletedCount = deletedRecords.size,
                                            knownParties = partyDirectory,
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
                                            onOpenRecoveryVault = { showGlobalRecoveryVault = true },
                                            onSyncPhoneStorage = viewModel::syncDatabaseToPhoneStorage,
                                            onExportStatementPdf = { viewModel.exportFinancialStatementPdf(selectedPeriod) },
                                            onQuickAddTransaction = {
                                                quickAddTxnTrigger++
                                                navigateTopLevel(TransactionsRoute)
                                            },
                                            onSaveSmartAutoEntry = viewModel::saveSmartAutoEntry,
                                            onReconcileLedger = viewModel::reconcileLedgerNow,
                                            onExportColorExcel = viewModel::exportColorExcelReport,
                                            onExportCategoryCsv = { viewModel.exportCategoryAnalyticsCsv(selectedPeriod) },
                                            onDownloadUpdate = viewModel::startUpdateDownloadAndInstall,
                                            onCancelDownloadUpdate = viewModel::cancelUpdateDownload,
                                            onOpenInstallPermissionSettings = {
                                                runCatching { context.startActivity(viewModel.openInstallPermissionSettings()) }
                                            }
                                        )
                                    }

                                    composable<TransactionsRoute> {
                                        val transactions by viewModel.transactions.collectAsStateWithLifecycle()
                                        val pendingTransactions by viewModel.pendingTransactions.collectAsStateWithLifecycle()
                                        val categories by viewModel.categories.collectAsStateWithLifecycle()
                                        val partyDirectory by viewModel.partyDirectory.collectAsStateWithLifecycle()
                                        val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
                                        TransactionsScreen(
                                            transactions = transactions,
                                            pendingTransactions = pendingTransactions,
                                            categories = categories,
                                            knownParties = partyDirectory,
                                            selectedPeriod = selectedPeriod,
                                            currency = settings.currency.ifBlank { "TZS" },
                                            language = language,
                                            openEditorTrigger = quickAddTxnTrigger,
                                            onSelectPeriod = viewModel::setSelectedPeriod,
                                            onSaveTransaction = viewModel::saveTransaction,
                                            onDuplicateTransaction = viewModel::duplicateTransaction,
                                            onShareTransactionReceipt = { txn ->
                                                val send = viewModel.buildShareTransactionReceiptIntent(txn)
                                                runCatching { context.startActivity(Intent.createChooser(send, null)) }
                                            },
                                            onDeleteTransaction = viewModel::deleteTransaction,
                                            onSavePendingTransaction = viewModel::savePendingTransaction,
                                            onRecordPendingPayment = viewModel::recordPendingPayment,
                                            onCompletePendingTransaction = viewModel::completePendingTransaction,
                                            onDeletePendingTransaction = viewModel::deletePendingTransaction,
                                            onSaveCategory = viewModel::saveCategory,
                                            onDeleteCategory = { viewModel.deleteCategory(it) },
                                            onExportStatementPdf = viewModel::exportFinancialStatementPdf,
                                            onExportTransactionsCsv = viewModel::exportTransactionsCsv,
                                            onExportCategoryAnalyticsCsv = viewModel::exportCategoryAnalyticsCsv
                                        )
                                    }

                                    composable<InvoicesRoute> {
                                        val invoices by viewModel.invoices.collectAsStateWithLifecycle()
                                        val partyDirectory by viewModel.partyDirectory.collectAsStateWithLifecycle()
                                        val savedPublicPdfs by viewModel.savedPublicPdfs.collectAsStateWithLifecycle()
                                        val phonePermissions by viewModel.phonePermissions.collectAsStateWithLifecycle()
                                        InvoicesScreen(
                                            invoices = invoices,
                                            knownParties = partyDirectory,
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
                                            onShareInvoicePdf = { inv ->
                                                scope.launch {
                                                    val pdfIntent = withContext(Dispatchers.IO) {
                                                        viewModel.buildShareInvoicePdfIntent(inv)
                                                    }
                                                    if (pdfIntent != null) {
                                                        runCatching { context.startActivity(Intent.createChooser(pdfIntent, null)) }
                                                    } else {
                                                        viewModel.exportInvoicePdf(inv)
                                                    }
                                                }
                                            },
                                            onMarkUnpaid = viewModel::markInvoiceUnpaid,
                                            onDeleteInvoice = viewModel::deleteInvoice,
                                            onOpenPdf = { inv ->
                                                scope.launch {
                                                    val viewIntent = withContext(Dispatchers.IO) {
                                                        viewModel.buildViewInvoicePdfIntent(inv)
                                                    }
                                                    if (viewIntent != null) {
                                                        val opened = runCatching {
                                                            context.startActivity(Intent.createChooser(viewIntent, null))
                                                        }.isSuccess
                                                        if (!opened) viewModel.notifyNoPdfViewer()
                                                    } else {
                                                        viewModel.notifyNoPdfViewer()
                                                    }
                                                }
                                            },
                                            onExportPdf = viewModel::exportInvoicePdf,
                                            onPrintPdf = viewModel::printInvoicePdf,
                                            savedPublicPdfs = savedPublicPdfs,
                                            storageManagerGranted = phonePermissions.storageManagerGranted,
                                            onRequestManageStoragePermission = requestManageStoragePermission,
                                            onRefreshSavedPublicPdfs = viewModel::refreshSavedPublicPdfs,
                                            onOpenSavedPublicPdf = { pdf ->
                                                val viewIntent = viewModel.buildViewSavedPublicPdfIntent(pdf)
                                                if (viewIntent != null) {
                                                    val opened = runCatching {
                                                        context.startActivity(Intent.createChooser(viewIntent, null))
                                                    }.isSuccess
                                                    if (!opened) viewModel.notifyNoPdfViewer()
                                                } else {
                                                    viewModel.notifyNoPdfViewer()
                                                }
                                            },
                                            onShareSavedPublicPdf = { pdf ->
                                                val shareIntent = viewModel.buildShareSavedPublicPdfIntent(pdf)
                                                if (shareIntent != null) {
                                                    runCatching { context.startActivity(Intent.createChooser(shareIntent, null)) }
                                                }
                                            },
                                            onDeleteSavedPublicPdf = viewModel::deleteSavedPublicPdf
                                        )
                                    }

                                    composable<BillsRoute> {
                                        val bills by viewModel.bills.collectAsStateWithLifecycle()
                                        val transactions by viewModel.transactions.collectAsStateWithLifecycle()
                                        val zeroLossLedger by viewModel.zeroLossLedger.collectAsStateWithLifecycle()
                                        val partyDirectory by viewModel.partyDirectory.collectAsStateWithLifecycle()
                                        BillsScreen(
                                            bills = bills,
                                            transactions = transactions,
                                            zeroLossLedger = zeroLossLedger,
                                            knownParties = partyDirectory,
                                            currency = settings.currency.ifBlank { "TZS" },
                                            language = language,
                                            onSaveBill = viewModel::saveBill,
                                            onTogglePaid = viewModel::toggleBillPaid,
                                            onDuplicateBill = viewModel::duplicateBill,
                                            onPayAllDueBills = viewModel::payAllDueBills,
                                            onScheduleNextMonth = viewModel::scheduleNextRecurringBill,
                                            onDeleteBill = viewModel::deleteBill,
                                            onExportBillsCsv = viewModel::exportBillsCsv
                                        )
                                    }

                                    composable<HubRoute> {
                                        val savingsGoals by viewModel.savingsGoals.collectAsStateWithLifecycle()
                                        val savingsLogs by viewModel.savingsLogs.collectAsStateWithLifecycle()
                                        val partyDirectory by viewModel.partyDirectory.collectAsStateWithLifecycle()
                                        HubScreen(
                                            savingsGoals = savingsGoals,
                                            savingsLogs = savingsLogs,
                                            knownParties = partyDirectory,
                                            currency = settings.currency.ifBlank { "TZS" },
                                            language = language,
                                            onSaveGoal = viewModel::saveSavingsGoal,
                                            onDeleteGoal = viewModel::deleteSavingsGoal,
                                            onRecordSavingsTxn = viewModel::recordSavingsTransaction,
                                            onExportSavingsCsv = viewModel::exportSavingsCsv
                                        )
                                    }

                                    composable<SettingsRoute> {
                                        val autoUpdateEnabled by viewModel.autoUpdateEnabled.collectAsStateWithLifecycle()
                                        val autoPhoneBackupEnabled by viewModel.autoPhoneBackupEnabled.collectAsStateWithLifecycle()
                                        val uninstallGuardStatus by viewModel.uninstallGuardStatus.collectAsStateWithLifecycle()
                                        val phonePermissions by viewModel.phonePermissions.collectAsStateWithLifecycle()
                                        val updateState by viewModel.updateState.collectAsStateWithLifecycle()
                                        val hasSafetySnapshot by viewModel.hasSafetySnapshot.collectAsStateWithLifecycle()
                                        val deletedRecords by viewModel.deletedRecords.collectAsStateWithLifecycle()
                                        val phoneStorageStatus by viewModel.phoneStorageStatus.collectAsStateWithLifecycle()
                                        val transactions by viewModel.transactions.collectAsStateWithLifecycle()
                                        val invoices by viewModel.invoices.collectAsStateWithLifecycle()
                                        val bills by viewModel.bills.collectAsStateWithLifecycle()
                                        val savingsGoals by viewModel.savingsGoals.collectAsStateWithLifecycle()
                                        val summary = tr(
                                            language,
                                            "${transactions.size} miamala · ${invoices.size} ankara · ${bills.size} bili · ${savingsGoals.size} akiba",
                                            "${transactions.size} transactions · ${invoices.size} invoices · ${bills.size} bills · ${savingsGoals.size} savings"
                                        )
                                        SettingsScreen(
                                            settings = settings,
                                            themeMode = themeMode,
                                            language = language,
                                            autoUpdateEnabled = autoUpdateEnabled,
                                            autoPhoneBackupEnabled = autoPhoneBackupEnabled,
                                            privacyMaskEnabled = privacyMaskEnabled,
                                            pinLockEnabled = pinLockEnabled,
                                            uninstallProtectionActive = uninstallGuardStatus.isProtected,
                                            isDeviceOwner = uninstallGuardStatus.isDeviceOwner,
                                            adbDeviceOwnerCommand = adbDeviceOwnerCommand,
                                            biometricAvailability = uninstallGuardStatus.biometricAvailability,
                                            phonePermissions = phonePermissions,
                                            updateState = updateState,
                                            statsSummary = summary,
                                            hasSafetySnapshot = hasSafetySnapshot,
                                            deletedRecords = deletedRecords,
                                            phoneStorageStatus = phoneStorageStatus,
                                            devicePerformance = devicePerformance,
                                            onSetPerformanceMode = viewModel::setPerformanceMode,
                                            onOptimizeSystemNow = viewModel::optimizeSystemNow,
                                            onSetThemeMode = viewModel::setThemeMode,
                                            onSetLanguage = viewModel::setLanguage,
                                            onSetAutoUpdate = viewModel::setAutoUpdateEnabled,
                                            onSetAutoPhoneBackup = viewModel::setAutoPhoneBackupEnabled,
                                            onPickCustomBackupFolder = {
                                                runCatching { openBackupFolderLauncher.launch(null) }
                                            },
                                            onSetPrivacyMask = viewModel::setPrivacyMaskEnabled,
                                            onSavePin = { pin -> viewModel.setupAppPin(pin) },
                                            onDisablePin = viewModel::disableAppPin,
                                            onLockNow = viewModel::lockAppNow,
                                            onToggleUninstallProtection = handleToggleUninstallProtection,
                                            onRequestFingerprintUninstall = handleFingerprintGuidedUninstall,
                                            onOpenEnrollFingerprint = openEnrollFingerprintSettings,
                                            onCopyDeviceOwnerCommand = handleCopyDeviceOwnerCommand,
                                            onRequestManageStoragePermission = requestManageStoragePermission,
                                            onCheckUpdates = { viewModel.checkForUpdates(silentIfUpToDate = false) },
                                            onDownloadUpdate = viewModel::startUpdateDownloadAndInstall,
                                            onCancelDownloadUpdate = viewModel::cancelUpdateDownload,
                                            onOpenInstallPermissionSettings = {
                                                runCatching { context.startActivity(viewModel.openInstallPermissionSettings()) }
                                            },
                                            onOpenReleasesBrowser = {
                                                runCatching { context.startActivity(viewModel.openReleasesInBrowser()) }
                                            },
                                            onDirectInstallApk = viewModel::launchDirectApkInstall,
                                            onUninstallOldApp = handleFingerprintGuidedUninstall,
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
                                            onSyncPhoneStorage = viewModel::syncDatabaseToPhoneStorage,
                                            onRestoreSafetySnapshot = viewModel::restoreLatestSafetySnapshot,
                                            onRestoreDeletedRecord = viewModel::restoreDeletedRecord,
                                            onDeleteTrashPermanently = viewModel::permanentlyDeleteTrashRecord,
                                            onEmptyTrash = viewModel::emptyTrash,
                                            onLoadDemoData = viewModel::loadDemoData,
                                            onClearAllData = viewModel::clearAllFinancialData
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Samsung One UI 8.5 Solid Opaque Floating Capsule Bottom Dock
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
            val transactions by viewModel.transactions.collectAsStateWithLifecycle()
            val invoices by viewModel.invoices.collectAsStateWithLifecycle()
            val bills by viewModel.bills.collectAsStateWithLifecycle()
            val savingsGoals by viewModel.savingsGoals.collectAsStateWithLifecycle()
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

        if (showAutoAccountingSheet) {
            val zeroLossLedger by viewModel.zeroLossLedger.collectAsStateWithLifecycle()
            AutoAccountingSheet(
                ledger = zeroLossLedger,
                currency = settings.currency.ifBlank { "TZS" },
                language = language,
                onVerifyLedger = viewModel::reconcileLedgerNow,
                onExportColorExcel = viewModel::exportColorExcelReport,
                onDismiss = { showAutoAccountingSheet = false }
            )
        }

        if (showPartyDirectory) {
            val partyDirectory by viewModel.partyDirectory.collectAsStateWithLifecycle()
            PartyDirectorySheet(
                parties = partyDirectory,
                businessName = settings.businessName,
                currency = settings.currency.ifBlank { "TZS" },
                language = language,
                onDismiss = { showPartyDirectory = false }
            )
        }

        if (showGlobalRecoveryVault) {
            val deletedRecords by viewModel.deletedRecords.collectAsStateWithLifecycle()
            RecoveryVaultSheet(
                deletedRecords = deletedRecords,
                currency = settings.currency.ifBlank { "TZS" },
                language = language,
                onRestore = viewModel::restoreDeletedRecord,
                onDeletePermanently = viewModel::permanentlyDeleteTrashRecord,
                onEmptyVault = viewModel::emptyTrash,
                onDismiss = { showGlobalRecoveryVault = false }
            )
        }

        if (showAboutApp) {
            AboutAppSheet(
                language = language,
                onDismiss = { showAboutApp = false }
            )
        }
        }
    }
}

private class CreateDocumentWithMimeContract : ActivityResultContract<PendingFileExport, Uri?>() {
    override fun createIntent(context: Context, input: PendingFileExport): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = input.mimeType.ifBlank { "application/octet-stream" }
            putExtra(Intent.EXTRA_TITLE, input.filename)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) intent?.data else null
    }
}

private fun openPdfSafely(
    context: Context,
    safUri: Uri,
    filename: String,
    pdfBytes: ByteArray,
    viewModel: MengiViewModel
) {
    // 1. Prefer FileProvider URI with guaranteed application/pdf MIME & read permission
    val fileProviderIntent = viewModel.buildViewPdfFileIntent(filename, pdfBytes)
    if (fileProviderIntent != null) {
        val opened = runCatching {
            context.startActivity(Intent.createChooser(fileProviderIntent, null))
        }.isSuccess
        if (opened) return
    }
    // 2. Fallback to the SAF URI directly with explicit application/pdf MIME
    val safIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(safUri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val openedSaf = runCatching {
        context.startActivity(Intent.createChooser(safIntent, null))
    }.isSuccess
    if (!openedSaf) {
        viewModel.notifyNoPdfViewer()
    }
}

private fun writeBytesToUri(context: Context, uri: Uri, bytes: ByteArray): Boolean {
    if (bytes.isEmpty()) return false
    return runCatching {
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: context.contentResolver.openOutputStream(uri)
            ?: return false
        stream.use { out ->
            out.write(bytes)
            out.flush()
        }
        true
    }.getOrDefault(false)
}

private fun readTextFromUri(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    }.getOrNull()
}
