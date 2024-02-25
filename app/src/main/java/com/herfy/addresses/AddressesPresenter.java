package com.herfy.addresses;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.herfy.addresses.viewholders.AddressViewHolder;
import com.herfy.addresses.viewholders.PlaceViewHolder;
import com.herfy.utils.constants.Text;
import com.herfy.location.FusedLocationProvider;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.BuildUtil;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.calls.LocationApiCall;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.skylinedynamics.solosdk.api.models.requestbodies.CreateCustomerAddressBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.UpdateCustomerAddressBody;
import com.herfy.util.AppLogger;
import com.herfy.utils.analytics.MixpanelUtil;
import com.herfy.utils.handler.ErrorHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AddressesPresenter implements AddressesContract.Presenter, FusedLocationProvider.LocationResultCallback {

    private final AddressesContract.View addressesView;

    private FusedLocationProvider fusedLocationProvider;

    private ArrayList<AutocompletePrediction> places = new ArrayList<>();

    private ArrayList<com.skylinedynamics.solosdk.api.models.objects.Address> homeAddresses = new ArrayList<>();
    private ArrayList<com.skylinedynamics.solosdk.api.models.objects.Address> workAddresses = new ArrayList<>();
    private ArrayList<com.skylinedynamics.solosdk.api.models.objects.Address> otherAddresses = new ArrayList<>();

    private Address currentAddress;
    private LatLng currentLocationLatLng;
    private String currentLocation = "";

    private String placeId = "";

    public AddressesPresenter(AddressesContract.View addressesView) {
        this.addressesView = addressesView;
        this.addressesView.setPresenter(this);
    }

    @Override
    public void start() {
        addressesView.setupViews();
        addressesView.setupFonts();
        addressesView.setupTranslations();
    }

    @Override
    public void getNewFusedLocation(Location location) {

        addressesView.showLocation(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    @Override
    public void getLocation(FusedLocationProvider fusedLocationProvider) {

        if (this.fusedLocationProvider != null) {
            this.fusedLocationProvider.disconnect();
        }

        this.fusedLocationProvider = fusedLocationProvider;

        this.fusedLocationProvider.setLocationResultCallback(this);

        this.fusedLocationProvider.connect();

    }

    @Override
    public String getLocation(Geocoder geocoder, LatLng latLng) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            Address address = (addresses.isEmpty() ? null : addresses.get(0));
            if (address != null) {
                currentAddress = address;
                currentLocationLatLng = latLng;
                currentLocation = address.getAddressLine(0);
            } else {
                currentAddress = null;
                currentLocationLatLng = null;
                currentLocation = "";
            }
        } catch (IOException e) {
            e.printStackTrace();
            currentLocationLatLng = null;
            currentLocation = "";
        }
        return currentLocation;
    }

    @Override
    public LatLng getLocation() {
        if (currentLocationLatLng != null) {
            return currentLocationLatLng;
        }
        return null;
    }

    @Override
    public void searchPlaces(PlacesClient placesClient, String query) {

        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(query)
                .build();

        AsyncTask<Void, String, Void> asyncTask = new AsyncTask<Void, String, Void>() {

            @Override
            protected Void doInBackground(Void... params) {

                places.clear();

                Task<FindAutocompletePredictionsResponse> autocompletePredictions = placesClient.findAutocompletePredictions(request);

                try {
                    Tasks.await(autocompletePredictions, 60, TimeUnit.SECONDS);
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    e.printStackTrace();
                }

                if (autocompletePredictions.isSuccessful()) {
                    FindAutocompletePredictionsResponse findAutocompletePredictionsResponse = autocompletePredictions.getResult();
                    if (findAutocompletePredictionsResponse != null) {
                        places.addAll(findAutocompletePredictionsResponse.getAutocompletePredictions());
                    }

                }

                return null;
            }

            @Override
            protected void onPostExecute(Void results) {
                super.onPostExecute(results);
                addressesView.showPlaces(places);
            }
        };

        asyncTask.execute();

    }

    @Override
    public void onBindPlaceItemViewAtPosition(int position, PlaceViewHolder placeViewHolder, boolean selected) {
        placeViewHolder.setPlace(position, places.get(position), selected);
    }

    @Override
    public int getPlacesCount() {
        return places.size();
    }

    @Override
    public void selectPlace(int position) {
        if (position > 0) {
            placeId = places.get(position).getPlaceId();
        } else {
            placeId = "";
        }
        addressesView.selectPlace(position);

    }

    @Override
    public void getAddresses(boolean fromSave) {
        CustomersApiCall.getInstance().getCustomerAddresses(AuthUtil.getInstance().getCustomer().getId(), "1", new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    JSONArray array = object.getJSONArray("data");
                    com.skylinedynamics.solosdk.api.models.objects.Address latestAddress = null;
                    homeAddresses = new ArrayList<>();
                    workAddresses = new ArrayList<>();
                    otherAddresses = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        com.skylinedynamics.solosdk.api.models.objects.Address address = com.skylinedynamics.solosdk.api.models.objects.Address.parseAddress(array.getJSONObject(i));
                        if (i == 0) {
                            latestAddress = address;
                        }
                        String label = address.getAttributes().getLabel();
                        if (label.equalsIgnoreCase("Home") || label.equalsIgnoreCase("الصفحة الرئيسية")) {
                            homeAddresses.add(address);
                        } else if (label.equalsIgnoreCase("Work") || label.equalsIgnoreCase("عمل")) {
                            workAddresses.add(address);
                        } else {
                            otherAddresses.add(address);
                        }
                    }

                    if (!fromSave) {
                        addressesView.showAddresses(homeAddresses, workAddresses, otherAddresses);
                    } else {
                        getStore(latestAddress);
                        //addressesView.saveAddress(latestAddress);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e.getMessage());
                }

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                homeAddresses = new ArrayList<>();
                workAddresses = new ArrayList<>();
                otherAddresses = new ArrayList<>();
                addressesView.showAddresses(homeAddresses, workAddresses, otherAddresses);
                addressesView.showMessage(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(Text.UNABLE_TO_SAVE_ADDRESS)));
            }
        });
    }

    @Override
    public void getStore(com.skylinedynamics.solosdk.api.models.objects.Address address) {
        if (AuthUtil.getInstance().isSignedIn()) {
            LocationApiCall.getInstance().getClosestStoreToAddress(AuthUtil.getInstance().getCustomer().getId(), address.getId(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {

                    if (o instanceof JSONObject) {
                        try {
                            JSONObject jsonObject = (JSONObject) o;
                            JSONObject data = jsonObject.getJSONObject("data");

                            CacheUtil.getInstance().setOrderStore(Store.parseStore(data));

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    addressesView.saveAddress(address);
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    addressesView.saveAddress(address);
                }
            });
        } else {
            addressesView.saveAddress(address);
        }
    }

    @Override
    public void onBindAddressItemViewAtPosition(int position, AddressViewHolder addressViewHolder, boolean home, boolean work, boolean selected) {
        if (home) {
            addressViewHolder.setAddress(homeAddresses.get(position), selected);
        } else if (work) {
            addressViewHolder.setAddress(workAddresses.get(position), selected);
        } else {
            addressViewHolder.setAddress(otherAddresses.get(position), selected);
        }
    }

    @Override
    public int getAddressesCount(boolean home, boolean work) {
        if (home) {
            return homeAddresses.size();
        } else if (work) {
            return workAddresses.size();
        }
        return otherAddresses.size();
    }

    @Override
    public void selectAddress(int position, com.skylinedynamics.solosdk.api.models.objects.Address address) {
        String label = address.getAttributes().getLabel();

        if (label.equalsIgnoreCase("Home") || label.equalsIgnoreCase("الصفحة الرئيسية")) {
            addressesView.selectAddressHome(position);
        } else if (label.equalsIgnoreCase("Work") || label.equalsIgnoreCase("عمل")) {
            addressesView.selectAddressWork(position);
        } else {
            addressesView.selectAddressOther(position);
        }

    }

    @Override
    public void saveAddress(boolean home) {
        if (currentAddress != null && currentLocationLatLng != null) {
            CreateCustomerAddressBody createCustomerAddressBody = new CreateCustomerAddressBody("", home ? "Home" : "Work", currentAddress.getCountryName(), "",
                    "", "", "", currentAddress.getAddressLine(0), currentLocationLatLng.latitude, currentLocationLatLng.longitude,
                    AuthUtil.getInstance().getCustomer().getAttributes().getMobile(), "", "");
            //For Burgerizzr
            if (BuildUtil.soloAppKey().equalsIgnoreCase("SeBwUS2G9SEnkwty")) {
                String mobile = CacheUtil.getInstance().getConcept().getDialingCode() + AuthUtil.getInstance().getCustomer().getAttributes().getMobile().substring(1);
                createCustomerAddressBody = new CreateCustomerAddressBody("", home ? "Home" : "Work", currentAddress.getCountryName(), "",
                        "", "", "", currentAddress.getAddressLine(0), currentLocationLatLng.latitude, currentLocationLatLng.longitude,
                        mobile, "", "");
            }
            CustomersApiCall.getInstance().addCustomerAddress(createCustomerAddressBody, AuthUtil.getInstance().getCustomer().getId(), "1", new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    getAddresses(false);
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    addressesView.showMessage(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.UNABLE_TO_SAVE_ADDRESS),null));
                }
            });
        } else {
            addressesView.showMessage(CacheUtil.getInstance().getTranslations(Text.UNABLE_TO_SAVE_ADDRESS));
        }
    }

    @Override
    public void getPlace(PlacesClient placesClient, int position) {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);

        FetchPlaceRequest request = FetchPlaceRequest.newInstance(places.get(position).getPlaceId(), placeFields);

        placesClient.fetchPlace(request).addOnSuccessListener(
                (response) -> {
                    addressesView.showPlace(response.getPlace());

                }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                int statusCode = apiException.getStatusCode();
                AppLogger.error("Place not found: " + statusCode + " " + exception.getMessage());
                addressesView.showMessage("Place not found: " + statusCode + " " + exception.getMessage());
            }
        });
    }

    @Override
    public com.skylinedynamics.solosdk.api.models.objects.Address getAddressHome(int position) {
        if (position < homeAddresses.size()) {
            return homeAddresses.get(position);
        }
        return null;
    }

    @Override
    public com.skylinedynamics.solosdk.api.models.objects.Address getAddressWork(int position) {
        if (position < workAddresses.size()) {
            return workAddresses.get(position);
        }
        return null;
    }

    @Override
    public com.skylinedynamics.solosdk.api.models.objects.Address getAddressOther(int position) {
        if (position < otherAddresses.size()) {
            return otherAddresses.get(position);
        }
        return null;
    }

    @Override
    public void getAddress(Place place, Geocoder geocoder, String latitude, String longitude) {

        if (AuthUtil.getInstance().isSignedIn()) {

            com.skylinedynamics.solosdk.api.models.objects.Address updateAddress = null;

            for (com.skylinedynamics.solosdk.api.models.objects.Address address : homeAddresses) {
                if (address.getAttributes().getLat().equals(latitude) && address.getAttributes().getLongt().equals(longitude)) {
                    updateAddress = address;
                    break;
                }
            }

            if (updateAddress == null) {
                for (com.skylinedynamics.solosdk.api.models.objects.Address address : workAddresses) {
                    if (address.getAttributes().getLat().equals(latitude) && address.getAttributes().getLongt().equals(longitude)) {
                        updateAddress = address;
                        break;
                    }
                }
            }

            if (updateAddress == null) {
                for (com.skylinedynamics.solosdk.api.models.objects.Address address : otherAddresses) {
                    if (address.getAttributes().getLat().equals(latitude) && address.getAttributes().getLongt().equals(longitude)) {
                        updateAddress = address;
                        break;
                    }
                }
            }

            if (updateAddress != null) {
                addressesView.updateAddress(place, updateAddress);
            } else {
                try {
                    List<Address> addresses = geocoder.getFromLocation(Double.valueOf(latitude), Double.valueOf(longitude), 1);
                    Address address = (addresses.isEmpty() ? null : addresses.get(0));
                    if (address != null) {
                        addressesView.addAddress(place, address);
                    } else {
                        addressesView.showMessage(CacheUtil.getInstance().getTranslations(Text.UNABLE_TO_SAVE_ADDRESS));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    addressesView.showMessage(CacheUtil.getInstance().getTranslations(Text.UNABLE_TO_SAVE_ADDRESS));
                }
            }

        } else {
            addressesView.showMessage(CacheUtil.getInstance().getTranslations(Text.UNABLE_TO_SAVE_ADDRESS));
        }

    }

    @Override
    public void addAddress(Place place, Address address, String phoneNumber, String label, String note) {

        CreateCustomerAddressBody createCustomerAddressBody;

        if (place != null) {
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
                MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_ADD_ADDRESS);
                getAddresses(true);
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                addressesView.showMessage(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.UNABLE_TO_SAVE_ADDRESS),null));
            }
        });

    }

    @Override
    public void updateAddress(com.skylinedynamics.solosdk.api.models.objects.Address address) {
        if (AuthUtil.getInstance().isSignedIn()) {
            UpdateCustomerAddressBody updateCustomerAddressBody = new UpdateCustomerAddressBody(address.getAttributes().getLabel(), address.getAttributes().getTelephone(), address.getAttributes().getInstructions());
            CustomersApiCall.getInstance().updateCustomerAddress(AuthUtil.getInstance().getCustomer().getId(), address.getId(), updateCustomerAddressBody, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    getStore(address);
                    //addressesView.saveAddress(address);
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    addressesView.showMessage(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.UNABLE_TO_SAVE_ADDRESS),null));
                }
            });
        } else {
            addressesView.showMessage(CacheUtil.getInstance().getTranslations(Text.UNABLE_TO_SAVE_ADDRESS));
        }
    }


}
