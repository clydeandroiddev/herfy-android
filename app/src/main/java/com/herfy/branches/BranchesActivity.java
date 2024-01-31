package com.herfy.branches;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.data.constants.Text;
import com.herfy.data.constants.Values;
import com.herfy.location.FusedLocationProvider;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.herfy.util.AppLogger;
import com.herfy.util.CacheUtil;
import com.herfy.util.StringUtil;
import com.herfy.util.ViewUtil;
import com.herfy.views.RecyclerViewUtil;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BranchesActivity extends BaseActivity implements BranchesContract.View {

    private BranchesContract.Presenter branchesPresenter;

    @BindView(R.id.back)
    MaterialButton back;
    @BindView(R.id.cart)
    ConstraintLayout cart;
    @BindView(R.id.title)
    TextView title;

    @BindView(R.id.location)
    FloatingActionButton location;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    private GoogleMap map;

    private LinearLayoutManager layoutManager;
    private BranchesRecyclerViewAdapter adapter;

    private Marker locationMarker;
    private ArrayList<Marker> branchMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branches);
        ButterKnife.bind(this);
        branchesPresenter = new BranchesPresenter(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (map == null) {
            showLoadingDialog();
        }

        branchesPresenter.start();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Values.PERMISSION_LOCATION) {
            getLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == Values.PERMISSION_LOCATION) {
                getLocation();
            } else if (requestCode == Values.PERMISSION_CALL) {
                branchesPresenter.callBranch();
            }
        }

    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void setPresenter(BranchesContract.Presenter presenter) {
        branchesPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
    }

    @Override
    public void setupViews() {

        cart.setVisibility(View.GONE);
        back.setVisibility(View.VISIBLE);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.branches_map);
        if (supportMapFragment != null) {
            supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {

                    if (map == null) {
                        initializeLocation();
                        branchesPresenter.getBranches(1);
                    }

                    map = googleMap;
                    map.getUiSettings().setCompassEnabled(false);
                    map.getUiSettings().setZoomControlsEnabled(false);
                    map.getUiSettings().setMapToolbarEnabled(false);

                    map.setPadding(0, 0, 0, ViewUtil.dpToPx(BranchesActivity.this, 255));

                    map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(Marker marker) {

                            selectBranch(true, Integer.parseInt(marker.getTitle()));

                            return true;
                        }
                    });

                }
            });
        }

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new BranchesRecyclerViewAdapter(branchesPresenter);
        recyclerView.setAdapter(adapter);

        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initializeLocation();
            }
        });

    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {
        back.setText(CacheUtil.getInstance().getTranslations(Text.BACK, "Back"));
        title.setText(CacheUtil.getInstance().getTranslations(Text.OUR_BRANCHES));
    }

    @Override
    public void initializeLocation() {

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(60000);
        locationRequest.setFastestInterval(60000);
        locationRequest.setSmallestDisplacement(0.0f);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder locationSettingsRequestBuilder = new LocationSettingsRequest.Builder();
        locationSettingsRequestBuilder.addLocationRequest(locationRequest);
        locationSettingsRequestBuilder.setAlwaysShow(true);

        LocationServices.getSettingsClient(this).checkLocationSettings(locationSettingsRequestBuilder.build()).addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {

                try {

                    task.getResult(ApiException.class);

                    getLocation();

                } catch (ApiException e) {

                    e.printStackTrace();

                    if (e.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {

                        try {

                            ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                            resolvableApiException.startResolutionForResult(BranchesActivity.this, Values.PERMISSION_LOCATION);

                        } catch (IntentSender.SendIntentException exception) {

                            exception.printStackTrace();

                        }

                    } else if (e.getStatusCode() == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                        AppLogger.error("Settings change unavailable");
                    }

                }

            }
        });

    }

    @Override
    public void getLocation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ActivityCompat.checkSelfPermission(BranchesActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            branchesPresenter.getLocation(new FusedLocationProvider(BranchesActivity.this));
        } else {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, Values.PERMISSION_LOCATION);
        }
    }

    @Override
    public void showLocation(LatLng latLng) {

        location.setImageResource(R.drawable.ic_location_enabled);
        location.setColorFilter(Color.parseColor(CacheUtil.getInstance().getColorMain()));

        if (locationMarker != null) {
            locationMarker.remove();
        }

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);

        BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.marker_location);
        if (bitmapDrawable != null) {
            Bitmap bitmap = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), 110, 110, true);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            ColorFilter filter = new PorterDuffColorFilter(Color.parseColor(CacheUtil.getInstance().getColorMain()), PorterDuff.Mode.SRC_IN);
            paint.setColorFilter(filter);
            canvas.drawBitmap(bitmap, 0, 0, paint);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
        }

        markerOptions.anchor(0.5f, 0.5f);

        locationMarker = map.addMarker(markerOptions);
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11f));

    }

    @Override
    public void showBranches(ArrayList<Store> branches) {

        adapter.notifyDataSetChanged();

        if (map != null) {

            if (!branchMarkers.isEmpty()) {
                for (Marker marker : branchMarkers) {
                    marker.remove();
                }
                branchMarkers.clear();
            }

            for (int i = 0; i < branches.size(); i++) {

                Store store = branches.get(i);

                //LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                //View view = inflater.inflate(R.layout.marker_store, null);

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.title(String.valueOf(i));
                markerOptions.position(new LatLng(store.getAttributes().getLat(), store.getAttributes().getLng()));
                //markerOptions.icon(BitmapDescriptorFactory.fromBitmap(ViewUtil.createDrawableFromView(getActivity(), view)));
                BitmapDescriptor icon = ViewUtil.vectorDrawableToBitmapDescriptor(this, R.drawable.marker_store_unselected);
                markerOptions.icon(icon);
                markerOptions.anchor(0.5f, 1f);

                Marker marker = map.addMarker(markerOptions);
                marker.setTitle(String.valueOf(i));

                branchMarkers.add(marker);

            }
        }

        dismissDialogs();

    }

    @Override
    public void setBranch(Store branch, boolean selected) {

    }

    @Override
    public void selectBranch(boolean markerClicked, int position) {

        if (markerClicked && adapter.getSelected() == position) {
            Store branch = branchesPresenter.getBranch(position);
            Uri.Builder directionsBuilder = new Uri.Builder()
                    .scheme("https")
                    .authority("www.google.com")
                    .appendPath("maps")
                    .appendPath("dir")
                    .appendPath("")
                    .appendQueryParameter("api", "1")
                    .appendQueryParameter("travelmode", "driving")
                    .appendQueryParameter("destination", branch.getAttributes().getLat() + "," + branch.getAttributes().getLng());

            if(StringUtil.hasOrderLatLong()) {
                directionsBuilder.appendQueryParameter("origin", CacheUtil.getInstance().getOrderLatitude() + "," + CacheUtil.getInstance().getOrderLongitude());
            }

            startActivity(new Intent(Intent.ACTION_VIEW, directionsBuilder.build()));
        } else {

            adapter.setSelected(position);

            RecyclerViewUtil.smoothSnapToPosition(recyclerView, position, LinearSmoothScroller.SNAP_TO_START);

            for (int i = 0; i < branchMarkers.size(); i++) {

                Marker branchMarker = branchMarkers.get(i);

                if (branchMarker.getTitle() != null && branchMarker.getTitle().equals(String.valueOf(position))) {

                    Store branch = branchesPresenter.getBranch(Integer.parseInt(branchMarker.getTitle()));

                    if (branch != null) {

                        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                        View view = inflater.inflate(R.layout.marker_store_new, null);

                        MaterialButton open = view.findViewById(R.id.open);
                        open.setText(CacheUtil.getInstance().getTranslations(Text.OPEN_IN_GOOGLE_MAPS, "Open in Google Maps"));
                        open.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                Uri.Builder directionsBuilder = new Uri.Builder()
                                        .scheme("https")
                                        .authority("www.google.com")
                                        .appendPath("maps")
                                        .appendPath("dir")
                                        .appendPath("")
                                        .appendQueryParameter("api", "1")
                                        .appendQueryParameter("travelmode", "driving")

                                        .appendQueryParameter("destination", branch.getAttributes().getLat() + "," + branch.getAttributes().getLng());

                                if(StringUtil.hasOrderLatLong()) {
                                    directionsBuilder.appendQueryParameter("origin", CacheUtil.getInstance().getOrderLatitude() + "," + CacheUtil.getInstance().getOrderLongitude());
                                }

                                startActivity(new Intent(Intent.ACTION_VIEW, directionsBuilder.build()));

                            }
                        });

                        branchMarker.setIcon(BitmapDescriptorFactory.fromBitmap(ViewUtil.createDrawableFromView(this, view)));
                        branchMarker.setAnchor(0.1f, 1.0f);
                        branchMarker.setTitle(String.valueOf(i));

                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(branchMarker.getPosition(), 11f));

                    }

                } else {

                    branchMarker.setIcon(ViewUtil.vectorDrawableToBitmapDescriptor(this, R.drawable.marker_store_unselected));
                    branchMarker.setAnchor(0.5f, 1f);
                    branchMarker.setTitle(String.valueOf(i));

                }
            }

        }

    }

    @Override
    public void callBranch(Store store) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + store.getAttributes().getTelephone().trim().replace(" ", "")));
            startActivity(intent);
        } else {
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, Values.PERMISSION_CALL);
        }

    }

    @Override
    public void showError(String error) {
        dismissDialogs();
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void selectBranch(String id) {

    }

}
