package com.test.healthbox_app.domain

import com.test.healthbox_app.bluetooth.DeviceType
import java.util.UUID

data class BleResponse(
    val data: ByteArray,
    val deviceType: DeviceType,
    val characteristicUuid: UUID
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleResponse
        return data.contentEquals(other.data) &&
                deviceType == other.deviceType &&
                characteristicUuid == other.characteristicUuid
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + deviceType.hashCode()
        result = 31 * result + characteristicUuid.hashCode()
        return result
    }
}