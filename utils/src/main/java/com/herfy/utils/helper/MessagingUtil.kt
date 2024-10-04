package com.herfy.utils.helper

import android.content.Context
import androidx.compose.ui.res.stringResource
import com.herfy.utils.R
import com.herfy.utils.constants.Text

object MessagingUtil {

    fun Context.whatsAppMessage(orderId : String, storeName : String): String {
        val msg = StringBuilder()
        AuthUtil.getInstance().customer?.attributes?.apply {
            msg.append("${CacheUtil.getInstance().getTranslations(Text.CUSTOMER_NAME, "Customer Name:")} $firstName $lastName")
        }
        msg.append("\n")
        val preferredLanguage = when {
            LocaleUtil.getInstance().isArabic -> resources.getString(R.string.arabic_language)
            LocaleUtil.getInstance().isFrench -> resources.getString(R.string.french_language)
            else -> resources.getString(R.string.english_language)
        }
        msg.append("${CacheUtil.getInstance().getTranslations(Text.PREFERRED_LANGUAGE, "Preferred Language:")} $preferredLanguage")
        msg.append("\n")
        AuthUtil.getInstance().customer?.attributes?.apply {
            msg.append("${CacheUtil.getInstance().getTranslations(Text.CUSTOMER_MOBILE_NUMBER, "Customer Mobile Number:")} $mobile")
        }
        msg.append("\n")
        msg.append("${CacheUtil.getInstance().getTranslations(Text.ORDER_NUMBER, "Order Number:")} $orderId")
        msg.append("\n")
        msg.append("${CacheUtil.getInstance().getTranslations(Text.STORE_NAME, "Store Name:")} $storeName")
        return msg.toString()
    }


}