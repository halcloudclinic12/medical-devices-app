package com.test.healthbox_app.domain.repository

import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.domain.model.Measurement

interface MeasurementRepository {
    fun parseMeasurement(deviceType: DeviceType, rawData: ByteArray): Measurement?

    /**
     * Drops any partially received HbA1c frame. Call on connect and disconnect so a
     * truncated record can never be spliced onto the next test's data.
     */
    fun resetHba1cBuffer()
}