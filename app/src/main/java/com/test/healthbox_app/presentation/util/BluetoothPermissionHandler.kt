package com.test.healthbox_app.presentation.util

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class BluetoothPermissionHandler(private val activity: AppCompatActivity) {

    // Permission request launcher
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    // Bluetooth enable request launcher
    private lateinit var bluetoothEnableLauncher: ActivityResultLauncher<Intent>

    // Callback for permission results
    private var onPermissionsResult: ((Boolean) -> Unit)? = null

    init {
        setupPermissionLaunchers()
    }

    private fun setupPermissionLaunchers() {
        // Setup permission request launcher
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            // Check if all required permissions are granted
            val allGranted = permissions.entries.all { it.value }
            onPermissionsResult?.invoke(allGranted)
        }

        // Setup Bluetooth enable request launcher
        bluetoothEnableLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Bluetooth is enabled, now check permissions
                checkAndRequestPermissions { permissionsGranted ->
                    onPermissionsResult?.invoke(permissionsGranted)
                }
            } else {
                // User declined to enable Bluetooth
                onPermissionsResult?.invoke(false)
            }
        }
    }

    /**
     * Request all necessary permissions for BLE operations
     * @param onResult Callback that will be invoked with the result (true if all permissions granted)
     */
    fun requestPermissions(onResult: (Boolean) -> Unit) {
        onPermissionsResult = onResult

        // First check if Bluetooth is enabled
        val bluetoothManager =
            activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            onResult(false)
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            // Request to enable Bluetooth
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(enableBtIntent)
        } else {
            // Bluetooth is enabled, check permissions
            checkAndRequestPermissions(onResult)
        }
    }

    private fun checkAndRequestPermissions(onResult: (Boolean) -> Unit) {
        val requiredPermissions = getRequiredPermissions()

        // Check if we already have permissions
        val allPermissionsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            onResult(true)
        } else {
            // Request permissions
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()

        // Location permission is required for Bluetooth scanning on all Android versions
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        // Android 12+ (API 31+) requires new Bluetooth permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // Older versions use these permissions
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        return permissions
    }

}