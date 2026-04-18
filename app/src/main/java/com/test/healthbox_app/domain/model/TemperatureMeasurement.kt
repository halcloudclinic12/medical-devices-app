package com.test.healthbox_app.domain.model

import com.test.healthbox_app.bluetooth.DeviceType

data class TemperatureMeasurement(
    override val deviceType: DeviceType = DeviceType.THERMOMETER,
    override val timestamp: Long = System.currentTimeMillis(),
    val temperatureCelsius: Double,
    val temperatureFahrenheit: Double,
    override val isValid: Boolean
) : Measurement()
