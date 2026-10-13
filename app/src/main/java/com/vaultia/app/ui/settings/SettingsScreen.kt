package com.vaultia.app.ui.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HelpOutline
import com.vaultia.app.ui.components.Vaultia3DIcon
import com.vaultia.app.ui.components.VaultiaIconType
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.vaultia.app.core.theme.ClayCyan
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultia.app.core.VaultResult
import com.vaultia.app.core.security.SecureStorageManager
import com.vaultia.app.core.theme.ClayPrimary
import com.vaultia.app.core.theme.StatusError
import com.vaultia.app.core.theme.StatusSuccess
import com.vaultia.app.core.theme.StatusWarning
import com.vaultia.app.data.repository.ThemeMode
import com.vaultia.app.ui.components.ClayButton
import com.vaultia.app.ui.components.ClayCard
import com.vaultia.app.ui.components.VaultiaBottomNav
import com.vaultia.app.ui.components.VaultiaTopAppBar
import com.vaultia.app.ui.components.VaultiaSwitch
import com.vaultia.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    secureStorage: SecureStorageManager,
    settingsViewModel: SettingsViewModel,
    onLogout: () -> Unit,
    onNavigateTab: (String) -> Unit,
    onNavigateToAutoBackup: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToWelcome: () -> Unit = {}
) {
    val settings by settingsViewModel.settings.collectAsState()
    val workers by settingsViewModel.workers.collectAsState()
    val addWorkerResult by settingsViewModel.addWorkerResult.collectAsState()
    val clientStatus by settingsViewModel.telegramClientStatus.collectAsState()
    val isTestingMtproto by settingsViewModel.isTestingMtproto.collectAsState()
    val bridgeState by settingsViewModel.bridgeState.collectAsState()

    var selectedDurationMinutes by remember { mutableStateOf(10) }

    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var workerTokenInput by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    var apiIdInput by remember { mutableStateOf(secureStorage.apiId ?: "") }
    var apiHashInput by remember { mutableStateOf(secureStorage.apiHash ?: "") }
    var customUrlInput by remember { mutableStateOf(secureStorage.customApiUrl ?: "") }
    var isApiIdVisible by remember { mutableStateOf(false) }
    var isApiHashVisible by remember { mutableStateOf(false) }
    var showApiHelpDialog by remember { mutableStateOf(false) }
    var credentialsSavedMessage by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            VaultiaTopAppBar(title = "Settings")
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme & Appearance Section
            item {
                Text(
                    text = "APPEARANCE & THEME",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClayPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                ClayCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Color Palette",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Select your preferred visual atmosphere",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Light Mode (Pearl Titanium Luxury)
                            val isLightSelected = settings.themeMode == ThemeMode.LIGHT
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(androidx.compose.ui.graphics.Color(0xFFF7F6F2))
                                    .border(
                                        width = if (isLightSelected) 2.dp else 1.dp,
                                        color = if (isLightSelected) ClayPrimary else androidx.compose.ui.graphics.Color(0xFFE6E3DC),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { settingsViewModel.setThemeMode(ThemeMode.LIGHT) }
                                    .padding(10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(0xFF6C4CE0)))
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(0xFF43C9D9)))
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(0xFFD5B77B)))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Pearl Light", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF191A28))
                                    Text("Titanium", fontSize = 10.sp, color = androidx.compose.ui.graphics.Color(0xFF727487))
                                    if (isLightSelected) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("● Active", fontSize = 10.sp, color = ClayPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Dark Mode (Obsidian Aurora)
                            val isDarkSelected = settings.themeMode == ThemeMode.DARK
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(androidx.compose.ui.graphics.Color(0xFF0B0D13))
                                    .border(
                                        width = if (isDarkSelected) 2.dp else 1.dp,
                                        color = if (isDarkSelected) androidx.compose.ui.graphics.Color(0xFFA38AFF) else androidx.compose.ui.graphics.Color(0xFF202437),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { settingsViewModel.setThemeMode(ThemeMode.DARK) }
                                    .padding(10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(0xFFA38AFF)))
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(0xFF53DCE8)))
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(0xFFD7B77A)))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Obsidian", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                                    Text("Aurora Dark", fontSize = 10.sp, color = androidx.compose.ui.graphics.Color(0xFFA5A9BC))
                                    if (isDarkSelected) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("● Active", fontSize = 10.sp, color = androidx.compose.ui.graphics.Color(0xFFA38AFF), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // System Mode
                            val isSystemSelected = settings.themeMode == ThemeMode.SYSTEM
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = if (isSystemSelected) 2.dp else 1.dp,
                                        color = if (isSystemSelected) ClayPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { settingsViewModel.setThemeMode(ThemeMode.SYSTEM) }
                                    .padding(10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("System", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Auto-match", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    if (isSystemSelected) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("● Active", fontSize = 10.sp, color = ClayPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Telegram Configuration Section
            item {
                Text(
                    text = "TELEGRAM BACKEND",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClayPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                ClayCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = secureStorage.channelTitle ?: "Private Channel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Channel ID: ${secureStorage.channelId ?: "Not set"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Bot: @${secureStorage.botUsername ?: "bot"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ClayButton(
                                text = "Sync Cloud",
                                icon = Icons.Default.CloudSync,
                                onClick = {
                                    com.vaultia.app.VaultiaApplication.instance.cloudSyncManager.performFullSync()
                                },
                                isPrimary = true,
                                modifier = Modifier.weight(1f)
                            )
                            ClayButton(
                                text = "Deep Scan",
                                icon = Icons.Default.SyncAlt,
                                onClick = {
                                    com.vaultia.app.VaultiaApplication.instance.cloudSyncManager.performFullSync(forceDeepScan = true)
                                },
                                isPrimary = false,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        ClayButton(
                            text = "Delete Configuration",
                            icon = Icons.Default.Delete,
                            onClick = { showDeleteConfirmDialog = true },
                            isPrimary = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Telegram MTProto Direct Cloud Engine Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Vaultia3DIcon(type = VaultiaIconType.MTPROTO, size = 26.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MTPROTO DIRECT CLOUD ENGINE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ClayPrimary
                        )
                    }
                    IconButton(
                        onClick = { showApiHelpDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.HelpOutline,
                            contentDescription = "Help Guide",
                            tint = ClayCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                ClayCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Telegram MTProto Engine",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Direct high-speed DC connection. Bypasses 20MB bot limit for zero-buffering 4K/movies streaming and unlimited files.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Connection Status Pill
                        val statusText = when {
                            isTestingMtproto -> "Probing Telegram Data Centers..."
                            clientStatus.state == com.vaultia.app.engine.core.TelegramConnectionState.CONNECTED || clientStatus.state == com.vaultia.app.engine.core.TelegramConnectionState.AUTHORIZED ->
                                "Connected DC ${clientStatus.connectedDc} • ${clientStatus.latencyMs}ms"
                            clientStatus.state == com.vaultia.app.engine.core.TelegramConnectionState.ERROR ->
                                "Connection Failed: ${clientStatus.errorMessage ?: "Check credentials"}"
                            !secureStorage.apiId.isNullOrBlank() -> "MTProto Configured (DC 5 Ready)"
                            else -> "MTProto Unconfigured"
                        }
                        val statusColor = when {
                            isTestingMtproto -> ClayCyan
                            clientStatus.state == com.vaultia.app.engine.core.TelegramConnectionState.CONNECTED || clientStatus.state == com.vaultia.app.engine.core.TelegramConnectionState.AUTHORIZED -> StatusSuccess
                            clientStatus.state == com.vaultia.app.engine.core.TelegramConnectionState.ERROR -> StatusError
                            !secureStorage.apiId.isNullOrBlank() -> ClayCyan
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusColor.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isTestingMtproto) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = statusColor)
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(statusText, fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = apiIdInput,
                            onValueChange = { 
                                apiIdInput = it.trim()
                                credentialsSavedMessage = false
                            },
                            label = { Text("Telegram API ID") },
                            placeholder = { Text("e.g. 12345678") },
                            singleLine = true,
                            visualTransformation = if (isApiIdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isApiIdVisible = !isApiIdVisible }) {
                                    Icon(
                                        if (isApiIdVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = apiHashInput,
                            onValueChange = { 
                                apiHashInput = it.trim()
                                credentialsSavedMessage = false
                            },
                            label = { Text("Telegram API Hash") },
                            placeholder = { Text("e.g. 0123456789abcdef0123456789abcdef") },
                            singleLine = true,
                            visualTransformation = if (isApiHashVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isApiHashVisible = !isApiHashVisible }) {
                                    Icon(
                                        if (isApiHashVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customUrlInput,
                            onValueChange = { 
                                customUrlInput = it.trim()
                                credentialsSavedMessage = false
                            },
                            label = { Text("Custom Bot API / MTProto Server URL (Optional)") },
                            placeholder = { Text("https://api.telegram.org (Default)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ClayButton(
                                text = if (isTestingMtproto) "Testing..." else "Test Connection",
                                icon = Icons.Default.FlashOn,
                                onClick = { settingsViewModel.testMtprotoConnection() },
                                isPrimary = false,
                                modifier = Modifier.weight(1f)
                            )

                            ClayButton(
                                text = if (credentialsSavedMessage) "Saved!" else "Save Credentials",
                                icon = Icons.Default.Save,
                                onClick = {
                                    settingsViewModel.saveTelegramApiCredentials(
                                        apiId = apiIdInput,
                                        apiHash = apiHashInput,
                                        customApiUrl = customUrlInput
                                    )
                                    credentialsSavedMessage = true
                                },
                                isPrimary = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Telegram Bot Bridge (Isolated Ingestion Mode) Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Vaultia3DIcon(type = VaultiaIconType.BOT_BRIDGE, size = 26.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BOT BRIDGE SERVER (ISOLATED INGESTION)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClayPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                ClayCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val isBridgeRunning = bridgeState.isRunning
                        val remainingSec = bridgeState.remainingSeconds

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Telegram Chat Receiver",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isBridgeRunning) "Actively listening for files shared to @${secureStorage.botUsername ?: "bot"}" else "Server is currently sleeping (Zero battery drain)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isBridgeRunning) StatusSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isBridgeRunning) "RUNNING (${remainingSec / 60}m ${remainingSec % 60}s)" else "STOPPED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBridgeRunning) StatusSuccess else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ℹ️ Note: Vaultia strictly isolates the bot. Standard app uploads, downloads, and streaming run exclusively through direct MTProto without bot limits.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Duration Selector Pills
                        if (!isBridgeRunning) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(10, 30, 60).forEach { mins ->
                                    val isSelected = selectedDurationMinutes == mins
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) ClayPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) ClayPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { selectedDurationMinutes = mins }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${mins} Min",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) ClayPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        ClayButton(
                            text = if (isBridgeRunning) "Stop Server" else "Start Server ($selectedDurationMinutes min)",
                            icon = if (isBridgeRunning) Icons.Default.SmartToy else Icons.Default.FlashOn,
                            onClick = {
                                if (isBridgeRunning) {
                                    settingsViewModel.stopBridgeServer()
                                } else {
                                    settingsViewModel.startBridgeServer(selectedDurationMinutes)
                                }
                            },
                            isPrimary = !isBridgeRunning,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Worker Bots Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WORKER BOTS (${workers.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClayPrimary
                    )
                    TextButton(onClick = { showAddWorkerDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Worker", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    workers.forEach { worker ->
                        ClayCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.SmartToy,
                                        contentDescription = null,
                                        tint = if (worker.isPrimary) ClayPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = worker.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                            if (worker.isPrimary) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "(Primary)",
                                                    fontSize = 11.sp,
                                                    color = ClayPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val statusIcon = if (worker.isRateLimited) Icons.Default.Warning else Icons.Default.CheckCircle
                                            val statusColor = if (worker.isRateLimited) StatusWarning else StatusSuccess
                                            val statusText = if (worker.isRateLimited) "Rate limited" else "Online & Authorized"

                                            Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(statusText, fontSize = 11.sp, color = statusColor)
                                        }
                                    }
                                }

                                if (!worker.isPrimary) {
                                    IconButton(onClick = { settingsViewModel.removeWorker(worker.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = StatusError)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Worker Recommendation Tip
                ClayCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = ClayCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "💡 Tip: Add extra Worker Bots via @BotFather. Vaultia utilizes all active worker bots in parallel to cut streaming buffering to zero and speed up downloads.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Transfer Settings Section
            item {
                Text(
                    text = "TRANSFERS & CONCURRENCY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClayPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                ClayCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("In-App Chunk Size", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Auto-split for instant zero-buffering playback", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Text(
                                text = "5 MB (Ultra-Fast)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ClayCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Parallel Worker Scheduling", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Automatically utilizes all available bot workers", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Text(
                                text = "Active (${workers.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ClayPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { settingsViewModel.setWifiOnlyUploads(!settings.wifiOnlyUploads) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Wi-Fi Only Uploads", fontSize = 14.sp)
                            VaultiaSwitch(
                                checked = settings.wifiOnlyUploads,
                                onCheckedChange = { settingsViewModel.setWifiOnlyUploads(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { settingsViewModel.setAutoResume(!settings.autoResume) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto Resume Transfers", fontSize = 14.sp)
                            VaultiaSwitch(
                                checked = settings.autoResume,
                                onCheckedChange = { settingsViewModel.setAutoResume(it) }
                            )
                        }
                    }
                }
            }

            // Security Section
            item {
                Text(
                    text = "SECURITY & VAULT LOCK",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClayPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                ClayCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (!settings.isAppLockEnabled) {
                                        showPinDialog = true
                                    } else {
                                        settingsViewModel.setAppLock(false)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("App PIN Lock", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Require PIN to open Vaultia", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            VaultiaSwitch(
                                checked = settings.isAppLockEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        showPinDialog = true
                                    } else {
                                        settingsViewModel.setAppLock(false)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { settingsViewModel.setBiometric(!settings.isBiometricEnabled) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Biometric Unlock", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Use fingerprint / face unlock", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            VaultiaSwitch(
                                checked = settings.isBiometricEnabled,
                                onCheckedChange = { settingsViewModel.setBiometric(it) }
                            )
                        }
                    }
                }
            }

            // Auto-Backup Section
            item {
                Text(
                    text = "AUTOMATED BACKUP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClayPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                ClayCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = ClayPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Folder Auto-Backup", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Differential backup for camera & device folders", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        ClayButton(
                            text = "Configure Auto-Backup",
                            icon = Icons.Default.CloudSync,
                            onClick = onNavigateToAutoBackup,
                            modifier = Modifier.fillMaxWidth(),
                            isPrimary = true
                        )
                    }
                }
            }

            // About Section
            item {
                Text(
                    text = "ABOUT & DEVELOPER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClayPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                ClayCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = ClayPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Vaultia v2.0.0 (Evolution Edition)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Built by Rishi Shakya • Open Source", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        ClayButton(
                            text = "Showcase Welcome & Feature Tour",
                            icon = Icons.Default.CloudSync,
                            onClick = onNavigateToWelcome,
                            modifier = Modifier.fillMaxWidth(),
                            isPrimary = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        ClayButton(
                            text = "About Vaultia & Developer Profile",
                            icon = Icons.Default.Info,
                            onClick = onNavigateToAbout,
                            modifier = Modifier.fillMaxWidth(),
                            isPrimary = false
                        )
                    }
                }
            }

            // Privacy Notice
            item {
                ClayCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Privacy Guarantee", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ClayPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Your bot tokens are stored in Android Keystore.\n• All files are stored directly in your private channel.\n• Vaultia has no central servers and never sends telemetry or keys.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }

    // Add Worker Dialog
    if (showAddWorkerDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddWorkerDialog = false
                settingsViewModel.clearAddWorkerResult()
            },
            title = { Text("Add Worker Bot") },
            text = {
                Column {
                    Text("Add a secondary bot added as Admin to the same private channel to distribute upload traffic.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = workerTokenInput,
                        onValueChange = { workerTokenInput = it.trim() },
                        label = { Text("Worker Bot Token") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (addWorkerResult is VaultResult.Loading) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validating worker...", fontSize = 12.sp)
                        }
                    } else if (addWorkerResult is VaultResult.Error) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text((addWorkerResult as VaultResult.Error).message, color = StatusError, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.addWorker(workerTokenInput)
                    },
                    enabled = workerTokenInput.isNotBlank() && addWorkerResult !is VaultResult.Loading
                ) {
                    Text("Verify & Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddWorkerDialog = false
                    settingsViewModel.clearAddWorkerResult()
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Set PIN Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set App Lock PIN") },
            text = {
                Column {
                    Text("Enter a 4-6 digit numeric PIN to protect Vaultia.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it.filter { char -> char.isDigit() } },
                        label = { Text("PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinInput.length >= 4) {
                            settingsViewModel.setPin(pinInput)
                            showPinDialog = false
                        }
                    },
                    enabled = pinInput.length >= 4
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Configuration?") },
            text = { Text("This will remove all stored bot tokens, wipe local cached database records, and reset Vaultia back to setup. Remote Telegram channel files will remain untouched.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    settingsViewModel.deleteConfiguration(onLogout)
                }) {
                    Text("Delete Everything", color = StatusError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Telegram API ID & Hash Setup Guide Dialog
    if (showApiHelpDialog) {
        AlertDialog(
            onDismissRequest = { showApiHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = ClayCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("How to Get API ID & Hash", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "To enable instant streaming of large files (> 20MB) forwarded or shared directly to your Telegram bot, Telegram requires an API ID & Hash:",
                        fontSize = 13.sp
                    )
                    Text(
                        text = "1. Open https://my.telegram.org in your web browser.\n" +
                                "2. Log in using your Telegram phone number.\n" +
                                "3. Click on 'API development tools'.\n" +
                                "4. Enter any App title (e.g. 'Vaultia') and Short name, then click 'Create application'.\n" +
                                "5. Copy your api_id (numeric) and api_hash (32-character string).\n" +
                                "6. Paste them into Vaultia and tap 'Save Credentials'.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🔒 Your credentials are encrypted on-device with hardware AES-256 (Android Keystore).",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ClayCyan
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showApiHelpDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}
