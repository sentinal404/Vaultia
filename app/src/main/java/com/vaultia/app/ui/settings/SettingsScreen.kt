package com.vaultia.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SyncAlt
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
import com.vaultia.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    secureStorage: SecureStorageManager,
    settingsViewModel: SettingsViewModel,
    onLogout: () -> Unit,
    onNavigateTab: (String) -> Unit,
    onNavigateToAutoBackup: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {}
) {
    val settings by settingsViewModel.settings.collectAsState()
    val workers by settingsViewModel.workers.collectAsState()
    val addWorkerResult by settingsViewModel.addWorkerResult.collectAsState()

    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var workerTokenInput by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            VaultiaTopAppBar(title = "Settings")
        },
        bottomBar = {
            VaultiaBottomNav(
                currentRoute = "settings",
                onNavigate = onNavigateTab
            )
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

                        Spacer(modifier = Modifier.height(14.dp))

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
                        Text(
                            text = "Parallel Workers: ${settings.maxWorkers}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Slider(
                            value = settings.maxWorkers.toFloat(),
                            onValueChange = { settingsViewModel.updateMaxWorkers(it.toInt()) },
                            valueRange = 1f..5f,
                            steps = 3,
                            colors = SliderDefaults.colors(thumbColor = ClayPrimary, activeTrackColor = ClayPrimary)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Chunk Size: ${settings.chunkSizeMb} MB",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Slider(
                            value = settings.chunkSizeMb.toFloat(),
                            onValueChange = { settingsViewModel.updateChunkSizeMb(it.toInt()) },
                            valueRange = 5f..45f,
                            steps = 7,
                            colors = SliderDefaults.colors(thumbColor = ClayPrimary, activeTrackColor = ClayPrimary)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Wi-Fi Only Uploads", fontSize = 14.sp)
                            Switch(
                                checked = settings.wifiOnlyUploads,
                                onCheckedChange = { settingsViewModel.setWifiOnlyUploads(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = ClayPrimary)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto Resume Transfers", fontSize = 14.sp)
                            Switch(
                                checked = settings.autoResume,
                                onCheckedChange = { settingsViewModel.setAutoResume(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = ClayPrimary)
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("App PIN Lock", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Require PIN to open Vaultia", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Switch(
                                checked = settings.isAppLockEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        showPinDialog = true
                                    } else {
                                        settingsViewModel.setAppLock(false)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = ClayPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Biometric Unlock", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Use fingerprint / face unlock", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Switch(
                                checked = settings.isBiometricEnabled,
                                onCheckedChange = { settingsViewModel.setBiometric(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = ClayPrimary)
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
                                Text("Vaultia v1.0.0", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Built by Rishi Shakya • Open Source", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
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
}
