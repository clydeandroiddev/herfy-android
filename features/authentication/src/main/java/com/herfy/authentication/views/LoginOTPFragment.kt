package com.herfy.authentication.views

import androidx.fragment.app.viewModels
import com.herfy.authentication.R
import com.herfy.authentication.databinding.FragmentLoginBinding
import com.herfy.authentication.databinding.FragmentOtpPageBinding
import com.herfy.authentication.viewmodel.LoginOTPViewModel
import com.herfy.authentication.viewmodel.LoginViewModel
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginOTPFragment : BaseFragment<LoginOTPViewModel, FragmentOtpPageBinding>() {

    override val viewModel: LoginOTPViewModel by viewModels()
    override val layout: Int = R.layout.fragment_otp_page

    override fun initViews() {
        super.initViews()
        binding.apply {

        }
    }

    override fun initTranslations() {
        super.initTranslations()
        binding.apply {

        }
    }

    override fun observe() {
        super.observe()
    }
}