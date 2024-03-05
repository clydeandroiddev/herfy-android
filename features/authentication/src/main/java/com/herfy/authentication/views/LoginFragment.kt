package com.herfy.authentication.views

import android.net.Uri
import androidx.fragment.app.viewModels
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.findNavController
import com.herfy.authentication.R
import com.herfy.authentication.databinding.FragmentLoginBinding
import com.herfy.authentication.viewmodel.LoginViewModel
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.base.mvvm.BaseActivity
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.cast
import com.herfy.utils.extensions.observeNonNull
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.NavigationUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : BaseFragment<LoginViewModel, FragmentLoginBinding>() {

    override val viewModel: LoginViewModel by viewModels()
    override val layout: Int = R.layout.fragment_login

    override fun initViews() {
        super.initViews()
        binding.apply {
            loginButton.setOnClickListener {
                viewModel.login(mobileNumberDialCode.text.toString() + binding.phoneNumberEdit.text.toString())
            }

            closeButton.setOnClickListener {
                requireActivity().finish()
            }

            loginGoogle.setOnClickListener {

            }

            loginGuest.setOnClickListener {

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

        viewModel.showLoading.observeNonNull(this) { showLoading ->
            activity?.cast<BaseActivity<*, *>> {
                it.runOnUiThread {
                    if (showLoading) {
                        it.showProgressDialog(false)
                    } else {
                        it.dismissProgressDialog()
                    }
                }
            }
        }

        viewModel.customer.observeNonNull(this) {

        }
    }
}