package com.test.healthbox_app.data.repository

import android.app.Activity
import android.bluetooth.BluetoothManager
import android.content.Context
import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.data.ble.BleConnectionManager
import com.test.healthbox_app.data.ble.BleScanner
import com.test.healthbox_app.data.data_source.BiosenseDataSource
import com.test.healthbox_app.data.mapper.toBluetoothDevice
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.ConnectionState
import com.test.healthbox_app.domain.model.HbCheckMeasurement
import com.test.healthbox_app.domain.repository.BleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class BleRepositoryImpl @Inject constructor(
    private val context: Context,
    private val bleScanner: BleScanner,
    private val bleConnectionManager: BleConnectionManager,
    private val biosenseDataSource: BiosenseDataSource
) : BleRepository {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val scanner = bluetoothAdapter.bluetoothLeScanner

    override val  connectionState: Flow<Map<DeviceType, ConnectionState>>
        get() = bleConnectionManager.connectionsState

    override suspend fun scanForDevices(timeout: Long): Flow<List<BleDevice>> {
        return bleScanner.scan(timeout).map { scanResults ->
            scanResults.map { scanResult ->
//                scanResults.mapScanResultToDevice(scanResult)
                scanResult.toBluetoothDevice()
            }
        }
    }

    override suspend fun stopBleScan() {
        if (bleScanner != null)
            bleScanner.stopBleScan()
    }

    /*@SuppressLint("MissingPermission")
    override fun scanForDevices(scanDurationMillis: Long): Flow<List<BleDevice>> = callbackFlow {
        if (!bluetoothAdapter.isEnabled) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val devices = ConcurrentHashMap<String, BleDevice>()

        val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.toBluetoothDevice()

                devices[result.device.address] = device

                trySend(devices.values.toList())
            }

            override fun onScanFailed(errorCode: Int) {
                close(Throwable("Scan failed with error code: $errorCode"))
            }
        }

        scanner.startScan(null, scanSettings, scanCallback)

        // Auto-stop scan after specified duration
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            scanner.stopScan(scanCallback)
            if (!isClosedForSend) {
                trySend(devices.values.toList())
                close()
            }
        }, scanDurationMillis)

        awaitClose {
            scanner.stopScan(scanCallback)
        }
    }.flowOn(Dispatchers.IO)*/


    override suspend fun connectToDevice(
        device: BleDevice, deviceType: DeviceType
    ): Flow<Result<Unit>> {
        return bleConnectionManager.connect(device, deviceType)
    }

    override suspend fun disconnect(
        device: BleDevice, deviceType: DeviceType
    ): Flow<Result<Unit>> {
        return bleConnectionManager.disconnect(device, deviceType)
    }

    override suspend fun sendCommand(
        command: String,
        deviceType: DeviceType,
        serviceUuid: UUID,
        characteristicUuid: UUID
    ): Result<Unit> {
        return try {
            // Convert string command to ByteArray
            val data = command.toByteArray(Charsets.UTF_8)
            bleConnectionManager.writeCharacteristic(
                deviceType,
                serviceUuid,
                characteristicUuid,
                data
            ).first()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun startNotifications(
        deviceType: DeviceType,
        serviceUuid: UUID,
        characteristicUuid: UUID
    ): Result<Unit> {
        return try {
            bleConnectionManager.enableNotifications(
                deviceType,
                serviceUuid,
                characteristicUuid
            ).first()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeNotifications(
        deviceType: DeviceType,
        characteristicUuid: UUID
    ): Flow<ByteArray> {
        return bleConnectionManager.getNotifications(deviceType, characteristicUuid)
    }

    override fun observeConnectionState(): Flow<Map<DeviceType, ConnectionState>> {
        return bleConnectionManager.connectionsState
    }

    override fun startHbTest() {
        return biosenseDataSource.startHbTest()
    }

    override fun stopHbTest() {
        return biosenseDataSource.stopHbTest()
    }

    override fun getHBCheckResultFlow(): Flow<HbCheckMeasurement> {
        return biosenseDataSource.getResultFlow()
    }

    override val hbCheckConnectionState: Flow<Boolean> = biosenseDataSource.connectionState

    override fun connectToHBDevice(deviceAddress: String, deviceName: String, mActivity: Activity): Flow<HbCheckMeasurement> {
        return biosenseDataSource.connectToDevice(deviceAddress, deviceName, mActivity)
    }

    override fun disconnectHBDevice() {
        biosenseDataSource.disconnectDevice()
    }

    override fun sendQppData(deviceType: DeviceType, data: ByteArray): Boolean {
        return bleConnectionManager.sendQppData(deviceType, data)
    }

    override fun observeQppData(deviceType: DeviceType): Flow<ByteArray> {
        return bleConnectionManager.observeQppData(deviceType)
    }

    /*@SuppressLint("MissingPermission")
    override suspend fun connectToDevice(
        device: BluetoothDevice, deviceType: DeviceType
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val remoteDevice = bluetoothAdapter.getRemoteDevice(device.address)
            var connected = false

            val gattCallback = object : BluetoothGattCallback() {
                @SuppressLint("MissingPermission")
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt, status: Int, newState: Int
                ) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (newState == BluetoothGatt.STATE_CONNECTED) {
                            connected = true
                            currentDevice = device
//                                _connectionState.value = ConnectionState(true, device)
                            _connectionState.update {
                                it + (deviceType to ConnectionState(
                                    true, device
                                ))
                            }
                        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                            gatt.close()
                            currentDevice = null
                            _connectionState.value = ConnectionState(false, null)
                        }
                    } else {
                        gatt.close()
                        currentDevice = null
                        _connectionState.value = ConnectionState(false, null)
                    }
                }
            }

            // Close existing connection if any
            bluetoothGatt?.close()

            // Connect to the new device
            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                remoteDevice.connectGatt(
                    context, false, gattCallback, android.bluetooth.BluetoothDevice.TRANSPORT_LE
                )
            } else {
                remoteDevice.connectGatt(context, false, gattCallback)
            }

            // Wait for connection to be established or fail
            var timeout = 10000L // 10 seconds
            val sleepInterval = 100L
            while (!connected && timeout > 0) {
                Thread.sleep(sleepInterval)
                timeout -= sleepInterval
            }

            if (connected) {
                Result.success(Unit)
            } else {
                bluetoothGatt?.close()
                bluetoothGatt = null
                Result.failure(Exception("Connection timeout"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }*/

    /*@SuppressLint("MissingPermission")
    override suspend fun disconnect(device: BluetoothDevice, deviceType: DeviceType): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()
                bluetoothGatt = null
                currentDevice = null

                _connectionState.value = ConnectionState(false, null)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }*/

    /*override fun observeConnectionState(): Flow<ConnectionState> {
        return _connectionState.asStateFlow()
    }*/

}
