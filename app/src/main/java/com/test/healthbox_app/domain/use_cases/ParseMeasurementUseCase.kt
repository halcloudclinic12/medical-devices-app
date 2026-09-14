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

    /**
     * Clears any partially received HbA1c frame. Called when the HbA1c screen starts or
     * stops listening, so a truncated record from one session can never be spliced onto
     * the next.
     */
    fun resetHba1cBuffer() {
        measurementRepository.resetHba1cBuffer()
    }
}