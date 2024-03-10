package com.herfy.notification.presenter.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.herfy.notification.R
import com.herfy.notification.databinding.FragmentNotificationBinding
import com.herfy.notification.presenter.NotificationViewModel
import com.herfy.utils.base.mvvm.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationFragment: BaseFragment<NotificationViewModel,FragmentNotificationBinding>() {
    override val viewModel: NotificationViewModel by viewModels()
    override val layout: Int = R.layout.fragment_notification

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}