package com.herfy.util

import android.content.Context
import android.os.Bundle
import com.capillary.hydra.pushbase.listener.HydraPushNotificationListener

class AppPushMessageListener: HydraPushNotificationListener {
    private val TAG = HydraPushNotificationListener::class.java.name

    override fun onNotificationReceived(context: Context, payload: Bundle) {
        super.onNotificationReceived(context, payload)
    }

    override fun onNotificationClick(context: Context, payload: Bundle) {
        return super.onNotificationClick(context, payload)
    }
    override fun onNotificationDismissed(context: Context, payload: Bundle) {
        super.onNotificationDismissed(context, payload)
    }
    override fun handleCustomAction(context: Context, payload: String) {
        super.handleCustomAction(context, payload)
    }

}