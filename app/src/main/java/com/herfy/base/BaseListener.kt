package com.herfy.base

import android.view.View
import com.daimajia.androidanimations.library.Techniques
import java.util.HashMap

interface BaseListeners: BaseLogListener, BaseViewManipulationListener

interface BaseDialogListener {
    fun dismissDialogs()
    fun showLoadingDialog()
    fun showMessage(message: String)
    fun showAlertDialog(title: String, success: String)
}

interface BaseNavigationListener {
    fun showMain()
}

interface BaseLogListener {
    fun logEventAttributesMetric(eventName: String?, eventAttributes: HashMap<String, String>?, eventMetricName: String?, eventMetricValue: Double)
}

interface BaseViewManipulationListener {
    fun animateSpringView(technique: Techniques, duration: Long, view: View, offset: Int)
}