package com.herfy.productpagedetails.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.herfy.productpagedetails.databinding.ItemModifierFrequentlyBoughtBinding
import com.herfy.productpagedetails.ui.viewholder.FrequentlyBoughtItemViewholder
import com.skylinedynamics.solosdk.api.models.objects.MenuItem

class FrequentlyBoughtItemAdapter(private var items : List<MenuItem> = arrayListOf(), private val listener : FrequentlyBoughtItemViewholder.OnClickListener ) : RecyclerView.Adapter<FrequentlyBoughtItemViewholder>(){


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FrequentlyBoughtItemViewholder {
        val binding: ItemModifierFrequentlyBoughtBinding =
            ItemModifierFrequentlyBoughtBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return FrequentlyBoughtItemViewholder(binding, listener)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: FrequentlyBoughtItemViewholder, position: Int) {
        holder.apply {
            val data = items[position]
            setItems(data, position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(items: ArrayList<MenuItem>){
        this.items = items
        notifyDataSetChanged()
    }

    fun updateFavorite(position: Int, menuItem: MenuItem) {
        this.items[position].isFavorite = menuItem.isFavorite
        notifyItemChanged(position)
    }

    fun updateAddToCartItem(second: Int, first: MenuItem) {
        this.items[second].isAddedToCart = first.isAddedToCart
        notifyItemChanged(second)
    }


}