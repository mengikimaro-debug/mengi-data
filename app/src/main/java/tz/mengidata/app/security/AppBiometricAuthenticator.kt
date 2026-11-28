package tz.mengidata.app.security

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.tr

enum class AppBiometricStatus {
    READY,
    CREDENTIAL_ONLY,
    NONE_ENROLLED,
    UNAVAILABLE
}

sealed class BiometricAuthOutcome {
    data object Success : BiometricAuthOutcome()
    data object Cancelled : BiometricAuthOutcome()
    data object UseAppPinFallback : BiometricAuthOutcome()
    data class Unavailable(val status: AppBiometricStatus) : BiometricAuthOutcome()
    data class Error(val errorCode: Int, val message: String) : BiometricAuthOutcome()
}

data class BiometricPromptConfig(
    val canPrompt: Boolean,
    val allowedAuthenticators: Int,
    val useNegativeButtonAsPinFallback: Boolean,
    val allowNegativeButton: Boolean
)

/**
 * AndroidX Biometric authentication engine securing VaultLedger™ financial data
 * on cold start, background return, and sensitive security setting changes.
 */
object AppBiometricAuthenticator {

    const val BIOMETRIC_AUTHENTICATORS = BIOMETRIC_STRONG or BIOMETRIC_WEAK
    const val BIOMETRIC_OR_DEVICE_CREDENTIAL = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    fun getStatus(context: Context): AppBiometricStatus {
        val manager = BiometricManager.from(context.applicationContext)
        val biometricCode = manager.canAuthenticate(BIOMETRIC_AUTHENTICATORS)
        val credentialCode = manager.canAuthenticate(BIOMETRIC_OR_DEVICE_CREDENTIAL)
        return mapStatusCodes(biometricCode = biometricCode, credentialCode = credentialCode)
    }

    /**
     * Pure deterministic mapper for unit-testing BiometricManager availability combinations.
     */
    fun mapStatusCodes(biometricCode: Int, credentialCode: Int): AppBiometricStatus {
        return when {
            biometricCode == BiometricManager.BIOMETRIC_SUCCESS -> AppBiometricStatus.READY
            credentialCode == BiometricManager.BIOMETRIC_SUCCESS -> AppBiometricStatus.CREDENTIAL_ONLY
            biometricCode == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> AppBiometricStatus.NONE_ENROLLED
            else -> AppBiometricStatus.UNAVAILABLE
        }
    }

    /**
     * Resolves AndroidX BiometricPrompt authenticators without violating the AndroidX rule
     * that DEVICE_CREDENTIAL cannot be combined with setNegativeButtonText().
     */
    fun resolvePromptConfig(
        status: AppBiometricStatus,
        hasAppPinFallback: Boolean,
        allowDeviceCredentialFallback: Boolean
    ): BiometricPromptConfig {
        return when (status) {
            AppBiometricStatus.READY -> {
                if (hasAppPinFallback) {
                    BiometricPromptConfig(
                        canPrompt = true,
                        allowedAuthenticators = BIOMETRIC_AUTHENTICATORS,
                        useNegativeButtonAsPinFallback = true,
                        allowNegativeButton = true
                    )
                } else if (allowDeviceCredentialFallback) {
                    BiometricPromptConfig(
                        canPrompt = true,
                        allowedAuthenticators = BIOMETRIC_OR_DEVICE_CREDENTIAL,
                        useNegativeButtonAsPinFallback = false,
                        allowNegativeButton = false
                    )
                } else {
                    BiometricPromptConfig(
                        canPrompt = true,
                        allowedAuthenticators = BIOMETRIC_AUTHENTICATORS,
                        useNegativeButtonAsPinFallback = false,
                        allowNegativeButton = true
                    )
                }
            }
            AppBiometricStatus.CREDENTIAL_ONLY -> {
                if (allowDeviceCredentialFallback) {
                    BiometricPromptConfig(
                        canPrompt = true,
                        allowedAuthenticators = BIOMETRIC_OR_DEVICE_CREDENTIAL,
                        useNegativeButtonAsPinFallback = false,
                        allowNegativeButton = false
                    )
                } else {
                    BiometricPromptConfig(
                        canPrompt = false,
                        allowedAuthenticators = 0,
                        useNegativeButtonAsPinFallback = false,
                        allowNegativeButton = false
                    )
                }
            }
            AppBiometricStatus.NONE_ENROLLED,
            AppBiometricStatus.UNAVAILABLE -> BiometricPromptConfig(
                canPrompt = false,
                allowedAuthenticators = 0,
                useNegativeButtonAsPinFallback = false,
                allowNegativeButton = false
            )
        }
    }

