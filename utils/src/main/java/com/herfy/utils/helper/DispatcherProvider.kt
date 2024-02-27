package com.herfy.utils.helper

import kotlinx.coroutines.CoroutineDispatcher

interface DispatcherProvider {

    val default: CoroutineDispatcher

    val io: CoroutineDispatcher

    val ui: CoroutineDispatcher

    val uiImmediate: CoroutineDispatcher
}