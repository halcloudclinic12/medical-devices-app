package com.test.healthbox_app.data.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import com.quintic.libqpp.QppApi
import com.quintic.libqpp.iQppCallback
import com.test.healthbox_app.bluetooth.DeviceType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.internal.parseCookie
import javax.inject.Inject
import javax.inject.Singleton

// ── Data model for a parsed glucose reading ───────────────────────────────────
data class GlucoseReading(
    val glucoseMgDl: Int,           // e.g. 127
    val glucoseMmol: Double,        // e.g. 7.1  (auto-converted)
    val month: Int,                 // e.g. 3
    val day: Int,                   // e.g. 20
    val hour: Int,                  // e.g. 0
    val minute: Int,                // e.g. 59
    val rawText: String             // full raw string for debugging
)

@Singleton
class QppManager @Inject constructor() {

    private val tag = "QppManager"

    companion object {
        const val UUID_QPP_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb"
        const val UUID_QPP_CHAR_NOTIFY = "0000fff1-0000-1000-8000-00805f9b34fb"
        const val UUID_QPP_CHAR_WRITE = "0000fff2-0000-1000-8000-00805f9b34fb"
    }

    // ── Internal state ────────────────────────────────────────────────────────
    private val initializedDevices = mutableSetOf<DeviceType>()
    private val gattDeviceMap = mutableMapOf<BluetoothGatt, DeviceType>()
    private val deviceGattMap = mutableMapOf<DeviceType, BluetoothGatt>()

    // ── Accumulation buffer — device splits one reading across multiple packets
    private val packetBuffer = mutableMapOf<DeviceType, StringBuilder>()

    // ── Flows exposed upward ──────────────────────────────────────────────────

    // Raw bytes — for debugging or custom parsing
    private val _qppDataFlow = MutableSharedFlow<Pair<DeviceType, ByteArray>>(
        extraBufferCapacity = 64
    )
    val qppDataFlow: SharedFlow<Pair<DeviceType, ByteArray>> = _qppDataFlow.asSharedFlow()

    // Parsed glucose readings — ready to display in UI
    private val _glucoseReadingFlow = MutableSharedFlow<Pair<DeviceType, GlucoseReading>>(
        extraBufferCapacity = 64
    )
    val glucoseReadingFlow: SharedFlow<Pair<DeviceType, GlucoseReading>> = _glucoseReadingFlow.asSharedFlow()

