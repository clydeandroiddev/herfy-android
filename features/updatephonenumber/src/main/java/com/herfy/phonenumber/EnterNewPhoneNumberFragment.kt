package com.herfy.phonenumber

import android.os.Bundle
import android.view.View
import com.herfy.phonenumber.databinding.FragmentEnterNewPhoneNumberBinding
import com.herfy.utils.base.mvvm.BaseFragment
import androidx.fragment.app.viewModels
import com.herfy.utils.extensions.debounceTextChanges
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.helper.launchWithTimber
import com.herfy.utils.helper.viewLifecycleScope
import kotlinx.coroutines.flow.collectLatest

class EnterNewPhoneNumberFragment: BaseFragment<EnterNewPhoneNumberViewModel,FragmentEnterNewPhoneNumberBinding>() {
    override val viewModel: EnterNewPhoneNumberViewModel by viewModels()
    override val layout: Int = R.layout.fragment_enter_new_phone_number

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.setUpViews()
    }
    private fun FragmentEnterNewPhoneNumberBinding.setUpViews() {
        viewLifecycleScope.launchWithTimber {
            mobileNumber.debounceTextChanges()
                .collectLatest {

                }
        }
        mobileNumber.ninjaTap(viewLifecycleScope) {
            // todo call the endpoint to enter
        }



    }
}