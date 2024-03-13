package com.herfy.account.account

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TextView
import com.herfy.account.AccountContract
import com.herfy.account.AccountPresenter
import com.herfy.base.BaseFragment
import com.herfy.profile.R
import com.herfy.profile.databinding.FragmentUserEditProfileBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.Address
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class EditProfileFragment : BaseFragment(), AccountContract.View {
    private var accountPresenter: AccountContract.Presenter? = null

    private lateinit var binding: FragmentUserEditProfileBinding

    private var fullName: String = ""
    private var genderSelected: String = ""
    private var originalBirthday: String = ""
    private var isSaveName = false
    private var isSaveGender = false
    private var isSaveBirthdate = false

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
                processGender("male")
                checkGenderState()
            }
            femaleSelection.setOnClickListener {
                processGender("female")
                checkGenderState()
            }
            nameEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    checkNameState()
                }
            })
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
                fullName = "$firstName $lastName"
                binding.nameEdit.setText("$firstName $lastName")
                binding.nameEdit.setBackgroundResource(R.drawable.background_common_selection)
                checkNameState()
            }

            gender?.let {
                processGender(it)
                checkGenderState()
            }
        }
    }

    private fun checkGenderState() {
        binding.nameEdit.setBackgroundResource(R.drawable.background_common_default)
        if (binding.nameEdit.text.toString().isNotEmpty()) {
            isSaveName = true
            if (accountPresenter?.originalName.orEmpty() == binding.nameEdit.text.toString()) {
                isSaveName = false
                binding.nameEdit.setBackgroundResource(R.drawable.background_common_selection)
            }
        } else {
            isSaveName = false
        }
    }
    private fun checkNameState() {
        binding.nameEdit.setBackgroundResource(R.drawable.background_common_default)
        if (binding.nameEdit.text.toString().isNotEmpty()) {
            isSaveName = true
            if (accountPresenter?.originalName.orEmpty() == binding.nameEdit.text.toString()) {
                isSaveName = false
                binding.nameEdit.setBackgroundResource(R.drawable.background_common_selection)
            }
        } else {
            isSaveName = false
        }
    }

    private fun processGender(gender: String) {
        if (gender == "male") {
            genderSelected = "male"
            binding.maleSelection.setBackgroundResource(R.drawable.background_common_selection)
            binding.femaleSelection.setBackgroundResource(R.drawable.background_common_default)
        } else {
            genderSelected = "female"
            binding.femaleSelection.setBackgroundResource(R.drawable.background_common_selection)
            binding.maleSelection.setBackgroundResource(R.drawable.background_common_default)
        }
    }

    override fun showDateOfBirth(birthDate: Date?, date: String?) {
        val calendar = Calendar.getInstance()
        if (!date.isNullOrEmpty()) {
            if (birthDate != null) {
                calendar.time = birthDate
            }
            originalBirthday = date
            binding.birthDate.setText(date)
        } else {
            calendar.time = Date()
        }

        val datePickerDialog = DatePickerDialog(
            requireActivity(),
            { datePicker: DatePicker?, year: Int, month: Int, day: Int -> },
            calendar[Calendar.YEAR],
            calendar[Calendar.MONTH],
            calendar[Calendar.DAY_OF_MONTH]
        )
        datePickerDialog.datePicker.maxDate = Date().time
        datePickerDialog.setButton(
            DatePickerDialog.BUTTON_POSITIVE,
            CacheUtil.getInstance().getTranslations(Text.SELECT, "SELECT")
        ) { dialogInterface: DialogInterface, i: Int ->
            val datePicker = datePickerDialog.datePicker
            calendar[Calendar.YEAR] = datePicker.year
            calendar[Calendar.MONTH] = datePicker.month
            calendar[Calendar.DAY_OF_MONTH] = datePicker.dayOfMonth
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            binding.birthDate.setText(sdf.format(date))
            dialogInterface.dismiss()
        }
        datePickerDialog.setButton(
            DatePickerDialog.BUTTON_NEGATIVE,
            CacheUtil.getInstance().getTranslations(Text.CANCEL, "CANCEL")
        ) { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() }
        binding.dateOfBirthLayout.setOnClickListener { view: View? -> datePickerDialog.show() }
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
