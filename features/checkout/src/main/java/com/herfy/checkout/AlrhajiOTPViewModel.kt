package com.herfy.checkout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonObject
import com.herfy.model.ErrorMsgItem
import com.herfy.model.payment.OTPData
import com.herfy.repository.PaymentRepository
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class AlrhajiOTPViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository
) : BaseViewModel() {

    private val _showError = MutableLiveData<ErrorMsgItem>()
    val showError: LiveData<ErrorMsgItem> = _showError

    private val _showCountDown = MutableLiveData<Long>()
    val showCountdown: LiveData<Long> = _showCountDown

    private val _showVerified = MutableLiveData<Event<OTPData>>()
    val showVerified: LiveData<Event<OTPData>> = _showVerified

    private val _isLoading = MutableLiveData<Event<Boolean>>()
    val isLoading: LiveData<Event<Boolean>> = _isLoading

    fun sendOTP(number: String) {
        _isLoading.value = Event(true)
        invoke {
            paymentRepository.sendOTP(number) {
                _isLoading.value = Event(false)
                if (it.isSuccess) {
                    _showCountDown.value = (3 * 60 * 1000)
                } else {
                    it.error?.let { error ->
                        _showError.value = error
                        // 210 means there is an existing OTP code
                        // proceed to input
                        if (!error.closePage) {
                            _showCountDown.value = (3 * 60 * 1000)
                        }
                    }
                }
            }
        }
    }

    fun verifyOTP(otp: String, amount: String, cartBody: JsonObject) {
        _isLoading.value = Event(true)
        invoke {
            paymentRepository.verifyOTP(otp, amount, cartBody) {
                _isLoading.value = Event(false)
                if (it.isSuccess) {
                    it.data?.let { data ->
                        _showVerified.value = Event(data)
                    }
                } else {
                    it.error?.let {
                        _showError.value = it
                    }
                }
            }
        }
    }

    fun getFormattedTimeForCountdown(millisUntilFinished: Long): String? {
        var millisUntilFinished = millisUntilFinished
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
        millisUntilFinished -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)
        var countdownTime = ""
        if (minutes > 0) {
            val minlabel = if (minutes == 1L) CacheUtil.getInstance()
                .getTranslations(Text.MINUTE) else CacheUtil.getInstance().getTranslations(
                Text.MINUTES
            )
            countdownTime += "$minutes $minlabel "
        }
        if (seconds > 0) {
            val seclabel = if (seconds == 1L) CacheUtil.getInstance()
                .getTranslations(Text.SECOND) else CacheUtil.getInstance().getTranslations(
                Text.SECONDS
            )
            countdownTime += "$seconds $seclabel"
        }
        return countdownTime
    }

}