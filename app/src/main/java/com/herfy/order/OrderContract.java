package com.herfy.order;

import android.graphics.drawable.Drawable;
import android.location.Geocoder;

import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.tabs.TabLayout;
import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.herfy.location.FusedLocationProvider;
import com.herfy.order.viewholders.AddressViewHolder;
import com.herfy.order.viewholders.StoreViewHolder;
import com.herfy.order.viewholders.TimeLineViewHolder;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.Store;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public interface OrderContract {

    interface View extends BaseView<Presenter> {

        void showOrderTypes(Set<OrderType> orderTypes);

        void createOrderType(OrderType orderType);

        void selectOrderType(OrderType orderType);

        TabLayout.Tab createTab(String tag, String text, Drawable icon);

        void initializeLocation();

        void getLocation();

        void showLocation(boolean isPlace, LatLng latLng, String address);

        void showAddresses(ArrayList<Address> bookmarkedAddresses, ArrayList<Address> otherAddresses);

        void setAddress(Address address, boolean selected);

        void selectAddressHome(Address address, boolean redownloadMenu);

        void selectAddressWork(int position);

        void selectAddressOther(Address address, boolean redownloadMenu);

        void updateAddAddress(@Nullable Address updateAddress, @Nullable android.location.Address saveAddress);

        void showStores(ArrayList<Store> pickupStores, ArrayList<Store> curbsideStores);

        void setStore(Store store, boolean selected);

        void callStore(Store store);

        void selectStore(int position, Store store, boolean initialLoad);

        void showStore(boolean markerClicked, int position);

        void showStore(boolean open, Store store, String error);

        void showOrderType(OrderType orderType);

        void selectDelivery();

        void selectPickup();

        void selectDineIn();

        void selectCurbside();

        void saveAddress(String address);

        void saveAddress(Address address);

        void updateAddress(String address);

        void showUnavailableMenuItems();

        void deleteUnavailableItems();

        void showPhoneNumberError(String error);

        void showError(String error);

        void updateMap(OrderType orderType, LatLng startLocation, LatLng endLocation, LatLngBounds bounds);

        void showOrder(OrderDetails order);

        void showOrderStatuses(ArrayList<OrderStatus> orderStatuses);

        void setTimeLine(OrderStatus orderStatus);

        void showStatus(OrderStatus orderStatus);

        void showDriver(LatLng latLng);

        void updateDriver(LatLng latLng);

        void showMessage(String message);

        void showRatings(OrderStatus orderStatus, List<Rating> ratings);

        void showOrders();
        
        void enableConfirmButton(boolean enable, String storeName);

        void saveToCache(String key, String value);

        void preloadImage(String imageUrl, RequestOptions requestOptions);

        void diffMenuSaved(Address address);

        void showPromptChangeOrderType(Address address);

        void showMinimumDeliveryForAreaMessage(String error);

    }

    interface Presenter extends BasePresenter {

        void getOrderTypes();

        void setGeocoder(Geocoder geocoder);

        void getLocation(FusedLocationProvider fusedLocationProvider);

        void getAddresses(boolean fromSave);

        void onBindAddressItemViewAtPosition(int position, AddressViewHolder addressViewHolder, boolean home, boolean work, boolean selected);

        int getAddressesCount();

        int getAddressesCount(boolean home, boolean work);

        void selectAddress(int position, Address address);

        Address getAddressHome(int position);

        Address getAddressWork(int position);

        Address getAddressOther(int position);

        ArrayList<Store> getPickupCurbsideStores();

        ArrayList<Store> getDineInStores();

        void getStores(int page);

        void getDistances();

        void onBindStoreViewAtPosition(boolean isDineIn, int position, StoreViewHolder storeViewHolder, boolean selected);

        int getStoresCount(boolean isDineIn);

        void callStore(boolean isDineIn, int position);

        void callStore();

        void selectStore(boolean isDineIn, int position);

        void continueStore(boolean isDineIn, int position, Store store);

        void setOrderType(OrderType orderType);

        android.location.Address getAddress();

        String getAddress(LatLng latLng);

        android.location.Address getCurrentAddress();

        void updateAddAddress();

        void deleteAddress(Address address);

        void getStoreOpen(Store store, Address selectedAddress);

        void checkUnavailableMenuItems();

        String getUnavailableMenuItemsMessage();

        ArrayList<MenuItem> getUnavailableMenuItems();

        void deleteUnavailableItems();

        void setOrderType();

        void setAddress(android.location.Address address);

        boolean validateUpdateAddAddress(String phoneNumber);

        void addAddress(String phoneNumber, String label, String note);

        void updateAddress(boolean fromSave, Address address, boolean isExistingAddress);

        void bookmarkAddress(Address address);

        void getOrder(String id);

        void getStore(Address address);

        int getStorePosition(boolean isDineIn, String id);

        Store getStore(boolean isDineIn, int position);

        void getStoreDetails();

        void getLocation(Store store);

        void getDriver();

        void getDriverBearing();

        void unsubscribeAbly();

        void getOrderStatuses();

        void onBindTimeLineViewAtPosition(int position, TimeLineViewHolder timeLineViewHolder);

        int getTimeLineCount();

        void cancelOrder();

        void getOrderFeedbackTopics();

        void getOrderFeedbacks();

        List<Rating> getRatings();

        void sendOrderFeedback(List<Rating> ratings);

        void removeMukafaaRedemption();

        void removeCurbside();

        void getDiffMenuCdn(Address address);


        void parseMenu(JSONObject data, Address address);

        void setSelectedAddress(@NotNull Address address);
    }

}
