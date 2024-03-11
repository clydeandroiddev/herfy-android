package com.herfy.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.herfy.R
import com.herfy.databinding.AmountViewBinding
import com.herfy.util.StringUtil.localize
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.skylinedynamics.solosdk.api.models.objects.CurrencyFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale


@ExperimentalStdlibApi
class AmountTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    val concept = CacheUtil.getInstance().concept

    var amount: Amount = Amount(0.0, "SR", Locale(LocaleUtil.getInstance().language))
        set(value) {
            field = value

            val symbols = DecimalFormatSymbols(value.locale)

            val pattern = if (concept.decimalPlaces > 0) {
                "#,##0.${"0".repeat(concept.decimalPlaces)}"
            } else {
                "#"
            }

            val decimalFormat = DecimalFormat(pattern, symbols)
            val formattedDecimal: String = decimalFormat.format(value.amount)

            binding.midText.text = localize(formattedDecimal)

            binding.endText.isVisible = false
            binding.endSpace.isVisible = false
            binding.startText.text = when(concept.currencyFormat) {
                CurrencyFormat.currencySymbolAmount -> concept.currencyCode
                CurrencyFormat.currencyCodeAmount -> concept.currencyCode
                else -> ""
            }

        }

    fun amountTextView(): TextView {
        return binding.midText
    }

    fun currencyTextView(): TextView {
        return binding.startText
    }

    private val binding: AmountViewBinding

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = AmountViewBinding.inflate(inflater, this, true)

        context.withStyledAttributes(attrs, R.styleable.AmountView) {
            getDimension(R.styleable.AmountView_amountTextSize, 26f).let {
                binding.midText.setTextSize(TypedValue.COMPLEX_UNIT_PX, it)
            }
            getDimension(R.styleable.AmountView_currencyTextSize, 18f).let {
                binding.endText.setTextSize(TypedValue.COMPLEX_UNIT_PX, it)
                binding.startText.setTextSize(TypedValue.COMPLEX_UNIT_PX, it)
            }

            getColor(R.styleable.AmountView_amountTextColor, Color.BLACK).let {
                binding.midText.setTextColor(it)
            }

            getColor(R.styleable.AmountView_currencyTextColor, Color.BLACK).let {
                binding.midText.setTextColor(it)
            }
        }
    }
}

data class Amount(
    val amount: Double,
    val symbol: String,
    val locale: Locale
)