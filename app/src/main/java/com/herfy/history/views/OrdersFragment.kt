package com.herfy.history.views

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.analytics.FirebaseAnalytics
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.databinding.FragmentOrdersBinding
import com.herfy.ext.cast
import com.herfy.history.OrdersContract
import com.herfy.history.OrdersPresenter
import com.herfy.history.adapters.OrdersRecyclerViewAdapter
import com.herfy.main.MainActivity
import com.herfy.utils.helper.CacheHelper
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.NavigationUtils
import com.skylinedynamics.solosdk.api.models.objects.Address
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus
import com.skylinedynamics.solosdk.api.models.objects.Rating
import com.skylinedynamics.solosdk.api.models.objects.Store

class OrdersFragment : BaseFragment(), OrdersContract.View {

    private lateinit var binding: FragmentOrdersBinding

    private lateinit var ordersPresenter: OrdersContract.Presenter

    private var adapter: OrdersRecyclerViewAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ordersPresenter = OrdersPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.showLoadingDialog()
        }
        ordersPresenter!!.start()
        binding.sortCard.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
        ordersPresenter!!.getOrders()
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
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue)
        }
    }

    override fun setCartExpiry() {

    }

    override fun setupViews() {

        activity?.cast<MainActivity> {
            it.setCloseButton {
                findNavController().navigateUp()
            }
        }

        binding.goToMenu.setOnClickListener { findNavController().navigateUp() }
        binding.sortContainer.setOnClickListener { ordersPresenter!!.sortOrders() }

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.empty.visibility = View.GONE
            binding.sortCard.visibility = View.GONE
            binding.recyclerView.visibility = View.GONE
            ordersPresenter.getOrders()
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = OrdersRecyclerViewAdapter(context, ordersPresenter)
        binding.recyclerView.adapter = adapter
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.sortLabel.text = CacheUtil.getInstance()
            .getTranslations(Text.NEWEST_TO_OLDEST, "Newest to oldest")
        binding.emptyTitle.text = CacheUtil.getInstance()
            .getTranslations(Text.NO_ORDER_FOUND, "No order found")
        binding.emptySubtitle.text = CacheUtil.getInstance().getTranslations(
            Text.YOU_WILL_SEE_YOUR_ORDER_HISTORY_HERE_IF,
            "You will see your order history here if"
        )
        binding.emptyMessage.text = CacheUtil.getInstance().getTranslations(
            Text.YOU_BUY_SOMETHING_FROM_OUR_MENU,
            "you buy something from our menu"
        )
        binding.goToMenu.text = CacheUtil.getInstance()
            .getTranslations(Text.GO_TO_MENU, "GO TO MENU")
    }

    override fun showLocation(isPlace: Boolean, latLng: LatLng?, address: String?) {
        TODO("Not yet implemented")
    }

    override fun showOrders(newestToOldest: Boolean, orders: ArrayList<OrderHistory>) {
        if (newestToOldest) {
            binding.sortLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.NEWEST_TO_OLDEST, "Newest to oldest")
        } else {
            binding.sortLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.OLDEST_TO_NEWEST, "Oldest to newest")
        }
        binding.swipeRefreshLayout.isRefreshing = false
        adapter!!.notifyDataSetChanged()
        if (orders.size > 0) {
            binding.empty.visibility = View.GONE
            binding.sortCard.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.VISIBLE
        } else {
            binding.empty.visibility = View.VISIBLE
            binding.sortCard.visibility = View.GONE
            binding.recyclerView.visibility = View.GONE
        }
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
        }
    }

    override fun setOrder(order: OrderHistory) {}
    override fun updateOrder(position: Int) {
        activity?.let {
            it.runOnUiThread { adapter!!.notifyItemChanged(position) }
        }
    }

    override fun selectOrder(order: OrderHistory) {
        MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_ORDER_DETAILS)
        NavigationUtils.navigateSafe(findNavController(), OrdersFragmentDirections.actionOrdersToOrder(order.id))
    }

    override fun showError(error: String) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.showAlertDialog("", error)
        }
    }

    override fun showOrder(order: OrderDetails?, store: Store?, address: Address?) {
        ordersPresenter!!.checkReorder()
    }

    override fun showOrderStatuses(orderStatuses: ArrayList<OrderStatus>, isDone: Boolean) {}
    override fun showMenuItems(order: OrderDetails, menuItems: ArrayList<MenuItem>) {}
    override fun setTimeLine(isCancelled: Boolean, orderStatus: OrderStatus, noDriver: Boolean) {}
    override fun setMenuItem(menuItem: MenuItem) {}
    override fun showCart() {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.dismissDialogs()
            it.runOnUiThread {
                val vibrator = baseActivity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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
                    baseActivity,
                    CacheUtil.getInstance().getTranslations(Text.ITEM_ADDED_TO_CART, "Item added to cart"),
                    Toast.LENGTH_SHORT
                ).show()
                baseActivity.dismissDialogs()
                MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_CART)
                NavigationUtils.navigateSafe(findNavController(), OrdersFragmentDirections.actionOrdersToCart())
            }
        }
    }

    override fun updateStatus(orderDetails: OrderDetails, orderStatus: OrderStatus) {}
    override fun showDriver(customerLatLng: LatLng, driverLatLng: LatLng) {}
    override fun updateDriver(latLng: LatLng, mobileNumber: String) {}
    override fun cancel() {}
    override fun showRatings(orderStatus: OrderStatus, ratings: List<Rating>) {}
    override fun showMessage(message: String) {}
    override fun customerArrivedSuccess() {}
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

    override fun showWhatsApp(mobileNumber: String) {}
}