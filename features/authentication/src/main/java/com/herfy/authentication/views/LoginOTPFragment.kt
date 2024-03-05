package com.herfy.authentication.views

import android.annotation.SuppressLint
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.herfy.authentication.R
import com.herfy.authentication.databinding.FragmentOtpPageBinding
import com.herfy.authentication.viewmodel.LoginOTPViewModel
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.CacheUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginOTPFragment : BaseFragment<LoginOTPViewModel, FragmentOtpPageBinding>() {

    private val args: LoginOTPFragmentArgs by navArgs()

    override val viewModel: LoginOTPViewModel by viewModels()

    override val layout: Int = R.layout.fragment_otp_page

    @SuppressLint("SetTextI18n")
    override fun initViews() {
        super.initViews()
        binding.apply {
            continueButton.setOnClickListener {

            }
            backButton.setOnClickListener {
                navigateUp()
            }
        }
        initArgs()
    }

    override fun initTranslations() {
        super.initTranslations()
        binding.apply {
            title.text = CacheUtil.getInstance()
                .getTranslations(Text.CONFIRM_YOUR_NUMBER, "Confirm your number")
            continueButton.text = CacheUtil.getInstance().getTranslations(Text.NEXT, "Next")
            enterForDigitLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.ENTER_SIX_DIGIT, "Enter the 6 digit code sent to your number")
            notReceiveLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.DID_NOT_GE_CODE, "Did not get the code?")
        }
    }

    override fun observe() {
        super.observe()
    }

    private fun initArgs() {
        binding.userNumber.text = "+" + args.phoneNumber.replace("+", "")
    }
}