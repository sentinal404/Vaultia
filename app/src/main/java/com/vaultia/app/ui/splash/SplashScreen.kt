package com.vaultia.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultia.app.core.security.SecureStorageManager
import com.vaultia.app.core.theme.ClayPrimary
import com.vaultia.app.core.theme.claymorphic
import com.vaultia.app.data.repository.VaultRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun SplashScreen(
    secureStorage: SecureStorageManager,
    vaultRepository: VaultRepository,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToLock: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val scale = remember { Animatable(0.7f) }
    var statusText by remember { mutableStateOf("Initializing Vaultia...") }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.05f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )

        delay(400)

        // Check local configuration
        if (!secureStorage.isTelegramConfigured) {
            statusText = "Setup required..."
            delay(300)
            onNavigateToOnboarding()
            return@LaunchedEffect
        }

        // App Lock check
        if (secureStorage.isAppLockEnabled) {
            onNavigateToLock()
            return@LaunchedEffect
        }

        // Validate Telegram & reload workers with a graceful 8-second timeout
        statusText = "Connecting to Telegram backend..."
        val token = secureStorage.botToken ?: ""
        val channelId = secureStorage.channelId ?: ""

        withTimeoutOrNull(8000L) {
            try {
                vaultRepository.reloadWorkers()
                vaultRepository.validateTelegramConnection(token, channelId)
            } catch (_: Exception) {}
        }

        statusText = "Ready"
        delay(200)
        onNavigateToDashboard()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            com.vaultia.app.ui.components.VaultiaBrandLogo(
                size = 110.dp,
                modifier = Modifier.scale(scale.value)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "VAULTIA",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Private Cloud Storage Vault",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(36.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = ClayPrimary,
                strokeWidth = 2.5.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = statusText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}
