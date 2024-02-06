package com.herfy.curbside

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.gson.JsonObject
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.curbside.adapter.CustomerCarAdapter
import com.herfy.curbside.adapter.CustomerCarAdapter.CustomerCarListener
import com.herfy.curbside.dialog.CarMakerDialog
import com.herfy.curbside.dialog.CarMakerDialog.OnActionCarMaker
import com.herfy.curbside.dialog.ColorListDialog
import com.herfy.curbside.dialog.DeleteCarDialog
import com.herfy.curbside.dialog.EditCarDialog
import com.herfy.databinding.FragmentCurbsideBinding
import com.herfy.ext.cast
import com.herfy.main.MainActivity
import com.herfy.main.NavigationActivity
import com.herfy.order.views.ordertype.OrderTypeNewFragment
import com.herfy.utils.helper.*
import com.herfy.util.StringUtil
import com.herfy.util.ViewUtil
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarMaker
import com.skylinedynamics.solosdk.api.models.objects.curbside.CustomerCar
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.BuildUtil
import com.herfy.utils.helper.CacheUtil
import java.util.Locale

class CurbsideFragment : BaseFragment(), CurbsideContract.View, CustomerCarListener {

    companion object {
        const val ARGS_KEY_FRAGMENT_RESULT = "fragmentResult"
    }

    private lateinit var binding: FragmentCurbsideBinding

    private val args: CurbsideFragmentArgs by navArgs()

    private lateinit var presenter: CurbsideContract.Presenter

    private var colorListDialog: ColorListDialog? = null
    private var carMakerDialog: CarMakerDialog? = null
    private var deleteCarDialog: DeleteCarDialog? = null
    private var editCarDialog: EditCarDialog? = null
    private var adapter: CustomerCarAdapter? = null

