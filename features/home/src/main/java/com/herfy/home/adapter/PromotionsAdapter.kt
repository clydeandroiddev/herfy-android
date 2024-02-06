package com.herfy.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.herfy.home.R
import com.herfy.home.databinding.ItemPromotionBinding
import com.skylinedynamics.solosdk.api.models.objects.Campaign

class PromotionsAdapter(
    val promoList : ArrayList<Campaign> = arrayListOf(),
    val promoListener : PromotionListener,
    val itemHeight : Int, val itemWidth : Int) : RecyclerView.Adapter<PromotionsAdapter.ViewHolder>() {
    inner class ViewHolder(val binding : ItemPromotionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding : ItemPromotionBinding = DataBindingUtil.inflate(
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
            promo = promoList[position]
            listener = promoListener
            executePendingBindings()
        }
    }

    fun updatePromoList(promoList: ArrayList<Campaign>){
        this.promoList.clear()
        this.promoList.addAll(promoList)
        notifyDataSetChanged()
    }

    interface PromotionListener{
        fun onSelectedPromo(promo : Campaign)
    }
}