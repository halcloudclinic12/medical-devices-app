package com.test.healthbox_app.domain.repository

import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.data.data_source.CalibrationOperators
import com.test.healthbox_app.data.data_source.CalibrationType
import com.test.healthbox_app.data.model.response.ClinicLoginResponse
import com.test.healthbox_app.data.model.response.Patient
import com.test.healthbox_app.domain.model.BleDevice

interface SharePreferenceRepository {
    fun saveClinic(clinicResponse: ClinicLoginResponse): Boolean
    fun savePatient(patient: Patient): Boolean
    fun getPatient(): Patient?
    fun getClinic(): ClinicLoginResponse?
    fun saveDevice(device: BleDevice): Boolean
    fun getDevice(type: DeviceType): BleDevice?
    fun getAllDevices(): List<BleDevice>
    fun saveTokens(token: String, refreshToken: String)
    fun getToken(): String?
    fun getRefreshToken(): String?
    fun isVerificationExpired(): Boolean?
    fun saveVerificationTime()
    fun removeDevice(type: DeviceType): Boolean
    fun clearAll(): Boolean
    fun saveCalibration(calibrationType: CalibrationType, operator: CalibrationOperators, value: Float): Boolean
    fun getCalibration(calibrationType: CalibrationType): Pair<CalibrationOperators, Float>?
    fun clearAllExceptDevices(): Boolean
}