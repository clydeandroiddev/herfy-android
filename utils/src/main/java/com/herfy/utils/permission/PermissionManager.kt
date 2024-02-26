package com.herfy.utils.permission

import androidx.annotation.MainThread

interface PermissionManager {
    interface RationaleCallback {
        @MainThread
        fun onRationaleRequested()
        @MainThread
        fun launchUserPermission() {}

    }
    @MainThread
    fun requestPermissions(
        permissions: Array<String>,
        showRationale: ((RationaleCallback) -> Unit)? = null,
        resultCallback: (PermissionResult) -> Unit
    )
}