package com.test.healthbox_app.domain.model

data class BluetoothCommand(
    val command: String, val data: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BluetoothCommand
        if (command != other.command) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = command.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        return result
    }
}
