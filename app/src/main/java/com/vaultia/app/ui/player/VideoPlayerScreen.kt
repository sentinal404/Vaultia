package com.vaultia.app.ui.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.vaultia.app.core.theme.ClayPrimary
import com.vaultia.app.ui.components.ClayCard
import com.vaultia.app.ui.components.ClayProgressBar
import com.vaultia.app.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    fileId: String,
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val currentFile by viewModel.currentFile.collectAsState()
    val playerState by viewModel.playerState.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var showRewindIndicator by remember { mutableStateOf(false) }
    var showForwardIndicator by remember { mutableStateOf(false) }

    // Gesture HUD States
    var showVolumeHud by remember { mutableStateOf(false) }
    var volumePercent by remember { mutableFloatStateOf(0.5f) }

    var showBrightnessHud by remember { mutableStateOf(false) }
    var brightnessPercent by remember { mutableFloatStateOf(0.5f) }

    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var isFitMode by remember { mutableStateOf(true) }

    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    // Clean up and stop playback when leaving screen or pressing system back
    BackHandler {
        viewModel.stopPlayback()
        onBackClick()
    }

    DisposableEffect(fileId) {
        onDispose {
            viewModel.stopPlayback()
        }
    }

    LaunchedEffect(fileId) {
        viewModel.loadMedia(fileId)
    }

    // Auto-hide controls timer
    LaunchedEffect(showControls, playerState.isPlaying) {
        if (showControls && playerState.isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    // Hide seek pills after delay
    LaunchedEffect(showRewindIndicator) {
        if (showRewindIndicator) {
            delay(800)
            showRewindIndicator = false
        }
    }
    LaunchedEffect(showForwardIndicator) {
        if (showForwardIndicator) {
            delay(800)
            showForwardIndicator = false
        }
    }

    // Hide HUDs after delay
    LaunchedEffect(showVolumeHud) {
        if (showVolumeHud) {
            delay(1200)
            showVolumeHud = false
        }
    }
    LaunchedEffect(showBrightnessHud) {
        if (showBrightnessHud) {
            delay(1200)
            showBrightnessHud = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ExoPlayer Video View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.playerManager.player
                    useController = false
                    resizeMode = if (isFitMode) AspectRatioFrameLayout.RESIZE_MODE_FIT else AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = if (isFitMode) AspectRatioFrameLayout.RESIZE_MODE_FIT else AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            },
            modifier = Modifier.fillMaxSize()
        )

        // Touch & Gesture Detection Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                        },
                        onDoubleTap = { offset ->
                            if (offset.x < size.width / 2) {
                                viewModel.seekBackward()
                                showRewindIndicator = true
                            } else {
                                viewModel.seekForward()
                                showForwardIndicator = true
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        val isRightSide = change.position.x > size.width / 2

                        if (isRightSide) {
                            // Volume control
                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val delta = if (dragAmount < 0) 1 else -1
                            val newVol = (currentVol + delta).coerceIn(0, maxVolume)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                            volumePercent = newVol.toFloat() / maxVolume
                            showVolumeHud = true
                        } else {
                            // Brightness control
                            if (activity != null) {
                                val lp = activity.window.attributes
                                val currentBright = if (lp.screenBrightness < 0) 0.5f else lp.screenBrightness
                                val delta = -dragAmount / 350f
                                val newBright = (currentBright + delta).coerceIn(0.05f, 1.0f)
                                lp.screenBrightness = newBright
                                activity.window.attributes = lp
                                brightnessPercent = newBright
                                showBrightnessHud = true
                            }
                        }
                    }
                }
        )

        // Buffering Indicator
        if (playerState.isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = ClayPrimary,
                    modifier = Modifier.size(52.dp),
                    strokeWidth = 3.dp
                )
            }
        }

        // Double Tap Seek Feedback: Rewind Indicator (Left)
        AnimatedVisibility(
            visible = showRewindIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 36.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FastRewind, contentDescription = null, tint = ClayPrimary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("-10s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        // Double Tap Seek Feedback: Forward Indicator (Right)
        AnimatedVisibility(
            visible = showForwardIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 36.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("+10s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.FastForward, contentDescription = null, tint = ClayPrimary, modifier = Modifier.size(28.dp))
                }
            }
        }

        // Gesture HUD: Volume (Right)
        AnimatedVisibility(
            visible = showVolumeHud,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = ClayPrimary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    ClayProgressBar(progress = volumePercent, modifier = Modifier.width(100.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${(volumePercent * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Gesture HUD: Brightness (Left)
        AnimatedVisibility(
            visible = showBrightnessHud,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.BrightnessHigh, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    ClayProgressBar(progress = brightnessPercent, modifier = Modifier.width(100.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${(brightnessPercent * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Controls Overlay (Top Header & Bottom Clay Bar)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            viewModel.stopPlayback()
                            onBackClick()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Column(modifier = Modifier.padding(start = 6.dp)) {
                            Text(
                                text = currentFile?.name ?: "Streaming Video",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "Zero-Buffering Telegram Stream",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Top Action Buttons: Speed & Aspect Ratio
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Playback Speed Toggle
                        IconButton(
                            onClick = {
                                val nextSpeed = when (currentSpeed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2.0f
                                    else -> 1.0f
                                }
                                currentSpeed = nextSpeed
                                viewModel.setSpeed(nextSpeed)
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(ClayPrimary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${currentSpeed}x",
                                    color = ClayPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Fit / Fill Toggle
                        IconButton(onClick = { isFitMode = !isFitMode }) {
                            Icon(
                                Icons.Default.AspectRatio,
                                contentDescription = "Aspect Ratio",
                                tint = if (isFitMode) Color.White else ClayPrimary
                            )
                        }
                    }
                }

                // Bottom Controls Card
                ClayCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    surfaceColor = Color(0xFF0F172A).copy(alpha = 0.92f)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Seek Bar
                        val progressFraction = if (playerState.durationMs > 0) {
                            playerState.currentPositionMs.toFloat() / playerState.durationMs
                        } else 0f

                        Slider(
                            value = progressFraction.coerceIn(0f, 1f),
                            onValueChange = { fraction ->
                                viewModel.seekTo((fraction * playerState.durationMs).toLong())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = ClayPrimary,
                                activeTrackColor = ClayPrimary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Time indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(playerState.currentPositionMs),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = formatTime(playerState.durationMs),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Playback controls row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.seekBackward() }) {
                                Icon(Icons.Default.FastRewind, contentDescription = "Rewind 10s", tint = Color.White)
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            IconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(ClayPrimary.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                    tint = ClayPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            IconButton(onClick = { viewModel.seekForward() }) {
                                Icon(Icons.Default.FastForward, contentDescription = "Fast Forward 10s", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
