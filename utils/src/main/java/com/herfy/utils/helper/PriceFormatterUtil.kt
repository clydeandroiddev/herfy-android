package com.herfy.utils.helper

import android.text.SpannedString

object PriceFormatter {

    fun formatPriceWithLocale(originalPrice: Double, translatedName: String): String {
        return when {
            isEnglishOrFrench() -> "+${formatPrice(originalPrice)}"
            ArabicUtils.isArabic(translatedName) -> convertFormattedPriceToString(StringUtil.getFormattedPriceWithText(originalPrice))
            else -> convertFormattedPriceToString(StringUtil.getFormattedPriceWithText(originalPrice))
        }
    }

    private fun isEnglishOrFrench(): Boolean {
        return LocaleUtil.getInstance().isEnglish || LocaleUtil.getInstance().isFrench
    }

    private fun formatPrice(originalPrice: Double): SpannedString {
        return StringUtil.getFormattedPrice(originalPrice)
    }

    private fun convertFormattedPriceToString(formattedPrice: SpannedString): String {
        return formattedPrice.toString()
    }
}