    private var carColor: String? = null
    private var carMakerName: String? = null
    private var carMakerId: String? = null
    private var isEditMode = false
    private val customerCars: MutableList<CustomerCar> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = CurbsidePresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCurbsideBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.start()
        presenter.getColorsList(false)
        presenter.getMakersList(false)
        showCars()
    }

    override fun showCarColors(colors: List<String>) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
        }
        showCarColorDialog()
    }

    override fun showCarMakers(makers: List<CarMaker>) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
        }
        showCarMakerDialog()
    }

    override fun showError(error: String) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
            baseActivity.showAlertDialog("", error)
        }
    }

    override fun showCustomerCars(cars: List<CustomerCar>) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
        }
        customerCars.clear()
        customerCars.addAll(cars)
        setToolbarActionButton(customerCars.isNotEmpty())
        if (cars.isNotEmpty()) {
            binding.empty.visibility = View.GONE
        } else {
            binding.empty.visibility = View.VISIBLE
        }
        adapter!!.notifyDataSetChanged()
    }

    private fun setToolbarActionButton(hasCars: Boolean) {
        activity?.cast<MainActivity> {
            if(hasCars) {
                it.showToolbarActionButton(true)
                it.setToolbarActionText(
                    CacheUtil.getInstance().getTranslations(Text.EDIT, "EDIT")
                        .uppercase(Locale.getDefault())
                )
                it.setToolbarActionButton {
                    onActionClicked()
                }
                it.enableToolbarActionButton(true)
            } else {
                it.showToolbarActionButton(false)
                it.setToolbarActionButton(null)
                it.enableToolbarActionButton(false)
            }
        }
        activity?.cast<NavigationActivity> {
            if(hasCars) {
                it.showToolbarActionButton(true)
                it.setToolbarActionText(
                    CacheUtil.getInstance().getTranslations(Text.EDIT, "EDIT")
                        .uppercase(Locale.getDefault())
                )
                it.setToolbarActionButton {
                    onActionClicked()
                }
                it.enableToolbarActionButton(true)
            } else {
                it.showToolbarActionButton(false)
                it.setToolbarActionButton(null)
                it.enableToolbarActionButton(false)
            }
        }
    }

    override fun successAdd(cars: CustomerCar) {
        carMakerName = null
        carColor = null
        binding.empty.visibility = View.GONE
        binding.carColorIcon.visibility = View.VISIBLE
        binding.makerColorIcon.visibility = View.VISIBLE
        binding.makerOtherText.text = ""
        binding.makerOtherText.visibility = View.GONE
        binding.makerImage.setImageResource(android.R.color.transparent)
        binding.colorImageSelected.setImageResource(android.R.color.transparent)
        carMakerId = null
        binding.plateNumberText!!.setText("")
        binding.plateNumberText!!.clearFocus()
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
        }
        customerCars.add(0, cars)
        setToolbarActionButton(customerCars.isNotEmpty())
        adapter!!.notifyDataSetChanged()
    }

    override fun successEdit(car: CustomerCar, position: Int) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
        }
        val car1 = CacheUtil.getInstance().curbsideCar
        if (car1 != null) {
            if (car1.id == car.id) {
                CacheUtil.getInstance().curbsideCar = car1
            }
        }
        customerCars[position] = car
        adapter!!.notifyDataSetChanged()
    }

    override fun successDelete(position: Int) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
        }
        if (adapter!!.selectedCarId == customerCars[position].id) {
            adapter!!.selectedCarId = ""
            binding.confirm.isEnabled = false
        }
        customerCars.removeAt(position)
        if(customerCars.isNotEmpty()) {
            binding.empty.visibility = View.GONE
        } else {
            setToolbarActionButton(false)
            onActionClicked()
            binding.empty.visibility = View.VISIBLE
        }
        adapter!!.notifyDataSetChanged()
    }

    fun navigateUp() {
        activity?.cast<MainActivity> {
            sendFragmentResult()
        }
        activity?.cast<NavigationActivity> {
            if(it.navigation == R.id.nav_curbside) {
                val intent = Intent()
                it.setResult(Activity.RESULT_OK, intent)
                it.finish()
            } else {
                sendFragmentResult()
            }
        }
    }

    private fun sendFragmentResult() {
        setFragmentResult(
            ARGS_KEY_FRAGMENT_RESULT,
            bundleOf(
                OrderTypeNewFragment.ARGS_KEY_CURBSIDE to args.curbside
            )
        )
        findNavController().navigateUp()
    }

    override fun setPresenter(presenter: CurbsideContract.Presenter) {
        this.presenter = presenter
    }

    override fun logEvent(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue)
        }
    }

    override fun setCartExpiry() {

    }

    override fun setupViews() {
        binding.carColorConstraint.setOnClickListener { v: View? -> showCarColorDialog() }
        binding.makerColorConstraint.setOnClickListener { v: View? -> showCarMakerDialog() }
        if (adapter == null) {
            adapter = CustomerCarAdapter(requireContext(), customerCars, this)
            if(CacheUtil.getInstance().curbsideCar != null) {
                adapter?.selectedCarId = CacheUtil.getInstance().curbsideCar.id
            } else {
                adapter?.selectedCarId = ""
            }
        }
        binding.confirm.isEnabled = CacheUtil.getInstance().curbsideCar != null
        if(AuthUtil.getInstance().isSignedIn) {
            //For Burgerizzr
            val phone = if (BuildUtil.soloAppKey().equals("SeBwUS2G9SEnkwty", ignoreCase = true)) {
                CacheUtil.getInstance().concept.dialingCode + AuthUtil.getInstance().customer.attributes.mobile.substring(
                            1
                        )
            } else {
                AuthUtil.getInstance().customer.attributes.mobile
            }
            binding.phoneNumberText.append(StringUtil.localize(phone.replace("+", "")))
        }
        binding.addNewCar.setOnClickListener { v: View? ->
            activity?.let {
                val baseActivity = it as BaseActivity
                BaseActivity.hideKeyboard(baseActivity)
                baseActivity.showLoadingDialog()
            }
            presenter.addNewCar(
                carColor,
                carMakerId?.toIntOrNull(),
                carMakerName,
                binding.plateNumberText.text.toString().trim { it <= ' ' },
                binding.phoneNumberText.text.toString().trim().replace("+", "")
            )
        }
        binding.confirm.setOnClickListener(View.OnClickListener {
            if (!AuthUtil.getInstance().isSignedIn) {
                return@OnClickListener
            }
            for (i in customerCars.indices) {
                if (customerCars[i].id == adapter!!.selectedCarId) {
                    CacheUtil.getInstance().curbsideCar = customerCars[i]
                    navigateUp()
                }
            }
        })
        binding.carsList.adapter = adapter
    }

    private fun onActionClicked() {
        if (isEditMode) {
            isEditMode = false
            adapter!!.isEditMode = false
            binding.footer.visibility = View.VISIBLE
            if (adapter?.selectedCarId?.isNotEmpty() == true) {
                binding.confirm.isEnabled = true
            }
            binding.carLayout.visibility = View.VISIBLE
            activity?.cast<MainActivity> {
                it.setToolbarTitle(
                    CacheUtil.getInstance().getTranslations(Text.CHOOSE_CAR, "CHOOSE CAR")
                    .uppercase(
                        Locale.getDefault()
                    ))
                it.setToolbarActionText(CacheUtil.getInstance().getTranslations(Text.EDIT_CAPS, "EDIT"))
            }
            activity?.cast<NavigationActivity> {
                it.setToolbarTitle(
                    CacheUtil.getInstance().getTranslations(Text.CHOOSE_CAR, "CHOOSE CAR")
                    .uppercase(
                        Locale.getDefault()
                    ))
                it.setToolbarActionText(CacheUtil.getInstance().getTranslations(Text.EDIT_CAPS, "EDIT"))
            }
        } else {
            if (customerCars.isEmpty()) {
                return
            }
            binding.footer.visibility = View.GONE
            binding.confirm.isEnabled = false
            isEditMode = true
            adapter!!.isEditMode = true
            binding.carLayout.visibility = View.GONE
            activity?.cast<MainActivity> {
                it.setToolbarTitle(
                    CacheUtil.getInstance().getTranslations(Text.EDIT_CARS, "EDIT CARS")
                    .uppercase(
                        Locale.getDefault()
                    ))
                it.setToolbarActionText(
                    CacheUtil.getInstance().getTranslations(Text.DONE_CAPS, "DONE").uppercase(
                    Locale.getDefault()
                ))
            }
            activity?.cast<NavigationActivity> {
                it.setToolbarTitle(
                    CacheUtil.getInstance().getTranslations(Text.EDIT_CARS, "EDIT CARS")
                    .uppercase(
                        Locale.getDefault()
                    ))
                it.setToolbarActionText(
                    CacheUtil.getInstance().getTranslations(Text.DONE_CAPS, "DONE").uppercase(
                    Locale.getDefault()
                ))
            }
        }
        adapter?.notifyDataSetChanged()
    }

    override fun setupFonts() {}
    override fun setupTranslations() {
        binding.plateNumberText.hint = CacheUtil.getInstance().getTranslations(
            Text.PLATE_NUMBER,
            "Plate Number"
        )
        binding.phoneNumberText.hint = CacheUtil.getInstance().getTranslations(
            Text.MOBILE_NUMBER,
            "Mobile Number"
        )
        binding.carMakerLabel.text =
            CacheUtil.getInstance().getTranslations(Text.MAKER, "Maker")
        binding.carColorLabel.text =
            CacheUtil.getInstance().getTranslations(Text.COLOR, "Color")
        binding.addNewCar.text = CacheUtil.getInstance().getTranslations(
            Text.ADD_THIS_AS_A_NEW_CAR,
            "ADD THIS AS A NEW CAR"
        ).uppercase(Locale.getDefault())
        binding.confirm.text = CacheUtil.getInstance()
            .getTranslations(Text.CONFIRM_CAPS, "CONFIRM")
            .uppercase(Locale.getDefault())
        binding.emptyCarText.text = CacheUtil.getInstance().getTranslations(
            Text.NO_CAR_REGISTERED_YET,
            "No cars registered yet"
        ).uppercase(Locale.getDefault())
        binding.emptyCarTextDesc.text = CacheUtil.getInstance().getTranslations(
            Text.ADD_A_CAR_MESSAGE,
            "Add a car by filling up the details of your vehicle above this message"
        )
    }

    private fun showCars() {
        if (AuthUtil.getInstance().isSignedIn) {
            activity?.let {
                val baseActivity = it as BaseActivity
                baseActivity.showLoadingDialog()
            }
            presenter!!.getCustomerCars()
        }
    }

    private fun showCarColorDialog() {
        try {
            val colors = presenter!!.carColors
            if (colors.isEmpty()) {
                activity?.let {
                    val baseActivity = it as BaseActivity
                    baseActivity.showLoadingDialog()
                }
                presenter!!.getColorsList(true)
            } else {
                colorListDialog = ColorListDialog.newInstance(
                    presenter!!.carColors,
                    object : ColorListDialog.OnAction {
                        override fun onClose() {
                            colorListDialog!!.dismiss()
                        }

                        override fun onAvail(color: String) {
                            colorListDialog!!.dismiss()
                            carColor = color
                            binding.carColorIcon.visibility = View.GONE
                            binding.colorImageSelected.visibility = View.VISIBLE
                            binding.colorImageSelected.setImageDrawable(
                                ViewUtil.getColorDrawable(
                                    "#" + color.replace(
                                        "#",
                                        ""
                                    )
                                )
                            )
                            checkData()
                        }
                    })
                colorListDialog?.isCancelable = true
                colorListDialog?.show(childFragmentManager, ColorListDialog::class.java.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showCarMakerDialog() {
        val carMakers = presenter!!.carMakers
        try {
            if (carMakers.isEmpty()) {
                activity?.let {
                    val baseActivity = it as BaseActivity
                    baseActivity.showLoadingDialog()
                }
                presenter!!.getMakersList(true)
            } else {
                carMakerDialog =
                    CarMakerDialog.newInstance(presenter!!.carMakers, object : OnActionCarMaker {
                        override fun onClose() {
                            carMakerDialog!!.dismiss()
                        }

                        override fun onAvail(color: CarMaker?, other: String?) {
                            carMakerDialog!!.dismiss()
                            binding.makerColorIcon.visibility = View.GONE
                            if (other == null) {
                                var imageUrl = color?.attributes?.imageUri
                                binding.makerImage.visibility = View.VISIBLE
                                binding.makerOtherText.visibility = View.GONE
                                if (imageUrl == null || imageUrl.equals(
                                        "null",
                                        ignoreCase = true
                                    ) || imageUrl.isEmpty() || imageUrl.lowercase(
                                        Locale.getDefault()
                                    ).contains("default-image.png")
                                ) {
                                    imageUrl = Values.DEFAULT_IMAGE_URL
                                }
                                Glide.with(this@CurbsideFragment).load(imageUrl)
                                    .apply(RequestOptions().override(80, 80)).skipMemoryCache(false)
                                    .diskCacheStrategy(
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
                                    }).into(binding.makerImage)
                                carMakerId = color?.id
                                carMakerName = null
                            } else {
                                binding.makerImage.visibility = View.GONE
                                binding.makerOtherText.visibility = View.VISIBLE
                                carMakerName = other
                                carMakerId = null
                                binding.makerOtherText.text = carMakerName
                            }
                            checkData()
                        }
                    })
                carMakerDialog?.isCancelable = true
                activity?.let {
                    carMakerDialog?.show(it.supportFragmentManager, CarMakerDialog::class.java.toString())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkData() {
        val isEnabled = carColor != null && (carMakerId != null || carMakerName != null)
        binding.addNewCar.isEnabled = isEnabled
    }

    override fun selectCar(maker: CustomerCar, position: Int, isSelected: Boolean) {
        if (isEditMode) return
        if (!AuthUtil.getInstance().isSignedIn) {
            return
        }
        if (isSelected) {
            binding.confirm.isEnabled = false
            adapter!!.selectedCarId = ""
        } else {
            binding.confirm.isEnabled = true
            adapter!!.selectedCarId = maker.id
        }
        adapter!!.notifyDataSetChanged()
    }

    override fun editCar(maker: CustomerCar, position: Int) {
        editCarDialog = EditCarDialog.newInstance(
            maker,
            presenter!!.carColors,
            presenter!!.carMakers,
            object : EditCarDialog.OnAction {
                override fun onClose() {
                    editCarDialog!!.dismiss()
                }

                override fun onEdit(customerCar: CustomerCar) {
                    editCarDialog!!.dismiss()
                    activity?.let {
                        val baseActivity = it as BaseActivity
                        baseActivity.showLoadingDialog()
                        BaseActivity.hideKeyboard(baseActivity)
                    }
                    val editBody = JsonObject()
                    editBody.addProperty("color", customerCar.attributes.color)
                    if (customerCar.attributes.carMakerId != null) {
                        editBody.addProperty(
                            "car-maker-id",
                            customerCar.attributes.carMakerId.toInt()
                        )
                    } else {
                        if (customerCar.attributes.carMakerName != null) {
                            editBody.addProperty(
                                "car-maker-name",
                                customerCar.attributes.carMakerName
                            )
                        }
                    }
                    if (customerCar.attributes.plateNumber != null) {
                        editBody.addProperty("plate-number", customerCar.attributes.plateNumber)
                    }
                    presenter!!.editNewCar(maker.id, editBody, position)
                }
            })
        editCarDialog?.isCancelable = true
        editCarDialog?.show(childFragmentManager, EditCarDialog::class.java.toString())
    }

    override fun deleteCar(maker: CustomerCar, position: Int) {
        deleteCarDialog = DeleteCarDialog.newInstance(object : DeleteCarDialog.OnAction {
            override fun onClose() {
                deleteCarDialog!!.dismiss()
            }

            override fun onDelete() {
                deleteCarDialog!!.dismiss()
                activity?.let {
                    val baseActivity = it as BaseActivity
                    baseActivity.showLoadingDialog()
                }
                presenter!!.deleteCar(maker.id, position)
            }
        })
        deleteCarDialog?.isCancelable = true
        deleteCarDialog?.show(childFragmentManager, DeleteCarDialog::class.java.toString())
    }
}