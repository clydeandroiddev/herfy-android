package com.herfy.favourites.ui

sealed class DialogClearBasketState {
    data class IsLoading(val isLoading:Boolean):DialogClearBasketState()

    object Success: DialogClearBasketState()
}