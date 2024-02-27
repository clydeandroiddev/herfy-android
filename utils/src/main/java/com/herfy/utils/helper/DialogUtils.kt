package com.herfy.utils.helper

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import androidx.core.text.HtmlCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.herfy.utils.constants.Text

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
        title: String? = "",
        message:String?,
        body: String = "",
        positiveButtonText: String = CacheUtil.getInstance().getTranslations(Text.OK),
        negativeButtonText: String = CacheUtil.getInstance().getTranslations(Text.CANCEL),
        positiveClickListener: () -> Unit = {},
        negativeClickListener: () -> Unit = {}
    ) {
        val alertBuilder = MaterialAlertDialogBuilder(context)
        title?.isNotEmpty().let {
            alertBuilder.setTitle(title)
        }
        alertBuilder.setMessage(message.orEmpty())

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



    private var progressDialog: ProgressDialog? = null

    fun showProgressDialog(context: Context, cancelTouchOutside: Boolean = true, isLoading: Boolean) {
        if (isLoading) {
            if (progressDialog == null) {
                progressDialog = ProgressDialog(context)
                progressDialog?.setCanceledOnTouchOutside(cancelTouchOutside)
                progressDialog?.show()
            }
        } else {
            dismissProgressDialog()
        }
    }

   private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

}