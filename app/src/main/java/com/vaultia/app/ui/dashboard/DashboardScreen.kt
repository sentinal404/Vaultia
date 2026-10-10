package com.vaultia.app.ui.dashboard

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vaultia.app.VaultiaApplication
import com.vaultia.app.core.theme.ClayAccentEmerald
import com.vaultia.app.core.theme.StatusError
import com.vaultia.app.core.theme.StatusSuccess
import com.vaultia.app.core.theme.VioletDuskBg
import com.vaultia.app.core.theme.VioletDuskCream
import com.vaultia.app.core.theme.VioletDuskPeach
import com.vaultia.app.core.theme.VioletDuskPlum
import com.vaultia.app.core.theme.VioletDuskRose
import com.vaultia.app.data.local.entity.FolderEntity
import com.vaultia.app.domain.model.MediaType
import com.vaultia.app.domain.model.VaultFile
import com.vaultia.app.ui.components.ClayButton
import com.vaultia.app.ui.components.ClayCard
import com.vaultia.app.ui.components.Vaultia3DIcon
import com.vaultia.app.ui.components.VaultiaIconType
import com.vaultia.app.ui.components.VaultiaBottomNav
import com.vaultia.app.ui.components.VaultiaBrandLogo
import com.vaultia.app.ui.viewmodel.TransferViewModel
import com.vaultia.app.ui.viewmodel.VaultViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun DashboardScreen(
    vaultViewModel: VaultViewModel,
    transferViewModel: TransferViewModel,
    onNavigateToMedia: (file: VaultFile) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateTab: (String) -> Unit,
    onNavigateToAutoBackup: () -> Unit = {}
) {
    val context = LocalContext.current
    val totalStorageUsed by vaultViewModel.totalStorageUsed.collectAsState()
    val totalFilesCount by vaultViewModel.totalFilesCount.collectAsState()
    val allFiles by vaultViewModel.allFiles.collectAsState()
    val folders by vaultViewModel.allFolders.collectAsState()
    val transfers by transferViewModel.transfers.collectAsState()
    val syncState by (vaultViewModel.syncState?.collectAsState() ?: remember { mutableStateOf(null) })
    val isSyncing = syncState?.isSyncing == true

    val bridgeServer = VaultiaApplication.instance.bridgeServer
    val bridgeState by bridgeServer.serverState.collectAsState()
    val botUsername = VaultiaApplication.instance.secureStorage.botUsername ?: "VaultiaBot"

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var selectedFolderColor by remember { mutableStateOf("#8B5CF6") }

    // Media type statistics
    val videoFiles = allFiles.filter { it.mediaType == MediaType.VIDEOS }
    val imageFiles = allFiles.filter { it.mediaType == MediaType.IMAGES }
    val audioFiles = allFiles.filter { it.mediaType == MediaType.AUDIO }
    val docFiles = allFiles.filter { it.mediaType == MediaType.DOCUMENTS }

    val videoBytes = videoFiles.sumOf { it.size }
    val imageBytes = imageFiles.sumOf { it.size }
    val audioBytes = audioFiles.sumOf { it.size }
    val docBytes = docFiles.sumOf { it.size }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileName = getFileNameFromUri(context, uri) ?: "upload_${System.currentTimeMillis()}.bin"
                val tempFile = File(context.cacheDir, fileName)
                FileOutputStream(tempFile).use { out ->
                    inputStream?.copyTo(out)
                }
                vaultViewModel.uploadFile(tempFile)
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch("*/*") },
                containerColor = Color(0xFF8B5CF6),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Files", modifier = Modifier.size(28.dp))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // ================= TOP HEADER =================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Logo Badge + Vaultia Title + Online Subtitle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Vaultia Futuristic Circular Emblem Logo
                        VaultiaBrandLogo(size = 42.dp)

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Vaultia",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.clickable {
                                    vaultViewModel.triggerCloudSync()
                                    android.widget.Toast.makeText(context, "Checking Telegram Cloud for updates...", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSyncing) Icons.Default.Sync else Icons.Default.Send,
                                    contentDescription = null,
                                    tint = if (isSyncing) MaterialTheme.colorScheme.primary else Color(0xFF06B6D4),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = if (isSyncing) (syncState?.statusMessage ?: "Syncing...") else "Telegram Cloud Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSyncing) MaterialTheme.colorScheme.primary else Color(0xFF06B6D4)
                                )
                            }
                        }
                    }

                    // Right: Search + Settings + Anime Profile Avatar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Circular Search Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), CircleShape)
                                .clickable { onNavigateToSearch() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }

                        // Circular Settings Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), CircleShape)
                                .clickable { onNavigateToSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }

                        // Anime Profile Avatar with Online Halo Ring
                        Box(
                            modifier = Modifier.size(42.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = "https://api.dicebear.com/7.x/bottts/png?seed=VaultiaAnime",
                                contentDescription = "User Avatar",
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2C163C))
                                    .border(1.5.dp, Color(0xFF8B5CF6), CircleShape)
                            )
                            // Green Online Dot
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(Color(0xFF10B981), CircleShape)
                                    .border(1.5.dp, Color(0xFF0C0514), CircleShape)
                            )
                        }
                    }
                }
            }

            // ================= TOTAL STORAGE CARD =================
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
                            )
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Vaultia3DIcon(type = VaultiaIconType.VAULT, size = 42.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Total Storage",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = VaultFile.formatBytes(totalStorageUsed),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Used of Unlimited",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            // Sync & Files Pills + Cosmic Cloud Graphic
                            Column(horizontalAlignment = Alignment.End) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Sync Button Pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                            .clickable {
                                                vaultViewModel.triggerCloudSync()
                                                android.widget.Toast.makeText(context, "Syncing files from Telegram Cloud...", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Sync,
                                                contentDescription = "Sync",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (isSyncing) "Syncing..." else "Sync",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Files Pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            .clickable { onNavigateTab("library") }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "$totalFilesCount Files >",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Glowing Cosmic Cloud Graphic
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), Color.Transparent)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dual-color Gradient Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.18f.coerceAtLeast(0.04f))
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF43C9D9), MaterialTheme.colorScheme.primary)
                                            )
                                        )
                                )
                            }
                            Text(
                                text = "Unlimited",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ================= PRIMARY FOLDERS (HORIZONTAL SCROLL) =================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Vaultia3DIcon(type = VaultiaIconType.VAULT, size = 26.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cloud Folders",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    TextButton(onClick = { showCreateFolderDialog = true }) {
                        Icon(
                            Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ New Folder", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Videos Folder
                    item {
                        CategoryFolderCard(
                            title = "Videos",
                            filesCount = videoFiles.size,
                            sizeBytes = videoBytes,
                            iconType = VaultiaIconType.VIDEO,
                            onClick = {
                                vaultViewModel.selectMediaType(MediaType.VIDEOS)
                                onNavigateTab("library")
                            }
                        )
                    }

                    // Audio Folder
                    item {
                        CategoryFolderCard(
                            title = "Audio",
                            filesCount = audioFiles.size,
                            sizeBytes = audioBytes,
                            iconType = VaultiaIconType.AUDIO,
                            onClick = {
                                vaultViewModel.selectMediaType(MediaType.AUDIO)
                                onNavigateTab("library")
                            }
                        )
                    }

                    // Images Folder
                    item {
                        CategoryFolderCard(
                            title = "Images",
                            filesCount = imageFiles.size,
                            sizeBytes = imageBytes,
                            iconType = VaultiaIconType.PHOTO,
                            onClick = {
                                vaultViewModel.selectMediaType(MediaType.IMAGES)
                                onNavigateTab("library")
                            }
                        )
                    }

                    // Documents Folder
                    item {
                        CategoryFolderCard(
                            title = "Docs",
                            filesCount = docFiles.size,
                            sizeBytes = docBytes,
                            iconType = VaultiaIconType.DOCUMENT,
                            onClick = {
                                vaultViewModel.selectMediaType(MediaType.DOCUMENTS)
                                onNavigateTab("library")
                            }
                        )
                    }

                    // Custom User Folders
                    items(folders, key = { it.id }) { customFolder ->
                        val count = allFiles.count { it.folderId == customFolder.id }
                        val folderColor = try { Color(android.graphics.Color.parseColor(customFolder.colorHex)) } catch (_: Exception) { Color(0xFF8B5CF6) }
                        FolderCardView(
                            name = customFolder.name,
                            count = count,
                            color = folderColor,
                            onDelete = { vaultViewModel.deleteFolder(customFolder.id) },
                            onClick = {
                                vaultViewModel.selectMediaType(MediaType.ALL)
                                onNavigateTab("library")
                            }
                        )
                    }
                }
            }

            // ================= UPLOAD FILES & AUTO BACKUP ROW =================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Upload Files Card (Purple gradient)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6D28D9), Color(0xFF4C1D95))
                                )
                            )
                            .clickable { filePickerLauncher.launch("*/*") }
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Upload Files", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, maxLines = 1)
                                    Text("Add to vault", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
                                }
                            }
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                        }
                    }

                    // Auto Backup Card (Cyan / Emerald glass)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF047857), Color(0xFF064E3B))
                                )
                            )
                            .clickable { onNavigateToAutoBackup() }
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Auto Backup", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, maxLines = 1)
                                    Text("Folder Sync", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
                                }
                            }
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            // ================= BOT RECEIVER SERVER CARD =================
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (bridgeState.isRunning) Color(0xFF10B981) else Color.Gray, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (bridgeState.isRunning) "Bot Receiver Active" else "Bot Receiver Server",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (bridgeState.isRunning) {
                                val mins = bridgeState.remainingSeconds / 60
                                val secs = bridgeState.remainingSeconds % 60
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = String.format("%02d:%02d left", mins, secs),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (bridgeState.isRunning)
                                "Receiving files sent to @$botUsername in PM (${bridgeState.receivedFilesCount} files auto-saved)!"
                            else
                                "Receive videos/files shared by friends to your Telegram Bot without downloading.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (bridgeState.isRunning) {
                                ClayButton(
                                    text = "Stop Server",
                                    icon = Icons.Default.Stop,
                                    onClick = { bridgeServer.stopServer() },
                                    modifier = Modifier.weight(1f),
                                    isPrimary = false
                                )
                                ClayButton(
                                    text = "+10 Mins",
                                    icon = Icons.Default.Add,
                                    onClick = { bridgeServer.extendServer(10) },
                                    modifier = Modifier.weight(1f),
                                    isPrimary = true
                                )
                            } else {
                                ClayButton(
                                    text = "Start Server (10 Mins)",
                                    icon = Icons.Default.PowerSettingsNew,
                                    onClick = { bridgeServer.startServer(10) },
                                    modifier = Modifier.fillMaxWidth(),
                                    isPrimary = true
                                )
                            }
                        }
                    }
                }
            }


            // Active Transfers Banner
            val activeTransfers = transfers.filter { it.status.isActive }
            if (activeTransfers.isNotEmpty()) {
                item {
                    ClayCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateTab("transfers") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = VioletDuskPeach)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${activeTransfers.size} Active Transfer(s)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tap to view real-time progress",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "View >",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = VioletDuskPeach
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Create New Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create New Folder", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Folder Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Pick Color:", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("#8B5CF6", "#EC4899", "#06B6D4", "#F97316", "#10B981").forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedFolderColor == hex) 2.5.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedFolderColor = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            vaultViewModel.createFolder(newFolderName.trim(), selectedFolderColor)
                            newFolderName = ""
                            showCreateFolderDialog = false
                        }
                    }
                ) {
                    Text("Create", color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun CategoryFolderCard(
    title: String,
    filesCount: Int,
    sizeBytes: Long,
    iconType: VaultiaIconType,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .width(130.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Vaultia3DIcon(type = iconType, size = 36.dp)
                Icon(
                    Icons.Default.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(10.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text("$filesCount files", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
            Text(VaultFile.formatBytes(sizeBytes), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
fun FolderCardView(
    name: String,
    count: Int,
    color: Color,
    onDelete: (() -> Unit)?,
    onClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(115.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }

                if (onDelete != null) {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Folder", color = StatusError) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusError) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$count items", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
        }
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
    }
    return uri.path?.substringAfterLast('/')
}
