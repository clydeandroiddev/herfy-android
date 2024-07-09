package com.herfy.model.combo

sealed class ComboComponentAdapterItem{
    abstract val viewType: Int

    companion object {
        const val VIEW_TYPE_MENU_ITEM_COMPONENT= 0
        const val VIEW_TYPE_COMPONENT_MODIFIER_ITEM = 1
    }
}
