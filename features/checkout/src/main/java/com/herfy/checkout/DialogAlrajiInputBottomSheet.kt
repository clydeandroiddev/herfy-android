package com.herfy.checkout

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.herfy.checkout.databinding.DialogAlrajiEnterAmountBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.extensions.showToast
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.DialogUtils
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.StringUtil
import com.herfy.utils.helper.launchWithTimber
import com.herfy.utils.helper.viewLifecycleScope
import com.herfy.utils.widgets.otp.OTPListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class DialogAlrajiInputBottomSheet: BottomSheetDialogFragment() {

    private lateinit var binding: DialogAlrajiEnterAmountBinding

    private val viewModel: DialogAlrajiBottomSheetViewModel by viewModels()

    private val cacheUtilHelper by lazy {
        CacheUtilHelper(requireContext())
    }

    companion object {
        const val TAG = "com.herfy.checkout.DialogAlrajiInputBottomSheet"
        fun newInstance(): DialogAlrajiInputBottomSheet {
            return DialogAlrajiInputBottomSheet()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogAlrajiEnterAmountBinding.inflate(layoutInflater)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
        dialog?.window?.decorView?.layoutDirection = if(LocaleUtil.getInstance().isArabic) {
            View.LAYOUT_DIRECTION_RTL
        } else {
            View.LAYOUT_DIRECTION_LTR
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpViews()
        setupDialog()
        setUpTranslations()
        setUpVmObservers()
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    private fun setUpViews() {
        var otpLength = CacheUtil.getInstance().concept.otpLength
        if (otpLength == 0) {
            otpLength = 6
        }
        binding.userNumber.text =  "+${AuthUtil.getInstance().customer.attributes.mobile}"
        binding.sendAgainLayout.setOnClickListener(null)
        binding.otpView.setLength(otpLength)
        binding.otpView.generate()
        binding.otpView.otpListener = object : OTPListener {
            override fun onInteractionListener() {
                // do nothing
            }
            override fun onOTPComplete(otp: String) {
                verifyCode(otp)
            }
        }
        binding
            .icClose
            .ninjaTap(viewLifecycleScope) {
                dismiss()
            }

        binding
            .submitOtp
            .ninjaTap(viewLifecycleScope) {
                verifyCode(binding.otpView.otp.orEmpty())
            }
        binding.enterForDigitLabel.text = CacheUtil.getInstance().getTranslations(
            Text.DYNAMIC_CODE_DIGIT, "Enter the (${otpLength}) digit code sent to your number")
    }


    private fun setUpTranslations() {
        binding.txtTitle.text = cacheUtilHelper.getTranslations(Text.AlRAJI_BANK_TITLE,"Al Rhaji Bank (Mokafa's Rewards)")
        binding.submitOtp.text = cacheUtilHelper
            .getTranslations(Text.SUBMIT_OTP,"Submit OTP")
        binding.txtAmount.text = cacheUtilHelper.getTranslations(Text.AMOUNT,"Amount")
        binding.notReceiveLabel.text =cacheUtilHelper
            .getTranslations(Text.DID_NOT_GE_CODE, "Did not get the code?")
    }

    private fun verifyCode(otp:String) {
        val phoneNumber = binding.amountEditText.text.toString().trim()
            viewModel.verifyCode(otp,phoneNumber)
    }

    private fun setUpVmObservers() {
        viewLifecycleScope.launchWithTimber {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel
                    .state
                    .collectLatest { state ->
                        handleState(state)
                    }
            }
        }
    }
    private fun setupDialog() {
        val bottomSheet = (requireView().parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
        bottomSheet.elevation = 0f
    }
    private fun handleState(state: DialogAlrajiState) {
        when(state) {

            is DialogAlrajiState.Error -> {
                requireContext().showToast(state.message)
            }

            is DialogAlrajiState.IsLoading -> {
            DialogUtils.showProgressDialog(requireContext(), isLoading = state.isLoading)
            }

            DialogAlrajiState.Success -> {

            }

            is DialogAlrajiState.InvalidAmountError ->  {
                binding
                    .txtErrorMessage.text = state.message
                binding.icAlert.setVisible(true)
            }

            DialogAlrajiState.ResendTimeCompleted -> {
                binding
                    .sendAgainLayout
                    .ninjaTap(viewLifecycleScope) {
                        viewModel.resend()
                    }
                binding.resendText.text =
                    (CacheUtil.getInstance().getTranslations(Text.SEND_AGAIN, "Send again"))

            }
            is DialogAlrajiState.ResendTimer -> {
                binding.resendText.text = (CacheUtil.getInstance()
                    .getTranslations(Text.SEND_AGAIN_LABEL, "Send again ((x)s)").replace(
                        "(x)", StringUtil.localize(state.remainingSeconds.toString())
                    ))
            }
        }
    }

}