package com.herfy.loyalty

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.herfy.loyalty.databinding.ItemRewardTierBinding
import com.herfy.utils.helper.LocaleUtil
import com.skylinedynamics.solosdk.api.models.objects.Tier
import java.util.Locale

class RewardTierAdapter(private val tierList : ArrayList<Tier> = arrayListOf()) :  RecyclerView.Adapter<RewardTierAdapter.ViewHolder>(){
    inner class ViewHolder (val binding : ItemRewardTierBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding : ItemRewardTierBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_reward_tier,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = tierList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tierData = tierList[position]
       holder.binding.apply {
           tier = tierData
           var loyaltyTier = tierData.name
           if(LocaleUtil.getInstance().isEnglish) {
               loyaltyTier = loyaltyTier.lowercase().replaceFirstChar(Char::titlecase)
           }
           tvRewardTier.text = loyaltyTier
           executePendingBindings()
       }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(tierList: MutableList<Tier>){
        this.tierList.apply {
            clear()
            addAll(tierList)
            notifyDataSetChanged()
        }

    }
}