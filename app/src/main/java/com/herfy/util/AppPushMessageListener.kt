package com.herfy.util

import android.content.Context
import android.os.Bundle
import com.capillary.hydra.pushbase.listener.HydraPushNotificationListener
import com.capillary.hydra.pushbase.notification.model.CTA
import com.capillary.hydra.pushbase.notification.model.NotificationPayload

class AppPushMessageListener: HydraPushNotificationListener {


    override fun onNotificationReceived(context: Context, payload: NotificationPayload) {
        super.onNotificationReceived(context, payload)
    }
    override fun onNotificationClick(context: Context, payload: NotificationPayload, cta: CTA?): Boolean {
        return super.onNotificationClick(context, payload, cta)
    }

    override fun onNotificationDismissed(context: Context, payload: NotificationPayload) {
        super.onNotificationDismissed(context, payload)
    }
    override fun handleCustomAction(context: Context, payload: String) {
        super.handleCustomAction(context, payload)
    }

}