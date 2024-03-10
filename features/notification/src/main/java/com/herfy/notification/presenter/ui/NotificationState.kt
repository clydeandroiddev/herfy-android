package com.herfy.notification.presenter.ui

import com.herfy.notification.presenter.model.NotificationUIModel

sealed class NotificationState {

    data class IsLoading(val isLoading: Boolean): NotificationState()

    data class Error(val message:String): NotificationState()

    data class Success(val notifications: List<NotificationUIModel>): NotificationState()
}