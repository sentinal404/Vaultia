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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.vaultia.app.core.theme.ClayPrimary
import com.vaultia.app.core.theme.VioletDuskCream
import com.vaultia.app.core.theme.VioletDuskPeach
import com.vaultia.app.core.theme.VioletDuskPlum
import com.vaultia.app.core.theme.VioletDuskRose
import com.vaultia.app.ui.components.ClayButton
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
    var isScreenLocked by remember { mutableStateOf(false) }
    var showLockPill by remember { mutableStateOf(false) }

    var showRewindIndicator by remember { mutableStateOf(false) }
    var showForwardIndicator by remember { mutableStateOf(false) }

    // Gesture HUD States with Debounce Timestamp Keys (fixes glitch where icon fails to fade!)
    var volumePercent by remember { mutableFloatStateOf(0.5f) }
    var volumeHudTimestamp by remember { mutableLongStateOf(0L) }
    var showVolumeHud by remember { mutableStateOf(false) }

    var brightnessPercent by remember { mutableFloatStateOf(0.5f) }
    var brightnessHudTimestamp by remember { mutableLongStateOf(0L) }
    var showBrightnessHud by remember { mutableStateOf(false) }

    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var isFitMode by remember { mutableStateOf(true) }

    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    // Instant stop when leaving screen or pressing system back
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

    // Auto-hide controls timer (4s)
    LaunchedEffect(showControls, playerState.isPlaying, isScreenLocked) {
        if (showControls && playerState.isPlaying && !isScreenLocked) {
            delay(4000L)
            showControls = false
        }
    }

    // Auto-hide lock pill when screen is locked
    LaunchedEffect(showLockPill) {
        if (showLockPill) {
            delay(2500L)
            showLockPill = false
        }
    }

    // Double tap indicators auto-hide (800ms)
    LaunchedEffect(showRewindIndicator) {
        if (showRewindIndicator) {
            delay(800L)
            showRewindIndicator = false
        }
    }
    LaunchedEffect(showForwardIndicator) {
        if (showForwardIndicator) {
            delay(800L)
            showForwardIndicator = false
        }
    }

    // Volume HUD debounced auto-hide (1200ms after last gesture)
    LaunchedEffect(volumeHudTimestamp) {
        if (volumeHudTimestamp > 0L) {
            showVolumeHud = true
            delay(1200L)
            showVolumeHud = false
        }
    }

    // Brightness HUD debounced auto-hide (1200ms after last gesture - PERMANENT FIX)
    LaunchedEffect(brightnessHudTimestamp) {
        if (brightnessHudTimestamp > 0L) {
            showBrightnessHud = true
            delay(1200L)
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

        // Gesture Touch Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isScreenLocked) {
                    if (isScreenLocked) {
                        detectTapGestures(onTap = { showLockPill = true })
                    } else {
                        detectTapGestures(
                            onTap = { showControls = !showControls },
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
                }
                .pointerInput(isScreenLocked) {
                    if (!isScreenLocked) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            val isRightSide = change.position.x > size.width / 2

                            if (isRightSide) {
                                // Volume Control (Right side)
                                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val delta = if (dragAmount < 0) 1 else -1
                                val newVol = (currentVol + delta).coerceIn(0, maxVolume)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                volumePercent = newVol.toFloat() / maxVolume
                                volumeHudTimestamp = System.currentTimeMillis()
                            } else {
                                // Brightness Control (Left side)
                                if (activity != null) {
                                    val lp = activity.window.attributes
                                    val currentBright = if (lp.screenBrightness < 0) 0.5f else lp.screenBrightness
                                    val delta = -dragAmount / 320f
                                    val newBright = (currentBright + delta).coerceIn(0.05f, 1.0f)
                                    lp.screenBrightness = newBright
                                    activity.window.attributes = lp
                                    brightnessPercent = newBright
                                    brightnessHudTimestamp = System.currentTimeMillis()
                                }
                            }
                        }
                    }
                }
        )

        // Buffering Indicator
        if (playerState.isBuffering && playerState.errorMessage == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = VioletDuskPeach,
                    modifier = Modifier.size(54.dp),
                    strokeWidth = 3.5.dp
                )
            }
        }

        // Error Indicator
        if (playerState.errorMessage != null) {
            val isDirectBotShare = (currentFile?.totalChunks ?: 1) <= 1 && currentFile?.localUri.isNullOrBlank()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.92f))
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Text("⚠️", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isDirectBotShare) "Stream Reconnecting" else "Unable to Stream",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isDirectBotShare) {
                                "File: ${currentFile?.name ?: "Video"}\nSize: ${com.vaultia.app.domain.model.VaultFile.formatBytes(currentFile?.size ?: 0L)}\n\nDirect Telegram MTProto connection failed or stream timed out. Tap Retry to reconnect, or Download to save to device storage."
                            } else {
                                playerState.errorMessage ?: "Check network connection or worker bots"
                            },
                            color = VioletDuskPeach.copy(alpha = 0.90f),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ClayButton(
                                text = "Retry",
                                onClick = { currentFile?.id?.let { viewModel.loadMedia(it) } }
                            )
                            ClayButton(
                                text = "Download",
                                onClick = { viewModel.downloadCurrentFile() }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(onClick = onBackClick) {
                            Text("Back to Library", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Double Tap Feedback: Rewind Indicator (Left)
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
                    Icon(Icons.Default.FastRewind, contentDescription = null, tint = VioletDuskPeach, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("-10s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        // Double Tap Feedback: Forward Indicator (Right)
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
                    Icon(Icons.Default.FastForward, contentDescription = null, tint = VioletDuskPeach, modifier = Modifier.size(28.dp))
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
                    .background(VioletDuskPlum.copy(alpha = 0.9f))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = VioletDuskPeach, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    ClayProgressBar(progress = volumePercent, modifier = Modifier.width(110.dp))
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
                    .background(VioletDuskPlum.copy(alpha = 0.9f))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.BrightnessHigh, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    ClayProgressBar(progress = brightnessPercent, modifier = Modifier.width(110.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${(brightnessPercent * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Screen Lock Floating Toggle (When locked or when lock pill active)
        AnimatedVisibility(
            visible = isScreenLocked && showLockPill,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(VioletDuskPlum.copy(alpha = 0.9f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = VioletDuskPeach, modifier = Modifier.size(18.dp))
                    Text("Screen Locked", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    IconButton(
                        onClick = {
                            isScreenLocked = false
                            showControls = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = "Unlock", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Classic Controls Overlay (Matching Screenshot 2)
        AnimatedVisibility(
            visible = showControls && !isScreenLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row (Back, Title, Speed, Aspect, Screen Lock)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if ((currentFile?.totalChunks ?: 1) > 1) {
                                    "Multi-Bot 5MB Chunk Stream (Zero-Buffering)"
                                } else {
                                    "Direct Telegram MTProto Stream"
                                },
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Top Action Controls
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
                                    .background(VioletDuskRose.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${currentSpeed}x",
                                    color = VioletDuskPeach,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Fit / Zoom Aspect Ratio Toggle
                        IconButton(onClick = { isFitMode = !isFitMode }) {
                            Icon(
                                Icons.Default.AspectRatio,
                                contentDescription = "Aspect Ratio",
                                tint = if (isFitMode) Color.White else VioletDuskPeach
                            )
                        }

                        // Screen Lock Button
                        IconButton(onClick = {
                            isScreenLocked = true
                            showControls = false
                            showLockPill = true
                        }) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock Screen", tint = Color.White)
                        }
                    }
                }

                // Bottom Classic Playback Controls Bar (Matching Screenshot 2)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Time & Slider Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(playerState.currentPositionMs),
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            val progressFraction = if (playerState.durationMs > 0) {
                                playerState.currentPositionMs.toFloat() / playerState.durationMs
                            } else 0f

                            Slider(
                                value = progressFraction.coerceIn(0f, 1f),
                                onValueChange = { fraction ->
                                    viewModel.seekTo((fraction * playerState.durationMs).toLong())
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF10B981), // Classic vibrant green thumb
                                    activeTrackColor = Color(0xFF10B981), // Classic green track
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )

                            Text(
                                text = formatTime(playerState.durationMs),
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Classic Transport Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Center Playback Buttons: Rewind 10s, Prev, Play/Pause, Next, Forward 10s
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                IconButton(onClick = { viewModel.seekBackward() }) {
                                    Icon(Icons.Default.FastRewind, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                                }

                                IconButton(onClick = { viewModel.seekBackward() }) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(28.dp))
                                }

                                // Main Play/Pause Button
                                IconButton(
                                    onClick = { viewModel.togglePlayPause() },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(VioletDuskRose, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                        tint = VioletDuskCream,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                IconButton(onClick = { viewModel.seekForward() }) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(28.dp))
                                }

                                IconButton(onClick = { viewModel.seekForward() }) {
                                    Icon(Icons.Default.FastForward, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }

                            // Right Aspect Ratio / Fullscreen Button
                            IconButton(onClick = { isFitMode = !isFitMode }) {
                                Icon(Icons.Default.AspectRatio, contentDescription = "Fullscreen mode", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
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
