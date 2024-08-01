package com.herfy.checkout

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.ImageView
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.checkout.tokenization.model.TokenDetails
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.CancelableCallback
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.model.mutation.Overlay
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.herfy.BuildConfig
import com.herfy.R
import com.herfy.analytics.facebook.FacebookAppEventUtil
import com.herfy.analytics.firebase.FirebaseECommerceEventUtil
import com.herfy.analytics.firebase.FirebaseECommerceEventUtil.Companion.addPaymentInfoEvent
import com.herfy.analytics.firebase.FirebaseService
import com.herfy.base.BaseActivity
import com.herfy.checkout.databinding.ActivityCheckoutPageBinding
import com.herfy.common.base.OrderSupportMapFragment
import com.herfy.common.drawArcPolyline
import com.herfy.decorator.BeforeDateDecorator
import com.herfy.decorator.OneDayDecorator
import com.herfy.history.adapters.OrderServiceChargeRecyclerViewAdapter
import com.herfy.main.MainActivity
import com.herfy.main.MainActivity.Companion.currentMainActivity
import com.herfy.order.views.OrderTypeActivity
import com.herfy.payment.PaymentContract
import com.herfy.payment.PaymentPresenter
import com.herfy.payment.savedcards.CVVDialogAlert
import com.herfy.payment.savedcards.CVVDialogAlert.OnCVVAction
import com.herfy.payment.savedcards.SaveDialogAlert
import com.herfy.payment.savedcards.SavedCardsDialog
import com.herfy.payment.views.CustomCheckoutDialog
import com.herfy.payment.views.CustomCheckoutDialog.OnActionSelectedListener
import com.herfy.payment.views.MoyasarActivity
import com.herfy.payment.views.PaymentDialogFragment
import com.herfy.ratings.AppRatingBroadcastReceiver
import com.herfy.util.AppLogger
import com.herfy.util.ShipBookLogger.logError
import com.herfy.util.ShipBookLogger.logInfo
import com.herfy.util.StringUtil
import com.herfy.util.ViewUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.debounceTextChanged
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.extensions.showToast
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.ApiLogLevel
import com.herfy.utils.helper.ApiLogUtil.Companion.sendOrderLog
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.BuildUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CurrencyUtils.Companion.updateCurrency
import com.herfy.utils.helper.DialogUtils
import com.herfy.utils.helper.DistanceCalculator
import com.herfy.utils.helper.KeyboardUtils
import com.herfy.utils.helper.KeyboardUtils.SoftKeyboardToggleListener
import com.herfy.utils.helper.launchWithTimber
import com.herfy.utils.permission.PermissionManager
import com.herfy.utils.permission.PermissionResult
import com.herfy.utils.permission.permissionProvider
import com.moneyhash.sdk.android.ResultType
import com.moneyhash.sdk.android.payment.PaymentResultContract
import com.moneyhash.sdk.android.payment.PaymentStatus
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.OrdersApiCall
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.PaymentType
import com.skylinedynamics.solosdk.api.models.objects.PrayerTime
import com.skylinedynamics.solosdk.api.models.objects.ServiceCharge
import company.tap.gosellapi.internal.api.callbacks.GoSellError
import company.tap.gosellapi.internal.api.models.Authorize
import company.tap.gosellapi.internal.api.models.Charge
import company.tap.gosellapi.internal.api.models.Token
import company.tap.gosellapi.open.controllers.SDKSession
import company.tap.gosellapi.open.delegate.SessionDelegate
import company.tap.gosellapi.open.models.CardsList
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.floor
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@AndroidEntryPoint
class CheckoutActivity : BaseActivity(), PaymentContract.View, SessionDelegate,
    OnDateSelectedListener, BottomSheetAlrhaji.OTPAlrahjiListener {
    private var binding: ActivityCheckoutPageBinding? = null
    var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var mMap: GoogleMap? = null
    var isBranchImageAvailable = false
    var mukafaaSelected = false
    var dialogCheckout: CustomCheckoutDialog? = null
    private var paymentPresenter: PaymentContract.Presenter? = null
    private val oneDayDecorator: OneDayDecorator? = null
    private val beforeDateDecorator: BeforeDateDecorator? = null
    private val maxDate: Date? = null
    private val minDate: Date? = null
    private val isViewLoaded = false
    private val selectedDate: Date? = null
    private val daySelected: String? = null
    private var goodDaySelected: String? = null
    private val goodSelectedDate: Date? = null
    private var goodSelectedDateString: String? = null
    private val isRemove = true
    private val tapSdkSession: SDKSession? = null
    private var isScheduled = false
    private var orderId: String? = null
    private var orderType: OrderType? = null
    private val OTP_REQUEST = 100
    private var loyaltypointsInput = 0
    private var saveDialogAlert: SaveDialogAlert? = null
    private var savedCardsDialog: SavedCardsDialog? = null
    private var cvvDialogAlert: CVVDialogAlert? = null
    private var passedTotalFromCart = 0.0
    private var totalValue = 0.0
    private val isPaymentSet = false
    private val isCashPaymentAvailable = false
    private val isCardPaymentAvailable = false
    private val isCODPaymentAvailable = false
    private var orderServiceChargeRecyclerViewAdapter: OrderServiceChargeRecyclerViewAdapter? = null
    var firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    private var myPolyline: Polyline? = null

    var isFromCheckout = false
    private var moyasarActivityResultLauncher: ActivityResultLauncher<Intent>? = null
    private var isShowCardFailed = false

    private val permissionManager: PermissionManager by permissionProvider()

    private val markerOptionsList = arrayListOf<MarkerOptions>()

    private val mapOverlays: List<Overlay> = arrayListOf()
    private val projection: Projection? = null

    private var bottomSheetAlrhaji: BottomSheetAlrhaji? = null
    private var finalTotal = 0.0
    private var finalDiscount = 0.0

    var alrahjiCheckChangeListener = OnCheckedChangeListener { p0, p1 ->
        if (!p1) {
            paymentPresenter?.let {
                if (it.hasMukafaaTransactionId()) {
                    it.removeMukafaa()
                }
            }
        } else {
            paymentPresenter?.let {
                bottomSheetAlrhaji = BottomSheetAlrhaji.newInstance(
                    it.validatedTotal, it.validatedBody, it.validatedDeliveryCharge
                )
                bottomSheetAlrhaji?.isCancelable = false
                bottomSheetAlrhaji?.show(
                    supportFragmentManager, BottomSheetAlrhaji::class.java.simpleName
                )

            }
        }
    }

    fun handleBack() {
        binding?.layoutLoyalty?.etPoints?.apply {
            if (this.hasFocus()) {
                this.clearFocus()
            } else {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutPageBinding.inflate(layoutInflater)
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBack()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        setContentView(binding!!.root)
        popupUpsellBackStack()
        passedTotalFromCart = intent.getDoubleExtra("totalPrice", 0.0)
        paymentPresenter = PaymentPresenter(this)
        initData()
        paymentPresenter?.getLoyalty()
        paymentPresenter?.start()
        paymentPresenter?.getPaymentTypes()
        moyasarActivityResultLauncher = registerForActivityResult<Intent, ActivityResult>(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result != null) {
                if (result.resultCode == RESULT_OK) {
                    val intent = result.data
                    if (intent != null) {
                        if (intent.hasExtra(MoyasarActivity.EXTRA_MOYASAR_ID) && intent.hasExtra(
                                MoyasarActivity.EXTRA_MOYASAR_STATUS
                            )
                        ) {
                            if (!intent.getStringExtra(MoyasarActivity.EXTRA_MOYASAR_ID).equals(
                                    MoyasarActivity.STATUS_FAILED, ignoreCase = true
                                ) && !intent.getStringExtra(MoyasarActivity.EXTRA_MOYASAR_ID)
                                    .equals(
                                        MoyasarActivity.STATUS_CANCELLED, ignoreCase = true
                                    ) && !intent.getStringExtra(MoyasarActivity.EXTRA_MOYASAR_STATUS)
                                    .equals(MoyasarActivity.STATUS_FAILED, ignoreCase = true)
                            ) {
                                paymentPresenter!!.sendUpsells()
                            } else {
                                val errorMessage =
                                    intent.getStringExtra(MoyasarActivity.EXTRA_MOYASAR_STATUS)
                                val orderId = intent.getStringExtra(MoyasarActivity.EXTRA_ORDER_ID)
                                if (orderId != null && errorMessage != null && errorMessage != MoyasarActivity.STATUS_CANCELLED) {
//                                        logError(BuildConfig.TAG + " - Order ID: " + orderId + " -> " + paymentPresenter?.paymentProvider + " Card Payment Failed (Reason: Moyasar " + errorMessage + ")")
                                    sendOrderLog(
                                        ApiLogLevel.ERROR,
                                        orderId,
                                        paymentPresenter?.paymentProvider.orEmpty(),
                                        "Card Payment Failed (Reason: Moyasar $errorMessage)"
                                    )
                                }
                                isShowCardFailed = true
                            }
                        }
                    }
                } else {
                    isShowCardFailed = true
                }
            }
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

    }

    private fun deletePolyline() {
        myPolyline?.remove()
        myPolyline = null  // Clear the reference
    }

    private fun popupUpsellBackStack() {
        if (intent.getBooleanExtra("fromUpsellPage", false)) {
            try {
                if (currentMainActivity != null) {
                    Handler(Looper.getMainLooper()).postDelayed(
                        { currentMainActivity!!.popUpsell() }, 300
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun requestPermissionLocation() {
        permissionManager.requestPermissions(permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        ), showRationale = { rationaleCallback ->
            rationaleCallback.showRationale()
        }) { result ->
            when (result) {
                is PermissionResult.Denied -> {
                    showToast(result.deniedPermissions.joinToString(", "))
                }

                PermissionResult.Granted -> {
                    showMap()
                    mMap?.apply {
                        isMyLocationEnabled = true
                        uiSettings.isMyLocationButtonEnabled = true
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (BuildUtil.isEkfc()) {
            showLoadingDialog()
        }
        if (isFromCheckout) {
            isFromCheckout = false
            return
        }
        paymentPresenter!!.validateOrder(
            false, CacheUtil.getInstance().orderStore, null, loyaltypointsInput, isShowCardFailed
        )
        paymentPresenter!!.fetchCustomerCards()
        showSelectedPayment()
        isShowCardFailed = false
    }

    private fun initData() {
        isScheduled = intent.getBooleanExtra("isScheduled", false)
        if (isScheduled) {
            goodSelectedDateString = intent.getStringExtra("goodSelectedDate")
            goodDaySelected = intent.getStringExtra("goodDaySelected")
        }
    }

    override fun setPresenter(presenter: PaymentContract.Presenter) {
        paymentPresenter = presenter
    }

    override fun logEvent(
        eventName: String,
        eventAttributes: HashMap<String, String>,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue)
    }


    private fun showMap() {
        markerOptionsList.clear()
        var currentMarkerIcon = com.herfy.utils.helper.ViewUtil.vectorDrawableToBitmapDescriptor(
            this, R.drawable.user_current_location
        )
        val storeLat = CacheUtil.getInstance().orderStore.attributes.lat
        val storeLon = CacheUtil.getInstance().orderStore.attributes.lng
        val storeLocation = LatLng(storeLat, storeLon)
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val markerOptions = MarkerOptions()
        val supportMapFragment =
            supportFragmentManager.findFragmentById(R.id.checkout_map) as OrderSupportMapFragment?
        supportMapFragment?.setListener {
            binding?.nestedScrollView?.requestDisallowInterceptTouchEvent(
                true
            )
        }
        supportMapFragment?.getMapAsync { googleMap ->
            mMap = googleMap
            mMap?.uiSettings?.isCompassEnabled = false
            mMap?.uiSettings?.isZoomControlsEnabled = false
            mMap?.uiSettings?.isMapToolbarEnabled = false

            if (orderType == OrderType.DELIVERY) {
                currentMarkerIcon =
                    com.herfy.utils.helper.ViewUtil.vectorDrawableToBitmapDescriptor(
                        this, R.drawable.ic_address_user_home
                    )
                if (CacheUtil.getInstance().orderAddress.attributes.label.equals(
                        "work",
                        ignoreCase = true
                    )
                ) {
                    currentMarkerIcon =
                        com.herfy.utils.helper.ViewUtil.vectorDrawableToBitmapDescriptor(
                            this, R.drawable.ic_address_user_work
                        )
                }
                val lat = CacheUtil.getInstance().orderAddress.attributes.lat.toDouble()
                val lon = CacheUtil.getInstance().orderAddress.attributes.longt.toDouble()
                val addressLocation = LatLng(lat, lon)
                val view = inflater.inflate(R.layout.marker_store_new, null)
                val textView = view.findViewById<TextView>(R.id.kmAway)
                val image = view.findViewById<ImageView>(R.id.icon)
                image.setImageResource(R.drawable.delivery_marker_selected)
                val currentLocation =
                    LatLng(addressLocation.latitude, addressLocation.longitude)
                val currentDistance = DistanceCalculator.calculateDistance(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    storeLocation.latitude,
                    storeLocation.longitude
                )
                val distanceValue = StringUtil.getDistance(currentDistance)
                textView.text =
                    CacheUtil.getInstance().getTranslations(Text.KM_AWAY, "(x) away")
                        .replace("(x)", distanceValue).toLowerCase()
                val icon = BitmapDescriptorFactory.fromBitmap(
                    ViewUtil.createDrawableFromView(
                        this@CheckoutActivity, view
                    )
                )
                val currentLocationMarker = MarkerOptions()
                currentLocationMarker.icon(currentMarkerIcon)
                currentLocationMarker.position(addressLocation)
                currentLocationMarker.anchor(0.5f, 1f)
                markerOptionsList.add(currentLocationMarker)
                mMap?.addMarker(currentLocationMarker)
                val storeLocationMarker = MarkerOptions()
                storeLocationMarker.icon(icon)
                storeLocationMarker.position(storeLocation)
                storeLocationMarker.anchor(0.5f, 1f)
                markerOptionsList.add(storeLocationMarker)
                val markerStore = mMap?.addMarker(storeLocationMarker)
                markerStore?.tag = "storeMarker"
                mMap?.setOnMarkerClickListener { marker ->
                    if (marker.tag == "storeMarker") {
                        goToStoreInMap(storeLocation)
                        true // Consume the click event
                    } else {
                        false // Default behavior (showing the InfoWindow)
                    }
                }
                val builder = LatLngBounds.Builder()
                builder.include(addressLocation)
                builder.include(storeLocation)
                val bounds = builder.build()
                val width = Resources.getSystem().displayMetrics.widthPixels
                val padding = (width * 0.21).toInt()
                val cu = CameraUpdateFactory.newLatLngBounds(
                    bounds, padding
                )
                mMap?.animateCamera(cu, object : CancelableCallback {
                    override fun onCancel() {}
                    override fun onFinish() {
                        val zout = CameraUpdateFactory.zoomBy(-1.0f)
                        mMap?.animateCamera(zout)
                    }
                })
                try {
                    deletePolyline()
                    val storeLoc = LatLng(
                        storeLocation.latitude, storeLocation.longitude
                    )
                    myPolyline = drawArcPolyline(
                        mMap, currentLocation, storeLoc, view.context
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                markerOptions.position(storeLocation)
                val view = inflater.inflate(R.layout.marker_store_new, null)
                val textView = view.findViewById<TextView>(R.id.kmAway)
                val imageView = view.findViewById<AppCompatImageView>(R.id.icon)
                imageView.setImageResource(R.drawable.ic_marker_red)
                lifecycleScope.launchWithTimber {
                    val task = fusedLocationProviderClient?.lastLocation
                    task?.addOnSuccessListener { location ->
                        location?.let {
                            val currentLocation = LatLng(it.latitude, it.longitude)
                            val currentDistance = DistanceCalculator.calculateDistance(
                                currentLocation.latitude,
                                currentLocation.longitude,
                                storeLocation.latitude,
                                storeLocation.longitude
                            )

                            val distanceValue = StringUtil.getDistance(currentDistance)

                            textView.text =
                                CacheUtil.getInstance().getTranslations(Text.KM_AWAY, "(x) away")
                                    .replace("(x)", distanceValue).toLowerCase()
                            val icon = BitmapDescriptorFactory.fromBitmap(
                                ViewUtil.createDrawableFromView(
                                    this@CheckoutActivity, view
                                )
                            )
                            val currentLocationMarker = MarkerOptions()
                            currentLocationMarker.icon(currentMarkerIcon)
                            currentLocationMarker.position(currentLocation)
                            currentLocationMarker.anchor(0.5f, 0.5f)
                            markerOptionsList.add(currentLocationMarker)
                            mMap?.addMarker(currentLocationMarker)

                            val storeLocationMarker = MarkerOptions()
                            storeLocationMarker.icon(icon)
                            storeLocationMarker.position(storeLocation)
                            storeLocationMarker.anchor(0.5f, 1f)
                            markerOptionsList.add(storeLocationMarker)
                            val markerStore = mMap?.addMarker(storeLocationMarker)
                            markerStore?.tag = "storeMarker"
                            mMap?.setOnMarkerClickListener { marker ->
                                if (marker.tag == "storeMarker") {
                                    goToStoreInMap(storeLocation)
                                    true // Consume the click event
                                } else {
                                    false // Default behavior (showing the InfoWindow)
                                }
                            }

                            val builder = LatLngBounds.Builder()
                            builder.include(currentLocation)
                            builder.include(storeLocation)
                            val bounds = builder.build()
                            val width = Resources.getSystem().displayMetrics.widthPixels
                            val padding = (width * 0.21).toInt()
                            val cu = CameraUpdateFactory.newLatLngBounds(
                                bounds, padding
                            )
                            mMap?.animateCamera(cu, object : CancelableCallback {
                                override fun onCancel() {}
                                override fun onFinish() {
                                    val zout = CameraUpdateFactory.zoomBy(-1.0f)
                                    mMap?.animateCamera(zout)
                                }
                            })
                            try {
                                deletePolyline()
                                val storeLoc = LatLng(
                                    storeLocation.latitude, storeLocation.longitude
                                )
                                myPolyline = drawArcPolyline(
                                    mMap, currentLocation, storeLoc, view.context
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }
                    }?.addOnFailureListener {
                        it.printStackTrace()
                        AppLogger.printRetrofitError(it)
                    }

                }
            }

        }


    }

    private fun goToStoreInMap(store: LatLng) {
        store.let {
            val directionsBuilder =
                Uri.Builder().scheme("https").authority("www.google.com").appendPath("maps")
                    .appendPath("dir").appendPath("").appendQueryParameter("api", "1")
                    .appendQueryParameter("travelmode", "driving").appendQueryParameter(
                        "destination", it.latitude.toString() + "," + it.longitude.toString()
                    )

            if (StringUtil.hasOrderLatLong()) {
                directionsBuilder.appendQueryParameter(
                    "origin",
                    CacheUtil.getInstance().orderLatitude + "," + CacheUtil.getInstance().orderLongitude
                )
            }

            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW, directionsBuilder.build()
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    override fun setupViews() {

        orderType = CacheUtil.getInstance().orderType
        requestPermissionLocation()

        binding!!.layoutPayment.tvEditPaymentMethod.setOnClickListener { showPaymentMethod() }
        val orderStore = CacheUtil.getInstance().orderStore
        if (orderStore != null && orderStore.attributes != null) {
            val branchImage = orderStore.attributes.image
            if (branchImage != null) {
                isBranchImageAvailable = branchImage.isNotEmpty()
            }
        }/*if (isScheduled) {
            schedLayout.setVisibility(View.VISIBLE);
            scheduleText.setText(TimeUtil.properSchedDate(goodDaySelected + " " + goodSelectedDateString));
        } else {
            schedLayout.setVisibility(View.GONE);
        }*/

        /*   String tableDineInNumber = CacheUtil.getInstance().getDineInTableNumber();
        if (tableDineInNumber != null && !tableDineInNumber.isEmpty()) {
            tableNumberLayout.setVisibility(View.VISIBLE);
            tableNumber.setText(tableDineInNumber);
        }*/

//        slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        binding!!.tvMobileNumber.text = "+${AuthUtil.getInstance().customer.attributes.mobile}"

        binding!!.toolbarLayout.btnBack.setOnClickListener {
            handleBack()
        }

        if (CacheUtil.getInstance().orderStore != null) {
            paymentPresenter!!.validateOrder(
                false, CacheUtil.getInstance().orderStore, null, 0, false
            )
        }
        KeyboardUtils.addKeyboardToggleListener(this, object : SoftKeyboardToggleListener {
            override fun onToggleSoftKeyboard(isVisible: Boolean) {
                binding?.layoutLoyalty?.apply {
                    if (!isVisible && etPoints.hasFocus()) {
                        etPoints.clearFocus()
                    }
                }
            }
        })
        if (orderType === OrderType.DELIVERY) {
            binding!!.groupDeliveryInstruction.visible()/*showLoadingDialog();
            Geocoder geocoder = new Geocoder(PaymentActivity.this, LocaleUtil.getInstance().isEnglish() ? new Locale("en", "us") : new Locale("ar", "sa"));
            paymentPresenter.getStore(geocoder);*/
            if (CacheUtil.getInstance().orderAddress != null) {
                binding!!.tvBranchName.text = CacheUtil.getInstance().orderAddress.attributes.line1
            }
            val instruction = CacheUtil.getInstance().orderAddress.attributes.instructions
            binding!!.tvDeliveryInstruction.text = instruction
            binding!!.tvDeliveryInstruction.setVisible(instruction.isNotEmpty())
        } else {
            binding!!.groupDeliveryInstruction.gone()/*Geocoder geocoder = new Geocoder(PaymentActivity.this, LocaleUtil.getInstance().isEnglish() ? new Locale("en", "us") : new Locale("ar", "sa"));
            paymentPresenter.getAddresses(geocoder);*/
            if (CacheUtil.getInstance().orderStore != null) {
                binding!!.tvBranchName.text = CacheUtil.getInstance().orderStore.attributes.name
            }
        }
        when (CacheUtil.getInstance().orderType) {
            OrderType.PICKUP -> {
                binding!!.tvOrderType.text =
                    CacheUtil.getInstance().getTranslations(Text.PICKUP, "Pickup")
                binding!!.ivOrderType.setImageResource(com.herfy.checkout.R.drawable.ic_herfy_pickup)
            }

            OrderType.DINE_IN -> {
                binding!!.tvOrderType.text = CacheUtil.getInstance().getTranslations(Text.DINE_IN)
                binding!!.ivOrderType.setImageResource(com.herfy.checkout.R.drawable.ic_dine_in)
            }

            OrderType.DELIVERY -> {
                binding!!.tvOrderType.text = CacheUtil.getInstance()
                    .getTranslations(Text.DELIVERY_ADDRESS, "Delivery Address")
                binding!!.ivOrderType.setImageResource(R.drawable.ic_delivery_medium)
            }

            OrderType.CURBSIDE -> {
                binding!!.tvOrderType.text =
                    CacheUtil.getInstance().getTranslations(Text.CURBSIDE_PICKUP, "Curbside")
                binding!!.ivOrderType.setImageResource(com.herfy.checkout.R.drawable.ic_pickup_curbside)
            }

            OrderType.DRIVE_THRU -> {
                binding!!.tvOrderType.text =
                    CacheUtil.getInstance().getTranslations(Text.PICKUP, "Pickup")
                binding!!.ivOrderType.setImageResource(R.drawable.ic_pickup_drive_thru)
            }

            else -> {}
        }
        binding!!.placeOrder.setOnClickListener { view: View? ->
            val canProceedViaFullPoints = paymentPresenter!!.canProceedViaFullPoints()
            if (canProceedViaFullPoints || CacheUtil.getInstance().orderPayment != PaymentType.NONE) {
                if (!isScheduled) {
                    showLoadingDialog()
                    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
                    simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT+0")
                    paymentPresenter!!.getPrayerTime()
                    paymentPresenter!!.getStoreOpen(false, simpleDateFormat.format(Date()))
                } else {
                    try {
                        showLoadingDialog()
                        val originalCalendar = Calendar.getInstance()
                        val calendar = Calendar.getInstance()
                        val sdf = SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.ENGLISH)
                        calendar.time = sdf.parse("$goodDaySelected $goodSelectedDateString")
                        paymentPresenter!!.getPrayerTime()
                        paymentPresenter!!.scheduleOrder(originalCalendar, calendar, false)
                    } catch (pe: ParseException) {
                        pe.printStackTrace()
                        var minSchedule =
                            CacheUtil.getInstance().concept.defaultScheduleDeliveryTime
                        if (minSchedule == 0) {
                            minSchedule = 1
                        }
                        showMessage(
                            CacheUtil.getInstance().getTranslations(
                                Text.MINIMUM_SCHEDULED_ORDER,
                                "Unable to process order, scheduled order is allowed minimum of (x) min from current time."
                            ).replace("(x)", "" + minSchedule)
                        )
                        dismissDialogs()
                    }
                }
                firebaseAddPaymentInfoEvent()
            } else {
                Toast.makeText(
                    this@CheckoutActivity, CacheUtil.getInstance().getTranslations(
                        Text.PLEASE_SELECT_PAYMENT_TYPE, "Please select payment type"
                    ), Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding!!.layoutLoyalty.loyaltySwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding!!.layoutLoyalty.layoutLoyaltyPoints.visibility = View.VISIBLE
            } else {
                binding!!.layoutLoyalty.layoutLoyaltyPoints.visibility = View.GONE
                paymentPresenter!!.setLoyaltyPointsUsed(0)
                binding!!.layoutLoyalty.etPoints.setText("")
                loyaltypointsInput = 0
                paymentPresenter!!.validateOrder(
                    false, CacheUtil.getInstance().orderStore, null, 0, false
                )
            }
        }

        val loyalty = AuthUtil.getInstance().customerLoyalty
        val debounceDuration = 400L
        lifecycleScope.launchWithTimber {
            binding!!.layoutLoyalty.etPoints.debounceTextChanged(
                debounceDuration.toDuration(
                    DurationUnit.MILLISECONDS
                )
            ).map { it.toString() }.collectLatest { pointsText ->
                if (pointsText.isNotEmpty()) {
                    var userSetPoints = StringUtil.toEnglish(pointsText).toDouble()
                    val currentPoints =
                        AuthUtil.getInstance().customerLoyalty.attributes.currentBalance
                    val pointConversion =
                        AuthUtil.getInstance().customerLoyalty.attributes.pointConversion.toDouble()
                    val roundedTotal = floor((finalTotal - finalDiscount))
                    val orderTotalPoints =
                        if (loyalty.attributes.currentBalanceAmount > roundedTotal) roundedTotal * AuthUtil.getInstance().customerLoyalty.attributes.pointConversion.toDouble() else loyalty!!.attributes.currentBalance.toDouble()

                    if (userSetPoints >= currentPoints || userSetPoints >= orderTotalPoints) {

                        userSetPoints = orderTotalPoints
                        binding?.layoutLoyalty?.etPoints?.setText(
                            StringUtil.localize(
                                userSetPoints.toInt().toString()
                            )
                        )
                    } else {
                        binding!!.layoutLoyalty.etPoints.setText(
                            StringUtil.localize(
                                userSetPoints.toInt().toString()
                            )
                        )
                    }

                    val sarAmount = Math.min(
                        userSetPoints / pointConversion, paymentPresenter!!.menuItemsTotalPrice
                    )

                    val formattedSarAmount = String.format("%.2f", sarAmount)
                    if (sarAmount >= 1) {
                        binding?.layoutLoyalty?.etSarAmount?.text = formattedSarAmount
                    }

                    binding!!.layoutLoyalty.etPoints.setSelection(binding!!.layoutLoyalty.etPoints.text.toString().length)
                    loyaltypointsInput = userSetPoints.toInt()
                    if (sarAmount >= 1) {
                        paymentPresenter!!.validateOrder(
                            false,
                            CacheUtil.getInstance().orderStore,
                            null,
                            loyaltypointsInput,
                            false
                        )
                    }
                } else {
                    binding!!.layoutLoyalty.etSarAmount.setText("")
                    binding!!.layoutLoyalty.etPoints.setSelection(0)
                    paymentPresenter!!.setLoyaltyPointsUsed(0)
                }
            }
        }

        if (loyalty != null && loyalty.attributes != null) {
            firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener { task: Task<Boolean?> ->
                if (task.isSuccessful) {
                    val loyaltySettings = firebaseRemoteConfig.getString("loyalty_settings")
                    if (loyaltySettings != null && loyaltySettings == "full") {
                        if (loyalty.attributes.currentBalanceAmount >= paymentPresenter!!.menuItemsTotalPrice) {
                            binding!!.layoutLoyalty.loyaltySwitch.isEnabled = true
                            binding!!.layoutLoyalty.loyaltyPointsPaymentContainer.isEnabled = true
                        } else {
                            binding!!.layoutLoyalty.loyaltySwitch.isEnabled = false
                            binding!!.layoutLoyalty.loyaltyPointsPaymentContainer.isEnabled = false
                            binding!!.layoutLoyalty.loyaltyPointsPaymentContainer.setCardBackgroundColor(
                                Color.parseColor("#d5d5d5")
                            )
                        }
                    } else {
                        binding!!.layoutLoyalty.loyaltySwitch.isEnabled = true
                        binding!!.layoutLoyalty.loyaltyPointsPaymentContainer.isEnabled = true
                    }
                }
            }
            val currentBalanceAmount = loyalty.attributes.currentBalanceAmount.toDouble()
            val loyaltyPoints =
                AuthUtil.getInstance().customerLoyalty.attributes.currentBalance.toDouble()
            val formattedLoyaltyPoints = StringUtil.getFormattedAmount(loyaltyPoints)

            val loyaltyBalanceLabelText = CacheUtil.getInstance().getTranslations(
                Text.LOYALTY_POINTS_PAYMENT_LABEL, "(x) Points ((y))"
            ).replace("(x)", if (currentBalanceAmount.toInt().toDouble() == currentBalanceAmount) {
                AuthUtil.getInstance().customerLoyalty.attributes.currentBalance.toString()
            } else {
                formattedLoyaltyPoints.takeIf { it.isNotEmpty() } ?: "0"
            }).replace(
                "(y)", updateCurrency(currentBalanceAmount, true).toString()
            )
            binding!!.layoutLoyalty.tvLoyaltyPoints.text = loyaltyBalanceLabelText
            //loyaltyLabel.setText(StringUtil.getFormattedAmount(AuthUtil.getInstance().getCustomerLoyalty().getAttributes().getCurrentBalance())+" Points ("+StringUtil.getFormattedAmount(loyalty.getAttributes().getCurrentBalanceAmount()) +")");
        }


        val customer = AuthUtil.getInstance().customer
        val isSignedIn = AuthUtil.getInstance().isSignedIn
        val isFullSignedIn =
            isSignedIn && !customer.attributes.accountType.equals("guest", ignoreCase = true)

        try {
            var paymentTypes: Set<PaymentType?> =
                CacheUtil.getInstance().application.attributes.getDeliverPaymentTypes()
            if (CacheUtil.getInstance().orderType != OrderType.DELIVERY) {
                paymentTypes =
                    CacheUtil.getInstance().application.attributes.getPickupPaymentTypes()
                binding!!.tvBranchName.text = CacheUtil.getInstance().orderStore.attributes.name;
            } else {
                binding!!.tvBranchName.text = CacheUtil.getInstance().orderAddress.attributes.line1
//                tvBranchAddress.setText(CacheUtil.getInstance().getOrderAddress().getAttributes().getLine1());
            }
            val isEligibleForLoyaltyDiscount =
                isFullSignedIn && paymentTypes.contains(PaymentType.LOYALTY) && passedTotalFromCart > 0
            binding!!.layoutLoyalty.loyaltyPointsPaymentContainer.setVisible(
                isEligibleForLoyaltyDiscount
            )
            binding!!.ivOrderType.setVisible(isBranchImageAvailable)
            // TODO I commented this for now. QA has a card to change it to static icon
            /**GlideUtils.loadImageUrl(
            binding!!.ivOrderType, url = CacheUtil.getInstance().orderStore.attributes.image,
            this
            )**/
        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding!!.layoutAlrahji.apply {
            cvAlrhaji.visibility = when (paymentPresenter?.isMukafaaEnabled) {
                true -> View.VISIBLE
                else -> View.GONE
            }

            switchAlrahji.setOnCheckedChangeListener(alrahjiCheckChangeListener)
        }
        binding?.layoutLoyalty?.etPoints?.apply {
            setOnEditorActionListener(object : OnEditorActionListener {
                override fun onEditorAction(p0: TextView?, p1: Int, p2: KeyEvent?): Boolean {
                    if (p1 == EditorInfo.IME_ACTION_DONE || (p2 != null && p2.keyCode == KeyEvent.KEYCODE_ENTER)) {
                        this@apply.clearFocus()
                        hideKeyboardFrom(context, this@apply)
                        return true
                    }
                    return false
                }
            })
        }
        //total.setText(StringUtil.getFormattedPrice(paymentPresenter.getTotalPrice()));

        /* Concept concept = CacheUtil.getInstance().getConcept();
        if (concept != null) {
            String vatNumber = concept.getVatNumber();
            String vatCertificateUrl = concept.getVatCertificateUrl();
            if (vatNumber != null && !vatNumber.isEmpty() && vatCertificateUrl != null && !vatCertificateUrl.isEmpty()) {
                financialValue.setText(vatNumber);
                financialContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (vatCertificateUrl.contains(".pdf")) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                            browserIntent.setDataAndType(Uri.parse(vatCertificateUrl), "application/pdf");
                            Intent chooser = Intent.createChooser(browserIntent, CacheUtil.getInstance().getTranslations(Text.VAT_NO_CAPS, "VAT NO."));
                            startActivity(chooser);
                        } else {
                            Intent intent = new Intent(CheckoutActivity.this, BrowserActivity.class);
                            intent.putExtra("title", CacheUtil.getInstance().getTranslations(Text.VAT_NO_CAPS, "VAT NO."));
                            intent.putExtra("url", vatCertificateUrl);
                            startActivity(intent);
                        }
                    }
                });
                financialCard.setVisibility(View.VISIBLE);
            } else {
                financialCard.setVisibility(View.GONE);
            }
        } else {
            financialCard.setVisibility(View.GONE);
        }*/

        /*tvMukafaaRemove.setOnClickListener(view -> {
            paymentPresenter.removeMukafaa();
            cardMukafaaContainer.performClick();
        });

        cardMukafaaContainer.setOnClickListener(view -> {
            if (!paymentPresenter.hasMukafaaTransactionId()) {
                CacheUtil.getInstance().setOrderPayment(PaymentType.NONE);
                mukafaaSelected = !mukafaaSelected;
                selectMukafaa(mukafaaSelected, true);
            }
        });

        requestOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String number = getNumber();
                if (number == null || number.isEmpty()) {
                    showMessage(CacheUtil.getInstance().getTranslations(Text.MOBILE_OR_CIC_REQUIRED, "Please input either your Mobile Number or Loyalty CIC Number to request an OTP."));
                } else if (!phoneNumber.getText().toString().isEmpty() && !StringUtil.isValidMobileNumber(number)) {
                    showMessage(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_INVALID));
                } else {
                    Intent intent = new Intent(PaymentActivity.this, MukafaaOTPActivity.class);
                    intent.putExtra("number", number);
                    intent.putExtra("total", String.valueOf(paymentPresenter.getValidatedTotal()));
                    intent.putExtra("cart", paymentPresenter.getValidatedBody());
                    intent.putExtra("delivery_charge", paymentPresenter.getValidatedDeliveryCharge());
                    startActivityForResult(intent, OTP_REQUEST);

                }
            }
        });

        if (paymentPresenter.hasMukafaaTransactionId()) {
            if (!mukafaaSelected) {
                mukafaaSelected = true;
                mukafaaPayment.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
                mukafaaNumbers.setVisibility(View.GONE);
                requestOTP.setVisibility(View.GONE);
                placeOrder.setVisibility(View.VISIBLE);
            }
        }*/
    }

    override fun setupFonts() {}
    override fun setupTranslations() {
        binding!!.toolbarLayout.title.text =
            CacheUtil.getInstance().getTranslations(Text.CHECKOUT, "Checkout")
        binding!!.layoutPayment.tvEditPaymentMethod.text =
            CacheUtil.getInstance().getTranslations(Text.EDIT, "Edit")
        binding!!.tvDeliveryInstructionLabel.text = CacheUtil.getInstance().getTranslations(
            Text.DELIVERY_INSTRUCTION, "Delivery Instruction"
        )
        binding!!.layoutPayment.tvPaymentMethod.text =
            CacheUtil.getInstance().getTranslations(Text.PAYMENT_METHOD_NOT_CAPS, "Payment Method")
        //        tvChangeOrderType.setText(CacheUtil.getInstance().getTranslations(Text.CHANGE, "Change").toUpperCase());
        binding!!.layoutLoyalty.tvPointsLabel.text =
            CacheUtil.getInstance().getTranslations(Text.POINTS, "Points")
        binding!!.layoutLoyalty.tvSarAmountLabel.text =
            CacheUtil.getInstance().getTranslations(Text.SAR_AMOUNT, "SAR Amount")

        binding!!.layoutPaymentSummary.tvDiscountLabel.text =
            CacheUtil.getInstance().getTranslations(Text.DISCOUNT, "Discount")
        binding!!.layoutLoyalty.tvLoyaltyPointsLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.LOYALTY_USE_POINTS_NOT_CAPS, "Use Loyalty Points")
        val orderType = CacheUtil.getInstance().orderType
        try {
            if (orderType === OrderType.DELIVERY) {
                binding!!.tvTimeLabel.text =
                    CacheUtil.getInstance().getTranslations(Text.DELIVERY_TIME, "Delivery Time")
                val mins =
                    CacheUtil.getInstance().orderStore.attributes.promisedTimeDelta.delivery.toString()
                binding!!.tvWithin.text =
                    CacheUtil.getInstance().getTranslations(Text.WITHIN, "Within (x) mins")
                        .replace("(x)", mins)
            } else {
                binding!!.tvTimeLabel.text =
                    CacheUtil.getInstance().getTranslations(Text.PICKUP_TIME, "Pickup Time")
                val mins =
                    CacheUtil.getInstance().orderStore.attributes.promisedTimeDelta.pickup.toString()
                binding!!.tvWithin.text =
                    CacheUtil.getInstance().getTranslations(Text.WITHIN, "Within (x) mins")
                        .replace("(x)", mins)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        binding!!.placeOrder.text =
            CacheUtil.getInstance().getTranslations(Text.PLACE_ORDER, "Place Order")
        binding!!.layoutPaymentSummary.tvSubtotalLabel.text =
            CacheUtil.getInstance().getTranslations(Text.SUBTOTAL)
        binding!!.layoutPaymentSummary.tvDiscount.text =
            CacheUtil.getInstance().getTranslations(Text.PROMO_DISCOUNT)
        binding!!.layoutPaymentSummary.tvTotalLabel.text =
            CacheUtil.getInstance().getTranslations(Text.TOTAL_LABEL, "Total").replace(":", "")
        binding!!.tvDeliveryInstructionLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.DELIVERY_INSTRUCTION, "Delivery Instruction")
        binding!!.layoutPaymentSummary.tvSubtotalLabel.text =
            CacheUtil.getInstance().getTranslations(Text.SUBTOTAL)
        binding!!.layoutPaymentSummary.tvDiscount.text =
            CacheUtil.getInstance().getTranslations(Text.PROMO_DISCOUNT)
        binding!!.totalVatDescription.text = CacheUtil.getInstance()
            .getTranslations(Text.TOTAL_VAT_DESCRIPTION, "Total is inclusive of VAT")
        binding?.layoutAlrahji?.apply {
            tvAlrhajiLabel.text = CacheUtil.getInstance().getTranslations(
                Text.ALRHAJI_LABEL, "Al Rhaji Bank (Mokafa's Rewards)"
            )
            tvAlrhajiDescription.text = CacheUtil.getInstance().getTranslations(
                Text.ALRHAJI_DESCRIPTION,
                "The user has to be a registered mobile number with Mokafa'a Al Rajhi Program, otherwise the redemption will not be successful."
            )
        }
        binding!!.layoutPaymentSummary.tvDeliveryChargeLabel.text =
            CacheUtil.getInstance().getTranslations(Text.DELIVERY_CHARGE, "Delivery Charges")
        binding!!.totalVatDescription.text = CacheUtil.getInstance()
            .getTranslations(Text.TOTAL_VAT_DESCRIPTION, "Total is inclusive of VAT")
        binding!!.layoutPaymentSummary.tvMukafaaLabel.text =
            CacheUtil.getInstance().getTranslations(Text.MOKAFAA_POINTS, "Mokafa’a Points")
    }

    override fun showPaymentTypes(paymentTypes: Set<PaymentType>) {}
    override fun showPaymentTypes(paymentTypes: ArrayList<PaymentType>) {
    }

    override fun setStartName(startName: String) {}
    override fun setStartAddress(startAddress: String) {}
    override fun setEndName(endName: String) {}
    override fun setEndAddress(endAddress: String) {}
    override fun updateMap(
        orderType: OrderType, startLocation: LatLng, endLocation: LatLng, bounds: LatLngBounds
    ) {
    }

    override fun updateTime(calendar: Calendar) {}
    override fun selectPaymentTypeCash() {
        CacheUtil.getInstance().orderPayment = PaymentType.CASH

        binding!!.layoutPayment.tvSelectedPayment.text =
            CacheUtil.getInstance().getTranslations(Text.CASH, "Cash")
        binding!!.layoutPayment.ivSelectedPayment.setImageResource(R.drawable.ic_payment_cash)


        if (paymentPresenter!!.hasMukafaaTransactionId()) {
        } else {
            mukafaaSelected = false
            binding!!.placeOrder.setVisibility(View.VISIBLE)
        }
        val validatedTotal = paymentPresenter?.validatedTotal?.toInt()
//        if (CacheUtil.getInstance().orderType === OrderType.DELIVERY && validatedTotal != null && validatedTotal != 0) {
//            val paymentDialogFragment: PaymentDialogFragment = PaymentDialogFragment.newInstance(
//                paymentPresenter
//            ) { dialog, cashPresented ->
//                dialog.dismiss()
//                paymentPresenter!!.cashPresented = cashPresented
//            }
//            paymentDialogFragment.show(
//                supportFragmentManager, PaymentDialogFragment::class.java.simpleName
//            )
//        }
    }

    override fun showValidateOrderError(errors: List<String>) {}
    override fun sendOrder() {}
    override fun initializeTap(orderId: String, total: Double) {}
    override fun showCheckoutPaymentForm(orderId: String?) {
        if (isCanProceedFragmentTransaction(supportFragmentManager)) {
            dialogCheckout = CustomCheckoutDialog(this)
            dialogCheckout!!.setOnActionSelectedListener(object : OnActionSelectedListener {
                override fun onFormSubmit() {
                    showLoadingDialog()
                }

                override fun onTokenGenerated(response: TokenDetails?) {
                    //dialogCheckout.clear();
                    showLoadingDialog()
                    saveDialogAlert =
                        SaveDialogAlert.newInstance(object : SaveDialogAlert.OnSavedAction {
                            override fun onClose() {
                                saveDialogAlert!!.dismissAllowingStateLoss()
                                checkTokenGenerated(
                                    response!!.token, null, false, orderId.orEmpty()
                                )
                            }

                            override fun onProceed() {
                                saveDialogAlert!!.dismissAllowingStateLoss()
                                checkTokenGenerated(response!!.token, null, true, orderId.orEmpty())
                            }
                        })
                    saveDialogAlert?.setCancelable(false)
                    saveDialogAlert?.show(
                        supportFragmentManager, SaveDialogAlert::class.java.toString()
                    )
                }

                override fun onError(error: String?) {
                    var reason = "CardTokenisationFail"
                    if (error != null) {
                        reason = error
                    }
                    logError(BuildConfig.TAG + " - Order ID: " + orderId + " -> " + paymentPresenter!!.paymentProvider + " Card Payment Failed (Reason: " + reason + ")")
                    sendOrderLog(
                        ApiLogLevel.ERROR,
                        orderId,
                        paymentPresenter!!.paymentProvider,
                        "Card Payment Failed (Reason: $reason)"
                    )
                    dismissDialogs()
                }

                override fun on3DSSuccess() {
                    logInfo(BuildConfig.TAG + " - Order ID: " + orderId + " -> " + paymentPresenter!!.paymentProvider + " Card Payment Success")
                    paymentPresenter!!.sendUpsells()
                    dialogCheckout!!.dismissAllowingStateLoss()
                }

                override fun on3DSError(error: String?) {

                }

                override fun on3DSFailure() {
                    logError(BuildConfig.TAG + " - Order ID: " + orderId + " -> " + paymentPresenter!!.paymentProvider + " Card Payment Failed (Reason: on3DSFailure)")
                    sendOrderLog(
                        ApiLogLevel.ERROR,
                        orderId,
                        paymentPresenter!!.paymentProvider,
                        "Card Payment Failed (Reason: on3DSFailure)"
                    )
                    showCardFailed()
                }

                override fun onBackPressed() {
                    sendOrderLog(
                        ApiLogLevel.INFO,
                        orderId,
                        paymentPresenter!!.paymentProvider,
                        "Card Payment onBackPressed"
                    )
                    sendOrderLog(
                        ApiLogLevel.WARNING,
                        orderId,
                        paymentPresenter!!.paymentProvider,
                        "Card Payment onBackPressed"
                    )
                    dialogCheckout!!.dismissAllowingStateLoss()
                }
            })


            dismissDialogs()
            dialogCheckout!!.show(
                supportFragmentManager, CustomCheckoutDialog::class.java.simpleName
            )
        }
    }

    override fun showInputCvvDialog(orderId: String) {
        dismissDialogs()
        inputCvvDialog(orderId, CacheUtil.getInstance().customCard)
    }

    override fun showCardFailed() {
        if (CacheUtil.getInstance().orderPayment == PaymentType.CASH) {
            showAlertDialog(
                "", CacheUtil.getInstance().getTranslations(
                    Text.CARD_PAYMENT_FAILED_PAY_WITH_CASH,
                    "Card payment failed. Do you want to Pay with Cash instead?"
                ), CacheUtil.getInstance().getTranslations(Text.YES_CAPS), { dialog, which ->
                    dialogCheckout?.dismissAllowingStateLoss()
                    dialog.dismiss()
                    selectPaymentTypeCash()
                }, CacheUtil.getInstance().getTranslations(Text.NO_CAPS)
            ) { dialog, which ->
                dialogCheckout?.dismissAllowingStateLoss()
                dialog.dismiss()
            }
        } else {
            showAlertDialog(
                "",
                CacheUtil.getInstance().getTranslations(Text.CARD_FAILED),
                CacheUtil.getInstance().getTranslations(Text.OK)
            ) { dialog, _ ->
                dialogCheckout?.dismissAllowingStateLoss()
                dialog.dismiss()
            }
        }
    }

    override fun showMessage(message: String) {
        dismissDialogs()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showError(error: String) {
        dismissDialogs()
        showAlertDialog("", error)
    }

    override fun showCompleted(orderId: String, orderType: OrderType) {
        dismissDialogs()
        this.orderId = orderId
        CacheUtil.getInstance().setCurrentOrderId(orderId)
        continueOrder()
    }

    override fun setSchedule() {
//        try {
//            FragmentManager fragmentManager = getSupportFragmentManager();
//            if (!fragmentManager.isDestroyed()) {
//                OrderDialogFragment fragment = OrderDialogFragment.newInstance(true);
//                fragment.show(fragmentManager, OrderDialogFragment.class.getSimpleName());
//            }
//            String colorStr = DynamicTheme.getStringColor(CheckoutActivity.this);
//            String start = CacheUtil.getInstance().getTranslations(Text.ORDER_SCHEDULED_FOR, "Order scheduled for:");
//            String at = CacheUtil.getInstance().getTranslations(Text.AT, "at");
//            String dayOnly = TimeUtil.dateToMonthAndDayOnly(daySelected);
//            String timeOnly = TimeUtil.dateToHourOnly(selectedDate);
//            goodDaySelected = daySelected;
//            goodSelectedDate = selectedDate;
//            String day = "<font color=" + colorStr + ">" + dayOnly + "</font>";
//            String time = "<font color=" + colorStr + ">" + timeOnly + "</font>";
//            schedTime.setText(Html.fromHtml(start + " " + day + " " + at + " " + time));
//            removeLayout.setVisibility(View.VISIBLE);
//            removeLabel.setVisibility(View.VISIBLE);
//            noSchedLayout.setVisibility(View.GONE);
//            spacingTop.setVisibility(View.VISIBLE);
//            withSchedLayout.setVisibility(View.VISIBLE);
//            placeOrder.setText(CacheUtil.getInstance().getTranslations(Text.PLACE_SCHEDULED_ORDER, "PLACE SCHEDULED ORDER"));
//            isRemove = false;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    override fun setTax(inclusive: Boolean, taxAmount: CharSequence) {
//        vatLayout.setVisibility(View.VISIBLE);
//        if (inclusive) {
//            vatLayout.setVisibility(View.GONE);
//        }
//        tax.setText(taxAmount);
//        taxLabel.setText(CacheUtil.getInstance().getConcept().getVatRate() + "% " + CacheUtil.getInstance().getTranslations(Text.VAT, "VAT"));
    }


    override fun setPriceDetails(
        subtotalPrice: Double,
        discount: Double,
        totalPrice: Double,
        deliveryCharge: Double,
        pointsSarValue: Double,
        pointsEarnable: Int,
        pointsUsed: Int,
        charges: ArrayList<ServiceCharge>
    ) {
        val isDelivery = CacheUtil.getInstance().orderType == OrderType.DELIVERY
        val hasPromotion = CacheUtil.getInstance().hasPromotion()
        val total = updateCurrency(subtotalPrice, true, hasPromotion).toString()
        val deliveryChargeTotal = updateCurrency(deliveryCharge, withSpace = true).toString()
        binding!!.layoutPaymentSummary.tvSubtotal.text = total
        val isDeliverCharge = deliveryCharge >= 1 && isDelivery
        binding!!.layoutPaymentSummary.viewDeliverySeparator.setVisible(isDeliverCharge)
        binding!!.layoutPaymentSummary.tvDeliveryChargeValue.setVisible(isDeliverCharge)
        binding!!.layoutPaymentSummary.tvDeliveryChargeLabel.setVisible(isDeliverCharge)
        binding
        binding!!.layoutPaymentSummary.tvDeliveryChargeValue.text = deliveryChargeTotal
        if (discount != 0.0) {
            binding!!.layoutPaymentSummary.grpDiscount.visibility = View.VISIBLE
            binding!!.layoutPaymentSummary.tvDiscount.text =
                updateCurrency(discount, true, hasPromotion, discount > 0)
        } else {
            binding!!.layoutPaymentSummary.grpDiscount.visibility = View.GONE
        }

//        if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
//            if (deliveryCharge != 0) {
//                deliveryText.setText(StringUtil.getFormattedPrice(deliveryCharge));
//            } else {
//                Campaign campaign = CacheUtil.getInstance().getCampaign();
//                boolean hideDiscount = false;
//                try {
//                    if (campaign != null) {
//                        if (campaign.getPromotion().getAttributes().getType().equals("delivery")) {
//                            hideDiscount = true;
//                        }
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                if (hideDiscount) {
//                    grpDiscount.setVisibility(View.GONE);
//                }
//                deliveryText.setText(CacheUtil.getInstance().getTranslations(Text.FREE, "Free"));
//            }
//            deliveryLayout.setVisibility(View.VISIBLE);
//        } else {
//            deliveryLayout.setVisibility(View.GONE);
//        }
        if (charges.size > 0) {
            val linearLayoutManager: LinearLayoutManager = object : LinearLayoutManager(this) {
                override fun canScrollVertically(): Boolean {
                    return false
                }
            }
            binding!!.layoutPaymentSummary.rvServiceCharge.layoutManager = linearLayoutManager
            orderServiceChargeRecyclerViewAdapter = OrderServiceChargeRecyclerViewAdapter(charges)
            binding!!.layoutPaymentSummary.rvServiceCharge.adapter =
                orderServiceChargeRecyclerViewAdapter
        }

//        if (deliveryCharge != 0) {
//            deliveryLayout.setVisibility(View.VISIBLE);
//            deliveryText.setText(StringUtil.getFormattedPrice(deliveryCharge));
//        } else {
//            Promotion promotion = CacheUtil.getInstance().getCampaignPromotion();
//            if (promotion != null) {
//                if (promotion.getAttributes().getType().equalsIgnoreCase("delivery")) {
//                    deliveryLayout.setVisibility(View.VISIBLE);
//                    deliveryText.setText(CacheUtil.getInstance().getTranslations(Text.FREE, "Free"));
//                } else {
//                    deliveryLayout.setVisibility(View.GONE);
//                }
//            } else {
//                deliveryLayout.setVisibility(View.GONE);
//            }
//        }


        val mukafaaAmount = paymentPresenter!!.getMukafaaTransactionAmount();
        val _discount = discount - mukafaaAmount;
        binding!!.layoutPaymentSummary.tvDiscount.text =
            updateCurrency(discount, true, hasPromotion, _discount > 0)
        binding!!.layoutPaymentSummary.grpDiscount.visibility =
            if (_discount != 0.0) View.VISIBLE else View.GONE
        binding?.layoutPaymentSummary?.apply {
            if (mukafaaAmount > 0) {
                if (paymentPresenter!!.hasMukafaaTransactionId() && paymentPresenter!!.isMukafaaEnabled()) {
                    var amnt = StringUtil.getFormattedPrice(mukafaaAmount).toString()
                    if (!"0".equals(amnt) && !amnt.contains("-")) amnt = "-" + amnt;
                    tvMukafaaSar.text = amnt
                    grpMukafaa.visibility = View.VISIBLE;
                } else {
                    grpMukafaa.visibility = View.GONE;
                }
            } else {
                grpMukafaa.setVisibility(View.GONE);
            }
        }


        if (pointsUsed > 0) {
            binding!!.layoutLoyalty.etPoints.clearFocus()
            binding!!.layoutPaymentSummary.grpRedeemedPoints.visible()
            binding!!.layoutPaymentSummary.tvRedeemPointsLabel.text =
                CacheUtil.getInstance().getTranslations(Text.LOYALTY_POINTS, "Loyalty points (x)")
                    .replace("x", pointsUsed.toString())
            val sarAmount = Math.min(
                pointsSarValue, paymentPresenter!!.menuItemsTotalPrice
            )
            binding!!.layoutPaymentSummary.tvRedeemPointsEquiv.text = updateCurrency(
                sarAmount, true, hasPromotion, true
            )
        } else {
            binding!!.layoutPaymentSummary.grpRedeemedPoints.gone()
        }
        if (totalPrice == 0.0 && pointsUsed > 0) {
            if (deliveryCharge <= 0) {
                binding!!.layoutLoyalty.loyaltyPointsPaymentContainer.visibility = View.VISIBLE
                selectPaymentTypeCash()
            } else if (subtotalPrice == pointsSarValue && deliveryCharge > 0) {
                CacheUtil.getInstance().orderPayment = PaymentType.NONE
                showSelectedPayment()
            }
        } else {
            paymentPresenter!!.getPaymentTypes()
        }

        val formattedTotal = updateCurrency(
            totalPrice, true, true
        )
        val formattedDiscountedPrice = updateCurrency(
            totalPrice, true, false
        )
        finalDiscount = _discount

        if (mukafaaAmount > 0) {
            finalTotal = subtotalPrice - mukafaaAmount
        } else {
            finalTotal = subtotalPrice
        }
        binding!!.tvDiscountedPrice.text = formattedDiscountedPrice
        binding!!.layoutPaymentSummary.tvTotal.text = formattedTotal
        totalValue = totalPrice

        val currSubTotal = if (isDelivery) subtotalPrice + deliveryCharge else subtotalPrice
        val formattedCurrentSubtotal = updateCurrency(currSubTotal, true, false)
        if (totalPrice != currSubTotal) {
            binding?.tvOriginalPrice?.visible()
            binding?.tvOriginalPrice?.text = formattedCurrentSubtotal
        } else {
            binding?.tvOriginalPrice?.gone()
        }

    }

    override fun setMenuItem(menuItem: MenuItem) {}
    override fun logFirebaseEvent(event: String, orderTotalPrice: String) {
        if (Values.sDeepLinkUtm != null) {
            val utm = Values.sDeepLinkUtm
            utm!!.putString("purchase_amount", orderTotalPrice)
            @SuppressLint("MissingPermission") val firebaseAnalytics =
                FirebaseAnalytics.getInstance(this)
            firebaseAnalytics.logEvent(event, utm)
        }
    }

    @SuppressLint("MissingPermission")
    private fun continueOrder() {
        try {
            showLoadingDialog()
            Handler().postDelayed({
                runOnUiThread {
                    binding?.blocker?.isVisible = true
                    binding?.blocker?.setOnClickListener(null)
                    val concept = CacheUtil.getInstance().concept
                    if (concept != null) {
                        var promptRatingSeconds =
                            CacheUtil.getInstance().concept.promptRatingSeconds.toLong()
                        if (promptRatingSeconds > 0) {
                            promptRatingSeconds = promptRatingSeconds * 1000L
                            val intent = Intent(this, AppRatingBroadcastReceiver::class.java)
                            val pendingIntent = PendingIntent.getBroadcast(
                                this,
                                0,
                                intent,
                                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                            )
                            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                            if (alarmManager != null) {
                                alarmManager.cancel(pendingIntent)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || alarmManager.canScheduleExactAlarms()) {
                                        alarmManager.setExactAndAllowWhileIdle(
                                            AlarmManager.RTC_WAKEUP,
                                            System.currentTimeMillis() + promptRatingSeconds,
                                            pendingIntent
                                        )
                                    }
                                } else {
                                    alarmManager.setExact(
                                        AlarmManager.RTC_WAKEUP,
                                        System.currentTimeMillis() + promptRatingSeconds,
                                        pendingIntent
                                    )
                                }
                            }
                        }
                    }
                    dismissDialogs()
                    val intent = Intent(this@CheckoutActivity, MainActivity::class.java)
                    intent.putExtra("payment", true)
                    intent.putExtra("order_id", orderId)
                    startActivity(intent)
                    finish()
                }
            }, 2300)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun showPrayerTime(prayerTime: PrayerTime) {
        if (prayerTime.prayer) {
            FirebaseService.newNotification(
                CacheUtil.getInstance().getTranslations("prayer_time_notification", "Prayer Time"),
                this
            )
        }
    }

    override fun showErrorPrayerTime() {}
    val activityResultLauncher: ActivityResultLauncher<Pair<String, ResultType>> =
        registerForActivityResult(PaymentResultContract() as ActivityResultContract<Pair<String, ResultType>, Any>,
            ActivityResultCallback<Any> { result ->
                if (result == null) return@ActivityResultCallback
                when (result) {
                    is PaymentStatus.Success -> paymentPresenter?.sendUpsells()
                    is PaymentStatus.Redirect -> {
                        if (result.result.component2()?.status.equals(
                                "SUCCESSFUL", ignoreCase = true
                            )
                        ) {
                            paymentPresenter?.sendUpsells()
                        } else {
                            showCardFailed()
                        }
                    }

                    else -> showCardFailed()
                }
            })

    override fun moneyHashContract(): ActivityResultLauncher<Pair<String, ResultType>> {
        return activityResultLauncher
    }

    override fun setOrderId(orderId: String) {}
    override fun showCardDialogList(orderId: String) {
        if (isCanProceedFragmentTransaction(supportFragmentManager)) {
            savedCardsDialog = SavedCardsDialog.newInstance(CacheUtil.getInstance().customerCards,
                object : SavedCardsDialog.OnSavedAction {
                    override fun onClose() {
                        dismissDialogs()
                        savedCardsDialog!!.dismissAllowingStateLoss()
                    }

                    override fun onEditCards() {
                        if (savedCardsDialog != null) {
                            savedCardsDialog!!.switchToEditMode()
                        }
                    }

                    override fun onUseNewCard() {
                        savedCardsDialog!!.dismissAllowingStateLoss()
                        showCheckoutPaymentForm(orderId)
                    }

                    override fun onAvail(card: CustomerCard) {
                        if (card != null) {
                            inputCvvDialog(orderId, card)
                        }
                    }
                })
            savedCardsDialog?.setCancelable(false)
            // Check if the fragment is still added to the activity
            if (!savedCardsDialog?.isAdded()!!) {
                savedCardsDialog?.show(
                    supportFragmentManager, SavedCardsDialog::class.java.toString()
                )
            }
        }
    }

    private fun isCanProceedFragmentTransaction(fragmentManager: FragmentManager): Boolean {
        return !isFinishing && !fragmentManager.isStateSaved && !fragmentManager.isDestroyed
    }

    override fun firebaseECommercePurchaseEvent(orderedItem: List<MenuItem>, total: Double) {
        FirebaseECommerceEventUtil.purchaseEvent(this, paymentPresenter!!.menuItemsList, total)
        FacebookAppEventUtil.purchaseEvent(this, paymentPresenter!!.menuItemsList, total)
    }

    override fun firebaseAddPaymentInfoEvent() {
        try {
            if (this != null) {
                addPaymentInfoEvent(this, paymentPresenter!!.menuItemsList, totalValue)
                val orderPayment = CacheUtil.getInstance().orderPayment
                if (orderPayment != null) {
                    addPaymentInfoEvent(this, paymentPresenter!!.menuItemsList, totalValue)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun firebaseApplyPromotionEvent(
        promotionId: String, promotionName: String, orderedItems: List<MenuItem>
    ) {
        FirebaseECommerceEventUtil.applyPromotionEvent(
            this, promotionId, promotionName, paymentPresenter!!.menuItemsList
        )
    }

    override fun updateMenuItems() {
//        menuItemsRecyclerViewAdapter.notifyDataSetChanged();
    }

    override fun showMoyasarSheet(
        orderId: String, validatedTotal: Double, menuItems: String, apiKey: String
    ) {
        Handler().post {
            val intent = Intent(this@CheckoutActivity, MoyasarActivity::class.java)
            intent.putExtra(MoyasarActivity.EXTRA_ORDER_ID, orderId)
            intent.putExtra(MoyasarActivity.EXTRA_AMOUNT, validatedTotal)
            intent.putExtra(MoyasarActivity.EXTRA_MENU_ITEMS, menuItems)
            intent.putExtra(MoyasarActivity.EXTRA_API_KEY, apiKey)
            moyasarActivityResultLauncher!!.launch(intent)
        }
    }

    override fun showOrderType(checkout: Boolean) {
        val storeId = CacheUtil.getOrderStoreId()
        val intent = Intent(this, OrderTypeActivity::class.java)
        //        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("checkout", true)
        startActivity(intent)
    }

    private fun isCardPaymentAllowed(): Boolean {
        val application = CacheUtil.getInstance().application
        val paymentTypes = mutableListOf<PaymentType>()
        if (CacheUtil.getInstance().orderType == OrderType.DELIVERY) {
            paymentTypes.addAll(application.attributes.getDeliverPaymentTypes());
        } else paymentTypes.addAll(application.attributes.getPickupPaymentTypes());
        return paymentTypes.contains(PaymentType.CARD)
    }

    override fun showSelectedPayment() {
        val selectedPayment = CacheUtil.getInstance().orderPayment
        val selectedCard = CacheUtil.getInstance().customCard
        if (selectedCard != null && selectedCard.attributes.isDefault && isCardPaymentAllowed()) {
            val cardPair = setupSaveCard(selectedCard)
            CacheUtil.getInstance().setCustomerCards(Gson().toJson(selectedCard))
            CacheUtil.getInstance().orderPayment = PaymentType.CARD
            binding!!.layoutPayment.ivSelectedPayment.setImageResource(cardPair.first)
            binding!!.layoutPayment.tvSelectedPayment.text =
                cardPair.second + selectedCard.attributes.last4Digits
        } else {
            val isUserNotSelectedAnyPaymentMethod =
                selectedPayment == PaymentType.NONE

            if (isUserNotSelectedAnyPaymentMethod) {
                binding!!.layoutPayment.tvSelectedPayment.visibility = View.VISIBLE
                binding!!.layoutPayment.ivSelectedPayment.visibility = View.VISIBLE
                binding!!.layoutPayment.ivSelectedPayment.setImageResource(R.drawable.ic_no_selected_payment_method)
                binding!!.layoutPayment.tvSelectedPayment.text = "-"
            } else {
                binding!!.layoutPayment.tvSelectedPayment.visibility = View.VISIBLE
                binding!!.layoutPayment.ivSelectedPayment.visibility = View.VISIBLE
            }

            if (selectedPayment == PaymentType.CASH) {
                binding!!.layoutPayment.tvSelectedPayment.text =
                    CacheUtil.getInstance().getTranslations(Text.CASH, "Cash")
                binding!!.layoutPayment.ivSelectedPayment.setImageResource(R.drawable.ic_common_payment_type_cash)
            } else if (selectedPayment == PaymentType.CARD) {
                if (selectedCard != null) {
                    val cardPair = setupSaveCard(selectedCard)
                    binding!!.layoutPayment.ivSelectedPayment.setImageResource(cardPair.first)
                    binding!!.layoutPayment.tvSelectedPayment.text =
                        cardPair.second + selectedCard.attributes.last4Digits
                } else {
                    binding!!.layoutPayment.tvSelectedPayment.text =
                        CacheUtil.getInstance().getTranslations(Text.CARD, "Card")
                    binding!!.layoutPayment.ivSelectedPayment.setImageResource(R.drawable.ic_common_payment_type_card)
                }
            } else if (selectedPayment == PaymentType.CARD_ON_DELIVERY || selectedPayment == PaymentType.CARD_ON_PICKUP) {
                binding!!.layoutPayment.tvSelectedPayment.text =
                    if (CacheUtil.getInstance().orderType === OrderType.DELIVERY) {
                        CacheUtil.getInstance()
                            .getTranslations(Text.CARD_ON_DELIVERY, "Card on delivery")
                    } else CacheUtil.getInstance()
                        .getTranslations(Text.CARD_ON_PICKUP, "Card on Pickup")
                binding!!.layoutPayment.ivSelectedPayment.setImageResource(R.drawable.ic_common_payment_type_cod)
            }
        }
    }

    override fun insufficientLoyaltyPoints() {
        binding?.layoutPaymentSummary?.apply {
            loyaltypointsInput = 0

            grpRedeemedPoints.gone()
        }
    }

    private fun setupSaveCard(selectedCard: CustomerCard): Pair<Int, String> {
        var res = R.drawable.ic_card_new
        var cardLabel = "Visa ****"
        if (selectedCard.attributes.brand.equals("visa", true)) {
            res = R.drawable.ic_visa
            cardLabel = "Visa ****"
        } else if (selectedCard.attributes.brand.endsWith("mastercard", true)) {
            res = R.drawable.ic_mastercard
            cardLabel = "Master ****"
        } else if (selectedCard.attributes.brand.endsWith("amex", true)) {
            res = R.drawable.ic_amex_card
            cardLabel = "Amex ****"
        }
        return Pair(res, cardLabel)
    }

    private fun showPaymentMethod() {
        val intent = Intent(this, ChoosePaymentActivity::class.java)
        startActivity(intent)
    }

    private fun inputCvvDialog(orderId: String, card: CustomerCard) {
        cvvDialogAlert = CVVDialogAlert.newInstance(object : OnCVVAction {
            override fun onClose() {
                cvvDialogAlert!!.dismiss()
            }

            override fun onProceed(cvv: String) {
                cvvDialogAlert!!.dismiss()
                showLoadingDialog()
                checkTokenGenerated(card.attributes.getToken(), cvv, false, orderId)
            }
        }, card)
        cvvDialogAlert?.setCancelable(false)
        cvvDialogAlert?.show(supportFragmentManager, CVVDialogAlert::class.java.toString())
    }

    override fun onDateSelected(
        materialCalendarView: MaterialCalendarView, calendarDay: CalendarDay, b: Boolean
    ) {
//        oneDayDecorator.setDate(calendarDay.getDate());
//        daySelected = calendarDay.getDate().toString();
//        widget.invalidateDecorators();
    }

    override fun paymentSucceed(charge: Charge) {
        logInfo(BuildConfig.TAG + " - Order ID: " + paymentPresenter!!.orderId + " -> Tap Card Payment Success")
        paymentPresenter!!.verifyTap(true, charge.id)
    }

    override fun paymentFailed(charge: Charge?) {
        logError(BuildConfig.TAG + " - Order ID: " + paymentPresenter!!.orderId + " -> Tap Card Payment Failed")
        if (charge != null) {
            sendOrderLog(
                ApiLogLevel.ERROR,
                orderId,
                paymentPresenter!!.paymentProvider,
                "Card Payment Failed (Charge: " + Gson().toJson(charge) + ")"
            )
            paymentPresenter!!.verifyTap(false, charge.id)
        } else {
            sendOrderLog(
                ApiLogLevel.ERROR,
                orderId,
                paymentPresenter!!.paymentProvider,
                "Card Payment Failed"
            )
            showCardFailed()
        }
    }

    override fun authorizationSucceed(authorize: Authorize) {}
    override fun authorizationFailed(authorize: Authorize) {
        sendOrderLog(
            ApiLogLevel.ERROR,
            orderId,
            paymentPresenter!!.paymentProvider,
            "Card Payment Failed (Reason: authorizationFailed)"
        )
        showCardFailed()
    }

    override fun cardSaved(charge: Charge) {}
    override fun cardSavingFailed(charge: Charge) {}
    override fun cardTokenizedSuccessfully(token: Token) {}
    override fun savedCardsList(cardsList: CardsList) {}
    override fun sdkError(goSellError: GoSellError?) {
        logError(BuildConfig.TAG + " - Order ID: " + paymentPresenter!!.orderId + " -> Tap Card Payment SDK Error")
        if (goSellError != null) {
            sendOrderLog(
                ApiLogLevel.ERROR,
                orderId,
                paymentPresenter!!.paymentProvider,
                "Card Payment Failed (Reason: sdkError " + Gson().toJson(goSellError) + ")"
            )
        } else {
            sendOrderLog(
                ApiLogLevel.ERROR,
                orderId,
                paymentPresenter!!.paymentProvider,
                "Card Payment Failed (Reason: sdkError)"
            )
        }
        showCardFailed()
    }

    override fun sessionIsStarting() {}
    override fun sessionHasStarted() {}
    override fun sessionCancelled() {
        sendOrderLog(
            ApiLogLevel.INFO,
            orderId,
            paymentPresenter!!.paymentProvider,
            "Card Payment sessionCancelled"
        )
        sendOrderLog(
            ApiLogLevel.WARNING,
            orderId,
            paymentPresenter!!.paymentProvider,
            "Card Payment sessionCancelled"
        )
    }

    override fun sessionFailedToStart() {
        sendOrderLog(
            ApiLogLevel.ERROR,
            orderId,
            paymentPresenter!!.paymentProvider,
            "Card Payment Failed (Reason: sessionFailedToStart)"
        )
        showCardFailed()
    }

    override fun invalidCardDetails() {
        sendOrderLog(
            ApiLogLevel.ERROR,
            orderId,
            paymentPresenter!!.paymentProvider,
            "Card Payment Failed (Reason: invalidCardDetails)"
        )
        showCardFailed()
    }

    override fun backendUnknownError(message: String) {
        sendOrderLog(
            ApiLogLevel.ERROR,
            orderId,
            paymentPresenter!!.paymentProvider,
            "Card Payment Failed (Reason: backendUnknownError $message)"
        )
        showCardFailed()
    }

    override fun invalidTransactionMode() {
        sendOrderLog(
            ApiLogLevel.ERROR,
            orderId,
            paymentPresenter!!.paymentProvider,
            "Card Payment Failed (Reason: invalidTransactionMode)"
        )
        showCardFailed()
    }

    override fun invalidCustomerID() {
        sendOrderLog(
            ApiLogLevel.ERROR,
            orderId,
            paymentPresenter!!.paymentProvider,
            "Card Payment Failed (Reason: invalidCustomerID)"
        )
        showCardFailed()
    }

    override fun userEnabledSaveCardOption(saveCardEnabled: Boolean) {}
    override fun cardTokenizedSuccessfully(token: Token, saveCardEnabled: Boolean) {}
    private fun checkTokenGenerated(
        token: String, cvv: String?, isSave: Boolean, orderIds: String
    ) {
        val body = JsonObject()
        body.addProperty("order", orderIds)
        body.addProperty("token", token)
        body.addProperty("save-card", isSave)
        if (cvv != null && !cvv.isEmpty()) {
            body.addProperty("cvv", cvv)
        }
        OrdersApiCall.getInstance().checkoutAuthorize(body, object : ApiRequestListener {
            override fun onSuccess(`object`: Any) {
                dismissDialogs()
                if (`object` is JSONObject) {
                    try {
                        val data = `object`
                        val isApproved = data.getBoolean("is-approved")
                        if (isApproved) {
                            if (dialogCheckout != null) {
                                dialogCheckout!!.handle3ds(data)
                            } else {
                                checkoutForce(data)
                            }
                        } else {
                            onError(
                                CacheUtil.getInstance()
                                    .getTranslations(Text.CARD_PAYMENT_FAILED_PAY_WITH_CASH)
                            )
                        }
                    } catch (ex: JSONException) {
                        ex.printStackTrace()
                        onError(ex)
                    }
                } else {
                    dismissDialogs()
                    onError(
                        CacheUtil.getInstance()
                            .getTranslations(Text.CARD_PAYMENT_FAILED_PAY_WITH_CASH)
                    )
                }
            }

            override fun onError(o: Any) {
                AppLogger.printRetrofitError(o)
                if (o is JSONObject) {
                    sendOrderLog(
                        ApiLogLevel.ERROR,
                        orderId,
                        paymentPresenter!!.paymentProvider,
                        "Card Payment Failed (Reason: $o)"
                    )
                }
                dismissDialogs()
                showCardFailed()
            }
        })
    }

    private fun checkoutForce(data: JSONObject) {/*Intent intent = new Intent(PaymentActivity.this, CustomCheckoutActivity.class);
        intent.putExtra(CustomCheckoutActivity.EXTRA_ORDER_ID , orderId);
        intent.putExtra(CustomCheckoutActivity.EXTRA_PAYMENT_PROVIDER, paymentPresenter.getPaymentProvider());
        intent.putExtra(CustomCheckoutActivity.EXTRA_3DS_DATA, data.toString());
        checkoutActivityLauncher.launch(intent);
        isFromCheckout = true;*/
        showCheckoutPaymentForm(orderId)

        Handler().postDelayed({ dialogCheckout?.handle3ds(data) }, 400)
    }

    private fun showPanel() {
//        if (CacheUtil.getInstance().getOrderPayment() == PaymentType.NONE) {
//            Toast.makeText(CheckoutActivity.this, CacheUtil.getInstance().getTranslations(Text.SELECT_PAYMENT_METHOD), Toast.LENGTH_SHORT).show();
//        } else {
//            if (isRemove) {
//                widget.setSelectedDate(LocalDate.now());
//                setUpDatePicker();
//            }
//            slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
//        }
    }

    private fun PermissionManager.RationaleCallback.showRationale() {
        DialogUtils.showConfirmDialog(
            context = this@CheckoutActivity,
            title = getString(R.string.permission_denied),
            body = getString(R.string.permission_read_write_denied_msg),
            positiveButtonText = getString(R.string.try_again_later),
            negativeButtonText = getString(R.string.cancel),
            positiveClickListener = { onRationaleRequested() },
            message = getString(R.string.permission_read_write_denied_msg)
        )
    }

    fun setUpdatedPhoneNumber(phoneNumber: String) {
        binding?.tvMobileNumber?.text = phoneNumber
    }

    override fun otpResult(transactionId: String, amount: String, invalidMukafaa: Boolean) {
        if (!invalidMukafaa && transactionId.isNotEmpty()) {
            paymentPresenter?.mukafaaTransactionId = transactionId
            val validatedTotal = paymentPresenter?.validatedTotal ?: 0.0
            val orderPayment = CacheUtil.getInstance().orderPayment
            val amountDbl = amount.toDouble()
            if (orderPayment == PaymentType.NONE && validatedTotal == amountDbl && loyaltypointsInput <= 0 && CacheUtil.getInstance()
                    .getOrderType() != OrderType.DELIVERY
            ) {
                selectPaymentTypeCash()
            }
            binding?.apply {
                layoutAlrahji.switchAlrahji.apply {
                    setOnCheckedChangeListener(null)
                    isChecked = true
                    setOnCheckedChangeListener(alrahjiCheckChangeListener)
                }
            }
            bottomSheetAlrhaji?.dismissAllowingStateLoss()
        } else {
            CacheUtil.getInstance().orderPayment = PaymentType.NONE
            binding?.apply {
                layoutAlrahji.switchAlrahji.apply {
                    setOnCheckedChangeListener(null)
                    isChecked = false
                    setOnCheckedChangeListener(alrahjiCheckChangeListener)
                }
            }
        }

        paymentPresenter?.validateOrder(
            false, CacheUtil.getInstance().orderStore, null, 0, false
        )
    }

    override fun onClose() {
        paymentPresenter?.let {
            if (!it.hasMukafaaTransactionId()) {
                binding?.apply {
                    layoutAlrahji.switchAlrahji.apply {
                        setOnCheckedChangeListener(null)
                        isChecked = false
                        setOnCheckedChangeListener(alrahjiCheckChangeListener)
                    }
                }
                bottomSheetAlrhaji?.dismissAllowingStateLoss()
                bottomSheetAlrhaji = null
            }
        }
    }


}
