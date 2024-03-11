package com.herfy.repository

import com.skylinedynamics.solosdk.api.calls.NotificationApiCall
import javax.inject.Inject

interface NotificationRepository {

    fun getListOfNotifications()
}

class NotificationRepositoryImpl @Inject constructor(): NotificationRepository {
    override fun getListOfNotifications() {
        NotificationApiCall.getInstance()
            .getNotifications()
    }
}