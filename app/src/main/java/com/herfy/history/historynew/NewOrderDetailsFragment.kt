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
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.herfy.R
import com.herfy.analytics.firebase.FirebaseService
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.common.deviceHeight
import com.herfy.common.formatOrderTime
import com.herfy.common.getDisplayId
import com.herfy.common.orderDetailsType
import com.herfy.common.orderTypeString
import com.herfy.ext.cast
import com.herfy.history.OrdersContract
import com.herfy.history.OrdersPresenter
import com.herfy.history.databinding.FragmentNewOrderDetailsBinding
import com.herfy.history.historynew.adapter.OrderMenuItemsAdapter
import com.herfy.history.historynew.adapter.RatingOrderAdapter
import com.herfy.main.MainActivity
import com.herfy.util.AppLogger
import com.herfy.util.StringUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CurrencyUtils
import com.herfy.utils.helper.DialogUtils
import com.skylinedynamics.solosdk.api.models.kotlin.DeliveryDriver
import com.skylinedynamics.solosdk.api.models.objects.Address
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus
import com.skylinedynamics.solosdk.api.models.objects.PaymentType
import com.skylinedynamics.solosdk.api.models.objects.Rating
import com.skylinedynamics.solosdk.api.models.objects.Store
import dagger.hilt.android.AndroidEntryPoint
import io.shipbook.shipbooksdk.Log

@AndroidEntryPoint
class NewOrderDetailsFragment : BaseFragment(), OrdersContract.View {

    private var orderPresenter: OrdersContract.Presenter? = null

    private val args: NewOrderDetailsFragmentArgs by navArgs()

    private lateinit var binding: FragmentNewOrderDetailsBinding

    private lateinit var ratingOrderAdapter: RatingOrderAdapter

    private var orderDetails: OrderDetails? = null

    private var orderAddress: Address? = null

    private var orderStore: Store? = null

    private var fromRate = false

    private var isRateButtonClicked = false

