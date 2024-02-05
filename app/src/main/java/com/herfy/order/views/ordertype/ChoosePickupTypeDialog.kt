package com.herfy.order.views.ordertype

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.herfy.ordertype.R
import com.herfy.ordertype.databinding.DialogOrderTypeSelectionBinding
import com.herfy.util.CacheUtil

class ChoosePickupTypeDialog : BottomSheetDialogFragment() {

    private var onSubmit: OnSubmit? = null

    private lateinit var binding: DialogOrderTypeSelectionBinding

    interface OnSubmit {
        fun isPickup()
        fun isCurbside()
        fun exit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogOrderTypeSelectionBinding.inflate(layoutInflater)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
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
        binding.soloType.text =
            CacheUtil.getInstance().getTranslations("choose_pickup_type", "Choose Pickup Type")
        binding.storePickup.text =
            CacheUtil.getInstance().getTranslations("store_pickup_label", "Store Pickup")
        binding.descriptionPickup.text = CacheUtil.getInstance()
            .getTranslations("description_pickup", "Pickup your order from the store.")
        binding.curbside.text = CacheUtil.getInstance().getTranslations("curbside", "Curbside")
        binding.descriptionCurbside.text = CacheUtil.getInstance()
            .getTranslations("description_curbside", "Your order will be served in your car.")

        binding.choosePickup.setOnClickListener {
            onSubmit?.isPickup()
        }

        binding.chooseCurbside.setOnClickListener {
            onSubmit?.isCurbside()
        }

        binding.back.setOnClickListener {
            onSubmit?.exit()
        }
    }

    companion object {
        fun newInstance(listener: OnSubmit): ChoosePickupTypeDialog {
            val choosePickupTypeDialogFragment = ChoosePickupTypeDialog()
            choosePickupTypeDialogFragment.onSubmit = listener
            return choosePickupTypeDialogFragment
        }
    }
}
