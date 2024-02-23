package com.herfy.common

import android.annotation.SuppressLint
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
