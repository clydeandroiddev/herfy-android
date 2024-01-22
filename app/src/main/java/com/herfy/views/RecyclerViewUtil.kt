package com.herfy.views

import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

object RecyclerViewUtil {

    @JvmStatic
    fun smoothSnapToPosition(recyclerView: RecyclerView, position: Int, snapMode: Int = LinearSmoothScroller.SNAP_TO_START) {
        val scrollDuration = 1500f
        val smoothScroller = object : LinearSmoothScroller(recyclerView.context) {
            override fun getVerticalSnapPreference(): Int = snapMode
            override fun getHorizontalSnapPreference(): Int = snapMode
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                return scrollDuration / recyclerView.computeVerticalScrollRange()
            }
        }
        smoothScroller.targetPosition = position
        recyclerView.layoutManager?.startSmoothScroll(smoothScroller)
    }

}