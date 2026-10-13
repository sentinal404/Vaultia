package com.vaultia.app.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultia.app.core.theme.ClayPrimary
import com.vaultia.app.ui.components.Vaultia3DHeroIllustration
import com.vaultia.app.ui.components.VaultiaBrandLogo

data class OnboardingSlideData(
    val title: String,
    val subtitle: String,
    val badge: String
)

/**
 * First-Time User Welcome Carousel Screen matching User Reference Images 2 & 5.
 * Features:
 * - Top brand bar with Skip action
 * - 3D floating folder and cloud illustration
 * - Responsive dot & pill progress indicators
 * - Smooth step navigation with "Get Started" and pulsating forward button
 */
@Composable
fun WelcomeOnboardingScreen(
    onComplete: () -> Unit
) {
    val slides = remember {
        listOf(
            OnboardingSlideData(
                title = "Upload Your Folders",
                subtitle = "Upload a folder or file to our app and be sure to find everything very quickly with infinite cloud capacity.",
                badge = "DECENTRALIZED STORAGE"
            ),
            OnboardingSlideData(
                title = "Manage Your Files\nIn a Simple Way",
                subtitle = "You could integrate your local files with the files in cloud storage, synced across devices seamlessly.",
                badge = "CLOUD SYNC"
            ),
            OnboardingSlideData(
                title = "Zero-Buffering\n4K & Audio Stream",
                subtitle = "Play your video movies and hi-res lossless music directly from high-speed MTProto cloud channels without downloading.",
                badge = "STREAMING ENGINE"
            )
        )
    }

    var currentSlideIndex by remember { mutableIntStateOf(0) }
    val isLastSlide = currentSlideIndex == slides.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF2E1744),
                        Color(0xFF1B0D2D),
                        Color(0xFF0F0619)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HEADER: Brand Logo + Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VaultiaBrandLogo(size = 34.dp)
                    Text(
                        text = "Vaultia Cloud",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                }

                TextButton(onClick = onComplete) {
                    Text(
                        text = "Skip",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CENTER 3D HERO ILLUSTRATION (Matching Images 2 & 5)
            Vaultia3DHeroIllustration(
                size = 260.dp,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // TEXT CONTENT
            val currentSlide = slides[currentSlideIndex]

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.25f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = currentSlide.badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFC084FC),
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = currentSlide.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = currentSlide.subtitle,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // BOTTOM CONTROLS: Dot Indicators + Next / Get Started Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated Dots / Pill Indicator (Matching Image 2: — o o)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    slides.indices.forEach { index ->
                        val isSelected = index == currentSlideIndex
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 7.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "DotWidth"
                        )

                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White else Color.White.copy(alpha = 0.25f)
                                )
                                .clickable { currentSlideIndex = index }
                        )
                    }
                }

                // Action Button (Next or Get Started)
                if (isLastSlide) {
                    Button(
                        onClick = onComplete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1B0D2D)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .shadow(8.dp, RoundedCornerShape(20.dp))
                    ) {
                        Text(
                            text = "Get Started",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                } else {
                    // Pulsating Glowing Next Button with Arrow (Matching Image 5)
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(10.dp, CircleShape, spotColor = Color(0xFFEC4899))
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))
                                )
                            )
                            .clickable {
                                if (currentSlideIndex < slides.size - 1) {
                                    currentSlideIndex++
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
