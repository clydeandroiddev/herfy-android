package com.herfy.order.views.ordertype

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.tabs.TabLayout
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.ext.cast
import com.herfy.main.MainActivity
import com.herfy.main.NavigationActivity
import com.herfy.order.OrderContract
import com.herfy.order.OrderPresenter
import com.herfy.ordertype.databinding.FragmentOrderTypeNewBinding
import com.herfy.util.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.Address
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Rating
import com.skylinedynamics.solosdk.api.models.objects.Store

class OrderTypeNewFragment : BaseFragment(), OrderContract.View {

    companion object {
        const val ARGS_KEY_FRAGMENT_RESULT = "fragmentResult"
        const val ARGS_KEY_CURBSIDE = "curbside"
        const val ARGS_KEY_IS_SAME_BRANCH = "is_same_branch"
    }

    private lateinit var binding: FragmentOrderTypeNewBinding

    private lateinit var orderPresenter: OrderContract.Presenter

    private var typeOrder: Int = 0
    private var isMenuSeparate = false
    private var isSingleOrderType = false
    private var isDeliveryAvailable = false
    private var isCurbsideAvailable = false
    private var isPickupAvailable = false
    private var isDineInAvailable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderPresenter = OrderPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentOrderTypeNewBinding.inflate(inflater, container, false)
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
            activity?.cast<NavigationActivity> {
                it.hideBackButton()
            }
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

    override fun showOrderTypes(orderTypes: MutableSet<OrderType>?) {
        activity?.cast<BaseActivity> {
            it.dismissDialogs()
        }
        orderTypes?.let {
            if (orderTypes.size > 1) {
                isSingleOrderType = false
                orderTypes.map {
                    createOrderType(it)
                }
            } else {
                if (orderTypes.size == 1) {
                    for (orderType: OrderType in orderTypes) {
                        if (orderType === OrderType.DELIVERY) {
                            isDeliveryAvailable = true
                        } else if (orderType === OrderType.CURBSIDE) {
                            isCurbsideAvailable = true
                        } else if (orderType === OrderType.PICKUP) {
                            isPickupAvailable = true
                        } else if (orderType === OrderType.DINE_IN) {
                            isDineInAvailable = true
                        }
                    }
                }
                isSingleOrderType = true
            }
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

    override fun setupViews() {}

    override fun setupFonts() {
    }

    override fun setupTranslations() {
    }

    override fun createOrderType(orderType: OrderType?) {
    }

    override fun selectOrderType(orderType: OrderType?) {
    }

    override fun createTab(tag: String?, text: String?, icon: Drawable?): TabLayout.Tab? {
        return null
    }

    override fun initializeLocation() {
    }

    override fun getLocation() {
    }

    override fun showLocation(isPlace: Boolean, latLng: LatLng?, address: String?) {
    }

    override fun showAddresses(
        bookmarkedAddresses: ArrayList<Address>?, otherAddresses: ArrayList<Address>?
    ) {
    }

    override fun setAddress(address: Address?, selected: Boolean) {
    }

    override fun selectAddressHome(address: Address?, redownloadMenu: Boolean) {
    }

    override fun selectAddressWork(position: Int) {
    }

    override fun selectAddressOther(address: Address?, redownloadMenu: Boolean) {
    }

    override fun updateAddAddress(updateAddress: Address?, saveAddress: android.location.Address?) {
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
    }

    override fun saveAddress(address: Address?) {
    }

    override fun updateAddress(address: String?) {
    }

    override fun showUnavailableMenuItems() {
    }

    override fun deleteUnavailableItems() {
    }

    override fun showPhoneNumberError(error: String?) {
    }

    override fun showError(error: String?) {
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
    }

    override fun showRatings(orderStatus: OrderStatus?, ratings: MutableList<Rating>?) {
    }

    override fun showOrders() {
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
    }

}