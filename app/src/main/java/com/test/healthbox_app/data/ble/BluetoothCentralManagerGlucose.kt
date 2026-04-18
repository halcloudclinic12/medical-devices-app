/*
package com.kiosk.healthbox_app.data.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.kiosk.healthbox_app.data.ble.Glucose.Logger
import com.kiosk.healthbox_app.data.ble.Glucose.BluetoothCentralManagerCallback
import com.kiosk.healthbox_app.data.ble.Glucose.BluetoothPeripheral
import com.kiosk.healthbox_app.data.ble.Glucose.BluetoothPeripheral.InternalCallback
import com.kiosk.healthbox_app.data.ble.Glucose.BluetoothPeripheralCallback
import com.kiosk.healthbox_app.data.model.ScanFailure
import com.kiosk.healthbox_app.data.model.Transport
import java.util.Objects
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BluetoothCentralManagerGlucose {


    val TAG: String = BluetoothCentralManagerGlucose::class.java.getSimpleName()

    val SCAN_TIMEOUT: Long = 180000L

    val SCAN_RESTART_DELAY: Int = 1000

    val MAX_CONNECTION_RETRIES: Int = 1

    val DEFAULT_TRANSPORT: Transport = Transport.LE


    val NO_PERIPHERAL_ADDRESS_PROVIDED: String = "no peripheral address provided"

    val NO_VALID_PERIPHERAL_PROVIDED: String = "no valid peripheral provided"

    val NO_VALID_PERIPHERAL_CALLBACK_SPECIFIED: String = "no valid peripheral callback specified"

    val CANNOT_CONNECT_TO_PERIPHERAL_BECAUSE_BLUETOOTH_IS_OFF: String = "cannot connect to peripheral because Bluetooth is off"

    private val context: Context? = null
    private val callBackHandler: Handler? = null
    private val bluetoothAdapter: BluetoothAdapter? = null

    @Volatile
    private var bluetoothScanner: BluetoothLeScanner? = null

    @Volatile
    private var autoConnectScanner: BluetoothLeScanner? = null
    private val bluetoothCentralManagerCallback: BluetoothCentralManagerCallback? = null
    protected val connectedPeripherals: MutableMap<String?, BluetoothPeripheral> = ConcurrentHashMap<String?, BluetoothPeripheral>()
    protected val unconnectedPeripherals: MutableMap<String?, BluetoothPeripheral> = ConcurrentHashMap<String?, BluetoothPeripheral>()
    private val scannedPeripherals: MutableMap<String?, BluetoothPeripheral?> = ConcurrentHashMap<String?, BluetoothPeripheral?>()
    private val reconnectPeripheralAddresses: MutableList<String?> = ArrayList<String?>()
    private val reconnectCallbacks: MutableMap<String?, BluetoothPeripheralCallback?> = ConcurrentHashMap<String?, BluetoothPeripheralCallback?>()
    private var scanPeripheralNames = arrayOfNulls<String>(0)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var autoConnectRunnable: Runnable? = null
    private val connectLock = Any()
    private val scanLock = Any()

    @Volatile
    private var currentCallback: ScanCallback? = null
    private var currentFilters: MutableList<ScanFilter?>? = null
    private var scanSettings: ScanSettings? = null
    private val autoConnectScanSettings: ScanSettings? = null
    private val connectionRetries: MutableMap<String?, Int?> = ConcurrentHashMap<String?, Int?>()
    private val pinCodes: MutableMap<String?, String?> = ConcurrentHashMap<String?, String?>()
    private var transport: Transport = DEFAULT_TRANSPORT


    //region Callbacks
    private val scanByNameCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            synchronized(this) {
                val deviceName = result.getDevice().getName()
                if (deviceName == null) return
                for (name in scanPeripheralNames) {
                    if (deviceName.contains(name!!)) {
                        sendScanResult(result)
                        return
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            stopScan()
            sendScanFailed(ScanFailure.fromValue(errorCode))
        }
    }

    private val defaultScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            synchronized(this) {
                sendScanResult(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            stopScan()
            sendScanFailed(ScanFailure.fromValue(errorCode))
        }
    }

    private fun sendScanResult(result: ScanResult) {
        callBackHandler!!.post(object : Runnable {
            override fun run() {
                if (isScanning()) {
                    val peripheral: BluetoothPeripheral = getPeripheral(result.getDevice().getAddress())
                    peripheral.setDevice(result.getDevice())
                    bluetoothCentralManagerCallback?.onDiscoveredPeripheral(peripheral, result)
                }
            }
        })
    }

    private fun sendScanFailed(scanFailure: ScanFailure) {
        currentCallback = null
        currentFilters = null
        callBackHandler!!.post(object : Runnable {
            override fun run() {
                Log.e(TAG, "scan failed with error code %d (%s)" + scanFailure.value + "   :  " + scanFailure)
                Log.e(TAG, "scan failed with error code %d (%s)" + scanFailure.value + "   :  " + scanFailure)
                bluetoothCentralManagerCallback.onScanFailed(scanFailure)
            }
        })
    }

    private val autoConnectScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            synchronized(this) {
                if (!isAutoScanning()) return
                Logger.d(TAG, "peripheral with address '%s' found", result.getDevice().getAddress())
                stopAutoconnectScan()

                val deviceAddress = result.getDevice().getAddress()
                val peripheral: BluetoothPeripheral? = unconnectedPeripherals.get(deviceAddress)
                val callback: BluetoothPeripheralCallback? = reconnectCallbacks.get(deviceAddress)

                reconnectPeripheralAddresses.remove(deviceAddress)
                reconnectCallbacks.remove(deviceAddress)
                removePeripheralFromCaches(deviceAddress)

                if (peripheral != null && callback != null) {
                    connectPeripheral(peripheral, callback)
                }
                if (reconnectPeripheralAddresses.size > 0) {
                    scanForAutoConnectPeripherals()
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            val scanFailure: ScanFailure? = ScanFailure.fromValue(errorCode)
            Logger.e(TAG, "autoConnect scan failed with error code %d (%s)", errorCode, scanFailure)
            stopAutoconnectScan()
            callBackHandler!!.post(object : Runnable {
                override fun run() {
                    bluetoothCentralManagerCallback.onScanFailed(scanFailure)
                }
            })
        }
    }

    protected val internalCallback: BluetoothPeripheral.InternalCallback = object : InternalCallback() {
        public override fun connecting(peripheral: BluetoothPeripheral) {
            callBackHandler!!.post(object : Runnable {
                override fun run() {
                    bluetoothCentralManagerCallback.onConnectingPeripheral(peripheral)
                }
            })
        }

        public override fun connected(peripheral: BluetoothPeripheral) {
            val peripheralAddress: String? = peripheral.getAddress()
            removePeripheralFromCaches(peripheralAddress)
            connectedPeripherals.put(peripheralAddress, peripheral)

            callBackHandler!!.post(object : Runnable {
                override fun run() {
                    bluetoothCentralManagerCallback.onConnectedPeripheral(peripheral)
                }
            })
        }

        public override fun connectFailed(peripheral: BluetoothPeripheral, status: HciStatus) {
            val peripheralAddress: String? = peripheral.getAddress()

            // Get the number of retries for this peripheral
            var nrRetries = 0
            val retries = connectionRetries.get(peripheralAddress)
            if (retries != null) nrRetries = retries

            removePeripheralFromCaches(peripheralAddress)

            // Retry connection or conclude the connection has failed
            if (nrRetries < MAX_CONNECTION_RETRIES && status !== HciStatus.CONNECTION_FAILED_ESTABLISHMENT) {
                Logger.i(TAG, "retrying connection to '%s' (%s)", peripheral.getName(), peripheralAddress)
                nrRetries++
                connectionRetries.put(peripheralAddress, nrRetries)
                unconnectedPeripherals.put(peripheralAddress, peripheral)
                peripheral.connect()
            } else {
                Logger.i(TAG, "connection to '%s' (%s) failed", peripheral.getName(), peripheralAddress)
                callBackHandler!!.post(object : Runnable {
                    override fun run() {
                        bluetoothCentralManagerCallback.onConnectionFailed(peripheral, status)
                    }
                })
            }
        }

        public override fun disconnecting(peripheral: BluetoothPeripheral) {
            callBackHandler!!.post(object : Runnable {
                override fun run() {
                    bluetoothCentralManagerCallback.onDisconnectingPeripheral(peripheral)
                }
            })
        }

        public override fun disconnected(peripheral: BluetoothPeripheral, status: HciStatus) {
            removePeripheralFromCaches(peripheral.getAddress())
            callBackHandler!!.post(object : Runnable {
                override fun run() {
                    bluetoothCentralManagerCallback.onDisconnectedPeripheral(peripheral, status)
                }
            })
        }

        public override fun getPincode(peripheral: BluetoothPeripheral): String? {
            return pinCodes.get(peripheral.getAddress())
        }
    }

    private fun removePeripheralFromCaches(peripheralAddress: String?) {
        connectedPeripherals.remove(peripheralAddress)
        unconnectedPeripherals.remove(peripheralAddress)
        scannedPeripherals.remove(peripheralAddress)
        connectionRetries.remove(peripheralAddress)
    }


    //endregion
    */
