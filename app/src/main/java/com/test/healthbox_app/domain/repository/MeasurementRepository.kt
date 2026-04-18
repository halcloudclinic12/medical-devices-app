package com.test.healthbox_app.domain.repository

import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.domain.model.Measurement

interface MeasurementRepository {
    fun parseMeasurement(deviceType: DeviceType, rawData: ByteArray): Measurement?
}