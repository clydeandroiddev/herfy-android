package com.herfy.utils.permission

import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

 interface PermissionOwnerProvider {
    fun <I, O> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I>

    fun hasPermissions(vararg permissions: String): Boolean

    fun shouldShowRationale(vararg permissions: String): Boolean
}

internal class PermissionActivityOwner(
    private val activity: FragmentActivity
) : PermissionOwnerProvider {

    override fun <I, O> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I> = activity.registerForActivityResult(contract, callback)

    override fun hasPermissions(vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun shouldShowRationale(vararg permissions: String): Boolean = permissions.any {
        activity.shouldShowRequestPermissionRationale(it)
    }
}

internal class PermissionFragmentOwner(
    private val fragment: Fragment
) : PermissionOwnerProvider {

    private val activity get() = fragment.requireActivity()

    override fun <I, O> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I> = fragment.registerForActivityResult(contract, callback)

    override fun hasPermissions(vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun shouldShowRationale(vararg permissions: String): Boolean = permissions.any {
        fragment.shouldShowRequestPermissionRationale(it)
    }
}