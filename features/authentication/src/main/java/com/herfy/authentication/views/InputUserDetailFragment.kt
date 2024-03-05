package com.herfy.authentication.views

import androidx.fragment.app.viewModels
import com.herfy.authentication.R
import com.herfy.authentication.databinding.FragmentAddNewNameBinding
import com.herfy.authentication.databinding.FragmentLoginBinding
import com.herfy.authentication.viewmodel.InputUserDetailViewModel
import com.herfy.authentication.viewmodel.LoginViewModel
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InputUserDetailFragment :
    BaseFragment<InputUserDetailViewModel, FragmentAddNewNameBinding>() {

    override val viewModel: InputUserDetailViewModel by viewModels()
    override val layout: Int = R.layout.fragment_add_new_name

    override fun initViews() {
        super.initViews()
        binding.apply {

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
    }
}