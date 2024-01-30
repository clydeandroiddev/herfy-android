package com.herfy.productpagedetails.ui.viewholder.util

object ModifierGroupMinimumUtil {

    fun validateType(type:Int): ModifierGroupMinimumType {
        return if (type > 0) {
            ModifierGroupMinimumType.REQUIRED
        }else {
            ModifierGroupMinimumType.OPTIONAL
        }
    }
}

enum class ModifierGroupMinimumType() {
    REQUIRED,
    OPTIONAL
}