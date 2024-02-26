package com.herfy.checkout_temp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Build

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback

import com.moneyhash.sdk.android.payment.PaymentStatus
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.foodics.permissionprovider.PermissionManager
import com.foodics.permissionprovider.PermissionResult
import com.foodics.permissionprovider.permissionProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.maps.android.SphericalUtil
import com.herfy.BuildConfig
import com.herfy.R
import com.herfy.analytics.FacebookAppEventUtil
import com.herfy.analytics.FacebookAppEventUtil.Companion.addPaymentInfoEvent
import com.herfy.analytics.FirebaseECommerceEventUtil
import com.herfy.analytics.FirebaseECommerceEventUtil.Companion.addPaymentInfoEvent
import com.herfy.analytics.FirebaseECommerceEventUtil.Companion.applyPromotionEvent
import com.herfy.base.BaseActivity
import com.herfy.checkout.databinding.ActivityCheckoutPageBinding
import com.herfy.decorator.BeforeDateDecorator
import com.herfy.decorator.OneDayDecorator
import com.herfy.firebase.FirebaseService
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
import com.herfy.payment.views.MoyasarActivity
import com.herfy.payment.views.OrderDialogFragment
import com.herfy.ratings.AppRatingBroadcastReceiver
import com.herfy.util.ApiLogLevel
import com.herfy.util.ApiLogUtil.Companion.sendOrderLog
import com.herfy.util.AppLogger
import com.herfy.util.CurrencyUtils.Companion.updateCurrency
import com.herfy.util.ShipBookLogger.logError
import com.herfy.util.ShipBookLogger.logInfo
import com.herfy.util.StringUtil
import com.herfy.util.ViewUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.extensions.showToast
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.BuildUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DialogUtils
import com.herfy.utils.helper.GlideUtils
import com.moneyhash.sdk.android.ResultType
import com.moneyhash.sdk.android.payment.PaymentResultContract
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
import io.reactivex.BackpressureStrategy
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.shipbook.shipbooksdk.Log
import org.json.JSONException
import org.json.JSONObject
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset
import org.threeten.bp.temporal.ChronoField
import org.threeten.bp.temporal.ChronoUnit
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

