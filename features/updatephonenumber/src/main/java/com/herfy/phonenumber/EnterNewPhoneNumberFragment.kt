package com.herfy.phonenumber

import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.herfy.common.base.BaseBottomSheetDialog
import com.herfy.common.utils.autoCleared
import com.herfy.phonenumber.adapter.PhoneNumberAdapter
import com.herfy.phonenumber.databinding.FragmentEnterNewPhoneNumberBinding
import com.herfy.phonenumber.model.CountriesUIModel
import com.herfy.phonenumber.model.EnterNewPhoneNumberState
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.debounceTextChanges
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DialogUtils
import com.herfy.utils.helper.StringUtil
import com.herfy.utils.helper.launchWithTimber
import com.herfy.utils.helper.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapNotNull
@AndroidEntryPoint
class EnterNewPhoneNumberFragment: BaseBottomSheetDialog<FragmentEnterNewPhoneNumberBinding>() {

    private val viewModel: EnterNewPhoneNumberViewModel by viewModels()
    override fun getLayoutId(): Int = R.layout.fragment_enter_new_phone_number

    override fun isFullScreen(): Boolean = true

    override fun isDraggable(): Boolean = false

    override fun isDismissible(): Boolean = false
    override fun shouldExpandOnOpen(): Boolean = true

    private var adapter: PhoneNumberAdapter by autoCleared()

    override fun setupViews(binding: FragmentEnterNewPhoneNumberBinding) {
        viewLifecycleScope.launchWithTimber {
            binding.mobileNumber.debounceTextChanges()
                .collectLatest {

                }
        }

        binding.btnConfirm.ninjaTap(viewLifecycleScope) {
            val phoneNumber = "${StringUtil.toEnglish(
                (binding.phoneNumberPrefix.selectedItem as CountriesUIModel).dialCode)} + ${binding.mobileNumber.text.toString().trim()}"
            viewModel.updatePhoneNumber(phoneNumber)
        }
        binding.phoneNumberPrefix.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                viewModel.setSelectedCountry(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle case where nothing is selected
            }
        }
        binding.icBack
            .ninjaTap(viewLifecycleScope) {
                dismiss()
            }
        binding.setUpTranslations()
        setUpVmObservers()
    }

    private fun FragmentEnterNewPhoneNumberBinding.setUpTranslations() {
        txtEnterNewPhoneDescription.text = CacheUtil.getInstance().getTranslations(Text.UPDATE_NEW_PHONE_DESCRIPTION,"This phone number will only be used for this delivery and not be saved.")
        btnConfirm.text = CacheUtil.getInstance().getTranslations(Text.CONFIRM_CAPS,"Confirm")
        txtNewPhone.text = CacheUtil.getInstance().getTranslations(Text.ENTER_NEW_PHONE_NUMBER,"Enter New Phone Number")
        txtMobileNumberTitle.text = CacheUtil.getInstance().getTranslations(Text.MOBILE_NUMBER,"Mobile Number")
    }

    private fun setUpVmObservers() {
        viewLifecycleScope.launchWithTimber {
            viewModel
                .distinctCountriesState
                .flowWithLifecycle(lifecycle = lifecycle,Lifecycle.State.CREATED)
                .filterNotNull()
                .mapNotNull {
                    it.map { countries ->
                        val prefix = countries.dialCode
                        CountriesUIModel(
                            StringUtil.fetchEmojiFlag(countries.code).orEmpty(),
                            StringUtil.localizeNumberForLocale(if (prefix.contains("+")) prefix else "+$prefix")
                                .orEmpty()
                        )
                    }
                }.collectLatest {
                    binding.setUpAdapter(it)
                }
        }
        viewLifecycleScope.launchWithTimber {
            repeatOnLifecycle(state = Lifecycle.State.CREATED) {
                viewModel
                    .currentSelectedItem
                    .collectLatest {
                        adapter.setItemToHide(it)
                    }
            }
        }
        viewLifecycleScope.launchWithTimber {
            viewModel.state
                .collectLatest { state->
                    handleState(state)
                }
        }
    }

    private fun handleState(state: EnterNewPhoneNumberState) {
        when(state) {
            is EnterNewPhoneNumberState.Error -> {
            DialogUtils.showConfirmDialog(requireContext(), message = state.message,)
            }
            is EnterNewPhoneNumberState.IsLoading -> {
                DialogUtils.showProgressDialog(requireContext(), isLoading = state.isLoading)
            }
            is EnterNewPhoneNumberState.Success ->  {
                // The TODO is commented for now. We are waiting for the designer to provide the Snackbar UI for when the user updates a phone number.
//                SnackBarUtils.showGenericSuccessSnackBar(binding.root,state.phoneNumber)
                dismiss()
            }
        }
    }

    private fun FragmentEnterNewPhoneNumberBinding.setUpAdapter(allowedCountries: List<CountriesUIModel>) {
        adapter = PhoneNumberAdapter(
            requireContext(), resource = R.layout.item_spinner_dial_code,
            objects = allowedCountries,
            textViewResource = R.id.txtPrefix,
        )
        phoneNumberPrefix.adapter = adapter
        adapter.notifyDataSetChanged()

    }

    companion object {
        const val TAG = "com.herfy.phonenumber.EnterNewPhoneNumberFragment"
        fun newInstance(): EnterNewPhoneNumberFragment {
            return EnterNewPhoneNumberFragment()
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }
}