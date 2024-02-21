package com.herfy.order.views.ordertype.addaddress

import android.app.Dialog
import android.location.Address
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.herfy.ordertype.R
import com.herfy.ordertype.databinding.DialogAddAddressBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import io.shipbook.shipbooksdk.Log

class AddAddressDialog : BottomSheetDialogFragment() {

    private var onSubmit: OnSubmit? = null

    private var address: Address? = null

    private lateinit var binding: DialogAddAddressBinding

    interface OnSubmit {
        fun onSuccess()
        fun exit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogAddAddressBinding.inflate(layoutInflater)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme_White)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        binding.apply {
            saveAddressLabel.text =
                CacheUtil.getInstance().getTranslations(Text.SAVE_ADDRESS, "Save Address")
            continueButton.text =
                CacheUtil.getInstance().getTranslations(Text.SAVE_AND_CONTINUE, "Save & Continue")
            back.setOnClickListener {
                onSubmit?.exit()
                dismiss()
            }
            address?.let {
                Log.e("ADADADADADADAD", "ADADADADAD:" + it.featureName)
                Log.e("ADADADADADADAD", "ADADADADAD:" + it.getAddressLine(0))
                Log.e("ADADADADADADAD", "ADADADADAD:" + it.latitude)
                Log.e("ADADADADADADAD", "ADADADADAD:" + it.longitude)
            }

        }
    }

    companion object {
        fun newInstance(address: Address?, listener: OnSubmit): AddAddressDialog {
            val choosePickupTypeDialogFragment = AddAddressDialog()
            choosePickupTypeDialogFragment.onSubmit = listener
            choosePickupTypeDialogFragment.address = address
            return choosePickupTypeDialogFragment
        }
    }
}
