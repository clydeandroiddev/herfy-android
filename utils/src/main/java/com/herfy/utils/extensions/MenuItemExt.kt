package com.herfy.utils.extensions

import com.skylinedynamics.solosdk.api.models.objects.MenuItem

fun MenuItem.canBeCustomized(): Boolean {
    var hasSizes = false
    if(modifierGroups.size == 1) {
        modifierGroups.firstOrNull()?.let {
            hasSizes = it.modifierItems.size > 1
        }
    }
    return hasSizes || isCustomizable || isMustBeCustomized || isCombo
}
