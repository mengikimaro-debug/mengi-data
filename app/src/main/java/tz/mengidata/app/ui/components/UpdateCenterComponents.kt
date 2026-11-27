package tz.mengidata.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tz.mengidata.app.BuildConfig
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.updater.UpdateInfo
import tz.mengidata.app.updater.UpdateUiState
import java.util.Locale

// Bespoke Obsidian-Emerald-Gold OTA Console Palette (high contrast in both Light & Dark themes)
private val VaultObsidian = Color(0xFF0C1520)
private val VaultSlate = Color(0xFF142334)
private val VaultEmeraldDeep = Color(0xFF10312A)
private val VaultPanel = Color(0xFF172636)
private val VaultPanelBorder = Color(0xFF283C50)
private val VaultTextPrimary = Color(0xFFF1F5F9)
private val VaultTextMuted = Color(0xFFA7B7C8)
private val TurboMint = Color(0xFF34D399)
private val TurboMintSoft = Color(0xFF12362C)
private val ChampagneGold = Color(0xFFD9B675)
private val ChampagneGoldSoft = Color(0xFF2E2618)
private val AlertCoral = Color(0xFFF87171)
private val AlertCoralSoft = Color(0xFF351B20)

/**
 * Flagship bespoke OTA Update Console for SettingsScreen.
 * Features an animated Canvas orbital telemetry dial, 3-stage pipeline stepper,
 * live 4x Turbo parallel download speedometer (MB/s, ETA, streams), and collapsible release highlights.
 */
