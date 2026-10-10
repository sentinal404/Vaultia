package com.vaultia.app.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VideoPlayerState(
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null
)

class VideoPlayerManager(private val context: Context) {

    private val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            2500,  // minBufferMs (>= bufferForPlaybackAfterRebufferMs)
            30000, // maxBufferMs
            500,   // bufferForPlaybackMs - starts video immediately!
            1000   // bufferForPlaybackAfterRebufferMs
        )
        .build()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setLoadControl(loadControl)
        .build()

    private val _playerState = MutableStateFlow(VideoPlayerState())
    val playerState: StateFlow<VideoPlayerState> = _playerState.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _playerState.value = _playerState.value.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING
                )
                updateState()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("VideoPlayerManager", "ExoPlayer error: ${error.message}", error)
                _playerState.value = _playerState.value.copy(
                    isBuffering = false,
                    errorMessage = error.localizedMessage ?: "Playback error"
                )
            }
        })
    }

    fun prepareStream(streamUrl: String) {
        _playerState.value = _playerState.value.copy(errorMessage = null)
        val mediaItem = MediaItem.fromUri(streamUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        updateState()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        updateState()
    }

    fun seekForward(deltaMs: Long = 10000L) {
        val target = (player.currentPosition + deltaMs).coerceAtMost(player.duration.coerceAtLeast(0L))
        player.seekTo(target)
        updateState()
    }

    fun seekBackward(deltaMs: Long = 10000L) {
        val target = (player.currentPosition - deltaMs).coerceAtLeast(0L)
        player.seekTo(target)
        updateState()
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
    }

    fun updateProgress() {
        if (player.duration > 0) {
            _playerState.value = _playerState.value.copy(
                currentPositionMs = player.currentPosition,
                durationMs = player.duration,
                bufferedPositionMs = player.bufferedPosition,
                isPlaying = player.isPlaying
            )
        }
    }

    fun stop() {
        try {
            player.stop()
            player.clearMediaItems()
        } catch (_: Exception) {}
        _playerState.value = VideoPlayerState()
    }

    fun release() {
        stop()
        player.release()
    }

    private fun updateState() {
        _playerState.value = _playerState.value.copy(
            isPlaying = player.isPlaying,
            durationMs = player.duration.coerceAtLeast(0L),
            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L)
        )
    }
}
