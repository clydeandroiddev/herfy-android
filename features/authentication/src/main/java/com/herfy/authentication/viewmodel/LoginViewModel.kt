package com.herfy.authentication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.herfy.authentication.state.LoginViewState
import com.herfy.repository.AuthRepository
import com.herfy.repository.HomeRepository
import com.herfy.utils.base.mvvm.BaseViewModel
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.cast
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DispatcherProvider
import com.herfy.utils.helper.launchWithTimber
import com.skylinedynamics.solosdk.api.models.objects.Customer
import com.skylinedynamics.solosdk.api.models.objects.Slide
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val homeRepository: HomeRepository,
    private val dispatchers: DispatcherProvider
) : BaseViewModel() {

    private val _bannerList = MutableLiveData<ArrayList<Slide>?>()
    val bannerList: LiveData<ArrayList<Slide>?> = _bannerList

    private val _state: MutableSharedFlow<LoginViewState> = MutableSharedFlow()

    val state: Flow<LoginViewState> = _state

    fun login(code: String, phoneNumber: String) {
        viewModelScope.launchWithTimber(dispatchers.io) {
            if (phoneNumber.isEmpty()) {
                _state.emit(
                    LoginViewState.ShowError(
                        CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_REQUIRED)
                    )
                )
            } else {
                _state.emit(LoginViewState.ShowLoading(true))
                val mobile = code + "" + phoneNumber
                processLogin(mobile)
            }
        }
    }

    fun getBannerSlider() {
        invoke {
            val sliderId = CacheUtil.getInstance().getLoginBannerId(Values.LOGIN_BANNER)
            if (!sliderId.isNullOrEmpty()) {
                homeRepository.getSliders(sliderId) { homeResponse ->
                    if (homeResponse.success) {
                        homeResponse.data?.let {
                            it.cast<ArrayList<Slide>> { slides ->
                                _bannerList.value = slides
                            }
                        } ?: run {
                            _bannerList.value = arrayListOf()
                        }
                    } else {
                        _bannerList.value = arrayListOf()
                    }
                }
            } else {
                _bannerList.value = arrayListOf()
            }
        }
    }

    private suspend fun processGoogle(email: String, token: String) {
        authRepository.googleLogin(email, token) {
            viewModelScope.launchWithTimber(dispatchers.io) {
                _state.emit(LoginViewState.ShowLoading(false))
                if (it.success) {
                    val cust = it.data as Customer
                    if (it.showAdd) {
                        _state.emit(LoginViewState.ShowAddNumber(cust))
                    } else {
                        if (it.showCode) {
                            _state.emit(LoginViewState.GoogleGoOTP(cust))
                        } else {
                            getCustomer(cust.id)
                        }
                    }
                } else {
                    _state.emit(
                        LoginViewState.ShowError(
                            it.message
                        )
                    )
                }
            }
        }
    }

    fun getCustomer(customerId: String) {
        viewModelScope.launchWithTimber(dispatchers.io) {
            _state.emit(LoginViewState.ShowLoading(true))
            authRepository.getCustomer(customerId) {
                viewModelScope.launchWithTimber(dispatchers.io) {
                    _state.emit(LoginViewState.ShowLoading(false))
                    if (it.success) {
                        _state.emit(LoginViewState.GoogleSuccess(it.data as Customer))
                    } else {
                        _state.emit(
                            LoginViewState.ShowError(
                                it.message
                            )
                        )
                    }
                }
            }
        }
    }

    fun updateMobile(mobile: String, customer: Customer) {
        viewModelScope.launchWithTimber(dispatchers.io) {
            if (mobile.isEmpty()) {
                _state.emit(
                    LoginViewState.ShowError(
                        CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_REQUIRED)
                    )
                )
            } else {
                _state.emit(LoginViewState.ShowLoading(true))
                authRepository.sendVerification(customer.id, mobile) {
                    viewModelScope.launchWithTimber(dispatchers.io) {
                        _state.emit(LoginViewState.ShowLoading(false))
                        if (it.success) {
                            _state.emit(
                                LoginViewState.GoogleSuccessFromAddNumber(
                                    customer, mobile
                                )
                            )
                        } else {
                            _state.emit(
                                LoginViewState.ShowError(
                                    it.message
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun loginViaGoogle(email: String, token: String) {
        viewModelScope.launchWithTimber(dispatchers.io) {
            processGoogle(email, token)
        }
    }

    fun guestLogin(phoneNumber: String) {
        viewModelScope.launchWithTimber(dispatchers.io) {
            if (phoneNumber.isEmpty()) {
                _state.emit(
                    LoginViewState.ShowError(
                        CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_REQUIRED)
                    )
                )
            } else {
                _state.emit(LoginViewState.ShowLoading(true))
                processGuest(phoneNumber)
            }
        }
    }

    private suspend fun processGuest(mobile: String) {
        authRepository.guestLogin(mobile) {
            viewModelScope.launchWithTimber(dispatchers.io) {
                _state.emit(LoginViewState.ShowLoading(false))
                if (it.success) {
                    _state.emit(LoginViewState.GuestSuccess(it.customerId, mobile))
                } else {
                    _state.emit(
                        LoginViewState.ShowError(
                            it.message
                        )
                    )
                }
            }
        }
    }

    private suspend fun processLogin(mobile: String) {
        authRepository.login(mobile) {
            viewModelScope.launchWithTimber(dispatchers.io) {
                _state.emit(LoginViewState.ShowLoading(false))
                if (it.success) {
                    _state.emit(LoginViewState.UserLogin(it.data as Customer))
                } else {
                    _state.emit(
                        LoginViewState.ShowError(
                            it.message
                        )
                    )
                }
            }
        }
    }
}

