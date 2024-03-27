package com.herfy.order.ordertrackernew

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
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
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.common.showTime
import com.herfy.ext.cast
import com.herfy.history.OrdersContract
import com.herfy.history.OrdersPresenter
import com.herfy.history.historynew.adapter.OrderMenuItemsAdapter
import com.herfy.ordertracker.databinding.FragmentOrderNewTrackerBinding
import com.herfy.util.AppLogger
import com.herfy.util.StringUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DialogUtils
import com.herfy.utils.helper.DistanceCalculator
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.ViewUtil
import com.location.FusedLocationProvider
import com.skylinedynamics.solosdk.api.models.objects.Address
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Rating
import com.skylinedynamics.solosdk.api.models.objects.Store
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.Locale


@AndroidEntryPoint
class OrderNewTrackerFragment : BaseFragment(), OrdersContract.View {

    private var orderPresenter: OrdersContract.Presenter? = null

    private lateinit var binding: FragmentOrderNewTrackerBinding

    private var googleMap: GoogleMap? = null

    private var locationMarker: Marker? = null

    private var driverMarker: Marker? = null

    private var storeMarker: Marker? = null

    private val args: OrderNewTrackerFragmentArgs by navArgs()

    private var userLatLng: LatLng? = null

    private var userStore: Store? = null

    private lateinit var orderMenuItemsAdapter: OrderMenuItemsAdapter

    private var isSummaryHidden = true

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

