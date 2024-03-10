package com.herfy.favorites

interface OrderTypeView {
    fun setOrderTypeSelected()
    fun setOrderTypeUnselected()

    fun setInitialSelectedOrderType()
    val tag: String
}