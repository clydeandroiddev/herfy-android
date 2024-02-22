package com.herfy.common

import android.annotation.SuppressLint
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

@SuppressLint("SimpleDateFormat")
fun String.formatDate(): String {
    return try {
        var format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val newDate = format.parse(this)
        format = SimpleDateFormat("MMM dd, yyyy")
        format.format(newDate.toString())
    } catch (pe: ParseException) {
        this
    }
}
