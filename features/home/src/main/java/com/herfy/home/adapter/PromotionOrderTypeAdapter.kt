package com.herfy.home.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.herfy.home.R
import com.herfy.home.databinding.ItemPromoOrderTypesBinding
import com.herfy.model.PromotionOrderType

class PromotionOrderTypeAdapter(private var orderTypes : List<PromotionOrderType> = arrayListOf()) : RecyclerView.Adapter<PromotionOrderTypeAdapter.ViewHolder>() {

    inner class ViewHolder(val binding : ItemPromoOrderTypesBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<ItemPromoOrderTypesBinding>(
            LayoutInflater.from(parent.context),
            R.layout.item_promo_order_types,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = orderTypes.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = orderTypes[position]
        holder.binding.apply {
            orderType = data
            executePendingBindings()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(promoOrderTypeList: ArrayList<PromotionOrderType>) {
        orderTypes = promoOrderTypeList
        notifyDataSetChanged()
    }


}