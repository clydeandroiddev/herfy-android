package com.herfy.productpagedetails.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.herfy.productpagedetails.databinding.ItemNewSizesFixedBinding
import com.herfy.productpagedetails.ui.viewholder.SizesItemViewholder
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem

class SizesItemAdapter (private var items : List<ModifierItem> = arrayListOf(),
                        private val listener : OnClickListener,
                        private val itemWidth : Int) : RecyclerView.Adapter<SizesItemViewholder>() {

    interface OnClickListener{
        fun onSelectSize(size: ModifierItem, pos: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SizesItemViewholder {
        val binding = ItemNewSizesFixedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        val params = binding.root.layoutParams
        params.width = itemWidth
        params.height = itemWidth
        binding.root.layoutParams = params
        return SizesItemViewholder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SizesItemViewholder, position: Int) {
        holder.apply {
            val data = items[position]
            setSize(data)
            itemView.setOnClickListener {
                listener.onSelectSize(data, position)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(items : List<ModifierItem>){
        this.items = items
        notifyDataSetChanged()
    }

}