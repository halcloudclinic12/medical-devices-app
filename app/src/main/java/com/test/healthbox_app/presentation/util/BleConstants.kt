package com.test.healthbox_app.presentation.util

/**
 * Constants for BLE communication
 */
object BleConstants {

    // Common BLE UUIDs
    const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"

    // Height Service
    const val HEIGHT_SERVICE = "0000abf0-0000-1000-8000-00805f9b34fb"
    const val HEIGHT_MEASUREMENT = "0000abf1-0000-1000-8000-00805f9b34fb"
    const val HEIGHT_CONTROL_POINT = "00002a39-0000-1000-8000-00805f9b34fb"

    // Thermometer Service
    const val THERMOMETER_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb"
    const val THERMOMETER_MEASUREMENT = "0000fff3-0000-1000-8000-00805f9b34fb"
//    const val HEIGHT_CONTROL_POINT = "00002a39-0000-1000-8000-00805f9b34fb"

    // Thermometer Service
    const val PULSE_SERVICE = "cdeacb80-5235-4c07-8846-93a37ee6b86d"
    const val PULSE_MEASUREMENT = "cdeacb81-5235-4c07-8846-93a37ee6b86d"
//    const val HEIGHT_CONTROL_POINT = "00002a39-0000-1000-8000-00805f9b34fb"

    // Heart Rate Service
    const val HEART_RATE_SERVICE = "0000180d-0000-1000-8000-00805f9b34fb"
    const val HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb"
    const val HEART_RATE_CONTROL_POINT = "00002a39-0000-1000-8000-00805f9b34fb"

    // Blood Pressure Service
    const val BLOOD_PRESSURE_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb"
    const val BLOOD_PRESSURE_MEASUREMENT = "0000fff1-0000-1000-8000-00805f9b34fb"
    const val BLOOD_PRESSURE_MEASUREMENT_WRITE = "0000fff2-0000-1000-8000-00805f9b34fb"

    // Glucose Service
    const val GLUCOSE_QPP_SERVICE    = "0000fff0-0000-1000-8000-00805f9b34fb"
    const val GLUCOSE_QPP_CHAR_NOTIFY = "0000fff1-0000-1000-8000-00805f9b34fb"  // RX  props=16
    const val GLUCOSE_QPP_CHAR_WRITE  = "0000fff2-0000-1000-8000-00805f9b34fb"  // TX  props=4
//    const val GLUCOSE_SERVICE_UUID = "00001808-0000-1000-8000-00805f9b34fb"
//    const val GLUCOSE_MEASUREMENT_CHARACTERISTIC_UUID = "00002A18-0000-1000-8000-00805f9b34fb"
//    const val GLUCOSE_RECORD_ACCESS_POINT_CHARACTERISTIC_UUID = "00002A52-0000-1000-8000-00805f9b34fb"
//    const val GLUCOSE_MEASUREMENT_CONTEXT_CHARACTERISTIC_UUID = "00002A34-0000-1000-8000-00805f9b34fb"

    // Battery Service
    const val BATTERY_SERVICE = "0000180f-0000-1000-8000-00805f9b34fb"
    const val BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb"

    // Device Information Service
    const val DEVICE_INFO_SERVICE = "0000180a-0000-1000-8000-00805f9b34fb"
    const val MANUFACTURER_NAME = "00002a29-0000-1000-8000-00805f9b34fb"
    const val MODEL_NUMBER = "00002a24-0000-1000-8000-00805f9b34fb"
    const val SERIAL_NUMBER = "00002a25-0000-1000-8000-00805f9b34fb"
    const val FIRMWARE_REVISION = "00002a26-0000-1000-8000-00805f9b34fb"

    // Generic Access Service
    const val GENERIC_ACCESS_SERVICE = "00001800-0000-1000-8000-00805f9b34fb"
    const val DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb"

    // Connection parameters
    const val CONNECTION_TIMEOUT_MS = 10000L // 10 seconds
    const val SCAN_PERIOD_MS = 10000L // 10 seconds

    // Operation timeouts
    const val READ_TIMEOUT_MS = 5000L // 5 seconds
    const val WRITE_TIMEOUT_MS = 5000L // 5 seconds

    // Maximum MTU size to request (if supported by device)
    const val MAX_MTU_SIZE = 512

}