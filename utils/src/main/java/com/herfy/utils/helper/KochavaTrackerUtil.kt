package com.herfy.utils.helper

import android.content.Context
import com.herfy.utils.BuildConfig
import com.kochava.tracker.Tracker
import com.kochava.tracker.events.Event
import com.kochava.tracker.events.EventType
import com.kochava.tracker.events.Events
import com.kochava.tracker.log.LogLevel


object KochavaTrackerUtil {
    fun init(context: Context, guid : String){
        if (BuildConfig.DEBUG) {
            Tracker.getInstance().setLogLevel(LogLevel.TRACE)
        } else {
            Tracker.getInstance().setLogLevel(LogLevel.INFO)
        }
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