package tz.mengidata.app.system

import android.app.ActivityManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.view.Display
import androidx.compose.runtime.staticCompositionLocalOf
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.tr
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

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
        ECO -> tr(language, "Eco (Simu ya Kawaida / Okoa Betri)", "Eco (Light / Battery Saver)")
        BALANCED -> tr(language, "Wastani (Haraka na Salama)", "Balanced (Smooth & Efficient)")
        HIGH -> tr(language, "Kasi ya Juu (Full Turbo & High-Hz)", "High Performance (Full Turbo & High-Hz)")
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
    val appUsedHeapMb: Int = 24,
    val appMaxHeapMb: Int = 256,
    val nativeHeapMb: Int = 18,
    val isLowRamDevice: Boolean = false,
    val isSystemLowMemory: Boolean = false,
    val cpuCores: Int = 8,
    val refreshRateHz: Int = 60,
    val maxRefreshRateHz: Int = 60,
    val smallestWidthDp: Int = 380,
    val isPowerSaveMode: Boolean = false,
    val isThermallyThrottled: Boolean = false,
    val thermalStatusCode: Int = 0,
    val batteryPercent: Int = 100,
    val batteryTempC: Float = 0f,
    val isCharging: Boolean = false,
    val memoryPressureActive: Boolean = false,
    val liveFps: Int = 60,
    val droppedFramePercent: Int = 0,
    val dbQueryLatencyMs: Double = 0.0,
    val totalDbRows: Int = 0,
    val hardwareScore: Int = 72,
    val primaryAbi: String = "arm64-v8a",
    val capturedAtMillis: Long = System.currentTimeMillis()
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
    val usedRamPercent: Float
        get() = if (totalRamMb > 0L) {
            ((totalRamMb - availableRamMb).toFloat() / totalRamMb.toFloat()).coerceIn(0f, 1f)
        } else {
            0.5f
        }
}

data class OptimizationReport(
    val freedCacheBytes: Long = 0L,
    val reclaimedHeapMb: Int = 0,
    val dbLatencyMs: Double = 0.0,
    val walCheckpointed: Boolean = false,
    val appliedRefreshHz: Int = 60,
    val appliedDbCacheMb: Int = 6,
    val appliedDbMmapMb: Int = 16,
    val resolvedTier: DevicePerformanceTier = DevicePerformanceTier.BALANCED,
    val completedAtMillis: Long = System.currentTimeMillis()
)

