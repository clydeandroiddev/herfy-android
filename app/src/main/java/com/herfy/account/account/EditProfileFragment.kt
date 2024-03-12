package com.herfy.account.account

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.herfy.account.AccountContract
import com.herfy.account.AccountPresenter
import com.herfy.base.BaseFragment
import com.herfy.profile.databinding.FragmentUserEditProfileBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.Address
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date

@AndroidEntryPoint
class EditProfileFragment : BaseFragment(), AccountContract.View {
    private var accountPresenter: AccountContract.Presenter? = null

    private lateinit var binding: FragmentUserEditProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountPresenter = AccountPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserEditProfileBinding.inflate(inflater, container, false)
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
            maleSelection.setOnClickListener {

            }
            femaleSelection.setOnClickListener {

            }
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.apply {
            title.text = CacheUtil.getInstance().getTranslations(Text.EDIT_PROFILE, "Edit Profile")

            nameLabel.text = CacheUtil.getInstance().getTranslations(Text.NAME, "Name")
            nameEdit.hint = CacheUtil.getInstance().getTranslations(Text.YOUR_NAME, "Your Name")

            maleLabel.text = CacheUtil.getInstance().getTranslations(Text.MALE, "Male")
            femaleLabel.text = CacheUtil.getInstance().getTranslations(Text.FEMALE, "Female")

            phoneNumberLabel.text =
                CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_NEW, "Phone Number")

            dateOfBirthLabel.hint =
                CacheUtil.getInstance().getTranslations(Text.DATE_OF_BIRTH, "Date of Birth")
            birthDate.hint = CacheUtil.getInstance()
                .getTranslations(Text.SELECT_YOUR_DATE_OF_BIRTH, "Select your date of birth")

            genderLabel.text = CacheUtil.getInstance().getTranslations(Text.GENDER, "Gender")
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
        binding.apply {
            if (!phoneNumber.isNullOrEmpty()) {
                binding.mobileNum.text = "+" + phoneNumber.toString().replace("+", "")
            }

            if (!firstName.isNullOrEmpty()) {
                binding.nameEdit.setText("$firstName $lastName")
            }
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