@Composable
fun UpdateCenterCard(
    language: AppLanguage,
    autoUpdateEnabled: Boolean,
    updateState: UpdateUiState,
    onSetAutoUpdate: (Boolean) -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: (UpdateInfo) -> Unit,
    onCancelDownload: () -> Unit,
    onOpenInstallPermissionSettings: () -> Unit,
    onOpenReleasesBrowser: () -> Unit,
    onDirectInstallApk: (String) -> Unit = {},
    onUninstallOldApp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showWhatsNew by remember { mutableStateOf(false) }

    val targetVersion: String? = when (updateState) {
        is UpdateUiState.Available -> updateState.info.version
        is UpdateUiState.PermissionRequired -> updateState.info.version
        is UpdateUiState.Downloading -> updateState.info.version
        is UpdateUiState.Verifying -> updateState.info.version
        is UpdateUiState.Installing -> updateState.info.version
        is UpdateUiState.Success -> updateState.version
        else -> null
    }
    val expectedSizeBytes: Long = when (updateState) {
        is UpdateUiState.Available -> updateState.info.expectedSizeBytes
        is UpdateUiState.Downloading -> updateState.totalBytes
        is UpdateUiState.PermissionRequired -> updateState.info.expectedSizeBytes
        is UpdateUiState.Verifying -> updateState.info.expectedSizeBytes
        is UpdateUiState.Installing -> updateState.info.expectedSizeBytes
        else -> 0L
    }

    val isBusy = updateState is UpdateUiState.Checking ||
        updateState is UpdateUiState.Downloading ||
        updateState is UpdateUiState.Verifying

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = VaultObsidian,
        border = BorderStroke(1.2.dp, VaultPanelBorder),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("update_center_console")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(VaultObsidian, VaultSlate, VaultEmeraldDeep),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 900f)
                    )
                )
        ) {
            // Subtle architectural backdrop arcs in top-right corner
            Canvas(modifier = Modifier.matchParentSize()) {
                val center = Offset(size.width * 0.92f, size.height * 0.14f)
                drawCircle(
                    color = TurboMint.copy(alpha = 0.055f),
                    radius = size.minDimension * 0.42f,
                    center = center
                )
                drawCircle(
                    color = ChampagneGold.copy(alpha = 0.09f),
                    radius = size.minDimension * 0.56f,
                    center = center,
                    style = Stroke(width = 1.2f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Top Telemetry Hero Row: Orbital Dial + Version Flight Path
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OrbitalUpdateDial(updateState = updateState)

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ConsoleTagPill(
                                text = "4× TURBO",
                                icon = Icons.Rounded.Bolt,
                                bg = ChampagneGoldSoft,
                                fg = ChampagneGold,
                                border = ChampagneGold.copy(alpha = 0.45f)
                            )
                            ConsoleTagPill(
                                text = "SHA-256",
                                icon = Icons.Rounded.VerifiedUser,
                                bg = TurboMintSoft,
                                fg = TurboMint,
                                border = TurboMint.copy(alpha = 0.4f)
                            )
                        }

                        Text(
                            text = tr(language, "Kituo cha Masasisho", "Mengi OTA Console"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = VaultTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Version flight-path row: v2.8.2 -> v2.9.0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = VaultTextMuted
                            )
                            if (targetVersion != null && targetVersion != BuildConfig.VERSION_NAME) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = ChampagneGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = TurboMintSoft,
                                    border = BorderStroke(1.dp, TurboMint.copy(alpha = 0.55f))
                                ) {
                                    Text(
                                        text = "v$targetVersion",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TurboMint,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "· Build ${BuildConfig.VERSION_CODE}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VaultTextMuted.copy(alpha = 0.8f)
                                )
                            }
                            if (expectedSizeBytes > 0L) {
                                Text(
                                    text = "· ${formatFileSize(expectedSizeBytes)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ChampagneGold
                                )
                            }
                        }
                    }
                }

                // 2. 3-Step Visual Pipeline Rail (visible whenever an update flow is active)
                val activePipelineStep = when (updateState) {
                    is UpdateUiState.Downloading -> 1
                    is UpdateUiState.Verifying -> 2
                    is UpdateUiState.Installing -> 3
                    is UpdateUiState.Success -> 4
                    else -> 0
                }
                AnimatedVisibility(
                    visible = activePipelineStep > 0,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    UpdatePipelineStepper(
                        activeStep = activePipelineStep,
                        language = language
                    )
                }

                // 3. Dynamic State Stage
                AnimatedContent(
                    targetState = updateState,
                    transitionSpec = {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                    },
                    label = "update_stage_content"
                ) { state ->
                    when (state) {
                        is UpdateUiState.Idle -> {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = VaultPanel.copy(alpha = 0.82f),
                                border = BorderStroke(1.dp, VaultPanelBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Speed,
                                        contentDescription = null,
                                        tint = ChampagneGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = tr(
                                            language,
                                            "Upakuaji wa kasi 4× (Multi-Stream) + uthibitisho wa cheti SHA-256 moja kwa moja kutoka GitHub.",
                                            "4× Multi-Stream Turbo downloader with automatic SHA-256 verification directly from GitHub."
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = VaultTextMuted
                                    )
                                }
                            }
                        }

                        is UpdateUiState.Checking -> {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = VaultPanel,
                                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = ChampagneGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = tr(language, "Inatafuta toleo jipya GitHub...", "Scanning GitHub Releases..."),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = VaultTextPrimary
                                        )
                                        Text(
                                            text = tr(language, "Inalinganisha v${BuildConfig.VERSION_NAME} na seva salama ya HTTPS", "Comparing v${BuildConfig.VERSION_NAME} over strict HTTPS"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VaultTextMuted
                                        )
                                    }
                                }
                            }
                        }

                        is UpdateUiState.UpToDate -> {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = TurboMintSoft,
                                border = BorderStroke(1.dp, TurboMint.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = TurboMint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = tr(
                                            language,
                                            "Unatumia toleo la hivi karibuni kabisa (v${state.currentVersion}).",
                                            "You are running the very latest version (v${state.currentVersion})."
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TurboMint
                                    )
                                }
                            }
                        }

                        is UpdateUiState.Available -> {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = VaultPanel,
                                border = BorderStroke(1.2.dp, TurboMint.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tr(
                                                language,
                                                "Toleo Jipya v${state.info.version} Tayari",
                                                "Version v${state.info.version} Ready"
                                            ),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TurboMint
                                        )
                                        ConsoleTagPill(
                                            text = formatFileSize(state.info.expectedSizeBytes),
                                            icon = null,
                                            bg = ChampagneGoldSoft,
                                            fg = ChampagneGold,
                                            border = ChampagneGold.copy(alpha = 0.4f)
                                        )
                                    }

                                    if (state.info.releaseNotes.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = VaultObsidian.copy(alpha = 0.72f),
                                            border = BorderStroke(1.dp, VaultPanelBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = state.info.releaseNotes.take(450),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = VaultTextMuted,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { onDownloadUpdate(state.info) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = TurboMint,
                                            contentColor = VaultObsidian
                                        ),
                                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 50.dp)
                                            .testTag("download_update_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Bolt,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = tr(
                                                language,
                                                "Pakua Haraka (4× Turbo) & Sakinisha",
                                                "Turbo Download (4×) & Install"
                                            ),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        is UpdateUiState.PermissionRequired -> {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = ChampagneGoldSoft,
                                border = BorderStroke(1.2.dp, ChampagneGold.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Security,
                                            contentDescription = null,
                                            tint = ChampagneGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = tr(
                                                language,
                                                "Ruhusu usakinishaji wa programu kwa VaultLedger ili kuendelea na v${state.info.version}.",
                                                "Allow 'Install unknown apps' for VaultLedger to install v${state.info.version}."
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VaultTextPrimary
                                        )
                                    }
                                    Button(
                                        onClick = onOpenInstallPermissionSettings,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ChampagneGold,
                                            contentColor = VaultObsidian
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("grant_install_permission_btn")
                                    ) {
                                        Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = tr(language, "Fungua Ruhusa ya Kusakinisha", "Open Install Permission"),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        is UpdateUiState.Downloading -> {
                            TurboDownloadTelemetryPanel(
                                state = state,
                                language = language,
                                onCancelDownload = onCancelDownload
                            )
                        }

                        is UpdateUiState.Verifying -> {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = VaultPanel,
                                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        tint = ChampagneGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = tr(language, "Inathibitisha Usalama wa APK...", "Verifying APK Integrity..."),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = VaultTextPrimary
                                        )
                                        Text(
                                            text = tr(language, "Inakagua cheti cha saini SHA-256 na ukubwa halisi.", "Checking SHA-256 certificate & exact byte size."),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VaultTextMuted
                                        )
                                    }
                                }
                            }
                        }

                        is UpdateUiState.Installing -> {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = TurboMintSoft,
                                border = BorderStroke(1.dp, TurboMint.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.InstallMobile,
                                            contentDescription = null,
                                            tint = TurboMint,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column {
                                            Text(
                                                text = tr(language, "APK Imethibitishwa — Inasakinisha!", "APK Verified — Installing!"),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = TurboMint
                                            )
                                            Text(
                                                text = tr(
                                                    language,
                                                    "Thibitisha dirisha la Android kumaliza. Kama halijatokea, bonyeza kitufe hapa chini.",
                                                    "Confirm the Android prompt to finish. If it didn't appear, tap below."
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = VaultTextPrimary
                                            )
                                        }
                                    }
                                    if (!state.apkPath.isNullOrBlank()) {
                                        Button(
                                            onClick = { onDirectInstallApk(state.apkPath) },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = TurboMint,
                                                contentColor = VaultObsidian
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 44.dp)
                                                .testTag("direct_install_apk_btn")
                                        ) {
                                            Icon(Icons.Rounded.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = tr(language, "Fungua Kisakinishi Moja kwa Moja", "Open Direct APK Installer"),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is UpdateUiState.Success -> {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = TurboMintSoft,
                                border = BorderStroke(1.2.dp, TurboMint),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = TurboMint, modifier = Modifier.size(22.dp))
                                    Text(
                                        text = tr(language, "Toleo v${state.version} limesakinishwa kikamilifu!", "Version v${state.version} installed successfully!"),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TurboMint
                                    )
                                }
                            }
                        }

                        is UpdateUiState.Error -> {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = AlertCoralSoft,
                                border = BorderStroke(1.dp, AlertCoral.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = AlertCoral, modifier = Modifier.size(20.dp))
                                        Text(state.message, style = MaterialTheme.typography.bodySmall, color = VaultTextPrimary)
                                    }
                                    if (state.isSignatureConflict) {
                                        Button(
                                            onClick = onUninstallOldApp,
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ChampagneGold,
                                                contentColor = VaultObsidian
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 46.dp)
                                                .testTag("uninstall_old_app_btn")
                                        ) {
                                            Icon(Icons.Rounded.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = tr(language, "Ondoa Toleo la Zamani (Data Zipo Salama)", "Uninstall Old Version (Data Safe)"),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else if (!state.cachedApkPath.isNullOrBlank()) {
                                        Button(
                                            onClick = { onDirectInstallApk(state.cachedApkPath) },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = TurboMint,
                                                contentColor = VaultObsidian
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 46.dp)
                                                .testTag("install_cached_apk_btn")
                                        ) {
                                            Icon(Icons.Rounded.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = tr(language, "Sakinisha APK Iliyopakuliwa", "Install Downloaded APK"),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (state.retryInfo != null) {
                                            Button(
                                                onClick = { onDownloadUpdate(state.retryInfo) },
                                                shape = CircleShape,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AlertCoral,
                                                    contentColor = VaultObsidian
                                                ),
                                                modifier = Modifier.heightIn(min = 44.dp)
                                            ) {
                                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(tr(language, "Jaribu Tena", "Retry"), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = onOpenReleasesBrowser,
                                            shape = CircleShape,
                                            border = BorderStroke(1.dp, VaultPanelBorder),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultTextPrimary),
                                            modifier = Modifier.heightIn(min = 44.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(tr(language, "GitHub APK", "GitHub APK"))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Collapsible "What's New in Current Version" Drawer
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = VaultPanel.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, VaultPanelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showWhatsNew = !showWhatsNew }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                    tint = ChampagneGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tr(
                                        language,
                                        "Mpya katika Toleo v${BuildConfig.VERSION_NAME}",
                                        "What's New in v${BuildConfig.VERSION_NAME}"
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = VaultTextPrimary
                                )
                            }
                            Icon(
                                imageVector = if (showWhatsNew) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = tr(language, "Fungua maelezo", "Toggle release notes"),
                                tint = VaultTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        AnimatedVisibility(
                            visible = showWhatsNew,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Text(
                                text = tr(
                                    language,
                                    "• Orodha mpya ya Malipo Yanayosubiri yenye mtindo wa mazungumzo + dirisha dogo la maelezo (Pop View)\n• Chaguo la Sarafu za Kimataifa (TSh, $, €, £, د.إ, ¥) kwenye slide bar na ubadilishaji wa pesa moja kwa moja kwa viwango vya soko\n• Kurasa ya Kuhusu App (About App) yenye maelezo kamili na mawasiliano ya mwanzilishi\n• Mfumo thabiti wa 4× Turbo OTA na utendaji wa kasi zaidi bila kukwama",
                                    "• New conversation-style Pending Payments list in Ledger with sleek compact detail Pop View\n• International currency selector (TSh, $, €, £, د.إ, ¥) in slide bar with live automatic market conversion\n• Added About App guide with complete workflow overview and founder contact\n• Reliable 4× Turbo OTA updates and smoother zero-jank performance"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = VaultTextMuted,
                                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                            )
                        }
                    }
                }

                // 5. Auto-Check Toggle Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(VaultPanel.copy(alpha = 0.55f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(language, "Kagua masasisho kiotomatiki", "Auto-check for updates"),
                            style = MaterialTheme.typography.titleSmall,
                            color = VaultTextPrimary
                        )
                        Text(
                            text = tr(language, "Kila saa 12 unapofungua VaultLedger", "Every 12 hours on app launch"),
                            style = MaterialTheme.typography.bodySmall,
                            color = VaultTextMuted
                        )
                    }
                    Switch(
                        checked = autoUpdateEnabled,
                        onCheckedChange = onSetAutoUpdate,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VaultObsidian,
                            checkedTrackColor = TurboMint,
                            uncheckedThumbColor = VaultTextMuted,
                            uncheckedTrackColor = VaultObsidian,
                            uncheckedBorderColor = VaultPanelBorder
                        ),
                        modifier = Modifier.testTag("auto_update_switch")
                    )
                }

                // 6. Action Footer Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCheckUpdates,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChampagneGold,
                            contentColor = VaultObsidian,
                            disabledContainerColor = VaultPanel,
                            disabledContentColor = VaultTextMuted
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("check_updates_btn")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = tr(language, "Kagua Sasisho", "Check Updates"),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenReleasesBrowser,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, VaultPanelBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultTextPrimary),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("open_releases_web_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("GitHub", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Sleek dark-slate-to-emerald Floating Upgrade Capsule shown at the top of DashboardScreen
 * whenever a new version is available or actively downloading/verifying/installing.
 */
@Composable
fun DashboardUpdateCapsule(
    updateState: UpdateUiState,
    language: AppLanguage,
    onOpenSettings: () -> Unit,
    onDownloadUpdate: (UpdateInfo) -> Unit,
    onCancelDownload: () -> Unit,
    onOpenInstallPermissionSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = updateState is UpdateUiState.Available ||
        updateState is UpdateUiState.Downloading ||
        updateState is UpdateUiState.Verifying ||
        updateState is UpdateUiState.Installing ||
        updateState is UpdateUiState.PermissionRequired

    if (!isVisible) return

    Surface(
        onClick = onOpenSettings,
        shape = RoundedCornerShape(24.dp),
        color = VaultObsidian,
        border = BorderStroke(1.2.dp, TurboMint.copy(alpha = 0.5f)),
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_update_banner")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(VaultObsidian, VaultSlate, VaultEmeraldDeep)
                    )
                )
                .padding(16.dp)
        ) {
            when (updateState) {
                is UpdateUiState.Available -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(TurboMintSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SystemUpdate,
                                    contentDescription = tr(language, "Sasisho linapatikana", "Update available"),
                                    tint = TurboMint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = tr(
                                            language,
                                            "Toleo v${updateState.info.version} Tayari",
                                            "Version v${updateState.info.version} Ready"
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = VaultTextPrimary
                                    )
                                    ConsoleTagPill(
                                        text = "4× TURBO",
                                        icon = Icons.Rounded.Bolt,
                                        bg = ChampagneGoldSoft,
                                        fg = ChampagneGold,
                                        border = ChampagneGold.copy(alpha = 0.45f)
                                    )
                                }
                                Text(
                                    text = "${formatFileSize(updateState.info.expectedSizeBytes)} · " + tr(
                                        language,
                                        "Pakua haraka na usakinishe kwa usalama",
                                        "Fast multi-stream download & verified install"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VaultTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onDownloadUpdate(updateState.info) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TurboMint,
                                    contentColor = VaultObsidian
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("dashboard_quick_download_btn")
                            ) {
                                Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = tr(language, "Pakua Haraka", "Fast Download"),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = onOpenSettings,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, VaultPanelBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultTextPrimary),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text(tr(language, "Maelezo", "Details"))
                            }
                        }
                    }
                }

                is UpdateUiState.Downloading -> {
                    TurboDownloadTelemetryPanel(
                        state = updateState,
                        language = language,
                        onCancelDownload = onCancelDownload,
                        compact = true
                    )
                }

                is UpdateUiState.Verifying -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(22.dp))
                        Text(
                            text = tr(language, "Inathibitisha saini SHA-256 ya v${updateState.info.version}...", "Verifying v${updateState.info.version} SHA-256 signature..."),
                            style = MaterialTheme.typography.labelLarge,
                            color = VaultTextPrimary
                        )
                    }
                }

                is UpdateUiState.Installing -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.InstallMobile, contentDescription = null, tint = TurboMint, modifier = Modifier.size(22.dp))
                        Text(
                            text = tr(language, "Thibitisha dirisha la Android kusakinisha v${updateState.info.version}.", "Confirm Android dialog to install v${updateState.info.version}."),
                            style = MaterialTheme.typography.labelLarge,
                            color = TurboMint
                        )
                    }
                }

                is UpdateUiState.PermissionRequired -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(22.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(language, "Ruhusa ya kusakinisha inahitajika", "Install permission required"),
                                style = MaterialTheme.typography.titleSmall,
                                color = ChampagneGold
                            )
                            Text(
                                text = tr(language, "Gusa kuruhusu VaultLedger kusakinisha v${updateState.info.version}", "Tap to allow VaultLedger to install v${updateState.info.version}"),
                                style = MaterialTheme.typography.bodySmall,
                                color = VaultTextMuted
                            )
                        }
                        Button(
                            onClick = onOpenInstallPermissionSettings,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChampagneGold,
                                contentColor = VaultObsidian
                            )
                        ) {
                            Text(tr(language, "Ruhusu", "Allow"), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun OrbitalUpdateDial(updateState: UpdateUiState) {
    val isSpinning = updateState is UpdateUiState.Checking ||
        updateState is UpdateUiState.Verifying ||
        updateState is UpdateUiState.Installing
    val isPulsing = isSpinning || updateState is UpdateUiState.Available

    val spinAngleState = if (isSpinning) {
        rememberInfiniteTransition(label = "orbital_spin_tx").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
            label = "orbital_spin"
        )
    } else null

    val pulseState = if (isPulsing) {
        rememberInfiniteTransition(label = "orbital_pulse_tx").animateFloat(
            initialValue = 0.68f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "orbital_pulse"
        )
    } else null

    val downloadFraction = if (updateState is UpdateUiState.Downloading) {
        (updateState.progressPercent / 100f).coerceIn(0f, 1f)
    } else 0f
    val animatedSweep = animateFloatAsState(
        targetValue = downloadFraction * 360f,
        animationSpec = spring(stiffness = 220f),
        label = "dial_sweep"
    )

    val ringColor = when (updateState) {
        is UpdateUiState.Error -> AlertCoral
        is UpdateUiState.PermissionRequired, is UpdateUiState.Checking, is UpdateUiState.Verifying -> ChampagneGold
        else -> TurboMint
    }

    Box(
        modifier = Modifier.size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 4.dp.toPx()
            val inset = strokeW / 2f + 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)
            val pulseVal = pulseState?.value ?: 0.85f

            // Inner ambient glow
            drawCircle(
                color = ringColor.copy(alpha = 0.14f * pulseVal),
                radius = size.minDimension * 0.42f
            )

            // Outer precision dashed orbit ring
            drawArc(
                color = VaultPanelBorder,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
            )

            when (updateState) {
                is UpdateUiState.Downloading -> {
                    drawArc(
                        brush = Brush.sweepGradient(listOf(TurboMint, ChampagneGold, TurboMint)),
                        startAngle = -90f,
                        sweepAngle = animatedSweep.value.coerceAtLeast(10f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                is UpdateUiState.Checking, is UpdateUiState.Verifying, is UpdateUiState.Installing -> {
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Color.Transparent, ringColor)),
                        startAngle = spinAngleState?.value ?: 0f,
                        sweepAngle = 125f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                else -> {
                    drawArc(
                        color = ringColor.copy(alpha = 0.78f * pulseVal),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = 2.4.dp.toPx())
                    )
                }
            }
        }

        // Center Icon or Live Percentage Readout
        if (updateState is UpdateUiState.Downloading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${updateState.progressPercent}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = VaultTextPrimary,
                    fontSize = 15.sp
                )
                Text(
                    text = "${updateState.activeStreams}×",
                    style = MaterialTheme.typography.labelSmall,
                    color = ChampagneGold,
                    fontSize = 9.5.sp
                )
            }
        } else {
            val icon = when (updateState) {
                is UpdateUiState.UpToDate, is UpdateUiState.Success -> Icons.Rounded.CheckCircle
                is UpdateUiState.Verifying -> Icons.Rounded.VerifiedUser
                is UpdateUiState.Installing -> Icons.Rounded.InstallMobile
                is UpdateUiState.PermissionRequired -> Icons.Rounded.Security
                is UpdateUiState.Error -> Icons.Rounded.ErrorOutline
                is UpdateUiState.Available -> Icons.Rounded.Bolt
                else -> Icons.Rounded.SystemUpdate
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ringColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun TurboDownloadTelemetryPanel(
    state: UpdateUiState.Downloading,
    language: AppLanguage,
    onCancelDownload: () -> Unit,
    compact: Boolean = false
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (state.progressPercent / 100f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 180f),
        label = "turbo_bar_progress"
    )
    val shimmerTransition = rememberInfiniteTransition(label = "turbo_shimmer")
    val shimmerPhase by shimmerTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmer_phase"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (compact) Color.Transparent else VaultPanel,
        border = if (compact) null else BorderStroke(1.dp, TurboMint.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 0.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = ChampagneGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = tr(
                            language,
                            "Inapakua v${state.info.version} (${state.activeStreams}× Turbo)",
                            "Turbo Downloading v${state.info.version} (${state.activeStreams}×)"
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextPrimary
                    )
                }
                Text(
                    text = "${state.progressPercent}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TurboMint
                )
            }

            // Custom Glowing Shimmer Progress Bar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
            ) {
                drawRect(color = VaultObsidian)
                val fillW = (size.width * animatedProgress).coerceAtLeast(10f)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(TurboMint, ChampagneGold),
                        startX = 0f,
                        endX = fillW
                    ),
                    size = Size(fillW, size.height)
                )
                // Specular moving light pulse across filled bar
                val shimmerCenter = fillW * shimmerPhase
                val shimmerW = (fillW * 0.28f).coerceAtLeast(24f)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
                        startX = shimmerCenter - shimmerW / 2f,
                        endX = shimmerCenter + shimmerW / 2f
                    ),
                    size = Size(fillW, size.height)
                )
            }

            // Live Speed / ETA / Bytes Telemetry Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryMetricChip(
                    icon = Icons.Rounded.Speed,
                    label = formatSpeed(state.speedBytesPerSec),
                    tint = TurboMint,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricChip(
                    icon = Icons.Rounded.Timer,
                    label = formatEta(state.etaSeconds, language),
                    tint = ChampagneGold,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricChip(
                    icon = Icons.Rounded.Bolt,
                    label = "${formatFileSize(state.downloadedBytes)} / ${formatFileSize(state.totalBytes)}",
                    tint = VaultTextPrimary,
                    modifier = Modifier.weight(1.35f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onCancelDownload,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, AlertCoral.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertCoral),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .testTag("cancel_update_download_btn")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = tr(language, "Ghairi Upakuaji", "Cancel Download"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdatePipelineStepper(
    activeStep: Int,
    language: AppLanguage
) {
    val steps = listOf(
        1 to tr(language, "1. Turbo 4×", "1. Turbo 4×"),
        2 to tr(language, "2. SHA-256", "2. SHA-256"),
        3 to tr(language, "3. Sakinisha", "3. Install")
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        steps.forEach { (stepNum, label) ->
            val completed = activeStep > stepNum
            val current = activeStep == stepNum
            val bg = when {
                completed -> TurboMintSoft
                current -> ChampagneGoldSoft
                else -> VaultPanel.copy(alpha = 0.55f)
            }
            val fg = when {
                completed -> TurboMint
                current -> ChampagneGold
                else -> VaultTextMuted
            }
            val border = when {
                completed -> TurboMint.copy(alpha = 0.5f)
                current -> ChampagneGold.copy(alpha = 0.65f)
                else -> VaultPanelBorder
            }
            Surface(
                shape = CircleShape,
                color = bg,
                border = BorderStroke(1.dp, border),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (completed) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = fg,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryMetricChip(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = VaultObsidian.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, VaultPanelBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ConsoleTagPill(
    text: String,
    icon: ImageVector?,
    bg: Color,
    fg: Color,
    border: Color
) {
    Surface(
        shape = CircleShape,
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(11.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = fg,
                letterSpacing = 0.5.sp
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        val kb = bytes / 1024.0
        String.format(Locale.US, "%.0f KB", kb)
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0L) return "..."
    val mbps = bytesPerSec / (1024.0 * 1024.0)
    return if (mbps >= 1.0) {
        String.format(Locale.US, "%.1f MB/s", mbps)
    } else {
        val kbps = bytesPerSec / 1024.0
        String.format(Locale.US, "%.0f KB/s", kbps)
    }
}

private fun formatEta(seconds: Long, language: AppLanguage): String {
    if (seconds < 0L) return "--"
    if (seconds < 60L) {
        return tr(language, "sek ${seconds}", "${seconds}s")
    }
    val mins = seconds / 60L
    val remSec = seconds % 60L
    return tr(language, "${mins}dk ${remSec}s", "${mins}m ${remSec}s")
}
