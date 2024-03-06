package com.herfy.cart.views

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.maps.model.LatLng
import com.herfy.R
import com.herfy.analytics.FacebookAppEventUtil
import com.herfy.analytics.FirebaseECommerceEventUtil
import com.herfy.analytics.kochava.KochavaTrackerUtil.trackEvent
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.cart.CartContract
import com.herfy.cart.adapters.CartMenuItemsRecyclerViewAdapter
import com.herfy.cart.databinding.FragmentHerfyCartBinding
import com.herfy.checkout_temp.CheckoutActivity
import com.herfy.decorator.BeforeDateDecorator
import com.herfy.decorator.OneDayDecorator
import com.herfy.ext.cast
import com.herfy.history.adapters.OrderServiceChargeRecyclerViewAdapter
import com.herfy.main.MainActivity
import com.herfy.main.NavigationActivity
import com.herfy.order.views.ordertype.OrderTypeNewFragment
import com.herfy.payment.dialog.PaymentOrderTimeDialog
import com.herfy.payment.views.OrderDialogFragment
import com.herfy.upsell.UpsellRecyclerViewAdapter
import com.herfy.util.*
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.OnLayoutLoadingCompleted
import com.herfy.utils.extensions.collapse
import com.herfy.utils.extensions.expand
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.loadImageFromUrl
import com.herfy.utils.extensions.runOnMain
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.BuildUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.NavigationUtils
import com.herfy.views.DynamicTheme
import com.kochava.tracker.events.EventType
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import com.skylinedynamics.solosdk.api.models.objects.*
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class CartFragment : BaseFragment(), CartContract.View, OnDateSelectedListener {

    companion object {
        const val ARGS_KEY_FRAGMENT_RESULT = "fragmentResult"
        const val ARGS_KEY_CHECKOUT = "checkout"
    }

    private lateinit var binding: FragmentHerfyCartBinding

    private val args: CartFragmentArgs by navArgs()

    @Inject
    lateinit var cartPresenter: CartContract.Presenter

    private var digitalCouponsDialogFragment: CartPromoDialogFragment? = null
    private var digitalAvailableCouponsDialogFragment: CartAvailablePromoDialogFragment? = null

    private var scheduleTime: String? = null
    private var oneDayDecorator: OneDayDecorator? = null
    private var beforeDateDecorator: BeforeDateDecorator? = null
    private var scheduleShown = false
    private var maxDate: Date? = null
    private var minDate: Date? = null
    private var selectedDate: Date? = null
    private var daySelected: String? = null
    private var goodDaySelected: String? = null
    private var goodSelectedDate: Date? = null
    private var isRemove = true

    private var adapter: CartMenuItemsRecyclerViewAdapter? = null
    private var signIn = false
    private var orderType = false
    private var dontShowDigitalCoupons = false
    private var validatedTotalPrice = 0.0
    private var isEventSent = false
    private var promoAlertDialog: AlertDialog? = null
    private var paymentOrderTimeDialog: PaymentOrderTimeDialog? = null
    private var orderServiceChargeRecyclerViewAdapter: OrderServiceChargeRecyclerViewAdapter? = null
    private var continueCheckout = false
    private var fromInitialLaunch = false
    private var isFromPromotions = false

    private var upsellRecyclerViewAdapter: UpsellRecyclerViewAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dontShowDigitalCoupons = args.isFromPromotions
        isFromPromotions = args.isFromPromotions
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHerfyCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fromInitialLaunch = true
        activity?.cast<BaseActivity> {
            it.showLoadingDialog()
        }
        checkBackground()
        cartPresenter.start()
        cartPresenter.fetchCustomerCards()
        update()
    }

    private fun checkBackground() {
        binding.contentMain.setBackgroundResource(R.color.page_background_color)
    }

    override fun onResume() {
        super.onResume()
        activity?.cast<MainActivity> {
            it.setCartQuantity(CacheUtil.getInstance().cartQuantity)
        }
        val mukafaaAmount = cartPresenter.mukafaaTransactionAmount
        if (mukafaaAmount > 0 || BuildUtil.isEkfc()) {
            if (BuildUtil.isEkfc()) {
                showLoadingDialog()
            }
            cartPresenter.validateOrder(false, false, null)
        }
        if (cartPresenter.menuItemsCount > 0) {
            binding.placeOrder.isEnabled = true
        }
        if (Values.fromAddAddressBool || Values.fromUpsell) {
            Values.fromAddAddressBool = false
            Values.fromUpsell = false
            update()
        }
    }

    override fun update() {
        super.update()
        if (binding.orderTypeContainer.orderTypeText != null) {
            //binding.main.curbsideLayout.visibility = View.GONE
            when (val oT = CacheUtil.getInstance().orderType) {
                OrderType.PICKUP, OrderType.EAT_IN, OrderType.DRIVE_THRU -> {
                    cartPresenter.getStore()
                    var headerText = CacheUtil.getInstance().getTranslations(Text.PICKUP_AT)
                    if (oT == OrderType.EAT_IN) {
                        headerText = CacheUtil.getInstance()
                            .getTranslations(Text.WALK_IN_PICKUP, "Walk In Pickup")
                    } else if (oT == OrderType.DRIVE_THRU) {
                        headerText = CacheUtil.getInstance()
                            .getTranslations(Text.DRIVE_THRU_PICKUP, "Drive Thru Pickup")
                    }
                    binding.orderTypeContainer.orderTypeText.text = headerText
                    if (CacheUtil.getInstance().orderStore != null) {
                        binding.orderTypeContainer.branchName.text =
                            CacheUtil.getInstance().orderStore.attributes.name
                    } else {
                        binding.orderTypeContainer.orderTypeText.text =
                            CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE)
                        binding.orderTypeContainer.branchName.text =
                            CacheUtil.getInstance().getTranslations(
                                Text.PLEASE_TAP_HERE_TO_PICK_AN_ORDER_TYPE,
                                "Please tap here to pick an order type"
                            )
                    }
                }

                OrderType.DINE_IN -> {
                    cartPresenter.getStore()
                    binding.orderTypeContainer.orderTypeText.text =
                        CacheUtil.getInstance().getTranslations(Text.DINE_IN_AT)
                    if (CacheUtil.getInstance().orderStore != null) {
                        binding.orderTypeContainer.branchName.text =
                            CacheUtil.getInstance().orderStore.attributes.name
                    } else {
                        binding.orderTypeContainer.orderTypeText.text =
                            CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE)
                        binding.orderTypeContainer.branchName.text =
                            CacheUtil.getInstance().getTranslations(
                                Text.PLEASE_TAP_HERE_TO_PICK_AN_ORDER_TYPE,
                                "Please tap here to pick an order type"
                            )
                    }
                }

                OrderType.DELIVERY -> {
                    cartPresenter.getAddress()
                    binding.orderTypeContainer.orderTypeText.text =
                        CacheUtil.getInstance().getTranslations(Text.DELIVER_TO)
                    if (CacheUtil.getInstance().orderAddress != null) {
                        binding.orderTypeContainer.branchName.text =
                            CacheUtil.getInstance().orderAddress.attributes.line1
                    } else if (CacheUtil.getInstance().orderDeliveryAddress != null) {
                        binding.orderTypeContainer.branchName.text =
                            CacheUtil.getInstance().orderDeliveryAddress
                    } else {
                        binding.orderTypeContainer.orderTypeText.text =
                            CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE)
                        binding.orderTypeContainer.branchName.text =
                            CacheUtil.getInstance().getTranslations(
                                Text.PLEASE_TAP_HERE_TO_PICK_AN_ORDER_TYPE,
                                "Please tap here to pick an order type"
                            )
                    }
                }

                OrderType.CURBSIDE -> {
                    cartPresenter.getStore()
                    binding.orderTypeContainer.orderTypeText.text =
                        CacheUtil.getInstance().getTranslations(
                            Text.CURBSIDE_PICKUP_FROM_CAPS, "CURBSIDE PICKUP FROM"
                        ).uppercase(Locale.getDefault())
                    if (CacheUtil.getInstance().orderStore != null) {
                        binding.orderTypeContainer.branchName.text =
                            CacheUtil.getInstance().orderStore.attributes.name
                        setupCurbside()
                    } else {
                        binding.orderTypeContainer.orderTypeText.text =
                            CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE)
                        binding.orderTypeContainer.branchName.text =
                            CacheUtil.getInstance().getTranslations(
                                Text.PLEASE_TAP_HERE_TO_PICK_AN_ORDER_TYPE,
                                "Please tap here to pick an order type"
                            )
                    }
                }

                OrderType.NONE -> {
                    binding.orderTypeContainer.orderTypeText.text =
                        CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE)
                    binding.orderTypeContainer.branchName.text =
                        CacheUtil.getInstance().getTranslations(
                            Text.PLEASE_TAP_HERE_TO_PICK_AN_ORDER_TYPE,
                            "Please tap here to pick an order type"
                        )
                }

                OrderType.EAT_IN -> {}
                OrderType.DRIVE_THRU -> {}
            }
        }
        if (adapter != null) {
            adapter!!.notifyDataSetChanged()
        }
        if (cartPresenter != null) {
            if (!dontShowDigitalCoupons) {
                cartPresenter.getMenuItems()
            } else {
                binding.placeOrder.isEnabled = cartPresenter.menuItemsCount > 0
            }
            cartPresenter.getIntegrations()
            cartPresenter.getConcept()
            cartPresenter.showCoupon()
            cartPresenter.getUpsells()
            if (CacheUtil.getInstance().campaign != null) {
                cartPresenter.validateDigitalCoupon(
                    isFromPromotions, CacheUtil.getInstance().campaign
                )
                isFromPromotions = false
            } else if (dontShowDigitalCoupons) {
                cartPresenter.validateOrder(false, false, null)
            }
        }
    }

    fun cartExpired() {
        if (cartPresenter != null) {
            cartPresenter.getMenuItems()
        }
    }

    private fun setupCurbside() {
        val customerCar = CacheUtil.getInstance().curbsideCar/*if (customerCar != null) {
            binding.main.curbsideLayout.visibility = View.VISIBLE
            if (customerCar.attributes.plateNumber != null && !customerCar.attributes.plateNumber.isEmpty()) {
                binding.main.plateNumber.text = customerCar.attributes.plateNumber
            } else {
                binding.main.plateNumber.text = ""
            }
            if (customerCar.attributes.carMakerId == null || customerCar.attributes.carMakerId.isEmpty()) {
                binding.main.makerColor.visibility = View.GONE
                binding.main.makerOtherText.visibility = View.VISIBLE
                binding.main.makerOtherText.text = customerCar.attributes.carMakerName
            } else {
                binding.main.makerColor.visibility = View.VISIBLE
                binding.main.makerOtherText.visibility = View.GONE
                var imageUrl = customerCar.attributes.carMakerImageUri
                if ((imageUrl == null) || imageUrl.equals(
                        "null", ignoreCase = true
                    ) || imageUrl.isEmpty() || imageUrl.lowercase(
                        Locale.getDefault()
                    ).contains("default-image.png")
                ) {
                    imageUrl = Values.DEFAULT_IMAGE_URL
                }
                try {
                    val activity: Activity? = activity
                    if (activity != null) {
                        Glide.with(activity).load(imageUrl).apply(RequestOptions().override(80, 80))
                            .skipMemoryCache(false).diskCacheStrategy(
                                DiskCacheStrategy.ALL
                            ).apply(RequestOptions.centerInsideTransform())
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
                            }).into(binding.main.makerImage)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            binding.main.colorImageSelected.setImageDrawable(
                ViewUtil.getColorDrawable(
                    "#" + customerCar.attributes.color.replace(
                        "#", ""
                    )
                )
            )
        }*/
    }

    override fun setPresenter(presenter: CartContract.Presenter) {
        cartPresenter = presenter
    }

    override fun logEvent(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
        activity?.cast<BaseActivity> {
            it.logEventAttributesMetric(
                eventName, eventAttributes, eventMetricName, eventMetricValue
            )
        }
    }

    override fun setCartExpiry() {
        activity?.cast<BaseActivity> {
            it.setCartExpiry()
        }
    }

    fun removeScheduleAndDiscount() {
        cartPresenter!!.removeCoupon()
        removeSchedTotally()
    }

    private fun removeSchedTotally() {
        isRemove = true
        //binding.main.schedTime.text = ""
        goodDaySelected = null
        goodSelectedDate = null
        scheduleTime = null
        //binding.main.withSchedLayout.visibility = View.GONE
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun setupViews() {
        try {
            val activity: Activity? = activity
            if (activity != null) {
                var maxDays = CacheUtil.getInstance().concept.maxDaysAllowedInAdvanceOrder
                if (maxDays == 0) {
                    maxDays = 1
                }
                /* binding.paymentSlidingPanel.calendarView.setOnDateChangedListener(this@CartFragment)
                 binding.paymentSlidingPanel.calendarView.showOtherDates =
                     MaterialCalendarView.SHOW_ALL
                 val instance = LocalDate.now()
                 val max = instance.plusDays(maxDays.toLong())
                 beforeDateDecorator = BeforeDateDecorator(
                     ContextCompat.getDrawable(
                         activity, R.drawable.background_unavailable_dates
                     ), maxDays
                 )
                 oneDayDecorator = OneDayDecorator(binding.stubImage.drawable)
                 binding.paymentSlidingPanel.calendarView.setSelectedDate(instance)
                 daySelected = instance.toString()
                 binding.paymentSlidingPanel.calendarView.state().edit().setMinimumDate(instance)
                     .setMaximumDate(max).commit()
                 binding.paymentSlidingPanel.calendarView.setLeftArrow(R.drawable.ic_calendar_arrow_left)
                 binding.paymentSlidingPanel.calendarView.setRightArrow(R.drawable.ic_calendar_arrow_right)
                 binding.paymentSlidingPanel.calendarView.setDateTextAppearance(R.style.NewDateTextAppearance)
                 binding.paymentSlidingPanel.calendarView.setHeaderTextAppearance(R.style.HeaderTextAppearance)
                 binding.paymentSlidingPanel.calendarView.setWeekDayTextAppearance(R.style.WeekTextAppearance)
                 binding.paymentSlidingPanel.calendarView.setWeekDayFormatter(
                     ArrayWeekDayFormatter(
                         resources.getTextArray(R.array.custom_weekdays_common)
                     )
                 )
                 binding.paymentSlidingPanel.calendarView.addDecorators(
                     MySelectorDecorator(activity), beforeDateDecorator, oneDayDecorator
                 )
                 binding.main.orderNotes.setOnTouchListener { v, event ->
                     v.parent.requestDisallowInterceptTouchEvent(true)
                     when (event.action and MotionEvent.ACTION_MASK) {
                         MotionEvent.ACTION_UP -> v.parent.requestDisallowInterceptTouchEvent(false)
                     }
                     false
                 }*/
                setupNewDate()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        binding.toolbarTitle.apply {
            setNavButtonAction {
                activity?.cast<BaseActivity> {
                    it.onBackPressed()
                }
            }
        }
        /* binding.main.removeSched.setOnClickListener { v: View? ->
             removeSchedTotally()
             cartPresenter!!.removeMukafaa()
         }
         binding.paymentSlidingPanel.cancelButton.setOnClickListener { v: View? ->
             binding.slidingPanel.setPanelState(
                 PanelState.HIDDEN
             )
         }
         binding.main.withSchedLayout.setOnClickListener { v: View? -> showSchedule() }
         binding.slidingPanel.addPanelSlideListener(object :
             SlidingUpPanelLayout.PanelSlideListener {
             override fun onPanelSlide(panel: View, slideOffset: Float) {}
             override fun onPanelStateChanged(
                 panel: View, previousState: PanelState, newState: PanelState
             ) {
                 if (newState == PanelState.COLLAPSED || newState == PanelState.HIDDEN) {
                     scheduleShown = false
                 }
             }
         })
         binding.paymentSlidingPanel.confirmButton.setOnClickListener { v: View? ->
             binding.slidingPanel.setPanelState(PanelState.HIDDEN)
             cartPresenter!!.removeMukafaa()
             try {

                 var minSchedule = CacheUtil.getInstance().concept.defaultScheduleDeliveryTime
                 var maxSchedule = CacheUtil.getInstance().concept.maxDaysAllowedInAdvanceOrder

                 if (minSchedule == 0) {
                     minSchedule = 15
                 }

                 if (maxSchedule == 0) {
                     maxSchedule = 1
                     val instance = LocalDate.now()
                     daySelected = instance.toString()
                 }

                 selectedDate = binding.paymentSlidingPanel.datePicker.date

                 val originalCalendar: Calendar = Calendar.getInstance()
                 val calendar: Calendar = Calendar.getInstance()
                 val sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.ENGLISH)
                 calendar.setTime(
                     Objects.requireNonNull(
                         sdf.parse(
                             daySelected + " " + TimeUtil.dateToHourOnly(
                                 selectedDate
                             )
                         )
                     )
                 )

                 val calendarMinimum = Calendar.getInstance()
                 calendarMinimum.time = originalCalendar.time
                 calendarMinimum.add(Calendar.MINUTE, minSchedule)
                 calendarMinimum[Calendar.SECOND] = 59

                 val calendarMaxDays = Calendar.getInstance()
                 calendarMaxDays.time = originalCalendar.time
                 calendarMaxDays.add(Calendar.DAY_OF_MONTH, maxSchedule)
                 calendarMaxDays[Calendar.SECOND] = 59

                 originalCalendar[Calendar.SECOND] = 59
                 calendar[Calendar.SECOND] = 59


                 if (maxSchedule == 1 && calendar <= originalCalendar) {
                     calendar.add(Calendar.DAY_OF_YEAR, 1)
                     if (selectedDate == null) {
                         selectedDate = calendar.time
                     }
                     val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                     val newDaySelected = sdf.parse(daySelected)
                     val cal = Calendar.getInstance()
                     cal.time = newDaySelected
                     cal.add(Calendar.DAY_OF_YEAR, 1)
                     daySelected = sdf.format(cal.time)

                 }

                 cartPresenter!!.scheduleOrder(originalCalendar, calendar, true)
             } catch (pe: ParseException) {
                 pe.printStackTrace()
                 showMessage(CacheUtil.getInstance().getTranslations(Text.SCHEDULED_ORDER_MINIMUM))
             }
         }
         binding.main.changeVehicle.setOnClickListener { v: View? ->
             dismissDialogFragments()
             curbside = true
             setFragmentResultListener(ARGS_KEY_FRAGMENT_RESULT) { key, bundle ->
                 clearFragmentResultListener(requestKey = ARGS_KEY_FRAGMENT_RESULT)
                 setupCurbside()
             }
             NavigationUtils.navigateSafe(
                 findNavController(), CartFragmentDirections.actionCartToCurbside(
                     curbside = true
                 )
             )
         }
         binding.main.containerOrderTypeMain.isEnabled = !args.isBranchPreselected
         */
        binding.orderTypeContainer.orderType.setOnClickListener { v: View? ->
            showOrderType(false)
        }
        try {
            val activity: Activity? = activity
            if (activity != null) {
                binding.orderDetailsContainer.rvCartItems.layoutManager =
                    LinearLayoutManager(activity)
                (Objects.requireNonNull(
                    binding.orderDetailsContainer.rvCartItems.itemAnimator
                ) as SimpleItemAnimator).supportsChangeAnimations = false
                adapter = CartMenuItemsRecyclerViewAdapter(activity, cartPresenter)
                binding.orderDetailsContainer.rvCartItems.adapter = adapter
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        /*
        binding.main.promoCode.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.main.applyCode.performClick()
                return@setOnEditorActionListener true
            }
            false
        }
        binding.main.applyCode.setOnClickListener { view: View? ->
            if (CacheUtil.getInstance().couponCode == null) {
                activity?.cast<BaseActivity> {
                    val inputMethodManager: InputMethodManager? = it.getSystemService(
                        Activity.INPUT_METHOD_SERVICE
                    ) as InputMethodManager?
                    assert(inputMethodManager != null)
                    inputMethodManager!!.hideSoftInputFromWindow(
                        binding.main.promoCode?.windowToken, 0
                    )
                    val promotion: Promotion? = CacheUtil.getInstance().campaignPromotion
                    if (promotion != null) {
                        //if (promotion.getAttributes().getType().equalsIgnoreCase("discount")) {
                        it.showPromptAlertDialog("",
                            CacheUtil.getInstance().getTranslations(
                                Text.ONE_DISCOUNT_ONLY_MESSAGE,
                                "You can only apply one discount at a time. Currently applied discount will be removed"
                            ),
                            CacheUtil.getInstance().getTranslations(
                                Text.YES_CAPS
                            ),
                            { dialogInterface, i ->
                                dialogInterface.dismiss()
                                CacheUtil.getInstance().campaign = null
                                CacheUtil.getInstance().campaignPromotion = null
                                CacheUtil.getInstance().campaignPromotionMenuItem = null
                                CacheUtil.getInstance().campaignToAdd = null
                                showDigitalCoupon(CacheUtil.getInstance().campaign)
                                promoCodeShow()
                            },
                            CacheUtil.getInstance().getTranslations(Text.NO_CAPS),
                            { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() })
                        //                    } else {
//                        promoCodeShow();
//                    }
                    } else {
                        promoCodeShow()
                    }
                }
                cartPresenter!!.removeMukafaaRedemption()
            } else {
                showProgressDialog()
                cartPresenter!!.removeCoupon()
            }
        }*/
        binding.placeOrder.setOnClickListener { view: View? ->
            binding.placeOrder.isEnabled = false
            if ((((CacheUtil.getInstance().orderType == OrderType.PICKUP) || (CacheUtil.getInstance().orderType == OrderType.CURBSIDE) || (CacheUtil.getInstance().orderType == OrderType.EAT_IN) || (CacheUtil.getInstance().orderType == OrderType.DRIVE_THRU))) && CacheUtil.getOrderStoreId() != null && !continueCheckout) {
                showDynamicPickUpPrompt()
            } else {
                activity?.cast<BaseActivity> {
                    it.showLoadingDialog()
                }
                cartPresenter!!.proceed(scheduleTime)
            }
        }
        /*
        binding.main.goToMenu.setOnClickListener { v: View? ->
            dismissDialogFragments()
            NavigationUtils.navigateSafe(
                findNavController(), CartFragmentDirections.actionCartToMenu("")
            )
        }
        if (goodDaySelected != null) {
            loadSched()
        }
        binding.main.campaignLayout.setOnClickListener { v: View? ->
            dontShowDigitalCoupons = false
            showDigitalCoupons(true, ArrayList(), -1)
            cartPresenter!!.showDigitalCoupons(true)
        }
        showBubbleOrNot()
        if (CacheUtil.getInstance().concept.isCartNotesEnabled) {
            binding.main.orderNotes.setText(CacheUtil.getInstance().cartNotes)
            binding.main.orderNotes.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    CacheUtil.getInstance().cartNotes = s.toString()
                }
            })
            if (CacheUtil.getInstance().cartQuantity > 0) {
                binding.main.orderNotesLabel.visibility = View.VISIBLE
                binding.main.orderNotesCard.visibility = View.VISIBLE
            } else {
                binding.main.orderNotesLabel.visibility = View.GONE
                binding.main.orderNotesCard.visibility = View.GONE
            }
        } else {
            binding.main.orderNotesLabel.visibility = View.GONE
            binding.main.orderNotesCard.visibility = View.GONE
        }
        binding.main.mukafaaRemove.setOnClickListener { view: View? ->
            cartPresenter.removeMukafaa()
            binding.main.mukafaaLayout.setVisibility(View.GONE)
        }*/

        binding.upsellContainer.apply {
            upsellRecyclerViewAdapter = UpsellRecyclerViewAdapter(cartPresenter)
            rvUpsellItems.apply {
                adapter = upsellRecyclerViewAdapter
                isNestedScrollingEnabled = true
                setHasFixedSize(true)
            }
        }

        binding.paymentSummaryContainer.viewCoupon.setOnClickListener {
            val couponVal = binding.paymentSummaryContainer.applyCouponLabel.text.toString()
            if(couponVal.equals(CacheUtil.getInstance().getTranslations(Text.APPLY_A_COUPON_LABEL, "Apply a coupon"), ignoreCase = true)){
                showLoadingDialog()
                cartPresenter.showDigitalCoupons(true)
            }else {
                showProgressDialog()
                cartPresenter.removeCoupon()
            }

        }
    }

    private fun promoCodeShow() {
        activity?.cast<BaseActivity> {
            it.showLoadingDialog()
        }/* cartPresenter!!.getCoupon(
             Objects.requireNonNull(binding.main.promoCode.text).toString().lowercase(Locale.getDefault())
         )*/
    }

    override fun setupFonts() {}

    @SuppressLint("SetTextI18n")
    override fun setupTranslations() {
        binding.toolbarTitle.title =
            CacheUtil.getInstance().getTranslations(Text.YOUR_BAG_LABEL, "Your Bag")
        binding.paymentSummaryContainer.totalLabel.text =
            CacheUtil.getInstance().getTranslations(Text.TOTAL_LABEL, "Total")
        binding.paymentSummaryContainer.subtotalLabel.text =
            CacheUtil.getInstance().getTranslations(Text.SUBTOTAL, "Subtotal")
        binding.paymentSummaryContainer.deliveryChargeLabel.text =
            CacheUtil.getInstance().getTranslations(Text.DELIVERY_CHARGE, "Delivery charge");
        binding.placeOrder.text = CacheUtil.getInstance().getTranslations(Text.CHECKOUT, "CHECKOUT")
        binding.totalVatLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.TOTAL_INCLUSIVE_VAT, "(Total is inclusive of VAT)");
        binding.upsellContainer.upsellLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.WOULD_YOU_LIKE_TO_ADD_THESE, "Would you like to add these?")
        binding.paymentSummaryContainer.applyCouponLabel.text =
            CacheUtil.getInstance().getTranslations(Text.APPLY_A_COUPON_LABEL, "Apply a coupon")
        binding.paymentSummaryContainer.viewCoupon.text =
            CacheUtil.getInstance().getTranslations(Text.VIEW_COUPONS_LABEL, "View Coupons")
    }

    override fun showProgressDialog() {
        activity?.cast<BaseActivity> {
            it.showLoadingDialog()
        }
    }

    override fun dismissProgressDialog() {
        runOnMain {
            if (!continueCheckout) {
                activity?.cast<BaseActivity> {
                    it.dismissDialogs()
                }
            }
        }
    }

    override fun showStore(store: Store) {
        if (binding.orderTypeContainer.orderTypeText != null) {
            val selectedOrderType = CacheUtil.getInstance().orderType
            if (selectedOrderType == OrderType.PICKUP || selectedOrderType == OrderType.EAT_IN || selectedOrderType == OrderType.DRIVE_THRU) {
                if (CacheUtil.getInstance().orderStore != null) {
                    var headerText = CacheUtil.getInstance().getTranslations(Text.PICKUP_AT)
                    if (selectedOrderType == OrderType.EAT_IN) {
                        headerText = CacheUtil.getInstance()
                            .getTranslations(Text.WALK_IN_PICKUP, "Walk In Pickup")
                    } else if (selectedOrderType == OrderType.DRIVE_THRU) {
                        headerText = CacheUtil.getInstance()
                            .getTranslations(Text.DRIVE_THRU_PICKUP, "Drive Thru Pickup")
                    }
                    binding.orderTypeContainer.orderTypeText.text = headerText
                    binding.orderTypeContainer.branchName.text =
                        CacheUtil.getInstance().orderStore.attributes.name
                } else {
                    binding.orderTypeContainer.orderTypeText.text =
                        CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE)
                    binding.orderTypeContainer.branchName.text =
                        CacheUtil.getInstance().getTranslations(Text.SELECTED_ADDRESS)
                }
            } else if (selectedOrderType == OrderType.DINE_IN) {
                if (CacheUtil.getInstance().orderStore != null) {
                    binding.orderTypeContainer.orderTypeText.text =
                        CacheUtil.getInstance().getTranslations(Text.DINE_IN_AT)
                    binding.orderTypeContainer.branchName.text =
                        CacheUtil.getInstance().orderStore.attributes.name
                } else {
                    binding.orderTypeContainer.orderTypeText.text =
                        CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE)
                    binding.orderTypeContainer.branchName.text =
                        CacheUtil.getInstance().getTranslations(Text.SELECTED_ADDRESS)
                }
            } else if (selectedOrderType == OrderType.NONE) {
                binding.orderTypeContainer.orderTypeText.text =
                    CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE)
                binding.orderTypeContainer.branchName.text =
                    CacheUtil.getInstance().getTranslations(Text.SELECTED_ADDRESS)
            }
        }
    }

    override fun showAddress(address: String) {
        if (binding.orderTypeContainer.orderTypeText != null) {
            val selectedOrderType = CacheUtil.getInstance().orderType
            if (selectedOrderType == OrderType.DELIVERY) {
                binding.orderTypeContainer.orderTypeText.text =
                    CacheUtil.getInstance().getTranslations(Text.DELIVER_TO)
                binding.orderTypeContainer.branchName.text = address
            } else if (selectedOrderType == OrderType.NONE) {
                binding.orderTypeContainer.orderTypeText.text =
                    CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE)
                binding.orderTypeContainer.branchName.text =
                    CacheUtil.getInstance().getTranslations(Text.SELECTED_ADDRESS)
            }
        }
    }

    fun hideOrShowList(state: Int) {/* binding.main.itemsInYourCartLabel.visibility = state
         binding.main.promoLayout.visibility = state
         binding.main.cartsList.visibility = state*/
    }

    override fun showMenuItems(
        menuItems: List<MenuItem>, fromPromo: Boolean, fromRemoveCoupon: Boolean
    ) {
        dontShowDigitalCoupons = false
        cartPresenter!!.getTotalPrice(false, false)
        binding.placeOrder.isEnabled = menuItems.size > 0
        var itemCount = 0
        for (item in menuItems) {
            itemCount += item.attributes.quantity
        }
        binding.orderDetailsContainer.apply {
            if (itemCount > 0) {
                placeholderOrderDetails.shimmerOrderDetails.visibility = View.GONE
                yourOrderLabel.text = "Your Order (${itemCount})"
                rvCartItems.OnLayoutLoadingCompleted {
                    Handler(Looper.getMainLooper()).postDelayed({
                        clOrderDetails.visibility = View.VISIBLE
                    }, 500)
                }
                adapter!!.notifyDataSetChanged()
            } else {
                clOrderDetails.visibility = View.GONE
                placeholderOrderDetails.shimmerOrderDetails.visibility = View.GONE
            }
        }

        if (signIn) {
            signIn = false
            if (AuthUtil.getInstance().isSignedIn) {
                cartPresenter!!.proceed(scheduleTime)
            }
        }
        if (orderType) {
            orderType = false
            if (CacheUtil.getInstance().orderType !== OrderType.NONE) {
                activity?.cast<BaseActivity> {
                    it.showLoadingDialog()
                }
                cartPresenter!!.proceed(scheduleTime)
            }
        }
        if (!fromRemoveCoupon && !continueCheckout) {
            dismissProgressDialog()
        }
    }

    override fun refreshMenuItems(position: Int, fromChangeQuantity: Boolean) {
        if (position != -1) {
            adapter?.notifyItemChanged(position)
        } else {
            adapter?.notifyDataSetChanged()
        }
        if (!fromChangeQuantity) update()
    }

    override fun updateMenuItems(menuItems: List<MenuItem>) {
        runOnMain {
            adapter?.notifyDataSetChanged()
        }
    }

    override fun setMenuItem(menuItem: MenuItem) {}

    @SuppressLint("SetTextI18n")
    override fun setTotalPrice(
        hasPromotion: Boolean,
        subTotal: Double,
        discountAmount: Double,
        totalPrice: Double,
        isDelivery: Boolean,
        deliveryChargeValue: Double,
        charges: ArrayList<ServiceCharge>,
        alrajhiAmount: Double
    ) {
        activity?.runOnUiThread {
            var discountAmount = discountAmount
            var totalPrice = totalPrice
            validatedTotalPrice = totalPrice
            binding.paymentSummaryContainer.subtotal.text =
                CurrencyUtils.updateCurrency(subTotal, true, hasPromotion)
            /*if (alrajhiAmount > 0) {
                var amnt = StringUtil.getFormattedPrice(alrajhiAmount).toString()
                if ("0" != amnt && !amnt.contains("-")) amnt = "-$amnt"
                binding.main.mukafaaSar.text = amnt
                binding.main.mukafaaLayout.visibility = View.VISIBLE
            } else {
                binding.main.mukafaaLayout.visibility = View.GONE
            }
            discountAmount -= alrajhiAmount*/
            //  binding.main.discountLoader.visibility = View.GONE
            //binding.main.dotLoader.visibility = View.GONE
            if (discountAmount != 0.0) {
                binding.paymentSummaryContainer.discountLayout.visibility = View.VISIBLE
                binding.paymentSummaryContainer.couponDiscount.text = CurrencyUtils.updateCurrency(
                    amount = discountAmount,
                    withSpace = true,
                    ignoreDecimalPlaces = hasPromotion,
                    isDiscount = discountAmount > 0
                )
                binding.paymentSummaryContainer.couponDiscount.visibility = View.VISIBLE
            } else {
                binding.paymentSummaryContainer.discountLayout.visibility = View.GONE
            }

//        if (isDelivery) {
//            if (deliveryChargeValue != 0) {
//                deliveryPrice.setText(StringUtil.getFormattedPrice(deliveryChargeValue));
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
//                    discountLayout.setVisibility(View.GONE);
//                }
//                deliveryPrice.setText(CacheUtil.getInstance().getTranslations(Text.FREE, "Free"));
//            }
//            deliveryLayout.setVisibility(View.VISIBLE);
//        } else {
//            deliveryLayout.setVisibility(View.GONE);
//        }

//        if (isDelivery) {
//            if (deliveryChargeValue != 0) {
//                deliveryPrice.setText(StringUtil.getFormattedPrice(deliveryChargeValue));
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
//                    discountLayout.setVisibility(View.GONE);
//                }
//                deliveryPrice.setText(CacheUtil.getInstance().getTranslations(Text.FREE, "Free"));
//            }
//            deliveryLayout.setVisibility(View.VISIBLE);
//        } else {
//            deliveryLayout.setVisibility(View.GONE);
//        }
            /*if (charges.size > 0) {
                activity?.let {
                    val linearLayoutManager: LinearLayoutManager = object : LinearLayoutManager(it) {
                        override fun canScrollVertically(): Boolean {
                            return false
                        }
                    }
                    binding.main.rvServiceCharge.visibility = View.VISIBLE
                    binding.main.rvServiceCharge.layoutManager = linearLayoutManager
                    orderServiceChargeRecyclerViewAdapter =
                        OrderServiceChargeRecyclerViewAdapter(charges)
                    binding.main.rvServiceCharge.adapter = orderServiceChargeRecyclerViewAdapter
                }
            } else {
                binding.main.rvServiceCharge.adapter = null
                binding.main.rvServiceCharge.visibility = View.GONE
            }*/
            if (totalPrice < 0) {
                totalPrice = 0.0
            }
            if (cartPresenter!!.menuItemsCount == 0) {
//            deliveryLayout.setVisibility(View.GONE);
                binding.paymentSummaryContainer.discountLayout.visibility = View.GONE
                //            vatLayout.setVisibility(View.GONE);
                binding.paymentSummaryContainer.total.text = StringUtil.getFormattedPrice(0.0)
                binding.totalPrice.text = StringUtil.getFormattedPrice(0.0)
            } else {
                binding.paymentSummaryContainer.total.text = CurrencyUtils.updateCurrency(
                    totalPrice, withSpace = true, ignoreDecimalPlaces = hasPromotion
                )
                binding.totalPrice.text = CurrencyUtils.updateCurrency(
                    totalPrice, withSpace = true, ignoreDecimalPlaces = hasPromotion
                )
            }
            binding.paymentSummaryContainer.total.visibility = View.VISIBLE
            if (!isEventSent && totalPrice > 0) {
                isEventSent = true
                firebaseViewCartEvent()
            }
        }
    }

    override fun setOriginalPrice(hasPromotion: Boolean, originalPrice: Double) {
        runOnMain {
            binding.originalPrice.text = CurrencyUtils.updateCurrency(
                originalPrice, withSpace = true, ignoreDecimalPlaces = hasPromotion
            )
            binding.originalPrice.visibility = View.VISIBLE
            binding.dashed.visibility = View.VISIBLE
        }
    }

    @SuppressLint("SetTextI18n")
    override fun setTax(inclusive: Boolean, taxAmount: CharSequence, vatAmount: Double) {
//        ///vatLayout.setVisibility(View.VISIBLE);
//        if (vatAmount <= 0) {
//            taxAmount = StringUtil.getFormattedAmount(0);
//        }
//        vatLayout.setVisibility(inclusive ? View.GONE : View.VISIBLE);
//        vatPrice.setText(taxAmount);
        //vatLabel.setText(CacheUtil.getInstance().getConcept().getVatRate() + "% " + CacheUtil.getInstance().getTranslations(Text.VAT, "VAT"));
        runOnMain {
            binding.totalVatLabel.visibility = if (inclusive) View.VISIBLE else View.GONE
        }

    }

    override fun deleteCartItem(position: Int) {
        runOnMain {
            activity?.cast<BaseActivity> {
                it.showDeleteAlertDialog("",
                    CacheUtil.getInstance().getTranslations(Text.PROMPT_REMOVE_ITEM),
                    CacheUtil.getInstance().getTranslations(
                        Text.YES_CAPS
                    ),
                    { dialogInterface, i ->
                        dialogInterface.dismiss()
                        cartPresenter!!.deleteCartItem(true, position)
                    },
                    CacheUtil.getInstance().getTranslations(Text.NO_CAPS),
                    { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() })
            }
        }

    }

    override fun editCartItem(position: Int) {
        runOnMain {
            dismissDialogFragments()
            NavigationUtils.navigateSafe(
                findNavController(), CartFragmentDirections.actionCartToMenuItem(
                    menuCategoryId = "",
                    menuItemId = "",
                    cartMenuItemPosition = position,
                    isPromo = false
                )
            )
        }

    }

    override fun showCoupon(couponCode: String, description: String) {
        runOnMain {
            activity?.cast<BaseActivity> {
                it.dismissDialogs()
            }
            binding.paymentSummaryContainer.apply {
                applyCouponLabel.text = couponCode
                tvCouponDescription.visible()
                tvCouponDescription.text = description
                viewCoupon.text = CacheUtil.getInstance().getTranslations(Text.REMOVE, "Remove")
            }
        }
    }

    override fun removeCoupon() {
        if(CacheUtil.getInstance().couponCode.isNullOrBlank() && CacheUtil.getInstance().campaign == null){
            runOnMain {
                hideRelatedCouponViews()
                try {
                    if (cartPresenter != null) cartPresenter!!.removeMukafaaRedemption()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                binding.paymentSummaryContainer.apply {
                    applyCouponLabel.text = CacheUtil.getInstance().getTranslations(Text.APPLY_A_COUPON_LABEL, "Apply a coupon")
                    tvCouponDescription.text = ""
                    tvCouponDescription.gone()
                    ivCoupon.setImageResource(R.drawable.ic_apply_coupon)
                    viewCoupon.text = CacheUtil.getInstance().getTranslations(Text.VIEW_COUPONS_LABEL,"View Coupons")
                }
            }
        }
    }

    override fun proceed(signIn: Boolean) {
        this.signIn = signIn
    }

    override fun showOrderType(checkout: Boolean) {
        dismissDialogFragments()
        continueCheckout = false
        setFragmentResultListener(ARGS_KEY_FRAGMENT_RESULT) { key, bundle ->
            clearFragmentResultListener(requestKey = OrderTypeNewFragment.ARGS_KEY_FRAGMENT_RESULT)
            continueCheckout = if (bundle.containsKey(ARGS_KEY_CHECKOUT)) {
                bundle.getBoolean(ARGS_KEY_CHECKOUT, false)
            } else {
                false
            }
            if (continueCheckout && CacheUtil.getInstance().cart.isNotEmpty()) {
                dontShowDigitalCoupons = true
                binding.placeOrder.performClick()
            } else {
                continueCheckout = false
            }
        }
        val storeId = CacheUtil.getOrderStoreId() ?: ""
        NavigationUtils.navigateSafe(
            findNavController(), CartFragmentDirections.actionCartToOrderType(
                fromCart = true, checkout = checkout, storeId = storeId
            )
        )
    }

    override fun showSignIn() {
        dismissDialogFragments()
        dismissProgressDialog()
        continueCheckout = false
        setFragmentResultListener(ARGS_KEY_FRAGMENT_RESULT) { key, bundle ->
            clearFragmentResultListener(requestKey = ARGS_KEY_FRAGMENT_RESULT)
            continueCheckout = if (bundle.containsKey(ARGS_KEY_CHECKOUT)) {
                bundle.getBoolean(ARGS_KEY_CHECKOUT, false)
            } else {
                false
            }
            if (continueCheckout && CacheUtil.getInstance().cart.isNotEmpty()) {
                dontShowDigitalCoupons = true
                if (AuthUtil.getInstance().isSignedIn) {
                    binding.placeOrder.performClick()
                }
            } else {
                continueCheckout = false
            }
        }
        CacheUtil.getInstance().signinUsername = ""
        NavigationUtils.navigateSafe(
            findNavController(), CartFragmentDirections.actionCartToAuth(
                signIn = true, guest = true, curbside = false, checkout = true
            )
        )
    }

    override fun showMessage(message: String) {
        activity?.runOnUiThread {
            binding.placeOrder.isEnabled = cartPresenter!!.menuItemsCount > 0
            activity?.cast<BaseActivity> {
                it.dismissDialogs()
                Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun showError(error: String) {
        activity?.runOnUiThread {
            continueCheckout = false
            binding.placeOrder.isEnabled = cartPresenter!!.menuItemsCount > 0
            // binding.main.totalLoader.gone()
            activity?.cast<BaseActivity> {
                it.dismissDialogs()
                it.showAlertDialog("", error)
            }
        }
    }

    override fun showValidateOrderError(errors: List<String>) {
        continueCheckout = false
        binding.placeOrder.isEnabled = cartPresenter!!.menuItemsCount > 0
        activity?.cast<BaseActivity> {
            it.dismissDialogs()
            val builder = StringBuilder()
            builder.append("")
            for (i in errors.indices) {
                builder.append("- ")
                builder.append(errors[i])
                if ((i + 1) != errors.size) {
                    builder.append("\n\n")
                }
            }
            it.showAlertDialog("", builder.toString())
        }
    }

    override fun showUnavailableMenuItems() {
        activity?.runOnUiThread {
            continueCheckout = false
            binding.placeOrder.isEnabled = cartPresenter!!.menuItemsCount > 0
            activity?.cast<BaseActivity> { baseActivity ->
                baseActivity.dismissDialogs()
                val cartDialogFragment = CartDialogFragment.newInstance(cartPresenter)
                var currentDestination: Int? = null
                activity?.cast<MainActivity> {
                    currentDestination = it.currentDestination
                }
                activity?.cast<NavigationActivity> {
                    currentDestination = it.currentDestination
                }
                if (currentDestination == R.id.nav_cart) {
                    cartDialogFragment.show(
                        baseActivity.supportFragmentManager,
                        CartDialogFragment::class.java.simpleName
                    )
                }
            }
        }
    }

    fun showDynamicPickUpPrompt() {
        activity?.cast<BaseActivity> { baseActivity ->
            baseActivity.dismissDialogs()
            val lat = CacheUtil.getInstance().orderStore.attributes.lat
            val lon = CacheUtil.getInstance().orderStore.attributes.lng
            val branch = CacheUtil.getInstance().orderStore.attributes.name
            val latLng = LatLng(lat, lon)
            val imageUrl = CacheUtil.getInstance().orderStore.attributes.image
            val promisedTime =
                CacheUtil.getInstance().orderStore.attributes.promisedTimeDelta.pickup
            paymentOrderTimeDialog =
                PaymentOrderTimeDialog.newInstance(CacheUtil.getInstance().orderType,
                    promisedTime,
                    branch,
                    imageUrl,
                    latLng,
                    object : PaymentOrderTimeDialog.OnAction {
                        override fun close() {
                            continueCheckout = false
                            binding.placeOrder.isEnabled = true
                        }

                        override fun change() {
                            continueCheckout = false
                            // binding.main.placeOrder.isEnabled = true
                            showOrderType(false)
                        }

                        override fun confirm() {
                            try {
                                baseActivity.showLoadingDialog()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            continueCheckout = false
                            firebaseCheckoutEvent()
                            binding.placeOrder.isEnabled = true
                            cartPresenter!!.proceed(scheduleTime)
                        }
                    })
            paymentOrderTimeDialog?.isCancelable = false
            var currentDestination: Int? = null
            activity?.cast<MainActivity> {
                currentDestination = it.currentDestination
            }
            activity?.cast<NavigationActivity> {
                currentDestination = it.currentDestination
            }
            if (currentDestination == R.id.nav_cart) {
                paymentOrderTimeDialog?.show(
                    baseActivity.supportFragmentManager,
                    PaymentOrderTimeDialog::class.java.toString()
                )
            }
        }
    }

    override fun deleteUnavailableItems(cartQuantity: Int) {
        showMessage(
            CacheUtil.getInstance()
                .getTranslations(Text.REMOVED_ITEM_CART_SUCCESSFULLY, "Item removed from cart")
        )
        adapter!!.notifyDataSetChanged()
        if (cartQuantity > 0) {
            binding.placeOrder.performClick()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun showUpsell(upsells: ArrayList<MenuItem>) {
        runOnMain {
            binding.upsellContainer.apply {
                placeholderUpsell.shimmerOrderDetails.visibility = View.GONE
                if (upsells.size > 0) {
                    rvUpsellItems.OnLayoutLoadingCompleted {
                        Handler(Looper.getMainLooper()).postDelayed({
                            clUpsell.visibility = View.VISIBLE

                        }, 500)
                    }
                    upsellRecyclerViewAdapter?.notifyDataSetChanged()
                } else {
                    clUpsell.visibility = View.GONE
                }

            }
        }
    }

    override fun showPayment() {
        MixpanelUtil.trackEvent(MixpanelUtil.EVENT_PLACE_ORDER)
        trackEvent(EventType.CHECKOUT_START)
        runOnMain {
            continueCheckout = false
            activity?.cast<BaseActivity> {
                it.dismissDialogs()
                val intent = Intent(it, CheckoutActivity::class.java)
                if (!isRemove) {
                    intent.putExtra("isScheduled", true)
                    intent.putExtra("goodSelectedDate", TimeUtil.dateToHourOnly(goodSelectedDate))
                    intent.putExtra("goodDaySelected", goodDaySelected)
                }
                intent.putExtra("totalPrice", validatedTotalPrice)
                it.startActivity(intent)
            }
        }
    }

    private fun hideRelatedCouponViews() {
        binding.originalPrice.visibility = View.GONE
        binding.dashed.visibility = View.GONE
    }

    val totalPriceAfterReset: Unit
        get() {
            if (cartPresenter != null) cartPresenter!!.getMenuItems()
            cartPresenter!!.getTotalPrice(false, false)
        }

    override fun refreshDigitalCoupons() {
        if (cartPresenter != null && cartPresenter!!.menuItemsCount > 0) {
            //binding.main.campaignLayout.visibility = View.VISIBLE
        }
    }

    override fun setSchedule(showAlert: Boolean, scheduleTime: String) {
        try {
            val fragmentManager: FragmentManager = childFragmentManager
            if (!fragmentManager.isDestroyed) {
                val fragment = OrderDialogFragment.newInstance(true)
                fragment.show(
                    fragmentManager, OrderDialogFragment::class.java.simpleName
                )
            }
            loadSched()
            goodDaySelected = daySelected
            goodSelectedDate = selectedDate
            this.scheduleTime = scheduleTime
            loadSched()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setDigitalCoupon(campaign: Campaign, selected: Boolean) {}
    override fun showDigitalCoupons(
        loading: Boolean, digitalCoupons: List<Campaign>, selected: Int
    ) {
        dismissProgressDialog()
        if (Values.NO_PROMOTIONS) {
            return
        }
        try {
            if ((digitalCouponsDialogFragment != null) && (digitalCouponsDialogFragment!!.dialog != null) && digitalCouponsDialogFragment!!.dialog!!.isShowing) {
                 digitalCouponsDialogFragment?.updateCampaigns(
                     loading, false, selected, digitalCoupons
                 )
            } else if (!dontShowDigitalCoupons) {
                digitalCouponsDialogFragment = CartPromoDialogFragment.newInstance(loading,
                    selected,
                    digitalCoupons,
                    object : CartPromoDialogFragment.OnAction {
                        override fun onClose() {
                            if (digitalCouponsDialogFragment != null) {
                                try {
                                    digitalCouponsDialogFragment!!.dismiss()
                                } catch (exception: Exception) {
                                    // do not dismiss when fragment is detaching or is not added
                                    exception.printStackTrace()
                                }
                            }
                        }

                        override fun onAvail(removeCoupon: Boolean, selected: Int) {
                            dontShowDigitalCoupons = true
                            try {
                                digitalCouponsDialogFragment?.dismiss()
                            } catch (exception: Exception) {
                                // do not dismiss when fragment is detaching or is not added
                                exception.printStackTrace()
                            }
                            activity?.cast<BaseActivity> {
                                it.showLoadingDialog()
                            }
                            if (removeCoupon) {
                                cartPresenter!!.removeCoupon()
                            }
                            if (selected < cartPresenter!!.digitalCouponsList.size) {
                                Handler().postDelayed(
                                    { cartPresenter!!.availDigitalCoupon(selected) }, 200
                                )
                            }
                        }

                        override fun onRemove() {
                            dontShowDigitalCoupons = true
                            digitalCouponsDialogFragment!!.dismiss()
                            CacheUtil.getInstance().campaign = null
                            CacheUtil.getInstance().campaignPromotion = null
                            CacheUtil.getInstance().campaignPromotionMenuItem = null
                            CacheUtil.getInstance().campaignToAdd = null
                            showDigitalCoupon(null)
                            cartPresenter!!.removeCoupon()
                        }

                        override fun goToMenu() {
                            digitalCouponsDialogFragment?.dismiss()
                            NavigationUtils.navigateSafe(
                                findNavController(),
                                CartFragmentDirections.actionCartToMenu("")
                            )
                        }

                        override fun onApplied() {
                            digitalCouponsDialogFragment?.dismiss()
                        }
                    }, cartPresenter
                )
                digitalCouponsDialogFragment?.isCancelable = false
                activity?.let {
                    var currentDestination: Int? = null
                    activity?.cast<MainActivity> {
                        currentDestination = it.currentDestination
                    }
                    activity?.cast<NavigationActivity> {
                        currentDestination = it.currentDestination
                    }
                    if (currentDestination == R.id.nav_cart) {
                        digitalCouponsDialogFragment?.show(
                            it.supportFragmentManager,
                            CartPromoDialogFragment::class.java.toString()
                        )
                    }
                }
            } else {
                dontShowDigitalCoupons = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun showAvailableDigitalCoupons(digitalCoupons: List<Campaign>, selected: Int) {
        try {
            if (digitalAvailableCouponsDialogFragment != null) {
                if (digitalAvailableCouponsDialogFragment!!.dialog!!.isShowing) {
                    digitalAvailableCouponsDialogFragment!!.dismiss()
                }
            }
            digitalAvailableCouponsDialogFragment = CartAvailablePromoDialogFragment.newInstance(
                selected,
                digitalCoupons,
                object : CartAvailablePromoDialogFragment.OnAction {
                    override fun onClose() {
                        if (digitalAvailableCouponsDialogFragment != null) {
                            try {
                                digitalAvailableCouponsDialogFragment!!.dismiss()
                            } catch (exception: Exception) {
                                exception.printStackTrace()
                            }
                        }
                    }

                    override fun onAvail(removeCoupon: Boolean, position: Int) {
                        try {
                            digitalAvailableCouponsDialogFragment!!.dismiss()
                        } catch (exception: Exception) {
                            exception.printStackTrace()
                        }
                        activity?.cast<BaseActivity> {
                            it.showLoadingDialog()
                        }
                        if (removeCoupon) {
                            cartPresenter!!.removeCoupon()
                        }
                        cartPresenter!!.availDigitalCouponCampaign(digitalCoupons[position])
                    }

                    override fun onRemove() {
                        digitalAvailableCouponsDialogFragment!!.dismiss()
                        CacheUtil.getInstance().campaign = null
                        CacheUtil.getInstance().campaignPromotion = null
                        CacheUtil.getInstance().campaignPromotionMenuItem = null
                        CacheUtil.getInstance().campaignToAdd = null
                        showDigitalCoupon(null)
                        cartPresenter!!.removeCoupon()
                    }
                })
            digitalAvailableCouponsDialogFragment?.isCancelable = false
            activity?.let {
                var currentDestination: Int? = null
                activity?.cast<MainActivity> {
                    currentDestination = it.currentDestination
                }
                activity?.cast<NavigationActivity> {
                    currentDestination = it.currentDestination
                }
                if (currentDestination == R.id.nav_cart) {
                    digitalAvailableCouponsDialogFragment?.show(
                        it.supportFragmentManager, CartPromoDialogFragment::class.java.toString()
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 999) {
            setupCurbside()
        }
    }

    override fun selectDigitalCoupon(position: Int) {
        /*if (digitalCouponsDialogFragment != null) {
            digitalCouponsDialogFragment!!.selectDigitalCoupon(position)
        }*/
    }

    override fun showDigitalCoupon(campaign: Campaign?) {
        runOnMain {
            if (!continueCheckout) {
                dismissProgressDialog()
            }
            binding.paymentSummaryContainer.apply {
                campaign?.let {
                    applyCouponLabel.text = it.attributes.name
                    tvCouponDescription.visible()
                    tvCouponDescription.text = it.attributes.description
                    var imageUrl = campaign.attributes.imageUri
                    if (imageUrl == null || imageUrl.equals(
                            "null",
                            ignoreCase = true
                        ) || imageUrl.isEmpty() || imageUrl.lowercase(
                            Locale.getDefault()
                        ).contains("default-image.png")
                    ) {
                        imageUrl = Values.DEFAULT_IMAGE_URL
                    }
                    ivCoupon.loadImageFromUrl(imageUrl, isSkipMemoryCache = false)
                    viewCoupon.text = CacheUtil.getInstance().getTranslations(Text.REMOVE, "Remove")
                }?: run {
                    applyCouponLabel.text = CacheUtil.getInstance().getTranslations(Text.APPLY_A_COUPON_LABEL, "Apply a coupon")
                    tvCouponDescription.text = ""
                    tvCouponDescription.gone()
                    ivCoupon.setImageResource(R.drawable.ic_apply_coupon)
                    viewCoupon.text = CacheUtil.getInstance().getTranslations(Text.VIEW_COUPONS_LABEL,"View Coupons")
                }
            }
        }
    }

    override fun addPromotionItem(menuItem: MenuItem) {
        runOnMain {
            if (menuItem != null) {
                binding.orderDetailsContainer.layoutItemCoupons.apply {
                    contentMain.visibility = View.VISIBLE

                    var imageUrl = menuItem.attributes.image
                    if ((imageUrl == null) || imageUrl.equals(
                            "null", ignoreCase = true
                        ) || imageUrl.isEmpty() || imageUrl.lowercase(
                            Locale.getDefault()
                        ).contains("default-image.png")
                    ) {
                        imageUrl = Values.DEFAULT_IMAGE_URL
                    }

                    image.loadImageFromUrl(
                        imageUrl,
                        isSkipMemoryCache = false,
                        strategy = DiskCacheStrategy.ALL,
                        requestOptions = RequestOptions.centerInsideTransform()
                    )
                    name.text = menuItem.attributes.name

                    var first = true
                    val stringBuilder = java.lang.StringBuilder()
                    var modifierItemsCounter = 0
                    var comboIndex = 0
                    var trueIndex = 0
                    for (size in menuItem.sizes.modifierItems) {
                        if (size.attributes.isSelected) {
                            if (!first) {
                                stringBuilder.append("\n")
                            } else {
                                first = false
                            }
                            modifierItemsCounter++
                            trueIndex = comboIndex
                            stringBuilder.append(
                                size.attributes.translatedName.getValue(LocaleUtil.getInstance().language) + " (" + StringUtil.localize(
                                    size.attributes.quantity.toString() + ")"
                                )
                            )
                        }
                        comboIndex++
                    }
                    for (drink in menuItem.drinks.modifierItems) {
                        if (drink.attributes.isSelected) {
                            if (!first) {
                                stringBuilder.append("\n")
                            } else {
                                first = false
                            }
                            modifierItemsCounter++
                            stringBuilder.append(
                                drink.attributes.translatedName.getValue(LocaleUtil.getInstance().language) + " (" + StringUtil.localize(
                                    drink.attributes.quantity.toString() + ")"
                                )
                            )
                        }
                    }

                    for (fries in menuItem.fries.modifierItems) {
                        if (fries.attributes.isSelected) {
                            if (!first) {
                                stringBuilder.append("\n")
                            } else {
                                first = false
                            }
                            modifierItemsCounter++
                            stringBuilder.append(
                                fries.attributes.translatedName.getValue(LocaleUtil.getInstance().language) + " (" + StringUtil.localize(
                                    fries.attributes.quantity.toString() + ")"
                                )
                            )
                        }
                    }

                    for (modifierGroup in menuItem.modifierGroups) {
                        val visibilityConfig = modifierGroup.attributes.visibilityConfig
                        if (visibilityConfig == null || visibilityConfig.initialValue == null || !visibilityConfig.initialValue.equals(
                                "hidden",
                                ignoreCase = true
                            )
                        ) {
                            for (modifierItem in modifierGroup.modifierItems) {
                                if (modifierItem.attributes.isSelected) {
                                    if (!first) {
                                        stringBuilder.append("\n")
                                    } else {
                                        first = false
                                    }
                                    modifierItemsCounter++
                                    stringBuilder.append(
                                        modifierItem.attributes.translatedName.getValue(
                                            LocaleUtil.getInstance().language
                                        ) + " (" + StringUtil.localize(modifierItem.attributes.quantity.toString() + ")")
                                    )
                                }
                            }
                        }
                    }

                    for (modifierItem in menuItem.modifierItems) {
                        if (modifierItem.attributes.isSelected) {
                            if (!first) {
                                stringBuilder.append("\n")
                            } else {
                                first = false
                            }
                            modifierItemsCounter++
                            stringBuilder.append(
                                modifierItem.attributes.translatedName.getValue(
                                    LocaleUtil.getInstance().language
                                ) + " (" + StringUtil.localize(modifierItem.attributes.quantity.toString()) + ")"
                            )
                        }
                    }

                    try {
                        if (menuItem.isCombo) {
                            for (components in menuItem.combo[trueIndex].components) {
                                for (item in components.items) {
                                    if (item.selected) {
                                        if (!menuItem.isSandwichOnly) {
                                            if (!first) {
                                                stringBuilder.append("\n")
                                            } else {
                                                first = false
                                            }
                                            modifierItemsCounter++
                                            stringBuilder.append(
                                                translated(
                                                    LocaleUtil.getInstance().language,
                                                    item.name
                                                ) + " (" + StringUtil.localize(1.toString()) + ")"
                                            )
                                        }
                                        if (item.modifierGroups.isNotEmpty()) {
                                            for (modifierGroup in item.modifierGroups) {
                                                for (i in modifierGroup.modifierItems.indices) {
                                                    if (modifierGroup.modifierItems[i].attributes.isSelected) {
                                                        if (!first) {
                                                            stringBuilder.append("\n")
                                                        } else {
                                                            first = false
                                                        }
                                                        stringBuilder.append(
                                                            modifierGroup.modifierItems[i].attributes.translatedName.getValue(
                                                                LocaleUtil.getInstance().language
                                                            ) + "(" + StringUtil.localize(
                                                                modifierGroup.modifierItems[i].attributes.quantity.toString()
                                                            ) + ")"
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }


                    if (modifierItemsCounter > 1) {
                        lnModifiersDropdown.visibility = View.VISIBLE
                        tvModifiers.visibility = View.VISIBLE
                        spaceModifiers.visibility = View.GONE
                        tvModifiers.text = stringBuilder
                        txtAddOnTotal.text = modifierItemsCounter.toString()
                    } else if (modifierItemsCounter > 0) {
                        lnModifiersDropdown.visibility = View.GONE
                        tvModifiers.visibility = View.VISIBLE
                        spaceModifiers.visibility = View.GONE
                        tvModifiers.text = stringBuilder
                    } else {
                        lnModifiersDropdown.visibility = View.GONE
                        tvModifiers.visibility = View.GONE
                        spaceModifiers.visibility = View.VISIBLE
                    }

                    lnModifiersDropdown.setOnClickListener {
                        if (tvModifiers.visibility == View.VISIBLE) {
                            tvModifiers.collapse()
                            icModifierItemsDropDown.rotation = 180f
                            spaceModifiers.visibility = View.VISIBLE
                        } else {
                            spaceModifiers.visibility = View.GONE
                            tvModifiers.expand()
                            icModifierItemsDropDown.rotation = 0f
                        }
                    }

                    val menuItemTotalPrice = cartPresenter.getMenuItemTotalPrice(menuItem, -1)
                    quantityLayout.visibility =
                        if (menuItem.isPromo || menuItemTotalPrice == 0.0) View.INVISIBLE else View.VISIBLE

                    if (!menuItem.isPromo && menuItem.attributes.quantity > 1) {
                        delete.visibility = View.GONE
                        minus.visibility = View.VISIBLE
                    } else {
                        delete.visibility = View.VISIBLE
                        minus.visibility = View.GONE
                    }

                    quantity.text = StringUtil.localize(menuItem.attributes.quantity.toString())


                    if (menuItemTotalPrice > 0) {
                        totalPrice.text = StringUtil.getNewFormattedPrice(
                            menuItemTotalPrice * menuItem.attributes.quantity,
                            true
                        )
                    } else {
                        totalPrice.text =
                            CacheUtil.getInstance().getTranslations(Text.FREE_CAPS, "FREE")
                    }

                    if (menuItem.isMustBeCustomized || menuItem.isCombo) {
                        edit.apply {
                            visible()
                            setOnClickListener {
                                editPromotionItem()
                            }
                        }
                    } else {
                       edit.gone()
                    }
                    delete.setOnClickListener(View.OnClickListener {
                        activity?.cast<BaseActivity> {
                            it.showDeleteAlertDialog("",
                                CacheUtil.getInstance().getTranslations(Text.PROMPT_REMOVE_ITEM),
                                CacheUtil.getInstance().getTranslations(
                                    Text.YES_CAPS
                                ),
                                { dialogInterface, i ->
                                    dialogInterface.dismiss()
                                    dontShowDigitalCoupons = true
                                    contentMain.gone()
                                    showDigitalCoupon(null)
                                    CacheUtil.getInstance().campaign = null
                                    CacheUtil.getInstance().campaignPromotion = null
                                    CacheUtil.getInstance().campaignPromotionMenuItem = null
                                    CacheUtil.getInstance().campaignToAdd = null
                                    cartPresenter.removeCoupon()
                                },
                                CacheUtil.getInstance().getTranslations(Text.NO_CAPS),
                                { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() })
                        }
                    })
                }
            }
        }
    }

    override fun showPromotionItem() {
        runOnMain {
            val promoItem: MenuItem? = if (CacheUtil.getInstance().couponToAdd != null) {
                CacheUtil.getInstance().couponToAdd.menuItem
            } else if (CacheUtil.getInstance().campaignToAdd != null) {
                CacheUtil.getInstance().campaignToAdd.menuItem
            } else {
                null
            }
            if (promoItem?.isMustBeCustomized == true || promoItem?.isCombo == true) {
                NavigationUtils.navigateSafe(
                    findNavController(), CartFragmentDirections.actionCartToMenuItem(
                        menuCategoryId = "",
                        menuItemId = promoItem.id,
                        cartMenuItemPosition = -1,
                        isPromo = true,
                        isEdit = false
                    )
                )
            }
        }
    }

    private fun editPromotionItem() {
        activity?.runOnUiThread {
            val promoItem: MenuItem? =
                if (CacheUtil.getInstance().couponCode != null && CacheUtil.getInstance().couponMenuItem != null) {
                    CacheUtil.getInstance().couponMenuItem
                } else if (CacheUtil.getInstance().campaign != null && CacheUtil.getInstance().campaignPromotionMenuItem != null) {
                    CacheUtil.getInstance().campaignPromotionMenuItem
                } else {
                    null
                }
            if (promoItem?.isMustBeCustomized == true || promoItem?.isCombo == true) {
                NavigationUtils.navigateSafe(
                    findNavController(), CartFragmentDirections.actionCartToMenuItem(
                        menuCategoryId = "",
                        menuItemId = promoItem.id,
                        cartMenuItemPosition = -1,
                        isPromo = true,
                        isEdit = true
                    )
                )
            }
        }
    }

    override fun showPromoError(error: String) {
        continueCheckout = false
        binding.placeOrder.isEnabled = cartPresenter.menuItemsCount > 0
        activity?.cast<BaseActivity> {
            val builder = AlertDialog.Builder(it)
            builder.setCancelable(true)
            builder.setTitle("")
            builder.setMessage(error)
            if (promoAlertDialog == null || !promoAlertDialog!!.isShowing) {
                promoAlertDialog = builder.create()
                promoAlertDialog!!.setButton(
                    AlertDialog.BUTTON_POSITIVE, CacheUtil.getInstance().getTranslations(
                        Text.OK
                    )
                ) { dialogInterface, i -> dialogInterface.dismiss() }
                promoAlertDialog!!.setOnShowListener(object : DialogInterface.OnShowListener {
                    override fun onShow(dialog: DialogInterface) {
                        promoAlertDialog!!.getButton(AlertDialog.BUTTON_POSITIVE).isAllCaps = false
                        promoAlertDialog!!.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                            ContextCompat.getColor(it, R.color.primary_text)
                        )
                    }
                })
                it.dismissDialogs()
                promoAlertDialog!!.show()
            }
        }
    }

    override fun removeMokafaaView() {
        // binding.main.mukafaaLayout.visibility = View.GONE
    }

    override fun getFromCache(key: String): String? {
        activity?.let {
            return CacheHelper.retrieve(it, key)
        }
        return null
    }

    override fun validatingOrder(subtotal: Double) {
        runOnMain {
            binding.paymentSummaryContainer.subtotal.text = StringUtil.getFormattedPrice(subtotal)
            binding.dashed.visibility = View.GONE
            binding.originalPrice.visibility = View.GONE
            binding.totalPrice.visibility = View.INVISIBLE
        }
    }

    override fun removeItemEvent(menuItem: MenuItem?, totalPrice: Double) {
        activity?.cast<BaseActivity> {
            menuItem?.let { item ->
                FirebaseECommerceEventUtil.removeItemEvent(
                    it, item, CurrencyUtils.updateCurrency(
                        totalPrice,
                        withSpace = true,
                        ignoreDecimalPlaces = CacheUtil.getInstance().hasPromotion()
                    ).toString()
                )
            }
        }
    }

    override fun firebaseViewCartEvent() {
        activity?.cast<BaseActivity> {
            FirebaseECommerceEventUtil.viewCartEvent(
                it, cartPresenter.menuItemsList, validatedTotalPrice
            )
        }
    }

    override fun firebaseCheckoutEvent() {
        activity?.cast<BaseActivity> {
            FirebaseECommerceEventUtil.checkoutEvent(
                it, cartPresenter.menuItemsList, validatedTotalPrice
            )
            FacebookAppEventUtil.initiateCheckoutEvent(
                it, cartPresenter.menuItemsList, validatedTotalPrice
            )
        }
    }

    private fun loadSched() {
        try {
            val activity: Activity? = activity
            if (activity != null) {
                val colorStr = DynamicTheme.getStringColor(activity)
                val start = CacheUtil.getInstance()
                    .getTranslations(Text.ORDER_SCHEDULED_FOR, "Order scheduled for:")
                val at = CacheUtil.getInstance().getTranslations(Text.AT, "at")
                val dayOnly = TimeUtil.dateToMonthAndDayOnly(daySelected)
                val timeOnly = TimeUtil.dateToHourOnly(selectedDate)
                val day = "<font color=$colorStr>$dayOnly</font>"
                val time = "<font color=$colorStr>$timeOnly</font>"/*  binding.main.schedTime.text = Html.fromHtml("$start $day $at $time")
                  binding.main.withSchedLayout.visibility = View.VISIBLE*/
                isRemove = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showBubbleOrNot() {
        activity?.cast<MainActivity> {
            it.showScheduleButton(View.OnClickListener { showSchedule() })
            if (goodDaySelected == null) {
                it.showScheduleBubble()
            }
        }
        activity?.cast<NavigationActivity> {
            //it.showScheduleButton(View.OnClickListener { showSchedule() })
            if (goodDaySelected == null) {
                it.showScheduleBubble()
            }
        }
    }

    fun setupNewDate() {
        if (isRemove) {
            //binding.paymentSlidingPanel.calendarView.setSelectedDate(LocalDate.now())
        }
        setUpDatePicker()
    }

    fun showSchedule() {
        scheduleShown = true
        setupNewDate()
        //binding.slidingPanel.panelState = PanelState.EXPANDED
    }

    private fun setUpDatePicker() {
        val c: Calendar = GregorianCalendar()
        c[Calendar.HOUR_OF_DAY] = 23
        c[Calendar.MINUTE] = 59
        c[Calendar.SECOND] = 59
        maxDate = c.time
        val minTime = Calendar.getInstance()
        minDate = minTime.time
        if (selectedDate == null) {
            selectedDate = minDate
        }/* binding.paymentSlidingPanel.datePicker.addOnDateChangedListener { displayed: String?, date: Date? ->
             selectedDate = binding.paymentSlidingPanel.datePicker.getDate()
         }
         binding.paymentSlidingPanel.datePicker.setCustomLocale(Locale(LocaleUtil.getInstance().language))
         binding.paymentSlidingPanel.datePicker.setIsAmPm(true)
         binding.paymentSlidingPanel.datePicker.setDisplayDays(false)
         binding.paymentSlidingPanel.datePicker.setSelectorColor(android.R.color.white)
         binding.paymentSlidingPanel.datePicker.setDisplayYears(false)
         binding.paymentSlidingPanel.datePicker.setDefaultDate(minDate)
         binding.paymentSlidingPanel.datePicker.setTextSize(resources.getDimensionPixelSize(R.dimen.font_size_16sp))
         binding.paymentSlidingPanel.datePicker.setDisplayMonths(false)
         binding.paymentSlidingPanel.datePicker.setTextColor(this.resources.getColor(R.color.silver_chalice))
         binding.paymentSlidingPanel.datePicker.setSelectedTextColor(Color.parseColor(
             CacheUtil.getInstance().colorMain))
         binding.paymentSlidingPanel.datePicker.setTypeface(FontHandler.getMediumTypeface())*/

        var maxDays = CacheUtil.getInstance().concept.maxDaysAllowedInAdvanceOrder
        if (maxDays == 0) {
            maxDays = 1
        }
        if (maxDays == 1) {/*binding.paymentSlidingPanel.calendarCard.visibility = View.GONE
            binding.paymentSlidingPanel.pickADayLabel.visibility = View.GONE*/
        }
    }

    override fun onDateSelected(
        materialCalendarView: MaterialCalendarView, calendarDay: CalendarDay, b: Boolean
    ) {
        oneDayDecorator!!.setDate(calendarDay.date)
        daySelected = calendarDay.date.toString()
        //binding.paymentSlidingPanel.calendarView.invalidateDecorators()
    }

    fun translated(language: String, name: Translated): String {
        return if (language.equals("en-us", ignoreCase = true)) {
            name.english
        } else {
            if (language.equals("fr", ignoreCase = true)) name.french else name.arabic
        }
    }

    fun dismissDialogFragments() {
        digitalCouponsDialogFragment?.dialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
        digitalAvailableCouponsDialogFragment?.dialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
        paymentOrderTimeDialog?.dialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
    }


    fun showOrder(orderId: String) {
        NavigationUtils.navigateSafe(
            findNavController(), CartFragmentDirections.actionCartToOrder(orderId)
        )
    }

    override fun goToItemDetails(position: Int, upsell: MenuItem) {
        NavigationUtils.navigateSafe(
            findNavController(), CartFragmentDirections.actionCartToMenuItem(
                upsell.attributes.categoryId.toString(), upsell.id, -1
            )
        )
    }

    override fun showCartMenuItemsLoading() {
        binding.orderDetailsContainer.apply {
            clOrderDetails.visibility = View.GONE
            placeholderOrderDetails.apply {
                shimmerOrderDetails.visibility = View.VISIBLE
                shimmerOrderDetails.startShimmer()
            }
        }
    }

}