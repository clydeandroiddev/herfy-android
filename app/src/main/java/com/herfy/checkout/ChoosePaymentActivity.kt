package com.herfy.checkout

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.checkout.databinding.ActivityChoosePaymentMethodBinding
import com.herfy.payment.PaymentContract
import com.herfy.payment.PaymentPresenter
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.PaymentType
import com.skylinedynamics.solosdk.api.models.objects.PrayerTime
import com.skylinedynamics.solosdk.api.models.objects.ServiceCharge
import java.util.Calendar
import com.herfy.checkout.R as checkoutR

class ChoosePaymentActivity : BaseActivity(), PaymentContract.View {

    private lateinit var checkoutPresenter: PaymentContract.Presenter
    private lateinit var binding: ActivityChoosePaymentMethodBinding

    private var selectedCard: CustomerCard? = null
    private lateinit var creditCardAdapter : CardsAdapter

    private var isPaymentSet = false
    private var isCashPaymentAvailable = false
    private var isCardPaymentAvailable = false
    private var isCODPaymentAvailable = false

    private var selectedPaymentType: PaymentType? =null

    private lateinit var rbCash: ImageView
    private lateinit var rbCard: ImageView
    private lateinit var rbCod: ImageView
    private lateinit var cashPayment: MaterialCardView
    private lateinit var cardPayment: MaterialCardView
    private lateinit var codPayment: MaterialCardView
    private lateinit var cashContainer: ConstraintLayout
    private lateinit var cardContainer: ConstraintLayout
    private lateinit var codContainer: ConstraintLayout
    private lateinit var cardImage: ImageView
    private lateinit var codImage: ImageView
    private lateinit var cashImage: ImageView
    private lateinit var cashLabel: TextView
    private lateinit var cardLabel: TextView
    private lateinit var codLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChoosePaymentMethodBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkoutPresenter  = PaymentPresenter(this)
        checkoutPresenter.getPaymentTypes()
        setupViews()
    }

    override fun onResume() {
        super.onResume()
        checkoutPresenter.start()
    }

    override fun setPresenter(presenter: PaymentContract.Presenter) {
        checkoutPresenter = presenter
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
        binding.confirmButton
            .setText(CacheUtil.getInstance().getTranslations(Text.CONFIRM,"Confirm"))

        binding.toolbarContainer.btnBack.setOnClickListener { onBackPressed() }
        binding
            .confirmButton
            .ninjaTap(lifecycleScope) {
                selectedCard?.let {selectedCards->
                    CacheUtil
                        .getInstance().orderPayment = PaymentType.CARD
                    CacheUtil
                        .getInstance()
                        .setCustomCard(Gson().toJson(selectedCards))
                }
                selectedPaymentType?.let { paymentType->
                    CacheUtil
                        .getInstance()
                        .setCustomCard("")
                    CacheUtil
                        .getInstance().orderPayment = paymentType
                }
                onBackPressed()
            }

    }


    override fun showValidateOrderError(errors: MutableList<String>?) {

    }

    override fun sendOrder() {

    }

    override fun initializeTap(orderId: String?, total: Double) {

    }

    override fun showCheckoutPaymentForm(orderId: String?) {

    }

    override fun showInputCvvDialog(orderId: String?) {
    }

    override fun showCardFailed() {

    }

    override fun showMessage(message: String?) {

    }

    override fun showError(error: String?) {

    }

    override fun showCompleted(orderId: String?, orderType: OrderType?) {

    }

    override fun setSchedule() {

    }

    override fun setTax(inclusive: Boolean, taxAmount: CharSequence?) {

    }

    override fun setPriceDetails(
        subtotalPrice: Double,
        discount: Double,
        totalPrice: Double,
        deliveryCharge: Double,
        pointsSarValue: Double,
        pointsEarnable: Int,
        pointsUsed: Int,
        charges: java.util.ArrayList<ServiceCharge>?
    ) {

    }

    override fun setMenuItem(menuItem: MenuItem?) {

    }

    override fun logFirebaseEvent(event: String?, orderTotalPrice: String?) {

    }

    override fun showPrayerTime(prayerTime: PrayerTime?) {

    }

    override fun showErrorPrayerTime() {

    }

    override fun setOrderId(orderId: String?) {

    }

    override fun showCardDialogList(orderId: String?) {

    }

    override fun firebaseECommercePurchaseEvent(
        orderedItem: MutableList<MenuItem>?,
        total: Double
    ) {

    }

    override fun firebaseAddPaymentInfoEvent() {

    }

    override fun firebaseApplyPromotionEvent(
        promotionId: String?,
        promotionName: String?,
        orderedItems: MutableList<MenuItem>?
    ) {

    }

    override fun updateMenuItems() {

    }

    override fun showMoyasarSheet(
        orderId: String?,
        validatedTotal: Double,
        menuItems: String?,
        apiKey: String?
    ) {

    }

    override fun showOrderType(checkout: Boolean?) {

    }

    override fun showSelectedPayment() {
    }

    override fun insufficientLoyaltyPoints() {
        TODO("Not yet implemented")
    }

    override fun setStartName(startName: String?) {

    }

    override fun setStartAddress(startAddress: String?) {

    }

    override fun setEndName(endName: String?) {

    }

    override fun setEndAddress(endAddress: String?) {

    }

    override fun updateMap(
        orderType: OrderType?,
        startLocation: LatLng?,
        endLocation: LatLng?,
        bounds: LatLngBounds?
    ) {

    }

    override fun updateTime(calendar: Calendar?) {

    }

    override fun selectPaymentTypeCash() {
        selectedPaymentType = PaymentType.CASH
        if (this::creditCardAdapter.isInitialized) creditCardAdapter.setSelectedPosition(-1)
        selectedView(rbCash, cashImage, cashContainer, cashPayment)
        if (isCardPaymentAvailable) unselectedView(rbCard, cardImage, cardContainer, cardPayment)
        if (isCODPaymentAvailable) unselectedView(rbCod, codImage, codContainer, codPayment)
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.toolbarContainer.title.text = CacheUtil.getInstance().getTranslations(Text.PAYMENT_METHODS, "Choose Payment Method")
        binding.chooseMethodLabel.text = CacheUtil.getInstance().getTranslations(Text.DEBIT_CREDIT_CARD, "Debit/Credit Card")
    }

    override fun showPaymentTypes(paymentTypes: MutableSet<PaymentType>) {

    }

    override fun showPaymentTypes(paymentTypes: ArrayList<PaymentType>) {
        val type = CacheUtil.getInstance().orderType
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        paymentTypes.forEach { paymentType ->
            if (paymentType == PaymentType.CASH) {
                isCashPaymentAvailable = true
                val view: View = inflater.inflate(com.herfy.checkout.R.layout.item_payment_cash, null)
                cashPayment = view.findViewById(com.herfy.checkout.R.id.cash_layout)
                cashContainer = view.findViewById(com.herfy.checkout.R.id.container_cash)
                rbCash = view.findViewById(com.herfy.checkout.R.id.rb_cash)
                cashLabel = view.findViewById(R.id.cash_label)
                cashImage = view.findViewById(R.id.cash_image)
                cashLabel.text = CacheUtil.getInstance().getTranslations(Text.CASH_ON_PICKUP, "Cash")
                cashContainer.setOnClickListener { selectPaymentTypeCash() }
                binding.paymentTypeContainer.addView(view)
            } else if (paymentType == PaymentType.CARD) {
                isCardPaymentAvailable = true
                setupSavedCards()
                val view: View = inflater.inflate(com.herfy.checkout.R.layout.item_card_payment, null)
                rbCard = view.findViewById(com.herfy.checkout.R.id.rb_card)
                cardLabel = view.findViewById(com.herfy.checkout.R.id.card_label)
                cardContainer = view.findViewById(com.herfy.checkout.R.id.container_card)
                cardImage = view.findViewById(com.herfy.checkout.R.id.card_image)
                cardPayment = view.findViewById(com.herfy.checkout.R.id.cards_layout)
                cardLabel.text = CacheUtil.getInstance().getTranslations(Text.NEW_DEBIT_CREDIT_CARD, "New Debit/Credit Card")
                binding.paymentTypeContainer.addView(view)
                cardContainer.setOnClickListener {
                    selectedPaymentType = PaymentType.CARD
                    if (isCashPaymentAvailable) unselectedView(rbCash, cashImage, cashContainer, cashPayment)
                    if (isCODPaymentAvailable) unselectedView(rbCod, codImage, codContainer, codPayment)
                    if (this::creditCardAdapter.isInitialized)  creditCardAdapter.setSelectedPosition(-1)
                    selectedView(rbCard, cardImage, cardContainer, cardPayment)
                    checkoutPresenter.cashPresented = 0.0
                }
            } else if (paymentType == PaymentType.CARD_ON_DELIVERY || paymentType == PaymentType.CARD_ON_PICKUP) {
                isCODPaymentAvailable = true
                val view: View = inflater.inflate(com.herfy.checkout.R.layout.item_payment_cod_cop, null)
                codPayment = view.findViewById(com.herfy.checkout.R.id.cod_layout)
                codContainer = view.findViewById(com.herfy.checkout.R.id.container_cod)
                codLabel = view.findViewById(com.herfy.checkout.R.id.cod_label)
                rbCod = view.findViewById(com.herfy.checkout.R.id.rb_cod)
                codImage = view.findViewById(com.herfy.checkout.R.id.cod_image)
                binding.cashOnDeliveryContainer.addView(view)
                codLabel.text = if (CacheUtil.getInstance().orderType === OrderType.DELIVERY) {
                    CacheUtil.getInstance().getTranslations(Text.CARD_ON_DELIVERY, "Card on delivery")
                } else CacheUtil.getInstance().getTranslations(Text.CARD_ON_PICKUP, "Card on Pickup")

                codContainer.setOnClickListener {
                    selectedPaymentType = if (CacheUtil.getInstance().orderType === OrderType.DELIVERY) {
                        PaymentType.CARD_ON_DELIVERY
                    } else {
                        PaymentType.CARD_ON_PICKUP
                    }
                    selectedView(rbCod, codImage, codContainer, codPayment)

                    if (isCashPaymentAvailable) unselectedView(rbCash, cashImage, cashContainer, cashPayment)
                    if (isCardPaymentAvailable) unselectedView(rbCard, cardImage, cardContainer, cardPayment)
                    if (this::creditCardAdapter.isInitialized)  creditCardAdapter.setSelectedPosition(-1)
                    checkoutPresenter.cashPresented = 0.0

                   /* if (paymentPresenter.hasMukafaaTransactionId()) {
                        mukafaaPayment.setStrokeColor(Color.parseColor(CacheUtil.getInstance().colorMain))
                    } else {
                        mukafaaPayment.setStrokeColor(Color.TRANSPARENT)
                        mukafaaNumbers.setVisibility(View.GONE)
                        mukafaaSelected = false
                        requestOTP.setVisibility(View.GONE)
                        placeOrder.setVisibility(View.VISIBLE)
                    }*/

                }

            }
        }

        if(paymentTypes.isEmpty() || (!isCashPaymentAvailable && !isCardPaymentAvailable && !isCODPaymentAvailable)) {
            binding.emptyState.setVisible(true)
            binding.border.setVisible(true)
            binding.paymentContainer.setVisible(false)
            binding.emptyState.setTitle(
                CacheUtil.getInstance()
                    .getTranslations(Text.NO_PAYMENT_METHOD_SELECTED, "No payment method found.")
            )
            return
        }
    }

    private fun unselectedView(radioButton: ImageView, imageView: ImageView, constraintLayout: ConstraintLayout, cardView: MaterialCardView) {
        radioButton.setImageResource(checkoutR.drawable.ic_payment_radio_unselected)
        cardView.strokeColor = ContextCompat.getColor(this, com.herfy.commonstyles.R.color.card_stroke_color)
        constraintLayout.setBackgroundColor(Color.WHITE)
        imageView.background = ContextCompat.getDrawable(this, com.herfy.checkout.R.drawable.unselected_image_bg)
    }
    private fun selectedView(radioButton: ImageView, imageView: ImageView, constraintLayout: ConstraintLayout, cardView: MaterialCardView) {
        radioButton.setImageResource(checkoutR.drawable.ic_payment_radio_selected)
        cardView.strokeColor = ContextCompat.getColor(this, com.herfy.commonstyles.R.color.border_color_light)
        constraintLayout.setBackgroundColor(ContextCompat.getColor(this, com.herfy.commonstyles.R.color.card_selected_bg))
        imageView.background = ContextCompat.getDrawable(this, com.herfy.checkout.R.drawable.selected_image_bg)
    }

    private fun setupSavedCards() {
        val cards = CacheUtil.getInstance().customerCards
        binding.chooseMethodLabel.setVisible(cards.size >= 1)
        binding.notesDivider.setVisible(true)
        creditCardAdapter =  CardsAdapter(cards) { card, position ->
            selectedCard = card
            if (isCashPaymentAvailable) unselectedView(rbCash, cashImage, cashContainer, cashPayment)
            if (isCardPaymentAvailable) unselectedView(rbCard, cardImage, cardContainer, cardPayment)
            if (isCODPaymentAvailable) unselectedView(rbCod, codImage, codContainer, codPayment)
        }

        binding.rvCards.adapter = creditCardAdapter
        creditCardAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        selectedPaymentType = null
    }

}