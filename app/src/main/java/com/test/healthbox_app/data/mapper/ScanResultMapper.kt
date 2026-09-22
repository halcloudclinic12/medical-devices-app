package com.test.healthbox_app.data.mapper

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.domain.model.BleDevice

@SuppressLint("MissingPermission")
fun ScanResult.toBluetoothDevice(): BleDevice {
    return BleDevice(
        address = device.address,
        name = device.name ?: "Unknown Device",
        rssi = rssi,
        deviceType = resolveDeviceType(this),
        bondState = device.bondState,
        scanRecord = scanRecord?.bytes ?: byteArrayOf()
    )
}

/**
 * Resolve device type from scan result
 */
@SuppressLint("MissingPermission")
private fun resolveDeviceType(scanResult: ScanResult): DeviceType? {
    // In a real app, would use more sophisticated logic based on:
    // 1. Advertised services
    // 2. Manufacturer data
    // 3. Device name patterns

    val scanRecord = scanResult.scanRecord
    val deviceName = scanResult.device.name ?: ""

    return when {
        // Check device name for known patterns
        deviceName.contains("COC-Hybrid", ignoreCase = true) || deviceName.contains("COC-Hybrid", ignoreCase = true) -> DeviceType.HEIGHT

        deviceName.contains("My Thermometer", ignoreCase = true) || deviceName.contains("My Thermometer", ignoreCase = true) -> DeviceType.THERMOMETER

        deviceName.contains("My Oximeter", ignoreCase = true) || deviceName.contains("My Oximeter", ignoreCase = true) -> DeviceType.PULSE

        deviceName.contains("QN-Scale", ignoreCase = true) || deviceName.contains("QN-Scale", ignoreCase = true) -> DeviceType.WEIGHING_SCALE

        deviceName.contains("JPD BPM", ignoreCase = true) || deviceName.contains("Blood", ignoreCase = true) -> DeviceType.BLOOD_PRESSURE_MONITOR

        // HbA1c meter (A1cEZ 2.0). MUST stay above the HbCheck branch — the meter
        // identifies itself as "HbA1c-…", so any loose "Hb" match would swallow it.
        // Confirmed against a real meter (adv name "HbA1c-EJB24510443") on 2026-09-13.
        deviceName.contains("A1cEZ", ignoreCase = true) ||
                deviceName.contains("HbA1c", ignoreCase = true) -> DeviceType.HBA1C_METER

        // Confirmed against a real meter (adv name "HbChek", address 00:A0:50:97:25:89) on
        // 2026-09-21 via DEBUG-DEVICESAVE logs - the real device's advertised name is missing
        // a 'c' compared to "HbCheck", so it never matched here and deviceType stayed null,
        // which meant saveDevice() persisted it under a null key instead of "HB_CHECK" and it
        // could never be found again by type. Kept both spellings in case some units do
        // advertise the fully-spelled "HbCheck".
        deviceName.contains("HbChek", ignoreCase = true) || deviceName.contains("HbCheck", ignoreCase = true) -> DeviceType.HB_CHECK

        deviceName.contains("LYSUN", ignoreCase = true) || deviceName.contains("LYSUN BGM", ignoreCase = true) -> DeviceType.GLUCOSE_METER

        // Check for advertised services if available
        /*scanRecord?.serviceUuids?.any {
            it.toString().contains("180d", ignoreCase = true)
        } == true -> DeviceType.HEART_RATE_MONITOR*/

        scanRecord?.serviceUuids?.any {
            it.toString().contains("1810", ignoreCase = true)
        } == true -> DeviceType.BLOOD_PRESSURE_MONITOR

        scanRecord?.serviceUuids?.any {
            it.toString().contains("1808", ignoreCase = true)
        } == true -> DeviceType.GLUCOSE_METER

        // Default
        else -> null
    }
}