package com.test.healthbox_app.data.permission

import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class PermissionHandler(
    private val fragment: Fragment,
    private val permissionLauncher: ActivityResultLauncher<Array<String>>
) {

    fun arePermissionsGranted(permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(
                fragment.requireContext(), it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermissions(permissions: Array<String>) {
        permissionLauncher.launch(permissions)
    }

}