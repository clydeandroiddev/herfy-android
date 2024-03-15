package com.herfy.account.account

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.tabs.TabLayout
import com.herfy.account.AccountContract
import com.herfy.account.AccountPresenter
import com.herfy.account.account.adapter.PaymentMethodsAdapter
import com.herfy.account.account.adapter.UsersAddressesAdapter
import com.herfy.addresses.AddressesContract
import com.herfy.addresses.AddressesPresenter
import com.herfy.base.BaseFragment
import com.herfy.order.OrderContract
import com.herfy.order.OrderPresenter
import com.herfy.profile.databinding.FragmentUserAddressesBinding
import com.herfy.profile.databinding.FragmentUserEditProfileBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.DialogUtils
import com.skylinedynamics.solosdk.api.models.objects.Address
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Rating
import com.skylinedynamics.solosdk.api.models.objects.Store
import dagger.hilt.android.AndroidEntryPoint
import java.util.ArrayList
import java.util.Date

@AndroidEntryPoint
class AddressUserFragment : BaseFragment(), AddressesContract.View,
    UsersAddressesAdapter.AddressUserListener {
    private var accountPresenter: AddressesContract.Presenter? = null

    private lateinit var binding: FragmentUserAddressesBinding

    private lateinit var adapter: UsersAddressesAdapter

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

    override fun setupViews() {
        binding.apply {
            back.setOnClickListener {
                navigateUp()
            }
            continueButton.setOnClickListener {

            }
            adapter = UsersAddressesAdapter(ArrayList(), this@AddressUserFragment)
            binding.addresses.adapter = adapter
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.title.text =
            CacheUtil.getInstance().getTranslations(Text.ADDRESSES_LABEL, "Addresses")
        binding.continueButton.text =
            CacheUtil.getInstance().getTranslations(Text.ADD_NEW_ADDRESS_LABEL, "Add New Address")
    }


    override fun initializeLocation() {

    }

    override fun getLocation() {

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

    override fun onEditCard(card: Address, position: Int) {

    }

    override fun onDeleteCard(card: Address, position: Int) {

    }
}
