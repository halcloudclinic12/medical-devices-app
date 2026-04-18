package com.test.healthbox_app.data.repository

import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.data.data_source.CalibrationOperators
import com.test.healthbox_app.data.data_source.CalibrationType
import com.test.healthbox_app.data.data_source.SharedPreferencesManager
import com.test.healthbox_app.data.model.response.ClinicLoginResponse
import com.test.healthbox_app.data.model.response.Patient
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.repository.SharePreferenceRepository
import javax.inject.Inject

class SharedPreferenceRepositoryImpl @Inject constructor(
    private val sharedPreferencesManager: SharedPreferencesManager

) : SharePreferenceRepository {
    override fun saveClinic(clinicResponse: ClinicLoginResponse): Boolean {
        return sharedPreferencesManager.saveClinic(clinicResponse)
    }

    override fun getClinic(): ClinicLoginResponse? {
        return sharedPreferencesManager.getClinic()
    }

    override fun savePatient(patient: Patient): Boolean {
        return sharedPreferencesManager.savePatient(patient)
    }

    override fun getPatient(): Patient? {
        return sharedPreferencesManager.getPatient()
    }

    override fun saveDevice(device: BleDevice): Boolean {
        return sharedPreferencesManager.saveDevice(device)
    }

    override fun getDevice(type: DeviceType): BleDevice? {
        return sharedPreferencesManager.getDevice(type)
    }

    override fun getAllDevices(): List<BleDevice> {
        return sharedPreferencesManager.getAllDevices()
    }

    override fun saveTokens(token: String, refreshToken: String) {
        return sharedPreferencesManager.saveTokens(token = token, refreshToken = refreshToken)
    }

    override fun getToken(): String? {
        return sharedPreferencesManager.getToken()
    }

    override fun getRefreshToken(): String? {
        return sharedPreferencesManager.getRefreshToken()
    }

    override fun saveVerificationTime() {
        return sharedPreferencesManager.saveVerificationTime()
    }

    override fun isVerificationExpired(): Boolean? {
        return sharedPreferencesManager.isVerificationExpired()
    }

    override fun removeDevice(type: DeviceType): Boolean {
        return sharedPreferencesManager.removeDevice(type)
    }

    override fun clearAll(): Boolean {
        return sharedPreferencesManager.clearAll()
    }

    override fun clearAllExceptDevices(): Boolean {
        return sharedPreferencesManager.clearAllExceptDevices()
    }

    override fun saveCalibration(calibrationType: CalibrationType, operator: CalibrationOperators, value: Float): Boolean {
        return sharedPreferencesManager.saveCalibration(calibrationType, operator, value)
    }

    override fun getCalibration(calibrationType: CalibrationType): Pair<CalibrationOperators, Float>? {
        return sharedPreferencesManager.getCalibration(calibrationType)
    }
}