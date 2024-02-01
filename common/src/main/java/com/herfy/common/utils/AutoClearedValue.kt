package com.herfy.common.utils

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A lazy property that gets cleaned up when the fragment's view is destroyed.
 *
 * Accessing this variable while the fragment's view is destroyed will throw NPE.
 *
 * @param fragment fragment which this value will be hooked
 * @param cleanup lambda that is called to run cleanup logic. This is called before the object is set to null.
 */
class AutoClearedValue<T : Any>(
    fragment: Fragment,
    cleanup: ((T) -> Unit)? = null
) : ReadWriteProperty<Fragment, T> {
    private var _value: T? = null

    init {
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                fragment.viewLifecycleOwnerLiveData.observe(fragment) { viewLifecycleOwner ->
                    viewLifecycleOwner?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            if (cleanup != null) {
                                _value?.let(cleanup)
                            }
                            _value = null
                        }
                    })
                }
            }
        })
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        return _value ?: throw IllegalStateException(
            "should never call auto-cleared-value get when it might not be available"
        )
    }

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T) {
        _value = value
    }
}

/**
 * Creates an [AutoClearedValue] associated with this fragment.
 *
 * @param cleanup lambda that is called to run cleanup logic. This is called before the object is set to null.
 */
fun <T : Any> Fragment.autoCleared(cleanup: ((T) -> Unit)? = null) =
    AutoClearedValue<T>(this, cleanup)
