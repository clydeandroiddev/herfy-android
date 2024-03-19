package com.herfy.common

import android.annotation.SuppressLint
import android.util.Log
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@SuppressLint("SimpleDateFormat")
fun String.formatDate(): String {
    return try {
        val originalFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val date: Date
        try {
            date = originalFormat.parse(this)!!
        } catch (e: ParseException) {
            e.printStackTrace()
            return this
        }
        // Format the date in the desired format
        val targetFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        targetFormat.format(date)
    } catch (pe: ParseException) {
        this
    }
}


fun String.orderTypeString(): String {
    val type = OrderType.fromString(this.toLowerCase())
    Log.e("ADADADADAD", "ADADADAd:" + type)
    when (type) {
        OrderType.PICKUP, OrderType.EAT_IN, OrderType.DRIVE_THRU -> {
            val headerText: String? = if (type == OrderType.EAT_IN) {
                CacheUtil.getInstance().getTranslations(Text.DINE_IN, "Dine In")
            } else if (type == OrderType.DRIVE_THRU) {
                CacheUtil.getInstance().getTranslations(Text.DRIVE_THRU, "Drive Thru")
            } else {
                CacheUtil.getInstance().getTranslations(Text.PICKUP, "Pickup")
            }
            return headerText.orEmpty()
        }
        OrderType.DINE_IN -> {
            return CacheUtil.getInstance().getTranslations(Text.DINE_IN, "Dine In")
        }
        OrderType.DELIVERY -> {
            return CacheUtil.getInstance().getTranslations(Text.DELIVERY, "Delivery")
        }
        OrderType.CURBSIDE -> {
            return CacheUtil.getInstance().getTranslations(Text.CURBSIDE, "Curbside")
        }
        else -> {}
    }
    return ""
}

fun String.formatOrderTime(): String {
    try {
        var simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT+0")
        val dateValue = simpleDateFormat.parse(this)

        simpleDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getDefault()
        val dateMonth = dateValue?.let { simpleDateFormat.format(it) }

        // Split the formatted date to extract date and time parts
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        var time = dateValue?.let { timeFormat.format(it) }

        // Get current date and time
        val currentDate = Date()
        val currentCalendar = Calendar.getInstance().apply {
            time = currentDate.toString()
        }

        // Parse the date part to check if it's today, yesterday, or in the same year
        val parsedCalendar = Calendar.getInstance().apply {
            if (dateValue != null) {
                time = dateValue.toString()
            }
        }

        val dateValues = mutableListOf<String>()

        // Check if it's today
        if (currentCalendar.get(Calendar.YEAR) == parsedCalendar.get(Calendar.YEAR) && currentCalendar.get(
                Calendar.DAY_OF_YEAR
            ) == parsedCalendar.get(Calendar.DAY_OF_YEAR)
        ) {
            dateValues.add(CacheUtil.getInstance().getTranslations(Text.TODAY, "Today"))
        }

        // Check if it's yesterday
        currentCalendar.add(Calendar.DAY_OF_YEAR, -1)
        if (currentCalendar.get(Calendar.YEAR) == parsedCalendar.get(Calendar.YEAR) && currentCalendar.get(
                Calendar.DAY_OF_YEAR
            ) == parsedCalendar.get(Calendar.DAY_OF_YEAR)
        ) {
            dateValues.add(CacheUtil.getInstance().getTranslations(Text.YESTERDAY, "Yesterday"))
        }

        // Check if it's in the same year
        currentCalendar.time = currentDate // Reset calendar to current date
        if (currentCalendar.get(Calendar.YEAR) != parsedCalendar.get(Calendar.YEAR)) {
            if (dateMonth != null) {
                dateValues.add(dateMonth)
            }
        }

        // Combine date values with time
        val formattedDate = if (dateValues.isNotEmpty()) {
            "${dateValues.joinToString(", ")} $time"
        } else {
            "$dateMonth $time"
        }

        return formattedDate
    } catch (e: ParseException) {
        e.printStackTrace()
    }
    return this.defaultTime()
}

fun String.defaultTime(): String {
    try {
        var simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT+0")
        val dateValue = simpleDateFormat.parse(this)
        simpleDateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getDefault()
        return dateValue?.let { simpleDateFormat.format(it) }.orEmpty()
    } catch (e: ParseException) {
        e.printStackTrace()
    }
    return ""
}