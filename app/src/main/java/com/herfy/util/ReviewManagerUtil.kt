package com.herfy.util

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import com.herfy.utils.helper.CacheUtil


class ReviewManagerUtil(val activity: Activity) {

    fun showReviewDialog() {
        if (StateUtil.INSTANCE.hasShownRating) return
        StateUtil.INSTANCE.hasShownRating = true
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { requestReview ->
            if (requestReview.isSuccessful) {
                CacheUtil.getInstance().appReview = true
                val reviewInfo = requestReview.result

                val reviewFlow = manager.launchReviewFlow(activity, reviewInfo)
                reviewFlow.addOnCompleteListener {

                }
                reviewFlow.addOnFailureListener {

                }
            } else {

            }
        }.addOnFailureListener {

        }
    }
}