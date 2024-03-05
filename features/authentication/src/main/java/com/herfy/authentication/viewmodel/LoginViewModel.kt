package com.herfy.authentication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.herfy.model.ErrorMsgItem
import com.herfy.repository.AuthRepository
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.Customer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _showError = MutableLiveData<ErrorMsgItem>()
    val showError: LiveData<ErrorMsgItem> = _showError

    private val _showLoading = MutableLiveData<Boolean>()
    val showLoading: LiveData<Boolean> = _showLoading

    private val _customer = MutableLiveData<Customer>()
    val customer: LiveData<Customer> = _customer

    fun login(phoneNumber: String) {
        invoke {
            if (phoneNumber.isEmpty()) {
                _showError.value = ErrorMsgItem(
                    true, CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_REQUIRED)
                )
            } else {
                _showLoading.postValue(true)
                authRepository.login(phoneNumber) {
                    _showLoading.postValue(false)
                    if (it.success) {
                        _customer.value = it.data as Customer
                    } else {
                        _showError.value = ErrorMsgItem(true, it.message)
                    }
                }
            }
        }
    }
}

