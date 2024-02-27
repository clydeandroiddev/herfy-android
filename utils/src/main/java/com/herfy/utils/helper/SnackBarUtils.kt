package com.herfy.utils.helper

import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.herfy.utils.R
import com.herfy.utils.extensions.SNACKBAR_DURATION

object SnackBarUtils {

    fun showGenericSuccessSnackBar(
        parentView: View,
        text: String
    ) {
        showSuccessSnackBar(
            parentView = parentView,
            text = text,
            actionText = text
        ) {
            it.dismiss()
        }
    }

    fun showSuccessSnackBar(
        parentView: View,
        text: String,
        actionText: String,
        actionClickListener: (Snackbar) -> Unit
    ): Snackbar {
        return Snackbar.make(parentView, text, SNACKBAR_DURATION)
            .apply {
                setAction(actionText) {
                    actionClickListener(this)
                }
                setTextColor(
                    ContextCompat.getColor(parentView.context, R.color.colorAlertSuccess)
                )
                setActionTextColor(
                    ContextCompat.getColor(parentView.context, R.color.colorOnAlertSuccess)
                )
                setBackgroundTint(
                    ContextCompat.getColor(parentView.context, R.color.colorOnAlertSuccess)
                )
            }.also {
                it.show()
            }
    }
}