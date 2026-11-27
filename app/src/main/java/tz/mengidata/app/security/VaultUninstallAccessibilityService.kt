package tz.mengidata.app.security

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import tz.mengidata.app.data.prefs.AppPreferences
import java.util.Locale

/**
 * Native Android System Accessibility Guard that monitors Android's PackageInstaller
 * and System Settings screens in real time.
 *
 * Whenever anyone attempts to uninstall VaultLedger, deactivate its Device Admin,
 * clear its data, or disable this Accessibility Guard, the service immediately dismisses
 * the system screen and launches UninstallAuthActivity to demand the owner's Fingerprint
 * via Android's native BiometricPrompt.
 */
class VaultUninstallAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        AppPreferences(this).setUninstallProtectionPreferred(true)
        // Brief 2.5s settle window so toggling the service ON doesn't immediately prompt on the same screen,
        // while ensuring immediate enforcement as soon as the user navigates away to test uninstall.
        UninstallGuardSession.grantTemporaryUnlock(2_500L)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString()?.lowercase(Locale.ROOT) ?: return

        // Never intercept VaultLedger's own UI or non-system apps
        if (pkg == packageName.lowercase(Locale.ROOT)) return
        if (!isMonitoredSystemPackage(pkg)) return

        // Respect temporary grace window after Fingerprint verification or intentional setup
        if (UninstallGuardSession.isTemporarilyUnlocked()) return

        // Only enforce when uninstall guard is enabled
        if (!AppPreferences(this).uninstallProtectionPreferred.value) return

        val root = rootInActiveWindow ?: return
        val collectedText = StringBuilder(512)
        collectVisibleTexts(root, collectedText, maxDepth = 10, maxChars = 2400)
        val screenText = collectedText.toString().lowercase(Locale.ROOT)
        if (screenText.isBlank()) return

        // Must specifically reference VaultLedger (app name, package id, or receiver label)
        val mentionsApp = screenText.contains("vaultledger") ||
            screenText.contains("tz.mengidata.app") ||
            screenText.contains("ulinzi wa kuondoa vaultledger")
        if (!mentionsApp) return

        // Whitelist benign Android permission-granting screens so normal setup never gets blocked
        if (isBenignPermissionScreen(screenText)) return

        val isPackageInstaller = pkg.contains("packageinstaller")
        val isPlayStore = pkg.contains("vending")
        val isUninstallTrigger = containsAny(screenText, UNINSTALL_KEYWORDS)
        val isTamperTrigger = containsAny(screenText, TAMPER_KEYWORDS)

        val shouldBlock = when {
            isPackageInstaller && isUninstallTrigger -> true
            isPlayStore && isUninstallTrigger -> true
            !isPackageInstaller && (isUninstallTrigger || isTamperTrigger) -> true
            else -> false
        }

        if (!shouldBlock) return
        if (!UninstallGuardSession.canLaunchAuthPromptNow()) return

        // Immediately back out of the Android System Uninstall / Deactivate screen
        performGlobalAction(GLOBAL_ACTION_BACK)

        val triggerMode = if (isPackageInstaller || isUninstallTrigger) {
            UninstallAuthActivity.TRIGGER_UNINSTALL
        } else {
            UninstallAuthActivity.TRIGGER_SETTINGS_GUARD
        }

        val authIntent = Intent(this, UninstallAuthActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            putExtra(UninstallAuthActivity.EXTRA_TRIGGER_MODE, triggerMode)
        }
        runCatching { startActivity(authIntent) }
    }

    override fun onInterrupt() {
        // No persistent speech/audio state to release
    }

    private fun isMonitoredSystemPackage(pkg: String): Boolean {
        return pkg.contains("packageinstaller") ||
            pkg == "com.android.settings" ||
            pkg.startsWith("com.samsung.android.settings") ||
            pkg.startsWith("com.miui.securitycenter") ||
            pkg.startsWith("com.coloros.safecenter") ||
            pkg.startsWith("com.oplus.safecenter") ||
            pkg.startsWith("com.transsion") ||
            pkg == "com.android.vending" ||
            pkg.contains("launcher")
    }

    private fun isBenignPermissionScreen(text: String): Boolean {
        return containsAny(text, BENIGN_SETUP_KEYWORDS) && !containsAny(text, HARD_UNINSTALL_KEYWORDS)
    }

    private fun collectVisibleTexts(
        node: AccessibilityNodeInfo?,
        out: StringBuilder,
        maxDepth: Int,
        maxChars: Int
    ) {
        if (node == null || maxDepth < 0 || out.length >= maxChars) return
        node.text?.let {
            if (it.isNotBlank()) {
                out.append(it).append(' ')
            }
        }
        node.contentDescription?.let {
            if (it.isNotBlank()) {
                out.append(it).append(' ')
            }
        }
        val count = node.childCount
        for (i in 0 until count) {
            if (out.length >= maxChars) break
            val child = node.getChild(i) ?: continue
            collectVisibleTexts(child, out, maxDepth - 1, maxChars)
        }
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    companion object {
        private val HARD_UNINSTALL_KEYWORDS = listOf(
            "do you want to uninstall",
            "uninstalling",
            "deactivate & uninstall",
            "deactivate and uninstall",
            "ondoa programu hii",
            "unataka kuondoa"
        )

        private val UNINSTALL_KEYWORDS = listOf(
            "uninstall",
            "ondoa",
            "sanidua",
            "futa programu",
            "remove app",
            "delete app",
            "deactivate & uninstall",
            "deactivate and uninstall",
            "do you want to uninstall"
        )

        private val TAMPER_KEYWORDS = listOf(
            "deactivate",
            "zima msimamizi",
            "device admin",
            "msimamizi wa kifaa",
            "clear storage",
            "clear data",
            "futa data",
            "force stop",
            "simamisha kwa nguvu",
            "use ulinzi wa kuondoa",
            "tumia ulinzi wa kuondoa",
            "stop ulinzi wa kuondoa",
            "turn off ulinzi wa kuondoa"
        )

        private val BENIGN_SETUP_KEYWORDS = listOf(
            "activate this device admin",
            "washa programu hii ya msimamizi",
            "all files access",
            "allow access to manage all files",
            "install unknown apps",
            "allow from this source",
            "allow restricted settings",
            "ruhusu mipangilio",
            "notifications",
            "arifa"
        )
    }
}
