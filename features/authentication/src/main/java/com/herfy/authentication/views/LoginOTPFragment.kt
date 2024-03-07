package com.herfy.authentication.views

import android.annotation.SuppressLint
import android.content.Intent
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.herfy.authentication.R
import com.herfy.authentication.databinding.FragmentOtpPageBinding
import com.herfy.authentication.state.LoginOTPPageViewState
import com.herfy.authentication.viewmodel.LoginOTPViewModel
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.StringUtil
import com.herfy.utils.helper.collectViewModelState
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
                val code =
                    StringUtil.toEnglish(codeOne.text.toString() + codeTwo.text.toString() + codeThree.text.toString() + codeFour.text.toString() + codeFive.text.toString() + codeSix.text.toString())
                viewModel.verify(code.orEmpty(), args.customerId)
            }

            backButton.setOnClickListener {
                navigateUp()
            }

            codeOne.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    val nextField = codeOne.focusSearch(View.FOCUS_RIGHT) as TextView
                    nextField.requestFocus()
                }
            })

            codeTwo.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    val nextField = codeTwo.focusSearch(View.FOCUS_RIGHT) as TextView
                    nextField.requestFocus()
                }
            })

            codeThree.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    val nextField = codeThree.focusSearch(View.FOCUS_RIGHT) as TextView
                    nextField.requestFocus()
                }
            })

            codeFour.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    val nextField = codeFour.focusSearch(View.FOCUS_RIGHT) as TextView
                    nextField.requestFocus()
                }
            })

            codeFive.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    val nextField = codeFive.focusSearch(View.FOCUS_RIGHT) as TextView
                    nextField.requestFocus()
                }
            })

            codeSix.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                }
            })

            codeSix.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    continueButton.performClick()
                    true
                } else false
            }
        }
        initTimer()
        initArgs()
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
            enterForDigitLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.ENTER_SIX_DIGIT, "Enter the 6 digit code sent to your number")
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
            requireContext().applicationContext.sendBroadcast(
                Intent(
                    requireContext().getString(R.string.exit_to_home)
                )
            )
        } else {
            findNavController().navigate(
                LoginOTPFragmentDirections.actionOtpToInputUser(
                    phoneNumber = args.phoneNumber,
                    customerId = args.customerId,
                    isVerified = args.isVerified,
                    isGuest = args.isGuest
                )
            )
        }
    }

    private fun initArgs() {
        binding.userNumber.text = "+" + args.phoneNumber.replace("+", "")
    }
}