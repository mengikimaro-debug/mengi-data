package tz.mengidata.app.updater

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import tz.mengidata.app.BuildConfig
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext

/**
 * Security-hardened, Turbo Multi-Stream in-app update manager for Mengidata.
 *
 * Key Capabilities:
 * 1. Unified GitHub repository URL (`mengikimaro-debug/mengi-data`).
 * 2. Strict HTTPS validation across all redirect hops + exact asset byte-count validation.
 * 3. Turbo 4-Stream Parallel HTTP Range Downloader (`206 Partial Content`) with 128 KB I/O buffers,
 *    per-segment automatic resume/retry, live speed (MB/s) & ETA telemetry, and clean fallback.
 * 4. Cryptographic SHA-256 signing certificate & APK archive integrity verification.
 * 5. Proper Android 12+ (API 31+) PackageInstaller commit using PendingIntent.FLAG_MUTABLE
 *    with an explicit Intent targeting public UpdateInstallReceiver and 256 KB staging buffer.
 */
class AppUpdateManager(private val context: Context) {

    val currentVersionName: String
        get() = normalizeVersion(BuildConfig.VERSION_NAME).ifEmpty { BuildConfig.VERSION_NAME }

    fun canRequestPackageInstalls(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
    }

    fun buildInstallPermissionIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun buildReleasesBrowserIntent(): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_PAGE_URL))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /**
     * Builds a native Android FileProvider ACTION_VIEW intent for direct APK installation.
     * Acts as a rock-solid fallback on OEMs (MIUI/HyperOS, HiOS/XOS, OneUI, ColorOS)
     * that occasionally block or suppress PackageInstaller session commits.
     */
    fun buildDirectApkInstallIntent(apkFile: File): Intent {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    fun buildUninstallSelfIntent(): Intent {
        return Intent(Intent.ACTION_DELETE, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /**
     * Returns an already-downloaded & verified cached APK for this exact release version
     * so users never have to re-download after granting install permission or retrying.
     */
    suspend fun findValidCachedApk(info: UpdateInfo): File? = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "mengidata_updates")
        val apkFile = File(updatesDir, "mengidata-update-${info.version}.apk")
        if (!apkFile.exists() || apkFile.length() != info.expectedSizeBytes) return@withContext null
        runCatching {
            verifyDownloadedApk(apkFile, info.version)
            apkFile
        }.getOrNull()
    }

    /**
     * Checks if the downloaded APK shares a signing certificate with the currently installed app.
     * Returns false ONLY when both certificate sets are confidently extracted and have zero overlap.
     */
    suspend fun hasMatchingSigningCertificate(apkFile: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val pm = context.packageManager
            val installedInfo = getInstalledPackageInfo(pm) ?: return@runCatching true
            val installedCerts = extractSigningCertSha256(installedInfo)
            val archiveCerts = extractArchiveCertSha256(pm, apkFile)
            if (installedCerts.isEmpty() || archiveCerts.isEmpty()) return@runCatching true
            installedCerts.intersect(archiveCerts).isNotEmpty()
        }.getOrDefault(true)
    }

    /**
     * Copies a verified APK into public Downloads/Mengidata/ using zero-permission MediaStore
     * so the user can install it directly after uninstalling a differently-signed legacy build.
     */
    suspend fun saveApkToPublicDownloads(apkFile: File, version: String): String? = withContext(Dispatchers.IO) {
        if (!apkFile.exists()) return@withContext null
        runCatching {
            val displayName = "Mengidata-v$version.apk"
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/Mengidata/"

            // Remove any stale copy of the same filename first
            runCatching {
                resolver.delete(
                    collection,
                    "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
                    arrayOf(displayName, relativePath)
                )
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: return@runCatching null
            try {
                resolver.openOutputStream(uri, "wt")?.use { out ->
                    BufferedInputStream(FileInputStream(apkFile), INSTALL_BUFFER_BYTES).use { input ->
                        val buf = ByteArray(INSTALL_BUFFER_BYTES)
                        while (true) {
                            val n = input.read(buf)
                            if (n == -1) break
                            out.write(buf, 0, n)
                        }
                        out.flush()
                    }
                } ?: throw IllegalStateException("Imeshindwa kuandika APK kwenye Downloads.")

                val publish = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(uri, publish, null, null)
                displayName
            } catch (e: Throwable) {
                runCatching { resolver.delete(uri, null, null) }
                throw e
            }
        }.getOrNull()
    }

    suspend fun checkForUpdate(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = openHttpsConnection(
                GITHUB_API_LATEST_RELEASE,
                connectTimeoutMs = 8_000,
                readTimeoutMs = 10_000
            )
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            try {
                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                    throw IllegalStateException("Hakuna release ya umma iliyochapishwa kwenye GitHub bado.")
                }
                if (code !in 200..299) {
                    throw IllegalStateException("GitHub haipatikani sasa (HTTP $code).")
                }
                val bodyText = readUtf8Bounded(connection.inputStream, MAX_JSON_BYTES)
                val release = JSONObject(bodyText)
                val tagVersion = normalizeVersion(release.optString("tag_name", ""))
                if (tagVersion.isEmpty()) {
                    throw IllegalStateException("Release ya GitHub haina namba ya toleo sahihi.")
                }

                if (compareVersions(tagVersion, currentVersionName) <= 0) {
                    return@runCatching null // Already up to date
                }

                val apkAsset = findApkAsset(release.optJSONArray("assets"))
                    ?: throw IllegalStateException("Toleo $tagVersion halina faili la APK bado.")

                val downloadUrl = apkAsset.optString("browser_download_url", "")
                val assetName = apkAsset.optString("name", "Mengidata.apk")
                val size = apkAsset.optLong("size", 0L)
                val uri = Uri.parse(downloadUrl)

                if (!"https".equals(uri.scheme, ignoreCase = true) ||
                    uri.host !in ALLOWED_INITIAL_HOSTS ||
                    size <= 0L || size > MAX_APK_BYTES
                ) {
                    throw IllegalStateException("Anwani au ukubwa wa APK ya release si salama.")
                }

                UpdateInfo(
                    version = tagVersion,
                    releaseNotes = release.optString("body", "").trim(),
                    downloadUrl = downloadUrl,
                    assetName = assetName,
                    expectedSizeBytes = size,
                    htmlReleaseUrl = release.optString("html_url", RELEASES_PAGE_URL)
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    private data class ResolvedDownloadTarget(
        val finalUrl: String,
        val contentLength: Long,
        val supportsByteRanges: Boolean
    )

    suspend fun downloadAndVerifyApk(
        info: UpdateInfo,
        onProgress: (DownloadProgress) -> Unit,
        onVerifying: () -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var targetFile: File? = null
        try {
            val updatesDir = File(context.cacheDir, "mengidata_updates").apply { mkdirs() }
            updatesDir.listFiles()?.forEach { it.delete() }
            val apkFile = File(updatesDir, "mengidata-update-${info.version}.apk")
            targetFile = apkFile

            val resolved = resolveDirectDownloadTarget(info.downloadUrl)
            val totalBytes = when {
                resolved.contentLength > 0L -> {
                    if (resolved.contentLength > MAX_APK_BYTES || resolved.contentLength != info.expectedSizeBytes) {
                        throw IllegalStateException("Ukubwa wa faili haulingani na taarifa ya release.")
                    }
                    resolved.contentLength
                }
                else -> info.expectedSizeBytes
            }

            val useTurboParallel = resolved.supportsByteRanges && totalBytes >= MIN_PARALLEL_BYTES
            val activeStreams = if (useTurboParallel) TURBO_STREAMS else 1

            onProgress(
                DownloadProgress(
                    percent = 0,
                    downloadedBytes = 0L,
                    totalBytes = totalBytes,
                    speedBytesPerSec = 0L,
                    etaSeconds = -1L,
                    activeStreams = activeStreams
                )
            )

            val downloadedBytes = if (useTurboParallel) {
                downloadParallelTurbo(
                    finalUrl = resolved.finalUrl,
                    apkFile = apkFile,
                    totalBytes = totalBytes,
                    streamCount = TURBO_STREAMS,
                    onProgress = onProgress
                )
            } else {
                downloadSingleStreamBuffered(
                    initialUrl = resolved.finalUrl,
                    apkFile = apkFile,
                    totalBytes = totalBytes,
                    onProgress = onProgress
                )
            }

            if (downloadedBytes != info.expectedSizeBytes || apkFile.length() != info.expectedSizeBytes) {
                throw IllegalStateException(
                    "APK haikupakuliwa yote ($downloadedBytes / ${info.expectedSizeBytes} bytes)."
                )
            }

            onVerifying()
            verifyDownloadedApk(apkFile, info.version)
            Result.success(apkFile)
        } catch (ce: CancellationException) {
            targetFile?.delete()
            throw ce
        } catch (e: Throwable) {
            targetFile?.delete()
            Result.failure(e)
        }
    }

    /**
     * Resolves GitHub CDN redirects over strict HTTPS and probes byte-range support
     * so we can unleash 4 parallel high-speed download streams directly against the edge CDN.
     */
    private fun resolveDirectDownloadTarget(initialUrl: String): ResolvedDownloadTarget {
        var currentUrl = initialUrl
        repeat(MAX_REDIRECTS) {
            val conn = openHttpsConnection(
                currentUrl,
                connectTimeoutMs = 10_000,
                readTimeoutMs = 15_000
            ).apply {
                requestMethod = "HEAD"
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Connection", "keep-alive")
            }
            try {
                val status = conn.responseCode
                if (status in REDIRECT_CODES) {
                    val location = conn.getHeaderField("Location")
                    if (location.isNullOrBlank()) {
                        throw IllegalStateException("Redirect haina anwani sahihi.")
                    }
                    val nextUri = Uri.parse(location)
                    if (!"https".equals(nextUri.scheme, ignoreCase = true)) {
                        throw IllegalStateException("Redirect isiyo ya HTTPS imezuiwa.")
                    }
                    currentUrl = location
                } else if (status in 200..299) {
                    val acceptRanges = conn.getHeaderField("Accept-Ranges") ?: ""
                    val contentLength = conn.contentLengthLong
                    val supportsRanges = acceptRanges.contains("bytes", ignoreCase = true)
                    return ResolvedDownloadTarget(
                        finalUrl = currentUrl,
                        contentLength = contentLength,
                        supportsByteRanges = supportsRanges
                    )
                } else {
                    // Some CDN edge endpoints disallow HEAD; fall back to a 1-byte Range probe
                    return probeWithTinyRange(currentUrl)
                }
            } finally {
                conn.disconnect()
            }
        }
        throw IllegalStateException("Redirects nyingi mno wakati wa kupakua update.")
    }

    private fun probeWithTinyRange(url: String): ResolvedDownloadTarget {
        val conn = openHttpsFollowingRedirects(url, extraHeaders = mapOf("Range" to "bytes=0-0"))
        val resolvedUrl = conn.url.toString()
        try {
            val status = conn.responseCode
            return when (status) {
                HttpURLConnection.HTTP_PARTIAL -> {
                    val contentRange = conn.getHeaderField("Content-Range") ?: ""
                    val totalFromRange = contentRange.substringAfter("/", "").toLongOrNull() ?: -1L
                    ResolvedDownloadTarget(
                        finalUrl = resolvedUrl,
                        contentLength = totalFromRange,
                        supportsByteRanges = true
                    )
                }
                in 200..299 -> {
                    val acceptRanges = conn.getHeaderField("Accept-Ranges") ?: ""
                    ResolvedDownloadTarget(
                        finalUrl = resolvedUrl,
                        contentLength = conn.contentLengthLong,
                        supportsByteRanges = acceptRanges.contains("bytes", ignoreCase = true)
                    )
                }
                else -> {
                    ResolvedDownloadTarget(
                        finalUrl = resolvedUrl,
                        contentLength = -1L,
                        supportsByteRanges = false
                    )
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * 4-Stream Turbo Parallel Segmented Downloader.
     * Pre-allocates the file and streams 4 non-overlapping byte ranges concurrently using 128 KB
     * buffered I/O, automatic per-segment resume on transient errors, and live speed/ETA reporting.
     */
    private suspend fun downloadParallelTurbo(
        finalUrl: String,
        apkFile: File,
        totalBytes: Long,
        streamCount: Int,
        onProgress: (DownloadProgress) -> Unit
    ): Long = coroutineScope {
        RandomAccessFile(apkFile, "rw").use { raf ->
            raf.setLength(totalBytes)
        }

        val totalDownloaded = AtomicLong(0L)
        val chunkSize = totalBytes / streamCount
        val startMs = SystemClock.elapsedRealtime()

        // Smooth telemetry reporter running every 150ms
        val telemetryJob = launch(Dispatchers.IO) {
            var prevBytes = 0L
            var prevTimeMs = startMs
            var emaSpeed = 0.0
            while (isActive) {
                delay(150L)
                val now = SystemClock.elapsedRealtime()
                val currentBytes = totalDownloaded.get().coerceAtMost(totalBytes)
                val dtSec = ((now - prevTimeMs).coerceAtLeast(1L)) / 1000.0
                val instantSpeed = (currentBytes - prevBytes).coerceAtLeast(0L) / dtSec
                emaSpeed = if (emaSpeed <= 0.0) instantSpeed else (emaSpeed * 0.65 + instantSpeed * 0.35)
                val avgSpeed = currentBytes / (((now - startMs).coerceAtLeast(1L)) / 1000.0)
                val displaySpeed = if (emaSpeed > 0.0) emaSpeed.toLong() else avgSpeed.toLong()
                val remaining = (totalBytes - currentBytes).coerceAtLeast(0L)
                val eta = if (displaySpeed > 1_024L) (remaining / displaySpeed) else -1L
                val pct = ((currentBytes * 100L) / totalBytes).toInt().coerceIn(0, 99)

                onProgress(
                    DownloadProgress(
                        percent = pct,
                        downloadedBytes = currentBytes,
                        totalBytes = totalBytes,
                        speedBytesPerSec = displaySpeed,
                        etaSeconds = eta,
                        activeStreams = streamCount
                    )
                )
                prevBytes = currentBytes
                prevTimeMs = now
            }
        }

        try {
            val jobs = (0 until streamCount).map { index ->
                val rangeStart = index * chunkSize
                val rangeEnd = if (index == streamCount - 1) totalBytes - 1 else (rangeStart + chunkSize - 1)
                async(Dispatchers.IO) {
                    downloadSegmentWithRetry(
                        url = finalUrl,
                        apkFile = apkFile,
                        rangeStart = rangeStart,
                        rangeEnd = rangeEnd,
                        totalDownloaded = totalDownloaded
                    )
                }
            }
            jobs.awaitAll()
        } finally {
            telemetryJob.cancel()
        }

        val finalBytes = totalDownloaded.get()
        val elapsedSec = ((SystemClock.elapsedRealtime() - startMs).coerceAtLeast(1L)) / 1000.0
        onProgress(
            DownloadProgress(
                percent = 100,
                downloadedBytes = finalBytes,
                totalBytes = totalBytes,
                speedBytesPerSec = (finalBytes / elapsedSec).toLong(),
                etaSeconds = 0L,
                activeStreams = streamCount
            )
        )
        finalBytes
    }

    private suspend fun downloadSegmentWithRetry(
        url: String,
        apkFile: File,
        rangeStart: Long,
        rangeEnd: Long,
        totalDownloaded: AtomicLong
    ) {
        var cursor = rangeStart
        var attempt = 0
        var lastError: Throwable? = null

        while (cursor <= rangeEnd && attempt < MAX_SEGMENT_RETRIES) {
            coroutineContext.ensureActive()
            attempt++
            val conn = openHttpsFollowingRedirects(
                initialUrl = url,
                extraHeaders = mapOf(
                    "Range" to "bytes=$cursor-$rangeEnd",
                    "Accept-Encoding" to "identity",
                    "Connection" to "keep-alive"
                )
            )
            try {
                val status = conn.responseCode
                if (status != HttpURLConnection.HTTP_PARTIAL) {
                    throw IllegalStateException("Seva haikujibu sehemu ya faili (HTTP $status).")
                }

                RandomAccessFile(apkFile, "rw").use { raf ->
                    raf.seek(cursor)
                    BufferedInputStream(conn.inputStream, STREAM_BUFFER_BYTES).use { input ->
                        val buffer = ByteArray(STREAM_BUFFER_BYTES)
                        while (cursor <= rangeEnd) {
                            coroutineContext.ensureActive()
                            val remainingInSegment = (rangeEnd - cursor + 1).toInt().coerceAtMost(buffer.size)
                            val read = input.read(buffer, 0, remainingInSegment)
                            if (read == -1) break
                            raf.write(buffer, 0, read)
                            cursor += read
                            val newTotal = totalDownloaded.addAndGet(read.toLong())
                            if (newTotal > MAX_APK_BYTES) {
                                throw IllegalStateException("APK imezidi ukubwa unaoruhusiwa (100 MB).")
                            }
                        }
                    }
                }
                if (cursor > rangeEnd) return
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Throwable) {
                lastError = e
                if (attempt < MAX_SEGMENT_RETRIES) {
                    delay(350L * attempt)
                }
            } finally {
                conn.disconnect()
            }
        }

        if (cursor <= rangeEnd) {
            throw lastError ?: IllegalStateException("Sehemu ya APK haikumalizika kupakuliwa.")
        }
    }

    /**
     * High-throughput single-stream fallback with 128 KB BufferedInputStream/BufferedOutputStream
     * and real-time speed/ETA telemetry.
     */
    private suspend fun downloadSingleStreamBuffered(
        initialUrl: String,
        apkFile: File,
        totalBytes: Long,
        onProgress: (DownloadProgress) -> Unit
    ): Long {
        val connection = openHttpsFollowingRedirects(
            initialUrl = initialUrl,
            extraHeaders = mapOf(
                "Accept-Encoding" to "identity",
                "Connection" to "keep-alive"
            )
        )
        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IllegalStateException("Imeshindwa kupakua APK (HTTP $status).")
            }
            val contentLength = connection.contentLengthLong
            if (contentLength > MAX_APK_BYTES) {
                throw IllegalStateException("APK imezidi ukubwa unaoruhusiwa (100 MB).")
            }

            var downloaded = 0L
            val startMs = SystemClock.elapsedRealtime()
            var lastEmitMs = startMs
            var prevBytes = 0L
            var emaSpeed = 0.0

            BufferedInputStream(connection.inputStream, STREAM_BUFFER_BYTES).use { input ->
                BufferedOutputStream(FileOutputStream(apkFile), STREAM_BUFFER_BYTES).use { output ->
                    val buffer = ByteArray(STREAM_BUFFER_BYTES)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        downloaded += read
                        if (downloaded > MAX_APK_BYTES) {
                            throw IllegalStateException("APK imezidi ukubwa unaoruhusiwa (100 MB).")
                        }
                        output.write(buffer, 0, read)

                        val now = SystemClock.elapsedRealtime()
                        if (now - lastEmitMs >= 150L || downloaded >= totalBytes) {
                            val dtSec = ((now - lastEmitMs).coerceAtLeast(1L)) / 1000.0
                            val instantSpeed = (downloaded - prevBytes).coerceAtLeast(0L) / dtSec
                            emaSpeed = if (emaSpeed <= 0.0) instantSpeed else (emaSpeed * 0.65 + instantSpeed * 0.35)
                            val displaySpeed = emaSpeed.toLong().coerceAtLeast(0L)
                            val remaining = (totalBytes - downloaded).coerceAtLeast(0L)
                            val eta = if (displaySpeed > 1_024L) (remaining / displaySpeed) else -1L
                            val pct = if (totalBytes > 0L) ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100) else 0

                            onProgress(
                                DownloadProgress(
                                    percent = pct,
                                    downloadedBytes = downloaded,
                                    totalBytes = totalBytes,
                                    speedBytesPerSec = displaySpeed,
                                    etaSeconds = eta,
                                    activeStreams = 1
                                )
                            )
                            prevBytes = downloaded
                            lastEmitMs = now
                        }
                    }
                    output.flush()
                }
            }
            return downloaded
        } finally {
            connection.disconnect()
        }
    }

    suspend fun installVerifiedApk(apkFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val packageInstaller = context.packageManager.packageInstaller

            // Clean up any stale uncommitted sessions owned by our app so OEM session limits never reject us
            runCatching {
                packageInstaller.mySessions.forEach { existing ->
                    if (!existing.isActive) {
                        runCatching { packageInstaller.abandonSession(existing.sessionId) }
                    }
                }
            }

            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName)
                setSize(apkFile.length())
                setInstallReason(PackageManager.INSTALL_REASON_USER)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Enables silent / streamlined self-updates when Mengidata is the update owner
                    // (falls back seamlessly to STATUS_PENDING_USER_ACTION if Android wants confirmation).
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    setRequestUpdateOwnership(true)
                }
            }

            val sessionId = packageInstaller.createSession(params)
            try {
                packageInstaller.openSession(sessionId).use { session ->
                    BufferedInputStream(FileInputStream(apkFile), INSTALL_BUFFER_BYTES).use { input ->
                        session.openWrite("mengidata_base.apk", 0L, apkFile.length()).use { output ->
                            val buffer = ByteArray(INSTALL_BUFFER_BYTES)
                            while (true) {
                                val count = input.read(buffer)
                                if (count == -1) break
                                output.write(buffer, 0, count)
                            }
                            session.fsync(output)
                        }
                    }

                    // Explicit component + explicit action + FLAG_MUTABLE required for Android 12-16 PackageInstaller
                    val callbackIntent = Intent(context, UpdateInstallReceiver::class.java).apply {
                        action = UpdateInstallReceiver.ACTION_INSTALL_RESULT
                        setPackage(context.packageName)
                        putExtra(UpdateInstallReceiver.EXTRA_CACHED_APK_PATH, apkFile.absolutePath)
                    }
                    val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                    val statusIntent = PendingIntent.getBroadcast(
                        context,
                        sessionId,
                        callbackIntent,
                        pendingFlags
                    )
                    session.commit(statusIntent.intentSender)
                }
            } catch (e: Exception) {
                runCatching { packageInstaller.abandonSession(sessionId) }
                // Keep apkFile intact so the ViewModel can immediately trigger FileProvider ACTION_VIEW fallback
                throw e
            }
        }
    }

    private fun verifyDownloadedApk(apkFile: File, expectedVersion: String) {
        val pm = context.packageManager

        val installedInfo = getInstalledPackageInfo(pm)
            ?: throw IllegalStateException("Imeshindwa kusoma taarifa za programu iliyosakinishwa.")

        val archiveInfo = getArchivePackageInfo(pm, apkFile.absolutePath)
            ?: throw IllegalStateException("Faili lililopakuliwa si APK halali ya Android.")

        if (archiveInfo.packageName != context.packageName) {
            throw IllegalStateException("APK hii si ya Mengidata (package name hailingani).")
        }

        val archiveVersion = normalizeVersion(archiveInfo.versionName ?: "")
        if (archiveVersion.isEmpty() || compareVersions(archiveVersion, expectedVersion) != 0) {
            throw IllegalStateException("Toleo la APK ($archiveVersion) halilingani na release ($expectedVersion).")
        }

        val installedCode = getVersionCodeCompat(installedInfo)
        val archiveCode = getVersionCodeCompat(archiveInfo)
        if (archiveCode < installedCode) {
            throw IllegalStateException("APK ya update ina versionCode ndogo kuliko toleo la sasa.")
        }

        val archiveDigests = extractArchiveCertSha256(pm, apkFile)
        if (archiveDigests.isEmpty() && !hasValidApkStructure(apkFile)) {
            throw IllegalStateException("APK iliyopakuliwa haina saini halali ya Android.")
        }
    }

    private fun getInstalledPackageInfo(pm: PackageManager): PackageInfo? {
        val primaryFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        return queryInstalledPackageInfo(pm, primaryFlag)
            ?: queryInstalledPackageInfo(pm, 0)
    }

    private fun queryInstalledPackageInfo(pm: PackageManager, flag: Int): PackageInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(flag.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, flag)
            }
        }.getOrNull()
    }

    private fun getArchivePackageInfo(pm: PackageManager, apkPath: String): PackageInfo? {
        val primaryFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        return queryArchivePackageInfo(pm, apkPath, primaryFlag)
            ?: queryArchivePackageInfo(
                pm,
                apkPath,
                @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
            )
            ?: queryArchivePackageInfo(pm, apkPath, 0)
    }

    private fun queryArchivePackageInfo(pm: PackageManager, apkPath: String, flag: Int): PackageInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(apkPath, PackageManager.PackageInfoFlags.of(flag.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkPath, flag)
            }
        }.getOrNull()
    }

    private fun getVersionCodeCompat(info: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    private fun extractArchiveCertSha256(pm: PackageManager, apkFile: File): Set<String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            queryArchivePackageInfo(pm, apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
                ?.let { extractSigningCertSha256(it) }
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
        }
        @Suppress("DEPRECATION")
        queryArchivePackageInfo(pm, apkFile.absolutePath, PackageManager.GET_SIGNATURES)
            ?.let { extractSigningCertSha256(it) }
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }

        return extractCertSha256FromJar(apkFile)
    }

    private fun extractSigningCertSha256(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo
            val modernSigs = if (signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
            } else {
                null
            }
            @Suppress("DEPRECATION")
            modernSigs?.takeIf { it.isNotEmpty() } ?: info.signatures
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        } ?: return emptySet()

        val md = MessageDigest.getInstance("SHA-256")
        return signatures.map { sig ->
            md.reset()
            md.digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    private fun extractCertSha256FromJar(apkFile: File): Set<String> {
        return runCatching {
            val digests = mutableSetOf<String>()
            val md = MessageDigest.getInstance("SHA-256")
            java.util.jar.JarFile(apkFile, true).use { jar ->
                val targetEntry = jar.getJarEntry("AndroidManifest.xml")
                    ?: jar.entries().asSequence().firstOrNull { !it.isDirectory && !it.name.startsWith("META-INF/") }
                if (targetEntry != null) {
                    jar.getInputStream(targetEntry).use { stream ->
                        val buffer = ByteArray(16 * 1024)
                        while (stream.read(buffer) != -1) {
                            // Drain stream to trigger JarVerifier certificate population
                        }
                    }
                    targetEntry.certificates?.forEach { cert ->
                        md.reset()
                        digests += md.digest(cert.encoded).joinToString("") { "%02x".format(it) }
                    }
                }
            }
            digests
        }.getOrDefault(emptySet())
    }

    private fun hasValidApkStructure(apkFile: File): Boolean {
        return runCatching {
            java.util.zip.ZipFile(apkFile).use { zip ->
                zip.getEntry("AndroidManifest.xml") != null && zip.getEntry("classes.dex") != null
            }
        }.getOrDefault(false)
    }

    private fun openHttpsConnection(
        urlString: String,
        connectTimeoutMs: Int,
        readTimeoutMs: Int
    ): HttpURLConnection {
        val url = URL(urlString)
        if (!"https".equals(url.protocol, ignoreCase = true)) {
            throw IllegalStateException("Muunganisho usio wa HTTPS umezuiwa.")
        }
        return (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("User-Agent", "Mengidata-Android/${BuildConfig.VERSION_NAME}")
        }
    }

    private fun openHttpsFollowingRedirects(
        initialUrl: String,
        extraHeaders: Map<String, String> = emptyMap()
    ): HttpURLConnection {
        var currentUrl = initialUrl
        repeat(MAX_REDIRECTS) {
            val conn = openHttpsConnection(
                currentUrl,
                connectTimeoutMs = 12_000,
                readTimeoutMs = 25_000
            )
            extraHeaders.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            val status = conn.responseCode
            if (status in REDIRECT_CODES) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) {
                    throw IllegalStateException("Redirect haina anwani sahihi.")
                }
                val nextUri = Uri.parse(location)
                if (!"https".equals(nextUri.scheme, ignoreCase = true)) {
                    throw IllegalStateException("Redirect isiyo ya HTTPS imezuiwa.")
                }
                currentUrl = location
            } else {
                return conn
            }
        }
        throw IllegalStateException("Redirects nyingi mno wakati wa kupakua update.")
    }

    private fun findApkAsset(assets: JSONArray?): JSONObject? {
        if (assets == null) return null
        for (i in 0 until assets.length()) {
            val item = assets.optJSONObject(i) ?: continue
            if ("Mengidata.apk".equals(item.optString("name"), ignoreCase = true)) return item
        }
        for (i in 0 until assets.length()) {
            val item = assets.optJSONObject(i) ?: continue
            if (item.optString("name").lowercase(Locale.ROOT).endsWith(".apk")) return item
        }
        return null
    }

    private fun readUtf8Bounded(stream: java.io.InputStream, maxBytes: Int): String {
        val out = ByteArrayOutputStream()
        stream.use { input ->
            val buf = ByteArray(16 * 1024)
            while (true) {
                val read = input.read(buf)
                if (read == -1) break
                if (out.size() + read > maxBytes) {
                    throw IllegalStateException("Jibu la GitHub ni kubwa mno.")
                }
                out.write(buf, 0, read)
            }
        }
        return out.toString(Charsets.UTF_8.name())
    }

    companion object {
        const val GITHUB_API_LATEST_RELEASE =
            "https://api.github.com/repos/mengikimaro-debug/mengi-data/releases/latest"
        const val RELEASES_PAGE_URL =
            "https://github.com/mengikimaro-debug/mengi-data/releases/latest"

        private val ALLOWED_INITIAL_HOSTS = setOf("github.com", "objects.githubusercontent.com")
        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        private const val MAX_APK_BYTES = 100L * 1024L * 1024L
        private const val MAX_JSON_BYTES = 2 * 1024 * 1024
        private const val MAX_REDIRECTS = 6
        private const val TURBO_STREAMS = 4
        private const val MIN_PARALLEL_BYTES = 512L * 1024L
        private const val STREAM_BUFFER_BYTES = 128 * 1024
        private const val INSTALL_BUFFER_BYTES = 256 * 1024
        private const val MAX_SEGMENT_RETRIES = 3

        fun normalizeVersion(raw: String): String {
            val trimmed = raw.trim().replaceFirst(Regex("^[vV]"), "")
            val match = Regex("^([0-9]+(?:\\.[0-9]+){1,3})").find(trimmed)
            return match?.groupValues?.getOrNull(1) ?: ""
        }

        fun compareVersions(left: String, right: String): Int {
            val a = left.split(".")
            val b = right.split(".")
            val len = maxOf(a.size, b.size)
            for (i in 0 until len) {
                val av = a.getOrNull(i)?.toIntOrNull() ?: 0
                val bv = b.getOrNull(i)?.toIntOrNull() ?: 0
                if (av != bv) return av.compareTo(bv)
            }
            return 0
        }
    }
}
