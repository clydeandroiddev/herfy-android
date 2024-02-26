package com.herfy.phonenumber

import androidx.lifecycle.viewModelScope
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.AllowedCountries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
@HiltViewModel
class EnterNewPhoneNumberViewModel @Inject constructor(): BaseViewModel() {


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
        // todo wait for the BE endpoint
    }
}


