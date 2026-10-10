package com.vaultia.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * Premium 2026 persistent floating glassmorphic navigation bar.
 * Designed with proper system navigation bar insets, spring scale icon animations,
 * and high-contrast glowing pill indicators.
 */
@Composable
fun VaultiaBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Library,
        BottomNavItem.Transfers,
        BottomNavItem.Settings
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Glassmorphic Pill Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF241334).copy(alpha = 0.94f),
                            Color(0xFF13091C).copy(alpha = 0.98f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            VioletDuskRose.copy(alpha = 0.35f),
                            Color(0xFF8B5CF6).copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route

                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) VioletDuskPeach else VioletDuskCream.copy(alpha = 0.45f),
                        animationSpec = tween(durationMillis = 200),
                        label = "NavColor"
                    )

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.14f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
                        label = "IconScale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(
                                        listOf(
                                            VioletDuskRose.copy(alpha = 0.30f),
                                            Color(0xFF8B5CF6).copy(alpha = 0.22f)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onNavigate(item.route) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = iconTint,
                                modifier = Modifier
                                    .size(22.dp)
                                    .scale(iconScale)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
