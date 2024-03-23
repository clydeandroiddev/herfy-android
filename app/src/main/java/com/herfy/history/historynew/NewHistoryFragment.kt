package com.herfy.history.historynew

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.model.LatLng
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.common.formatOrderTime
import com.herfy.common.orderTypeString
import com.herfy.ext.cast
import com.herfy.firebase.FirebaseService
import com.herfy.history.OrdersContract
import com.herfy.history.OrdersPresenter
import com.herfy.history.databinding.FragmentNewOrderHistoryBinding
import com.herfy.history.historynew.adapter.HistoryListAdapter
import com.herfy.main.MainActivity
import com.herfy.util.StringUtil
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DialogUtils
import com.skylinedynamics.solosdk.api.models.objects.Address
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus
import com.skylinedynamics.solosdk.api.models.objects.OrderStatusType
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
        if (ordersPresenter?.orderHistories?.isEmpty() != true) {
            showOrders(true, ordersPresenter?.orderHistories)
        } else {
            activity?.let {
                val baseActivity = it as BaseActivity
                baseActivity.showLoadingDialog()
            }
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
                        findNavController().navigate(
                            NewHistoryFragmentDirections.actionHistoryToDetails(item.id)
                        )
                    }

                    override fun onReorder(item: OrderHistory, position: Int) {
                        MixpanelUtil.trackEvent(MixpanelUtil.EVENT_REORDER)
                        if (CacheUtil.getInstance().application.attributes.menuOnly) {
                            FirebaseService.newNotification(
                                CacheUtil.getInstance().getTranslations(
                                    Text.MENU_ONLY_MESSAGE,
                                    "Sorry, ordering is not available at the moment"
                                ), binding.back.context
                            )
                            return
                        }
                        activity?.cast<BaseActivity> {
                            it.showLoadingDialog()
                        }
                        ordersPresenter?.getOrder(item.id, -1, false, true)
                    }

                    override fun onRate(item: OrderHistory, position: Int) {

                    }
                })
            pastOrdersList.adapter = historyListAdapter
        }
    }

    private fun checkEmptyState() {
        if (ordersPresenter?.orderHistories?.isEmpty() != true) {
            binding.emptyStatesLayout.isVisible = false
        } else {
            binding.emptyStatesLayout.isVisible = true
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.apply {
            title.text = CacheUtil.getInstance().getTranslations(Text.MY_ORDERS, "My Orders")
            pastOrdersLabel.text =
                CacheUtil.getInstance().getTranslations(Text.PAST_ORDERS, "Past Orders")
            tapOnLabel.text = CacheUtil.getInstance().getTranslations(
                Text.TAP_TRACK_ORDER, "Tap on “Track Order” for detailed tracking."
            )
            inProgressLabel.text =
                CacheUtil.getInstance().getTranslations(Text.IN_PROGRESS, "In Progress")
            trackButton.text =
                CacheUtil.getInstance().getTranslations(Text.TRACK_ORDER, "Track Order")
            noOrdersLabel.text =
                CacheUtil.getInstance().getTranslations(Text.NO_ORDERS_FOUND, "No orders found.")
        }
    }

    override fun showLocation(isPlace: Boolean, latLng: LatLng?, address: String?) {
        TODO("Not yet implemented")
    }

    override fun showOrders(newestToOldest: Boolean, orders: ArrayList<OrderHistory>?) {
        dismissDialogs()
        orders?.let {
            if (orders.isNotEmpty()) {
                val orderProgressId = processInProgress(orders)
                var pastOrders = ArrayList<OrderHistory>()
                if (orderProgressId != null) {
                    pastOrders.addAll(orders.filter { it.id != orderProgressId })
                } else {
                    pastOrders = orders
                }
                binding.pastOrdersLabel.isVisible = pastOrders.isNotEmpty()
                historyListAdapter.updateList(pastOrders)
            }
        }
        checkEmptyState()
    }

    @SuppressLint("SetTextI18n")
    private fun processInProgress(orders: ArrayList<OrderHistory>): String? {
        val order = orders.firstOrNull {
            it.sequence < OrderStatusType.FINISHED.value
        }
        order?.let { history ->
            binding.apply {
                inProgressLayout.isVisible = true
                itemPrice.text = StringUtil.getNewFormattedPrice(
                    history.total, true
                )
                deliveryAndStore.text =
                    history.attrType.orderTypeString() + " - " + history.locationName
                itemsCount.text =
                    CacheUtil.getInstance().getTranslations(Text.CART_ITEM_COUNT_LABEL, "(x) items")
                        .replace("(x)", history.menuImages.size.toString())

                dateToday.text = history.createdAt.formatOrderTime()
                orderNumber.text = CacheUtil.getInstance()
                    .getTranslations(Text.ORDER_NUM, "Order #") + " " + history.id
                status.text = history.description
                trackButton.setOnClickListener {
                    findNavController().navigate(
                        NewHistoryFragmentDirections.actionHistoryToTracker(history.id)
                    )
                }
            }
            return history.id
        }
        return null
    }

    override fun setOrder(order: OrderHistory?) {

    }

    override fun updateOrder(position: Int) {

    }

    override fun selectOrder(order: OrderHistory?) {

    }

    override fun showError(error: String?) {
        checkEmptyState()
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
        activity?.cast<MainActivity> {
            it.runOnUiThread {
                val vibrator = it.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                500, VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        vibrator.vibrate(100)
                    }
                }
                Toast.makeText(
                    it,
                    CacheUtil.getInstance()
                        .getTranslations(Text.ITEM_ADDED_TO_CART, "Item added to cart"),
                    Toast.LENGTH_SHORT
                ).show()
                it.dismissDialogs()
                MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_CART)
                it.showCartFromHerfyHome()
                navigateUp()
            }
        }
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
