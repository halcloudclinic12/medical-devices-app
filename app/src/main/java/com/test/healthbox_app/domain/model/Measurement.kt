package com.test.healthbox_app.domain.model

import com.test.healthbox_app.bluetooth.DeviceType

sealed class Measurement {
    abstract val deviceType: DeviceType
    abstract val timestamp: Long
    abstract val isValid: Boolean
}