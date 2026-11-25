package tz.mengidata.app.data.db

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tz.mengidata.app.i18n.FormatUtils
import java.io.File

data class SavedPublicPdf(
    val displayName: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val absolutePath: String? = null,
    val contentUri: Uri? = null,
    val locationLabel: String = "Documents/Mengidata/Ankara"
)

data class PhoneStorageStatus(
    val hasInternalBackup: Boolean = false,
    val hasPhoneStorageBackup: Boolean = false,
    val hasPublicDocumentsBackup: Boolean = false,
    val hasCustomFolderBackup: Boolean = false,
    val hasPreClearBackup: Boolean = false,
    val lastSyncedMillis: Long = 0L,
    val backupSizeBytes: Long = 0L,
    val dailySnapshotCount: Int = 0,
    val phoneBackupPathLabel: String = "Documents/Mengidata",
    val customFolderLabel: String = "",
    val isSyncingNow: Boolean = false
) {
    val hasAnyBackup: Boolean
        get() = hasInternalBackup || hasPhoneStorageBackup || hasPublicDocumentsBackup || hasCustomFolderBackup || hasPreClearBackup
}

/**
 * Zero-permission automatic durable phone storage vault for Mengidata (Android 13+ Scoped Storage).
 *
 * Automatically mirrors every database change to multiple resilient storage layers:
 * 1. Atomic rolling internal snapshots (`latest` + `previous` + `pre-clear`)
 * 2. App-specific external phone storage (`Android/data/.../Documents/MengidataDatabase/` + 7-day history)
 * 3. Public phone `Documents/Mengidata/Mengidata_Database_Backup.json` via MediaStore (`IS_PENDING` atomic commit)
 *    + daily rolling `Documents/Mengidata/History/Mengidata_Backup_YYYY-MM-DD.json` (survives app uninstall!)
 * 4. Optional user-chosen SAF folder (`OpenDocumentTree` persistent URI permission, e.g., SD card / custom folder)
 * 5. Clean `application/pdf` Invoice PDF mirror into `Documents/Mengidata/Ankara/` so saved PDFs always open cleanly.
 */
class PhoneStorageVault(private val context: Context) {

    @Volatile
    private var customFolderTreeUri: Uri? = null

    private val _statusFlow = MutableStateFlow(PhoneStorageStatus())
    val statusFlow: StateFlow<PhoneStorageStatus> = _statusFlow.asStateFlow()

    fun setCustomFolderUri(uriString: String) {
        customFolderTreeUri = uriString.takeIf { it.isNotBlank() }?.let { runCatching { Uri.parse(it) }.getOrNull() }
    }

    fun getStatus(): PhoneStorageStatus {
        val fresh = computeStatus(isSyncing = _statusFlow.value.isSyncingNow)
        _statusFlow.value = fresh
        return fresh
    }

    fun refreshStatus() {
        _statusFlow.value = computeStatus(isSyncing = _statusFlow.value.isSyncingNow)
    }

    private fun internalLatestFile(): File = File(context.filesDir, INTERNAL_LATEST_FILE)
    private fun internalPrevFile(): File = File(context.filesDir, INTERNAL_PREV_FILE)
    private fun preClearFile(): File = File(context.filesDir, PRE_CLEAR_FILE)

    private fun externalFolder(): File? {
        val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return null
        return File(docsDir, EXTERNAL_SUBDIR).apply { if (!exists()) mkdirs() }
    }

    private fun externalBackupFile(): File? = externalFolder()?.let { File(it, EXTERNAL_BACKUP_FILE) }

    private fun externalHistoryDir(): File? = externalFolder()?.let {
        File(it, "history").apply { if (!exists()) mkdirs() }
    }

    fun hasAllFilesAccess(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()
    }

    private fun publicMengidataRootDir(): File {
        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        return File(docs, PUBLIC_FOLDER)
    }

    private fun publicBackupFileDirect(): File = File(publicMengidataRootDir(), PUBLIC_BACKUP_FILE)

