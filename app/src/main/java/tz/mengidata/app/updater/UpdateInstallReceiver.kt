package tz.mengidata.app.updater

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import android.widget.Toast
import java.io.File

/**
 * Public BroadcastReceiver for Android PackageInstaller session results.
 * Hands STATUS_PENDING_USER_ACTION intents to the foreground Activity via UpdateInstallEventBus
 * to avoid Android 14+/15/16 Background Activity Launch (BAL) blocks, and triggers automatic
 * FileProvider ACTION_VIEW installer fallback when an OEM blocks PackageInstaller sessions.
 */
class UpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )
        val statusMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val cachedApkPath = intent.getStringExtra(EXTRA_CACHED_APK_PATH)

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmIntent != null) {
                    if (UpdateInstallEventBus.hasActiveSubscribers()) {
                        // Launch from foreground Activity context so Android 14/15/16 BAL never blocks it
                        UpdateInstallEventBus.emit(
                            InstallResultEvent.PendingUserAction(
                                confirmIntent = confirmIntent,
                                cachedApkPath = cachedApkPath
                            )
                        )
                    } else {
                        // App is backgrounded — attempt direct NEW_TASK launch as backup
                        confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        try {
                            context.startActivity(confirmIntent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Background confirmIntent launch failed", e)
                            UpdateInstallEventBus.emit(
                                InstallResultEvent.Failure(
                                    reason = "Imeshindwa kufungua dirisha la kusakinisha.",
                                    cachedApkPath = cachedApkPath,
                                    shouldFallbackToDirectInstaller = true
                                )
                            )
                        }
                    }
                } else {
                    UpdateInstallEventBus.emit(
                        InstallResultEvent.Failure(
                            reason = "Dirisha la uthibitisho wa Android halikupatikana.",
                            cachedApkPath = cachedApkPath,
                            shouldFallbackToDirectInstaller = true
                        )
                    )
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                cleanupApk(cachedApkPath)
                UpdateInstallEventBus.emit(InstallResultEvent.Success)
                Toast.makeText(context, "Update ya Mengidata imesakinishwa.", Toast.LENGTH_SHORT).show()
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                // Keep cached APK so user can tap Install again instantly without re-downloading
                UpdateInstallEventBus.emit(
                    InstallResultEvent.Failure(
                        reason = "Usakinishaji wa update umeghairiwa. Unaweza kubonyeza Sakinisha Tena bila kupakua upya.",
                        cachedApkPath = cachedApkPath,
                        shouldFallbackToDirectInstaller = false
                    )
                )
            }

            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                // Keep cached APK so MengiViewModel can preserve a copy in Downloads/Mengidata
                Log.e(TAG, "PackageInstaller conflict/incompatible: status=$status msg=$statusMessage")
                val osReason = statusMessage?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
                UpdateInstallEventBus.emit(
                    InstallResultEvent.Failure(
                        reason = "Android imezuia kusakinisha juu ya toleo la zamani$osReason kwa sababu saini ya toleo la zamani ilikuwa tofauti.",
                        cachedApkPath = cachedApkPath,
                        isSignatureConflict = true,
                        shouldFallbackToDirectInstaller = false
                    )
                )
            }

            PackageInstaller.STATUS_FAILURE_BLOCKED,
            PackageInstaller.STATUS_FAILURE_INVALID,
            PackageInstaller.STATUS_FAILURE -> {
                Log.e(TAG, "PackageInstaller failed (status=$status, msg=$statusMessage) — triggering direct APK installer fallback")
                val detail = statusMessage?.takeIf { it.isNotBlank() }
                    ?: "Usakinishaji wa mfumo umeshindwa (msimbo $status)."
                UpdateInstallEventBus.emit(
                    InstallResultEvent.Failure(
                        reason = detail,
                        cachedApkPath = cachedApkPath,
                        shouldFallbackToDirectInstaller = true
                    )
                )
            }

            else -> {
                val detail = statusMessage?.takeIf { it.isNotBlank() }
                    ?: "Usakinishaji umeshindwa (msimbo $status)."
                Log.e(TAG, "PackageInstaller failure: status=$status msg=$statusMessage")
                UpdateInstallEventBus.emit(
                    InstallResultEvent.Failure(
                        reason = detail,
                        cachedApkPath = cachedApkPath,
                        shouldFallbackToDirectInstaller = true
                    )
                )
            }
        }
    }

    private fun cleanupApk(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val file = File(path)
            if (file.exists()) file.delete()
        }
    }

    companion object {
        private const val TAG = "MengidataUpdater"
        const val ACTION_INSTALL_RESULT = "tz.mengidata.app.ACTION_UPDATE_INSTALL_RESULT"
        const val EXTRA_CACHED_APK_PATH = "tz.mengidata.app.extra.CACHED_APK_PATH"
    }
}
