package com.test.healthbox_app.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.icu.text.DecimalFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.ConnectionState
import com.test.healthbox_app.presentation.util.BleConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val qppManager: QppManager
) {
    private val tag = "BleConnectionManager"

    // Bluetooth system services
    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy { bluetoothManager.adapter }

    // Maps to track connections by device type
    private val gattConnections = mutableMapOf<DeviceType, BluetoothGatt?>()
    private val gattCallbacks = mutableMapOf<DeviceType, DeviceGattCallback>()

    private val servicesDiscovered = mutableMapOf<DeviceType, Boolean>()

    // Connection state for all device types
    private val _connectionsState = MutableStateFlow<Map<DeviceType, ConnectionState>>(emptyMap())
    val connectionsState: MutableStateFlow<Map<DeviceType, ConnectionState>> = _connectionsState

    /**
     * Connect to a BLE device
     * @param bleDevice BleDevice to connect to
     * @param deviceType Type of the device for proper service/characteristic handling
     * @return Flow emitting connection results
     */
    @SuppressLint("MissingPermission")
    fun connect(bleDevice: BleDevice, deviceType: DeviceType): Flow<Result<Unit>> = callbackFlow {

        // ── Close any stale connection before opening a new one ──────────
        gattConnections[deviceType]?.let { staleGatt ->
            Log.w(tag, "Closing stale $deviceType connection before reconnect")
            try {
                staleGatt.disconnect()
                staleGatt.close()
            } catch (e: Exception) {
                Log.e(tag, "Error closing stale gatt", e)
            }
            gattConnections.remove(deviceType)
            gattCallbacks.remove(deviceType)
            servicesDiscovered.remove(deviceType)
        }
        // ─────────────────────────────────────────────────────────────────

        // Check if Bluetooth is enabled
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            trySend(Result.failure(IllegalStateException("Bluetooth is not enabled")))
            close()
            return@callbackFlow
        }

        // Get the Bluetooth device
        val device = try {
            bluetoothAdapter!!.getRemoteDevice(bleDevice.address)
        } catch (e: IllegalArgumentException) {
            trySend(Result.failure(IllegalArgumentException("Invalid device address: ${bleDevice.address}")))
            close()
            return@callbackFlow
        }

        // Update connection state
        _connectionsState.update { it + (deviceType to ConnectionState.Connecting) }

        // Create device-specific callback
        val callback = DeviceGattCallback(deviceType) { result ->
            trySend(result)
        }

        // Store callback
        gattCallbacks[deviceType] = callback

        // Connect to GATT server
        val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.e("autoConnDevLog", "   : $deviceType >M :  ${deviceType == DeviceType.THERMOMETER}")
            device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        } else {

            Log.e("autoConnDevLog", " $deviceType  : <M :   ${deviceType == DeviceType.THERMOMETER}")
            device.connectGatt(context, false, callback)
        }

        // Store GATT connection
        gattConnections[deviceType] = gatt

        // Clean up when flow collection ends
        awaitClose {
            disconnect(bleDevice, deviceType)
        }
    }

    /**
     * Disconnect from a BLE device
     * @param device BleDevice to disconnect from
     * @param deviceType Type of the device
     * @return Flow emitting disconnection results
     */
    @SuppressLint("MissingPermission")
    fun disconnect(device: BleDevice, deviceType: DeviceType): Flow<Result<Unit>> = callbackFlow {

        Log.e("disDeviceLogs", "  :$deviceType  : device :  $device")

        val gatt = gattConnections[deviceType]
        if (gatt == null) {
            trySend(Result.success(Unit)) // Already disconnected
            _connectionsState.update { it + (deviceType to ConnectionState.Disconnected()) }
            close()
            return@callbackFlow
        }

        try {
            gatt.disconnect()
            gatt.close()

            gattConnections.remove(deviceType)
            gattCallbacks.remove(deviceType)

            servicesDiscovered.remove(deviceType)  // Clear discovery state

            _connectionsState.update { it + (deviceType to ConnectionState.Disconnected()) }
            trySend(Result.success(Unit))
        } catch (e: Exception) {
            Log.e(tag, "Error disconnecting from device", e)
            trySend(Result.failure(e))
        }

        close()
    }

    /**
     * Read characteristic value from connected device
     * @param deviceType Type of the connected device
     * @param serviceUuid UUID of the service
     * @param characteristicUuid UUID of the characteristic to read
     * @return Flow emitting characteristic value read results
     */
    @SuppressLint("MissingPermission")
    fun readCharacteristic(
        deviceType: DeviceType, serviceUuid: UUID, characteristicUuid: UUID
    ): Flow<Result<ByteArray>> = callbackFlow {
        val gatt = gattConnections[deviceType]
        val callback = gattCallbacks[deviceType]

        if (gatt == null || callback == null) {
            trySend(Result.failure(IllegalStateException("Device not connected")))
            close()
            return@callbackFlow
        }

        // Set read listener for this specific read operation
        callback.setCharacteristicReadListener { uuid, value, error ->
            if (uuid == characteristicUuid) {
                if (error == null) {
                    trySend(Result.success(value))
                    close()
                } else {
                    trySend(Result.failure(error))
                    close()
                }
            }
        }

        // Find service and characteristic
        val service = gatt.getService(serviceUuid)
        if (service == null) {
            trySend(Result.failure(IllegalStateException("Service not found: $serviceUuid")))
            close()
            return@callbackFlow
        }

        val characteristic = service.getCharacteristic(characteristicUuid)
        if (characteristic == null) {
            trySend(Result.failure(IllegalStateException("Characteristic not found: $characteristicUuid")))
            close()
            return@callbackFlow
        }

        // Read characteristic
        val success = gatt.readCharacteristic(characteristic)

        if (!success) {
            trySend(Result.failure(IllegalStateException("Failed to read characteristic")))
            close()
        }

        awaitClose {
            callback.setCharacteristicReadListener(null)
        }
    }

    /**
     * Write characteristic value to connected device
     * @param deviceType Type of the connected device
     * @param serviceUuid UUID of the service
     * @param characteristicUuid UUID of the characteristic to write
     * @param data Data to write
     * @return Flow emitting write operation results
     */
    @SuppressLint("MissingPermission")
    fun writeCharacteristic(
        deviceType: DeviceType, serviceUuid: UUID, characteristicUuid: UUID, data: ByteArray
    ): Flow<Result<Unit>> = callbackFlow {
        val gatt = gattConnections[deviceType]
        val callback = gattCallbacks[deviceType]

        if (gatt == null || callback == null) {
            trySend(Result.failure(IllegalStateException("Device not connected")))
            close()
            return@callbackFlow
        }

        // Set write listener for this specific write operation
        /*callback.setCharacteristicWriteListener { uuid, error ->
            if (uuid == characteristicUuid) {
                if (error == null) {
                    trySend(Result.success(Unit))
                    close()
                } else {
                    trySend(Result.failure(error))
                    close()
                }
            }
        }*/

        // Find service and characteristic
        val service = gatt.getService(serviceUuid)
//        val service = gatt.getService(UUID.fromString(BleConstants.HEIGHT_SERVICE))
        if (service == null) {
            trySend(Result.failure(IllegalStateException("Service not found: $serviceUuid")))
            close()
            return@callbackFlow
        }

        val characteristic = service.getCharacteristic(characteristicUuid)
//        val characteristic = service.getCharacteristic(UUID.fromString(BleConstants.HEIGHT_MEASUREMENT))
        if (characteristic == null) {
            trySend(Result.failure(IllegalStateException("Characteristic not found: $characteristicUuid")))
            close()
            return@callbackFlow
        }

        // Set value and write type
        characteristic.value = data
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

        // Write characteristic - handle API level differences
        val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            gatt.writeCharacteristic(characteristic)
        }


        if (success == false) {
            trySend(Result.failure(IllegalStateException("Failed to write characteristic")))
            close()
        }

        awaitClose {
            callback.setCharacteristicWriteListener(null)
        }
    }

    /**
     * Enable notifications for a characteristic
     * @param deviceType Type of the connected device
     * @param serviceUuid UUID of the service
     * @param characteristicUuid UUID of the characteristic to enable notifications for
     * @return Flow emitting enable notification results
     */
    @SuppressLint("MissingPermission")
    fun enableNotifications(
        deviceType: DeviceType,
        serviceUuid: UUID,
        characteristicUuid: UUID
    ): Flow<Result<Unit>> = callbackFlow {

        Log.d("enableNotesLogs", "Enabling notifications for $deviceType : $characteristicUuid")

        val callback = gattCallbacks[deviceType]
        if (callback == null) {
            trySend(Result.failure(IllegalStateException("Device not connected")))
            close()
            return@callbackFlow
        }

        // Set notification listener early
        callback.setNotificationListener(characteristicUuid) { _, _ ->
            trySend(Result.success(Unit))
        }

        // ── Wait for services, always re-fetching gatt from the live map ──
        var attempts = 0
        val maxAttempts = 10
        while (attempts < maxAttempts) {
            val currentGatt = gattConnections[deviceType]
            if (currentGatt != null && !currentGatt.services.isNullOrEmpty()) {
                Log.d(tag, "Services ready after $attempts attempts")
                break
            }
            delay(300)
            attempts++
            Log.d(tag, "Waiting for services... Attempt $attempts")
        }

        // ── Always use the live gatt reference from this point on ──────────
        val gatt = gattConnections[deviceType]
        if (gatt == null) {
            trySend(Result.failure(IllegalStateException("GATT connection lost while waiting for services")))
            close()
            return@callbackFlow
        }

        if (gatt.services.isNullOrEmpty()) {
            trySend(Result.failure(IllegalStateException("Services not discovered in time")))
            close()
            return@callbackFlow
        }

        Log.d(tag, "Available services: ${gatt.services.size}")
        Log.e("enableNotiLogs", ":$deviceType : services : ${gatt.services}")

        val service = gatt.getService(serviceUuid)
        if (service == null) {
            trySend(Result.failure(IllegalStateException("Service not found: $serviceUuid")))
            close()
            return@callbackFlow
        }

        val characteristic = service.getCharacteristic(characteristicUuid)
        if (characteristic == null) {
            trySend(Result.failure(IllegalStateException("Characteristic not found: $characteristicUuid")))
            close()
            return@callbackFlow
        }

        // Enable local notifications on the live gatt
        val notificationEnabled = gatt.setCharacteristicNotification(characteristic, true)
        if (!notificationEnabled) {
            Log.e("strtListInP", "Error: Failed to enable local notifications")
            trySend(Result.failure(IllegalStateException("Failed to enable local notifications")))
            close()
            return@callbackFlow
        }

        // Write CCCD descriptor
        val descriptor = characteristic.getDescriptor(
            UUID.fromString(BleConstants.CLIENT_CHARACTERISTIC_CONFIG)
        )

        if (descriptor != null) {
            val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                if (gatt.writeDescriptor(descriptor)) BluetoothStatusCodes.SUCCESS else BluetoothStatusCodes.ERROR_UNKNOWN
            }

            if (success != BluetoothStatusCodes.SUCCESS) {
                trySend(Result.failure(IllegalStateException("Failed to write notification descriptor")))
                close()
            }
        } else {
            trySend(Result.success(Unit)) // No CCCD needed
        }

        awaitClose {
            try {
                // Optionally disable on teardown
                // gattConnections[deviceType]?.setCharacteristicNotification(characteristic, false)
                callback.removeNotificationListener(characteristicUuid)
            } catch (e: Exception) {
                Log.e(tag, "Error disabling notifications", e)
            }
        }
    }


    /*fun enableNotifications(
        deviceType: DeviceType, serviceUuid: UUID, characteristicUuid: UUID
    ): Flow<Result<Unit>> = callbackFlow {
        Log.d("enableNotesLogs", "Enabling notifications for  $deviceType : : $characteristicUuid   : servi : ${getServices(deviceType)}")

        val gatt = gattConnections[deviceType]
        val callback = gattCallbacks[deviceType]

        if (gatt == null || callback == null) {
            trySend(Result.failure(IllegalStateException("Device not connected")))
            close()
            return@callbackFlow
        }

        Log.d(tag, "Enabling notifications for: $characteristicUuid   ")

        // Set notification listener
        callback.setNotificationListener(characteristicUuid) { _, _ ->
            // Initial setup success just returns success
            trySend(Result.success(Unit))
        }

        // Add a check and wait for services to be discovered
        if (gatt.services.isNullOrEmpty()) {
            Log.d(tag, "Services not yet discovered, waiting...")
            // Wait for services to be discovered with a timeout
            var attempts = 0
            val maxAttempts = 10
            while (gatt.services.isNullOrEmpty() && attempts < maxAttempts) {
                delay(300) // Wait 300ms
                attempts++
                Log.d(tag, "Waiting for services... Attempt $attempts")
            }

            if (gatt.services.isNullOrEmpty()) {
                Log.e(tag, "Services still not available after waiting")
                trySend(Result.failure(IllegalStateException("Services not discovered in time")))
                close()
                return@callbackFlow
            }
        }

        Log.d(tag, "Available services: ${gatt.services.size}")

        Log.e("enableNotiLogs", "  :$deviceType :  services  :  ${gatt.services}")
        // Find service and characteristic
        val service = gatt.getService(serviceUuid)
        if (service == null) {
            trySend(Result.failure(IllegalStateException("Service not found: $serviceUuid")))
            close()
            return@callbackFlow
        }

        val characteristic = service.getCharacteristic(characteristicUuid)
        if (characteristic == null) {
            trySend(Result.failure(IllegalStateException("Characteristic not found: $characteristicUuid")))
            close()
            return@callbackFlow
        }

        // Enable local notifications
        val notificationEnabled = gatt.setCharacteristicNotification(characteristic, true)
        if (!notificationEnabled) {
            trySend(Result.failure(IllegalStateException("Failed to enable local notifications")))
            close()
            return@callbackFlow
        }

        // Write descriptor to enable remote notifications
//        val descriptor = characteristic.getDescriptor(characteristicUuid)
        val descriptor = characteristic.getDescriptor(UUID.fromString(BleConstants.CLIENT_CHARACTERISTIC_CONFIG))
        if (descriptor != null) {
            // Write descriptor - handle API level differences
            val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33+ method
                gatt.writeDescriptor(
                    descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                )
            } else {
                // Pre-API 33 method
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }

            if (success == false) {
                trySend(Result.failure(IllegalStateException("Failed to write notification descriptor")))
                close()
            }
        } else {
            // Some characteristics can enable notifications without descriptor
            trySend(Result.success(Unit))
        }

        awaitClose {
            // Disable notifications when flow collection ends
            try {
//                gatt.setCharacteristicNotification(characteristic, false)
//                callback.removeNotificationListener(characteristicUuid)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(tag, "Error disabling notifications", e)
            }
        }
    }*/

    /**
     * Get a flow of characteristic notifications
     * @param deviceType Type of the connected device
     * @param characteristicUuid UUID of the characteristic to receive notifications from
     * @return Flow emitting notification values
     */
    fun getNotifications(
        deviceType: DeviceType,
        characteristicUuid: UUID
    ): Flow<ByteArray> = callbackFlow {
        val callback = gattCallbacks[deviceType]

        if (callback == null) {
            close()
            return@callbackFlow
        }

        // Set notification listener for characteristic data
        callback.setNotificationDataListener(characteristicUuid) { data ->
            trySend(data)
        }

        awaitClose {
            callback.removeNotificationDataListener(characteristicUuid)
        }
    }

    /**
     * Get services discovered for a connected device
     * @param deviceType Type of connected device
     * @return List of discovered services or null if not connected
     */
    fun getServices(deviceType: DeviceType): List<BluetoothGattService>? {
        return gattConnections[deviceType]?.services
    }

    /**
     * Check if device is connected
     * @param deviceType Type of the device
     * @return boolean indicating if the device is connected
     */
    fun isConnected(deviceType: DeviceType): Boolean {
        return connectionsState.value[deviceType] == ConnectionState.Connected
    }

    /**
     * Custom GATT callback for handling device-specific BLE operations
     */
    private inner class DeviceGattCallback(
        private val deviceType: DeviceType,
        private val connectionCallback: (Result<Unit>) -> Unit
    ) : BluetoothGattCallback() {

        private var characteristicReadListener: ((UUID, ByteArray, Throwable?) -> Unit)? = null
        private var characteristicWriteListener: ((UUID, Throwable?) -> Unit)? = null
        private val notificationListeners = mutableMapOf<UUID, (UUID, Boolean) -> Unit>()
        private val notificationDataListeners = mutableMapOf<UUID, (ByteArray) -> Unit>()

        fun setCharacteristicReadListener(listener: ((UUID, ByteArray, Throwable?) -> Unit)?) {
            characteristicReadListener = listener
        }

        fun setCharacteristicWriteListener(listener: ((UUID, Throwable?) -> Unit)?) {
            characteristicWriteListener = listener
        }

        fun setNotificationListener(characteristicUuid: UUID, listener: (UUID, Boolean) -> Unit) {
            notificationListeners[characteristicUuid] = listener
        }

        fun removeNotificationListener(characteristicUuid: UUID) {
            notificationListeners.remove(characteristicUuid)
        }

        fun setNotificationDataListener(characteristicUuid: UUID, listener: (ByteArray) -> Unit) {
            notificationDataListeners[characteristicUuid] = listener
        }

        fun removeNotificationDataListener(characteristicUuid: UUID) {
            notificationDataListeners.remove(characteristicUuid)
        }

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (gatt == null) return

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(tag, "Connected to GATT server")
                    _connectionsState.update { it + (deviceType to ConnectionState.Connected) }

                    // Discover services after successful connection
                    gatt.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(tag, "Disconnected from GATT server")

                    // ✅ QPP cleanup on disconnect
                    if (isQppDevice(deviceType)) {
                        qppManager.onDeviceDisconnected(gatt, deviceType)
                    }

                    _connectionsState.update { it + (deviceType to ConnectionState.Disconnected()) }

                    gattConnections.remove(deviceType)

                    // Clean up
                    gatt.close()

                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        connectionCallback(Result.failure(Exception("Disconnected with status: $status")))
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (gatt == null) return

            // ── Only process if this gatt is the one we're tracking ──────────
            val activeGatt = gattConnections[deviceType]
            if (activeGatt != null && gatt != activeGatt) {
                Log.w(tag, "Stale GATT for $deviceType — closing extra gatt")
                gatt.disconnect()
                gatt.close()
                return
            }

            if (servicesDiscovered[deviceType] == true) {
                Log.w(tag, "Duplicate onServicesDiscovered for $deviceType — ignoring")
                return  // Don't close — it's the same gatt, just a duplicate callback
            }

            // ── Deduplicate: only process once per deviceType ─────────────────
            /*if (servicesDiscovered[deviceType] == true) {
                Log.w(tag, "Duplicate onServicesDiscovered for $deviceType — closing extra gatt")
                gatt.disconnect()
                gatt.close()
                return
            }*/
            // ─────────────────────────────────────────────────────────────────

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(tag, "Services discovered")

                // Store the successful connection in a flag
                servicesDiscovered[deviceType] = true

                // ✅ QPP Hook 1 — initialize QPP after services found
                if (isQppDevice(deviceType)) {
                    val qppReady = qppManager.enable(gatt, deviceType)

                    Log.d(tag, "QPP enabled for $deviceType: $qppReady")
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    // Now enable your notifications or configure services
//                    configureDeviceServices(gatt, deviceType)
                    connectionCallback(Result.success(Unit))
                }, 300)

