package com.herfy.model.modifiers

import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup

data class ModifierLineAdapterItem (var modifierGroup: ModifierGroup) : ModifierAdapterItem() {
    override val viewType: Int = VIEW_TYPE_MODIFIER_LINE
}