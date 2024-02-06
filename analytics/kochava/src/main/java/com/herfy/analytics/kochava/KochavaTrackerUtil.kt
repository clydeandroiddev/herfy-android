package com.herfy.analytics.kochava

import android.content.Context
import com.herfy.utils.helper.StringUtil
import com.kochava.tracker.Tracker
import com.kochava.tracker.events.Event
import com.kochava.tracker.events.EventType
import com.kochava.tracker.events.Events


object KochavaTrackerUtil {
    fun init(context: Context, guid : String){
        Tracker.getInstance().startWithAppGuid(context, guid)
        Events.getInstance().registerDefaultStringParameter("app_name", StringUtil.getAppName(context))
    }

    @JvmStatic
    fun trackEvent(eventType : EventType){
        Event.buildWithEventType(eventType)
            .send()
    }

    @JvmStatic
    fun shutdownKochava(context: Context){
        Tracker.getInstance().shutdown(context, false)
    }

}