package com.herfy.productpagedetails.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.herfy.productpagedetails.R
import com.herfy.productpagedetails.databinding.ItemModifierDrinksTypeBinding
import com.herfy.productpagedetails.databinding.ItemModifierOptionTypeBinding
import com.herfy.productpagedetails.ui.viewholder.BaseModifierItemViewHolder
import com.herfy.productpagedetails.ui.viewholder.ImageItemModifierHolder
import com.herfy.productpagedetails.ui.viewholder.ModifierItemOptionViewHolder
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupType
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem

class ComboItemDetailsModifierItemAdapter(
    private val items: List<ModifierItem> = arrayListOf(),
    private val modifierGroup : ModifierGroup?,
    private val modifierGroupPosition: Int = -1,
    private val showModifierItemPrice : Boolean
    ) : RecyclerView.Adapter<BaseModifierItemViewHolder>() {

    private var onAction: BaseModifierItemViewHolder.OnAction? = null
    private val ITEM_MODIFIER_OPTION_TYPE = 1
    private val ITEM_WITHOUT_DESCRIPTION_VIEW_TYPE = 2
    private val ITEM_MODIFIER_OPTIONS_VIEW_TYPE = 3
    private val HIDDEN_VIEW_TYPE = 4;
    fun setOnAction(onAction: BaseModifierItemViewHolder.OnAction?) {
        this.onAction = onAction
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseModifierItemViewHolder {

        return if (viewType == ITEM_MODIFIER_OPTION_TYPE) {
            val binding = ItemModifierOptionTypeBinding.inflate(LayoutInflater.from(parent.context),parent,false)
            val modifierTypeOption = ModifierItemOptionViewHolder(binding)
            onAction?.let {
                modifierTypeOption.setAction(it)
            }

            modifierTypeOption
        } else if (viewType == ITEM_WITHOUT_DESCRIPTION_VIEW_TYPE) {
            val binding = ItemModifierDrinksTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            val holder = ImageItemModifierHolder(binding)
            holder.setAction(onAction)
            holder
        } else {
            throw UnsupportedOperationException("Invalid view type $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        val modifierGroupType: ModifierGroupType = items[position].modifierGroup.type
        return determineModifierItemViewType(modifierGroupType)
    }

    private fun determineModifierItemViewType(type: ModifierGroupType): Int {
        val modifierItemSize: Int = items.size
        if (isOptionModifierItem(type, modifierItemSize)) {
            return ITEM_MODIFIER_OPTION_TYPE
        } else if (isDrinkModifierItem(type, modifierItemSize)) {
            return ITEM_WITHOUT_DESCRIPTION_VIEW_TYPE
        } else {
            return HIDDEN_VIEW_TYPE
        }
    }

    private fun isDrinkModifierItem(type: ModifierGroupType, modifierItemSize: Int): Boolean {
        return modifierItemSize > 3 && type == ModifierGroupType.DRINKS
    }

    private fun isOptionModifierItem(type: ModifierGroupType, modifierItemSize: Int): Boolean {
        return type == ModifierGroupType.OPTIONS && modifierItemSize >= 1
    }
    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: BaseModifierItemViewHolder, position: Int) {
       holder.setModifierItem(
           modifierGroupPosition,
           modifierGroup,
           position,
           items[position],
           showModifierItemPrice
       )
    }
}