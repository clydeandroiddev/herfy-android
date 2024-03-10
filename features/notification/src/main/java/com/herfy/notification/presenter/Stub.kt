package com.herfy.notification.presenter

import com.herfy.notification.presenter.model.NotificationUIModel

object Stub {

    fun fakeNotifications(): List<NotificationUIModel> {
        val notifications = mutableListOf<NotificationUIModel>()
        repeat(5) {
            notifications.add(
                NotificationUIModel(
                    id = it + 1,
                    description = "Notification text explaining about notification $it.",
                    minutes = "$it minutes ago",
                )
            )
        }
        return notifications
    }
}