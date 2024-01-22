package com.herfy.promotions.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.herfy.R
import com.herfy.databinding.ItemRequirementsBinding
import com.skylinedynamics.solosdk.api.models.objects.PromotionRules

/**
 * [PromotionRulesAdapter] adapter for displaying all the menu categories and items
 */
class PromotionRulesAdapter(
    private var rules: Array<PromotionRules>
) : RecyclerView.Adapter<PromotionRulesAdapter.PromotionRulesViewHolder>() {

    /**
     * [PromotionRuleViewHolder] for binding the views
     * @param binding as [ItemRequirementsBinding] the views to be binded
     */
    class PromotionRulesViewHolder(
        private val binding: ItemRequirementsBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        /**
         * bind the [PromotionRules] to display
         */
        fun bind(rule: PromotionRules) {

            if (rule.isMet) {
                binding.requirementCheck.setImageResource(R.drawable.ic_check_instore)
            } else {
                binding.requirementCheck.setImageResource(R.drawable.ic_uncheck_instore)
            }

            binding.requirementLabel.text = rule.text

        }
    }

    /**
     * Setup the binding for the view holder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromotionRulesViewHolder {

        /**
         * inflates the current layout with view holder
         */
        ItemRequirementsBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
            return PromotionRulesViewHolder(it)
        }
    }

    /**
     * Binds the data to the view
     */
    override fun onBindViewHolder(holder: PromotionRulesViewHolder, position: Int) {
        holder.bind(rules[position])
    }

    /**
     * Returns the number of menu categories
     */
    override fun getItemCount(): Int = rules.size

    fun updateRules(promotionRules: Array<PromotionRules>) {
        rules = promotionRules
        notifyDataSetChanged()
    }

}