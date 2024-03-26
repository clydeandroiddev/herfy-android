package com.herfy.order.ordertrackernew

import android.Manifest
import android.app.Activity
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.navArgs
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.ext.cast
import com.herfy.history.OrdersContract
import com.herfy.history.OrdersPresenter
import com.herfy.ordertracker.databinding.FragmentOrderNewTrackerBinding
import com.herfy.util.AppLogger
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DialogUtils
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.ViewUtil
import com.location.FusedLocationProvider
import com.skylinedynamics.solosdk.api.models.objects.Address
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus
import com.skylinedynamics.solosdk.api.models.objects.Rating
import com.skylinedynamics.solosdk.api.models.objects.Store
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class OrderNewTrackerFragment : BaseFragment(), OrdersContract.View {

    private var orderPresenter: OrdersContract.Presenter? = null

    private lateinit var binding: FragmentOrderNewTrackerBinding

    private var googleMap: GoogleMap? = null

    private var locationMarker: Marker? = null

    private var driverMarker: Marker? = null

    private val args: OrderNewTrackerFragmentArgs by navArgs()

    private var userLatLng: LatLng? = null

    private var userStore: Store? = null

    private val gpsLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
            initializeLocation()
        }

    private val locationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getLocation()
        }
    }

    fun initializeLocation() {
        context?.let {
            val locationRequest = LocationRequest.create()
            locationRequest.interval = 60000
            locationRequest.fastestInterval = 60000
            locationRequest.smallestDisplacement = 0.0f
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            val locationSettingsRequestBuilder = LocationSettingsRequest.Builder()
            locationSettingsRequestBuilder.addLocationRequest(locationRequest)
            locationSettingsRequestBuilder.setAlwaysShow(true)
            LocationServices.getSettingsClient(it)
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
                                val intentSenderRequest =
                                    IntentSenderRequest.Builder(resolvableApiException.resolution)
                                        .build()
                                gpsLauncher.launch(intentSenderRequest)
                            } catch (exception: IntentSender.SendIntentException) {
                                exception.printStackTrace()
                            }
                        } else if (e.statusCode == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                            AppLogger.error("Settings change unavailable")
                        }
                    }
                }
        }

    }

    fun getLocation() {
        activity?.apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                context?.let {
                    orderPresenter?.getLocation(FusedLocationProvider(it))
                }
            } else {
                CacheUtil.getInstance().orderLatitude = Values.DEFAULT_LATITUDE.toString()
                CacheUtil.getInstance().orderLongitude = Values.DEFAULT_LONGITUDE.toString()
                locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderPresenter = OrdersPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentOrderNewTrackerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        orderPresenter?.start()
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.showLoadingDialog()
        }
        orderPresenter?.getOrder(args.orderId, -1, false, false)
    }

    override fun setPresenter(presenter: OrdersContract.Presenter) {
        orderPresenter = presenter
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
                navigateUp()
            }

            val supportMapFragment =
                childFragmentManager.findFragmentById(R.id.order_type_map) as SupportMapFragment?
            supportMapFragment?.getMapAsync { map ->
                googleMap = map
                googleMap?.uiSettings?.isCompassEnabled = false
                googleMap?.uiSettings?.isZoomControlsEnabled = false
                googleMap?.uiSettings?.isMapToolbarEnabled = false
                setUpMapLocale()
                googleMap?.setOnMapLoadedCallback {
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                Values.DEFAULT_LATITUDE, Values.DEFAULT_LONGITUDE
                            ), 15f
                        )
                    )
                    initializeLocation()
                    activity?.cast<BaseActivity> {
                        googleMap?.setPadding(0, 0, 0, 0)
                    }
                }
            }

            binding.location.setOnClickListener { initializeLocation() }
        }
    }

    private fun setUpMapLocale() {
        val languageToLoad = if (LocaleUtil.getInstance().language == "ar") "ar" else "en"
        val locale = Locale(languageToLoad)
        Locale.setDefault(locale)
        resources.configuration.setLocale(locale)
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.apply {
            title.text =
                CacheUtil.getInstance().getTranslations(Text.ORDER_TRACKER, "Order Trackre")
        }
    }

    override fun showLocation(isPlace: Boolean, latLng: LatLng, address: String) {
        try {
            if (locationMarker != null) {
                locationMarker?.remove()
            }
            val markerOptions = MarkerOptions()
            markerOptions.position(latLng)
            try {
                val activity: Activity? = activity
                if (activity != null) {
                    val icon = ViewUtil.vectorDrawableToBitmapDescriptor(
                        activity, R.drawable.user_current_location
                    )
                    markerOptions.icon(icon)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            markerOptions.anchor(0.5f, 0.5f)
            locationMarker = googleMap?.addMarker(markerOptions)
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11f))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun showOrders(newestToOldest: Boolean, orders: ArrayList<OrderHistory>?) {

    }

    override fun setOrder(order: OrderHistory?) {

    }

    override fun updateOrder(position: Int) {

    }

    override fun selectOrder(order: OrderHistory?) {

    }


    override fun showError(error: String?) {
        dismissDialogs()
        DialogUtils.showAlertDialog(context = requireContext(),
            title = "",
            body = error.orEmpty(),
            dialogInterface = { dialog, which ->
                dialog.dismiss()
            })
    }

    override fun showOrder(order: OrderDetails?, store: Store?, address: Address?) {
        dismissDialogs()
        order?.let { item ->
            binding.apply {
                blocker.gone()

            }
        }
    }

    override fun showOrderStatuses(orderStatuses: ArrayList<OrderStatus>?, isDone: Boolean) {

    }

    override fun showMenuItems(order: OrderDetails?, menuItems: ArrayList<MenuItem>?) {

    }

    override fun setTimeLine(isCancelled: Boolean, orderStatus: OrderStatus?, noDriver: Boolean) {

    }

    override fun setMenuItem(menuItem: MenuItem?) {

    }

    override fun showCart() {

    }

    override fun updateStatus(orderDetails: OrderDetails?, orderStatus: OrderStatus?) {

    }

    override fun showDriver(customerLatLng: LatLng?, driverLatLng: LatLng?) {

    }

    override fun updateDriver(latLng: LatLng?, mobileNumber: String?) {

    }

    override fun cancel() {

    }

    override fun showRatings(orderStatus: OrderStatus?, ratings: MutableList<Rating>?) {

    }

    override fun showMessage(message: String?) {

    }

    override fun customerArrivedSuccess() {

    }

    override fun logFirebaseEvent(event: String?) {

    }

    override fun getFromCache(key: String?): String? {
        return null
    }

    override fun showWhatsApp(mobileNumber: String?) {

    }
}
