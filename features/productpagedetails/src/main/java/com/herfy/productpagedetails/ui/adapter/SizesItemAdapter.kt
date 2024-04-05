package com.herfy.productpagedetails.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.herfy.productpagedetails.databinding.ItemNewSizesFixedBinding
import com.herfy.productpagedetails.ui.viewholder.SizesItemViewholder
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem

class SizesItemAdapter (private var items : List<ModifierItem> = arrayListOf(),
                        private val itemWidth : Int) : RecyclerView.Adapter<SizesItemViewholder>() {

    private var prevSelectedItemPos: Int = 0

    private var onAction : SizesItemViewholder.OnClickListener? = null
    fun setOnAction(onAction : SizesItemViewholder.OnClickListener){
        this.onAction = onAction
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SizesItemViewholder {
        val binding = ItemNewSizesFixedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        val params = binding.root.layoutParams
        /*params.height = itemWidth
        binding.root.layoutParams = params*/
        val holder = SizesItemViewholder(binding)
        holder.setOnAction(onAction)
        return holder
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SizesItemViewholder, position: Int) {
        holder.apply {
            val data = items[position]
            setSize(data,position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(items : List<ModifierItem>){
        this.items = items
        notifyDataSetChanged()
    }

    fun updateSelectedSize(
        previous: Int,
        selected: Int
    ) {
        if(previous != selected){
            items[previous].apply {
                this.attributes.isSelected = false
            }
            items[selected].apply {
                attributes.isSelected = true
            }
            prevSelectedItemPos = selected
        }

        notifyItemChanged(previous)
        notifyItemChanged(selected)

    }


}