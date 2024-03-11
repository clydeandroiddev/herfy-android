package com.herfy.favorites

import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.OrderTypeMenus

object FavoriteOrderTypeValidator {
    fun determineSelectedOrderState(
        selectedOrderType: OrderType,
        favouriteOrderType: OrderTypeMenus
    ): FavoriteOrderState {
        return when {
            isDeliveryOrder(favouriteOrderType) -> FavoriteOrderState.DeliverOrderSelected
            isMatchingType(selectedOrderType, favouriteOrderType.type) -> FavoriteOrderState.PickUpSelected
            isDineInSelected(selectedOrderType, favouriteOrderType.type) -> FavoriteOrderState.DineInSelected
            else -> determineDefaultState(selectedOrderType, favouriteOrderType)
        }
    }

    private fun isDeliveryOrder(orderType: OrderTypeMenus): Boolean {
        return orderType.type.contains("deliver", ignoreCase = true) &&
                CacheUtil.getInstance().orderType == OrderType.NONE
    }

    private fun isMatchingType(selectedOrderType: OrderType, orderTypeName: String): Boolean {
        return selectedOrderType.name.lowercase() == orderTypeName.lowercase()
    }

    private fun isDineInSelected(selectedOrderType: OrderType, orderTypeName: String): Boolean {
        return selectedOrderType.name.replace("_", " ").lowercase() == orderTypeName.replace("-", " ").lowercase()
    }

    private fun determineDefaultState(selectedOrderType: OrderType, favouriteOrderType: OrderTypeMenus): FavoriteOrderState {
        return if (selectedOrderType == OrderType.DELIVERY && favouriteOrderType.type.contains("deliver")) {
            FavoriteOrderState.DeliverOrderSelected
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