    fun isLockScreenRequired(
        biometricLockEnabled: Boolean,
        pinLockEnabled: Boolean,
        isAppUnlocked: Boolean
    ): Boolean = (biometricLockEnabled || pinLockEnabled) && !isAppUnlocked

    fun buildEnrollBiometricIntent(): Intent {
        return Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
            putExtra(
                Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                BIOMETRIC_AUTHENTICATORS
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildSecuritySettingsFallbackIntent(): Intent {
        return Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun findFragmentActivity(context: Context): FragmentActivity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is FragmentActivity) return current
            current = current.baseContext
        }
        return null
    }

    fun authenticateAppAccess(
        context: Context,
        language: AppLanguage,
        title: String,
        subtitle: String,
        description: String,
        hasAppPinFallback: Boolean = false,
        allowDeviceCredentialFallback: Boolean = true,
        confirmationRequired: Boolean = false,
        onOutcome: (BiometricAuthOutcome) -> Unit
    ) {
        val activity = findFragmentActivity(context)
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            onOutcome(BiometricAuthOutcome.Unavailable(AppBiometricStatus.UNAVAILABLE))
            return
        }

        val status = getStatus(activity)
        val config = resolvePromptConfig(
            status = status,
            hasAppPinFallback = hasAppPinFallback,
            allowDeviceCredentialFallback = allowDeviceCredentialFallback
        )

        if (!config.canPrompt) {
            onOutcome(BiometricAuthOutcome.Unavailable(status))
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onOutcome(BiometricAuthOutcome.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        if (config.useNegativeButtonAsPinFallback) {
                            onOutcome(BiometricAuthOutcome.UseAppPinFallback)
                        } else {
                            onOutcome(BiometricAuthOutcome.Cancelled)
                        }
                    }
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> {
                        onOutcome(BiometricAuthOutcome.Cancelled)
                    }
                    BiometricPrompt.ERROR_NO_BIOMETRICS,
                    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> {
                        onOutcome(BiometricAuthOutcome.Unavailable(AppBiometricStatus.NONE_ENROLLED))
                    }
                    BiometricPrompt.ERROR_HW_NOT_PRESENT,
                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> {
                        onOutcome(BiometricAuthOutcome.Unavailable(AppBiometricStatus.UNAVAILABLE))
                    }
                    else -> {
                        onOutcome(BiometricAuthOutcome.Error(errorCode, errString.toString()))
                    }
                }
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val infoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setAllowedAuthenticators(config.allowedAuthenticators)
            .setConfirmationRequired(confirmationRequired)

        if (config.allowNegativeButton) {
            val negativeText = if (config.useNegativeButtonAsPinFallback) {
                tr(language, "Tumia Namba ya Siri (PIN)", "Use 4-Digit PIN")
            } else {
                tr(language, "Ghairi", "Cancel")
            }
            infoBuilder.setNegativeButtonText(negativeText)
        }

        runCatching {
            prompt.authenticate(infoBuilder.build())
        }.onFailure { err ->
            onOutcome(
                BiometricAuthOutcome.Error(
                    errorCode = -1,
                    message = err.localizedMessage ?: "BIOMETRIC_PROMPT_ERROR"
                )
            )
        }
    }
}
