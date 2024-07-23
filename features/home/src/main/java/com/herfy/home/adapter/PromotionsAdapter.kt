package com.herfy.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.herfy.home.R
import com.herfy.home.databinding.ItemPromotionBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import java.util.regex.Pattern

class PromotionsAdapter(
    val promoList: ArrayList<Campaign> = arrayListOf(),
    val promoListener: PromotionListener,
    val itemHeight: Int, val itemWidth: Int,
    val defaultOrderTypeCount: Int,
    val cacheUtil: CacheUtil
) : RecyclerView.Adapter<PromotionsAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemPromotionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemPromotionBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_promotion,
            parent,
            false
        )
        val params = binding.root.layoutParams as GridLayoutManager.LayoutParams
        params.height = itemHeight

        binding.root.layoutParams = params
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = promoList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            val promoRules = promoList[position].promotion.attributes.rules
            tvPickupPromo.text = cacheUtil.getTranslations(Text.PICKUP)
            tvDeliveryPromo.text = cacheUtil.getTranslations(Text.DELIVERY)
            pickupLabel.gone()
            deliveryLabel.gone()
            if (promoRules.isNotEmpty() && promoRules.find { rule ->
                    rule.ruleKey.equals(
                        "order-type",
                        true
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
            promo = promoList[position]
            listener = promoListener
            executePendingBindings()
        }
    }

    fun updatePromoList(promoList: List<Campaign>) {
        this.promoList.clear()
        this.promoList.addAll(promoList)
        notifyDataSetChanged()
    }

    interface PromotionListener {
        fun onSelectedPromo(promo: Campaign)
    }
}