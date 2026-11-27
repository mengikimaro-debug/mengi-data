package tz.mengidata.app.security

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.net.Uri
import android.os.CancellationSignal
import android.os.UserManager
import android.provider.Settings
import android.text.TextUtils
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.tr

enum class BiometricAvailability {
    READY,
    NONE_ENROLLED,
    UNAVAILABLE
}

data class UninstallGuardStatus(
    val isDeviceOwner: Boolean = false,
    val isDeviceOwnerUninstallBlocked: Boolean = false,
    val isDeviceAdminActive: Boolean = false,
    val isAccessibilityGuardActive: Boolean = false,
    val biometricAvailability: BiometricAvailability = BiometricAvailability.READY
) {
    /** True when either Device Owner OR both Android Device Admin + System Accessibility Guard are active. */
    val isFullyProtected: Boolean
        get() = isDeviceOwnerUninstallBlocked || (isDeviceAdminActive && isAccessibilityGuardActive)

    /** True when at least one Android system protection layer is active. */
    val isProtected: Boolean
        get() = isDeviceOwnerUninstallBlocked || isAccessibilityGuardActive || isDeviceAdminActive
}

/**
 * Coordinates native Android System Fingerprint (BiometricPrompt),
 * Android System Accessibility Uninstall Interceptor, Android Device Administrator,
 * and Android Enterprise Device Owner (`setUninstallBlocked` + `DISALLOW_UNINSTALL_APPS`).
 */
object UninstallProtectionManager {

    private const val BIOMETRIC_ONLY_AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK

    private const val BIOMETRIC_OR_CREDENTIAL_AUTHENTICATORS =
        BIOMETRIC_ONLY_AUTHENTICATORS or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun adminComponentName(context: Context): ComponentName =
        ComponentName(context.applicationContext, VaultUninstallGuardAdminReceiver::class.java)

    fun accessibilityComponentName(context: Context): ComponentName =
        ComponentName(context.applicationContext, VaultUninstallAccessibilityService::class.java)

