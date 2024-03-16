package com.herfy.productpagedetails.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.herfy.productpagedetails.databinding.ItemModifierGroupNewBinding
import com.herfy.productpagedetails.ui.viewholder.ComboMealModifierGroupViewholder
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup

class ComboMealModifierGroupAdapter(
    private val items: List<ModifierGroup> = arrayListOf(),
    private var showModifierItemPrice: HashMap<Int, Boolean> = hashMapOf(),
    private var shouldShowModifierGroupCheckbox : HashMap<Int, Boolean> = hashMapOf(),
    private var shouldShowQuantity : HashMap<Int, Boolean> = hashMapOf()
) : RecyclerView.Adapter<ComboMealModifierGroupViewholder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ComboMealModifierGroupViewholder {
        val binding = ItemModifierGroupNewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ComboMealModifierGroupViewholder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ComboMealModifierGroupViewholder, position: Int) {
        holder.apply {
            val data = items[position]
            setModifierGroup(data, showModifierItemPrice.get(position),shouldShowModifierGroupCheckbox.get(position),shouldShowQuantity)
        }
    }

    fun showModifierItemPrice(show: Boolean, position: Int) {
        showModifierItemPrice.put(position, show)
        notifyItemChanged(position)
    }

    fun shouldShowCheckBox(show: Boolean, position: Int) {
        shouldShowModifierGroupCheckbox.put(position, show)
        notifyItemChanged(position)
    }

    fun shouldShowQuantity(show: Boolean, position: Int) {
        shouldShowQuantity.put(position, show)
        notifyItemChanged(position)
    }


}