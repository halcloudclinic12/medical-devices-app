package com.test.healthbox_app.domain.use_cases

import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.data.data_source.CalibrationOperators
import com.test.healthbox_app.data.data_source.CalibrationType
import com.test.healthbox_app.data.model.response.ClinicLoginResponse
import com.test.healthbox_app.data.model.response.Patient
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.repository.SharePreferenceRepository
import javax.inject.Inject

class SharedPreferenceUseCases @Inject constructor(
    private val sharePreferenceRepository: SharePreferenceRepository
) {


    fun saveClinic(clinicLoginResponse: ClinicLoginResponse): Boolean {
        return sharePreferenceRepository.saveClinic(clinicLoginResponse)
    }

    fun getClinic(): ClinicLoginResponse? {
        return sharePreferenceRepository.getClinic()
    }

    fun savePatient(patient: Patient): Boolean {
        return sharePreferenceRepository.savePatient(patient = patient)
    }

    fun getPatient(): Patient? {
        return sharePreferenceRepository.getPatient()
    }


    fun saveTokens(token: String, refreshToken: String) {
        return sharePreferenceRepository.saveTokens(token = token, refreshToken = refreshToken)
    }


    fun getToken(): String? {
        return sharePreferenceRepository.getToken()
    }

    fun getRefreshToken(): String? {
        return sharePreferenceRepository.getRefreshToken()
    }

    fun saveVerificationTime() {
        return sharePreferenceRepository.saveVerificationTime()
    }

    fun isVerificationExpired(): Boolean? {
        return sharePreferenceRepository.isVerificationExpired()
    }

    fun saveDevice(device: BleDevice): Boolean {
        return sharePreferenceRepository.saveDevice(device)
    }

    fun getDevice(deviceType: DeviceType): BleDevice? {
        return sharePreferenceRepository.getDevice(deviceType)
    }

    fun getAllDevices(): List<BleDevice> {
        return sharePreferenceRepository.getAllDevices()
    }

    fun removeDevice(deviceType: DeviceType): Boolean {
        return sharePreferenceRepository.removeDevice(deviceType)
    }

    fun clearAll(): Boolean {
        return sharePreferenceRepository.clearAll()
    }

    fun clearAllExceptDevices(): Boolean {
        return sharePreferenceRepository.clearAllExceptDevices()
    }

    fun saveCalibration(calibrationType: CalibrationType, operator: CalibrationOperators, value: Float): Boolean {
        return sharePreferenceRepository.saveCalibration(calibrationType, operator, value)
    }

    fun getCalibration(calibrationType: CalibrationType): Pair<CalibrationOperators, Float>? {
        return sharePreferenceRepository.getCalibration(calibrationType)
    }
}
