package com.vaultia.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultia.app.core.theme.ClayPrimary
import com.vaultia.app.domain.model.VaultFile
import com.vaultia.app.ui.components.VaultiaTopAppBar
import com.vaultia.app.ui.components.FileCardItem
import com.vaultia.app.ui.viewmodel.VaultViewModel

@Composable
fun SearchScreen(
    vaultViewModel: VaultViewModel,
    onNavigateToMedia: (file: VaultFile) -> Unit,
    onBackClick: () -> Unit
) {
    val searchQuery by vaultViewModel.searchQuery.collectAsState()
    val allFiles by vaultViewModel.allFiles.collectAsState()

    val searchResults = if (searchQuery.isBlank()) {
        emptyList()
    } else {
        allFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            VaultiaTopAppBar(
                title = "Search Vault",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vaultViewModel.updateSearchQuery(it) },
                placeholder = { Text("Search by filename or extension...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ClayPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { vaultViewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ClayPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(searchResults) { file ->
                    FileCardItem(
                        file = file,
                        onClick = { onNavigateToMedia(file) },
                        onFavorite = { vaultViewModel.toggleFavorite(file) },
                        onDownload = { vaultViewModel.downloadFile(file) },
                        onDelete = { vaultViewModel.deleteFile(file.id) }
                    )
                }
            }
        }
    }
}
