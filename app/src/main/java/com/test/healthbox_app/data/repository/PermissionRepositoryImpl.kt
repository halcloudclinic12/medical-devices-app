package com.test.healthbox_app.data.repository

import android.Manifest
import android.content.Context
import android.os.Build
import com.test.healthbox_app.data.permission.PermissionHandler
import com.test.healthbox_app.domain.repository.PermissionRepository
import com.test.healthbox_app.domain.repository.SetPermissionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PermissionRepositoryImpl(
    private val context: Context
) : PermissionRepository, SetPermissionHandler {

    private var permissionHandler: PermissionHandler? = null

    override fun setPermissionHandler(handler: PermissionHandler) {
        this.permissionHandler = handler
    }

    override suspend fun arePermissionsGranted(): Boolean = withContext(Dispatchers.IO) {
        val permissions = getRequiredPermissions()
        return@withContext permissionHandler!!.arePermissionsGranted(permissions)
    }

    override fun requestPermissions() {
        val permissions = getRequiredPermissions()
        permissionHandler!!.requestPermissions(permissions)
    }

    private fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()

        // Always needed for Bluetooth operations
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        // Android 12+ permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            // Only add location if you're using it for Bluetooth scanning
            // Otherwise, use neverForLocation flag in manifest
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            // Below Android 12
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        return permissions.toTypedArray()
    }
}