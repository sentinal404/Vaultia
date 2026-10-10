package com.vaultia.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultia.app.core.theme.ClayAccentPink
import com.vaultia.app.core.theme.ClayPrimary
import com.vaultia.app.core.theme.VioletDuskBg
import com.vaultia.app.core.theme.VioletDuskCream
import com.vaultia.app.core.theme.VioletDuskPeach
import com.vaultia.app.core.theme.VioletDuskPlum
import com.vaultia.app.core.theme.VioletDuskRose
import com.vaultia.app.domain.model.MediaType
import com.vaultia.app.domain.model.VaultFile
import com.vaultia.app.ui.components.ClayCard
import com.vaultia.app.ui.components.VaultiaBottomNav
import com.vaultia.app.ui.components.VaultiaTopAppBar
import com.vaultia.app.ui.components.FileCardItem
import com.vaultia.app.ui.components.VaultiaFileThumbnail
import com.vaultia.app.ui.viewmodel.VaultViewModel

@Composable
fun LibraryScreen(
    vaultViewModel: VaultViewModel,
    onNavigateToMedia: (file: VaultFile) -> Unit,
    onNavigateTab: (String) -> Unit
) {
    val allFiles by vaultViewModel.allFiles.collectAsState()
    val selectedMediaType by vaultViewModel.selectedMediaType.collectAsState()

    var isGridView by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredFiles = allFiles
        .filter { selectedMediaType == MediaType.ALL || it.mediaType == selectedMediaType }
        .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            VaultiaTopAppBar(
                title = "Drive Library",
                subtitle = "${filteredFiles.size} items in cloud",
                actions = {
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View",
                            tint = VioletDuskPeach
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // Search Input Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search files by name or type...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaType.values().forEach { type ->
                    val isSelected = selectedMediaType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { vaultViewModel.selectMediaType(type) },
                        label = { Text(type.displayName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ClayCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            com.vaultia.app.ui.components.Vaultia3DIcon(
                                type = com.vaultia.app.ui.components.VaultiaIconType.DOCUMENT,
                                size = 56.dp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No files found",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Upload files or receive them via Telegram Bot to populate.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else if (isGridView) {
                // Google Drive-style 2-Column Grid View
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredFiles, key = { it.id }) { file ->
                        DriveGridFileCard(
                            file = file,
                            onClick = { onNavigateToMedia(file) },
                            onFavorite = { vaultViewModel.toggleFavorite(file) },
                            onDownload = { vaultViewModel.downloadFile(file) },
                            onDelete = { vaultViewModel.deleteFile(file.id) }
                        )
                    }
                }
            } else {
                // Google Drive-style List View
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredFiles, key = { it.id }) { file ->
                        FileCardItem(
                            file = file,
                            onClick = { onNavigateToMedia(file) },
                            onFavorite = { vaultViewModel.toggleFavorite(file) },
                            onDownload = { vaultViewModel.downloadFile(file) },
                            onDelete = { vaultViewModel.deleteFile(file.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DriveGridFileCard(
    file: VaultFile,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ClayCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Futuristic Vaultia File Thumbnail Preview
            Box(modifier = Modifier.fillMaxWidth()) {
                VaultiaFileThumbnail(
                    file = file,
                    modifier = Modifier.fillMaxWidth(),
                    height = 100.dp
                )

                // Top right favorite icon
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(30.dp)
                ) {
                    Icon(
                        imageVector = if (file.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (file.isFavorite) Color(0xFFFF5D8F) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // File Name
            Text(
                text = file.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom info: Size + Options menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = file.formattedSize,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Download to Device") },
                            onClick = {
                                menuExpanded = false
                                onDownload()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
