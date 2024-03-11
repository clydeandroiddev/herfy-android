package com.herfy.utils.helper

import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import com.herfy.utils.constants.ISO_DATE_TIME_FORMAT_PATTERN
import java.util.Date
import java.util.Locale

object DateFormatterUtil {


    fun formatTimeAgo(dateString: String): String {
        val dateFormat = SimpleDateFormat(ISO_DATE_TIME_FORMAT_PATTERN, Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        val date = dateFormat.parse(dateString)
        val currentTime = Date()

        val timeDifference = currentTime.time - date.time

        val second = 1000L
        val minute = 60 * second
        val hour = 60 * minute
        val day = 24 * hour

        return when {
            timeDifference < second -> "just now"
            timeDifference < 2 * minute -> "1 minute ago"
            timeDifference < hour -> "${(timeDifference / minute).toInt()} minutes ago"
            timeDifference < 2 * hour -> "1 hour ago"
            timeDifference < day -> "${(timeDifference / hour).toInt()} hours ago"
            timeDifference < 2 * day -> "1 day ago"
            else -> "${(timeDifference / day).toInt()} days ago"
        }
    }
}