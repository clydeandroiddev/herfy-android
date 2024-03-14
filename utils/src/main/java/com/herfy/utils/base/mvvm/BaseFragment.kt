package com.herfy.utils.base.mvvm

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.herfy.utils.R
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.NavigationCommand
import com.herfy.utils.helper.ViewUtil


abstract class BaseFragment<T : BaseViewModel, B : ViewDataBinding> : Fragment() {
    abstract val viewModel: T
    abstract val layout: Int

    lateinit var binding: B
    lateinit var rootView: View

    private var progressDialog: ProgressDialog? = null

    private var alertDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, layout, container, false)
        binding.lifecycleOwner = this
        rootView = binding.root
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initTranslations()
        observe()
    }

    open fun initViews() {
        progressDialog = ProgressDialog(requireContext())
//        progressDialog?.setMessage(getString(R.string.loading))
    }

    open fun observe() {
        viewModel.showProgressBar.observe(viewLifecycleOwner, {
            if (it == true) showProgressDialog()
            else dismissProgressDialog()
        })
    }

    fun showProgressDialog(cancelTouchOutside: Boolean? = true) {
        if (progressDialog?.isShowing == false) {
            progressDialog?.setCanceledOnTouchOutside(cancelTouchOutside ?: true)
            progressDialog?.show()
        }
    }

    fun showProgressDialog(cancelTouchOutside: Boolean? = true, message: String?) {
        if (progressDialog?.isShowing == false) {
            progressDialog?.setMessage(message)
            progressDialog?.setCanceledOnTouchOutside(cancelTouchOutside ?: true)
            progressDialog?.show()
        }
    }

    fun dismissProgressDialog() {
        if (progressDialog?.isShowing == true) {
            progressDialog?.dismiss()
        }
    }

    private fun observeNavigation() {
        viewModel.navigation.observeNonNull(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { navigationCommand ->
                handleNavigation(navigationCommand)
            }
        }
    }

    fun hideKeyboard(view: View) {
        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm!!.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun showKeyboard(view: EditText) {
        val inputMethodManager =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun handleNavigation(navCommand: NavigationCommand) {
        when (navCommand) {
            is NavigationCommand.ToDirection -> findNavController().navigate(navCommand.directions)
            is NavigationCommand.Back -> findNavController().navigateUp()
        }
    }

    open fun initTranslations() {

    }

    open fun showAlertDialog(title: String?, message: String?) {
        if (message?.isNotEmpty() == true) {
            activity?.apply {
                runOnUiThread {
                    context?.let { ctx ->
                        try {
                            dismissProgressDialog()
                            val builder = AlertDialog.Builder(ctx)
                            builder.setCancelable(true)
                            builder.setTitle(title)
                            builder.setMessage(message)
                            alertDialog = builder.create()
                            alertDialog?.apply {
                                setButton(
                                    AlertDialog.BUTTON_POSITIVE,
                                    CacheUtil.getInstance().getTranslations(Text.OK)
                                ) { dialogInterface, i -> dialogInterface.dismiss() }
                                setOnShowListener {
                                    getButton(AlertDialog.BUTTON_POSITIVE).isAllCaps = false
                                    if (!CacheUtil.getInstance().colorMain.equals(
                                            "#FFFFFF", ignoreCase = true
                                        )
                                    ) {
                                        getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                                            ViewUtil.getColorMain(
                                                ctx
                                            )
                                        )
                                    } else {
                                        getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                                            ContextCompat.getColor(
                                                ctx, R.color.primary_text
                                            )
                                        )
                                    }
                                }
                                show()
                            }

                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    open fun showAlertDialog(
        title: String?,
        message: String?,
        neutral: String?,
        neutralOnClickListener: DialogInterface.OnClickListener?
    ) {
        if (message?.isNotEmpty() == true) {
            activity?.apply {
                runOnUiThread {
                    context?.let { ctx ->
                        try {
                            dismissProgressDialog()
                            val builder = AlertDialog.Builder(this)
                            builder.setCancelable(true)
                            builder.setTitle(title)
                            builder.setMessage(message)
                            alertDialog = builder.create()
                            alertDialog?.apply {
                                setButton(
                                    AlertDialog.BUTTON_POSITIVE, neutral, neutralOnClickListener
                                )
                                setOnShowListener {
                                    getButton(AlertDialog.BUTTON_POSITIVE).isAllCaps = false
                                    if (!CacheUtil.getInstance().colorMain.equals(
                                            "#FFFFFF", ignoreCase = true
                                        )
                                    ) {
                                        getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                                            ViewUtil.getColorMain(
                                                ctx
                                            )
                                        )
                                    } else {
                                        getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                                            ContextCompat.getColor(
                                                ctx, R.color.primary_text
                                            )
                                        )
                                    }
                                }
                                show()
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    open fun showAlertDialog(
        title: String?,
        message: String?,
        positive: String,
        positiveOnClickListener: DialogInterface.OnClickListener?,
        negative: String,
        negativeOnClickListener: DialogInterface.OnClickListener?
    ) {
        if (message?.isNotEmpty() == true) {
            activity?.apply {
                runOnUiThread {
                    context?.let { ctx ->
                        dismissProgressDialog()
                        val builder = AlertDialog.Builder(ctx)
                        builder.setCancelable(true)
                        builder.setTitle(title)
                        builder.setMessage(message)
                        alertDialog = builder.create()
                        alertDialog?.apply {
                            if (positive != "") {
                                setButton(
                                    AlertDialog.BUTTON_POSITIVE, positive, positiveOnClickListener
                                )
                            }
                            if (negative != "") {
                                setButton(
                                    AlertDialog.BUTTON_NEGATIVE, negative, negativeOnClickListener
                                )
                            }
                            setOnShowListener {
                                if (positive != "") {
                                    getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false)
                                    if (!CacheUtil.getInstance().colorMain.equals(
                                            "#FFFFFF", ignoreCase = true
                                        )
                                    ) {
                                        getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                                            ViewUtil.getColorMain(
                                                ctx
                                            )
                                        )
                                    } else {
                                        getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                                            ContextCompat.getColor(
                                                ctx, R.color.primary_text
                                            )
                                        )
                                    }
                                }
                                if (negative != "") {
                                    getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false)
                                    getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                                        ContextCompat.getColor(
                                            ctx, R.color.secondary_text
                                        )
                                    )
                                }
                            }
                            show()
                        }

                    }
                }
            }
        }
    }

}