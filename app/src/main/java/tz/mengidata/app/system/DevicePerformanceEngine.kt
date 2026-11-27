package tz.mengidata.app.system

import android.app.ActivityManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.PowerManager
import android.view.Display
import androidx.compose.runtime.staticCompositionLocalOf
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.tr
import java.io.File
import java.util.Locale

enum class PerformanceModePreference(val key: String) {
    AUTO("auto"),
    ECO("eco"),
    BALANCED("balanced"),
    HIGH("high");

    fun label(language: AppLanguage): String = when (this) {
        AUTO -> tr(language, "Auto", "Auto")
        ECO -> tr(language, "Eco", "Eco")
        BALANCED -> tr(language, "Wastani", "Balanced")
        HIGH -> tr(language, "Kasi", "High")
    }

    companion object {
        fun fromKey(key: String?): PerformanceModePreference =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: AUTO
    }
}

enum class DevicePerformanceTier {
    ECO,
    BALANCED,
    HIGH;

    fun label(language: AppLanguage): String = when (this) {
        ECO -> tr(language, "Eco (Simu ya Kawaida / Betri)", "Eco (Light / Battery Saver)")
        BALANCED -> tr(language, "Wastani (Haraka na Salama)", "Balanced (Smooth & Efficient)")
        HIGH -> tr(language, "Kasi ya Juu (Full Expressive)", "High Performance (Full Motion)")
    }

    fun shortLabel(language: AppLanguage): String = when (this) {
        ECO -> tr(language, "Eco", "Eco")
        BALANCED -> tr(language, "Wastani", "Balanced")
        HIGH -> tr(language, "Kasi Juu", "High")
    }
}

data class DeviceHardwareSnapshot(
    val manufacturer: String = "Android",
    val model: String = "Device",
    val androidVersion: String = "13+",
    val sdkInt: Int = 33,
    val totalRamMb: Long = 4096L,
    val availableRamMb: Long = 1536L,
    val isLowRamDevice: Boolean = false,
    val isSystemLowMemory: Boolean = false,
    val cpuCores: Int = 8,
    val refreshRateHz: Int = 60,
    val smallestWidthDp: Int = 380,
    val isPowerSaveMode: Boolean = false,
    val isThermallyThrottled: Boolean = false,
    val memoryPressureActive: Boolean = false,
    val primaryAbi: String = "arm64-v8a"
) {
    val deviceDisplayName: String
        get() {
            val cleanMan = manufacturer.trim()
            val cleanModel = model.trim()
            if (cleanModel.isBlank()) return cleanMan.ifBlank { "Android" }
            if (cleanMan.isBlank() || cleanModel.startsWith(cleanMan, ignoreCase = true)) {
                return cleanModel
            }
            val titledMan = cleanMan.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }
            return "$titledMan $cleanModel"
        }

    val totalRamGb: Double get() = totalRamMb / 1024.0
    val availableRamGb: Double get() = availableRamMb / 1024.0
}

data class DevicePerformanceProfile(
    val hardware: DeviceHardwareSnapshot = DeviceHardwareSnapshot(),
    val selectedMode: PerformanceModePreference = PerformanceModePreference.AUTO,
    val resolvedTier: DevicePerformanceTier = DevicePerformanceTier.BALANCED,
    val animationScale: Float = 0.7f,
    val navTransitionMs: Int = 110,
    val enableChartAnimations: Boolean = true,
    val enableSpringMotion: Boolean = false,
    val downloadParallelStreams: Int = 3,
    val downloadBufferBytes: Int = 128 * 1024,
    val recentItemsLimit: Int = 5,
    val isCompactScreen: Boolean = false
) {
    fun scaledDuration(baseMs: Int): Int {
        if (animationScale <= 0.01f || baseMs <= 0) return 0
        return (baseMs * animationScale).toInt().coerceAtLeast(16)
    }

    companion object {
        fun defaultBalanced(): DevicePerformanceProfile =
            DevicePerformanceEngine.resolveProfile(
                hardware = DeviceHardwareSnapshot(),
                mode = PerformanceModePreference.AUTO
            )
    }
}