/**
     * Construct a new BluetoothCentralManager object
     *
     * @param context                  Android application environment.
     * @param bluetoothCentralManagerCallback the callback to call for updates
     * @param handler                  Handler to use for callbacks.
     *//*

    fun BluetoothCentralManager(context: Context, bluetoothCentralManagerCallback: BluetoothCentralManagerCallback, handler: Handler) {
        this.context = Objects.requireNonNull<Context>(context, "no valid context provided")
        this.bluetoothCentralManagerCallback =
            Objects.requireNonNull<BluetoothCentralManagerCallback?>(bluetoothCentralManagerCallback, "no valid bluetoothCallback provided")
        this.callBackHandler = Objects.requireNonNull<Handler>(handler, "no valid handler provided")
        val manager = Objects.requireNonNull<BluetoothManager?>(
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager,
            "cannot get BluetoothManager"
        )
        this.bluetoothAdapter = Objects.requireNonNull<BluetoothAdapter?>(manager.getAdapter(), "no bluetooth adapter found")
        this.autoConnectScanSettings = getScanSettings(ScanMode.LOW_POWER)
        this.scanSettings = getScanSettings(ScanMode.LOW_LATENCY)

        // Register for broadcasts on BluetoothAdapter state change
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(adapterStateReceiver, filter)
    }

    */
