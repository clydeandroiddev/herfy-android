package com.herfy.utils.helper

import android.content.Context
import androidx.compose.ui.res.stringResource
import com.herfy.utils.R
import com.herfy.utils.constants.Text

object MessagingUtil {

    fun Context.whatsAppMessage(orderId : String, storeName : String): String {
        val msg = StringBuilder()
        AuthUtil.getInstance().customer?.attributes?.apply {
            msg.append("Customer Name: $firstName $lastName")
        }
        msg.append("\n")
        val preferredLanguage = when {
            LocaleUtil.getInstance().isArabic -> resources.getString(R.string.arabic_language)
            LocaleUtil.getInstance().isFrench -> resources.getString(R.string.french_language)
            else -> resources.getString(R.string.english_language)
        }
        msg.append("Preferred Language: $preferredLanguage")
        msg.append("\n")
        AuthUtil.getInstance().customer?.attributes?.apply {
            msg.append("Customer Mobile Number: $mobile")
        }
        msg.append("\n")
        msg.append("Order Number: $orderId")
        msg.append("\n")
        msg.append("Store Name: $storeName")
        return msg.toString()
    }


}