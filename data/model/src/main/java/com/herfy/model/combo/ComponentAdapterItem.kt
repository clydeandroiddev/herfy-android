package com.herfy.model.combo

import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents

data class ComponentAdapterItem(
    var menuItemComponents: MenuItemComponents,
    var hasSelectedModifierItems : Boolean,
    val menuItemComponentPos : Int
) : ComboComponentAdapterItem() {
    override val viewType: Int = VIEW_TYPE_MENU_ITEM_COMPONENT
}
