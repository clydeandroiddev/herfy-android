package com.herfy.authentication.views

import android.content.Intent
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.herfy.authentication.R
import com.herfy.authentication.databinding.FragmentAddNewNameBinding
import com.herfy.authentication.viewmodel.InputUserDetailViewModel
import com.herfy.utils.base.mvvm.BaseActivity
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.cast
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.CacheUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InputUserDetailFragment :
    BaseFragment<InputUserDetailViewModel, FragmentAddNewNameBinding>() {

    private val args: InputUserDetailFragmentArgs by navArgs()

    override val viewModel: InputUserDetailViewModel by viewModels()

    override val layout: Int = R.layout.fragment_add_new_name

    override fun initViews() {
        super.initViews()
        binding.apply {
            backButton.setOnClickListener {
                navigateUp()
            }

            continueButton.setOnClickListener {

            }

            skipButton.setOnClickListener {
                requireContext().applicationContext.sendBroadcast(
                    Intent(
                        requireContext().getString(R.string.exit_to_home)
                    )
                )
            }

            fullName.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    continueButton.performClick()
                    true
                } else false
            }
        }
    }

    override fun initTranslations() {
        super.initTranslations()
        binding.apply {
            title.text =
                CacheUtil.getInstance().getTranslations(Text.SETUP_ACCOUNT, "Set up your account")
            fewMoreLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.A_FEW_MORE_THINGS, "A few more things to know you better")
            fullNameLabel.text =
                CacheUtil.getInstance().getTranslations(Text.FULL_NAME, "Full Name")
            continueButton.text = CacheUtil.getInstance().getTranslations(Text.NEXT, "Next")
            skipButton.text = CacheUtil.getInstance().getTranslations(Text.SKIP, "Skip")
        }
    }

    override fun observe() {
        super.observe()
    }
}