package tz.mengidata.app.ui.screens

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
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
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
import androidx.compose.ui.unit.dp
import tz.mengidata.app.BuildConfig
import tz.mengidata.app.data.db.BusinessSettingsEntity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.ThemeMode
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.ui.components.ConfirmActionDialog
import tz.mengidata.app.ui.components.MengiCard
import tz.mengidata.app.ui.components.OneUiFieldShape
import tz.mengidata.app.ui.components.OneUiScreenHeader
import tz.mengidata.app.ui.components.OneUiSegmentedTabs
import tz.mengidata.app.ui.components.PinSetupDialog
import tz.mengidata.app.ui.components.ReportColorLegendStrip
import tz.mengidata.app.ui.components.StatusPill
import tz.mengidata.app.ui.components.oneUiTextFieldColors
import tz.mengidata.app.ui.theme.ContrastAccessibility
import tz.mengidata.app.ui.theme.LocalMengiColors
import tz.mengidata.app.ui.theme.LocalMengiThemeState
import tz.mengidata.app.updater.UpdateInfo
import tz.mengidata.app.updater.UpdateUiState
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: BusinessSettingsEntity,
    themeMode: ThemeMode,
    language: AppLanguage,
    autoUpdateEnabled: Boolean,
    privacyMaskEnabled: Boolean,
    pinLockEnabled: Boolean,
    updateState: UpdateUiState,
    statsSummary: String,
    hasSafetySnapshot: Boolean,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetAutoUpdate: (Boolean) -> Unit,
    onSetPrivacyMask: (Boolean) -> Unit,
    onSavePin: (String) -> Unit,
    onDisablePin: () -> Unit,
    onLockNow: () -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: (UpdateInfo) -> Unit,
    onOpenInstallPermissionSettings: () -> Unit,
    onOpenReleasesBrowser: () -> Unit,
    onSaveSettings: (BusinessSettingsEntity) -> Unit,
    onExportColorExcel: () -> Unit,
    onExportTransactionsCsv: () -> Unit,
    onExportBillsCsv: () -> Unit,
    onExportSavingsCsv: () -> Unit,
    onExportMasterCsv: () -> Unit,
    onExportJson: () -> Unit,
    onImportJsonRequest: () -> Unit,
    onReconcileLedger: () -> Unit,
    onRestoreSafetySnapshot: () -> Unit,
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
                    eyebrow = tr(language, "Sanidi Mfumo & Updates", "System & Updates"),
                    title = tr(language, "Mipangilio", "Settings")
                )
            }

            // 1. Samsung One UI 8.5 Appearance, Contrast & Language Grouped Card
            item(key = "theme_card") {
                val themeState = LocalMengiThemeState.current
                val bodyContrast = ContrastAccessibility.contrastRatio(
                    MaterialTheme.colorScheme.onSurface,
                    MaterialTheme.colorScheme.surface
                )
                val mutedContrast = ContrastAccessibility.contrastRatio(
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    MaterialTheme.colorScheme.surfaceVariant
                )
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
                    HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))

                    // Android 12+ Material You Dynamic Color toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = tr(language, "Rangi za Mfumo (Dynamic Color)", "Material You Dynamic Color"),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (themeState.dynamicColorSupported) {
                                    tr(
                                        language,
                                        "Badilisha rangi kulingana na mandhari ya simu (Android 12+) ikiwa na ulinzi wa utofauti.",
                                        "Adapts colors to your Android 12+ wallpaper with automatic contrast protection."
                                    )
                                } else {
                                    tr(
                                        language,
                                        "Inahitaji Android 12+ — inatumia rangi salama za Mengidata.",
                                        "Requires Android 12+ — using accessible Mengidata palette."
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
                                text = tr(language, "Utofauti wa Juu (High Contrast)", "High Contrast Mode"),
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
                    HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.5f))
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

            // 1B. Security & Privacy Card (PIN Lock + Balance Mask)
            item(key = "security_privacy_card") {
                MengiCard {
                    OneUiSettingsSectionHeader(
                        icon = Icons.Rounded.Lock,
                        iconBg = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = tr(language, "Usalama & Faragha", "Security & Privacy"),
                        subtitle = tr(language, "Linda salio na funga app kwa PIN ya tarakimu 4.", "Hide balances in public and lock app with a 4-digit PIN."),
                        badge = {
                            StatusPill(
                                text = if (pinLockEnabled) tr(language, "PIN Imewashwa", "PIN Active")
                                else tr(language, "Wazi", "Unlocked"),
                                containerColor = if (pinLockEnabled) colors.incomeSoft else colors.subtleSurface,
                                contentColor = if (pinLockEnabled) colors.income else MaterialTheme.colorScheme.onSurfaceVariant
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
                                text = tr(language, "Ficha Salio (Privacy Mode)", "Hide Balances (Privacy Mode)"),
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
                    HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = tr(language, "Kufunga App kwa PIN", "4-Digit App PIN Lock"),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = tr(
                                    language,
                                    "Huhitaji PIN unapofungua Mengidata (usimbaji wa SHA-256).",
                                    "Requires your 4-digit PIN to open Mengidata (salted SHA-256)."
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
                                Text(tr(language, "Badili PIN", "Change PIN"), maxLines = 1)
                            }
                            FilledTonalButton(
                                onClick = onLockNow,
                                shape = CircleShape,
                                modifier = Modifier.weight(1f).testTag("lock_app_now_btn")
                            ) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(tr(language, "Funga Sasa", "Lock Now"), maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 2. App Update System Card
            item(key = "update_card") {
                MengiCard {
                    OneUiSettingsSectionHeader(
                        icon = Icons.Rounded.SystemUpdate,
                        iconBg = colors.savingsSoft,
                        iconTint = colors.savings,
                        title = tr(language, "Mfumo wa Updates", "App Update System"),
                        subtitle = "${tr(language, "Toleo la sasa", "Installed")}: v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        badge = {
                            StatusPill(
                                text = "SHA-256",
                                containerColor = colors.limeSoft,
                                contentColor = colors.limeInk
                            )
                        }
                    )

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = tr(
                            language,
                            "Hukagua GitHub Releases, kuthibitisha cheti cha saini (SHA-256) cha APK, na kusakinisha salama kwenye Android 12+.",
                            "Checks GitHub Releases, verifies APK SHA-256 signing certificate against this install, and installs cleanly on Android 12+."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(language, "Kagua updates kiotomatiki", "Auto-check for updates"),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = tr(language, "Kila baada ya saa 12 unapofungua app", "Every 12 hours on app launch"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoUpdateEnabled,
                            onCheckedChange = onSetAutoUpdate,
                            modifier = Modifier.testTag("auto_update_switch")
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    when (updateState) {
                        is UpdateUiState.Idle -> Unit

                        is UpdateUiState.Checking -> {
                            UpdateStatusBanner(
                                containerColor = colors.subtleSurface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text(tr(language, "Inakagua toleo jipya GitHub...", "Checking GitHub Releases..."), style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        is UpdateUiState.UpToDate -> {
                            UpdateStatusBanner(
                                containerColor = colors.incomeSoft,
                                contentColor = colors.income
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(
                                    tr(
                                        language,
                                        "Unatumia toleo la hivi karibuni (v${updateState.currentVersion}).",
                                        "You are on the latest version (v${updateState.currentVersion})."
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        is UpdateUiState.Available -> {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = colors.limeSoft,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = tr(
                                            language,
                                            "Toleo jipya v${updateState.info.version} linapatikana",
                                            "New version v${updateState.info.version} available"
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.limeInk
                                    )
                                    if (updateState.info.releaseNotes.isNotBlank()) {
                                        Text(
                                            text = updateState.info.releaseNotes.take(400),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.limeInk.copy(alpha = 0.9f)
                                        )
                                    }
                                    Button(
                                        onClick = { onDownloadUpdate(updateState.info) },
                                        shape = CircleShape,
                                        modifier = Modifier.testTag("download_update_btn")
                                    ) {
                                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.size(6.dp))
                                        Text(tr(language, "Pakua, Thibitisha & Sakinisha", "Download, Verify & Install"))
                                    }
                                }
                            }
                        }

                        is UpdateUiState.PermissionRequired -> {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = colors.warningSoft,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = tr(
                                            language,
                                            "Ruhusu 'Install unknown apps' kwa Mengidata ili kuendelea na usakinishaji wa v${updateState.info.version}.",
                                            "Allow 'Install unknown apps' for Mengidata to install v${updateState.info.version}."
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.warning
                                    )
                                    Button(
                                        onClick = onOpenInstallPermissionSettings,
                                        shape = CircleShape,
                                        modifier = Modifier.testTag("grant_install_permission_btn")
                                    ) {
                                        Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.size(6.dp))
                                        Text(tr(language, "Fungua Ruhusa ya Kusakinisha", "Open Install Permission"))
                                    }
                                }
                            }
                        }

                        is UpdateUiState.Downloading -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        tr(language, "Inapakua v${updateState.info.version}...", "Downloading v${updateState.info.version}..."),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text("${updateState.progressPercent}%", style = MaterialTheme.typography.labelMedium)
                                }
                                LinearProgressIndicator(
                                    progress = { updateState.progressPercent / 100f },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                                )
                            }
                        }

                        is UpdateUiState.Verifying -> {
                            UpdateStatusBanner(
                                containerColor = colors.subtleSurface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.income)
                                Text(
                                    tr(language, "Inathibitisha saini ya SHA-256 na toleo la APK...", "Verifying APK SHA-256 signature & version..."),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        is UpdateUiState.Installing -> {
                            UpdateStatusBanner(
                                containerColor = colors.incomeSoft,
                                contentColor = colors.income
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.income)
                                Text(
                                    tr(language, "APK imethibitishwa! Thibitisha dirisha la Android kusakinisha.", "APK verified! Confirm the Android install dialog."),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        is UpdateUiState.Success -> {
                            UpdateStatusBanner(
                                containerColor = colors.incomeSoft,
                                contentColor = colors.income
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(
                                    tr(language, "Toleo v${updateState.version} limesakinishwa!", "Version v${updateState.version} installed!"),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        is UpdateUiState.Error -> {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = colors.expenseSoft,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = colors.expense, modifier = Modifier.size(18.dp))
                                        Text(updateState.message, style = MaterialTheme.typography.bodySmall, color = colors.expense)
                                    }
                                    if (updateState.retryInfo != null) {
                                        OutlinedButton(onClick = { onDownloadUpdate(updateState.retryInfo) }, shape = CircleShape) {
                                            Text(tr(language, "Jaribu tena", "Retry"))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onCheckUpdates,
                            enabled = updateState !is UpdateUiState.Checking &&
                                updateState !is UpdateUiState.Downloading &&
                                updateState !is UpdateUiState.Verifying,
                            shape = CircleShape,
                            modifier = Modifier.weight(1f).height(46.dp).testTag("check_updates_btn")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(tr(language, "Kagua Update", "Check Now"), maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onOpenReleasesBrowser,
                            shape = CircleShape,
                            modifier = Modifier.height(46.dp).testTag("open_releases_web_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("GitHub")
                        }
                    }
                }
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
                            label = { Text(tr(language, "Maelezo mafupi (Tagline)", "Tagline")) },
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
                                label = { Text(tr(language, "Prefix ya Ankara", "Invoice Prefix")) },
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
                                        businessName = businessName.trim().ifBlank { "Mengidata" },
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

            // 4. Reports & Zero-Loss Backups
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
                                tr(
                                    language,
                                    "Pakua Ripoti yenye Rangi (.xls — Tab 4)",
                                    "Export Color-Coded Excel (.xls — 4 Tabs)"
                                ),
                                maxLines = 1
                            )
                        }

                        Text(
                            text = tr(
                                language,
                                "CSV Zilizotenganishwa (Miamala, Bili & Akiba Kila Kimoja Peke Yake):",
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
                                Text(tr(language, "Miamala (.csv)", "Txns (.csv)"), maxLines = 1)
                            }
                            FilledTonalButton(
                                onClick = onExportBillsCsv,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_bills_only_csv_btn")
                            ) {
                                Text(tr(language, "Bili Pekee (.csv)", "Bills (.csv)"), maxLines = 1)
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
                                Text(tr(language, "Akiba (.csv)", "Savings (.csv)"), maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = onExportMasterCsv,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_master_csv_btn")
                            ) {
                                Text(tr(language, "CSV Kamili", "Master CSV"), maxLines = 1)
                            }
                        }

                        HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                        FilledTonalButton(
                            onClick = onReconcileLedger,
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reconcile_ledger_btn")
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(tr(language, "Kagua & Sawazisha Hesabu (0% Upotevu)", "Verify & Reconcile Zero-Loss Ledger"))
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
                                Text(tr(language, "Rejesha Auto-Snapshot ya Usalama", "Restore Local Auto-Snapshot"))
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
                                Text(tr(language, "Backup .json", "Backup .json"), maxLines = 1)
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
                                Text(tr(language, "Rejesha .json", "Restore .json"), maxLines = 1)
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.5f))
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
                            Text(tr(language, "Data ya Mfano", "Sample Data"), maxLines = 1)
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
                            Text(tr(language, "Futa Data", "Clear Data"), maxLines = 1)
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

    if (showClearConfirm) {
        ConfirmActionDialog(
            title = tr(language, "Futa data zote?", "Clear all financial data?"),
            message = tr(language, "Miamala, ankara, bili na akiba vitafutwa kabisa. Pakua backup (.json) kwanza.", "All transactions, invoices, bills, and savings will be permanently cleared."),
            confirmLabel = tr(language, "Futa Yote", "Clear All"),
            cancelLabel = tr(language, "Ghairi", "Cancel"),
            isDanger = true,
            onConfirm = { onClearAllData(); showClearConfirm = false },
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
