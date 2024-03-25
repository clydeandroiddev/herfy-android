package com.herfy.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herfy.utils.helper.DispatcherProvider
import com.herfy.utils.helper.launchWithTimber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@HiltViewModel
class DialogAlrajiRequestOtpViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider
): ViewModel() {

    private val _state = MutableSharedFlow<DialogRequestOTPState>()

    val state = _state


    fun requestOtp(userInputNumber:String) {

        viewModelScope.launchWithTimber(dispatchers.io) {
            if (userInputNumber.isNotEmpty()) {
                _state.emit(DialogRequestOTPState.Error("")) // todo leave empty for nwo ask designer what text should be display here
            }
            try {
                // todo ask maam rio what endpoint will be use here
            }catch (e: Exception) {
                _state.emit(DialogRequestOTPState.Error(e.localizedMessage.orEmpty()))
            }finally {
                _state.emit(DialogRequestOTPState.IsLoading(false))
            }
        }
    }
}