package com.herfy.promotions.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.herfy.databinding.ItemPromotionsBinding
import com.herfy.home.databinding.ItemPromotionBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import java.util.regex.Pattern

/**
 * [PromotionsAdapter] adapter for displaying all the menu categories and items
 */
class PromotionsAdapter(
    val promoList: ArrayList<Campaign> = arrayListOf(),
    val promoListener: com.herfy.home.adapter.PromotionsAdapter.PromotionListener,
    val itemHeight: Int,
    val itemWidth: Int,
    val defaultOrderTypeCount: Int,
    val cacheUtil: CacheUtil
) : RecyclerView.Adapter<PromotionsAdapter.CampaignViewHolder>() {

    /**
     * [CampaignViewHolder] for binding the views
     * @param binding as [ItemPromotionsBinding] the views to be binded
     */
    class CampaignViewHolder(
        private val binding: ItemPromotionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        /**
         * bind the [Campaign] to display
         */
        fun bind(
            campaign: Campaign,
            cacheUtil: CacheUtil,
            promoListener: com.herfy.home.adapter.PromotionsAdapter.PromotionListener,
            defaultOrderTypeCount: Int
        ) {
            binding.apply {
                val promoRules = campaign.promotion.attributes.rules
                tvPickupPromo.text = cacheUtil.getTranslations(Text.PICKUP)
                tvDeliveryPromo.text = cacheUtil.getTranslations(Text.DELIVERY)
                if (promoRules.isNotEmpty() && promoRules.find { rule ->
                        rule.ruleKey.equals(
                            "order-type", true
                        )
                    } != null) {
                    promoRules.find { rule -> rule.ruleKey.equals("order-type", true) }?.let {
                        val orderTypes = it.text.split(Pattern.compile(","))
                        for (orderType in orderTypes) {
                            when (orderType.trim()) {
                                "pickup", "curbside", "drive-thru" -> {
                                    pickupLabel.visible()
                                }

                                "delivery", "deliver" -> {
                                    deliveryLabel.visible()
                                }
                            }
                        }
                    }
                } else if (promoRules.isEmpty() || promoRules.find { rules -> rules.ruleKey.equals("order-type") } == null) {
                    if (defaultOrderTypeCount > 0) {
                        deliveryLabel.visible()
                    }
                }
                promo = campaign
                listener = promoListener
                executePendingBindings()
            }
        }
    }

    /**
     * Setup the binding for the view holder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CampaignViewHolder {

        /**
         * inflates the current layout with view holder
         */
        ItemPromotionBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
            val params = it.root.layoutParams as GridLayoutManager.LayoutParams
            params.height = itemHeight
            return CampaignViewHolder(it)
        }
    }

    /**
     * Binds the data to the view
     */
    override fun onBindViewHolder(holder: CampaignViewHolder, position: Int) {
        holder.bind(promoList[position], cacheUtil, promoListener, defaultOrderTypeCount)
    }

    /**
     * Returns the number of menu categories
     */
    override fun getItemCount(): Int = promoList.size

    fun updateCampaigns(campaigns: List<Campaign>) {
        this.promoList.clear()
        this.promoList.addAll(campaigns)
        notifyDataSetChanged()
    }

}