    // ── Register QPP receive callback once at construction ────────────────────
    init {
        QppApi.setCallback(object : iQppCallback {
            override fun onQppReceiveData(
                gatt: BluetoothGatt, qppUUIDForNotifyChar: String, qppData: ByteArray
            ) {
                val deviceType = gattDeviceMap[gatt] ?: run {
                    Log.w(tag, "QPP data from unknown gatt — ignoring")
                    return
                }

                // Log raw packet

                /*val hex = qppData.toHexString()
                Log.d(tag, "══════════════════════════════════════")
                Log.d(tag, "QPP RX [$deviceType] ${qppData.size} bytes")
                Log.d(tag, "  Hex   : $hex")
                Log.d(tag, "  ASCII : ${qppData.toAsciiString()}")
                Log.d(tag, "══════════════════════════════════════")*/

                Log.d(tag, "QPP data from gatt — qppData ::  ${qppData}")
                // Emit raw bytes for any raw observer
                _qppDataFlow.tryEmit(Pair(deviceType, qppData))

                // Accumulate and try to parse
                accumulateAndParse(deviceType, qppData)
            }
        })
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Packet accumulation + parsing
    //
    // Your device sends one reading split across packets, e.g.:
    //   Packet 1: "EAL:\r\nGLU: 127 mg/dL "   (20 bytes)
    //   Packet 2: "\r\n3-20 0:59\r\n\r\n"      (15 bytes)
    //
    // We buffer until we see the double \r\n\r\n terminator, then parse.
    // ─────────────────────────────────────────────────────────────────────────
    private fun accumulateAndParse(deviceType: DeviceType, data: ByteArray) {
        val buffer = packetBuffer.getOrPut(deviceType) { StringBuilder() }
        buffer.append(data.toAsciiString())

        val accumulated = buffer.toString()
        Log.d(tag, "Buffer now: [${accumulated.replace("\r", "\\r").replace("\n", "\\n")}]")

        // Double CRLF = end of one complete reading
        if (accumulated.contains("\r\n\r\n")) {
            // There may be multiple readings in the buffer — split on double CRLF
            val readings = accumulated.split("\r\n\r\n").filter { it.isNotBlank() }

            readings.forEach { block ->
                val trimmed = block.trim()
                // Must contain "GLU:" to be a real reading — skip junk blocks
                if (!trimmed.contains("GLU:")) {
                    Log.d(tag, "Skipping non-glucose block: [${trimmed.take(40)}]")
                    return@forEach
                }

                val parsed = parseReading(trimmed, deviceType)

//                val parsed = parseReading(block.trim(), deviceType)

                Log.d("parsed logs for each ::", "${parsed}")

                if (parsed != null) {
                    Log.d(
                        tag,
                        "✓ Parsed reading: ${parsed.glucoseMgDl} mg/dL  " + "${parsed.glucoseMmol} mmol/L  " + "${parsed.month}/${parsed.day} ${parsed.hour}:${
                            "%02d".format(parsed.minute)
                        }"
                    )

                    _glucoseReadingFlow.tryEmit(Pair(deviceType, parsed))

                } else {
                    Log.w(tag, "Could not parse block: [$block]")
                }
            }

            // Clear buffer after processing complete readings
            // Keep any incomplete trailing data
            val lastTerminator = accumulated.lastIndexOf("\r\n\r\n")
            val remainder = accumulated.substring(lastTerminator + 4)
            buffer.clear()
            if (remainder.isNotBlank()) buffer.append(remainder)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parser
    //
    // Input block example (after trim):
    //   "EAL:\r\nGLU: 127 mg/dL\r\n3-20 0:59"
    //
    // Lines after splitting on \r\n:
    //   [0] "EAL:"
    //   [1] "GLU: 127 mg/dL"
    //   [2] "3-20 0:59"
    // ─────────────────────────────────────────────────────────────────────────
    private fun parseReading(block: String, deviceType: DeviceType): GlucoseReading? {
        return try {
            val lines = block.split("\r\n").map { it.trim() }.filter { it.isNotEmpty() }
            Log.d(tag, "Parsing lines: $lines")

            // Find glucose line — contains "GLU:"
            val glucoseLine = lines.firstOrNull { it.startsWith("GLU:") } ?: return null

            // Extract mg/dL value — "GLU: 127 mg/dL"
            //                              ^^^
            val glucoseMgDl = glucoseLine.removePrefix("GLU:").trim().split(" ").firstOrNull()?.toIntOrNull() ?: return null

            // Convert to mmol/L  (1 mg/dL = 0.0555 mmol/L)
            val glucoseMmol = (glucoseMgDl * 0.0555).let {
                // Round to 1 decimal place
                Math.round(it * 10) / 10.0
            }

            // Find date/time line — format "M-DD H:MM" or "MM-DD HH:MM"
            // e.g. "3-20 0:59"
            val dateTimeLine = lines.firstOrNull { it.matches(Regex("\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{2}")) }

            var month = 0;
            var day = 0;
            var hour = 0;
            var minute = 0

            if (dateTimeLine != null) {
                val parts = dateTimeLine.trim().split("\\s+".toRegex())
                if (parts.size == 2) {
                    val dateParts = parts[0].split("-")
                    val timeParts = parts[1].split(":")
                    if (dateParts.size == 2 && timeParts.size == 2) {
                        month = dateParts[0].toIntOrNull() ?: 0
                        day = dateParts[1].toIntOrNull() ?: 0
                        hour = timeParts[0].toIntOrNull() ?: 0
                        minute = timeParts[1].toIntOrNull() ?: 0
                    }
                }
            }

            GlucoseReading(
                glucoseMgDl = glucoseMgDl, glucoseMmol = glucoseMmol, month = month, day = day, hour = hour, minute = minute, rawText = block
            )

        } catch (e: Exception) {
            Log.e(tag, "Parse error: ${e.message} — block was: [$block]")
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hook 1 — BleConnectionManager.onServicesDiscovered()
    // ─────────────────────────────────────────────────────────────────────────
    fun enable(gatt: BluetoothGatt, deviceType: DeviceType): Boolean {
        val service = gatt.getService(java.util.UUID.fromString(UUID_QPP_SERVICE))
        if (service == null) {
            Log.e(tag, "QPP service not found on device")
            return false
        }
        val writeChar = service.getCharacteristic(java.util.UUID.fromString(UUID_QPP_CHAR_WRITE))
        if (writeChar == null) {
            Log.e(tag, "QPP write characteristic not found")
            return false
        }

        Log.d(tag, "Service OK — ${service.characteristics.size} characteristics")

        return try {
            val success = QppApi.qppEnable(gatt, UUID_QPP_SERVICE, UUID_QPP_CHAR_WRITE)
            if (success) {
                initializedDevices.add(deviceType)
                gattDeviceMap[gatt] = deviceType
                deviceGattMap[deviceType] = gatt
                packetBuffer[deviceType] = StringBuilder()

                // ── Call setQppNextNotify ONCE here, right after qppEnable ────
                // This registers fff1 internally in the library.
                // Do NOT call it again in onDescriptorWrite — that causes
                // IndexOutOfBoundsException because the library only has 1 slot.
                try {
                    QppApi.setQppNextNotify(gatt, true)
                    Log.d(tag, "setQppNextNotify called once ✓")
                } catch (e: Exception) {
                    // Library bug on some firmware — safe to ignore if qppEnable succeeded
                    Log.w(tag, "setQppNextNotify threw (ignored): ${e.message}")
                }

                Log.d(tag, "QPP enabled for $deviceType ✓")
            } else {
                Log.e(tag, "QppApi.qppEnable() returned false")
            }
            success
        } catch (e: Exception) {
            Log.e(tag, "QPP enable exception: ${e.message}")
            false
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hook 2 — BleConnectionManager.onDescriptorWrite()
    // NO-OP: this device has only 1 notify characteristic (fff1).
    // Calling setQppNextNotify() causes IndexOutOfBoundsException.
    // ─────────────────────────────────────────────────────────────────────────
    fun enableNotify(gatt: BluetoothGatt) {
        Log.d(tag, "enableNotify — skipped (single notify char device)")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hook 3 — BleConnectionManager.onCharacteristicChanged()
    // ─────────────────────────────────────────────────────────────────────────
    fun updateNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        QppApi.updateValueForNotification(gatt, characteristic)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hook 4 — BleConnectionManager.onConnectionStateChange() disconnected
    // ─────────────────────────────────────────────────────────────────────────
    fun onDeviceDisconnected(gatt: BluetoothGatt, deviceType: DeviceType) {
        initializedDevices.remove(deviceType)
        gattDeviceMap.remove(gatt)
        deviceGattMap.remove(deviceType)
        packetBuffer.remove(deviceType)
        Log.d(tag, "QPP state cleaned up for $deviceType")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Send — called by BleConnectionManager.sendQppData() which handles chunking
    // ─────────────────────────────────────────────────────────────────────────
    fun sendData(gatt: BluetoothGatt, data: ByteArray): Boolean {
        if (data.size > 20) {
            Log.e(tag, "Packet too large: ${data.size} bytes (max 20)")
            return false
        }
        Log.d(tag, "QPP TX: ${data.toHexString()}")
        return QppApi.qppSendData(gatt, data)
    }

    fun sendData(deviceType: DeviceType, data: ByteArray): Boolean {
        val gatt = deviceGattMap[deviceType] ?: run {
            Log.e(tag, "sendData — no gatt for $deviceType")
            return false
        }
        return sendData(gatt, data)
    }

    fun isInitialized(deviceType: DeviceType): Boolean = deviceType in initializedDevices

    // ─────────────────────────────────────────────────────────────────────────
    // Extensions
    // ─────────────────────────────────────────────────────────────────────────
    private fun ByteArray.toHexString(): String = joinToString(" ") { "%02X".format(it) }

    private fun ByteArray.toAsciiString(): String =
//        toString(Charsets.ISO_8859_1)
        toString(Charsets.ISO_8859_1).filter { it == '\r' || it == '\n' || it in ' '..'~' }
}