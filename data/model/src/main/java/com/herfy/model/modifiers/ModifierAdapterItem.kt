package com.herfy.model.modifiers

sealed class ModifierAdapterItem{
    abstract val viewType: Int

    companion object {
        const val VIEW_TYPE_MODIFIER_GROUP = 0
        const val VIEW_TYPE_MODIFIER_ITEM = 1
    }
}