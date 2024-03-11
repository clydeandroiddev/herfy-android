package com.herfy.favorites

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.textview.MaterialTextView
import com.herfy.R
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.OrderTypeMenus

class OrderTypeViewHolder(
    private val view: View,
    private val orderTypeMenus: OrderTypeMenus,
    private val onClick: (String) -> Unit,
) : OrderTypeView {

    private val txtOrder = view.findViewById<MaterialTextView>(R.id.txtOrderType)
    private val container = view.findViewById<ConstraintLayout>(R.id.favouriteOrderContainer)

    override val tag: String
        get() =  view.tag as String
    init {
        view.tag = orderTypeMenus.type
        container.setOnClickListener { onClick(view.tag as String) }
    }

    override fun setOrderTypeSelected() {
        txtOrder.setTextColor(ContextCompat.getColor(view.context, R.color.white))
        txtOrder.setBackgroundResource(R.drawable.bg_favourite_order_type_selected)
    }

    override fun setOrderTypeUnselected() {
        txtOrder.setTextColor(ContextCompat.getColor(view.context, R.color.color_gray))
        txtOrder.setBackgroundResource(R.drawable.bg_favourite_order_types_unselected)
    }

    override fun setInitialSelectedOrderType() {
        val orderType = FavoriteOrderTypeValidator.determineSelectedOrderState(
            selectedOrderType =CacheUtil.getInstance().orderType,
            favouriteOrderType = orderTypeMenus
        )
        when(orderType) {
            FavoriteOrderState.DeliverOrderSelected -> setOrderTypeSelected()
            FavoriteOrderState.DineInSelected -> setOrderTypeSelected()
            FavoriteOrderState.PickUpSelected -> setOrderTypeSelected()
            else -> setOrderTypeUnselected()
        }
    }


    fun bind() {
        val formattedOrderType = OrderType.convertOrderTypeNameToDisplayName(orderTypeMenus.type)
        txtOrder.text = formattedOrderType
    }
}