package com.herfy.favourites.ui

import androidx.lifecycle.viewModelScope
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DispatcherProvider
import com.herfy.utils.helper.launchWithTimber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@HiltViewModel
class DialogClearCurrentBasketViewModel @Inject constructor(private val dispatchers: DispatcherProvider): BaseViewModel() {

    private val _state = MutableSharedFlow<DialogClearBasketState>()

    val state = _state

    fun clearCart() {
        viewModelScope.launchWithTimber(dispatchers.io) {
            try {
                _state.emit(DialogClearBasketState.IsLoading(true))
                CacheUtil.getInstance().cart = arrayListOf()

                val currentCart = CacheUtil.getInstance()
                    .cart
                _state.emit(DialogClearBasketState.Success(currentCart))
            } catch (e: Exception) {
                _state.emit(DialogClearBasketState.Error(e.localizedMessage.orEmpty()))

            } finally {
                _state.emit(DialogClearBasketState.IsLoading(false))
            }
        }
    }
}