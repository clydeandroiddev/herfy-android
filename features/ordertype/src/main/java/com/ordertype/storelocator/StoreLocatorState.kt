package com.ordertype.storelocator

sealed class StoreLocatorState {
    data class ShowLoading(val loading: Boolean) : StoreLocatorState()
    data class ShowError(val error: String): StoreLocatorState()
}