package com.herfy.utils.base.mvvm

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.herfy.utils.R
import com.herfy.utils.helper.LocaleUtil
import java.util.Locale

open class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {
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


    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        val anim = if (enter) AnimationUtils.loadAnimation(
            activity,
            R.anim.fade_in
        ) else AnimationUtils.loadAnimation(
            activity, R.anim.fade_out
        )
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(anim)
        return animationSet
    }

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
}