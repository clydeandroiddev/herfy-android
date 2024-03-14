package com.herfy.account.account

import android.os.Bundle
import android.view.View
import com.herfy.profile.databinding.DialogDeleteAccountNewBinding
import com.herfy.utils.base.mvvm.BaseDialogFragmentV2
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeleteAccountDialogFragment : BaseDialogFragmentV2<DialogDeleteAccountNewBinding>() {

    private lateinit var onAction: OnAction

    companion object {
        fun newInstance(onAction: OnAction): DeleteAccountDialogFragment {
            val fragment = DeleteAccountDialogFragment()
            fragment.onAction = onAction
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(binding)
    }

    override fun getLayoutId(): Int = com.herfy.profile.R.layout.dialog_delete_account_new

    override fun setupViews(view: DialogDeleteAccountNewBinding) {
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
            Text.ARE_YOU_SURE_YOU_WANT_TO_DELETE,
            "Are you sure you want to delete your account?"
        )
        binding.txtClearDescription.text = CacheUtil.getInstance()
            .getTranslations(Text.THIS_ACTION_CAN_NOT_REVERSE, "This action can not be reversed.")
        binding.deleteButton.text = CacheUtil.getInstance()
            .getTranslations(Text.DELETE_LABEL, "Delete")
        binding.cancelButton.text = CacheUtil.getInstance()
            .getTranslations(Text.CANCEL_LABEL, "Cancel")
        binding.txtClearDescription.text = CacheUtil.getInstance()
            .getTranslations(Text.THIS_ACTION_CAN_NOT_REVERSE, "This action can not be reversed.")
    }

    override fun shouldBackgroundTransparent(): Boolean = false

    override fun shouldHeightMatchParent() = false

    override fun shouldDialogHaveRedius(): Boolean = true


    interface OnAction {
        fun onClose()
        fun onDelete()
    }

}
