package com.herfy.util

import com.herfy.BuildConfig
import io.shipbook.shipbooksdk.Log


object ShipBookLogger {

    @JvmStatic
    fun logInfo(info: String) {
        Log.i(BuildConfig.TAG, info)
    }

    @JvmStatic
    fun logError(error: String) {
        Log.e(BuildConfig.TAG, error)
    }

}
