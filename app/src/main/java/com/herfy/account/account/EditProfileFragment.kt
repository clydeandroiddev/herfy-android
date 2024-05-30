package com.herfy.account.account

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.Toast
import com.herfy.account.AccountContract
import com.herfy.account.AccountPresenter
import com.herfy.base.BaseFragment
import com.herfy.ext.cast
import com.herfy.main.MainActivity
import com.herfy.profile.R
import com.herfy.profile.databinding.FragmentUserEditProfileBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DialogUtils
import com.herfy.utils.helper.StringUtil
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
    private var birthdaySelected: String = ""
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

    private fun closeAndClear() {
        binding.nameEdit.apply {
            closeKeyboard(this)
            clearFocus()
        }
    }

    override fun setupViews() {
        binding.apply {
            back.setOnClickListener {
                navigateUp()
            }
            maleSelection.setOnClickListener {
                closeAndClear()
                processGender("Male")
                checkGenderState()
            }
            femaleSelection.setOnClickListener {
                closeAndClear()
                processGender("Female")
                checkGenderState()
            }
            binding.continueButton.setOnClickListener {
                if (it.alpha == 1F) {
                    saveUser()
                }
            }
            binding.numberLayout.setOnClickListener {
                canNotBeEdited.visible()
                closeKeyboard(binding.nameEdit)
                Handler(Looper.getMainLooper()).postDelayed({
                    canNotBeEdited.gone()
                }, 1200)
            }
            nameEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    fullName = nameEdit.text.toString()
                    checkNameState()
                }
            })
        }
    }

    private fun saveUser() {
        val fName: Array<String> =
            fullName.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (!StringUtil.isValidName(fullName)) {
            val error = CacheUtil.getInstance().getTranslations(
                Text.NAME_VALIDATION, "Name fields should not accept numbers and special character."
            )
            showError(error)
            return
        } else if (fullName.isEmpty()) {
            val error = CacheUtil.getInstance()
                .getTranslations(Text.FIRST_NAME_EMPTY, "First name cannot be empty")
            showError(error)
        } else {
            showLoadingDialog()
            val firstName = fName[0]
            var lastName = ""
            if (fName.size > 1) {
                lastName = fName.drop(1).joinToString(" ")
            }
            accountPresenter?.save(
                firstName,
                lastName,
                birthdaySelected,
                genderSelected.toLowerCase(),
                binding.mobileNum.text.toString(),
                AuthUtil.getInstance().customer.attributes.email,
                "",
                ""
            )
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

            dateOfBirthLabel.text =
                CacheUtil.getInstance().getTranslations(Text.DATE_OF_BIRTH, "Date of Birth")

            birthDate.hint = CacheUtil.getInstance()
                .getTranslations(Text.SELECT_YOUR_DATE_OF_BIRTH, "Select your date of birth")

            genderLabel.text = CacheUtil.getInstance().getTranslations(Text.GENDER, "Gender")

            continueButton.text =
                CacheUtil.getInstance().getTranslations(Text.SAVE_CHANGES, "Save Changes")

            canNotBeLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.CAN_NOT_BE_EDITED, "Can not be edited.")
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
                if (lastName.isNullOrEmpty()) {
                    fullName = "$firstName"
                }
                binding.nameEdit.setText(fullName)
                binding.nameEdit.setBackgroundResource(R.drawable.background_common_selection)
                checkNameState()
            }

            gender?.let {
                processGender(it)
                checkGenderState()
            }
        }
    }

    private fun finalState() {
        if (fullName.isNotEmpty() && genderSelected.isNotEmpty() && birthdaySelected.isNotEmpty()) {
            if (!isSaveGender && !isSaveBirthdate && !isSaveName) {
                binding.continueButton.alpha = 0.5f
            } else {
                binding.continueButton.alpha = 1f
            }
        } else {
            binding.continueButton.alpha = 0.5f
        }
    }

    private fun checkGenderState() {
        isSaveGender = true
        if (genderSelected.equals(accountPresenter?.originalGender, ignoreCase = true)) {
            isSaveGender = false
        }
        finalState()
    }

    private fun checkBirthdayState() {
        isSaveBirthdate = true
        if (birthdaySelected == accountPresenter?.originalBirthday) {
            isSaveBirthdate = false
        }
        finalState()
    }

    private fun checkNameState() {
        binding.nameEdit.setBackgroundResource(R.drawable.background_edit_text)
        if (binding.nameEdit.text.toString().isNotEmpty()) {
            isSaveName = true
            if (accountPresenter?.originalName.orEmpty() == binding.nameEdit.text.toString()) {
                isSaveName = false
                binding.nameEdit.setBackgroundResource(R.drawable.background_edit_filled)
            }
        } else {
            isSaveName = false
        }
        finalState()
    }

    private fun processGender(gender: String) {
        if (gender.equals("Male", ignoreCase = true)) {
            genderSelected = "Male"
            binding.maleSelection.setBackgroundResource(R.drawable.background_common_selection)
            binding.femaleSelection.setBackgroundResource(R.drawable.background_common_default)
            binding.selectionMale.setImageResource(R.drawable.ic_store_selected)
            binding.selectionFemale.setImageResource(R.drawable.ic_store_unselected)
        } else if (gender.equals("Female", ignoreCase = true)) {
            genderSelected = "Female"
            binding.selectionMale.setImageResource(R.drawable.ic_store_unselected)
            binding.selectionFemale.setImageResource(R.drawable.ic_store_selected)
            binding.femaleSelection.setBackgroundResource(R.drawable.background_common_selection)
            binding.maleSelection.setBackgroundResource(R.drawable.background_common_default)
        }
    }

    @SuppressLint("DiscouragedApi")
    override fun showDateOfBirth(birthDate: Date?, date: String?) {
        val calendar = Calendar.getInstance()
        if (!date.isNullOrEmpty()) {
            if (birthDate != null) {
                calendar.time = birthDate
            }
            birthdaySelected = date
            binding.birthDate.setText(date)
            checkBirthdayState()
        } else {
            calendar.time = Date()
            calendar.add(Calendar.YEAR, -10)
        }

        activity?.cast<MainActivity> {
            val datePickerDialog = DatePickerDialog(
                it,
                com.herfy.commonstyles.R.style.CustomDatePickerDialog,
                { datePicker: DatePicker?, year: Int, month: Int, day: Int -> },
                calendar[Calendar.YEAR],
                calendar[Calendar.MONTH],
                calendar[Calendar.DAY_OF_MONTH]
            )
            val maxDateCalendar = Calendar.getInstance()
            maxDateCalendar.add(Calendar.YEAR, -10)
            val maxDate = maxDateCalendar.timeInMillis

            datePickerDialog.setOnShowListener {
                val positiveButton = datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
                positiveButton.setTextColor(android.graphics.Color.parseColor("#0064AC"))
                val negativeButton = datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)
                negativeButton.setTextColor(android.graphics.Color.parseColor("#0064AC"))
            }

            datePickerDialog.datePicker.maxDate = maxDate
            datePickerDialog.setButton(
                DatePickerDialog.BUTTON_POSITIVE,
                CacheUtil.getInstance().getTranslations(Text.SELECT, "SELECT")
            ) { dialogInterface: DialogInterface, i: Int ->
                val datePicker = datePickerDialog.datePicker
                calendar[Calendar.YEAR] = datePicker.year
                calendar[Calendar.MONTH] = datePicker.month
                calendar[Calendar.DAY_OF_MONTH] = datePicker.dayOfMonth
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                binding.birthDate.setText(sdf.format(calendar.time))
                birthdaySelected = sdf.format(calendar.time)
                checkBirthdayState()
                dialogInterface.dismiss()
            }
            datePickerDialog.setButton(
                DatePickerDialog.BUTTON_NEGATIVE,
                CacheUtil.getInstance().getTranslations(Text.CANCEL, "CANCEL")
            ) { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() }

            binding.dateOfBirthView.setOnClickListener { view: View? ->
                closeAndClear()
                datePickerDialog.show()
            }
        }
    }

    override fun showEmail(show: Boolean) {

    }

    override fun showPassword(show: Boolean) {

    }

    override fun showErrorFirstName(error: String?) {
        showError(error)
    }

    override fun showErrorLastName(error: String?) {
        showError(error)
    }

    override fun showErrorPhoneNumber(error: String?) {
        showError(error)
    }

    override fun showErrorEmail(error: String?) {
        showError(error)
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
        try {
            dismissDialogs()
            Toast.makeText(
                requireActivity(),
                CacheUtil.getInstance().getTranslations(Text.PROFILE_UPDATED, "Profile Updated"),
                Toast.LENGTH_SHORT
            ).show()
            navigateUp()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
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

    override fun showCard(isDefault: Boolean) {
    }
}