    @SuppressLint("ClickableViewAccessibility")
    override fun setupViews() {
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

            orderHelpDetails.callHelpLayout.setOnClickListener {
                // TODO call
            }

            orderHelpDetails.whatsAppHelpLayout.setOnClickListener {
                // TODO whatsapp
            }

            paymentSummaryFrame.setOnClickListener {
                if (isSummaryHidden) {
                    orderPaymentDetailsFrame.isVisible = true
                    isSummaryHidden = false
                } else {
                    isSummaryHidden = true
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
            orderPaymentDetails.subtotalLabel.text =
                CacheUtil.getInstance().getTranslations(Text.SUBTOTAL, "Subtotal")
            orderPaymentDetails.deliverLabel.text =
                CacheUtil.getInstance().getTranslations(Text.DELIVERY_CHARGE, "Delivery Charges")
            orderPaymentDetails.discountLabel.text =
                CacheUtil.getInstance().getTranslations(Text.DISCOUNT, "Discount")
            totalLabel.text = CacheUtil.getInstance().getTranslations(Text.TOTAL_LABEL, "Total")
            paymentSummaryLabel.text =
                CacheUtil.getInstance().getTranslations(Text.PAYMENT_SUMMARY, "Payment Summary")
            orderPaymentDetails.paymentSummaryLabel.text =
                CacheUtil.getInstance().getTranslations(Text.PAYMENT_SUMMARY, "Payment Summary")
            orderPaymentDetails.totalLabel.text =
                CacheUtil.getInstance().getTranslations(Text.TOTAL_LABEL, "Total")
            orderHelpDetails.haveATroubleLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.HAVE_A_TROUBLE, "Have a trouble with your order?")
            orderHelpDetails.herfySupportLabel.text = CacheUtil.getInstance().getTranslations(
                Text.HERFY_SUPPORT_TEAM, "Herfy’s Support team is here to help you."
            )
            orderHelpDetails.callHelpLabel.text =
                CacheUtil.getInstance().getTranslations(Text.CALL, "Call")
            orderHelpDetails.whatsAppHelpMessageLabel.text =
                CacheUtil.getInstance().getTranslations(Text.WHATSAPP_MESSAGE, "Whatsapp Message")
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
            userStore?.let {
                showStoreMarker(userStore, false)
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

    private fun showStoreMarker(store: Store?) {
        showStoreMarker(store, true)
    }

    private fun showStoreMarker(store: Store?, isMoveCamera: Boolean) {
        store?.let { store ->
            try {
                userStore = store
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
                            .replace("(x)", distanceValue).toLowerCase()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (distanceValue.isNullOrEmpty()) {
                    textView.isVisible = false
                }
                view.setOnClickListener {
                    goToStoreInMap(store)
                }

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
                markerOptions.title(store.toString())
                storeMarker = googleMap?.addMarker(markerOptions)
                if (isMoveCamera) {
                    latLng?.let { CameraUpdateFactory.newLatLngZoom(it, 11f) }
                        ?.let { googleMap?.animateCamera(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showStoreDeliveryTime(store: Store?, order: OrderDetails?) {
        if (order != null) {
            store?.let {
                val orderType = OrderType.fromString(order.orderType)
                if (orderType == OrderType.DELIVERY) {
                    binding.estimatedLabel.text = CacheUtil.getInstance()
                        .getTranslations(Text.ESTIMATED_DELIVERY_TIME, "Estimated delivery time")
                    binding.timeDuration.text = showTime(
                        order.createdAt.orEmpty(), store.attributes.promisedTimeDelta.delivery
                    )
                } else if (orderType == OrderType.CURBSIDE) {
                    binding.estimatedLabel.text = CacheUtil.getInstance()
                        .getTranslations(Text.ESTIMATE_PICKUP_TIME, "Estimate pickup time")
                    binding.timeDuration.text = showTime(
                        order.createdAt.orEmpty(), store.attributes.promisedTimeDelta.pickup
                    )
                } else {
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
                val activity: Activity? = getActivity()
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
                binding.orderAddressDetails.addressTime.setText(openingFinal)
            } else {
                if (pickupHoursAvailable) {
                    val hoursText = StringUtil.formatOpenHours(
                        store.attributes.openingHoursPickup[day].open,
                        store.attributes.openingHoursPickup[day].closed
                    )
                    binding.orderAddressDetails.addressTime.setText(hoursText)
                } else {
                    binding.orderAddressDetails.addressTime.setVisibility(View.GONE)
                }
            }
        }
    }

    private fun getDistanceCount(store: Store?) {
        store?.let {
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
                val distanceValue = StringUtil.getDistance(
                    currentDistance ?: 0.0
                )
                binding.orderAddressDetails.addressKm.text = distanceValue.toLowerCase()
                if (distanceValue.isNullOrEmpty()) {
                    binding.orderAddressDetails.addressKm.text = ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showDeliveryDetails(order: OrderDetails?, store: Store?, address: Address?) {
        binding.apply {
            order?.let {
                val orderType = OrderType.fromString(order.orderType)
                orderAddressDetails.getDirection.isVisible = true
                orderAddressDetails.addressKm.isVisible = true
                orderAddressDetails.addressTime.isVisible = true
                when (orderType) {
                    OrderType.DELIVERY -> {
                        orderAddressDetails.addressKm.isVisible = false
                        orderAddressDetails.addressTime.isVisible = false
                        orderAddressDetails.addressType.text = address?.attributes?.label
                        orderAddressDetails.addressName.text = address?.attributes?.line1
                        orderAddressDetails.getDirection.isVisible = false
                        orderAddressDetails.deliveryDetailsLabel.text = CacheUtil.getInstance()
                            .getTranslations(Text.DELIVERY_DETAILS, "Delivery Details")
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

                    OrderType.DINE_IN -> {
                        getBranchTime(store)
                        getDirection(store)
                        getDistanceCount(store)
                        orderAddressDetails.addressType.text = store?.attributes?.name
                        orderAddressDetails.addressName.text = store?.attributes?.line1
                        orderAddressDetails.deliveryDetailsLabel.text = CacheUtil.getInstance()
                            .getTranslations(Text.DINE_IN_DETAILS, "Dine In Details")
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
                        getDistanceCount(store)
                        orderAddressDetails.tellLabel.text = when (orderType) {
                            OrderType.CURBSIDE -> CacheUtil.getInstance().getTranslations(
                                Text.TELL_THE_ORDER_NUMB_CURBSIDE, "Tell the order number if asked."
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

    override fun showOrder(order: OrderDetails?, store: Store?, address: Address?) {
        dismissDialogs()
        order?.let { item ->
            binding.apply {
                blocker.gone()
                Glide.with(requireActivity()).asGif().load(R.drawable.cooking)
                    .transition(DrawableTransitionOptions.withCrossFade()).into(cookingImage)
                showStoreMarker(store)
                showStoreDeliveryTime(store, order)
                showDeliveryDetails(order, store, address)

                orderAddressDetails.orderNumberLabel.text =
                    CacheUtil.getInstance().getTranslations(Text.ORDER_NUM, "Order #")
                orderAddressDetails.orderNumber.text = item.id

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

    override fun showOrderStatuses(orderStatuses: ArrayList<OrderStatus>?, isDone: Boolean) {

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
