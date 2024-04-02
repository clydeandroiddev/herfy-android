package com.herfy.common

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.TypedValue
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

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


fun String.orderDetailsType(): String {
    val type = OrderType.fromString(this.toLowerCase())
    return when (type) {
        OrderType.DINE_IN -> {
            CacheUtil.getInstance().getTranslations(Text.DINE_IN, "Dine In") + ", "
        }

        OrderType.DELIVERY -> {
            CacheUtil.getInstance().getTranslations(Text.DELIVERED, "Delivered") + ", "
        }

        else -> {
            CacheUtil.getInstance().getTranslations(Text.PICKED, "Picked") + ", "
        }
    }
}

fun String.orderTypeString(): String {
    val type = OrderType.fromString(this.toLowerCase())
    when (type) {
        OrderType.PICKUP, OrderType.DRIVE_THRU -> {
            val headerText: String? = if (type == OrderType.DRIVE_THRU) {
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
    return this.formatOrderTime(false)
}

fun getCurrentTime(): String {
    return try {
        val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val currentTime = Date()
        dateFormat.format(currentTime)
    } catch (e: Exception) {
        ""
    }
}

fun String.todayOrYesterday(): String {
    try {
        var simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT+0")
        val timeValue = simpleDateFormat.parse(this)
        simpleDateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getDefault()
        val actualDate = timeValue?.let { simpleDateFormat.format(it) }.orEmpty()
        if (isToday(actualDate)) {
            return (CacheUtil.getInstance().getTranslations(Text.TODAY, "Today") + ", ")
        } else if (isYesterday(actualDate)) {
            return (CacheUtil.getInstance().getTranslations(Text.YESTERDAY, "Yesterday") + ", ")
        }
    } catch (e: ParseException) {
        e.printStackTrace()
    }
    return ""
}

fun String.formatOrderTime(forceIncludeYear: Boolean): String {
    return try {
        var simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT+0")
        val timeValue = simpleDateFormat.parse(this)
        simpleDateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getDefault()
        val actualDate = timeValue?.let { simpleDateFormat.format(it) }.orEmpty()

        simpleDateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getDefault()
        val finalTime = timeValue?.let { simpleDateFormat.format(it) }.orEmpty()
        simpleDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getDefault()
        val finalDay = timeValue?.let { simpleDateFormat.format(it) }.orEmpty()
        simpleDateFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getDefault()
        val finalYear = timeValue?.let { simpleDateFormat.format(it) }.orEmpty()

        val dateValues = mutableListOf<String>()
        if (!forceIncludeYear) {
            if (isToday(actualDate)) {
                dateValues.add(CacheUtil.getInstance().getTranslations(Text.TODAY, "Today") + ", ")
            } else if (isYesterday(actualDate)) {
                dateValues.add(
                    CacheUtil.getInstance().getTranslations(Text.YESTERDAY, "Yesterday") + ", "
                )
            }
        }

        dateValues.add("$finalDay, ")
        if (forceIncludeYear || !isSameYear(actualDate)) {
            dateValues.add("$finalYear, ")
        }
        dateValues.add(finalTime)

        var displayTime = ""
        for (i in dateValues) {
            displayTime += i
        }
        displayTime
    } catch (e: ParseException) {
        e.printStackTrace()
        return this.defaultTime()
    }
}

fun isToday(dateString: String): Boolean {
    return try {
        val inputFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        val calendarToday = Calendar.getInstance()
        val calendarGiven = Calendar.getInstance().apply {
            time = date
        }
        calendarToday.get(Calendar.YEAR) == calendarGiven.get(Calendar.YEAR) && calendarToday.get(
            Calendar.MONTH
        ) == calendarGiven.get(Calendar.MONTH) && calendarToday.get(Calendar.DAY_OF_MONTH) == calendarGiven.get(
            Calendar.DAY_OF_MONTH
        )
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}


fun isYesterday(dateString: String): Boolean {
    return try {
        val inputFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        val calendarGiven = Calendar.getInstance().apply {
            time = date
        }
        val calendarYesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -1) // Subtract one day
        }
        calendarGiven.get(Calendar.YEAR) == calendarYesterday.get(Calendar.YEAR) && calendarGiven.get(
            Calendar.MONTH
        ) == calendarYesterday.get(Calendar.MONTH) && calendarGiven.get(Calendar.DAY_OF_MONTH) == calendarYesterday.get(
            Calendar.DAY_OF_MONTH
        )
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}


fun isSameYear(dateString: String): Boolean {
    return try {
        val inputFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        val calendarGiven = Calendar.getInstance().apply {
            time = date
        }
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        calendarGiven.get(Calendar.YEAR) == currentYear
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
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

fun formatSecondsToMinutesAndSeconds(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

fun cancelTime(timestamp: String): Long {
    return try {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT+0")
        val timeValue = simpleDateFormat.parse(timestamp)
        val localTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        localTimeFormat.timeZone = TimeZone.getDefault()
        val actualDate = timeValue?.let { localTimeFormat.format(it) }.orEmpty()
        val calendar1 = Calendar.getInstance().apply {
            time = localTimeFormat.parse(actualDate)!!
        }
        val calendar2 = Calendar.getInstance()
        val differenceInMillis = abs(calendar2.timeInMillis - calendar1.timeInMillis)
        differenceInMillis / 1000
    } catch (e: Exception) {
        e.printStackTrace()
        return 0
    }
}

fun statusTime(timestamp: String): String {
    return try {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT+0")
        val timeValue = simpleDateFormat.parse(timestamp)
        val localTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        localTimeFormat.timeZone = TimeZone.getDefault()
        val actualDate = timeValue?.let { localTimeFormat.format(it) }.orEmpty()
        val calendar = Calendar.getInstance().apply {
            time = localTimeFormat.parse(actualDate)!!
        }
        val startTimeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
        return startTimeFormat.format(calendar.time)
    } catch (e: Exception) {
        e.printStackTrace()
        "--"
    }
}

fun showTime(timestamp: String, timeAdded: Int): String {
    try {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT+0")
        val timeValue = simpleDateFormat.parse(timestamp)
        val localTimeFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        localTimeFormat.timeZone = TimeZone.getDefault()
        val actualDate = timeValue?.let { localTimeFormat.format(it) }.orEmpty()
        val calendar = Calendar.getInstance().apply {
            time = localTimeFormat.parse(actualDate)!! // Parse actualDate back into a Date object
            add(Calendar.MINUTE, timeAdded)
        }
        val startTimeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
        val endTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val startTime = startTimeFormat.format(calendar.time)
        val endTime = endTimeFormat.format(calendar.apply { add(Calendar.MINUTE, 10) }.time)
        return "$startTime - $endTime"
    } catch (e: Exception) {
        e.printStackTrace()
        return ""
    }
}

fun dpToPx(dp: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().displayMetrics
    ).toInt()
}

fun deviceHeight(context: Activity): Int {
    val displayMetrics = DisplayMetrics()
    context.windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.heightPixels - dpToPx(300f)
}
