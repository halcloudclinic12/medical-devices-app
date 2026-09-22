package com.test.healthbox_app

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.data.model.TestFlowType
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.BleResult
import com.test.healthbox_app.domain.model.ConnectionState
import com.test.healthbox_app.domain.model.Hba1cMeasurement
import com.test.healthbox_app.domain.model.Measurement
import com.test.healthbox_app.domain.model.PrintState
import com.test.healthbox_app.domain.model.PrinterError
import com.test.healthbox_app.domain.model.ScanState
import com.test.healthbox_app.domain.model.WeightData
import com.test.healthbox_app.domain.model.WeightMeasurement
import com.test.healthbox_app.domain.use_cases.BleUseCases
import com.test.healthbox_app.domain.use_cases.BluetoothUseCases
import com.test.healthbox_app.domain.use_cases.ParseMeasurementUseCase
import com.test.healthbox_app.domain.use_cases.SharedPreferenceUseCases
import com.test.healthbox_app.domain.use_cases.WeighingScaleUseCases
import com.test.healthbox_app.presentation.tests.bloodPressure.ByteHelper
import com.test.healthbox_app.presentation.util.BleConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BleConnectionViewModel @Inject constructor(
    private val bleUseCases: BleUseCases,
    private val bluetoothUseCases: BluetoothUseCases,
    private val sharedPreferenceUseCases: SharedPreferenceUseCases,
    private val parseMeasurementUseCase: ParseMeasurementUseCase,
    private val weighingScaleUseCases: WeighingScaleUseCases
) : ViewModel() {

    private val TAG = "BleConnectionViewModel"

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    // Connection state as StateFlow
    private val _connectionState = MutableStateFlow<Map<DeviceType, ConnectionState>>(emptyMap())
    val btConnectionState: StateFlow<Map<DeviceType, ConnectionState>> = _connectionState.asStateFlow()

    // Expose the connection state to the UI
    val connectionState: StateFlow<Map<DeviceType, ConnectionState>> = bleUseCases.getConnectionState().stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyMap()
    )

    // Expose the HB Check device connection state to the UI
    val hbCheckConnectionState = bleUseCases.getHbCheckConnectionState().distinctUntilChanged().stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false
    )

    // Connected devices
    private val connectedDevices = mutableMapOf<DeviceType, BleDevice>()

