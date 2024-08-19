package com.herfy.authentication.views

import android.annotation.SuppressLint
import android.app.Activity
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.herfy.analytics.kochava.KochavaEventType
import com.herfy.analytics.kochava.KochavaEventUtil
import com.herfy.analytics.kochava.KochavaTrackerUtil
import com.herfy.authentication.R
import com.herfy.authentication.databinding.FragmentOtpPageBinding
import com.herfy.authentication.state.LoginOTPPageViewState
import com.herfy.authentication.viewmodel.LoginOTPViewModel
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.StringUtil
import com.herfy.utils.helper.collectViewModelState
import com.herfy.utils.widgets.otp.OTPListener
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
        adjustWindow()
        binding.apply {

            continueButton.setOnClickListener {
                verify(binding.otpView.otp.orEmpty())
            }
            backButton.setOnClickListener {
                navigateUp()
            }
            var otpLength = CacheUtil.getInstance().concept.otpLength
            if (otpLength == 0) {
                otpLength = 6
            }
            binding.otpView.setLength(otpLength)
            binding.otpView.generate()
            binding.otpView.otpListener = object : OTPListener {
                override fun onInteractionListener() {}
                override fun onOTPComplete(otp: String) {
                    verify(otp)
                }
            }
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                enterForDigitLabel.text = CacheUtil.getInstance().getTranslations(
                    Text.DYNAMIC_CODE_DIGIT, "Enter the (x) digit code sent to your number"
                ).replace("(x)", otpLength.toString())
                showKeyboard(requireContext(), dummyEdit)
                binding.otpView.requestFocusOTP()
                initTimer()
                initArgs()
            }, 500)
        }
    }

    private fun adjustWindow() {
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun verify(otp: String) {
        val code = StringUtil.toEnglish(otp)
        viewModel.verify(code.orEmpty(), args.customerId)
    }

    private fun enableResend() {
        binding.sendAgainLayout.setOnClickListener {
            viewModel.resend(args.customerId, args.phoneNumber)
        }
    }

    private fun initTimer() {
        binding.sendAgainLayout.setOnClickListener(null)
        countDownTimer = object : CountDownTimer(countDownTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countDownTime = millisUntilFinished
                var second = (millisUntilFinished / 1000).toInt()
                if (second == 0) {
                    second = 1
                }
                binding.resendText.text = (CacheUtil.getInstance()
                    .getTranslations(Text.SEND_AGAIN_LABEL, "Send again ((x)s)").replace(
                        "(x)", StringUtil.localize(second.toString())
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
            notReceiveLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.DID_NOT_GE_CODE, "Did not get the code?")
        }
    }

    override fun observe() {
        super.observe()
        viewModel.state.collectViewModelState(viewLifecycleOwner) {
            when (it) {
                is LoginOTPPageViewState.UserResend -> {
                    if (countDownTimer != null) {
                        countDownTimer!!.cancel()
                    }
                    initTimer()
                }

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
                        title = CacheUtil.getInstance().getTranslations(Text.ERROR_CAPS, "ERROR"),
                        message = it.error
                    )
                }

                is LoginOTPPageViewState.SuccessVerify -> {
                    toNextPage()
                }

                else -> {}
            }
        }
    }

    private fun toNextPage() {
        if (args.isVerified) {
            KochavaTrackerUtil.trackEventWithData(
                KochavaEventType.LOGIN.eventName,
                KochavaEventUtil.customer(args.customerId)
            )
            requireActivity().setResult(Activity.RESULT_OK)
            requireActivity().finish()
        } else {
            findNavController().navigate(
                LoginOTPFragmentDirections.actionOtpToInputUser(
                    phoneNumber = args.phoneNumber,
                    customerId = args.customerId,
                    isVerified = args.isVerified,
                    isGuest = args.isGuest,
                    givenName = args.givenName
                )
            )
        }
    }

    private fun initArgs() {
        binding.userNumber.text = "+" + args.phoneNumber.replace("+", "")
    }
}