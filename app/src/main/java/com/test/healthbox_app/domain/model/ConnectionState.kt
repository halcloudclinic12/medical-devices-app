package com.test.healthbox_app.domain.model

sealed class ConnectionState {
    object Connected : ConnectionState()
    object Connecting : ConnectionState()
    object Paired : ConnectionState()
    object PairedFailed : ConnectionState()
    data class Disconnected(val reason: String? = null) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

