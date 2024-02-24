package com.herfy.utils.helper

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext


open class CoroutineContextProvider {
    open val Main: CoroutineContext by lazy { Dispatchers.Main }
    open val IO: CoroutineContext by lazy { Dispatchers.Default }
}


