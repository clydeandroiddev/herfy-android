package com.herfy.order.views.ordertype

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.tabs.TabLayout
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.cart.views.CartDialogFragment
import com.herfy.cart.views.CartFragment
import com.herfy.curbside.curbsidenew.CurbsideNewActivity
import com.herfy.ext.cast
import com.herfy.font.FontHandler
import com.herfy.location.FusedLocationProvider
import com.herfy.main.MainActivity
import com.herfy.main.NavigationActivity
import com.herfy.order.OrderContract
import com.herfy.order.OrderPresenter
import com.herfy.order.adapters.StoresRecyclerViewAdapter
import com.herfy.order.views.BookmarkAddressDialogFragment
import com.herfy.order.views.DineInTableNumberDialogFragment
import com.herfy.order.views.OrderTypeDialogFragment
import com.herfy.order.views.ordertype.addaddress.AddNewAddressActivity
import com.herfy.ordertype.databinding.FragmentOrderTypeBinding
import com.herfy.util.*
import com.herfy.util.StringUtil
import com.herfy.utils.constants.*
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.*
import com.herfy.utils.helper.ViewUtil
import com.herfy.utils.menu.MenuUtil
import com.herfy.views.RecyclerViewUtil
import com.skylinedynamics.solosdk.api.models.objects.*
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import java.util.*


class OrderTypeNewFragment : BaseFragment(), OrderContract.View {

    companion object {
        const val ARGS_KEY_FRAGMENT_RESULT = "fragmentResult"
        const val ARGS_KEY_CURBSIDE = "curbside"
        const val ARGS_KEY_IS_SAME_BRANCH = "is_same_branch"
    }

    private lateinit var binding: FragmentOrderTypeBinding

    private lateinit var choosePickupTypeDialog: ChoosePickupTypeDialog

    private val args: OrderTypeNewFragmentArgs by navArgs()

    private lateinit var orderPresenter: OrderContract.Presenter

    private var currentSelectedStore: Store? = null
    private var isSingleOrderType = false
    private var isDeliveryAvailable = false
    private var isCurbsideAvailable = false
    private var isPickupAvailable = false
    private var isDineInAvailable = false
    private var googleMap: GoogleMap? = null
    private var storesLayoutManager: LinearLayoutManager? = null
    private lateinit var deliveryAddressAdapter: DeliveryAddressAdapter
    private var dineInStoresRecyclerViewAdapter: StoresRecyclerViewAdapter? = null
    private var pickupCurbsideStoresRecyclerViewAdapter: StoresRecyclerViewAdapter? = null
    private var orderTypeSelected: OrderType? = null
    private var locationMarker: Marker? = null
    private val storeMarkers = ArrayList<Marker?>()
    private var bookmarkAddressDialogFragment: BookmarkAddressDialogFragment? = null
    private var dineInTableNumberDialogFragment: DineInTableNumberDialogFragment? = null

