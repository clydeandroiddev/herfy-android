package com.herfy.model

import com.skylinedynamics.solosdk.api.models.objects.Loyalty
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTierV2
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyV2
import com.skylinedynamics.solosdk.api.models.objects.Tier

data class TierProgress(
    val nextTier: LoyaltyTierV2?,
    val totalPointsAccumulated : Int,
    val pointsToNextTier : Int,
    val startPoints : Int,
    val isMaxTier : Boolean,
    val loyalty : LoyaltyV2?
)