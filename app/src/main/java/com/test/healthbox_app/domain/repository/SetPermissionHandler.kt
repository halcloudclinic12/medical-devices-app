package com.test.healthbox_app.domain.repository

import com.test.healthbox_app.data.permission.PermissionHandler

interface SetPermissionHandler {
    fun setPermissionHandler(permissionHandler: PermissionHandler)
}