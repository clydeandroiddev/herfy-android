package com.herfy.notification.presenter.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.herfy.notification.databinding.ItemNotificationBinding
import com.herfy.notification.presenter.model.NotificationUIModel
import com.herfy.utils.base.mvvm.BaseListAdapter

class NotificationAdapter(private val onNotificationClicked: (NotificationUIModel) -> Unit): BaseListAdapter<NotificationUIModel,BaseListAdapter.BaseViewViewHolder<NotificationUIModel>>(NotificationCallback) {


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewViewHolder<NotificationUIModel> {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BaseViewViewHolder<NotificationUIModel>, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }


    inner class NotificationViewHolder(override val binding: ItemNotificationBinding): BaseListAdapter.BaseViewViewHolder<NotificationUIModel>(binding =binding ) {
        private var notificationUIModel: NotificationUIModel? =null

        init {
            binding
                .root
                .setOnClickListener {
                    notificationUIModel?.let { notification -> onNotificationClicked(notification) }
                }
        }
        override fun bind(item: NotificationUIModel) {
            this.notificationUIModel = item
            binding.apply {
                txtNotification.text = item.description
                txtMinutes.text = item.minutes
            }


        }
    }
}

object NotificationCallback: DiffUtil.ItemCallback<NotificationUIModel>() {
    override fun areItemsTheSame(
        oldItem: NotificationUIModel,
        newItem: NotificationUIModel
    ): Boolean {
      return oldItem == newItem
    }

    override fun areContentsTheSame(
        oldItem: NotificationUIModel,
        newItem: NotificationUIModel
    ): Boolean = oldItem == newItem

}