/**
     * Closes BluetoothCentralManager and cleans up internals. BluetoothCentralManager will not work anymore after this is called.
     *//*

    fun close() {
        scannedPeripherals.clear()
        unconnectedPeripherals.clear()
        connectedPeripherals.clear()
        reconnectCallbacks.clear()
        reconnectPeripheralAddresses.clear()
        connectionRetries.clear()
        pinCodes.clear()
        context!!.unregisterReceiver(adapterStateReceiver)
    }

    */
/**
     * Enable logging
     *//*

    fun enableLogging() {
        Logger.enabled = true
    }

    */
/**
     * Disable logging
     *//*

    fun disableLogging() {
        Logger.enabled = false
    }

    private fun getScanSettings(scanMode: ScanMode): ScanSettings {
        Objects.requireNonNull<Any?>(scanMode, "scanMode is null")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ScanSettings.Builder()
                .setScanMode(scanMode.value)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .setReportDelay(0L)
                .build()
        } else {
            return ScanSettings.Builder()
                .setScanMode(scanMode.value)
                .setReportDelay(0L)
                .build()
        }
    }

    */
/**
     * Set the default scanMode.
     *
     * @param scanMode the scanMode to set
     *//*

    fun setScanMode(scanMode: ScanMode) {
        Objects.requireNonNull<Any?>(scanMode)

        scanSettings = getScanSettings(scanMode)
    }

    */
/**
     * Get the transport to be used during connection phase.
     *
     * @return transport
     *//*

    fun getTransport(): Transport {
        return transport
    }

    */
/**
     * Set the transport to be used when creating instances of [BluetoothPeripheral].
     *
     * @param transport the Transport to set
     *//*

    fun setTransport(transport: Transport) {
        this.transport = Objects.requireNonNull<Transport?>(transport, "not a valid transport")
    }

    private fun startScan(filters: MutableList<ScanFilter?>, scanSettings: ScanSettings, scanCallback: ScanCallback) {
        if (bleNotReady()) return

        if (isScanning()) {
            Logger.e(TAG, "other scan still active, stopping scan")
            stopScan()
        }

        if (bluetoothScanner == null) {
            bluetoothScanner = bluetoothAdapter!!.getBluetoothLeScanner()
        }

        if (bluetoothScanner != null) {
            setScanTimer()
            currentCallback = scanCallback
            currentFilters = filters
            bluetoothScanner!!.startScan(filters, scanSettings, scanCallback)
            Logger.i(TAG, "scan started")
        } else {
            Logger.e(TAG, "starting scan failed")
        }
    }

    */
/**
     * Scan for peripherals that advertise at least one of the specified service UUIDs.
     *
     * @param serviceUUIDs an array of service UUIDs
     *//*

    fun scanForPeripheralsWithServices(serviceUUIDs: Array<UUID?>?) {
        Objects.requireNonNull<Array<UUID?>?>(serviceUUIDs, "no service UUIDs supplied")

        require(serviceUUIDs!!.size != 0) { "at least one service UUID  must be supplied" }

        val filters: MutableList<ScanFilter?> = ArrayList<ScanFilter?>()
        for (serviceUUID in serviceUUIDs) {
            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(serviceUUID))
                .build()
            filters.add(filter)
        }

        startScan(filters, scanSettings!!, defaultScanCallback)
    }

    */
/**
     * Scan for peripherals with advertisement names containing any of the specified peripheral names.
     *
     *
     * Substring matching is used so only a partial peripheral names has to be supplied.
     *
     * @param peripheralNames array of partial peripheral names
     *//*

    fun scanForPeripheralsWithNames(peripheralNames: Array<String?>) {
        Objects.requireNonNull<Array<String?>?>(peripheralNames, "no peripheral names supplied")

        require(peripheralNames.size != 0) { "at least one peripheral name must be supplied" }

        // Start the scanner with no filter because we'll do the filtering ourselves
        scanPeripheralNames = peripheralNames
        startScan(mutableListOf<ScanFilter?>(), scanSettings!!, scanByNameCallback)
    }

    */
/**
     * Scan for peripherals that have any of the specified peripheral mac addresses.
     *
     * @param peripheralAddresses array of peripheral mac addresses to scan for
     *//*

    fun scanForPeripheralsWithAddresses(peripheralAddresses: Array<String?>?) {
        Objects.requireNonNull<Array<String?>?>(peripheralAddresses, "no peripheral addresses supplied")

        require(peripheralAddresses!!.size != 0) { "at least one peripheral address must be supplied" }

        val filters: MutableList<ScanFilter?> = ArrayList<ScanFilter?>()
        for (address in peripheralAddresses) {
            if (BluetoothAdapter.checkBluetoothAddress(address)) {
                val filter = ScanFilter.Builder()
                    .setDeviceAddress(address)
                    .build()
                filters.add(filter)
            } else {
                Logger.e(TAG, "%s is not a valid address. Make sure all alphabetic characters are uppercase.", address)
            }
        }

        startScan(filters, scanSettings!!, defaultScanCallback)
    }

    */
