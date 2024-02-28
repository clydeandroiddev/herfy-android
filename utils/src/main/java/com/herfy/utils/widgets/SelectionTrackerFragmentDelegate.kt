package com.herfy.utils.widgets

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private class SelectionTrackerFragmentDelegate(
    private val fragment: Fragment
) : ReadWriteProperty<Fragment, SelectionTrackerFactory> {

    private var selectionTrackerFactory: SelectionTrackerFactory? = null

    init {
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                fragment.apply {
                    selectionTrackerFactory = DefaultSelectionTrackerFactory()
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                selectionTrackerFactory = null
            }
        })
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): SelectionTrackerFactory {
        selectionTrackerFactory?.let { return it }

        error("Failed to initialize SelectionTrackerFactory. Note that we can only access this after onCreate")
    }

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: SelectionTrackerFactory) {
        selectionTrackerFactory = value
    }
}

fun Fragment.selectionTrackerProvider(): ReadOnlyProperty<Fragment, SelectionTrackerFactory> =
    SelectionTrackerFragmentDelegate(this)