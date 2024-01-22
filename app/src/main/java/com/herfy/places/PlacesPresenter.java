package com.herfy.places;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.Place;
import com.herfy.constants.Text;
import com.herfy.location.FusedLocationProvider;
import com.herfy.util.AppLogger;
import com.herfy.util.AuthUtil;
import com.herfy.util.CacheUtil;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.models.requestbodies.CreateCustomerAddressBody;
import com.herfy.util.ErrorHandler;

import java.io.IOException;
import java.util.List;

public class PlacesPresenter implements PlacesContract.Presenter, FusedLocationProvider.LocationResultCallback {

    private final PlacesContract.View placesView;

    //private PlacesClient placesClient;

    private Geocoder geocoder;

    private FusedLocationProvider fusedLocationProvider;

    private Address address;
    private Place place;

    public PlacesPresenter(PlacesContract.View placesView) {
        this.placesView = placesView;
        this.placesView.setPresenter(this);
    }

    @Override
    public void start() {

        placesView.setupViews();
        placesView.setupFonts();
        placesView.setupTranslations();

    }

    @Override
    public void setGeocoder(Geocoder geocoder) {
        this.geocoder = geocoder;
    }

    @Override
    public void getLocation(FusedLocationProvider fusedLocationProvider) {
        if(this.fusedLocationProvider != null) {
            this.fusedLocationProvider.disconnect();
        }

        this.fusedLocationProvider = fusedLocationProvider;

        this.fusedLocationProvider.setLocationResultCallback(this);

        this.fusedLocationProvider.connect();

    }

    @Override
    public void getAddress(LatLng latLng) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            address = (addresses.isEmpty() ? null : addresses.get(0));
            place = null;
            placesView.showLocation(latLng, address == null ? "" : address.getAddressLine(0));
        } catch (IOException e) {
            e.printStackTrace();
            placesView.showMessage(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
        }
    }

    @Override
    public void addAddress(String phoneNumber, String label, String note) {
        if(address == null && place == null) {
            placesView.showMessage(CacheUtil.getInstance().getTranslations(Text.UNABLE_TO_SAVE_ADDRESS));
        } else if(phoneNumber == null && label == null && note == null) {
            placesView.showAddAddress(address, place);
        } else {
            CreateCustomerAddressBody createCustomerAddressBody;

            if(place != null && place.getLatLng() != null) {
                createCustomerAddressBody = new CreateCustomerAddressBody("", label, "", "",
                        "", "", "", place.getName(), place.getLatLng().latitude, place.getLatLng().longitude,
                        phoneNumber, note, "");
            } else {
                createCustomerAddressBody = new CreateCustomerAddressBody("", label, "", "",
                        "", "", "", address.getAddressLine(0), address.getLatitude(), address.getLongitude(),
                        phoneNumber, note, "");
            }

            CustomersApiCall.getInstance().addCustomerAddress(createCustomerAddressBody, AuthUtil.getInstance().getCustomer().getId(), "1", new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    placesView.showAccount();
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    String detail = ErrorHandler.getErrorMessage(o , CacheUtil.getInstance().getTranslations(Text.UNABLE_TO_SAVE_ADDRESS), null);
                    placesView.showMessage(detail);
                }
            });
        }
    }

    @Override
    public void getNewFusedLocation(Location location) {
        fusedLocationProvider.disconnect();

        getAddress(new LatLng(location.getLatitude(), location.getLongitude()));

    }
}