/**
     * Scan for any peripheral that matches the supplied filters
     *
     * @param filters A list of ScanFilters
     *//*

    fun scanForPeripheralsUsingFilters(filters: MutableList<ScanFilter?>) {
        Objects.requireNonNull<MutableList<ScanFilter?>>(filters, "no filters supplied")

        require(!filters.isEmpty()) { "at least one scan filter must be supplied" }

        startScan(filters, scanSettings!!, defaultScanCallback)
    }

    */
/**
     * Scan for any peripheral that is advertising.
     *//*

    fun scanForPeripherals() {
        startScan(mutableListOf<ScanFilter?>(), scanSettings!!, defaultScanCallback)
    }

    */
/**
     * Scan for peripherals that need to be autoconnected but are not cached
     *//*

    private fun scanForAutoConnectPeripherals() {
        if (bleNotReady()) return

        if (autoConnectScanner != null) {
            stopAutoconnectScan()
        }

        autoConnectScanner = bluetoothAdapter!!.getBluetoothLeScanner()
        if (autoConnectScanner != null) {
            val filters: MutableList<ScanFilter?> = ArrayList<ScanFilter?>()
            for (address in reconnectPeripheralAddresses) {
                val filter = ScanFilter.Builder()
                    .setDeviceAddress(address)
                    .build()
                filters.add(filter)
            }

            autoConnectScanner!!.startScan(filters, autoConnectScanSettings, autoConnectScanCallback)
            println("Mahesh->startedScanning")
            Logger.d(TAG, "started scanning to autoconnect peripherals (" + reconnectPeripheralAddresses.size + ")")
            setAutoConnectTimer()
        } else {
            Logger.e(TAG, "starting autoconnect scan failed")
        }
    }

    private fun stopAutoconnectScan() {
        cancelAutoConnectTimer()
        if (autoConnectScanner != null) {
            try {
                autoConnectScanner!!.stopScan(autoConnectScanCallback)
            } catch (ignore: Exception) {
            }
            autoConnectScanner = null
            Logger.i(TAG, "autoscan stopped")
        }
    }

    private fun isAutoScanning(): Boolean {
        return autoConnectScanner != null
    }

    */
/**
     * Stop scanning for peripherals.
     *//*

    fun stopScan() {
        synchronized(scanLock) {
            cancelTimeoutTimer()
            if (isScanning()) {
                // Note that we can't call stopScan if the adapter is off
                // On some phones like the Nokia 8, the adapter will be already off at this point
                // So add a try/catch to handle any exceptions
                try {
                    if (bluetoothScanner != null) {
                        bluetoothScanner!!.stopScan(currentCallback)
                        currentCallback = null
                        currentFilters = null
                        Logger.i(TAG, "scan stopped")
                    }
                } catch (ignore: Exception) {
                    Logger.e(TAG, "caught exception in stopScan")
                }
            } else {
                Logger.i(TAG, "no scan to stop because no scan is running")
            }

            bluetoothScanner = null
            scannedPeripherals.clear()
        }
    }

    */
/**
     * Check if a scanning is active
     *
     * @return true if a scan is active, otherwise false
     *//*

    fun isScanning(): Boolean {
        return (bluetoothScanner != null && currentCallback != null)
    }

    */
/**
     * Connect to a known peripheral immediately. The peripheral must have been found by scanning for this call to succeed. This method will time out in max 30 seconds on most phones and in 5 seconds on Samsung phones.
     * If the peripheral is already connected, no connection attempt will be made. This method is asynchronous and there can be only one outstanding connect.
     *
     * @param peripheral BLE peripheral to connect with
     *//*

    fun connectPeripheral(peripheral: BluetoothPeripheral, peripheralCallback: BluetoothPeripheralCallback) {
        synchronized(connectLock) {
            Objects.requireNonNull<Any?>(peripheral, NO_VALID_PERIPHERAL_PROVIDED)
            Objects.requireNonNull<Any?>(peripheralCallback, NO_VALID_PERIPHERAL_CALLBACK_SPECIFIED)

            if (connectedPeripherals.containsKey(peripheral.getAddress())) {
                Logger.w(TAG, "already connected to %s'", peripheral.getAddress())
                return
            }

            if (unconnectedPeripherals.containsKey(peripheral.getAddress())) {
                Logger.w(TAG, "already connecting to %s'", peripheral.getAddress())
                return
            }

            if (!bluetoothAdapter!!.isEnabled()) {
                Logger.e(TAG, CANNOT_CONNECT_TO_PERIPHERAL_BECAUSE_BLUETOOTH_IS_OFF)
                return
            }

            // Check if the peripheral is cached or not. If not, issue a warning because connection may fail
            // This is because Android will guess the address type and when incorrect it will fail
            if (peripheral.isUncached()) {
                Logger.w(TAG, "peripheral with address '%s' is not in the Bluetooth cache, hence connection may fail", peripheral.getAddress())
            }

            peripheral.setPeripheralCallback(peripheralCallback)
            scannedPeripherals.remove(peripheral.getAddress())
            unconnectedPeripherals.put(peripheral.getAddress(), peripheral)
            peripheral.connect()
        }
    }

    */
