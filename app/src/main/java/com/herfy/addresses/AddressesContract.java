package com.herfy.addresses;

import android.location.Geocoder;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.herfy.addresses.viewholders.AddressViewHolder;
import com.herfy.addresses.viewholders.PlaceViewHolder;
import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.location.FusedLocationProvider;
import com.skylinedynamics.solosdk.api.models.objects.Address;

import java.util.ArrayList;

public interface AddressesContract {

    interface View extends BaseView<Presenter> {

        void initializeLocation();

        void getLocation();

        void showLocation(LatLng latLng);

        void showPlaces(ArrayList<AutocompletePrediction> places);

        void setPlace(int position, AutocompletePrediction place, boolean selected);

        void selectPlace(int position);

        void showAddresses(ArrayList<Address> homeAddresses, ArrayList<Address> workAddresses, ArrayList<Address> otherAddresses);

        void setAddress(Address address, boolean selected);

        void showPlace(Place place);

        void selectAddressHome(int position);

        void selectAddressWork(int position);

        void selectAddressOther(int position);

        void addAddress(Place place, android.location.Address address);

        void updateAddress(Place place, Address address);

        void saveAddress(Address address);

        void showOrderType();

        void showMessage(String message);

    }

    interface Presenter extends BasePresenter {

        void getLocation(FusedLocationProvider fusedLocationProvider);

        String getLocation(Geocoder geocoder, LatLng latLng);

        LatLng getLocation();

        void searchPlaces(PlacesClient placesClient, String query);

        void onBindPlaceItemViewAtPosition(int position, PlaceViewHolder placeViewHolder, boolean selected);

        int getPlacesCount();

        void selectPlace(int position);

        void getAddresses(boolean fromSave);

        void getStore(Address address);

        void onBindAddressItemViewAtPosition(int position, AddressViewHolder addressViewHolder, boolean home, boolean work, boolean selected);

        int getAddressesCount(boolean home, boolean work);

        void selectAddress(int position, Address address);

        void saveAddress(boolean home);

        void getPlace(PlacesClient placesClient, int position);

        Address getAddressHome(int position);

        Address getAddressWork(int position);

        Address getAddressOther(int position);

        void getAddress(Place place, Geocoder geocoder, String latitude, String longitude);

        void addAddress(Place place, android.location.Address address, String phoneNumber, String label, String note);

        void updateAddress(Address address);


    }

}
