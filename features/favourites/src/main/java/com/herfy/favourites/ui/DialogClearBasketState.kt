package com.herfy.favourites.ui

import com.skylinedynamics.solosdk.api.models.objects.MenuItem

sealed class DialogClearBasketState {
    data class IsLoading(val isLoading:Boolean):DialogClearBasketState()

    data class Success(val currentCart: List<MenuItem>) : DialogClearBasketState()

    data class Error(val message:String) : DialogClearBasketState()
}