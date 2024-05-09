package com.herfy.order.ordertrackernew

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.gson.Gson
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.common.cancelTime
import com.herfy.common.drawArcPolyline
import com.herfy.common.formatSecondsToMinutesAndSeconds
import com.herfy.common.getCurrentTime
import com.herfy.common.getDisplayId
import com.herfy.common.showTime
import com.herfy.common.statusTime
import com.herfy.ext.cast
import com.herfy.history.OrdersContract
import com.herfy.history.OrdersPresenter
import com.herfy.history.historynew.adapter.OrderMenuItemsAdapter
import com.herfy.main.MainActivity
import com.herfy.ordertracker.databinding.FragmentOrderNewTrackerBinding
import com.herfy.util.AppLogger
import com.herfy.util.StringUtil
import com.herfy.util.extensions.dialPhoneNumber
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.extensions.showToast
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DialogUtils
import com.herfy.utils.helper.DistanceCalculator
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.ViewUtil
import com.location.FusedLocationProvider
import com.skylinedynamics.solosdk.api.models.kotlin.AppDriver
import com.skylinedynamics.solosdk.api.models.objects.Address
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus
import com.skylinedynamics.solosdk.api.models.objects.OrderStatusType
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Rating
import com.skylinedynamics.solosdk.api.models.objects.Store
import dagger.hilt.android.AndroidEntryPoint
import io.shipbook.shipbooksdk.Log
import java.util.Calendar
import java.util.Locale


