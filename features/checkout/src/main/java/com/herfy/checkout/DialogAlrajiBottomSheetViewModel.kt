package com.herfy.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DispatcherProvider
import com.herfy.utils.helper.launchWithTimber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DialogAlrajiBottomSheetViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _state = MutableSharedFlow<DialogAlrajiState>()

    val state = _state
    private var timerJob: Job? = null

    companion object {
        const val COUNTDOWN_MAX_TIMER = 30
    }

    init {
        startTimer()
    }

    fun verifyCode(code: String, userInputAmount: String) {

        viewModelScope.launchWithTimber(dispatchers.io) {
            val areAllFieldsValid =
                validateFields(userInputAmount = userInputAmount, code = code) { errorState ->
                    _state.emit(errorState)
                }
            if (areAllFieldsValid.not()) {
                return@launchWithTimber
            }
            _state.emit(DialogAlrajiState.IsLoading(true))
            // todo ask maam rio what endpoint should be use here
        }
    }

    private inline fun validateFields(
        code: String,
        userInputAmount: String,
        onError: (DialogAlrajiState) -> Unit
    ): Boolean {
        val customerId = CacheUtil.getInstance()
        var otpLength = CacheUtil.getInstance().concept.otpLength
        val totalAmount = 20 // mocking for now
        if (code.length < otpLength && userInputAmount.isEmpty()) {
            onError(
                DialogAlrajiState.Error(
                    CacheUtil.getInstance().getTranslations(Text.CODE_REQUIRED)
                )
            )
            onError(
                DialogAlrajiState.InvalidAmountError(
                    CacheUtil.getInstance().getTranslations(
                        Text.ALRAJI_ENTER_AMOUNT_ERROR,
                        "Amount cannot exceed the total amount"
                    )
                )
            )
            return false
        }
        if (code.length < otpLength) {
            onError(
                DialogAlrajiState.Error(
                    CacheUtil.getInstance().getTranslations(Text.CODE_REQUIRED)
                )
            )
            return false
        }
        if (userInputAmount.isEmpty()) {
            onError(
                DialogAlrajiState.InvalidAmountError(
                    CacheUtil.getInstance().getTranslations(
                        Text.ALRAJI_ENTER_AMOUNT_ERROR,
                        "Amount cannot exceed the total amount"
                    )
                )
            )
            return false
        }
        if (userInputAmount.isNotEmpty() && userInputAmount.toInt() > totalAmount) {
            onError(
                DialogAlrajiState.InvalidAmountError(
                    CacheUtil.getInstance().getTranslations(
                        Text.ALRAJI_ENTER_AMOUNT_ERROR,
                        "Amount cannot exceed the total amount"
                    )
                )
            )
            return false
        }
        return true

    }

    fun resend() {
        viewModelScope.launchWithTimber(dispatchers.io) {
            startTimer()
            try {
                // todo call the endpoint
            } catch (e: Exception) {
                _state.emit(DialogAlrajiState.Error(e.localizedMessage.orEmpty()))
            } finally {
                _state.emit(DialogAlrajiState.IsLoading(false))
            }
        }
    }


    private fun startTimer() {
        // Cancel other timer job, if to prevent memory leaks
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            _state.emit(DialogAlrajiState.IsLoading(true))
            flow {
                for (eachTime in COUNTDOWN_MAX_TIMER downTo 1) {
                    emit(eachTime)
                    delay(1000L)
                }
            }.onCompletion {
                _state.emit(DialogAlrajiState.ResendTimeCompleted)
            }.collect {
                _state.emit(DialogAlrajiState.ResendTimer(it))
            }
        }
    }
}