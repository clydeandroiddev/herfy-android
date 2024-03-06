package com.herfy.favourites.ui

import android.os.Bundle
import android.view.View
import com.herfy.common.base.BaseBottomSheetDialog
import com.herfy.favourites.R
import com.herfy.favourites.databinding.DialogClearBasketBinding
import com.herfy.utils.base.mvvm.BaseDialogFragmentV2
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DialogClearCurrentBasketFragment: BaseDialogFragmentV2<DialogClearBasketBinding>() {


    companion object {
        const val TAG = "com.herfy.favourites.ui.DialogClearCurrentBasketFragment"
        fun newInstance(): DialogClearCurrentBasketFragment {
            return DialogClearCurrentBasketFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(binding)
    }

    override fun getLayoutId(): Int = R.layout.dialog_clear_basket

    override fun setupViews(view: DialogClearBasketBinding) {
        binding.btnKeepCurrentBasket
            .ninjaTap(viewLifecycleScope) {
                dismiss()
            }
        binding.btnClearBasket
            .ninjaTap(viewLifecycleScope) {
                // todo clear the basket
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
