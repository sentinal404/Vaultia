package com.vaultia.app.ui.lock

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultia.app.core.security.AppLockManager
import com.vaultia.app.core.theme.ClayPrimary
import com.vaultia.app.core.theme.StatusError
import com.vaultia.app.core.theme.claymorphic
import com.vaultia.app.ui.components.ClayCard

@Composable
fun LockScreen(
    appLockManager: AppLockManager,
    onUnlocked: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity

    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun triggerBiometric() {
        if (activity != null && com.vaultia.app.core.security.BiometricAuthUtil.isBiometricAvailable(activity)) {
            com.vaultia.app.core.security.BiometricAuthUtil.authenticate(
                activity = activity,
                onSuccess = {
                    appLockManager.unlockBiometrically()
                    onUnlocked()
                },
                onError = {
                    // Fingerprint error or not recognized
                }
            )
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        triggerBiometric()
    }

    fun onKeyPress(digit: String) {
        if (pin.length < 6) {
            val newPin = pin + digit
            pin = newPin
            isError = false
            if (newPin.length >= 4) {
                if (appLockManager.unlockWithPin(newPin)) {
                    onUnlocked()
                } else if (newPin.length == 6) {
                    isError = true
                    pin = ""
                }
            }
        }
    }

    fun onBackspace() {
        if (pin.isNotEmpty()) {
            pin = pin.dropLast(1)
            isError = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .claymorphic(shape = CircleShape, elevation = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = ClayPrimary, modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Vault Locked",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = if (isError) "Incorrect PIN. Try again." else "Enter your security PIN to unlock",
            color = if (isError) StatusError else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // PIN Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val isFilled = index < pin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            if (isFilled) ClayPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // PIN Keypad 3x4
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("bio", "0", "del")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            keys.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        when (key) {
                            "bio" -> {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clickable { triggerBiometric() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = "Biometric", tint = ClayPrimary, modifier = Modifier.size(32.dp))
                                }
                            }
                            "del" -> {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clickable { onBackspace() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .claymorphic(shape = CircleShape, elevation = 4.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { onKeyPress(key) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
