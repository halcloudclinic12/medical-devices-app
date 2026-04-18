package com.test.healthbox_app.domain.model

sealed class BleResult<out T> {
    data class Success<T>(val data: T) : BleResult<T>()
    data class Error(val exception: Throwable) : BleResult<Nothing>()
    object Loading : BleResult<Nothing>()
}