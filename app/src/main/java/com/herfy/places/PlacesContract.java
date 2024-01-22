package com.herfy.places;

import android.location.Address;
import android.location.Geocoder;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.Place;
import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.herfy.location.FusedLocationProvider;

public interface PlacesContract {

    interface View extends BaseView<Presenter> {

        void initializeLocation();

        void getLocation();

        void showLocation(LatLng location, String address);

        void showAddAddress(Address address, Place place);

        void showAccount();

        void showMessage(String message);

    }

    interface Presenter extends BasePresenter {

        void setGeocoder(Geocoder geocoder);

        void getLocation(FusedLocationProvider fusedLocationProvider);

        void getAddress(LatLng latLng);

        void addAddress(String phoneNumber, String label, String note);

    }

}
