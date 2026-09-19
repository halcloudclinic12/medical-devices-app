package com.test.healthbox_app.presentation.view.deviceStatus

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.domain.model.ConnectionState
import com.test.healthbox_app.domain.model.ScanState
import com.test.healthbox_app.domain.model.StepItem
import com.test.healthbox_app.domain.use_cases.BleUseCases
import com.test.healthbox_app.domain.use_cases.BluetoothUseCases
import com.test.healthbox_app.domain.use_cases.SharedPreferenceUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceStatusViewModel @Inject constructor(
    private val bleUseCases: BleUseCases,
    private val bluetoothUseCases: BluetoothUseCases,
    private val sharedPreferenceUseCases: SharedPreferenceUseCases
) : ViewModel() {

    // Connection state as StateFlow
    private val _connectionState = MutableStateFlow<Map<DeviceType, ConnectionState>>(emptyMap())

    // Expose the connection state to the UI
    val connectionState: StateFlow<Map<DeviceType, ConnectionState>> = bleUseCases.getConnectionState().stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyMap()
    )

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    // This ViewModel is activity-scoped/shared across every test screen. Without tracking
    // the running scan's own coroutine Job, a scan started on one screen keeps running (and
    // can still deliver a result into _scanState) after the user has navigated to a
    // different screen — whichever screen is now collecting scanState reacts to a result it
    // never asked for. Held so it can be cancelled outright, not just asked nicely to stop.
    private var scanJob: Job? = null

//    private val _selectedDevice = MutableStateFlow<BleDevice?>(null)
//    val selectedDevice: StateFlow<BleDevice?> = _selectedDevice.asStateFlow()
//
//    private val _selectedDeviceType = MutableStateFlow<DeviceType?>(null)
//    val selectedDeviceType: StateFlow<DeviceType?> = _selectedDeviceType.asStateFlow()

    fun initializeSteps(steps: List<StepItem>) {

    }

    fun startScan(scanDuration: Long = 10000) {
        // Defensive net only: callers are expected to gate on MainActivity.ensureBluetoothEnabled()
        // before invoking this (see DeviceStatusLayout.setOnScanClickListener and
        // BaseFragment.ensureBluetoothEnabled), which prompts the user to turn Bluetooth on.
        // If one of those is ever bypassed, silently no-op rather than starting a scan that
        // BleScanner would immediately fail anyway.
        if (!bluetoothUseCases.isBluetoothEnabled()) {
            Log.w("DeviceStatusViewModel", "startScan() called while Bluetooth is disabled")
            return
        }

        // Cancelling any scan already in flight (rather than letting two run at once) is
        // what actually stops a previous screen's abandoned scan from later delivering a
        // result nobody asked for - see the comment on scanJob.
        scanJob?.cancel()

        scanJob = viewModelScope.launch {
            _scanState.value = ScanState.Scanning

            bleUseCases.scanForDevices(scanDuration).collect { devices ->
                Log.d("BleRepoSCanned", "Received ${devices.size} scan results")
                // Process devices

                Log.i("DashboardViewModelLog", "  :  devices  :  $devices")
                _scanState.value = ScanState.DevicesFound(devices)
                Log.i("DashboardViewModelLog", "  :  devices after :  ${scanState}")
            }

            /*bluetoothUseCases.scanForDevices(scanDuration).onEach { devices ->
                Log.i("DashboardViewModelLog", "  :  devices  :  $devices")
                _scanState.value = ScanState.DevicesFound(devices)
                Log.i("DashboardViewModelLog", "  :  devices after :  ${scanState}")

            }.launchIn(viewModelScope)*/
        }
    }

    fun stopBleScan() {
        // Cancelling the Job outright (not just closing the scanner's channel) reliably
        // stops a collect{} loop that's mid-flight, which merely setting _scanState back to
        // Idle does not - a result the scan callback already produced a moment earlier would
        // otherwise still land in _scanState right after this runs.
        scanJob?.cancel()
        scanJob = null

        _scanState.value = ScanState.Idle

        viewModelScope.launch {
            bleUseCases.stopBleScan()
        }
    }

    /**
     * Clears the scanned-devices list after a device is picked/connected (called from each test
     * fragment's onXConnected()). This must reset to Idle, not DevicesFound(emptyList()) — every
     * fragment's scanState observer treats DevicesFound(emptyList()) as "scan finished, found
     * nothing" and shows a "No devices found" message, which would misfire here even though no
     * scan ever ran and the connection actually succeeded.
     */
    fun updateScannedDevicesList() {
        _scanState.value = ScanState.Idle
    }

    /**
     * Consumes a terminal DevicesFound(emptyList()) result after the fragment has already shown
     * "No devices found" for it. This ViewModel is activity-scoped (shared across every test
     * screen via activityViewModels()), so without this, the StateFlow keeps holding that value
     * and replays it — misfiring the same message — to any collector that (re)subscribes later:
     * navigating back to this screen, backgrounding/foregrounding, or opening a different test
     * screen that never scanned at all. Call this only from the empty-DevicesFound branch, right
     * after showing the message; never from Error or a non-empty DevicesFound branch.
     */
    fun clearScanState() {
        _scanState.value = ScanState.Idle
    }

//    https://claude.ai/share/7ff5eb17-3a33-425f-8ced-cd6aa8586cd2

}