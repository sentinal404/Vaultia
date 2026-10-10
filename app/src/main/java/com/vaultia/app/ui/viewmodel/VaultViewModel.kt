package com.vaultia.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultia.app.core.VaultResult
import com.vaultia.app.data.repository.VaultRepository
import com.vaultia.app.domain.model.MediaType
import com.vaultia.app.domain.model.TelegramConfig
import com.vaultia.app.domain.model.VaultFile
import com.vaultia.app.domain.model.WorkerBot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class VaultViewModel(
    private val repository: VaultRepository,
    val cloudSyncManager: com.vaultia.app.engine.sync.CloudSyncManager? = null
) : ViewModel() {

    val syncState = cloudSyncManager?.syncState

    fun triggerCloudSync(forceDeepScan: Boolean = false) {
        cloudSyncManager?.performFullSync(forceDeepScan)
    }

    val allFiles: StateFlow<List<VaultFile>> = repository.allFiles
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentFiles: StateFlow<List<VaultFile>> = repository.recentFiles
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalStorageUsed: StateFlow<Long> = repository.totalStorageUsed
        .stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val totalFilesCount: StateFlow<Int> = repository.totalFilesCount
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val workers: StateFlow<List<WorkerBot>> = repository.allWorkers
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allFolders: StateFlow<List<com.vaultia.app.data.local.entity.FolderEntity>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun createFolder(name: String, colorHex: String = "#8B5CF6") {
        viewModelScope.launch {
            repository.createFolder(name, colorHex)
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
        }
    }

    fun moveFileToFolder(fileId: String, folderId: String?) {
        viewModelScope.launch {
            repository.moveFileToFolder(fileId, folderId)
        }
    }

    private val _selectedMediaType = MutableStateFlow(MediaType.ALL)
    val selectedMediaType: StateFlow<MediaType> = _selectedMediaType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _connectionStatus = MutableStateFlow<VaultResult<TelegramConfig>?>(null)
    val connectionStatus: StateFlow<VaultResult<TelegramConfig>?> = _connectionStatus.asStateFlow()

    fun selectMediaType(mediaType: MediaType) {
        _selectedMediaType.value = mediaType
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun uploadFile(file: File): String {
        return repository.uploadFile(file)
    }

    fun downloadFile(file: VaultFile): String {
        return repository.downloadFile(file)
    }

    fun toggleFavorite(file: VaultFile) {
        viewModelScope.launch {
            repository.toggleFavorite(file.id, file.isFavorite)
        }
    }

    fun deleteFile(fileId: String) {
        viewModelScope.launch {
            repository.deleteFile(fileId)
        }
    }

    fun validateAndConnect(token: String, channelId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _connectionStatus.value = VaultResult.Loading
            val result = repository.validateTelegramConnection(token, channelId)
            _connectionStatus.value = result
            onComplete(result is VaultResult.Success)
        }
    }

    fun getStreamUrl(fileId: String): String {
        return repository.getStreamUrl(fileId)
    }
}
