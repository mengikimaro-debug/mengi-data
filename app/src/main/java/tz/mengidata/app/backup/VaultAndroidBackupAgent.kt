package tz.mengidata.app.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.os.ParcelFileDescriptor
import tz.mengidata.app.data.db.MengiDatabase

/**
 * Native Android System BackupAgent (`android:fullBackupOnly="true"`).
 *
 * - Before Android System archives `mengidata_room.db` + `mengidata_auto_snapshot.json`
 *   to Android Cloud Backup / Device-to-Device transfer, flushes Room SQLite WAL
 *   (`PRAGMA wal_checkpoint(TRUNCATE)`) so every recent transaction is guaranteed consistent.
 * - Automatically fires `onRestoreFinished()` immediately upon APK reinstall (even before
 *   the user taps the app icon) and records the restore event so VaultLedger seamlessly
 *   validates and displays all restored data.
 */
class VaultAndroidBackupAgent : BackupAgent() {

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?
    ) {
        // Unused: fullBackupOnly="true" routes all backups through onFullBackup().
    }

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?
    ) {
        // Unused: fullBackupOnly="true" routes restores through onRestoreFile() & onRestoreFinished().
    }

    override fun onFullBackup(data: FullBackupDataOutput?) {
        runCatching {
            MengiDatabase.getInstance(applicationContext).checkpointWal()
        }
        super.onFullBackup(data)
    }

    override fun onRestoreFinished() {
        super.onRestoreFinished()
        runCatching {
            AndroidSystemBackupScheduler.markSystemRestoreCompleted(applicationContext)
            AndroidSystemBackupScheduler.ensurePeriodicBackupScheduled(applicationContext)
        }
    }
}
