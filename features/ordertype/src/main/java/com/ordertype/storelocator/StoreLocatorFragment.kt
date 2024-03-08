package com.ordertype.storelocator

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityCompat
import androidx.fragment.app.viewModels
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.libraries.places.api.model.Place
import com.herfy.ordertype.R
import com.herfy.ordertype.databinding.StoreLocatorBinding
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.AppLogger
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.ViewUtil
import com.location.FusedLocationProvider
import com.location.FusedLocationProvider.LocationResultCallback
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.util.Locale

@AndroidEntryPoint
class StoreLocatorFragment : BaseFragment<StoreLocatorViewModel, StoreLocatorBinding>(),
    LocationResultCallback {

    override val viewModel: StoreLocatorViewModel by viewModels()

    override val layout: Int = R.layout.store_locator

    private var fusedLocationProvider: FusedLocationProvider? = null

    private var googleMap: GoogleMap? = null

    private var geocoder: Geocoder? = null

    private var address: Address? = null

    private var place: Place? = null

    private var location: Location? = null

    var hasLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geocoder = Geocoder(
            requireActivity(), LocaleUtil.getInstance().createLocale()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let {

        }
    }

    private fun setUpMapLocale() {
        val languageToLoad = if (LocaleUtil.getInstance().language == "ar") "ar" else "en"
        val locale = Locale(languageToLoad)
        Locale.setDefault(locale)
        resources.configuration.setLocale(locale)
    }

    override fun initViews() {
        super.initViews()
        binding.apply {
            val supportMapFragment =
                childFragmentManager.findFragmentById(R.id.order_type_map) as SupportMapFragment?
            supportMapFragment?.getMapAsync { map ->
                googleMap = map
                googleMap?.uiSettings?.isCompassEnabled = false
                googleMap?.uiSettings?.isZoomControlsEnabled = false
                googleMap?.uiSettings?.isMapToolbarEnabled = false
                setUpMapLocale()
                googleMap?.setOnMapLoadedCallback {
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                Values.DEFAULT_LATITUDE, Values.DEFAULT_LONGITUDE
                            ), 15f
                        )
                    )
                    initializeLocation()
                    googleMap?.setPadding(0, 0, 0, ViewUtil.dpToPx(requireContext(), 255))
                }
                googleMap?.setOnMarkerClickListener { marker ->
                    try {
                        if (marker.title != null && marker.title?.isEmpty() == false) {
                            val position = marker.title?.toInt() ?: 0

                        }
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }
                    true
                }
            }

            pickupLayout.location.setOnClickListener { initializeLocation() }
        }
    }

    fun initializeLocation() {
        context?.let {
            val locationRequest = LocationRequest.create()
            locationRequest.interval = 60000
            locationRequest.fastestInterval = 60000
            locationRequest.smallestDisplacement = 0.0f
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            val locationSettingsRequestBuilder = LocationSettingsRequest.Builder()
            locationSettingsRequestBuilder.addLocationRequest(locationRequest)
            locationSettingsRequestBuilder.setAlwaysShow(true)
            LocationServices.getSettingsClient(it)
                .checkLocationSettings(locationSettingsRequestBuilder.build())
                .addOnCompleteListener(OnCompleteListener { task ->
                    try {
                        task.getResult(ApiException::class.java)
                        getLocation()
                    } catch (e: ApiException) {
                        getStoresFallback()
                        e.printStackTrace()
                        if (e.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                            try {
                                val resolvableApiException = e as ResolvableApiException
                                val intentSenderRequest =
                                    IntentSenderRequest.Builder(resolvableApiException.resolution)
                                        .build()
                                gpsLauncher.launch(intentSenderRequest)
                            } catch (exception: IntentSender.SendIntentException) {
                                exception.printStackTrace()
                            }
                        } else if (e.statusCode == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                            AppLogger.error("Settings change unavailable")
                        }
                    }
                })
        }

    }

    private fun setupFusedLocationProvider() {
        fusedLocationProvider = FusedLocationProvider(requireContext())
        fusedLocationProvider?.disconnect()
        fusedLocationProvider = fusedLocationProvider
        fusedLocationProvider?.setLocationResultCallback(this)
        fusedLocationProvider?.connect()
    }

    fun getLocation() {
        activity?.apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                context?.let {
                    setupFusedLocationProvider()
                    Handler(Looper.getMainLooper()).postDelayed({
                        getStoresFallback()
                    }, 5_000L)
                }
            } else {
                CacheUtil.getInstance().orderLatitude = Values.DEFAULT_LATITUDE.toString()
                CacheUtil.getInstance().orderLongitude = Values.DEFAULT_LONGITUDE.toString()
                locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    override fun initTranslations() {
        super.initTranslations()
        binding.apply {}
    }

    override fun observe() {
        super.observe()
    }

    private fun showLocation(isPlace: Boolean, latLng: LatLng, address: String) {

    }

    override fun getNewFusedLocation(location: Location?) {
        hasLocation = true
        fusedLocationProvider!!.disconnect()
        this.location = location
        CacheUtil.getInstance().orderLatitude = location!!.latitude.toString()
        CacheUtil.getInstance().orderLongitude = location!!.longitude.toString()
        CacheUtil.getInstance().orderDeliveryLatitude = location!!.latitude.toString()
        CacheUtil.getInstance().orderDeliveryLongitude = location!!.longitude.toString()
        getStores(1)
        val latLng = LatLng(
            location!!.latitude, location!!.longitude
        )
        getAddress(latLng)?.let { showLocation(false, latLng, it) }
    }

    private fun getAddress(latLng: LatLng): String? {
        if (geocoder != null) {
            try {
                val addresses: MutableList<Address>? =
                    geocoder!!.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (addresses != null) {
                    address = if (addresses.isEmpty()) null else addresses[0]
                }
                if (address != null) {
                    place = null
                    return address!!.getAddressLine(0)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return ""
    }
}