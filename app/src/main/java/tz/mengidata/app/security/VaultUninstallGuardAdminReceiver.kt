package tz.mengidata.app.security

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import tz.mengidata.app.data.prefs.AppPreferences
import tz.mengidata.app.i18n.tr

/**
 * Android Enterprise Device Owner & Device Administrator receiver.
 *
 * - In Device Owner mode (provisioned via ADB `dpm set-device-owner`, QR, or Zero-Touch):
 *   Automatically enforces `setUninstallBlocked(pkg, true)` and `DISALLOW_UNINSTALL_APPS`,
 *   making the app impossible to uninstall by the user from Launcher, App Info, or System Settings.
 * - In standard Device Admin mode:
 *   Blocks standard uninstallation until the owner verifies their Fingerprint inside VaultLedger.
 */
class VaultUninstallGuardAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        AppPreferences(context).setUninstallProtectionPreferred(true)
        if (UninstallProtectionManager.isDeviceOwner(context)) {
            UninstallProtectionManager.enforceDeviceOwnerUninstallBlock(context, blocked = true)
        }
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        AppPreferences(context).setUninstallProtectionPreferred(true)
        if (UninstallProtectionManager.isDeviceOwner(context)) {
            UninstallProtectionManager.enforceDeviceOwnerUninstallBlock(context, blocked = true)
        }
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        val lang = AppPreferences(context).language.value
        return tr(
            lang,
            "Ulinzi wa Alama ya Kidole wa VaultLedger umewashwa. Ili kuondoa app hii kwa usalama, thibitisha Alama ya Kidole (Fingerprint) ndani ya VaultLedger > Mipangilio > Usalama.",
            "VaultLedger Fingerprint Uninstall Protection is active. Please verify your Fingerprint inside VaultLedger > Settings > Security before removing this app."
        )
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        AppPreferences(context).setUninstallProtectionPreferred(false)
    }
}
