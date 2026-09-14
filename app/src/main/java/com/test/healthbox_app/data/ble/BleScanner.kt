package com.test.healthbox_app.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject


class BleScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "BleScanner"

    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy { bluetoothManager.adapter }
    private val bluetoothLeScanner by lazy { bluetoothAdapter?.bluetoothLeScanner }

    private var scanCallback: ScanCallback? = null
    private var isScanning = false

    // ✅ Hold a reference to the channel so stopBleScan() can close it
    private var scanChannel: ProducerScope<List<ScanResult>>? = null

    @SuppressLint("MissingPermission")
    fun scan(timeout: Long = 10000): Flow<List<ScanResult>> = callbackFlow {

        scanChannel = this

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e(tag, "Bluetooth not enabled")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val scanner = bluetoothLeScanner
        if (scanner == null) {
            Log.e(tag, "Bluetooth LE Scanner not available")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        Log.d(tag, "Starting BLE scan")

        val scanResults = CopyOnWriteArrayList<ScanResult>()

        scanCallback = object : ScanCallback() {
            // Android's very first advertisement packet from a device very often arrives
            // without a name (the name comes in a later packet/scan response), so most scans
            // start with one or more callbacks that add nothing. Previously trySend() ran
            // unconditionally here, so that first no-op callback emitted scanResults.toList()
            // while it was still empty — the UI read that as "scan finished, 0 devices" and
            // showed "No devices found" a split second after Scan was pressed, well before the
            // scan actually finished. Only emit when a named device was actually added/updated.
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                Log.i("scanCallBackDevice", "  : 59  : ${result.device.name}   :   ${result.device.address}   :   ${result.device.type}")
                if (result.device.name.isNullOrEmpty()) return

                val existingPosition = scanResults.indexOfFirst { it.device.address == result.device.address }
                if (existingPosition >= 0) {
                    scanResults[existingPosition] = result
                } else if (!scanResults.contains(result)) {
                    scanResults.add(result)
                } else {
                    return
                }
                trySend(scanResults.toList())
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                var changed = false
                for (result in results) {
                    Log.e("scanCallBackDevice", "  : 82 : ${result.device.name}   :   ${result.device.address}   :   ${result.device.type}")
                    if (result.device.name.isNullOrEmpty()) continue

                    val existingPosition = scanResults.indexOfFirst { it.device.address == result.device.address }
                    if (existingPosition >= 0) {
                        scanResults[existingPosition] = result
                        changed = true
                    } else if (!scanResults.contains(result)) {
                        scanResults.add(result)
                        changed = true
                    }
                }
                if (changed) trySend(scanResults.toList())
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(tag, "Scan failed with error: $errorCode")
                trySend(emptyList())
                close()
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(emptyList(), settings, scanCallback)

        // ✅ Auto-stop after timeout by launching a sibling coroutine
        // This does NOT block awaitClose like delay() did before
        launch {
            delay(timeout)
            Log.d(tag, "Scan timeout reached, closing channel")
            // Emit the definitive final result before closing — including an empty list when
            // nothing was ever found — so a genuine "scanned the full duration, no devices"
            // outcome is actually reported instead of leaving the collector (and its progress
            // dialog) waiting forever with no final emission.
            trySend(scanResults.toList())
            close() // ← triggers awaitClose below
        }

        awaitClose {
            Log.d(tag, "Stopping BLE scan")
            try {
                scanner.stopScan(scanCallback)
            } catch (e: Exception) {
                Log.e(tag, "Error stopping scan: ${e.message}")
            } finally {
                isScanning = false
                scanCallback = null
                scanChannel = null
            }
        }
    }

    fun stopBleScan() {
        Log.d(tag, "stopBleScan() called manually")
        // ✅ Closing the channel triggers awaitClose which stops the hardware scan
        scanChannel?.close()
    }
}


/*
class BleScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "BleScanner"

    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy { bluetoothManager.adapter }
    private val bluetoothLeScanner by lazy { bluetoothAdapter?.bluetoothLeScanner }

    private var scanCallback: ScanCallback? = null
    private var isScanning = false

    */
/**
 * Scan for BLE devices
 * @param timeout Scan duration in milliseconds
 * @return Flow emitting scan results
 *//*

    @SuppressLint("MissingPermission")
    fun scan(timeout: Long = 10000): Flow<List<ScanResult>> = callbackFlow {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e(tag, "Bluetooth not enabled")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val scanner = bluetoothLeScanner
        if (scanner == null) {
            Log.e(tag, "Bluetooth LE Scanner not available")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        Log.d(tag, "Starting BLE scan")

        val scanResults = CopyOnWriteArrayList<ScanResult>()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {

                Log.i("scanCallBackDevice", "  : 59  : ${result.device.name}   :   ${result.device.address}   :   ${result.device.type}")

                // Check if device is already in the list

                val existingPosition = scanResults.indexOfFirst { it.device.address == result.device.address }

                if (existingPosition >= 0 && !result.device.name.isNullOrEmpty()) {

                    Log.i("scanCallBackDevice", "  : 62  : ${result.device.name}   :   ${result.device.address}   :   ${result.device.type}")

                    // Update existing result
                    scanResults[existingPosition] = result
                } else {
                    Log.e("scanCallBackDevice", "  : 67 : ${result.device.name}   :   ${result.device.address}   :   ${result.device.type}")
                    // Add new result
                    if (!scanResults.contains(result) && !result.device.name.isNullOrEmpty())
                        scanResults.add(result)
                }

                // Send updated list
                trySend(scanResults.toList())
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                for (result in results) {
                    val existingPosition = scanResults.indexOfFirst { it.device.address == result.device.address }

                    Log.e("scanCallBackDevice", "  : 82 : ${result.device.name}   :   ${result.device.address}   :   ${result.device.type}")

                    if (existingPosition >= 0 && !result.device.name.isNullOrEmpty()) {
                        scanResults[existingPosition] = result
                    } else {
                        if (!scanResults.contains(result))
                            scanResults.add(result)
                    }
                }

                trySend(scanResults.toList())
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(tag, "Scan failed with error: $errorCode")
                trySend(emptyList())
                close()
            }
        }

        // Configure scan settings
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        // Optional: Set scan filters
        val filters = mutableListOf<ScanFilter>()
        // Add filters if needed - leave empty for all devices

        // Start scanning
        scanner.startScan(filters, settings, scanCallback)

        // Set a timer to stop scanning after timeout
        kotlinx.coroutines.delay(timeout)
        scanner.stopScan(scanCallback)
        trySend(scanResults.toList())
        close()

        // Clean up when flow is canceled
        awaitClose {
            Log.d(tag, "Stopping BLE scan")
            scanner.stopScan(scanCallback)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBleScan() {
        Log.d(tag, "stopScan() called manually")
        stopScanInternal()
    }

    @SuppressLint("MissingPermission")
    private fun stopScanInternal() {
        if (!isScanning) return
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            Log.d(tag, "BLE scan stopped")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping scan: ${e.message}")
        } finally {
            isScanning = false
            scanCallback = null
        }
    }

}*/
