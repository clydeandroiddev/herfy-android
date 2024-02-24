package com.foodics.permissionprovider

import androidx.activity.result.contract.ActivityResultContracts

class PermissionManagerImpl(private val owner: PermissionOwnerProvider): PermissionManager {
    private var resultCallback: ((PermissionResult) -> Unit)? = null
    private val permissionRequest = owner.registerForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        callback = ::handlePermissionRequestResult
    )

    override fun requestPermissions(
        permissions: Array<String>,
        showRationale: ((PermissionManager.RationaleCallback) -> Unit)?,
        resultCallback: (PermissionResult) -> Unit
    ) {
        this.resultCallback = resultCallback
        val isGrantedPermission = owner.hasPermissions(permissions = permissions)
        when {
            isGrantedPermission -> resultCallback(PermissionResult.Granted)
            showRationale != null && owner.shouldShowRationale(permissions = permissions) -> showRationaleCallback(permissions = permissions,
                showRationale = showRationale)

            else -> launchPermissionRequest(permissions = permissions)
        }
    }

    private fun handlePermissionRequestResult(map: Map<Permissions, Boolean>) {
        val deniedPermissions = map.entries
            .filter { !it.value }
            .map { it.key }

        if (deniedPermissions.isEmpty()) {
            resultCallback?.invoke(PermissionResult.Granted)
        } else {
            resultCallback?.invoke(PermissionResult.Denied(deniedPermissions))
        }
    }
    private fun launchPermissionRequest(permissions:Array<String>) {
        permissionRequest.launch(permissions)
    }

    private fun showRationaleCallback(
        showRationale: (PermissionManager.RationaleCallback) -> Unit,
        permissions: Array<String>
    ) {
        showRationale(object : PermissionManager.RationaleCallback {
            override fun onRationaleRequested() {
                launchPermissionRequest(permissions)
            }
        })
    }
}
typealias Permissions = String