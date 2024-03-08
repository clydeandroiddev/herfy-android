package com.herfy.places;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.material.button.MaterialButton;
import com.herfy.R;
import com.herfy.addresses.views.AddAddressDialogFragment;
import com.herfy.base.BaseActivity;
import com.herfy.font.FontHandler;
import com.herfy.util.AppLogger;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.LocaleUtil;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PlacesActivity extends BaseActivity implements PlacesContract.View {

    private PlacesContract.Presenter placesPresenter;

    @BindView(R.id.back) ImageButton back;
    @BindView(R.id.clear) ImageButton clear;
    @BindView(R.id.location) ImageButton location;

    @BindView(R.id.current_location) RelativeLayout currentLocation;

    @BindView(R.id.search) AutoCompleteTextView search;

    @BindView(R.id.select_location) MaterialButton selectLocation;

    private GoogleMap map;

    //private Marker marker;

    private boolean mapMoved = true;
    private boolean mapZoom = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places);
        ButterKnife.bind(this);
        placesPresenter = new PlacesPresenter(this);
        placesPresenter.setGeocoder(new Geocoder(this, LocaleUtil.getInstance().createLocale()));
    }

    @Override
    protected void onResume() {
        super.onResume();

        placesPresenter.start();

        showLoadingDialog();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Values.PERMISSION_LOCATION) {
            getLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == Values.PERMISSION_LOCATION) {
                getLocation();
            }
        }

    }

    @Override
    public void setPresenter(PlacesContract.Presenter presenter) {
        placesPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
    }

    @Override
    public void setupViews() {
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                finish();
            }
        });

        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.places_map);
        if(supportMapFragment != null) {
            supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {

                    map = googleMap;
                    map.getUiSettings().setCompassEnabled(false);
                    map.getUiSettings().setZoomControlsEnabled(false);
                    map.getUiSettings().setMapToolbarEnabled(false);

                    /*map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                        @Override
                        public void onMapClick(LatLng latLng) {
                            placesPresenter.getAddress(latLng);
                        }
                    });*/

                    map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                        @Override
                        public void onCameraIdle() {

                            if(!mapMoved) {

                                mapZoom = false;

                                LatLng latLng = map.getCameraPosition().target;

                                placesPresenter.getAddress(latLng);

                                /*search.setThreshold(1000);
                                search.setText(currentLocation);
                                search.setThreshold(1);
                                search.clearFocus();

                                if(mapMarker != null) {
                                    mapMarker.remove();
                                }

                                MarkerOptions markerOptions = new MarkerOptions();
                                markerOptions.position(latLng);

                                mapMarker = map.addMarker(markerOptions);*/

                            } else {
                                mapMoved = false;
                            }

                        }
                    });

                    dismissDialogs();

                    initializeLocation();

                }
            });
        }

        /*search.setThreshold(1);

        PlacesAutoCompleteAdapter placesAdapter = new PlacesAutoCompleteAdapter(this, android.R.layout.simple_list_item_1, placesPresenter.getPlacesClient());

        search.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showProgressDialog(CacheUtil.getInstance().getTranslations(Text.LOADING));
                placesPresenter.fetchPlace(placesAdapter.getItem(i).placeId.toString());
            }
        });

        search.setAdapter(placesAdapter);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(editable.length() != 0) {
                    clear.setVisibility(View.VISIBLE);
                } else {
                    clear.setVisibility(View.GONE);
                }
            }
        };
        search.addTextChangedListener(textWatcher);*/

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                search.setText("");
                search.requestFocus();
            }
        });

        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoadingDialog();
                mapZoom = true;
                initializeLocation();
            }
        });

        currentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoadingDialog();
                mapZoom = true;
                initializeLocation();
            }
        });

        selectLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(search.getText().length() > 0) {
                    placesPresenter.addAddress(null, null, null);
                }
            }
        });

    }

    @Override
    public void setupFonts() {

        search.setTypeface(FontHandler.getRegularTypeface());
        selectLocation.setTypeface(FontHandler.getRegularTypeface());

    }

    @Override
    public void setupTranslations() {

        //search.setHint(CacheUtil.getInstance().getTranslations(Text.SEARCH_HERE));

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

                    if(e.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {

                        try {

                            ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                            resolvableApiException.startResolutionForResult(PlacesActivity.this, Values.PERMISSION_LOCATION);

                        } catch (IntentSender.SendIntentException exception) {

                            exception.printStackTrace();

                        }

                    } else if(e.getStatusCode() == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                        AppLogger.error("Settings change unavailable");
                    }

                }

            }
        });

    }

    @Override
    public void getLocation() {

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ActivityCompat.checkSelfPermission(PlacesActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            placesPresenter.getLocation(new FusedLocationProvider(PlacesActivity.this));
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Values.PERMISSION_LOCATION);
        }
    }

    @Override
    public void showLocation(LatLng location, String address) {

        /*MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(location);

        if (marker != null) {
            marker.remove();
        }

        marker = map.addMarker(markerOptions);*/

        //search.setThreshold(1000);
        search.setText(address);
        //search.setThreshold(1);
        //search.clearFocus();

        mapMoved = true;
        if(!mapZoom) {
            map.animateCamera(CameraUpdateFactory.newLatLng(location));
        } else {
            mapZoom = false;
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(search.getWindowToken(), 0);

        dismissDialogs();

    }

    @Override
    public void showAddAddress(Address address, Place place) {
        AddAddressDialogFragment addAddressDialogFragment = AddAddressDialogFragment.newInstance(new AddAddressDialogFragment.OnSubmit() {
            @Override
            public void onSubmit(Dialog dialog, String phoneNumber, String label, String note) {
                dialog.dismiss();
                showLoadingDialog();
                placesPresenter.addAddress(phoneNumber, label, note);
            }
        }, null);
        addAddressDialogFragment.show(getSupportFragmentManager(), AddAddressDialogFragment.class.getSimpleName());
    }

    @Override
    public void showAccount() {
        dismissDialogs();
        onBackPressed();
        finish();
    }

    @Override
    public void showMessage(String message) {
        dismissDialogs();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
