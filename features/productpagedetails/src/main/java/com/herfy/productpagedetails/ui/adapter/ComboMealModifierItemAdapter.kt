package com.herfy.productpagedetails.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.herfy.productpagedetails.databinding.ItemComboComponentItemBinding
import com.herfy.productpagedetails.ui.viewholder.ComboMealModifierItemViewHolder
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem

class ComboMealModifierItemAdapter(
    private var items: List<ComponentModifierItem> = arrayListOf(),
    private val menuItemComponentPos: Int,
    private val listener: onClickListener
) : RecyclerView.Adapter<ComboMealModifierItemViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ComboMealModifierItemViewHolder {
        ItemComboComponentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            .apply {
                return ComboMealModifierItemViewHolder(this)
            }
    }

    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: ComboMealModifierItemViewHolder, position: Int) {
        val data = items[position]
        holder.apply {
            itemView.setOnClickListener{
                listener.onSelectComboComponentItem(item = data , menuItemComponentPos, position)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(items: ArrayList<ComponentModifierItem>) {
        this.items = items
        notifyDataSetChanged()
    }


    interface onClickListener {
        fun onSelectComboComponentItem(item: ComponentModifierItem, menuItemComponentPos: Int, componentModifierItemPos : Int)
    }
}