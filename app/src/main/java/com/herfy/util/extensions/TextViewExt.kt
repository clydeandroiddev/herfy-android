package com.herfy.util.extensions

import android.widget.TextView
import androidx.annotation.DrawableRes
const val DEFAULT_POSITION_VALUE = 0
fun TextView.setDrawable(
    @DrawableRes left: Int? = null,
    @DrawableRes top: Int? = null,
    @DrawableRes right: Int? = null,
    @DrawableRes bottom: Int? = null
) {
    setCompoundDrawablesWithIntrinsicBounds(
        left ?: DEFAULT_POSITION_VALUE,
        top ?: DEFAULT_POSITION_VALUE,
        right ?: DEFAULT_POSITION_VALUE,
        bottom ?: DEFAULT_POSITION_VALUE
    )
}