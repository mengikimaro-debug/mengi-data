package tz.mengidata.app.backup

import android.app.backup.BackupManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Coordinates native Android System automatic backup services:
 * 1. Android System `BackupManager.dataChanged()` -> triggers Google/Android OS Auto-Backup via `VaultAndroidBackupAgent`.
 * 2. Android System `JobScheduler` (`VaultAutoBackupJobService`) -> guarantees background backup completion even if the app is swiped away, plus persistent periodic 6-hour backups surviving device reboots.
 */
object AndroidSystemBackupScheduler {
    const val JOB_ID_IMMEDIATE_SYNC = 4101
    const val JOB_ID_PERIODIC_SYNC = 4102

    private const val META_PREFS = "vault_android_system_backup_meta"
    private const val KEY_LAST_BACKUP_MANAGER_SIGNAL_MS = "last_backup_manager_signal_ms"
    private const val KEY_LAST_JOB_SYNC_MS = "last_job_sync_ms"
    private const val KEY_LAST_SYSTEM_RESTORE_MS = "last_system_restore_ms"
    private const val PERIODIC_INTERVAL_MS = 6L * 60L * 60L * 1000L // Every 6 hours

    private fun metaPrefs(context: Context) =
        context.applicationContext.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)

    /**
     * Signals Android's BackupManagerService that app data changed and should be backed up
     * automatically to the user's Android System / Google Backup.
     */
    fun notifyAndroidBackupManager(context: Context) {
        val appCtx = context.applicationContext
        runCatching {
            BackupManager.dataChanged(appCtx.packageName)
            metaPrefs(appCtx).edit()
                .putLong(KEY_LAST_BACKUP_MANAGER_SIGNAL_MS, System.currentTimeMillis())
                .apply()
        }
    }

    /**
     * Schedules a fast, guaranteed Android System JobScheduler backup that survives
     * the user immediately swiping VaultLedger out of Recents.
     */
    fun scheduleImmediateBackup(context: Context, delayMs: Long = 1_200L) {
        val appCtx = context.applicationContext
        val scheduler = appCtx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
        val component = ComponentName(appCtx, VaultAutoBackupJobService::class.java)
        val job = JobInfo.Builder(JOB_ID_IMMEDIATE_SYNC, component)
            .setMinimumLatency(delayMs.coerceAtLeast(300L))
            .setOverrideDeadline((delayMs + 3_500L).coerceAtLeast(4_000L))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
            .build()
        runCatching { scheduler.schedule(job) }
    }

    /**
     * Ensures a persistent periodic Android System backup job is registered with JobScheduler
     * (survives phone reboots via RECEIVE_BOOT_COMPLETED).
     */
    fun ensurePeriodicBackupScheduled(context: Context): Boolean {
        val appCtx = context.applicationContext
        val scheduler = appCtx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return false
        val alreadyScheduled = scheduler.getPendingJob(JOB_ID_PERIODIC_SYNC) != null
        if (alreadyScheduled) return true

        val component = ComponentName(appCtx, VaultAutoBackupJobService::class.java)
        val job = JobInfo.Builder(JOB_ID_PERIODIC_SYNC, component)
            .setPeriodic(PERIODIC_INTERVAL_MS)
            .setPersisted(true)
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
            .build()
        return runCatching {
            scheduler.schedule(job) == JobScheduler.RESULT_SUCCESS
        }.getOrDefault(false)
    }

    fun isPeriodicJobScheduled(context: Context): Boolean {
        val scheduler = context.applicationContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
            ?: return false
        return runCatching { scheduler.getPendingJob(JOB_ID_PERIODIC_SYNC) != null }.getOrDefault(false)
    }

    fun markJobSyncCompleted(context: Context) {
        metaPrefs(context).edit()
            .putLong(KEY_LAST_JOB_SYNC_MS, System.currentTimeMillis())
            .apply()
    }

    fun markSystemRestoreCompleted(context: Context) {
        metaPrefs(context).edit()
            .putLong(KEY_LAST_SYSTEM_RESTORE_MS, System.currentTimeMillis())
            .apply()
    }

    fun getLastBackupManagerSignalMillis(context: Context): Long =
        metaPrefs(context).getLong(KEY_LAST_BACKUP_MANAGER_SIGNAL_MS, 0L)

    fun getLastJobSyncMillis(context: Context): Long =
        metaPrefs(context).getLong(KEY_LAST_JOB_SYNC_MS, 0L)

    fun getLastSystemRestoreMillis(context: Context): Long =
        metaPrefs(context).getLong(KEY_LAST_SYSTEM_RESTORE_MS, 0L)

    /**
     * Opens Android's native System Backup & Reset / Google Backup settings screen so the user
     * can confirm Android System Cloud Backup is enabled on their phone.
     */
    fun openAndroidSystemBackupSettings(context: Context): Boolean {
        val candidates = listOf(
            Intent("android.settings.BACKUP_AND_RESET_SETTINGS"),
            Intent(Settings.ACTION_PRIVACY_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val opened = runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
            if (opened) return true
        }
        return false
    }
}
