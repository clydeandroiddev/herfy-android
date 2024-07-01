package com.herfy.checkout

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.JsonObject
import com.herfy.checkout.databinding.BottomSheetAlrhajiBinding
import com.herfy.common.base.BaseBottomSheetDialogFragment
import com.herfy.model.ErrorMsgItem
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.invisible
import com.herfy.utils.extensions.isVisible
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.StringUtil
import com.skylinedynamics.solosdk.util.GsonHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


const val KEY_TOTAL = "total"
const val KEY_CART_BODY = "cart"
const val KEY_DELIVERY_CHARGE = "delivery_charge"

@AndroidEntryPoint
class BottomSheetAlrhaji : BaseBottomSheetDialogFragment() {

    val viewModel: AlrhajiOTPViewModel by viewModels()

    private lateinit var binding: BottomSheetAlrhajiBinding

    @Inject
    lateinit var cacheUtilHelper: CacheUtilHelper

    private var invalidOTPDialog: InvalidOTPDialog? = null

    private var amountTextWatcher: TextWatcher? = null

    private var amountCount = -1
    private var total = 0.0
    private var cartBody = ""
    private var deliveryCharge = 0.0
    private var number = ""
    private var mBottomSheetListener: OTPAlrahjiListener? = null
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogStyle)

        arguments?.let {
            total = it.getDouble(KEY_TOTAL, 0.0)
            cartBody = it.getString(KEY_CART_BODY, "")
            deliveryCharge = it.getDouble(KEY_DELIVERY_CHARGE, 0.0)
        }
        observe()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetAlrhajiBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(binding)
        setupTranslations(binding)
    }

    fun setupViews(binding: BottomSheetAlrhajiBinding) {
        binding.apply {
            ibClose.setOnClickListener {
                mBottomSheetListener?.onClose()
            }
            val phoneTextWatcher = object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {
                }

                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
                override fun afterTextChanged(editable: Editable) {
                    etMobileNumber.removeTextChangedListener(this)
                    try {
                        if (etCicNumber.text.isNotEmpty() && etMobileNumber.text.isNotEmpty()) {
                            etCicNumber.setText("")
                        }
                        etMobileNumber.setText(
                            StringUtil.localize(
                                etMobileNumber.getText().toString()
                            )
                        )
                        val length = editable.length
                        etMobileNumber.setSelection(length)
                    } catch (e: Exception) {
                    }
                    etMobileNumber.addTextChangedListener(this)
                }
            }
            etMobileNumber.addTextChangedListener(phoneTextWatcher)

            val cicNumberTextWatcher = object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {
                }

                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
                override fun afterTextChanged(editable: Editable) {
                    try {
                        if (etMobileNumber.text.isNotEmpty() && etCicNumber.text.isNotEmpty()) {
                            etMobileNumber.setText("")
                        }
                        etCicNumber.removeTextChangedListener(this)
                        etCicNumber.setText(
                            StringUtil.localize(
                                etCicNumber.getText().toString()
                            )
                        )
                        val length = editable.length
                        etCicNumber.setSelection(length)
                    } catch (e: Exception) {
                    }
                    etCicNumber.addTextChangedListener(this)
                }
            }
            etCicNumber.addTextChangedListener(cicNumberTextWatcher)


            amountTextWatcher = object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence, i: Int, i1: Int, i2: Int
                ) {
                }

                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                    if (tvAmountError.isVisible()) {
                        tvAmountError.gone()
                    }
                }

                override fun afterTextChanged(editable: Editable) {
                    if (editable.length > 0) {
                        val str: String = etAmount.getText().toString()
                        etAmount.setOnKeyListener(View.OnKeyListener { v: View?, keyCode: Int, event: KeyEvent? ->
                            if (keyCode == KeyEvent.KEYCODE_DEL) {
                                amountCount--
                                val fArray = arrayOfNulls<InputFilter>(1)
                                fArray[0] = InputFilter.LengthFilter(100)
                                etAmount.setFilters(fArray)
                            }
                            false
                        })
                        val t = str[editable.length - 1]
                        if (t == '.') {
                            amountCount = 0
                        }
                        if (amountCount >= 0) {
                            if (amountCount == 2) {
                                val fArray = arrayOfNulls<InputFilter>(1)
                                fArray[0] = InputFilter.LengthFilter(editable.length)
                                etAmount.setFilters(fArray)
                            }
                            amountCount++
                        }
                    }
                    etAmount.removeTextChangedListener(this)
                    try {
                        etAmount.setText(
                            StringUtil.localize(
                                etAmount.text.toString()
                            )
                        )
                        val length = editable.length
                        etAmount.setSelection(length)
                    } catch (e: Exception) {
                    }
                    etAmount.addTextChangedListener(this)
                }
            }
            etAmount.addTextChangedListener(amountTextWatcher)
            cvResend.setOnClickListener {
                if (number.isNotEmpty()) {
                    cvResend.isEnabled = false
                    viewModel.sendOTP(number)
                }

            }

            btnRequest.setOnClickListener {
                val _number = getNumber()
                if (_number == null || _number.isEmpty()) {
                    number = ""
                    showMessage(
                        cacheUtilHelper.getTranslations(
                            Text.MOBILE_OR_CIC_REQUIRED,
                            "Please input either your Mobile Number or Loyalty CIC Number to request an OTP."
                        )
                    )
                } else if (etMobileNumber.text.toString()
                        .isNotEmpty() && !StringUtil.isValidPhoneNumber(etMobileNumber.text.toString())
                ) {
                    number = ""
                    showMessage(cacheUtilHelper.getTranslations(Text.PHONE_NUMBER_INVALID))
                } else {
                    number = _number
                    viewModel.sendOTP(number)
                }

            }

            btnSubmit.setOnClickListener {
                val totalMinusDC = total - deliveryCharge
                val amount = etAmount.text.toString().toDouble()
                val o = StringUtil.toEnglish(otpAlrhaji.text.toString()) ?: ""
                val a = StringUtil.toEnglish(etAmount.text.toString()) ?: ""
                if (o.isEmpty()) {
                    otpAlrhaji.requestFocus()
                    showMessage(
                        CacheUtil.getInstance()
                            .getTranslations(Text.OTP_REQUIRED, "OTP is required")
                    )
                } else if (a.isEmpty()) {
                    etAmount.requestFocus()
                    showMessage(
                        CacheUtil.getInstance()
                            .getTranslations(Text.AMOUNT_REQUIRED, "Amount is required")
                    )
                } else if (amount > totalMinusDC) {
                    etAmount.text.clear()
                    tvAmountError.visible()
                } else {
                    viewModel.verifyOTP(
                        o, a, GsonHelper.gsonFinal().fromJson(
                            cartBody,
                            JsonObject::class.java
                        )
                    )
                }
            }

            tvEditMobileNumber.setOnClickListener {
                countDownTimer?.cancel()
                countDownTimer = null
                setLayoutInputOTP(false)
                setLayoutInputNumber(true)
            }

            val formattedPrice: String = StringUtil.getFormattedPrice(total).toString()
            txtTotalCartLabel.text = cacheUtilHelper.getTranslations(
                Text.TOTAL_CART_VALUE,
                "Total Cart Value:"
            ) + " " + formattedPrice
        }
    }

    private fun setupTranslations(binding: BottomSheetAlrhajiBinding) {
        binding.apply {
            tvAlrhaji.text = cacheUtilHelper.getTranslations(
                Text.ALRHAJI_LABEL, "Al Rhaji Bank (Mokafa's Rewards)"
            )
            tvAlrhajiDescription.text = cacheUtilHelper.getTranslations(
                Text.ALRHAJI_DESCRIPTION,
                "The user has to be a registered mobile number with Mokafa'a Al Rajhi Program, otherwise the redemption will not be successful."
            )
            tvMobileNumberLabel.text =
                cacheUtilHelper.getTranslations(Text.MOBILE_NUMBER, "Mobile Number")
            tvCicNumberLabel.text =
                cacheUtilHelper.getTranslations(Text.CIC_NUMBER_LABEL, "CIC Number")
            tvOr.text = cacheUtilHelper.getTranslations(Text.OR, "OR")
            btnRequest.text = cacheUtilHelper.getTranslations(Text.REQUEST_OTP, "Request OTP")
            tvInputOtp.text = cacheUtilHelper.getTranslations(
                Text.ENTER_OTP_LABEL, "Enter the 4 digit code sent to your number"
            )
            tvEditMobileNumber.text = cacheUtilHelper.getTranslations(Text.EDIT, "Edit")
            tvResendCode.text =
                cacheUtilHelper.getTranslations(Text.DID_NOT_GE_CODE, "Did not get the code?")
            tvAmountLabel.text = cacheUtilHelper.getTranslations(Text.AMOUNT_SAR, "Amount (SAR)")
            btnSubmit.text = cacheUtilHelper.getTranslations(Text.SUBMIT_OTP, "Submit OTP")
            tvAmountError.text = cacheUtilHelper.getTranslations(
                Text.ALRHAJI_AMOUNT_ERROR,
                "Amount cannot exceed the total amount"
            )
            tvResend.text = cacheUtilHelper.getTranslations(Text.SEND_AGAIN, "Send Again")
        }
    }

    private fun observe() {
        viewModel.isLoading.observe(this) {
            it.getContentIfNotHandled()?.let { loading ->
                if (loading) {
                    showLoadingDialog()
                } else {
                    dismissLoadingDialog()
                }
            }

        }
        viewModel.showError.observe(this) {
            showInvalidOTPDialog(it)
        }

        viewModel.showCountdown.observe(this) { time ->
            binding.apply {
                setLayoutInputNumber(false)
                setLayoutInputOTP(true)
                setCountdown(time)
            }

        }

        viewModel.showVerified.observe(this) {
            it.getContentIfNotHandled()?.let { data ->
                mBottomSheetListener?.otpResult(
                    transactionId = data.transactionId,
                    amount = data.amount,
                    invalidMukafaa = false
                )
            }
        }
    }

    private fun setLayoutInputOTP(show: Boolean) {
        binding.apply {
            if (show) {
                lnInputOtp.visible()
                tvMobileNumberValue.text = number
            } else {
                lnInputOtp.gone()
            }
        }
    }

    private fun setLayoutInputNumber(show: Boolean) {
        binding.apply {
            if (show) {
                lnInputNumber.visible()
                tvAlrhajiDescription.visible()
            } else {
                lnInputNumber.gone()
                tvAlrhajiDescription.invisible()
            }
        }

    }

    private fun showInvalidOTPDialog(errorMsgItem: ErrorMsgItem) {
        invalidOTPDialog?.dismiss()
        invalidOTPDialog = null

        invalidOTPDialog = InvalidOTPDialog.newInstance(
            errorMsgItem.message
        ) {
            if (errorMsgItem.closePage) {
                mBottomSheetListener?.otpResult("", "", true)
            }
            invalidOTPDialog?.dismiss()
        }
        invalidOTPDialog?.isCancelable = true
        invalidOTPDialog?.show(childFragmentManager, InvalidOTPDialog::class.java.toString())

    }

    private fun getNumber(): String? {
        binding.apply {
            val pnum: String = etMobileNumber.text.toString()
            val lnum: String = etCicNumber.text.toString()
            if (pnum.isNotEmpty()) {
                return StringUtil.toEnglish(pnum)
            } else if (lnum.isNotEmpty()) {
                return StringUtil.toEnglish(lnum)
            }
        }

        return null
    }

    private fun showMessage(message: String?) {
        //dismissDialogs()
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            mBottomSheetListener = context as OTPAlrahjiListener?
        } catch (e: ClassCastException) {
        }
    }

    private fun setCountdown(millis: Long) {
        if (countDownTimer != null)
            countDownTimer?.cancel();

        binding.cvResend.isEnabled = false
        countDownTimer = object : CountDownTimer(millis, 1000) {
            override fun onTick(p0: Long) {
                var second = (p0 / 1000).toInt()
                if (second == 0) {
                    second = 1
                }
                binding.tvResend.text = (CacheUtil.getInstance()
                    .getTranslations(Text.SEND_AGAIN_LABEL, "Send again ((x)s)").replace(
                        "(x)", StringUtil.localize(second.toString())
                    ))
            }

            override fun onFinish() {
                binding.tvResend.text =
                    (CacheUtil.getInstance().getTranslations(Text.SEND_AGAIN, "Send again"))
                binding.cvResend.isEnabled = true
            }
        }
        countDownTimer?.start();
    }


    companion object {

        @JvmStatic
        fun newInstance(total: Double, cartBody: String, deliveryCharge: Double) =
            BottomSheetAlrhaji().apply {
                arguments = Bundle().apply {
                    putDouble(KEY_TOTAL, total)
                    putString(KEY_CART_BODY, cartBody)
                    putDouble(KEY_DELIVERY_CHARGE, deliveryCharge)
                }
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog =
                dialogInterface as BottomSheetDialog
            val bottomSheet =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(
                bottomSheet!!
            )
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        return dialog
    }

    interface OTPAlrahjiListener {
        fun onClose()
        fun otpResult(transactionId: String, amount: String, invalidMukafaa: Boolean)

    }
}