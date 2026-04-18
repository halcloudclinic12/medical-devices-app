package com.test.healthbox_app.domain.model

sealed class PrintState {
    object Idle : PrintState()
    object Loading : PrintState()
    data class Success(val message: String) : PrintState()
    data class Error(val message: String) : PrintState()
}