//    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
//    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _selectedDevice = MutableStateFlow<BleDevice?>(null)
    val selectedDevice: StateFlow<BleDevice?> = _selectedDevice.asStateFlow()

    private val _selectedDeviceType = MutableStateFlow<DeviceType?>(null)
    val selectedDeviceType: StateFlow<DeviceType?> = _selectedDeviceType.asStateFlow()

    /**
     * Which test flow is in progress, tracked for the session so ResultsFragment
     * (activity-scoped, same as this ViewModel) knows which API to call and which
     * parameter list to show without needing a nav-arg Bundle. Set on entry to a test
     * screen, read once Results loads.
     */
    private val _currentTestFlow = MutableStateFlow<TestFlowType?>(null)
    val currentTestFlow: StateFlow<TestFlowType?> = _currentTestFlow.asStateFlow()

    fun setTestFlow(testFlowType: TestFlowType?) {
        _currentTestFlow.value = testFlowType
    }

    private val _commandResult = MutableStateFlow<BleResult<Unit>>(BleResult.Loading)
    val commandResult: StateFlow<BleResult<Unit>> = _commandResult

    private val _deviceResponses = MutableStateFlow<Map<DeviceType, Measurement>>(emptyMap())
    val deviceResponses: StateFlow<Map<DeviceType, Measurement>> = _deviceResponses

    private val _deviceResponsesReceived = MutableLiveData<Boolean>()
    val deviceResponsesReceived: LiveData<Boolean> = _deviceResponsesReceived

    private val _printState = MutableStateFlow<PrintState>(PrintState.Idle)
    val printState: MutableStateFlow<PrintState> = _printState

    /**
     * Same stale-replay hazard as resetTestState() in ResultsViewModel — printState is
     * activity-scoped and only ever moves forward (Loading -> Success/Error) from a print
     * attempt, never back. Confirmed via device logcat (2026-09-22): a stale Success from
     * an earlier print replayed synchronously into ResultsFragment.observePrintStatus()'s
     * collector on the very next Results visit, crashing with "No suitable parent found
     * from the given view" (CustomSnackBar.make(binding.root, ...) firing before
     * onCreateView had returned) — before createBasicTest() ever ran, silently losing that
     * whole submission. Call this at the top of observePrintStatus() so any replay is
     * always safe.
     */
    fun resetPrintState() {
        _printState.value = PrintState.Idle
    }

    private val _weightWiState = MutableStateFlow(WeighingUiState())
    val weightUiState: StateFlow<WeighingUiState> = _weightWiState.asStateFlow()

    init {

        observeConnectionState()
    }

    fun setDeviceType(deviceType: DeviceType?) {
        _selectedDeviceType.value = deviceType
    }

    fun setSelectedDevice(device: BleDevice?) {
        _selectedDevice.value = device
    }

    fun setDeviceResponseStatus(status: Boolean) {
//        _deviceResponsesReceived.value = status

        _deviceResponsesReceived.postValue(status)
    }

    /**
     * Clears every device's last stored measurement. deviceResponses is activity-scoped
     * and only ever grows (see the various _deviceResponses.update { current + ... } call
     * sites) — nothing resets it as a whole. All ~8 test fragments (Height, Temperature,
     * Pulse, BP, Blood Sugar, Body Analysis, Vision, HB Check) collect it and write
     * straight into their result field on whatever value is already there, so a fresh
     * StateFlow collector on the next visit to any of those screens replays the
     * *previous* patient's reading before a new one ever arrives. Call this at the same
     * session boundary as BodyCheckupPref.clearAll() (Results "Home" button) so the next
     * patient starts with a clean slate on every screen, not just the ones with their own
     * per-screen cleanup (e.g. stopHba1cListening()).
     */
    fun clearDeviceResponses() {
        _deviceResponses.value = emptyMap()
    }

    fun getDevice() {
        viewModelScope.launch {
            _selectedDevice.value = selectedDeviceType.value?.let { sharedPreferenceUseCases.getDevice(it) }
        }
        Log.i("getDeviceLog", "  :  type  : ${selectedDeviceType.value}  :  devices  :  ${selectedDevice.value}")
    }

    fun removeDevice(deviceType: DeviceType): Boolean {

        Log.i("RemoveDeviceByIdLog", "  :  type  : ${deviceType}  ")

        return sharedPreferenceUseCases.removeDevice(deviceType)

    }

    fun getDeviceByType(deviceType: DeviceType): BleDevice? {
        // sharedPreferenceUseCases.getDevice() is a plain synchronous SharedPreferences read
        // (see SharedPreferencesManager.getDevice()) - no suspend calls anywhere in that
        // chain, so it needs no coroutine. Previously this was wrapped in
        // viewModelScope.launch and the function returned its local var immediately
        // afterward, before that coroutine had a chance to run - so it always returned null
        // regardless of what was actually saved. Confirmed via DEBUG-DEVICESAVE logs: after
        // saveDevice() correctly persisted the HB_CHECK device (isSaved=true, verified by a
        // read-back), ConnectedDevicesFragment still couldn't find it through this function.
        val bleDevice = sharedPreferenceUseCases.getDevice(deviceType)

        Log.i("getDeviceByIdLog", "  :  type  : ${deviceType}  :  devices  :  ${bleDevice}")

        return bleDevice
    }


    // Inside your ViewModel class
    fun updateSelectedDevice(device: BleDevice?) {
        _selectedDevice.value = device
    }

    fun connectToDevice(device: BleDevice, deviceType: DeviceType? /*= selectedDeviceType.value*/) {
        viewModelScope.launch {
//            _scanState.value = ScanState.Connecting

            _connectionState.update { it + (deviceType!! to ConnectionState.Connecting) }

            bleUseCases.connectToDevice(device, deviceType!!).collect { result ->

                if (result.isSuccess) {
                    connectedDevices[deviceType] = device
                    _connectionState.update { it + (deviceType to ConnectionState.Connected) }

                } else if (result.isFailure) {
                    _connectionState.update { it + (deviceType to ConnectionState.Disconnected(result.exceptionOrNull().toString())) }
                }
            }
        }
    }

    fun getHeight() {

        Log.e("getHeightLogs", "   :  " + connectedDevices.values + "  :  ${connectionState.value}")

        // NOTE: previously this did `connectionState.value.forEach { ... }`, iterating every
        // device-type entry ever accumulated in the map (from other screens visited earlier
        // in the flow). Any entry that wasn't (HEIGHT, Connected) fell into the `else` branch
        // and called connectToDevice() again — redundantly reconnecting the Height device once
        // per stale entry (e.g. 4x in a row). Look up only the HEIGHT entry directly instead,
        // matching the pattern already used by getHba1cData().
        val deviceType = selectedDeviceType.value ?: return
        if (connectionState.value[deviceType] == ConnectionState.Connected) {
            val serviceUuid = UUID.fromString(BleConstants.HEIGHT_SERVICE)
            val characteristicUuid = UUID.fromString(BleConstants.HEIGHT_MEASUREMENT)

            startListeningForResponses(serviceUuid, characteristicUuid, deviceType)

            viewModelScope.launch {
                delay(1000)

                // Send "1" as the command
                sendCommand("1", serviceUuid, characteristicUuid, deviceType)
            }
        } else {
            selectedDevice.value?.let { connectToDevice(it, deviceType) }
        }
    }

    fun getTemperature() {
        Log.e("getTempLogs", "   :  " + connectedDevices.values + "  :  ${connectionState.value}")

        connectionState.value.forEach { deviceType, connectionState ->

            if (deviceType == selectedDeviceType.value && connectionState == ConnectionState.Connected) {

                val serviceUuid = UUID.fromString(BleConstants.THERMOMETER_SERVICE)
                val characteristicUuid = UUID.fromString(BleConstants.THERMOMETER_MEASUREMENT)

                startListeningForResponses(serviceUuid, characteristicUuid, selectedDeviceType.value!!)

            } /*else {
                connectToDevice(selectedDevice.value!!, selectedDeviceType.value!!)

            }*/
        }
    }

    fun getPulse() {
        Log.e("getTempLogs", "   :  " + connectedDevices.values + "  :  ${connectionState.value}")

        connectionState.value.forEach { deviceType, connectionState ->

            if (deviceType == selectedDeviceType.value && connectionState == ConnectionState.Connected) {

                val serviceUuid = UUID.fromString(BleConstants.PULSE_SERVICE)
                val characteristicUuid = UUID.fromString(BleConstants.PULSE_MEASUREMENT)

                startListeningForResponses(serviceUuid, characteristicUuid, selectedDeviceType.value!!)

            } /*else {
                connectToDevice(selectedDevice.value!!, selectedDeviceType.value!!)

            }*/
        }
    }

    fun getBloodPressure() {
        Log.e("getBPLogs", "   :  " + connectedDevices.values + "  :  ${connectionState.value}")

        connectionState.value.forEach { deviceType, connectionState ->

            if (deviceType == selectedDeviceType.value && connectionState == ConnectionState.Connected) {

                val serviceUuid = UUID.fromString(BleConstants.BLOOD_PRESSURE_SERVICE)
                val characteristicUuid = UUID.fromString(BleConstants.BLOOD_PRESSURE_MEASUREMENT)

                startListeningForResponses(serviceUuid, characteristicUuid, selectedDeviceType.value!!)

            } /*else {
                connectToDevice(selectedDevice.value!!, selectedDeviceType.value!!)

            }*/
        }
    }

    fun connectHBDevice(bleDevice: BleDevice, mActivity: Activity) {

        viewModelScope.launch {
//            bluetoothUseCases.connectToHBDevice(bleDevice, mActivity)

            bleUseCases.connectToHBDevice(bleDevice, mActivity).catch { error ->
                Log.e("hbCheckConn", "  :  error  :   $error   ")
            }.collect { result ->
                Log.e("hbCheckConn", "  :  res  :   $result   ")
            }

            launch {
                bleUseCases.getHBCheckResultFlow().distinctUntilChanged().collect { result ->

                    println("hbCheckResult Connect HB Device  :  result  :   $result   ")
                    _deviceResponses.update { current ->
                        current + (DeviceType.HB_CHECK to result)
                    }
                }
            }
        }
    }

    fun disconnectToHbDevice() {
        viewModelScope.launch {
            bleUseCases.disconnectToHBDevice()
        }
    }


    fun startHbTest() {

        viewModelScope.launch {
            bleUseCases.startHbTest()

            launch {
                bleUseCases.getHBCheckResultFlow().distinctUntilChanged().collect { result ->

                    println("hbCheckResult get Result Hb  :: result Check ::   $result   ")

                    _deviceResponses.update { current ->
                        current + (DeviceType.HB_CHECK to result)
                    }
                }
            }

        }
    }

    fun stopHbTest() {
        viewModelScope.launch {
            bleUseCases.stopHbTest()

        }
    }

    fun sendCommand(
        command: String, serviceUuid: UUID, characteristicUuid: UUID, deviceType: DeviceType
    ) {
        if (deviceType != null) {
            viewModelScope.launch {
                _commandResult.value = BleResult.Loading
                bleUseCases.sendCommand(command, deviceType, serviceUuid, characteristicUuid).fold(onSuccess = {
                    Log.e("sendCmdResLog", " : Succ : " + BleResult.Success(Unit))
                    _commandResult.value = BleResult.Success(Unit)
                }, onFailure = {
                    Log.e("sendCmdResLog", " : Err : " + BleResult.Error(it))

                    _commandResult.value = BleResult.Error(it)
                })
            }
        }
    }

    fun startListeningForResponses(
        serviceUuid: UUID, characteristicUuid: UUID, deviceType: DeviceType
    ) {
        if (deviceType != null) {
            // First enable notifications
            viewModelScope.launch {
                Log.e("strtLisFRes", "   : bef  : ")
                bleUseCases.startNotifications(deviceType, serviceUuid, characteristicUuid).fold(onSuccess = {
                    Log.e("strtListInP", "   : Success  : ")
                    // Then collect notification data
                    viewModelScope.launch {
                        bleUseCases.observeNotifications(deviceType, characteristicUuid).collect { data ->
                            Log.i("deviceResLogs", "   : bef  : $data")

                            // Parse the data using the repository through the use case
                            val parsedMeasurement = parseMeasurementUseCase.invoke(deviceType, data)

                            val scanRecord: ByteArray = data

                            if (scanRecord.size == 1) {
//                                mBluetoothGatt.writeData(startCmd);
                            } else if (scanRecord.size == 7) {
                                Log.d("MeasurementParsed", "FAiled: $deviceType, Data: ${ByteHelper.unsignedByteToInt(data[4])}")
//                                BloodPressureResult(TESTING, "" + ByteHelper.unsignedByteToInt(data[4]))
                            } else if (scanRecord.size == 8) {
                                val shrinkage: String = ByteHelper.unsignedByteToInt(data[3]).toString()
                                val diastolic: String = ByteHelper.unsignedByteToInt(data[4]).toString()
                                val heartRate: String = ByteHelper.unsignedByteToInt(data[5]).toString()

                                Log.d("MeasurementParsed", "Device: $deviceType, Data: $shrinkage  ::  $diastolic   ::  $heartRate")
                            }

                            if (parsedMeasurement != null) {
                                _deviceResponses.update { current ->
                                    current + (deviceType to parsedMeasurement)
                                }
                                Log.d("MeasurementParsed", "Device: $deviceType, Data: $parsedMeasurement")
                            }

//                            Log.d("deviceResLogs", "   :  " + _deviceResponses.value[deviceType]?.toString(Charsets.UTF_8))
                            Log.d("deviceResLogs", "   :  " + _deviceResponses.value[deviceType])
                        }
                    }
                }, onFailure = {
                    Log.e("strtListInP", "   : Error  : " + BleResult.Error(it).toString())
                    _commandResult.value = BleResult.Error(it)
                })
            }
        }
    }

    // Helper function to convert ByteArray to String
    fun getResponseAsString(deviceType: DeviceType): Measurement? {
//        return _deviceResponses.value[deviceType]?.toString(Charsets.UTF_8)
        return _deviceResponses.value[deviceType]
    }

    // Helper function to convert ByteArray to String
    fun getResultData(): Measurement? {
//        return _deviceResponses.value[deviceType]?.toString(Charsets.UTF_8)
        return _deviceResponses.value[selectedDeviceType.value]
    }

    fun disconnect(device: BleDevice, deviceType: DeviceType) {
        Log.e("DiscDeviceLog", "  :   " + device)

        viewModelScope.launch {
//            bluetoothUseCases.disconnect(device, deviceType)
            try {
                bleUseCases.disconnect(device, deviceType).collect { result ->
                    result.onSuccess {
                        Log.d("DisconnectDevice", "Successfully disconnected device")
                        // Update UI or state as needed
                    }.onFailure { exception ->
                        Log.e("DisconnectDevice", "Disconnection failed", exception)
                        // Handle error - show toast, update UI, etc.
                    }
                }
            } catch (e: Exception) {
                Log.e("DisconnectDevice", "Error in disconnection process", e)
            }
        }
    }

    private fun observeConnectionState() {
    }

    fun saveDevice(selectedDevice: BleDevice) {
        println("DEBUG-DEVICESAVE BleConnectionViewModel.saveDevice() called: name=${selectedDevice.name} address=${selectedDevice.address} deviceType=${selectedDevice.deviceType}")
        if (selectedDevice.deviceType == null) {
            // saveDevice() below keys the SharedPreferences entry on device.deviceType?.name -
            // if deviceType is null here, it silently writes under a null key instead of the
            // real type's name, and getDevice(HB_CHECK) (a real, non-null key) will never
            // find it again. This would explain "connected but not showing up" independent
            // of the getDeviceByType() race below.
            println("DEBUG-DEVICESAVE WARNING: deviceType is NULL - this device will be saved under a null key and won't be retrievable by DeviceType later")
            Log.e("DEBUG-DEVICESAVE", "WARNING: deviceType is NULL for device being saved: $selectedDevice")
        }

        viewModelScope.launch {
            selectedDevice.let {
                Log.e("savingDeviceLog", "  :   " + selectedDevice)
                val isSaved = sharedPreferenceUseCases.saveDevice(it)
                Log.e("savingDeviceLog", "  : saved : " + isSaved)
                println("DEBUG-DEVICESAVE saveDevice() isSaved=$isSaved for deviceType=${it.deviceType}")

                // Read the same type straight back to confirm it round-trips correctly.
                val readBack = it.deviceType?.let { type -> sharedPreferenceUseCases.getDevice(type) }
                println("DEBUG-DEVICESAVE saveDevice() read-back for deviceType=${it.deviceType} immediately after save: $readBack")
                Log.e("DEBUG-DEVICESAVE", "saveDevice() read-back for deviceType=${it.deviceType} immediately after save: $readBack")

//                Log.e("savingDeviceLog", "  : saved : " + deviceUseCases.getDevice(selectedDeviceType.value!!))
                Log.e("savingDeviceLog", "  : saved : " + sharedPreferenceUseCases.getDevice(DeviceType.BT_PRINTER))

            }
        }
    }

    fun startBTDeviceScanning() {
        viewModelScope.launch {
//            _uiState.value = _uiState.value.copy(isScanning = true, devices = emptyList())

            bluetoothUseCases.scanForDevices().collect { devices ->

                Log.e("scannedBTDevice", " : $devices")
                _scanState.value = ScanState.DevicesFound(devices)

                /*_scanState.value.po(
                devices = devices,
                isScanning = false
            )*/
            }

            delay(20000)

            bluetoothUseCases.stopScanning()
        }
    }

    fun pairBTDevice(bleDevice: BleDevice) {
        viewModelScope.launch {
//            _uiState.value = _uiState.value.copy(isScanning = true, devices = emptyList())

            bluetoothUseCases.pairDevice(bleDevice).collect { success ->
                Log.e("pairedBTDevice", " : $success")

                if (success) _connectionState.update { it + (DeviceType.BT_PRINTER to ConnectionState.Paired) }
                else _connectionState.update { it + (DeviceType.BT_PRINTER to ConnectionState.PairedFailed) }
            }
        }
    }

    /*fun connectBTDevice(bleDevice: BleDevice) {

        viewModelScope.launch {
//            _uiState.value = _uiState.value.copy(isScanning = true, devices = emptyList())
            println("connectBTDevice  : connecting : $connectionState")

            bluetoothUseCases.connectToDevice(bleDevice).collect { connectionState ->
                println("connectBTDevice  : connecting : $connectionState")
//                _connectionState.update { it + (DeviceType.BT_PRINTER to connectionState) }
            }
        }
    }*/

    fun connectBTDevice(bleDevice: BleDevice) {
        viewModelScope.launch(Dispatchers.IO) { // ⬅️ run on IO thread, not Main
            try {
                Log.d("connectBTDevice", "Connecting to device: ${bleDevice.name}")

                bluetoothUseCases.connectToDevice(bleDevice).catch { e ->
                    Log.e("connectBTDevice", "Connection failed: ${e.message}")
                    _connectionState.update {
                        it + (DeviceType.BT_PRINTER to ConnectionState.Error(e.message ?: "Unknown error"))
                    }
                }.collect { connectionState ->
                    Log.d("connectBTDevice", "State: $connectionState")

                    // ✅ Update state safely from background
                    _connectionState.update {
                        it + (DeviceType.BT_PRINTER to connectionState)
                    }
                }
            } catch (e: Exception) {
                Log.e("connectBTDevice", "Exception: ${e.message}", e)
                _connectionState.update {
                    it + (DeviceType.BT_PRINTER to ConnectionState.Error(e.message ?: "Unknown error"))
                }
            }
        }
    }


    fun getBTDeviceConnectionState() {
        viewModelScope.launch {
//            _uiState.value = _uiState.value.copy(isScanning = true, devices = emptyList())

            bluetoothUseCases.getConnectionState().collect { connectionState ->
                println("connectBTDeviceState BLE Connection : $connectionState")

                _connectionState.update { it + (DeviceType.BT_PRINTER to connectionState) }

            }
        }
    }

    fun printText(message: String) {
        viewModelScope.launch {
            _printState.value = PrintState.Loading

            try {
                val result = bluetoothUseCases.printText(message)

                Log.e("printResLogs", "  :  $result")

                result.fold(onSuccess = { printResult ->
                    _printState.value = PrintState.Success(printResult.message)
//                        _isConnected.value = true
                }, onFailure = { error ->
                    val errorMessage = when (error) {
                        is PrinterError.DeviceNotConnected -> {
//                                _isConnected.value = false
                            "Device not connected"
                        }

                        is PrinterError.PlatenOpen -> "Platen open"
                        is PrinterError.PaperOut -> "Paper out"
                        is PrinterError.ImproperVoltage -> "Printer at improper voltage"
                        is PrinterError.PrintFailure -> "Print failed"
                        is PrinterError.ParameterError -> "Parameter error"
                        is PrinterError.NoResponse -> "No response from Pride device"
                        is PrinterError.DemoVersion -> "Library in demo version"
                        is PrinterError.InvalidDeviceId -> "Connected device is not authenticated"
                        is PrinterError.NotActivated -> "Library not activated"
                        is PrinterError.NotSupported -> "Not Supported"
                        is PrinterError.UnknownError -> "Unknown Response from Device"
                        else -> "Unknown error occurred"
                    }
                    _printState.value = PrintState.Error(errorMessage)
                })
            } catch (e: Exception) {
                _printState.value = PrintState.Error("Unexpected error: ${e.message}")
            }
        }
    }

    fun observeWeightScannedDevices() {

        viewModelScope.launch {
            val result = weighingScaleUseCases.initSDK()

            Log.e("resultAiLinkLogs", " ::  " + result);
        }

        /*viewModelScope.launch {
            weighingScaleUseCases.getScannedDevicesStream().collect { devices ->

                Log.e(TAG, "weighingScaleLogs devicesLogs  :  ${devices}")

                _weightWiState.value = _weightWiState.value.copy(scannedDevices = devices)
            }
        }*/
    }

    fun startWeighing(sex: Int, age: Int, height: Int) {/*viewModelScope.launch {
            weighingScaleUseCases.startWeighingProcess(sex, age, height).collect { result ->
                Log.e(TAG, "weighingScaleLogs weighingProcess  :  ${result}")

                when (result) {
                    is WeighingScaleResult.Loading -> {
                        _weightWiState.value = _weightWiState.value.copy(
                            isLoading = true,
                            errorMessage = null
                        )
                    }

                    is WeighingScaleResult.Success -> {
                        val measurement = result.data
                        _weightWiState.value = _weightWiState.value.copy(
                            isLoading = false,
                            currentWeight = measurement.weightData,
                            lastMeasurement = if (measurement.bodyFatData != null) measurement else _weightWiState.value.lastMeasurement,
                            errorMessage = null
                        )
                    }

                    is WeighingScaleResult.Error -> {
                        _weightWiState.value = _weightWiState.value.copy(
                            isLoading = false,
                            errorMessage = result.exception.message
                        )
                    }
                }
            }
        }*/
    }

    fun getGlucoseData() {
        Log.e("getGlucoseDataLogs", "   : Get Glucose :  " + connectedDevices.values + "  :  ${connectionState.value}")

        // Same fix as getHeight(): don't forEach the whole connectionState map (it accumulates
        // one entry per device type ever visited this session) — look up only this screen's
        // entry directly, so stale entries from other screens don't trigger redundant
        // connectToDevice() calls for the glucose meter.
        val deviceType = selectedDeviceType.value ?: return
        Log.e("getGlucoseDataLogs", "   : Get Glucose Device type :  " + deviceType + "  :  ${connectionState.value[deviceType]}")

        if (connectionState.value[deviceType] == ConnectionState.Connected) {
            val serviceUuid = UUID.fromString(BleConstants.GLUCOSE_QPP_SERVICE)
            val characteristicUuid = UUID.fromString(BleConstants.GLUCOSE_QPP_CHAR_NOTIFY)

            startListeningForResponses(serviceUuid, characteristicUuid, deviceType)
        } else {
            selectedDevice.value?.let { connectToDevice(it, deviceType) }
        }
    }

    // ─── HbA1c (A1cEZ 2.0) ──────────────────────────────────────────────────
    //
    // Deliberately does NOT follow the getGlucoseData() shape:
    //  - no forEach over the whole connection map (would react to other devices)
    //  - no connectToDevice() fallback (would fire a reconnect for an unrelated type)
    //  - no !! assertions (state is nulled when leaving the screen)
    //  - the notification collector is held in a Job so a repeated call cannot
    //    stack duplicate collectors, which would feed every fragment to the
    //    stateful record parser twice and corrupt reassembly.

    private var hba1cNotificationJob: kotlinx.coroutines.Job? = null

    /**
     * Guards against waiting forever when the meter aborts a test on its own screen
     * (E-7 "delay in adding blood sample", E-3 used strip, HI/LO out-of-range, etc.).
     * Per the vendor's BLE dev manual, only the successful 49-byte record is ever
     * pushed over BLE — error states shown on the meter's LCD have no BLE equivalent
     * (confirmed against the manual: its BLE section defines no other message, and its
     * error-code table lives entirely in the LCD/USB-troubleshooting section). So a
     * failed test is indistinguishable from "still running" until this fires.
     */
    private var hba1cTimeoutJob: kotlinx.coroutines.Job? = null

    /** Manual specs the full cycle at 6±1 min; padded generously for a slow operator. */
    private val hba1cTestTimeoutMs = 8 * 60 * 1000L

    private val _hba1cMeasurement = MutableStateFlow<Hba1cMeasurement?>(null)
    val hba1cMeasurement: StateFlow<Hba1cMeasurement?> = _hba1cMeasurement.asStateFlow()

    /** One-shot "no result" signal for the HbA1c screen. Cleared once shown/consumed. */
    private val _hba1cError = MutableStateFlow<String?>(null)
    val hba1cError: StateFlow<String?> = _hba1cError.asStateFlow()

    fun clearHba1cError() {
        _hba1cError.value = null
    }

    /**
     * Starts listening for HbA1c records. Safe to call repeatedly — any previous
     * collector (and timeout) is cancelled first.
     *
     * The A1cEZ pushes its record unsolicited once the operator completes the test
     * on the meter, so there is nothing to "start" beyond enabling notifications.
     * If hardware sniffing later shows a start command is required, write it to
     * BleConstants.HBA1C_WRITE at the marked point below.
     */
    fun getHba1cData() {
        val deviceType = selectedDeviceType.value ?: return
        if (deviceType != DeviceType.HBA1C_METER) return
        if (connectionState.value[deviceType] != ConnectionState.Connected) {
            Log.w(TAG, "getHba1cData ignored — $deviceType is not connected")
            return
        }

        // Clear any half-received frame left over from a previous session.
        parseMeasurementUseCase.resetHba1cBuffer()
        _hba1cError.value = null

        hba1cNotificationJob?.cancel()
        hba1cTimeoutJob?.cancel()

        hba1cNotificationJob = viewModelScope.launch {
            val serviceUuid = UUID.fromString(BleConstants.HBA1C_SERVICE)
            val characteristicUuid = UUID.fromString(BleConstants.HBA1C_MEASUREMENT)

            bleUseCases.startNotifications(deviceType, serviceUuid, characteristicUuid).fold(
                onSuccess = {
                    Log.d(TAG, "HbA1c notifications enabled")

                    // If the meter turns out to need a kick, send it here:
                    // bleUseCases.sendCommand("<cmd>", deviceType, serviceUuid,
                    //     UUID.fromString(BleConstants.HBA1C_WRITE))

                    startHba1cTimeout()

                    bleUseCases.observeNotifications(deviceType, characteristicUuid)
                        .catch { e -> Log.e(TAG, "HbA1c notification stream failed: ${e.message}") }
                        .collect { data ->
                            val parsed = parseMeasurementUseCase.invoke(deviceType, data)
                                    as? Hba1cMeasurement ?: return@collect

                            // A genuine record decoded — stop the "no result" clock.
                            hba1cTimeoutJob?.cancel()

                            _hba1cMeasurement.value = parsed
                            _deviceResponses.update { current -> current + (deviceType to parsed) }
                        }
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to enable HbA1c notifications: ${error.message}")
                    _commandResult.value = BleResult.Error(error)
                    _hba1cError.value = error.message ?: "Failed to enable HbA1c notifications"
                }
            )
        }
    }

    /**
     * Fires once if no valid record has arrived within [hba1cTestTimeoutMs]. This is the
     * only failure signal the app has — see [hba1cTimeoutJob] for why.
     */
    private fun startHba1cTimeout() {
        hba1cTimeoutJob = viewModelScope.launch {
            delay(hba1cTestTimeoutMs)
            if (_hba1cMeasurement.value == null) {
                Log.w(TAG, "HbA1c test timed out with no result — meter may have shown an error")
                _hba1cError.value =
                    "No result received from the meter. Check its display for an error code and retry."
            }
        }
    }

    /** Stops listening and clears the last result. Call when leaving the HbA1c screen. */
    fun stopHba1cListening() {
        hba1cNotificationJob?.cancel()
        hba1cNotificationJob = null
        hba1cTimeoutJob?.cancel()
        hba1cTimeoutJob = null
        _hba1cMeasurement.value = null
        _hba1cError.value = null
        parseMeasurementUseCase.resetHba1cBuffer()
        _deviceResponses.update { current -> current - DeviceType.HBA1C_METER }
    }

    fun startScanning() {
        viewModelScope.launch {
            _weightWiState.value = _weightWiState.value.copy(isScanning = true, errorMessage = null)

            Log.e(TAG, "weighingScaleLogs  Scanning:  ${_weightWiState.value.isScanning}")

            weighingScaleUseCases.startScanning()

            /*when (val result = weighingScaleUseCases.startScanning()) {

                else -> {
                    Log.e(TAG, "weighingScaleLogs  ScanSuccess:  ${result}")

                    // Scanning started successfully
                }
            }*/
        }
    }

    fun stopScanning() {/*viewModelScope.launch {
            weighingScaleUseCases.stopScanning()
            _weightWiState.value = _weightWiState.value.copy(isScanning = false)
        }*/
    }

    fun clearError() {
        _weightWiState.value = _weightWiState.value.copy(errorMessage = null)
    }


}

data class WeighingUiState(
    val isScanning: Boolean = false,
    val scannedDevices: List<BleDevice> = emptyList(),
    val currentWeight: WeightData? = null,
    val lastMeasurement: WeightMeasurement? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)
