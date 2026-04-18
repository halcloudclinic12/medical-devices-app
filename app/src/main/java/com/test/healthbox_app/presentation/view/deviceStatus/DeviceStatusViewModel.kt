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

//    private val _selectedDevice = MutableStateFlow<BleDevice?>(null)
//    val selectedDevice: StateFlow<BleDevice?> = _selectedDevice.asStateFlow()
//
//    private val _selectedDeviceType = MutableStateFlow<DeviceType?>(null)
//    val selectedDeviceType: StateFlow<DeviceType?> = _selectedDeviceType.asStateFlow()

    fun initializeSteps(steps: List<StepItem>) {

    }

    fun startScan(scanDuration: Long = 10000) {
        viewModelScope.launch {
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
        viewModelScope.launch {
            _scanState.value = ScanState.Idle

            bleUseCases.stopBleScan()
        }
    }

    fun updateScannedDevicesList() {
        _scanState.value = ScanState.DevicesFound(emptyList())
    }


//    https://claude.ai/share/7ff5eb17-3a33-425f-8ced-cd6aa8586cd2

}