package com.herfy.loyalty

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.herfy.R
import com.herfy.base.BaseDialogFragment
import com.herfy.data.constants.Text
import com.herfy.databinding.DialogRedeemLoyaltyBinding
import com.skylinedynamics.solosdk.api.models.objects.Loyalty
import com.herfy.util.AuthUtil
import com.herfy.util.CacheUtil
import com.herfy.util.StringUtil
import com.herfy.util.ViewUtil

class RedeemLoyaltyDialog(var loyalty: Loyalty?) : BaseDialogFragment() {

    private var _onAction: OnAction? = null
    private var _binding: DialogRedeemLoyaltyBinding? = null

    private val onAction get() = _onAction ?: throw Error("onAction should not be null")
    private val binding get() = _binding ?: throw Error("binding should not be null")

    var loyaltyHandler = Handler(Looper.getMainLooper())
    var loyaltyRunnable: Runnable? = null


    companion object {
        fun newInstance(loyalty: Loyalty?): RedeemLoyaltyDialog {
            return RedeemLoyaltyDialog(loyalty)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        _binding = DialogRedeemLoyaltyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadPreferences()
    }

    private fun loadPreferences() {
        setupViews()
        setupTranslations()
    }


    private fun setupViews() {
        binding.apply {

            loyalty?.let {
                if (it.attributes.currentBalanceAmount.toInt().toDouble() == it.attributes.currentBalanceAmount) {
                    binding.loyaltyLabel.text = CacheUtil.getInstance()
                        .getTranslations(Text.POINTS_CONVERSION_LABEL, "(x) Points ((y))").replace(
                            "(x)",
                            AuthUtil.getInstance().customerLoyalty.attributes.currentBalance.toString()
                        ).replace(
                            "(y)",
                            StringUtil.localize(it.attributes.currentBalanceAmount.toInt().toString()) + " " + CacheUtil.getInstance().getTranslations(CacheUtil.getInstance().concept.currencyCode.lowercase(java.util.Locale.getDefault())).uppercase())

                } else {
                    binding.loyaltyLabel.text = CacheUtil.getInstance()
                        .getTranslations(Text.POINTS_SAR_LABEL, "(x) Points ((y))").replace(
                            "(x)",
                            AuthUtil.getInstance().customerLoyalty.attributes.currentBalance.toString()
                        ).replace(
                            "(y)",
                            StringUtil.getFormattedAmount(it.attributes.currentBalanceAmount) + " " + CacheUtil.getInstance().getTranslations(CacheUtil.getInstance().concept.currencyCode.lowercase(java.util.Locale.getDefault())).uppercase())

                }
            }

            btnUseAll.setOnClickListener {
                loyalty?.let {
                    val points = it.attributes.currentBalanceAmount.toInt()
                    binding.pointsInSar.setText(StringUtil.localize(points.toString()))
                }
            }

            btnCancel.setOnClickListener {
                dismiss()
                onAction.cancel()
            }

            disableApply()

            btnApply.setOnClickListener {
                if (pointsInSar.text.toString().isNotEmpty()) {
                    onAction.navigate(StringUtil.localize(pointsInSar.text.toString()).toInt())
                }
            }

            pointsInSar.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence, i: Int, i1: Int, i2: Int
                ) {
                }

                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                    loyaltyRunnable?.let {
                        loyaltyHandler.removeCallbacks(it)
                    }
                }

                override fun afterTextChanged(editable: Editable) {
                    loyaltyRunnable?.let {
                        loyaltyHandler.removeCallbacks(it)
                    }
                    loyaltyRunnable = buildLoyaltyRunnable(this, editable, loyalty)
                    loyaltyHandler.postDelayed(loyaltyRunnable!!, 500)

                }
            })

        }
    }

    private fun buildLoyaltyRunnable(
        textWatcher: TextWatcher, editable: Editable, loyalty: Loyalty?
    ): Runnable {
        return Runnable {
            if (dialog != null && dialog!!.isShowing) {
                try {
                    if (editable.toString().isNotEmpty() && loyalty != null) {

                        binding.tvErrorPointsInSar.visibility = View.GONE
                        binding.pointsInSar.removeTextChangedListener(textWatcher)
                        val userSetPoints = StringUtil.toEnglish(editable.toString()).toDouble()
                        val points = loyalty.attributes.currentBalance.toDouble()
                        val currentBalance = AuthUtil.getInstance().customerLoyalty.attributes.currentBalanceAmount.toInt()
                        val pointConversion = AuthUtil.getInstance().customerLoyalty.attributes.pointConversion.toDouble()
                        binding.pointsInSar.setText(
                            StringUtil.localize(
                                binding.pointsInSar.text.toString()
                            )
                        )
                        binding.pointsInSar.setSelection(
                            binding.pointsInSar.text.toString().length
                        )

                        if (userSetPoints <= 0) {
                            binding.tvErrorPointsInSar.visibility = View.VISIBLE
                            binding.tvErrorPointsInSar.text =
                                CacheUtil.getInstance().getTranslations(
                                    Text.ZERO_POINTS_MESSAGE,
                                    "You have entered 0 points please enter the points to be used"
                                )
                            disableApply()
                        } else if (userSetPoints > points || userSetPoints > currentBalance) {
                            binding.tvErrorPointsInSar.visibility = View.VISIBLE
                            binding.tvErrorPointsInSar.text =
                                CacheUtil.getInstance().getTranslations(
                                    Text.INSUFFICIENT_POINTS, "You have insufficient points"
                                )
                            disableApply()
                        } else if (userSetPoints == points) {
                            enableApply()
                        } else {
                            enableApply()
                        }
                        binding.pointsInSar.addTextChangedListener(textWatcher)
                    } else {
                        disableApply()
                    }
                } catch (ex: java.lang.Exception) {
                    ex.printStackTrace()
                }
                loyaltyRunnable?.let { loyaltyHandler.removeCallbacks(it) }
            }
        }
    }

    private fun setupTranslations() {
        val cacheUtil = CacheUtil.getInstance()
        binding.apply {
            btnCancel.text = cacheUtil.getTranslations(Text.CANCEL, "CANCEL").uppercase()
            btnUseAll.text = cacheUtil.getTranslations(Text.USE_ALL, "USE ALL".uppercase())
            btnApply.text = cacheUtil.getTranslations(Text.APPLY, "APPLY").uppercase()
            howMuchLabel.text = cacheUtil.getTranslations(
                Text.REDEEM_AMOUNT_IN, "HOW MANY POINTS WOULD YOU LIKE TO REDEEM?"
            ).replace("(X)", CacheUtil.getInstance().getTranslations(CacheUtil.getInstance().concept.currencyCode.lowercase(java.util.Locale.getDefault())).uppercase())
            availableLabel.text = cacheUtil.getTranslations(
                Text.AVAILABLE_LOYALTY_POINTS, "Available Loyalty Points"
            )
            pointsInSar.hint = cacheUtil.getTranslations(
                Text.ENTER_POINTS_HERE, "Enter points amount here"
            )
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.setLayout(width, height)
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
            val typedValue = TypedValue()
            dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            dialog.window!!.statusBarColor = typedValue.data
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            val ft = manager.beginTransaction()
            ft.add(this, tag)
            ft.commitAllowingStateLoss()
        } catch (e: IllegalStateException) {
            Log.d("ABSDIALOGFRAG", "Exception", e)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        _onAction = when {
            context is OnAction -> context
            activity is OnAction -> activity as OnAction
            else -> null
        }
    }

    override fun onDestroy() {
        _binding = null
        _onAction = null
        super.onDestroy()

    }

    private fun enableApply() {
        binding.btnApply.isEnabled = true
        context?.let {
            binding.btnApply.backgroundTintList = ColorStateList.valueOf(ViewUtil.getColorMain(it))
        }
    }

    private fun disableApply() {
        binding.btnApply.isEnabled = false
        context?.let {
            binding.btnApply.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    it, R.color.gray_light
                )
            )
        }
    }


    interface OnAction {
        fun navigate(points: Int)
        fun cancel()
    }
}