/**
     * Connect to a known peripheral and bond immediately. The peripheral must have been found by scanning for this call to succeed. This method will time out in max 30 seconds on most phones and in 5 seconds on Samsung phones.
     * If the peripheral is already connected, no connection attempt will be made. This method is asynchronous and there can be only one outstanding connect.
     *
     * @param peripheral BLE peripheral to connect with
     *//*

    fun createBond(peripheral: BluetoothPeripheral, peripheralCallback: BluetoothPeripheralCallback) {
        synchronized(connectLock) {
            Objects.requireNonNull<Any?>(peripheral, NO_VALID_PERIPHERAL_PROVIDED)
            Objects.requireNonNull<Any?>(peripheralCallback, NO_VALID_PERIPHERAL_CALLBACK_SPECIFIED)

            if (connectedPeripherals.containsKey(peripheral.getAddress())) {
                Logger.w(TAG, "already connected to %s'", peripheral.getAddress())
                return
            }

            if (unconnectedPeripherals.containsKey(peripheral.getAddress())) {
                Logger.w(TAG, "already connecting to %s'", peripheral.getAddress())
                return
            }

            if (!bluetoothAdapter!!.isEnabled()) {
                Logger.e(TAG, CANNOT_CONNECT_TO_PERIPHERAL_BECAUSE_BLUETOOTH_IS_OFF)
                return
            }

            // Check if the peripheral is cached or not. If not, issue a warning because connection may fail
            // This is because Android will guess the address type and when incorrect it will fail
            if (peripheral.isUncached()) {
                Logger.w(TAG, "peripheral with address '%s' is not in the Bluetooth cache, hence connection may fail", peripheral.getAddress())
            }

            peripheral.setPeripheralCallback(peripheralCallback)
            peripheral.createBond()
        }
    }

    */
/**
     * Automatically connect to a peripheral when it is advertising. It is not necessary to scan for the peripheral first. This call is asynchronous and will not time out.
     *
     * @param peripheral the peripheral
     *//*

    fun autoConnectPeripheral(peripheral: BluetoothPeripheral, peripheralCallback: BluetoothPeripheralCallback) {
        synchronized(connectLock) {
            Objects.requireNonNull<Any?>(peripheral, NO_VALID_PERIPHERAL_PROVIDED)
            Objects.requireNonNull<Any?>(peripheralCallback, NO_VALID_PERIPHERAL_CALLBACK_SPECIFIED)

            if (connectedPeripherals.containsKey(peripheral.getAddress())) {
                Logger.w(TAG, "already connected to %s'", peripheral.getAddress())
                return
            }

            if (unconnectedPeripherals.get(peripheral.getAddress()) != null) {
                Logger.w(TAG, "already issued autoconnect for '%s' ", peripheral.getAddress())
                return
            }

            if (!bluetoothAdapter!!.isEnabled()) {
                Logger.e(TAG, CANNOT_CONNECT_TO_PERIPHERAL_BECAUSE_BLUETOOTH_IS_OFF)
                return
            }

            // Check if the peripheral is uncached and start autoConnectPeripheralByScan
            if (peripheral.isUncached()) {
                Logger.d(TAG, "peripheral with address '%s' not in Bluetooth cache, autoconnecting by scanning", peripheral.getAddress())
                scannedPeripherals.remove(peripheral.getAddress())
                unconnectedPeripherals.put(peripheral.getAddress(), peripheral)
                autoConnectPeripheralByScan(peripheral.getAddress(), peripheralCallback)
                return
            }

            if (peripheral.getType() === PeripheralType.CLASSIC) {
                Logger.e(TAG, "peripheral does not support Bluetooth LE")
                return
            }

            peripheral.setPeripheralCallback(peripheralCallback)
            scannedPeripherals.remove(peripheral.getAddress())
            unconnectedPeripherals.put(peripheral.getAddress(), peripheral)
            peripheral.autoConnect()
        }
    }

    private fun autoConnectPeripheralByScan(peripheralAddress: String?, peripheralCallback: BluetoothPeripheralCallback?) {
        if (reconnectPeripheralAddresses.contains(peripheralAddress)) {
            Logger.w(TAG, "peripheral already on list for reconnection")
            return
        }

        reconnectPeripheralAddresses.add(peripheralAddress)
        reconnectCallbacks.put(peripheralAddress, peripheralCallback)
        scanForAutoConnectPeripherals()
    }

    */
