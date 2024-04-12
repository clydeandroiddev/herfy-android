package com.herfy.common.base

import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.herfy.common.R
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.ViewUtil
import java.util.Locale

open class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var promoDialog: AlertDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (activity != null) {
            LocaleUtil.getInstance().setLocale(activity)
            val locale = LocaleUtil.getInstance().createDefaultLocale()
            Locale.setDefault(locale)
            val cfg = Configuration()
            cfg.locale = locale
            activity?.resources?.updateConfiguration(cfg, null)
        }
    }


   /* override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        val anim = if (enter) AnimationUtils.loadAnimation(
            activity,
            R.anim.fade_in
        ) else AnimationUtils.loadAnimation(
            activity, R.anim.fade_out
        )
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(anim)
        return animationSet
    }*/

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //ViewUtil.overrideFonts(view);
        initViews()
        initTranslations()
    }

    open fun initViews() {

    }

    open fun initTranslations() {

    }

    open fun showPromptAlertDialog(
        title: String?,
        message: String?,
        positive: String,
        positiveOnClickListener: DialogInterface.OnClickListener?,
        negative: String,
        negativeOnClickListener: DialogInterface.OnClickListener?
    ) {
        dismissPromptDialog()
        activity?.let {act ->
            val builder = AlertDialog.Builder(
                act
            )
            builder.setCancelable(false)
            builder.setTitle(title)
            builder.setMessage(message)
            promoDialog = builder.create()
            promoDialog?.apply {
                if (positive != "") {
                    setButton(AlertDialog.BUTTON_POSITIVE, positive, positiveOnClickListener)
                }
                if (negative != "") {
                    setButton(AlertDialog.BUTTON_NEGATIVE, negative, negativeOnClickListener)
                }
                setOnShowListener {
                    if (positive != "") {
                        getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false)
                        if (!CacheUtil.getInstance().colorMain.equals(
                                "#FFFFFF",
                                ignoreCase = true
                            )
                        ) {
                            getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                                ViewUtil.getColorMain(act)
                            )
                        } else {
                            getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                                ContextCompat.getColor(
                                    act, R.color.primary_text
                                )
                            )
                        }
                    }
                    if (negative != "") {
                        getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false)
                        getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                            ContextCompat.getColor(
                                act, R.color.secondary_text
                            )
                        )
                    }
                }
                show()
            }
        }

    }

    open fun dismissPromptDialog() {
        try {
            if (promoDialog != null && promoDialog!!.isShowing) {
                promoDialog!!.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var loadingDialog: AlertDialog? = null
    open fun showLoadingDialog() {
        try {
            dismissLoadingDialog()
            context?.let {
                val builder = AlertDialog.Builder(it)
                builder.setCancelable(false)
                builder.setView(layoutInflater.inflate(R.layout.loading, null))
                loadingDialog = builder.create()
                loadingDialog?.apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    show()
                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    open fun dismissLoadingDialog() {
        try {
            loadingDialog?.let { dialog ->
                if(dialog.isShowing){
                    dialog.dismiss()
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}