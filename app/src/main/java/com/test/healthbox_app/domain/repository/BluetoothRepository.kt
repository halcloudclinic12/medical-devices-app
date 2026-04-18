package com.test.healthbox_app.domain.repository

import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.BluetoothCommand
import com.test.healthbox_app.domain.model.ConnectionState
import com.test.healthbox_app.domain.model.PrintJob
import com.test.healthbox_app.domain.model.PrintResult
import kotlinx.coroutines.flow.Flow

interface BluetoothRepository {
    suspend fun startScan(): Flow<List<BleDevice>>
    suspend fun stopScan()
    suspend fun pairDevice(device: BleDevice): Flow<Boolean>
    suspend fun connectDevice(device: BleDevice): Flow<ConnectionState>
    suspend fun disconnectDevice()
    suspend fun sendCommand(command: BluetoothCommand): Flow<ByteArray>
    suspend fun printText(printJob: PrintJob): Result<PrintResult>
    suspend fun isDeviceConnected(): Boolean
    fun getConnectionState(): Flow<ConnectionState>
    fun isBluetoothEnabled(): Boolean
    suspend fun enableBluetooth(): Boolean

}