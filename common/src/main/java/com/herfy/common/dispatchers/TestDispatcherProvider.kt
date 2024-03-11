package com.herfy.common.dispatchers

import com.herfy.utils.helper.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherProvider (val dispatchers: TestDispatcher = UnconfinedTestDispatcher()): DispatcherProvider {
    override val default: CoroutineDispatcher
        get() = dispatchers
    override val io: CoroutineDispatcher
        get() = dispatchers
    override val ui: CoroutineDispatcher
        get() = dispatchers
    override val uiImmediate: CoroutineDispatcher
        get() = dispatchers

}