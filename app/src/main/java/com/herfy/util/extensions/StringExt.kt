package com.herfy.util.extensions

import android.util.Patterns

fun String.removeSpecialCharacters(): String {
    return replace("[^\\d]".toRegex(), "")
}

fun String.isValidEmail() =
    isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

