package com.herfy.productpagedetails.ui.viewmodel

import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.herfy.model.ErrorMsgItem
import com.herfy.utils.base.mvvm.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ComboViewModel  @Inject constructor(): BaseViewModel() {

    val notes = ObservableField("")

    private val _showError = MutableLiveData<ErrorMsgItem>()
    val showError : LiveData<ErrorMsgItem> = _showError



    fun addQuantity(){

    }
    fun minusQuantity(){

    }

    fun addToCart(){
        notes.get()?.let {  }
    }




}