package com.herfy.home.views

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.herfy.home.R
import com.herfy.home.adapter.PromotionOrderTypeAdapter
import com.herfy.home.adapter.PromotionRulesAdapter
import com.herfy.home.databinding.BottomSheetPromotionDetailBinding
import com.herfy.model.PromotionOrderType
import com.herfy.utils.base.mvvm.BaseBottomSheetDialogFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.ViewUtil
import com.herfy.utils.widgets.DividerItemDecorator
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.util.GsonHelper
import java.util.regex.Pattern


class PromotionDetailBottomSheet : BaseBottomSheetDialogFragment() {

    private var promo: Campaign? = null
    private lateinit var binding: BottomSheetPromotionDetailBinding
    private var action: onAction? = null
    private val cacheUtilHelper by lazy {
        context?.let {
            CacheUtilHelper(it)
        }
    }
    private val promotionOrderTypeAdapter by lazy {
        PromotionOrderTypeAdapter()
    }

    private val promoOrderTypeList: ArrayList<PromotionOrderType> by lazy {
        val cacheUtil = CacheUtil.getInstance()
        val defaultOrderTypes = cacheUtil.application.attributes.getOrderTypes()
        val typeList = arrayListOf<PromotionOrderType>()
        defaultOrderTypes.find { t -> t == OrderType.DELIVERY }?.let {
            typeList.add(
                PromotionOrderType(
                    cacheUtil.getTranslations(Text.DELIVERY),
                    R.drawable.ic_delivery_promo
                )
            )
        }
        defaultOrderTypes.find { t -> t == OrderType.PICKUP }?.let {
            typeList.add(
                PromotionOrderType(
                    cacheUtil.getTranslations(Text.PICKUP),
                    R.drawable.ic_pickup_promo
                )
            )
        }
        defaultOrderTypes.find { t -> t == OrderType.EAT_IN || t == OrderType.DINE_IN }?.let {
            typeList.add(
                PromotionOrderType(
                    cacheUtil.getTranslations(Text.DINE_IN),
                    R.drawable.ic_dine_in_promo
                )
            )
        }
        defaultOrderTypes.find { t -> t == OrderType.CURBSIDE }?.let {
            typeList.add(
                PromotionOrderType(
                    cacheUtil.getTranslations(Text.CURBSIDE_PICKUP),
                    R.drawable.ic_drive_thru_promo
                )
            )
        }
        defaultOrderTypes.find { t -> t == OrderType.DRIVE_THRU }?.let {
            typeList.add(
                PromotionOrderType(
                    cacheUtil.getTranslations(Text.DRIVE_THRU),
                    R.drawable.ic_drive_thru_promo
                )
            )
        }
        typeList
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
        binding = DataBindingUtil.inflate(
            inflater,
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
        promo?.let { promoData ->
            binding.apply {
                setupPromotionOrderType(promoData)
                ibClose.setOnClickListener {
                    dismissAllowingStateLoss()
                }
                val cacheUtil = CacheUtil.getInstance()
                val cacheCampaign = cacheUtil.campaign
                var toRemove = false
                cacheUtilHelper?.getCartQuantity { qty ->
                    if (qty > 0) {
                        btnClaim.visible()
                        if (cacheCampaign != null && promoData.id.equals(
                                cacheCampaign.id,
                                ignoreCase = true
                            )
                        ) {
                            toRemove = true
                            btnClaim.text = CacheUtil.getInstance()
                                .getTranslations(Text.REMOVE_PROMO_LABEL, "Remove Promo")
                        }
                    }
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

                    it.find { r-> r.ruleKey.equals("total.gte", ignoreCase = true)  }?.let { gte ->
                        fmPromoCategory.visible()
                        totalGte = gte.text
                    }

                    val requirements = it.filter { r -> r.ruleKey != "order-type" && !r.ruleKey.equals("total.gte", ignoreCase = true) }
                    if (requirements.isNotEmpty()) {

                        val promotionRulesAdapter = PromotionRulesAdapter(requirements)

                        rvRequirements.apply {
                            adapter = promotionRulesAdapter
                            layoutManager = LinearLayoutManager(
                                context,
                                LinearLayoutManager.VERTICAL,
                                false
                            )
                            setHasFixedSize(true)
                            ContextCompat.getDrawable(context, R.drawable.divider_promo_rules)
                                ?.let { divider ->
                                    addItemDecoration(DividerItemDecorator(divider))
                                }
                        }
                        promotionRulesAdapter.notifyDataSetChanged()
                    } else {
                        tvRequirements.gone()
                        rvRequirements.gone()
                    }
                } ?: run {
                    tvRequirements.gone()
                    rvRequirements.gone()
                }

            }
        }

    }

    private fun setupPromotionOrderType(promoData: Campaign) {


        context?.let {
            val flexBoxLayoutManager = FlexboxLayoutManager(it)
            flexBoxLayoutManager.flexDirection = FlexDirection.ROW
            flexBoxLayoutManager.flexWrap = FlexWrap.WRAP
            flexBoxLayoutManager.justifyContent = JustifyContent.FLEX_START
            binding.rvPromoOrderTypes.apply {
                adapter = promotionOrderTypeAdapter
                layoutManager = flexBoxLayoutManager
                setHasFixedSize(true)
                isNestedScrollingEnabled = false
            }
        }

        val cacheUtil = CacheUtil.getInstance()
        promoData.promotion?.attributes?.rules?.let {
            val orderTypeRule = it.find { rule -> rule.ruleKey.equals("order-type", ignoreCase = true) }
            if (orderTypeRule != null) {
                promoOrderTypeList.clear()
                val orderTypes = orderTypeRule.text.split(Pattern.compile(","))
                for(type in orderTypes){
                    var orderTypeIcon = R.drawable.ic_delivery_promo
                    var orderTypeLabel = type.trim()
                    when(type.trim()){
                        "delivery" -> {
                            orderTypeLabel = cacheUtil.getTranslations(Text.DELIVERY)
                            orderTypeIcon = R.drawable.ic_delivery_promo
                        }
                        "pickup" -> {
                            orderTypeLabel = cacheUtil.getTranslations(Text.PICKUP)
                            orderTypeIcon = R.drawable.ic_pickup_promo
                        }
                        "eat-in" -> {
                            orderTypeLabel = cacheUtil.getTranslations(Text.DINE_IN)
                            orderTypeIcon = R.drawable.ic_dine_in_promo
                        }
                        "curbside" -> {
                            orderTypeLabel = cacheUtil.getTranslations(Text.CURBSIDE_PICKUP)
                            orderTypeIcon = R.drawable.ic_drive_thru_promo
                        }
                        "drive-thru"->{
                            orderTypeLabel = cacheUtil.getTranslations(Text.DRIVE_THRU,"Drive Thru")
                            orderTypeIcon = R.drawable.ic_drive_thru_promo
                        }
                    }

                    promoOrderTypeList.add(PromotionOrderType(
                        orderTypeLabel,
                        orderTypeIcon
                    ))
                }
            }
        }
        promotionOrderTypeAdapter.updateList(promoOrderTypeList)
    }

    override fun initTranslations() {
        super.initTranslations()
        binding.apply {
            btnClaim.text = CacheUtil.getInstance().getTranslations(Text.REDEEM_LABEL, "Redeem")
            tvRequirements.text =
                CacheUtil.getInstance().getTranslations(Text.REQUIREMENTS, "Requirements")
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

    interface onAction {
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
                layoutParams.height = LayoutParams.MATCH_PARENT
            }
        }
        bottomSheet.layoutParams = layoutParams
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }


}