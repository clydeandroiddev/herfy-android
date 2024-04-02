package com.herfy.curbside.curbsidenew.dialog

import android.os.Bundle
import android.view.View
import com.herfy.ordertype.databinding.DialogCurbsideOtherBinding
import com.herfy.utils.base.mvvm.BaseDialogFragmentV2
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil

class CurbsideOtherDialog : BaseDialogFragmentV2<DialogCurbsideOtherBinding>() {

    private lateinit var onAction: OnCurbsideOtherListener

    private lateinit var otherText: String

    companion object {
        fun newInstance(other: String, onAction: OnCurbsideOtherListener): CurbsideOtherDialog {
            val fragment = CurbsideOtherDialog()
            fragment.onAction = onAction
            fragment.otherText = other
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(binding)
    }

    override fun getLayoutId(): Int = com.herfy.ordertype.R.layout.dialog_curbside_other

    override fun setupViews(view: DialogCurbsideOtherBinding) {
        binding.confirmButton.setOnClickListener {
            if (binding.otherNameEdit.text.toString().trim().isNotEmpty()) {
                onAction.onOther(binding.otherNameEdit.text.toString())
                dismiss()
            }
        }

        binding.otherNameEdit.setText(otherText)
        binding.otherNameEdit.setSelection(binding.otherNameEdit.length())

        binding.cancelButton.setOnClickListener {
            onAction.onCancel()
            dismiss()
        }

        setupTranslations()
    }

    fun setupTranslations() {
        binding.enterCarBrandLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.ENTER_CAR_BRAND_NAME, "Enter the name of your car brand.")
        binding.confirmButton.text =
            CacheUtil.getInstance().getTranslations(Text.CONFIRM_LABEL, "Confirm")
        binding.cancelButton.text =
            CacheUtil.getInstance().getTranslations(Text.CANCEL_LABEL, "Cancel")
    }

    override fun shouldBackgroundTransparent(): Boolean = false

    override fun shouldHeightMatchParent() = false

    override fun shouldDialogHaveRedius(): Boolean = true


    interface OnCurbsideOtherListener {
        fun onCancel()
        fun onOther(other: String)
    }

}
