package com.herfy.promotions.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.herfy.databinding.ItemPromotionsBinding
import com.herfy.home.R
import com.herfy.home.databinding.ItemPromotionBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import java.util.regex.Pattern

/**
 * [PromotionsAdapter] adapter for displaying all the menu categories and items
 */
class PromotionsAdapter(
    val promoList : ArrayList<Campaign> = arrayListOf(),
    val promoListener : com.herfy.home.adapter.PromotionsAdapter.PromotionListener,
    val itemHeight : Int, val itemWidth : Int,
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
        fun bind(campaign: Campaign, cacheUtil: CacheUtil, promoListener: com.herfy.home.adapter.PromotionsAdapter.PromotionListener, defaultOrderTypeCount: Int) {
            binding.apply {
                var orderTypeCountVal = 0
                var orderTypeVal = ""
                var orderTypeIconVal = -1

                val promoRules = campaign.promotion.attributes.rules
                if(promoRules.isNotEmpty() && promoRules.find { rule -> rule.ruleKey.equals("order-type", true) } != null){
                    promoRules.find { rule -> rule.ruleKey.equals("order-type", true) }?.let {
                        val orderTypes = it.text.split(Pattern.compile(","))
                        when(orderTypes[0].trim()){
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
                                orderTypeVal = cacheUtil.getTranslations(Text.DRIVE_THRU, "Drive Thru")
                                orderTypeIconVal = R.drawable.ic_drive_thru_promo
                            }
                        }
                        orderTypeCountVal = orderTypes.size
                    }
                }else if(promoRules.isEmpty() || promoRules.find { rules -> rules.ruleKey.equals("order-type") } == null) {
                    if(defaultOrderTypeCount > 0){
                        orderTypeVal = cacheUtil.getTranslations(Text.DELIVERY)
                        orderTypeIconVal = R.drawable.ic_delivery_promo
                        orderTypeCountVal = defaultOrderTypeCount
                    }
                }
                promo = campaign
                orderTypeCount = orderTypeCountVal
                orderType = orderTypeVal
                orderTypeIcon = orderTypeIconVal
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
        holder.bind(promoList[position],cacheUtil, promoListener, defaultOrderTypeCount)
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