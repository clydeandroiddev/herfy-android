package com.herfy.addresses.views;

import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.addresses.AddressesContract;
import com.herfy.addresses.AddressesPresenter;
import com.herfy.addresses.adapters.AddressesRecyclerViewAdapter;
import com.herfy.addresses.adapters.PlacesRecyclerViewAdapter;
import com.herfy.base.BaseActivity;
import com.herfy.utils.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.location.FusedLocationProvider;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.ViewUtil;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AddressesActivity extends BaseActivity implements AddressesContract.View {

    private AddressesContract.Presenter addressesPresenter;

    private boolean permissions = false;

    @BindView(R.id.back) ImageButton back;
    @BindView(R.id.clear) ImageButton clear;

    @BindView(R.id.title) TextView title;
    @BindView(R.id.searched_places_label) TextView searchedPlacesLabel;
    @BindView(R.id.current_location_label) TextView currentLocationLabel;
    @BindView(R.id.current_location) TextView currentLocation;
    @BindView(R.id.add_address) TextView addAddress;
    @BindView(R.id.home_addresses_label) TextView homeAddressesLabel;
    @BindView(R.id.work_addresses_label) TextView workAddressesLabel;
    @BindView(R.id.other_addresses_label) TextView otherAddressesLabel;
    @BindView(R.id.addresses_continue) TextView addressesContinue;

    @BindView(R.id.search) EditText search;

    @BindView(R.id.searched_places_recycler_view) RecyclerView searchedPlaces;
    @BindView(R.id.home_addresses_recycler_view) RecyclerView homeAddressesRecyclerView;
    @BindView(R.id.work_addresses_recycler_view) RecyclerView workAddressesRecyclerView;
    @BindView(R.id.other_addresses_recycler_view) RecyclerView otherAddressesRecyclerView;

    @BindView(R.id.current_location_container) MaterialCardView currentLocationContainer;
    @BindView(R.id.add_address_container) MaterialCardView addAddressContainer;

    @BindView(R.id.current_location_icon) ImageView currentLocationIcon;

    private PlacesClient placesClient;

    private PlacesRecyclerViewAdapter placesAdapter;
    private AddressesRecyclerViewAdapter homeAddressesAdapter;
    private AddressesRecyclerViewAdapter workAddressesAdapter;
    private AddressesRecyclerViewAdapter otherAddressesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addresses);
        ButterKnife.bind(this);
        addressesPresenter = new AddressesPresenter(this);

        Places.initialize(this, new String(Base64.decode(getResources().getString(R.string.google_maps_api_key_encoded), Base64.DEFAULT)));
        placesClient = Places.createClient(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dismissDialogs();
        addressesPresenter.start();
        if (!permissions) {
            initializeLocation();
        } else {
            permissions = false;
        }
        if(AuthUtil.getInstance().isSignedIn()) {
            showLoadingDialog();
            addressesPresenter.getAddresses(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Values.PERMISSION_LOCATION:
                getLocation();
                break;
            /*case Values.REQUEST_ADDRESS_HOME:
                placesAdapter.setSelected(-1);
                currentLocationContainer.setStrokeWidth(1);
                currentLocationContainer.setStrokeColor(Color.parseColor("#BEBEBE"));
                currentLocationIcon.setImageResource(R.drawable.current_location);
                homeAddressesAdapter.setSelected(0);
                workAddressesAdapter.setSelected(-1);
                otherAddressesAdapter.setSelected(-1);
                break;
            case Values.REQUEST_ADDRESS_WORK:
                placesAdapter.setSelected(-1);
                currentLocationContainer.setStrokeWidth(1);
                currentLocationContainer.setStrokeColor(Color.parseColor("#BEBEBE"));
                currentLocationIcon.setImageResource(R.drawable.current_location);
                homeAddressesAdapter.setSelected(-1);
                workAddressesAdapter.setSelected(0);
                otherAddressesAdapter.setSelected(-1);
                break;*/
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
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent();
        intent.putExtra("addresses", true);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void setPresenter(AddressesContract.Presenter presenter) {
        addressesPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
    }

    @Override
    public void setupViews() {

        //background.setBackgroundColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //save.setVisibility(View.VISIBLE);

        //int secondaryColor = Color.parseColor(CacheUtil.getInstance().getColorSecondary());

        //title.setTextColor(secondaryColor);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(editable.length() > 0) {
                    clear.setVisibility(View.VISIBLE);
                    /*new Handler().post(new Runnable() {
                        @Override
                        public void run() {*/
                            addressesPresenter.searchPlaces(placesClient, editable.toString());
                        /*}
                    });*/
                } else {
                    clear.setVisibility(View.GONE);
                    searchedPlacesLabel.setVisibility(View.GONE);
                    searchedPlaces.setVisibility(View.GONE);
                }
            }
        });

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                search.setText("");
            }
        });

        searchedPlaces.setLayoutManager(new LinearLayoutManager(this));
        placesAdapter = new PlacesRecyclerViewAdapter(this, addressesPresenter);
        searchedPlaces.setAdapter(placesAdapter);

        //currentLocationLabel.setTextColor(secondaryColor);

        currentLocationContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentLocationContainer.getStrokeWidth() == ViewUtil.dpToPx(AddressesActivity.this, 2)) {
                    addressesContinue.setVisibility(View.GONE);
                    currentLocationContainer.setStrokeWidth(1);
                    currentLocationContainer.setStrokeColor(Color.parseColor("#BEBEBE"));
                    currentLocationIcon.setImageResource(R.drawable.current_location);
                } else {
                    addressesContinue.setVisibility(View.VISIBLE);
                    placesAdapter.setSelected(-1);
                    currentLocationContainer.setStrokeWidth(ViewUtil.dpToPx(AddressesActivity.this, 2));
                    currentLocationContainer.setStrokeColor(ViewUtil.getColorMain(AddressesActivity.this));
                    currentLocationIcon.setImageResource(R.drawable.address_selected);
                    homeAddressesAdapter.setSelected(-1);
                    workAddressesAdapter.setSelected(-1);
                    otherAddressesAdapter.setSelected(-1);
                }
            }
        });

        addAddressContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //homeAddressesLabel.setTextColor(secondaryColor);

        homeAddressesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        homeAddressesAdapter = new AddressesRecyclerViewAdapter(this, addressesPresenter, true, false);
        homeAddressesRecyclerView.setAdapter(homeAddressesAdapter);

        //workAddressesLabel.setTextColor(secondaryColor);

        workAddressesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        workAddressesAdapter = new AddressesRecyclerViewAdapter(this, addressesPresenter, false, true);
        workAddressesRecyclerView.setAdapter(workAddressesAdapter);

        //otherAddressesLabel.setTextColor(secondaryColor);

        otherAddressesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        otherAddressesAdapter = new AddressesRecyclerViewAdapter(this, addressesPresenter, false, false);
        otherAddressesRecyclerView.setAdapter(otherAddressesAdapter);

        //addressesContinue.setTextColor(Color.parseColor(CacheUtil.getInstance().getColorButtonText()));
        addressesContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showLoadingDialog();

                String latitude = null;
                String longitude = null;

                if(placesAdapter.getSelected() >= 0) {

                    addressesPresenter.getPlace(placesClient, placesAdapter.getSelected());

                } else {

                    if (currentLocationContainer.getStrokeWidth() == ViewUtil.dpToPx(AddressesActivity.this, 2)) {

                        LatLng latLng = addressesPresenter.getLocation();
                        if (latLng != null) {
                            latitude = String.valueOf(latLng.latitude);
                            longitude = String.valueOf(latLng.longitude);
                            Locale locale = new Locale("en", "us");
                            if(LocaleUtil.getInstance().isFrench()){
                                locale = new Locale("fr","fr");
                            }else if(LocaleUtil.getInstance().isArabic()){
                                locale = new Locale("ar", "sa");
                            }
                            addressesPresenter.getAddress(null, new Geocoder(AddressesActivity.this,locale), latitude, longitude);
                        } else {
                            showMessage(CacheUtil.getInstance().getTranslations(Text.SELECT_DELIVERY_ADDRESS));
                        }

                    } else if (homeAddressesAdapter.getSelected() >= 0) {

                        Address address = addressesPresenter.getAddressHome(homeAddressesAdapter.getSelected());
                        showLoadingDialog();
                        addressesPresenter.getStore(address);
                        //saveAddress(address);

                    } else if (workAddressesAdapter.getSelected() >= 0) {

                        Address address = addressesPresenter.getAddressWork(workAddressesAdapter.getSelected());
                        showLoadingDialog();
                        addressesPresenter.getStore(address);
                        //saveAddress(address);

                    } else if (otherAddressesAdapter.getSelected() >= 0) {

                        Address address = addressesPresenter.getAddressOther(otherAddressesAdapter.getSelected());
                        showLoadingDialog();
                        addressesPresenter.getStore(address);
                        //aveAddress(address);
                    }

                }


            }
        });

    }

    @Override
    public void setupFonts() {
        title.setTypeface(FontHandler.getSemiboldTypeface());
        search.setTypeface(FontHandler.getMediumTypeface());
        searchedPlacesLabel.setTypeface(FontHandler.getMediumTypeface());
        currentLocationLabel.setTypeface(FontHandler.getMediumTypeface());
        currentLocation.setTypeface(FontHandler.getMediumTypeface());
        addAddress.setTypeface(FontHandler.getMediumTypeface());
        homeAddressesLabel.setTypeface(FontHandler.getMediumTypeface());
        workAddressesLabel.setTypeface(FontHandler.getMediumTypeface());
        otherAddressesLabel.setTypeface(FontHandler.getMediumTypeface());
        addressesContinue.setTypeface(FontHandler.getMediumTypeface());
    }

    @Override
    public void setupTranslations() {
        title.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERY_ADDRESS));
        search.setHint(CacheUtil.getInstance().getTranslations(Text.TYPE_DELIVERY_LOCATION));
        searchedPlacesLabel.setText(CacheUtil.getInstance().getTranslations(Text.SEARCHED_PLACES));
        currentLocationLabel.setText(CacheUtil.getInstance().getTranslations(Text.CURRENT_LOCATION));
        addAddress.setText(CacheUtil.getInstance().getTranslations(Text.ADD_ADDRESS));
        homeAddressesLabel.setText(CacheUtil.getInstance().getTranslations(Text.HOME));
        workAddressesLabel.setText(CacheUtil.getInstance().getTranslations(Text.WORK));
        otherAddressesLabel.setText(CacheUtil.getInstance().getTranslations(Text.OTHER));
        addressesContinue.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_CONTINUE));
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
                            resolvableApiException.startResolutionForResult(AddressesActivity.this, Values.PERMISSION_LOCATION);

                        } catch (IntentSender.SendIntentException exception) {

                            exception.printStackTrace();

                        }

                    } else if(e.getStatusCode() == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                        showMessage("Settings change unavailable");
                    }

                }

            }
        });
    }

    @Override
    public void getLocation() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            addressesPresenter.getLocation(new FusedLocationProvider(this));
        } else {
            permissions = false;
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, Values.PERMISSION_LOCATION);
        }
    }

    @Override
    public void showLocation(LatLng latLng) {
        String address = addressesPresenter.getLocation(new Geocoder(this, LocaleUtil.getInstance().isEnglish() ? new Locale("en", "us") : new Locale("ar", "sa")), latLng);
        if(!address.isEmpty()) {
            currentLocation.setText(address);
            currentLocationLabel.setVisibility(View.VISIBLE);
            currentLocationContainer.setVisibility(View.VISIBLE);
        } else {
            currentLocationLabel.setVisibility(View.GONE);
            currentLocationContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void showPlaces(ArrayList<AutocompletePrediction> places) {
        if(places.size() > 0) {
            placesAdapter.notifyDataSetChanged();
            searchedPlacesLabel.setVisibility(View.VISIBLE);
            searchedPlaces.setVisibility(View.VISIBLE);
        } else {
            searchedPlacesLabel.setVisibility(View.GONE);
            searchedPlaces.setVisibility(View.GONE);
        }
    }

    @Override
    public void setPlace(int position, AutocompletePrediction place, boolean selected) {

    }

    @Override
    public void selectPlace(int position) {
        addressesContinue.setVisibility(View.VISIBLE);
        placesAdapter.setSelected(position);
        currentLocationContainer.setStrokeWidth(0);
        if(currentLocationContainer.getVisibility() == View.VISIBLE) {
            currentLocationContainer.setVisibility(View.GONE);
            currentLocationContainer.setVisibility(View.VISIBLE);
        }
        homeAddressesAdapter.setSelected(-1);
        workAddressesAdapter.setSelected(-1);
        otherAddressesAdapter.setSelected(-1);
    }

    @Override
    public void showAddresses(ArrayList<Address> homeAddresses, ArrayList<Address> workAddresses, ArrayList<Address> otherAddresses) {

        if(homeAddresses.size() > 0) {
            homeAddressesAdapter.notifyDataSetChanged();
            homeAddressesLabel.setVisibility(View.VISIBLE);
            homeAddressesRecyclerView.setVisibility(View.VISIBLE);
        } else {
            homeAddressesLabel.setVisibility(View.GONE);
            homeAddressesRecyclerView.setVisibility(View.GONE);
        }

        if(workAddresses.size() > 0) {
            workAddressesAdapter.notifyDataSetChanged();
            workAddressesLabel.setVisibility(View.VISIBLE);
            workAddressesRecyclerView.setVisibility(View.VISIBLE);
        } else {
            workAddressesLabel.setVisibility(View.GONE);
            workAddressesRecyclerView.setVisibility(View.GONE);
        }

        if(otherAddresses.size() > 0) {
            otherAddressesAdapter.notifyDataSetChanged();
            otherAddressesLabel.setVisibility(View.VISIBLE);
            otherAddressesRecyclerView.setVisibility(View.VISIBLE);
        } else {
            otherAddressesLabel.setVisibility(View.GONE);
            otherAddressesRecyclerView.setVisibility(View.GONE);
        }

        dismissDialogs();
    }

    @Override
    public void setAddress(Address address, boolean selected) {

    }

    @Override
    public void showPlace(Place place) {
        if(place != null) {
            addressesPresenter.getAddress(place, new Geocoder(AddressesActivity.this, LocaleUtil.getInstance().isEnglish() ? new Locale("en", "us") : new Locale("ar", "sa")), String.valueOf(place.getLatLng().latitude), String.valueOf(place.getLatLng().longitude));
        } else {
            showMessage(CacheUtil.getInstance().getTranslations(Text.UNABLE_TO_SAVE_ADDRESS));
        }
    }

    @Override
    public void selectAddressHome(int position) {
        addressesContinue.setVisibility(View.VISIBLE);
        placesAdapter.setSelected(-1);
        currentLocationContainer.setStrokeWidth(1);
        currentLocationContainer.setStrokeColor(Color.parseColor("#BEBEBE"));
        currentLocationIcon.setImageResource(R.drawable.current_location);
        if(currentLocationContainer.getVisibility() == View.VISIBLE) {
            currentLocationContainer.setVisibility(View.GONE);
            currentLocationContainer.setVisibility(View.VISIBLE);
        }
        homeAddressesAdapter.setSelected(position);
        workAddressesAdapter.setSelected(-1);
        otherAddressesAdapter.setSelected(-1);
    }

    @Override
    public void selectAddressWork(int position) {
        addressesContinue.setVisibility(View.VISIBLE);
        placesAdapter.setSelected(-1);
        currentLocationContainer.setStrokeWidth(1);
        currentLocationContainer.setStrokeColor(Color.parseColor("#BEBEBE"));
        currentLocationIcon.setImageResource(R.drawable.current_location);
        if(currentLocationContainer.getVisibility() == View.VISIBLE) {
            currentLocationContainer.setVisibility(View.GONE);
            currentLocationContainer.setVisibility(View.VISIBLE);
        }
        homeAddressesAdapter.setSelected(-1);
        workAddressesAdapter.setSelected(position);
        otherAddressesAdapter.setSelected(-1);
    }

    @Override
    public void selectAddressOther(int position) {
        addressesContinue.setVisibility(View.VISIBLE);
        placesAdapter.setSelected(-1);
        currentLocationContainer.setStrokeWidth(1);
        currentLocationContainer.setStrokeColor(Color.parseColor("#BEBEBE"));
        currentLocationIcon.setImageResource(R.drawable.current_location);
        if(currentLocationContainer.getVisibility() == View.VISIBLE) {
            currentLocationContainer.setVisibility(View.GONE);
            currentLocationContainer.setVisibility(View.VISIBLE);
        }
        homeAddressesAdapter.setSelected(-1);
        workAddressesAdapter.setSelected(-1);
        otherAddressesAdapter.setSelected(position);
    }

    @Override
    public void addAddress(Place place, android.location.Address address) {
        AddAddressDialogFragment addAddressDialogFragment = AddAddressDialogFragment.newInstance(new AddAddressDialogFragment.OnSubmit() {
            @Override
            public void onSubmit(Dialog dialog, String phoneNumber, String label, String note) {
                dialog.dismiss();
                showLoadingDialog();
                addressesPresenter.addAddress(place, address, phoneNumber, label, note);
            }
        }, null);
        addAddressDialogFragment.show(getSupportFragmentManager(), AddAddressDialogFragment.class.getSimpleName());
    }

    @Override
    public void updateAddress(Place place, Address address) {
        AddAddressDialogFragment addAddressDialogFragment = AddAddressDialogFragment.newInstance(new AddAddressDialogFragment.OnSubmit() {
            @Override
            public void onSubmit(Dialog dialog, String phoneNumber, String label, String note) {
                dialog.dismiss();
                showLoadingDialog();
                if(place != null) {
                    address.getAttributes().setLine1(place.getName());
                }
                address.getAttributes().setLabel(phoneNumber);
                address.getAttributes().setLabel(label);
                address.getAttributes().setInstructions(note);
                addressesPresenter.updateAddress(address);
            }
        }, null);
        addAddressDialogFragment.show(getSupportFragmentManager(), AddAddressDialogFragment.class.getSimpleName());
    }

    @Override
    public void saveAddress(Address address) {
        dismissDialogs();
        if(address != null) {
            CacheUtil.getInstance().setOrderType(OrderType.DELIVERY);
            CacheUtil.getInstance().setOrderAddress(address);
            HashMap<String, String> eventAttributes = new HashMap<>();
            eventAttributes.put("Delivery", address.getAttributes().getLine1());
            logEvent("OrderTypeSelection", eventAttributes, null, 0);
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();
        } else {
            showMessage(CacheUtil.getInstance().getTranslations(Text.UNABLE_TO_SAVE_ADDRESS));
        }

    }

    @Override
    public void showOrderType() {
        dismissDialogs();
        onBackPressed();
    }

    @Override
    public void showMessage(String message) {
        dismissDialogs();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
