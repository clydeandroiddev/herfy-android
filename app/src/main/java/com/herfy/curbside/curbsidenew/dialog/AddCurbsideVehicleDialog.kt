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
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.herfy.curbside.CurbsideContract
import com.herfy.curbside.CurbsidePresenter
import com.herfy.order.views.ordertype.ChoosePickupTypeDialog
import com.herfy.ordertype.R
import com.herfy.ordertype.databinding.AddCurbsideVehicleDialogBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarColor
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarMaker
import com.skylinedynamics.solosdk.api.models.objects.curbside.CustomerCar

class AddCurbsideVehicleDialog : DialogFragment(), CurbsideContract.View {

    private lateinit var curbsidePresenter: CurbsideContract.Presenter

    private lateinit var vehicleBrandDialog: VehicleBrandDialog

    private var onSubmit: OnSubmit? = null

    private var isEmpty = false

    private var colorSelected = "";

    private var carMaker: CarMaker? = null

    private lateinit var binding: AddCurbsideVehicleDialogBinding

    interface OnSubmit {
        fun onSuccess(
            colorSelected: String, carMaker: CarMaker?
        )

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
        curbsidePresenter.start()
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
                onSubmit?.onSuccess(colorSelected, carMaker)
            }

            chooseCurbsideBrandColor.setOnClickListener {

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
                    binding.confirmContainer.isVisible =
                        binding.plateNumberEdit.text.toString().isNotEmpty()
                }
            })

            binding.continueButton.setOnClickListener {

            }

            plateNumberEdit.setText("ABC123")
            binding.continueButton.isVisible = true
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
        fun newInstance(isEmpty: Boolean, listener: OnSubmit): AddCurbsideVehicleDialog {
            val addressDialogFragment = AddCurbsideVehicleDialog()
            addressDialogFragment.onSubmit = listener
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

    }

    private fun showVehicleBrand() {
        dismissPickupTypeDialog()
        vehicleBrandDialog =
            VehicleBrandDialog.newInstance(carMaker, object : VehicleBrandDialog.OnSubmit {
                override fun selectedCar(maker: CarMaker) {
                    carMaker = maker
                    setupBrand()
                    dismissPickupTypeDialog()
                }

                override fun exit() {
                    dismissPickupTypeDialog()
                }

            })
        vehicleBrandDialog.show(
            requireActivity().supportFragmentManager, ChoosePickupTypeDialog::class.java.simpleName
        )
    }

    private fun dismissPickupTypeDialog() {
        try {
            vehicleBrandDialog.dismiss()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}
