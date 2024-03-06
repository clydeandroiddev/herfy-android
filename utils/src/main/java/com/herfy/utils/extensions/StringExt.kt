package com.herfy.utils.extensions

import android.util.Patterns
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun String.removeSpecialCharacters(): String {
    return replace("[^\\d]".toRegex(), "")
}

fun String.isValidEmail() =
    isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun String.formatDate(format : String): String {
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
        val targetFormat = SimpleDateFormat(format, Locale.US)
        targetFormat.format(date)
    } catch (pe: ParseException) {
        this
    }
}


