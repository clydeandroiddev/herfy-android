package com.herfy.account.account

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.herfy.account.AccountContract
import com.herfy.account.AccountPresenter
import com.herfy.base.BaseFragment
import com.herfy.commonstyles.R
import com.herfy.profile.databinding.FragmentUserProfileBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.Address
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date

@AndroidEntryPoint
class ProfileFragment : BaseFragment(), AccountContract.View {
    private var accountPresenter: AccountContract.Presenter? = null

    private lateinit var binding: FragmentUserProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountPresenter = AccountPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserProfileBinding.inflate(inflater, container, false)
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
        binding.apply {
            back.setOnClickListener {
                navigateUp()
            }
            editProfile.setOnClickListener {
                findNavController().navigate(
                    ProfileFragmentDirections.actionProfileToEdit()
                )
            }
            deliveryAddressLayout.setOnClickListener {
                findNavController().navigate(
                    ProfileFragmentDirections.actionProfileToUserAddresses()
                )
            }
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.apply {
            title.text =
                CacheUtil.getInstance().getTranslations(Text.MY_PROFILE_LABEL, "My Profile")
            editLabel.text = CacheUtil.getInstance().getTranslations(Text.EDIT)
            deliveryAddressesLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.DELIVERY_ADDRESSES, "Delivery Addresses")
            paymentMethodsLabel.text =
                CacheUtil.getInstance().getTranslations(Text.PAYMENT_METHODS, "Payment Methods")
            curbsideVehiclesLabel.text =
                CacheUtil.getInstance().getTranslations(Text.CURBSIDE_VEHICLES, "Curbside Vehicles")
            logoutLabel.text = CacheUtil.getInstance().getTranslations(Text.LOGOUT, "Logout")
            deleteAccountLabel.text =
                CacheUtil.getInstance().getTranslations(Text.DELETE_ACCOUNT, "Delete Account")
            deleteMyAccountLabel.text =
                CacheUtil.getInstance().getTranslations(Text.DELETE_MY_ACCOUNT, "Delete My Account")
            deleteAccountDescription.text = CacheUtil.getInstance().getTranslations(
                Text.DELETE_ACCOUNT_AND_REMOVE,
                "Delete account and remove all data. Please remember this action can not be reversed."
            )
        }
    }

    @SuppressLint("SetTextI18n")
    override fun showCustomer(
        firstName: String?,
        lastName: String?,
        gender: String?,
        phoneNumber: String?,
        email: String?,
        provider: String?
    ) {
        binding.profileName.text = "$firstName $lastName"
        if (phoneNumber.isNullOrEmpty()) {
            binding.profileNumber.text = email.orEmpty()
        } else {
            binding.profileNumber.text = "+" + phoneNumber.replace("+", "")
        }
        if (firstName.isNullOrEmpty() || lastName.isNullOrEmpty() || gender.isNullOrEmpty() || AuthUtil.getInstance().customer.attributes.birthDate.isNullOrEmpty()) {
            binding.profileCard.setCardBackgroundColor(requireActivity().resources.getColor(R.color.card_selected_bg))
            binding.editLabel.text =
                CacheUtil.getInstance().getTranslations(Text.COMPLETE_PROFILE, "Complete Profile")
            binding.profileName.text = CacheUtil.getInstance()
                .getTranslations(Text.COMPLETE_YOUR_PROFILE, "Complete your profile")
            binding.deleteAccountLabel.isVisible = false
            binding.deleteAccountDescription.isVisible = false
            binding.deleteMyAccountLabel.isVisible = false
            binding.deleteImage.isVisible = false
        } else {
            binding.profileCard.setCardBackgroundColor(requireActivity().resources.getColor(R.color.white))
            binding.deleteAccountLabel.isVisible = true
            binding.deleteAccountDescription.isVisible = true
            binding.deleteMyAccountLabel.isVisible = true
            binding.deleteImage.isVisible = true
            binding.editLabel.text = CacheUtil.getInstance().getTranslations(Text.EDIT, "Edit")
        }
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
