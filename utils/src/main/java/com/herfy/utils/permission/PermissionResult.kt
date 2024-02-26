package com.herfy.utils.permission

sealed class PermissionResult {
    object Granted : PermissionResult()

    data class Denied(val deniedPermissions: List<String>) : PermissionResult()
}