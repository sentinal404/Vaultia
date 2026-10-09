package com.vaultia.app.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.vaultia.app.core.theme.ClayAccentEmerald
import com.vaultia.app.core.theme.ClayPrimary
import com.vaultia.app.core.theme.ClaySecondary
import com.vaultia.app.core.theme.StatusSuccess
import com.vaultia.app.engine.backup.AutoBackupManager
import com.vaultia.app.ui.components.ClayButton
import com.vaultia.app.ui.components.ClayCard
import com.vaultia.app.ui.components.ClayProgressBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoBackupScreen(
    autoBackupManager: AutoBackupManager,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val folders by autoBackupManager.foldersFlow.collectAsState(initial = emptyList())
    val totalBackedUpCount by autoBackupManager.totalBackedUpCountFlow.collectAsState(initial = 0)
    val syncProgress by autoBackupManager.syncProgress.collectAsState()

    var isEnabled by remember { mutableStateOf(autoBackupManager.isEnabled) }
    var wifiOnly by remember { mutableStateOf(autoBackupManager.isWifiOnly) }
    var chargingOnly by remember { mutableStateOf(autoBackupManager.isChargingOnly) }
    var selectedIntervalHours by remember { mutableStateOf(autoBackupManager.intervalHours) }
    var dailyLimitBytes by remember { mutableStateOf(autoBackupManager.dailyDataLimitBytes) }

    val powerManager = remember { context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager }
    var isBatteryIgnored by remember { mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val doc = DocumentFile.fromTreeUri(context, uri)
            val displayName = doc?.name ?: "Folder_${System.currentTimeMillis() % 1000}"
            scope.launch {
                autoBackupManager.addFolder(uri, displayName)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Auto-Backup & Sync", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
            // Master Overview Card
            item {
                ClayCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(ClayPrimary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = ClayPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Automated Vault Sync",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        text = if (isEnabled) "Active in Background" else "Disabled",
                                        fontSize = 12.sp,
                                        color = if (isEnabled) StatusSuccess else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Switch(
                                checked = isEnabled,
                                onCheckedChange = {
                                    isEnabled = it
                                    autoBackupManager.isEnabled = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ClayPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(ClayPrimary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("Monitored Folders", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text("${folders.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ClayPrimary)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(ClayAccentEmerald.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("Files Protected", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text("$totalBackedUpCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ClayAccentEmerald)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        ClayButton(
                            text = if (syncProgress.isSyncing) "Syncing..." else "Sync Now (Incremental)",
                            icon = if (syncProgress.isSyncing) null else Icons.Default.Sync,
                            onClick = {
                                scope.launch {
                                    autoBackupManager.runIncrementalBackup()
                                }
                            },
                            enabled = !syncProgress.isSyncing,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Syncing Live Card
            item {
                AnimatedVisibility(visible = syncProgress.isSyncing || syncProgress.lastSyncMessage.isNotEmpty()) {
                    ClayCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (syncProgress.isSyncing) {
                                    CircularProgressIndicator(
                                        color = ClayPrimary,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (syncProgress.isSyncing) "Differential Scan in Progress" else "Sync Report",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = syncProgress.lastSyncMessage,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            if (syncProgress.totalScanned > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Scanned: ${syncProgress.totalScanned} | Already backed up: ${syncProgress.alreadySynced} | Newly uploaded: ${syncProgress.newlyUploaded}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ClayPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Backup Schedule and Settings
            item {
                Text(
                    text = "BACKUP SCHEDULE & CONDITIONS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClayPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                ClayCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Backup Frequency",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedIntervalHours == 6L,
                                onClick = {
                                    selectedIntervalHours = 6L
                                    autoBackupManager.intervalHours = 6L
                                },
                                label = { Text("6 Hours", fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = selectedIntervalHours == 12L,
                                onClick = {
                                    selectedIntervalHours = 12L
                                    autoBackupManager.intervalHours = 12L
                                },
                                label = { Text("12 Hours", fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = selectedIntervalHours == 24L,
                                onClick = {
                                    selectedIntervalHours = 24L
                                    autoBackupManager.intervalHours = 24L
                                },
                                label = { Text("Daily (24h)", fontSize = 12.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Wi-Fi Only", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Avoid cellular data consumption", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Switch(
                                checked = wifiOnly,
                                onCheckedChange = {
                                    wifiOnly = it
                                    autoBackupManager.isWifiOnly = it
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("When Idle & Charging", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Run when phone is idle on charger (preserves battery)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Switch(
                                checked = chargingOnly,
                                onCheckedChange = {
                                    chargingOnly = it
                                    autoBackupManager.isChargingOnly = it
                                }
                            )
                        }
                    }
                }
            }

            // Daily Backup Data Quota
            item {
                Text(
                    text = "DAILY BACKUP DATA LIMIT",
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
                            Text("Daily Quota", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            val todayUsed = autoBackupManager.getTodayUploadedBytes()
                            Text(
                                text = "Today: ${com.vaultia.app.domain.model.VaultFile.formatBytes(todayUsed)} / ${if (dailyLimitBytes == 0L) "Unlimited" else com.vaultia.app.domain.model.VaultFile.formatBytes(dailyLimitBytes)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val limits = listOf(
                                100L * 1024 * 1024 to "100 MB",
                                250L * 1024 * 1024 to "250 MB",
                                500L * 1024 * 1024 to "500 MB",
                                1024L * 1024 * 1024 to "1 GB",
                                0L to "Unlimited"
                            )
                            limits.forEach { (limitVal, label) ->
                                FilterChip(
                                    selected = dailyLimitBytes == limitVal,
                                    onClick = {
                                        dailyLimitBytes = limitVal
                                        autoBackupManager.dailyDataLimitBytes = limitVal
                                    },
                                    label = { Text(label, fontSize = 10.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "When daily limit is reached, backup pauses and remaining files wait 24h for the next daily run.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Unrestricted Background Execution (Ignore Battery Optimization)
            if (!isBatteryIgnored) {
                item {
                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        surfaceColor = Color(0xFF1E293B).copy(alpha = 0.95f)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = Color(0xFFFBBF24))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Allow Unrestricted Background Run", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Disable battery restrictions so Realme/Android background saver doesn't terminate automatic folder sync.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            ClayButton(
                                text = "Disable Battery Restrictions",
                                icon = Icons.Default.BatteryChargingFull,
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                        isBatteryIgnored = true
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.fillMaxWidth(),
                                isPrimary = true
                            )
                        }
                    }
                }
            }

            // Monitored Folders Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MONITORED FOLDERS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClayPrimary
                    )

                    ClayButton(
                        text = "+ Add Folder",
                        icon = Icons.Default.Add,
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.height(36.dp)
                    )
                }
            }

            if (folders.isEmpty()) {
                item {
                    ClayCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = ClayPrimary.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No folders added yet",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Select DCIM, Camera, Documents, or any folder to automatically protect new files.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(folders, key = { it.id }) { folder ->
                    ClayCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = ClayPrimary,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.displayName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${folder.totalFilesTracked} files synced • Last: ${formatTime(folder.lastBackupTimestamp)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }

                            IconButton(
                                onClick = {
                                    scope.launch { autoBackupManager.removeFolder(folder.id) }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0) return "Never"
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}
