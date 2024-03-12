package com.herfy.account.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.herfy.account.AccountContract
import com.herfy.account.AccountPresenter
import com.herfy.base.BaseFragment
import com.herfy.profile.databinding.FragmentUserAddressesBinding
import com.herfy.profile.databinding.FragmentUserEditProfileBinding
import com.skylinedynamics.solosdk.api.models.objects.Address
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date

@AndroidEntryPoint
class AddressUserFragment : BaseFragment(), AccountContract.View {
    private var accountPresenter: AccountContract.Presenter? = null

    private lateinit var binding: FragmentUserAddressesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountPresenter = AccountPresenter(this)
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

    override fun setPresenter(presenter: AccountContract.Presenter) {
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

    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {

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
