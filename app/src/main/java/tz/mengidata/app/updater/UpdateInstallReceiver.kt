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
 * Must be public so Android's ActivityThread can instantiate it when PackageInstaller commits.
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
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(confirmIntent)
                        UpdateInstallEventBus.emit(InstallResultEvent.PendingUserAction)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to launch install confirmation activity", e)
                        cleanupApk(cachedApkPath)
                        UpdateInstallEventBus.emit(
                            InstallResultEvent.Failure("Imeshindwa kufungua dirisha la kusakinisha.")
                        )
                    }
                } else {
                    cleanupApk(cachedApkPath)
                    UpdateInstallEventBus.emit(
                        InstallResultEvent.Failure("Dirisha la uthibitisho wa Android halikupatikana.")
                    )
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                cleanupApk(cachedApkPath)
                UpdateInstallEventBus.emit(InstallResultEvent.Success)
                Toast.makeText(context, "Update ya Mengidata imesakinishwa.", Toast.LENGTH_SHORT).show()
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                cleanupApk(cachedApkPath)
                UpdateInstallEventBus.emit(
                    InstallResultEvent.Failure("Usakinishaji wa update umeghairiwa.")
                )
            }

            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                cleanupApk(cachedApkPath)
                UpdateInstallEventBus.emit(
                    InstallResultEvent.Failure(
                        "Saini ya APK haifanani na toleo lililosakinishwa. Hifadhi backup kwanza."
                    )
                )
            }

            else -> {
                cleanupApk(cachedApkPath)
                val detail = statusMessage?.takeIf { it.isNotBlank() }
                    ?: "Usakinishaji umeshindwa (msimbo $status)."
                Log.e(TAG, "PackageInstaller failure: status=$status msg=$statusMessage")
                UpdateInstallEventBus.emit(InstallResultEvent.Failure(detail))
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
        const val EXTRA_CACHED_APK_PATH = "tz.mengidata.app.extra.CACHED_APK_PATH"
    }
}