val LocalDevicePerformance = staticCompositionLocalOf {
    DevicePerformanceProfile.defaultBalanced()
}

/**
 * Real Android device-adaptive performance engine.
 * Reads live device RAM, CPU cores, Power Saver, Thermal throttling, refresh rate, and screen width
 * so the app runs smoothly and reliably on any phone — from 2 GB RAM budget devices to 120 Hz flagships.
 */
object DevicePerformanceEngine {

    private const val ECO_RAM_THRESHOLD_MB = 3_200L
    private const val HIGH_RAM_THRESHOLD_MB = 7_000L
    private const val ECO_BUFFER_BYTES = 64 * 1024
    private const val STANDARD_BUFFER_BYTES = 128 * 1024

    fun captureHardwareSnapshot(
        context: Context,
        memoryPressureActive: Boolean = false
    ): DeviceHardwareSnapshot {
        val appCtx = context.applicationContext
        val am = runCatching {
            appCtx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        }.getOrNull()

        val memInfo = ActivityManager.MemoryInfo()
        val hasMemInfo = runCatching {
            am?.getMemoryInfo(memInfo)
            am != null
        }.getOrDefault(false)

        val totalRamMb = if (hasMemInfo && memInfo.totalMem > 0L) {
            memInfo.totalMem / (1024L * 1024L)
        } else {
            4096L
        }
        val availableRamMb = if (hasMemInfo && memInfo.availMem >= 0L) {
            memInfo.availMem / (1024L * 1024L)
        } else {
            1024L
        }
        val isLowRamDevice = runCatching { am?.isLowRamDevice == true }.getOrDefault(false)
        val isSystemLowMemory = hasMemInfo && memInfo.lowMemory

        val pm = runCatching {
            appCtx.getSystemService(Context.POWER_SERVICE) as? PowerManager
        }.getOrNull()
        val isPowerSave = runCatching { pm?.isPowerSaveMode == true }.getOrDefault(false)
        val isThermallyThrottled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val status = pm?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
                status >= PowerManager.THERMAL_STATUS_MODERATE
            }.getOrDefault(false)
        } else {
            false
        }

        val dm = runCatching {
            appCtx.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        }.getOrNull()
        val refreshHz = runCatching {
            val rate = dm?.getDisplay(Display.DEFAULT_DISPLAY)?.refreshRate ?: 60f
            rate.toInt().coerceIn(30, 240)
        }.getOrDefault(60)

        val smallestWidthDp = runCatching {
            appCtx.resources.configuration.smallestScreenWidthDp
        }.getOrDefault(380)

        val cpuCores = runCatching {
            Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        }.getOrDefault(4)

        val primaryAbi = runCatching {
            Build.SUPPORTED_ABIS?.firstOrNull().orEmpty().ifBlank { "arm64-v8a" }
        }.getOrDefault("arm64-v8a")

        return DeviceHardwareSnapshot(
            manufacturer = Build.MANUFACTURER.orEmpty().ifBlank { "Android" },
            model = Build.MODEL.orEmpty().ifBlank { "Phone" },
            androidVersion = Build.VERSION.RELEASE.orEmpty().ifBlank { Build.VERSION.SDK_INT.toString() },
            sdkInt = Build.VERSION.SDK_INT,
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            isLowRamDevice = isLowRamDevice,
            isSystemLowMemory = isSystemLowMemory,
            cpuCores = cpuCores,
            refreshRateHz = refreshHz,
            smallestWidthDp = smallestWidthDp,
            isPowerSaveMode = isPowerSave,
            isThermallyThrottled = isThermallyThrottled,
            memoryPressureActive = memoryPressureActive,
            primaryAbi = primaryAbi
        )
    }

    fun resolveProfile(
        hardware: DeviceHardwareSnapshot,
        mode: PerformanceModePreference
    ): DevicePerformanceProfile {
        val tier = when (mode) {
            PerformanceModePreference.ECO -> DevicePerformanceTier.ECO
            PerformanceModePreference.BALANCED -> DevicePerformanceTier.BALANCED
            PerformanceModePreference.HIGH -> DevicePerformanceTier.HIGH
            PerformanceModePreference.AUTO -> autoSelectTier(hardware)
        }

        val isCompactScreen = hardware.smallestWidthDp in 1..359

        return when (tier) {
            DevicePerformanceTier.ECO -> DevicePerformanceProfile(
                hardware = hardware,
                selectedMode = mode,
                resolvedTier = tier,
                animationScale = 0f,
                navTransitionMs = 0,
                enableChartAnimations = false,
                enableSpringMotion = false,
                downloadParallelStreams = 2,
                downloadBufferBytes = ECO_BUFFER_BYTES,
                recentItemsLimit = 4,
                isCompactScreen = isCompactScreen
            )
            DevicePerformanceTier.BALANCED -> DevicePerformanceProfile(
                hardware = hardware,
                selectedMode = mode,
                resolvedTier = tier,
                animationScale = 0.7f,
                navTransitionMs = 110,
                enableChartAnimations = true,
                enableSpringMotion = false,
                downloadParallelStreams = 3,
                downloadBufferBytes = STANDARD_BUFFER_BYTES,
                recentItemsLimit = 5,
                isCompactScreen = isCompactScreen
            )
            DevicePerformanceTier.HIGH -> DevicePerformanceProfile(
                hardware = hardware,
                selectedMode = mode,
                resolvedTier = tier,
                animationScale = 1.0f,
                navTransitionMs = 180,
                enableChartAnimations = true,
                enableSpringMotion = true,
                downloadParallelStreams = 4,
                downloadBufferBytes = STANDARD_BUFFER_BYTES,
                recentItemsLimit = 6,
                isCompactScreen = isCompactScreen
            )
        }
    }

    private fun autoSelectTier(hardware: DeviceHardwareSnapshot): DevicePerformanceTier {
        val constrainedMemory = hardware.isLowRamDevice ||
            hardware.isSystemLowMemory ||
            hardware.memoryPressureActive ||
            (hardware.totalRamMb in 1L..ECO_RAM_THRESHOLD_MB) ||
            (hardware.availableRamMb in 1L..512L)

        val constrainedCpuOrPower = hardware.cpuCores in 1..4 ||
            hardware.isPowerSaveMode ||
            hardware.isThermallyThrottled

        if (constrainedMemory || constrainedCpuOrPower) {
            return DevicePerformanceTier.ECO
        }

        val flagshipClass = hardware.totalRamMb >= HIGH_RAM_THRESHOLD_MB &&
            hardware.cpuCores >= 8 &&
            hardware.availableRamMb >= 1_536L

        return if (flagshipClass) {
            DevicePerformanceTier.HIGH
        } else {
            DevicePerformanceTier.BALANCED
        }
    }

    /**
     * Safely trims stale temporary cache files (partial downloads, old temp PDFs)
     * without touching user databases, backups, or preferences. Returns freed bytes.
     */
    fun clearSafeTempCaches(context: Context): Long {
        var freedBytes = 0L
        val cacheDir = context.applicationContext.cacheDir ?: return 0L
        val safeSubDirs = listOf("temp_pdf", "shared_pdfs", "mengidata_updates")
        for (dirName in safeSubDirs) {
            val dir = File(cacheDir, dirName)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    val size = if (file.isFile) file.length() else 0L
                    if (file.isFile && runCatching { file.delete() }.getOrDefault(false)) {
                        freedBytes += size
                    }
                }
            }
        }
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && (file.name.endsWith(".tmp") || file.name.endsWith(".part"))) {
                val size = file.length()
                if (runCatching { file.delete() }.getOrDefault(false)) {
                    freedBytes += size
                }
            }
        }
        return freedBytes
    }
}
