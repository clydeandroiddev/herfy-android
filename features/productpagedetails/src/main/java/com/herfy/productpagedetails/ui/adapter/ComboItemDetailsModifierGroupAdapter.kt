package com.herfy.productpagedetails.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.herfy.productpagedetails.databinding.ItemModifierGroupNewBinding
import com.herfy.productpagedetails.ui.viewholder.BaseModifierItemViewHolder
import com.herfy.productpagedetails.ui.viewholder.ComboItemDetailModifierGroupViewHolder
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup

class ComboItemDetailsModifierGroupAdapter(
    private var items: List<ModifierGroup> = arrayListOf(),
    private var showModifierItemPrice: HashMap<Int, Boolean> = hashMapOf(),
    private var shouldShowModifierGroupCheckbox: HashMap<Int, Boolean> = hashMapOf(),
) : RecyclerView.Adapter<ComboItemDetailModifierGroupViewHolder>() {

    private var onAction: BaseModifierItemViewHolder.OnAction? = null

    fun setOnAction(onAction: BaseModifierItemViewHolder.OnAction?) {
        this.onAction = onAction
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ComboItemDetailModifierGroupViewHolder {
        ItemModifierGroupNewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false
        ).apply {
            val holder = ComboItemDetailModifierGroupViewHolder(this)
            holder.setOnAction(onAction)
            return holder
        }
    }

    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: ComboItemDetailModifierGroupViewHolder, position: Int) {
        val data = items[position]
        holder.apply {
            setModifierGroup(
                data,
                position,
                showModifierItemPrice[position] ?: false,
                shouldShowModifierGroupCheckbox[position] ?: false
            )
        }
    }

    fun showModifierItemPrice(show: Boolean, position: Int) {
        showModifierItemPrice[position] = show
        notifyItemChanged(position)
    }

    fun shouldShowCheckBox(show: Boolean, position: Int) {
        shouldShowModifierGroupCheckbox[position] = show
        notifyItemChanged(position)
    }
    @SuppressLint("NotifyDataSetChanged")
    fun updateList(items: ArrayList<ModifierGroup>?, showModifierItemPriceMap : HashMap<Int, Boolean>, shouldShowModifierGroupCheckboxMap : HashMap<Int,Boolean>) {
        showModifierItemPrice = showModifierItemPriceMap
        shouldShowModifierGroupCheckbox = shouldShowModifierGroupCheckboxMap
        items?.let {
            this.items = it
            notifyDataSetChanged()
        }
    }



}