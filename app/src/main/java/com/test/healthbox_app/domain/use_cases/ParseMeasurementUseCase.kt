package com.test.healthbox_app.domain.use_cases

import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.domain.model.Measurement
import com.test.healthbox_app.domain.repository.MeasurementRepository
import javax.inject.Inject

class ParseMeasurementUseCase @Inject constructor(
    private val measurementRepository: MeasurementRepository
) {
    operator fun invoke(deviceType: DeviceType, rawData: ByteArray): Measurement? {
        return measurementRepository.parseMeasurement(deviceType, rawData)
    }
}