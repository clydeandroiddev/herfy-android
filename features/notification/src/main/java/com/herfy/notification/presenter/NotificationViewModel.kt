package com.herfy.notification.presenter

import com.herfy.utils.base.mvvm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class NotificationViewModel @Inject constructor(): BaseViewModel() {

    private val _notificationState = MutableStateFlow(Stub.fakeNotifications())
    val notificationState = _notificationState.asStateFlow()


}