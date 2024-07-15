package com.herfy.utils.helper

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.herfy.utils.R
import com.herfy.utils.extensions.SNACKBAR_DURATION

object SnackBarUtils {

    fun showGenericSuccessSnackBar(
        parentView: View, text: String
    ) {
        showSuccessSnackBar(
            parentView = parentView, text = text, actionText = text
        ) {
            it.dismiss()
        }
    }

    fun showSuccessSnackBar(
        parentView: View, text: String, actionText: String, actionClickListener: (Snackbar) -> Unit
    ): Snackbar {
        return Snackbar.make(parentView, text, SNACKBAR_DURATION).apply {
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

    @SuppressLint("RestrictedApi")
    fun showCustomSnackBar(
        parentView: View, title: String, showImage: Boolean, background: Int, textColor: Int
    ) {
        val snackBar = Snackbar.make(parentView, "", Snackbar.LENGTH_SHORT)
        val snackBarLayout = snackBar.view as Snackbar.SnackbarLayout
        snackBar.view.setBackgroundColor(Color.TRANSPARENT)

        val inflater = LayoutInflater.from(parentView.context)
        val customView: View = inflater.inflate(R.layout.snack_custom, null)
        val titleText = customView.findViewById<TextView>(R.id.title)
        val main = customView.findViewById<ConstraintLayout>(R.id.main_layout)
        val image = customView.findViewById<ImageView>(R.id.image)

        titleText.text = title
        image.isVisible = showImage
        main.setBackgroundResource(background)
        titleText.setTextColor(textColor)
        snackBarLayout.addView(customView, 0)

        snackBar.show()
    }
}