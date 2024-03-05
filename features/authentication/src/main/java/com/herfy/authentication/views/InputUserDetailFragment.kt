package com.herfy.authentication.views

import androidx.fragment.app.viewModels
import com.herfy.authentication.R
import com.herfy.authentication.databinding.FragmentAddNewNameBinding
import com.herfy.authentication.databinding.FragmentLoginBinding
import com.herfy.authentication.viewmodel.InputUserDetailViewModel
import com.herfy.authentication.viewmodel.LoginViewModel
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InputUserDetailFragment :
    BaseFragment<InputUserDetailViewModel, FragmentAddNewNameBinding>() {

    override val viewModel: InputUserDetailViewModel by viewModels()
    override val layout: Int = R.layout.fragment_add_new_name

    override fun initViews() {
        super.initViews()
        binding.apply {

        }
    }

    override fun initTranslations() {
        super.initTranslations()
        binding.apply {

        }
    }

    override fun observe() {
        super.observe()
    }
}