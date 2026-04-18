package com.test.healthbox_app.domain.model

import com.test.healthbox_app.bluetooth.DeviceType

data class BloodPressureMeasurement(
    override val deviceType: DeviceType = DeviceType.PULSE,
    override val timestamp: Long = System.currentTimeMillis(),
    val pulseRate: Int,
    val diastolic: Int,
    val systolic: Int,
    val isTesting: Boolean,
    override val isValid: Boolean
) : Measurement()

