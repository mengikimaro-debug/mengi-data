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
import androidx.compose.material.icons.rounded.ContentCopy
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
import tz.mengidata.app.security.BiometricAvailability
import tz.mengidata.app.system.DevicePerformanceProfile
import tz.mengidata.app.system.PerformanceModePreference
import tz.mengidata.app.ui.components.ConfirmActionDialog
import tz.mengidata.app.ui.components.DevicePerformanceSettingsCard
import tz.mengidata.app.ui.components.MengiCard
import tz.mengidata.app.ui.components.OneUiFieldShape
import tz.mengidata.app.ui.components.OneUiScreenHeader
import tz.mengidata.app.ui.components.OneUiSegmentedTabs
import tz.mengidata.app.ui.components.PinSetupDialog
import tz.mengidata.app.ui.components.RecoveryVaultSheet
import tz.mengidata.app.ui.components.ReportColorLegendStrip
import tz.mengidata.app.ui.components.StatusPill
import tz.mengidata.app.ui.components.TypedConfirmDangerDialog
import tz.mengidata.app.ui.components.UpdateCenterCard
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
    pinLockEnabled: Boolean,
    uninstallProtectionActive: Boolean = false,
    isDeviceOwner: Boolean = false,
    adbDeviceOwnerCommand: String = "",
    biometricAvailability: BiometricAvailability = BiometricAvailability.READY,
    phonePermissions: PhonePermissionsSnapshot,
    updateState: UpdateUiState,
    statsSummary: String,
    hasSafetySnapshot: Boolean,
    deletedRecords: List<DeletedRecordEntity>,
    phoneStorageStatus: PhoneStorageStatus,
    devicePerformance: DevicePerformanceProfile = DevicePerformanceProfile.defaultBalanced(),
    onSetPerformanceMode: (PerformanceModePreference) -> Unit = {},
    onOptimizeSystemNow: () -> Unit = {},
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetAutoUpdate: (Boolean) -> Unit,
    onSetAutoPhoneBackup: (Boolean) -> Unit = {},
    onPickCustomBackupFolder: () -> Unit = {},
    onSetPrivacyMask: (Boolean) -> Unit,
    onSavePin: (String) -> Unit,
    onDisablePin: () -> Unit,
    onLockNow: () -> Unit,
    onToggleUninstallProtection: (Boolean) -> Unit = {},
    onRequestFingerprintUninstall: () -> Unit = {},
    onOpenEnrollFingerprint: () -> Unit = {},
    onCopyDeviceOwnerCommand: () -> Unit = {},
    onRequestManageStoragePermission: () -> Unit = {},
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

    var businessName by remember(settings) { mutableStateOf(settings.businessName) }
    var ownerName by remember(settings) { mutableStateOf(settings.ownerName) }
    var tagline by remember(settings) { mutableStateOf(settings.tagline) }
    var address by remember(settings) { mutableStateOf(settings.address) }
    var phone by remember(settings) { mutableStateOf(settings.phone) }
    var email by remember(settings) { mutableStateOf(settings.email) }
    var tin by remember(settings) { mutableStateOf(settings.tin) }
    var invoicePrefix by remember(settings) { mutableStateOf(settings.invoicePrefix) }
    var vatRateText by remember(settings) { mutableStateOf(settings.vatRate.toString()) }
    var paymentDetails by remember(settings) { mutableStateOf(settings.paymentDetails) }
    var invoiceNote by remember(settings) { mutableStateOf(settings.invoiceNote) }

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
                    eyebrow = tr(language, "Sanidi Mfumo na Masasisho", "System & Updates"),
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

            // 1A. Adaptive Device Performance Card (auto-tunes for any phone)
            item(key = "device_performance_card") {
                DevicePerformanceSettingsCard(
                    profile = devicePerformance,
                    language = language,
                    onSelectMode = onSetPerformanceMode,
                    onOptimizeNow = onOptimizeSystemNow
                )
            }

            // 1B. Security & Privacy Card (PIN Lock + Balance Mask)
            item(key = "security_privacy_card") {
                MengiCard {
                    OneUiSettingsSectionHeader(
                        icon = Icons.Rounded.Lock,
                        iconBg = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = tr(language, "Usalama na Faragha", "Security & Privacy"),
                        subtitle = tr(language, "Linda salio na funga programu kwa namba ya siri ya tarakimu 4.", "Hide balances in public and lock app with a 4-digit PIN."),
                        badge = {
                            val isProtected = pinLockEnabled || uninstallProtectionActive || isDeviceOwner
                            val badgeText = when {
                                isDeviceOwner -> tr(language, "Device Owner", "Device Owner")
                                pinLockEnabled && uninstallProtectionActive -> tr(language, "Ulinzi Kamili", "Full Guard")
                                uninstallProtectionActive -> tr(language, "Alama ya Kidole", "Fingerprint Lock")
                                pinLockEnabled -> tr(language, "PIN Imewashwa", "PIN Active")
                                else -> tr(language, "Wazi", "Unlocked")
                            }
                            StatusPill(
                                text = badgeText,
                                containerColor = if (isProtected) colors.incomeSoft else colors.subtleSurface,
                                contentColor = if (isProtected) colors.income else MaterialTheme.colorScheme.onSurfaceVariant
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

                    if (pinLockEnabled) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showPinSetupDialog = true },
                                shape = CircleShape,
                                modifier = Modifier.weight(1f).testTag("change_pin_btn")
                            ) {
                                Text(
                                    text = tr(language, "Badili Namba ya Siri", "Change PIN"),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                            FilledTonalButton(
                                onClick = onLockNow,
                                shape = CircleShape,
                                modifier = Modifier.weight(1f).testTag("lock_app_now_btn")
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

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = colors.cardBorder)
                    Spacer(Modifier.height(14.dp))

                    // Android System Fingerprint Uninstall Protection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (uninstallProtectionActive) colors.incomeSoft else colors.subtleSurface,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Fingerprint,
                                        contentDescription = tr(language, "Alama ya kidole", "Fingerprint"),
                                        tint = if (uninstallProtectionActive) colors.income else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = tr(
                                        language,
                                        "Ulinzi wa Kuondoa App (Fingerprint)",
                                        "Fingerprint Uninstall Protection"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = when {
                                        isDeviceOwner && uninstallProtectionActive -> tr(
                                            language,
                                            "Android Enterprise Device Owner: Kuondoa (uninstall) kumezuiwa kabisa katika mfumo mzima hadi uthibitishe Fingerprint.",
                                            "Android Enterprise Device Owner: Uninstall is strictly blocked system-wide until Fingerprint verification."
                                        )
                                        uninstallProtectionActive -> tr(
                                            language,
                                            "Mfumo wa Android unazuia app ku-uninstalliwa bila uthibitisho wa Alama ya Kidole (Fingerprint).",
                                            "Android System blocks uninstalling VaultLedger until your Fingerprint is verified."
                                        )
                                        biometricAvailability == BiometricAvailability.NONE_ENROLLED -> tr(
                                            language,
                                            "Sajili Alama ya Kidole kwenye mipangilio ya Android ili kuwasha ulinzi wa kuondoa app.",
                                            "Enroll a Fingerprint in Android Settings first to enable uninstall protection."
                                        )
                                        biometricAvailability == BiometricAvailability.UNAVAILABLE -> tr(
                                            language,
                                            "Kifaa hiki hakina sensa ya Alama ya Kidole inayopatikana.",
                                            "Fingerprint hardware is unavailable on this device."
                                        )
                                        else -> tr(
                                            language,
                                            "Huzuia mtu yeyote ku-uninstall app kupitia mfumo wa Android hadi athibitishe Alama ya Kidole.",
                                            "Locks uninstallation at the Android System level until Fingerprint is verified."
                                        )
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uninstallProtectionActive) colors.income else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = uninstallProtectionActive,
                            onCheckedChange = onToggleUninstallProtection,
                            modifier = Modifier.testTag("uninstall_protection_switch")
                        )
                    }

                    if (uninstallProtectionActive) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onRequestFingerprintUninstall,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("fingerprint_uninstall_app_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = tr(
                                    language,
                                    "Thibitisha Fingerprint & Ondoa App",
                                    "Verify Fingerprint & Uninstall App"
                                ),
                                maxLines = 1
                            )
                        }
                    } else if (biometricAvailability == BiometricAvailability.NONE_ENROLLED) {
                        Spacer(Modifier.height(10.dp))
                        FilledTonalButton(
                            onClick = onOpenEnrollFingerprint,
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("enroll_fingerprint_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = tr(
                                    language,
                                    "Sajili Alama ya Kidole (Android)",
                                    "Enroll Fingerprint in Android"
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    // Android Enterprise Device Owner (MDM / Kiosk) status & ADB provisioning command
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.subtleSurface,
                        border = BorderStroke(
                            1.dp,
                            if (isDeviceOwner) colors.income.copy(alpha = 0.45f) else colors.cardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        tint = if (isDeviceOwner) colors.income else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = tr(
                                            language,
                                            "Android Enterprise (Device Owner)",
                                            "Android Enterprise (Device Owner)"
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                StatusPill(
                                    text = if (isDeviceOwner) {
                                        tr(language, "IMEWASHWA", "ACTIVE")
                                    } else {
                                        tr(language, "MDM / ADB", "MDM / ADB")
                                    },
                                    containerColor = if (isDeviceOwner) colors.incomeSoft else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = if (isDeviceOwner) colors.income else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isDeviceOwner) {
                                Text(
                                    text = tr(
                                        language,
                                        "Simu hii imesajiliwa kama Device Owner. Kuondoa programu kumezuiwa kabisa katika mfumo wa Android.",
                                        "Provisioned as Device Owner. App uninstallation is strictly locked at the OS level."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.income
                                )
                            } else if (adbDeviceOwnerCommand.isNotBlank()) {
                                Text(
                                    text = tr(
                                        language,
                                        "Kwa ulinzi wa Kiosk/MDM usioondolewa kwa vyovyote, endesha amri hii kupitia ADB kwenye kifaa kisicho na akaunti:",
                                        "For unremovable MDM/Kiosk protection, provision via ADB on a fresh device (no accounts added):"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, colors.cardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = adbDeviceOwnerCommand,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                                OutlinedButton(
                                    onClick = onCopyDeviceOwnerCommand,
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, colors.cardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("copy_device_owner_cmd_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.size(6.dp))
                                    Text(
                                        text = tr(language, "Nakili Amri ya ADB", "Copy ADB Command"),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Flagship Bespoke OTA Update Console (4x Turbo Multi-Stream)
            item(key = "update_card") {
                UpdateCenterCard(
                    language = language,
                    autoUpdateEnabled = autoUpdateEnabled,
                    updateState = updateState,
                    onSetAutoUpdate = onSetAutoUpdate,
                    onCheckUpdates = onCheckUpdates,
                    onDownloadUpdate = onDownloadUpdate,
                    onCancelDownload = onCancelDownloadUpdate,
                    onOpenInstallPermissionSettings = onOpenInstallPermissionSettings,
                    onOpenReleasesBrowser = onOpenReleasesBrowser,
                    onDirectInstallApk = onDirectInstallApk,
                    onUninstallOldApp = onUninstallOldApp
                )
            }

            // 3. Business Profile & Invoice Defaults
            item(key = "business_form") {
                MengiCard {
                    OneUiSettingsSectionHeader(
                        icon = Icons.Rounded.Business,
                        iconBg = colors.incomeSoft,
                        iconTint = colors.income,
                        title = tr(language, "Taarifa za Biashara & Ankara", "Business Profile & Invoice Defaults"),
                        subtitle = tr(language, "Zinaonekana kwenye ankara za PDF na risiti.", "Printed on your PDF invoices and business headers.")
                    )
                    Spacer(Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text(tr(language, "Jina la Biashara *", "Business Name *")) },
                            singleLine = true,
                            shape = OneUiFieldShape,
                            colors = oneUiTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = ownerName,
                                onValueChange = { ownerName = it },
                                label = { Text(tr(language, "Mmiliki", "Owner")) },
                                singleLine = true,
                                shape = OneUiFieldShape,
                                colors = oneUiTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = tin,
                                onValueChange = { tin = it },
                                label = { Text("TIN") },
                                singleLine = true,
                                shape = OneUiFieldShape,
                                colors = oneUiTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = tagline,
                            onValueChange = { tagline = it },
                            label = { Text(tr(language, "Maelezo Mafupi ya Biashara", "Tagline")) },
                            singleLine = true,
                            shape = OneUiFieldShape,
                            colors = oneUiTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text(tr(language, "Simu", "Phone")) },
                                singleLine = true,
                                shape = OneUiFieldShape,
                                colors = oneUiTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text(tr(language, "Barua pepe", "Email")) },
                                singleLine = true,
                                shape = OneUiFieldShape,
                                colors = oneUiTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text(tr(language, "Anwani ya Ofisi", "Office Address")) },
                            singleLine = true,
                            shape = OneUiFieldShape,
                            colors = oneUiTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = invoicePrefix,
                                onValueChange = { invoicePrefix = it.take(6).uppercase() },
                                label = { Text(tr(language, "Kiambishi cha Ankara", "Invoice Prefix")) },
                                singleLine = true,
                                shape = OneUiFieldShape,
                                colors = oneUiTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = vatRateText,
                                onValueChange = { vatRateText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("VAT (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = OneUiFieldShape,
                                colors = oneUiTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = paymentDetails,
                            onValueChange = { paymentDetails = it },
                            label = { Text(tr(language, "Maelezo ya Malipo (M-Pesa / Benki)", "Payment Details")) },
                            minLines = 2,
                            shape = OneUiFieldShape,
                            colors = oneUiTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = invoiceNote,
                            onValueChange = { invoiceNote = it },
                            label = { Text(tr(language, "Maelezo ya chini ya ankara", "Invoice Footer Note")) },
                            minLines = 2,
                            shape = OneUiFieldShape,
                            colors = oneUiTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onSaveSettings(
                                    settings.copy(
                                        businessName = businessName.trim().ifBlank { "VaultLedger" },
                                        ownerName = ownerName.trim(),
                                        tagline = tagline.trim(),
                                        address = address.trim(),
                                        phone = phone.trim(),
                                        email = email.trim(),
                                        tin = tin.trim(),
                                        invoicePrefix = invoicePrefix.trim().ifBlank { "MD" },
                                        vatRate = (vatRateText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0),
                                        paymentDetails = paymentDetails.trim(),
                                        invoiceNote = invoiceNote.trim()
                                    )
                                )
                            },
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_settings_btn")
                        ) {
                            Text(tr(language, "Hifadhi Taarifa za Biashara", "Save Business Settings"))
                        }
                    }
                }
            }

            // 4A. Automatic Phone Storage Backup & Zero-Loss Vault Card
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
                        title = tr(language, "Hifadhi Kiotomatiki kwenye Simu", "Auto Phone Storage Backup"),
                        subtitle = tr(
                            language,
                            "Data hujihifadhi zenyewe kwenye simu kila unapofanya badiliko au kufunga app.",
                            "Data automatically backs itself up to phone storage on every change & exit."
                        ),
                        badge = {
                            StatusPill(
                                text = when {
                                    phoneStorageStatus.isSyncingNow -> tr(language, "Inahifadhi...", "Syncing...")
                                    autoPhoneBackupEnabled -> tr(language, "Auto: WAZI", "Auto: ON")
                                    else -> tr(language, "Imesimama", "Paused")
                                },
                                containerColor = if (autoPhoneBackupEnabled) colors.incomeSoft else colors.warningSoft,
                                contentColor = if (autoPhoneBackupEnabled) colors.income else colors.warning
                            )
                        }
                    )
                    Spacer(Modifier.height(12.dp))

                    // Auto-Backup Master Toggle
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.subtleSurface,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(
                                        language,
                                        "Hifadhi Data Kiotomatiki (Auto-Save)",
                                        "Automatic Phone Storage Sync"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(
                                        language,
                                        "Huhifadhi nakala ya papo hapo + nakala za siku 7 na hurejesha kiotomatiki ukisakinisha upya.",
                                        "Instant auto-mirror + 7-day daily history + automatic restore on reinstall."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoPhoneBackupEnabled,
                                onCheckedChange = onSetAutoPhoneBackup,
                                modifier = Modifier.testTag("auto_phone_backup_switch")
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Live Storage Proof Panel
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = colors.income,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = tr(
                                        language,
                                        "Mahali Data Zinapohifadhiwa Kwenye Simu",
                                        "Active Phone Storage Vault Locations"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                            HorizontalDivider(color = colors.cardBorder)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (phonePermissions.storageManagerGranted) Icons.Rounded.CheckCircle else Icons.Rounded.Storage,
                                        contentDescription = null,
                                        tint = if (phonePermissions.storageManagerGranted) colors.income else colors.warning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (phonePermissions.storageManagerGranted) {
                                            tr(language, "Ruhusa ya Hifadhi Kamili (Storage Manager): Imewashwa", "All Files Access (Storage Manager): Granted")
                                        } else {
                                            tr(language, "Ruhusu Hifadhi Kamili (Storage Manager) kuhifadhi na kusoma PDF za ankara", "Grant Storage Manager access to save & retrieve Invoice PDFs")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (phonePermissions.storageManagerGranted) colors.income else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (!phonePermissions.storageManagerGranted) {
                                    FilledTonalButton(
                                        onClick = onRequestManageStoragePermission,
                                        shape = CircleShape,
                                        modifier = Modifier.testTag("perm_storage_manager_btn")
                                    ) {
                                        Text(tr(language, "Ruhusu", "Allow"), style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
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
                                    "Hifadhi kwenye Simu Sasa",
                                    "Sync Database to Phone Storage Now"
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

                        if (hasSafetySnapshot || phoneStorageStatus.hasAnyBackup) {
                            OutlinedButton(
                                onClick = onRestoreSafetySnapshot,
                                shape = CircleShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
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
