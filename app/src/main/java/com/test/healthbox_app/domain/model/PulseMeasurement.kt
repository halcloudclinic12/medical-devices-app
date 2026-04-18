package com.test.healthbox_app.domain.model

import com.test.healthbox_app.bluetooth.DeviceType

data class PulseMeasurement(
    override val deviceType: DeviceType = DeviceType.PULSE,
    override val timestamp: Long = System.currentTimeMillis(),
    val pulseRate: Int,
    val oxygenSaturation: Int,
    val pi: Double,
    override val isValid: Boolean
) : Measurement()

