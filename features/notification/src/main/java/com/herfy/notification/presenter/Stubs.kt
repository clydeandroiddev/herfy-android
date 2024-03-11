package com.herfy.notification.presenter

import com.herfy.notification.presenter.model.NotificationUIModel

object Stubs {

    fun fakeNotifications(): List<NotificationUIModel> {
        val notifications = mutableListOf<NotificationUIModel>()
        repeat(5) {
            notifications.add(
                NotificationUIModel(
                    id = it + 1,
                    description = "Notification text explaining about notification $it.",
                    minutes = "$it minutes ago",
                    image = "https://console-uploads-sandbox.s3.eu-west-1.amazonaws.com/cszalrtpn.png"
                )
            )
        }
        return notifications
    }
}