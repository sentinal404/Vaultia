package com.vaultia.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.vaultia.app.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultia.app.domain.model.VaultFile

/**
 * 3D Glassmorphic Folder Component matching User Reference Image 4 & Flaticon 3D Style.
 * Features:
 * - Rich dimensional back flap with angled tab
 * - Realistic layered document sheets peeking out from inside
 * - Frosted glassmorphic front flap with specular rim lighting and drop shadow
 * - High-contrast typography and subtle inner glows
 */
@Composable
fun Vaultia3DFolderCard(
    title: String,
    filesCount: Int,
    sizeBytes: Long? = null,
    baseColor: Color = Color(0xFF3B82F6), // Vibrant Azure / Glass Blue
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cornerRadius = 20.dp

    Box(
        modifier = modifier
            .width(150.dp)
            .height(145.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // LAYER 1: 3D Folder Back Flap with Tab
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 20.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            baseColor.copy(alpha = 0.95f),
                            baseColor.copy(alpha = 0.75f)
                        )
                    )
                )
        ) {
            // Tab highlight
            Box(
                modifier = Modifier
                    .width(62.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(baseColor)
            )
        }

        // LAYER 2: Document sheets peeking out (angled)
        // Back Document (Sheet 1)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = (-12).dp, y = 14.dp)
                .rotate(-7f)
                .width(68.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.88f))
                .border(0.8.dp, Color.White.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                .padding(6.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(baseColor.copy(alpha = 0.45f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color.LightGray.copy(alpha = 0.6f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color.LightGray.copy(alpha = 0.5f))
                )
            }
        }

        // Front Document (Sheet 2)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 14.dp, y = 16.dp)
                .rotate(8f)
                .width(72.dp)
                .height(76.dp)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(0.8.dp, Color.White.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                .padding(6.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(baseColor.copy(alpha = 0.7f))
                    )
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.LightGray.copy(alpha = 0.8f))
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color.LightGray.copy(alpha = 0.7f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color.LightGray.copy(alpha = 0.5f))
                )
            }
        }

        // LAYER 3: Glassmorphic Translucent Front Flap (Matching Image 4)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                    ambientColor = Color.Black.copy(alpha = 0.25f),
                    spotColor = Color.Black.copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            baseColor.copy(alpha = 0.68f),
                            baseColor.copy(alpha = 0.92f)
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.65f),
                            baseColor.copy(alpha = 0.4f)
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$filesCount Files",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    if (sizeBytes != null && sizeBytes > 0) {
                        Text(
                            text = VaultFile.formatBytes(sizeBytes),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3D Floating Illustration (Folder + Cloud + Floating Media Badges)
 * Matching User Reference Images 2 & 5 for Onboarding and Hero Banners.
 */
@Composable
fun Vaultia3DHeroIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 230.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HeroFloat")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Floating"
    )

    Box(
        modifier = modifier
            .size(size)
            .offset(y = floatOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        // Soft ambient radial backlight
        Canvas(modifier = Modifier.size(size * 1.15f)) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF8B5CF6).copy(alpha = 0.35f), Color.Transparent)
                ),
                radius = this.size.minDimension / 2
            )
        }

        // Floating 3D Main Folder (Authentic 3D Glass Folder asset)
        Image(
            painter = painterResource(id = R.drawable.vaultia_folder_3d),
            contentDescription = "Vaultia 3D Folder",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(size * 0.85f)
                .offset(y = 8.dp)
        )

        // Fluffy 3D Cloud anchored in front-left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 10.dp, y = 8.dp)
                .size(width = 96.dp, height = 54.dp)
                .shadow(10.dp, CircleShape)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White, Color(0xFFF1F5F9))
                    )
                )
                .border(1.5.dp, Color.White, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(30.dp)
            )
        }

        // Floating Micro 3D Media Badges (Photos, Music, Videos)
        // Music Badge (Top Left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 14.dp, y = 2.dp)
                .size(38.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFF43F5E), Color(0xFFFB7185))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }

        // Video Badge (Top Right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-8).dp, y = 14.dp)
                .size(42.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF8B5CF6), Color(0xFFA855F7))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }

        // Document Badge (Right Side)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 12.dp, y = (-10).dp)
                .size(36.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF10B981), Color(0xFF34D399))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}
