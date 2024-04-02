package com.herfy.history.views

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.*
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.widget.Chronometer
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.herfy.BuildConfig
import com.herfy.R
import com.herfy.analytics.FirebaseECommerceEventUtil.Companion.refundevent
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.databinding.FragmentOrderBinding
import com.herfy.ext.cast
import com.herfy.ext.hasDecimalPart
import com.herfy.firebase.FirebaseService
import com.herfy.history.OrdersContract
import com.herfy.history.OrdersPresenter
import com.herfy.history.adapters.OrderMenuItemsRecyclerViewAdapter
import com.herfy.history.adapters.OrderServiceChargeRecyclerViewAdapter
import com.herfy.history.adapters.TimeLineAdapter
import com.herfy.history.views.CancelOrderDialog.Companion.newInstance
import com.herfy.loyalty.LoyaltyActivity
import com.herfy.main.MainActivity
import com.herfy.util.*
import com.herfy.util.LatLngInterpolator.Spherical
import com.herfy.util.extensions.dialPhoneNumber
import com.herfy.utils.extensions.removeSpecialCharacters
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.NavigationUtils
import com.skylinedynamics.solosdk.api.models.objects.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class OrderFragment : BaseFragment(), OrdersContract.View, CancelOrderDialog.OnAction {

    private lateinit var binding: FragmentOrderBinding

    private val args: OrderFragmentArgs by navArgs()

    private lateinit var ordersPresenter: OrdersContract.Presenter

    private var payment = false
    private var order: String? = ""
    private var totalValue = 0.0
    private var cancelOrderDialog: CancelOrderDialog? = null

    private var driverMobile: String? = null
    private var googleMap: GoogleMap? = null
    private var customerMarker: Marker? = null
    private var driverMarker: Marker? = null
    private var timeLineAdapter: TimeLineAdapter? = null
    private var menuItemsRecyclerViewAdapter: OrderMenuItemsRecyclerViewAdapter? = null
    private var orderServiceChargeRecyclerViewAdapter: OrderServiceChargeRecyclerViewAdapter? = null
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var driverNumber = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ordersPresenter = OrdersPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ordersPresenter.start()
        order = args.orderId
        if(order != "") {
            activity?.cast<BaseActivity> {
                it.showLoadingDialog()
            }
            ordersPresenter.getOrder(order, -1, false, false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ordersPresenter.detachAbly()
    }

    override fun setPresenter(presenter: OrdersContract.Presenter) {
        ordersPresenter = presenter
    }

    override fun logEvent(
        eventName: String,
        eventAttributes: HashMap<String, String>,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
        activity?.cast<BaseActivity> {
            it.logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue)
        }
    }

    override fun setCartExpiry() {

    }

    private fun setUpMapLocale() {
        val languageToLoad = if (LocaleUtil.getInstance().language == "ar") "ar" else "en"
        val locale = Locale(languageToLoad)
        Locale.setDefault(locale)
        requireActivity().baseContext.resources.configuration.setLocale(locale)
    }

    override fun setupViews() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.nestedScrollView.visibility = View.GONE
            ordersPresenter.getOrder(order, -1, false, false)
        }
        val orderSupportMapFragment =
            childFragmentManager.findFragmentById(R.id.track_map) as OrderSupportMapFragment?
        orderSupportMapFragment!!.getMapAsync { map ->
            googleMap = map
            googleMap!!.uiSettings.isCompassEnabled = false
            googleMap!!.uiSettings.isZoomControlsEnabled = false
            googleMap!!.uiSettings.isMapToolbarEnabled = false
            setUpMapLocale()
            orderSupportMapFragment.setListener {
                binding.nestedScrollView.requestDisallowInterceptTouchEvent(
                    true
                )
            }
        }
        binding.orderStatusesRecyclerView.layoutManager = LinearLayoutManager(context)
        timeLineAdapter = TimeLineAdapter(context, -1, ordersPresenter, order)
        binding.orderStatusesRecyclerView.adapter = timeLineAdapter
        binding.menuItemsRecyclerView.layoutManager = LinearLayoutManager(context)
        menuItemsRecyclerViewAdapter = OrderMenuItemsRecyclerViewAdapter(context, ordersPresenter)
        binding.menuItemsRecyclerView.adapter = menuItemsRecyclerViewAdapter
        binding.btnOpenLoyalty.setOnClickListener { v: View? ->
            activity?.let {
                val intent = Intent(it, LoyaltyActivity::class.java)
                startActivity(intent)
            }
        }
        binding.cancel.setOnClickListener {
            MixpanelUtil.trackEvent(MixpanelUtil.EVENT_CANCEL_ORDER)
            if (cancelOrderDialog != null) cancelOrderDialog!!.dismiss()
            cancelOrderDialog = newInstance()
            cancelOrderDialog!!.show(childFragmentManager, null)


//                showLoadingDialog();
//                ordersPresenter.cancel();
        }
        activity?.let {
            if (CacheUtil.getInstance().application.attributes.menuOnly) {
                binding.reorder.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(it, R.color.disabled_tint))
            } else {
                binding.reorder.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(it, R.color.primary_tint))
            }
        }
        binding.reorder.setOnClickListener(View.OnClickListener {
            MixpanelUtil.trackEvent(MixpanelUtil.EVENT_REORDER)
            if (CacheUtil.getInstance().application.attributes.menuOnly) {
                FirebaseService.newNotification(
                    CacheUtil.getInstance().getTranslations(
                        Text.MENU_ONLY_MESSAGE,
                        "Sorry, ordering is not available at the moment"
                    ), binding.reorder!!.context
                )
                return@OnClickListener
            }
            activity?.cast<BaseActivity> {
                it.showLoadingDialog()
            }
            ordersPresenter!!.checkReorder()
        })

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if(granted) {
                requestCallPermission(driverNumber)
            }
        }
    }

    override fun setupFonts() {}
    override fun setupTranslations() {
        binding.iveArrived.text = CacheUtil.getInstance()
            .getTranslations(Text.IVE_ARRIVED, "I’ve arrived")
        binding.iveArrivedLabel.text = CacheUtil.getInstance().getTranslations(
            Text.ARRIVED_MESSAGE,
            "We have notified our crew that you have arrived"
        )
        binding.txtBranchNumberTitle.text = CacheUtil.getInstance().getTranslations(
            Text.BRANCH_NUMBER_CAPS,"BRANCH NUMBER")
        binding.maps.text = CacheUtil.getInstance().getTranslations(
            Text.OPEN_IN_GOOGLE_MAPS,
            "OPEN IN GOOGLE MAPS"
        )
        binding.cancel.text =
            CacheUtil.getInstance().getTranslations(Text.CANCEL_ORDER)
        binding.reorder.text = CacheUtil.getInstance()
            .getTranslations(Text.REORDER_THIS_CAPS, "REORDER THIS")
        binding.detailsLabel.text =
            CacheUtil.getInstance().getTranslations(Text.ORDER_DETAILS)
        binding.branchLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.BRANCH_CAPS, "BRANCH")
        binding.typeLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.ORDER_TYPE_CAPS, "ORDER TYPE")
        binding.statusLabel.text =
            CacheUtil.getInstance().getTranslations(Text.ORDER_STATUS)
        binding.trackLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.LIVE_TRACKING_CAPS, "LIVE TRACKING")
        binding.itemsLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.ORDERED_ITEMS_CAPS, "ORDERED ITEMS")
        binding.itemsQuantityLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.ITEMS_QUANTITY_CAPS, "ITEMS & QUANTITY")
        binding.priceLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.PRICE_CAPS, "PRICE")
        binding.subtotalLabel.text = CacheUtil.getInstance().getTranslations(Text.SUBTOTAL)
        binding.discountLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.PROMO_DISCOUNT, "Promo Discount")
        //        deliveryLabel.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERY));
