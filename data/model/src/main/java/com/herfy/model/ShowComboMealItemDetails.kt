package com.herfy.model

import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem

data class ShowComboMealItemDetails(
    val componentModifierItem: ComponentModifierItem,
    val componentModifierItemIndex: Int,
    val previousComponentModifierIndex : Int,
    val menuItemComponentIndex : Int
)
