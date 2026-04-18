package com.test.healthbox_app.presentation.dashboard

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.healthbox_app.data.permission.PermissionHandler
import com.test.healthbox_app.domain.model.ConnectionState1
import com.test.healthbox_app.domain.model.ScanState
import com.test.healthbox_app.domain.repository.SetPermissionHandler
import com.test.healthbox_app.domain.use_cases.BleUseCases
import com.test.healthbox_app.domain.use_cases.CheckAndRequestPermissionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val bleUseCases: BleUseCases,
    private val checkAndRequestPermissionsUseCase: CheckAndRequestPermissionsUseCase
) : ViewModel() {

    private val _permissionsGranted = MutableLiveData<Boolean>()
    val permissionsGranted: LiveData<Boolean> = _permissionsGranted

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState1>(ConnectionState1(false, null))
    val connectionState: StateFlow<ConnectionState1> = _connectionState.asStateFlow()

    init {
//        observeConnectionState()
    }

    // We need this to set the PermissionHandler from the Fragment
    fun setPermissionHandler(permissionHandler: PermissionHandler) {
        // You'll need to add a setter in your UseCase and Repository
        (checkAndRequestPermissionsUseCase as? SetPermissionHandler)?.setPermissionHandler(
            permissionHandler
        )
    }

    fun checkPermissions() {
        Log.e("checkPermissionsLogs", "  :  Called  :  ")

        viewModelScope.launch {
            try {
                val granted = checkAndRequestPermissionsUseCase.checkPermissions()
                _permissionsGranted.value = granted
            } catch (e: Exception) {
                // Handle error
                _permissionsGranted.value = false
            }
        }
    }

    fun requestPermissions() {
        checkAndRequestPermissionsUseCase.requestPermissions()
    }


    /*fun startScan(scanDuration: Long = 10000) {
        _scanState.value = ScanState.Scanning
        bluetoothUseCases.scanForDevices(scanDuration)
            .onEach { devices ->

                Log.i("DashboardViewModelLog", "  :  devices  :  $devices")
                _scanState.value = ScanState.DevicesFound(devices)
            }
            .launchIn(viewModelScope)
    }

    fun connectToDevice(device: BleBluetoothDevice) {
        viewModelScope.launch {
            _scanState.value = ScanState.Connecting
            val result = bluetoothUseCases.connectToDevice(device)
            if (result.isFailure) {
                _scanState.value =
                    ScanState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            bluetoothUseCases.disconnect()
        }
    }


    private fun observeConnectionState() {
        bluetoothUseCases.observeConnectionState()
            .onEach { state ->
                _connectionState.value = state
                if (state.isConnected) {
                    _scanState.value = ScanState.Connected
                } else if (_scanState.value is ScanState.Connected) {
                    _scanState.value = ScanState.Idle
                }
            }
            .launchIn(viewModelScope)
    }*/

}