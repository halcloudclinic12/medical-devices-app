package com.test.healthbox_app.domain.model

import com.test.healthbox_app.bluetooth.DeviceType

data class GlucoseMeasurement(
    override val deviceType: DeviceType = DeviceType.GLUCOSE_METER,
    override val timestamp: Long = System.currentTimeMillis(),
    val sugar: Int,
    override val isValid: Boolean
) : Measurement()