@AndroidEntryPoint
class OrderNewTrackerFragment : BaseFragment(), OrdersContract.View,
    CancelOrderDialogFragment.OnAction {

    private var orderPresenter: OrdersContract.Presenter? = null

    private lateinit var binding: FragmentOrderNewTrackerBinding

    private var googleMap: GoogleMap? = null

    private var locationMarker: Marker? = null

    private var driverMarker: Marker? = null

    private var storeMarker: Marker? = null

    private val args: OrderNewTrackerFragmentArgs by navArgs()

    private var orderDetails: OrderDetails? = null

    private var orderStore: Store? = null

    private var orderAddress: Address? = null

    private var userLatLng: LatLng? = null

    private var countDownTimer: CountDownTimer? = null

    private var countDownTime: Long = 60000

    private lateinit var orderMenuItemsAdapter: OrderMenuItemsAdapter

    private var isSummaryHidden = false

    private var numberToCall = ""

    private var orderId = ""

    private var isOverHeadFrameShows = false

    private var myPolyline: Polyline? = null

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
            if (ActivityCompat.checkSelfPermission(
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
        getOrderId()
        orderPresenter?.getOrder(orderId, -1, false, false)
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

    @SuppressLint("ClickableViewAccessibility")
    override fun setupViews() {
        showOrderHelpDetails()

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    if (numberToCall.isNotEmpty()) {
                        requestCallPermission(numberToCall)
                    }
                }
            }

        binding.apply {

            transparentImage.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        mainScroll.requestDisallowInterceptTouchEvent(true)
                        false
                    }

                    MotionEvent.ACTION_UP -> {
                        mainScroll.requestDisallowInterceptTouchEvent(false)
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        mainScroll.requestDisallowInterceptTouchEvent(true)
                        false
                    }

                    else -> true
                }
            }

            orderMenuItemsAdapter = OrderMenuItemsAdapter(ArrayList())

            orderListItem.adapter = orderMenuItemsAdapter

            back.setOnClickListener {
                navigateUp()
            }

            paymentSummaryFrame.setOnClickListener {
                if (isSummaryHidden) {
                    imageOpenAndCloseSumamry.setImageResource(R.drawable.ic_close_summary)
                    orderPaymentDetailsFrame.isVisible = true
                    isSummaryHidden = false
                } else {
                    isSummaryHidden = true
                    imageOpenAndCloseSumamry.setImageResource(R.drawable.ic_open_summary)
                    orderPaymentDetailsFrame.isVisible = false
                }
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

    private fun getOrderId() {
        orderId = args.orderId
        if (orderId.isNullOrEmpty()) {
            orderId = CacheUtil.getInstance().lastOrderId
        }
    }

    private fun setUpMapLocale() {
        val languageToLoad = if (LocaleUtil.getInstance().language == "ar") "ar" else "en"
        val locale = Locale(languageToLoad)
        Locale.setDefault(locale)
        resources.configuration.setLocale(locale)
    }

    private fun deletePolyline() {
        myPolyline?.remove()
        myPolyline = null  // Clear the reference
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.apply {
            title.text =
                CacheUtil.getInstance().getTranslations(Text.ORDER_TRACKER, "Order Trackre")
            orderPaymentDetails.subtotalLabel.text =
                CacheUtil.getInstance().getTranslations(Text.SUBTOTAL, "Subtotal")
            orderPaymentDetails.deliverLabel.text =
                CacheUtil.getInstance().getTranslations(Text.DELIVERY_CHARGE, "Delivery Charges")
            orderPaymentDetails.discountLabel.text =
                CacheUtil.getInstance().getTranslations(Text.DISCOUNT, "Discount")
            totalLabel.text = CacheUtil.getInstance().getTranslations(Text.TOTAL, "Total")
            paymentSummaryLabel.text =
                CacheUtil.getInstance().getTranslations(Text.PAYMENT_SUMMARY, "Payment Summary")
            orderPaymentDetails.paymentSummaryLabel.text =
                CacheUtil.getInstance().getTranslations(Text.PAYMENT_SUMMARY, "Payment Summary")
            orderPaymentDetails.totalLabel.text =
                CacheUtil.getInstance().getTranslations(Text.TOTAL, "Total")
            orderHelpDetails.haveATroubleLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.HAVE_A_TROUBLE, "Have a trouble with your order?")
            orderHelpDetails.herfySupportLabel.text = CacheUtil.getInstance().getTranslations(
                Text.HERFY_SUPPORT_TEAM, "Herfy’s Support team is here to help you."
            )
            orderCurbsideDetails.thankYouLabel.text =
                CacheUtil.getInstance().getTranslations(Text.THANK_YOU, "Thank You!")
            orderCurbsideDetails.orderServedLabel.text = CacheUtil.getInstance().getTranslations(
                Text.YOUR_ORDER_WILL_BE_SERVED_SHORTLY, "Your order will be served shortly"
            )
            orderCurbsideDetails.letUsKnowLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.LET_US_KNOW, "Let us know when you arrive.")
            orderCurbsideDetails.curbsideVehicleLabel.text =
                CacheUtil.getInstance().getTranslations(Text.CURBSIDE_VEHICLE, "Curbside Vehicle")
            orderCurbsideDetails.weWillServeLabel.text = CacheUtil.getInstance().getTranslations(
                Text.WE_WILL_SERVE_YOUR_ORDER, "We will serve your order to you when you are here."
            )
            orderCurbsideDetails.serveOrder.text = CacheUtil.getInstance().getTranslations(
                Text.ARRIVED_SERVE_ORDER, "Arrived - Serve my order"
            )
            orderHelpDetails.callHelpLabel.text =
                CacheUtil.getInstance().getTranslations(Text.CALL, "Call")
            orderHelpDetails.whatsAppHelpMessageLabel.text =
                CacheUtil.getInstance().getTranslations(Text.WHATSAPP_MESSAGE, "Whatsapp Message")
            orderCancelDetails.cancelOrderLabel.text =
                CacheUtil.getInstance().getTranslations(Text.CANCEL_ORDER_NEW, "Cancel Order")
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
            userLatLng = latLng
            orderStore?.let {
                showStoreMarker(false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun showOrders(newestToOldest: Boolean, orders: ArrayList<OrderHistory>?) {

    }

    override fun setOrder(order: OrderHistory?) {

    }

    override fun updateOrder(position: Int) {
        orderPresenter?.getOrderStatuses()
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

    private fun showStoreMarker() {
        showStoreMarker(true)
    }

    private fun showStoreMarker(isMoveCamera: Boolean) {
        orderStore?.let { store ->
            try {
                val activity: Activity? = activity
                val inflater =
                    activity?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val view = inflater.inflate(R.layout.order_store_marker, null)
                val textView = view.findViewById<TextView>(com.herfy.ordertype.R.id.kmAway)
                var distanceValue = ""
                try {
                    val currentLocation = userLatLng
                    val currentDistance = currentLocation?.latitude?.let {
                        currentLocation.longitude.let { it1 ->
                            store.attributes?.lat?.let { it2 ->
                                store.attributes?.lng?.let { it3 ->
                                    DistanceCalculator.calculateDistance(
                                        it, it1, it2, it3
                                    )
                                }
                            }
                        }
                    }

                    distanceValue = StringUtil.getDistance(
                        currentDistance ?: 0.0
                    )
                    textView.text =
                        CacheUtil.getInstance().getTranslations(Text.KM_AWAY, "(x) away")
                            .replace("(x)", distanceValue).lowercase(Locale.getDefault())
                    if (currentDistance != null) {
                        if (currentDistance <= 0.0) {
                            binding.orderAddressDetails.addressKm.text = ""
                            textView.isInvisible = true
                        } else {
                            textView.isVisible = true
                            binding.orderAddressDetails.addressKm.text = distanceValue.lowercase(
                                Locale.getDefault()
                            )
                        }
                    }

                    if (userLatLng != null) {
                        deletePolyline()
                        val startPoint = store.attributes?.lat?.let {
                            store.attributes?.lng?.let { it1 ->
                                LatLng(
                                    it, it1
                                )
                            }
                        }
                        myPolyline = startPoint?.let { it1 ->
                            drawArcPolyline(
                                googleMap, it1, userLatLng!!, binding.mapFrame.context
                            )
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

                Handler(Looper.getMainLooper()).post {
                    if (storeMarker != null) {
                        storeMarker?.remove()
                    }
                    val markerOptions = MarkerOptions()
                    val latLng = store.attributes?.lat?.let {
                        store.attributes?.lng?.let { it1 ->
                            LatLng(
                                it, it1
                            )
                        }
                    }
                    if (latLng != null) {
                        markerOptions.position(latLng)
                    }
                    markerOptions.icon(
                        BitmapDescriptorFactory.fromBitmap(
                            ViewUtil.createDrawableFromView(
                                activity, view
                            )
                        )
                    )
                    markerOptions.anchor(0.5f, 1f)
                    storeMarker = googleMap?.addMarker(markerOptions)
                    storeMarker?.tag = "storeTag"
                    googleMap?.setOnMarkerClickListener { marker ->
                        if (marker.tag == "storeTag") {
                            goToStoreInMap(store)
                            true // Consume the click event
                        } else {
                            false // Default behavior (showing the InfoWindow)
                        }
                    }
                    if (isMoveCamera) {
                        latLng?.let { CameraUpdateFactory.newLatLngZoom(it, 11f) }
                            ?.let { googleMap?.animateCamera(it) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showStoreDeliveryTime() {
        orderDetails?.let { order ->
            orderStore?.let { store ->
                val orderType = OrderType.fromString(order.orderType)
                if (orderType == OrderType.DELIVERY) {
                    binding.orderStatusOverhead.readyForPickupLabel.text = CacheUtil.getInstance()
                        .getTranslations(Text.RIDER_ASSIGNED, "Rider assigned.")
                    binding.orderStatusOverhead.readyForPickupDescription.text =
                        CacheUtil.getInstance().getTranslations(
                            Text.RIDER_ASSIGNED_DESCRIPTION,
                            "A rider is assigned and is taking your order to you."
                        )
                    binding.estimatedLabel.text = CacheUtil.getInstance()
                        .getTranslations(Text.ESTIMATED_DELIVERY_TIME, "Estimated delivery time")
                    binding.timeDuration.text = showTime(
                        order.createdAt.orEmpty(), store.attributes.promisedTimeDelta.delivery
                    )
                } else if (orderType == OrderType.CURBSIDE) {
                    binding.orderStatusOverhead.readyForPickupLabel.text = CacheUtil.getInstance()
                        .getTranslations(Text.READY_FOR_PICKUP, "Ready for pickup")
                    binding.orderStatusOverhead.readyForPickupDescription.text =
                        CacheUtil.getInstance().getTranslations(
                            Text.YOUR_ORDER_IS_READY,
                            "Your order is ready. Pick it up from the counter"
                        )
                    binding.estimatedLabel.text = CacheUtil.getInstance()
                        .getTranslations(Text.ESTIMATE_PICKUP_TIME, "Estimate pickup time")
                    binding.timeDuration.text = showTime(
                        order.createdAt.orEmpty(), store.attributes.promisedTimeDelta.pickup
                    )
                } else {
                    binding.orderStatusOverhead.readyForPickupLabel.text = CacheUtil.getInstance()
                        .getTranslations(Text.READY_FOR_PICKUP, "Ready for pickup")
                    binding.orderStatusOverhead.readyForPickupDescription.text =
                        CacheUtil.getInstance().getTranslations(
                            Text.YOUR_ORDER_IS_READY,
                            "Your order is ready. Pick it up from the counter"
                        )
                    binding.estimatedLabel.text =
                        CacheUtil.getInstance().getTranslations(Text.ESTIMATE_TIME, "Estimate time")
                    binding.timeDuration.text = showTime(
                        order.createdAt.orEmpty(), store.attributes.promisedTimeDelta.pickup
                    )
                }
            }
        }
    }

    private fun goToStoreInMap(store: Store?) {
        store?.let {
            val directionsBuilder =
                Uri.Builder().scheme("https").authority("www.google.com").appendPath("maps")
                    .appendPath("dir").appendPath("").appendQueryParameter("api", "1")
                    .appendQueryParameter("travelmode", "driving").appendQueryParameter(
                        "destination",
                        store.attributes?.lat.toString() + "," + store.attributes?.lng
                    )

            if (StringUtil.hasOrderLatLong()) {
                directionsBuilder.appendQueryParameter(
                    "origin",
                    CacheUtil.getInstance().orderLatitude + "," + CacheUtil.getInstance().orderLongitude
                )
            }

            try {
                val activity: Activity? = activity
                activity?.startActivity(
                    Intent(
                        Intent.ACTION_VIEW, directionsBuilder.build()
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getDirection(store: Store?) {
        store?.let {
            binding.orderAddressDetails.getDirection.text =
                CacheUtil.getInstance().getTranslations(Text.GET_DIRECTION, "Get Direction")
            binding.orderAddressDetails.getDirection.setOnClickListener {
                goToStoreInMap(store)
            }
        }
    }

    private fun getBranchTime(store: Store?) {
        store?.let {
            val calendar = Calendar.getInstance()
            var day = calendar[Calendar.DAY_OF_WEEK] - 1
            if (day < 0) {
                day = 0
            }
            val pickupHoursAvailable =
                store.attributes.openingHoursPickup != null && day < store.attributes.openingHoursPickup.size
            val deliverHoursAvailable =
                store.attributes.openingHoursDeliver != null && day < store.attributes.openingHoursDeliver.size
            val hoursAvailable =
                store.attributes.openingHoursUtc != null && day < store.attributes.openingHoursUtc.size

            binding.orderAddressDetails.addressTime.visibility = View.VISIBLE
            if (!pickupHoursAvailable && !deliverHoursAvailable && hoursAvailable) {
                val openingHours = StringUtil.getOpeningHours(store)
                val openingFinal = if (!openingHours.isEmpty()) openingHours else "-"
                binding.orderAddressDetails.addressTime.text = openingFinal
            } else {
                if (pickupHoursAvailable) {
                    val hoursText = StringUtil.formatOpenHours(
                        store.attributes.openingHoursPickup[day].open,
                        store.attributes.openingHoursPickup[day].closed
                    )
                    binding.orderAddressDetails.addressTime.text = hoursText
                } else {
                    binding.orderAddressDetails.addressTime.visibility = View.GONE
                }
            }
        }
    }

    private fun showDeliveryDetails() {
        binding.apply {
            orderDetails?.let { order ->
                orderStore?.let { store ->
                    val orderType = OrderType.fromString(order.orderType)
                    orderAddressDetails.getDirection.isVisible = true
                    orderAddressDetails.addressKm.isVisible = true
                    orderAddressDetails.addressTime.isVisible = true
                    when (orderType) {
                        OrderType.DELIVERY -> {
                            orderAddress?.let { address ->
                                orderAddressDetails.addressKm.isVisible = false
                                orderAddressDetails.addressTime.isVisible = false
                                if (address.attributes?.label.isNullOrEmpty()) {
                                    orderAddressDetails.addressType.text =
                                        CacheUtil.getInstance().getTranslations(Text.OTHER, "Other")
                                } else {
                                    orderAddressDetails.addressType.text = address.attributes?.label
                                }
                                orderAddressDetails.addressName.text = address.attributes?.line1
                                orderAddressDetails.getDirection.isVisible = false
                                orderAddressDetails.deliveryDetailsLabel.text =
                                    CacheUtil.getInstance().getTranslations(
                                        Text.DELIVERY_DETAILS, "Delivery Details"
                                    )
                                orderAddressDetails.tellLabel.text =
                                    CacheUtil.getInstance().getTranslations(
                                        Text.TELL_THE_ORDER_NUMB_DELIVER,
                                        "Tell the order number to the rider to receive your order"
                                    )
                                orderAddressDetails.deliveryAddressLabel.text =
                                    CacheUtil.getInstance().getTranslations(
                                        Text.DELIVERY_ADDRESS_LABEL, "Delivery Address"
                                    )
                            }
                        }

                        OrderType.DINE_IN -> {
                            getBranchTime(store)
                            getDirection(store)
                            orderAddressDetails.addressType.text = store.attributes?.name
                            orderAddressDetails.addressName.text = store.attributes?.line1
                            orderAddressDetails.deliveryDetailsLabel.text =
                                CacheUtil.getInstance().getTranslations(
                                    Text.DINE_IN_DETAILS, "Dine In Details"
                                )
                            orderAddressDetails.tellLabel.text =
                                CacheUtil.getInstance().getTranslations(
                                    Text.TELL_THE_ORDER_NUM_PICKUP,
                                    "Tell the order number at the counter to pick you order"
                                )
                            orderAddressDetails.deliveryAddressLabel.text =
                                CacheUtil.getInstance().getTranslations(
                                    Text.DINEIN_ADDRESS, "Dine In Address"
                                )
                        }

                        else -> {
                            getBranchTime(store)
                            getDirection(store)
                            orderAddressDetails.addressType.text = store.attributes?.name
                            orderAddressDetails.addressName.text = store.attributes?.line1
                            orderAddressDetails.tellLabel.text = when (orderType) {
                                OrderType.CURBSIDE -> CacheUtil.getInstance().getTranslations(
                                    Text.TELL_THE_ORDER_NUMB_CURBSIDE,
                                    "Tell the order number if asked."
                                )

                                else -> CacheUtil.getInstance().getTranslations(
                                    Text.TELL_THE_ORDER_NUM_PICKUP,
                                    "Tell the order number at the counter to pick you order"
                                )
                            }
                            orderAddressDetails.deliveryDetailsLabel.text = CacheUtil.getInstance()
                                .getTranslations(Text.PICKUP_DETAILS, "Pickup Details")
                            orderAddressDetails.deliveryAddressLabel.text =
                                CacheUtil.getInstance().getTranslations(
                                    Text.PICKUP_ADDRESS, "Pickup Address"
                                )
                        }
                    }
                }
            }
        }
    }

    private fun processCurbside() {
        orderDetails?.let { order ->
            binding.orderCurbsideDetails.apply {
                if (OrderType.fromString(order.orderType) == OrderType.CURBSIDE) {
                    binding.orderCurbsideDetailsFrame.isVisible = true
                    serveOrder.setOnClickListener {
                        activity?.cast<BaseActivity> {
                            it.showLoadingDialog()
                        }
                        orderPresenter?.customerArrived(order.id)
                    }

                    if (order.curbsideStatus != null) {
                        if (order.curbsideStatus.code.equals(
                                "customer-arrived", ignoreCase = true
                            )
                        ) {
                            notServedLayout.visibility = View.GONE
                            servedLayout.visibility = View.VISIBLE
                        }
                    } else {
                        notServedLayout.visibility = View.VISIBLE
                        servedLayout.visibility = View.GONE
                    }

                    curbsideBrandImage.setImageResource(com.herfy.ordertype.R.drawable.ic_curbside_other)

                    if (order.customerCar != null) {
                        if (!order.customerCar.attributes.colorName.isNullOrEmpty()) {
                            curbsideVehicleColor.text =
                                "- " + order.customerCar.attributes.colorName
                        }
                        try {
                            if (!order.customerCar.attributes.carMakerImageUri.isNullOrEmpty()) {
                                Glide.with(curbsideBrandImage.context)
                                    .load(order.customerCar.attributes.carMakerImageUri)
                                    .apply(RequestOptions().override(80, 80)).skipMemoryCache(false)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .apply(RequestOptions.centerInsideTransform())
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
                                    }).into(curbsideBrandImage)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    curbsideBrandName.text = order.carMakerName
                    if (!order.carPlateNumber.isNullOrEmpty()) {
                        curbsidePlateNumber.text = "- " + order.carPlateNumber
                    }

                }
            }
        }
    }

    private fun showDetailsState() {
        orderDetails?.let { order ->
            binding.apply {
                orderCurbsideDetailsFrame.isVisible = false
                orderDriverDetailsFrame.isVisible = false
                when (OrderType.fromString(order.orderType)) {
                    OrderType.DELIVERY -> {
                        orderDriverDetailsFrame.isVisible = true
                    }

                    OrderType.CURBSIDE -> {
                        orderCurbsideDetailsFrame.isVisible = false
                    }

                    else -> {}
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showDriverDetails() {
        orderDetails?.let { order ->
            orderAddress?.let { address ->
                binding.apply {
                    if (OrderType.fromString(order.orderType) == OrderType.DELIVERY) {
                        statusDriverDetails.deliveryInstructionLabel.text =
                            CacheUtil.getInstance().getTranslations(
                                Text.DELIVERY_INSTRUCTION_LABEL, "Delivery Instruction"
                            )
                        statusDriverDetails.driverInstruction.text =
                            address.attributes?.instructions
                        statusDriverDetails.yourRiderLabel.text =
                            CacheUtil.getInstance().getTranslations(Text.YOUR_RIDER, "Your rider")
                        statusDriverDetails.yourRiderDescription.text =
                            CacheUtil.getInstance().getTranslations(
                                Text.A_RIDER_WILL_BE,
                                "A rider will be assigned to you when order is ready. You will be able to track your order in real time"
                            )
                        statusDriverDetails.callRider.text =
                            CacheUtil.getInstance().getTranslations(
                                Text.CALL, "Call"
                            )
                        statusDriverDetails.messageRider.text =
                            CacheUtil.getInstance().getTranslations(
                                Text.MESSAGE, "Message"
                            )
                        updateDriverFromOrder()
                    }
                }
            }
        }
    }

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private fun requestCallPermission(number: String) {
        activity?.cast<MainActivity> { mainActivity ->
            if (ActivityCompat.checkSelfPermission(
                    mainActivity, Manifest.permission.CALL_PHONE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            } else mainActivity.dialPhoneNumber(number = number)
        }
    }

    private fun callWhatsApp(number: String) {
        activity?.cast<MainActivity> { mainActivity ->
            val whatsAppPackageName = "com.whatsapp"
            try {
                val packageManager = mainActivity.packageManager
                val intent = Intent(Intent.ACTION_VIEW)
                val url = "https://api.whatsapp.com/send?phone=" + number.trim { it <= ' ' }
                intent.setPackage(whatsAppPackageName)
                intent.data = Uri.parse(url)
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW, Uri.parse(
                                "market://details?id=$whatsAppPackageName"
                            )
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW, Uri.parse(
                            "market://details?id=$whatsAppPackageName"
                        )
                    )
                )
            }
        }
    }

    private fun showOrderStatus(orderStatus: OrderStatus?) {
        binding.statusTrackerLayout.apply {
            preparingLabel.text =
                CacheUtil.getInstance().getTranslations(Text.PREPARING, "Preparing")
            orderDetails?.let { order ->
                orderStatus?.let { status ->
                    val ot = OrderType.fromString(order.orderType)
                    if (ot == OrderType.DELIVERY) {
                        onWayLabel.text =
                            CacheUtil.getInstance().getTranslations(Text.ONWAY, "On Way")
                        finishedLabel.text =
                            CacheUtil.getInstance().getTranslations(Text.DELIVERED, "Delivered")
                    } else if (ot == OrderType.CURBSIDE) {
                        onWayLabel.text =
                            CacheUtil.getInstance().getTranslations(Text.READY, "Ready")
                        finishedLabel.text =
                            CacheUtil.getInstance().getTranslations(Text.SERVED, "Served")
                    } else {
                        onWayLabel.text = CacheUtil.getInstance()
                            .getTranslations(Text.READY_FOR_PICKUP, "Ready for pickup")
                        finishedLabel.text =
                            CacheUtil.getInstance().getTranslations(Text.PICKED, "Picked")
                    }

                    statusViewSecond.setBackgroundColor(resources.getColor(com.herfy.ordertracker.R.color.status_done))
                    statusViewThird.setBackgroundColor(resources.getColor(com.herfy.ordertracker.R.color.status_done))

                    if (ot == OrderType.DELIVERY) {
                        if (status.sequence <= OrderStatusType.DISPATCHED.value) {
                            updateStatusGIF(R.drawable.make_order)

                            statusViewSecond.setBackgroundColor(resources.getColor(com.herfy.ordertracker.R.color.status_not))
                            statusViewThird.setBackgroundColor(resources.getColor(com.herfy.ordertracker.R.color.status_not))

                            frameIconPreparing.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_enabled_status)
                            imageIconPreparing.setImageResource(com.herfy.ordertracker.R.drawable.ic_preparing_not_yet)
                            preparingLayout.setBackgroundResource(R.drawable.background_current_status)
                            preparingLabel.setTextColor(resources.getColor(R.color.text_status_color_current))
                            preparingTime.setTextColor(resources.getColor(R.color.time_status_color_current))

                            frameIconOnWay.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_disabled_status)
                            imageIconOnWay.setImageResource(com.herfy.ordertracker.R.drawable.ic_delivery_done)
                            onWayLayout.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_cards)
                            onWayLabel.setTextColor(resources.getColor(R.color.text_status_color_past))
                            onWayTime.setTextColor(resources.getColor(R.color.time_status_color_past))

                            frameIconFinished.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_disabled_status)
                            imageIconFinishes.setImageResource(com.herfy.ordertracker.R.drawable.ic_order_not_yet)
                            finishedLayout.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_cards)
                            finishedLabel.setTextColor(resources.getColor(R.color.text_status_color_past))
                            finishedTime.setTextColor(resources.getColor(R.color.time_status_color_past))

                        } else if (status.sequence >= OrderStatusType.DELIVERY_IN_PROGRESS.value && status.sequence <= OrderStatusType.ARRIVED_AT_CUSTOMER.value) {
                            updateStatusGIF(R.drawable.delivered_order)

                            frameIconPreparing.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_disabled_status)
                            imageIconPreparing.setImageResource(com.herfy.ordertracker.R.drawable.ic_preparing_done)
                            preparingLayout.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_past_status)
                            preparingLabel.setTextColor(resources.getColor(R.color.text_status_color_past))
                            preparingTime.setTextColor(resources.getColor(R.color.time_status_color_past))

                            frameIconOnWay.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_enabled_status)
                            imageIconOnWay.setImageResource(com.herfy.ordertracker.R.drawable.ic_delivery_not_yet)
                            onWayLayout.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_current_status)
                            onWayLabel.setTextColor(resources.getColor(R.color.text_status_color_current))
                            onWayTime.setTextColor(resources.getColor(R.color.time_status_color_current))

                            frameIconFinished.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_disabled_status)
                            imageIconFinishes.setImageResource(com.herfy.ordertracker.R.drawable.ic_order_not_yet)
                            finishedLayout.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_cards)
                            finishedLabel.setTextColor(resources.getColor(R.color.text_status_color_past))
                            finishedTime.setTextColor(resources.getColor(R.color.time_status_color_past))

                        } else {
                            binding.cookingImage.setImageResource(com.herfy.ordertracker.R.drawable.ic_order_done)

                            frameIconPreparing.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_disabled_status)
                            imageIconPreparing.setImageResource(com.herfy.ordertracker.R.drawable.ic_preparing_done)
                            preparingLayout.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_past_status)
                            preparingLabel.setTextColor(resources.getColor(R.color.text_status_color_past))
                            preparingTime.setTextColor(resources.getColor(R.color.time_status_color_past))

                            frameIconOnWay.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_disabled_status)
                            imageIconOnWay.setImageResource(com.herfy.ordertracker.R.drawable.ic_delivery_done)
                            onWayLayout.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_past_status)
                            onWayLabel.setTextColor(resources.getColor(R.color.text_status_color_past))
                            onWayTime.setTextColor(resources.getColor(R.color.time_status_color_past))

                            frameIconFinished.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_enabled_status)
                            imageIconFinishes.setImageResource(com.herfy.ordertracker.R.drawable.ic_order_done)
                            finishedLayout.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_current_status)
                            finishedLabel.setTextColor(resources.getColor(R.color.text_status_color_current))
                            finishedTime.setTextColor(resources.getColor(R.color.time_status_color_current))

                        }
                    } else {
                        if (status.sequence <= OrderStatusType.IN_KITCHEN.value) {
                            updateStatusGIF(R.drawable.make_order)
                            statusViewSecond.setBackgroundColor(resources.getColor(com.herfy.ordertracker.R.color.status_not))
                            statusViewThird.setBackgroundColor(resources.getColor(com.herfy.ordertracker.R.color.status_not))

                            frameIconPreparing.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_enabled_status)
                            imageIconPreparing.setImageResource(com.herfy.ordertracker.R.drawable.ic_preparing_not_yet)
                            preparingLayout.setBackgroundResource(R.drawable.background_current_status)
                            preparingLabel.setTextColor(resources.getColor(R.color.text_status_color_current))
                            preparingTime.setTextColor(resources.getColor(R.color.time_status_color_current))

                            frameIconOnWay.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_disabled_status)
                            imageIconOnWay.setImageResource(com.herfy.ordertracker.R.drawable.ic_pickup_ready_done)
                            onWayLayout.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_cards)
                            onWayLabel.setTextColor(resources.getColor(R.color.text_status_color_past))
                            onWayTime.setTextColor(resources.getColor(R.color.time_status_color_past))

                            frameIconFinished.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_disabled_status)
                            imageIconFinishes.setImageResource(com.herfy.ordertracker.R.drawable.ic_order_not_yet)
                            finishedLayout.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_cards)
                            finishedLabel.setTextColor(resources.getColor(R.color.text_status_color_past))
                            finishedTime.setTextColor(resources.getColor(R.color.time_status_color_past))

                        } else if (status.sequence >= OrderStatusType.READY.value && status.sequence <= OrderStatusType.ARRIVED_AT_CUSTOMER.value) {
                            updateStatusGIF(R.drawable.collected_order)
                            frameIconPreparing.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_disabled_status)
                            imageIconPreparing.setImageResource(com.herfy.ordertracker.R.drawable.ic_preparing_done)
                            preparingLayout.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_past_status)
                            preparingLabel.setTextColor(resources.getColor(R.color.text_status_color_past))
                            preparingTime.setTextColor(resources.getColor(R.color.time_status_color_past))

                            frameIconOnWay.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_enabled_status)
                            imageIconOnWay.setImageResource(com.herfy.ordertracker.R.drawable.ic_pickup_ready)
                            onWayLayout.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_current_status)
                            onWayLabel.setTextColor(resources.getColor(R.color.text_status_color_current))
                            onWayTime.setTextColor(resources.getColor(R.color.time_status_color_current))

                            frameIconFinished.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_disabled_status)
                            imageIconFinishes.setImageResource(com.herfy.ordertracker.R.drawable.ic_order_not_yet)
                            finishedLayout.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_cards)
                            finishedLabel.setTextColor(resources.getColor(R.color.text_status_color_past))
                            finishedTime.setTextColor(resources.getColor(R.color.time_status_color_past))

                        } else {
                            binding.cookingImage.setImageResource(R.drawable.ic_order_done)
                            frameIconPreparing.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_disabled_status)
                            imageIconPreparing.setImageResource(com.herfy.ordertracker.R.drawable.ic_preparing_done)
                            preparingLayout.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_past_status)
                            preparingLabel.setTextColor(resources.getColor(R.color.text_status_color_past))
                            preparingTime.setTextColor(resources.getColor(R.color.time_status_color_past))

                            frameIconOnWay.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_disabled_status)
                            imageIconOnWay.setImageResource(com.herfy.ordertracker.R.drawable.ic_pickup_ready_done)
                            onWayLayout.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_past_status)
                            onWayLabel.setTextColor(resources.getColor(R.color.text_status_color_past))
                            onWayTime.setTextColor(resources.getColor(R.color.time_status_color_past))

                            frameIconFinished.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_enabled_status)
                            imageIconFinishes.setImageResource(com.herfy.ordertracker.R.drawable.ic_order_done)
                            finishedLayout.setBackgroundResource(com.herfy.ordertracker.R.drawable.background_current_status)
                            finishedLabel.setTextColor(resources.getColor(R.color.text_status_color_current))
                            finishedTime.setTextColor(resources.getColor(R.color.time_status_color_current))

                        }
                    }

                    if (order.orderStatus.sequence >= OrderStatusType.CANCELLED_BY_CUSTOMER.value) {
                        if (order.orderStatus.sequence >= OrderStatusType.CANCELLED_BY_CUSTOMER.value && order.orderStatus.sequence <= OrderStatusType.CANCELLED_BY_EMPLOYEE.value) {
                            binding.cancelledText.text =
                                CacheUtil.getInstance().getTranslations(Text.CANCELLED, "Cancelled")
                            finishedLabel.text =
                                CacheUtil.getInstance().getTranslations(Text.CANCELLED, "Cancelled")
                        } else {
                            binding.cancelledText.text =
                                CacheUtil.getInstance().getTranslations(Text.FAILED, "Failed")
                            finishedLabel.text =
                                CacheUtil.getInstance().getTranslations(Text.FAILED, "Failed")
                        }
                        binding.cookingImage.setImageResource(R.drawable.ic_cancelled_order)
                        imageIconFinishes.setImageResource(com.herfy.ordertracker.R.drawable.ic_cancelled_order)
                        binding.estimateLayout.isVisible = false
                        binding.cancelledText.isVisible = true

                    } else if (order.orderStatus.sequence == OrderStatusType.FINISHED.value) {
                        if (ot == OrderType.DELIVERY) {
                            binding.cancelledText.text =
                                CacheUtil.getInstance().getTranslations(Text.DELIVERED, "Delivered")
                        } else {
                            binding.cancelledText.text =
                                CacheUtil.getInstance().getTranslations(Text.PICKED, "Picked")
                        }
                        binding.estimateLayout.isVisible = false
                        binding.cancelledText.isVisible = true
                    } else {
                        binding.estimateLayout.isVisible = true
                        binding.cancelledText.isVisible = false
                    }
                }
            }
        }

    }

    private fun initTimer() {
        countDownTimer = object : CountDownTimer(countDownTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countDownTime = millisUntilFinished
                binding.orderCancelDetails.timerCancel.text =
                    formatSecondsToMinutesAndSeconds((countDownTime / 1000))
            }

            override fun onFinish() {
                binding.orderCancelFrame.isVisible = false
            }
        }
        countDownTimer?.start()
    }

    private fun processCancel() {
        val time = CacheUtil.getInstance().concept.orderCancellationTime
        val maxStatus = CacheUtil.getInstance().concept.orderCancellationMaxStatus

        orderDetails?.let { order ->
            if (time > 0 && maxStatus > order.orderStatus.sequence) {
                val difference = cancelTime(order.createdAt)
                val totalCancelTime = time * 60
                if (difference >= totalCancelTime) {
                    binding.orderCancelFrame.isVisible = false
                } else {
                    binding.orderCancelDetails.cancelOrder.setOnClickListener {
                        CancelOrderDialogFragment.newInstance(onAction = this@OrderNewTrackerFragment)
                            .show(
                                parentFragmentManager,
                                CancelOrderDialogFragment::class.java.toString()
                            )
                    }
                    countDownTime = (totalCancelTime - difference) * 1000
                    binding.orderCancelFrame.isVisible = true
                    initTimer()
                }
            } else {
                binding.orderCancelFrame.isVisible = false
            }
        }
    }

    private fun showOrderHelpDetails() {
        binding.apply {
            val mobile = CacheUtil.getInstance().concept.whatsappNumber
            if (StringUtil.isValidPhoneNumber(mobile)) {
                binding.orderHelpDetailsFrame.isVisible = true
                orderHelpDetails.callHelpLayout.setOnClickListener {
                    numberToCall = mobile
                    requestCallPermission(numberToCall)
                }
                orderHelpDetails.whatsAppHelpLayout.setOnClickListener {
                    callWhatsApp(mobile)
                }
            } else {
                binding.orderHelpDetailsFrame.isVisible = false
            }
        }
    }

    override fun showOrder(order: OrderDetails?, store: Store?, address: Address?) {
        dismissDialogs()
        order?.let { item ->
            orderDetails = order
            orderStore = store
            orderAddress = address
            binding.apply {
                blocker.gone()
                showStoreMarker()
                showStoreDeliveryTime()
                showDeliveryDetails()
                showDetailsState()
                showDriverDetails()
                processCurbside()
                processCancel()
                orderAddressDetails.orderNumberLabel.text =
                    CacheUtil.getInstance().getTranslations(Text.ORDER_NUM, "Order #")
                orderAddressDetails.orderNumber.text = getDisplayId(item)

                orderPaymentDetails.priceSubtotal.text = StringUtil.getNewFormattedPrice(
                    item.subtotal, true
                )

                if (item.deliveryCharge > 0) {
                    orderPaymentDetails.deliverContainer.isVisible = true
                    orderPaymentDetails.deliverTotal.text = StringUtil.getNewFormattedPrice(
                        item.deliveryCharge, true
                    )
                } else {
                    orderPaymentDetails.deliverContainer.isVisible = false
                }

                if (item.discount > 0) {
                    orderPaymentDetails.discountContainer.isVisible = true
                    orderPaymentDetails.discountTotal.text = StringUtil.getNewFormattedPrice(
                        item.discount, true
                    )
                } else {
                    orderPaymentDetails.discountContainer.isVisible = false
                }

                orderPaymentDetails.totalPrice.text = StringUtil.getNewFormattedPrice(
                    item.total, true
                )
                totalPrice.text = StringUtil.getNewFormattedPrice(
                    item.total, true
                )
            }
        }
    }

    private fun getLastStatus(orderStatuses: ArrayList<OrderStatus>?, sequence: Int): String {
        var time = "--"
        orderStatuses?.let { statuses ->
            for (status in statuses) {
                if (status.sequence <= sequence) {
                    if (status.isDone && status.time.isNotEmpty()) {
                        time = (status.time)
                    }
                }
            }
        }
        return statusTime(time)
    }

    private fun getCurrentStatus(orderStatuses: ArrayList<OrderStatus>?): OrderStatus? {
        orderStatuses?.let {
            for (status in orderStatuses) {
                if (status.isCurrent && status.isDone) {
                    return status
                }
            }
        }
        return null
    }

    override fun showOrderStatuses(orderStatuses: ArrayList<OrderStatus>?, isDone: Boolean) {
        dismissDialogs()
        binding.statusTrackerLayout.apply {
            orderDetails?.let { order ->
                var currentStatus = getCurrentStatus(orderStatuses)
                if (currentStatus == null) {
                    currentStatus = order.orderStatus
                }
                showOrderStatus(currentStatus)
                val sequence = currentStatus?.sequence ?: -1
                val maxStatus = CacheUtil.getInstance().concept.orderCancellationMaxStatus
                if (maxStatus <= order.orderStatus.sequence) {
                    binding.orderCancelFrame.isVisible = false
                }
                val ot = OrderType.fromString(order.orderType)
                if (ot == OrderType.DELIVERY) {
                    preparingTime.text =
                        getLastStatus(orderStatuses, OrderStatusType.DISPATCHED.value)
                    if (sequence <= OrderStatusType.DISPATCHED.value) {
                        onWayTime.text = "--"
                        finishedTime.text = "--"
                    } else if (sequence >= OrderStatusType.DELIVERY_IN_PROGRESS.value && sequence <= OrderStatusType.ARRIVED_AT_CUSTOMER.value) {
                        onWayTime.text =
                            getLastStatus(orderStatuses, OrderStatusType.ARRIVED_AT_CUSTOMER.value)
                        finishedTime.text = "--"
                    } else {
                        onWayTime.text =
                            getLastStatus(orderStatuses, OrderStatusType.ARRIVED_AT_CUSTOMER.value)
                        finishedTime.text =
                            getLastStatus(orderStatuses, OrderStatusType.FINISHED.value)
                    }
                } else {
                    preparingTime.text =
                        getLastStatus(orderStatuses, OrderStatusType.IN_KITCHEN.value)
                    if (sequence <= OrderStatusType.IN_KITCHEN.value) {
                        onWayTime.text = "--"
                        finishedTime.text = "--"
                    } else if (sequence >= OrderStatusType.READY.value && sequence <= OrderStatusType.ARRIVED_AT_CUSTOMER.value) {
                        if (!isOverHeadFrameShows && orderPresenter?.fromAbly == true) {
                            showOverHeadFrame()
                        }
                        onWayTime.text =
                            getLastStatus(orderStatuses, OrderStatusType.ARRIVED_AT_CUSTOMER.value)
                        finishedTime.text = "--"
                    } else {
                        onWayTime.text =
                            getLastStatus(orderStatuses, OrderStatusType.ARRIVED_AT_CUSTOMER.value)
                        finishedTime.text =
                            getLastStatus(orderStatuses, OrderStatusType.FINISHED.value)
                    }
                }
            }
        }
    }

    private fun showOverHeadFrame() {
        orderPresenter?.setFromAblyToFalse()
        isOverHeadFrameShows = true
        binding.overHeadFrame.isVisible = true
        binding.orderStatusOverhead.pickupTime.text = getCurrentTime()
        Handler(Looper.getMainLooper()).postDelayed({
            binding.overHeadFrame.isVisible = false
        }, 3000)
    }

    override fun showMenuItems(order: OrderDetails?, menuItems: ArrayList<MenuItem>?) {
        menuItems?.let {
            val count = it.size
            orderMenuItemsAdapter.updateList(it)
            binding.yourOrderLabel.text =
                CacheUtil.getInstance().getTranslations(Text.YOUR_ORDER_TEMPLATE, "Your Order (x)")
                    .replace("(x)", "$count")

        }
    }

    override fun setTimeLine(isCancelled: Boolean, orderStatus: OrderStatus?, noDriver: Boolean) {

    }

    override fun setMenuItem(menuItem: MenuItem?) {

    }

    override fun showCart() {

    }

    override fun updateStatus(orderDetails: OrderDetails?, orderStatus: OrderStatus?) {
        this.orderDetails = orderDetails
        orderPresenter?.getOrderStatuses()
    }

    override fun showDriver(customerLatLng: LatLng?, driverLatLng: LatLng?) {
        updateDriverFromOrder()
        updateDriverLocation(driverLatLng)
    }

    @SuppressLint("SetTextI18n")
    private fun updateDriverFromOrder() {
        binding.statusDriverDetails.apply {
            val driver = orderPresenter?.driverDetail
            if (driver != null) {
                if (!isOverHeadFrameShows && orderPresenter?.fromAbly == true) {
                    showOverHeadFrame()
                }
                withDriverLayout.isVisible = true
                noDriverLayout.isVisible = false
                riderMotor.text = driver.plateNumber
                riderName.text = "${driver.employee.firstName} ${driver.employee.lastName}"
                driverInitial.text =
                    "${driver.employee.firstName.first()}${driver.employee.lastName.first()}"
                checkDriverMobile(driver.employee.mobile)
            } else {
                noDriverLayout.isVisible = true
                withDriverLayout.isVisible = false
            }
        }
    }

    private fun updateDriverLocation(latLng: LatLng?) {
        activity?.runOnUiThread {
            latLng?.let {
                try {
                    val activity: Activity? = activity
                    val inflater =
                        activity?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    val view =
                        inflater.inflate(com.herfy.ordertracker.R.layout.order_driver_marker, null)
                    val textView =
                        view.findViewById<TextView>(com.herfy.ordertracker.R.id.kmDriverAway)
                    var distanceValue = ""
                    try {
                        val currentLocation = userLatLng
                        val currentDistance = currentLocation?.latitude?.let {
                            currentLocation.longitude.let { it1 ->
                                latLng.latitude.let { it2 ->
                                    latLng.longitude.let { it3 ->
                                        DistanceCalculator.calculateDistance(
                                            it, it1, it2, it3
                                        )
                                    }
                                }
                            }
                        }

                        distanceValue = StringUtil.getDistance(
                            currentDistance ?: 0.0
                        )

                        textView.text =
                            CacheUtil.getInstance().getTranslations(Text.KM_AWAY, "(x) away")
                                .replace("(x)", distanceValue).lowercase(Locale.getDefault())
                        if (currentDistance != null) {
                            if (currentDistance <= 0.0) {
                                textView.isInvisible = true
                            } else {
                                textView.isVisible = true
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    Handler(Looper.getMainLooper()).post {
                        if (driverMarker != null) {
                            driverMarker?.remove()
                        }
                        val markerOptions = MarkerOptions()
                        markerOptions.position(latLng)
                        markerOptions.icon(
                            BitmapDescriptorFactory.fromBitmap(
                                ViewUtil.createDrawableFromView(
                                    activity, view
                                )
                            )
                        )
                        markerOptions.anchor(0.5f, 1f)
                        driverMarker = googleMap?.addMarker(markerOptions)
                        deletePolyline()
                        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11f))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun updateDriver(appDriver: AppDriver) {
        Log.e("ADADADADADADAD", "ADADADADAD:" + Gson().toJson(appDriver))
        try {
            activity?.runOnUiThread {
                activity?.cast<MainActivity> {
                    binding.statusDriverDetails.apply {
                        withDriverLayout.isVisible = true
                        noDriverLayout.isVisible = false
                        val driver = appDriver.driver
                        val driverOrder = orderPresenter?.driverDetail
                        riderMotor.text = driver.licensePlate.orEmpty()
                        riderName.text = driver.name.orEmpty()
                        if (driverOrder != null) {
                            driverInitial.isVisible = true
                            driverInitial.text =
                                "${driverOrder.employee.firstName.first()}${driverOrder.employee.lastName.first()}"
                        } else {
                            driverInitial.isVisible = false
                        }
                        Glide.with(it).load(driver.photoURL.orEmpty())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .apply(RequestOptions.centerInsideTransform())
                            .listener(object : RequestListener<Drawable?> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any,
                                    target: Target<Drawable?>,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    driverImage.visibility = View.GONE
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable?,
                                    model: Any,
                                    target: Target<Drawable?>,
                                    dataSource: DataSource,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    driverImage.visibility = View.VISIBLE
                                    return false
                                }
                            }).into(driverImage)
                        checkDriverMobile(driver.mobile)
                        updateDriverLocation(
                            LatLng(
                                (appDriver.bearing.lat ?: "0").toDouble(),
                                (appDriver.bearing.lng ?: "0").toDouble()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkDriverMobile(mobile: String?) {
        activity?.runOnUiThread {
            activity?.cast<MainActivity> {
                binding.statusDriverDetails.apply {
                    if (mobile.orEmpty().isNotBlank()) {
                        callRider.isVisible = true
                        messageRider.isVisible = true
                        callRider.setOnClickListener {
                            numberToCall = mobile.orEmpty()
                            requestCallPermission(numberToCall)
                        }
                        messageRider.setOnClickListener {
                            callWhatsApp(mobile.orEmpty())
                        }
                    } else {
                        callRider.isVisible = false
                        messageRider.isVisible = false
                    }
                }
            }
        }
    }

    override fun cancel() {
        orderPresenter?.getOrderStatuses()
    }

    override fun showRatings(orderStatus: OrderStatus?, ratings: MutableList<Rating>?) {

    }

    override fun showMessage(message: String?) {
        activity?.cast<BaseActivity> {
            it.showToast(message.orEmpty())
        }
    }

    override fun customerArrivedSuccess() {
        activity?.cast<BaseActivity> {
            it.dismissDialogs()
        }
        binding.orderCurbsideDetails.servedLayout.isVisible = true
        binding.orderCurbsideDetails.notServedLayout.isVisible = false
    }

    override fun logFirebaseEvent(event: String?) {

    }

    override fun getFromCache(key: String?): String? {
        return null
    }

    override fun showWhatsApp(mobileNumber: String?) {

    }

    override fun onClose() {

    }

    override fun onDelete() {
        if (binding.orderCancelFrame.isVisible) {
            activity?.cast<BaseActivity> {
                it.showLoadingDialog()
            }
            orderPresenter?.cancel()
        }
    }

    private fun updateStatusGIF(image: Int) {
        try {
            activity?.cast<BaseActivity> {
                Glide.with(it).asGif().load(image)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.cookingImage)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
