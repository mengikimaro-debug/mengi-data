package tz.mengidata.app

import androidx.biometric.BiometricManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tz.mengidata.app.security.AppBiometricAuthenticator
import tz.mengidata.app.security.AppBiometricStatus

class AppBiometricSecurityTest {

    @Test
    fun `mapStatusCodes prioritizes ready strong or weak biometrics`() {
        val status = AppBiometricAuthenticator.mapStatusCodes(
            biometricCode = BiometricManager.BIOMETRIC_SUCCESS,
            credentialCode = BiometricManager.BIOMETRIC_SUCCESS
        )
        assertEquals(AppBiometricStatus.READY, status)
    }

    @Test
    fun `mapStatusCodes detects credential only when no biometric enrolled but device lock ready`() {
        val status = AppBiometricAuthenticator.mapStatusCodes(
            biometricCode = BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            credentialCode = BiometricManager.BIOMETRIC_SUCCESS
        )
        assertEquals(AppBiometricStatus.CREDENTIAL_ONLY, status)
    }

    @Test
    fun `mapStatusCodes detects none enrolled when neither biometric nor device lock is set`() {
        val status = AppBiometricAuthenticator.mapStatusCodes(
            biometricCode = BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            credentialCode = BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
        )
        assertEquals(AppBiometricStatus.NONE_ENROLLED, status)
    }

    @Test
    fun `mapStatusCodes detects hardware unavailable`() {
        val status = AppBiometricAuthenticator.mapStatusCodes(
            biometricCode = BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            credentialCode = BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
        )
        assertEquals(AppBiometricStatus.UNAVAILABLE, status)
    }

    @Test
    fun `resolvePromptConfig uses app PIN fallback as negative button without DEVICE_CREDENTIAL`() {
        val config = AppBiometricAuthenticator.resolvePromptConfig(
            status = AppBiometricStatus.READY,
            hasAppPinFallback = true,
            allowDeviceCredentialFallback = true
        )
        assertTrue(config.canPrompt)
        assertTrue(config.useNegativeButtonAsPinFallback)
        assertTrue(config.allowNegativeButton)
        assertEquals(AppBiometricAuthenticator.BIOMETRIC_AUTHENTICATORS, config.allowedAuthenticators)
        assertEquals(0, config.allowedAuthenticators and BiometricManager.Authenticators.DEVICE_CREDENTIAL)
    }

    @Test
    fun `resolvePromptConfig never sets allowNegativeButton when DEVICE_CREDENTIAL is included`() {
        val readyConfig = AppBiometricAuthenticator.resolvePromptConfig(
            status = AppBiometricStatus.READY,
            hasAppPinFallback = false,
            allowDeviceCredentialFallback = true
        )
        assertTrue(readyConfig.canPrompt)
        assertFalse(readyConfig.allowNegativeButton)
        assertEquals(AppBiometricAuthenticator.BIOMETRIC_OR_DEVICE_CREDENTIAL, readyConfig.allowedAuthenticators)

        val credOnlyConfig = AppBiometricAuthenticator.resolvePromptConfig(
            status = AppBiometricStatus.CREDENTIAL_ONLY,
            hasAppPinFallback = false,
            allowDeviceCredentialFallback = true
        )
        assertTrue(credOnlyConfig.canPrompt)
        assertFalse(credOnlyConfig.allowNegativeButton)
        assertEquals(AppBiometricAuthenticator.BIOMETRIC_OR_DEVICE_CREDENTIAL, credOnlyConfig.allowedAuthenticators)
    }

    @Test
    fun `resolvePromptConfig blocks prompt when unavailable or none enrolled`() {
        val noneEnrolled = AppBiometricAuthenticator.resolvePromptConfig(
            status = AppBiometricStatus.NONE_ENROLLED,
            hasAppPinFallback = true,
            allowDeviceCredentialFallback = true
        )
        assertFalse(noneEnrolled.canPrompt)

        val unavailable = AppBiometricAuthenticator.resolvePromptConfig(
            status = AppBiometricStatus.UNAVAILABLE,
            hasAppPinFallback = false,
            allowDeviceCredentialFallback = true
        )
        assertFalse(unavailable.canPrompt)
    }

    @Test
    fun `isLockScreenRequired requires authentication whenever either lock is enabled and app is locked`() {
        assertFalse(AppBiometricAuthenticator.isLockScreenRequired(biometricLockEnabled = false, pinLockEnabled = false, isAppUnlocked = false))
        assertFalse(AppBiometricAuthenticator.isLockScreenRequired(biometricLockEnabled = true, pinLockEnabled = false, isAppUnlocked = true))
        assertTrue(AppBiometricAuthenticator.isLockScreenRequired(biometricLockEnabled = true, pinLockEnabled = false, isAppUnlocked = false))
        assertTrue(AppBiometricAuthenticator.isLockScreenRequired(biometricLockEnabled = false, pinLockEnabled = true, isAppUnlocked = false))
        assertTrue(AppBiometricAuthenticator.isLockScreenRequired(biometricLockEnabled = true, pinLockEnabled = true, isAppUnlocked = false))
    }
}
