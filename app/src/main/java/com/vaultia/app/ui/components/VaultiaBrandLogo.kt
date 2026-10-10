package com.vaultia.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaultia.app.R

/**
 * Premium 2026 Futuristic Neon Circular Emblem Logo for Vaultia.
 * Features glowing holographic energy halo and high-tech cyber core.
 */
@Composable
fun VaultiaBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VaultLogoPulse")
    val pulseScale by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Pulse"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(pulseScale),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing neon aura
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6).copy(alpha = 0.50f),
                            Color(0xFFEC4899).copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Futuristic Neon Circle Emblem
        Image(
            painter = painterResource(id = R.drawable.vaultia_logo),
            contentDescription = "Vaultia Futuristic Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(size * 0.92f)
                .clip(CircleShape)
        )
    }
}
