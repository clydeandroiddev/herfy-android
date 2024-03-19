package com.herfy.history.historynew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.model.LatLng
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.history.OrdersContract
import com.herfy.history.OrdersPresenter
import com.herfy.history.databinding.FragmentNewOrderHistoryBinding
import com.herfy.history.historynew.adapter.HistoryListAdapter
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DialogUtils
import com.skylinedynamics.solosdk.api.models.objects.Address
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus
import com.skylinedynamics.solosdk.api.models.objects.Rating
import com.skylinedynamics.solosdk.api.models.objects.Store
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewHistoryFragment : BaseFragment(), OrdersContract.View {
    private var ordersPresenter: OrdersContract.Presenter? = null

    private lateinit var binding: FragmentNewOrderHistoryBinding

    private lateinit var historyListAdapter: HistoryListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ordersPresenter = OrdersPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewOrderHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ordersPresenter?.start()
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.showLoadingDialog()
        }
        ordersPresenter?.getOrders()
    }

    override fun setPresenter(presenter: OrdersContract.Presenter) {
        ordersPresenter = presenter
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
            historyListAdapter =
                HistoryListAdapter(ArrayList(), object : HistoryListAdapter.HistoryListener {
                    override fun onViewDetails(item: OrderHistory, position: Int) {

                    }

                    override fun onReorder(item: OrderHistory, position: Int) {

                    }

                    override fun onRate(item: OrderHistory, position: Int) {

                    }
                })
            pastOrdersList.adapter = historyListAdapter
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.apply {
            title.text = CacheUtil.getInstance().getTranslations(Text.MY_ORDERS, "My Orders")
            pastOrdersLabel.text =
                CacheUtil.getInstance().getTranslations(Text.PAST_ORDERS, "Past Orders")
        }
    }

    override fun showOrders(newestToOldest: Boolean, orders: ArrayList<OrderHistory>?) {
        dismissDialogs()
        orders?.let {
            historyListAdapter.updateList(orders)
        }
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

    override fun showOrder(order: OrderDetails?, store: Store?, address: Address?) {

    }

    override fun showOrderStatuses(orderStatuses: ArrayList<OrderStatus>?, isDone: Boolean) {

    }

    override fun showMenuItems(order: OrderDetails?, menuItems: ArrayList<MenuItem>?) {

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
