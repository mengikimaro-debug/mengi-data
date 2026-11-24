package tz.mengidata.app.updater

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
        val totalBytes: Long
    ) : UpdateUiState
    data class Verifying(val info: UpdateInfo) : UpdateUiState
    data class Installing(val info: UpdateInfo) : UpdateUiState
    data class Success(val version: String) : UpdateUiState
    data class Error(val message: String, val retryInfo: UpdateInfo? = null) : UpdateUiState
}

sealed interface InstallResultEvent {
    data object PendingUserAction : InstallResultEvent
    data object Success : InstallResultEvent
    data class Failure(val reason: String) : InstallResultEvent
}

object UpdateInstallEventBus {
    private val _events = MutableSharedFlow<InstallResultEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<InstallResultEvent> = _events.asSharedFlow()

    fun emit(event: InstallResultEvent) {
        _events.tryEmit(event)
    }
}
