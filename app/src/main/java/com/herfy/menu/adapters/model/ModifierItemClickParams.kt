package com.herfy.menu.adapters.model

import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem

data class ModifierItemClickParams(
    val isSelected: Boolean,
    val modifierGroupPosition: Int,
    val group: ModifierGroup,
    val modifierPosition: Int,
    val modifierItem: ModifierItem,
    val isAdd: Boolean = false,
    val modifierGroup: ModifierGroup,
)