package com.herfy.account.account

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.herfy.account.account.adapter.UsersAddressesAdapter
import com.herfy.addresses.AddressesContract
import com.herfy.addresses.AddressesPresenter
import com.herfy.base.BaseFragment
import com.herfy.order.views.ordertype.addaddress.AddAddressDialog
import com.herfy.order.views.ordertype.addaddress.AddNewAddressActivity
import com.herfy.profile.databinding.FragmentUserAddressesBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DialogUtils
import com.location.FusedLocationProvider
import com.skylinedynamics.solosdk.api.models.objects.Address
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddressUserFragment : BaseFragment(), AddressesContract.View,
    UsersAddressesAdapter.AddressUserListener {
    private var accountPresenter: AddressesContract.Presenter? = null

    private lateinit var binding: FragmentUserAddressesBinding

    private lateinit var addAddressDialog: AddAddressDialog

    private lateinit var adapter: UsersAddressesAdapter

    private var phoneNumber = ""
    private var addressLabel = ""
    private var addressLabel2 = ""
    private var driverNote = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountPresenter = AddressesPresenter(this)
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
        showLoadingDialog()
        accountPresenter?.getAddresses(false)
    }

    override fun setPresenter(presenter: AddressesContract.Presenter) {
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

    private var addressLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                showLoadingDialog()
                accountPresenter?.getAddresses(false)
            }
        }

    private fun goToAddNewAddress() {
        val intent = Intent(activity, AddNewAddressActivity::class.java)
        intent.putExtra("fromProfile", true)
        addressLauncher.launch(intent)
    }

    private fun goToAddAddress() {
        val activity: Activity? = activity
        activity?.let {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                goToAddNewAddress()
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), Values.PERMISSION_LOCATION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == Values.PERMISSION_LOCATION) {
                goToAddNewAddress()
            }
        }
    }

    override fun setupViews() {
        binding.apply {
            back.setOnClickListener {
                navigateUp()
            }
            continueButton.setOnClickListener {
                goToAddAddress()
            }
            adapter = UsersAddressesAdapter(ArrayList(), this@AddressUserFragment)
            binding.addresses.adapter = adapter
        }
    }

    private fun dismissAddressDialog() {
        try {
            addAddressDialog.dismiss()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun proceedToUpdateAddress(addressTrue: Address) {
        dismissAddressDialog()
        addAddressDialog = AddAddressDialog.newInstance(addressTrue,
            null,
            true,
            object : AddAddressDialog.OnSubmit {
                override fun onSuccess(
                    labelValue: String,
                    address: android.location.Address?,
                    flatNumber: String,
                    mobile: String,
                    note: String
                ) {
                    phoneNumber = mobile
                    driverNote = note
                    addressLabel2 = flatNumber
                    addressLabel = labelValue
                    showLoadingDialog()
                    if (AuthUtil.getInstance().isSignedIn) {
                        showLoadingDialog()
                        accountPresenter?.updateAddress(
                            addressTrue, driverNote, addressLabel, phoneNumber
                        )
                    } else {
                        dismissDialogs()
                    }
                }

                override fun error(text: String) {
                    showMessage(text)
                }

                override fun exit() {
                    dismissAddressDialog()
                }

            })
        addAddressDialog.show(
            requireActivity().supportFragmentManager, AddAddressDialog::class.java.simpleName
        )
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.title.text =
            CacheUtil.getInstance().getTranslations(Text.ADDRESSES_LABEL, "Addresses")
        binding.continueButton.text =
            CacheUtil.getInstance().getTranslations(Text.ADD_NEW_ADDRESS_LABEL, "Add New Address")
        binding.emptyState.setTitle(
            CacheUtil.getInstance()
                .getTranslations(Text.NO_SAVED_ADDRESSES_FOUND, "No saved addresses found.")
        )
    }


    override fun initializeLocation() {

    }

    override fun getLocation() {

    }

    override fun successDelete(position: Int) {
        dismissDialogs()
        adapter.removeItem(position)
        binding.emptyState.setVisible(adapter.itemCount == 0)
    }

    override fun successEdit() {
        dismissAddressDialog()
    }

    override fun showLocation(latLng: LatLng?) {

    }

    override fun showPlaces(places: ArrayList<AutocompletePrediction>?) {

    }

    override fun setPlace(position: Int, place: AutocompletePrediction?, selected: Boolean) {

    }

    override fun selectPlace(position: Int) {

    }

    override fun showAddresses(
        homeAddresses: ArrayList<Address>,
        workAddresses: ArrayList<Address>,
        otherAddresses: ArrayList<Address>
    ) {
        dismissDialogs()
        binding.emptyState.setVisible(homeAddresses.isEmpty())
        adapter.updateList(ArrayList(homeAddresses))
    }

    override fun setAddress(address: Address?, selected: Boolean) {

    }

    override fun showPlace(place: Place?) {

    }

    override fun selectAddressHome(position: Int) {

    }

    override fun selectAddressWork(position: Int) {

    }

    override fun selectAddressOther(position: Int) {

    }

    override fun addAddress(place: Place?, address: android.location.Address?) {

    }

    override fun updateAddress(place: Place?, address: Address?) {

    }

    override fun saveAddress(address: Address?) {

    }

    override fun showOrderType() {

    }

    override fun showMessage(message: String?) {
        dismissDialogs()
        DialogUtils.showAlertDialog(context = requireContext(),
            title = "",
            body = message.orEmpty(),
            dialogInterface = { dialog, which ->
                dialog.dismiss()
            })
    }

    override fun onEditAddress(address: Address, position: Int) {
        proceedToUpdateAddress(address)
    }

    override fun onDeleteAddress(address: Address, position: Int) {
        showLoadingDialog()
        accountPresenter?.deleteUserLocation(address, position)
    }
}
