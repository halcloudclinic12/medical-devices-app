package com.test.healthbox_app.domain.model

sealed class ScanState {
    /**
     * No scan is currently in progress
     */
    object Idle : ScanState()

    /**
     * A scan is currently in progress
     */
    object Scanning : ScanState()

    /**
     * Devices have been found during scanning
     * @property devices List of discovered Bluetooth devices
     */
    data class DevicesFound(val devices: List<BleDevice>) : ScanState()

    /**
     * Currently attempting to connect to a device
     */
    object Connecting : ScanState()

    /**
     * Successfully connected to a device
     */
    object Connected : ScanState()

    /**
     * An error occurred during scanning or connection
     * @property message Error message describing what went wrong
     */
    data class Error(val message: String) : ScanState()
}