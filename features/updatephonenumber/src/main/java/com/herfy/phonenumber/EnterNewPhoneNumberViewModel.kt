package com.herfy.phonenumber

import androidx.lifecycle.viewModelScope
import com.herfy.phonenumber.model.EnterNewPhoneNumberState
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DispatcherProvider
import com.herfy.utils.helper.launchWithTimber
import com.skylinedynamics.solosdk.api.models.objects.AllowedCountries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
@HiltViewModel
class EnterNewPhoneNumberViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider
): BaseViewModel() {
    private val _state = MutableSharedFlow<EnterNewPhoneNumberState>()

    val state = _state


    private val _allowCountriesState = MutableStateFlow<ArrayList<AllowedCountries>>(
        ArrayList(CacheUtil.getInstance().concept.allowedCountries)
    )

    val distinctCountriesState = _allowCountriesState
        .map { country -> country.distinctBy { Pair(it.dialCode,it.code) } }
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5000), emptyList())


    private val _currentSelectedItemState = MutableStateFlow<Int>(0)
    val currentSelectedItem = _currentSelectedItemState.asStateFlow()


   fun setSelectedCountry(position:Int) {
       _currentSelectedItemState.value = position
   }


    fun updatePhoneNumber(phoneNumber:String) {
        viewModelScope.launchWithTimber(dispatchers.io) {
            try {
                _state.emit(EnterNewPhoneNumberState.IsLoading(true))
                delay(3000) // to simulate API call for now
                _state.emit(EnterNewPhoneNumberState.Success(phoneNumber))
            } catch (e: Exception) {
                _state.emit(EnterNewPhoneNumberState.Error(e.localizedMessage.orEmpty()))
            }finally {
                _state.emit(EnterNewPhoneNumberState.IsLoading(false))
            }
        }
    }
}


