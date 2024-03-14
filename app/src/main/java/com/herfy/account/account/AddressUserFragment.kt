package com.herfy.account.account

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.tabs.TabLayout
import com.herfy.account.AccountContract
import com.herfy.account.AccountPresenter
import com.herfy.base.BaseFragment
import com.herfy.order.OrderContract
import com.herfy.order.OrderPresenter
import com.herfy.profile.databinding.FragmentUserAddressesBinding
import com.herfy.profile.databinding.FragmentUserEditProfileBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.Address
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Rating
import com.skylinedynamics.solosdk.api.models.objects.Store
import dagger.hilt.android.AndroidEntryPoint
import java.util.ArrayList
import java.util.Date

@AndroidEntryPoint
class AddressUserFragment : BaseFragment(), OrderContract.View {
    private var accountPresenter: OrderContract.Presenter? = null

    private lateinit var binding: FragmentUserAddressesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountPresenter = OrderPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserAddressesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        accountPresenter?.start()
    }

    override fun setPresenter(presenter: OrderContract.Presenter) {
        accountPresenter = presenter
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
            continueButton.setOnClickListener {

            }
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.title.text =
            CacheUtil.getInstance().getTranslations(Text.ADDRESSES_LABEL, "Addresses")
        binding.continueButton.text =
            CacheUtil.getInstance().getTranslations(Text.ADD_NEW_ADDRESS_LABEL, "Add New Address")
    }

    override fun showOrderTypes(orderTypes: MutableSet<OrderType>?) {

    }

    override fun addToCartFavorite(menuItem: MenuItem?) {

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
        bookmarkedAddresses: ArrayList<Address>?,
        otherAddresses: ArrayList<Address>?
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

    override fun logFirebaseEvent(event: String?) {

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
        orderType: OrderType?,
        startLocation: LatLng?,
        endLocation: LatLng?,
        bounds: LatLngBounds?
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

    override fun showMenuItem(categoryId: String?, menuItemId: String?) {

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
