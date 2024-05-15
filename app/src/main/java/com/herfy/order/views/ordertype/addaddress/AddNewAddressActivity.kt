package com.herfy.order.views.ordertype.addaddress

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.tabs.TabLayout
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.order.OrderContract
import com.herfy.order.OrderPresenter
import com.herfy.order.views.ordertype.selectarea.SelectCityDialog
import com.herfy.ordertype.databinding.AddressNewActivityBinding
import com.herfy.util.AppLogger
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.AnimUtil
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.location.FusedLocationProvider
import com.skylinedynamics.solosdk.api.models.objects.Address
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Rating
import com.skylinedynamics.solosdk.api.models.objects.Store
import com.skylinedynamics.solosdk.api.models.objects.area.Area
import java.util.Locale

class AddNewAddressActivity : BaseActivity(), OrderContract.View {

    private lateinit var orderPresenter: OrderContract.Presenter

    private lateinit var binding: AddressNewActivityBinding

    private lateinit var googleMap: GoogleMap

    private lateinit var locationMarker: Marker

    private lateinit var addAddressDialog: AddAddressDialog

    private lateinit var selectCityDialog: SelectCityDialog

    private var permissions = false

    private var fromShowLocation = false

    private var phoneNumber = ""
    private var addressLabel = ""
    private var addressLabel2 = ""
    private var driverNote = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AddressNewActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        orderPresenter = OrderPresenter(this)
        orderPresenter.setGeocoder(Geocoder(this, LocaleUtil.getInstance().createLocale()))
        orderPresenter.start()
    }

    override fun setPresenter(presenter: OrderContract.Presenter) {
        orderPresenter = presenter
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun logEvent(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {

    }

    override fun setupViews() {
        binding.closeButton.setOnClickListener {
            onBackPressed()
        }

        binding.selectArea.setOnClickListener {
            selectCityDialog()
        }

        binding.selectAreaLabel.text =
            CacheUtil.getInstance().getTranslations(Text.SELECT_AREA, "Select Area")

        binding.descriptionLocation.text = CacheUtil.getInstance().getTranslations(
            Text.MOVE_THE_PIN,
            "Move the pin to the correct location to help rider deliver your order fast"
        )

        binding.continueButton.text = CacheUtil.getInstance().getTranslations(
            Text.CONFIRM_DELIVERY_LOCATION, "Confirm delivery location"
        )

        binding.continueButton.setOnClickListener {
            proceedToAddAddress()
        }

        val supportMapFragment =
            supportFragmentManager.findFragmentById(R.id.address_map) as SupportMapFragment?
        if (supportMapFragment != null) {
            setUpMapLocale()
            supportMapFragment.getMapAsync(OnMapReadyCallback { map ->
                googleMap = map
                googleMap.uiSettings.isCompassEnabled = false
                googleMap.uiSettings.isZoomControlsEnabled = false
                googleMap.uiSettings.isMapToolbarEnabled = false
                googleMap.setOnMapLoadedCallback {
                    showLoadingDialog()
                    initializeLocation()
                }
                googleMap.setOnMapClickListener { latLng ->
                    if (this::locationMarker.isInitialized) {
                        locationMarker.isVisible = true
                    }
                    orderPresenter.getAddress(latLng)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                }
                googleMap.setOnCameraIdleListener {
                    if (!fromShowLocation && this::locationMarker.isInitialized) {
                        locationMarker.isVisible = true
                    } else {
                        fromShowLocation = false
                    }
                    orderPresenter.getAddress(googleMap.cameraPosition.target)
                }
            })
        }

        binding.location.setOnClickListener { initializeLocation() }

        if (intent.getBooleanExtra("goToLocation", false)) {
            binding.location.performClick()
        }
    }

    private fun setUpMapLocale() {
        var languageToLoad = "en"
        if (LocaleUtil.getInstance().language.equals("ar", ignoreCase = true)) {
            languageToLoad = "ar"
        } else if (LocaleUtil.getInstance().language.equals("fr", ignoreCase = true)) {
            languageToLoad = "fr"
        }
        val locale = Locale(languageToLoad)
        Locale.setDefault(locale)
        baseContext.resources.configuration.setLocale(locale)
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {

    }

    override fun showOrderTypes(orderTypes: MutableSet<OrderType>?) {

    }

    override fun addToCartFavorite(menuItem: MenuItem?) {
        TODO("Not yet implemented")
    }

    override fun createOrderType(orderType: OrderType?) {

    }

    override fun selectOrderType(orderType: OrderType?) {

    }

    override fun createTab(tag: String?, text: String?, icon: Drawable?): TabLayout.Tab? {
        return null
    }

    override fun initializeLocation() {
        val locationRequest = LocationRequest.create()
        locationRequest.setInterval(60000)
        locationRequest.setFastestInterval(60000)
        locationRequest.setSmallestDisplacement(0.0f)
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        val locationSettingsRequestBuilder = LocationSettingsRequest.Builder()
        locationSettingsRequestBuilder.addLocationRequest(locationRequest)
        locationSettingsRequestBuilder.setAlwaysShow(true)
        LocationServices.getSettingsClient(this)
            .checkLocationSettings(locationSettingsRequestBuilder.build())
            .addOnCompleteListener { task ->
                try {
                    task.getResult(ApiException::class.java)
                    getLocation()
                } catch (e: ApiException) {
                    e.printStackTrace()
                    if (e.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                        try {
                            val resolvableApiException = e as ResolvableApiException
                            resolvableApiException.startResolutionForResult(
                                this@AddNewAddressActivity, Values.PERMISSION_LOCATION
                            )
                        } catch (exception: IntentSender.SendIntentException) {
                            dismissDialogs()
                            exception.printStackTrace()
                        }
                    } else if (e.statusCode == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                        dismissDialogs()
                        AppLogger.error("Settings change unavailable")
                    }
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Values.PERMISSION_LOCATION) {
            getLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == Values.PERMISSION_LOCATION) {
                getLocation()
            } else if (requestCode == Values.PERMISSION_CALL) {
                orderPresenter.callStore()
            }
        }
    }

    override fun getLocation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            orderPresenter.getLocation(FusedLocationProvider(this))
        } else {
            permissions = true
            CacheUtil.getInstance().orderLatitude = Values.DEFAULT_LATITUDE.toString()
            CacheUtil.getInstance().orderLongitude = Values.DEFAULT_LONGITUDE.toString()
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), Values.PERMISSION_LOCATION
            )
        }
    }

    override fun showLocation(isPlace: Boolean, latLng: LatLng?, address: String?) {
        if (this::locationMarker.isInitialized) {
            locationMarker.remove()
        }
        val markerOptions = MarkerOptions()
        markerOptions.position(latLng!!)
        try {
            val bitmapDrawable = ContextCompat.getDrawable(
                this, com.herfy.ordertype.R.drawable.ic_location_add_address
            ) as BitmapDrawable?
            val bitmap = Bitmap.createScaledBitmap(
                bitmapDrawable!!.bitmap, 50, 50, true
            )
            val canvas = Canvas(bitmap)
            val paint = Paint()
            val filter: ColorFilter = PorterDuffColorFilter(
                Color.parseColor(CacheUtil.getInstance().colorMain), PorterDuff.Mode.SRC_IN
            )
            paint.setColorFilter(filter)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap))
            markerOptions.anchor(0.5f, 0.5f)
            markerOptions.visible(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        locationMarker = googleMap.addMarker(markerOptions)!!
        fromShowLocation = true
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng!!, 15f))

        dismissDialogs()
    }

    private fun dismissAddressDialog() {
        try {
            addAddressDialog.dismiss()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun proceedToAddAddress() {
        dismissAddressDialog()
        addAddressDialog = AddAddressDialog.newInstance(null,
            orderPresenter.currentAddress,
            false,
            object : AddAddressDialog.OnSubmit {
                override fun onSuccess(
                    labelValue: String,
                    address: android.location.Address?,
                    flatNumber: String,
                    mobile: String,
                    note: String
                ) {
                    phoneNumber = mobile
                    driverNote = note
                    addressLabel2 = flatNumber
                    addressLabel = labelValue
                    orderPresenter.address = address
                    showLoadingDialog()
                    orderPresenter.addAddress(
                        phoneNumber, addressLabel, driverNote, addressLabel2
                    )
                }

                override fun error(text: String) {
                    showAlertDialog("", text)
                }

                override fun exit() {
                    dismissAddressDialog()
                }

            })
        addAddressDialog.show(
            this.supportFragmentManager, AddAddressDialog::class.java.simpleName
        )
    }

    private fun dismissCityDialog() {
        try {
            selectCityDialog.dismiss()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun selectCityDialog() {
        dismissCityDialog()
        selectCityDialog = SelectCityDialog.newInstance(object : SelectCityDialog.OnSubmit {
            override fun onSuccess(
                area: Area
            ) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(LatLng(area.lat, area.long)))
                dismissCityDialog()
            }

            override fun exit() {
                dismissAddressDialog()
            }
        })
        selectCityDialog.show(
            this.supportFragmentManager, SelectCityDialog::class.java.simpleName
        )
    }

    override fun showAddresses(
        bookmarkedAddresses: ArrayList<Address>?, otherAddresses: ArrayList<Address>?
    ) {

    }

    override fun setAddress(address: Address?, selected: Boolean) {

    }

    override fun selectAddressHome(address: Address?, redownloadMenu: Boolean) {

    }

    override fun navigateMenuItemDetails(menuItem: MenuItem?) {

    }

    override fun selectAddressWork(position: Int) {

    }

    override fun selectAddressOther(address: Address?, redownloadMenu: Boolean) {

    }

    override fun updateAddAddress(updateAddress: Address?, saveAddress: android.location.Address?) {
        if (AuthUtil.getInstance().isSignedIn) {
            orderPresenter.addAddress(phoneNumber, addressLabel, driverNote, addressLabel2)
        } else {
            dismissDialogs()
        }
    }

    override fun showStores(pickupStores: ArrayList<Store>?, curbsideStores: ArrayList<Store>?) {

    }

    override fun setStore(store: Store?, selected: Boolean) {

    }

    override fun callStore(store: Store?) {

    }

    override fun selectStore(position: Int, store: Store?, initialLoad: Boolean) {

    }

    override fun showStore(markerClicked: Boolean, position: Int) {

    }

    override fun showStore(open: Boolean, store: Store?, error: String?) {

    }

    override fun showOrderType(orderType: OrderType?) {

    }

    override fun selectDelivery() {

    }

    override fun selectPickup() {

    }

    override fun selectDineIn() {

    }

    override fun selectCurbside() {

    }

    override fun saveAddress(address: String?) {
        updateAddress(address.orEmpty())
    }

    override fun saveAddress(address: Address?) {
        updateAddress(address?.attributes?.line1.orEmpty())
    }

    override fun updateAddress(address: String?) {
        if (CacheUtil.getInstance().orderStore == null) {
            orderPresenter.getStore(orderPresenter.selectedAddress)
        } else {
            dismissDialogs()
            dismissAddressDialog()
            val eventAttributes = java.util.HashMap<String, String>()
            eventAttributes["Delivery"] = address!!
            logEvent("OrderTypeSelection", eventAttributes, null, 0.0)
            overridePendingTransition(AnimUtil.outF(), AnimUtil.outS())
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun logFirebaseEvent(event: String?) {
        TODO("Not yet implemented")
    }

    override fun showUnavailableMenuItems() {

    }

    override fun deleteUnavailableItems() {

    }

    override fun showPhoneNumberError(error: String?) {

    }

    override fun showError(error: String?) {
        showAlertDialog("", error)
    }

    override fun updateMap(
        orderType: OrderType?, startLocation: LatLng?, endLocation: LatLng?, bounds: LatLngBounds?
    ) {

    }

    override fun showOrder(order: OrderDetails?) {

    }

    override fun showOrderStatuses(orderStatuses: ArrayList<OrderStatus>?) {

    }

    override fun setTimeLine(orderStatus: OrderStatus?) {

    }

    override fun showStatus(orderStatus: OrderStatus?) {

    }

    override fun showDriver(latLng: LatLng?) {

    }

    override fun updateDriver(latLng: LatLng?) {

    }

    override fun showMessage(message: String?) {
        dismissDialogs()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showRatings(orderStatus: OrderStatus?, ratings: MutableList<Rating>?) {

    }

    override fun showOrders() {

    }

    override fun showMenuItem(categoryId: String?, menuItemId: String?) {
        TODO("Not yet implemented")
    }

    override fun enableConfirmButton(enable: Boolean, storeName: String?) {

    }

    override fun saveToCache(key: String?, value: String?) {

    }

    override fun preloadImage(imageUrl: String?, requestOptions: RequestOptions?) {

    }

    override fun diffMenuSaved(address: Address?) {

    }

    override fun showPromptChangeOrderType(address: Address?) {

    }

    override fun showMinimumDeliveryForAreaMessage(error: String?) {
        runOnUiThread {
            dismissDialogs()
            showAlertDialog(
                "", error, CacheUtil.getInstance().getTranslations(Text.OK)
            ) { _: DialogInterface?, i: Int -> orderPresenter.setOrderType() }
        }
    }

    override fun setCloseTime(time: String?) {

    }

}