package com.ordertype.storelocator

import com.herfy.repository.AuthRepository
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.helper.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StoreLocatorViewModel @Inject constructor(
    private val authRepository: AuthRepository, private val dispatchers: DispatcherProvider
) : BaseViewModel() {

}
