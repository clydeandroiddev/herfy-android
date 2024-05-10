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
// For kotlin classes
fun String.updateCurrency(currency: String, withSpace: Boolean): SpannedString {
    val space = if (withSpace) " " else ""
    val concept = CacheUtil.getInstance().concept
    return when (concept.currencyFormat) {
        CurrencyFormat.currencySymbolAmount -> buildSpannedString {
            scale(0.58f) {
                concept.currencySymbol
            }
            append(space)
            append(StringUtil.localize(this@updateCurrency))
        }
        CurrencyFormat.currencyCodeAmount -> buildSpannedString {
            scale(0.58f) {
                concept.currencyCode
            }
            append(space)
            append(StringUtil.localize(this@updateCurrency))
        }
        CurrencyFormat.amountCurrencySymbol -> buildSpannedString {
            append(StringUtil.localize(this@updateCurrency))
            append(space)
            scale(0.58f) {
                concept.currencySymbol
            }
        }
        CurrencyFormat.amountCurrencyCode -> buildSpannedString {
            append(StringUtil.localize(this@updateCurrency))
            append(space)
            scale(0.58f) {
                concept.currencyCode
            }
        }
    }
}

// For non-kotlin classes
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
            withSpace: Boolean = false,
            ignoreDecimalPlaces: Boolean = false,
            isDiscount: Boolean = false,
            ignoreScale: Boolean = false,
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
                    scale(scaleRatio) {
                        append(CacheUtil.getInstance().getTranslations(concept.currencySymbol))
                    }
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
        @JvmOverloads
        fun updateCurrencyWithText(
            amount: Double,
            withSpace: Boolean = false
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
            val space = if (withSpace) " " else ""
            return when (concept.currencyFormat) {
                CurrencyFormat.currencySymbolAmount -> buildSpannedString {
                    append(StringUtil.localize(formattedDecimal))
                    append(space)
                    scale(0.58f) {
                        append(CacheUtil.getInstance().getTranslations(concept.currencySymbol))
                    }
                }
                CurrencyFormat.currencyCodeAmount -> buildSpannedString {
                    append(StringUtil.localize(formattedDecimal))
                    append(space)
                    scale(0.58f) {
                        append(CacheUtil.getInstance().getTranslations(concept.currencyCode))
                    }
                }
                CurrencyFormat.amountCurrencySymbol -> buildSpannedString {
                    scale(0.58f) {
                        append(CacheUtil.getInstance().getTranslations(concept.currencySymbol))
                    }
                    append(space)
                    append(StringUtil.localize(formattedDecimal))
                }
                CurrencyFormat.amountCurrencyCode -> buildSpannedString {
                    scale(0.58f) {
                        append(CacheUtil.getInstance().getTranslations(concept.currencyCode))
                    }
                    append(space)
                    append(StringUtil.localize(formattedDecimal))
                }
            }
        }

        @JvmOverloads
        fun updateAmountWithCurrency(amount: Double, withSpace: Boolean = false): SpannedString {
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
            val space = if (withSpace) " " else ""
            val decimalFormat = DecimalFormat(pattern, symbols)
            val formattedDecimal: String = decimalFormat.format(amount)
            return buildSpannedString {
                append(StringUtil.localize(formattedDecimal))
                append(space)
                append(CacheUtil.getInstance().getTranslations(concept.currencySymbol))
            }
        }

    }

}