package com.herfy.authentication.views

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.herfy.authentication.R
import com.herfy.authentication.databinding.FragmentOtpPageBinding
import com.herfy.authentication.viewmodel.LoginOTPViewModel
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.StringUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginOTPFragment : BaseFragment<LoginOTPViewModel, FragmentOtpPageBinding>() {

    private val args: LoginOTPFragmentArgs by navArgs()

    override val viewModel: LoginOTPViewModel by viewModels()

    override val layout: Int = R.layout.fragment_otp_page

    private var countDownTimer: CountDownTimer? = null

    private var countDownTime: Long = 60000

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
        initTimer()
        initArgs()
    }

    private fun enableResend() {
        binding.sendAgainLayout.setOnClickListener {

        }
    }

    private fun initTimer() {
        binding.sendAgainLayout.setOnClickListener(null)
        countDownTimer = object : CountDownTimer(countDownTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countDownTime = millisUntilFinished
                binding.resendText.text = (CacheUtil.getInstance()
                    .getTranslations(Text.SEND_AGAIN_LABEL, "Send again ((x)s)").replace(
                        "(x)", StringUtil.localize((millisUntilFinished / 1000).toString())
                    ))
            }

            override fun onFinish() {
                binding.resendText.text =
                    (CacheUtil.getInstance().getTranslations(Text.SEND_AGAIN, "Send again"))
                countDownTime = 60000
                enableResend()
            }
        }
        countDownTimer?.start()
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