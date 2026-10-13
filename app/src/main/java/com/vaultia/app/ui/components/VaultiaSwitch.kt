package com.vaultia.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vaultia.app.core.theme.ClayPrimary

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Custom Vaultia Tactile Sliding Switch.
 * Provides guaranteed bidirectional sliding animation with smooth spring/tween physics,
 * tactile elevation, drag gestures, and clear left-to-right / right-to-left thumb motion.
 */
@Composable
fun VaultiaSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackWidth = 52.dp
    val trackHeight = 30.dp
    val thumbSize = 24.dp
    val thumbPadding = 3.dp
    val maxOffset = trackWidth - thumbSize - thumbPadding // 25.dp

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) maxOffset else thumbPadding,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "VaultiaSwitchThumbOffset"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) {
            ClayPrimary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 200),
        label = "VaultiaSwitchTrackColor"
    )

    val trackBorderColor by animateColorAsState(
        targetValue = if (checked) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "VaultiaSwitchTrackBorder"
    )

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(RoundedCornerShape(15.dp))
            .background(trackColor)
            .border(width = 1.dp, color = trackBorderColor, shape = RoundedCornerShape(15.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onCheckedChange(!checked)
            }
            .pointerInput(enabled, checked) {
                if (!enabled) return@pointerInput
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = {
                        if (totalDrag > 12f && !checked) {
                            onCheckedChange(true)
                        } else if (totalDrag < -12f && checked) {
                            onCheckedChange(false)
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                        if (totalDrag > 20f && !checked) {
                            onCheckedChange(true)
                        } else if (totalDrag < -20f && checked) {
                            onCheckedChange(false)
                        }
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Sliding tactile circular thumb
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .shadow(elevation = 3.dp, shape = CircleShape, ambientColor = Color.Black.copy(alpha = 0.35f))
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
