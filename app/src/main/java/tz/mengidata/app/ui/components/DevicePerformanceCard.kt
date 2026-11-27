package tz.mengidata.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.system.DeviceHardwareSnapshot
import tz.mengidata.app.system.DevicePerformanceProfile
import tz.mengidata.app.system.DevicePerformanceTier
import tz.mengidata.app.system.OptimizationReport
import tz.mengidata.app.system.PerformanceModePreference
import tz.mengidata.app.ui.theme.LocalMengiColors
import java.util.Locale

/**
 * Matte Android 17 Settings card displaying real live device hardware telemetry
 * (FPS, RAM/Heap, Battery Temp/Thermal, Active Display Hz, SQLite Cache & Latency)
 * and actively tuning the Android Window, SQLite PRAGMAs, and Compose Motion Engine.
 */
@Composable
fun DevicePerformanceSettingsCard(
    profile: DevicePerformanceProfile,
    telemetry: DeviceHardwareSnapshot = profile.hardware,
    isOptimizing: Boolean = false,
    lastOptimizationReport: OptimizationReport? = null,
    language: AppLanguage,
    onSelectMode: (PerformanceModePreference) -> Unit,
    onOptimizeNow: () -> Unit,
    onRefreshTelemetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalMengiColors.current

    // Gently refresh real hardware telemetry every 2.5s ONLY while this card is visible
    LaunchedEffect(Unit) {
        onRefreshTelemetry()
        while (true) {
            delay(2_500L)
            onRefreshTelemetry()
        }
    }

    val tierBg: Color
    val tierFg: Color
    when (profile.resolvedTier) {
        DevicePerformanceTier.ECO -> {
            tierBg = colors.warningSoft
            tierFg = colors.warning
        }
        DevicePerformanceTier.BALANCED -> {
            tierBg = MaterialTheme.colorScheme.primaryContainer
            tierFg = MaterialTheme.colorScheme.onPrimaryContainer
        }
        DevicePerformanceTier.HIGH -> {
            tierBg = colors.incomeSoft
            tierFg = colors.income
        }
    }

    val activeBadgeText = if (profile.selectedMode == PerformanceModePreference.AUTO) {
        "Auto · ${profile.resolvedTier.shortLabel(language)}"
    } else {
        profile.resolvedTier.shortLabel(language)
    }

    val ramText = String.format(
        Locale.US,
        "%.1f / %.1f GB",
        telemetry.availableRamGb.coerceAtLeast(0.0),
        telemetry.totalRamGb.coerceAtLeast(0.5)
    )
    val appHeapText = tr(
        language,
        "Heap ya App: ${telemetry.appUsedHeapMb} / ${telemetry.appMaxHeapMb} MB",
        "App Heap: ${telemetry.appUsedHeapMb} / ${telemetry.appMaxHeapMb} MB"
    )
    val displayCpuText = if (telemetry.maxRefreshRateHz > telemetry.refreshRateHz) {
        "${telemetry.cpuCores}c · ${telemetry.refreshRateHz}/${telemetry.maxRefreshRateHz}Hz"
    } else {
        "${telemetry.cpuCores} cores · ${telemetry.refreshRateHz}Hz"
    }
    val dbLatencyText = if (telemetry.dbQueryLatencyMs > 0.0) {
        String.format(Locale.US, "%.1f ms · %dMB", telemetry.dbQueryLatencyMs, profile.sqliteCacheMb)
    } else {
        "SQLite ${profile.sqliteCacheMb}MB"
    }
    val batteryTempText = when {
        telemetry.isThermallyThrottled -> tr(language, "Joto juu · Eco", "Warm · Throttled")
        telemetry.isPowerSaveMode -> tr(language, "Betri Saver (${telemetry.batteryPercent}%)", "Battery Saver (${telemetry.batteryPercent}%)")
        telemetry.batteryTempC > 0f -> String.format(
            Locale.US,
            "%d%% · %.1f°C",
            telemetry.batteryPercent,
            telemetry.batteryTempC
        )
        else -> "${telemetry.batteryPercent}% · " + tr(language, "Hali Nzuri", "Optimal")
    }

    val liveFpsLabel = if (telemetry.liveFps > 0) {
        "${telemetry.liveFps} FPS"
    } else {
        "${telemetry.refreshRateHz} Hz"
    }

    val engineRulesSummary = buildString {
        val motionLabel = when (profile.resolvedTier) {
            DevicePerformanceTier.ECO -> tr(language, "Mwendo: Papo hapo (0ms)", "Motion: Instant (0ms)")
            DevicePerformanceTier.BALANCED -> tr(language, "Mwendo: Mwepesi (110ms)", "Motion: Crisp (110ms)")
            DevicePerformanceTier.HIGH -> tr(language, "Mwendo: Spring Kamili", "Motion: Full Spring")
        }
        val dbLabel = if (profile.sqliteMmapMb > 0) {
            "SQLite ${profile.sqliteCacheMb}MB + ${profile.sqliteMmapMb}MB MMAP"
        } else {
            "SQLite ${profile.sqliteCacheMb}MB"
        }
        append("${profile.targetRefreshRateHz}Hz · $dbLabel · $motionLabel · ${profile.downloadParallelStreams}× OTA")
    }

    MengiCard(modifier = modifier.testTag("settings_device_performance_card")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr(language, "Utendaji Kulingana na Simu", "Adaptive Device Engine"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tr(
                        language,
                        "Hudhibiti kasi ya kioo (Hz), hifadhidata (SQLite), na RAM moja kwa moja.",
                        "Actively tunes display Hz, SQLite cache, and motion for this phone."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(
                text = activeBadgeText,
                containerColor = tierBg,
                contentColor = tierFg
            )
        }

        Spacer(Modifier.height(12.dp))

        // Live Real-Time Telemetry Strip (FPS + Hardware Score + RAM Bar)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
                        PulsingStatusDot(
                            color = if (telemetry.droppedFramePercent <= 8) colors.income else colors.warning,
                            size = 7.dp
                        )
                        Text(
                            text = liveFpsLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "· ${telemetry.deviceDisplayName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = tierBg
                    ) {
                        Text(
                            text = "${telemetry.hardwareScore}/100",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = tierFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Live System RAM utilization bar + App Heap
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tr(language, "RAM Huru: $ramText", "Free RAM: $ramText"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = appHeapText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { telemetry.usedRamPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = when {
                            telemetry.usedRamPercent >= 0.85f -> colors.expense
                            telemetry.usedRamPercent >= 0.70f -> colors.warning
                            else -> colors.income
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Live hardware & engine spec grid (2x2 matte tiles)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeviceSpecTile(
                icon = Icons.Rounded.Tune,
                label = tr(language, "CPU & Kasi ya Kioo", "CPU & Display Hz"),
                value = displayCpuText,
                modifier = Modifier.weight(1f)
            )
            DeviceSpecTile(
                icon = Icons.Rounded.Storage,
                label = tr(language, "Kasi ya SQLite DB", "SQLite DB Speed"),
                value = dbLatencyText,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeviceSpecTile(
                icon = if (telemetry.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatterySaver,
                label = tr(language, "Betri & Joto", "Battery & Thermal"),
                value = batteryTempText,
                modifier = Modifier.weight(1f)
            )
            DeviceSpecTile(
                icon = Icons.Rounded.PhoneAndroid,
                label = tr(language, "Mfumo", "OS & ABI"),
                value = "Android ${telemetry.androidVersion} · ${telemetry.primaryAbi.substringBefore("-")}",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = tr(language, "Chagua Hali ya Utendaji", "Performance Profile"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))

        val modes = listOf(
            PerformanceModePreference.AUTO,
            PerformanceModePreference.ECO,
            PerformanceModePreference.BALANCED,
            PerformanceModePreference.HIGH
        )
        OneUiSegmentedTabs(
            options = modes.map { it.label(language) },
            selectedIndex = modes.indexOf(profile.selectedMode).coerceAtLeast(0),
            onSelect = { idx -> onSelectMode(modes[idx]) },
            testTags = modes.map { "perf_mode_${it.key}" }
        )

        Spacer(Modifier.height(10.dp))

        // Active enforced engine rules summary
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = profile.resolvedTier.label(language),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = engineRulesSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Last real optimization report banner (shown immediately after running Optimize Now)
        if (lastOptimizationReport != null) {
            Spacer(Modifier.height(8.dp))
            val freedKb = (lastOptimizationReport.freedCacheBytes / 1024L).coerceAtLeast(0L)
            val freedLabel = if (freedKb >= 1024L) {
                String.format(Locale.US, "%.1f MB", freedKb / 1024.0)
            } else {
                "$freedKb KB"
            }
            val latencyStr = String.format(Locale.US, "%.1f ms", lastOptimizationReport.dbLatencyMs)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.incomeSoft,
                border = BorderStroke(1.dp, colors.income.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = colors.income,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = tr(
                            language,
                            "Imeboreshwa: DB $latencyStr · WAL & Index OK · ${lastOptimizationReport.appliedRefreshHz}Hz · Cache $freedLabel" +
                                if (lastOptimizationReport.reclaimedHeapMb > 0) " · RAM +${lastOptimizationReport.reclaimedHeapMb}MB" else "",
                            "Tuned: DB $latencyStr · WAL & Index OK · ${lastOptimizationReport.appliedRefreshHz}Hz · Cache $freedLabel" +
                                if (lastOptimizationReport.reclaimedHeapMb > 0) " · RAM +${lastOptimizationReport.reclaimedHeapMb}MB" else ""
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.income
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        FilledTonalButton(
            onClick = onOptimizeNow,
            enabled = !isOptimizing,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("settings_optimize_system_btn")
        ) {
            if (isOptimizing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.CleaningServices,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isOptimizing) {
                    tr(language, "Inaboresha SQLite & RAM...", "Tuning SQLite & RAM...")
                } else {
                    tr(language, "Boresha Kasi na Safisha Mfumo Sasa", "Optimize Engine & Clean Cache Now")
                },
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DeviceSpecTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
