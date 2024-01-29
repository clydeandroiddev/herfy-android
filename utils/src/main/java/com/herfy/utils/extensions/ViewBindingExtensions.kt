package com.herfy.utils.extensions

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

inline fun <reified T : ViewBinding> Activity.viewBinding(
    parent: ViewGroup? = null,
    attachToParent: Boolean = false,
): Lazy<T> {
    return object : Lazy<T> {

        private var cached: T? = null

        override val value: T
            get() {
                if (cached == null) {
                    cached = T::class.java.getDeclaredMethod(
                        "inflate", LayoutInflater::class.java,
                        ViewGroup::class.java, Boolean::class.java
                    ).invoke(null, layoutInflater, parent, attachToParent) as T
                }
                return cached!!
            }

        override fun isInitialized() = cached != null
    }
}

inline fun <reified T : ViewBinding> Fragment.viewBinding(
    parent: ViewGroup? = null,
    attachToParent: Boolean = false,
): Lazy<T> {
    return object : Lazy<T> {

        private var cached: T? = null

        override val value: T
            get() {
                if (cached == null) {
                    cached = T::class.java.getDeclaredMethod(
                        "inflate", LayoutInflater::class.java,
                        ViewGroup::class.java, Boolean::class.java
                    ).invoke(null, layoutInflater, parent, attachToParent) as T
                }
                return cached!!
            }

        override fun isInitialized() = cached != null
    }
}