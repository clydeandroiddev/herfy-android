package com.herfy.order.views.ordertype

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.herfy.ext.isVisible
import com.herfy.ordertype.R
import com.herfy.ordertype.databinding.DialogOrderTypeSelectionBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.*
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Store

class ChoosePickupTypeDialog : BottomSheetDialogFragment() {

    private var onSubmit: OnSubmit? = null

    private var store: Store? = null

    private lateinit var binding: DialogOrderTypeSelectionBinding

    interface OnSubmit {
        fun orderType(type: OrderType)
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
        binding.apply {
            soloType.text = CacheUtil.getInstance()
                .getTranslations(Text.CHOOSE_PICKUP_METHOD, "Choose Pickup Method")
            storePickup.text =
                CacheUtil.getInstance().getTranslations(Text.WALK_IN_PICKUP, "Walk In Pickup")
            descriptionPickup.text = CacheUtil.getInstance()
                .getTranslations(Text.PICKUP_DESCRIPTION, "Pickup your order inside the restaurant")
            curbside.text = CacheUtil.getInstance()
                .getTranslations(Text.CURBSIDE_PICKUP_LABEL, "Curbside pickup")
            descriptionCurbside.text = CacheUtil.getInstance().getTranslations(
                Text.CURBSIDE_DESCRIPTION,
                "Park at the restaurant and your order will be served in your car"
            )
            driveThru.text = CacheUtil.getInstance().getTranslations(Text.DRIVE_THRU, "Drive Thru")
            descriptionDriveThru.text = CacheUtil.getInstance().getTranslations(
                Text.DRIVE_THRU_DESCRIPTION, "Pick up your order from the drive thru."
            )

            choosePickup.setOnClickListener {
                onSubmit?.orderType(OrderType.EAT_IN)
            }

            chooseDriveThru.setOnClickListener {
                onSubmit?.orderType(OrderType.DRIVE_THRU)
            }

            chooseCurbside.setOnClickListener {
                onSubmit?.orderType(OrderType.CURBSIDE)
            }

            back.setOnClickListener {
                onSubmit?.exit()
            }
            store?.let {
                chooseDriveThruContainer.isVisible = it.attributes.isDriveThruEnabled
                curbsideContainer.isVisible = it.attributes.curbsideEnabled == 1
                pickupContainer.isVisible = it.attributes.pickupEnabled == 1
            }
        }
    }

    companion object {
        fun newInstance(store: Store?, listener: OnSubmit): ChoosePickupTypeDialog {
            val choosePickupTypeDialogFragment = ChoosePickupTypeDialog()
            choosePickupTypeDialogFragment.onSubmit = listener
            choosePickupTypeDialogFragment.store = store
            return choosePickupTypeDialogFragment
        }
    }
}
