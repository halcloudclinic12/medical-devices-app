package com.test.healthbox_app.domain.model

import com.test.healthbox_app.bluetooth.DeviceType

data class WeightMeasurement(
    val label: String? = null,
    val value: String? = null,
    val unit: String? = null,
    val result: String? = null,
    override val deviceType: DeviceType = DeviceType.WEIGHING_SCALE,
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean = false
) : Measurement()
