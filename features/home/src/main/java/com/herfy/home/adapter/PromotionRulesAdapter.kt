package com.herfy.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.herfy.home.R
import com.herfy.home.databinding.ItemPromoRulesBinding
import com.herfy.model.PromotionRequirement

class PromotionRulesAdapter(val requirements: List<PromotionRequirement>) :
    RecyclerView.Adapter<PromotionRulesAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemPromoRulesBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemPromoRulesBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_promo_rules,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = requirements.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            val rule = requirements[position]
            requirement = rule
            holder.binding.ivRule.setImageResource(rule.icon)
            holder.binding.ivIsMet.setImageResource(
                if (rule.isRequired) {
                    com.herfy.common.R.drawable.ic_promotion_required
                } else {
                    com.herfy.common.R.drawable.ic_promotion_not_required
                }
            )
        }
    }

}