/**
     * Cancel an active or pending connection for a peripheral.
     *
     * @param peripheral the peripheral
     *//*

    fun cancelConnection(peripheral: BluetoothPeripheral) {
        Objects.requireNonNull<Any?>(peripheral, NO_VALID_PERIPHERAL_PROVIDED)

        // First check if we are doing a reconnection scan for this peripheral
        val peripheralAddress: String? = peripheral.getAddress()
        if (reconnectPeripheralAddresses.contains(peripheralAddress)) {
            reconnectPeripheralAddresses.remove(peripheralAddress)
            reconnectCallbacks.remove(peripheralAddress)
            unconnectedPeripherals.remove(peripheralAddress)
            stopAutoconnectScan()
            Logger.d(TAG, "cancelling autoconnect for %s", peripheralAddress)
            callBackHandler!!.post(object : Runnable {
                override fun run() {
                    bluetoothCentralManagerCallback.onDisconnectedPeripheral(peripheral, HciStatus.SUCCESS)
                }
            })

            // If there are any devices left, restart the reconnection scan
            if (reconnectPeripheralAddresses.size > 0) {
                scanForAutoConnectPeripherals()
            }
            return
        }

        // Only cancel connectioins if it is an known peripheral
        if (unconnectedPeripherals.containsKey(peripheralAddress) || connectedPeripherals.containsKey(peripheralAddress)) {
            peripheral.cancelConnection()
        } else {
            Logger.e(TAG, "cannot cancel connection to unknown peripheral %s", peripheralAddress)
        }
    }

    */
/**
     * Autoconnect to a batch of peripherals.
     *
     *
     * Use this function to autoConnect to a batch of peripherals, instead of calling autoConnect on each of them.
     * Calling autoConnect on many peripherals may cause Android scanning limits to kick in, which is avoided by using autoConnectPeripheralsBatch.
     *
     * @param batch the map of peripherals and their callbacks to autoconnect to
     *//*

    fun autoConnectPeripheralsBatch(batch: MutableMap<BluetoothPeripheral, BluetoothPeripheralCallback?>) {
        Objects.requireNonNull<MutableMap<BluetoothPeripheral?, BluetoothPeripheralCallback?>>(batch, "no valid batch provided")

        if (!bluetoothAdapter!!.isEnabled()) {
            Logger.e(TAG, CANNOT_CONNECT_TO_PERIPHERAL_BECAUSE_BLUETOOTH_IS_OFF)
            return
        }

        // Find the uncached peripherals and issue autoConnectPeripheral for the cached ones
        val uncachedPeripherals: MutableMap<BluetoothPeripheral, BluetoothPeripheralCallback?> =
            HashMap<BluetoothPeripheral, BluetoothPeripheralCallback?>()
        for (peripheral in batch.keys) {
            if (peripheral.isUncached()) {
                uncachedPeripherals.put(peripheral, batch.get(peripheral))
            } else {
                autoConnectPeripheral(peripheral, batch.get(peripheral))
            }
        }

        // Add uncached peripherals to list of peripherals to scan for
        if (!uncachedPeripherals.isEmpty()) {
            for (peripheral in uncachedPeripherals.keys) {
                val peripheralAddress: String? = peripheral.getAddress()
                reconnectPeripheralAddresses.add(peripheralAddress)
                reconnectCallbacks.put(peripheralAddress, uncachedPeripherals.get(peripheral))
                unconnectedPeripherals.put(peripheralAddress, peripheral)
            }
            scanForAutoConnectPeripherals()
        }
    }

    */
/**
     * Get a peripheral object matching the specified mac address.
     *
     * @param peripheralAddress mac address
     * @return a BluetoothPeripheral object matching the specified mac address or null if it was not found
     *//*

    fun getPeripheral(peripheralAddress: String): BluetoothPeripheral {
        Objects.requireNonNull<String>(peripheralAddress, NO_PERIPHERAL_ADDRESS_PROVIDED)

        if (!BluetoothAdapter.checkBluetoothAddress(peripheralAddress)) {
            val message = String.format("%s is not a valid bluetooth address. Make sure all alphabetic characters are uppercase.", peripheralAddress)
            throw IllegalArgumentException(message)
        }

        if (connectedPeripherals.containsKey(peripheralAddress)) {
            return Objects.requireNonNull<BluetoothPeripheral?>(connectedPeripherals.get(peripheralAddress))
        } else if (unconnectedPeripherals.containsKey(peripheralAddress)) {
            return Objects.requireNonNull<BluetoothPeripheral?>(unconnectedPeripherals.get(peripheralAddress))
        } else if (scannedPeripherals.containsKey(peripheralAddress)) {
            return Objects.requireNonNull<BluetoothPeripheral?>(scannedPeripherals.get(peripheralAddress))
        } else {
            val peripheral: BluetoothPeripheral = BluetoothPeripheral(
                context,
                bluetoothAdapter!!.getRemoteDevice(peripheralAddress),
                internalCallback,
                NULL(),
                callBackHandler,
                transport
            )
            scannedPeripherals.put(peripheralAddress, peripheral)
            return peripheral
        }
    }

    */
