package com.herfy.model.modifiers

import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem

data class ModifierItemAdapterItem(
    var modifierGroup : ModifierGroup,
    var modifierItem: ModifierItem,
    var isAboveMax : Boolean = false
) : ModifierAdapterItem() {
    override val viewType: Int = VIEW_TYPE_MODIFIER_ITEM
}
