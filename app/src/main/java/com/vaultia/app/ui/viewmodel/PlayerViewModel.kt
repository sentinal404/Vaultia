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
import com.vaultia.app.VaultiaApplication
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

    private val _playlist = MutableStateFlow<List<VaultFile>>(emptyList())
    val playlist: StateFlow<List<VaultFile>> = _playlist.asStateFlow()

    init {
        playerManager.onPlaybackEnded = {
            playNext()
        }
    }

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
                // Scan background files to build active playlist for next/prev
                try {
                    val allMedia = repository.database.fileDao().getAllFilesList()
                        .map { it.toDomain() }
                        .filter { it.mediaType == file.mediaType }
                    if (allMedia.isNotEmpty()) {
                        _playlist.value = allMedia
                    } else {
                        _playlist.value = listOf(file)
                    }
                } catch (_: Exception) {
                    _playlist.value = listOf(file)
                }
            }
            val streamUrl = repository.getStreamUrl(fileId)
            playerManager.prepareStream(streamUrl)
            startProgressPolling()
        }
    }

    fun playNext(): Boolean {
        val list = _playlist.value
        if (list.isEmpty()) return false
        val current = _currentFile.value
        val currentIndex = list.indexOfFirst { it.id == current?.id }
        val nextIndex = if (currentIndex != -1 && currentIndex + 1 < list.size) currentIndex + 1 else 0
        val nextFile = list[nextIndex]
        loadMedia(nextFile.id)
        return true
    }

    fun playPrevious(): Boolean {
        val list = _playlist.value
        if (list.isEmpty()) return false
        if (playerState.value.currentPositionMs > 3000L) {
            seekTo(0L)
            return true
        }
        val current = _currentFile.value
        val currentIndex = list.indexOfFirst { it.id == current?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        val prevFile = list[prevIndex]
        loadMedia(prevFile.id)
        return true
    }

    fun playFile(file: VaultFile) {
        loadMedia(file.id)
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
        try {
            VaultiaApplication.instance.purgeStreamingCache()
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        playerManager.release()
        try {
            VaultiaApplication.instance.purgeStreamingCache()
        } catch (_: Exception) {}
    }
}
