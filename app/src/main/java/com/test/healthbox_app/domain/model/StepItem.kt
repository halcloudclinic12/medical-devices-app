package com.test.healthbox_app.domain.model

data class StepItem(
    val id: Int,
    val title: String,
    val icon: Int,
    val isActive: Boolean = false,
    val status: StepStatus = StepStatus.PENDING
)
