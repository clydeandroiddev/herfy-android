package com.herfy.favourites.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.herfy.favourites.R
import com.herfy.favourites.databinding.DialogClearBasketBinding
import com.herfy.utils.base.mvvm.BaseDialogFragmentV2
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DialogUtils
import com.herfy.utils.helper.SnackBarUtils
import com.herfy.utils.helper.launchWithTimber
import com.herfy.utils.helper.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class DialogClearCurrentBasketFragment: BaseDialogFragmentV2<DialogClearBasketBinding>() {

    private val viewModel by viewModels<DialogClearCurrentBasketViewModel>()

    companion object {
        const val TAG = "com.herfy.favourites.ui.DialogClearCurrentBasketFragment"
        const val CURRENT_CART_KEY = "CURRENT_CART_KEY"
        const val CART_TOTAL_KEY = "CART_TOTAL_KEY"
        const val SELECTED_ORDER_TYPE = "SELECTED_ORDER_TYPE"

        fun newInstance(bundle: Bundle): DialogClearCurrentBasketFragment {
            val fragment = DialogClearCurrentBasketFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(binding)
        arguments
            ?.getString(SELECTED_ORDER_TYPE)?.let { selectedOrderType ->
                viewModel.setSelectedOrderType(selectedOrderType)
            }
        setUpVmObservers()
    }


    private fun setUpVmObservers() {
        viewLifecycleScope.launchWithTimber {
        viewModel
            .state
            .flowWithLifecycle(lifecycle = lifecycle, Lifecycle.State.CREATED)
            .collectLatest { state->
                handleState(state)
            }
        }
    }

    private fun handleState(state: DialogClearBasketState) {
        when(state) {
            is DialogClearBasketState.Error ->  {
                SnackBarUtils
                    .showGenericSuccessSnackBar(binding.root,state.message
                    )

            }
            is DialogClearBasketState.IsLoading -> {
                DialogUtils.showProgressDialog(requireContext(), isLoading = state.isLoading)
            }
            is DialogClearBasketState.Success -> {
                val bundle = Bundle().apply {
                    putInt(CART_TOTAL_KEY, state.currentCart.size)
                }
                setFragmentResult(CURRENT_CART_KEY,bundle)
                dismiss()
            }
        }
    }

    override fun getLayoutId(): Int = R.layout.dialog_clear_basket

    override fun setupViews(view: DialogClearBasketBinding) {
        binding.btnKeepCurrentBasket
            .ninjaTap(viewLifecycleScope) {
                dismiss()
            }
        binding.btnClearBasket
            .ninjaTap(viewLifecycleScope) {
              viewModel.clearCart()
            }
        binding.setupTranslations()
    }

    fun DialogClearBasketBinding.setupTranslations() {
        val cacheUtil = CacheUtil.getInstance()
        txtClearYourBasket.text =
            cacheUtil.getTranslations(Text.CLEAR_YOUR_CURRENT_BASKET, "Clear your current basket")
        btnClearBasket.text = cacheUtil.getTranslations(Text.CLEAR_BASKET, "Clear basket")
        btnKeepCurrentBasket.text =
            cacheUtil.getTranslations(Text.KEEP_CURRENT_BASKET, "Keep Current Basket")
        txtClearDescription.text = cacheUtil.getTranslations(
            Text.CLEAR_YOUR_BASKET_DESCRIPTION,
            "You already have a items in your basket with another order type. Those will be removed to start a new one."
        )
    }


    override fun shouldBackgroundTransparent(): Boolean = false

    override fun shouldHeightMatchParent() = false

    override fun shouldDialogHaveRedius(): Boolean = true
}
