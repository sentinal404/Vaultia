package com.vaultia.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultia.app.core.theme.ClayPrimary
import com.vaultia.app.core.theme.VioletDuskCream
import com.vaultia.app.core.theme.VioletDuskPeach
import com.vaultia.app.core.theme.VioletDuskRose

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : BottomNavItem("dashboard", "Vault", Icons.Default.Home)
    object Library : BottomNavItem("library", "Library", Icons.Default.Folder)
    object Transfers : BottomNavItem("transfers", "Transfers", Icons.Default.SyncAlt)
    object Settings : BottomNavItem("settings", "Settings", Icons.Default.Settings)
}

/**
 * Custom Curved Floating Bottom Navigation Bar matching User Reference Image 1.
 * Features:
 * - Fluid curved dome / cutout contour on the top line
 * - Elevated active bubble button popping smoothly above the bar
 * - Spring-driven gliding animation across tab selections
 * - High-contrast active icon & typography
 * - System navigation bar padding and safe area insets
 */
@Composable
fun VaultiaBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember {
        listOf(
            BottomNavItem.Dashboard,
            BottomNavItem.Library,
            BottomNavItem.Transfers,
            BottomNavItem.Settings
        )
    }

    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.let {
        if (it == -1) 0 else it
    }

    val density = LocalDensity.current
    val navHeight = 72.dp
    val domeExtraHeight = 16.dp
    val totalHeight = navHeight + domeExtraHeight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(totalHeight),
        contentAlignment = Alignment.BottomCenter
    ) {
        // We measure width dynamically
        val count = items.size
        val animatedIndexFraction by animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "NavIndexAnim"
        )

        // Custom Curved Canvas Container
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
        ) {
            val totalWidth = size.width
            val itemWidth = totalWidth / count
            val domeExtraPx = domeExtraHeight.toPx()
            val baseTopY = domeExtraPx
            val barBottomY = size.height
            val cornerRadiusPx = 28.dp.toPx()

            val activeCenterXPx = (animatedIndexFraction * itemWidth) + (itemWidth / 2f)
            val domeHalfWidthPx = 42.dp.toPx()
            val domePeakY = 2.dp.toPx()

            val path = Path().apply {
                // Start at top-left corner
                moveTo(0f, baseTopY + cornerRadiusPx)
                // Top-left arc
                cubicTo(
                    0f, baseTopY,
                    0f, baseTopY,
                    cornerRadiusPx, baseTopY
                )

                // Line towards the dome start
                val domeLeftPx = (activeCenterXPx - domeHalfWidthPx).coerceAtLeast(cornerRadiusPx)
                val domeRightPx = (activeCenterXPx + domeHalfWidthPx).coerceAtMost(totalWidth - cornerRadiusPx)

                lineTo(domeLeftPx, baseTopY)

                // Smooth S-curve upward to peak
                val c1x = domeLeftPx + (activeCenterXPx - domeLeftPx) * 0.45f
                val c1y = baseTopY
                val c2x = activeCenterXPx - (activeCenterXPx - domeLeftPx) * 0.35f
                val c2y = domePeakY
                cubicTo(c1x, c1y, c2x, c2y, activeCenterXPx, domePeakY)

                // Smooth S-curve downward from peak to right shoulder
                val c3x = activeCenterXPx + (domeRightPx - activeCenterXPx) * 0.35f
                val c3y = domePeakY
                val c4x = domeRightPx - (domeRightPx - activeCenterXPx) * 0.45f
                val c4y = baseTopY
                cubicTo(c3x, c3y, c4x, c4y, domeRightPx, baseTopY)

                // Line to top-right corner
                lineTo(totalWidth - cornerRadiusPx, baseTopY)

                // Top-right corner
                cubicTo(
                    totalWidth, baseTopY,
                    totalWidth, baseTopY,
                    totalWidth, baseTopY + cornerRadiusPx
                )

                // Right edge
                lineTo(totalWidth, barBottomY - cornerRadiusPx)

                // Bottom-right corner
                cubicTo(
                    totalWidth, barBottomY,
                    totalWidth, barBottomY,
                    totalWidth - cornerRadiusPx, barBottomY
                )

                // Bottom edge
                lineTo(cornerRadiusPx, barBottomY)

                // Bottom-left corner
                cubicTo(
                    0f, barBottomY,
                    0f, barBottomY,
                    0f, barBottomY - cornerRadiusPx
                )

                close()
            }

            // Draw rich dark glassmorphic gradient body
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF231633).copy(alpha = 0.96f),
                        Color(0xFF120A1A).copy(alpha = 0.98f),
                        Color(0xFF09040E).copy(alpha = 1.0f)
                    )
                )
            )

            // Draw glowing specular rim stroke
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    listOf(
                        VioletDuskRose.copy(alpha = 0.5f),
                        Color(0xFF8B5CF6).copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(totalWidth, barBottomY)
                ),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        // Active Elevated Bubble Button (Glides horizontally matching animatedIndexFraction)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex

                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = 0.65f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "IconScale"
                )

                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) Color.White else VioletDuskCream.copy(alpha = 0.45f),
                    animationSpec = tween(180),
                    label = "IconTint"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(totalHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onNavigate(item.route) }
                        ),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (isSelected) {
                        // Raised Circular Bubble (Matching Image 1)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .shadow(
                                        elevation = 8.dp,
                                        shape = CircleShape,
                                        ambientColor = VioletDuskRose.copy(alpha = 0.5f),
                                        spotColor = VioletDuskRose.copy(alpha = 0.7f)
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF9D4EDD),
                                                Color(0xFF7B2CBF),
                                                Color(0xFF5A189A)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(Color.White.copy(alpha = 0.8f), VioletDuskPeach.copy(alpha = 0.4f))
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .scale(iconScale)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = VioletDuskPeach,
                                maxLines = 1
                            )
                        }
                    } else {
                        // Inactive icon (Clean & centered in the bar)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = domeExtraHeight)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.title,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                color = iconTint,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
