package tz.mengidata.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tz.mengidata.app.BuildConfig
import tz.mengidata.app.R
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.system.DevicePerformanceProfile
import tz.mengidata.app.ui.theme.LocalMengiColors
import tz.mengidata.app.updater.UpdateInfo
import tz.mengidata.app.updater.UpdateUiState
import java.util.Locale

/**
 * Clean, native One UI 8.5 NavigationDrawerItem placed directly above Settings in the Sidebar.
 * Tapping it navigates directly to the dedicated full-screen Software Update screen (`UpdateScreen`).
 */
@Composable
fun SidebarUpdateNavItem(
    language: AppLanguage,
    updateState: UpdateUiState,
    selected: Boolean,
    itemColors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMengiColors.current

    val isAvailable = updateState is UpdateUiState.Available
    val isDownloading = updateState is UpdateUiState.Downloading
    val isCheckingOrVerifying = updateState is UpdateUiState.Checking || updateState is UpdateUiState.Verifying
    val isPermissionOrError = updateState is UpdateUiState.PermissionRequired || updateState is UpdateUiState.Error

    val pillBg = when {
        isAvailable || isDownloading || updateState is UpdateUiState.UpToDate -> colors.incomeSoft
        isPermissionOrError -> colors.warningSoft
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val pillFg = when {
        isAvailable || isDownloading || updateState is UpdateUiState.UpToDate -> colors.income
        isPermissionOrError -> colors.warning
        else -> MaterialTheme.colorScheme.primary
    }

    val pillText = when (updateState) {
        is UpdateUiState.Available -> "v${updateState.info.version}"
        is UpdateUiState.Downloading -> "${updateState.progressPercent}%"
        is UpdateUiState.Verifying -> "SHA"
        is UpdateUiState.Installing -> "APK"
        is UpdateUiState.PermissionRequired -> "!"
        is UpdateUiState.Error -> "!"
        else -> "v${BuildConfig.VERSION_NAME}"
    }

    NavigationDrawerItem(
        label = {
            Text(
                text = tr(language, "Masasisho ya App", "Software Update"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected || isAvailable || isDownloading) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = when (updateState) {
                    is UpdateUiState.Available -> Icons.Rounded.CloudDownload
                    is UpdateUiState.Downloading -> Icons.Rounded.Bolt
                    else -> Icons.Rounded.SystemUpdate
                },
                contentDescription = tr(language, "Masasisho ya App", "Software Update")
            )
        },
        badge = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (isAvailable || isDownloading) {
                    PulsingStatusDot(
                        color = colors.income,
                        size = 6.dp
                    )
                }
                Surface(
                    color = pillBg,
                    shape = CircleShape
                ) {
                    if (isCheckingOrVerifying) {
                        Box(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = pillFg,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    } else {
                        Text(
                            text = pillText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = pillFg,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        },
        shape = CircleShape,
        colors = itemColors,
        modifier = modifier
            .padding(vertical = 2.dp)
            .testTag("drawer_update_item")
    )
}

/**
 * Clean, spacious Samsung One UI 8.5 / Google Pixel System Update full-screen experience (`UpdateScreen`).
 * Eliminates visual clutter in favor of refined Material 3 Expressive typography, generous whitespace,
 * a signature centered squircle app emblem, clear status hierarchy, and one prominent pill action.
 */
@Composable
fun UpdateScreen(
    language: AppLanguage,
    autoUpdateEnabled: Boolean,
    updateState: UpdateUiState,
    devicePerformance: DevicePerformanceProfile,
    onSetAutoUpdate: (Boolean) -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: (UpdateInfo) -> Unit,
    onCancelDownload: () -> Unit,
    onOpenInstallPermissionSettings: () -> Unit,
    onOpenReleasesBrowser: () -> Unit,
    onDirectInstallApk: (String) -> Unit,
    onUninstallOldApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMengiColors.current

    val targetVersion: String? = when (updateState) {
        is UpdateUiState.Available -> updateState.info.version
        is UpdateUiState.PermissionRequired -> updateState.info.version
        is UpdateUiState.Downloading -> updateState.info.version
        is UpdateUiState.Verifying -> updateState.info.version
        is UpdateUiState.Installing -> updateState.info.version
        is UpdateUiState.Success -> updateState.version
        else -> null
    }

    val activeInfo: UpdateInfo? = when (updateState) {
        is UpdateUiState.Available -> updateState.info
        is UpdateUiState.Downloading -> updateState.info
        is UpdateUiState.Verifying -> updateState.info
        is UpdateUiState.Installing -> updateState.info
        is UpdateUiState.PermissionRequired -> updateState.info
        else -> null
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .testTag("full_update_screen"),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 12.dp,
                bottom = 44.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Signature One UI 8.5 Centered System Update Hero Stage
            item {
                MengiCard(contentPadding = 24.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Status Pill
                        val isNewReady = updateState is UpdateUiState.Available || updateState is UpdateUiState.Downloading
                        val isErrorOrPerm = updateState is UpdateUiState.Error || updateState is UpdateUiState.PermissionRequired

                        Surface(
                            shape = CircleShape,
                            color = when {
                                isNewReady -> colors.incomeSoft
                                isErrorOrPerm -> colors.warningSoft
                                else -> MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isNewReady -> colors.income.copy(alpha = 0.35f)
                                    isErrorOrPerm -> colors.warning.copy(alpha = 0.35f)
                                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PulsingStatusDot(
                                    color = when {
                                        isNewReady -> colors.income
                                        isErrorOrPerm -> colors.warning
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    size = 7.dp
                                )
                                Text(
                                    text = when (updateState) {
                                        is UpdateUiState.Available -> tr(language, "TOLEO JIPYA LINAPATIKANA", "NEW UPDATE AVAILABLE")
                                        is UpdateUiState.Downloading -> tr(language, "INAPAKUA SASISHO", "DOWNLOADING UPDATE")
                                        is UpdateUiState.Checking -> tr(language, "INAKAGUA GITHUB...", "CHECKING FOR UPDATES...")
                                        is UpdateUiState.Verifying -> tr(language, "INATHIBITISHA SHA-256", "VERIFYING INTEGRITY")
                                        is UpdateUiState.Installing -> tr(language, "INASAKINISHA APK", "INSTALLING UPDATE")
                                        is UpdateUiState.PermissionRequired -> tr(language, "RUHUSA INAHITAJIKA", "PERMISSION REQUIRED")
                                        is UpdateUiState.Error -> tr(language, "ILANI YA SASISHO", "UPDATE NOTICE")
                                        else -> tr(language, "TOLEO RASMI LA GITHUB", "OFFICIAL RELEASE CHANNEL")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isNewReady -> colors.income
                                        isErrorOrPerm -> colors.warning
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(22.dp))

                        // Centered App Squircle Emblem with Floating Status Badge
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                shape = RoundedCornerShape(28.dp),
                                color = colors.heroStart,
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                                shadowElevation = if (colors.isDark) 0.dp else 6.dp,
                                modifier = Modifier.size(92.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.mengidata_oneui_icon_1795525888393),
                                    contentDescription = "VaultLedger",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = when {
                                    isNewReady -> colors.income
                                    isErrorOrPerm -> colors.warning
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.surface),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (updateState is UpdateUiState.Checking || updateState is UpdateUiState.Verifying) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.2.dp,
                                            color = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = when (updateState) {
                                                is UpdateUiState.Available -> Icons.Rounded.CloudDownload
                                                is UpdateUiState.Downloading -> Icons.Rounded.Bolt
                                                is UpdateUiState.Error -> Icons.Rounded.ErrorOutline
                                                is UpdateUiState.PermissionRequired -> Icons.Rounded.Security
                                                else -> Icons.Rounded.CheckCircle
                                            },
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // Bold Expressive Title
                        val mainHeadline = when (updateState) {
                            is UpdateUiState.Available -> tr(
                                language,
                                "VaultLedger v${updateState.info.version} Ipo Tayari",
                                "VaultLedger v${updateState.info.version} Is Ready"
                            )
                            is UpdateUiState.Downloading -> tr(
                                language,
                                "Inapakua v${updateState.info.version} (${updateState.progressPercent}%)",
                                "Downloading v${updateState.info.version} (${updateState.progressPercent}%)"
                            )
                            is UpdateUiState.Checking -> tr(
                                language,
                                "Inatafuta Toleo Jipya...",
                                "Checking for Updates..."
                            )
                            is UpdateUiState.Verifying -> tr(
                                language,
                                "Inathibitisha Usalama wa APK...",
                                "Verifying APK Signature..."
                            )
                            is UpdateUiState.Installing -> tr(
                                language,
                                "Kamilisha Usakinishaji",
                                "Complete Installation"
                            )
                            is UpdateUiState.PermissionRequired -> tr(
                                language,
                                "Ruhusu Usakinishaji wa App",
                                "Allow App Installation"
                            )
                            is UpdateUiState.Error -> tr(
                                language,
                                "Usakinishaji Haujakamilika",
                                "Update Interrupted"
                            )
                            is UpdateUiState.Success -> tr(
                                language,
                                "Imesakinishwa Kikamilifu!",
                                "Successfully Updated!"
                            )
                            else -> tr(
                                language,
                                "App Yako Iko Kwenye Toleo Jipya",
                                "Your App Is Up to Date"
                            )
                        }

                        Text(
                            text = mainHeadline,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(6.dp))

                        // Version Transition Pill Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = tr(
                                        language,
                                        "Toleo v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                        "Version v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            if (targetVersion != null && targetVersion != BuildConfig.VERSION_NAME) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = colors.income,
                                    modifier = Modifier.size(16.dp)
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = colors.incomeSoft
                                ) {
                                    Text(
                                        text = "v$targetVersion",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.income,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        val descriptionText = when (updateState) {
                            is UpdateUiState.Available -> {
                                val sizeStr = formatCompactSize(updateState.info.expectedSizeBytes)
                                tr(
                                    language,
                                    "Sasisho jipya ($sizeStr) linajumuisha maboresho ya kasi na ulinzi wa kumbukumbu zako.",
                                    "New release ($sizeStr) includes speed improvements with 100% zero-loss data preservation."
                                )
                            }
                            is UpdateUiState.Downloading -> {
                                val done = formatCompactSize(updateState.downloadedBytes)
                                val total = if (updateState.totalBytes > 0L) formatCompactSize(updateState.totalBytes) else "..."
                                "$done / $total · ${formatCompactSpeed(updateState.speedBytesPerSec)} · ETA ${formatCompactEta(updateState.etaSeconds)}"
                            }
                            is UpdateUiState.Installing -> tr(
                                language,
                                "Bonyeza 'Install / Update' kwenye dirisha la Android lililofunguka.",
                                "Tap 'Install / Update' on the Android system prompt to finish."
                            )
                            is UpdateUiState.PermissionRequired -> tr(
                                language,
                                "Washa ruhusa ya 'Install unknown apps' ili VaultLedger isakinishe sasisho moja kwa moja.",
                                "Enable 'Install unknown apps' so VaultLedger can install updates directly."
                            )
                            is UpdateUiState.Error -> updateState.message
                            else -> tr(
                                language,
                                "Miamala, ankara, bili na akiba zako zote zinahifadhiwa salama 100% wakati wa kila sasisho.",
                                "All your transactions, invoices, bills, and savings remain 100% safe across every update."
                            )
                        }

                        Text(
                            text = descriptionText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(Modifier.height(22.dp))

                        // Dynamic Action Area (Clean, Uncluttered)
                        AnimatedContent(
                            targetState = updateState,
                            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                            label = "clean_update_action"
                        ) { state ->
                            when (state) {
                                is UpdateUiState.Available -> {
                                    Button(
                                        onClick = { onDownloadUpdate(state.info) },
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.income,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .testTag("download_update_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.InstallMobile,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = tr(
                                                language,
                                                "Pakua na Sakinisha Sasa",
                                                "Download and Install Now"
                                            ),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                is UpdateUiState.Downloading -> {
                                    val animatedProgress by animateFloatAsState(
                                        targetValue = (state.progressPercent / 100f).coerceIn(0.02f, 1f),
                                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                                        label = "clean_dl_progress"
                                    )
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { animatedProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(CircleShape),
                                            color = colors.income,
                                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            strokeCap = StrokeCap.Round
                                        )

                                        // Clean 3-column live download metrics pill row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CleanMetricPill(
                                                label = tr(language, "Kasi", "Speed"),
                                                value = formatCompactSpeed(state.speedBytesPerSec),
                                                accent = colors.income,
                                                modifier = Modifier.weight(1f)
                                            )
                                            CleanMetricPill(
                                                label = tr(language, "Mkondo", "Turbo"),
                                                value = "${state.activeStreams}× NIO",
                                                accent = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            CleanMetricPill(
                                                label = tr(language, "Muda", "ETA"),
                                                value = formatCompactEta(state.etaSeconds),
                                                accent = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = onCancelDownload,
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("cancel_update_download_btn")
                                        ) {
                                            Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = tr(language, "Sitisha Upakuaji", "Cancel Download"),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                is UpdateUiState.Checking,
                                is UpdateUiState.Verifying -> {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(CircleShape),
                                        strokeCap = StrokeCap.Round
                                    )
                                }

                                is UpdateUiState.PermissionRequired -> {
                                    Button(
                                        onClick = onOpenInstallPermissionSettings,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                    ) {
                                        Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(19.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = tr(language, "Fungua Ruhusa ya Usakinishaji", "Allow Install Permission"),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                is UpdateUiState.Error -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (state.isSignatureConflict) {
                                            OutlinedButton(
                                                onClick = onUninstallOldApp,
                                                shape = CircleShape,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(50.dp)
                                            ) {
                                                Text(
                                                    text = tr(language, "Futa la Zamani", "Uninstall Old"),
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    val retry = state.retryInfo
                                                    if (retry != null) onDownloadUpdate(retry) else onCheckUpdates()
                                                },
                                                shape = CircleShape,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(50.dp)
                                            ) {
                                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = tr(language, "Jaribu Tena", "Try Again"),
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        if (!state.cachedApkPath.isNullOrBlank()) {
                                            FilledTonalButton(
                                                onClick = { onDirectInstallApk(state.cachedApkPath) },
                                                shape = CircleShape,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(50.dp)
                                            ) {
                                                Text(
                                                    text = tr(language, "Sakinisha APK", "Install APK"),
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                else -> {
                                    Button(
                                        onClick = onCheckUpdates,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .testTag("check_updates_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = tr(language, "Angalia Masasisho Sasa", "Check for Updates"),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. What's New / Release Notes Card (Only shown when release notes exist)
            if (activeInfo != null && activeInfo.releaseNotes.isNotBlank()) {
                item {
                    MengiCard(contentPadding = 20.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = colors.income,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = tr(
                                        language,
                                        "Maboresho Katika Toleo v${activeInfo.version}",
                                        "What's New in v${activeInfo.version}"
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (activeInfo.expectedSizeBytes > 0L) {
                                StatusPill(
                                    text = formatCompactSize(activeInfo.expectedSizeBytes),
                                    containerColor = colors.incomeSoft,
                                    contentColor = colors.income
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = activeInfo.releaseNotes.trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }

            // 3. Clean One UI 8.5 Grouped Update Settings Card
            item {
                MengiCard(contentPadding = 20.dp) {
                    // Row 1: Auto-Check Updates Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = tr(language, "Kagua Masasisho Kiotomatiki", "Auto-Check for Updates"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = tr(
                                    language,
                                    "Onyesha arifa kwenye Sidebar toleo jipya linapotoka.",
                                    "Show a badge in the Sidebar when a new version is released."
                                ),
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

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = colors.cardBorder)
                    Spacer(Modifier.height(14.dp))

                    // Row 2: High-Speed Engine Info
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
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.incomeSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Speed,
                                    contentDescription = null,
                                    tint = colors.income,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = tr(language, "Kasi ya Upakuaji (Hyper-Turbo)", "Hyper-Turbo Download Speed"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = tr(
                                        language,
                                        "Hadi 80 MB/s (${devicePerformance.downloadParallelStreams}× Mikondo Sambamba · SHA-256)",
                                        "Up to 80 MB/s (${devicePerformance.downloadParallelStreams}× Parallel Streams · SHA-256)"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusPill(
                            text = "80 MB/s",
                            containerColor = colors.incomeSoft,
                            contentColor = colors.income
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = colors.cardBorder)
                    Spacer(Modifier.height(14.dp))

                    // Row 3: Official GitHub Releases Page Link
                    Surface(
                        onClick = onOpenReleasesBrowser,
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = tr(
                                        language,
                                        "Fungua Ukurasa Rasmi wa GitHub Releases",
                                        "Open Official GitHub Releases Page"
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanMetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatCompactSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) String.format(Locale.US, "%.1f MB", mb)
    else String.format(Locale.US, "%.0f KB", bytes / 1024.0)
}

private fun formatCompactSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0L) return "0 MB/s"
    val mb = bytesPerSec / (1024.0 * 1024.0)
    return if (mb >= 1.0) String.format(Locale.US, "%.1f MB/s", mb)
    else String.format(Locale.US, "%.0f KB/s", bytesPerSec / 1024.0)
}

private fun formatCompactEta(seconds: Long): String {
    if (seconds <= 0L) return "--"
    val mins = seconds / 60L
    val rem = seconds % 60L
    return if (mins > 0L) "${mins}m ${rem}s" else "${rem}s"
}
