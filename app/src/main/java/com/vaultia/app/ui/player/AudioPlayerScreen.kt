package com.vaultia.app.ui.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultia.app.core.theme.ClayAccentEmerald
import com.vaultia.app.core.theme.ClayPrimary
import com.vaultia.app.core.theme.ClaySecondary
import com.vaultia.app.core.theme.claymorphic
import com.vaultia.app.ui.components.ClayButton
import com.vaultia.app.ui.components.ClayCard
import com.vaultia.app.ui.viewmodel.PlayerViewModel

@Composable
fun AudioPlayerScreen(
    fileId: String,
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit
) {
    val currentFile by viewModel.currentFile.collectAsState()
    val playerState by viewModel.playerState.collectAsState()

    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var isLooping by remember { mutableStateOf(false) }

    LaunchedEffect(fileId) {
        viewModel.loadMedia(fileId)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "VinylRotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing)
        ),
        label = "Rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Header with Stop Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text = "Audio Vault Player",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Stop & Close button
            IconButton(
                onClick = {
                    viewModel.stopPlayback()
                    onBackClick()
                }
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.6f))

        // Clay Vinyl Disc with Glowing Center
        Box(
            modifier = Modifier
                .size(240.dp)
                .rotate(if (playerState.isPlaying) rotation else 0f)
                .claymorphic(shape = CircleShape, elevation = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            // Vinyl Grooves
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                // Center Label
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.linearGradient(listOf(ClayPrimary, ClaySecondary)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Animated Frequency Waveform Visualizer Bars
        AudioVisualizerBars(isPlaying = playerState.isPlaying)

        Spacer(modifier = Modifier.weight(0.4f))

        Text(
            text = currentFile?.name ?: "Streaming Audio",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .background(ClayAccentEmerald.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Background Playback Active • Screen can be turned off",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = ClayAccentEmerald
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Progress Slider
        val fraction = if (playerState.durationMs > 0) {
            playerState.currentPositionMs.toFloat() / playerState.durationMs
        } else 0f

        Slider(
            value = fraction.coerceIn(0f, 1f),
            onValueChange = { f ->
                viewModel.seekTo((f * playerState.durationMs).toLong())
            },
            colors = SliderDefaults.colors(
                thumbColor = ClayPrimary,
                activeTrackColor = ClayPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatAudioTime(playerState.currentPositionMs),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Text(
                text = formatAudioTime(playerState.durationMs),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Extra Action Row: Speed & Loop
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        .background(ClayPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${currentSpeed}x",
                        color = ClayPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = {
                    isLooping = !isLooping
                    viewModel.playerManager.player.repeatMode = if (isLooping) {
                        androidx.media3.common.Player.REPEAT_MODE_ONE
                    } else {
                        androidx.media3.common.Player.REPEAT_MODE_OFF
                    }
                }
            ) {
                Icon(
                    Icons.Default.Repeat,
                    contentDescription = "Loop",
                    tint = if (isLooping) ClayPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Play Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.seekBackward() }) {
                Icon(Icons.Default.FastRewind, contentDescription = "Rewind", tint = ClayPrimary, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.size(28.dp))

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .claymorphic(shape = CircleShape, elevation = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { viewModel.togglePlayPause() }) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = ClayPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.size(28.dp))

            IconButton(onClick = { viewModel.seekForward() }) {
                Icon(Icons.Default.FastForward, contentDescription = "Forward", tint = ClayPrimary, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.weight(0.4f))
    }
}

@Composable
private fun AudioVisualizerBars(isPlaying: Boolean) {
    val barCount = 18
    val infiniteTransition = rememberInfiniteTransition(label = "Visualizer")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(36.dp)
    ) {
        for (i in 0 until barCount) {
            val animDuration = 450 + (i * 70) % 500
            val heightFraction by infiniteTransition.animateFloat(
                initialValue = 0.15f,
                targetValue = if (isPlaying) 1.0f else 0.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(animDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Bar_$i"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((32 * heightFraction).dp.coerceAtLeast(4.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(ClayPrimary, Color(0xFF38BDF8))
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
