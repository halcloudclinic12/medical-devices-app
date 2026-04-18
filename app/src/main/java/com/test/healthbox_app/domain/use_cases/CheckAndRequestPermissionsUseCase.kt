package com.test.healthbox_app.domain.use_cases

import com.test.healthbox_app.data.permission.PermissionHandler
import com.test.healthbox_app.domain.repository.PermissionRepository
import com.test.healthbox_app.domain.repository.SetPermissionHandler


class CheckAndRequestPermissionsUseCase(
    private val permissionRepository: PermissionRepository
) : SetPermissionHandler {

    override fun setPermissionHandler(permissionHandler: PermissionHandler) {
        (permissionRepository as? SetPermissionHandler)?.setPermissionHandler(permissionHandler)
    }

    suspend fun checkPermissions(): Boolean {
        return permissionRepository.arePermissionsGranted()
    }

    fun requestPermissions() {
        permissionRepository.requestPermissions()
    }
}