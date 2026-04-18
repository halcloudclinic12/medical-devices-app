package com.test.healthbox_app.domain.model

import com.test.healthbox_app.bluetooth.DeviceType

data class BleDevice(
    val address: String,
    val name: String,
    val rssi: Int,
    val deviceType: DeviceType?,
    val bondState: Int,
    val scanRecord: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleDevice

        if (address != other.address) return false

        return true
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }
}