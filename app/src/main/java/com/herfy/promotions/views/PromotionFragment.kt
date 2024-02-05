package com.herfy.promotions.views

import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.databinding.FragmentPromotionBinding
import com.herfy.ext.cast
import com.herfy.ext.isMet
import com.herfy.promotions.adapters.PromotionRulesAdapter
import com.herfy.util.ViewUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.PromotionRules
import com.skylinedynamics.solosdk.util.GsonHelper
import java.util.Locale

class PromotionFragment : BaseFragment() {

    companion object {
        const val ARGS_KEY_FRAGMENT_RESULT = "fragmentResult"
        const val ARGS_KEY_ACTION = "action"
    }

    private lateinit var binding: FragmentPromotionBinding

    private val args: PromotionFragmentArgs by navArgs()

    private var rules: List<PromotionRules>? = ArrayList()

    private val adapter: PromotionRulesAdapter by lazy {
        PromotionRulesAdapter(emptyArray())
    }

    private var campaign: Campaign? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        campaign = GsonHelper.gsonFinal().fromJson(args.campaign, Campaign::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPromotionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        binding.requirementsLabel.text =
            CacheUtil.getInstance().getTranslations(Text.REQUIREMENTS, " Requirements")
        binding.avail.text = CacheUtil.getInstance().getTranslations(Text.REDEEM_THIS_PROMO, "REDEEM THIS PROMO")
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
        setupAvail()
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
            var toRemove = false
            if (CacheUtil.getInstance().campaign != null && campaign?.id == CacheUtil.getInstance().campaign.id) {
                toRemove = true
                binding.avail.text = CacheUtil.getInstance()
                    .getTranslations(Text.REMOVE_PROMO_CAPS, "REMOVE PROMO")
            }
            if (it.isMet()) {
                binding.avail.setOnClickListener {
                    if(!toRemove) {
                        if(CacheUtil.getInstance().cartQuantity > 0) {
                            if (CacheUtil.getInstance().couponCode != null) {
                                activity?.cast<BaseActivity> { baseActivity ->
                                    baseActivity.showAlertDialog(
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
                                            dialogInterface.dismiss()
                                            navigateUp(action = "avail")
                                        },
                                        CacheUtil.getInstance().getTranslations(Text.NO_CAPS)
                                    ) { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() }
                                }
                            } else {
                                navigateUp(action = "avail")
                            }
                        } else {
                            showError(CacheUtil.getInstance().getTranslations(
                                Text.PLEASE_ADD_ITEMS_TO_YOUR_CART,
                                "Please add items to your cart to avail this offer"
                            ))
                        }
                    } else {
                        CacheUtil.getInstance().campaign = null
                        CacheUtil.getInstance().campaignPromotion = null
                        CacheUtil.getInstance().campaignPromotionMenuItem = null
                        CacheUtil.getInstance().campaignToAdd = null
                        navigateUp(action = "delete")
                    }
                }
            } else {
                activity?.cast<BaseActivity> { baseActivity ->
                    binding.avail.backgroundTintList = ContextCompat.getColorStateList(baseActivity, R.color.disabled)
                }
                binding.avail.setOnClickListener(null)
            }
        }

    }

    fun showError(error: String) {
        try {
            activity?.cast<BaseActivity> { baseActivity ->
                baseActivity.showAlertDialog(
                    "",
                    error,
                    CacheUtil.getInstance().getTranslations(Text.OK)
                ) { dialog, which ->
                    dialog.dismiss()
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun navigateUp(action: String) {
        setFragmentResult(
            ARGS_KEY_FRAGMENT_RESULT,
            bundleOf(
                ARGS_KEY_ACTION to action
            )
        )
        findNavController().navigateUp()
    }

}