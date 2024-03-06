package com.herfy.util

import android.text.SpannedString
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import com.herfy.ext.hasDecimalPart
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.skylinedynamics.solosdk.api.models.objects.CurrencyFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

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

            val pattern = if(ignoreDecimalPlaces) {
                if(amount.hasDecimalPart()) {
                    "#,##0.${"0".repeat(2)}"
                } else {
                    "#"
                }
            } else {
                if (concept.decimalPlaces > 0) {
                    "#,##0.${"0".repeat(concept.decimalPlaces)}"
                } else {
                    "#"
                }
            }

            val decimalFormat = DecimalFormat(pattern, symbols)
            return decimalFormat.format(amount)
        }

        @JvmOverloads
        fun updateCurrency(
            amount: Double,
            withSpace: Boolean = false,
            ignoreDecimalPlaces: Boolean = false,
            isDiscount: Boolean = false
        ): SpannedString {

            val concept = CacheUtil.getInstance().concept
            val symbols = DecimalFormatSymbols(LocaleUtil.getLocale())

            val pattern = if(ignoreDecimalPlaces) {
                if(amount.hasDecimalPart()) {
                    "#,##0.${"0".repeat(2)}"
                } else {
                    "#"
                }
            } else {
                if (concept.decimalPlaces > 0) {
                    "#,##0.${"0".repeat(concept.decimalPlaces)}"
                } else {
                    "#"
                }
            }

            val decimalFormat = DecimalFormat(pattern, symbols)
            val formattedDecimal: String = decimalFormat.format(amount)

            val discount = if (isDiscount) "-" else ""
            val space = if (withSpace) " " else ""
            return when (concept.currencyFormat) {
                CurrencyFormat.currencySymbolAmount -> buildSpannedString {
                    append(discount)
                    append(CacheUtil.getInstance().getTranslations(concept.currencySymbol))
                    append(space)
                    append(StringUtil.localize(formattedDecimal))
                }
                CurrencyFormat.currencyCodeAmount -> buildSpannedString {
                    append(discount)
                    append(CacheUtil.getInstance().getTranslations(concept.currencyCode))
                    append(space)
                    append(StringUtil.localize(formattedDecimal))
                }
                CurrencyFormat.amountCurrencySymbol -> buildSpannedString {
                    append(discount)
                    append(StringUtil.localize(formattedDecimal))
                    append(space)
                    append(CacheUtil.getInstance().getTranslations(concept.currencySymbol))
                }
                CurrencyFormat.amountCurrencyCode -> buildSpannedString {
                    append(discount)
                    append(StringUtil.localize(formattedDecimal))
                    append(space)
                    append(CacheUtil.getInstance().getTranslations(concept.currencyCode))
                }
            }
        }

        @JvmOverloads
        fun updateCurrencyWithText(
            amount: Double,
            withSpace: Boolean = false
        ): SpannedString {

            val concept = CacheUtil.getInstance().concept
            val symbols = DecimalFormatSymbols(LocaleUtil.getLocale())

            val pattern = if (concept.decimalPlaces > 0) {
                "#,##0.${"0".repeat(concept.decimalPlaces)}"
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
                    append(CacheUtil.getInstance().getTranslations(concept.currencySymbol))
                }
                CurrencyFormat.currencyCodeAmount -> buildSpannedString {
                    append(StringUtil.localize(formattedDecimal))
                    append(space)
                    append(CacheUtil.getInstance().getTranslations(concept.currencyCode))
                }
                CurrencyFormat.amountCurrencySymbol -> buildSpannedString {
                    append(CacheUtil.getInstance().getTranslations(concept.currencySymbol))
                    append(space)
                    append(StringUtil.localize(formattedDecimal))
                }
                CurrencyFormat.amountCurrencyCode -> buildSpannedString {
                    append(CacheUtil.getInstance().getTranslations(concept.currencyCode))
                    append(space)
                    append(StringUtil.localize(formattedDecimal))
                }
            }
        }

        @JvmOverloads
        fun updateAmountWithCurrency(amount: Double, withSpace: Boolean = false): SpannedString {
            val concept = CacheUtil.getInstance().concept
            val symbols = DecimalFormatSymbols(LocaleUtil.getLocale())
            val pattern = if (concept.decimalPlaces > 0) {
                "#,##0.${"0".repeat(concept.decimalPlaces)}"
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