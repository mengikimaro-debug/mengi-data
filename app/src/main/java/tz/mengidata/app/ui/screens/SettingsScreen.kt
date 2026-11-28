package tz.mengidata.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Copyright
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tz.mengidata.app.BuildConfig
import tz.mengidata.app.data.db.BusinessSettingsEntity
import tz.mengidata.app.data.db.DeletedRecordEntity
import tz.mengidata.app.data.db.PhoneStorageStatus
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.ThemeMode
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.notifications.PhonePermissionsSnapshot
import tz.mengidata.app.security.AppBiometricStatus
import tz.mengidata.app.security.BiometricAvailability
import tz.mengidata.app.security.CopyrightIntegrityReport
import tz.mengidata.app.security.CopyrightVault
import tz.mengidata.app.security.UninstallGuardStatus
import tz.mengidata.app.system.DevicePerformanceProfile
import tz.mengidata.app.system.PerformanceModePreference
import tz.mengidata.app.ui.components.ConfirmActionDialog
import tz.mengidata.app.ui.components.DevicePerformanceSettingsCard
import tz.mengidata.app.ui.components.MengiCard
import tz.mengidata.app.ui.components.OneUiFieldShape
import tz.mengidata.app.ui.components.OneUiScreenHeader
import tz.mengidata.app.system.DeviceHardwareSnapshot
import tz.mengidata.app.system.OptimizationReport
import tz.mengidata.app.ui.components.OneUiSegmentedTabs
import tz.mengidata.app.ui.components.PinSetupDialog
import tz.mengidata.app.ui.components.RecoveryVaultSheet
import tz.mengidata.app.ui.components.ReportColorLegendStrip
import tz.mengidata.app.ui.components.StatusPill
import tz.mengidata.app.ui.components.TypedConfirmDangerDialog
import tz.mengidata.app.ui.components.oneUiTextFieldColors
import tz.mengidata.app.ui.theme.ContrastAccessibility
import tz.mengidata.app.ui.theme.LocalMengiColors
import tz.mengidata.app.ui.theme.LocalMengiThemeState
import tz.mengidata.app.updater.UpdateInfo
import tz.mengidata.app.updater.UpdateUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: BusinessSettingsEntity,
    themeMode: ThemeMode,
    language: AppLanguage,
    autoUpdateEnabled: Boolean,
    autoPhoneBackupEnabled: Boolean = true,
    privacyMaskEnabled: Boolean,
    antiScreenCopyEnabled: Boolean = false,
    pinLockEnabled: Boolean,
    biometricLockEnabled: Boolean = false,
    appBiometricStatus: AppBiometricStatus = AppBiometricStatus.READY,
    copyrightIntegrity: CopyrightIntegrityReport = CopyrightIntegrityReport(),
    uninstallGuardStatus: UninstallGuardStatus = UninstallGuardStatus(),
    adbDeviceOwnerCommand: String = "",
    phonePermissions: PhonePermissionsSnapshot,
    updateState: UpdateUiState,
    statsSummary: String,
    hasSafetySnapshot: Boolean,
    deletedRecords: List<DeletedRecordEntity>,
    phoneStorageStatus: PhoneStorageStatus,
    devicePerformance: DevicePerformanceProfile = DevicePerformanceProfile.defaultBalanced(),
    deviceTelemetry: DeviceHardwareSnapshot = devicePerformance.hardware,
    isOptimizingPerformance: Boolean = false,
    lastOptimizationReport: OptimizationReport? = null,
    onSetPerformanceMode: (PerformanceModePreference) -> Unit = {},
    onOptimizeSystemNow: () -> Unit = {},
    onRefreshDeviceTelemetry: () -> Unit = {},
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetAutoUpdate: (Boolean) -> Unit,
    onSetAutoPhoneBackup: (Boolean) -> Unit = {},
    onPickCustomBackupFolder: () -> Unit = {},
    onSetPrivacyMask: (Boolean) -> Unit,
    onSetAntiScreenCopy: (Boolean) -> Unit = {},
    onCopyCopyrightCertificate: () -> Unit = {},
    onSavePin: (String) -> Unit,
    onDisablePin: () -> Unit,
    onToggleBiometricLock: (Boolean) -> Unit = {},
    onOpenEnrollBiometric: () -> Unit = {},
    onLockNow: () -> Unit,
    onActivateDeviceAdmin: () -> Unit = {},
    onOpenAccessibilityGuard: () -> Unit = {},
    onOpenRestrictedSettingsAppInfo: () -> Unit = {},
    onDisableUninstallProtection: () -> Unit = {},
    onRequestFingerprintUninstall: () -> Unit = {},
    onOpenEnrollFingerprint: () -> Unit = {},
    onCopyDeviceOwnerCommand: () -> Unit = {},
    onRequestManageStoragePermission: () -> Unit = {},
    onOpenAndroidSystemBackupSettings: () -> Unit = {},
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: (UpdateInfo) -> Unit,
    onCancelDownloadUpdate: () -> Unit = {},
    onOpenInstallPermissionSettings: () -> Unit,
    onOpenReleasesBrowser: () -> Unit,
    onDirectInstallApk: (String) -> Unit = {},
    onUninstallOldApp: () -> Unit = {},
    onSaveSettings: (BusinessSettingsEntity) -> Unit,
    onExportColorExcel: () -> Unit,
    onExportTransactionsCsv: () -> Unit,
    onExportBillsCsv: () -> Unit,
    onExportSavingsCsv: () -> Unit,
    onExportMasterCsv: () -> Unit,
    onExportJson: () -> Unit,
    onImportJsonRequest: () -> Unit,
    onReconcileLedger: () -> Unit,
    onSyncPhoneStorage: () -> Unit,
    onRestoreSafetySnapshot: () -> Unit,
    onRestoreDeletedRecord: (String) -> Unit,
    onDeleteTrashPermanently: (String) -> Unit,
    onEmptyTrash: () -> Unit,
    onLoadDemoData: () -> Unit,
    onClearAllData: () -> Unit
) {
    val colors = LocalMengiColors.current

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showRecoveryVault by remember { mutableStateOf(false) }
    var showDemoConfirm by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 920.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header") {
                OneUiScreenHeader(
                    eyebrow = tr(language, "Sanidi Mfumo na Usalama", "System & Security"),
                    title = tr(language, "Mipangilio", "Settings")
                )
            }

            // 1. Samsung One UI 8.5 Appearance, Contrast & Language Grouped Card
            item(key = "theme_card") {
                val themeState = LocalMengiThemeState.current
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                val surfaceColor = MaterialTheme.colorScheme.surface
                val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
                val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
                val bodyContrast = remember(onSurfaceColor, surfaceColor) {
                    ContrastAccessibility.contrastRatio(onSurfaceColor, surfaceColor)
                }
                val mutedContrast = remember(onSurfaceVariantColor, surfaceVariantColor) {
                    ContrastAccessibility.contrastRatio(onSurfaceVariantColor, surfaceVariantColor)
                }
                val wcagTier = if (themeState.highContrastEnabled && bodyContrast >= 7f) "WCAG AAA" else "WCAG AA"

                MengiCard {
                    OneUiSettingsSectionHeader(
                        icon = Icons.Rounded.Palette,
                        iconBg = colors.limeSoft,
                        iconTint = colors.limeInk,
                        title = tr(language, "Muonekano wa Mfumo", "Appearance & Language"),
                        subtitle = tr(language, "Chagua Mfumo, Mwanga, au Giza.", "Choose System, Light, or Dark mode."),
                        badge = {
                            StatusPill(
                                text = "$wcagTier · ${String.format(Locale.US, "%.1f:1", bodyContrast)}",
                                containerColor = colors.incomeSoft,
                                contentColor = colors.income
                            )
                        }
                    )
                    Spacer(Modifier.height(14.dp))
                    val themeModes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
                    OneUiSegmentedTabs(
                        options = listOf(
                            tr(language, "Mfumo", "System"),
                            tr(language, "Mwanga", "Light"),
                            tr(language, "Giza", "Dark")
                        ),
                        selectedIndex = themeModes.indexOf(themeMode).coerceAtLeast(0),
                        onSelect = { onSetThemeMode(themeModes[it]) },
                        testTags = themeModes.map { "theme_mode_${it.key}" }
                    )

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = colors.cardBorder)
                    Spacer(Modifier.height(12.dp))

                    // Android 12+ Material You Dynamic Color toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = tr(language, "Rangi za Mandhari ya Simu", "Material You Dynamic Color"),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (themeState.dynamicColorSupported) {
                                    tr(
                                        language,
                                        "Badilisha rangi kulingana na mandhari ya simu (Android 13+) ikiwa na ulinzi wa utofauti.",
                                        "Adapts colors to your Android 13+ wallpaper with automatic contrast protection."
                                    )
                                } else {
                                    tr(
                                        language,
                                        "Inahitaji Android 13+ — inatumia rangi salama za VaultLedger.",
                                        "Requires Android 13+ — using accessible VaultLedger palette."
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = themeState.dynamicColorEnabled && themeState.dynamicColorSupported,
                            onCheckedChange = themeState.onSetDynamicColor,
                            enabled = themeState.dynamicColorSupported,
                            modifier = Modifier.testTag("dynamic_color_switch")
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // High Contrast Accessibility toggle (WCAG AAA)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = tr(language, "Utofauti wa Juu wa Rangi", "High Contrast Mode"),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = tr(
                                    language,
                                    "Huongeza uwazi wa maandishi na mipaka (WCAG AAA ≥ 7:1) kwa Mwanga na Giza.",
                                    "Boosts text and border readability (WCAG AAA ≥ 7:1) in both Light & Dark modes."
                                ) + " (${String.format(Locale.US, "%.1f:1", mutedContrast)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = themeState.highContrastEnabled,
                            onCheckedChange = themeState.onSetHighContrast,
                            modifier = Modifier.testTag("high_contrast_switch")
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = colors.cardBorder)
                    Spacer(Modifier.height(14.dp))

                    Text(tr(language, "Lugha ya Mfumo", "App Language"), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    val langs = listOf(AppLanguage.SW, AppLanguage.EN)
                    OneUiSegmentedTabs(
                        options = listOf("Kiswahili", "English"),
                        selectedIndex = langs.indexOf(language).coerceAtLeast(0),
                        onSelect = { onSetLanguage(langs[it]) },
                        testTags = langs.map { "settings_lang_${it.code}" }
                    )
                }
            }

            // 1A. Live Adaptive Device Performance Engine Card
            item(key = "device_performance_card") {
                DevicePerformanceSettingsCard(
                    profile = devicePerformance,
                    telemetry = deviceTelemetry,
                    isOptimizing = isOptimizingPerformance,
                    lastOptimizationReport = lastOptimizationReport,
                    language = language,
                    onSelectMode = onSetPerformanceMode,
                    onOptimizeNow = onOptimizeSystemNow,
                    onRefreshTelemetry = onRefreshDeviceTelemetry
                )
            }

            // 1B. Security & Privacy Card (Biometric Lock + PIN Lock + Balance Mask)
            item(key = "security_privacy_card") {
                val anyAppLockActive = pinLockEnabled || biometricLockEnabled
                val lockStatusLabel = when {
                    biometricLockEnabled && pinLockEnabled -> tr(language, "Biometric + PIN", "Biometric + PIN")
                    biometricLockEnabled -> tr(language, "Biometric Imewashwa", "Biometric Active")
                    pinLockEnabled -> tr(language, "PIN Imewashwa", "PIN Active")
                    else -> tr(language, "Wazi", "Unlocked")
                }
                val biometricStatusHint = when (appBiometricStatus) {
                    AppBiometricStatus.READY -> tr(
                        language,
                        "Tayari (Alama ya Kidole / Uso ipo). Huhakiki kabla ya kufungua data za fedha.",
                        "Ready (Fingerprint / Face enrolled). Verifies before opening financial data."
                    )
                    AppBiometricStatus.CREDENTIAL_ONLY -> tr(
                        language,
                        "Kifuli cha kifaa (Device Lock) kipo tayari kutumika.",
                        "Device screen lock credential ready for AndroidX Biometric."
                    )
                    AppBiometricStatus.NONE_ENROLLED -> tr(
                        language,
                        "Hakuna alama ya kidole iliyosajiliwa. Gusa 'Sajili' hapa chini kuongeza.",
                        "No biometrics enrolled on this device yet. Enroll in Android Settings."
                    )
                    AppBiometricStatus.UNAVAILABLE -> tr(
                        language,
                        "Kifaa hiki hakina kihisi cha Biometric au kifuli kinachopatikana.",
                        "Biometric hardware is unavailable on this device."
                    )
                }

                MengiCard {
                    OneUiSettingsSectionHeader(
                        icon = Icons.Rounded.Lock,
                        iconBg = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = tr(language, "Usalama na Faragha", "Security & Privacy"),
                        subtitle = tr(
                            language,
                            "Linda salio na funga programu kwa Biometric (AndroidX) au namba ya siri.",
                            "Hide balances in public and secure access with AndroidX Biometrics or 4-digit PIN."
                        ),
                        badge = {
                            StatusPill(
                                text = lockStatusLabel,
                                containerColor = if (anyAppLockActive) colors.incomeSoft else colors.subtleSurface,
                                contentColor = if (anyAppLockActive) colors.income else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = tr(language, "Ficha Salio (Hali ya Faragha)", "Hide Balances (Privacy Mode)"),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = tr(
                                    language,
                                    "Huficha namba za faida na salio kwenye Dashibodi na Menyu.",
                                    "Masks profit and cash balance totals on Dashboard and Sidebar."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = privacyMaskEnabled,
                            onCheckedChange = onSetPrivacyMask,
                            modifier = Modifier.testTag("privacy_mask_switch")
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = colors.cardBorder)
                    Spacer(Modifier.height(12.dp))

                    // AndroidX Biometric App Lock Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (biometricLockEnabled) colors.incomeSoft else colors.subtleSurface,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Fingerprint,
                                        contentDescription = tr(language, "Ulinzi wa Biometric", "Biometric Lock"),
                                        tint = if (biometricLockEnabled) colors.income else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = tr(language, "Kufunga App kwa Biometric (Alama ya Kidole / Uso)", "Biometric App Lock (Fingerprint / Face)"),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = biometricStatusHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = biometricLockEnabled,
                            onCheckedChange = onToggleBiometricLock,
                            enabled = appBiometricStatus != AppBiometricStatus.UNAVAILABLE || biometricLockEnabled,
                            modifier = Modifier.testTag("biometric_lock_switch")
                        )
                    }

                    if (appBiometricStatus == AppBiometricStatus.NONE_ENROLLED) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onOpenEnrollBiometric,
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("enroll_biometrics_btn")
                        ) {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(tr(language, "Sajili Alama ya Kidole / Uso kwenye Simu", "Enroll Biometrics in Android Settings"))
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = colors.cardBorder)
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = tr(language, "Kufunga Programu kwa Namba ya Siri", "4-Digit App PIN Lock"),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = tr(
                                    language,
                                    "Huhitaji namba ya siri unapofungua VaultLedger (usimbaji wa SHA-256).",
                                    "Requires your 4-digit PIN to open VaultLedger (salted SHA-256)."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = pinLockEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    showPinSetupDialog = true
                                } else {
                                    onDisablePin()
                                }
                            },
                            modifier = Modifier.testTag("pin_lock_switch")
                        )
                    }

                    if (pinLockEnabled || biometricLockEnabled) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (pinLockEnabled) {
                                OutlinedButton(
                                    onClick = { showPinSetupDialog = true },
                                    shape = CircleShape,
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("change_pin_btn")
                                ) {
                                    Text(
                                        text = tr(language, "Badili Namba ya Siri", "Change PIN"),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = onLockNow,
                                shape = CircleShape,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("lock_app_now_btn")
                            ) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    text = tr(language, "Funga Sasa", "Lock Now"),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = colors.cardBorder)
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = tr(language, "Zuia Kunakili Skrini (Anti-Screenshot)", "Block Screenshots & Screen Recording"),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = tr(
                                    language,
                                    "Huzuia picha za skrini (screenshots) na kurekodi video ya mfumo wako.",
                                    "Prevents unauthorized screenshots and screen recording via Android FLAG_SECURE."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = antiScreenCopyEnabled,
                            onCheckedChange = onSetAntiScreenCopy,
                            modifier = Modifier.testTag("anti_screen_copy_switch")
                        )
                    }
                }
            }

            // 1C. Official Copyright & Anti-Clone Protection Card
            item(key = "copyright_ip_card") {
                MengiCard(modifier = Modifier.testTag("settings_copyright_card")) {
                    OneUiSettingsSectionHeader(
                        icon = Icons.Rounded.Copyright,
                        iconBg = colors.incomeSoft,
                        iconTint = colors.income,
                        title = tr(language, "Hatimiliki & Haki Miliki", "Copyright & Anti-Clone Guard"),
                        subtitle = tr(
                            language,
                            "© ${CopyrightVault.COPYRIGHT_YEARS} ${CopyrightVault.COPYRIGHT_HOLDER} · Ulinzi wa mfumo dhidi ya wizi na unakili.",
                            "© ${CopyrightVault.COPYRIGHT_YEARS} ${CopyrightVault.COPYRIGHT_HOLDER} · System protection against copying and cloning."
                        ),
                        badge = {
                            StatusPill(
                                text = if (copyrightIntegrity.isAuthentic) {
                                    tr(language, "Halisi", "Authentic")
                                } else {
                                    tr(language, "Onyo", "Alert")
                                },
                                containerColor = if (copyrightIntegrity.isAuthentic) colors.incomeSoft else colors.expenseSoft,
                                contentColor = if (copyrightIntegrity.isAuthentic) colors.income else colors.expense
                            )
                        }
                    )

                    Spacer(Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.subtleSurface,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "• ${tr(language, "Mmiliki", "Owner")}: ${CopyrightVault.COPYRIGHT_HOLDER} (${CopyrightVault.REGISTRATION_CODE})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "• ${tr(language, "Package ID Halisi", "Package ID")}: ${copyrightIntegrity.installedPackageId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "• ${tr(language, "Muhuri wa Umiliki", "Ownership Seal")}: ${copyrightIntegrity.ownershipSeal}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• ${tr(language, "Cheti cha Saini", "Cert SHA-256")}: ${copyrightIntegrity.signingCertShort}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = CopyrightVault.prohibitionStatement(language),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onCopyCopyrightCertificate,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_copy_copyright_btn")
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = tr(language, "Nakili Hati ya Hatimiliki", "Copy Copyright Certificate"),
                            maxLines = 1
                        )
                    }
                }
            }

            // 2. Automatic Phone Storage Backup & Zero-Loss Vault Card
            item(key = "database_phone_storage_card") {
                val lastSyncedLabel = remember(phoneStorageStatus.lastSyncedMillis, language) {
                    if (phoneStorageStatus.lastSyncedMillis > 0L) {
                        val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
                        fmt.format(Date(phoneStorageStatus.lastSyncedMillis))
                    } else {
                        tr(language, "Haijasawazishwa bado", "Not synced yet")
                    }
                }
                val sizeLabel = remember(phoneStorageStatus.backupSizeBytes) {
                    val kb = phoneStorageStatus.backupSizeBytes / 1024.0
                    if (kb >= 1024.0) String.format(Locale.US, "%.2f MB", kb / 1024.0)
                    else if (kb > 0.0) String.format(Locale.US, "%.1f KB", kb)
                    else "0 KB"
                }
                MengiCard {
                    OneUiSettingsSectionHeader(
                        icon = Icons.Rounded.PhoneAndroid,
                        iconBg = colors.incomeSoft,
                        iconTint = colors.income,
                        title = tr(language, "Hifadhi ya Data & Urejeshaji", "Data Backup & Recovery"),
                        subtitle = tr(
                            language,
                            "Android System huhifadhi na kurejesha data zako kiotomatiki bila kuhitaji kuwasha chochote.",
                            "Android System automatically backs up and restores your data in the background."
                        ),
                        badge = {
                            StatusPill(
                                text = if (phoneStorageStatus.isSyncingNow) {
                                    tr(language, "Inahifadhi...", "Syncing...")
                                } else {
                                    tr(language, "Auto", "Auto")
                                },
                                containerColor = colors.incomeSoft,
                                contentColor = colors.income
                            )
                        }
                    )
                    Spacer(Modifier.height(12.dp))

                    // Compact Read-Only Vault Summary
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.subtleSurface,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "• ${phoneStorageStatus.phoneBackupPathLabel} ($sizeLabel)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (phoneStorageStatus.hasCustomFolderBackup && phoneStorageStatus.customFolderLabel.isNotBlank()) {
                                Text(
                                    text = "• ${tr(language, "Folda Maalum", "Custom Folder")}: ${phoneStorageStatus.customFolderLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.income,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${tr(language, "Hifadhi ya mwisho", "Last auto-save")}: $lastSyncedLabel",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${tr(language, "Nakala za siku", "Daily snapshots")}: ${phoneStorageStatus.dailySnapshotCount} · ${tr(language, "Kikapu cha urejeshaji", "Recovery vault")}: ${deletedRecords.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onSyncPhoneStorage,
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sync_phone_storage_btn")
                        ) {
                            Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = tr(
                                    language,
                                    "Hifadhi Sasa",
                                    "Sync Backup Now"
                                ),
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = onPickCustomBackupFolder,
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pick_backup_folder_btn")
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = if (phoneStorageStatus.hasCustomFolderBackup) {
                                    tr(language, "Badili Folda Maalum ya Simu / SD", "Change Custom Phone / SD Folder")
                                } else {
                                    tr(language, "Chagua Folda ya Ziada (Simu / SD Card)", "Pick Extra Phone / SD Backup Folder")
                                },
                                maxLines = 1
                            )
                        }

                        FilledTonalButton(
                            onClick = { showRecoveryVault = true },
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_recovery_vault_btn")
                        ) {
                            Icon(Icons.Rounded.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = tr(
                                    language,
                                    "Kikapu cha Urejeshaji (${deletedRecords.size})",
                                    "Recovery Vault (${deletedRecords.size})"
                                ),
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = onRestoreSafetySnapshot,
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("restore_phone_storage_btn")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = tr(
                                    language,
                                    "Rejesha kutoka Hifadhi ya Simu",
                                    "Restore from Phone Storage"
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 4B. Reports & Zero-Loss Backups
            item(key = "backup_card") {
                MengiCard {
                    OneUiSettingsSectionHeader(
                        icon = Icons.Rounded.Storage,
                        iconBg = colors.warningSoft,
                        iconTint = colors.warning,
                        title = tr(language, "Ripoti (Excel/CSV) & Hifadhi", "Reports (Excel/CSV) & Backups"),
                        subtitle = statsSummary
                    )
                    Spacer(Modifier.height(12.dp))

                    ReportColorLegendStrip(language = language)
                    Spacer(Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onExportColorExcel,
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("export_color_excel_btn")
                        ) {
                            Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = tr(
                                    language,
                                    "Pakua Ripoti ya Excel (.xls — Kurasa 5)",
                                    "Export Excel Report (.xls — 5 Tabs)"
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }

                        Text(
                            text = tr(
                                language,
                                "Faili za CSV Zilizotenganishwa (Miamala, Bili na Akiba Kila Kimoja Peke Yake):",
                                "Separated CSV Exports (Transactions, Bills & Savings Separate):"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onExportTransactionsCsv,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_txns_only_csv_btn")
                            ) {
                                Text(
                                    text = tr(language, "Miamala (.csv)", "Transactions (.csv)"),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                            FilledTonalButton(
                                onClick = onExportBillsCsv,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_bills_only_csv_btn")
                            ) {
                                Text(
                                    text = tr(language, "Bili (.csv)", "Bills (.csv)"),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onExportSavingsCsv,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_savings_only_csv_btn")
                            ) {
                                Text(
                                    text = tr(language, "Akiba (.csv)", "Savings (.csv)"),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                            OutlinedButton(
                                onClick = onExportMasterCsv,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_master_csv_btn")
                            ) {
                                Text(
                                    text = tr(language, "CSV Kamili", "Full CSV"),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }

                        HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 4.dp))

                        FilledTonalButton(
                            onClick = onReconcileLedger,
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reconcile_ledger_btn")
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(tr(language, "Kagua na Sawazisha Hesabu (0% Upotevu)", "Verify & Reconcile Zero-Loss Ledger"))
                        }

                        if (hasSafetySnapshot) {
                            OutlinedButton(
                                onClick = onRestoreSafetySnapshot,
                                shape = CircleShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("restore_auto_snapshot_btn")
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text(tr(language, "Rejesha Hifadhi ya Usalama", "Restore Local Auto-Snapshot"))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onExportJson,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_json_backup_btn")
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    text = tr(language, "Nakala (.json)", "Backup (.json)"),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                            OutlinedButton(
                                onClick = onImportJsonRequest,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_json_backup_btn")
                            ) {
                                Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    text = tr(language, "Rejesha (.json)", "Restore (.json)"),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = colors.cardBorder)
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDemoConfirm = true },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(
                                text = tr(language, "Data ya Mfano", "Sample Data"),
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                        Button(
                            onClick = { showClearConfirm = true },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier.weight(1f).testTag("clear_all_data_btn")
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(
                                text = tr(language, "Futa Data", "Clear Data"),
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            language = language,
            isChangingExisting = pinLockEnabled,
            onDismiss = { showPinSetupDialog = false },
            onSavePin = { pin ->
                onSavePin(pin)
                showPinSetupDialog = false
            }
        )
    }

    if (showDemoConfirm) {
        ConfirmActionDialog(
            title = tr(language, "Pakia data ya mfano?", "Load sample business data?"),
            message = tr(language, "Data za sasa zitabadilishwa na miamala, ankara, bili na akiba ya mfano.", "Current records will be replaced with sample business data."),
            confirmLabel = tr(language, "Pakia", "Load"),
            cancelLabel = tr(language, "Ghairi", "Cancel"),
            onConfirm = { onLoadDemoData(); showDemoConfirm = false },
            onDismiss = { showDemoConfirm = false }
        )
    }

    if (showRecoveryVault) {
        RecoveryVaultSheet(
            deletedRecords = deletedRecords,
            currency = settings.currency.ifBlank { "TZS" },
            language = language,
            onRestore = onRestoreDeletedRecord,
            onDeletePermanently = onDeleteTrashPermanently,
            onEmptyVault = onEmptyTrash,
            onDismiss = { showRecoveryVault = false }
        )
    }

    if (showClearConfirm) {
        TypedConfirmDangerDialog(
            title = tr(language, "Futa data zote?", "Clear all financial data?"),
            message = tr(
                language,
                "Miamala, ankara, bili na akiba zote za sasa zitaondolewa. Nakala ya dharura itahifadhiwa kwenye hifadhi ya simu kabla ya kufuta.",
                "All current transactions, invoices, bills, and savings will be cleared. An emergency backup will be saved to phone storage first."
            ),
            requiredKeyword = tr(language, "FUTA", "DELETE"),
            confirmLabel = tr(language, "Futa Yote", "Clear All"),
            cancelLabel = tr(language, "Ghairi", "Cancel"),
            language = language,
            onConfirm = {
                onClearAllData()
                showClearConfirm = false
            },
            onDismiss = { showClearConfirm = false }
        )
    }
}

@Composable
private fun OneUiSettingsSectionHeader(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (badge != null) {
            Spacer(Modifier.size(8.dp))
            badge()
        }
    }
}

@Composable
private fun UpdateStatusBanner(
    containerColor: Color,
    contentColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
        }
    }
}
