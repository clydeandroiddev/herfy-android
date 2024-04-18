package com.herfy.account.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.herfy.account.AccountContract
import com.herfy.account.AccountPresenter
import com.herfy.account.account.adapter.PaymentMethodsAdapter
import com.herfy.base.BaseFragment
import com.herfy.profile.databinding.FragmentUserPaymentMethodsBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DialogUtils
import com.skylinedynamics.solosdk.api.models.objects.Address
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date

@AndroidEntryPoint
class PaymentMethodsUserFragment : BaseFragment(), AccountContract.View,
    PaymentMethodsAdapter.CardListener {
    private var paymentPresenter: AccountContract.Presenter? = null

    private lateinit var binding: FragmentUserPaymentMethodsBinding

    private lateinit var adapter: PaymentMethodsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paymentPresenter = AccountPresenter(this)
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
        showLoadingDialog()
        paymentPresenter?.fetchCustomerCards()
    }

    override fun setPresenter(presenter: AccountContract.Presenter) {
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
            adapter = PaymentMethodsAdapter(ArrayList(), this@PaymentMethodsUserFragment)
            binding.paymentsList.adapter = adapter
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.title.text =
            CacheUtil.getInstance().getTranslations(Text.PAYMENT_METHODS, "Payment Methods")
    }

    override fun showCustomer(
        firstName: String?,
        lastName: String?,
        gender: String?,
        phoneNumber: String?,
        email: String?,
        provider: String?
    ) {

    }

    override fun showDateOfBirth(birthDate: Date?, date: String?) {

    }

    override fun showEmail(show: Boolean) {

    }

    override fun showPassword(show: Boolean) {

    }

    override fun showErrorFirstName(error: String?) {

    }

    override fun showErrorLastName(error: String?) {

    }

    override fun showErrorPhoneNumber(error: String?) {

    }

    override fun showErrorEmail(error: String?) {

    }

    override fun showErrorNewPassword(error: String?) {

    }

    override fun showErrorConfirmPassword(error: String?) {

    }

    override fun showErrorConfirmNewPassword(error: String?) {

    }

    override fun showPasswordUpdated() {

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

    override fun showAccount() {

    }

    override fun showLocations(locations: ArrayList<Address>?) {

    }

    override fun hideLocations() {

    }

    override fun setLocation(location: Address?) {

    }

    override fun showMessage(message: String?) {

    }

    override fun accountDeleted() {

    }

    override fun showCustomerCards() {
        dismissDialogs()
        val customerCards = CacheUtil.getInstance().customerCards
        binding.emptyState.setTitle(CacheUtil.getInstance().getTranslations(Text.NO_PAYMENT_METHOD_SELECTED, "No payment method found"))
        binding.emptyState.setVisible(customerCards.isEmpty())
        adapter.updateList(ArrayList(customerCards))
    }

    override fun showRevelRewardPoints(pointValue: Double?) {

    }

    override fun showCard(isDefault: Boolean) {
    }

    override fun onSelectCard(card: CustomerCard, position: Int) {
        findNavController().navigate(
            PaymentMethodsUserFragmentDirections.actionProfileToCardDetail(card.getId())
        )
    }
}