    private fun publicHistoryDirDirect(): File = File(publicMengidataRootDir(), PUBLIC_HISTORY_SUBFOLDER)

    private fun publicInvoicesDirDirect(): File = File(publicMengidataRootDir(), PUBLIC_INVOICES_SUBFOLDER)

    private fun computeStatus(isSyncing: Boolean): PhoneStorageStatus {
        val internal = internalLatestFile()
        val external = externalBackupFile()
        val preClear = preClearFile()
        val historyDir = externalHistoryDir()

        val hasInternal = internal.exists() && internal.length() > 10L
        val hasExternal = external != null && external.exists() && external.length() > 10L
        val hasPreClear = preClear.exists() && preClear.length() > 10L
        val directPublic = if (hasAllFilesAccess()) publicBackupFileDirect().takeIf { it.exists() && it.length() > 10L } else null
        val publicInfo = if (directPublic == null) queryPublicMediaStoreBackupInfo() else null
        val hasPublic = directPublic != null || publicInfo != null

        val customUri = customFolderTreeUri
        val hasCustom = customUri != null && hasPersistedTreePermission(customUri)
        val customLabel = if (hasCustom && customUri != null) formatTreeLabel(customUri) else ""

        val latestMod = listOfNotNull(
            internal.takeIf { hasInternal }?.lastModified(),
            external?.takeIf { hasExternal }?.lastModified(),
            directPublic?.lastModified(),
            publicInfo?.second?.takeIf { it > 0L },
            preClear.takeIf { hasPreClear }?.lastModified()
        ).maxOrNull() ?: 0L

        val sizeBytes = listOfNotNull(
            internal.takeIf { hasInternal }?.length(),
            external?.takeIf { hasExternal }?.length(),
            directPublic?.length()
        ).maxOrNull() ?: 0L

        val dailyCount = historyDir?.listFiles { f -> f.isFile && f.name.endsWith(".json") }?.size ?: 0

        val pathLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Documents/$PUBLIC_FOLDER/$PUBLIC_BACKUP_FILE"
        } else {
            external?.absolutePath ?: "Phone Storage/Mengidata"
        }

