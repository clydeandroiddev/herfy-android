package com.herfy.notification.presenter.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.herfy.common.utils.autoCleared
import com.herfy.notification.R
import com.herfy.notification.databinding.FragmentNotificationBinding
import com.herfy.notification.presenter.NotificationViewModel
import com.herfy.notification.presenter.model.NotificationUIModel
import com.herfy.notification.presenter.ui.adapter.NotificationAdapter
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.helper.SnackBarUtils
import com.herfy.utils.helper.launchWithTimber
import com.herfy.utils.helper.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class NotificationFragment: BaseFragment<NotificationViewModel,FragmentNotificationBinding>() {

    private var adapter: NotificationAdapter by autoCleared()

    override val viewModel: NotificationViewModel by viewModels()
    override val layout: Int = R.layout.fragment_notification

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.setUpAdapter()
        setUpVmObservers()
    }

    private fun setUpVmObservers() {
        viewLifecycleScope.launchWithTimber {
            viewModel
                .state
                .flowWithLifecycle(lifecycle = lifecycle, Lifecycle.State.CREATED)
                .collectLatest { state->
                    handleState(state)
                }
        }
    }

    private fun handleState(state: NotificationState) {
        when(state) {
            is NotificationState.Error -> {
                SnackBarUtils
                    .showGenericSuccessSnackBar(binding.root,state.message)
            }
            is NotificationState.IsLoading -> {
                binding
                    .notificationShimmer.setVisible(state.isLoading)
                binding
                    .notificationShimmer.startShimmer()
            }
            is NotificationState.Success -> {
                adapter.updateItems(state.notifications, clearList = true)
            }
        }
    }

    private fun FragmentNotificationBinding.setUpAdapter() {
        adapter = NotificationAdapter(onNotificationClicked = ::handleNotificationClicked)
        binding.notificationRecycleview.adapter = adapter
    }

    private fun handleNotificationClicked(notificationUIModel: NotificationUIModel) {

    }
}