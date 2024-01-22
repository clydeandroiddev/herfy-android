package com.herfy.ext

import android.view.View

fun View.isVisible(): Boolean {
    return this.visibility == View.VISIBLE
}

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}