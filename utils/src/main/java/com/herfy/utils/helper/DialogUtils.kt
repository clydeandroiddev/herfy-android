package com.herfy.utils.helper

import android.content.Context
import android.content.DialogInterface
import androidx.core.text.HtmlCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object DialogUtils {

    fun showAlertDialog(
        context: Context,
        title: String?,
        body: String,
        dialogInterface: (DialogInterface, Int) -> Unit
    ) {
        if (body.isEmpty()) {
            return
        }

        val alertBuilder = MaterialAlertDialogBuilder(context)
        alertBuilder.setTitle(title)
        alertBuilder.setMessage(HtmlCompat.fromHtml(body, HtmlCompat.FROM_HTML_MODE_LEGACY))
        alertBuilder.setCancelable(false)
        alertBuilder.setPositiveButton(context.getString(android.R.string.ok), dialogInterface)
        alertBuilder.create().show()
    }
    fun showDialog(
        context: Context,
        title: String?,
        body: String,
        dialogInterface: (DialogInterface, Int) -> Unit,
        positiveButtonText: String,
        negativeButtonText: String,
    ) {
        if (body.isEmpty()) {
            return
        }

        val alertBuilder = MaterialAlertDialogBuilder(context)
        alertBuilder.setTitle(title)
        alertBuilder.setMessage(HtmlCompat.fromHtml(body, HtmlCompat.FROM_HTML_MODE_LEGACY))
        alertBuilder.setCancelable(false)
        alertBuilder.setPositiveButton(positiveButtonText, dialogInterface)
        alertBuilder.setNegativeButton(negativeButtonText, dialogInterface)
        alertBuilder.create().show()
    }
    fun showConfirmDialog(
        context: Context,
        title: String = "",
        body: String,
        positiveButtonText: String,
        negativeButtonText: String = "",
        positiveClickListener: () -> Unit = {},
        negativeClickListener: () -> Unit = {}
    ) {
        val alertBuilder = MaterialAlertDialogBuilder(context)
        title.isNotEmpty().let {
            alertBuilder.setTitle(title)
        }
        alertBuilder.setMessage(HtmlCompat.fromHtml(body, HtmlCompat.FROM_HTML_MODE_LEGACY))

        alertBuilder
            .setPositiveButton(
                positiveButtonText
            ) { _, _ ->
                positiveClickListener()
            }

        if (negativeButtonText.isNotEmpty()) {
            alertBuilder
                .setNegativeButton(
                    negativeButtonText
                ) { _, _ ->
                    negativeClickListener()
                }
        }

        alertBuilder.setCancelable(false)
        alertBuilder.create().show()
    }
}