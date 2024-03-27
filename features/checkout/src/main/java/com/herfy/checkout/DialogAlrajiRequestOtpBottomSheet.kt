package com.herfy.checkout

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.herfy.checkout.databinding.DialogAlrajiRequestOtpBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.debounceTextChanges
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.extensions.showToast
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.DialogUtils
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.launchWithTimber
import com.herfy.utils.helper.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class DialogAlrajiRequestOtpBottomSheet: BottomSheetDialogFragment() {

    private val cacheUtil by lazy {
        CacheUtilHelper(requireContext())
    }
    private val viewModel by viewModels<DialogAlrajiRequestOtpViewModel>()

    companion object {
        const val TAG = "com.herfy.checkout.DialogAlrajiRequestOtpBottomSheet"
        fun newInstance() = DialogAlrajiRequestOtpBottomSheet()
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private var _binding:  DialogAlrajiRequestOtpBinding? = null
    val binding  get() = _binding ?: throw NullPointerException("Binding not yet set nor created")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DialogAlrajiRequestOtpBinding.inflate(layoutInflater)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
        dialog?.window?.decorView?.layoutDirection = if(LocaleUtil.getInstance().isArabic) {
            View.LAYOUT_DIRECTION_RTL
        } else {
            View.LAYOUT_DIRECTION_LTR
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDialog()
        setUpViews()
        setUpTranslations()
        setUpVmObservers()
    }

    private fun setUpViews() {
        binding
            .icClose
            .ninjaTap(viewLifecycleScope) {
                dismiss()
            }

        binding
            .requestOtp
            .ninjaTap(viewLifecycleScope) {
                viewModel.requestOtp(getMobileNumberEditText().ifEmpty { getCicNumberEditText() })
            }
    }

    private fun getMobileNumberEditText(): String {
        return binding.mobileNumberEditText.text.toString()
    }

    private fun getCicNumberEditText(): String {
        return binding.cicNumberEditText.text.toString()
    }


    private fun setUpTranslations() {
        binding.apply {
            txtCicTitle.text = cacheUtil
                .getTranslations(Text.CIC_NUMBER,"CIC Number")
            txtOr.text = cacheUtil.getTranslations(Text.OR,"OR")
            txtTitle.text = cacheUtil.getTranslations(Text.AlRAJI_BANK_TITLE,"Al Rhaji Bank (Mokafa's Rewards)")
            txtDescription.text = cacheUtil
                .getTranslations(Text.ALRAJI_BANK_DESCRIPTION,"The user has to be a registered mobile number with Mokafa'a Al Rajhi Program, otherwise the redemption will not be successful.")
            binding.requestOtp.text = cacheUtil
                .getTranslations(Text.REQUEST_OTP_LABEL,"Request OTP")
            binding.txtMobileNumber.text = cacheUtil
                .getTranslations(Text.MOBILE_NUMBER,"Mobile Number")
        }
    }

    private fun setUpVmObservers() {
        viewLifecycleScope.launchWithTimber {
            repeatOnLifecycle(state =  Lifecycle.State.CREATED) {
                viewModel
                    .state
                    .collectLatest { state ->
                        handleState(state)

                    }
            }
        }
        viewLifecycleScope.launchWithTimber {
            binding.mobileNumberEditText
                .debounceTextChanges()
                .collectLatest {userInput->
                    binding.cicNumberEditText.isEnabled = userInput.isEmpty()
                }
        }

        viewLifecycleScope.launchWithTimber {
            binding
                .cicNumberEditText
                .debounceTextChanges()
                .collectLatest {cicInput->
                    binding.mobileNumberEditText.isEnabled = cicInput.isEmpty()
                }
        }
    }

    private fun handleState(state: DialogRequestOTPState) {
        when(state) {
            is DialogRequestOTPState.Error -> {
               requireContext().showToast(state.message)
            }
            is DialogRequestOTPState.IsLoading -> {
                DialogUtils.showProgressDialog(requireContext(), isLoading = state.isLoading)

            }

            DialogRequestOTPState.Success -> {

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}