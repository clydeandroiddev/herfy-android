package com.herfy.model

import com.skylinedynamics.solosdk.api.models.objects.Tier

data class TierProgress(
    val nextTier: Tier?,
    val totalPointsAccumulated : Int,
    val pointsToNextTier : Int,
    val startPoints : Int
)