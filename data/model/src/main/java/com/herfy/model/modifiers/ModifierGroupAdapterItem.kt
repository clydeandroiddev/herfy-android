package com.herfy.model.modifiers

import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup

data class ModifierGroupAdapterItem (
    var modifierGroup: ModifierGroup,
    var hasSelectedModifierItems : Boolean
) : ModifierAdapterItem(), Cloneable {

    public override fun clone(): ModifierGroupAdapterItem = super.clone() as ModifierGroupAdapterItem

    override val viewType: Int = VIEW_TYPE_MODIFIER_GROUP
}