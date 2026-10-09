package com.vaultia.app.core

sealed class VaultResult<out T> {
    data class Success<out T>(val data: T) : VaultResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null, val code: Int? = null) : VaultResult<Nothing>()
    object Loading : VaultResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
}
