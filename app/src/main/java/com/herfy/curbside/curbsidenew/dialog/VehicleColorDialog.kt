package com.herfy.curbside.curbsidenew.dialog

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.herfy.curbside.CurbsideContract
import com.herfy.curbside.CurbsidePresenter
import com.herfy.curbside.curbsidenew.adapter.CurbsideColorAdapter
import com.herfy.ext.getDialogCurbsideHeight
import com.herfy.ordertype.R
import com.herfy.ordertype.databinding.VehicleColorDialogBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarColor
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarMaker
import com.skylinedynamics.solosdk.api.models.objects.curbside.CustomerCar

class VehicleColorDialog : BottomSheetDialogFragment(), CurbsideContract.View {

    private lateinit var curbsidePresenter: CurbsideContract.Presenter

    private lateinit var curbsideVehicleAdapter: CurbsideColorAdapter

    private var onSubmit: OnSubmit? = null

    private var carColor: CarColor? = null

    private lateinit var binding: VehicleColorDialogBinding

    private var brandList = mutableListOf<CarColor>()

    interface OnSubmit {
        fun selectedColor(color: CarColor)
        fun exit()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isDraggable = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = VehicleColorDialogBinding.inflate(layoutInflater)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
        curbsidePresenter = CurbsidePresenter(this)
        curbsidePresenter.start()
        brandList = CacheUtil.getInstance().colors
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDialog()
        binding.frameLayout.layoutParams.height = getDialogCurbsideHeight(requireContext())
        if (brandList.isEmpty()) {
            curbsidePresenter.getColorsList(true)
        } else {
            showCarColors(brandList)
        }
    }

    private fun setupDialog() {
        val bottomSheet = (requireView().parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
        bottomSheet.elevation = 0f
    }

    override fun setPresenter(presenter: CurbsideContract.Presenter) {
        curbsidePresenter = presenter
    }

    override fun logEvent(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {

    }

    override fun setCartExpiry() {

    }

    private fun enableButton() {
        binding.apply {
            continueButton.alpha = 1f
            continueButton.setOnClickListener {
                carColor?.let {
                    onSubmit?.selectedColor(it)
                }
            }
        }
    }

    override fun setupViews() {
        binding.apply {

            back.setOnClickListener {
                onSubmit?.exit()
            }

            continueButton.alpha = 0.5f
            continueButton.setOnClickListener(null)

            var carId = ""
            carColor?.let {
                enableButton()
                carId = it.id
            }

            curbsideVehicleAdapter = CurbsideColorAdapter(
                ArrayList(), object : CurbsideColorAdapter.CurbsideListener {
                    override fun onSelectedColor(color: CarColor, position: Int) {
                        carColor = color
                        curbsideVehicleAdapter.updateSelectedId(color.id)
                        enableButton()
                    }
                }, carId
            )
            binding.vehicleColorList.adapter = curbsideVehicleAdapter
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.apply {
            title.text =
                CacheUtil.getInstance().getTranslations(Text.VEHICLE_COLOR, "Vehicle Color")

            continueButton.text =
                CacheUtil.getInstance().getTranslations(Text.CONFIRM_LABEL, "Confirm")
        }
    }

    override fun showCarColors(colors: MutableList<CarColor>?) {
        brandList.clear()
        brandList = CacheUtil.getInstance().colors
        curbsideVehicleAdapter.updateList(ArrayList(brandList))
    }

    override fun showCarMakers(makers: MutableList<CarMaker>?) {

    }

    override fun showError(error: String?) {

    }

    override fun showCustomerCars(cars: MutableList<CustomerCar>?) {

    }

    override fun successAdd(car: CustomerCar?) {

    }

    override fun successEdit(car: CustomerCar?, position: Int) {

    }

    override fun successDelete(position: Int) {

    }

    companion object {
        fun newInstance(carMaker: CarColor?, listener: OnSubmit): VehicleColorDialog {
            val choosePickupTypeDialogFragment = VehicleColorDialog()
            choosePickupTypeDialogFragment.onSubmit = listener
            choosePickupTypeDialogFragment.carColor = carMaker
            return choosePickupTypeDialogFragment
        }
    }
}
