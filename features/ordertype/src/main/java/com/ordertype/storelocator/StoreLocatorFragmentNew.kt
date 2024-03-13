package com.ordertype.storelocator

import android.Manifest
import android.app.Activity
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.model.Place
import com.herfy.ordertype.R
import com.herfy.ordertype.databinding.StoreLocatorBinding
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.AppLogger
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.ViewUtil
import com.location.FusedLocationProvider
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.skylinedynamics.solosdk.api.models.objects.Store
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.util.Locale

@AndroidEntryPoint
class StoreLocatorFragmentNew : BaseFragment<StoreLocatorViewModel, StoreLocatorBinding>(),
    FusedLocationProvider.LocationResultCallback {

    override val viewModel: StoreLocatorViewModel by viewModels()

    override val layout: Int = R.layout.store_locator

    private var fusedLocationProvider: FusedLocationProvider? = null

    private var googleMap: GoogleMap? = null

    private var geocoder: Geocoder? = null

    private var address: Address? = null

    private var place: Place? = null

    private var location: Location? = null

    private var locationMarker: Marker? = null

    private val storeMarkers = ArrayList<Marker?>()

    private var hasLocation = false

    private val gpsLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
            initializeLocation()
        }

    private val locationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getLocation()
        } else {
            getStoresFallback()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geocoder = Geocoder(
            requireActivity(), LocaleUtil.getInstance().createLocale()
        )
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

            back.setOnClickListener {
                navigateUp()
            }

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
                .addOnCompleteListener { task ->
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
                }
        }

    }

    private fun setupFusedLocationProvider() {
        if (fusedLocationProvider != null) {
            fusedLocationProvider!!.disconnect()
        }
        this.fusedLocationProvider = FusedLocationProvider(requireContext())
        this.fusedLocationProvider?.setLocationResultCallback(this)
        this.fusedLocationProvider?.connect()
    }

    fun getLocation() {
        activity?.apply {
            if (ActivityCompat.checkSelfPermission(
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
        binding.apply {
            title.text =
                CacheUtil.getInstance().getTranslations(Text.STORE_LOCATOR,"Store Locator")
        }
    }

    override fun observe() {
        super.observe()
    }

    private fun showLocation(isPlace: Boolean, latLng: LatLng, address: String) {
        try {
            if (locationMarker != null) {
                locationMarker?.remove()
            }
            val markerOptions = MarkerOptions()
            markerOptions.position(latLng)
            try {
                val activity: Activity? = activity
                if (activity != null) {
                    val bitmapDrawable = ContextCompat.getDrawable(
                        activity, R.drawable.user_current_location
                    ) as BitmapDrawable?
                    if (bitmapDrawable != null) {
                        val bitmap = Bitmap.createScaledBitmap(bitmapDrawable.bitmap, 50, 50, true)
                        val canvas = Canvas(bitmap)
                        val paint = Paint()
                        canvas.drawBitmap(bitmap, 0f, 0f, paint)
                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            markerOptions.anchor(0.5f, 0.5f)
            locationMarker = googleMap?.addMarker(markerOptions)
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11f))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getNewFusedLocation(location: Location?) {
        location?.let {
            hasLocation = true
            fusedLocationProvider!!.disconnect()
            this.location = it
            CacheUtil.getInstance().orderLatitude = it.latitude.toString()
            CacheUtil.getInstance().orderLongitude = it.longitude.toString()
            CacheUtil.getInstance().orderDeliveryLatitude = it.latitude.toString()
            CacheUtil.getInstance().orderDeliveryLongitude = it.longitude.toString()
            viewModel.getStores(1)
            val latLng = LatLng(
                it.latitude, it.longitude
            )
            getAddress(latLng)?.let { address ->
                showLocation(false, latLng, address)
            }
        }
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

    private fun showStores(pickupStores: ArrayList<Store>, curbsideStores: ArrayList<Store>) {
        activity?.runOnUiThread {
            val stores: ArrayList<Store>
            if (googleMap != null) {
                stores = curbsideStores
                //binding.storesRecyclerView.adapter = pickupCurbsideStoresRecyclerViewAdapter
                if (storeMarkers.isNotEmpty()) {
                    for (marker: Marker? in storeMarkers) {
                        marker?.remove()
                    }
                    storeMarkers.clear()
                }
                for (i in stores.indices) {
                    val store = stores[i]
                    val markerOptions = MarkerOptions()
                    markerOptions.title(i.toString())
                    markerOptions.position(LatLng(store.attributes.lat, store.attributes.lng))
                    try {
                        val activity: Activity? = activity
                        if (activity != null) {
                            val icon = ViewUtil.vectorDrawableToBitmapDescriptor(
                                activity, R.drawable.branch_unselected
                            )
                            if (icon != null) {
                                markerOptions.icon(icon)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    markerOptions.anchor(0.5f, 1f)
                    val marker = googleMap?.addMarker(markerOptions)
                    marker?.isVisible = CacheUtil.getInstance().orderType !== OrderType.DELIVERY
                    marker?.title = i.toString()
                    storeMarkers.add(marker)
                }
            }

        }
    }

    private fun getStoresFallback() {
        if (!hasLocation) {
            try {
                viewModel.getStores(1)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}