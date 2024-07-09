package com.herfy.model

import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents

data class ShowComboMealItemDetails(
    val menuItemComponent: MenuItemComponents,
    val componentModifierItem: ComponentModifierItem,
    val menuItemComponentIndex : Int,
    val isSelected : Boolean
)
