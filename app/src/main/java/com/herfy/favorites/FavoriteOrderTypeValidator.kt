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
            isPickUpSelected(selectedOrderType, favouriteOrderType.type) -> FavoriteOrderState.PickUpSelected
            isDineInSelected(selectedOrderType, favouriteOrderType.type) -> FavoriteOrderState.DineInSelected
            else -> determineDefaultState(selectedOrderType, favouriteOrderType)
        }
    }

    private fun isDeliveryOrder(orderType: OrderTypeMenus): Boolean {
        val cacheOrderType = CacheUtil.getInstance().orderType
        return orderType.type.contains("deliver", ignoreCase = true) &&
                cacheOrderType == OrderType.NONE
    }

    private fun isPickUpSelected(selectedOrderType: OrderType, orderTypeName: String): Boolean {
        val cachedSelectedOrderType = OrderType.convertOrderTypeToString(selectedOrderType)
        return selectedOrderType.name.lowercase() == orderTypeName.lowercase() || cachedSelectedOrderType == "pickup" && cachedSelectedOrderType == orderTypeName.lowercase()
    }

    private fun isDineInSelected(selectedOrderType: OrderType, orderTypeName: String): Boolean {
        return selectedOrderType.name.replace("_", " ").lowercase() == orderTypeName.replace("-", " ").lowercase()
    }

    private fun determineDefaultState(selectedOrderType: OrderType,favouriteOrderType: OrderTypeMenus): FavoriteOrderState {

        return if (selectedOrderType == OrderType.DELIVERY && favouriteOrderType.type.contains("deliver")) {
            FavoriteOrderState.DeliverOrderSelected
        } else if (isPickUpSelected(selectedOrderType,favouriteOrderType.type)) {
            FavoriteOrderState.PickUpSelected
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