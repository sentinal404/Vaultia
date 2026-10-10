package com.vaultia.app.ui.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultia.app.core.theme.ClayAccentEmerald
import com.vaultia.app.core.theme.VioletDuskBg
import com.vaultia.app.core.theme.VioletDuskCream
import com.vaultia.app.core.theme.VioletDuskNeonPink
import com.vaultia.app.core.theme.VioletDuskPeach
import com.vaultia.app.core.theme.VioletDuskPlum
import com.vaultia.app.core.theme.VioletDuskRose
import com.vaultia.app.ui.components.ClayButton
import com.vaultia.app.ui.viewmodel.PlayerViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AudioPlayerScreen(
    fileId: String,
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit
) {
    val currentFile by viewModel.currentFile.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val context = LocalContext.current

    var isFavorite by remember { mutableStateOf(false) }
    var isLooping by remember { mutableStateOf(false) }
    var isShuffle by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }

    LaunchedEffect(fileId) {
        viewModel.loadMedia(fileId)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "VinylRotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing)
        ),
        label = "Rotation"
    )

    val progressFraction = if (playerState.durationMs > 0) {
        (playerState.currentPositionMs.toFloat() / playerState.durationMs).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(VioletDuskBg, Color(0xFF1D0E27), VioletDuskPlum)
                )
            )
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Row (Back arrow + Title + Stop button)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = VioletDuskCream)
            }

            Text(
                text = "Audio Player",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = VioletDuskCream
            )

            // Stop button to kill playback cleanly
            IconButton(
                onClick = {
                    viewModel.stopPlayback()
                    onBackClick()
                }
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color(0xFFF43F5E))
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Circular Artwork with Outer Progress Arc (Matching Screenshot 3)
        Box(
            modifier = Modifier.size(260.dp),
            contentAlignment = Alignment.Center
        ) {
            // Circular Arc Progress Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 5.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)

                // Background track arc (from 140 degrees to 400 degrees, sweeping 260 degrees)
                drawArc(
                    color = Color.White.copy(alpha = 0.12f),
                    startAngle = 140f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active progress gradient arc
                val activeSweep = 260f * progressFraction
                if (activeSweep > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(VioletDuskRose, VioletDuskNeonPink, VioletDuskPeach)
                        ),
                        startAngle = 140f,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth + 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw glowing thumb circle at arc endpoint
                    val endAngleRad = Math.toRadians((140f + activeSweep).toDouble())
                    val thumbX = centerOffset.x + (radius * cos(endAngleRad)).toFloat()
                    val thumbY = centerOffset.y + (radius * sin(endAngleRad)).toFloat()
                    drawCircle(
                        color = VioletDuskPeach,
                        radius = 7.dp.toPx(),
                        center = Offset(thumbX, thumbY)
                    )
                }
            }

            // Rotating Vinyl Center Disc
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .rotate(if (playerState.isPlaying) rotation else 0f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF2E183D), Color(0xFF14091D))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl grooves rings
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = Color.White.copy(alpha = 0.05f), radius = size.minDimension * 0.42f, style = Stroke(width = 1.dp.toPx()))
                    drawCircle(color = Color.White.copy(alpha = 0.05f), radius = size.minDimension * 0.35f, style = Stroke(width = 1.dp.toPx()))
                    drawCircle(color = Color.White.copy(alpha = 0.05f), radius = size.minDimension * 0.28f, style = Stroke(width = 1.dp.toPx()))
                }

                // Inner Album Artwork / Label
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(VioletDuskPlum, VioletDuskRose)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = VioletDuskPeach,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            // Elapsed Time indicator on Left of arc
            Text(
                text = formatAudioTime(playerState.currentPositionMs),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = VioletDuskCream.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, bottom = 12.dp)
            )

            // Duration indicator on Right of arc
            Text(
                text = formatAudioTime(playerState.durationMs),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = VioletDuskCream.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action Icons Row below disc: Favorite (Heart) & Download to Device (Share button removed per user request)
        Row(
            modifier = Modifier.fillMaxWidth(0.44f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { isFavorite = !isFavorite }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) VioletDuskNeonPink else VioletDuskCream.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = {
                val started = viewModel.downloadCurrentFile()
                if (started) {
                    Toast.makeText(context, "Downloading audio to device...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Preparing audio...", Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download to Device",
                    tint = VioletDuskPeach,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Glowing Audio Waveform Ribbon (Matching Screenshot 3)
        GlowingWaveformRibbon(isPlaying = playerState.isPlaying)

        Spacer(modifier = Modifier.height(18.dp))

        // Song Title & Artist Info
        Text(
            text = currentFile?.name ?: "Streaming Track",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = VioletDuskCream,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Vaultia Cloud Stream",
            style = MaterialTheme.typography.bodyMedium,
            color = VioletDuskPeach.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Background Playback Active Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(ClayAccentEmerald.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Background Playback Active • Screen can be turned off",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = ClayAccentEmerald
            )
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Progress Slider
        Slider(
            value = progressFraction,
            onValueChange = { fraction ->
                viewModel.seekTo((fraction * playerState.durationMs).toLong())
            },
            colors = SliderDefaults.colors(
                thumbColor = VioletDuskPeach,
                activeTrackColor = VioletDuskRose,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Transport Controls Row (Shuffle, Prev, Big Glowing Play/Pause, Next, Loop)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(onClick = { isShuffle = !isShuffle }) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) VioletDuskPeach else VioletDuskCream.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Previous
            IconButton(onClick = { viewModel.seekBackward() }) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = VioletDuskCream,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Big Glowing Gradient Play / Pause Button (Matching Screenshot 3)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(VioletDuskRose, VioletDuskNeonPink)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(72.dp)
                ) {
                    if (playerState.isBuffering || (playerState.durationMs == 0L && !playerState.isPlaying && playerState.errorMessage == null)) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = VioletDuskCream,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = VioletDuskCream,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // Next
            IconButton(onClick = { viewModel.seekForward() }) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = VioletDuskCream,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Repeat / Loop
            IconButton(onClick = {
                isLooping = !isLooping
                viewModel.playerManager.player.repeatMode = if (isLooping) {
                    androidx.media3.common.Player.REPEAT_MODE_ONE
                } else {
                    androidx.media3.common.Player.REPEAT_MODE_OFF
                }
            }) {
                Icon(
                    Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = if (isLooping) VioletDuskPeach else VioletDuskCream.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.3f))
    }
}

@Composable
private fun GlowingWaveformRibbon(isPlaying: Boolean) {
    val barCount = 36
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformRibbon")

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(28.dp)
    ) {
        for (i in 0 until barCount) {
            val animDuration = 400 + (i * 45) % 600
            val fraction by infiniteTransition.animateFloat(
                initialValue = 0.15f,
                targetValue = if (isPlaying) 1.0f else 0.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(animDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Wave_$i"
            )

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((24 * fraction).dp.coerceAtLeast(3.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(VioletDuskPeach, VioletDuskRose, VioletDuskNeonPink)
                        )
                    )
            )
        }
    }
}

private fun formatAudioTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
