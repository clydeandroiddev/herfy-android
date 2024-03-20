package com.herfy.productpagedetails.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.herfy.productpagedetails.databinding.ItemModifierGroupNewBinding
import com.herfy.productpagedetails.ui.viewholder.ComboMealModifierGroupViewholder
import com.herfy.productpagedetails.ui.viewmodel.ComboViewModel
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents

class ComboMealModifierGroupAdapter(
    private var items: List<MenuItemComponents> = arrayListOf(),
    private val comboViewModel: ComboViewModel
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
        val holder = ComboMealModifierGroupViewholder(binding, comboViewModel)
        holder.setListener(comboViewModel)
        return holder
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ComboMealModifierGroupViewholder, position: Int) {
        holder.apply {
            val data = items[position]
            setComboModifierGroup(data, position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(items: List<MenuItemComponents>){
        this.items = items
        notifyDataSetChanged()
    }

}