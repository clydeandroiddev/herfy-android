package com.herfy.utils.permission

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.herfy.utils.R
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class PermissionProviderActivityDelegate(
    private val activity: FragmentActivity
) : ReadOnlyProperty<Activity, PermissionManager> {

    private var permissionManager: PermissionManager? = null

    init {
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                activity.apply {
                    permissionManager = PermissionManagerImpl(PermissionActivityOwner(activity))
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                permissionManager = null
            }
        })
    }

    override fun getValue(thisRef: Activity, property: KProperty<*>): PermissionManager {
        permissionManager?.let { return it }

        error(activity.resources.getString(R.string.permissions_error_message))
    }
}

fun FragmentActivity.permissionProvider(): PermissionProviderActivityDelegate =
    PermissionProviderActivityDelegate(this)
