package tz.mengidata.app.updater

import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class UpdateInfo(
    val version: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val assetName: String,
    val expectedSizeBytes: Long,
    val htmlReleaseUrl: String
)

data class DownloadProgress(
    val percent: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
    val etaSeconds: Long,
    val activeStreams: Int
)

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class UpToDate(val currentVersion: String) : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class PermissionRequired(val info: UpdateInfo) : UpdateUiState
    data class Downloading(
        val info: UpdateInfo,
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long = 0L,
        val etaSeconds: Long = -1L,
        val activeStreams: Int = 1
    ) : UpdateUiState
    data class Verifying(val info: UpdateInfo) : UpdateUiState
    data class Installing(
        val info: UpdateInfo,
        val apkPath: String? = null
    ) : UpdateUiState
    data class Success(val version: String) : UpdateUiState
    data class Error(
        val message: String,
        val retryInfo: UpdateInfo? = null,
        val cachedApkPath: String? = null,
        val isSignatureConflict: Boolean = false,
        val savedDownloadsName: String? = null
    ) : UpdateUiState
}

sealed interface InstallResultEvent {
    data class PendingUserAction(
        val confirmIntent: Intent,
        val cachedApkPath: String?
    ) : InstallResultEvent
    data object Success : InstallResultEvent
    data class Failure(
        val reason: String,
        val cachedApkPath: String? = null,
        val isSignatureConflict: Boolean = false,
        val shouldFallbackToDirectInstaller: Boolean = false
    ) : InstallResultEvent
}

object UpdateInstallEventBus {
    private val _events = MutableSharedFlow<InstallResultEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<InstallResultEvent> = _events.asSharedFlow()

    fun hasActiveSubscribers(): Boolean = _events.subscriptionCount.value > 0

    fun emit(event: InstallResultEvent) {
        _events.tryEmit(event)
    }
}
