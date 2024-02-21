package com.herfy.curbside.curbsidenew

import android.location.Address
import android.os.Bundle
import com.herfy.base.BaseActivity
import com.herfy.curbside.CurbsideContract
import com.herfy.curbside.CurbsidePresenter
import com.herfy.order.views.ordertype.addaddress.AddAddressDialog
import com.herfy.ordertype.databinding.CurbsideNewActivityBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarMaker
import com.skylinedynamics.solosdk.api.models.objects.curbside.CustomerCar

class CurbsideNewActivity : BaseActivity(), CurbsideContract.View {

    private lateinit var curbsidePresenter: CurbsideContract.Presenter

    private lateinit var binding: CurbsideNewActivityBinding

    private lateinit var addCurbsideDialog: AddCurbsideVehicleDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CurbsideNewActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        curbsidePresenter = CurbsidePresenter(this)
        curbsidePresenter.start()
        showLoadingDialog()
        curbsidePresenter.getCustomerCars()
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
        binding.apply {
            back.setOnClickListener {
                finish()
            }
        }
    }


    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.apply {
            curbsideLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.CHOOSE_YOUR_CURBSIDE_VEHICLE, "Choose your curbside vehicle")
            continueButton.text =
                CacheUtil.getInstance().getTranslations(Text.ADD_NEW_VEHICLE, "Add New Vehicle")
            continueButton.setOnClickListener {
                showAddDialog()
            }
        }
    }

    override fun showCarColors(colors: MutableList<String>?) {

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
            showAddDialog()
        } else {
            //TODO List vehicles
        }
    }

    private fun showAddDialog() {
        dismissAddressDialog()
        addCurbsideDialog =
            AddCurbsideVehicleDialog.newInstance(object : AddCurbsideVehicleDialog.OnSubmit {
                override fun onSuccess(
                    colorSelected: String, carMaker: CarMaker?
                ) {

                }

                override fun error(text: String) {
                    showAlertDialog("", text)
                }

                override fun exit() {
                    dismissAddressDialog()
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

    }

    private fun dismissAddressDialog() {
        try {
            addCurbsideDialog.dismiss()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


}