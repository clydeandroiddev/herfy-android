package com.herfy.order.ordertrackernew

import android.os.Bundle
import android.view.View
import com.herfy.ordertracker.databinding.DialogCancelOrderTrackerBinding
import com.herfy.utils.base.mvvm.BaseDialogFragmentV2
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CancelOrderDialogFragment : BaseDialogFragmentV2<DialogCancelOrderTrackerBinding>() {

    private lateinit var onAction: OnAction

    companion object {
        fun newInstance(onAction: OnAction): CancelOrderDialogFragment {
            val fragment = CancelOrderDialogFragment()
            fragment.onAction = onAction
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(binding)
    }

    override fun getLayoutId(): Int = com.herfy.ordertracker.R.layout.dialog_cancel_order_tracker

    override fun setupViews(view: DialogCancelOrderTrackerBinding) {
        binding.deleteButton.setOnClickListener {
            onAction.onDelete()
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            onAction.onClose()
            dismiss()
        }

        setupTranslations()
    }

    fun setupTranslations() {
        binding.txtDeleteLabel.text = CacheUtil.getInstance().getTranslations(
            Text.CANCEL_YOUR_ORDER_LABEL,
            "Cancel your order?"
        )
        binding.txtClearDescription.text = CacheUtil.getInstance()
            .getTranslations(Text.CANCEL_YOUR_ORDER_MESSAGE, "Are you sure you want to cancel your order?")
        binding.deleteButton.text = CacheUtil.getInstance()
            .getTranslations(Text.YES_CANCEL, "Yes, cancel")
        binding.cancelButton.text = CacheUtil.getInstance()
            .getTranslations(Text.NO_LABEL, "No")
    }

    override fun shouldBackgroundTransparent(): Boolean = false

    override fun shouldHeightMatchParent() = false

    override fun shouldDialogHaveRedius(): Boolean = true


    interface OnAction {
        fun onClose()
        fun onDelete()
    }

}
