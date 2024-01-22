package com.herfy.util.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.herfy.R


fun Context.openChromeTab(url: String?) {
    try {
        val builder = CustomTabsIntent.Builder()
        builder.setToolbarColor(ContextCompat.getColor(this, R.color.white))
        builder.setShowTitle(true)
        builder.build().launchUrl(this, Uri.parse(url))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
fun Context.dialPhoneNumber(number: String) {
    return try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Context.sendEmail(email: String) {
    if (email.isValidEmail()) {
        try {
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}