        return PhoneStorageStatus(
            hasInternalBackup = hasInternal,
            hasPhoneStorageBackup = hasExternal || hasPublic || hasCustom,
            hasPublicDocumentsBackup = hasPublic,
            hasCustomFolderBackup = hasCustom,
            hasPreClearBackup = hasPreClear,
            lastSyncedMillis = latestMod,
            backupSizeBytes = sizeBytes,
            dailySnapshotCount = dailyCount,
            phoneBackupPathLabel = pathLabel,
            customFolderLabel = customLabel,
            isSyncingNow = isSyncing
        )
    }

    fun saveAutoSnapshot(json: String): Boolean {
        _statusFlow.value = _statusFlow.value.copy(isSyncingNow = true)
        var anySaved = false
        val today = FormatUtils.todayIso()
        val payloadBytes = json.toByteArray(Charsets.UTF_8)

        try {
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
                tmp.writeBytes(payloadBytes)
                if (latest.exists()) latest.delete()
                if (tmp.renameTo(latest)) {
                    anySaved = true
                }
            }

            // 2. App external phone storage mirror + 7-day rolling daily history
            runCatching {
                val extFile = externalBackupFile()
                if (extFile != null) {
                    val tmp = File(extFile.parentFile, "$EXTERNAL_BACKUP_FILE.tmp")
                    tmp.writeBytes(payloadBytes)
                    if (extFile.exists()) extFile.delete()
                    if (tmp.renameTo(extFile)) {
                        anySaved = true
                    }
                }
                val histDir = externalHistoryDir()
                if (histDir != null) {
                    val dailyFile = File(histDir, "Mengidata_Backup_$today.json")
                    val dailyTmp = File(histDir, "Mengidata_Backup_$today.json.tmp")
                    dailyTmp.writeBytes(payloadBytes)
                    if (dailyFile.exists()) dailyFile.delete()
                    dailyTmp.renameTo(dailyFile)

                    // Keep only the newest 7 daily rolling snapshots
                    val allDaily = histDir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
                        ?.sortedByDescending { it.name }
                        .orEmpty()
                    if (allDaily.size > MAX_DAILY_HISTORY) {
                        allDaily.drop(MAX_DAILY_HISTORY).forEach { it.delete() }
                    }
                }
            }

            // 3. Public Phone Documents/Mengidata/ mirror (direct File I/O when MANAGE_EXTERNAL_STORAGE is granted, MediaStore fallback otherwise)
            if (hasAllFilesAccess()) {
                val directSaved = runCatching {
                    val rootDir = publicMengidataRootDir().apply { if (!exists()) mkdirs() }
                    val target = File(rootDir, PUBLIC_BACKUP_FILE)
                    val tmp = File(rootDir, "$PUBLIC_BACKUP_FILE.tmp")
                    tmp.writeBytes(payloadBytes)
                    if (target.exists()) target.delete()
                    val renamed = tmp.renameTo(target)
                    if (renamed) {
                        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("application/json"), null)
                    }

                    val histDir = publicHistoryDirDirect().apply { if (!exists()) mkdirs() }
                    val dailyTarget = File(histDir, "Mengidata_Backup_$today.json")
                    val dailyTmp = File(histDir, "Mengidata_Backup_$today.json.tmp")
                    dailyTmp.writeBytes(payloadBytes)
                    if (dailyTarget.exists()) dailyTarget.delete()
                    dailyTmp.renameTo(dailyTarget)
                    renamed
                }.getOrDefault(false)
                if (directSaved) anySaved = true
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching {
                    val okLatest = writePublicDocumentsFile(
                        relativePath = "${Environment.DIRECTORY_DOCUMENTS}/$PUBLIC_FOLDER",
                        fileName = PUBLIC_BACKUP_FILE,
                        fallbackName = PUBLIC_BACKUP_FILE_FALLBACK,
                        mimeType = "application/json",
                        bytes = payloadBytes
                    )
                    if (okLatest) anySaved = true

                    // Also keep today's daily snapshot in Documents/Mengidata/History/
                    writePublicDocumentsFile(
                        relativePath = "${Environment.DIRECTORY_DOCUMENTS}/$PUBLIC_FOLDER/$PUBLIC_HISTORY_SUBFOLDER",
                        fileName = "Mengidata_Backup_$today.json",
                        fallbackName = "Mengidata_Backup_${today}_auto.json",
                        mimeType = "application/json",
                        bytes = payloadBytes
                    )
                }
            }

            // 4. Optional user-picked SAF folder mirror (e.g., SD card or custom Downloads folder)
            val customUri = customFolderTreeUri
            if (customUri != null && hasPersistedTreePermission(customUri)) {
                runCatching {
                    if (writeToCustomSafFolder(customUri, PUBLIC_BACKUP_FILE, "application/json", payloadBytes)) {
                        anySaved = true
                    }
                }
            }
        } finally {
            _statusFlow.value = computeStatus(isSyncing = false)
        }
        return anySaved
    }

    fun savePreClearSnapshot(json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        runCatching {
            val target = preClearFile()
            val tmp = File(context.filesDir, "$PRE_CLEAR_FILE.tmp")
            tmp.writeBytes(bytes)
            if (target.exists()) target.delete()
            tmp.renameTo(target)
        }
        runCatching {
            val extFile = externalFolder()?.let { File(it, PRE_CLEAR_FILE) }
            extFile?.writeBytes(bytes)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                writePublicDocumentsFile(
                    relativePath = "${Environment.DIRECTORY_DOCUMENTS}/$PUBLIC_FOLDER/$PUBLIC_HISTORY_SUBFOLDER",
                    fileName = "Mengidata_PreClear_${FormatUtils.todayIso()}.json",
                    fallbackName = "Mengidata_PreClear_Latest.json",
                    mimeType = "application/json",
                    bytes = bytes
                )
            }
        }
        refreshStatus()
    }

    /**
     * Saves a clean, guaranteed-openable PDF file into `Documents/Mengidata/Ankara/`.
     * Uses direct public storage File I/O when `MANAGE_EXTERNAL_STORAGE` is granted (guaranteeing
     * full read/write/overwrite/retrieval across reinstalls) and falls back cleanly to MediaStore.
     */
    fun savePdfToPublicDocuments(filename: String, pdfBytes: ByteArray): Uri? {
        if (pdfBytes.isEmpty()) return null
        val safeName = if (filename.endsWith(".pdf", ignoreCase = true)) filename else "$filename.pdf"

        if (hasAllFilesAccess()) {
            val directUri = runCatching {
                val dir = publicInvoicesDirDirect().apply { if (!exists()) mkdirs() }
                val target = File(dir, safeName)
                val tmp = File(dir, "$safeName.tmp")
                tmp.writeBytes(pdfBytes)
                if (target.exists()) target.delete()
                if (!tmp.renameTo(target)) {
                    target.writeBytes(pdfBytes)
                    tmp.delete()
                }
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(target.absolutePath),
                    arrayOf("application/pdf"),
                    null
                )
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
            }.getOrNull()
            if (directUri != null) return directUri
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val relPath = "${Environment.DIRECTORY_DOCUMENTS}/$PUBLIC_FOLDER/$PUBLIC_INVOICES_SUBFOLDER"
        return runCatching {
            writePublicDocumentsFileReturningUri(
                relativePath = relPath,
                fileName = safeName,
                mimeType = "application/pdf",
                bytes = pdfBytes
            )
        }.getOrNull()
    }

    /**
     * Retrieves all saved PDF invoices from public device storage (`Documents/Mengidata/Ankara/`).
     * Combines direct filesystem scan (when MANAGE_EXTERNAL_STORAGE is granted) with MediaStore
     * query fallback so PDFs are always discoverable and openable.
     */
    fun listSavedPublicInvoicePdfs(): List<SavedPublicPdf> {
        val byName = LinkedHashMap<String, SavedPublicPdf>()

        if (hasAllFilesAccess()) {
            runCatching {
                val dir = publicInvoicesDirDirect()
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles { f -> f.isFile && f.name.endsWith(".pdf", ignoreCase = true) && f.length() > 0L }
                        ?.sortedByDescending { it.lastModified() }
                        ?.forEach { file ->
                            val uri = runCatching {
                                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            }.getOrNull()
                            byName[file.name] = SavedPublicPdf(
                                displayName = file.name,
                                sizeBytes = file.length(),
                                lastModifiedMillis = file.lastModified(),
                                absolutePath = file.absolutePath,
                                contentUri = uri,
                                locationLabel = "Documents/$PUBLIC_FOLDER/$PUBLIC_INVOICES_SUBFOLDER"
                            )
                        }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val projection = arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_MODIFIED
                )
                val relPath = "${Environment.DIRECTORY_DOCUMENTS}/$PUBLIC_FOLDER/$PUBLIC_INVOICES_SUBFOLDER"
                val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND (${MediaStore.MediaColumns.MIME_TYPE} = ? OR ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?)"
                val selectionArgs = arrayOf("$relPath%", "application/pdf", "%.pdf")
                val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

                context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val modIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameIdx) ?: continue
                        if (!byName.containsKey(name)) {
                            val id = cursor.getLong(idIdx)
                            val size = cursor.getLong(sizeIdx)
                            val modMillis = cursor.getLong(modIdx) * 1000L
                            val uri = ContentUris.withAppendedId(collection, id)
                            byName[name] = SavedPublicPdf(
                                displayName = name,
                                sizeBytes = size,
                                lastModifiedMillis = modMillis,
                                absolutePath = null,
                                contentUri = uri,
                                locationLabel = "Documents/$PUBLIC_FOLDER/$PUBLIC_INVOICES_SUBFOLDER"
                            )
                        }
                    }
                }
            }
        }

        return byName.values.sortedByDescending { it.lastModifiedMillis }
    }

    fun deleteSavedPublicPdf(pdf: SavedPublicPdf): Boolean {
        var deleted = false
        if (!pdf.absolutePath.isNullOrBlank() && hasAllFilesAccess()) {
            deleted = runCatching { File(pdf.absolutePath).delete() }.getOrDefault(false)
        }
        if (!deleted && pdf.contentUri != null && pdf.contentUri.scheme == "content") {
            deleted = runCatching {
                context.contentResolver.delete(pdf.contentUri, null, null) > 0
            }.getOrDefault(false)
        }
        return deleted
    }

    fun readBestAvailableSnapshot(): String? {
        // 1. Check local + external + rolling daily files sorted newest-first
        val localFiles = buildList {
            add(internalLatestFile())
            externalBackupFile()?.let { add(it) }
            add(preClearFile())
            add(internalPrevFile())
            if (hasAllFilesAccess()) {
                add(publicBackupFileDirect())
                publicHistoryDirDirect().listFiles { f -> f.isFile && f.name.endsWith(".json") }
                    ?.sortedByDescending { it.lastModified() }
                    ?.take(3)
                    ?.let { addAll(it) }
            }
            externalHistoryDir()?.listFiles { f -> f.isFile && f.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() }
                ?.take(3)
                ?.let { addAll(it) }
        }.filter { it.exists() && it.length() > 10L }
            .sortedByDescending { it.lastModified() }

        for (file in localFiles) {
            val text = runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
            if (isValidBackupPayload(text)) {
                return text
            }
        }

        // 2. Check Public Documents/Mengidata/ via MediaStore (survives app reinstall!)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val publicText = runCatching { readPublicDocumentsBackup() }.getOrNull()
            if (isValidBackupPayload(publicText)) {
                return publicText
            }
        }

        // 3. Check optional user-picked Custom SAF folder
        val customUri = customFolderTreeUri
        if (customUri != null && hasPersistedTreePermission(customUri)) {
            val customText = runCatching { readFromCustomSafFolder(customUri, PUBLIC_BACKUP_FILE) }.getOrNull()
            if (isValidBackupPayload(customText)) {
                return customText
            }
        }

        return null
    }

    private fun isValidBackupPayload(text: String?): Boolean {
        return !text.isNullOrBlank() &&
            text.trimStart().startsWith("{") &&
            text.contains("\"transactions\"")
    }

    private fun queryPublicMediaStoreBackupInfo(): Pair<Uri, Long>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val relPath = "${Environment.DIRECTORY_DOCUMENTS}/$PUBLIC_FOLDER"
        return findPublicFileUriWithModified(relPath, PUBLIC_BACKUP_FILE)
            ?: findPublicFileUriWithModified(relPath, PUBLIC_BACKUP_FILE_FALLBACK)
    }

    private fun findPublicFileUriWithModified(relativePath: String, fileName: String): Pair<Uri, Long>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DATE_MODIFIED
            )
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf(fileName, "$relativePath%")
            val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    val modSec = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED))
                    ContentUris.withAppendedId(collection, id) to (modSec * 1000L)
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun writePublicDocumentsFile(
        relativePath: String,
        fileName: String,
        fallbackName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val primary = writePublicDocumentsFileReturningUri(relativePath, fileName, mimeType, bytes)
        if (primary != null) return true
        // If previous install owns the primary file (SecurityException on Android 11+), write cleanly to fallback name
        val fallback = writePublicDocumentsFileReturningUri(relativePath, fallbackName, mimeType, bytes)
        return fallback != null
    }

    private fun writePublicDocumentsFileReturningUri(
        relativePath: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val resolver = context.contentResolver
        val existingUri = findPublicFileUriWithModified(relativePath, fileName)?.first

        if (existingUri != null) {
            val overwritten = runCatching {
                val out = resolver.openOutputStream(existingUri, "wt")
                    ?: resolver.openOutputStream(existingUri)
                    ?: return@runCatching false
                out.use {
                    it.write(bytes)
                    it.flush()
                }
                true
            }.getOrDefault(false)
            if (overwritten) return existingUri
        }

        // Atomic MediaStore insert using IS_PENDING = 1 -> write -> IS_PENDING = 0
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val insertedUri = resolver.insert(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            values
        ) ?: return null

        return try {
            val out = resolver.openOutputStream(insertedUri, "wt")
                ?: resolver.openOutputStream(insertedUri)
                ?: throw IllegalStateException("Cannot open MediaStore stream")
            out.use {
                it.write(bytes)
                it.flush()
            }
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(insertedUri, done, null, null)
            insertedUri
        } catch (_: Throwable) {
            runCatching { resolver.delete(insertedUri, null, null) }
            null
        }
    }

    private fun readPublicDocumentsBackup(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val uri = queryPublicMediaStoreBackupInfo()?.first ?: return null
        return context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    }

    private fun hasPersistedTreePermission(treeUri: Uri): Boolean {
        return context.contentResolver.persistedUriPermissions.any { perm ->
            perm.uri == treeUri && perm.isWritePermission && perm.isReadPermission
        }
    }

    private fun writeToCustomSafFolder(
        treeUri: Uri,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean {
        val resolver = context.contentResolver
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
        val existingDocUri = findChildDocumentUri(treeUri, treeDocId, fileName)

        val targetUri = existingDocUri ?: DocumentsContract.createDocument(
            resolver,
            parentDocUri,
            mimeType,
            fileName
        ) ?: return false

        val out = resolver.openOutputStream(targetUri, "wt")
            ?: resolver.openOutputStream(targetUri)
            ?: return false
        out.use {
            it.write(bytes)
            it.flush()
        }
        return true
    }

    private fun readFromCustomSafFolder(treeUri: Uri, fileName: String): String? {
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val childUri = findChildDocumentUri(treeUri, treeDocId, fileName) ?: return null
        return context.contentResolver.openInputStream(childUri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    }

    private fun findChildDocumentUri(treeUri: Uri, parentDocId: String, fileName: String): Uri? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )
        return runCatching {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    if (idIdx >= 0 && nameIdx >= 0 && cursor.getString(nameIdx) == fileName) {
                        val docId = cursor.getString(idIdx)
                        return@use DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    }
                }
                null
            }
        }.getOrNull()
    }

    private fun formatTreeLabel(treeUri: Uri): String {
        val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrDefault("")
        if (docId.isBlank()) return treeUri.lastPathSegment ?: "Phone Folder"
        return docId.replace("primary:", "Internal Storage/").replace(":", "/")
    }

    companion object {
        private const val INTERNAL_LATEST_FILE = "mengidata_auto_snapshot.json"
        private const val INTERNAL_PREV_FILE = "mengidata_auto_snapshot_prev.json"
        private const val PRE_CLEAR_FILE = "mengidata_pre_clear_snapshot.json"
        private const val EXTERNAL_SUBDIR = "MengidataDatabase"
        private const val EXTERNAL_BACKUP_FILE = "mengidata_phone_backup.json"
        private const val PUBLIC_FOLDER = "Mengidata"
        private const val PUBLIC_HISTORY_SUBFOLDER = "History"
        private const val PUBLIC_INVOICES_SUBFOLDER = "Ankara"
        private const val PUBLIC_BACKUP_FILE = "Mengidata_Database_Backup.json"
        private const val PUBLIC_BACKUP_FILE_FALLBACK = "Mengidata_Database_AutoBackup.json"
        private const val MAX_DAILY_HISTORY = 7
    }
}
