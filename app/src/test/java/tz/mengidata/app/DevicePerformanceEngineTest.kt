package tz.mengidata.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tz.mengidata.app.system.DeviceHardwareSnapshot
import tz.mengidata.app.system.DevicePerformanceEngine
import tz.mengidata.app.system.DevicePerformanceTier
import tz.mengidata.app.system.PerformanceModePreference

class DevicePerformanceEngineTest {

    @Test
    fun autoMode_budgetLowRamPhone_selectsEcoTier() {
        val budgetPhone = DeviceHardwareSnapshot(
            manufacturer = "TECNO",
            model = "TECNO POP 8",
            totalRamMb = 2048L,
            availableRamMb = 700L,
            isLowRamDevice = true,
            cpuCores = 4,
            smallestWidthDp = 340
        )

        val profile = DevicePerformanceEngine.resolveProfile(budgetPhone, PerformanceModePreference.AUTO)

        assertEquals(DevicePerformanceTier.ECO, profile.resolvedTier)
        assertEquals(0f, profile.animationScale, 0.001f)
        assertEquals(0, profile.navTransitionMs)
        assertFalse(profile.enableChartAnimations)
        assertFalse(profile.enableSpringMotion)
        assertEquals(2, profile.downloadParallelStreams)
        assertEquals(64 * 1024, profile.downloadBufferBytes)
        assertEquals(4, profile.recentItemsLimit)
        assertTrue(profile.isCompactScreen)
        assertEquals("TECNO POP 8", budgetPhone.deviceDisplayName)
    }

    @Test
    fun autoMode_midRangePhone_selectsBalancedTier() {
        val midRangePhone = DeviceHardwareSnapshot(
            manufacturer = "samsung",
            model = "SM-A256E",
            totalRamMb = 6144L,
            availableRamMb = 2400L,
            cpuCores = 8,
            smallestWidthDp = 384
        )

        val profile = DevicePerformanceEngine.resolveProfile(midRangePhone, PerformanceModePreference.AUTO)

        assertEquals(DevicePerformanceTier.BALANCED, profile.resolvedTier)
        assertEquals(0.7f, profile.animationScale, 0.001f)
        assertEquals(110, profile.navTransitionMs)
        assertTrue(profile.enableChartAnimations)
        assertFalse(profile.enableSpringMotion)
        assertEquals(3, profile.downloadParallelStreams)
        assertEquals(128 * 1024, profile.downloadBufferBytes)
        assertFalse(profile.isCompactScreen)
        assertEquals("Samsung SM-A256E", midRangePhone.deviceDisplayName)
    }

    @Test
    fun autoMode_flagshipPhone_selectsHighTier_andDowngradesOnPowerSaverOrThermal() {
        val flagship = DeviceHardwareSnapshot(
            manufacturer = "Google",
            model = "Pixel 9 Pro",
            totalRamMb = 12288L,
            availableRamMb = 5200L,
            cpuCores = 8,
            refreshRateHz = 120,
            smallestWidthDp = 412
        )

        val normalProfile = DevicePerformanceEngine.resolveProfile(flagship, PerformanceModePreference.AUTO)
        assertEquals(DevicePerformanceTier.HIGH, normalProfile.resolvedTier)
        assertTrue(normalProfile.enableSpringMotion)
        assertEquals(4, normalProfile.downloadParallelStreams)
        assertEquals(6, normalProfile.recentItemsLimit)

        // Power Saver active -> auto-downgrades to ECO
        val batterySaverProfile = DevicePerformanceEngine.resolveProfile(
            flagship.copy(isPowerSaveMode = true),
            PerformanceModePreference.AUTO
        )
        assertEquals(DevicePerformanceTier.ECO, batterySaverProfile.resolvedTier)

        // Thermal throttle active -> auto-downgrades to ECO
        val thermalProfile = DevicePerformanceEngine.resolveProfile(
            flagship.copy(isThermallyThrottled = true),
            PerformanceModePreference.AUTO
        )
        assertEquals(DevicePerformanceTier.ECO, thermalProfile.resolvedTier)

        // System memory pressure -> auto-downgrades to ECO
        val memPressureProfile = DevicePerformanceEngine.resolveProfile(
            flagship.copy(memoryPressureActive = true),
            PerformanceModePreference.AUTO
        )
        assertEquals(DevicePerformanceTier.ECO, memPressureProfile.resolvedTier)
    }

    @Test
    fun manualOverride_respectsUserChoice() {
        val budgetPhone = DeviceHardwareSnapshot(totalRamMb = 2048L, cpuCores = 4)
        val forcedHigh = DevicePerformanceEngine.resolveProfile(budgetPhone, PerformanceModePreference.HIGH)
        assertEquals(DevicePerformanceTier.HIGH, forcedHigh.resolvedTier)
        assertEquals(4, forcedHigh.downloadParallelStreams)

        val flagship = DeviceHardwareSnapshot(totalRamMb = 12288L, availableRamMb = 6000L, cpuCores = 8)
        val forcedEco = DevicePerformanceEngine.resolveProfile(flagship, PerformanceModePreference.ECO)
        assertEquals(DevicePerformanceTier.ECO, forcedEco.resolvedTier)
        assertEquals(0, forcedEco.scaledDuration(220))
    }
}
