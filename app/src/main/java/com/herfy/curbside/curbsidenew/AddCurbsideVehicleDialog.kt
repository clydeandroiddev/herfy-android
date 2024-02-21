package com.herfy.curbside.curbsidenew

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.herfy.ordertype.R
import com.herfy.ordertype.databinding.AddCurbsideVehicleDialogBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarMaker

class AddCurbsideVehicleDialog : DialogFragment() {

    private var onSubmit: OnSubmit? = null

    private var colorSelected = "";

    private var carMaker: CarMaker? = null

    private lateinit var binding: AddCurbsideVehicleDialogBinding

    interface OnSubmit {
        fun onSuccess(
            colorSelected: String, carMaker: CarMaker?
        )

        fun error(text: String)
        fun exit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AddCurbsideVehicleDialogBinding.inflate(layoutInflater)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme_White)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.setLayout(width, height)
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
            val typedValue = TypedValue()
            dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            dialog.window!!.statusBarColor = typedValue.data
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        binding.apply {

            back.setOnClickListener {
                onSubmit?.exit()
                dismiss()
            }
            continueButton.setOnClickListener {
                onSubmit?.onSuccess(colorSelected, carMaker)
            }

            continueButton.text =
                CacheUtil.getInstance().getTranslations(Text.SAVE_AND_CONTINUE, "Save & Continue")
            curbsideLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.ADD_YOUR_CURBSIDE_VEHICLE, "Add your curbside vehicle")
            curbsideColorName.text =
                CacheUtil.getInstance().getTranslations(Text.VEHICLE_COLOR, "Vehicle Color")
            curbsideBrandName.text =
                CacheUtil.getInstance().getTranslations(Text.VEHICLE_BRAND, "Vehicle Brand")
            optionalColor.text =
                CacheUtil.getInstance().getTranslations(Text.OPTIONAL_LABEL, "(Optional)")
            optionalBrand.text =
                CacheUtil.getInstance().getTranslations(Text.OPTIONAL_LABEL, "(Optional)")
            licensePlateNumberLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.LICENSE_PLATE_NUMBER, "License Plate Number")
            plateNumberEdit.hint = CacheUtil.getInstance()
                .getTranslations(Text.PLATE_NUMBER_HINT, "e.g. KLK98898")
        }
    }

    companion object {
        fun newInstance(listener: OnSubmit): AddCurbsideVehicleDialog {
            val addressDialogFragment = AddCurbsideVehicleDialog()
            addressDialogFragment.onSubmit = listener
            return addressDialogFragment
        }
    }
}
