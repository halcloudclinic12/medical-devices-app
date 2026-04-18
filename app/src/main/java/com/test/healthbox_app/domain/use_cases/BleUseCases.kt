package com.test.healthbox_app.domain.use_cases

import android.app.Activity
import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.ConnectionState
import com.test.healthbox_app.domain.model.HbCheckMeasurement
import com.test.healthbox_app.domain.repository.BleRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class BleUseCases(
    private val bleRepository: BleRepository
) {

    fun getConnectionState(): Flow<Map<DeviceType, ConnectionState>> {
        return bleRepository.connectionState
    }

    /**
     * Use case for scanning for available BLE devices
     * @param scanDurationMillis Duration of scan in milliseconds
     * @return Flow of discovered devices
     */

    suspend fun scanForDevices(scanDurationMillis: Long = 10000): Flow<List<BleDevice>> {
        return bleRepository.scanForDevices(scanDurationMillis)
    }

    suspend fun stopBleScan() {
        return bleRepository.stopBleScan()
    }

    /**
     * Use case for connecting to a specific BLE device
     * @param device The device to connect to
     * @return Result indicating success or failure
     */
    suspend fun connectToDevice(
        device: BleDevice, deviceType: DeviceType
    ): Flow<Result<Unit>> {
        return bleRepository.connectToDevice(device, deviceType)
    }


    /**
     * Use case for disconnecting from the currently connected device
     * @return Result indicating success or failure
     */
    suspend fun disconnect(
        device: BleDevice, deviceType: DeviceType
    ): Flow<Result<Unit>> {
        return bleRepository.disconnect(device = device, deviceType = deviceType)
    }

    suspend fun sendCommand(
        command: String, deviceType: DeviceType, serviceUuid: UUID, characteristicUuid: UUID
    ): Result<Unit> {
        return bleRepository.sendCommand(
            command = command, deviceType = deviceType, serviceUuid = serviceUuid, characteristicUuid = characteristicUuid
        )
    }

    suspend fun startNotifications(
        deviceType: DeviceType, serviceUuid: UUID, characteristicUuid: UUID
    ): Result<Unit> {
        return bleRepository.startNotifications(
            deviceType = deviceType, serviceUuid = serviceUuid, characteristicUuid = characteristicUuid
        )
    }

    fun observeNotifications(
        deviceType: DeviceType, characteristicUuid: UUID
    ): Flow<ByteArray> {
        return bleRepository.observeNotifications(
            deviceType = deviceType, characteristicUuid = characteristicUuid
        )
    }

    fun observeConnectionState(): Flow<Map<DeviceType, ConnectionState>> {
        return bleRepository.observeConnectionState()
    }

    fun getHbCheckConnectionState(): Flow<Boolean> {
        return bleRepository.hbCheckConnectionState
    }

    fun getHBCheckResultFlow(): Flow<HbCheckMeasurement> {
        return bleRepository.getHBCheckResultFlow()
    }


    fun connectToHBDevice(bleDevice: BleDevice, mActivity: Activity): Flow<HbCheckMeasurement> {
        return bleRepository.connectToHBDevice(bleDevice.address, bleDevice.name, mActivity)
    }

    fun disconnectToHBDevice() {
        bleRepository.disconnectHBDevice()
    }

    fun startHbTest() {
        bleRepository.startHbTest()
    }

    fun stopHbTest() {
        bleRepository.stopHbTest()
    }

    /*fun sendQppData(deviceType: DeviceType, data: ByteArray): Boolean {
        return bleRepository.sendQppData(deviceType, data)
    }

    fun observeQppData(deviceType: DeviceType): Flow<ByteArray> {
        return bleRepository.observeQppData(deviceType)
    }
*/

    /**
     * Use case for observing the current connection state
     * @return Flow of connection state updates
     *//*fun observeConnectionState(): Flow<ConnectionState> {
        return bluetoothRepository.observeConnectionState()
    }*/
}