package com.test.healthbox_app.domain.model

import com.test.healthbox_app.bluetooth.DeviceType

data class HeightMeasurement(
    override val deviceType: DeviceType = DeviceType.HEIGHT,
    override val timestamp: Long = System.currentTimeMillis(),
    val heightCm: Int,
    val error: String,
    override val isValid: Boolean
) : Measurement()