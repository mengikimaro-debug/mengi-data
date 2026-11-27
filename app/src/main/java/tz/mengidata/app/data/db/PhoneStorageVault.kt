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
import org.json.JSONArray
import org.json.JSONObject
import tz.mengidata.app.i18n.FormatUtils
import java.io.File
import java.io.FileOutputStream

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
    val phoneBackupPathLabel: String = "Documents/Mengidata/Mengidata_Database_Backup.json",
    val customFolderLabel: String = "",
    val isSyncingNow: Boolean = false
) {
    val hasAnyBackup: Boolean
        get() = hasInternalBackup || hasPhoneStorageBackup || hasPublicDocumentsBackup || hasCustomFolderBackup || hasPreClearBackup
}

private data class BackupCandidate(
    val json: String,
    val isRealUserData: Boolean,
    val timestampMillis: Long,
    val recordCount: Int,
    val sourceLabel: String
)

/**
 * Bulletproof multi-layer durable phone storage vault for VaultLedger (Android 13–16).
 *
 * Mirrors real user data automatically to:
 * 1. Atomic rolling internal snapshots (`latest` + `previous` + `pre-clear`)
 * 2. App-specific external phone storage (`Android/data/.../Documents/MengidataDatabase/` + 7-day history)
 * 3. Public `Documents/Mengidata/Mengidata_Database_Backup.json` + 14-day `History/` (survives uninstall!)
 * 4. Redundant Public `Download/VaultLedger/VaultLedger_AutoBackup.json` (survives uninstall!)
 * 5. Optional user-chosen SAF folder (`OpenDocumentTree` persistent URI permission)
 *
 * Safety guarantees:
 * - Unmodified Demo Data NEVER overwrites public phone backups.
 * - Atomic FUSE-safe writes never delete an existing backup unless the replacement is verified.
 * - Auto-restore scans ALL public/history/MediaStore/Downloads files and always prioritizes real user data over demo data.
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

    private fun publicVaultLedgerDocsDir(): File {
        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        return File(docs, PUBLIC_SECONDARY_FOLDER)
    }

    private fun publicVaultLedgerDownloadsDir(): File {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloads, PUBLIC_SECONDARY_FOLDER)
    }

    private fun publicBackupFileDirect(): File = File(publicMengidataRootDir(), PUBLIC_BACKUP_FILE)

    private fun publicSecondaryBackupFileDirect(): File =
        File(publicVaultLedgerDownloadsDir(), PUBLIC_SECONDARY_BACKUP_FILE)

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

        val directPublic = if (hasAllFilesAccess()) {
            listOf(publicBackupFileDirect(), publicSecondaryBackupFileDirect())
                .filter { it.exists() && it.length() > 10L }
                .maxByOrNull { it.lastModified() }
        } else {
            null
        }
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
            directPublic?.length(),
            internal.takeIf { hasInternal }?.length(),
            external?.takeIf { hasExternal }?.length()
        ).maxOrNull() ?: 0L

        val publicDailyCount = if (hasAllFilesAccess()) {
            publicHistoryDirDirect().listFiles { f -> f.isFile && f.name.endsWith(".json") }?.size ?: 0
        } else {
            0
        }
        val externalDailyCount = historyDir?.listFiles { f -> f.isFile && f.name.endsWith(".json") }?.size ?: 0
        val dailyCount = maxOf(publicDailyCount, externalDailyCount)

        val pathLabel = "Documents/$PUBLIC_FOLDER/$PUBLIC_BACKUP_FILE"

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

    /**
     * FUSE-safe atomic write that NEVER destroys an existing valid target file if renameTo fails
     * on Android's `/storage/emulated/0/` FUSE layer.
     */
    private fun writeFileAtomicallySafe(target: File, bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val parent = target.parentFile ?: return false
        if (!parent.exists()) parent.mkdirs()

        val tmp = File(parent, "${target.name}.tmp")
        val tmpWritten = runCatching {
            FileOutputStream(tmp, false).use { fos ->
                fos.write(bytes)
                fos.flush()
                runCatching { fos.fd.sync() }
            }
            tmp.exists() && tmp.length() == bytes.size.toLong()
        }.getOrDefault(false)

        if (tmpWritten) {
            if (tmp.renameTo(target) && target.exists() && target.length() == bytes.size.toLong()) {
                return true
            }
            // Fallback for Android FUSE filesystems where renameTo over an existing file returns false
            val directOk = runCatching {
                FileOutputStream(target, false).use { fos ->
                    fos.write(bytes)
                    fos.flush()
                    runCatching { fos.fd.sync() }
                }
                target.exists() && target.length() == bytes.size.toLong()
            }.getOrDefault(false)
            runCatching { tmp.delete() }
            if (directOk) return true
        }

        runCatching { tmp.delete() }
        return runCatching {
            FileOutputStream(target, false).use { fos ->
                fos.write(bytes)
                fos.flush()
                runCatching { fos.fd.sync() }
            }
            target.exists() && target.length() == bytes.size.toLong()
        }.getOrDefault(false)
    }

    fun saveAutoSnapshot(json: String, allowPublicOverwrite: Boolean = true): Boolean {
        _statusFlow.value = _statusFlow.value.copy(isSyncingNow = true)
        var anySaved = false
        val today = FormatUtils.todayIso()
        val payloadBytes = json.toByteArray(Charsets.UTF_8)
        val shouldWritePublic = allowPublicOverwrite && !isDemoOnlyBackupJson(json)

        try {
            // 1. Atomic rolling internal snapshot (rotate latest -> prev before replacing)
            runCatching {
                val latest = internalLatestFile()
                val prev = internalPrevFile()
                if (latest.exists() && latest.length() > 10L) {
                    val prevBytes = latest.readBytes()
                    writeFileAtomicallySafe(prev, prevBytes)
                }
                if (writeFileAtomicallySafe(latest, payloadBytes)) {
                    anySaved = true
                }
            }

            // 2. App external phone storage mirror + 7-day rolling daily history
            runCatching {
                val extFile = externalBackupFile()
                if (extFile != null && writeFileAtomicallySafe(extFile, payloadBytes)) {
                    anySaved = true
                }
                val histDir = externalHistoryDir()
                if (histDir != null) {
                    val dailyFile = File(histDir, "Mengidata_Backup_$today.json")
                    writeFileAtomicallySafe(dailyFile, payloadBytes)

                    val allDaily = histDir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
                        ?.sortedByDescending { it.name }
                        .orEmpty()
                    if (allDaily.size > MAX_DAILY_HISTORY) {
                        allDaily.drop(MAX_DAILY_HISTORY).forEach { it.delete() }
                    }
                }
            }

            // 3. Public Phone Storage mirrors (ONLY overwrite with REAL user data, NEVER unmodified demo data!)
            if (shouldWritePublic) {
                if (hasAllFilesAccess()) {
                    // Primary public mirror: Documents/Mengidata/Mengidata_Database_Backup.json + 14-day History
                    val primarySaved = runCatching {
                        val rootDir = publicMengidataRootDir().apply { if (!exists()) mkdirs() }
                        val target = File(rootDir, PUBLIC_BACKUP_FILE)
                        val ok = writeFileAtomicallySafe(target, payloadBytes)
                        if (ok) {
                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(target.absolutePath),
                                arrayOf("application/json"),
                                null
                            )
                        }

                        val histDir = publicHistoryDirDirect().apply { if (!exists()) mkdirs() }
                        val dailyTarget = File(histDir, "Mengidata_Backup_$today.json")
                        writeFileAtomicallySafe(dailyTarget, payloadBytes)
                        val allPublicDaily = histDir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
                            ?.sortedByDescending { it.name }
                            .orEmpty()
                        if (allPublicDaily.size > MAX_PUBLIC_DAILY_HISTORY) {
                            allPublicDaily.drop(MAX_PUBLIC_DAILY_HISTORY).forEach { it.delete() }
                        }
                        ok
                    }.getOrDefault(false)
                    if (primarySaved) anySaved = true

                    // Secondary redundant public mirror: Download/VaultLedger/VaultLedger_AutoBackup.json
                    val secondarySaved = runCatching {
                        val dlDir = publicVaultLedgerDownloadsDir().apply { if (!exists()) mkdirs() }
                        val target = File(dlDir, PUBLIC_SECONDARY_BACKUP_FILE)
                        val ok = writeFileAtomicallySafe(target, payloadBytes)
                        if (ok) {
                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(target.absolutePath),
                                arrayOf("application/json"),
                                null
                            )
                        }
                        ok
                    }.getOrDefault(false)
                    if (secondarySaved) anySaved = true
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

                        writePublicDocumentsFile(
                            relativePath = "${Environment.DIRECTORY_DOCUMENTS}/$PUBLIC_FOLDER/$PUBLIC_HISTORY_SUBFOLDER",
                            fileName = "Mengidata_Backup_$today.json",
                            fallbackName = "Mengidata_Backup_${today}_auto.json",
                            mimeType = "application/json",
                            bytes = payloadBytes
                        )

                        // Also mirror to public Downloads via MediaStore.Downloads
                        writePublicDownloadsFile(
                            relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$PUBLIC_SECONDARY_FOLDER",
                            fileName = PUBLIC_SECONDARY_BACKUP_FILE,
                            fallbackName = PUBLIC_SECONDARY_BACKUP_FALLBACK,
                            mimeType = "application/json",
                            bytes = payloadBytes
                        )
                    }
                }
            }

            // 4. Optional user-picked SAF folder mirror (e.g., SD card or custom Downloads folder)
            val customUri = customFolderTreeUri
            if (shouldWritePublic && customUri != null && hasPersistedTreePermission(customUri)) {
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
        if (isDemoOnlyBackupJson(json)) return
        val bytes = json.toByteArray(Charsets.UTF_8)
        runCatching { writeFileAtomicallySafe(preClearFile(), bytes) }
        runCatching {
            externalFolder()?.let { writeFileAtomicallySafe(File(it, PRE_CLEAR_FILE), bytes) }
        }
        if (hasAllFilesAccess()) {
            runCatching {
                val histDir = publicHistoryDirDirect().apply { if (!exists()) mkdirs() }
                writeFileAtomicallySafe(File(histDir, "Mengidata_PreClear_${FormatUtils.todayIso()}.json"), bytes)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

    fun savePdfToPublicDocuments(filename: String, pdfBytes: ByteArray): Uri? {
        if (pdfBytes.isEmpty()) return null
        val safeName = if (filename.endsWith(".pdf", ignoreCase = true)) filename else "$filename.pdf"

        if (hasAllFilesAccess()) {
            val directUri = runCatching {
                val dir = publicInvoicesDirDirect().apply { if (!exists()) mkdirs() }
                val target = File(dir, safeName)
                if (writeFileAtomicallySafe(target, pdfBytes)) {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(target.absolutePath),
                        arrayOf("application/pdf"),
                        null
                    )
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
                } else {
                    null
                }
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

    /**
     * Scans EVERY available backup location on the phone (Documents/Mengidata, History,
     * Download/VaultLedger, MediaStore, SAF folder, internal & external snapshots) and returns
     * the newest REAL user backup, strictly prioritizing real user data over unmodified demo snapshots.
     */
    fun readBestAvailableSnapshot(requireRealUserData: Boolean = false): String? {
        val candidates = mutableListOf<BackupCandidate>()

        fun considerPayload(text: String?, fileModMillis: Long, sourceLabel: String) {
            if (!isValidBackupPayload(text) || text == null) return
            val parsed = runCatching { JSONObject(text) }.getOrNull() ?: return
            val isReal = !isDemoOnlyJsonObject(parsed)
            val jsonTs = parsed.optLong("exportedAtMillis", 0L)
            val effectiveTs = maxOf(jsonTs, fileModMillis)
            val recordCount = countRecordsInBackup(parsed)
            candidates += BackupCandidate(
                json = text,
                isRealUserData = isReal,
                timestampMillis = effectiveTs,
                recordCount = recordCount,
                sourceLabel = sourceLabel
            )
        }

        // 1. Direct filesystem scan across ALL public surviving directories (when All Files Access is granted)
        if (hasAllFilesAccess()) {
            val publicDirs = listOf(
                publicMengidataRootDir(),
                publicHistoryDirDirect(),
                publicVaultLedgerDownloadsDir(),
                publicVaultLedgerDocsDir()
            )
            publicDirs.forEach { dir ->
                runCatching {
                    if (dir.exists() && dir.isDirectory) {
                        dir.listFiles { f ->
                            f.isFile && f.name.endsWith(".json", ignoreCase = true) && f.length() > 10L
                        }?.sortedByDescending { it.lastModified() }
                            ?.take(12)
                            ?.forEach { file ->
                                val text = runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
                                considerPayload(text, file.lastModified(), file.absolutePath)
                            }
                    }
                }
            }
        }

        // 2. Local & app-external snapshots
        val appFiles = buildList {
            add(internalLatestFile())
            externalBackupFile()?.let { add(it) }
            add(preClearFile())
            add(internalPrevFile())
            externalHistoryDir()?.listFiles { f -> f.isFile && f.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() }
                ?.take(5)
                ?.let { addAll(it) }
        }.filter { it.exists() && it.length() > 10L }

        appFiles.forEach { file ->
            val text = runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
            considerPayload(text, file.lastModified(), file.name)
        }

        // 3. Public MediaStore Files & Downloads scan (even if All Files Access not yet granted)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            readAllPublicMediaStoreBackups().forEach { (text, modMillis, name) ->
                considerPayload(text, modMillis, "MediaStore:$name")
            }
        }

        // 4. Optional user-picked Custom SAF folder
        val customUri = customFolderTreeUri
        if (customUri != null && hasPersistedTreePermission(customUri)) {
            val customText = runCatching { readFromCustomSafFolder(customUri, PUBLIC_BACKUP_FILE) }.getOrNull()
            considerPayload(customText, System.currentTimeMillis() - 1_000L, "SAF:$PUBLIC_BACKUP_FILE")
        }

        val filtered = if (requireRealUserData) {
            candidates.filter { it.isRealUserData }
        } else {
            candidates
        }
        if (filtered.isEmpty()) return null

        // Strictly prioritize real user data first, then newest timestamp, then richest record count
        val best = filtered.sortedWith(
            compareByDescending<BackupCandidate> { it.isRealUserData }
                .thenByDescending { it.timestampMillis }
                .thenByDescending { it.recordCount }
        ).firstOrNull()

        return best?.json
    }

    private fun countRecordsInBackup(root: JSONObject): Int {
        return (root.optJSONArray("transactions")?.length() ?: 0) +
            (root.optJSONArray("invoices")?.length() ?: 0) +
            (root.optJSONArray("bills")?.length() ?: 0) +
            (root.optJSONArray("savings")?.length() ?: 0) +
            (root.optJSONArray("pendingTransactions")?.length() ?: 0)
    }

    private fun isValidBackupPayload(text: String?): Boolean {
        return !text.isNullOrBlank() &&
            text.trimStart().startsWith("{") &&
            text.contains("\"transactions\"")
    }

    private fun hasAnyNonDemoItem(array: JSONArray?): Boolean {
        if (array == null) return false
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString("id", "")
            if (id.isNotBlank() && !id.startsWith("demo_")) return true
        }
        return false
    }

    private fun isDemoOnlyJsonObject(root: JSONObject): Boolean {
        val settings = root.optJSONObject("settings")
        val demoFlag = settings?.optBoolean("demo", false) ?: false
        if (!demoFlag) return false

        // Even if settings.demo wasn't cleared in an older version, if ANY real record exists, treat as real data!
        if (hasAnyNonDemoItem(root.optJSONArray("transactions"))) return false
        if (hasAnyNonDemoItem(root.optJSONArray("invoices"))) return false
        if (hasAnyNonDemoItem(root.optJSONArray("bills"))) return false
        if (hasAnyNonDemoItem(root.optJSONArray("savings"))) return false
        if (hasAnyNonDemoItem(root.optJSONArray("pendingTransactions"))) return false

        val owner = settings?.optString("ownerName", "")?.trim().orEmpty()
        val biz = settings?.optString("businessName", "")?.trim().orEmpty()
        if (owner.isNotEmpty()) return false
        if (biz.isNotEmpty() && !biz.equals("VaultLedger", ignoreCase = true) && !biz.equals("Mengidata", ignoreCase = true)) {
            return false
        }
        return true
    }

    fun isDemoOnlyBackupJson(text: String?): Boolean {
        if (!isValidBackupPayload(text) || text == null) return true
        val parsed = runCatching { JSONObject(text) }.getOrNull() ?: return true
        return isDemoOnlyJsonObject(parsed)
    }

    private fun queryPublicMediaStoreBackupInfo(): Pair<Uri, Long>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val relPath = "${Environment.DIRECTORY_DOCUMENTS}/$PUBLIC_FOLDER"
        return findPublicFileUriWithModified(relPath, PUBLIC_BACKUP_FILE)
            ?: findPublicFileUriWithModified(relPath, PUBLIC_BACKUP_FILE_FALLBACK)
            ?: findPublicDownloadUriWithModified("${Environment.DIRECTORY_DOWNLOADS}/$PUBLIC_SECONDARY_FOLDER", PUBLIC_SECONDARY_BACKUP_FILE)
    }

    private fun readAllPublicMediaStoreBackups(): List<Triple<String, Long, String>> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
        val results = mutableListOf<Triple<String, Long, String>>()
        val collections = listOf(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        )
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        val selection = "(${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? OR ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?) AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("Mengidata%", "VaultLedger%", "%.json")
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        for (collection in collections) {
            runCatching {
                context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val modIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    var count = 0
                    while (cursor.moveToNext() && count < 10) {
                        val id = cursor.getLong(idIdx)
                        val name = cursor.getString(nameIdx) ?: continue
                        val modMillis = cursor.getLong(modIdx) * 1000L
                        val uri = ContentUris.withAppendedId(collection, id)
                        val text = runCatching {
                            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        }.getOrNull()
                        if (!text.isNullOrBlank()) {
                            results += Triple(text, modMillis, name)
                            count++
                        }
                    }
                }
            }
        }
        return results
    }

    private fun findPublicFileUriWithModified(relativePath: String, fileName: String): Pair<Uri, Long>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return queryMediaStoreSingle(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            relativePath,
            fileName
        )
    }

    private fun findPublicDownloadUriWithModified(relativePath: String, fileName: String): Pair<Uri, Long>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return queryMediaStoreSingle(
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            relativePath,
            fileName
        )
    }

    private fun queryMediaStoreSingle(collection: Uri, relativePath: String, fileName: String): Pair<Uri, Long>? {
        return runCatching {
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
        val fallback = writePublicDocumentsFileReturningUri(relativePath, fallbackName, mimeType, bytes)
        return fallback != null
    }

    private fun writePublicDownloadsFile(
        relativePath: String,
        fileName: String,
        fallbackName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val primary = writeMediaStoreCollectionFile(collection, relativePath, fileName, mimeType, bytes)
        if (primary != null) return true
        val fallback = writeMediaStoreCollectionFile(collection, relativePath, fallbackName, mimeType, bytes)
        return fallback != null
    }

    private fun writePublicDocumentsFileReturningUri(
        relativePath: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        return writeMediaStoreCollectionFile(collection, relativePath, fileName, mimeType, bytes)
    }

    private fun writeMediaStoreCollectionFile(
        collection: Uri,
        relativePath: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Uri? {
        val resolver = context.contentResolver
        val existingUri = queryMediaStoreSingle(collection, relativePath, fileName)?.first

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

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val insertedUri = resolver.insert(collection, values) ?: return null

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
        private const val PUBLIC_SECONDARY_FOLDER = "VaultLedger"
        private const val PUBLIC_HISTORY_SUBFOLDER = "History"
        private const val PUBLIC_INVOICES_SUBFOLDER = "Ankara"
        private const val PUBLIC_BACKUP_FILE = "Mengidata_Database_Backup.json"
        private const val PUBLIC_BACKUP_FILE_FALLBACK = "Mengidata_Database_AutoBackup.json"
        private const val PUBLIC_SECONDARY_BACKUP_FILE = "VaultLedger_AutoBackup.json"
        private const val PUBLIC_SECONDARY_BACKUP_FALLBACK = "VaultLedger_AutoBackup_Mirror.json"
        private const val MAX_DAILY_HISTORY = 7
        private const val MAX_PUBLIC_DAILY_HISTORY = 14
    }
}
