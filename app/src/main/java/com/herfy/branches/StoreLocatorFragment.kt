package com.herfy.branches

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
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
import com.herfy.base.BaseFragment
import com.herfy.ordertype.R
import com.herfy.ordertype.databinding.StoreLocatorBinding
import com.herfy.util.StringUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.navigateUp
import com.herfy.utils.helper.CacheUtil
import com.herfy.views.RecyclerViewUtil.smoothSnapToPosition
import com.location.FusedLocationProvider
import com.skylinedynamics.solosdk.api.models.objects.Store
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale


@AndroidEntryPoint
class StoreLocatorFragment : BaseFragment(), BranchesContract.View {
    private var branchesPresenter: BranchesContract.Presenter? = null

    private lateinit var binding: StoreLocatorBinding

    private var map: GoogleMap? = null

    private var layoutManager: LinearLayoutManager? = null

    private var adapter: BranchesRecyclerViewAdapter? = null

    private var locationMarker: Marker? = null

    private val branchMarkers = ArrayList<Marker?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        branchesPresenter = BranchesPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = StoreLocatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showLoadingDialog()
        branchesPresenter?.start()
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Values.PERMISSION_LOCATION) {
            getLocation()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == Values.PERMISSION_LOCATION) {
                getLocation()
            } else if (requestCode == Values.PERMISSION_CALL) {
                branchesPresenter!!.callBranch()
            }
        }
    }

    override fun setPresenter(presenter: BranchesContract.Presenter) {
        branchesPresenter = presenter
    }

    override fun logEvent(
        eventName: String,
        eventAttributes: HashMap<String, String>,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {

    }

    override fun setCartExpiry() {

    }

    override fun setupViews() {
        binding.apply {
            back.setOnClickListener { navigateUp() }
            val supportMapFragment =
                childFragmentManager.findFragmentById(com.herfy.R.id.order_type_map) as SupportMapFragment?
            supportMapFragment?.getMapAsync { googleMap ->
                if (map == null) {
                    initializeLocation()
                    branchesPresenter?.getBranches(1)
                }
                map = googleMap
                map!!.uiSettings.isCompassEnabled = false
                map!!.uiSettings.isZoomControlsEnabled = false
                map!!.uiSettings.isMapToolbarEnabled = false
                map!!.setPadding(0, 0, 0, com.herfy.util.ViewUtil.dpToPx(requireContext(), 255))
                map!!.setOnMarkerClickListener { marker ->
                    selectBranch(true, marker.title!!.toInt())
                    true
                }
            }
            layoutManager = LinearLayoutManager(requireContext())
            storesRecyclerView.layoutManager = layoutManager
            adapter = BranchesRecyclerViewAdapter(branchesPresenter)
            storesRecyclerView.adapter = adapter
            pickupLayout.location.setOnClickListener { initializeLocation() }
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        binding.title.text =
            CacheUtil.getInstance().getTranslations(Text.STORE_LOCATOR, "Store Locator")
    }

    override fun initializeLocation() {
        val locationRequest = LocationRequest.create()
        locationRequest.setInterval(60000)
        locationRequest.setFastestInterval(60000)
        locationRequest.setSmallestDisplacement(0.0f)
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        val locationSettingsRequestBuilder = LocationSettingsRequest.Builder()
        locationSettingsRequestBuilder.addLocationRequest(locationRequest)
        locationSettingsRequestBuilder.setAlwaysShow(true)
        LocationServices.getSettingsClient(requireActivity())
            .checkLocationSettings(locationSettingsRequestBuilder.build())
            .addOnCompleteListener { task ->
                try {
                    task.getResult(ApiException::class.java)
                    getLocation()
                } catch (e: ApiException) {
                    e.printStackTrace()
                    if (e.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                        try {
                            val resolvableApiException = e as ResolvableApiException
                            resolvableApiException.startResolutionForResult(
                                requireActivity(), Values.PERMISSION_LOCATION
                            )
                        } catch (exception: IntentSender.SendIntentException) {
                            exception.printStackTrace()
                        }
                    } else if (e.statusCode == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                        com.herfy.util.AppLogger.error("Settings change unavailable")
                    }
                }
            }
    }

    override fun getLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            branchesPresenter!!.getLocation(FusedLocationProvider(requireActivity()))
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), Values.PERMISSION_LOCATION
            )
        }
    }

    override fun showLocation(latLng: LatLng) {
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
            locationMarker = map?.addMarker(markerOptions)
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11f))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun showBranches(branches: ArrayList<Store>) {
        adapter?.notifyDataSetChanged()
        if (map != null) {
            if (!branchMarkers.isEmpty()) {
                for (marker in branchMarkers) {
                    marker!!.remove()
                }
                branchMarkers.clear()
            }
            for (i in branches.indices) {
                val store = branches[i]
                val markerOptions = MarkerOptions()
                markerOptions.title(i.toString())
                markerOptions.position(LatLng(store.attributes.lat, store.attributes.lng))
                val icon = com.herfy.util.ViewUtil.vectorDrawableToBitmapDescriptor(
                    requireActivity(), R.drawable.branch_unselected
                )
                markerOptions.icon(icon)
                markerOptions.anchor(0.5f, 1f)
                val marker = map?.addMarker(markerOptions)
                marker?.title = i.toString()
                branchMarkers.add(marker)
            }
        }
        dismissDialogs()
    }

    override fun setBranch(branch: Store, selected: Boolean) {}

    override fun selectBranch(markerClicked: Boolean, position: Int) {
        if (markerClicked && adapter!!.selected == position) {
            val branch = branchesPresenter!!.getBranch(position)
            val directionsBuilder =
                Uri.Builder().scheme("https").authority("www.google.com").appendPath("maps")
                    .appendPath("dir").appendPath("").appendQueryParameter("api", "1")
                    .appendQueryParameter("travelmode", "driving").appendQueryParameter(
                        "destination",
                        branch.attributes.lat.toString() + "," + branch.attributes.lng
                    )
            if (StringUtil.hasOrderLatLong()) {
                directionsBuilder.appendQueryParameter(
                    "origin",
                    CacheUtil.getInstance().orderLatitude + "," + CacheUtil.getInstance().orderLongitude
                )
            }
            startActivity(Intent(Intent.ACTION_VIEW, directionsBuilder.build()))
        } else {
            adapter!!.selected = position
            smoothSnapToPosition(
                binding.storesRecyclerView, position, LinearSmoothScroller.SNAP_TO_START
            )
            for (i in branchMarkers.indices) {
                val branchMarker = branchMarkers[i]
                if (branchMarker!!.title != null && branchMarker.title == position.toString()) {
                    val branch = branchesPresenter!!.getBranch(
                        branchMarker.title!!.toInt()
                    )
                    if (branch != null) {
                        val inflater =
                            requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                        val view = inflater.inflate(com.herfy.R.layout.marker_store_new, null)
                        val textView = view.findViewById<TextView>(com.herfy.ordertype.R.id.kmAway)
                        val distanceValue = StringUtil.getDistance(
                            branch.attributes.distance
                        )
                        textView.text =
                            CacheUtil.getInstance().getTranslations(Text.KM_AWAY, "(x) away")
                                .replace("(x)", distanceValue).lowercase(Locale.getDefault())
                        view.setOnClickListener {
                            val directionsBuilder =
                                Uri.Builder().scheme("https").authority("www.google.com")
                                    .appendPath("maps").appendPath("dir").appendPath("")
                                    .appendQueryParameter("api", "1")
                                    .appendQueryParameter("travelmode", "driving")
                                    .appendQueryParameter(
                                        "destination",
                                        branch.attributes?.lat.toString() + "," + branch.attributes?.lng
                                    )

                            if (StringUtil.hasOrderLatLong()) {
                                directionsBuilder.appendQueryParameter(
                                    "origin",
                                    CacheUtil.getInstance().orderLatitude + "," + CacheUtil.getInstance().orderLongitude
                                )
                            }

                            try {
                                val activity: Activity? = activity
                                activity?.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW, directionsBuilder.build()
                                    )
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        branchMarker.setIcon(
                            BitmapDescriptorFactory.fromBitmap(
                                com.herfy.util.ViewUtil.createDrawableFromView(
                                    requireActivity(), view
                                )
                            )
                        )
                        branchMarker.setAnchor(0.5f, 1f)
                        branchMarker.title = i.toString()
                        map!!.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                branchMarker.position, 11f
                            )
                        )
                    }
                } else {
                    branchMarker.setIcon(
                        com.herfy.util.ViewUtil.vectorDrawableToBitmapDescriptor(
                            requireActivity(), R.drawable.branch_unselected
                        )
                    )
                    branchMarker.setAnchor(0.5f, 1f)
                    branchMarker.title = i.toString()
                }
            }
        }
    }

    override fun callBranch(store: Store) {

    }

    override fun showError(error: String) {
        dismissDialogs()
        Toast.makeText(requireActivity(), error, Toast.LENGTH_SHORT).show()
    }

    override fun selectBranch(id: String) {}
}

