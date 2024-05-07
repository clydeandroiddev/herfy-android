package com.herfy.common

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Resources
import android.graphics.Color
import android.util.DisplayMetrics
import android.util.TypedValue
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails
import com.skylinedynamics.solosdk.api.models.objects.OrderStatusType
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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


fun String.orderDetailsType(sequence: Int): String {
    if (sequence >= OrderStatusType.CANCELLED_BY_CUSTOMER.value && sequence <= OrderStatusType.CANCELLED_BY_EMPLOYEE.value) {
        return CacheUtil.getInstance().getTranslations(Text.CANCELLED, "Cancelled") + ", "
    } else if (sequence > OrderStatusType.CANCELLED_BY_EMPLOYEE.value) {
        return CacheUtil.getInstance().getTranslations(Text.FAILED, "Failed") + ", "
    } else {
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
}

fun getDisplayId(order: OrderDetails): String {
    var displayId = order.displayId
    if (displayId == null || displayId.isEmpty() || displayId.equals(
            "null", ignoreCase = true
        )
    ) {
        displayId = order.code
    }
    if (displayId == null || displayId.isEmpty() || displayId.equals(
            "null", ignoreCase = true
        )
    ) {
        displayId = order.id
    }
    return displayId
}

fun String.orderTypeStatus(): String {
    return when (OrderType.fromString(this.toLowerCase(Locale.ROOT))) {
        OrderType.DELIVERY -> {
            CacheUtil.getInstance().getTranslations(Text.DELIVERED, "Delivered")
        }

        else -> {
            CacheUtil.getInstance().getTranslations(Text.PICKED_UP, "Picked up")
        }
    }
}

fun String.orderTypeStatusIcon(): Int {
    return when (OrderType.fromString(this.toLowerCase(Locale.ROOT))) {
        OrderType.DELIVERY -> {
            R.drawable.ic_checkbox_input_green
        }

        else -> {
            R.drawable.ic_checkbox_input_blue
        }
    }
}

fun String.orderTypeStatusColor(): Int {
    return when (OrderType.fromString(this.toLowerCase(Locale.ROOT))) {
        OrderType.DELIVERY -> {
            R.color.delivered_color
        }

        else -> {
            R.color.picked_color
        }
    }
}

fun String.orderTypeString(): String {
    when (OrderType.fromString(this.toLowerCase(Locale.ROOT))) {
        OrderType.PICKUP, OrderType.DRIVE_THRU -> {
            return CacheUtil.getInstance().getTranslations(Text.PICKUP, "Pickup")
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

fun String.getPromisedMinutes(): Long {
    return try {
        var simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT+0")
        val timeValue = simpleDateFormat.parse(this)
        simpleDateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getDefault()
        val promisedDate = timeValue?.let { simpleDateFormat.format(it) }.orEmpty()
        val c = Calendar.getInstance().time
        val df = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val actualDate = df.format(c)
        val dateFormat = "dd/MM/yyyy hh:mm a"
        val sdf = SimpleDateFormat(dateFormat, Locale.getDefault())
        val startDate = sdf.parse(actualDate)
        val endDate = sdf.parse(promisedDate)
        val startTimeMillis = startDate?.time ?: 0
        val endTimeMillis = endDate?.time ?: 0
        return (endTimeMillis - startTimeMillis) / (1000 * 60)
    } catch (e: ParseException) {
        e.printStackTrace()
        0
    }
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
    return displayMetrics.heightPixels - dpToPx(310f)
}

fun drawArcPolyline(googleMap: GoogleMap?, start: LatLng, end: LatLng, numPoints: Int): Polyline? {
    val polylineOptions = PolylineOptions()
    val fraction = 1.0 / numPoints

    for (i in 0..numPoints) {
        val t = fraction * i
        val lat = start.latitude * (1 - t) + end.latitude * t
        val lng = start.longitude * (1 - t) + end.longitude * t
        // Adding an elevation factor based on the sine function to create an arc effect
        val altLat = lat + 3.5 * sin(Math.PI * t)  // Increase 0.1 to increase the height of the arc
        polylineOptions.add(LatLng(altLat, lng))
    }
    return googleMap?.addPolyline(polylineOptions.width(8f).color(Color.parseColor("#0064AC")))
}
