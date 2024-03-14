package com.herfy.account.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.herfy.base.BaseFragment
import com.herfy.payment.PaymentContract
import com.herfy.payment.PaymentPresenter
import com.herfy.profile.databinding.FragmentUserPaymentMethodsBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.CacheUtil
import com.moneyhash.sdk.android.ResultType
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.PaymentType
import com.skylinedynamics.solosdk.api.models.objects.PrayerTime
import com.skylinedynamics.solosdk.api.models.objects.ServiceCharge
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class PaymentMethodsUserFragment : BaseFragment(), PaymentContract.View {
    private var paymentPresenter: PaymentContract.Presenter? = null

    private lateinit var binding: FragmentUserPaymentMethodsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paymentPresenter = PaymentPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserPaymentMethodsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        paymentPresenter?.start()
        setupViews()
    }

    override fun setPresenter(presenter: PaymentContract.Presenter) {
        paymentPresenter = presenter
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
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.title.text =
            CacheUtil.getInstance().getTranslations(Text.PAYMENT_METHODS, "Payment Methods")
    }

    override fun showPaymentTypes(paymentTypes: MutableSet<PaymentType>?) {

    }

    override fun showPaymentTypes(paymentTypes: ArrayList<PaymentType>?) {

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
        orderType: OrderType?, startLocation: LatLng?, endLocation: LatLng?, bounds: LatLngBounds?
    ) {

    }

    override fun updateTime(calendar: Calendar?) {

    }

    override fun selectPaymentTypeCash() {

    }

    override fun showValidateOrderError(errors: MutableList<String>?) {

    }

    override fun sendOrder() {

    }

    override fun initializeTap(orderId: String?, total: Double) {

    }

    override fun showCheckoutPaymentForm(orderId: String?) {

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
        charges: ArrayList<ServiceCharge>?
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

    override fun moneyHashContract(): ActivityResultLauncher<Pair<String, ResultType>>? {
        return null
    }

    override fun setOrderId(orderId: String?) {

    }

    override fun showCardDialogList(orderId: String?) {

    }

    override fun firebaseECommercePurchaseEvent(
        orderedItem: MutableList<MenuItem>?, total: Double
    ) {

    }

    override fun firebaseAddPaymentInfoEvent() {

    }

    override fun firebaseApplyPromotionEvent(
        promotionId: String?, promotionName: String?, orderedItems: MutableList<MenuItem>?
    ) {

    }

    override fun updateMenuItems() {

    }

    override fun showMoyasarSheet(
        orderId: String?, validatedTotal: Double, menuItems: String?, apiKey: String?
    ) {

    }

    override fun showOrderType(checkout: Boolean?) {

    }

    override fun showSelectedPyament() {

    }


}
