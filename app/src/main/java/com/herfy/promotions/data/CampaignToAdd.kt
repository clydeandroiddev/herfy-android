package com.herfy.promotions.data

import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.Promotion

data class CampaignToAdd(
    val campaign: Campaign? = null,
    val promotion: Promotion? = null,
    val menuItem: MenuItem? = null
)