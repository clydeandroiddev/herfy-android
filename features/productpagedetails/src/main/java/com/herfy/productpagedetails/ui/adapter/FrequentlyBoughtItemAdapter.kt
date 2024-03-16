package com.herfy.productpagedetails.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.herfy.productpagedetails.databinding.ItemModifierFrequentlyBoughtBinding
import com.herfy.productpagedetails.ui.viewholder.FrequentlyBoughtItemViewholder
import com.skylinedynamics.solosdk.api.models.objects.MenuItem

class FrequentlyBoughtItemAdapter(private val items : List<MenuItem> = arrayListOf(), private val listener : OnClickListener ) : RecyclerView.Adapter<FrequentlyBoughtItemViewholder>(){




    interface OnClickListener{
        fun onSelectItem(menuItem : MenuItem)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FrequentlyBoughtItemViewholder {
        val binding :ItemModifierFrequentlyBoughtBinding = ItemModifierFrequentlyBoughtBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return FrequentlyBoughtItemViewholder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: FrequentlyBoughtItemViewholder, position: Int) {
        holder.apply {
            val data = items[position]
            setItems(data)
            itemView.setOnClickListener{
                listener.onSelectItem(data)
            }
        }
    }


}