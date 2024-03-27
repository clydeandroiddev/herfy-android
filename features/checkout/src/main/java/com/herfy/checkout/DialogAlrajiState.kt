package com.herfy.checkout

sealed class DialogAlrajiState {

    data class IsLoading(val isLoading:Boolean) : DialogAlrajiState()

    object Success: DialogAlrajiState()

    data class Error(val message:String) : DialogAlrajiState()

    data class InvalidAmountError(val message: String): DialogAlrajiState()

    object ResendTimeCompleted: DialogAlrajiState()

    data class ResendTimer(val remainingSeconds:Int) : DialogAlrajiState()
}