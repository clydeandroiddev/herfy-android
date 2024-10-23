package com.herfy.promotions.views

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.herfy.R
import com.herfy.base.BaseDialogFragment
import com.herfy.databinding.FragmentPromotionsDetailDialogBinding
import com.herfy.promotions.adapters.PromotionRulesAdapter
import com.herfy.util.ViewUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.isMet
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.PromotionRules
import java.util.Locale

class PromotionDetailsDialogFragment : BaseDialogFragment() {

    private lateinit var binding: FragmentPromotionsDetailDialogBinding

    private var rules: List<PromotionRules>? = ArrayList()

    private val adapter: PromotionRulesAdapter by lazy {
        PromotionRulesAdapter(emptyArray())
    }

    var onAction: OnAction? = null
    private var campaign: Campaign? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = FragmentPromotionsDetailDialogBinding.inflate(inflater, container, false)
        setupAdapter()
        binding.toolbar.title.text =
            CacheUtil.getInstance().getTranslations(Text.PROMOTIONS, "PROMOTIONS").uppercase(
                Locale.getDefault()
            )
        binding.toolbar.back.text = CacheUtil.getInstance().getTranslations(Text.BACK, "Back")
        binding.requirementsLabel.text =
            CacheUtil.getInstance().getTranslations(Text.REQUIREMENTS, " Requirements")
        binding.avail.text =
            CacheUtil.getInstance().getTranslations(Text.REDEEM_THIS_PROMO, "REDEEM THIS PROMO")
        binding.digitalName.text = campaign?.attributes?.name
        binding.digitalDesc.text = campaign?.attributes?.description
        var imageUrl = campaign?.attributes?.imageUri
        if (imageUrl == null || imageUrl.equals(
                "null",
                ignoreCase = true
            ) || imageUrl.isEmpty() || imageUrl.lowercase(
                Locale.getDefault()
            ).contains("default-image.png")
        ) {
            imageUrl = Values.DEFAULT_IMAGE_URL
        }
        val screenSize = ViewUtil.getScreenWidth(requireActivity())
        binding.image.layoutParams.width = screenSize
        binding.image.layoutParams.height = screenSize
        Glide.with(this).load(imageUrl).skipMemoryCache(false)
            .diskCacheStrategy(DiskCacheStrategy.ALL).listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.image.visibility = View.INVISIBLE
                    binding.imageShimmer.visibility = View.VISIBLE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any,
                    target: Target<Drawable?>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.image.visibility = View.VISIBLE
                    binding.imageShimmer.visibility = View.GONE
                    return false
                }
            }).into(binding.image)
        binding.toolbar.back.setOnClickListener { dismiss() }
        setupAvail()
        return view
    }

    private fun setupAdapter() {
        try {
            rules = campaign?.promotion?.attributes?.rules
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (rules == null) {
            rules = ArrayList()
        }
        rules?.let {
            adapter.updateRules(it.toTypedArray())
            binding.recyclerView.adapter = adapter
        }
    }

    private fun setupAvail() {
        rules?.let {
            if (it.isMet()) {
                var toRemove = false
                try {
                    if (campaign?.id.equals(
                            CacheUtil.getInstance().campaign.id,
                            ignoreCase = true
                        )
                    ) {
                        toRemove = true
                        binding.avail.text = CacheUtil.getInstance()
                            .getTranslations(Text.REMOVE_PROMO_CAPS, "REMOVE PROMO")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                binding.avail.setOnClickListener {
                    if (!toRemove) {
                        if (CacheUtil.getInstance().checkIfUsedPromotion(campaign?.promotion)) {
                            showAlertDialog(
                                "", CacheUtil.getInstance().getTranslations(
                                    Text.PROMO_ALREAD_USED_MESSAGE_KEY,
                                    "Sorry the promo is already used on this device"
                                )
                            )
                        } else {
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
                                            onAction?.onAvail(campaign)
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
                                        onAction?.onAvail(campaign)
                                        dismiss()
                                    }
                                    e.printStackTrace()
                                }
                            }
                        }
                    } else {
                        onAction?.onAvail(campaign)
                        dismiss()
                    }
                }
            } else {
                activity?.let {
                    binding.avail.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.disabled)
                }
                binding.avail.setOnClickListener(null)
            }
        }

    }

    override fun onStart() {
        super.onStart()
        dialog?.apply {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            window?.apply {
                setLayout(width, height)
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
                val typedValue = TypedValue()
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                statusBarColor = typedValue.data
            }
        }
    }

    interface OnAction {
        fun onClose()
        fun onAvail(campaign: Campaign?)
        fun onDelete(campaign: Campaign?)
    }

    companion object {
        fun newInstance(campaign: Campaign?, onAction: OnAction?): PromotionDetailsDialogFragment {
            val fragment = PromotionDetailsDialogFragment()
            fragment.campaign = campaign
            fragment.onAction = onAction
            return fragment
        }
    }
}