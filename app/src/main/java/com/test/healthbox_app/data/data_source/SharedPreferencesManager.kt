package com.test.healthbox_app.data.data_source

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.data.model.response.ClinicLoginResponse
import com.test.healthbox_app.data.model.response.Patient
import com.test.healthbox_app.domain.model.BleDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SharedPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val PREF_NAME = "ble_devices_prefs"
    private val TOKEN = "key_token"
    private val REFRESH_TOKEN = "key_refresh_token"
    private val VERIFIED_AT = "verified_at"
    private val PATIENT = "key_patient"
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveDevice(device: BleDevice): Boolean {
        val deviceJson = gson.toJson(device)
        return sharedPreferences.edit().putString(device.deviceType?.name, deviceJson).commit()
    }

    fun getDevice(type: DeviceType): BleDevice? {
        val deviceJson = sharedPreferences.getString(type.name, null) ?: return null
        return try {
            gson.fromJson(deviceJson, BleDevice::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getAllDevices(): List<BleDevice> {
        val devices = mutableListOf<BleDevice>()
        DeviceType.values().forEach { type ->
            getDevice(type)?.let { devices.add(it) }
        }
        return devices
    }

    fun removeDevice(type: DeviceType): Boolean {
        return sharedPreferences.edit().remove(type.name).commit()
    }

    fun clearAll(): Boolean {
        return sharedPreferences.edit().clear().commit()
    }

    fun saveClinic(clinicLoginResponse: ClinicLoginResponse): Boolean {
        val deviceJson = gson.toJson(clinicLoginResponse)

        saveTokens(token = clinicLoginResponse.data?.token.orEmpty(), refreshToken = clinicLoginResponse.data?.refreshToken.orEmpty())

        return sharedPreferences.edit().putString("clinic_info", deviceJson).commit()
    }

    fun getClinic(): ClinicLoginResponse? {
        val clinicJson = sharedPreferences.getString("clinic_info", null) ?: return null

        return try {
            gson.fromJson(clinicJson, ClinicLoginResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun saveTokens(token: String, refreshToken: String) {

        /*val token = token
        val refreshToken = refreshToken

        sharedPreferences.edit().apply {
            putString(TOKEN, token)
            putString(REFRESH_TOKEN, refreshToken)
            apply() // ✅ Only call once
        }*/
//        sharedPreferences.edit().putString(TOKEN, clinicLoginResponse.data?.token).apply()
        sharedPreferences.edit().putString(TOKEN, token).apply()

//        sharedPreferences.edit().putString(REFRESH_TOKEN, clinicLoginResponse.data?.refreshToken).apply()
        sharedPreferences.edit().putString(REFRESH_TOKEN, refreshToken).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString(TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString(REFRESH_TOKEN, null)
    }

    // ✅ Save current time after verifyToken()
    fun saveVerificationTime() {
        val currentTimeMillis = System.currentTimeMillis()
        sharedPreferences.edit().putLong(VERIFIED_AT, currentTimeMillis).apply()
    }

    // ✅ Get last verification time
    fun getVerificationTime(): Long {
        return sharedPreferences.getLong(VERIFIED_AT, 0L)
    }

    // ✅ Check if token expired (> 1 hour)
    fun isVerificationExpired(): Boolean {
        val lastVerified = getVerificationTime()
        if (lastVerified == 0L) return true // never verified
        val halfHourInMillis = 30 * 60 * 1000
//        val oneHourInMillis = 60 * 60 * 1000
        return System.currentTimeMillis() - lastVerified > halfHourInMillis
    }


    fun savePatient(patient: Patient): Boolean {
        val patientJson = gson.toJson(patient)

        return sharedPreferences.edit().putString(PATIENT, patientJson).commit()
    }

    fun getPatient(): Patient? {
        val patientJson = sharedPreferences.getString(PATIENT, null) ?: return null

        return try {
            gson.fromJson(patientJson, Patient::class.java)
        } catch (e: Exception) {
            null
        }
    }

    //  Save calibration value (can be positive or negative)
    /*fun saveCalibration(calibrationType: CalibrationType, value: Float): Boolean {
        val key = "${calibrationType.name}_CALIBRATION"
        return sharedPreferences.edit().putFloat(key, value).commit()
    }*/

    // Get calibration value (returns null if not set)
    /*fun getCalibration(calibrationType: CalibrationType): Float? {
        val key = "${calibrationType.name}_CALIBRATION"
        return if (sharedPreferences.contains(key)) sharedPreferences.getFloat(key, 0f) else null
    }*/

    // ✅ Save calibration (stores combined value like "+0.75" or "-1.25")
    fun saveCalibration(calibrationType: CalibrationType, operator: CalibrationOperators, value: Float): Boolean {
        val key = "${calibrationType.name}_CALIBRATION"
        val formattedValue = "${operator.symbol}$value"
        return sharedPreferences.edit().putString(key, formattedValue).commit()
    }

    // ✅ Get calibration (returns Pair<operator, value>)
    fun getCalibration(calibrationType: CalibrationType): Pair<CalibrationOperators, Float>? {
        val key = "${calibrationType.name}_CALIBRATION"
        val saved = sharedPreferences.getString(key, null) ?: return null

        // Extract operator and numeric value
        val operatorSymbol = saved.first().toString()
        val valuePart = saved.drop(1).toFloatOrNull() ?: return null

        val operator = CalibrationOperators.fromSymbol(operatorSymbol) ?: return null
        return Pair(operator, valuePart)
    }


    fun clearAllExceptDevices(): Boolean {
        val allKeys = sharedPreferences.all.keys
        val deviceKeys = DeviceType.values().map { it.name }

        val editor = sharedPreferences.edit()

        allKeys.forEach { key ->
            if (key !in deviceKeys) {
                editor.remove(key)
            }
        }

        return editor.commit()
    }

}