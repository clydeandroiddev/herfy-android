package com.herfy.notification.core

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.herfy.utils.base.mvvm.BaseViewModel
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.Before
import org.junit.Rule

abstract class BaseViewModelTest<VM : BaseViewModel> {

    @get:Rule
    val executor = InstantTaskExecutorRule()

    protected open val dispatcher: TestDispatcher = StandardTestDispatcher()

     val testDispatcherProvider by lazy { TestDispatcherProvider(dispatcher) }

    @get:Rule
    val mainDispatcherRule by lazy { MainDispatcherRule(testDispatcherProvider.dispatchers) }

    @get:Rule
    val mockkRule by lazy { MockKRule(this) }

    protected lateinit var viewModel: VM
        private set

    abstract fun createViewModel(): VM

    @Before
    fun setupViewModel() {
        viewModel = createViewModel()
    }
}