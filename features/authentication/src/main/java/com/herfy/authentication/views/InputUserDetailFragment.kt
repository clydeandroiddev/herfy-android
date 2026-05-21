package com.herfy.authentication.views

import android.app.Activity
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.capillary.hydra.android.sdk.HydraAPI
import com.herfy.analytics.kochava.KochavaEventType
import com.herfy.analytics.kochava.KochavaEventUtil
import com.herfy.analytics.kochava.KochavaTrackerUtil
import com.herfy.authentication.R
import com.herfy.authentication.databinding.FragmentAddNewNameBinding
import com.herfy.authentication.state.LoginOTPPageViewState
import com.herfy.authentication.viewmodel.LoginOTPViewModel
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.collectViewModelState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InputUserDetailFragment : BaseFragment<LoginOTPViewModel, FragmentAddNewNameBinding>() {

    private val args: InputUserDetailFragmentArgs by navArgs()

    override val viewModel: LoginOTPViewModel by viewModels()

    override val layout: Int = R.layout.fragment_add_new_name

    override fun initViews() {
        super.initViews()
        adjustWindow()
        binding.apply {
            backButton.setOnClickListener {
                skipButton.performClick()
            }

            continueButton.setOnClickListener {
                viewModel.updateName(binding.fullName.text.toString(), args.customerId)
            }

            skipButton.setOnClickListener {
                KochavaTrackerUtil.trackEventWithData(
                    KochavaEventType.REGISTER.eventName,
                    KochavaEventUtil.customer(args.customerId)
                )
                requireActivity().setResult(Activity.RESULT_OK)
                requireActivity().finish()
            }

            fullName.setText(args.givenName)
            fullName.setSelection(fullName.text.toString().length)

            fullName.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    continueButton.performClick()
                    true
                } else false
            }
        }
    }

    private fun adjustWindow() {
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun initTranslations() {
        super.initTranslations()
        binding.apply {
            title.text =
                CacheUtil.getInstance().getTranslations(Text.SETUP_ACCOUNT, "Set up your account")
            fewMoreLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.A_FEW_MORE_THINGS, "A few more things to know you better")
            fullNameLabel.text =
                CacheUtil.getInstance().getTranslations(Text.FULL_NAME_LABEL, "Full Name")
            continueButton.text = CacheUtil.getInstance().getTranslations(Text.NEXT, "Next")
            skipButton.text = CacheUtil.getInstance().getTranslations(Text.SKIP, "Skip")
        }
    }

    override fun observe() {
        super.observe()

        viewModel.state.collectViewModelState(viewLifecycleOwner) {
            when (it) {
                is LoginOTPPageViewState.ShowLoading -> {
                    if (it.loading) {
                        showProgressDialog(
                            false, CacheUtil.getInstance().getTranslations(Text.LOADING, "Loading")
                        )
                    } else {
                        dismissProgressDialog()
                    }
                }

                is LoginOTPPageViewState.ShowError -> {
                    showAlertDialog(
                        title = "", message = it.error
                    )
                }

                is LoginOTPPageViewState.SuccessUpdate -> {
                    HydraAPI.getDefaultInstance(requireActivity())?.pushUserUpdate(
                        AuthUtil.getInstance().customer.attributes.mobile,
                        AuthUtil.getInstance().customer.attributes.firstName,
                        AuthUtil.getInstance().customer.attributes.lastName,
                        AuthUtil.getInstance().customer.attributes.email,
                        AuthUtil.getInstance().customer.attributes.mobile,
                    )

                    HydraAPI.getDefaultInstance(requireActivity())?.pushUserSignin(
                        AuthUtil.getInstance().customer.attributes.mobile,
                        AuthUtil.getInstance().customer.attributes.firstName,
                        AuthUtil.getInstance().customer.attributes.lastName,
                        AuthUtil.getInstance().customer.attributes.email,
                        AuthUtil.getInstance().customer.attributes.mobile,
                    )
                    binding.skipButton.performClick()
                }

                else -> {}
            }
        }
    }
}