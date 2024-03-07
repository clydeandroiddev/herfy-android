package com.ordertype.storelocator

import androidx.fragment.app.viewModels
import com.herfy.ordertype.R
import com.herfy.ordertype.databinding.StoreLocatorBinding
import com.herfy.utils.base.mvvm.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StoreLocatorFragment : BaseFragment<StoreLocatorViewModel, StoreLocatorBinding>() {

    override val viewModel: StoreLocatorViewModel by viewModels()

    override val layout: Int = R.layout.store_locator
}