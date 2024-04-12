package com.herfy.utils.base.mvvm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDirections
import com.herfy.utils.helper.Event
import com.herfy.utils.helper.NavigationCommand
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

open class BaseViewModel : ViewModel() {


    private val _navigation = MutableLiveData<Event<NavigationCommand>>()
    val navigation: LiveData<Event<NavigationCommand>> get() = _navigation

    val showProgressBar = MutableLiveData<Boolean>()

    private val _commonException = MutableLiveData<Throwable?>()
    val commonException: LiveData<Throwable?> = _commonException

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable: Throwable ->
        throwable.printStackTrace()
        _commonException.postValue(throwable)
        showProgressBar.postValue(false)
    }

    private val exceptionHandlerForBackgroundTask =
        CoroutineExceptionHandler { _, throwable: Throwable ->
            throwable.printStackTrace()
            _commonException.postValue(throwable)
        }

    fun invoke(task: suspend () -> Unit) {

        showProgressBar.value = true
        viewModelScope.launch(exceptionHandler) {
            task.invoke()
        }.invokeOnCompletion {
            showProgressBar.value = false
        }
    }

    fun invokeAtBackground(task: suspend () -> Unit) {
        viewModelScope.launch(exceptionHandlerForBackgroundTask) { task.invoke() }
    }


    fun invokeExceptionIgnore(task: suspend () -> Unit) {
        showProgressBar.postValue(true)
        viewModelScope.launch {
            task.invoke()
            showProgressBar.postValue(false)
        }
    }




    fun navigate(navDirections: NavDirections) {
        _navigation.value = Event(NavigationCommand.ToDirection(navDirections))
    }

    fun navigateBack() {
        _navigation.value = Event(NavigationCommand.Back)
    }



}