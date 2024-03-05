package com.herfy.favourites.ui

import com.herfy.common.base.BaseBottomSheetDialog
import com.herfy.favourites.R
import com.herfy.favourites.databinding.DialogClearBasketBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DialogClearCurrentBasketFragment: BaseBottomSheetDialog<DialogClearBasketBinding>() {


    override fun isFullScreen(): Boolean = false

    override fun isDismissible(): Boolean = false

    override fun isDraggable(): Boolean = false

    override fun shouldExpandOnOpen(): Boolean = true

    override fun getLayoutId(): Int = R.layout.dialog_clear_basket

    override fun setupViews(binding: DialogClearBasketBinding) {
        binding.btnKeepCurrentBasket
            .ninjaTap(viewLifecycleScope) {
                dismiss()
            }
        binding.setUpTranslations()
    }

    private fun DialogClearBasketBinding.setUpTranslations() {
        txtClearDescription.text = CacheUtil.getInstance().getTranslations(Text.CLEAR_YOUR_CURRENT_BASKET, "Clear your current basket")
       btnClearBasket.text = CacheUtil.getInstance().getTranslations(Text.CLEAR_BASKET,"Clear basket")
       btnKeepCurrentBasket.text = CacheUtil.getInstance().getTranslations(Text.KEEP_CURRENT_BASKET,"Keep Current Basket")
       txtClearDescription.text = CacheUtil.getInstance().getTranslations(Text.CLEAR_YOUR_BASKET_DESCRIPTION, "You already have a items in your basket with another order type. Those will be removed to start a new one.")
    }

    companion object {
        const val TAG = "com.herfy.favourites.ui.DialogClearCurrentBasketFragment"
        fun newInstance(): DialogClearCurrentBasketFragment {
            return com.herfy.favourites.ui.DialogClearCurrentBasketFragment()
        }
    }
}
