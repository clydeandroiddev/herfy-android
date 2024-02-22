package com.herfy.curbside.curbsidenew

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.herfy.curbside.CurbsideContract
import com.herfy.curbside.CurbsidePresenter
import com.herfy.curbside.curbsidenew.adapter.CurbsideBrandAdapter
import com.herfy.ext.getDialogCurbsideHeight
import com.herfy.ordertype.R
import com.herfy.ordertype.databinding.VehicleBrandDialogBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarMaker
import com.skylinedynamics.solosdk.api.models.objects.curbside.CustomerCar
import io.shipbook.shipbooksdk.Log

class VehicleBrandDialog : DialogFragment(), CurbsideContract.View {

    private lateinit var curbsidePresenter: CurbsideContract.Presenter

    private lateinit var curbsideVehicleAdapter: CurbsideBrandAdapter

    private var onSubmit: OnSubmit? = null

    private var carMaker: CarMaker? = null

    private lateinit var binding: VehicleBrandDialogBinding

    private var brandList = mutableListOf<CarMaker>()

    interface OnSubmit {
        fun selectedCar(maker: CarMaker)
        fun exit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = VehicleBrandDialogBinding.inflate(layoutInflater)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
        curbsidePresenter = CurbsidePresenter(this)
        curbsidePresenter.start()
        brandList = CacheUtil.getInstance().brands
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.frameLayout.layoutParams.height = getDialogCurbsideHeight(requireContext())
        Log.e("ADADADADADADAD", "ADADADADAD:" + brandList.size)
        if (brandList.isEmpty()) {
            curbsidePresenter.getMakersList(true)
        } else {
            showCarMakers(brandList)
        }
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
            Log.e("ADADADADADADAD", "ADADADADAD:" + brandList.size)
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
