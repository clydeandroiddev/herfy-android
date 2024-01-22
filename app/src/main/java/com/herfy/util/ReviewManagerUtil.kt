package com.herfy.util

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

class ReviewManagerUtil(val activity: Activity) {

    fun showReviewDialog() {
        if (StateUtil.INSTANCE.hasShownRating) return
        StateUtil.INSTANCE.hasShownRating = true
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { request ->
            if (request.isSuccessful) {
                val reviewInfo = request.result

                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { _ ->

                }
            } else {

            }
        }
    }
}