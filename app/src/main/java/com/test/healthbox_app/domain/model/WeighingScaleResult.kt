package com.test.healthbox_app.domain.model

sealed class WeighingScaleResult<out T> {
    object Loading : WeighingScaleResult<Nothing>()
    data class Success<T>(val data: T) : WeighingScaleResult<T>()
    data class Error(val exception: Throwable) : WeighingScaleResult<Nothing>()
}