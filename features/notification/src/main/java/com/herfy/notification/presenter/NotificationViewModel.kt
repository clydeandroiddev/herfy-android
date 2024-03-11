package com.herfy.notification.presenter

import androidx.lifecycle.viewModelScope
import com.herfy.notification.presenter.ui.NotificationState
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.helper.DispatcherProvider
import com.herfy.utils.helper.launchWithTimber
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

class
NotificationViewModel @Inject constructor(private val dispatchers: DispatcherProvider): BaseViewModel() {

    private val _state = MutableSharedFlow<NotificationState>()
    val state = _state



    fun getNotifications() {
        viewModelScope.launchWithTimber(dispatchers.io) {
            try {
                _state.emit(NotificationState.IsLoading(true))
                delay(3000) // to simulate api call

                _state.emit(NotificationState.Success(Stubs.fakeNotifications()))
            } catch (e: Exception) {
                _state.emit(NotificationState.Error(e.message.orEmpty()))
            } finally {
                _state.emit(NotificationState.IsLoading(false))
            }
        }
    }


}