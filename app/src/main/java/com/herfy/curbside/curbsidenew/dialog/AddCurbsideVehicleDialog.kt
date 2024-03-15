package com.herfy.curbside.curbsidenew.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.herfy.base.BaseActivity
import com.herfy.curbside.CurbsideContract
import com.herfy.curbside.CurbsidePresenter
import com.herfy.order.views.ordertype.ChoosePickupTypeDialog
import com.herfy.ordertype.R
import com.herfy.ordertype.databinding.AddCurbsideVehicleDialogBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarColor
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarColorAttributes
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarMaker
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarMakerAttributes
import com.skylinedynamics.solosdk.api.models.objects.curbside.CustomerCar
import io.shipbook.shipbooksdk.Log

class AddCurbsideVehicleDialog : DialogFragment(), CurbsideContract.View {

    private lateinit var curbsidePresenter: CurbsideContract.Presenter

    private lateinit var vehicleBrandDialog: VehicleBrandDialog

    private lateinit var vehicleColorDialog: VehicleColorDialog

    private var onSubmit: OnSubmit? = null

    private var isEmpty = false

    private var carMaker: CarMaker? = null

    private var isEdit: Boolean = false

    private var carColor: CarColor? = null

    private var customerCar: CustomerCar? = null

    private lateinit var binding: AddCurbsideVehicleDialogBinding

    interface OnSubmit {
        fun onSuccess()

