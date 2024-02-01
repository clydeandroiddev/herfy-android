package com.herfy.utils.base.mvvm

import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.helper.NavigationCommand

abstract class BaseFragment<T : BaseViewModel, B : ViewDataBinding> : Fragment() {
    abstract val viewModel: T
    abstract val layout: Int

    lateinit var binding: B
    lateinit var rootView: View

    private var progressDialog: ProgressDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
        //progressDialog?.setMessage(getString(R.string.loading))
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

    private fun handleNavigation(navCommand: NavigationCommand) {
        when (navCommand) {
            is NavigationCommand.ToDirection -> findNavController().navigate(navCommand.directions)
            is NavigationCommand.Back -> findNavController().navigateUp()
        }
    }

    open fun initTranslations(){

    }


}