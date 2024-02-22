package com.herfy.curbside.curbsidenew

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.herfy.curbside.CurbsideContract
import com.herfy.curbside.CurbsidePresenter
import com.herfy.curbside.curbsidenew.adapter.CurbsideBrandAdapter
import com.herfy.ordertype.R
import com.herfy.ordertype.databinding.VehicleBrandDialogBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarMaker
import com.skylinedynamics.solosdk.api.models.objects.curbside.CustomerCar

class VehicleBrandDialog : BottomSheetDialogFragment(), CurbsideContract.View {

    private lateinit var curbsidePresenter: CurbsideContract.Presenter

    private lateinit var curbsideVehicleAdapter: CurbsideBrandAdapter

    private var onSubmit: OnSubmit? = null

    private var carMaker: CarMaker? = null

    private lateinit var binding: VehicleBrandDialogBinding

    private var brandList = mutableListOf<CarMaker>()

    interface OnSubmit {
        fun selectedCar(type: CarMaker)
        fun exit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = VehicleBrandDialogBinding.inflate(layoutInflater)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
        curbsidePresenter = CurbsidePresenter(this)
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
        curbsidePresenter.start()
        brandList = CacheUtil.getInstance().brands
        if (brandList.isEmpty()) {
            curbsidePresenter.getMakersList(true)
        }
    }

    override fun setPresenter(presenter: CurbsideContract.Presenter?) {

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
                carMaker?.let {
                    onSubmit?.selectedCar(it)
                }
            }
        }
    }

    override fun setupViews() {
        binding.apply {
            back.setOnClickListener {
                onSubmit?.exit()
                dismiss()
            }
            continueButton.alpha = 0.5f
            continueButton.setOnClickListener(null)
            var carId = ""
            carMaker?.let {
                enableButton()
                carId = it.id
            }
            curbsideVehicleAdapter = CurbsideBrandAdapter(
                ArrayList(), object : CurbsideBrandAdapter.CurbsideListener {
                    override fun onSelectedCar(car: CarMaker, position: Int) {
                        if (!AuthUtil.getInstance().isSignedIn) {
                            carMaker = car
                            curbsideVehicleAdapter.updateSelectedId(car.id)
                            enableButton()
                        }
                    }
                }, carId
            )
            binding.vehicleBrandList.adapter = curbsideVehicleAdapter
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.apply {
            title.text =
                CacheUtil.getInstance().getTranslations(Text.VEHICLE_BRAND, "Vehicle Brand")

            continueButton.text =
                CacheUtil.getInstance().getTranslations(Text.CONFIRM_LABEL, "Confirm")
        }
    }

    override fun showCarColors(colors: MutableList<String>?) {

    }

    override fun showCarMakers(makers: MutableList<CarMaker>) {
        brandList.clear()
        brandList.addAll(makers)
        curbsideVehicleAdapter.updateList(ArrayList(brandList))
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
        fun newInstance(carMaker: CarMaker?, listener: OnSubmit): VehicleBrandDialog {
            val choosePickupTypeDialogFragment = VehicleBrandDialog()
            choosePickupTypeDialogFragment.onSubmit = listener
            choosePickupTypeDialogFragment.carMaker = carMaker
            return choosePickupTypeDialogFragment
        }
    }
}
