package com.vaultia.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultia.app.core.theme.ClayPrimary
import com.vaultia.app.domain.model.MediaType
import com.vaultia.app.domain.model.VaultFile
import com.vaultia.app.ui.components.ClayCard
import com.vaultia.app.ui.components.VaultiaBottomNav
import com.vaultia.app.ui.components.VaultiaTopAppBar
import com.vaultia.app.ui.dashboard.FileCardItem
import com.vaultia.app.ui.viewmodel.VaultViewModel

@Composable
fun LibraryScreen(
    vaultViewModel: VaultViewModel,
    onNavigateToMedia: (file: VaultFile) -> Unit,
    onNavigateTab: (String) -> Unit
) {
    val allFiles by vaultViewModel.allFiles.collectAsState()
    val selectedMediaType by vaultViewModel.selectedMediaType.collectAsState()

    val filteredFiles = when (selectedMediaType) {
        MediaType.ALL -> allFiles
        else -> allFiles.filter { it.mediaType == selectedMediaType }
    }

    Scaffold(
        topBar = {
            VaultiaTopAppBar(
                title = "Library",
                subtitle = "${filteredFiles.size} items"
            )
        },
        bottomBar = {
            VaultiaBottomNav(
                currentRoute = "library",
                onNavigate = onNavigateTab
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
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
                        label = { Text(type.displayName, fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ClayPrimary,
                            selectedLabelColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = ClayPrimary.copy(alpha = 0.4f),
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No files in ${selectedMediaType.displayName}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredFiles) { file ->
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