//        taxLabel.setText(CacheUtil.getInstance().getTranslations(Text.TAX));
        binding.totalLabel.text =
            CacheUtil.getInstance().getTranslations(Text.TOTAL_LABEL)
        binding.paymentLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.PAYMENT_DETAILS_CAPS, "PAYMENT DETAILS")
        binding.paymentStatusLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.PAYMENT_STATUS_CAPS, "PAYMENT STATUS")
        binding.paymentMethodLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.PAYMENT_METHOD_CAPS)
        binding.promisedTimeLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.PROMISED_TIME_CAPS, "PROMISED TIME")
        binding.btnOpenLoyalty.text = CacheUtil.getInstance().getTranslations(Text.OPEN)
        binding.tableNumberLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.TABLE_NUMBER_CAPS, "TABLE NUMBER")
        binding.mukafaaLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.AL_RAJHI_MOKAFAA, "Al Rajhi Mokafa'a")
    }

    override fun showLocation(isPlace: Boolean, latLng: LatLng?, address: String?) {
        TODO("Not yet implemented")
    }

    override fun showOrders(newestToOldest: Boolean, orders: ArrayList<OrderHistory>) {}
    override fun setOrder(order: OrderHistory) {}
    override fun updateOrder(position: Int) {}
    override fun selectOrder(order: OrderHistory) {}
    override fun showError(error: String) {
        try {
            activity?.cast<BaseActivity> {
                it.dismissDialogs()
                Toast.makeText(it, error, Toast.LENGTH_SHORT).show()
            }
            findNavController().navigateUp()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun showOrder(order: OrderDetails?, store: Store?, address: Address?) {
        val cancellableStatus = CacheUtil.getInstance().concept.orderCancellationMaxStatus
        order?.let { orderDetails ->
            var displayId = orderDetails.displayId
            var ptsConversion: Double
            var ptsRedeemedDouble: Double
            val ptsRedeemedInt = 0
            val ptsReverseRedeem = 0
            val tableDineInNumber = orderDetails.tableLabel
            //hamburgini handle table number
            if (BuildConfig.SOLO_CONCEPT === "1") {
                binding.tableNumberContainer.visibility = View.GONE
            } else {
                if (tableDineInNumber != null && tableDineInNumber != "null" && tableDineInNumber.isNotEmpty()) {
                    binding.tableNumberContainer.visibility = View.VISIBLE
                    binding.tableNumber.text = tableDineInNumber
                } else {
                    binding.tableNumberContainer.visibility = View.GONE
                }
            }
            driverMobile = orderDetails.driverMobile
            CacheUtil.getInstance().latestDriverNumber = driverMobile
            if (displayId == null || displayId.isEmpty() || displayId.equals(
                    "null",
                    ignoreCase = true
                )
            ) {
                displayId = orderDetails.code
            }
            if (displayId == null || displayId.isEmpty() || displayId.equals(
                    "null",
                    ignoreCase = true
                )
            ) {
                displayId = orderDetails.id
            }
            activity?.cast<MainActivity> {
                it.setToolbarTitle(
                    CacheUtil.getInstance().getTranslations(Text.ORDER_CAPS, "ORDER (x)")
                    .replace("(x)", StringUtil.localize(displayId)))
            }
            store?.attributes?.apply {
                binding.branch.text = name
                binding.branchNumberCardView.isVisible = telephone.isNotEmpty()
                binding.txtBranchNumberValue.text = telephone.removeSpecialCharacters()
            }
            binding.txtBranchNumberValue.setOnClickListener {
                val inputBranchNumber = binding.txtBranchNumberValue.text.toString()
                if (StringUtil.isValidPhoneNumber(inputBranchNumber)) {
                    requestCallPermission(inputBranchNumber)
                }
            }
            binding.maps.setOnClickListener {
                store?.let { storeDetails ->
                    if (orderDetails.deliveryOption.equals(
                            "to-go",
                            ignoreCase = true
                        ) || orderDetails.deliveryOption.equals(
                            "pickup",
                            ignoreCase = true
                        ) || orderDetails.deliveryOption.equals("امسك", ignoreCase = true)
                    ) {
                        val gmmIntentUri =
                            Uri.parse("geo:" + storeDetails.attributes.lat.toString() + "," + storeDetails.attributes.lng.toString() + "?z=" + 16)
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        startActivity(mapIntent)
                    } else {
                        val directionsBuilder = Uri.Builder()
                            .scheme("https")
                            .authority("www.google.com")
                            .appendPath("maps")
                            .appendPath("dir")
                            .appendPath("")
                            .appendQueryParameter("api", "1")
                            .appendQueryParameter("travelmode", "driving")
                            .appendQueryParameter(
                                "origin",
                                orderDetails.customerLat.toString() + "," + orderDetails.customerLat
                            )
                            .appendQueryParameter(
                                "destination",
                                storeDetails.attributes.lat.toString() + "," + storeDetails.attributes.lng
                            )
                        startActivity(Intent(Intent.ACTION_VIEW, directionsBuilder.build()))
                    }
                }
            }
            if (orderDetails.orderStatus.sequence >= OrderStatusType.ARRIVED_AT_CUSTOMER.value) {
                binding.promisedTimeContainer.visibility = View.GONE
            } else {
                //promisedTime.setText(TimeUtil.serverToDisplay(orderDetails.getPromisedTime()));
                val promiseTime: Date
                try {
                    var simpleDateFormat =
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT+0")
                    promiseTime = simpleDateFormat.parse(order.promisedTime)
                    if (promiseTime != null) {
                        activity?.cast<BaseActivity> {
                            binding.promisedTime.setTextColor(ContextCompat.getColor(it, R.color.green))
                            binding.promisedTime.background = ContextCompat.getDrawable(it, R.drawable.bg_countdown_timer_green)
                        }

                        val duration = promiseTime.time - Calendar.getInstance().timeInMillis

                        if (duration > 0) {
                            object : CountDownTimer(duration, 1000) {
                                override fun onTick(millisUntilFinished: Long) {
                                    binding.promisedTime.text = StringUtil.getCountDownTime(millisUntilFinished)

                                    if(millisUntilFinished <= 180000) {
                                        activity?.cast<BaseActivity> {
                                            binding.promisedTime.setTextColor(ContextCompat.getColor(it, R.color.amber))
                                            binding.promisedTime.background = ContextCompat.getDrawable(it, R.drawable.bg_countdown_timer_amber)
                                        }
                                    }
                                }

                                override fun onFinish() {

                                    activity?.cast<BaseActivity> {
                                        binding.promisedTime.setTextColor(ContextCompat.getColor(it, R.color.red))
                                        binding.promisedTime.background = ContextCompat.getDrawable(it, R.drawable.bg_countdown_timer_red)
                                    }

                                    binding.timerChrono.format = if (LocaleUtil.getInstance().isEnglish || LocaleUtil.getInstance().isFrench) "-%s" else  "%s-"
                                    binding.timerChrono.base = SystemClock.elapsedRealtime()
                                    binding.timerChrono.start()

                                    binding.timerChrono.onChronometerTickListener =
                                        Chronometer.OnChronometerTickListener { chronometer ->
                                            binding.promisedTime.text = chronometer?.text.toString()
                                        }

                                }
                            }.start()
                        } else {

                            activity?.cast<BaseActivity> {
                                binding.promisedTime.setTextColor(ContextCompat.getColor(it, R.color.red))
                                binding.promisedTime.background = ContextCompat.getDrawable(it, R.drawable.bg_countdown_timer_red)
                            }

                            binding.timerChrono.base = SystemClock.elapsedRealtime().minus(Calendar.getInstance().timeInMillis.minus(promiseTime.time))
                            binding.timerChrono.start()
                            binding.timerChrono.format = if (LocaleUtil.getInstance().isEnglish || LocaleUtil.getInstance().isFrench) "-%s" else  "%s-"

                            binding.timerChrono.onChronometerTickListener =
                                Chronometer.OnChronometerTickListener { chronometer ->
                                    binding.promisedTime.text = chronometer?.text.toString()
                                }

                        }
                    }
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
                binding.promisedTimeContainer.visibility = View.VISIBLE
            }
            ordersPresenter!!.sequence = orderDetails.orderStatus.sequence
            updateStatus(orderDetails, orderDetails.orderStatus)
            binding.iveArrived.visibility = View.GONE
            binding.arrivedMessageLayout.visibility = View.GONE
            binding.detailsLayout.visibility = View.VISIBLE
            if (orderDetails.deliveryOption.equals(
                    "to-go",
                    ignoreCase = true
                ) || orderDetails.deliveryOption.equals(
                    "pickup",
                    ignoreCase = true
                ) || orderDetails.deliveryOption.equals("امسك", ignoreCase = true)
            ) {
                binding.type.text = CacheUtil.getInstance().getTranslations(Text.PICKUP)
                //            deliveryLabel.setVisibility(View.GONE);
//            delivery.setVisibility(View.GONE);
            } else if (orderDetails.deliveryOption.equals(
                    "dine-in",
                    ignoreCase = true
                ) || orderDetails.deliveryOption.equals(
                    "eat-in",
                    ignoreCase = true
                ) || orderDetails.deliveryOption.equals("أكلت داخل", ignoreCase = true)
            ) {
                binding.type.text = CacheUtil.getInstance().getTranslations(Text.DINE_IN)
                //            deliveryLabel.setVisibility(View.GONE);
//            delivery.setVisibility(View.GONE);
            } else if (orderDetails.deliveryOption.equals(
                    "deliver",
                    ignoreCase = true
                ) || orderDetails.deliveryOption.equals("ايصال", ignoreCase = true)
            ) {
                binding.type.text = CacheUtil.getInstance().getTranslations(Text.DELIVERY)
                binding.branchLabel.text =
                    CacheUtil.getInstance().getTranslations(Text.ADDRESS, "ADDRESS").uppercase(
                        Locale.getDefault()
                    )
                //            deliveryLabel.setVisibility(View.VISIBLE);
                binding.detailsLayout.visibility = View.GONE
                //            delivery.setVisibility(View.VISIBLE);
            } else if (orderDetails.deliveryOption.equals(
                    "curbside",
                    ignoreCase = true
                ) || orderDetails.deliveryOption.equals(
                    "سفري",
                    ignoreCase = true
                ) || orderDetails.deliveryOption.equals("\\u0633\\u0641\\u0631\\u064a", ignoreCase = true)
            ) {
                binding.type.text = CacheUtil.getInstance().getTranslations(Text.CURBSIDE_PICKUP, "Curbside")
                //            deliveryLabel.setVisibility(View.GONE);
//            delivery.setVisibility(View.GONE);
                if (orderDetails.orderStatus.sequence < OrderStatusType.FINISHED.value) {
                    binding.iveArrived.visibility = View.VISIBLE
                    binding.arrivedMessageLayout.visibility = View.GONE
                    binding.iveArrived.setOnClickListener {
                        activity?.cast<BaseActivity> {
                            it.showLoadingDialog()
                        }
                        ordersPresenter!!.customerArrived(orderDetails.id)
                    }
                    if (orderDetails.curbsideStatus != null) {
                        if (orderDetails.curbsideStatus.code.equals("customer-arrived", ignoreCase = true)) {
                            binding.iveArrived.visibility = View.GONE
                            binding.iveArrived.setOnClickListener(null)
                            binding.arrivedMessageLayout.visibility = View.VISIBLE
                        }
                    }
                }
            }
            val hasPromotion = orderDetails.discountDisplay.hasDecimalPart()
            binding.subtotal.text = CurrencyUtils.updateCurrency(orderDetails.subtotal, withSpace = true, ignoreDecimalPlaces = hasPromotion)
            var mukafaaAmount = 0.0
            if (ordersPresenter!!.mukafaaTransaction != null && ordersPresenter!!.isMukafaaEnabled) {
                val transaction = ordersPresenter!!.mukafaaTransaction
                binding.mukafaaLayout.visibility = View.VISIBLE
                mukafaaAmount = transaction.attributes.amount
                var dc = "-" + StringUtil.getFormattedPrice(mukafaaAmount)
                dc = dc.replace("--", "-")
                binding.mukafaaSar.text = dc
            } else {
                binding.mukafaaLayout.visibility = View.GONE
            }
            var loyaltyAmount = 0.0
            try {
                val loyaltyTransaction = ordersPresenter!!.soloLoyaltyTransaction
                if (loyaltyTransaction != null) {
                    binding.redeemedPointsLayout.visibility =
                        if (loyaltyTransaction.attributes.points > 0) View.VISIBLE else View.GONE
                    loyaltyAmount = loyaltyTransaction.attributes.amount
                    binding.redeemedPoints.text = CurrencyUtils.updateCurrency(
                        amount = loyaltyAmount,
                        withSpace = true,
                        ignoreDecimalPlaces = hasPromotion,
                        isDiscount = true
                    )
                    binding.redeemedPointsLabel.text = Html.fromHtml(
                        "<font color=#D4B38B>" + CacheUtil.getInstance().getTranslations(
                            Text.PTS_LABEL
                        ).replace(
                            "(x)",
                            loyaltyTransaction.attributes.points.toString()
                        ) + " </font> " + CacheUtil.getInstance().getTranslations(
                            Text.REDEEMED, "(x) pts redeemed"
                        ).replace("(x)", "").replace("pts", "")
                    )
                } else {
                    binding.redeemedPointsLayout.visibility = View.GONE
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            val discountAmount = orderDetails.discountDisplay - mukafaaAmount // - loyaltyAmount;
            if (discountAmount != 0.0) {
                binding.discountLabel.visibility = View.VISIBLE
                binding.discount.text = CurrencyUtils.updateCurrency(
                    amount = discountAmount,
                    withSpace = true,
                    ignoreDecimalPlaces = hasPromotion,
                    isDiscount = discountAmount > 0
                )
                binding.discount.visibility = View.VISIBLE
            } else {
                binding.discountLabel.visibility = View.GONE
                binding.discount.visibility = View.GONE
            }

//        delivery.setText(order.getDeliveryCharge() > 0 ? StringUtil.getFormattedPrice(order.getDeliveryCharge()) : CacheUtil.getInstance().getTranslations(Text.FREE));


//        if (CacheUtil.getInstance().getConcept().getVatType() != null && CacheUtil.getInstance().getConcept().getVatType().equalsIgnoreCase("inclusive")) {
//            taxLabel.setVisibility(View.GONE);
//            tax.setVisibility(View.GONE);
//        } else {
//            taxLabel.setVisibility(View.VISIBLE);
//            tax.setVisibility(View.VISIBLE);
//            tax.setText(StringUtil.getFormattedPrice(order.getVatAmount()));

            /*double finalVatRate = order.getVatRate();
                if (finalVatRate == 0) {
                    finalVatRate = CacheUtil.getInstance().getConcept().getVatRate();
                }

                taxLabel.setText(StringUtil.getFormattedAmountAbsolute(finalVatRate) + "% " + CacheUtil.getInstance().getTranslations(Text.VAT, "VAT"));

                if (order.getTotal() != (order.getSubtotal() - order.getDiscount()) + order.getDeliveryCharge()) {
                    double total = order.getTotal() - ((order.getSubtotal() - order.getDiscount()) + order.getDeliveryCharge());
                    if (total <= 0) {
                        total = 0;
                    }
                    tax.setText(StringUtil.getFormattedPrice(total));
                    taxLayout.setVisibility(View.VISIBLE);
                } else {
                    double sum = ((order.getSubtotal() - order.getDiscount()) + order.getDeliveryCharge());
                    double vatAmountToDisplay = sum - (sum / ((finalVatRate / 100.0) + 1));
                    if (vatAmountToDisplay <= 0) {
                        vatAmountToDisplay = 0;
                    }
                    taxLayout.setVisibility(View.GONE);
                    tax.setText(StringUtil.getFormattedPrice(vatAmountToDisplay));
                }*/

//        }
            if (orderDetails.charges.isNotEmpty() || orderDetails.taxes.isNotEmpty()) {
                val dataList = ArrayList<ServiceCharge>()
                dataList.addAll(orderDetails.charges)
                dataList.addAll(orderDetails.taxes)
                binding.serviceChargeLayout.visibility = View.VISIBLE
                val linearLayoutManager: LinearLayoutManager = object : LinearLayoutManager(context) {
                    override fun canScrollVertically(): Boolean {
                        return false
                    }
                }
                binding.rvServiceCharge.layoutManager = linearLayoutManager
                orderServiceChargeRecyclerViewAdapter = OrderServiceChargeRecyclerViewAdapter(dataList)
                binding.rvServiceCharge.adapter = orderServiceChargeRecyclerViewAdapter
            }
            if (orderDetails.total < 0) {
                binding.total.text = StringUtil.getFormattedPrice(0.0)
            } else {
                binding.total.text = CurrencyUtils.updateCurrency(orderDetails.total, withSpace = true, ignoreDecimalPlaces = hasPromotion)
                totalValue = orderDetails.total
            }
            val customer = AuthUtil.getInstance().customer
            val isSignedIn = AuthUtil.getInstance().isSignedIn
            val containsLoyalty =
                CacheUtil.getInstance().application.attributes.getDeliverPaymentTypes().contains(
                    PaymentType.LOYALTY
                ) ||
                        CacheUtil.getInstance().application.attributes.getPickupPaymentTypes()
                            .contains(PaymentType.LOYALTY)
            val isFullSignedIn =
                isSignedIn && !customer.attributes.accountType.equals("guest", ignoreCase = true)
            if (!isFullSignedIn || !containsLoyalty || orderDetails.total <= 0 || orderDetails.loyaltyPointsRedeemed != 0 && orderDetails.total == orderDetails.deliveryCharge) {
                binding.vEarnPointsCard.visibility = View.GONE
            } else {
                binding.vEarnPointsCard.visibility = View.VISIBLE
                val pointsText = CacheUtil.getInstance().getTranslations(Text.POINTS, "points")
                val color = CacheUtil.getInstance().colorMain
                val pointsEarnable: Int = if (orderDetails.orderStatus.sequence >= OrderStatusType.FINISHED.value) {
                    orderDetails.loyaltyPointsEarned
                } else {
                    CacheUtil.getInstance().getLoyaltyPointsEarnable(orderDetails.id)
                }
                if (pointsEarnable <= 0) {
                    binding.vEarnPointsCard.visibility = View.GONE
                } else {
                    val text = CacheUtil.getInstance().getTranslations(
                        Text.EARNABLE_POINTS,
                        "You will earn (x) once your order is completed."
                    )
                        .replace("(x)", "<b><font color=$color>$pointsEarnable $pointsText</font></b>")
                    binding.tvPoints.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
                }
            }
            try {
                if (!orderDetails.payments.statusDescription.isEmpty()) {
                    binding.paymentStatus.text = orderDetails.payments.statusDescription
                    if (orderDetails.paymentMethod.equals(PaymentType.CARD.value, ignoreCase = true)) {
                        binding.paymentStatusContainer.visibility = View.VISIBLE
                    } else {
                        binding.paymentStatusContainer.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (orderDetails.paymentMethod.equals(PaymentType.CASH.value, ignoreCase = true)) {
                binding.paymentMethod.text = CacheUtil.getInstance().getTranslations(
                    Text.CASH)
            } else if (orderDetails.paymentMethod.equals(PaymentType.CARD.value, ignoreCase = true)) {
                binding.paymentMethod.text = CacheUtil.getInstance().getTranslations(
                    Text.CARD, "Card")
            } else if (orderDetails.paymentMethod.equals(
                    PaymentType.CARD_ON_DELIVERY.value,
                    ignoreCase = true
                )
            ) {
                binding.paymentMethod.text = CacheUtil.getInstance().getTranslations(
                    Text.CARD_ON_DELIVERY)
            } else if (orderDetails.paymentMethod.equals(PaymentType.APPLE_PAY.value, ignoreCase = true)) {
                binding.paymentMethod.text = CacheUtil.getInstance().getTranslations(
                    Text.APPLE_PAY)
            } else if (orderDetails.paymentMethod.equals(
                    PaymentType.CARD_ON_PICKUP.value,
                    ignoreCase = true
                )
            ) {
                binding.paymentMethod.text = CacheUtil.getInstance().getTranslations(
                    Text.CARD_ON_PICKUP)
            } else {
                binding.paymentLabel.visibility = View.GONE
                binding.paymentContainer.visibility = View.GONE
            }
            val isWaybackBurgers =
                BuildConfig.FLAVOR == "wayback_burgers" || CacheUtil.getInstance().conceptKey == "CGqbTzpyKa9"
            binding.cancel.visibility =
                if (!isWaybackBurgers && orderDetails.isCanCancel && orderDetails.orderStatus.sequence < OrderStatusType.FINISHED.value || orderDetails.orderStatus.sequence < cancellableStatus) View.VISIBLE else View.GONE
            val showReorderable =
                orderDetails.isReorderable && orderDetails.orderStatus.sequence >= OrderStatusType.FINISHED.value
            binding.reorder.visibility = if (showReorderable) View.VISIBLE else View.GONE

            //showRatings(order.getOrderStatus(), ordersPresenter.getRatings());
            if (binding.cancel.visibility == View.VISIBLE) {
                val countDown = CacheUtil.getInstance().concept.orderCancellationTime * (1000 * 60)
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.cancel.visibility = View.GONE
                }, countDown.toLong())
            }

            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            formatter.timeZone = TimeZone.getTimeZone("GMT+0")
            val orderCreatedAt = formatter.parse(order?.createdAt)
            val currentDate = Calendar.getInstance().timeInMillis
            var elapsedMinutes = 0L
            if (orderCreatedAt != null) {
                 elapsedMinutes = (currentDate - orderCreatedAt.time) / 60000
            }

            if (orderDetails.orderStatus.sequence > cancellableStatus ||
                elapsedMinutes > CacheUtil.getInstance().concept.orderCancellationTime) {
                binding.cancel.visibility = View.GONE
            }
        }
    }

    override fun showOrderStatuses(orderStatuses: ArrayList<OrderStatus>, isDone: Boolean) {
        if (orderStatuses.size > 0) {
            binding.statusLabel!!.visibility = View.VISIBLE
            binding.statusContainer!!.visibility = View.VISIBLE
            timeLineAdapter!!.orderId = order

            if(isDone) {
                binding.promisedTimeContainer.visibility = View.GONE
            }
            if (timeLineAdapter != null) timeLineAdapter!!.notifyDataSetChanged()
        } else {
            binding.statusLabel.visibility = View.GONE
            binding.statusContainer.visibility = View.GONE
        }
    }

    override fun showMenuItems(order: OrderDetails, menuItems: ArrayList<MenuItem>) {
        menuItemsRecyclerViewAdapter!!.notifyDataSetChanged()
        binding.swipeRefreshLayout.isRefreshing = false
        binding.nestedScrollView.visibility = View.VISIBLE
        val showReorderable =
            order.isReorderable && order.orderStatus.sequence >= OrderStatusType.FINISHED.value
        binding.reorder.visibility = if (showReorderable) View.VISIBLE else View.GONE
        activity?.cast<BaseActivity> {
            it.dismissDialogs()
        }
    }

    override fun setTimeLine(isCancelled: Boolean, orderStatus: OrderStatus, noDriver: Boolean) {}
    override fun setMenuItem(menuItem: MenuItem) {}
    override fun showCart() {
        activity?.cast<BaseActivity> {
            it.runOnUiThread {
                val vibrator = it.getSystemService(VIBRATOR_SERVICE) as Vibrator
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                500,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        vibrator.vibrate(100)
                    }
                }
                Toast.makeText(
                    it,
                    CacheUtil.getInstance().getTranslations(Text.ITEM_ADDED_TO_CART, "Item added to cart"),
                    Toast.LENGTH_SHORT
                ).show()
                it.dismissDialogs()
                MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_CART)
                NavigationUtils.navigateSafe(findNavController(), OrderFragmentDirections.actionOrderToCart())
            }
        }
    }

    override fun updateStatus(orderDetails: OrderDetails, orderStatus: OrderStatus) {
        activity?.cast<BaseActivity> {
            it.runOnUiThread {
                val cancellableStatus = CacheUtil.getInstance().concept.orderCancellationMaxStatus

                if (!orderDetails.driverName.isNullOrEmpty() &&
                        (orderDetails.deliveryOption.equals("deliver",ignoreCase = true) ||
                            orderDetails.deliveryOption.equals("ايصال", ignoreCase = true))
                ) {
                    binding.driverNameContainer.visibility = View.VISIBLE
                    binding.driverNameLabel.text = CacheUtil.getInstance().getTranslations(
                        Text.DRIVER_NAME_CAPS, "DRIVER NAME")
                    binding.driverName.text = orderDetails.driverName
                    if (!orderDetails.driverMobile.isNullOrEmpty()) {
                        driverNumber = orderDetails.driverMobile
                        binding.driverNumberContainer.visibility = View.VISIBLE
                        binding.driverNumberLabel.text = CacheUtil.getInstance().getTranslations(
                            Text.DRIVER_NUMBER_CAPS, "DRIVER NUMBER")
                        binding.driverNumber.text = orderDetails.driverMobile

                        binding.driverNumberContainer.setOnClickListener {
                            requestCallPermission(orderDetails.driverMobile)
                        }
                    }
                } else {
                    binding.driverNameContainer.visibility = View.GONE
                    binding.driverNumberContainer.visibility = View.GONE
                }

                if (orderStatus.sequence > cancellableStatus) {
                    binding.cancel.visibility = View.GONE
                }

                if (orderStatus.sequence >= OrderStatusType.FINISHED.value) {
                    binding.promisedTimeContainer.visibility = View.GONE
                    binding.cancel.visibility = View.GONE
                    binding.reorder.visibility = if (orderDetails.isReorderable) View.VISIBLE else View.GONE
                } else {
                    binding.promisedTimeContainer.visibility = View.VISIBLE
                }
                if ((orderDetails.deliveryOption.equals(
                        "deliver",
                        ignoreCase = true
                    ) || orderDetails.deliveryOption.equals(
                        "ايصال",
                        ignoreCase = true
                    )) && orderStatus.sequence >= OrderStatusType.DELIVERY_IN_PROGRESS.value && orderStatus.sequence <= OrderStatusType.ARRIVED_AT_CUSTOMER.value
                ) {
                    try {
                        if (orderDetails.driverId != null && !orderDetails.driverId.isEmpty()) {
                            binding.trackLabel.visibility = View.VISIBLE
                            binding.trackContainer.visibility = View.VISIBLE
                            //timeLineAdapter.noDriver = false;
                            showWhatsApp(orderDetails.driverMobile)
                        } else {
                            activity?.cast<MainActivity> { mainActivity ->
                                mainActivity.showToolbarWhatsappButton(false)
                            }
                            binding.trackLabel.visibility = View.GONE
                            binding.trackContainer.visibility = View.GONE
                        }
                        if (timeLineAdapter != null) timeLineAdapter!!.notifyDataSetChanged()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        binding.trackLabel.visibility = View.GONE
                        binding.trackContainer.visibility = View.GONE
                        //timeLineAdapter.noDriver = true;
                        activity?.cast<MainActivity> { mainActivity ->
                            mainActivity.showToolbarWhatsappButton(false)
                        }
                        if (timeLineAdapter != null) timeLineAdapter!!.notifyDataSetChanged()
                    }
                } else {
                    //timeLineAdapter.noDriver = true;
                    activity?.cast<MainActivity> { mainActivity ->
                        mainActivity.showToolbarWhatsappButton(false)
                    }
                    if (timeLineAdapter != null) timeLineAdapter!!.notifyDataSetChanged()
                    binding.trackLabel.visibility = View.GONE
                    binding.trackContainer.visibility = View.GONE
                }
            }
        }
    }

    override fun showDriver(customerLatLng: LatLng, driverLatLng: LatLng) {
        activity?.cast<BaseActivity> {
            it.runOnUiThread {
                if (customerMarker != null) {
                    customerMarker!!.remove()
                }
                if (driverMarker != null) {
                    driverMarker!!.remove()
                }
                if (googleMap != null) {
                    var markerOptions = MarkerOptions()
                    markerOptions.position(customerLatLng)
                    markerOptions.anchor(0.5f, 0.8f)
                    markerOptions.icon(
                        ViewUtil.vectorDrawableToBitmapDescriptor(
                            it,
                            R.drawable.ic_marker_customer
                        )
                    )
                    customerMarker = googleMap!!.addMarker(markerOptions)
                    markerOptions = MarkerOptions()
                    markerOptions.position(driverLatLng)
                    markerOptions.anchor(0.5f, 0.5f)
                    markerOptions.icon(
                        ViewUtil.vectorDrawableToBitmapDescriptor(
                            it,
                            R.drawable.ic_marker_driver
                        )
                    )
                    driverMarker = googleMap!!.addMarker(markerOptions)
                    val builder = LatLngBounds.Builder()
                    builder.include(customerLatLng)
                    builder.include(driverLatLng)
                    googleMap!!.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
                }
            }
        }
    }

    override fun updateDriver(latLng: LatLng, mobileNumber: String) {
        activity?.cast<MainActivity> {
            it.runOnUiThread {
                if (driverMarker != null) {
                    val latLngInterpolator: LatLngInterpolator = Spherical()
                    //MarkerAnimation.animateMarker(driverMarker, latLng, interpolator);
                    val startPosition = driverMarker!!.position
                    val handler = Handler()
                    val start = SystemClock.uptimeMillis()
                    val interpolator: Interpolator = LinearInterpolator()
                    val durationInMs = 3000f
                    handler.post(object : Runnable {
                        var elapsed: Long = 0
                        var t = 0f
                        var v = 0f
                        override fun run() {
                            // Calculate progress using interpolator
                            elapsed = SystemClock.uptimeMillis() - start
                            t = elapsed / durationInMs
                            v = interpolator.getInterpolation(t)
                            driverMarker!!.position =
                                latLngInterpolator.interpolate(v, startPosition, latLng)
                            /*if(firstDriverUpdate) {
                                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngInterpolator.interpolate(v, startPosition, latLng), 16));
                                        firstDriverUpdate = false;
                                    } else {*/
                            //map.animateCamera(CameraUpdateFactory.newLatLng(latLngInterpolator.interpolate(v, startPosition, latLng)));
                            //}
                            // Repeat till progress is complete.
                            if (t < 1) {
                                // Post again 16ms later.
                                handler.postDelayed(this, 16)
                            }
                        }
                    })
                } else {
                    val markerOptions = MarkerOptions()
                    markerOptions.position(latLng)
                    markerOptions.anchor(0.5f, 0.5f)
                    markerOptions.icon(
                        ViewUtil.vectorDrawableToBitmapDescriptor(
                            it,
                            R.drawable.ic_marker_driver
                        )
                    )
                    driverMarker = googleMap!!.addMarker(markerOptions)
                }
                //CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(16).build();
                //map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                /*if(firstDriverUpdate) {
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
                            firstDriverUpdate = false;
                        } else {*/
                //map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                //}
            }
        }
    }

    override fun cancel() {
        binding.cancel!!.visibility = View.GONE
        activity?.cast<BaseActivity> {
            refundevent(it, ordersPresenter!!.orderItems, totalValue)
        }
    }

    override fun showRatings(orderStatus: OrderStatus, ratings: List<Rating>) {
        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean isWaybackBurgers = BuildConfig.FLAVOR.equals("wayback_burgers") || CacheUtil.getInstance().getConceptKey().equals("CGqbTzpyKa9");
                if (orderStatus.getSequence() >= OrderStatusType.ARRIVED_AT_CUSTOMER.getValue() && orderStatus.getSequence() <= OrderStatusType.FINISHED.getValue() && ratings.size() > 0) {
                    cancelMessage.setVisibility(View.GONE);
                    rate.setVisibility(View.VISIBLE);
                } else {
                    if(isWaybackBurgers) {
                        cancelMessage.setVisibility(View.VISIBLE);
                    } else {
                        cancelMessage.setVisibility(View.GONE);
                    }
                    rate.setVisibility(View.GONE);
                }
            }
        });*/
    }

    override fun showMessage(message: String) {
        activity?.cast<BaseActivity> {
            it.dismissDialogs()
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun customerArrivedSuccess() {
        activity?.cast<BaseActivity> {
            it.dismissDialogs()
        }
        binding.iveArrived.setOnClickListener(null)
        binding.arrivedMessageLayout.visibility = View.VISIBLE
        binding.iveArrived.visibility = View.GONE
    }

    @SuppressLint("MissingPermission")
    override fun logFirebaseEvent(event: String) {
        activity?.let {
            if (Values.sDeepLinkUtm != null) {
                val firebaseAnalytics = FirebaseAnalytics.getInstance(it)
                firebaseAnalytics.logEvent(event, Values.sDeepLinkUtm)
            }
        }
    }

    override fun getFromCache(key: String): String? {
        activity?.let {
            return CacheHelper.retrieve(it, key)
        }
        return null
    }

    override fun showWhatsApp(mobileNumber: String) {
        activity?.cast<MainActivity> { mainActivity ->
            if (mobileNumber != null && mobileNumber.trim { it <= ' ' }
                    .isNotEmpty() && !mobileNumber.trim { it <= ' ' }
                    .equals("null", ignoreCase = true)) {
                mainActivity.showToolbarWhatsappButton(true)
                mainActivity.setToolbarWhatsappButton {
                    val whatsAppPackageName = "com.whatsapp"
                    try {
                        val packageManager = mainActivity.packageManager
                        val intent = Intent(Intent.ACTION_VIEW)
                        val url =
                            "https://api.whatsapp.com/send?phone=" + mobileNumber.trim { it <= ' ' }
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
            } else {
                mainActivity.showToolbarWhatsappButton(false)
            }
        }
    }

    override fun onCancel() {
        activity?.cast<BaseActivity> {
            it.showLoadingDialog()
        }
        ordersPresenter!!.cancel()
    }

    override fun onBack() {
        findNavController().navigateUp()
    }

    private fun requestCallPermission(number: String){
        if(ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
            requestPermissionLauncher.launch(android.Manifest.permission.CALL_PHONE)
        } else requireContext().dialPhoneNumber(number = number)
    }
}