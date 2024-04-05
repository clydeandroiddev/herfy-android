package com.herfy.analytics.firebase

import com.google.firebase.perf.FirebasePerformance
import com.herfy.utils.helper.CacheUtil

object FirebaseEventUtilHelper {

    @JvmStatic
    fun logEventAttributesMetric(
        eventName: String,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {

        val trace = FirebasePerformance.getInstance().newTrace(
            eventName
        )
        trace.start()
        var mEventAttribute = eventAttributes?: HashMap()

        trace.putAttribute("ConceptID", CacheUtil.getInstance().concept.id)
        for (eventAttributeName in mEventAttribute.keys) {
            val eventAttributeValue = mEventAttribute[eventAttributeName]
            if (eventAttributeValue != null) {
                trace.putAttribute(eventAttributeName.replace(" ", ""), eventAttributeValue)
            }
        }
        if (!eventMetricName.isNullOrEmpty()) {
            trace.putAttribute(eventMetricName, eventMetricValue.toString())
        }
        trace.stop()
    }
}