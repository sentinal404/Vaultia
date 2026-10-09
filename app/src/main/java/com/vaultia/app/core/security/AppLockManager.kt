package com.vaultia.app.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLockManager(private val secureStorageManager: SecureStorageManager) {

    private val _isLocked = MutableStateFlow(secureStorageManager.isAppLockEnabled)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    fun unlockWithPin(pin: String): Boolean {
        val success = secureStorageManager.verifyPin(pin)
        if (success) {
            _isLocked.value = false
        }
        return success
    }

    fun unlockBiometrically() {
        if (secureStorageManager.isBiometricEnabled) {
            _isLocked.value = false
        }
    }

    fun lock() {
        if (secureStorageManager.isAppLockEnabled) {
            _isLocked.value = true
        }
    }
}
