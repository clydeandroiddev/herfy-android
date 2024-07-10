package com.herfy.model.combo

import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem

data class ComponentModifierAdapterItem(
    var menuItemComponents: MenuItemComponents,
    var componentModifierItem: ComponentModifierItem,
    var isAboveMax : Boolean = false,
    val menuItemComponentPos : Int,
    val componentModifierItemPos : Int
) : ComboComponentAdapterItem() {

    override val viewType: Int = VIEW_TYPE_COMPONENT_MODIFIER_ITEM
}