//import com.herfy.constants.Text;
//import com.herfy.constants.Values;
class CheckoutActivity : BaseActivity(), PaymentContract.View, SessionDelegate,
    OnDateSelectedListener {
    private var binding: ActivityCheckoutPageBinding? = null
    var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var mMap: GoogleMap? = null
    private val currentLocation: Location? = null
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
    private val saveDialogAlert: SaveDialogAlert? = null
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
    private val disposable = CompositeDisposable()
    private val _textInput = BehaviorSubject.create<String>()
    private val textInput = _textInput.toFlowable(BackpressureStrategy.LATEST)
    private val pointsInSarTextWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
        override fun afterTextChanged(editable: Editable) {
            _textInput.onNext(editable.toString())
        }
    }
    var isFromCheckout = false
    private var moyasarActivityResultLauncher: ActivityResultLauncher<Intent>? = null
    private var isShowCardFailed = false

   private val permissionManager: PermissionManager by permissionProvider()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutPageBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        popupUpsellBackStack()
        passedTotalFromCart = intent.getDoubleExtra("totalPrice", 0.0)
        paymentPresenter = PaymentPresenter(this)
        initData()
        paymentPresenter?.getLoyalty()
        paymentPresenter?.start()
        paymentPresenter?.getPaymentTypes()
        moyasarActivityResultLauncher =
            registerForActivityResult<Intent, ActivityResult>(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result != null) {
                    if (result.resultCode == RESULT_OK) {
                        val intent = result.data
                        if (intent != null) {
                            if (intent.hasExtra(MoyasarActivity.EXTRA_MOYASAR_ID)) {
                                if (!intent.getStringExtra(MoyasarActivity.EXTRA_MOYASAR_ID)
                                        .equals(
                                            "failed",
                                            ignoreCase = true
                                        ) && !intent.getStringExtra(MoyasarActivity.EXTRA_MOYASAR_ID)
                                        .equals("canceled", ignoreCase = true)
                                ) {
                                    paymentPresenter!!.sendUpsells()
                                } else {
                                    val errorMessage =
                                        intent.getStringExtra(MoyasarActivity.EXTRA_MOYASAR_STATUS)
                                    val orderId =
                                        intent.getStringExtra(MoyasarActivity.EXTRA_ORDER_ID)
                                    if (orderId != null && errorMessage != null && errorMessage != "cancelled") {
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

    private fun popupUpsellBackStack() {
        if (intent.getBooleanExtra("fromUpsellPage", false)) {
            try {
                if (currentMainActivity != null) {
                    Handler(Looper.getMainLooper()).postDelayed(
                        { currentMainActivity!!.popUpsell() },
                        300
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun requestPermissionLocation() {
        permissionManager.requestPermissions(
            permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            showRationale = { rationaleCallback ->
                rationaleCallback.showRationale()
            }
        ) { result ->
            when (result) {
                is PermissionResult.Denied -> {
                    showToast(result.deniedPermissions.joinToString(", "))
                }

                PermissionResult.Granted -> {
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
            false,
            CacheUtil.getInstance().orderStore,
            null,
            0,
            isShowCardFailed
        )
        paymentPresenter!!.fetchCustomerCards()
        showSelectedPyament()
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
        val storeLat = CacheUtil.getInstance().orderStore.attributes.lat
        val storeLon = CacheUtil.getInstance().orderStore.attributes.lng
        val storeLocation = LatLng(storeLat, storeLon)
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val markerOptions = MarkerOptions()
        if (orderType === OrderType.DELIVERY) {
            val lat = CacheUtil.getInstance().orderAddress.attributes.lat.toDouble()
            val lon = CacheUtil.getInstance().orderAddress.attributes.longt.toDouble()
            val addressLocation = LatLng(lat, lon)
            val bitmapDrawable = ContextCompat.getDrawable(
                this, R.drawable.ic_address_location
            ) as BitmapDrawable?
            if (bitmapDrawable != null) {
                val bitmap = Bitmap.createScaledBitmap(bitmapDrawable.bitmap, 110, 110, true)
                val canvas = Canvas(bitmap)
                val paint = Paint()
                val filter: ColorFilter = PorterDuffColorFilter(
                    Color.parseColor(CacheUtil.getInstance().colorMain),
                    PorterDuff.Mode.SRC_IN
                )
                paint.setColorFilter(filter)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                markerOptions.position(addressLocation)
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
            }
        } else {
            markerOptions.position(storeLocation)
            val view = inflater.inflate(R.layout.marker_store_new, null)
            val icon = BitmapDescriptorFactory.fromBitmap(
                ViewUtil.createDrawableFromView(
                    this, view
                )
            )
            markerOptions.icon(icon)
            markerOptions.anchor(0.5f, 1f)
        }


//        MarkerOptions markerOptions = new MarkerOptions();
//        markerOptions.position(storeLocation);
        val supportMapFragment =
            supportFragmentManager.findFragmentById(R.id.checkout_map) as SupportMapFragment?
        supportMapFragment!!.getMapAsync { googleMap ->
            mMap = googleMap
            mMap!!.uiSettings.isZoomControlsEnabled = false
            mMap!!.uiSettings.isCompassEnabled = false
            mMap!!.uiSettings.isMapToolbarEnabled = false
            mMap!!.isMyLocationEnabled = true
            if (orderType === OrderType.DELIVERY) {
                mMap!!.addMarker(markerOptions)
                mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(markerOptions.position, 13f))
            } else {
                mMap!!.addMarker(markerOptions)
                mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(markerOptions.position, 13f))
            }
        }
        if (orderType !== OrderType.DELIVERY) {
            val task = fusedLocationProviderClient!!.lastLocation
            task.addOnSuccessListener { location ->
                if (location != null) {
                    val myLocation = LatLng(location.latitude, location.longitude)
                    mMap!!.addPolyline(
                        PolylineOptions()
                            .add(storeLocation, myLocation).width(10f)
                            .color(Color.parseColor("#0064AC"))
                    )
                    showCurveLine(storeLocation, myLocation, 0.5)
                }
            }
        }
    }

    private fun showCurveLine(storeLocation: LatLng, currentLocation: LatLng, k: Double) {
        val distanceBetween = SphericalUtil.computeDistanceBetween(storeLocation, currentLocation)
        val heading = SphericalUtil.computeDistanceBetween(storeLocation, currentLocation)

        //midpoint position
        val p = SphericalUtil.computeOffset(storeLocation, distanceBetween * 0.5, heading)
        val x = (1 - k * k) * distanceBetween * 0.5 / (2 * k)
        val r = (1 + k * k) * distanceBetween * 0.5 / (2 * k)
        val c = SphericalUtil.computeOffset(p, x, heading + 90.0)
        val polygon = PolygonOptions()
        val h1 = SphericalUtil.computeHeading(c, storeLocation)
        val h2 = SphericalUtil.computeHeading(c, currentLocation)
        val numpoints = 200
        val step = (h2 - h1) / numpoints
        val temp = ArrayList<LatLng>()
        for (i in 0 until numpoints) {
            val latLng = SphericalUtil.computeOffset(c, r, h1 + i * step)
            polygon.add(latLng)
            temp.add(latLng)
        }
        for (i in temp.size - 1 downTo 1) {
            polygon.add(temp[i])
        }

//        for (int i=0; i < numpoints; i++) {
//            LatLng pi = SphericalUtil.computeOffset(c, r, h1 + i * step);
//            options.add(pi);
//        }

//        mMap.addPolyline(options.width(10).color(Color.BLACK).geodesic(false).pattern(pattern));
//        mMap.addPolyline(options.width(10).color(Color.BLACK).geodesic(false));
        mMap!!.addPolygon(polygon)
        temp.clear()
    }

    override fun setupViews() {
        orderType = CacheUtil.getInstance().orderType
        showMap()
        requestPermissionLocation()
        binding!!.tvEditMobileNumber.ninjaTap(lifecycleScope) {
            // todo wait for the designer what screen should be called here
        }
        //        rvOrderSummary.setLayoutManager(new LinearLayoutManager(this));
//        menuItemsRecyclerViewAdapter = new OrderSummaryItemRecyclerViewAdapter(paymentPresenter);
//        rvOrderSummary.setAdapter(menuItemsRecyclerViewAdapter);


//        tvChangeOrderType.setOnClickListener( new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showOrderType(true);
//            }
//        });

        /* ivShowOrders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rvOrderSummary.getVisibility() == View.VISIBLE) {
                    rvOrderSummary.setVisibility(View.GONE);
                } else {
                    rvOrderSummary.setVisibility(View.VISIBLE);
                }
            }
        });*/binding!!.layoutPayment.tvEditPaymentMethod.setOnClickListener { showPaymentMethod() }
        val orderStore = CacheUtil.getInstance().orderStore
        if (orderStore != null && orderStore.attributes != null) {
            val branchImage = orderStore.attributes.image
            if (branchImage != null) {
                isBranchImageAvailable = branchImage.isNotEmpty()
            }
        }
        /*if (isScheduled) {
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
        binding!!.tvMobileNumber.text = AuthUtil.getInstance().customer.attributes.mobile
        binding!!.toolbarLayout.btnBack.setOnClickListener { onBackPressed() }
        if (CacheUtil.getInstance().orderStore != null) {
            paymentPresenter!!.validateOrder(
                false,
                CacheUtil.getInstance().orderStore,
                null,
                0,
                false
            )
        }
        if (orderType === OrderType.DELIVERY) {
            /*showLoadingDialog();
            Geocoder geocoder = new Geocoder(PaymentActivity.this, LocaleUtil.getInstance().isEnglish() ? new Locale("en", "us") : new Locale("ar", "sa"));
            paymentPresenter.getStore(geocoder);*/
            if (CacheUtil.getInstance().orderAddress != null) {
                binding!!.tvBranchName.text = CacheUtil.getInstance().orderAddress.attributes.line1
            }
            binding!!.grpDeliveryNote.visibility = View.VISIBLE
            binding!!.tvDeliveryInstruction.text =
                CacheUtil.getInstance().orderAddress.attributes.instructions
        } else {
            /*Geocoder geocoder = new Geocoder(PaymentActivity.this, LocaleUtil.getInstance().isEnglish() ? new Locale("en", "us") : new Locale("ar", "sa"));
            paymentPresenter.getAddresses(geocoder);*/
            if (CacheUtil.getInstance().orderStore != null) {
                binding!!.tvBranchName.text = CacheUtil.getInstance().orderStore.attributes.name
            }
            binding!!.grpDeliveryNote.visibility = View.GONE
        }
        when (CacheUtil.getInstance().orderType) {
            OrderType.PICKUP -> {
                binding!!.tvOrderType.text = CacheUtil.getInstance().getTranslations(Text.PICKUP)
                binding!!.ivOrderType.setImageResource(com.herfy.checkout.R.drawable.ic_herfy_pickup)
            }

            OrderType.DINE_IN -> {
                binding!!.tvOrderType.text = CacheUtil.getInstance().getTranslations(Text.DINE_IN)
                binding!!.ivOrderType.setImageResource(com.herfy.checkout.R.drawable.ic_dine_in)
            }

            OrderType.DELIVERY -> {
                binding!!.tvOrderType.text = CacheUtil.getInstance()
                    .getTranslations(Text.DELIVERY_ADDRESS, "Delivery Address")
                binding!!.ivOrderType.setImageResource(com.herfy.checkout.R.drawable.ic_herfy_delivery)
            }

            OrderType.CURBSIDE -> {
                binding!!.tvOrderType.text =
                    CacheUtil.getInstance().getTranslations(Text.CURBSIDE_PICKUP, "Curbside")
                binding!!.ivOrderType.setImageResource(com.herfy.checkout.R.drawable.ic_pickup_curbside)
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

//                    String branch;
//                    LatLng latLng;
//                    String imageUrl;
//                    int promisedTime;
//                    if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
//                        promisedTime = CacheUtil.getInstance().getOrderStore().getAttributes().getPromisedTimeDelta().getDelivery();
//                        branch = (CacheUtil.getInstance().getOrderAddress().getAttributes().getLine1());
//                        latLng = new LatLng(Double.parseDouble(CacheUtil.getInstance().getOrderAddress().getAttributes().getLat()), Double.parseDouble(CacheUtil.getInstance().getOrderAddress().getAttributes().getLongt()));
//                        imageUrl = null;
//                    } else {
//                        promisedTime = CacheUtil.getInstance().getOrderStore().getAttributes().getPromisedTimeDelta().getPickup();
//                        branch = (CacheUtil.getInstance().getOrderStore().getAttributes().getName());
//                        latLng = new LatLng(CacheUtil.getInstance().getOrderStore().getAttributes().getLat(), CacheUtil.getInstance().getOrderStore().getAttributes().getLng());
//                        imageUrl = CacheUtil.getInstance().getOrderStore().getAttributes().getImage();
//                    }
//                    paymentOrderTimeDialog = PaymentOrderTimeDialog.newInstance(CacheUtil.getInstance().getOrderType(), promisedTime, branch, imageUrl, latLng, new PaymentOrderTimeDialog.OnAction() {
//                        @Override
//                        public void close() {
//                            paymentOrderTimeDialog.dismiss();
//                        }
//
//                        @Override
//                        public void confirm() {

//                        }
//                    });
//                    paymentOrderTimeDialog.setCancelable(false);
//                    paymentOrderTimeDialog.show(getSupportFragmentManager(), CarMakerDialog.class.toString());
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
                    this@CheckoutActivity,
                    CacheUtil.getInstance().getTranslations(Text.SELECT_PAYMENT_METHOD),
                    Toast.LENGTH_SHORT
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
                    false,
                    CacheUtil.getInstance().orderStore,
                    null,
                    0,
                    false
                )
            }
        }
        /*loyaltyPointsPayment.setOnClickListener(view -> {
            if (!loyaltyPointsPayment.isSelected()) {
                loyaltyPointsPayment.setSelected(true);
                loyaltyPointsPaymentContainer.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
                pointsLayout.setVisibility(View.VISIBLE);
            } else {
                loyaltyPointsPayment.setSelected(false);
                loyaltyPointsPaymentContainer.setStrokeColor(Color.TRANSPARENT);
                pointsLayout.setVisibility(View.GONE);
                paymentPresenter.setLoyaltyPointsUsed(0);
                etLoyaltyPoints.setText("");
                loyaltypointsInput = 0;
                paymentPresenter.validateOrder(false, CacheUtil.getInstance().getOrderStore(), null, 0,false);
            }
        });

        pointsLayout.setOnClickListener(v -> {
        });*/
        val loyalty = AuthUtil.getInstance().customerLoyalty
        if (loyalty.attributes != null) {
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
            if (loyalty.attributes.currentBalanceAmount.toInt()
                    .toDouble() == loyalty.attributes.currentBalanceAmount
            ) {
                CacheUtil.getInstance().getTranslations(
                    Text.LOYALTY_POINTS_PAYMENT_LABEL, "(x) Points ((y))"
                ).replace(
                    "(x)",
                    AuthUtil.getInstance().customerLoyalty.attributes.currentBalance.toString()
                ).replace("(y)", updateCurrency(loyalty.attributes.currentBalanceAmount, true).toString())
                //loyaltyLabel.setText(StringUtil.localize(String.valueOf(AuthUtil.getInstance().getCustomerLoyalty().getAttributes().getCurrentBalance()))+" Points ("+StringUtil.localize(String.valueOf(loyalty.getAttributes().getCurrentBalanceAmount())) +")");
            } else {
                binding!!.layoutLoyalty.tvLoyaltyPoints.text = CacheUtil.getInstance().getTranslations(
                    Text.LOYALTY_POINTS_PAYMENT_LABEL, "(x) Points ((y))"
                ).replace(
                    "(x)",
                    StringUtil.getFormattedAmount(AuthUtil.getInstance().customerLoyalty.attributes.currentBalance.toDouble())
                ).replace("(y)", updateCurrency(loyalty.attributes.currentBalanceAmount, true).toString())
                //loyaltyLabel.setText(StringUtil.getFormattedAmount(AuthUtil.getInstance().getCustomerLoyalty().getAttributes().getCurrentBalance())+" Points ("+StringUtil.getFormattedAmount(loyalty.getAttributes().getCurrentBalanceAmount()) +")");
            }
        }
        disposable.add(textInput.debounce(300, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.io())
            .subscribe(
                { pointsText: String ->
                    runOnUiThread {
                        if (pointsText.length > 0) {
                            val userSetPoints = StringUtil.toEnglish(pointsText).toDouble()
                            val currentBalance =
                                AuthUtil.getInstance().customerLoyalty.attributes.currentBalanceAmount
                            val pointConversion =
                                AuthUtil.getInstance().customerLoyalty.attributes.pointConversion.toDouble()
                            val points =
                                if (loyalty.attributes.currentBalanceAmount > paymentPresenter!!.menuItemsTotalPrice) paymentPresenter!!.menuItemsTotalPrice * pointConversion else loyalty.attributes.currentBalance
                                    .toDouble()
                            binding!!.layoutLoyalty.etPoints.removeTextChangedListener(
                                pointsInSarTextWatcher
                            )
                            if (userSetPoints >= currentBalance) {
                            } else {
                                binding!!.layoutLoyalty.etPoints.setText(StringUtil.localize(binding!!.layoutLoyalty.etPoints.text.toString()))
                            }
                            binding!!.layoutLoyalty.etPoints.setSelection(binding!!.layoutLoyalty.etPoints.text.toString().length)
                            binding!!.layoutLoyalty.etPoints.addTextChangedListener(
                                pointsInSarTextWatcher
                            )
                            //showLoadingDialog();
                            loyaltypointsInput =
                                binding!!.layoutLoyalty.etPoints.text.toString().toInt()
                            paymentPresenter!!.validateOrder(
                                false,
                                CacheUtil.getInstance().orderStore,
                                null,
                                loyaltypointsInput,
                                false
                            )
                        } else {
                            binding!!.layoutLoyalty.etPoints.setSelection(0)
                            paymentPresenter!!.setLoyaltyPointsUsed(0)
                        }
                    }
                }) { onError: Throwable? -> })
        binding!!.layoutLoyalty.etPoints.addTextChangedListener(pointsInSarTextWatcher)


        val isDelivery = false
        val customer = AuthUtil.getInstance().customer
        val isSignedIn = AuthUtil.getInstance().isSignedIn
        val isFullSignedIn =
            isSignedIn && !customer.attributes.accountType.equals("guest", ignoreCase = true)

        try {
            var paymentTypes: Set<PaymentType?> =
                CacheUtil.getInstance().application.attributes.getDeliverPaymentTypes()
            if (!isDelivery) {
                paymentTypes =
                    CacheUtil.getInstance().application.attributes.getPickupPaymentTypes()
                binding!!.tvBranchName.text = CacheUtil.getInstance().orderStore.attributes.name;
            } else {
                binding!!.tvBranchName.text = CacheUtil.getInstance().orderStore.attributes.name;
//                tvBranchAddress.setText(CacheUtil.getInstance().getOrderAddress().getAttributes().getLine1());
            }
            val isEligibleForLoyaltyDiscount =
                isFullSignedIn && paymentTypes.contains(PaymentType.LOYALTY) && passedTotalFromCart > 0
            binding!!.layoutLoyalty.loyaltyPointsPaymentContainer.setVisible(isEligibleForLoyaltyDiscount)
            binding!!.ivOrderType.setVisible(isBranchImageAvailable)
            GlideUtils.loadImageUrl(binding!!.ivOrderType, url = CacheUtil.getInstance().orderStore.attributes.image,
                this)
        } catch (e: Exception) {
            e.printStackTrace()
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
        binding!!.tvEditMobileNumber.text =
            CacheUtil.getInstance().getTranslations(Text.EDIT, "Edit")
        binding!!.tvDeliveryInstructionLabel.text = CacheUtil.getInstance().getTranslations(
            Text.DELIVERY_INSTRUCTION,
            "Delivery Instruction"
        )
        binding!!.layoutPayment.tvPaymentMethod.text = CacheUtil.getInstance()
            .getTranslations(Text.PAYMENT_METHOD_NOT_CAPS, "Payment Method")
        //        tvChangeOrderType.setText(CacheUtil.getInstance().getTranslations(Text.CHANGE, "Change").toUpperCase());
        binding!!.layoutLoyalty.tvPointsLabel.text =
            CacheUtil.getInstance().getTranslations(Text.POINTS, "Points")
        binding!!.layoutLoyalty.tvSarAmountLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.SAR_AMOUNT, "SAR Amount")

        binding!!.layoutPaymentSummary.tvDiscountLabel.text =
            CacheUtil.getInstance().getTranslations(Text.PROMO_DISCOUNT)
        binding!!.layoutLoyalty.tvLoyaltyPointsLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.LOYALTY_USE_POINTS, "Use Loyalty Points")
        val orderType = CacheUtil.getInstance().orderType
        if (orderType === OrderType.DELIVERY) {
            binding!!.tvTimeLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.DELIVERY_TIME, "Delivery Time")
            val mins =
                CacheUtil.getInstance().orderStore.attributes.promisedTimeDelta.delivery.toString() + " " + CacheUtil.getInstance()
                    .getTranslations("mins", "mins")
            binding!!.tvWithin.text = CacheUtil.getInstance()
                .getTranslations(Text.WITHIN, "Within $mins")
        } else {
            binding!!.tvTimeLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.PICKUP_TIME, "Pickup Time")
            val minsString =
                CacheUtil.getInstance().orderStore.attributes.promisedTimeDelta.pickup.toString()

            val minsValue = minsString.toInt()
            val currentUtcTime = Instant.now()
            val minTime = currentUtcTime.plus(
                minsValue.toLong(),
                ChronoUnit.MINUTES
            )

            val maxTime = currentUtcTime.plus(
                (minsValue + 1).toLong(),
                ChronoUnit.MINUTES
            ) // Adding one more minute for upper bound of deliveryRangeTime
            val minMinutes = minTime.atZone(ZoneOffset.UTC)[ChronoField.MINUTE_OF_HOUR]
            val maxMinutes = maxTime.atZone(ZoneOffset.UTC)[ChronoField.MINUTE_OF_HOUR]
            // todo temporary code for now ask BE where to get the start minutes range
            val deliveryRangeTime =
                "$minMinutes - $maxMinutes " +  CacheUtil.getInstance()
                    .getTranslations(
                        Text.MINS, "mins"
                    )
            binding!!.tvWithin.text = CacheUtil.getInstance()
                .getTranslations(Text.WITHIN, "Within $deliveryRangeTime")
        }
        binding!!.placeOrder.text = CacheUtil.getInstance()
            .getTranslations(Text.PLACE_ORDER, "Place Order")
        binding!!.layoutPaymentSummary.tvSubtotalLabel.text =
            CacheUtil.getInstance().getTranslations(Text.SUBTOTAL)
        binding!!.layoutPaymentSummary.tvDiscount.text =
            CacheUtil.getInstance().getTranslations(Text.PROMO_DISCOUNT)
        binding!!.layoutPaymentSummary.tvTotalLabel.text =
            CacheUtil.getInstance().getTranslations(Text.TOTAL_LABEL)
        binding!!.tvDeliveryInstructionLabel.text = CacheUtil.getInstance().getTranslations(Text.DELIVERY_INSTRUCTION, "Delivery Instruction")
        binding!!.layoutPaymentSummary.tvSubtotalLabel.text =
            CacheUtil.getInstance().getTranslations(Text.SUBTOTAL)
        binding!!.layoutPaymentSummary.tvDiscount.text =
            CacheUtil.getInstance().getTranslations(Text.PROMO_DISCOUNT)
        binding!!.totalVatDescription.text = CacheUtil.getInstance().getTranslations(Text.TOTAL_VAT_DESCRIPTION, "Total is inclusive of VAT")
    }

    override fun showPaymentTypes(paymentTypes: Set<PaymentType>) {}
    override fun showPaymentTypes(paymentTypes: ArrayList<PaymentType>) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
//        paymentTypes.forEach { payment
//
//        }
    }
    override fun setStartName(startName: String) {}
    override fun setStartAddress(startAddress: String) {}
    override fun setEndName(endName: String) {}
    override fun setEndAddress(endAddress: String) {}
    override fun updateMap(
        orderType: OrderType,
        startLocation: LatLng,
        endLocation: LatLng,
        bounds: LatLngBounds
    ) {
    }

    override fun updateTime(calendar: Calendar) {}
    override fun selectPaymentTypeCash() {}
    override fun showValidateOrderError(errors: List<String>) {}
    override fun sendOrder() {}
    override fun initializeTap(orderId: String, total: Double) {}
    override fun showCheckoutPaymentForm(orderId: String) {}
    override fun showCardFailed() {}
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
        val hasPromotion = CacheUtil.getInstance().hasPromotion()
        val total = updateCurrency(subtotalPrice, true, hasPromotion).toString()

        binding!!.layoutPaymentSummary.tvSubtotal.text =
            total
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


        /*   double mukafaaAmount = paymentPresenter.getMukafaaTransactionAmount();
        discount = discount - mukafaaAmount;*/binding!!.layoutPaymentSummary.tvDiscount.text =
            updateCurrency(discount, true, hasPromotion, discount > 0)
        binding!!.layoutPaymentSummary.grpDiscount.visibility =
            if (discount != 0.0) View.VISIBLE else View.GONE

        /*if (mukafaaAmount > 0) {
            if (paymentPresenter.hasMukafaaTransactionId() && paymentPresenter.isMukafaaEnabled()) {
                String amnt = String.valueOf(StringUtil.getFormattedPrice(mukafaaAmount));
                if (!"0".equals(amnt) && !amnt.contains("-")) amnt = "-" + amnt;
                tvMukafaaSar.setText(amnt);

                grpMukafaa.setVisibility(View.VISIBLE);
            } else {
                grpMukafaa.setVisibility(View.GONE);
            }
        } else {
            grpMukafaa.setVisibility(View.GONE);
            selectMukafaa(false, false);
        }*/if (pointsUsed != 0) {
            binding!!.layoutPaymentSummary.grpRedeemedPoints.visibility = View.VISIBLE
            val redeemedText = CacheUtil.getInstance().getTranslations(Text.REDEEMED, "redeemed")
            val color = CacheUtil.getInstance().colorMain
            val ptsRedeemed = CacheUtil.getInstance().getTranslations(Text.PTS_LABEL, "(x) pts")
                .replace("(x)", pointsUsed.toString())

//            tvRedeemPoints.setText(HtmlCompat.fromHtml("<font color=" + color + ">" + ptsRedeemed + "</font> ", HtmlCompat.FROM_HTML_MODE_LEGACY));
            binding!!.layoutPaymentSummary.tvRedeemPointsEquiv.text = CacheUtil.getInstance()
                .getTranslations(Text.LOYALTY_POINTS, "Loyalty poinst (x)")
                .replace("x", pointsUsed.toString())
            binding!!.layoutPaymentSummary.tvRedeemPointsEquiv.text =
                updateCurrency(
                    Math.min(
                        pointsSarValue,
                        paymentPresenter!!.menuItemsTotalPrice
                    ), true, hasPromotion, true
                )
        } else {
            binding!!.layoutPaymentSummary.grpRedeemedPoints.visibility = View.GONE
        }
        if (totalPrice == 0.0 && pointsUsed > 0) {
            if (deliveryCharge <= 0) {
//                chooseMethodLabel.setVisibility(View.GONE);
                /* if (cardOrderTypePayment != null) cardOrderTypePayment.setVisibility(View.GONE);
                if (cardPayment != null) cardPayment.setVisibility(View.GONE);
//                mukafaaPayment.setVisibility(View.GONE);
                if (cashPayment != null) cashPayment.setVisibility(View.GONE);*/
                binding!!.layoutLoyalty.loyaltyPointsPaymentContainer.visibility = View.VISIBLE
                //                useLoyaltyPointsLabel.setVisibility(View.VISIBLE);
//                loyaltyPointsAdditionalInfo.setVisibility(View.VISIBLE);
                /* if (mukafaaAmount > 0) {
                    mukafaaPayment.setVisibility(View.VISIBLE);
                }*/selectPaymentTypeCash()
            } else if (subtotalPrice == pointsSarValue && deliveryCharge > 0) {
//                if (cardOrderTypePayment != null) cardOrderTypePayment.setVisibility(View.VISIBLE);
//                if (cardPayment != null) cardPayment.setVisibility(View.VISIBLE);
////                mukafaaPayment.setVisibility(View.VISIBLE);
//                if (cashPayment != null) cashPayment.setVisibility(View.VISIBLE);
            }
        } else {
            paymentPresenter!!.getPaymentTypes()
            /* if (totalPrice == deliveryCharge && mukafaaAmount == 0) {
                mukafaaPayment.setVisibility(View.GONE);
            }*/
//            if (cardOrderTypePayment != null) cardOrderTypePayment.setVisibility(View.VISIBLE);
//            if (cardPayment != null) cardPayment.setVisibility(View.VISIBLE);
//            if (cashPayment != null) cashPayment.setVisibility(View.VISIBLE);
        }

        val formattedTotal = updateCurrency(
            totalPrice,
            true,
            CacheUtil.getInstance().hasPromotion()
        )
        binding!!.tvDiscountedPrice.text = formattedTotal
        binding!!.layoutPaymentSummary.tvTotal.text = formattedTotal
        totalValue = totalPrice
        binding!!.tvOriginalPrice.text = formattedTotal
        val loyalty = AuthUtil.getInstance().customerLoyalty
        /*double finalDiscount = discount;
        double finalTotal;
        if (mukafaaAmount > 0) {
            finalTotal = subtotalPrice - mukafaaAmount;
        } else {
            finalTotal = subtotalPrice;
        }*/

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
            val fragmentManager = supportFragmentManager
            if (!fragmentManager.isDestroyed) {
                val fragment = OrderDialogFragment.newInstance(false)
                fragment.show(fragmentManager, OrderDialogFragment::class.java.simpleName)
            }
            Handler().postDelayed({
                runOnUiThread {
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
    val activityResultLauncher: ActivityResultLauncher<Pair<String, ResultType>> = registerForActivityResult(
        PaymentResultContract() as ActivityResultContract<Pair<String, ResultType>, Any>,
        ActivityResultCallback<Any> { result ->
            if (result == null) return@ActivityResultCallback
            when (result) {
                is PaymentStatus.Success -> paymentPresenter?.sendUpsells()
                is PaymentStatus.Redirect -> {
                    if (result.result.component2()?.status.equals("SUCCESSFUL", ignoreCase = true)) {
                        paymentPresenter?.sendUpsells()
                    } else {
                        showCardFailed()
                    }
                }
                else -> showCardFailed()
            }
        }
    )

    override fun moneyHashContract(): ActivityResultLauncher<Pair<String, ResultType>> {
        return activityResultLauncher
    }

    override fun setOrderId(orderId: String) {}
    override fun showCardDialogList(orderId: String) {
        if (isCanProceedFragmentTransaction(supportFragmentManager)) {
            savedCardsDialog = SavedCardsDialog.newInstance(
                CacheUtil.getInstance().customerCards,
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
                    supportFragmentManager,
                    SavedCardsDialog::class.java.toString()
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
                addPaymentInfoEvent(this, CacheUtil.getInstance().orderPayment.value)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun firebaseApplyPromotionEvent(
        promotionId: String,
        promotionName: String,
        orderedItems: List<MenuItem>
    ) {
        applyPromotionEvent(this, promotionId, promotionName, paymentPresenter!!.menuItemsList)
    }

    override fun updateMenuItems() {
//        menuItemsRecyclerViewAdapter.notifyDataSetChanged();
    }

    override fun showMoyasarSheet(
        orderId: String,
        validatedTotal: Double,
        menuItems: String,
        apiKey: String
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

    override fun showSelectedPyament() {
        val selecetedPayment = CacheUtil.getInstance().orderPayment
        if (selecetedPayment == PaymentType.NONE) {
            binding!!.layoutPayment.tvSelectedPayment.visibility = View.GONE
            binding!!.layoutPayment.ivSelectedPayment.visibility = View.GONE
        } else {
            binding!!.layoutPayment.tvSelectedPayment.visibility = View.VISIBLE
            binding!!.layoutPayment.ivSelectedPayment.visibility = View.VISIBLE
        }
        if (selecetedPayment == PaymentType.CASH) {
            binding!!.layoutPayment.tvSelectedPayment.text =
                CacheUtil.getInstance().getTranslations(Text.CASH, "Cash")
            binding!!.layoutPayment.ivSelectedPayment.setImageResource(R.drawable.ic_payment_cash)
        } else if (selecetedPayment == PaymentType.CARD) {
            binding!!.layoutPayment.tvSelectedPayment.text =
                CacheUtil.getInstance().getTranslations(
                    Text.CARD, "Card"
                )
            binding!!.layoutPayment.ivSelectedPayment.setImageResource(R.drawable.ic_payment_type_card)
        } else if (selecetedPayment == PaymentType.CARD_ON_DELIVERY || selecetedPayment == PaymentType.CARD_ON_PICKUP) {
            binding!!.layoutPayment.tvSelectedPayment.text =
                CacheUtil.getInstance().getTranslations(Text.CASH, "Cash")
            binding!!.layoutPayment.ivSelectedPayment.setImageResource(R.drawable.ic_payment_cash)
        }
    }

    private fun showPaymentMethod() {
        val intent = Intent(this, ChoosePaymentAcitivty::class.java)
        startActivity(intent)
    }

    private fun inputCvvDialog(orderId: String, card: CustomerCard) {
        cvvDialogAlert = CVVDialogAlert.newInstance(object : OnCVVAction {
            override fun onClose() {
                cvvDialogAlert!!.dismiss()
            }

            override fun onProceed(cvv: String) {
                savedCardsDialog!!.dismiss()
                cvvDialogAlert!!.dismiss()
                showLoadingDialog()
                checkTokenGenerated(card.attributes.getToken(), cvv, false, orderId)
            }
        }, card)
        cvvDialogAlert?.setCancelable(false)
        cvvDialogAlert?.show(supportFragmentManager, CVVDialogAlert::class.java.toString())
    }

    override fun onDateSelected(
        materialCalendarView: MaterialCalendarView,
        calendarDay: CalendarDay,
        b: Boolean
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
        token: String,
        cvv: String?,
        isSave: Boolean,
        orderIds: String
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

    private fun checkoutForce(data: JSONObject) {
        /*Intent intent = new Intent(PaymentActivity.this, CustomCheckoutActivity.class);
        intent.putExtra(CustomCheckoutActivity.EXTRA_ORDER_ID , orderId);
        intent.putExtra(CustomCheckoutActivity.EXTRA_PAYMENT_PROVIDER, paymentPresenter.getPaymentProvider());
        intent.putExtra(CustomCheckoutActivity.EXTRA_3DS_DATA, data.toString());
        checkoutActivityLauncher.launch(intent);
        isFromCheckout = true;*/
        showCheckoutPaymentForm(orderId!!)
        Handler().postDelayed({ dialogCheckout!!.handle3ds(data) }, 400)
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
            positiveClickListener = { launchUserPermission() }
        )
    }
}
