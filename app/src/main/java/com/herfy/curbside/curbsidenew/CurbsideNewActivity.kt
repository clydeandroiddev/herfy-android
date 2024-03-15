package com.herfy.curbside.curbsidenew

import android.os.Bundle
import com.herfy.base.BaseActivity
import com.herfy.curbside.CurbsideContract
import com.herfy.curbside.CurbsidePresenter
import com.herfy.curbside.curbsidenew.adapter.CurbsideVehicleAdapter
import com.herfy.curbside.curbsidenew.dialog.AddCurbsideVehicleDialog
import com.herfy.order.views.ordertype.addaddress.AddAddressDialog
import com.herfy.ordertype.databinding.CurbsideNewActivityBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarColor
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarMaker
import com.skylinedynamics.solosdk.api.models.objects.curbside.CustomerCar

class CurbsideNewActivity : BaseActivity(), CurbsideContract.View {

    private lateinit var curbsidePresenter: CurbsideContract.Presenter

    private lateinit var binding: CurbsideNewActivityBinding

    private lateinit var addCurbsideDialog: AddCurbsideVehicleDialog

    private lateinit var curbsideVehicleAdapter: CurbsideVehicleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CurbsideNewActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        curbsidePresenter = CurbsidePresenter(this)
        curbsidePresenter.start()
        showLoadingDialog()
        curbsidePresenter.getCustomerCars()
        curbsidePresenter.getColorsList(false)
        curbsidePresenter.getMakersList(false)
    }


    override fun onBackPressed() {
        super.onBackPressed()
        finish()
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

    override fun setupViews() {
        var carId = ""
        val car = CacheUtil.getInstance().curbsideCar
        if (car != null && !intent.getBooleanExtra("fromProfile", false)) {
            carId = car.id.orEmpty()
        }
        binding.apply {
            back.setOnClickListener {
                finish()
            }
        }

        curbsideVehicleAdapter = CurbsideVehicleAdapter(
            ArrayList(), object : CurbsideVehicleAdapter.CurbsideListener {
                override fun onSelectedCar(car: CustomerCar, position: Int) {
                    if (!intent.getBooleanExtra("fromProfile", false)) {
                        if (AuthUtil.getInstance().isSignedIn) {
                            CacheUtil.getInstance().curbsideCar = car
                            setResult(RESULT_OK)
                            finish()
                        }
                    }
                }

                override fun onDeleteItem(car: CustomerCar, position: Int) {
                    showLoadingDialog()
                    curbsidePresenter.deleteCar(car.id, position)
                }

                override fun onEditItem(car: CustomerCar, position: Int) {
                    showAddDialog(false, car)
                }
            }, carId, intent.getBooleanExtra("fromProfile", false)
        )
        binding.curbsideList.adapter = curbsideVehicleAdapter
    }


    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.apply {
            if (intent.getBooleanExtra("fromProfile", false)) {
                curbsideLabel.text = CacheUtil.getInstance()
                    .getTranslations(Text.CURBSIDE_VEHICLES, "Curbside Vehicles")
            } else {
                curbsideLabel.text = CacheUtil.getInstance().getTranslations(
                    Text.CHOOSE_YOUR_CURBSIDE_VEHICLE, "Choose your curbside vehicle"
                )
            }
            continueButton.text =
                CacheUtil.getInstance().getTranslations(Text.ADD_NEW_VEHICLE, "Add New Vehicle")
            continueButton.setOnClickListener {
                showAddDialog(false)
            }
        }
    }

    override fun showCarColors(colors: MutableList<CarColor>?) {

    }

    override fun showCarMakers(makers: MutableList<CarMaker>?) {

    }

    override fun showError(error: String?) {
        dismissDialogs()
        showAlertDialog("", error)
    }

    override fun showCustomerCars(cars: MutableList<CustomerCar>) {
        dismissDialogs()
        if (cars.isEmpty()) {
            showAddDialog(true)
        } else {
            curbsideVehicleAdapter.updateList(ArrayList(cars))
        }
    }

    private fun showAddDialog(isEmpty: Boolean) {
        showAddDialog(isEmpty, null)
    }

    private fun showAddDialog(isEmpty: Boolean, customerCar: CustomerCar?) {
        dismissAddressDialog()
        addCurbsideDialog = AddCurbsideVehicleDialog.newInstance(
            customerCar,
            isEmpty,
            object : AddCurbsideVehicleDialog.OnSubmit {
                override fun onSuccess() {
                    dismissAddressDialog()
                    if (intent.getBooleanExtra("fromProfile", false)) {
                        curbsidePresenter.getCustomerCars()
                    } else {
                        setResult(RESULT_OK)
                        finish()
                    }
                }

                override fun error(text: String) {
                    showAlertDialog("", text)
                }

                override fun exit(isEmpty: Boolean) {
                    dismissAddressDialog()
                    if (isEmpty) {
                        finish()
                    }
                }

            })
        addCurbsideDialog.show(
            this.supportFragmentManager, AddAddressDialog::class.java.simpleName
        )
    }

    override fun successAdd(car: CustomerCar?) {

    }

    override fun successEdit(car: CustomerCar?, position: Int) {

    }

    override fun successDelete(position: Int) {
        dismissDialogs()
        val car = curbsideVehicleAdapter.getCar(position)
        curbsideVehicleAdapter.removeItem(position)
        if (curbsideVehicleAdapter.listCount() == 0) {
            showAddDialog(true)
        }
        val car1 = CacheUtil.getInstance().curbsideCar
        if (car1 != null) {
            if (car1.id == car.id) {
                CacheUtil.getInstance().curbsideCar = null
            }
        }
    }

    private fun dismissAddressDialog() {
        try {
            addCurbsideDialog.dismiss()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


}