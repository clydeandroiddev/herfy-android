package com.herfy.cart.views

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.herfy.cart.CartContract
import com.herfy.cart.adapters.CartPromoAdapter
import com.herfy.cart.adapters.CartPromoAdapter.CartPromoListener
import com.herfy.cart.databinding.FragmentCouponsBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.Campaign

class CartPromoDialogFragment : DialogFragment(), CartPromoListener {

    private lateinit var binding : FragmentCouponsBinding
    private var onAction: OnAction? = null
    private var cartPromoAdapter: CartPromoAdapter? = null
    private var campaigns: List<Campaign> = arrayListOf()
    private var donTriggerClose = false
    private var selected = 0
    private var loading = false
    private var cartPresenter : CartContract.Presenter? = null
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
        if (!donTriggerClose) onAction!!.onClose()
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
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog.window!!.setLayout(width, height)
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val typedValue = TypedValue()
                dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                dialog.window!!.statusBarColor = typedValue.data
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        onAction!!.onClose()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setupViews() {
        binding.apply {
            toolbarTitle.setNavButtonAction{
                dismissAllowingStateLoss()
            }
            val currentCartTotal = cartPresenter?.menuItemsTotalPrice?: 0.0
            cartPromoAdapter = CartPromoAdapter(campaigns = campaigns, listener = this@CartPromoDialogFragment, currentCartAmount =  currentCartTotal )
            cartPromoAdapter?.selected = selected
            rvDigitalCoupons.apply{
                adapter = cartPromoAdapter
            }
            placeholderCoupons.shimmerCouponItems.visibility = View.GONE
            if(campaigns.isNotEmpty()){
                rvDigitalCoupons.visibility = View.VISIBLE
                cartPromoAdapter?.notifyDataSetChanged()
            }else {
                empStayeView.visibility = View.VISIBLE
                rvDigitalCoupons.visibility = View.GONE

            }

            btnApply.setOnClickListener {
                showLoading()
                cartPresenter?.getCoupon
            }

        }
    }

    private fun showLoading() {
        activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        binding.progressContainer.visibility = View.VISIBLE
    }

    private fun dismissLoading(){
        binding.progressContainer.visibility = View.GONE
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    fun setupFonts() {}
    fun setupTranslations() {
        binding.apply {
            toolbarTitle.title = CacheUtil.getInstance().getTranslations(Text.APPLY_COUPONS_LABEL, "Apply Coupons")
            tvEnterCoupon.text = CacheUtil.getInstance().getTranslations(Text.ENTER_COUPON_CODE, "Enter coupon code?")
            tvOr.text = CacheUtil.getInstance().getTranslations(Text.OR, "Or")
            tvApplyCoupons.text = CacheUtil.getInstance().getTranslations(Text.APPLY_DIGITAL_COUPONS, "Apply Digital Coupons")
        }
    }
    override fun select(campaign: Campaign, position: Int) {
        if (cartPromoAdapter!!.selected == position) {
            cartPromoAdapter!!.selected = -1
        } else {
            if (CacheUtil.getInstance().campaign != null && CacheUtil.getInstance().campaign.id.equals(
                    campaign.id,
                    ignoreCase = true
                )
            ) {

            } else {

            }
            cartPromoAdapter!!.selected = position
        }
        cartPromoAdapter!!.notifyDataSetChanged()
    }

    interface OnAction {
        fun onClose()
        fun onAvail(removeCoupon: Boolean, selected: Int)
        fun onRemove()
    }

    companion object {
        fun newInstance(
            loading: Boolean,
            selected: Int,
            campaigns: List<Campaign>,
            onAction: OnAction?,
            cartPresenter : CartContract.Presenter
        ): CartPromoDialogFragment {
            val fragment = CartPromoDialogFragment()
            fragment.loading = loading
            fragment.campaigns = campaigns
            fragment.onAction = onAction
            fragment.selected = selected
            return fragment
        }
    }
}