    fun buildAdbDeviceOwnerCommand(context: Context): String {
        val component = adminComponentName(context)
        return "adb shell dpm set-device-owner ${component.flattenToShortString()}"
    }

    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        return runCatching {
            dpm.isDeviceOwnerApp(context.packageName)
        }.getOrDefault(false)
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        return runCatching {
            dpm.isAdminActive(adminComponentName(context))
        }.getOrDefault(false)
    }

    fun isDeviceOwnerUninstallBlocked(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        return runCatching {
            dpm.isDeviceOwnerApp(context.packageName) &&
                dpm.isUninstallBlocked(adminComponentName(context), context.packageName)
        }.getOrDefault(false)
    }

    fun isAccessibilityGuardEnabled(context: Context): Boolean {
        val enabledServices = runCatching {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        }.getOrNull() ?: return false

        val expectedComponent = accessibilityComponentName(context)
        val expectedFull = expectedComponent.flattenToString()
        val expectedShort = expectedComponent.flattenToShortString()
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            val service = splitter.next()
            if (service.equals(expectedFull, ignoreCase = true) ||
                service.equals(expectedShort, ignoreCase = true)
            ) {
                return true
            }
        }
        return false
    }

    fun isUninstallProtectionActive(context: Context): Boolean =
        isDeviceOwnerUninstallBlocked(context) ||
            isAccessibilityGuardEnabled(context) ||
            isDeviceAdminActive(context)

    fun getStatus(context: Context): UninstallGuardStatus {
        val owner = isDeviceOwner(context)
        val ownerBlocked = if (owner) isDeviceOwnerUninstallBlocked(context) else false
        val adminActive = isDeviceAdminActive(context)
        val accessibilityActive = isAccessibilityGuardEnabled(context)
        return UninstallGuardStatus(
            isDeviceOwner = owner,
            isDeviceOwnerUninstallBlocked = ownerBlocked,
            isDeviceAdminActive = adminActive,
            isAccessibilityGuardActive = accessibilityActive,
            biometricAvailability = getBiometricStatus(context)
        )
    }

    fun enforceDeviceOwnerUninstallBlock(context: Context, blocked: Boolean): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        if (!isDeviceOwner(context)) return false
        val component = adminComponentName(context)
        return runCatching {
            dpm.setUninstallBlocked(component, context.packageName, blocked)
            if (blocked) {
                dpm.addUserRestriction(component, UserManager.DISALLOW_UNINSTALL_APPS)
            } else {
                dpm.clearUserRestriction(component, UserManager.DISALLOW_UNINSTALL_APPS)
            }
            true
        }.getOrDefault(false)
    }

    @Suppress("DEPRECATION")
    fun releaseProtectionAfterFingerprint(
        context: Context,
        clearDeviceOwnerForUninstall: Boolean = false
    ): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        val component = adminComponentName(context)
        return runCatching {
            if (dpm.isDeviceOwnerApp(context.packageName)) {
                dpm.setUninstallBlocked(component, context.packageName, false)
                dpm.clearUserRestriction(component, UserManager.DISALLOW_UNINSTALL_APPS)
                if (clearDeviceOwnerForUninstall) {
                    dpm.clearDeviceOwnerApp(context.packageName)
                }
            } else if (dpm.isAdminActive(component)) {
                dpm.removeActiveAdmin(component)
            }
            true
        }.getOrDefault(false)
    }

    fun getBiometricStatus(context: Context): BiometricAvailability {
        val manager = context.getSystemService(BiometricManager::class.java)
            ?: return BiometricAvailability.UNAVAILABLE
        return when (manager.canAuthenticate(BIOMETRIC_ONLY_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.READY
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            else -> BiometricAvailability.UNAVAILABLE
        }
    }

    fun canUseDeviceCredentialFallback(context: Context): Boolean {
        val manager = context.getSystemService(BiometricManager::class.java) ?: return false
        return manager.canAuthenticate(BIOMETRIC_OR_CREDENTIAL_AUTHENTICATORS) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun buildActivateDeviceAdminIntent(context: Context, language: AppLanguage): Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName(context))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                tr(
                    language,
                    "Washa ulinzi huu ili kuzuia mtu yeyote kuondoa (uninstall) VaultLedger bila uthibitisho wa Alama ya Kidole (Fingerprint).",
                    "Activate this guard to prevent anyone from uninstalling VaultLedger without Fingerprint verification."
                )
            )
        }
    }

    fun buildOpenAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildOpenAppInfoSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildEnrollFingerprintIntent(): Intent {
        return Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
            putExtra(
                Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                BIOMETRIC_ONLY_AUTHENTICATORS
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildSecuritySettingsFallbackIntent(): Intent {
        return Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildUninstallSelfIntent(context: Context): Intent {
        return Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    /**
     * Shows the native Android System Fingerprint dialog (BiometricPrompt).
     * Unwraps Compose's ContextWrapper to pass the true Activity context for maximum OEM reliability.
     */
    fun authenticateFingerprint(
        context: Context,
        title: String,
        subtitle: String,
        description: String,
        cancelLabel: String,
        allowCredentialFallbackIfNoBiometric: Boolean = false,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val hostContext: Context = findActivity(context) ?: context
        val status = getBiometricStatus(hostContext)
        val useCredentialFallback = status != BiometricAvailability.READY &&
            allowCredentialFallbackIfNoBiometric &&
            canUseDeviceCredentialFallback(hostContext)

        if (status != BiometricAvailability.READY && !useCredentialFallback) {
            onFailure(
                if (status == BiometricAvailability.NONE_ENROLLED) {
                    "NONE_ENROLLED"
                } else {
                    "UNAVAILABLE"
                }
            )
            return
        }

        val executor = hostContext.mainExecutor
        val cancellationSignal = CancellationSignal()

        val builder = BiometricPrompt.Builder(hostContext)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setConfirmationRequired(true)

        if (useCredentialFallback) {
            builder.setAllowedAuthenticators(BIOMETRIC_OR_CREDENTIAL_AUTHENTICATORS)
        } else {
            builder.setAllowedAuthenticators(BIOMETRIC_ONLY_AUTHENTICATORS)
            builder.setNegativeButton(cancelLabel, executor) { _, _ ->
                onFailure("CANCELLED")
            }
        }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                ) {
                    onFailure("CANCELLED")
                } else {
                    onFailure(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        }

        runCatching {
            builder.build().authenticate(cancellationSignal, executor, callback)
        }.onFailure { err ->
            onFailure(err.localizedMessage ?: "BIOMETRIC_ERROR")
        }
    }
}