    private lateinit var ratingBarDialog: RatingBarDialog

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
                orderDetails?.let {
                    activity?.cast<BaseActivity> {
                        it.showLoadingDialog()
                    }
                    isRateButtonClicked = true
                    orderPresenter?.getOrderFeedbackTopics(it.id)
                }
            }

            orderMenuItemsAdapter = OrderMenuItemsAdapter(ArrayList())
            itemsList.adapter = orderMenuItemsAdapter
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.apply {
            youRatedThisOrderLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.YOU_RATED_THIS_ORDER, "Your rated this order.")
            title.text =
                CacheUtil.getInstance().getTranslations(Text.ORDER_DETAILS_LABEL, "Order Details")
            orderItemsLabel.text =
                CacheUtil.getInstance().getTranslations(Text.ORDER_ITEMS, "Order Items")
            paymentSummaryLabel.text =
                CacheUtil.getInstance().getTranslations(Text.PAYMENT_SUMMARY, "Payment Summary")
            paymentMethodLabel.text =
                CacheUtil.getInstance().getTranslations(Text.PAYMENT_METHOD, "Payment Method")
            reOrderButton.text =
                CacheUtil.getInstance().getTranslations(Text.REORDER_LABEL, "Reorder")
            reOrderButtonFull.text =
                CacheUtil.getInstance().getTranslations(Text.REORDER_LABEL, "Reorder")
            rateButton.text = CacheUtil.getInstance().getTranslations(Text.RATE, "Rate")
            subtotalLabel.text = CacheUtil.getInstance().getTranslations(Text.SUBTOTAL, "Subtotal")
            deliverLabel.text =
                CacheUtil.getInstance().getTranslations(Text.DELIVERY_CHARGE, "Delivery Charges")
            discountLabel.text = CacheUtil.getInstance().getTranslations(Text.DISCOUNT, "Discount")
            totalLabel.text = CacheUtil.getInstance().getTranslations(Text.TOTAL, "Total")
            mukafaaLabel.text =
                CacheUtil.getInstance().getTranslations(Text.MOKAFAA_POINTS, "Mokafa’a Points")
        }
    }

    override fun showLocation(isPlace: Boolean, latLng: LatLng?, address: String?) {

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

    @SuppressLint("SetTextI18n")
    override fun showOrder(order: OrderDetails?, store: Store?, address: Address?) {
        dismissDialogs()
        order?.let { item ->
            binding.apply {
                orderDetails = item
                orderAddress = address
                orderStore = store
                blocker.gone()
                dateAndTime.text = item.createdAt.formatOrderTime()
                orderNumber.text = CacheUtil.getInstance()
                    .getTranslations(Text.ORDER_NUM, "Order #") + " " + getDisplayId(item)
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
                    item.orderType.orderDetailsType(item.orderStatus.sequence) + item.createdAt.formatOrderTime(
                        true
                    )

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

                var mukafaaAmount = 0.0
                orderPresenter?.let {
                    val mukafaaTransaction = it.mukafaaTransaction
                    if (mukafaaTransaction != null && it.isMukafaaEnabled) {
                        mukafaaContainer.visible()
                        mukafaaAmount = mukafaaTransaction.attributes.amount
                        var dc = "-" + StringUtil.getFormattedPrice(mukafaaAmount)
                        dc = dc.replace("--", "-")
                        mukafaaTotal.text = dc
                    } else {
                        mukafaaContainer.gone()
                    }
                }

                val discountAmount = order.discount - mukafaaAmount

                if (discountAmount > 0.0) {
                    discountContainer.isVisible = true
                    discountTotal.text = "-" + StringUtil.getNewFormattedPrice(
                        discountAmount,
                        true
                    )
                } else {
                    discountContainer.isVisible = false
                }

                if (item.loyaltyActualPointsRedeemed > 0) {
                    orderPresenter?.let {
                        val loyaltyTransaction = it.capillaryTransaction //it.soloLoyaltyTransaction
                        if (loyaltyTransaction != null || item.loyaltyPointsRedeemed > 0) {
                            loyaltyContainer.visible()

                            AppLogger.longError("AAAAAAAAAAAAAA ${Gson().toJson(loyaltyTransaction)}")

                            if (loyaltyTransaction != null) {
                                tvRedeemPointsEquiv.text = CurrencyUtils.updateCurrency(
                                    loyaltyTransaction.attributes.amount, true, false, true
                                )
                                tvRedeemPointsLabel.text = CacheUtil.getInstance()
                                    .getTranslations(Text.LOYALTY_POINTS, "Loyalty points (x)")
                                    .replace("x", loyaltyTransaction.attributes.points.toString())
                            } else {
                                tvRedeemPointsEquiv.text = CurrencyUtils.updateCurrency(
                                    item.loyaltyPointsRedeemed, true, false, true
                                )
                                tvRedeemPointsLabel.text = CacheUtil.getInstance()
                                    .getTranslations(Text.LOYALTY_POINTS, "Loyalty points (x)")
                                    .replace("x", item.loyaltyActualPointsRedeemed.toString())
                            }
                        } else {
                            loyaltyContainer.gone()
                        }
                    }
                } else {
                    loyaltyContainer.gone()
                }

                totalPrice.text = StringUtil.getNewFormattedPrice(
                    item.total, true
                )

                reOrderButtonFull.isVisible = item.averageRating > 0
                rateButton.isVisible = item.averageRating <= 0
                reOrderButton.isVisible = item.averageRating <= 0

                if (order.payment != null) {
                    if (order.payment.attributes.method.equals(
                            PaymentType.CASH.value, ignoreCase = true
                        )
                    ) {
                        cardImage.setImageResource(com.herfy.commonstyles.R.drawable.ic_payment_type_cash_new)
                        cardNumber.text =
                            CacheUtil.getInstance().getTranslations(Text.CASH_ON_PICKUP, "Cash")
                    } else if (order.payment.attributes.method.equals(
                            PaymentType.CARD_ON_DELIVERY.value, ignoreCase = true
                        )
                    ) {
                        cardImage.setImageResource(com.herfy.commonstyles.R.drawable.ic_card_new)
                        cardNumber.text = CacheUtil.getInstance()
                            .getTranslations(Text.CARD_ON_DELIVERY, "Card on Delivery")
                    } else if (order.payment.attributes.method.equals(
                            PaymentType.CARD_ON_PICKUP.value, ignoreCase = true
                        )
                    ) {
                        cardImage.setImageResource(com.herfy.commonstyles.R.drawable.ic_card_new)
                        cardNumber.text = CacheUtil.getInstance()
                            .getTranslations(Text.CARD_ON_PICKUP, "Card on Pickup")
                    } else if (order.payment.attributes.method.equals(
                            PaymentType.APPLE_PAY.value, ignoreCase = true
                        )
                    ) {
                        cardImage.setImageResource(com.herfy.commonstyles.R.drawable.ic_card_new)
                        cardNumber.text =
                            CacheUtil.getInstance().getTranslations(Text.APPLE_PAY, "Apple Pay")
                    } else {
                        if (order.payment.attributes.brand != null && order.payment.attributes.last4Digits != null) {
                            if (order.payment.attributes.brand.equals("visa", ignoreCase = true)) {
                                cardImage.setImageResource(com.herfy.commonstyles.R.drawable.ic_card_visa_common)
                            } else {
                                cardImage.setImageResource(com.herfy.commonstyles.R.drawable.ic_card_new)
                            }
                            cardNumber.text =
                                order.payment.attributes.brand + " **** " + order.payment.attributes.last4Digits
                        } else {
                            cardImage.setImageResource(com.herfy.commonstyles.R.drawable.ic_card_new)
                            cardNumber.text =
                                CacheUtil.getInstance().getTranslations(Text.CARD, "Card")
                        }
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
                it.showCartFromHerfyHome()
                findNavController().popBackStack(R.id.new_history_fragment, true)
            }
        }
    }

    override fun updateStatus(orderDetails: OrderDetails?, orderStatus: OrderStatus?) {

    }

    override fun showDriver(customerLatLng: LatLng?, driverLatLng: LatLng?, eta: Int?) {

    }

    override fun updateDriver(appDriver: DeliveryDriver) {

    }

    override fun cancel() {

    }

    override fun showRatings(orderStatus: OrderStatus?, ratings: MutableList<Rating>?) {
        binding.apply {
            orderDetails?.let { order ->
                ratings?.let {
                    if (order.averageRating > 0) {
                        ratingOrderAdapter = RatingOrderAdapter(it.toList())
                        ratingsList.adapter = ratingOrderAdapter
                        ratingContainer.isVisible = true
                        reOrderButtonFull.isVisible = true
                        rateButton.isVisible = false
                        reOrderButton.isVisible = false
                    } else {
                        if (isRateButtonClicked) {
                            showRatingDialog(ratings)
                        }
                        isRateButtonClicked = false
                    }
                }
            }
        }
    }

    private fun getLocationName(): String {
        val name = ""
        orderDetails?.let { order ->
            if (order.orderType.equals("deliver") || order.orderType.equals("delivery")) {
                orderAddress?.let {
                    return it.attributes.line1
                }
            } else {
                orderStore?.let {
                    return it.attributes.line1
                }
            }
        }
        return name
    }

    private fun checkRatings(ratings: List<Rating>): Boolean {
        return ratings.any { it.attributes.rating != 0 }
    }

    private fun showRatingDialog(ratings: MutableList<Rating>?) {
        dismissRatingBarDialog()
        activity?.let {
            orderDetails?.let { order ->
                ratingBarDialog = RatingBarDialog.newInstance(order.createdAt,
                    order.orderType.orderTypeString(),
                    getLocationName(),
                    deviceHeight(it),
                    ratings!!.toList(),
                    object : RatingBarDialog.OnRatingListener {
                        override fun orderRatings(ratings: List<Rating>, feedbackOther: String) {
                            if (checkRatings(ratings)) {
                                fromRate = true
                                activity?.cast<BaseActivity> {
                                    it.showLoadingDialog()
                                }
                                orderPresenter?.sendOrderFeedback(ratings, order.id, feedbackOther)
                            }
                        }

                        override fun exit() {
                            dismissRatingBarDialog()
                        }

                    })
                ratingBarDialog.show(
                    it.supportFragmentManager, RatingBarDialog::class.java.simpleName
                )
            }
        }
    }

    private fun dismissRatingBarDialog() {
        try {
            ratingBarDialog.dismiss()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun showMessage(message: String?) {
        dismissDialogs()
        if (fromRate) {
            orderDetails?.let {
                binding.reOrderButtonFull.isVisible = true
                binding.rateButton.isVisible = false
                binding.reOrderButton.isVisible = false
                fromRate = false
                dismissRatingBarDialog()
                it.averageRating = 1.0
                orderPresenter?.getOrderFeedbackTopics(it.id)
            }
        }
        activity?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
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
