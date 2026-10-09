package com.vaultia.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultia.app.core.VaultResult
import com.vaultia.app.data.repository.SettingsRepository
import com.vaultia.app.data.repository.ThemeMode
import com.vaultia.app.data.repository.UserSettings
import com.vaultia.app.data.repository.VaultRepository
import com.vaultia.app.domain.model.WorkerBot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: VaultRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings

    val workers: StateFlow<List<WorkerBot>> = repository.allWorkers
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _addWorkerResult = MutableStateFlow<VaultResult<WorkerBot>?>(null)
    val addWorkerResult: StateFlow<VaultResult<WorkerBot>?> = _addWorkerResult.asStateFlow()

    fun updateMaxWorkers(count: Int) {
        settingsRepository.updateMaxWorkers(count)
    }

    fun updateChunkSizeMb(sizeMb: Int) {
        settingsRepository.updateChunkSizeMb(sizeMb)
    }

    fun setWifiOnlyUploads(enabled: Boolean) {
        settingsRepository.setWifiOnlyUploads(enabled)
    }

    fun setWifiOnlyDownloads(enabled: Boolean) {
        settingsRepository.setWifiOnlyDownloads(enabled)
    }

    fun setAutoResume(enabled: Boolean) {
        settingsRepository.setAutoResume(enabled)
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }

    fun setAppLock(enabled: Boolean) {
        settingsRepository.setAppLock(enabled)
        repository.secureStorage.isAppLockEnabled = enabled
    }

    fun setBiometric(enabled: Boolean) {
        settingsRepository.setBiometric(enabled)
        repository.secureStorage.isBiometricEnabled = enabled
    }

    fun setPin(pin: String) {
        repository.secureStorage.setAppLockPin(pin)
        settingsRepository.setAppLock(true)
    }

    fun addWorker(token: String) {
        viewModelScope.launch {
            _addWorkerResult.value = VaultResult.Loading
            val result = repository.addWorkerBot(token)
            _addWorkerResult.value = result
        }
    }

    fun removeWorker(workerId: String) {
        viewModelScope.launch {
            repository.removeWorker(workerId)
        }
    }

    fun clearAddWorkerResult() {
        _addWorkerResult.value = null
    }

    fun deleteConfiguration(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.wipeAllConfiguration()
            onComplete()
        }
    }
}
