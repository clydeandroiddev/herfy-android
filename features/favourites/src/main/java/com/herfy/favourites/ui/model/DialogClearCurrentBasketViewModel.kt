package com.herfy.favourites.ui.model

import com.herfy.favourites.ui.DialogClearBasketState
import com.herfy.utils.base.mvvm.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class DialogClearCurrentBasketViewModel: BaseViewModel() {

    private val _state = MutableSharedFlow<DialogClearBasketState>()
    val state = _state.asSharedFlow()

    fun clearBasket() {
        // TODO ask maam rio
    }
}