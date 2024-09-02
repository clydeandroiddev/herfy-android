package com.herfy.account.account

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.herfy.account.AccountContract
import com.herfy.account.AccountPresenter
import com.herfy.base.BaseFragment
import com.herfy.commonstyles.R
import com.herfy.curbside.curbsidenew.CurbsideNewActivity
import com.herfy.profile.databinding.FragmentUserProfileBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.BuildUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DialogUtils.showAlertDialog
import com.skylinedynamics.solosdk.api.SoloAuth
import com.skylinedynamics.solosdk.api.models.objects.Address
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date

@AndroidEntryPoint
class ProfileFragment : BaseFragment(), AccountContract.View, DeleteAccountDialogFragment.OnAction,
    LogoutAccountDialogFragment.OnLogoutAction {
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

            paymentMethodsLayout.setOnClickListener {
                findNavController().navigate(
                    ProfileFragmentDirections.actionProfileToUserPayments()
                )
            }

            curbsideLayout.setOnClickListener {
                showCurbside()
            }

            deleteAccountLayout.setOnClickListener {
                DeleteAccountDialogFragment.newInstance(onAction = this@ProfileFragment).show(
                    parentFragmentManager, DeleteAccountDialogFragment::class.java.toString()
                )
            }

            logoutLayout.setOnClickListener {
                LogoutAccountDialogFragment.newInstance(onAction = this@ProfileFragment).show(
                    parentFragmentManager, LogoutAccountDialogFragment::class.java.toString()
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
        var name = "$firstName $lastName"
        if (lastName.isNullOrEmpty()) {
            name = "$firstName"
        }
        binding.profileName.text = name
        if (phoneNumber.isNullOrEmpty()) {
            binding.profileNumber.text = email.orEmpty()
        } else {
            binding.profileNumber.text = "+" + phoneNumber.replace("+", "")
        }
        if (firstName.isNullOrEmpty() || gender.isNullOrEmpty() || AuthUtil.getInstance().customer.attributes.birthDate.isNullOrEmpty()) {
            binding.profileCard.setCardBackgroundColor(requireActivity().resources.getColor(R.color.card_selected_bg))
            binding.editLabel.text =
                CacheUtil.getInstance().getTranslations(Text.COMPLETE_PROFILE, "Complete Profile")
            binding.profileName.text = CacheUtil.getInstance()
                .getTranslations(Text.COMPLETE_YOUR_PROFILE, "Complete your profile")
        } else {
            binding.profileCard.setCardBackgroundColor(requireActivity().resources.getColor(R.color.white))
            binding.editLabel.text = CacheUtil.getInstance().getTranslations(Text.EDIT, "Edit")
        }
    }

    private var curbsideLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == Activity.RESULT_OK) {
        }
    }

    private fun showCurbside() {
        activity?.let {
            val intent = Intent(it, CurbsideNewActivity::class.java)
            intent.putExtra("fromProfile", true)
            curbsideLauncher.launch(intent)
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
        dismissDialogs()
        showAlertDialog(context = requireContext(),
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
        dismissDialogs()
        LoginManager.getInstance().logOut()
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildUtil.googleInClient(requireActivity())).requestEmail().build()
        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), googleSignInOptions)
        googleSignInClient.signOut()
        CacheUtil.getInstance().clearOrder()
        SoloAuth.getInstance().logout()
        CacheUtil.getInstance().setCurrentOrderId(null)
        AuthUtil.getInstance().customer = null
        navigateUp()
    }

    override fun showCustomerCards() {

    }

    override fun showRevelRewardPoints(pointValue: Double?) {

    }

    override fun showCard(isDefault: Boolean) {
    }

    override fun onClose() {

    }

    override fun onDelete() {
        showLoadingDialog()
        accountPresenter?.deleteAccount()
    }

    override fun onLogoutClose() {

    }

    override fun onLogout() {
        LoginManager.getInstance().logOut()
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildUtil.googleInClient(requireActivity())).requestEmail().build()
        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), googleSignInOptions)
        googleSignInClient.signOut()
        accountPresenter!!.removeMukafaaRedemption()
        CacheUtil.getInstance().clearOrder()
        SoloAuth.getInstance().logout()
        CacheUtil.getInstance().setCurrentOrderId(null)
        AuthUtil.getInstance().customerLoyalty = null
        AuthUtil.getInstance().customerLoyaltyTier = null
        AuthUtil.getInstance().customer = null
        navigateUp()
    }
}
