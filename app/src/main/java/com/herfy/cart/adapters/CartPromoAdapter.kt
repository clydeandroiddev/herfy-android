package com.herfy.cart.adapters

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.herfy.R
import com.herfy.cart.databinding.ItemCouponsBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.formatDate
import com.herfy.utils.extensions.isMet
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import java.util.Locale

class CartPromoAdapter(
    private var campaigns: List<Campaign> = arrayListOf(),
    private val listener: CartPromoListener? = null,
    private val currentCartAmount: Double = 0.0
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    interface CartPromoListener {
        fun select(campaign: Campaign, position: Int)
        fun error()
    }


    @JvmField
    var selected = -1

    inner class ViewHolderData(val binding: ItemCouponsBinding) : RecyclerView.ViewHolder(
        binding.root
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemCouponsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolderData(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        setupDataView((holder as ViewHolderData).binding, position)
    }

    override fun getItemCount(): Int {
        return campaigns.size
    }

    private fun setupDataView(binding: ItemCouponsBinding, position: Int) {
        binding.apply {
            campaigns.let { list ->
                val campaign = list[position]
                if (campaign.promotion != null && campaign.promotion.attributes.type.equals(
                        "item",
                        ignoreCase = true
                    )
                ) {
                } else {
                }
                tvCoupon.text = campaign.attributes.name
                tvCouponDescription.text = campaign.attributes.description
                val validFromDate =
                    campaign.attributes.validFrom //campaign.attributes.validFrom.split(".")[0]
                val validUntilDate =
                    campaign.attributes.validUntil //campaign.attributes.validUntil.split(".")[0]
                tvCouponExpiryDate.text =
                    CacheUtil.getInstance().getTranslations(Text.EXPIRES_LABEL, "Expires: (x)")
                        .replace("(x)", validUntilDate.formatDate("MMMM dd, YYYY"))
                val validFromTime = validFromDate.formatDate("h a")
                val validUntilTime = validUntilDate.formatDate("h a")
                tvCouponValidTime.text = CacheUtil.getInstance()
                    .getTranslations(Text.VALID_TIME_LABEL, "Valid Time: (x) to (y)")
                    .replace("(x)", validFromTime).replace("(y)", validUntilTime)

                if (campaign.promotion != null && campaign.promotion.attributes.rules.isMet()) {
                    tvFooterCoupon.text = CacheUtil.getInstance()
                        .getTranslations(Text.AVAILABLE_TO_REDEEM, "Available to Redeem")
                    val tvContext = tvFooterCoupon.context
                    val textColor =
                        tvContext.resources.getColor(com.herfy.commonstyles.R.color.text_coupon_available, null)
                    tvFooterCoupon.setTextColor(ColorStateList.valueOf(textColor))
                    lnFooterCoupon.setBackgroundResource(com.herfy.cart.R.drawable.background_available_to_redeem)
                    ivFooterCoupon.setImageResource(com.herfy.cart.R.drawable.ic_coupon_available)
                    clCoupons.setOnClickListener { v: View? ->
                        if (CacheUtil.getInstance().checkIfUsedPromotion(campaign.promotion)) {
                            listener?.error()
                        } else {
                            listener?.select(
                                campaign,
                                position
                            )
                        }
                    }
                } else {
                    val tvContext = tvFooterCoupon.context
                    val textColor =
                        tvContext.resources.getColor(com.herfy.commonstyles.R.color.secondaryTextColor, null)
                    tvFooterCoupon.text = campaign.attributes.error
                    tvFooterCoupon.setTextColor(ColorStateList.valueOf(textColor))
                    ivFooterCoupon.setImageResource(com.herfy.cart.R.drawable.ic_coupon_unavailable)
                    lnFooterCoupon.setBackgroundResource(com.herfy.cart.R.drawable.background_lock_redeem)
                }
                var imageUrl = campaign.attributes.imageUri
                if (imageUrl == null || imageUrl.equals(
                        "null",
                        ignoreCase = true
                    ) || imageUrl.isEmpty() || imageUrl.lowercase(
                        Locale.getDefault()
                    ).contains("default-image.png")
                ) {
                    imageUrl = Values.DEFAULT_IMAGE_URL
                }
                Glide.with(ivCoupon.context).load(imageUrl).skipMemoryCache(false)
                    .diskCacheStrategy(
                        DiskCacheStrategy.ALL
                    ).listener(object : RequestListener<Drawable?> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any,
                            target: Target<Drawable?>,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any,
                            target: Target<Drawable?>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }
                    }).into(ivCoupon)

            }


        }
    }

    fun updateCampaigns(selected: Int, campaigns: List<Campaign>) {
        this.selected = selected
        this.campaigns = campaigns
        notifyDataSetChanged()
    }
}
