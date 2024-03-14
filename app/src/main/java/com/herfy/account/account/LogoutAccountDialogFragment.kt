package com.herfy.account.account

import android.os.Bundle
import android.view.View
import com.herfy.profile.databinding.DialogLogoutAccountBinding
import com.herfy.utils.base.mvvm.BaseDialogFragmentV2
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LogoutAccountDialogFragment : BaseDialogFragmentV2<DialogLogoutAccountBinding>() {

    private lateinit var onAction: OnLogoutAction

    companion object {
        fun newInstance(onAction: OnLogoutAction): LogoutAccountDialogFragment {
            val fragment = LogoutAccountDialogFragment()
            fragment.onAction = onAction
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(binding)
    }

    override fun getLayoutId(): Int = com.herfy.profile.R.layout.dialog_logout_account

    override fun setupViews(view: DialogLogoutAccountBinding) {
        binding.deleteButton.setOnClickListener {
            onAction.onLogout()
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            onAction.onLogout()
            dismiss()
        }

        setupTranslations()
    }

    fun setupTranslations() {
        binding.txtDeleteLabel.text = CacheUtil.getInstance().getTranslations(
            Text.ARE_YOU_SURE_YOU_WANT_TO_LOGOUT,
            "Are you sure you want to log out?"
        )
        binding.txtClearDescription.text = CacheUtil.getInstance()
            .getTranslations(
                Text.YOU_CAN_ALWAYS_LOGIN_ANYTIME,
                "You can always log in again anytime."
            )
        binding.deleteButton.text = CacheUtil.getInstance()
            .getTranslations(Text.LOGOUT, "Logout")
        binding.cancelButton.text = CacheUtil.getInstance()
            .getTranslations(Text.CANCEL_LABEL, "Cancel")
    }

    override fun shouldBackgroundTransparent(): Boolean = false

    override fun shouldHeightMatchParent() = false

    override fun shouldDialogHaveRedius(): Boolean = true


    interface OnLogoutAction {
        fun onLogoutClose()
        fun onLogout()
    }

}
