package tz.mengidata.app.data.db

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

data class PhoneStorageStatus(
    val hasInternalBackup: Boolean = false,
    val hasPhoneStorageBackup: Boolean = false,
    val hasPreClearBackup: Boolean = false,
    val lastSyncedMillis: Long = 0L,
    val phoneBackupPathLabel: String = "Documents/Mengidata"
) {
    val hasAnyBackup: Boolean
        get() = hasInternalBackup || hasPhoneStorageBackup || hasPreClearBackup
}

/**
 * Zero-permission durable phone storage mirror for the in-app Room SQLite database.
 * Keeps multi-layer backups so user records are never lost accidentally:
 * 1. Atomic rolling internal snapshots (latest + previous + pre-clear)
 * 2. App-specific external phone storage (Android/data/.../Documents/MengidataDatabase/)
 * 3. Public phone Documents/Mengidata/Mengidata_Database_Backup.json on Android 10+ via MediaStore
 *    (survives even app uninstall with zero dangerous storage permissions).
 */
class PhoneStorageVault(private val context: Context) {

    private fun internalLatestFile(): File = File(context.filesDir, INTERNAL_LATEST_FILE)
    private fun internalPrevFile(): File = File(context.filesDir, INTERNAL_PREV_FILE)
    private fun preClearFile(): File = File(context.filesDir, PRE_CLEAR_FILE)

    private fun externalBackupFile(): File? {
        val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return null
        val folder = File(docsDir, EXTERNAL_SUBDIR)
        if (!folder.exists()) folder.mkdirs()
        return File(folder, EXTERNAL_BACKUP_FILE)
    }

    fun getStatus(): PhoneStorageStatus {
        val internal = internalLatestFile()
        val external = externalBackupFile()
        val preClear = preClearFile()
        val hasInternal = internal.exists() && internal.length() > 10L
        val hasExternal = external != null && external.exists() && external.length() > 10L
        val hasPreClear = preClear.exists() && preClear.length() > 10L
        val latestMod = listOfNotNull(
            internal.takeIf { hasInternal }?.lastModified(),
            external?.takeIf { hasExternal }?.lastModified(),
            preClear.takeIf { hasPreClear }?.lastModified()
        ).maxOrNull() ?: 0L
        val pathLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Documents/Mengidata/$PUBLIC_BACKUP_FILE"
        } else {
            external?.absolutePath ?: "Phone Storage/Mengidata"
        }
        return PhoneStorageStatus(
            hasInternalBackup = hasInternal,
            hasPhoneStorageBackup = hasExternal || hasPublicMediaStoreBackup(),
            hasPreClearBackup = hasPreClear,
            lastSyncedMillis = latestMod,
            phoneBackupPathLabel = pathLabel
        )
    }

    fun saveAutoSnapshot(json: String): Boolean {
        var anySaved = false

        // 1. Atomic rolling internal snapshot (rotate latest -> prev before replacing)
        runCatching {
            val latest = internalLatestFile()
            val prev = internalPrevFile()
            if (latest.exists() && latest.length() > 10L) {
                val prevTmp = File(context.filesDir, "$INTERNAL_PREV_FILE.tmp")
                latest.copyTo(prevTmp, overwrite = true)
                if (prev.exists()) prev.delete()
                prevTmp.renameTo(prev)
            }
            val tmp = File(context.filesDir, "$INTERNAL_LATEST_FILE.tmp")
            tmp.writeText(json, Charsets.UTF_8)
            if (latest.exists()) latest.delete()
            if (tmp.renameTo(latest)) {
                anySaved = true
            }
        }

        // 2. App external phone storage mirror
        runCatching {
            val extFile = externalBackupFile()
            if (extFile != null) {
                val tmp = File(extFile.parentFile, "$EXTERNAL_BACKUP_FILE.tmp")
                tmp.writeText(json, Charsets.UTF_8)
                if (extFile.exists()) extFile.delete()
                if (tmp.renameTo(extFile)) {
                    anySaved = true
                }
            }
        }

        // 3. Public Phone Documents/Mengidata/ mirror on Android 10+ (zero permissions, survives uninstall)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                if (writePublicDocumentsBackup(json)) {
                    anySaved = true
                }
            }
        }

        return anySaved
    }

    fun savePreClearSnapshot(json: String) {
        runCatching {
            val target = preClearFile()
            val tmp = File(context.filesDir, "$PRE_CLEAR_FILE.tmp")
            tmp.writeText(json, Charsets.UTF_8)
            if (target.exists()) target.delete()
            tmp.renameTo(target)
        }
        // Also mirror to phone storage before clear
        runCatching {
            val extFile = externalBackupFile()?.parentFile?.let { File(it, PRE_CLEAR_FILE) }
            if (extFile != null) {
                extFile.writeText(json, Charsets.UTF_8)
            }
        }
    }

    fun readBestAvailableSnapshot(): String? {
        val candidates = listOfNotNull(
            internalLatestFile(),
            externalBackupFile(),
            preClearFile(),
            internalPrevFile()
        ).filter { it.exists() && it.length() > 10L }
            .sortedByDescending { it.lastModified() }

        for (file in candidates) {
            val text = runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
            if (!text.isNullOrBlank() && text.contains("\"transactions\"")) {
                return text
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val publicText = runCatching { readPublicDocumentsBackup() }.getOrNull()
            if (!publicText.isNullOrBlank() && publicText.contains("\"transactions\"")) {
                return publicText
            }
        }
        return null
    }

    private fun hasPublicMediaStoreBackup(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching { findPublicBackupUri() != null }.getOrDefault(false)
    }

    private fun findPublicBackupUri(): android.net.Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf(PUBLIC_BACKUP_FILE, "${Environment.DIRECTORY_DOCUMENTS}/$PUBLIC_FOLDER%")
        context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                return ContentUris.withAppendedId(collection, id)
            }
        }
        return null
    }

    private fun writePublicDocumentsBackup(json: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val resolver = context.contentResolver
        val existingUri = findPublicBackupUri()
        val targetUri = existingUri ?: run {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, PUBLIC_BACKUP_FILE)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/$PUBLIC_FOLDER")
            }
            resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
        } ?: return false

        resolver.openOutputStream(targetUri, "wt")?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
            out.flush()
            return true
        }
        return false
    }

    private fun readPublicDocumentsBackup(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val uri = findPublicBackupUri() ?: return null
        return context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    }

    companion object {
        private const val INTERNAL_LATEST_FILE = "mengidata_auto_snapshot.json"
        private const val INTERNAL_PREV_FILE = "mengidata_auto_snapshot_prev.json"
        private const val PRE_CLEAR_FILE = "mengidata_pre_clear_snapshot.json"
        private const val EXTERNAL_SUBDIR = "MengidataDatabase"
        private const val EXTERNAL_BACKUP_FILE = "mengidata_phone_backup.json"
        private const val PUBLIC_FOLDER = "Mengidata"
        private const val PUBLIC_BACKUP_FILE = "Mengidata_Database_Backup.json"
    }
}
