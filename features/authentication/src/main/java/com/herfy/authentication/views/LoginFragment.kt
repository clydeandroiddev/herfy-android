package com.herfy.authentication.views

import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.herfy.authentication.R
import com.herfy.authentication.databinding.FragmentLoginBinding
import com.herfy.authentication.state.LoginViewState
import com.herfy.authentication.viewmodel.LoginViewModel
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.collectViewModelState
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class LoginFragment : BaseFragment<LoginViewModel, FragmentLoginBinding>() {

    override val viewModel: LoginViewModel by viewModels()

    override val layout: Int = R.layout.fragment_login

    override fun initViews() {
        super.initViews()
        binding.apply {
            loginButton.setOnClickListener {
                viewModel.login(
                    mobileNumberDialCode.text.toString(), binding.phoneNumberEdit.text.toString()
                )
            }

            closeButton.setOnClickListener {
                requireActivity().finish()
            }

            loginGoogle.setOnClickListener {

            }

            loginGuest.setOnClickListener {

            }

            phoneNumberEdit.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginButton.performClick()
                    true
                } else false
            }
        }
    }

    override fun initTranslations() {
        super.initTranslations()
        binding.apply {
            mobileNumberDialCode.text = "+966"

            titleLabel.text = CacheUtil.getInstance().getTranslations(
                Text.JOIN_NOW, "Join now and start ordering, earning points and much more"
            )
            mobileNumberLabel.text = CacheUtil.getInstance().getTranslations(
                Text.MOBILE_NUMBER, "Mobile Number"
            )
            loginButton.text = CacheUtil.getInstance().getTranslations(
                Text.CONTINUE_LABEL, "Continue"
            )
            orLabel.text = CacheUtil.getInstance().getTranslations(
                Text.OR, "Or"
            )
            loginGoogle.text = CacheUtil.getInstance().getTranslations(
                Text.CONTINUE_WITH_GOOGLE, "Continue with Google"
            )
            loginGuest.text = CacheUtil.getInstance().getTranslations(
                Text.CONTINUE_AS_GUEST, "Continue as Guest"
            )
        }
    }

    override fun observe() {
        super.observe()

        viewModel.showError.observeNonNull(this) {
            showAlertDialog(
                title = CacheUtil.getInstance().getTranslations(Text.ERROR_CAPS, "ERROR"),
                message = it.message
            )
        }

        viewModel.state.collectViewModelState(viewLifecycleOwner) {
            when (it) {
                is LoginViewState.UserLogin -> {
                    findNavController().navigate(
                        LoginFragmentDirections.actionLoginToOtp(
                            phoneNumber = it.customer.attributes.mobile, customerId = it.customer.id
                        )
                    )
                }

                is LoginViewState.ShowLoading -> {
                    if (it.loading) {
                        showProgressDialog(false)
                    } else {
                        dismissProgressDialog()
                    }
                }
            }
        }
    }
}