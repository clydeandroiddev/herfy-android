package com.foodics.permissionprovider

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private class PermissionProviderFragmentDelegate(
    private val fragment: Fragment
) : ReadOnlyProperty<Fragment, PermissionManager> {

    private var provider: PermissionManager? = null

    init {
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                fragment.apply {
                    provider = PermissionManagerImpl(PermissionFragmentOwner(this))
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                provider = null
            }
        })
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): PermissionManager {
        provider?.let { return it }
        error(fragment.resources.getString(R.string.permissions_error_message))
    }
}

fun Fragment.permissionProvider(): ReadOnlyProperty<Fragment, PermissionManager> =
    PermissionProviderFragmentDelegate(this)
