package com.herfy.util

import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup


fun fetchCalories(modifierGroups: List<ModifierGroup>): Int {
    var calorieTotal = 0
    for (i in modifierGroups.indices) {
        modifierGroups[i].modifierItems.filter { x -> x.attributes.isSelected || x.selected }
            .forEach {
                calorieTotal += it.attributes.quantity * it.attributes.calorieCount
            }
    }

    return calorieTotal
}