/**
     * Get the list of connected peripherals.
     *
     * @return list of connected peripherals
     *//*

    fun getConnectedPeripherals(): MutableList<BluetoothPeripheral?> {
        return ArrayList<BluetoothPeripheral?>(connectedPeripherals.values)
    }

    private fun bleNotReady(): Boolean {
        if (isBleSupported()) {
            if (isBluetoothEnabled()) {
                return !permissionsGranted()
            }
        }
        return true
    }

    private fun isBleSupported(): Boolean {
        if (context!!.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return true
        }

        Logger.e(TAG, "BLE not supported")
        return false
    }

    */
/**
     * Check if Bluetooth is enabled
     *
     * @return true is Bluetooth is enabled, otherwise false
     *//*

    fun isBluetoothEnabled(): Boolean {
        if (bluetoothAdapter!!.isEnabled()) {
            return true
        }
        Logger.e(TAG, "Bluetooth disabled")
        return false
    }

    private fun permissionsGranted(): Boolean {
        val targetSdkVersion = context!!.getApplicationInfo().targetSdkVersion
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && targetSdkVersion >= Build.VERSION_CODES.S) {
            if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                throw SecurityException("app does not have BLUETOOTH_SCAN permission, cannot start scan")
            }
            if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                throw SecurityException("app does not have BLUETOOTH_CONNECT permission, cannot connect")
            } else return true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetSdkVersion >= Build.VERSION_CODES.Q) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                throw SecurityException("app does not have ACCESS_FINE_LOCATION permission, cannot start scan")
            } else return true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                throw SecurityException("app does not have ACCESS_COARSE_LOCATION permission, cannot start scan")
            } else return true
        } else {
            return true
        }
    }

    */
/**
     * Set scan timeout timer, timeout time is `SCAN_TIMEOUT`.
     * If timeout is executed the scan is stopped and automatically restarted. This is done to avoid Android 9 scan restrictions
     *//*

    private fun setScanTimer() {
        cancelTimeoutTimer()

        timeoutRunnable = object : Runnable {
            override fun run() {
                Logger.d(TAG, "scanning timeout, restarting scan")
                val callback = currentCallback
                val filters: MutableList<ScanFilter?> =
                    (if (currentFilters != null) currentFilters else kotlin.collections.mutableListOf<ScanFilter?>())!!
                stopScan()

                // Restart the scan and timer
                callBackHandler!!.postDelayed(object : Runnable {
                    override fun run() {
                        if (callback != null) {
                            startScan(filters, scanSettings!!, callback)
                        }
                    }
                }, SCAN_RESTART_DELAY.toLong())
            }
        }

        mainHandler.postDelayed(timeoutRunnable!!, SCAN_TIMEOUT)
    }

    */
/**
     * Cancel the scan timeout timer
     *//*

    private fun cancelTimeoutTimer() {
        if (timeoutRunnable != null) {
            mainHandler.removeCallbacks(timeoutRunnable!!)
            timeoutRunnable = null
        }
    }

    */
/**
     * Set scan timeout timer, timeout time is `SCAN_TIMEOUT`.
     * If timeout is executed the scan is stopped and automatically restarted. This is done to avoid Android 9 scan restrictions
     *//*

    private fun setAutoConnectTimer() {
        cancelAutoConnectTimer()
        autoConnectRunnable = object : Runnable {
            override fun run() {
                Logger.d(TAG, "autoconnect scan timeout, restarting scan")

                // Stop previous autoconnect scans if any
                stopAutoconnectScan()

                // Restart the auto connect scan and timer
                mainHandler.postDelayed(object : Runnable {
                    override fun run() {
                        scanForAutoConnectPeripherals()
                    }
                }, SCAN_RESTART_DELAY.toLong())
            }
        }

        mainHandler.postDelayed(autoConnectRunnable!!, SCAN_TIMEOUT)
    }

    */
/**
     * Cancel the scan timeout timer
     *//*

    private fun cancelAutoConnectTimer() {
        if (autoConnectRunnable != null) {
            mainHandler.removeCallbacks(autoConnectRunnable!!)
            autoConnectRunnable = null
        }
    }

    */
/**
     * Set a fixed PIN code for a peripheral that asks for a PIN code during bonding.
     *
     *
     * This PIN code will be used to programmatically bond with the peripheral when it asks for a PIN code. The normal PIN popup will not appear anymore.
     *
     * Note that this only works for peripherals with a fixed PIN code.
     *
     * @param peripheralAddress the address of the peripheral
     * @param pin               the 6 digit PIN code as a string, e.g. "123456"
     * @return true if the pin code and peripheral address are valid and stored internally
     *//*

    fun setPinCodeForPeripheral(peripheralAddress: String, pin: String): Boolean {
        Objects.requireNonNull<String>(peripheralAddress, NO_PERIPHERAL_ADDRESS_PROVIDED)
        Objects.requireNonNull<String>(pin, "no pin provided")

        if (!BluetoothAdapter.checkBluetoothAddress(peripheralAddress)) {
            Logger.e(TAG, "%s is not a valid address. Make sure all alphabetic characters are uppercase.", peripheralAddress)
            return false
        }

        if (pin.length != 6) {
            Logger.e(TAG, "%s is not 6 digits long", pin)
            return false
        }

        pinCodes.put(peripheralAddress, pin)
        return true
    }

    */
