package com.herfy.history.historynew.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.herfy.common.R
import com.herfy.common.formatOrderTime
import com.herfy.common.orderTypeStatus
import com.herfy.common.orderTypeStatusColor
import com.herfy.common.orderTypeStatusIcon
import com.herfy.common.orderTypeString
import com.herfy.history.databinding.ItemNewOrderHistoryBinding
import com.herfy.util.StringUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory
import com.skylinedynamics.solosdk.api.models.objects.OrderStatusType


class HistoryListAdapter(
    val historyList: ArrayList<OrderHistory> = arrayListOf(), val historyListener: HistoryListener
) : RecyclerView.Adapter<HistoryListAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemNewOrderHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemNewOrderHistoryBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            com.herfy.history.R.layout.item_new_order_history,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = historyList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyList[position]
        holder.binding.apply {

            viewDetails.setOnClickListener {
                historyListener.onViewDetails(item, position)
            }

            rate.setOnClickListener {
                historyListener.onRate(item, position)
            }

            reOrder.setOnClickListener {
                historyListener.onReorder(item, position)
            }

            itemPrice.text = StringUtil.getNewFormattedPrice(
                item.total, true
            )

            if (item.sequence < OrderStatusType.CANCELLED_BY_CUSTOMER.value) {
                status.text = item.attrType.orderTypeStatus()
                statusIcon.setImageResource(item.attrType.orderTypeStatusIcon())
                status.setTextColor(status.context.resources.getColor(item.attrType.orderTypeStatusColor()))
            } else if (item.sequence >= OrderStatusType.CANCELLED_BY_CUSTOMER.value &&
                item.sequence <= OrderStatusType.CANCELLED_BY_EMPLOYEE.value
            ) {
                status.setTextColor(status.context.resources.getColor(R.color.cancelled_color))
                statusIcon.setImageResource(R.drawable.ic_circle_xmark)
                status.text = CacheUtil.getInstance().getTranslations(Text.CANCELLED, "Cancelled")
            } else {
                status.setTextColor(status.context.resources.getColor(R.color.cancelled_color))
                statusIcon.setImageResource(R.drawable.ic_circle_xmark)
                status.text = CacheUtil.getInstance().getTranslations(Text.FAILED, "Failed")
            }

            deliveryAndStore.text = item.attrType.orderTypeString() + " - " + item.locationName

            viewDetailsLabel.text =
                CacheUtil.getInstance().getTranslations(Text.VIEW_DETAILS, "View Details")
            rateLabel.text = CacheUtil.getInstance().getTranslations(Text.RATE_LABEL, "Rate")
            reOrderLabel.text =
                CacheUtil.getInstance().getTranslations(Text.REORDER_LABEL, "Reorder")
            rated.text = CacheUtil.getInstance().getTranslations(Text.RATED, "Rated")

            rated.isInvisible = item.averageRating <= 0

            rate.isVisible = item.averageRating <= 0

            val adapter = MenuImagesAdapter(item.menuImages)
            itemImagesList.adapter = adapter
            adapter.notifyDataSetChanged()

            itemsCount.text =
                CacheUtil.getInstance().getTranslations(Text.CART_ITEM_COUNT_LABEL, "(x) items")
                    .replace("(x)", item.menuImages.size.toString())

            dateAndTime.text = item.createdAt.formatOrderTime()

            executePendingBindings()
        }
    }


    fun updateList(historyList: ArrayList<OrderHistory>) {
        this.historyList.clear()
        this.historyList.addAll(historyList)
        notifyDataSetChanged()
    }

    interface HistoryListener {
        fun onViewDetails(item: OrderHistory, position: Int)
        fun onReorder(item: OrderHistory, position: Int)
        fun onRate(item: OrderHistory, position: Int)
    }
}
