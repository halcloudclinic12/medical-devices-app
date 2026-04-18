package com.test.healthbox_app.data.repository

import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.test.healthbox_app.R
import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.BluetoothCommand
import com.test.healthbox_app.domain.model.ConnectionState
import com.test.healthbox_app.domain.model.PrintJob
import com.test.healthbox_app.domain.model.PrintResult
import com.test.healthbox_app.domain.model.PrinterError
import com.test.healthbox_app.domain.repository.BluetoothRepository
import com.prowess.sdk.Printer
import com.prowess.sdk.Setup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okio.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothRepositoryImpl @Inject constructor(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) : BluetoothRepository {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected("Device not available"))
    private val _discoveredDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    private var bluetoothSocket: BluetoothSocket? = null
    private var connectedDevice: BluetoothDevice? = null
    private var printer: Printer? = null

    private var setupInstance: Setup? = null

    private val DEVICE_NOTCONNECTED: Int = -100

    private val TAG = "BluetoothRepositoryImpl"


    /** Input stream object  */
    private var misIn: InputStream? = null

    /** Output stream object  */
    private var mosOut: OutputStream? = null

    private var iRetVal = 0

    private val deviceReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.e("deviceIntActionLog", "  :  " + intent?.action)

            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (hasBluetoothPermission() && it.name != null) {

                            try {
                                val deviceInfo = BleDevice(
                                    name = it.name ?: "Unknown Device",
                                    address = it.address,
                                    bondState = it.bondState,
                                    deviceType = DeviceType.BT_PRINTER,
                                    rssi = 0,
                                    scanRecord = ByteArray(0)
                                )

                                val currentDevices = _discoveredDevices.value.toMutableList()

                                if (!currentDevices.any { existing -> existing.address == deviceInfo.address }) {
                                    currentDevices.add(deviceInfo)
                                    _discoveredDevices.value = currentDevices
                                }

                            } catch (e: Exception) {
                                // Handle exception
                                e.printStackTrace()
                            }
                        }
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    // Discovery finished
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    // Handle pairing state changes
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun startScan(): Flow<List<BleDevice>> = callbackFlow {
        if (!isBluetoothEnabled() || !hasBluetoothPermission()) {
            trySend(emptyList())
            awaitClose()
            return@callbackFlow
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(deviceReceiver, filter)

        _discoveredDevices.value = emptyList()
        bluetoothAdapter?.startDiscovery()

        _discoveredDevices.collect { devices ->
            trySend(devices)
        }

        awaitClose {
            try {
                bluetoothAdapter?.cancelDiscovery()
                context.unregisterReceiver(deviceReceiver)
            } catch (e: Exception) {
                // Receiver already unregistered
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopScan() {
        if (hasBluetoothPermission()) {
            bluetoothAdapter?.cancelDiscovery()
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun pairDevice(device: BleDevice): Flow<Boolean> = callbackFlow {
        if (!hasBluetoothPermission()) {
            trySend(false)
            awaitClose()
            return@callbackFlow
        }

        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)

        val pairingReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                    val deviceFromIntent = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                    if (deviceFromIntent?.address == device.address) {
                        when (bondState) {
                            BluetoothDevice.BOND_BONDED -> trySend(true)
                            BluetoothDevice.BOND_NONE -> trySend(false)
                        }
                    }
                }
            }
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(pairingReceiver, filter)

        val success = bluetoothDevice?.createBond() ?: false
        if (!success) {
            trySend(false)
        }

        awaitClose {
            try {
                context.unregisterReceiver(pairingReceiver)
            } catch (e: Exception) {
                // Receiver already unregistered
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connectDevice(device: BleDevice): Flow<ConnectionState> = callbackFlow {
        if (!hasBluetoothPermission()) {
            trySend(ConnectionState.Error("Bluetooth permission not granted"))
            awaitClose()
            return@callbackFlow
        }

        setupInstance = Setup()

        val activate: Boolean = setupInstance!!.blActivateLibrary(context, R.raw.licence)

        Log.e("activatedSetup", "$activate")

        if (activate == true) {
            Log.d(TAG, "Identi5 Library Activated......")
        } else if (activate == false) {
            Log.d(TAG, "Identi5 Library Not Activated...")
        }

        _connectionState.value = ConnectionState.Connecting
        trySend(ConnectionState.Connecting)

        try {
            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID

            bluetoothSocket = bluetoothDevice?.createRfcommSocketToServiceRecord(uuid)
            bluetoothAdapter?.cancelDiscovery()

            bluetoothSocket?.connect()

            delay(300)

            mosOut = bluetoothSocket?.outputStream //Get global output stream object
            misIn = bluetoothSocket?.inputStream

            connectedDevice = bluetoothDevice

            printer = Printer(setupInstance, mosOut, misIn)

            _connectionState.value = ConnectionState.Connected
            trySend(ConnectionState.Connected)

        } catch (e: IOException) {
            _connectionState.value = ConnectionState.Error("Connection failed: ${e.message}")
            trySend(ConnectionState.Error("Connection failed: ${e.message}"))
            bluetoothSocket?.close()
            bluetoothSocket = null
        }

        awaitClose {
            // Connection flow closed
        }
    }

    override suspend fun disconnectDevice() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
            connectedDevice = null
            _connectionState.value = ConnectionState.Disconnected("Disconnected Device")
        } catch (e: IOException) {
            _connectionState.value = ConnectionState.Error("Disconnect failed: ${e.message}")
        }
    }

    override suspend fun sendCommand(command: BluetoothCommand): Flow<ByteArray> = callbackFlow {
        if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
            trySend(ByteArray(0))
            awaitClose()
            return@callbackFlow
        }

        try {
            val outputStream = bluetoothSocket?.outputStream
            val inputStream = bluetoothSocket?.inputStream

            var setupInstance: Setup? = null

            val printer: Printer = Printer(setupInstance, outputStream, inputStream)

            iRetVal = printer.iStartPrinting(1);

            // Send command
            val dataToSend = command.data ?: command.command.toByteArray()
            outputStream?.write(dataToSend)
            outputStream?.flush()

            // Read response
            val buffer = ByteArray(1024)
            val bytesRead = inputStream?.read(buffer) ?: 0
            val response = buffer.copyOf(bytesRead)

            trySend(response)

        } catch (e: IOException) {
            _connectionState.value = ConnectionState.Error("Communication failed: ${e.message}")
            trySend(ByteArray(0))
        }

        awaitClose {
            // Command flow closed
        }
    }

    override fun getConnectionState(): Flow<ConnectionState> = _connectionState.asStateFlow()

    override suspend fun printText(printJob: PrintJob): Result<PrintResult> {

        return try {
            val result = executePrint(printJob)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }

        /* return try {
 //            val result = printerDataSource.executePrint(printJob)


             val outputStream = bluetoothSocket?.outputStream
             val inputStream = bluetoothSocket?.inputStream

             var setupInstance: Setup? = null

             val printer: Printer = Printer(setupInstance, outputStream, inputStream)

             printer.iFlushBuf()
             printer.iPrinterAddData(printJob.fontType, printJob.message)

             val resultCode = printer.iStartPrinting(1)

             when (resultCode) {
                 Printer.PR_SUCCESS -> PrintResult(
                     code = resultCode,
                     message = "Printing Successful",
                     isSuccess = true
                 )

                 else -> throw mapErrorCodeToException(resultCode)
             }

             Result.success(result)


         } catch (e: Exception) {
             Result.failure(e)
         }*/
    }

    override suspend fun isDeviceConnected(): Boolean {
        return true
    }


    override fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    override suspend fun enableBluetooth(): Boolean {
        return if (hasBluetoothPermission()) {
            bluetoothAdapter?.enable() == true
        } else {
            false
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context, BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, BLUETOOTH) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context, BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private suspend fun executePrint(printJob: PrintJob): PrintResult = withContext(Dispatchers.IO) {
        try {

            /*val outputStream = bluetoothSocket?.outputStream
            val inputStream = bluetoothSocket?.inputStream

            var setupInstance: Setup? = null

            val printer: Printer = Printer(setupInstance, outputStream, inputStream)*/

            Log.e("ptrSerialNumber", "    :   ${printer?.sGetSerialNumber()}")

            printer?.iFlushBuf()
            printer?.iPrinterAddData(printJob.fontType, printJob.message)

            val resultCode = printer?.iStartPrinting(1)

            when (resultCode) {
                Printer.PR_SUCCESS -> PrintResult(
                    code = resultCode, message = "Printing Successful", isSuccess = true
                )

                else -> throw mapErrorCodeToException(resultCode!!)
            }
        } catch (e: Exception) {
            if (e is PrinterError) throw e
            throw PrinterError.DeviceNotConnected
        }
    }

    private fun mapErrorCodeToException(code: Int): PrinterError {
        return when (code) {
            DEVICE_NOTCONNECTED, -1 -> PrinterError.DeviceNotConnected
            Printer.PR_PLATEN_OPEN -> PrinterError.PlatenOpen
            Printer.PR_PAPER_OUT -> PrinterError.PaperOut
            Printer.PR_IMPROPER_VOLTAGE -> PrinterError.ImproperVoltage
            Printer.PR_FAIL -> PrinterError.PrintFailure
            Printer.PR_PARAM_ERROR -> PrinterError.ParameterError
            Printer.PR_NO_RESPONSE -> PrinterError.NoResponse
            Printer.PR_DEMO_VERSION -> PrinterError.DemoVersion
            Printer.PR_INVALID_DEVICE_ID -> PrinterError.InvalidDeviceId
            Printer.PR_INACTIVE_PERIPHERAL -> PrinterError.NotActivated
            Printer.PR_CHARACTER_NOT_SUPPORTED -> PrinterError.NotSupported
            else -> PrinterError.UnknownError(code)
        }
    }


}