package com.herfy.history.historynew

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
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.model.LatLng
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.common.formatOrderTime
import com.herfy.common.orderDetailsType
import com.herfy.ext.cast
import com.herfy.firebase.FirebaseService
import com.herfy.history.OrdersContract
import com.herfy.history.OrdersPresenter
import com.herfy.history.databinding.FragmentNewOrderDetailsBinding
import com.herfy.history.historynew.adapter.OrderMenuItemsAdapter
import com.herfy.history.views.OrderFragmentDirections
import com.herfy.main.MainActivity
import com.herfy.util.StringUtil
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DialogUtils
import com.herfy.utils.helper.NavigationUtils
import com.skylinedynamics.solosdk.api.models.objects.Address
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus
import com.skylinedynamics.solosdk.api.models.objects.Rating
import com.skylinedynamics.solosdk.api.models.objects.Store
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewOrderDetailsFragment : BaseFragment(), OrdersContract.View {

    private var orderPresenter: OrdersContract.Presenter? = null

    private val args: NewOrderDetailsFragmentArgs by navArgs()

    private lateinit var binding: FragmentNewOrderDetailsBinding

    private lateinit var orderMenuItemsAdapter: OrderMenuItemsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderPresenter = OrdersPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewOrderDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        orderPresenter?.start()
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.showLoadingDialog()
        }
        orderPresenter?.getOrder(args.orderId, -1, false, false)
    }

    override fun setPresenter(presenter: OrdersContract.Presenter) {
        orderPresenter = presenter
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

    private fun reOrder() {
        MixpanelUtil.trackEvent(MixpanelUtil.EVENT_REORDER)
        if (CacheUtil.getInstance().application.attributes.menuOnly) {
            FirebaseService.newNotification(
                CacheUtil.getInstance().getTranslations(
                    Text.MENU_ONLY_MESSAGE, "Sorry, ordering is not available at the moment"
                ), binding.back.context
            )
            return
        }
        activity?.cast<BaseActivity> {
            it.showLoadingDialog()
        }
        orderPresenter?.checkReorder()
    }

    override fun setupViews() {
        binding.apply {
            back.setOnClickListener {
                navigateUp()
            }

            reOrderButton.setOnClickListener {
                reOrder()
            }

            reOrderButtonFull.setOnClickListener {
                reOrder()
            }

            rateButton.setOnClickListener {
                // TODO Rate here
            }

            orderMenuItemsAdapter = OrderMenuItemsAdapter(ArrayList())
            itemsList.adapter = orderMenuItemsAdapter
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.apply {
            title.text =
                CacheUtil.getInstance().getTranslations(Text.ORDER_DETAILS, "Order Details")
            orderItemsLabel.text =
                CacheUtil.getInstance().getTranslations(Text.ORDER_ITEMS, "Order Items")
            paymentSummaryLabel.text =
                CacheUtil.getInstance().getTranslations(Text.PAYMENT_SUMMARY, "Payment Summary")
            paymentMethodLabel.text =
                CacheUtil.getInstance().getTranslations(Text.PAYMENT_METHOD, "Payment Method")
            reOrderButton.text =
                CacheUtil.getInstance().getTranslations(Text.REORDER_LABEL, "Reorder")
            rateButton.text = CacheUtil.getInstance().getTranslations(Text.RATE, "Rate")
            subtotalLabel.text = CacheUtil.getInstance().getTranslations(Text.SUBTOTAL, "Subtotal")
            deliverLabel.text = CacheUtil.getInstance().getTranslations(Text.DELIVERY_CHARGE, "Delivery Charges")
            discountLabel.text = CacheUtil.getInstance().getTranslations(Text.DISCOUNT, "Discount")
            totalLabel.text = CacheUtil.getInstance().getTranslations(Text.TOTAL_LABEL, "Total")
        }
    }

    override fun showOrders(newestToOldest: Boolean, orders: ArrayList<OrderHistory>?) {

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
        dismissDialogs()
        order?.let { item ->
            binding.apply {
                blocker.gone()
                dateAndTime.text = item.createdAt.formatOrderTime()
                orderNumber.text = CacheUtil.getInstance()
                    .getTranslations(Text.ORDER_NUM, "Order #") + " " + item.id
                if (order.orderType.equals("deliver") || order.orderType.equals("delivery")) {
                    address?.let {
                        locationName.text = address.attributes.label
                        locationAddress.text = address.attributes.line1
                    }
                    locationImage.setImageResource(com.herfy.history.R.drawable.ic_store_image)
                } else {
                    store?.let {
                        locationName.text = store.attributes.name
                        locationAddress.text = store.attributes.line1
                    }
                    locationImage.setImageResource(com.herfy.history.R.drawable.ic_location_dot)
                }

                statusDateAndTime.text =
                    item.orderType.orderDetailsType() + item.createdAt.formatOrderTime(true)

                priceSubtotal.text = StringUtil.getNewFormattedPrice(
                    item.subtotal, true
                )

                if (item.deliveryCharge > 0) {
                    deliverContainer.isVisible = true
                    deliverTotal.text = StringUtil.getNewFormattedPrice(
                        item.deliveryCharge, true
                    )
                } else {
                    deliverContainer.isVisible = false
                }

                if (item.discount > 0) {
                    discountContainer.isVisible = true
                    discountTotal.text = StringUtil.getNewFormattedPrice(
                        item.discount, true
                    )
                } else {
                    discountContainer.isVisible = false
                }

                totalPrice.text = StringUtil.getNewFormattedPrice(
                    item.total, true
                )

                reOrderButtonFull.isVisible = item.averageRating > 0
                rateButton.isVisible = item.averageRating <= 0
                reOrderButton.isVisible = item.averageRating <= 0

                if (order.payment != null) {
                    if (order.payment.attributes.method.equals("cash", ignoreCase = true)) {
                        cardImage.setImageResource(com.herfy.commonstyles.R.drawable.ic_payment_type_cash_new)
                        cardNumber.text = CacheUtil.getInstance().getTranslations(Text.CASH, "Cash")
                    } else {
                        if (order.payment.attributes.brand.equals("visa", ignoreCase = true)) {
                            cardImage.setImageResource(com.herfy.commonstyles.R.drawable.ic_card_visa_common)
                        } else {
                            cardImage.setImageResource(com.herfy.commonstyles.R.drawable.ic_card_new)
                        }
                        cardNumber.text =
                            order.payment.attributes.brand + " **** " + order.payment.attributes.last4Digits
                    }
                }
            }
        }
    }

    override fun showOrderStatuses(orderStatuses: ArrayList<OrderStatus>?, isDone: Boolean) {

    }

    override fun showMenuItems(order: OrderDetails?, menuItems: ArrayList<MenuItem>?) {
        menuItems?.let {
            orderMenuItemsAdapter.updateList(menuItems)
        }
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
                findNavController().popBackStack(R.id.new_history_fragment, true)
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
