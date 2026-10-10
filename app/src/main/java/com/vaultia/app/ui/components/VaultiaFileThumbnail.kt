package com.vaultia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vaultia.app.domain.model.MediaType
import com.vaultia.app.domain.model.VaultFile
import java.io.File

/**
 * Premium Futuristic Holographic File Thumbnail Component for Vaultia.
 * Custom crafted cyber visuals for Videos, Audio, Images, Documents, and Archives.
 */
@Composable
fun VaultiaFileThumbnail(
    file: VaultFile,
    modifier: Modifier = Modifier,
    height: Dp = 105.dp
) {
    val cornerShape = RoundedCornerShape(14.dp)
    val localFile = file.localUri?.let { File(it) }
    val hasLocalPreview = localFile?.exists() == true && (file.mediaType == MediaType.IMAGES || file.mediaType == MediaType.VIDEOS)

    val (bgGradient, rimColor, badgeColor, badgeText, icon) = when (file.mediaType) {
        MediaType.VIDEOS -> Quintuple(
            Brush.linearGradient(listOf(Color(0xFF240046), Color(0xFF10002B), Color(0xFF3C096C))),
            Color(0xFF9D4EDD).copy(alpha = 0.6f),
            Color(0xFF7B2CBF),
            "4K VIDEO",
            Icons.Default.Movie
        )
        MediaType.AUDIO -> Quintuple(
            Brush.linearGradient(listOf(Color(0xFF3A015C), Color(0xFF1E0124), Color(0xFF7209B7))),
            Color(0xFFFF007F).copy(alpha = 0.6f),
            Color(0xFFF72585),
            "HI-RES AUDIO",
            Icons.Default.Audiotrack
        )
        MediaType.IMAGES -> Quintuple(
            Brush.linearGradient(listOf(Color(0xFF003566), Color(0xFF001D3D), Color(0xFF0077B6))),
            Color(0xFF00B4D8).copy(alpha = 0.6f),
            Color(0xFF0096C7),
            "PHOTO",
            Icons.Default.Image
        )
        MediaType.DOCUMENTS -> Quintuple(
            Brush.linearGradient(listOf(Color(0xFF1B263B), Color(0xFF0D1B2A), Color(0xFF415A77))),
            Color(0xFF778DA9).copy(alpha = 0.6f),
            Color(0xFF415A77),
            "DOCUMENT",
            Icons.Default.Description
        )
        else -> Quintuple(
            Brush.linearGradient(listOf(Color(0xFF2B2D42), Color(0xFF1B1B1E), Color(0xFF4A4E69))),
            Color(0xFF8D99AE).copy(alpha = 0.6f),
            Color(0xFF6C757D),
            "FILE",
            Icons.Default.Folder
        )
    }

    Box(
        modifier = modifier
            .height(height)
            .clip(cornerShape)
            .background(bgGradient)
            .border(1.2.dp, rimColor, cornerShape),
        contentAlignment = Alignment.Center
    ) {
        if (hasLocalPreview) {
            AsyncImage(
                model = localFile,
                contentDescription = file.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Subtle dark vignette gradient over local image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                        )
                    )
            )
        } else {
            // Futuristic Cyber Center Graphic
            if (file.mediaType == MediaType.VIDEOS) {
                // Sleek glowing play button for videos
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(badgeColor, badgeColor.copy(alpha = 0.4f))
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .padding(start = 2.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.35f))
                        .border(1.2.dp, rimColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Cyber Category Badge Pill at Bottom-Start
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .border(0.8.dp, rimColor, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = badgeText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
        }

        // Extension / Size Pill at Bottom-End
        val ext = file.extension.uppercase().take(4)
        if (ext.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.75f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = ext,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
