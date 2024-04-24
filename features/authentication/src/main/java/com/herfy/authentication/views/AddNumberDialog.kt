package com.herfy.authentication.views

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.herfy.authentication.databinding.FragmentAddNumberDialogBinding
import com.herfy.ordertype.R
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil

class AddNumberDialog : BottomSheetDialogFragment() {

    private var onSubmit: OnSubmit? = null

    private var isMaxTen = false

    private lateinit var binding: FragmentAddNumberDialogBinding

    interface OnSubmit {
        fun onSubmit(phoneNumber: String)
        fun exit()
        fun onError(error: String)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentAddNumberDialogBinding.inflate(layoutInflater)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
        dialog?.window?.decorView?.layoutDirection = if (LocaleUtil.getInstance().isArabic) {
            View.LAYOUT_DIRECTION_RTL
        } else {
            View.LAYOUT_DIRECTION_LTR
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    private fun setupDialog() {
        val bottomSheet = (requireView().parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
        bottomSheet.elevation = 0f
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDialog()
        setupViews()
    }

    private fun setupViews() {
        binding.apply {
            mobileNumberDialCode.text = "+966"

            sendCodeButton.setOnClickListener {
                hideKeyboard(it)
                var phoneNumber = phoneNumberEdit.text.toString()
                if (phoneNumber.isEmpty()) {
                    onSubmit?.onError(
                        CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_REQUIRED)
                    )
                } else {
                    if (isMaxTen && phoneNumber.startsWith("0")) {
                        phoneNumber = phoneNumber.removePrefix(phoneNumber.substring(0, 1))
                    }
                    onSubmit?.onSubmit(mobileNumberDialCode.text.toString() + phoneNumber)
                }
            }

            back.setOnClickListener {
                onSubmit?.exit()
            }

            title.text = CacheUtil.getInstance()
                .getTranslations(Text.ENTER_PHONE_NUMBER_LABEL, "Enter Phone Number")
            descriptionLabel.text = CacheUtil.getInstance().getTranslations(
                Text.ENTER_NUMBER_DESCRIPTION,
                "We need you mobile number to help us contact you in case of an order query. Standard data charge may apply."
            )
            mobileNumberLabel.text =
                CacheUtil.getInstance().getTranslations(Text.MOBILE_NUMBER, "Mobile Number")
            sendCodeButton.text =
                CacheUtil.getInstance().getTranslations(Text.SEND_CODE_LABEL, "Send Code")

            phoneNumberEdit.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    sendCodeButton.performClick()
                    true
                } else false
            }

            phoneNumberEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (s != null) {
                        if (s.toString().startsWith("0")) {
                            setPhoneNumberLength(10)
                            isMaxTen = true
                        } else {
                            isMaxTen = false
                            setPhoneNumberLength(9)
                        }
                    }
                }
            })
        }
    }

    fun setPhoneNumberLength(length: Int) {
        val fArray = arrayOfNulls<InputFilter>(1)
        fArray[0] = InputFilter.LengthFilter(length)
        binding.phoneNumberEdit.filters = fArray
    }

    companion object {
        fun newInstance(listener: OnSubmit): AddNumberDialog {
            val choosePickupTypeDialogFragment = AddNumberDialog()
            choosePickupTypeDialogFragment.onSubmit = listener
            return choosePickupTypeDialogFragment
        }
    }

    fun hideKeyboard(view: View) {
        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm!!.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