/**
     * Remove bond for a peripheral.
     *
     * @param peripheralAddress the address of the peripheral
     * @return true if the peripheral was succesfully bonded or it wasn't bonded, false if it was bonded and removing it failed
     *//*

    fun removeBond(peripheralAddress: String): Boolean {
        Objects.requireNonNull<String>(peripheralAddress, NO_PERIPHERAL_ADDRESS_PROVIDED)

        // Get the set of bonded devices
        val bondedDevices = bluetoothAdapter!!.getBondedDevices()

        // See if the device is bonded
        var peripheralToUnBond: BluetoothDevice? = null
        if (bondedDevices.size > 0) {
            for (device in bondedDevices) {
                if (device.getAddress() == peripheralAddress) {
                    peripheralToUnBond = device
                }
            }
        } else {
            return true
        }

        // Try to remove the bond
        if (peripheralToUnBond != null) {
            try {
                val method = peripheralToUnBond.javaClass.getMethod("removeBond", *null as Array<Class<*>?>?)
                val result = method.invoke(peripheralToUnBond, *null as Array<Any?>?) as Boolean
                if (result) {
                    Logger.i(TAG, "Succesfully removed bond for '%s'", peripheralToUnBond.getName())
                }
                return result
            } catch (e: Exception) {
                Logger.i(TAG, "could not remove bond")
                e.printStackTrace()
                return false
            }
        } else {
            return true
        }
    }

    */
/**
     * Make the pairing popup appear in the foreground by doing a 1 sec discovery.
     *
     *
     * If the pairing popup is shown within 60 seconds, it will be shown in the foreground.
     *//*

    fun startPairingPopupHack() {
        // Check if we are on a Samsung device because those don't need the hack
        val manufacturer = Build.MANUFACTURER
        //        if (!manufacturer.equalsIgnoreCase("samsung")) {
        if (bleNotReady()) return
        bluetoothAdapter!!.startDiscovery()
        callBackHandler!!.postDelayed(object : Runnable {
            override fun run() {
                Logger.d(TAG, "popup hack completed")
                bluetoothAdapter.cancelDiscovery()
            }
        }, 1000)
//        }
    }

    */
/**
     * Some phones, like Google/Pixel phones, don't automatically disconnect devices so this method does it manually
     *//*

    private fun cancelAllConnectionsWhenBluetoothOff() {
        Logger.d(TAG, "disconnect all peripherals because bluetooth is off")
        // Call cancelConnection for connected peripherals
        for (peripheral in connectedPeripherals.values) {
            peripheral.disconnectWhenBluetoothOff()
        }
        connectedPeripherals.clear()

        // Call cancelConnection for unconnected peripherals
        for (peripheral in unconnectedPeripherals.values) {
            peripheral.disconnectWhenBluetoothOff()
        }
        unconnectedPeripherals.clear()

        // Clean up autoconnect by scanning information
        reconnectPeripheralAddresses.clear()
        reconnectCallbacks.clear()
    }

    protected val adapterStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.getAction()
            if (action == null) return

            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                handleAdapterState(state)
                callBackHandler!!.post(object : Runnable {
                    override fun run() {
                        bluetoothCentralManagerCallback.onBluetoothAdapterStateChanged(state)
                    }
                })
            }
        }
    }

    private fun handleAdapterState(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_OFF -> {
                // Check if there are any connected peripherals or connections in progress
                if (connectedPeripherals.size > 0 || unconnectedPeripherals.size > 0) {
                    cancelAllConnectionsWhenBluetoothOff()
                }
                Logger.d(TAG, "bluetooth turned off")
            }

            BluetoothAdapter.STATE_TURNING_OFF -> {
                // Disconnect connected peripherals
                for (peripheral in connectedPeripherals.values) {
                    peripheral.cancelConnection()
                }

                // Disconnect unconnected peripherals
                for (peripheral in unconnectedPeripherals.values) {
                    peripheral.cancelConnection()
                }

                // Clean up autoconnect by scanning information
                reconnectPeripheralAddresses.clear()
                reconnectCallbacks.clear()

                // Stop all scans so that we are back in a clean state
                if (isScanning()) {
                    stopScan()
                }

                if (isAutoScanning()) {
                    stopAutoconnectScan()
                }

                cancelTimeoutTimer()
                cancelAutoConnectTimer()
                autoConnectScanner = null
                bluetoothScanner = null
                Logger.d(TAG, "bluetooth turning off")
            }

            BluetoothAdapter.STATE_ON -> {
                Logger.d(TAG, "bluetooth turned on")

                // On some phones like Nokia 8, this scanner may still have an older active scan from us
                // This happens when bluetooth is toggled. So make sure it is gone.
                bluetoothScanner = bluetoothAdapter!!.getBluetoothLeScanner()
                if (bluetoothScanner != null && currentCallback != null) {
                    try {
                        bluetoothScanner!!.stopScan(currentCallback)
                    } catch (ignore: Exception) {
                    }
                }
                currentCallback = null
                currentFilters = null
            }

            BluetoothAdapter.STATE_TURNING_ON -> Logger.d(TAG, "bluetooth turning on")
        }
    }

}*/
