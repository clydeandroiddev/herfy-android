package com.herfy.utils.extensions

import android.util.Log
import com.skylinedynamics.solosdk.api.models.objects.PromotionRules

fun List<PromotionRules>.isMet(): Boolean {
    val count = this.size
    var metCounter = count
    for (i in 0 until count) {
        if (!this[i].isMet && this[i].ruleKey != "order-type") {
            metCounter -= 1
        }
    }
    return metCounter == count
}
