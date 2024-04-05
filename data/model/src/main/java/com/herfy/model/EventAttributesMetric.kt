package com.herfy.model

data class EventAttributesMetric(
    val eventName : String,
    val eventAttributes : HashMap<String, String> = hashMapOf(),
    val eventMetrictName : String = "",
    val eventMetricValue : Double
)
