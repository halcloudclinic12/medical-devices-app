package com.test.healthbox_app.domain.model

data class BleUiState(
    val isSending: Boolean = false,
    val lastSentValue: String? = null,
    val lastReceivedValue: String? = null
)
