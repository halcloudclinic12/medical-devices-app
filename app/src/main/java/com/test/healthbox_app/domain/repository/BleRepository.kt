package com.test.healthbox_app.domain.repository

import android.app.Activity
import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.ConnectionState
import com.test.healthbox_app.domain.model.HbCheckMeasurement
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface BleRepository {
    /**
     * Get the current connection state for all device types
     */
    val connectionState: Flow<Map<DeviceType, ConnectionState>>

    suspend fun scanForDevices(scanDurationMillis: Long): Flow<List<BleDevice>>
    suspend fun stopBleScan()
    suspend fun connectToDevice(device: BleDevice, deviceType: DeviceType): Flow<Result<Unit>>
    suspend fun disconnect(device: BleDevice, deviceType: DeviceType): Flow<Result<Unit>>
//    suspend fun writeCharacteristic(deviceType: DeviceType, serviceUuid: UUID, characteristicUuid: UUID, data: ByteArray): Flow<Result<Unit>>

    suspend fun sendCommand(command: String, deviceType: DeviceType, serviceUuid: UUID, characteristicUuid: UUID): Result<Unit>
    suspend fun startNotifications(deviceType: DeviceType, serviceUuid: UUID, characteristicUuid: UUID): Result<Unit>
    fun observeNotifications(deviceType: DeviceType, characteristicUuid: UUID): Flow<ByteArray>
    fun observeConnectionState(): Flow<Map<DeviceType, ConnectionState>>

    // For HB Devices
    val hbCheckConnectionState: Flow<Boolean>
    fun startHbTest()
    fun stopHbTest()
    fun getHBCheckResultFlow(): Flow<HbCheckMeasurement>
    fun connectToHBDevice(deviceAddress: String, deviceName: String, mActivity: Activity): Flow<HbCheckMeasurement>
    fun disconnectHBDevice()

    // QPP-specific
    fun sendQppData(deviceType: DeviceType, data: ByteArray): Boolean
    fun observeQppData(deviceType: DeviceType): Flow<ByteArray>

}