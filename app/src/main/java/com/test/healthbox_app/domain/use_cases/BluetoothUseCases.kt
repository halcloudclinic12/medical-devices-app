package com.test.healthbox_app.domain.use_cases

import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.BluetoothCommand
import com.test.healthbox_app.domain.model.ConnectionState
import com.test.healthbox_app.domain.model.PrintJob
import com.test.healthbox_app.domain.model.PrintResult
import com.test.healthbox_app.domain.repository.BluetoothRepository
import kotlinx.coroutines.flow.Flow

class BluetoothUseCases(private val repository: BluetoothRepository) {

    // Scan Use Case
    suspend fun scanForDevices(): Flow<List<BleDevice>> {
        return repository.startScan()
    }

    suspend fun stopScanning() {
        repository.stopScan()
    }

    // Pairing Use Case
    suspend fun pairDevice(device: BleDevice): Flow<Boolean> {
        return repository.pairDevice(device)
    }

    // Connection Use Case
    suspend fun connectToDevice(device: BleDevice): Flow<ConnectionState> {
        return repository.connectDevice(device)
    }

    // Connection Use Case
    suspend fun printText(message: String): Result<PrintResult> {
        return try {
            val printJob = PrintJob(message = message)
            repository.printText(printJob)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkPrinterConnection(): Boolean {
        return repository.isDeviceConnected()
    }


    suspend fun disconnectDevice() {
        repository.disconnectDevice()
    }

    // Communication Use Case
    suspend fun sendCommand(command: BluetoothCommand): Flow<ByteArray> {
        return repository.sendCommand(command)
    }

    fun getConnectionState(): Flow<ConnectionState> {
        return repository.getConnectionState()
    }

    // Bluetooth State Use Case
    fun isBluetoothEnabled(): Boolean {
        return repository.isBluetoothEnabled()
    }

    suspend fun enableBluetooth(): Boolean {
        return repository.enableBluetooth()
    }
}