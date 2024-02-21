package com.herfy.curbside.curbsidenew

import android.os.Bundle
import com.herfy.base.BaseActivity
import com.herfy.curbside.CurbsideContract
import com.herfy.curbside.CurbsidePresenter
import com.herfy.ordertype.databinding.CurbsideNewActivityBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarMaker
import com.skylinedynamics.solosdk.api.models.objects.curbside.CustomerCar

class CurbsideNewActivity : BaseActivity(), CurbsideContract.View {

    private lateinit var curbsidePresenter: CurbsideContract.Presenter

    private lateinit var binding: CurbsideNewActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CurbsideNewActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        curbsidePresenter = CurbsidePresenter(this)
        curbsidePresenter.start()
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
        }
    }

    override fun showCarColors(colors: MutableList<String>?) {

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

}