//                configureDeviceServices(gatt, deviceType)

//                enableNotifications(deviceType = deviceType, serviceUuid = UUID.fromString(BleConstants.THERMOMETER_SERVICE), characteristicUuid = UUID.fromString(BleConstants.THERMOMETER_MEASUREMENT))

//                connectionCallback(Result.success(Unit))

            } else {
                Log.e(tag, "Service discovery failed with status: $status")
                connectionCallback(Result.failure(Exception("Service discovery failed: $status")))
            }
        }

        private fun isQppDevice(deviceType: DeviceType): Boolean {
            return deviceType in listOf(
                DeviceType.GLUCOSE_METER
            )
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            // For pre-API 33 devices
            onCharacteristicRead(gatt, characteristic, characteristic.value, status)
        }

        // For API 33+
        override fun onCharacteristicRead(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int
        ) {
            val uuid = characteristic.uuid

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(tag, "Characteristic read successfully: $uuid")
                characteristicReadListener?.invoke(uuid, value, null)
            } else {
                Log.e(tag, "Characteristic read failed: $status")
                characteristicReadListener?.invoke(
                    uuid, ByteArray(0), Exception("Read failed: $status")
                )
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            val uuid = characteristic.uuid

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(tag, "Characteristic write successful: $uuid")
                characteristicWriteListener?.invoke(uuid, null)
            } else {
                Log.e(tag, "Characteristic write failed: $status")
                characteristicWriteListener?.invoke(uuid, Exception("Write failed: $status"))
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic
        ) {
            // For pre-API 33 devices
            onCharacteristicChanged(gatt, characteristic, characteristic.value)
        }

        // For API 33+
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray
        ) {
            val uuid = characteristic.uuid
            Log.d(tag, "Characteristic changed :: $uuid, value length: ${value.size}")

            Log.d(tag, "Notification data :: val :: $value   : :  ${characteristic.value}")

            // ✅ QPP Hook 3 — forward notification to QPP layer
            if (isQppDevice(deviceType)) {
                qppManager.updateNotification(gatt, characteristic)
                // QPP library will call onQppReceiveData in QppManager
                // No need to forward to notificationDataListeners — handled via qppDataFlow
//                return
            }

            // Debug log the data in hex format
            if (value.isNotEmpty()) {

                if (uuid == UUID.fromString(BleConstants.THERMOMETER_MEASUREMENT)) {
                    val data = characteristic.value
                    val a = java.lang.Byte.toUnsignedInt(data[2])
                    val b = java.lang.Byte.toUnsignedInt(data[3])
                    Log.e("temperatureDataLogs", " : a :$a  : b :$b")

                    val decimal = DecimalFormat("##.#")
                    var tem = ((a shl 8) + b) * 1.0 / 100
                    Log.e("temperatureDataLogs", " : Centigrade 1 :$tem")
                    tem = Math.round(tem * 10) * 1.0 / 10
                    Log.e("temperatureDataLogs", " : Centigrade :$tem")
                    val fahrenheit = tem * 9 / 5 + 32
                    Log.e("temperatureDataLogs", " : fahrenheit :$fahrenheit")
                } else if (uuid == UUID.fromString(BleConstants.PULSE_MEASUREMENT)) {

                    val data = characteristic.value

                    Log.e("BplOxyDatadata", "SPO2:$data")

                    if (data[0].toInt() == -128) {
                    } else if (data[0].toInt() == -127) {

                        val pulse = data[1].toInt() and 0xFF
                        val oxygen = data[2].toInt() and 0xFF
                        val pi = data[3].toInt() and 0xFF

//                        Log.e("BplOxyDataLogs", " : SPO2 : " + oxygen + "% :  Pulse : " + pulse + " : PI : " + pi.toDouble() * 10 / 100)

                        if (oxygen != 127) {
                            oxygen
                        }

                    }


                } else if (uuid == UUID.fromString(QppManager.UUID_QPP_CHAR_NOTIFY)) {

                    Log.d(tag, "Notification data :: Device Type ::  $deviceType  ${value}")

                    // Log raw packet
                    val hex = value.toHexString()
                    Log.d(tag, "══════════════════════════════════════")
                    Log.d(tag, "QPP RX [$deviceType] ${value.size} bytes")
                    Log.d(tag, "  Hex   : $hex")
                    Log.d(tag, "  ASCII : ${value.toAsciiString()}")
                    Log.d(tag, "══════════════════════════════════════")

                } else {

                    val hexString = value.joinToString(" ") { String.format("%02X", it) }
                    Log.d(tag, "Notification data :: Else :: $uuid  $hexString")
                }

            }

            // Notify listeners for this characteristic
            notificationDataListeners[uuid]?.invoke(value)
        }

        private fun ByteArray.toHexString(): String =
            joinToString(" ") { "%02X".format(it) }

        private fun ByteArray.toAsciiString(): String =
            toString(Charsets.ISO_8859_1)


        override fun onDescriptorWrite(
            gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int
        ) {
            val characteristicUuid = descriptor.characteristic.uuid

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(tag, "Descriptor write successful for: $characteristicUuid")

                // ✅ QPP Hook 2 — enable QPP notifications after descriptor write
                // ── QPP Hook 2 — only fire for the QPP notify characteristic ──
                if (isQppDevice(deviceType) && characteristicUuid.toString() == QppManager.UUID_QPP_CHAR_NOTIFY
                ) {
                    qppManager.enableNotify(gatt)  // now a safe no-op
                }

                if (descriptor.uuid.toString() == BleConstants.CLIENT_CHARACTERISTIC_CONFIG) {
                    val isNotificationEnabled = descriptor.value.contentEquals(
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    )
                    notificationListeners[characteristicUuid]?.invoke(
                        characteristicUuid, isNotificationEnabled
                    )
                }
            } else {
                Log.e(tag, "Descriptor write failed: $status")
                // Notify listeners of failure
                notificationListeners[characteristicUuid]?.invoke(
                    characteristicUuid, false
                )
            }
        }

        @SuppressLint("MissingPermission")
        private fun configureDeviceServices(gatt: BluetoothGatt, deviceType: DeviceType) {

            Log.d(tag, "Configuring device services for: $deviceType")


            // Device-specific configuration can be added here
            when (deviceType) {
                /*DeviceType.HEART_RATE_MONITOR -> {
                    // Configure heart rate monitoring services
                    val service = gatt.getService(UUID.fromString(BleConstants.HEART_RATE_SERVICE))

                    service?.let {
                        val characteristic = it.getCharacteristic(UUID.fromString(BleConstants.HEART_RATE_MEASUREMENT))

                        characteristic?.let { heartRateChar ->
                            gatt.setCharacteristicNotification(heartRateChar, true)

                            val descriptor = heartRateChar.getDescriptor(
                                UUID.fromString(BleConstants.CLIENT_CHARACTERISTIC_CONFIG)
                            )

                            descriptor?.let { desc ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    gatt.writeDescriptor(
                                        desc, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                    )
                                } else {
                                    desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                    gatt.writeDescriptor(desc)
                                }
                            }
                        }
                    }
                }*/

                DeviceType.BLOOD_PRESSURE_MONITOR -> {
                    // Configure blood pressure monitoring services
                    // Similar implementation as heart rate
                }

                DeviceType.HB_CHECK -> {
                    // Configure blood pressure monitoring services
                    // Similar implementation as heart rate
                }

                DeviceType.GLUCOSE_METER -> {
                    // Configure glucose meter services
                }

                DeviceType.HBA1C_METER -> {
                    // A1cEZ 2.0 pushes its record unsolicited once a test completes.
                    // Notifications are enabled on demand from the HbA1c screen, so
                    // there is nothing to configure at service-discovery time.
                }
                // Add other device types as needed
                DeviceType.HEIGHT -> {

                }
                // Add other device types as needed
                DeviceType.PULSE -> {

                }

                DeviceType.WEIGHING_SCALE -> {

                }

                DeviceType.THERMOMETER -> {

                    Log.e("confDevice", "  :  $deviceType    :  services  :  ${gatt.services}")

                    /*   enableNotifications(
                           deviceType = deviceType,
                           serviceUuid = UUID.fromString(BleConstants.THERMOMETER_SERVICE),
                           characteristicUuid = UUID.fromString(BleConstants.THERMOMETER_MEASUREMENT)
                       )*/

                    // Configure heart rate monitoring services
                    val service = gatt.getService(UUID.fromString(BleConstants.THERMOMETER_SERVICE))

                    Log.e("confDevice", "  : serv :  $service")

                    service?.let {
                        val characteristic = it.getCharacteristic(UUID.fromString(BleConstants.THERMOMETER_MEASUREMENT))

                        characteristic?.let { tempChar ->

                            readCharacteristic(
                                deviceType = deviceType, serviceUuid = service.uuid, characteristicUuid = tempChar.uuid
                            )

                            gatt.setCharacteristicNotification(tempChar, true)

                            val descriptor = tempChar.getDescriptor(UUID.fromString(BleConstants.THERMOMETER_MEASUREMENT))

                            descriptor?.let { desc ->

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    gatt.writeDescriptor(
                                        desc, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                    )
                                } else {

                                    desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

                                    gatt.writeDescriptor(desc)
                                }
                            }
                        }
                    }
                }

                DeviceType.BT_PRINTER -> {}

            }
        }
    }

    // ─── QPP Send (called from Repository) ──────────────────────────────────
    fun sendQppData(deviceType: DeviceType, data: ByteArray): Boolean {
        val gatt = gattConnections[deviceType] ?: return false
        if (!qppManager.isInitialized(deviceType)) return false

        // Split into 20-byte chunks automatically
        return data.toList().chunked(20).all { chunk ->
            qppManager.sendData(gatt, chunk.toByteArray())
        }
    }

    fun observeQppData(deviceType: DeviceType) = qppManager.qppDataFlow
        .let { flow ->
            kotlinx.coroutines.flow.flow {
                flow.collect { (type, data) ->
                    if (type == deviceType) emit(data)

                    println("received QppData Logs ${data}")
                }
            }
        }

    fun observeGlucoseReadings(deviceType: DeviceType) =
        qppManager.glucoseReadingFlow
            .let { flow ->
                kotlinx.coroutines.flow.flow {
                    flow.collect { (type, reading) ->
                        if (type == deviceType) emit(reading)
                    }


                }
            }

}