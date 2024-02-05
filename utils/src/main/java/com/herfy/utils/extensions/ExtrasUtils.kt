package com.herfy.utils.extensions

import android.content.Intent
import android.os.Build
import android.os.Bundle
import java.io.Serializable

inline fun <reified T> Intent.getParcelableValue(key: String) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        getParcelableExtra(key)
    }

inline fun <reified T> Bundle.getParcelableValue(key: String) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, T::class.java)
    } else {
        getParcelable(key)
    }

inline fun <reified T : Serializable> Intent.getSerializableValue(key: String) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializableExtra(key, T::class.java)
    } else {
        getSerializableExtra(key)
    }

inline fun <reified T : Serializable> Bundle.getSerializableFromBundle(key: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(key, T::class.java)
    } else {
        getSerializable(key) as? T
    }