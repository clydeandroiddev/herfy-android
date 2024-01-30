package com.herfy.util

import android.text.SpannedString
import com.herfy.productpagedetails.ui.viewholder.util.ArabicUtils
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierItemAttributes

interface ModifierProcessorCallback {
    fun onModifierProcessed(name: String, formattedPrice: String, text: String)
}

class ModifierProcessor(private val callback: ModifierProcessorCallback) {

    private lateinit var attributes: ModifierItemAttributes

    fun processModifier(modifierItem: ModifierItem) {
        attributes = modifierItem.attributes

        val originalPrice = attributes.price
        val translatedName = attributes.translatedName.getValue(LocaleUtil.getInstance().language)

        val formattedPrice = formatPriceBasedOnLocale(originalPrice, translatedName)

        val text = createFormattedText(originalPrice, formattedPrice)

        callback.onModifierProcessed(translatedName, formattedPrice, text)
    }

    private fun createFormattedText(originalPrice: Double, formattedPrice: String): String {
        return if (originalPrice <= 0) "" else "<font color='#D62300'>$formattedPrice</font>"
    }

    private fun formatPriceBasedOnLocale(originalPrice: Double, translatedName: String): String {
        return when {
            isEnglishOrFrench() -> "+${getFormattedPrice(originalPrice)}"
            ArabicUtils.isArabic(translatedName) -> convertFormattedPriceAsString(StringUtil.getFormattedPriceWithText(originalPrice))
            else -> convertFormattedPriceAsString(getFormattedPrice(originalPrice))
        }
    }
    private fun isEnglishOrFrench(): Boolean {
        return LocaleUtil.getInstance().isEnglish || LocaleUtil.getInstance().isFrench
    }
    private fun getFormattedPrice(originalPrice: Double): SpannedString {
        return StringUtil.getFormattedPrice(originalPrice)
    }

    private fun convertFormattedPriceAsString(formattedPrice: SpannedString): String {
        return formattedPrice.toString()
    }

}