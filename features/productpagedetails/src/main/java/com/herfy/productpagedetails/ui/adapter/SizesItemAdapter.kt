package com.herfy.productpagedetails.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.herfy.productpagedetails.databinding.ItemNewSizesFixedBinding
import com.herfy.productpagedetails.ui.viewholder.SizesItemViewholder
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem

class SizesItemAdapter (private val items : List<ModifierItem> = arrayListOf(), private val listener : OnClickListener ) : RecyclerView.Adapter<SizesItemViewholder>() {

    interface OnClickListener{
        fun onSelectSize(size : ModifierItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SizesItemViewholder {
        val binding = ItemNewSizesFixedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SizesItemViewholder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SizesItemViewholder, position: Int) {
        holder.apply {
            val data = items[position]
            setSize(data)
            itemView.setOnClickListener {
                listener.onSelectSize(data)
            }
        }
    }

}