data class DevicePerformanceProfile(
    val hardware: DeviceHardwareSnapshot = DeviceHardwareSnapshot(),
    val selectedMode: PerformanceModePreference = PerformanceModePreference.AUTO,
    val resolvedTier: DevicePerformanceTier = DevicePerformanceTier.BALANCED,
    val animationScale: Float = 0.7f,
    val navTransitionMs: Int = 110,
    val enableChartAnimations: Boolean = true,
    val enableSpringMotion: Boolean = false,
    val targetRefreshRateHz: Int = 60,
    val sqliteCacheKb: Int = 6144,
    val sqliteMmapBytes: Long = 16L * 1024L * 1024L,
    val sqliteTempInMemory: Boolean = true,
    val workerThreadPriority: Int = Process.THREAD_PRIORITY_DEFAULT,
    val downloadParallelStreams: Int = 12,
    val downloadBufferBytes: Int = 1024 * 1024,
    val recentItemsLimit: Int = 5,
    val isCompactScreen: Boolean = false
) {
    val sqliteCacheMb: Int get() = (sqliteCacheKb / 1024).coerceAtLeast(1)
    val sqliteMmapMb: Int get() = (sqliteMmapBytes / (1024L * 1024L)).toInt()

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
 * Reads live device RAM, JVM heap, CPU cores, Power Saver, Thermal status,
 * BatteryManager capacity, display refresh rate, and SQLite query latency —
 * without registering broadcast receivers or touching Activity Window attributes on startup.
 */
object DevicePerformanceEngine {

    private const val ECO_RAM_THRESHOLD_MB = 3_200L
    private const val HIGH_RAM_THRESHOLD_MB = 7_000L
    private const val ECO_BUFFER_BYTES = 512 * 1024
    private const val STANDARD_BUFFER_BYTES = 1024 * 1024

    fun captureHardwareSnapshot(
        context: Context,
        memoryPressureActive: Boolean = false,
        liveFps: Int = 0,
        droppedFramePercent: Int = 0,
        dbQueryLatencyMs: Double = 0.0,
        totalDbRows: Int = 0
    ): DeviceHardwareSnapshot {
        val appCtx = runCatching { context.applicationContext ?: context }.getOrDefault(context)
        val am = runCatching {
            appCtx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        }.getOrNull()

        val memInfo = ActivityManager.MemoryInfo()
        val hasMemInfo = runCatching {
            if (am != null) {
                am.getMemoryInfo(memInfo)
                true
            } else {
                false
            }
        }.getOrDefault(false)

        val totalRamMb = if (hasMemInfo && memInfo.totalMem > 0L) {
            memInfo.totalMem / (1024L * 1024L)
        } else {
            4096L
        }
        val availableRamMb = if (hasMemInfo && memInfo.availMem >= 0L) {
            memInfo.availMem / (1024L * 1024L)
        } else {
            1536L
        }
        val isLowRamDevice = runCatching { am?.isLowRamDevice == true }.getOrDefault(false)
        val isSystemLowMemory = hasMemInfo && memInfo.lowMemory

        val runtime = Runtime.getRuntime()
        val appUsedHeapMb = runCatching {
            (((runtime.totalMemory() - runtime.freeMemory()).coerceAtLeast(0L)) / (1024L * 1024L)).toInt()
        }.getOrDefault(24)
        val appMaxHeapMb = runCatching {
            (runtime.maxMemory() / (1024L * 1024L)).toInt().coerceAtLeast(64)
        }.getOrDefault(256)

        val pm = runCatching {
            appCtx.getSystemService(Context.POWER_SERVICE) as? PowerManager
        }.getOrNull()
        val isPowerSave = runCatching { pm?.isPowerSaveMode == true }.getOrDefault(false)
        val thermalStatusCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { pm?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE }.getOrDefault(0)
        } else {
            0
        }
        val isThermallyThrottled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            thermalStatusCode >= PowerManager.THERMAL_STATUS_MODERATE
        } else {
            false
        }

        // Safely query battery level & charging state via BatteryManager system service
        // (avoids calling registerReceiver during ViewModel init on Android 14/15/16).
        val bm = runCatching {
            appCtx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        }.getOrNull()
        val batteryPercent = runCatching {
            val cap = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            if (cap in 1..100) cap else 100
        }.getOrDefault(100)
        val isCharging = runCatching {
            bm?.isCharging == true
        }.getOrDefault(false)

        val dm = runCatching {
            appCtx.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        }.getOrNull()
        val display = runCatching { dm?.getDisplay(Display.DEFAULT_DISPLAY) }.getOrNull()
        val refreshHz = runCatching {
            (display?.refreshRate ?: 60f).roundToInt().coerceIn(30, 240)
        }.getOrDefault(60)
        val maxRefreshHz = runCatching {
            val modes = display?.supportedModes
            val highest = modes?.maxOfOrNull { it.refreshRate } ?: refreshHz.toFloat()
            highest.roundToInt().coerceIn(refreshHz, 240)
        }.getOrDefault(refreshHz)

        val smallestWidthDp = runCatching {
            appCtx.resources.configuration.smallestScreenWidthDp
        }.getOrDefault(380)

        val cpuCores = runCatching {
            Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        }.getOrDefault(4)

        val primaryAbi = runCatching {
            Build.SUPPORTED_ABIS?.firstOrNull().orEmpty().ifBlank { "arm64-v8a" }
        }.getOrDefault("arm64-v8a")

        val hwScore = computeHardwareScore(
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            cpuCores = cpuCores,
            maxRefreshHz = maxRefreshHz,
            isLowRam = isLowRamDevice,
            isPowerSave = isPowerSave,
            isThrottled = isThermallyThrottled
        )

        val effectiveFps = if (liveFps > 0) liveFps else refreshHz

        return DeviceHardwareSnapshot(
            manufacturer = Build.MANUFACTURER.orEmpty().ifBlank { "Android" },
            model = Build.MODEL.orEmpty().ifBlank { "Phone" },
            androidVersion = Build.VERSION.RELEASE.orEmpty().ifBlank { Build.VERSION.SDK_INT.toString() },
            sdkInt = Build.VERSION.SDK_INT,
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            appUsedHeapMb = appUsedHeapMb,
            appMaxHeapMb = appMaxHeapMb,
            nativeHeapMb = (appUsedHeapMb / 2).coerceAtLeast(8),
            isLowRamDevice = isLowRamDevice,
            isSystemLowMemory = isSystemLowMemory,
            cpuCores = cpuCores,
            refreshRateHz = refreshHz,
            maxRefreshRateHz = maxRefreshHz,
            smallestWidthDp = smallestWidthDp,
            isPowerSaveMode = isPowerSave,
            isThermallyThrottled = isThermallyThrottled,
            thermalStatusCode = thermalStatusCode,
            batteryPercent = batteryPercent,
            batteryTempC = 0f,
            isCharging = isCharging,
            memoryPressureActive = memoryPressureActive,
            liveFps = effectiveFps,
            droppedFramePercent = droppedFramePercent,
            dbQueryLatencyMs = dbQueryLatencyMs,
            totalDbRows = totalDbRows,
            hardwareScore = hwScore,
            primaryAbi = primaryAbi,
            capturedAtMillis = System.currentTimeMillis()
        )
    }

    private fun computeHardwareScore(
        totalRamMb: Long,
        availableRamMb: Long,
        cpuCores: Int,
        maxRefreshHz: Int,
        isLowRam: Boolean,
        isPowerSave: Boolean,
        isThrottled: Boolean
    ): Int {
        var score = 0
        score += when {
            totalRamMb >= 10_000L -> 35
            totalRamMb >= 7_000L -> 30
            totalRamMb >= 5_000L -> 24
            totalRamMb >= 3_200L -> 16
            else -> 8
        }
        score += when {
            availableRamMb >= 2_500L -> 20
            availableRamMb >= 1_200L -> 15
            availableRamMb >= 600L -> 9
            else -> 3
        }
        score += when {
            cpuCores >= 8 -> 25
            cpuCores >= 6 -> 18
            cpuCores >= 4 -> 12
            else -> 6
        }
        score += when {
            maxRefreshHz >= 120 -> 20
            maxRefreshHz >= 90 -> 15
            else -> 10
        }
        if (isLowRam) score -= 18
        if (isPowerSave) score -= 12
        if (isThrottled) score -= 15
        return score.coerceIn(15, 100)
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
        val maxHz = hardware.maxRefreshRateHz.coerceAtLeast(60)

        return when (tier) {
            DevicePerformanceTier.ECO -> DevicePerformanceProfile(
                hardware = hardware,
                selectedMode = mode,
                resolvedTier = tier,
                animationScale = 0f,
                navTransitionMs = 0,
                enableChartAnimations = false,
                enableSpringMotion = false,
                targetRefreshRateHz = 60,
                sqliteCacheKb = 2048,
                sqliteMmapBytes = 0L,
                sqliteTempInMemory = false,
                workerThreadPriority = Process.THREAD_PRIORITY_BACKGROUND,
                downloadParallelStreams = 8,
                downloadBufferBytes = ECO_BUFFER_BYTES,
                recentItemsLimit = 4,
                isCompactScreen = isCompactScreen
            )
            DevicePerformanceTier.BALANCED -> DevicePerformanceProfile(
                hardware = hardware,
                selectedMode = mode,
                resolvedTier = tier,
                animationScale = 0.65f,
                navTransitionMs = 110,
                enableChartAnimations = true,
                enableSpringMotion = false,
                targetRefreshRateHz = maxHz.coerceAtMost(90),
                sqliteCacheKb = 6144,
                sqliteMmapBytes = 16L * 1024L * 1024L,
                sqliteTempInMemory = true,
                workerThreadPriority = Process.THREAD_PRIORITY_DEFAULT,
                downloadParallelStreams = 12,
                downloadBufferBytes = STANDARD_BUFFER_BYTES,
                recentItemsLimit = 5,
                isCompactScreen = isCompactScreen
            )
            DevicePerformanceTier.HIGH -> DevicePerformanceProfile(
                hardware = hardware,
                selectedMode = mode,
                resolvedTier = tier,
                animationScale = 1.0f,
                navTransitionMs = 200,
                enableChartAnimations = true,
                enableSpringMotion = true,
                targetRefreshRateHz = maxHz,
                sqliteCacheKb = 16384,
                sqliteMmapBytes = 64L * 1024L * 1024L,
                sqliteTempInMemory = true,
                workerThreadPriority = Process.THREAD_PRIORITY_MORE_FAVORABLE,
                downloadParallelStreams = 16,
                downloadBufferBytes = STANDARD_BUFFER_BYTES,
                recentItemsLimit = 7,
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

        val lowBatteryNotCharging = hardware.batteryPercent in 1..15 && !hardware.isCharging
        val constrainedCpuOrPower = hardware.cpuCores in 1..4 ||
            hardware.isPowerSaveMode ||
            hardware.isThermallyThrottled ||
            lowBatteryNotCharging

        if (constrainedMemory || constrainedCpuOrPower) {
            return DevicePerformanceTier.ECO
        }

        val flagshipClass = (hardware.totalRamMb >= HIGH_RAM_THRESHOLD_MB &&
            hardware.cpuCores >= 8 &&
            hardware.availableRamMb >= 1_400L) ||
            (hardware.maxRefreshRateHz >= 120 && hardware.totalRamMb >= 5_500L && hardware.availableRamMb >= 1_200L)

        return if (flagshipClass) {
            DevicePerformanceTier.HIGH
        } else {
            DevicePerformanceTier.BALANCED
        }
    }

    /**
     * Safely trims stale temporary cache files (partial downloads, old temp PDFs, image cache)
     * without touching user databases, backups, or preferences. Returns freed bytes.
     */
    fun clearSafeTempCaches(context: Context): Long {
        var freedBytes = 0L
        val cacheDir = context.applicationContext.cacheDir ?: return 0L
        val safeSubDirs = listOf("temp_pdf", "shared_pdfs", "mengidata_updates", "image_cache", "http_cache")
        for (dirName in safeSubDirs) {
            val dir = File(cacheDir, dirName)
            if (dir.exists() && dir.isDirectory) {
                freedBytes += deleteDirectoryContents(dir)
            }
        }
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && (file.name.endsWith(".tmp") || file.name.endsWith(".part") || file.name.endsWith(".bak"))) {
                val size = file.length()
                if (runCatching { file.delete() }.getOrDefault(false)) {
                    freedBytes += size
                }
            }
        }
        return freedBytes
    }

    private fun deleteDirectoryContents(dir: File): Long {
        var freed = 0L
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                freed += deleteDirectoryContents(child)
                runCatching { child.delete() }
            } else if (child.isFile) {
                val size = child.length()
                if (runCatching { child.delete() }.getOrDefault(false)) {
                    freed += size
                }
            }
        }
        return freed
    }
}