        fun error(text: String)
        fun exit(isEmpty: Boolean)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AddCurbsideVehicleDialogBinding.inflate(layoutInflater)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme_White)
        curbsidePresenter = CurbsidePresenter(this)
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
        isEdit = customerCar != null
        curbsidePresenter.start()
        setupCar()
    }

    private fun setupCar() {
        if (isEdit) {
            customerCar?.let {
                binding.plateNumberEdit.setText(it.attributes.plateNumber)
                binding.plateNumberEdit.setSelection(binding.plateNumberEdit.length())
                binding.confirmContainer.isVisible =
                    binding.plateNumberEdit.text.toString().isNotEmpty()
                try {
                    val carMake = CarMaker()
                    val carAtt = CarMakerAttributes()
                    carMake.id = it.attributes.carMakerId
                    carAtt.imageUri = it.attributes.carMakerImageUri
                    carAtt.name = it.attributes.carMakerName
                    carMake.attributes = carAtt
                    carMaker = carMake
                    val cColor = CarColor()
                    val colorAtt = CarColorAttributes()
                    colorAtt.color = it.attributes.color
                    colorAtt.name = it.attributes.colorName
                    colorAtt.label = it.attributes.colorName.toLowerCase()
                    cColor.attributes = colorAtt
                    carColor = cColor
                    setupColor()
                    setupBrand()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

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

    override fun setupViews() {
        binding.apply {

            back.setOnClickListener {
                onSubmit?.exit(isEmpty)
                dismiss()
            }
            continueButton.setOnClickListener {
                saveAndContinue()
            }

            chooseCurbsideBrandColor.setOnClickListener {
                showVehicleColor()
            }

            chooseCurbsideBrand.setOnClickListener {
                showVehicleBrand()
            }

            binding.plateNumberEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    charSequence: CharSequence?, start: Int, before: Int, count: Int
                ) {
                }

                override fun afterTextChanged(editable: Editable?) {
                    confirmContainer.isVisible = plateNumberEdit.text.toString().isNotEmpty()
                }
            })

            plateNumberEdit.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (confirmContainer.isVisible) {
                        continueButton.performClick()
                    }
                    true
                } else false
            }

            continueButton.setOnClickListener {
                saveAndContinue()
            }

            binding.continueButton.isVisible = true
        }
    }

    private fun saveAndContinue() {
        if (AuthUtil.getInstance().isSignedIn) {
            val user = AuthUtil.getInstance().customer
            activity?.let {
                val baseActivity = it as BaseActivity
                BaseActivity.hideKeyboard(baseActivity)
                baseActivity.showLoadingDialog()
            }
            var color = ""
            carColor?.let {
                color = it.attributes.color
            }

            var carMakerId = "0"
            var carMakerName = ""
            carMaker?.let {
                carMakerId = it.id
                carMakerName = it.attributes.name
            }

            if (isEdit) {
                val editBody = JsonObject()
                carColor?.let {
                    editBody.addProperty("color", carColor?.attributes?.color)
                }
                carMaker?.let {
                    editBody.addProperty(
                        "car-maker-id", carMaker?.id?.toInt()
                    )
                    if (!it.attributes.name.isNullOrEmpty()) {
                        editBody.addProperty(
                            "car-maker-name", it.attributes.name
                        )
                    }
                }
                editBody.addProperty("plate-number", binding.plateNumberEdit.text.toString())
                curbsidePresenter.editNewCar(customerCar?.id, editBody, -1)
            } else {
                curbsidePresenter.addNewCar(
                    color,
                    carMakerId.toInt(),
                    carMakerName,
                    binding.plateNumberEdit.text.toString().trim(),
                    user.attributes.mobile.toString().trim().replace("+", "")
                )
            }
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.apply {
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
            plateNumberEdit.hint =
                CacheUtil.getInstance().getTranslations(Text.PLATE_NUMBER_HINT, "e.g. KLK98898")
        }
    }

    override fun showCarColors(colors: MutableList<CarColor>?) {

    }

    override fun showCarMakers(makers: MutableList<CarMaker>?) {

    }

    override fun showError(error: String?) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
            baseActivity.showAlertDialog("", error)
        }
    }

    override fun showCustomerCars(cars: MutableList<CustomerCar>?) {

    }

    override fun successAdd(car: CustomerCar?) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
        }
        if (!isEdit) {
            CacheUtil.getInstance().curbsideCar = car
        }
        onSubmit?.onSuccess()
    }

    override fun successEdit(car: CustomerCar?, position: Int) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
        }
        val car1 = CacheUtil.getInstance().curbsideCar
        if (car1 != null) {
            if (car1.id == car?.id) {
                CacheUtil.getInstance().curbsideCar = car1
            }
        }
        onSubmit?.onSuccess()
    }

    override fun successDelete(position: Int) {

    }

    companion object {
        fun newInstance(
            customerCar: CustomerCar?, isEmpty: Boolean, listener: OnSubmit
        ): AddCurbsideVehicleDialog {
            val addressDialogFragment = AddCurbsideVehicleDialog()
            addressDialogFragment.onSubmit = listener
            addressDialogFragment.customerCar = customerCar
            addressDialogFragment.isEmpty = isEmpty
            return addressDialogFragment
        }
    }

    private fun setupBrand() {
        carMaker?.let {
            binding.curbsideBrandName.text = it.attributes.name
            Glide.with(binding.curbsideBrand.context).load(it.attributes.imageUri)
                .apply(RequestOptions().override(80, 80)).skipMemoryCache(false).diskCacheStrategy(
                    DiskCacheStrategy.ALL
                ).apply(RequestOptions.centerInsideTransform())
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any,
                        target: Target<Drawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any,
                        target: Target<Drawable?>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                }).into(binding.curbsideBrand)
        }
    }

    private fun setupColor() {
        carColor?.let {
            binding.apply {
                curbsideColor.isVisible = false
                curbsideColorSelected.isVisible = true
                curbsideColorName.text = it.attributes.name
                if (it.attributes.label.toLowerCase() != "white") {
                    curbsideColorSelected.strokeColor =
                        curbsideColor.context.resources.getColor(android.R.color.transparent)
                } else {
                    curbsideColorSelected.strokeColor =
                        curbsideColor.context.resources.getColor(com.herfy.commonstyles.R.color.color_stroke)
                }
                curbsideColorSelected.setCardBackgroundColor(
                    Color.parseColor(
                        "#" + it.attributes.color.replace(
                            "#", ""
                        )
                    )
                )
            }
        }
    }

    private fun showVehicleBrand() {
        dismissVehicleBrandDialog()
        vehicleBrandDialog =
            VehicleBrandDialog.newInstance(carMaker, object : VehicleBrandDialog.OnSubmit {
                override fun selectedCar(maker: CarMaker) {
                    carMaker = maker
                    setupBrand()
                    dismissVehicleBrandDialog()
                }

                override fun exit() {
                    dismissVehicleBrandDialog()
                }

            })
        vehicleBrandDialog.show(
            requireActivity().supportFragmentManager, VehicleBrandDialog::class.java.simpleName
        )
    }

    private fun showVehicleColor() {
        dismissVehicleColorDialog()
        vehicleColorDialog =
            VehicleColorDialog.newInstance(carColor, object : VehicleColorDialog.OnSubmit {
                override fun selectedColor(color: CarColor) {
                    carColor = color
                    setupColor()
                    dismissVehicleColorDialog()
                }

                override fun exit() {
                    dismissVehicleColorDialog()
                }

            })
        vehicleColorDialog.show(
            requireActivity().supportFragmentManager, VehicleColorDialog::class.java.simpleName
        )
    }

    private fun dismissVehicleBrandDialog() {
        try {
            vehicleBrandDialog.dismiss()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun dismissVehicleColorDialog() {
        try {
            vehicleColorDialog.dismiss()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}
