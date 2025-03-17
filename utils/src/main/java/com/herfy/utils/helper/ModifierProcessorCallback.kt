package com.herfy.utils.helper

import android.text.SpannedString
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierItemAttributes

interface ModifierProcessorCallback {
    fun onModifierProcessed(name: String, formattedPrice: String)
}

class ModifierProcessor(private val callback: ModifierProcessorCallback) {

    private lateinit var attributes: ModifierItemAttributes

    fun processModifier(modifierItem: ModifierItem) {
        attributes = modifierItem.attributes

        val originalPrice = attributes.price
        val translatedName = attributes.translatedName.getValue(LocaleUtil.getInstance().language)

        val formattedPrice = formatPriceBasedOnLocale(originalPrice, translatedName)
        val text = createFormattedText(originalPrice, formattedPrice)
        if (originalPrice != 0.0) {
            callback.onModifierProcessed(translatedName, formattedPrice)
        }else {
            callback.onModifierProcessed(translatedName, "")
        }
     }

    private fun createFormattedText(originalPrice: Double, formattedPrice: String): String {
        return if (originalPrice <= 0) "" else "<font color='#D62300'>$formattedPrice</font>"
    }

    private fun formatPriceBasedOnLocale(originalPrice: Double, translatedName: String): String {
        return when {
            isEnglishOrFrench() -> "+${getFormattedPrice(originalPrice)}"
            ArabicUtils.isArabic(translatedName) -> convertFormattedPriceAsString(StringUtil.getFormattedPrice(originalPrice))
            else -> convertFormattedPriceAsString(StringUtil.getFormattedPrice(originalPrice))
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