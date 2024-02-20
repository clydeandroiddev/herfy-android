package com.herfy.order.views.ordertype.addaddress

import android.graphics.drawable.Drawable
import android.os.Bundle
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.tabs.TabLayout
import com.herfy.base.BaseActivity
import com.herfy.databinding.ActivityFeedbackBinding
import com.herfy.order.OrderContract
import com.herfy.order.OrderPresenter
import com.skylinedynamics.solosdk.api.models.objects.Address
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Rating
import com.skylinedynamics.solosdk.api.models.objects.Store

class AddNewAddressActivity : BaseActivity(), OrderContract.View {

    private lateinit var orderPresenter: OrderContract.Presenter

    private lateinit var binding: ActivityFeedbackBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)
        orderPresenter = OrderPresenter(this)
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

    }

    override fun setupViews() {

    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {

    }

    override fun showOrderTypes(orderTypes: MutableSet<OrderType>?) {

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