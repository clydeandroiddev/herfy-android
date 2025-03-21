package com.herfy.utils.helper

import android.text.SpannedString
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import com.herfy.utils.extensions.hasDecimalPart
import com.skylinedynamics.solosdk.api.models.objects.CurrencyFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

internal const val ONE_RATIO_PROPOTION = 1F
internal const val SCALE_VALUE = 0.58f

class CurrencyUtils {

    companion object {

        @JvmOverloads
        fun getFormattedAmount(amount: Double, ignoreDecimalPlaces: Boolean = false): String {
            val concept = CacheUtil.getInstance().concept
            val symbols = DecimalFormatSymbols(LocaleUtil.getLocale())

            val pattern = if(amount.hasDecimalPart()) {
                if (concept.decimalPlaces > 0) {
                    "#,##0.${"0".repeat(concept.decimalPlaces)}"
                } else {
                    "#,##0.${"0".repeat(2)}"
                }
            } else {
                "#"
            }

            val decimalFormat = DecimalFormat(pattern, symbols)
            return decimalFormat.format(amount)
        }

        @JvmOverloads
        fun updateCurrency(
            amount: Double,
            withSpace: Boolean = true,
            ignoreDecimalPlaces: Boolean = false,
            isDiscount: Boolean = false,
            ignoreScale: Boolean = true
        ): SpannedString {

            val concept = CacheUtil.getInstance().concept
            val symbols = DecimalFormatSymbols(LocaleUtil.getLocale())

            val pattern = if(amount.hasDecimalPart()) {
                if (concept.decimalPlaces > 0) {
                    "#,##0.${"0".repeat(concept.decimalPlaces)}"
                } else {
                    "#,##0.${"0".repeat(2)}"
                }
            } else {
                "#"
            }

            val decimalFormat = DecimalFormat(pattern, symbols)
            val formattedDecimal: String = decimalFormat.format(amount)
            val scaleRatio = getScaleRatio(ignoreScale)
            val discount = if (isDiscount) "-" else ""
            val space = if (withSpace) " " else ""
            return when (concept.currencyFormat) {
                CurrencyFormat.currencySymbolAmount -> buildSpannedString {
                    append(discount)
                    append(space)
                    append("\u200F") // Force SAR symbol to be treated as RTL
                    scale(scaleRatio) {
                        append(CacheUtil.getInstance().getTranslations(concept.currencySymbol))
                    }
                    append(space)
                    append("\u200E") // Force number to be treated as LTR
                    append(StringUtil.localize(formattedDecimal))
                }
                CurrencyFormat.currencyCodeAmount -> buildSpannedString {
                    append(discount)
                    append(space)
                    scale(scaleRatio) {
                        append(CacheUtil.getInstance().getTranslations(concept.currencyCode))

                    }
                    append(space)
                    append(StringUtil.localize(formattedDecimal))
                }
                CurrencyFormat.amountCurrencySymbol -> buildSpannedString {
                    append(discount)
                    append(StringUtil.localize(formattedDecimal))
                    append(space)
                    scale(scaleRatio) {
                        append(CacheUtil.getInstance().getTranslations(concept.currencySymbol))
                    }
                }
                CurrencyFormat.amountCurrencyCode -> buildSpannedString {
                    append(discount)
                    append(StringUtil.localize(formattedDecimal))
                    append(space)
                    scale(scaleRatio) {
                        append(CacheUtil.getInstance().getTranslations(concept.currencyCode))
                    }
                }
            }
        }
        private fun getScaleRatio(ignoreScale: Boolean): Float {
            return if (ignoreScale) ONE_RATIO_PROPOTION else SCALE_VALUE
        }

    }

}