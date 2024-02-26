package com.herfy.utils.base.mvvm

import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.herfy.utils.R
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.ViewUtil

abstract class BaseActivity<T : BaseViewModel, B : ViewDataBinding> : AppCompatActivity() {

    abstract val layout: Int
    abstract val viewModel: T
    lateinit var binding: B

    private lateinit var progressDialog: ProgressDialog
    private var alertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = DataBindingUtil.setContentView(this, layout)

        initViews()
        observe()
    }

    override fun onPause() {
        super.onPause()
        dismissProgressDialog()
    }

    open fun initViews() {
        progressDialog = ProgressDialog(this)
    }

    open fun observe() {
        viewModel.showProgressBar.observe(this, {
            if (it == true) showProgressDialog()
            else dismissProgressDialog()
        })
    }

    fun showProgressDialog(cancelTouchOutside: Boolean? = true) {
        if (!progressDialog.isShowing)  {
            progressDialog.setCanceledOnTouchOutside(cancelTouchOutside ?: true)
            progressDialog.show()
        }
    }

    fun dismissProgressDialog() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    open fun showAlertDialog(title: String?, message: String?) {
        try {
            val builder = AlertDialog.Builder(this)
            builder.setCancelable(true)
            builder.setTitle(title)
            builder.setMessage(message)
            alertDialog = builder.create()
            alertDialog?.setButton(
                AlertDialog.BUTTON_POSITIVE, CacheUtil.getInstance().getTranslations(Text.OK)
            ) { dialogInterface, i -> dialogInterface.dismiss() }
            alertDialog?.setOnShowListener {
                alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                    isAllCaps = false
                    setTextColor(
                        if (!CacheUtil.getInstance().colorMain.equals("#FFFFFF", ignoreCase = true)) {
                            ViewUtil.getColorMain(this@BaseActivity)
                        } else {
                            ContextCompat.getColor(
                                this@BaseActivity,
                                R.color.primary_text
                            )
                        }
                    )
                }
            }
            alertDialog?.show()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }

    fun back() {
        onBackPressed()
    }

    fun close() {
        finish()
    }
}