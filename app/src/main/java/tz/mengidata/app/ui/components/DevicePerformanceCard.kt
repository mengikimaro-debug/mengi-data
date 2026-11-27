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
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.tr
import tz.mengidata.app.system.DevicePerformanceProfile
import tz.mengidata.app.system.DevicePerformanceTier
import tz.mengidata.app.system.PerformanceModePreference
import tz.mengidata.app.ui.theme.LocalMengiColors
import java.util.Locale

/**
 * Matte Android 17 Settings card displaying real live device hardware telemetry
 * and allowing Auto or manual device-adaptive performance optimization for any phone.
 */
@Composable
fun DevicePerformanceSettingsCard(
    profile: DevicePerformanceProfile,
    language: AppLanguage,
    onSelectMode: (PerformanceModePreference) -> Unit,
    onOptimizeNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMengiColors.current
    val hw = profile.hardware

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
        hw.availableRamGb.coerceAtLeast(0.0),
        hw.totalRamGb.coerceAtLeast(0.5)
    )
    val cpuText = tr(
        language,
        "${hw.cpuCores} cores · ${hw.refreshRateHz}Hz",
        "${hw.cpuCores} cores · ${hw.refreshRateHz}Hz"
    )
    val powerStatusText = when {
        hw.isThermallyThrottled -> tr(language, "Joto juu · Eco", "Warm · Throttled")
        hw.isPowerSaveMode -> tr(language, "Betri Saver", "Battery Saver")
        hw.memoryPressureActive || hw.isSystemLowMemory -> tr(language, "RAM Chache", "Low Memory")
        hw.isLowRamDevice -> tr(language, "Simu Nyepesi", "Low-RAM Device")
        else -> tr(language, "Hali Nzuri", "Optimal")
    }

    val tuningSummary = buildString {
        val motionLabel = when (profile.resolvedTier) {
            DevicePerformanceTier.ECO -> tr(language, "Mwendo: Papo hapo", "Motion: Instant")
            DevicePerformanceTier.BALANCED -> tr(language, "Mwendo: Mwepesi", "Motion: Fast")
            DevicePerformanceTier.HIGH -> tr(language, "Mwendo: Kamili", "Motion: Full")
        }
        val streamsLabel = tr(
            language,
            "Upakuaji: Mikondo ${profile.downloadParallelStreams}",
            "Updates: ${profile.downloadParallelStreams} streams"
        )
        val bufferKb = profile.downloadBufferBytes / 1024
        append("$motionLabel · $streamsLabel · ${bufferKb}KB")
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
                        "Mfumo hujipanga kiotomatiki kulingana na RAM, CPU na betri ya simu yoyote.",
                        "Automatically tunes speed, motion, and memory for any phone's hardware."
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

        Spacer(Modifier.height(14.dp))

        // Live hardware spec grid (2x2 matte tiles)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeviceSpecTile(
                icon = Icons.Rounded.PhoneAndroid,
                label = tr(language, "Simu · Android ${hw.androidVersion}", "Device · Android ${hw.androidVersion}"),
                value = hw.deviceDisplayName,
                modifier = Modifier.weight(1f)
            )
            DeviceSpecTile(
                icon = Icons.Rounded.Memory,
                label = tr(language, "RAM Huru / Jumla", "Free / Total RAM"),
                value = ramText,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeviceSpecTile(
                icon = Icons.Rounded.Tune,
                label = tr(language, "Kichakataji & Kioo", "CPU & Display"),
                value = cpuText,
                modifier = Modifier.weight(1f)
            )
            DeviceSpecTile(
                icon = Icons.Rounded.BatterySaver,
                label = tr(language, "Hali ya Mfumo", "System Health"),
                value = powerStatusText,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = tr(language, "Hali ya Utendaji", "Performance Profile"),
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

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = profile.resolvedTier.label(language),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tuningSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        FilledTonalButton(
            onClick = onOptimizeNow,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("settings_optimize_system_btn")
        ) {
            Icon(
                imageVector = Icons.Rounded.CleaningServices,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = tr(language, "Boresha Kasi na Safisha Cache Sasa", "Optimize Speed & Trim Cache Now"),
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
