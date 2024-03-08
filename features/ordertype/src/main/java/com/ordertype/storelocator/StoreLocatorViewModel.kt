package com.ordertype.storelocator

import android.util.Log
import com.herfy.repository.AuthRepository
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.helper.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@HiltViewModel
class StoreLocatorViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dispatchers: DispatcherProvider
) : BaseViewModel() {

    private val _state: MutableSharedFlow<StoreLocatorState> = MutableSharedFlow()

    val state: Flow<StoreLocatorState> = _state

    fun getStores(page: Int) {
        Log.e("AAADADAD", "ADADADADA:" + page)
    }
}
