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
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.*

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
        binding.soloType.text = CacheUtil.getInstance()
            .getTranslations(Text.CHOOSE_PICKUP_METHOD, "Choose Pickup Method")
        binding.storePickup.text =
            CacheUtil.getInstance().getTranslations(Text.WALK_IN_PICKUP, "Walk In Pickup")
        binding.descriptionPickup.text = CacheUtil.getInstance()
            .getTranslations(Text.PICKUP_DESCRIPTION, "Pickup your order inside the restaurant")
        binding.curbside.text =
            CacheUtil.getInstance().getTranslations(Text.CURBSIDE_PICKUP_LABEL, "Curbside pickup")
        binding.descriptionCurbside.text = CacheUtil.getInstance().getTranslations(
                Text.CURBSIDE_DESCRIPTION,
                "Park at the restaurant and your order will be served in your car"
            )
        binding.driveThru.text =
            CacheUtil.getInstance().getTranslations(Text.DRIVE_THRU, "Drive Thru")
        binding.descriptionDriveThru.text = CacheUtil.getInstance().getTranslations(
                Text.DRIVE_THRU_DESCRIPTION, "Pick up your order from the drive thru."
            )

        binding.choosePickup.setOnClickListener {
            onSubmit?.isPickup()
        }

        binding.chooseDriveThru.setOnClickListener {
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
