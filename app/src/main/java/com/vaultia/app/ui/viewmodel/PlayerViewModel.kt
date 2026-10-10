package com.vaultia.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultia.app.data.repository.VaultRepository
import com.vaultia.app.domain.model.VaultFile
import com.vaultia.app.player.VideoPlayerManager
import com.vaultia.app.player.VideoPlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import com.vaultia.app.engine.transfer.DownloadManager
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val repository: VaultRepository,
    val playerManager: VideoPlayerManager,
    private val downloadManager: DownloadManager
) : ViewModel() {

    val playerState: StateFlow<VideoPlayerState> = playerManager.playerState

    private val _currentFile = MutableStateFlow<VaultFile?>(null)
    val currentFile: StateFlow<VaultFile?> = _currentFile.asStateFlow()

    fun downloadCurrentFile(): Boolean {
        val file = _currentFile.value ?: return false
        downloadManager.enqueueDownload(file)
        return true
    }

    private var progressJob: Job? = null

    fun loadMedia(fileId: String) {
        viewModelScope.launch {
            val file = repository.getFileById(fileId)
            _currentFile.value = file
            if (file != null) {
                repository.prewarmMedia(file)
            }
            val streamUrl = repository.getStreamUrl(fileId)
            playerManager.prepareStream(streamUrl)
            startProgressPolling()
        }
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                playerManager.updateProgress()
                delay(500)
            }
        }
    }

    fun togglePlayPause() = playerManager.togglePlayPause()
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)
    fun seekForward() = playerManager.seekForward(10000L)
    fun seekBackward() = playerManager.seekBackward(10000L)
    fun setSpeed(speed: Float) = playerManager.setPlaybackSpeed(speed)

    fun stopPlayback() {
        progressJob?.cancel()
        progressJob = null
        playerManager.stop()
        _currentFile.value = null
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        playerManager.release()
    }
}