    private val gpsLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
            initializeLocation()
        }

    private val locationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getLocation()
        } else {
            getStoresFallback()
        }
    }

    private var addressLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val cachedOrderType = CacheUtil.getInstance().orderType;
                if (CacheUtil.getInstance().orderAddress != null || CacheUtil.getInstance().orderDeliveryAddress != null) {
                    CacheUtil.getInstance().orderType = OrderType.DELIVERY
                }
                if (CacheUtil.getInstance().isSeparatedMenus) {
                    val cartItems = CacheUtil.getInstance().cart
                    if (cartItems.size > 0) {
                        showPromptChangeOrderTypeDialog(CacheUtil.getInstance().getTranslations(
                            Text.CHANGE_DELIVERY_INSTEAD_LABEL,
                            "Your current Pickup order will be removed from cart. Would you like to proceed and have your order delivered instead?"
                        ), { _, _ ->
                            val previousDifferentMenuId = CacheUtil.getInstance().differentMenuId
                            MenuUtil.getDiffMenuCdnUrl()
                            if (previousDifferentMenuId != CacheUtil.getInstance().differentMenuId) {
                                CacheUtil.getInstance().cart = ArrayList()
                                showLoadingDialog()
                                fromAddAddressDifferentMenu = true
                                orderPresenter.getDiffMenuCdn(null)
                            } else {
                                fromAddAddressDifferentMenu = false
                                navigateUp()
                            }
                        }, { dialog, which ->
                            activity?.cast<BaseActivity> { it.dismissDialogs() }
                            dialog.dismiss()
                            CacheUtil.getInstance().orderType = cachedOrderType
                        })
                    } else {
                        val previousDifferentMenuId = CacheUtil.getInstance().differentMenuId
                        MenuUtil.getDiffMenuCdnUrl()
                        if (previousDifferentMenuId != CacheUtil.getInstance().differentMenuId) {
                            CacheUtil.getInstance().cart = ArrayList()
                            showLoadingDialog()
                            fromAddAddressDifferentMenu = true
                            orderPresenter.getDiffMenuCdn(null)
                        } else {
                            fromAddAddressDifferentMenu = false
                            navigateUp()
                        }
                    }
                } else {
                    fromAddAddressDifferentMenu = false
                    navigateUp()
                }
            }
        }


    private var deliveryTabSelected = false
    private var curbsideTabSelected = false
    private var dineInTabSelected = false
    private var isPreselectedBranch = false
    private var isMenuSeparate = false
    private var typeOrder: Int = 0
    private var fromCurbside = false
    private var curbsideStoreId = ""
    private var fromAddAddressDifferentMenu = false
    private var isMenuRedownloaded = false;

    private var curbsideLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == Activity.RESULT_OK) {
            activity?.cast<BaseActivity> {
                continueSelectingType(it, OrderType.CURBSIDE)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderPresenter = OrderPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentOrderTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.cast<BaseActivity> {
            it.showLoadingDialog()
        }

        isMenuSeparate = CacheUtil.getInstance().isSeparatedMenus

        activity?.apply {
            if (intent?.extras?.containsKey("selectedOrderType") == true) {
                typeOrder = intent?.extras?.getInt("selectedOrderType", 0) ?: 0
                intent?.removeExtra("selectedOrderType")
            }
        }

        if (activity is NavigationActivity && isMenuSeparate && CacheUtil.getInstance().orderType == OrderType.NONE) {
        }
        orderPresenter.start()
        isCurbsideAvailable = CacheUtil.getInstance().application.attributes.getOrderTypes()
            .contains(OrderType.CURBSIDE)
        orderPresenter.getOrderTypes()
        orderPresenter.getAddresses(false)
        if (typeOrder != 0) {
            selectOrderType(OrderType.fromInt(typeOrder))
        } else {
            selectOrderType(CacheUtil.getInstance().orderType)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()
        binding.root.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (event?.action === KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        return isMenuSeparate && CacheUtil.getInstance().orderType == OrderType.NONE
                    }
                }
                return false
            }
        })
    }

    private fun navigateUp() {
        activity?.runOnUiThread {
            activity?.cast<MainActivity> {
                it.forceLocation = false
                it.forceLocationMenuRefresh = true
                sendFragmentResult()
            }
            activity?.cast<NavigationActivity> {
                if (args.fromCart && !isMenuRedownloaded) {
                    sendFragmentResult()
                } else {
                    it.finish()
                }

            }
        }
    }

    private fun sendFragmentResult() {
        val isSameBranch = args.storeId == (CacheUtil.getOrderStoreId() ?: "")
        setFragmentResult(
            ARGS_KEY_FRAGMENT_RESULT, bundleOf(
                CartFragment.ARGS_KEY_CHECKOUT to args.checkout,
                ARGS_KEY_IS_SAME_BRANCH to isSameBranch
            )
        )
        findNavController().navigateUp()
    }

    override fun update() {
        try {
            isCurbsideAvailable = CacheUtil.getInstance().application.attributes.getOrderTypes()
                .contains(OrderType.CURBSIDE)
            orderPresenter.getOrderTypes()
            if (AuthUtil.getInstance().isSignedIn) {

            } else if (CacheUtil.getInstance().orderDeliveryAddress != null) {

            }
            selectOrderType(CacheUtil.getInstance().orderType)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setPresenter(presenter: OrderContract.Presenter) {
        orderPresenter = presenter
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
        activity?.cast<MainActivity> {
            it.setCartExpiry()
        }
        activity?.cast<NavigationActivity> {
            it.setCartExpiry()
        }
    }

    private fun setUpMapLocale() {
        val languageToLoad = if (LocaleUtil.getInstance().language == "ar") "ar" else "en"
        val locale = Locale(languageToLoad)
        Locale.setDefault(locale)
        resources.configuration.setLocale(locale)
    }

    override fun setupViews() {
        binding.back.setOnClickListener {
            navigateUp()
        }

        binding.deliveryLayout.userCurrentLocation.setOnClickListener {
            goToAddAddress(true)
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
                    googleMap?.setPadding(0, 0, 0, ViewUtil.dpToPx(it, 255))
                }
            }
            googleMap?.setOnMarkerClickListener { marker ->
                try {
                    if (marker.title != null && marker.title?.isEmpty() == false) {
                        val position = marker.title?.toInt() ?: 0
                        if (dineInStoresRecyclerViewAdapter?.selected == position) {
                            showStore(true, position)
                        } else if (pickupCurbsideStoresRecyclerViewAdapter?.selected == position) {
                            showStore(true, position)
                        } else {
                            orderPresenter.getStore(!curbsideTabSelected, position)?.let { store ->
                                selectStore(
                                    position, store, true
                                )
                            }
                        }
                    }
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }
                true
            }
        }

        binding.pickupLayout.location.setOnClickListener { initializeLocation() }

        activity?.cast<BaseActivity> {
            deliveryAddressAdapter = DeliveryAddressAdapter(
                ArrayList(), object : DeliveryAddressAdapter.AddressListener {
                    override fun onSelectedAddress(address: Address, position: Int) {
                        deliveryAddressAdapter.updateSelectedId(address.id)
                        CacheUtil.getInstance().orderType = OrderType.DELIVERY
                        orderPresenter.selectAddress(position, address)
                    }

                    override fun onEditAddress(address: Address, position: Int) {

                    }
                }, ""
            )
            binding.deliveryLayout.addressList.adapter = deliveryAddressAdapter
            storesLayoutManager = LinearLayoutManager(activity)
            binding.storesRecyclerView.layoutManager = storesLayoutManager
            dineInStoresRecyclerViewAdapter = StoresRecyclerViewAdapter(orderPresenter, true)
            pickupCurbsideStoresRecyclerViewAdapter =
                StoresRecyclerViewAdapter(orderPresenter, false)
            binding.continueButton.setOnClickListener { v: View? ->
                if (dineInTabSelected) {
                    continueSelectingType(it, OrderType.DINE_IN)
                } else if (deliveryTabSelected) {
                    goToAddAddress(false)
                } else {
                    chooseType(it)
                }
            }
        }

        binding.typeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.deliveryButton -> {
                    selectOrderType(OrderType.DELIVERY)
                }

                R.id.dineInButton -> {
                    selectOrderType(OrderType.DINE_IN)
                }

                R.id.pickupButton -> {
                    selectOrderType(OrderType.PICKUP)
                }
            }
        }
    }

    private fun goToAddAddress(location: Boolean) {
        val intent = Intent(activity, AddNewAddressActivity::class.java)
        intent.putExtra("goToLocation", location)
        addressLauncher.launch(intent)
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.continueButton.text =
            CacheUtil.getInstance().getTranslations(Text.CONTINUE_LABEL, "Continue")
        binding.deliveryButton.text =
            CacheUtil.getInstance().getTranslations(Text.DELIVERY, "Delivery")
        binding.pickupButton.text = CacheUtil.getInstance().getTranslations(Text.PICKUP, "Pickup")
        binding.dineInButton.text = CacheUtil.getInstance().getTranslations(Text.DINE_IN, "Dine In")
        binding.deliveryLayout.useCurrentLocationLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.USE_CURRENT_LOCATION, "Use current location")
        binding.deliveryLayout.savedAddressesLabel.text =
            CacheUtil.getInstance().getTranslations(Text.SAVED_ADDRESSES, "Saved Addresses")
        binding.deliveryLayout.emptyStateLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.NO_SAVED_ADDRESSES_FOUND, "No saved addresses found.")
    }

    private fun adjustOrderType(withVisibility: Boolean, orderTypes: MutableSet<OrderType>) {
        for (orderType: OrderType in orderTypes) {
            if (orderType == OrderType.DELIVERY) {
                isDeliveryAvailable = true
                if (withVisibility) binding.deliveryButton.visible()
            } else if (orderType == OrderType.DINE_IN) {
                isDineInAvailable = true
                if (withVisibility) binding.dineInButton.visible()
            } else if (orderType == OrderType.PICKUP) {
                isPickupAvailable = true
                if (withVisibility) binding.pickupButton.visible()
            } else if (orderType == OrderType.CURBSIDE) {
                isCurbsideAvailable = true
                if (withVisibility) binding.pickupButton.visible()
            }
        }
    }

    override fun showOrderTypes(orderTypes: MutableSet<OrderType>) {
        activity?.cast<BaseActivity> {
            it.dismissDialogs()
        }
        orderTypes.let {
            val hasOrderTypes = orderTypes.size > 1
            isSingleOrderType = !hasOrderTypes
            adjustOrderType(hasOrderTypes, orderTypes)
            var title: String? = ""
            initialTypeState()
            for (orderType: OrderType in orderTypes) {
                if (orderType === OrderType.DELIVERY) {
                    title = CacheUtil.getInstance().getTranslations(Text.DELIVERY, "Delivery")
                } else if (orderType === OrderType.PICKUP) {
                    title = CacheUtil.getInstance().getTranslations(Text.PICKUP, "Pickup")
                } else if (orderType === OrderType.DINE_IN) {
                    title = CacheUtil.getInstance().getTranslations(Text.DINE_IN, "Dine In")
                } else if (orderType === OrderType.CURBSIDE) {
                    title = CacheUtil.getInstance()
                        .getTranslations(Text.CURBSIDE_PICKUP_CAPS, "Curbside").uppercase(
                            Locale.getDefault()
                        )
                }
            }
            if (isSingleOrderType) {
                binding.typeGroup.gone()
                binding.soloType.visible()
                binding.soloType.text = title
            } else {
                binding.typeGroup.visible()
                binding.soloType.gone()
            }
        }
    }

    private fun initialTypeState() {
        if (typeOrder != 0 || CacheUtil.getInstance().orderType != OrderType.NONE) {
            var orderType = CacheUtil.getInstance().orderType
            if (typeOrder != 0) {
                orderType = OrderType.fromInt(typeOrder)
            }
            when (orderType) {
                OrderType.DELIVERY -> {
                    binding.typeGroup.check(R.id.deliveryButton)
                }

                OrderType.DINE_IN -> {
                    binding.typeGroup.check(R.id.dineInButton)
                }

                else -> {
                    binding.typeGroup.check(R.id.pickupButton)
                }
            }
        } else {
            if (isDeliveryAvailable) {
                binding.typeGroup.check(R.id.deliveryButton)
            } else if (isPickupAvailable) {
                binding.typeGroup.check(R.id.pickupButton)
            } else if (isDineInAvailable) {
                binding.typeGroup.check(R.id.dineInButton)
            } else if (isCurbsideAvailable) {
                binding.typeGroup.check(R.id.pickupButton)
            }
        }
    }

    override fun createOrderType(orderType: OrderType) {}

    override fun selectOrderType(type: OrderType) {
        binding.deliveryButton.typeface = FontHandler.getRegularTypeface()
        binding.dineInButton.typeface = FontHandler.getRegularTypeface()
        binding.pickupButton.typeface = FontHandler.getRegularTypeface()
        val isNone = false
        var orderType = type
        if (type === OrderType.NONE) {
            if (isDeliveryAvailable) {
                orderType = OrderType.DELIVERY
            } else if (isCurbsideAvailable) {
                orderType = OrderType.CURBSIDE
            } else if (isPickupAvailable) {
                orderType = OrderType.PICKUP
            } else if (isDineInAvailable) {
                orderType = OrderType.DINE_IN
            }
        } else {
            if (type === OrderType.DELIVERY) {
                if (!isDeliveryAvailable) {
                    if (isCurbsideAvailable) {
                        orderType = OrderType.CURBSIDE
                    } else if (isPickupAvailable) {
                        orderType = OrderType.PICKUP
                    } else {
                        orderType = OrderType.DINE_IN
                    }
                }
            } else if (type === OrderType.PICKUP) {
                if (!isPickupAvailable) {
                    if (isDeliveryAvailable) {
                        orderType = OrderType.DELIVERY
                    } else if (isCurbsideAvailable) {
                        orderType = OrderType.CURBSIDE
                    } else {
                        orderType = OrderType.DINE_IN
                    }
                }
            } else if (type === OrderType.DINE_IN) {
                if (!isDineInAvailable) {
                    if (isDeliveryAvailable) {
                        orderType = OrderType.DELIVERY
                    } else if (isCurbsideAvailable) {
                        orderType = OrderType.CURBSIDE
                    } else {
                        orderType = OrderType.PICKUP
                    }
                }
            }
        }

        var title: String? = ""
        if (orderType === OrderType.DELIVERY) {
            selectDelivery()
            binding.deliveryButton.typeface = FontHandler.getSemiboldTypeface()
            title = CacheUtil.getInstance().getTranslations(Text.DELIVERY)
        } else if (orderType === OrderType.PICKUP) {
            selectPickup()
            binding.pickupButton.typeface = FontHandler.getSemiboldTypeface()
            title = CacheUtil.getInstance().getTranslations(Text.PICKUP)
        } else if (orderType === OrderType.DINE_IN) {
            binding.dineInButton.typeface = FontHandler.getSemiboldTypeface()
            selectDineIn()
            title = CacheUtil.getInstance().getTranslations(Text.DINE_IN)
        } else if (orderType === OrderType.CURBSIDE) {
            binding.pickupButton.typeface = FontHandler.getSemiboldTypeface()
            selectPickup()
            title = CacheUtil.getInstance().getTranslations(Text.CURBSIDE_PICKUP_CAPS, "CURBSIDE")
                .uppercase(
                    Locale.getDefault()
                )
        }

        if (!isSingleOrderType && isNone) {
            showOrderTypeDialog()
        }

        orderTypeSelected = orderType
        try {
            if (isSingleOrderType && !title.isNullOrEmpty()) {
                activity?.cast<MainActivity> {
                    it.setToolbarTitle(title.uppercase(Locale.getDefault()))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun createTab(tag: String, text: String, icon: Drawable): TabLayout.Tab? {
        return null
    }

    private fun showOrderTypeDialog() {
        setFragmentResultListener(ARGS_KEY_FRAGMENT_RESULT) { key, bundle ->
            clearFragmentResultListener(requestKey = ARGS_KEY_FRAGMENT_RESULT)
            try {
                val selectedOrderType =
                    OrderType.fromInt(bundle.getInt(OrderTypeDialogFragment.ARGS_KEY_ORDER_TYPE, 0))
                if (selectedOrderType != OrderType.NONE) {
                    selectOrderType(selectedOrderType)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        NavigationUtils.navigateSafe(
            findNavController(), OrderTypeNewFragmentDirections.actionOrderTypeFragmentToDialog()
        )
    }

    override fun initializeLocation() {
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
                .addOnCompleteListener(OnCompleteListener { task ->
                    try {
                        task.getResult(ApiException::class.java)
                        getLocation()
                    } catch (e: ApiException) {
                        getStoresFallback()
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
                })
        }

    }

    override fun getLocation() {
        activity?.apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                context?.let {
                    orderPresenter.getLocation(FusedLocationProvider(it))
                    Handler(Looper.getMainLooper()).postDelayed({
                        getStoresFallback()
                    }, 5_000L)
                }
            } else {
                CacheUtil.getInstance().orderLatitude = Values.DEFAULT_LATITUDE.toString()
                CacheUtil.getInstance().orderLongitude = Values.DEFAULT_LONGITUDE.toString()
                locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun getStoresFallback() {
        if ((orderPresenter as? OrderPresenter)?.hasLocation == false) {
            try {
                orderPresenter.getStores(1)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                    val bitmapDrawable = ContextCompat.getDrawable(
                        activity, com.herfy.ordertype.R.drawable.user_current_location
                    ) as BitmapDrawable?
                    if (bitmapDrawable != null) {
                        val bitmap = Bitmap.createScaledBitmap(bitmapDrawable.bitmap, 50, 50, true)
                        val canvas = Canvas(bitmap)
                        val paint = Paint()
                        canvas.drawBitmap(bitmap, 0f, 0f, paint)
                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                    }
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

    override fun showAddresses(
        bookmarkedAddresses: ArrayList<Address>, otherAddresses: ArrayList<Address>
    ) {
        binding.deliveryLayout.emptyState.isVisible = bookmarkedAddresses.isEmpty()
        deliveryAddressAdapter.updateDeliveryList(bookmarkedAddresses)
        val selectedAddress = CacheUtil.getInstance().orderAddress
        if (selectedAddress != null) {
            deliveryAddressAdapter.updateSelectedId(selectedAddress.id)
        }
    }

    override fun setAddress(address: Address, selected: Boolean) {}
    override fun selectAddressHome(address: Address?, redownloadMenu: Boolean) {
        activity?.cast<BaseActivity> {
            it.showLoadingDialog()
        }
        if (redownloadMenu) {
            orderPresenter.getDiffMenuCdn(address)
        } else {
            diffMenuSaved(address)
        }
    }

    override fun selectAddressWork(position: Int) {

    }

    override fun selectAddressOther(address: Address, redownloadMenu: Boolean) {
        activity?.cast<BaseActivity> {
            it.showLoadingDialog()
        }
        if (redownloadMenu) {
            orderPresenter.getDiffMenuCdn(address)
        } else {
            diffMenuSaved(address)
        }
    }

    override fun updateAddAddress(updateAddress: Address?, saveAddress: android.location.Address?) {
        activity?.runOnUiThread {
            bookmarkAddressDialogFragment?.dismiss()
            activity?.cast<BaseActivity> {
                bookmarkAddressDialogFragment = BookmarkAddressDialogFragment.newInstance(
                    orderPresenter, updateAddress
                ) { label ->
                    it.showLoadingDialog()
                    updateAddress?.attributes?.label = label
                    orderPresenter.updateAddress(false, updateAddress, false)
                }
                bookmarkAddressDialogFragment?.show(
                    it.supportFragmentManager, BookmarkAddressDialogFragment::class.java.simpleName
                )
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun showStores(pickupStores: ArrayList<Store>, curbsideStores: ArrayList<Store>) {
        activity?.runOnUiThread {
            val stores: ArrayList<Store>
            if (!curbsideTabSelected) {
                stores = pickupStores
                dineInStoresRecyclerViewAdapter?.notifyDataSetChanged()
                binding.storesRecyclerView.adapter = dineInStoresRecyclerViewAdapter
            } else {
                stores = curbsideStores
                pickupCurbsideStoresRecyclerViewAdapter?.notifyDataSetChanged()
                if (orderPresenter.pickupCurbsideStores.size == 1) {
                    pickupCurbsideStoresRecyclerViewAdapter?.selected = 0
                }
                binding.storesRecyclerView.adapter = pickupCurbsideStoresRecyclerViewAdapter
            }
            if (googleMap != null) {
                if (storeMarkers.isNotEmpty()) {
                    for (marker: Marker? in storeMarkers) {
                        marker?.remove()
                    }
                    storeMarkers.clear()
                }
                for (i in stores.indices) {
                    val store = stores[i]
                    val markerOptions = MarkerOptions()
                    markerOptions.title(i.toString())
                    markerOptions.position(LatLng(store.attributes.lat, store.attributes.lng))
                    try {
                        val activity: Activity? = activity
                        if (activity != null) {
                            val icon = ViewUtil.vectorDrawableToBitmapDescriptor(
                                activity, R.drawable.branch_unselected
                            )
                            if (icon != null) {
                                markerOptions.icon(icon)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    markerOptions.anchor(0.5f, 1f)
                    val marker = googleMap?.addMarker(markerOptions)
                    marker?.isVisible = CacheUtil.getInstance().orderType !== OrderType.DELIVERY
                    marker?.title = i.toString()
                    storeMarkers.add(marker)
                }
            }
            if (fromCurbside) {
                val storeId = curbsideStoreId.ifEmpty {
                    CacheUtil.getInstance().orderStore.id.orEmpty()
                }
                val position = orderPresenter.getStorePosition(
                    true, storeId
                )
                orderPresenter.pickupCurbsideStores?.let { list ->
                    val store: Store? = if (position != -1) list[position] else null
                    store?.let {
                        selectStore(
                            position, it, true
                        )
                    }
                }
                fromCurbside = false
                curbsideStoreId = ""
            } else if (!deliveryTabSelected && (CacheUtil.getInstance().orderType === OrderType.PICKUP) || (CacheUtil.getInstance().orderType === OrderType.DINE_IN) || (CacheUtil.getInstance().orderType === OrderType.CURBSIDE)) {
                if (CacheUtil.getInstance().orderStore != null) {
                    val position = orderPresenter.getStorePosition(
                        true, CacheUtil.getInstance().orderStore.id
                    )
                    orderPresenter.dineInStores?.let { list ->
                        val store: Store? = if (position != -1) list[position] else null
                        store?.let {
                            selectStore(
                                position, it, true
                            )
                        }

                    }
                }
            }
        }
    }

    override fun setStore(store: Store, selected: Boolean) {}
    override fun callStore(store: Store) {}
    override fun selectStore(position: Int, store: Store, initialLoad: Boolean) {
        currentSelectedStore = store

        if (!curbsideTabSelected) {
            dineInStoresRecyclerViewAdapter?.selected = position
            if (dineInStoresRecyclerViewAdapter?.selected != -1) {
                binding.continueButton.alpha = 1f
            } else {
                binding.continueButton.alpha = 0f
            }
            binding.continueButton.isFocusable = dineInStoresRecyclerViewAdapter?.selected != -1
            binding.continueButton.isClickable = dineInStoresRecyclerViewAdapter?.selected != -1
            if (!initialLoad && dineInTabSelected && store.attributes.hasPromptForTableNumber == 1) {
                showDineInTableNumberDialog(initialLoad, store)
            }
        } else {
            pickupCurbsideStoresRecyclerViewAdapter?.selected = position
            if (pickupCurbsideStoresRecyclerViewAdapter?.selected != -1) {
                binding.continueButton.alpha = 1f
            } else {
                binding.continueButton.alpha = 0f
            }

            binding.continueButton.isFocusable =
                pickupCurbsideStoresRecyclerViewAdapter?.selected != -1
            binding.continueButton.isClickable =
                pickupCurbsideStoresRecyclerViewAdapter?.selected != -1
        }
        showStore(false, position)
    }

    private fun showDineInTableNumberDialog(initialLoad: Boolean, store: Store?) {
        try {
            if (!initialLoad && dineInTabSelected && store != null && store.attributes.hasPromptForTableNumber == 1) {
                dineInTableNumberDialogFragment =
                    DineInTableNumberDialogFragment.newInstance(orderPresenter, store.id)
                dineInTableNumberDialogFragment?.isCancelable = false
                if (dineInTableNumberDialogFragment?.isVisible == false) {
                    activity?.let {
                        var currentDestination: Int? = null
                        it.cast<MainActivity> { mainActivity ->
                            currentDestination = mainActivity.currentDestination
                        }
                        it.cast<NavigationActivity> { menuItemActivity ->
                            currentDestination = menuItemActivity.currentDestination
                        }
                        if (currentDestination == R.id.nav_order_type) {
                            dineInTableNumberDialogFragment?.show(
                                it.supportFragmentManager,
                                DineInTableNumberDialogFragment::class.java.simpleName
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun showStore(markerClicked: Boolean, position: Int) {
        val activity: FragmentActivity = activity ?: return
        if (markerClicked && ((!curbsideTabSelected && dineInStoresRecyclerViewAdapter?.selected == position) || pickupCurbsideStoresRecyclerViewAdapter?.selected == position)) {
            val branch = orderPresenter.getStore(!curbsideTabSelected, position)
            if (branch != null) {
                val directionsBuilder =
                    Uri.Builder().scheme("https").authority("www.google.com").appendPath("maps")
                        .appendPath("dir").appendPath("").appendQueryParameter("api", "1")
                        .appendQueryParameter("travelmode", "driving").appendQueryParameter(
                            "destination",
                            branch.attributes.lat.toString() + "," + branch.attributes.lng
                        )

                if (StringUtil.hasOrderLatLong()) {
                    directionsBuilder.appendQueryParameter(
                        "origin",
                        CacheUtil.getInstance().orderLatitude + "," + CacheUtil.getInstance().orderLongitude
                    )
                }

                startActivity(Intent(Intent.ACTION_VIEW, directionsBuilder.build()))
            }
        } else {
            if (!curbsideTabSelected) {
                dineInStoresRecyclerViewAdapter?.selected = position
            } else {
                pickupCurbsideStoresRecyclerViewAdapter?.selected = position
            }
            RecyclerViewUtil.smoothSnapToPosition(
                binding.storesRecyclerView, position, LinearSmoothScroller.SNAP_TO_START
            )
            for (i in storeMarkers.indices) {
                val storeMarker = storeMarkers[i]
                if (storeMarker?.title != null && (storeMarker.title == position.toString())) {
                    try {
                        var store: Store? = null
                        storeMarker.title?.toInt()?.let {
                            store = orderPresenter.getStore(!curbsideTabSelected, it)
                        }
                        if (store != null) {
                            val inflater =
                                activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                            val view = inflater.inflate(R.layout.marker_store_new, null)
                            val textView =
                                view.findViewById<TextView>(com.herfy.ordertype.R.id.kmAway)
                            val distanceValue = StringUtil.getDistance(
                                store!!.attributes.distance
                            )
                            textView.text =
                                CacheUtil.getInstance().getTranslations(Text.KM_AWAY, "(x) away")
                                    .replace("(x)", distanceValue).toLowerCase()
                            view.setOnClickListener {
                                val directionsBuilder =
                                    Uri.Builder().scheme("https").authority("www.google.com")
                                        .appendPath("maps").appendPath("dir").appendPath("")
                                        .appendQueryParameter("api", "1")
                                        .appendQueryParameter("travelmode", "driving")
                                        .appendQueryParameter(
                                            "destination",
                                            store?.attributes?.lat.toString() + "," + store?.attributes?.lng
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
                            storeMarker.setIcon(
                                BitmapDescriptorFactory.fromBitmap(
                                    ViewUtil.createDrawableFromView(
                                        activity, view
                                    )
                                )
                            )
                            storeMarker.setAnchor(0.5f, 1f)
                            storeMarker.title = i.toString()
                            googleMap?.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    storeMarker.position, 11f
                                )
                            )
                        }
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }
                } else {
                    storeMarker?.setIcon(
                        ViewUtil.vectorDrawableToBitmapDescriptor(
                            activity, R.drawable.branch_unselected
                        )
                    )
                    storeMarker?.setAnchor(0.5f, 1f)
                    storeMarker?.title = i.toString()
                }
            }
        }
    }

    override fun showStore(open: Boolean, store: Store, error: String) {
        activity?.runOnUiThread {
            if (open) {

                if (!dineInTabSelected) {
                    if (CacheUtil.getInstance().curbsideCar != null) {
                        CacheUtil.getInstance().orderType = OrderType.CURBSIDE
                    } else {
                        CacheUtil.getInstance().orderType = OrderType.PICKUP
                        CacheUtil.getInstance().curbsideCar = null
                    }
                } else {
                    CacheUtil.getInstance().curbsideCar = null
                }

                val eventAttributes = HashMap<String, String>()
                if (CacheUtil.getInstance().orderType === OrderType.PICKUP) {
                    eventAttributes["Pickup"] = store.attributes.name
                } else if (CacheUtil.getInstance().orderType === OrderType.DINE_IN) {
                    eventAttributes["DineIn"] = store.attributes.name
                }
                if (eventAttributes.isNotEmpty()) {
                    logEvent("OrderTypeSelection", eventAttributes, "", 0.0)
                }
                CacheUtil.getInstance().orderStore = store
                navigateUp()
            } else {
                activity?.cast<BaseActivity> {
                    it.showAlertDialog(store.attributes.name, error)
                }
            }
        }
    }

    override fun showOrderType(orderType: OrderType) {}

    @SuppressLint("NotifyDataSetChanged")
    override fun selectDelivery() {
        deliveryTabSelected = true
        curbsideTabSelected = false
        dineInTabSelected = false
        binding.deliveryFrame.visible()
        binding.pickupFrame.gone()
        binding.continueButton.text =
            CacheUtil.getInstance().getTranslations(Text.ADD_NEW_ADDRESS, "Add new address")
        binding.slidingPanel.post {
            binding.slidingPanel.panelState = SlidingUpPanelLayout.PanelState.HIDDEN
        }
        try {
            val activity: Activity? = activity
            if (activity != null) {
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        binding.pickupLayout.location.visibility = View.GONE
        activity?.cast<BaseActivity> {
            it.dismissDialogs()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun selectCurbside() {
        deliveryTabSelected = false
        curbsideTabSelected = true
        dineInTabSelected = false
        binding.storesRecyclerView.adapter = pickupCurbsideStoresRecyclerViewAdapter
        pickupCurbsideStoresRecyclerViewAdapter?.notifyDataSetChanged()
        binding.slidingPanel.post {
            binding.slidingPanel.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        }
        activity?.cast<BaseActivity> {
            binding.pickupLayout.location.visibility = View.VISIBLE
            it.dismissDialogs()
        }
    }

    private fun showCurbside() {
        activity?.let {
            val intent = Intent(it, CurbsideNewActivity::class.java)
            curbsideLauncher.launch(intent)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun selectPickup() {
        deliveryTabSelected = false
        curbsideTabSelected = true
        dineInTabSelected = false
        binding.pickupFrame.visible()
        binding.deliveryFrame.gone()
        binding.storesRecyclerView.adapter = pickupCurbsideStoresRecyclerViewAdapter
        if (orderPresenter.pickupCurbsideStores.size == 1) {
            pickupCurbsideStoresRecyclerViewAdapter?.selected = 0
            showStore(false, 0)
        } else {
            pickupCurbsideStoresRecyclerViewAdapter?.notifyDataSetChanged()
        }
        pickupCurbsideStoresRecyclerViewAdapter?.selected?.let {
            if (it != -1) {
                RecyclerViewUtil.smoothSnapToPosition(
                    binding.storesRecyclerView, it, LinearSmoothScroller.SNAP_TO_START
                )
            }
        }
        binding.slidingPanel.post {
            binding.slidingPanel.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        }
        activity?.cast<BaseActivity> {

        }

        binding.pickupLayout.location.visibility = View.VISIBLE
        if (pickupCurbsideStoresRecyclerViewAdapter?.selected != -1) {
            binding.continueButton.alpha = 1f
        } else {
            binding.continueButton.alpha = 0f
        }
        binding.continueButton.isFocusable = pickupCurbsideStoresRecyclerViewAdapter?.selected != -1
        binding.continueButton.isClickable = pickupCurbsideStoresRecyclerViewAdapter?.selected != -1
        activity?.cast<BaseActivity> {
            it.dismissDialogs()
        }
        binding.continueButton.text =
            CacheUtil.getInstance().getTranslations(Text.CONTINUE_LABEL, "Continue")
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun selectDineIn() {
        binding.continueButton.text =
            CacheUtil.getInstance().getTranslations(Text.CONTINUE_LABEL, "Continue")
        deliveryTabSelected = false
        curbsideTabSelected = false
        dineInTabSelected = true
        binding.pickupFrame.visible()
        binding.deliveryFrame.gone()
        binding.storesRecyclerView.adapter = dineInStoresRecyclerViewAdapter
        if (orderPresenter.dineInStores.size == 1) {
            dineInStoresRecyclerViewAdapter?.selected = 0
            showStore(false, 0)
        } else {
            dineInStoresRecyclerViewAdapter?.notifyDataSetChanged()
        }
        dineInStoresRecyclerViewAdapter?.selected?.let {
            if (it != -1) {
                RecyclerViewUtil.smoothSnapToPosition(
                    binding.storesRecyclerView, it, LinearSmoothScroller.SNAP_TO_START
                )
            }
        }
        binding.slidingPanel.post {
            binding.slidingPanel.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        }
        activity?.cast<BaseActivity> {

        }

        binding.pickupLayout.location.visibility = View.VISIBLE
        if (dineInStoresRecyclerViewAdapter?.selected != -1) {
            binding.continueButton.alpha = 1f
        } else {
            binding.continueButton.alpha = 0f
        }
        binding.continueButton.isFocusable = dineInStoresRecyclerViewAdapter?.selected != -1
        binding.continueButton.isClickable = dineInStoresRecyclerViewAdapter?.selected != -1
        binding.confirmContainer.visibility = View.VISIBLE
        activity?.cast<BaseActivity> {
            it.dismissDialogs()
        }
    }

    override fun saveAddress(address: String) {
        activity?.runOnUiThread {
            if (CacheUtil.getInstance().orderType !== OrderType.CURBSIDE) {
                CacheUtil.getInstance().curbsideCar = null
            }
            if (isMenuSeparate && isMenuRedownloaded) {
                navigateUp()
            } else {
                activity?.cast<BaseActivity> {
                    it.dismissDialogs()
                    val eventAttributes = HashMap<String, String>()
                    eventAttributes["Delivery"] = StringUtil.trimEventAttribute(address)
                    logEvent("OrderTypeSelection", eventAttributes, "", 0.0)
                    it.onBackPressed()
                }
            }
        }
    }


    override fun saveAddress(address: Address) {}
    override fun updateAddress(address: String) {
        if (bookmarkAddressDialogFragment != null) {
            bookmarkAddressDialogFragment?.dismiss()
        }
        activity?.cast<BaseActivity> {
            it.dismissDialogs()
        }
    }

    override fun showUnavailableMenuItems() {
        activity?.runOnUiThread {
            activity?.cast<BaseActivity> {
                it.dismissDialogs()
                val cartDialogFragment = CartDialogFragment.newInstance(orderPresenter)
                cartDialogFragment.show(
                    it.supportFragmentManager, CartDialogFragment::class.java.simpleName
                )
            }
        }
    }

    override fun deleteUnavailableItems() {
        showMessage(
            CacheUtil.getInstance()
                .getTranslations(Text.REMOVED_ITEM_CART_SUCCESSFULLY, "Item removed from cart")
        )
        orderPresenter.setOrderType()
        activity?.cast<MainActivity> {
            it.setCartQuantity(CacheUtil.getInstance().cartQuantity)
        }
        activity?.cast<NavigationActivity> {
            it.setCartQuantity(CacheUtil.getInstance().cartQuantity)
        }
    }

    override fun showPhoneNumberError(error: String) {}
    override fun showError(error: String) {
        activity?.runOnUiThread {
            activity?.cast<BaseActivity> {
                if (bookmarkAddressDialogFragment != null) {
                    bookmarkAddressDialogFragment?.dismiss()
                }
                it.showAlertDialog("", error)
            }
        }
    }

    override fun updateMap(
        orderType: OrderType, startLocation: LatLng, endLocation: LatLng, bounds: LatLngBounds
    ) {
    }

    override fun showOrder(order: OrderDetails) {}
    override fun showOrderStatuses(orderStatuses: ArrayList<OrderStatus>) {}
    override fun setTimeLine(orderStatus: OrderStatus) {}
    override fun showStatus(orderStatus: OrderStatus) {}
    override fun showDriver(latLng: LatLng) {}
    override fun updateDriver(latLng: LatLng) {}
    override fun showMessage(message: String) {
        activity?.runOnUiThread {
            activity?.cast<BaseActivity> {
                it.dismissDialogs()
                Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun showRatings(orderStatus: OrderStatus, ratings: List<Rating>) {}
    override fun showOrders() {}
    override fun enableConfirmButton(enable: Boolean, storeName: String) {
        if (!deliveryTabSelected) {
            isPreselectedBranch = enable
            if (enable) {
                binding.continueButton.alpha = 1f
            } else {
                binding.continueButton.alpha = 0f
            }
            binding.continueButton.isFocusable = enable
            binding.continueButton.isClickable = enable
        }
    }

    override fun saveToCache(key: String?, value: String?) {
        activity?.apply {
            runOnUiThread(Runnable { CacheHelper.save(this, key, value) })
        }
    }

    override fun preloadImage(imageUrl: String?, requestOptions: RequestOptions) {
        activity?.apply {
            runOnUiThread(Runnable {
                if (!isDestroyed && !isFinishing) {
                    Glide.with(this).downloadOnly().load(imageUrl).apply(requestOptions).submit()
                }
            })
        }
    }

    override fun diffMenuSaved(address: Address?) {
        orderPresenter.removeMukafaaRedemption()
        if (CacheUtil.getInstance().orderType == OrderType.DELIVERY || deliveryTabSelected) {
            orderPresenter.getStore(address)
        } else {
            if (dineInTabSelected) {
                dineInStoresRecyclerViewAdapter?.let { adapter ->
                    orderPresenter.continueStore(
                        true, adapter.selected, null
                    )
                }
            } else {
                val position =
                    if (isPreselectedBranch) 0 else pickupCurbsideStoresRecyclerViewAdapter?.selected
                position?.let {
                    orderPresenter.continueStore(
                        false, it, null
                    )
                }
            }
        }
        if (fromAddAddressDifferentMenu) {
            fromAddAddressDifferentMenu = false
            navigateUp()
        }
    }

    private fun showPromptChangeOrderTypeDialog(
        message: String,
        positive: DialogInterface.OnClickListener,
        negative: DialogInterface.OnClickListener
    ) {
        activity?.let {
            (it as BaseActivity).showPromptAlertDialog(
                "",
                message,
                CacheUtil.getInstance().getTranslations(Text.YES_CAPS),
                positive,
                CacheUtil.getInstance().getTranslations(Text.NO_CAPS),
                negative
            )
        }
    }

    override fun showPromptChangeOrderType(address: Address) {
        showPromptChangeOrderTypeDialog(CacheUtil.getInstance().getTranslations(
            Text.CHANGE_DELIVERY_INSTEAD_LABEL,
            "Your current Pickup order will be removed from cart. Would you like to proceed and have your order delivered instead?"
        ), { _, _ ->
            CacheUtil.getInstance().cart = ArrayList()
            orderPresenter.setSelectedAddress(address)
            val label = address.attributes.label
            CacheUtil.getInstance().orderType = OrderType.DELIVERY
            if (label != null && !label.equals(
                    "null", ignoreCase = true
                ) && label.isNotEmpty()
            ) {
                selectAddressHome(address, true)
            } else {
                selectAddressOther(address, true)
            }
            isMenuRedownloaded = true;

        }, { dialog, which ->
            activity?.cast<BaseActivity> { it.dismissDialogs() }
            dialog.dismiss()
        })

    }

    override fun showMinimumDeliveryForAreaMessage(error: String?) {
        activity?.runOnUiThread {
            activity?.cast<BaseActivity> {
                if (bookmarkAddressDialogFragment != null) {
                    bookmarkAddressDialogFragment?.dismiss()
                }
                it.showAlertDialog(
                    "", error, CacheUtil.getInstance().getTranslations(Text.OK)
                ) { dialogInterface, which ->
                    orderPresenter.setOrderType()
                }
            }
        }
    }

    private fun proceedToCurbside() {
        showCurbside()
    }

    private fun continueSelectingType(it: BaseActivity, otPickupSelected: OrderType) {
        it.showLoadingDialog()
        val cachedOrderType = CacheUtil.getInstance().orderType
        if (orderTypeSelected == OrderType.PICKUP) {
            if (otPickupSelected == OrderType.DRIVE_THRU) {
                orderTypeSelected = OrderType.DRIVE_THRU
            } else if (otPickupSelected == OrderType.EAT_IN) {
                orderTypeSelected = OrderType.EAT_IN
            } else if (otPickupSelected == OrderType.CURBSIDE) {
                orderTypeSelected = OrderType.CURBSIDE
            }
        }

        if (isMenuSeparate && cachedOrderType == OrderType.DELIVERY && orderTypeSelected == OrderType.PICKUP) {
            val itemsInCart = CacheUtil.getInstance().cart
            if (itemsInCart.size > 0) {
                showPromptChangeOrderTypeDialog(CacheUtil.getInstance().getTranslations(
                    Text.CHANGE_PICKUP_INSTEAD_LABEL,
                    "Your current Delivery order will be removed from cart. Would you like to proceed and pick up your order instead?"
                ), { _, _ ->
                    CacheUtil.getInstance().cart = ArrayList()
                    orderPresenter.getDiffMenuCdn(null)
                    isMenuRedownloaded = true;
                    CacheUtil.getInstance().orderType = orderTypeSelected
                }, { dialog, which ->
                    it.dismissDialogs()
                    dialog.dismiss()
                })
            } else {
                orderPresenter.getDiffMenuCdn(null)
                isMenuRedownloaded = true;
                CacheUtil.getInstance().orderType = orderTypeSelected
            }
        } else {
            CacheUtil.getInstance().orderType = orderTypeSelected
            if (orderTypeSelected != OrderType.NONE) {
                if (cachedOrderType == OrderType.NONE) {
                    orderPresenter.getDiffMenuCdn(null)
                } else diffMenuSaved(null)
            } else {
                diffMenuSaved(null)
            }
        }
    }

    private fun chooseType(activity: BaseActivity) {
        dismissPickupTypeDialog()
        choosePickupTypeDialog = ChoosePickupTypeDialog.newInstance(currentSelectedStore,
            object : ChoosePickupTypeDialog.OnSubmit {
                override fun orderType(type: OrderType) {
                    if (type == OrderType.CURBSIDE) {
                        proceedToCurbside()
                    } else {
                        continueSelectingType(activity, type)
                    }
                    dismissPickupTypeDialog()
                }

                override fun exit() {
                    dismissPickupTypeDialog()
                }

            })
        choosePickupTypeDialog.show(
            requireActivity().supportFragmentManager, ChoosePickupTypeDialog::class.java.simpleName
        )
    }

    private fun dismissPickupTypeDialog() {
        try {
            choosePickupTypeDialog.dismiss()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

}