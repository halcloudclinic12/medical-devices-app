package com.test.healthbox_app.domain.model

import java.util.UUID

data class BleCommand(
    val command: ByteArray,
    val serviceUuid: UUID,
    val characteristicUuid: UUID
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleCommand
        return command.contentEquals(other.command) &&
                serviceUuid == other.serviceUuid &&
                characteristicUuid == other.characteristicUuid
    }

    override fun hashCode(): Int {
        var result = command.contentHashCode()
        result = 31 * result + serviceUuid.hashCode()
        result = 31 * result + characteristicUuid.hashCode()
        return result
    }
}