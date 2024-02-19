package com.herfy.home.views

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.herfy.home.R
import com.herfy.home.adapter.PromotionRulesAdapter
import com.herfy.home.databinding.BottomSheetPromotionDetailBinding
import com.herfy.utils.base.mvvm.BaseBottomSheetDialogFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.isMet
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.ViewUtil
import com.herfy.utils.widgets.DividerItemDecorator
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.util.GsonHelper
import java.util.regex.Pattern


class PromotionDetailBottomSheet : BaseBottomSheetDialogFragment() {

    private var promo : Campaign? = null
    private lateinit var binding : BottomSheetPromotionDetailBinding
    private var action : onAction? = null
    private val cacheUtilHelper by lazy {
        context?.let {
            CacheUtilHelper(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            bundle.getString(KEY_PROMO)?.let {
                promo = GsonHelper.gsonFinal().fromJson(it, Campaign::class.java)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.bottom_sheet_promotion_detail,
            container,
            false
            )
        promo?.let {
            binding.promo = it

        }
        return binding.root
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun initViews() {
        super.initViews()
        promo?.let {promoData ->
            binding.apply {
                ibClose.setOnClickListener {
                    dismissAllowingStateLoss()
                }
                val cacheUtil = CacheUtil.getInstance()
                var orderTypeVal = cacheUtil.getTranslations(Text.DELIVERY)
                var orderTypeIconVal = R.drawable.ic_delivery_promo
                promoData.promotion?.attributes?.rules?.let {
                    val orderTypeRule = it.find { rule-> rule.ruleKey.equals("order-type", ignoreCase = true) }
                    if(it.isNotEmpty() && orderTypeRule != null){
                        val orderTypes = orderTypeRule.text.split(Pattern.compile(","))
                        when(orderTypes[0]){
                            "pickup" -> {
                                orderTypeVal = cacheUtil.getTranslations(Text.PICKUP)
                                orderTypeIconVal = R.drawable.ic_pickup_promo
                            }
                            "eat-in"-> {
                                orderTypeVal = cacheUtil.getTranslations(Text.DINE_IN)
                            }
                            "delivery"->{
                                orderTypeVal = cacheUtil.getTranslations(Text.DELIVERY)
                                orderTypeIconVal = R.drawable.ic_delivery_promo
                            }
                            "curbside"->{
                                orderTypeVal = cacheUtil.getTranslations(Text.CURBSIDE_PICKUP)
                                orderTypeIconVal = R.drawable.ic_drive_thru_promo
                            }
                            "drive-thru"->{
                                orderTypeVal = cacheUtil.getTranslations(Text.DRIVE_THRU)
                                orderTypeIconVal = R.drawable.ic_drive_thru_promo
                            }
                        }
                    }
                    orderType = orderTypeVal
                    orderTypeIcon = orderTypeIconVal
                }?: run {
                    orderType = orderTypeVal
                    orderTypeIcon = orderTypeIconVal
                }

                var toRemove = false

                cacheUtilHelper?.getCartQuantity {qty ->
                    if(qty > 0){
                        btnClaim.visibility = View.VISIBLE
                        promoData.promotion?.attributes?.rules?.let {
                            if (it.isMet()) {
                                try {
                                    if (promoData.id.equals(CacheUtil.getInstance().campaign.id, ignoreCase = true)) {
                                        toRemove = true
                                        btnClaim.text = CacheUtil.getInstance()
                                            .getTranslations(Text.REMOVE_PROMO_CAPS, "REMOVE PROMO")
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }else {
                        btnClaim.visibility = View.GONE
                    }
                }?: run {
                    btnClaim.visibility = View.GONE
                }

                btnClaim.setOnClickListener {
                    if (!toRemove) {
                        if (CacheUtil.getInstance().couponCode != null) {
                            try {
                                showPromptAlertDialog(
                                    "",
                                    CacheUtil.getInstance().getTranslations(
                                        Text.ONE_DISCOUNT_ONLY_MESSAGE,
                                        "You can only apply one discount at a time. Currently applied discount will be removed"
                                    ),
                                    CacheUtil.getInstance().getTranslations(
                                        Text.YES_CAPS
                                    ),
                                    { dialogInterface, i ->
                                        CacheUtil.getInstance().couponCode = null
                                        CacheUtil.getInstance().couponName = null
                                        CacheUtil.getInstance().couponMenuItem = null
                                        CacheUtil.getInstance().couponToAdd = null
                                        action?.onClickClaimPromo(promoData)
                                        dismiss()
                                    },
                                    CacheUtil.getInstance().getTranslations(Text.NO_CAPS)
                                ) { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() }
                            } catch (e: Exception) {
                                if (CacheUtil.getInstance().couponCode != null) {
                                    CacheUtil.getInstance().couponCode = null
                                    CacheUtil.getInstance().couponName = null
                                    CacheUtil.getInstance().couponMenuItem = null
                                    CacheUtil.getInstance().couponToAdd = null
                                    action?.onClickClaimPromo(promoData)
                                    dismiss()
                                }
                                e.printStackTrace()
                            }
                        }
                    } else {
                        action?.onClickClaimPromo(promoData)
                        dismissAllowingStateLoss()
                    }




                    action?.onClickClaimPromo(promoData)
                }


                promoData.promotion?.attributes?.rules?.let {
                    if(it.isNotEmpty()){
                        val promotionRulesAdapter = PromotionRulesAdapter(it)
                        val divider =
                        rvRequirements.apply {
                            adapter = promotionRulesAdapter
                            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                            setHasFixedSize(true)
                            ContextCompat.getDrawable(context, R.drawable.divider_promo_rules)?.let {divider ->
                                addItemDecoration(DividerItemDecorator(divider))
                            }
                        }
                        promotionRulesAdapter.notifyDataSetChanged()
                    }
                }

            }
        }

    }
    override fun initTranslations() {
        super.initTranslations()
        binding.apply {
            btnClaim.text = CacheUtil.getInstance().getTranslations(Text.REDEEM_LABEL, "Redeem")
            tvRequirements.text = CacheUtil.getInstance().getTranslations(Text.REQUIREMENTS, "Requirements")
        }
    }

    companion object {
        const val KEY_PROMO = "key_promo"
        @JvmStatic
        fun newInstance(promo: Campaign, action: onAction) =
            PromotionDetailBottomSheet().apply {
                this.action = action
                arguments = Bundle().apply {
                    putString(KEY_PROMO, GsonHelper.gsonFinal().toJson(promo))
                }
            }
    }

    interface onAction{
        fun onClickClaimPromo(promo: Campaign)
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog =
                dialogInterface as BottomSheetDialog
            setupFullHeight(bottomSheetDialog)
        }
        return dialog
    }


    private fun setupFullHeight(bottomSheetDialog: BottomSheetDialog) {
        val bottomSheet =
            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(
            bottomSheet!!
        )
        val layoutParams = bottomSheet.layoutParams
        activity?.let {
            val windowHeight = ViewUtil.getScreenHeight(it)
            if (layoutParams != null) {
                layoutParams.height = windowHeight
            }
        }
        bottomSheet.layoutParams = layoutParams
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }
}