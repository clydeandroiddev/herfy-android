package com.herfy.utils.extensions

import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.databinding.BindingAdapter
import com.herfy.utils.helper.throttleFirst
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import reactivecircus.flowbinding.android.view.clicks
import kotlin.time.Duration.Companion.milliseconds
const val NINJA_TAP_THROTTLE_TIME = 400L
// to prevent duplicate click
inline fun View.ninjaTap(scope: CoroutineScope, crossinline action: (View) -> Unit) {
    scope.launch {
        ninjaTap(action)
    }
}

/**
 * Suspend version of [View.ninjaTap]. Should be called in its own coroutine builder ([launch]) as
 * this suspends indefinitely.
 */
suspend inline fun View.ninjaTap(crossinline action: (View) -> Unit) {
    this@ninjaTap.clicks()
        .throttleFirst(NINJA_TAP_THROTTLE_TIME.milliseconds)
        .collect {
            action.invoke(this@ninjaTap)
        }
}
@BindingAdapter("setVisible")
infix fun View.setVisible(isVisible: Boolean?) {
    if (isVisible == true && visibility == View.VISIBLE) {
        return
    }

    if (isVisible == false && visibility == View.GONE) {
        return
    }

    this.visibility = when (isVisible) {
        false -> View.GONE
        else -> View.VISIBLE
    }
}

fun View.expand() {
    measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    val targtetHeight = measuredHeight
    layoutParams.height = 0
    visibility = View.VISIBLE
    val a: Animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            layoutParams.height =
                if (interpolatedTime == 1f) ViewGroup.LayoutParams.WRAP_CONTENT else (targtetHeight * interpolatedTime).toInt()
            requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }
    a.duration = (targtetHeight / context.resources.displayMetrics.density).toInt().toLong()
    startAnimation(a)
}

fun View.collapse() {
    val initialHeight = measuredHeight
    val a: Animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            if (interpolatedTime == 1f) {
                visibility = View.GONE
            } else {
                layoutParams.height = initialHeight - (initialHeight * interpolatedTime).toInt()
                requestLayout()
            }
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }
    a.duration = (initialHeight / context.resources.displayMetrics.density).toInt().toLong()
    startAnimation(a)
}
