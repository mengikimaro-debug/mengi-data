package tz.mengidata.app.backup

import android.app.job.JobParameters
import android.app.job.JobService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import tz.mengidata.app.data.db.MengiDatabase
import tz.mengidata.app.data.prefs.AppPreferences
import tz.mengidata.app.data.repository.FinanceRepository

/**
 * Native Android System JobService that runs automatic VaultLedger backups outside the Activity
 * lifecycle so backups still complete even if the user immediately closes or swipes the app away,
 * and periodically refreshes surviving phone storage + Android System BackupManager.
 */
class VaultAutoBackupJobService : JobService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var runningJob: Job? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        val jobParams = params ?: return false
        runningJob?.cancel()
        runningJob = serviceScope.launch {
            try {
                val appCtx = applicationContext
                val prefs = AppPreferences(appCtx)
                if (prefs.autoPhoneBackupEnabled.value) {
                    val db = MengiDatabase.getInstance(appCtx)
                    runCatching { db.checkpointWal() }
                    val repo = FinanceRepository(appCtx, db, prefs)
                    val saved = runCatching { repo.persistSafetySnapshot(force = false) }.getOrDefault(false)
                    if (saved) {
                        AndroidSystemBackupScheduler.markJobSyncCompleted(appCtx)
                    }
                    AndroidSystemBackupScheduler.notifyAndroidBackupManager(appCtx)
                }
            } finally {
                jobFinished(jobParams, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        runningJob?.cancel()
        return true // Reschedule if Android interrupted mid-backup
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
