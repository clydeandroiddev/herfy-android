package com.herfy.home.views

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.herfy.home.R
import com.herfy.home.databinding.BottomSheetPromotionDetailBinding
import com.herfy.utils.base.mvvm.BaseBottomSheetDialogFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.util.GsonHelper


class PromotionDetailBottomSheet : BaseBottomSheetDialogFragment() {

    private var promo : Campaign? = null
    private lateinit var binding : BottomSheetPromotionDetailBinding
    private var action : onAction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            bundle.getString(KEY_PROMO)?.let {
                promo = GsonHelper.gsonFinal().fromJson(it, Campaign::class.java)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog?.setOnShowListener { it ->
            val d = it as BottomSheetDialog
            val bottomSheet =
                d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return super.onCreateDialog(savedInstanceState)
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


    override fun initViews() {
        super.initViews()
        binding.apply {
            btnClaim.setOnClickListener {
                dismissAllowingStateLoss()
            }
        }
    }
    override fun initTranslations() {
        super.initTranslations()
        binding.apply {
            btnClaim.text = CacheUtil.getInstance().getTranslations(Text.CLAIM_PROMO, "Claim Promo")
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            ibClose.setOnClickListener {
                dismissAllowingStateLoss()
            }

            btnClaim.setOnClickListener {
                this@PromotionDetailBottomSheet.promo?.let {
                    action?.onClickClaimPromo(it)
                }

            }
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
}