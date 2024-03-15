package com.herfy.account.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.herfy.account.AccountContract
import com.herfy.account.AccountPresenter
import com.herfy.base.BaseFragment
import com.herfy.profile.databinding.FragmentUserCardDetailBinding
import com.herfy.util.AppLogger
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DialogUtils
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall
import com.skylinedynamics.solosdk.api.models.objects.Address
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Date

@AndroidEntryPoint
class CardDetailsFragment : BaseFragment(), AccountContract.View {
    private var paymentPresenter: AccountContract.Presenter? = null

    private lateinit var binding: FragmentUserCardDetailBinding

    private var card: CustomerCard? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paymentPresenter = AccountPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserCardDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        paymentPresenter?.start()
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
            deletePaymentMethod.setOnClickListener {
                card?.let {
                    showLoadingDialog()
                    CustomersApiCall.getInstance().deleteCard(AuthUtil.getInstance().customer.id,
                        it.getId() + "",
                        object : ApiRequestListener {
                            override fun onSuccess(o: Any) {
                                dismissDialogs()
                                navigateUp()
                            }

                            override fun onError(o: Any) {
                                dismissDialogs()
                                AppLogger.printRetrofitError(o)
                                if (o is JSONObject) {
                                    try {
                                        val error = o["error"]
                                        var detail = CacheUtil.getInstance()
                                            .getTranslations(Text.SORRY_ERROR_PAYMENT)
                                        if (error is JSONObject) {
                                            detail = error.getString("detail")
                                        } else if (error is JSONArray) {
                                            val errorObject = error.getJSONObject(0)
                                            detail = errorObject.getString("detail")
                                        }
                                        showError(detail)
                                    } catch (e: JSONException) {
                                        e.printStackTrace()
                                        showError(
                                            CacheUtil.getInstance()
                                                .getTranslations(Text.SORRY_ERROR_PAYMENT)
                                        )
                                    }
                                } else {
                                    showError(
                                        CacheUtil.getInstance()
                                            .getTranslations(Text.SORRY_ERROR_PAYMENT)
                                    )
                                }
                            }
                        })
                }
            }
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.title.text =
            CacheUtil.getInstance().getTranslations(Text.CARD_DETAILS, "Card Details")
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

    }

    override fun showRevelRewardPoints(pointValue: Double?) {

    }
}
