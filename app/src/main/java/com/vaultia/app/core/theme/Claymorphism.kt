package com.vaultia.app.core.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modern Claymorphism / Soft UI modifier for Vaultia.
 * Adds soft volumetric depth, gentle shadows, and tactile feel.
 */
@Composable
fun Modifier.claymorphic(
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 6.dp,
    surfaceColor: Color? = null,
    isPressed: Boolean = false
): Modifier {
    val isDark = isSystemInDarkTheme()
    val baseSurface = surfaceColor ?: if (isDark) ClayCardDark else ClayCardLight
    val shadowDark = if (isDark) ClayShadowDarkDark else ClayShadowDarkLight
    val highlight = if (isDark) ClayShadowLightDark else ClayShadowLightLight

    return this
        .shadow(
            elevation = if (isPressed) 2.dp else elevation,
            shape = shape,
            ambientColor = shadowDark,
            spotColor = shadowDark
        )
        .clip(shape)
        .background(baseSurface)
        .drawBehind {
            // Draw subtle top-left soft highlight border to give clay 3D bevel
            val strokeWidth = 1.5.dp.toPx()
            val highlightColor = if (isPressed) Color.Transparent else highlight
            drawCircle(
                color = highlightColor.copy(alpha = 0.35f),
                radius = size.maxDimension * 0.8f,
                center = Offset(0f, 0f)
            )
        }
}

@Composable
fun Modifier.claymorphicGradient(
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 8.dp,
    brush: Brush = Brush.linearGradient(listOf(ClayPrimary, ClayPrimaryVariant))
): Modifier {
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = ClayPrimaryVariant.copy(alpha = 0.5f),
            spotColor = ClayPrimary.copy(alpha = 0.6f)
        )
        .clip(shape)
        .background(brush)
}
