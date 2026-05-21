package com.herfy.cart.views

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.cart.CartContract
import com.herfy.cart.adapters.CartPromoAdapter
import com.herfy.cart.adapters.CartPromoAdapter.CartPromoListener
import com.herfy.cart.databinding.FragmentCouponsBinding
import com.herfy.common.dialog.CommonCustomDialog
import com.herfy.ext.cast
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.runOnMain
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.Campaign

class CartPromoDialogFragment : DialogFragment(), CartPromoListener {

    private lateinit var binding: FragmentCouponsBinding
    private var onAction: OnAction? = null
    private var cartPromoAdapter: CartPromoAdapter? = null
    private var campaigns: List<Campaign> = arrayListOf()
    private var donTriggerClose = false
    private var selected = 0
    private var loading = false
    private var cartPresenter: CartContract.Presenter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = FragmentCouponsBinding.inflate(inflater)


        return binding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!donTriggerClose) {
            if (onAction != null) {
                onAction!!.onClose()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupTranslations()
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
            dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            dialog.window!!.statusBarColor = typedValue.data
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        onAction!!.onClose()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setupViews() {
        binding.apply {
            toolbarTitle.setNavButtonAction {
                dismissAllowingStateLoss()
            }
            val currentCartTotal = cartPresenter?.menuItemsTotalPrice ?: 0.0
            cartPromoAdapter = CartPromoAdapter(
                campaigns = campaigns,
                listener = this@CartPromoDialogFragment,
                currentCartAmount = currentCartTotal
            )
            cartPromoAdapter?.selected = selected
            rvDigitalCoupons.apply {
                adapter = cartPromoAdapter
            }

            placeholderCoupons.shimmerCouponItems.visibility = View.GONE

            if (campaigns.isNotEmpty()) {
                tvApplyCoupons.visibility = View.VISIBLE
                rvDigitalCoupons.visibility = View.VISIBLE
                emptyStateView.visibility = View.GONE
                cartPromoAdapter?.notifyDataSetChanged()
            } else {
                tvApplyCoupons.visibility = View.GONE
                emptyStateView.visibility = View.VISIBLE
                rvDigitalCoupons.visibility = View.GONE
            }

            btnApply.setOnClickListener {
                showLoading()
                val couponCode = binding.tieCoupon.text.toString()
                if (couponCode.isEmpty()) {
                    dismissLoading()
                    showInvalidCouponDialog(
                        CacheUtil.getInstance().getTranslations(
                            Text.PLEASE_INPUT_A_COUPON_CODE_FIRST,
                            "Coupon code is required"
                        )
                    )
                    return@setOnClickListener
                }

                if (couponCode.isNotEmpty()) {
                    cartPresenter?.validateDigitalCoupon(couponCode) { valitedResult ->
                        runOnMain {
                            dismissLoading()
                            if (valitedResult.isSuccess) {
                                onAction?.onApplied()
                            } else {
                                showInvalidCouponDialog(valitedResult.message)
                            }
                        }
                    }
                } else {
                    runOnMain {
                        dismissLoading()
                        showInvalidCouponDialog(
                            CacheUtil.getInstance().getTranslations(
                                Text.PLEASE_INPUT_A_COUPON_CODE_FIRST,
                                "Coupon code is required"
                            )
                        )
                    }
                }
            }

        }
    }

    private fun showLoading() {
        dialog?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
        activity?.cast<BaseActivity> {
            BaseActivity.hideKeyboardFrom(it, binding.root)
        }
        binding.progressContainer.visibility = View.VISIBLE
    }

    private fun dismissLoading() {
        binding.progressContainer.visibility = View.GONE
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    fun setupFonts() {}
    fun setupTranslations() {
        binding.apply {
            toolbarTitle.title =
                CacheUtil.getInstance().getTranslations(Text.APPLY_COUPONS_LABEL, "Apply Coupons")
            tvEnterCoupon.text = CacheUtil.getInstance()
                .getTranslations(Text.ENTER_COUPON_CODE, "Enter coupon code?")
            tvOr.text = CacheUtil.getInstance().getTranslations(Text.OR, "Or")
            tvApplyCoupons.text = CacheUtil.getInstance()
                .getTranslations(Text.APPLY_DIGITAL_COUPONS, "Apply Digital Coupons")
            btnApply.text = CacheUtil.getInstance().getTranslations(Text.APPLY, "Apply")
            txtEmptyTitle.text =
                CacheUtil.getInstance().getTranslations(Text.NO_COUPONS_FOUND, "No coupons found.")
        }
    }

    override fun select(campaign: Campaign, position: Int) {
        onAction?.onAvail(true, position)
    }

    override fun error() {
        try {
            activity?.cast<BaseActivity> {
                it.showAlertDialog(
                    "", CacheUtil.getInstance().getTranslations(
                        Text.PROMO_ALREAD_USED_MESSAGE_KEY,
                        "Sorry the promo is already used on this device"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showInvalidCouponDialog(message: String) {
        context?.let {
            if (message.isNotEmpty()) {
                val commonCustomDialog = CommonCustomDialog(
                    it,
                    title = CacheUtil.getInstance()
                        .getTranslations(Text.INVALID_FOR_THIS_ORDER, "Invalid for this Order"),
                    message = message,
                    icon = com.herfy.common.R.drawable.ic_dialog_error,
                    buttonALabel = CacheUtil.getInstance()
                        .getTranslations(Text.GOT_IT, "Got it"),
                    buttonBLabel = CacheUtil.getInstance()
                        .getTranslations(Text.ADD_MORE_ITEMS, "Add more items"),
                )
                commonCustomDialog.show()
                commonCustomDialog.setCancelable(false)
                commonCustomDialog.setButtonAListener {
                    commonCustomDialog.dismiss()
                }
                commonCustomDialog.setButtonBListener {
                    onAction?.goToMenu()
                    commonCustomDialog.dismiss()
                }
            }
        }

    }

    fun updateCampaigns(
        loading: Boolean,
        b: Boolean,
        selected: Int,
        digitalCoupons: List<Campaign>
    ) {
        if (!b) {
            cartPromoAdapter?.updateCampaigns(selected, digitalCoupons)
        }
    }

    interface OnAction {
        fun onClose()
        fun onAvail(removeCoupon: Boolean, selected: Int)
        fun onRemove()
        fun goToMenu()
        fun onApplied()
    }

    companion object {
        fun newInstance(
            loading: Boolean,
            selected: Int,
            campaigns: List<Campaign>,
            onAction: OnAction?,
            cartPresenter: CartContract.Presenter
        ): CartPromoDialogFragment {
            val fragment = CartPromoDialogFragment()
            fragment.loading = loading
            fragment.campaigns = campaigns
            fragment.onAction = onAction
            fragment.selected = selected
            fragment.cartPresenter = cartPresenter
            return fragment
        }
    }
}
