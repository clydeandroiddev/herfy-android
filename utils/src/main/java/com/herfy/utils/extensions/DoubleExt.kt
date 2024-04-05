package com.herfy.utils.extensions

fun Double.hasDecimalPart(): Boolean {
    return this % 1.0 != 0.0
}
