package com.vaultia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class VaultiaIconType {
    VAULT,
    VIDEO,
    AUDIO,
    PHOTO,
    DOCUMENT,
    TRANSFERS,
    BACKUP,
    SECURITY,
    MTPROTO,
    BOT_BRIDGE
}

/**
 * 3D Dimensional Luxury Icon Component.
 * Implements layered gradient depth, specular rim lighting, and luminous cyber-energy glyphs.
 */
@Composable
fun Vaultia3DIcon(
    type: VaultiaIconType,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    glyphSize: Dp = (size.value * 0.52f).dp
) {
    val (primaryBrush, rimColor, iconVector, iconTint) = when (type) {
        VaultiaIconType.VAULT -> Quad(
            Brush.linearGradient(listOf(Color(0xFF6C4CE0), Color(0xFF43C9D9))),
            Color(0xFFB4A0FF).copy(alpha = 0.6f),
            Icons.Default.Security,
            Color.White
        )
        VaultiaIconType.VIDEO -> Quad(
            Brush.linearGradient(listOf(Color(0xFF7547FF), Color(0xFF9D4EDD))),
            Color(0xFFE0AAFF).copy(alpha = 0.6f),
            Icons.Default.Movie,
            Color.White
        )
        VaultiaIconType.AUDIO -> Quad(
            Brush.linearGradient(listOf(Color(0xFFFF5D8F), Color(0xFFB5179E))),
            Color(0xFFFF99C8).copy(alpha = 0.6f),
            Icons.Default.Audiotrack,
            Color.White
        )
        VaultiaIconType.PHOTO -> Quad(
            Brush.linearGradient(listOf(Color(0xFF43C9D9), Color(0xFF2EC4B6))),
            Color(0xFFA5F3FC).copy(alpha = 0.6f),
            Icons.Default.Image,
            Color.White
        )
        VaultiaIconType.DOCUMENT -> Quad(
            Brush.linearGradient(listOf(Color(0xFF3A86FF), Color(0xFF4361EE))),
            Color(0xFF90E0EF).copy(alpha = 0.6f),
            Icons.Default.Description,
            Color.White
        )
        VaultiaIconType.TRANSFERS -> Quad(
            Brush.linearGradient(listOf(Color(0xFF13896A), Color(0xFF3AC6A0))),
            Color(0xFF6EE7B7).copy(alpha = 0.6f),
            Icons.Default.SyncAlt,
            Color.White
        )
        VaultiaIconType.BACKUP -> Quad(
            Brush.linearGradient(listOf(Color(0xFF5031B8), Color(0xFF6C4CE0))),
            Color(0xFFC4B5FD).copy(alpha = 0.6f),
            Icons.Default.CloudDone,
            Color.White
        )
        VaultiaIconType.SECURITY -> Quad(
            Brush.linearGradient(listOf(Color(0xFFD5B77B), Color(0xFFC38A32))),
            Color(0xFFFDE68A).copy(alpha = 0.6f),
            Icons.Default.Lock,
            Color.White
        )
        VaultiaIconType.MTPROTO -> Quad(
            Brush.linearGradient(listOf(Color(0xFF00B4D8), Color(0xFF0077B6))),
            Color(0xFF90E0EF).copy(alpha = 0.7f),
            Icons.Default.FlashOn,
            Color.White
        )
        VaultiaIconType.BOT_BRIDGE -> Quad(
            Brush.linearGradient(listOf(Color(0xFF8338EC), Color(0xFF3A86FF))),
            Color(0xFFC77DFF).copy(alpha = 0.6f),
            Icons.Default.SmartToy,
            Color.White
        )
    }

    val cornerRadius = (size.value * 0.32f).dp

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.2f),
                spotColor = Color.Black.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(primaryBrush)
            .border(1.2.dp, rimColor, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(glyphSize)
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
