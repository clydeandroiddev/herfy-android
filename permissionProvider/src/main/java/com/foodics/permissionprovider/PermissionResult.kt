package com.foodics.permissionprovider

sealed class PermissionResult {
    object Granted : PermissionResult()

    data class Denied(val deniedPermissions: List<String>) : PermissionResult()
}