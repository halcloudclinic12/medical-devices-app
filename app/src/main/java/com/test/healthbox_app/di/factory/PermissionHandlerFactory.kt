package com.test.healthbox_app.di.factory

import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.test.healthbox_app.data.permission.PermissionHandler
import javax.inject.Inject

class PermissionHandlerFactory @Inject constructor() {
    fun create(
        fragment: Fragment,
        permissionLauncher: ActivityResultLauncher<Array<String>>
    ): PermissionHandler {
        return PermissionHandler(fragment, permissionLauncher)
    }
}