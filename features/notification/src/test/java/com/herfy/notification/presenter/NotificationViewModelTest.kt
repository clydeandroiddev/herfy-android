package com.herfy.notification.presenter

import com.google.common.truth.Truth.assertThat
import com.herfy.notification.core.BaseViewModelTest
import com.herfy.notification.presenter.ui.NotificationState
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NotificationViewModelTest: BaseViewModelTest<NotificationViewModel>() {

    private val mockNotification = Stubs.fakeNotifications()

    override fun createViewModel(): NotificationViewModel = NotificationViewModel(dispatchers =testDispatcherProvider)

    @Test
    fun `get notification should emit success when api return successful`() {
        notificationTest(expectedStates = listOf(NotificationState.IsLoading(true),NotificationState.Success(mockNotification),
            NotificationState.IsLoading(false)) )

    }
    private fun notificationTest(expectedStates: List<NotificationState>) = runTest {
        viewModel.getNotifications()
        val states = viewModel.state
            .take(expectedStates.size).toList()
        advanceUntilIdle()
        assertThat(states).containsExactlyElementsIn(expectedStates).inOrder()
    }
}