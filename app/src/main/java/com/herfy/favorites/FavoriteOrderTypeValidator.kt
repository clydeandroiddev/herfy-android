package com.herfy.favorites

import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.OrderType

object FavoriteOrderTypeValidator {
    fun determineSelectedOrderState(
        selectedOrderType: OrderType,
        favouriteOrderType: FavoriteOrderTypeUIModel
    ): FavoriteOrderState {
        return when {
            isDeliveryOrder(favouriteOrderType) -> FavoriteOrderState.DeliverOrderSelected
            isPickUpSelected(selectedOrderType, favouriteOrderType.orderTypeName) -> FavoriteOrderState.PickUpSelected
            isDineInSelected(selectedOrderType, favouriteOrderType.orderTypeName) -> FavoriteOrderState.DineInSelected
            else -> determineDefaultState(selectedOrderType, favouriteOrderType)
        }
    }

    private fun isDeliveryOrder(orderType: FavoriteOrderTypeUIModel): Boolean {
        val cacheOrderType = CacheUtil.getInstance().orderType
        return orderType.orderTypeName.contains("deliver", ignoreCase = true) &&
                cacheOrderType == OrderType.NONE
    }

    private fun isPickUpSelected(selectedOrderType: OrderType, orderTypeName: String): Boolean {
        val cachedSelectedOrderType = OrderType.convertOrderTypeToString(selectedOrderType)
        return selectedOrderType.name.lowercase() == orderTypeName.lowercase() || cachedSelectedOrderType == "pickup" && cachedSelectedOrderType == orderTypeName.lowercase()
    }

    private fun isDineInSelected(selectedOrderType: OrderType, orderTypeName: String): Boolean {
        return selectedOrderType.name.replace("_", "").lowercase() == orderTypeName.replace("-", "").lowercase()
    }

    private fun determineDefaultState(selectedOrderType: OrderType,favouriteOrderType: FavoriteOrderTypeUIModel): FavoriteOrderState {
        val orderTypeName =favouriteOrderType.orderTypeName
        return if (selectedOrderType == OrderType.DELIVERY && favouriteOrderType.orderTypeName.contains("deliver")) {
            FavoriteOrderState.DeliverOrderSelected
        } else if (isPickUpSelected(selectedOrderType,orderTypeName)) {
            FavoriteOrderState.PickUpSelected
        }else if (isDineInSelected(selectedOrderType,orderTypeName)) {
            FavoriteOrderState.DineInSelected
        } else {
            FavoriteOrderState.UnSelected
        }
    }
}

sealed class FavoriteOrderState {
    object DeliverOrderSelected : FavoriteOrderState()
    object PickUpSelected : FavoriteOrderState()
    object DineInSelected : FavoriteOrderState()
    object UnSelected : FavoriteOrderState()
}