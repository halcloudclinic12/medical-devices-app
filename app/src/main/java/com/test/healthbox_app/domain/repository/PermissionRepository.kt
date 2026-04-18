package com.test.healthbox_app.domain.repository

interface PermissionRepository {
    suspend fun arePermissionsGranted(): Boolean
    fun requestPermissions()
}