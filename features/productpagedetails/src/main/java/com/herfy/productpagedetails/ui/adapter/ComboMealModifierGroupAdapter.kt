package com.herfy.productpagedetails.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.herfy.model.MenuItemComponentItem
import com.herfy.productpagedetails.databinding.ItemModifierGroupNewBinding
import com.herfy.productpagedetails.ui.viewholder.ComboMealModifierGroupViewholder
import com.herfy.productpagedetails.ui.viewmodel.ComboViewModel

class ComboMealModifierGroupAdapter(
    private var items: List<MenuItemComponentItem> = arrayListOf(),
    private val comboViewModel: ComboViewModel,
    private var showComboMealModifierGroupCheckIndicator : HashMap<Int, Boolean> = hashMapOf()
) : RecyclerView.Adapter<ComboMealModifierGroupViewholder>() {

    var comboMealModifierItemAdapters: HashMap<Int, ComboMealModifierItemAdapter> = hashMapOf()
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
            setComboModifierGroup(data.menuItemComponents, data.menuItemComponentPos, comboMealModifierItemAdapters,showComboMealModifierGroupCheckIndicator)
        }
    }



    @SuppressLint("NotifyDataSetChanged")
    fun updateList(items: List<MenuItemComponentItem>, checkBoxIndicatorMap : HashMap<Int, Boolean>){
        this.items = items
        comboMealModifierItemAdapters = hashMapOf()
        showComboMealModifierGroupCheckIndicator = checkBoxIndicatorMap
        notifyDataSetChanged()
    }

    fun updateComboMealModifierItem(menuItemComponentIndex : Int, selectedComboMealModifierItemIndex : Int){
        comboMealModifierItemAdapters[menuItemComponentIndex]?.apply {
            updateComponents(prevSelectedItemPos, selectedComboMealModifierItemIndex)
        }

    }

}