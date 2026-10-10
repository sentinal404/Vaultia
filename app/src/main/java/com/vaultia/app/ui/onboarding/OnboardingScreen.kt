package com.vaultia.app.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultia.app.core.VaultResult
import com.vaultia.app.core.theme.ClayPrimary
import com.vaultia.app.core.theme.StatusError
import com.vaultia.app.core.theme.StatusSuccess
import com.vaultia.app.ui.components.ClayButton
import com.vaultia.app.ui.components.ClayCard
import com.vaultia.app.ui.components.VaultiaBrandLogo
import com.vaultia.app.ui.viewmodel.VaultViewModel

@Composable
fun OnboardingScreen(
    viewModel: VaultViewModel,
    onSetupComplete: () -> Unit
) {
    var botToken by remember { mutableStateOf("") }
    var channelId by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }

    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        VaultiaBrandLogo(size = 80.dp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome to Vaultia",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Your private cloud storage powered by Telegram",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Step-by-Step Instructions Card
        ClayCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick Setup Guide",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ClayPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                StepRow(step = "1", text = "Open Telegram & search for @BotFather")
                StepRow(step = "2", text = "Create a new bot using /newbot & copy the API token")
                StepRow(step = "3", text = "Create a new PRIVATE channel in Telegram")
                StepRow(step = "4", text = "Add your bot as an Administrator with full posting rights")
                StepRow(step = "5", text = "Paste the bot token and channel ID below")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Credentials Input Card
        ClayCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Telegram Credentials",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = botToken,
                    onValueChange = { botToken = it.trim() },
                    label = { Text("Bot Token (from @BotFather)") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = ClayPrimary) },
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClayPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = channelId,
                    onValueChange = { channelId = it.trim() },
                    label = { Text("Private Channel ID (e.g. -1001234567890)") },
                    leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, tint = ClayPrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClayPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "🔒 Credentials are encrypted on this device using Android Keystore and never sent to any third-party server.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Live Validation Feedback
        AnimatedVisibility(visible = connectionStatus != null) {
            when (val status = connectionStatus) {
                is VaultResult.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ClayPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Verifying credentials with Telegram Bot API...", fontSize = 13.sp)
                    }
                }
                is VaultResult.Success -> {
                    val config = status.data
                    ClayCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Storage Backend Ready!", fontWeight = FontWeight.Bold, color = StatusSuccess)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("✓ Bot: @${config.botUsername} (${config.botName})", fontSize = 13.sp)
                            Text("✓ Channel: ${config.channelTitle} (${config.channelId})", fontSize = 13.sp)
                            Text("✓ Administrator rights confirmed", fontSize = 13.sp)
                        }
                    }
                }
                is VaultResult.Error -> {
                    ClayCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusError)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Validation Failed", fontWeight = FontWeight.Bold, color = StatusError)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(status.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
                else -> Unit
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        ClayButton(
            text = "Connect & Sync",
            onClick = {
                viewModel.validateAndConnect(botToken, channelId) { success ->
                    if (success) {
                        viewModel.triggerCloudSync()
                        onSetupComplete()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = botToken.isNotBlank() && channelId.isNotBlank() && connectionStatus !is VaultResult.Loading
        )

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun StepRow(step: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(ClayPrimary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = step, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ClayPrimary)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
    }
}
