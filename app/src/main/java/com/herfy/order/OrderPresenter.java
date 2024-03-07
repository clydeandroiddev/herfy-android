package com.herfy.order;

import android.annotation.SuppressLint;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import androidx.annotation.NonNull;

import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.gson.reflect.TypeToken;
import com.herfy.BuildConfig;
import com.herfy.ably.AblyUtil;
import com.herfy.database.AppDatabase;
import com.herfy.database.entities.AllergenDb;
import com.herfy.database.entities.FacilityEntity;
import com.herfy.database.entities.IngredientDb;
import com.herfy.database.entities.MenuCategoryEntity;
import com.herfy.database.entities.MenuItemDb;
import com.herfy.database.entities.MenuItemEntity;
import com.herfy.database.entities.ModifierGroupDb;
import com.herfy.database.entities.ModifierItemDb;
import com.herfy.database.entities.StoreDb;
import com.herfy.database.responses.MenuCategoryItemResponse;
import com.herfy.model.SupportedCountry;
import com.herfy.order.viewholders.AddressViewHolder;
import com.herfy.order.viewholders.StoreViewHolder;
import com.herfy.order.viewholders.TimeLineViewHolder;
import com.herfy.util.AppLogger;
import com.herfy.util.StringUtil;
import com.herfy.utils.analytics.MixpanelUtil;
import com.herfy.utils.constants.Text;
import com.herfy.utils.constants.Values;
import com.herfy.utils.handler.ErrorHandler;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.BuildUtil;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.utils.menu.MenuUtil;
import com.location.FusedLocationProvider;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.ApiRequestWithHeadersListener;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.calls.LocationApiCall;
import com.skylinedynamics.solosdk.api.calls.MenuApiCall;
import com.skylinedynamics.solosdk.api.calls.OrdersApiCall;
import com.skylinedynamics.solosdk.api.calls.UtilitiesApiCall;
import com.skylinedynamics.solosdk.api.models.Pagination;
import com.skylinedynamics.solosdk.api.models.objects.Allergen;
import com.skylinedynamics.solosdk.api.models.objects.AllergenAttributes;
import com.skylinedynamics.solosdk.api.models.objects.Customer;
import com.skylinedynamics.solosdk.api.models.objects.Employee;
import com.skylinedynamics.solosdk.api.models.objects.Facility;
import com.skylinedynamics.solosdk.api.models.objects.Favorite;
import com.skylinedynamics.solosdk.api.models.objects.Ingredient;
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupType;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderData;
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails;
import com.skylinedynamics.solosdk.api.models.objects.OrderFeedback;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatusType;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.skylinedynamics.solosdk.api.models.objects.Translated;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfig;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfigRule;
import com.skylinedynamics.solosdk.api.models.objects.store.DeliveryArea;
import com.skylinedynamics.solosdk.api.models.requestbodies.CreateCustomerAddressBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.OrderFeedbackBody;
import com.skylinedynamics.solosdk.api.models.requestbodies.UpdateCustomerAddressBody;
import com.skylinedynamics.solosdk.util.GsonHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.ably.lib.types.Message;
import io.reactivex.Observable;
import kotlin.Unit;
import okhttp3.Headers;

public class OrderPresenter implements OrderContract.Presenter, FusedLocationProvider.LocationResultCallback {

    private final OrderContract.View orderView;

    private Geocoder geocoder;

    private FusedLocationProvider fusedLocationProvider;

    private Location location;

    private Address address;
    private Place place;

    private ArrayList<Store> dineInStores = new ArrayList<>();
    private ArrayList<Store> pickupCurbsideStores = new ArrayList<>();

    //private ArrayList<com.skylinedynamics.solosdk.api.models.objects.Address> homeAddresses = new ArrayList<>();
    //private ArrayList<com.skylinedynamics.solosdk.api.models.objects.Address> workAddresses = new ArrayList<>();
    private ArrayList<com.skylinedynamics.solosdk.api.models.objects.Address> bookmarkedAddresses = new ArrayList<>();
    private ArrayList<com.skylinedynamics.solosdk.api.models.objects.Address> otherAddresses = new ArrayList<>();

    private Store callStore = new Store();

    private Store store = null;
    private com.skylinedynamics.solosdk.api.models.objects.Address selectedAddress = null;

    private HashMap<String, MenuItem> unavailableMenuItems = new HashMap<>();

    private OrderDetails order = null;
    private OrderStatus orderStatus = null;
    private ArrayList<OrderStatus> orderStatuses = new ArrayList<>();
    private Employee driver = null;
    private boolean isCategoryUnavailable;
    private int orderTypeCount = 0;

    private List<Rating> ratings = new ArrayList<>();
    public boolean hasLocation = false;

    public OrderPresenter(OrderContract.View orderView) {
        this.orderView = orderView;
        this.orderView.setPresenter(this);
    }

    @Override
    public void start() {
        orderTypeCount = CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().size();
        orderView.setupViews();
        orderView.setupFonts();
        orderView.setupTranslations();
    }

    @Override
    public void getOrderTypes() {
        Set<OrderType> orderTypes = CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes();
        orderTypes.remove(OrderType.CURBSIDE);
        orderView.showOrderTypes(orderTypes);
    }

    @Override
    public void setGeocoder(Geocoder geocoder) {
        this.geocoder = geocoder;
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
    public void getAddresses(boolean fromSave) {
        bookmarkedAddresses = new ArrayList<>();
        otherAddresses = new ArrayList<>();
        if (AuthUtil.getInstance().isSignedIn()) {
            CustomersApiCall.getInstance().getCustomerAddresses(AuthUtil.getInstance().getCustomer().getId(), "1", new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {

                    try {
                        JSONObject object = new JSONObject(o.toString());
                        JSONArray array = object.getJSONArray("data");
                        com.skylinedynamics.solosdk.api.models.objects.Address latestAddress = null;
                        for (int i = 0; i < array.length(); i++) {
                            com.skylinedynamics.solosdk.api.models.objects.Address address = com.skylinedynamics.solosdk.api.models.objects.Address.parseAddress(array.getJSONObject(i));
                            if (i == 0) {
                                latestAddress = address;
                            }
                            bookmarkedAddresses.add(address);
                        }

                        if (!fromSave) {
                            orderView.showAddresses(bookmarkedAddresses, otherAddresses);
                        } else if (latestAddress != null) {
                            CacheUtil.getInstance().setOrderType(OrderType.DELIVERY);
                            CacheUtil.getInstance().setOrderAddress(latestAddress);
                            orderView.saveAddress(latestAddress);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.onError(e);
                    }

                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    orderView.showAddresses(bookmarkedAddresses, otherAddresses);
                }
            });
        } else {
            orderView.showAddresses(bookmarkedAddresses, otherAddresses);
        }

    }

    @Override
    public void onBindAddressItemViewAtPosition(int position, AddressViewHolder addressViewHolder, boolean home, boolean work, boolean selected) {
        if (home) {
            addressViewHolder.setAddress(bookmarkedAddresses.get(position), selected);
        } else if (work) {
            addressViewHolder.setAddress(bookmarkedAddresses.get(position), selected);
        } else {
            addressViewHolder.setAddress(otherAddresses.get(position), selected);
        }
    }

    @Override
    public int getAddressesCount() {
        //return homeAddresses.size() + workAddresses.size() + otherAddresses.size();
        return bookmarkedAddresses.size() + otherAddresses.size();
    }

    @Override
    public int getAddressesCount(boolean home, boolean work) {
        if (home) {
            return bookmarkedAddresses.size();
        } else if (work) {
            return bookmarkedAddresses.size();
        }
        return otherAddresses.size();
    }

    @Override
    public void selectAddress(int position, com.skylinedynamics.solosdk.api.models.objects.Address address) {
        OrderType cachedOrderType = CacheUtil.getInstance().getOrderType();
        ArrayList<MenuItem> cartItems = CacheUtil.getInstance().getCart();
        boolean isMenuSeparate = CacheUtil.getInstance().isSeparatedMenus();
        boolean redownloadMenu = true;
        if (cachedOrderType != OrderType.NONE) {
            redownloadMenu = false;
        }
        if (isMenuSeparate && cachedOrderType != OrderType.DELIVERY) {
            if (cartItems.size() > 0) {
                orderView.showPromptChangeOrderType(address);
                return;
            } else {
                redownloadMenu = true;
            }
        }

        CacheUtil.getInstance().setOrderType(OrderType.DELIVERY);
        selectedAddress = address;
        String label = address.getAttributes().getLabel();
        if (label != null && !label.equalsIgnoreCase("null") && !label.isEmpty()) {
            orderView.selectAddressHome(address, redownloadMenu);
        } else {
            orderView.selectAddressOther(address, redownloadMenu);
        }
    }

    @Override
    public com.skylinedynamics.solosdk.api.models.objects.Address getAddressHome(int position) {
        if (position < bookmarkedAddresses.size()) {
            return bookmarkedAddresses.get(position);
        }
        return null;
    }

    @Override
    public com.skylinedynamics.solosdk.api.models.objects.Address getAddressWork(int position) {
        /*if(position < workAddresses.size()) {
            return workAddresses.get(position);
        }*/
        if (position < bookmarkedAddresses.size()) {
            return bookmarkedAddresses.get(position);
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
    public ArrayList<Store> getPickupCurbsideStores() {
        return pickupCurbsideStores;
    }

    @Override
    public ArrayList<Store> getDineInStores() {
        return dineInStores;
    }

    @Override
    public void getStores(int page) {

        String applicationId = CacheUtil.getInstance().getApplication().getId();
        AppDatabase appDatabase = AppDatabase.getInstance();

        if (page > 1 || !appDatabase.storeDao().hasByApplication(applicationId)) {

            LocationApiCall.getInstance().getStoreLocationsWithLatLng(String.valueOf(page), CacheUtil.getInstance().getOrderLatitude(), CacheUtil.getInstance().getOrderLongitude(), "20", new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    try {
                        JSONObject object = new JSONObject(o.toString());
                        JSONArray array = object.getJSONArray("data");

                        Pagination pagination = Pagination.parse(object);
                        if (page == 1) {
                            dineInStores = new ArrayList<>();
                            pickupCurbsideStores = new ArrayList<>();
                        }
                        List<StoreDb> storeDbs = new ArrayList<>();
                        List<FacilityEntity> facilityEntities = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject storeObject = array.getJSONObject(i);
                            if (hasLocation) {
                                StoreDb storeDb = new StoreDb();
                                storeDb.id = storeObject.getString("id");
                                storeDb.applicationId = applicationId;
                                storeDb.data = storeObject.toString();
                                storeDbs.add(storeDb);
                            }
                            Store store = Store.parseStore(storeObject);
                            if (store.getAttributes().getPickupEnabled() == 1) {
                                dineInStores.add(store);
                            }
                            boolean curbsideEnabled = CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().contains(OrderType.CURBSIDE);
                            if (store.getAttributes().getPickupEnabled() == 1 || (curbsideEnabled && store.getAttributes().getCurbsideEnabled() == 1)) {
                                pickupCurbsideStores.add(store);
                            }
                        }

                        if (object.has("included") && !object.isNull("included")) {
                            JSONArray facilitiesArray = object.getJSONArray("included");
                            if (hasLocation) {
                                for (int i = 0; i < facilitiesArray.length(); i++) {
                                    JSONObject facilityObject = facilitiesArray.getJSONObject(i);
                                    FacilityEntity facilityEntity = new FacilityEntity();
                                    facilityEntity.id = facilityObject.getString("id");
                                    facilityEntity.applicationId = applicationId;
                                    facilityEntity.data = facilityObject.toString();
                                    facilityEntities.add(facilityEntity);
                                }
                            }
                            ArrayList<Facility> facilities = GsonHelper.gsonFinal().fromJson(facilitiesArray.toString(), new TypeToken<ArrayList<Facility>>() {
                            }.getType());
                            if (facilities.size() > 0) {
                                for (Store store : dineInStores) {
                                    for (String id : store.getFacilitiesIds()) {
                                        for (Facility facility : facilities) {
                                            if (id.equals(facility.getId())) {
                                                if (!store.getFacilities().contains(facility)) {
                                                    store.getFacilities().add(facility);
                                                }
                                            }
                                        }
                                    }
                                }
                                for (Store store : pickupCurbsideStores) {
                                    for (String id : store.getFacilitiesIds()) {
                                        for (Facility facility : facilities) {
                                            if (id.equals(facility.getId())) {
                                                if (store.getFacilities().contains(facility)) {
                                                    store.getFacilities().add(facility);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (hasLocation) {
                            Executor executor = Executors.newSingleThreadExecutor();
                            executor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if (page == 1) {
                                            appDatabase.storeDao().deleteByApplication(applicationId);
                                            appDatabase.facilityDao().deleteByApplication(applicationId);
                                        }
                                        appDatabase.storeDao().insertAll(storeDbs.toArray(new StoreDb[storeDbs.size()]));
                                        appDatabase.facilityDao().insertAll(facilityEntities.toArray(new FacilityEntity[facilityEntities.size()]));
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                    }
                                }
                            });
                        }

                        if (pagination.getTotalPages() > 1 && page < pagination.getTotalPages()) {
                            getStores(page + 1);
                        }
                        sortStoresByDistance();
                        orderView.showStores(dineInStores, pickupCurbsideStores);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        orderView.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                    }

                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    orderView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
                }
            });

        } else {

            Executor executor = Executors.newSingleThreadExecutor();

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        dineInStores = new ArrayList<>();
                        pickupCurbsideStores = new ArrayList<>();
                        List<StoreDb> storeDbs = appDatabase.storeDao().findAllByApplicationId(applicationId);
                        for (StoreDb storeDb : storeDbs) {
                            JSONObject storeObject = new JSONObject(storeDb.data);
                            Store store = Store.parseStore(storeObject);
                            if (store.getAttributes().getPickupEnabled() == 1) {
                                dineInStores.add(store);
                            }
                            boolean curbsideEnabled = CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().contains(OrderType.CURBSIDE);
                            if (store.getAttributes().getPickupEnabled() == 1 || (curbsideEnabled && store.getAttributes().getCurbsideEnabled() == 1)) {
                                pickupCurbsideStores.add(store);
                            }
                        }
                        List<Facility> facilities = new ArrayList<>();
                        List<FacilityEntity> facilityEntities = appDatabase.facilityDao().findAllByApplicationId(applicationId);
                        for (FacilityEntity facilityEntity : facilityEntities) {
                            Facility facility = GsonHelper.gsonFinal().fromJson(facilityEntity.data, Facility.class);
                            facilities.add(facility);
                        }
                        if (facilities.size() > 0) {
                            for (Store store : dineInStores) {
                                for (String id : store.getFacilitiesIds()) {
                                    for (Facility facility : facilities) {
                                        if (id.equals(facility.getId())) {
                                            if (!store.getFacilities().contains(facility)) {
                                                store.getFacilities().add(facility);
                                            }
                                        }
                                    }
                                }
                            }
                            for (Store store : pickupCurbsideStores) {
                                for (String id : store.getFacilitiesIds()) {
                                    for (Facility facility : facilities) {
                                        if (id.equals(facility.getId())) {
                                            if (store.getFacilities().contains(facility)) {
                                                store.getFacilities().add(facility);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    sortStoresByDistance();
                    orderView.showStores(dineInStores, pickupCurbsideStores);
                }
            });
        }

    }

    @Override
    public void addToCartFavorite(Favorite favorite) {
        addCartItem(favorite);
    }

    private void sortStoresByDistance() {

        Collections.sort(dineInStores, new Comparator<Store>() {
            @Override
            public int compare(Store store1, Store store2) {
                if (store1 != null && store1.getAttributes() != null && store1.getAttributes().getDistance() != null && store2 != null && store2.getAttributes() != null && store2.getAttributes().getDistance() != null) {
                    return store1.getAttributes().getDistance().compareTo(store2.getAttributes().getDistance());
                } else {
                    throw new IllegalStateException("One or more required objects or attributes are null.");

                }
            }
        });
        Collections.sort(pickupCurbsideStores, new Comparator<Store>() {
            @Override
            public int compare(Store store1, Store store2) {
                if (store1 != null && store1.getAttributes() != null && store1.getAttributes().getDistance() != null && store2 != null && store2.getAttributes() != null && store2.getAttributes().getDistance() != null) {
                    return store1.getAttributes().getDistance().compareTo(store2.getAttributes().getDistance());
                } else {
                    throw new IllegalStateException("One or more required objects or attributes are null.");
                }
            }

        });
    }


    private HashMap<String, Double> storeDistances = new HashMap<>();

    @Override
    public void getDistances() {
//        if (location != null) {
//            for (Store store : pickupStores) {
//
//                Location storeLocation = new Location("Store Location");
//                storeLocation.setLatitude(store.getAttributes().getLat());
//                storeLocation.setLongitude(store.getAttributes().getLng());
//
//                double distance = location.distanceTo(storeLocation);
//                store.getAttributes().setDistance(distance);
//
//                //distances.put(store.getId(), distance);
//
//            }
//
//            for (Store store : curbsideStores) {
//
//                Location storeLocation = new Location("Store Location");
//                storeLocation.setLatitude(store.getAttributes().getLat());
//                storeLocation.setLongitude(store.getAttributes().getLng());
//
//                double distance = location.distanceTo(storeLocation);
//                store.getAttributes().setDistance(distance);
//
//                //distances.put(store.getId(), distance);
//
//            }
//
//        }
//        Collections.sort(pickupStores, new Comparator<Store>() {
//            @Override
//            public int compare(Store store1, Store store2) {
//                return (int) Math.round(store1.getAttributes().getDistance()) - (int) Math.round(store2.getAttributes().getDistance());
//            }
//        });
//
//        Collections.sort(curbsideStores, new Comparator<Store>() {
//            @Override
//            public int compare(Store store1, Store store2) {
//                return (int) Math.round(store1.getAttributes().getDistance()) - (int) Math.round(store2.getAttributes().getDistance());
//            }
//        });
//
//        orderView.showStores(pickupStores, curbsideStores);

    }

    @Override
    public void onBindStoreViewAtPosition(boolean isDineIn, int position, StoreViewHolder storeViewHolder, boolean selected) {
        if (isDineIn) {
            if (position < dineInStores.size()) {
                if (pickupCurbsideStores.size() == 1 && orderTypeCount > 1) {
                    storeViewHolder.setStore(dineInStores.get(0), true);
                    orderView.enableConfirmButton(true, dineInStores.get(0).getAttributes().getName());
                } else {
                    storeViewHolder.setStore(dineInStores.get(position), selected);
                }
            } else {
                storeViewHolder.setStore(null, false);
            }
        } else if (position < pickupCurbsideStores.size()) {
            if (pickupCurbsideStores.size() == 1 && orderTypeCount > 1) {
                storeViewHolder.setStore(pickupCurbsideStores.get(0), true);
                orderView.enableConfirmButton(true, pickupCurbsideStores.get(0).getAttributes().getName());
            } else {
                storeViewHolder.setStore(pickupCurbsideStores.get(position), selected);
            }
        } else {
            storeViewHolder.setStore(null, false);
        }
    }

    @Override
    public int getStoresCount(boolean isDineIn) {
        return isDineIn ? dineInStores.size() : pickupCurbsideStores.size();
    }

    @Override
    public void callStore(boolean isDineIn, int position) {
        callStore = null;
        if (isDineIn) {
            callStore = dineInStores.get(position);
        } else {
            callStore = pickupCurbsideStores.get(position);
        }
        if (callStore != null) {
            orderView.callStore(callStore);
        }
    }

    @Override
    public void callStore() {
        if (callStore != null) {
            orderView.callStore(callStore);
        }
    }

    @Override
    public void selectStore(boolean isDineIn, int position) {
        if (position >= 0) {
            if (isDineIn) {
                if (position < dineInStores.size()) {
                    orderView.selectStore(position, dineInStores.get(position), false);
                }
            } else if (position < pickupCurbsideStores.size()) {
                orderView.selectStore(position, pickupCurbsideStores.get(position), false);
            }
        }
    }

    @Override
    public void continueStore(boolean isDineIn, int position, Store store) {

        selectedAddress = null;

        if (position == -1) {
            position = 0;
        }

        if (store != null) {
            this.store = store;
        } else if (isDineIn && position < dineInStores.size()) {
            this.store = dineInStores.get(position);
        } else if (position < pickupCurbsideStores.size()) {
            this.store = pickupCurbsideStores.get(position);
        }

        if (this.store != null) {
            checkUnavailableMenuItems();
        }

    }

    @Override
    public void setOrderType(OrderType orderType) {
        CacheUtil.getInstance().setOrderType(orderType);
        orderView.showOrderType(orderType);
    }

    @Override
    public Address getAddress() {
        return address;
    }

    @Override
    public void addCartItem(Favorite favorite) {
        MenuItem favoriteMenuItem = MenuUtil.getFavoriteMenuItem(favorite);
        HashMap<String, ModifierItem> favoriteModifiers = new HashMap<>();
        for (ModifierItem modifierItem : favoriteMenuItem.getModifierItems()) {
            favoriteModifiers.put(modifierItem.getId(), modifierItem);
        }

        HashMap<String, Ingredient> favoriteIngredients = new HashMap<>();
        for (Ingredient ingredient : favoriteMenuItem.getIngredients()) {
            favoriteIngredients.put(ingredient.getId(), ingredient);
        }

        MenuApiCall.getInstance().getAllItemDetails(CacheUtil.getInstance().getMenuId(), String.valueOf(favoriteMenuItem.getId()), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    String categoryId;
                    JSONObject object = new JSONObject(o.toString());
                    JSONArray dataArray = object.getJSONArray("data");

                    if (dataArray.length() > 0) {
                        MenuItem menuItem = GsonHelper.gsonFinal().fromJson(dataArray.getJSONObject(0).toString(), MenuItem.class);
                        menuItem.toCdn(CacheUtil.getOrderStoreId());

                        try {
                            menuItem.isCombo = dataArray.getJSONObject(0).getJSONObject("attributes").getBoolean("is-combo");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        categoryId = getMenuCategory(menuItem.getId());

                        if (menuItem.getAttributes().getEnabled() == 0) {
                            orderView.showError(CacheUtil.getInstance().getTranslations(Text.ITEM_NO_LONGER_AVAILABLE));
                        } else if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY && menuItem.getAttributes().getDisableForDelivery() == 1) {
                            orderView.showError(CacheUtil.getInstance().getTranslations(Text.ITEM_NOT_AVAILABLE_FOR_DELIVERY, "(x) is not available for Delivery.").replace("(x)", menuItem.getAttributes().getName()));
                        } else if (CacheUtil.getInstance().pickupTypes() && menuItem.getAttributes().getDisableForPickup() == 1) {
                            orderView.showError(CacheUtil.getInstance().getTranslations(Text.ITEM_NOT_AVAILABLE_FOR_PICKUP, "(x) is not available for Pickup.").replace("(x)", menuItem.getAttributes().getName()));
                        } else {
                            if (object.has("included") && !object.isNull("included")) {
                                JSONArray included = object.getJSONArray("included");
                                for (int i = 0; i < included.length(); i++) {
                                    Store store = Store.parseStore(included.getJSONObject(i));
                                    menuItem.getDisabledStores().put(store.getId(), store);
                                }
                            }
                            if (object.has("included") && !object.isNull("included")) {
                                JSONArray included = object.getJSONArray("included");
                                for (int i = 0; i < included.length(); i++) {
                                    JSONObject includedObject = included.getJSONObject(i);
                                    if (includedObject.has("id") && !includedObject.isNull("id") && includedObject.has("type") && !includedObject.isNull("type")) {
                                        String includedId = includedObject.getString("id");
                                        String includedType = includedObject.getString("type");

                                        if (includedType.equalsIgnoreCase("modifier-group")) {

                                            if (menuItem.getModifierGroupsIds().contains(includedId)) {

                                                ModifierGroup modifierGroup = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<ModifierGroup>() {
                                                }.getType());

                                                if (modifierGroup.getAttributes().getEnabled() == 1) {
                                                    String code = "";
                                                    if (modifierGroup.getAttributes().getCode() != null) {
                                                        code = modifierGroup.getAttributes().getCode().trim().toLowerCase();
                                                    }
                                                    String type = "";
                                                    if (modifierGroup.getAttributes().getType() != null) {
                                                        type = modifierGroup.getAttributes().getType().trim().toLowerCase();
                                                    }
                                                    String label = "";
                                                    if (modifierGroup.getAttributes().getLabel() != null) {
                                                        label = modifierGroup.getAttributes().getLabel().trim().toLowerCase();
                                                    }

                                                    if (code.equals("sizes") || type.equals("sizes") || label.equals("sizes")) {
                                                        modifierGroup.setType(ModifierGroupType.SIZES);
                                                        menuItem.setSizes(modifierGroup);
                                                    } else {
                                                        modifierGroup.setType(ModifierGroupType.OPTIONS);
                                                        List<ModifierGroup> items = new ArrayList<>(menuItem.getModifierGroups());
                                                        items.add(modifierGroup);
                                                        menuItem.setModifierGroups(items);
                                                    }

                                                }

                                            }

                                        } else if (includedType.equalsIgnoreCase("ingredient")) {
                                            if (menuItem.getIngredientsIds().contains(includedId)) {
                                                Ingredient ingredient = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<Ingredient>() {
                                                }.getType());
                                                if (favoriteIngredients.containsKey(ingredient.getId())) {
                                                    ingredient.getAttributes().setSelected(false);
                                                }
                                                if (ingredient.getAttributes().getEnabled() == 1) {
                                                    List<Ingredient> items = new ArrayList<>(menuItem.getIngredients());
                                                    items.add(ingredient);
                                                    menuItem.setIngredients(items);
                                                }
                                            }
                                        } else if (includedType.equalsIgnoreCase("allergen")) {
                                            if (menuItem.getAllergenIds().contains(includedId)) {
                                                Allergen allergen = Allergen.parse(includedObject);
                                                if (allergen.getEnabled() == 1) {
                                                    List<Allergen> items = new ArrayList<>(menuItem.getAllergens());
                                                    AllergenAttributes attributes = new AllergenAttributes();
                                                    Translated name = new Translated();
                                                    name.setArabic(allergen.getName());
                                                    name.setEnglish(allergen.getName());
                                                    name.setFrench(allergen.getName());
                                                    attributes.setName(name);
                                                    Translated icon = new Translated();
                                                    icon.setArabic(allergen.getIcon());
                                                    icon.setEnglish(allergen.getIcon());
                                                    icon.setFrench(allergen.getIcon());
                                                    attributes.setIcon(icon);
                                                    attributes.setEnabled(allergen.getEnabled());
                                                    allergen.setAttributes(attributes);
                                                    items.add(allergen);
                                                    menuItem.setAllergens(items);
                                                }
                                            }
                                        } else if (includedType.equalsIgnoreCase("location")) {
                                            if (menuItem.getLocationIds().contains(includedId)) {
                                                Store store = Store.parseStore(included.getJSONObject(i));
                                                menuItem.getDisabledStores().put(store.getId(), store);
                                            }
                                        }

                                    }
                                }

                                for (int i = 0; i < included.length(); i++) {

                                    JSONObject includedObject = included.getJSONObject(i);

                                    if (includedObject.has("id") && !includedObject.isNull("id") && includedObject.has("type") && !includedObject.isNull("type")) {
                                        String includedId = includedObject.getString("id");
                                        String includedType = includedObject.getString("type");

                                        if (includedType.equalsIgnoreCase("modifier")) {

                                            ModifierItem modifierItem = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<ModifierItem>() {
                                            }.getType());

                                            String name = modifierItem.getAttributes().getName();
                                            Translated translated = new Translated();
                                            translated.setEnglish(name);
                                            translated.setArabic(name);
                                            translated.setFrench(name);
                                            modifierItem.getAttributes().setTranslatedName(translated);

                                            if (favoriteModifiers.containsKey(modifierItem.getId())) {
                                                modifierItem.getAttributes().setSelected(true);
                                                modifierItem.getAttributes().setQuantity(favoriteModifiers.get(modifierItem.getId()).getAttributes().getQuantity());
                                            }

                                            if (modifierItem.getAttributes().getEnabled() == 1) {
                                                if (menuItem.getSizes().getModifierItemsIds().contains(includedId)) {
                                                    List<ModifierItem> items = new ArrayList<>(menuItem.getSizes().getModifierItems());
                                                    items.add(modifierItem);
                                                    menuItem.getSizes().setModifierItems(items);
                                                } else {
                                                    for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                                                        if (modifierGroup.getModifierItemsIds().contains(includedId)) {
                                                            List<ModifierItem> items = new ArrayList<>(modifierGroup.getModifierItems());
                                                            items.add(modifierItem);
                                                            modifierGroup.setModifierItems(items);
                                                        }
                                                    }
                                                }

                                            }

                                        }
                                    }
                                }

                                LinkedList<ModifierGroup> filteredModifierGroups = new LinkedList<>();

                                for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                                    if (modifierGroup.getModifierItems().size() > 0) {
                                        filteredModifierGroups.add(modifierGroup);
                                    }
                                }

                                Collections.sort(filteredModifierGroups, new Comparator<ModifierGroup>() {
                                    @Override
                                    public int compare(ModifierGroup modifierGroup1, ModifierGroup modifierGroup2) {
                                        return modifierGroup1.getAttributes().getDisplayOrder() - modifierGroup2.getAttributes().getDisplayOrder();
                                    }
                                });

                                menuItem.setModifierGroups(filteredModifierGroups);

                                if (menuItem.getSizes().getModifierItems().size() > 0) {
                                    menuItem.getSizes().getModifierItems().get(0).getAttributes().setSelected(true);
                                }

                                for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                                    if (modifierGroup.getAttributes().getMinimum() == 1 && modifierGroup.getModifierItems().size() > 0) {
                                        modifierGroup.getModifierItems().get(0).getAttributes().setSelected(true);
                                    }
                                }
                                VisibilityConfig visibilityConfig = menuItem.getSizes().getAttributes().getVisibilityConfig();
                                if (visibilityConfig != null) {
                                    List<VisibilityConfigRule> rules = visibilityConfig.getRules();
                                    if (rules != null && rules.size() > 0) {
                                        VisibilityConfigRule rule = rules.get(0);
                                        if (rule.getTargetType() != null) {
                                            if (rule.getTargetType().equalsIgnoreCase("modifier-group")) {
                                                for (ModifierGroup modGroup : menuItem.getModifierGroups()) {
                                                    if (!menuItem.getSizes().getAttributes().getId().equals(modGroup.getAttributes().getId()) && rule.getTargetId().equals(modGroup.getAttributes().getId())) {
                                                        if (rule.getTargetIs() != null && rule.getTargetIs().equalsIgnoreCase("selected")) {
                                                            int selected = 0;
                                                            for (ModifierItem modItem : modGroup.getModifierItems()) {
                                                                if (modItem.getAttributes().isSelected()) {
                                                                    selected += 1;
                                                                }
                                                            }
                                                            if (selected > 0) {
                                                                menuItem.getSizes().getAttributes().getVisibilityConfig().setInitialValue("");
                                                            } else {
                                                                menuItem.getSizes().getAttributes().getVisibilityConfig().setInitialValue("hidden");
                                                            }
                                                        }
                                                        break;
                                                    }

                                                }
                                            }
                                        }

                                    }
                                }

                                for (ModifierGroup group : menuItem.getModifierGroups()) {
                                    visibilityConfig = group.getAttributes().getVisibilityConfig();
                                    if (visibilityConfig != null) {
                                        List<VisibilityConfigRule> rules = visibilityConfig.getRules();
                                        if (rules != null && rules.size() > 0) {
                                            VisibilityConfigRule rule = rules.get(0);
                                            if (rule.getTargetType() != null) {

                                                if (rule.getTargetType().equalsIgnoreCase("modifier-group")) {
                                                    if (!group.getAttributes().getId().equals(menuItem.getSizes().getAttributes().getId()) && rule.getTargetId().equals(menuItem.getSizes().getAttributes().getId())) {
                                                        if (rule.getTargetIs() != null && rule.getTargetIs().equalsIgnoreCase("selected")) {
                                                            int selected = 0;
                                                            for (ModifierItem modItem : menuItem.getSizes().getModifierItems()) {
                                                                if (modItem.getAttributes().isSelected()) {
                                                                    selected += 1;
                                                                }
                                                            }
                                                            if (selected > 0) {
                                                                group.getAttributes().getVisibilityConfig().setInitialValue("");
                                                            } else {
                                                                group.getAttributes().getVisibilityConfig().setInitialValue("hidden");
                                                            }
                                                        }
                                                        break;
                                                    }

                                                    for (ModifierGroup modGroup : menuItem.getModifierGroups()) {
                                                        if (!group.getAttributes().getId().equals(modGroup.getAttributes().getId()) && rule.getTargetId().equals(modGroup.getAttributes().getId())) {
                                                            if (rule.getTargetIs() != null && rule.getTargetIs().equalsIgnoreCase("selected")) {
                                                                int selected = 0;
                                                                for (ModifierItem modItem : modGroup.getModifierItems()) {
                                                                    if (modItem.getAttributes().isSelected()) {
                                                                        selected += 1;
                                                                    }
                                                                }
                                                                if (selected > 0) {
                                                                    group.getAttributes().getVisibilityConfig().setInitialValue("");
                                                                } else {
                                                                    group.getAttributes().getVisibilityConfig().setInitialValue("hidden");
                                                                }
                                                            }
                                                            break;
                                                        }

                                                    }

                                                } else if (rule.getTargetType().equalsIgnoreCase("modifier")) {
                                                    Set<String> selectedModifiers = new HashSet<>();
                                                    for (ModifierItem modItem : menuItem.getSizes().getModifierItems()) {
                                                        if (modItem.getAttributes().isSelected()) {
                                                            selectedModifiers.add(modItem.getId());
                                                            break;
                                                        }
                                                    }

                                                    for (ModifierGroup modGroup : menuItem.getModifierGroups()) {
                                                        if (!group.getId().equals(modGroup.getId())) {
                                                            for (ModifierItem modItem : modGroup.getModifierItems()) {
                                                                if (modItem.getAttributes().isSelected()) {
                                                                    selectedModifiers.add(modItem.getId());
                                                                }
                                                            }
                                                        }
                                                    }

                                                    //for (ModifierGroup modGroup : menuItem.getModifierGroups()) {
                                                    //if (!group.getId().equals(modGroup.getId())) {
                                                    if (rule.getTargetId() != null && selectedModifiers.contains(rule.getTargetId())) {
                                                        if (rule.getTargetIs() != null && rule.getTargetIs().equalsIgnoreCase("selected")) {
                                                            group.getAttributes().getVisibilityConfig().setInitialValue("");
                                                        } else {
                                                            group.getAttributes().getVisibilityConfig().setInitialValue("hidden");
                                                        }
                                                    } else if (!rule.getTargetId().isEmpty()) {
                                                        group.getAttributes().getVisibilityConfig().setInitialValue("hidden");
                                                    }
                                                    //}
                                                    //}

                                                }
                                            }

                                        }
                                    }
                                }
                                menuItem.setFavourites(true);
                                CacheUtil.getInstance().addToCart(menuItem);
                                Log.d("FAFA","Added to cart bai");
                                orderView.addToCartFavorite(menuItem);
                            }

                            Store store = CacheUtil.getInstance().getOrderStore();
                            if (store != null && menuItem.getDisabledStores().containsKey(store.getId())) {
                                orderView.showMessage(CacheUtil.getInstance().getTranslations(Text.LOCATION_AVAILABLE).replace("(x)", store.getAttributes().getName()));
                            } else if (menuItem.getAttributes().isMustBeCustomized()) {
                                orderView.showMenuItem(categoryId, menuItem.getId());
                            } else if (menuItem.isCombo) {
                                orderView.showMenuItem(categoryId, menuItem.getId());
                            } else {
                                boolean invalid = false;
                                ArrayList<ModifierGroup> visibleModifierGroups = new ArrayList<>();

                                for (ModifierGroup group : menuItem.getModifierGroups()) {
                                    VisibilityConfig visibilityConfig = group.getAttributes().getVisibilityConfig();
                                    if (visibilityConfig != null && visibilityConfig.getInitialValue() != null) {
                                        if (!visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
                                            visibleModifierGroups.add(group);
                                        }
                                    } else {
                                        visibleModifierGroups.add(group);
                                    }
                                }

                                for (ModifierGroup modifierGroup : visibleModifierGroups) {
                                    int selected = 0;
                                    for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                                        if (modifierItem.getAttributes().isSelected()) {
                                            selected += modifierItem.getAttributes().getQuantity();
                                        }
                                    }
                                    if (selected < modifierGroup.getAttributes().getMinimum()) {
                                        invalid = true;
                                        break;
                                    }
                                }

                                if (invalid) {
                                    orderView.showMenuItem(categoryId, menuItem.getId());
                                } else {

                                    HashMap<String, String> eventAttributes = new HashMap<>();
                                    eventAttributes.put("Item Name", menuItem.getAttributes().getName());
                                    eventAttributes.put("Item Quantity", String.valueOf(menuItem.getAttributes().getQuantity()));

                                    double totalPrice = menuItem.getAttributes().getPrice(CacheUtil.getOrderStoreId());

                                    for (ModifierItem modifierItem : menuItem.getSizes().getModifierItems()) {
                                        if (modifierItem.getAttributes().isSelected()) {
                                            eventAttributes.put("Modifier Name", modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                                            eventAttributes.put("Modifier Quantity", String.valueOf(modifierItem.getAttributes().getQuantity()));
                                            totalPrice += modifierItem.getAttributes().getPrice();
                                        }
                                    }

                                    for (ModifierItem modifierItem : menuItem.getDrinks().getModifierItems()) {
                                        if (modifierItem.getAttributes().isSelected()) {
                                            eventAttributes.put("Modifier Name", modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                                            eventAttributes.put("Modifier Quantity", String.valueOf(modifierItem.getAttributes().getQuantity()));
                                            totalPrice += modifierItem.getAttributes().getPrice();
                                        }
                                    }

                                    for (ModifierItem modifierItem : menuItem.getFries().getModifierItems()) {
                                        if (modifierItem.getAttributes().isSelected()) {
                                            eventAttributes.put("Modifier Name", modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                                            eventAttributes.put("Modifier Quantity", String.valueOf(modifierItem.getAttributes().getQuantity()));
                                            totalPrice += modifierItem.getAttributes().getPrice();
                                        }
                                    }

                                    for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                                        VisibilityConfig visibilityConfig = modifierGroup.getAttributes().getVisibilityConfig();
                                        if (visibilityConfig == null || visibilityConfig.getInitialValue() == null || !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
                                            for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                                                if (modifierItem.getAttributes().isSelected()) {
                                                    eventAttributes.put("Modifier Name", modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                                                    eventAttributes.put("Modifier Quantity", String.valueOf(modifierItem.getAttributes().getQuantity()));
                                                    totalPrice += modifierItem.getAttributes().getPrice() * modifierItem.getAttributes().getQuantity();
                                                }
                                            }
                                        }
                                    }

                                    for (ModifierItem modifierItem : menuItem.getModifierItems()) {
                                        eventAttributes.put("Modifier Name", modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                                        eventAttributes.put("Modifier Quantity", String.valueOf(modifierItem.getAttributes().getQuantity()));
                                        totalPrice += modifierItem.getAttributes().getPrice();
                                    }

                                    for (Ingredient ingredient : menuItem.getIngredients()) {
                                        if (!ingredient.getAttributes().isSelected()) {
                                            eventAttributes.put("Ingredient Name", ingredient.getAttributes().getName());
                                            eventAttributes.put("Ingredient Quantity", String.valueOf(ingredient.getAttributes().getQuantity()));
                                            totalPrice += ingredient.getAttributes().getPrice();
                                        }
                                    }

                                    totalPrice += menuItem.getAttributes().getQuantity();

                                    orderView.logEvent("AddToCartFromFavorites", eventAttributes, "Price", totalPrice);
                                    orderView.logFirebaseEvent("add_to_cart_event");

                                    menuItem.getAttributes().setReorder(true);


                                    int cartCount = CacheUtil.getInstance().getCartQuantity();
                                    removeMukafaaRedemption();
                                    menuItem.setFavourites(true);
                                    CacheUtil.getInstance().addToCart(menuItem);
                                    if (cartCount == 0) {
                                        orderView.setCartExpiry();
                                    }
                                    Log.d("FAFA","Item to cart");
                                    orderView.addToCartFavorite(menuItem);
                                }

                            }
                        }

                    } else {
                        this.onError("");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                orderView.showError(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(Text.ITEM_NO_LONGER_AVAILABLE)));
            }
        });
    }

    @Override
    public String getAddress(LatLng latLng) {
        if (geocoder != null) {
            try {
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                address = (addresses.isEmpty() ? null : addresses.get(0));
                if (address != null) {
                    place = null;
                    return address.getAddressLine(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    @Override
    public Address getCurrentAddress() {
        return address;
    }

    @Override
    public void updateAddAddress() {

        String latitude = "";
        String longitude = "";

        if (address != null) {
            latitude = String.valueOf(address.getLatitude());
            longitude = String.valueOf(address.getLongitude());
        } else if (place != null) {
            latitude = String.valueOf(place.getLatLng().latitude);
            longitude = String.valueOf(place.getLatLng().longitude);
        }

        LocationApiCall.getInstance().getNearestStoreOrderOptions(latitude, longitude, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    Object data = object.get("data");

                    if (data instanceof JSONObject) {

                        CacheUtil.getInstance().setOrderType(OrderType.DELIVERY);
                        store = Store.parseStore((JSONObject) data);
                        CacheUtil.getInstance().setOrderStore(store);
                        selectedAddress = null;

                        checkUnavailableMenuItems();

                    } else if (data instanceof JSONArray) {
                        JSONArray array = (JSONArray) data;
                        if (array.length() > 0) {
                            CacheUtil.getInstance().setOrderType(OrderType.DELIVERY);
                            store = Store.parseStore(array.getJSONObject(0));
                            CacheUtil.getInstance().setOrderStore(store);
                            selectedAddress = null;
                            checkUnavailableMenuItems();
                        } else {
                            orderView.showError(CacheUtil.getInstance().getTranslations(Text.WE_DO_NOT_DELIVER_TO_YOUR_AREA));
                        }

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    orderView.showError(CacheUtil.getInstance().getTranslations(Text.WE_DO_NOT_DELIVER_TO_YOUR_AREA));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                orderView.showError(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(Text.WE_DO_NOT_DELIVER_TO_YOUR_AREA)));
            }
        });

    }

    @Override
    public void deleteAddress(com.skylinedynamics.solosdk.api.models.objects.Address address) {

        boolean home = false;
        boolean work = false;
        int position = -1;

        com.skylinedynamics.solosdk.api.models.objects.Address deleteAddress = null;
        for (com.skylinedynamics.solosdk.api.models.objects.Address homeAddress : bookmarkedAddresses) {
            if (homeAddress.getId().equalsIgnoreCase(address.getId())) {
                home = true;
                work = false;
                position = bookmarkedAddresses.indexOf(homeAddress);
                deleteAddress = homeAddress;
                break;
            }
        }

        if (deleteAddress == null) {
            for (com.skylinedynamics.solosdk.api.models.objects.Address workAddress : bookmarkedAddresses) {
                if (workAddress.getId().equalsIgnoreCase(address.getId())) {
                    home = false;
                    work = true;
                    deleteAddress = workAddress;
                    position = bookmarkedAddresses.indexOf(workAddress);
                    break;
                }
            }
        }

        if (deleteAddress == null) {
            for (com.skylinedynamics.solosdk.api.models.objects.Address otherAddress : otherAddresses) {
                if (otherAddress.getId().equalsIgnoreCase(address.getId())) {
                    home = false;
                    work = false;
                    deleteAddress = otherAddress;
                    position = otherAddresses.indexOf(otherAddress);
                    break;
                }
            }
        }

        final boolean homeFinal = home;
        final boolean workFinal = work;
        final int positionFinal = position;

        if (deleteAddress != null) {
            CustomersApiCall.getInstance().deleteCustomerAddress(AuthUtil.getInstance().getCustomer().getId(), deleteAddress.getId(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    if (homeFinal) {
                        bookmarkedAddresses.remove(positionFinal);
                    } else if (workFinal) {
                        bookmarkedAddresses.remove(positionFinal);
                    } else {
                        otherAddresses.remove(positionFinal);
                    }
                    //orderView.showAddresses(homeAddresses, workAddresses, otherAddresses);
                    orderView.showAddresses(bookmarkedAddresses, otherAddresses);
                    orderView.showMessage(CacheUtil.getInstance().getTranslations(Text.ADDRESS_DELETED, "Address successfully deleted"));
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    orderView.showMessage(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(Text.DELETING_ADDRESS_ERROR)));
                }
            });
        } else {
            orderView.showMessage(CacheUtil.getInstance().getTranslations(Text.DELETING_ADDRESS_ERROR));
        }
    }

    @Override
    public void getStoreOpen(Store storeValue, com.skylinedynamics.solosdk.api.models.objects.Address selectedAddressValue) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        LocationApiCall.getInstance().isOpen(storeValue.getId(), simpleDateFormat.format(new Date()), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                try {
                    JSONObject object = (JSONObject) o;
                    JSONObject data = object.getJSONObject("data");

                    OrderType orderType = CacheUtil.getInstance().getOrderType();
                    Store openStore = Store.parseStore(data);
                    if ((orderType == OrderType.DELIVERY && openStore.getAttributes().getOpenDeliver()) || openStore.getAttributes().getOpenPickup()) {

                        store = openStore;
                        selectedAddress = selectedAddressValue;

                        checkUnavailableMenuItems();

                    } else {
                        orderView.showError(CacheUtil.getInstance().getTranslations(Text.STORE_IS_CLOSED));
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                orderView.showError(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(Text.STORE_IS_CLOSED)));
            }
        });
    }

    @SuppressLint("CheckResult")
    @Override
    public void checkUnavailableMenuItems() {
        unavailableMenuItems.clear();
        ArrayList<MenuItem> menuItems = CacheUtil.getInstance().getCart();
        if (!menuItems.isEmpty() && store != null) {
            String orderStoreId = store.getId();
            Observable.fromIterable(menuItems).filter(item -> Observable.fromIterable(item.getModifierGroups()).filter(modifierGroup -> {
                // filter only items with a selected modifier
                return Observable.fromIterable(modifierGroup.getModifierItems()).any(modifierItem -> modifierItem.getAttributes().isSelected()).blockingGet();
            }).any(modifierGroup -> {
                boolean isModifierGroupDisabledForLocation = Observable.fromIterable(modifierGroup.getAttributes().getExcludedLocations()).map(Object::toString).any(modifierGroupExcludedLocation -> modifierGroupExcludedLocation.equals(orderStoreId)).blockingGet();

                boolean modifierGroupContainsModifierItemThatIsDisabledForLocation = Observable.fromIterable(modifierGroup.getModifierItems()).filter(modifierItem -> modifierItem.getAttributes().isSelected()).any(modifierItem -> Observable.fromIterable(modifierItem.getAttributes().getExcludedLocations()).map(Object::toString).any(modifierItemExcludedLocation -> modifierItemExcludedLocation.equals(orderStoreId)).blockingGet()).blockingGet();
                return isModifierGroupDisabledForLocation || modifierGroupContainsModifierItemThatIsDisabledForLocation;
            }).blockingGet()).forEach(menuItem -> {
                unavailableMenuItems.put(menuItem.getId(), menuItem);
            });
            Executor executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                String applicationId = CacheUtil.getInstance().getApplication().getId();
                AppDatabase appDatabase = AppDatabase.getInstance();
                for (MenuItem item : menuItems) {
                    if (!item.isPromo()) {
                        MenuItemEntity menuItemEntity = appDatabase.menuItemEntityDao().findById(applicationId, item.getId());
                        if (menuItemEntity != null) {
                            boolean isUnavailable = false;
                            MenuCategoryEntity menuCategoryEntity = appDatabase.menuCategoryEntityDao().findById(applicationId, menuItemEntity.getMenuCategoryId());
                            if (menuCategoryEntity != null) {
                                MenuCategory menuCategory = MenuCategoryEntity.Companion.toDomain(menuCategoryEntity);
                                if (menuCategory.getExcludedLocations().contains(orderStoreId)) {
                                    isUnavailable = true;
                                    unavailableMenuItems.put(item.getId(), item);
                                }
                            }
                            if (!isUnavailable) {
                                MenuItem menuItem = MenuItemEntity.Companion.toDomain(menuItemEntity);
                                boolean disable = false;
                                if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
                                    disable = menuItem.getDisableForDelivery() == 1;
                                } else if (CacheUtil.getInstance().pickupTypes()) {
                                    disable = menuItem.getDisableForPickup() == 1;
                                }
                                if ((disable || menuItem.getExcludedLocations().contains(orderStoreId) || menuItem.getOutOfStockLocations().contains(orderStoreId))) {
                                    unavailableMenuItems.put(item.getId(), item);
                                }
                            }
                        }
                    }
                }
                if (unavailableMenuItems.size() > 0) {
                    orderView.showUnavailableMenuItems();
                } else {
                    if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
                        checkMinimumDeliveryForArea();
                    } else {
                        setOrderType();
                    }
                }
            });
        } else {
            if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
                checkMinimumDeliveryForArea();
            } else {
                setOrderType();
            }
        }
    }

    @Override
    public String getUnavailableMenuItemsMessage() {
        String message = CacheUtil.getInstance().getTranslations(Text.DISABLED_ITEMS_DELIVERY);
        if (CacheUtil.getInstance().getOrderType() != OrderType.DELIVERY) {
            message = CacheUtil.getInstance().getTranslations(Text.DISABLED_ITEMS_PICKUP);
        }
        if (unavailableMenuItems.size() > 0) {
            message = CacheUtil.getInstance().getTranslations(Text.DISABLED_ITEMS, "Sorry, the items listed below are no longer available. Kindly remove them to proceed to checkout.");
        }
        return message;
    }

    @Override
    public ArrayList<MenuItem> getUnavailableMenuItems() {
        return new ArrayList<>(unavailableMenuItems.values());
    }

    @Override
    public void deleteUnavailableItems() {

        Set<String> unavailableItemIds = new HashSet<>(unavailableMenuItems.keySet());

        ArrayList<MenuItem> updatedCartItems = new ArrayList<>();
        for (MenuItem menuItem : CacheUtil.getInstance().getCart()) {
            if (!unavailableItemIds.contains(menuItem.getId())) {
                updatedCartItems.add(menuItem);
            }
        }
        removeMukafaaRedemption();
        CacheUtil.getInstance().setCart(updatedCartItems);
        if (CacheUtil.getInstance().getCartQuantity() == 0) {
            CacheUtil.getInstance().setUpsell(true);
        }
        orderView.deleteUnavailableItems();

    }

    public void checkMinimumDeliveryForArea() {
        DeliveryArea deliveryArea = store.getAttributes().getDeliveryArea();
        if (deliveryArea != null) {
            Double minimumOrderAmount = deliveryArea.getMinimumOrderAmount();
            if (minimumOrderAmount != null && minimumOrderAmount != 0) {
                orderView.showMinimumDeliveryForAreaMessage(CacheUtil.getInstance().getTranslations(Text.MINIMUM_DELIVERY_FOR_AREA, "Delivery to your area is available for orders over (x).").replace("(x)", StringUtil.getFormattedPrice(minimumOrderAmount)));
            } else {
                setOrderType();
            }
        } else {
            setOrderType();
        }
    }


    @Override
    public void setOrderType() {
        CacheUtil.getInstance().setOrderStore(store);
        if (CacheUtil.getInstance().getOrderType() != OrderType.DINE_IN || store.getAttributes().hasPromptForTableNumber == 0) {
            CacheUtil.getInstance().setDineInTableNumber("");
        }

        if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {

            if (selectedAddress != null) {
                CacheUtil.getInstance().setOrderAddress(selectedAddress);
                CacheUtil.getInstance().setOrderDeliveryAddress(null);
                orderView.saveAddress(selectedAddress.getAttributes().getLine1());
            } else {

                com.skylinedynamics.solosdk.api.models.objects.Address updateAddress = null;

                if (AuthUtil.getInstance().isSignedIn()) {

                    for (com.skylinedynamics.solosdk.api.models.objects.Address homeAddress : bookmarkedAddresses) {
                        if (homeAddress.getAttributes().getLat().equals(String.valueOf(address.getLatitude())) && homeAddress.getAttributes().getLongt().equals(String.valueOf(address.getLongitude()))) {
                            updateAddress = homeAddress;
                            break;
                        }
                    }

                    if (updateAddress == null) {
                        for (com.skylinedynamics.solosdk.api.models.objects.Address otherAddress : otherAddresses) {
                            if (address != null && otherAddress.getAttributes().getLat().equals(String.valueOf(address.getLatitude())) && otherAddress.getAttributes().getLongt().equals(String.valueOf(address.getLongitude()))) {
                                updateAddress = otherAddress;
                                break;
                            }
                        }
                    }

                }

                if (updateAddress != null) {
                    orderView.updateAddAddress(updateAddress, null);
                } else {
                    orderView.updateAddAddress(null, address);
                }
            }

        } else {
            orderView.showStore(true, store, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
        }
    }

    @Override
    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public boolean validateUpdateAddAddress(String phoneNumber) {
        if (phoneNumber.isEmpty()) {
            orderView.showPhoneNumberError(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_REQUIRED));
            return false;
        } else if (!StringUtil.isValidPhoneNumber(phoneNumber)) {
            orderView.showPhoneNumberError(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_INVALID));
            return false;
        }
        orderView.showPhoneNumberError("");
        return true;
    }

    @Override
    public void addAddress(String phoneNumber, String label, String note) {

        if (AuthUtil.getInstance().isSignedIn()) {

            CreateCustomerAddressBody createCustomerAddressBody = null;

            if (address != null) {
                createCustomerAddressBody = new CreateCustomerAddressBody("", label, "", "", "", "", "", address.getAddressLine(0), address.getLatitude(), address.getLongitude(), phoneNumber, note, "");
            } else if (place != null) {
                createCustomerAddressBody = new CreateCustomerAddressBody("", label, "", "", "", "", "", place.getName(), place.getLatLng().latitude, place.getLatLng().longitude, phoneNumber, note, "");
            }

            if (createCustomerAddressBody != null) {

                CustomersApiCall.getInstance().addCustomerAddress(createCustomerAddressBody, AuthUtil.getInstance().getCustomer().getId(), "1", new ApiRequestListener() {
                    @Override
                    public void onSuccess(Object o) {
                        MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_ADD_ADDRESS);
                        getAddresses(true);
                    }

                    @Override
                    public void onError(Object o) {
                        AppLogger.printRetrofitError(o);
                        orderView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
                    }
                });

            } else {
                orderView.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
            }

        } else {

            CacheUtil.getInstance().setOrderType(OrderType.DELIVERY);
            double lat = 0;
            double lng = 0;
            String addressName = "";
            if (address != null) {
                lat = address.getLatitude();
                lng = address.getLongitude();
                addressName = address.getAddressLine(0);
            } else if (place != null) {
                lat = place.getLatLng().latitude;
                lng = place.getLatLng().longitude;
                addressName = place.getName();
                CacheUtil.getInstance().setOrderDeliveryAddress(place.getName());
            }

            CacheUtil.getInstance().setOrderDeliveryLatitude(String.valueOf(lat));
            CacheUtil.getInstance().setOrderDeliveryLongitude(String.valueOf(lng));
            CacheUtil.getInstance().setOrderDeliveryAddress(addressName);
            CacheUtil.getInstance().setOrderDeliveryTelephone(phoneNumber);
            CacheUtil.getInstance().setOrderDeliveryLabel(label);
            CacheUtil.getInstance().setOrderDeliveryInstructions(note);

            String finalAddressName = addressName;

            orderView.saveAddress(finalAddressName);

        }
    }

    @Override
    public void updateAddress(boolean fromSave, com.skylinedynamics.solosdk.api.models.objects.Address updatedAddress, boolean isExistingAddress) {

        if (AuthUtil.getInstance().isSignedIn() && updatedAddress != null) {
            UpdateCustomerAddressBody updateCustomerAddressBody = new UpdateCustomerAddressBody(updatedAddress.getAttributes().getLabel(), updatedAddress.getAttributes().getTelephone(), updatedAddress.getAttributes().getInstructions());
            CustomersApiCall.getInstance().updateCustomerAddress(AuthUtil.getInstance().getCustomer().getId(), updatedAddress.getId(), updateCustomerAddressBody, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {

                    if (fromSave) {
                        CacheUtil.getInstance().setOrderType(OrderType.DELIVERY);
                        CacheUtil.getInstance().setOrderAddress(updatedAddress);

                    } else {

                        if (AuthUtil.getInstance().isSignedIn()) {

                            boolean found = false;

                            for (com.skylinedynamics.solosdk.api.models.objects.Address homeAddress : bookmarkedAddresses) {
                                if (homeAddress.getAttributes().getLat().equals(String.valueOf(updatedAddress.getAttributes().getLat())) && homeAddress.getAttributes().getLongt().equals(String.valueOf(updatedAddress.getAttributes().getLongt()))) {
                                    found = true;
                                    bookmarkedAddresses.get(bookmarkedAddresses.indexOf(homeAddress)).getAttributes().setLabel(updatedAddress.getAttributes().getLabel());
                                    break;
                                }
                            }

                            if (!found) {
                                for (com.skylinedynamics.solosdk.api.models.objects.Address otherAddress : otherAddresses) {
                                    if (otherAddress.getAttributes().getLat().equals(String.valueOf(updatedAddress.getAttributes().getLat())) && otherAddress.getAttributes().getLongt().equals(String.valueOf(updatedAddress.getAttributes().getLongt()))) {
                                        otherAddresses.get(otherAddresses.indexOf(otherAddress)).getAttributes().setLabel(updatedAddress.getAttributes().getLabel());
                                        if (isExistingAddress) {
                                            CacheUtil.getInstance().setOrderType(OrderType.DELIVERY);
                                            CacheUtil.getInstance().setOrderAddress(updatedAddress);
                                        }
                                        break;

                                    }
                                }
                            }

                        }

                    }

                    //orderView.saveAddress(updatedAddress.getAttributes().getLine1());
                    orderView.updateAddress(updatedAddress.getAttributes().getLine1());

                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    orderView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.UNABLE_TO_SAVE_ADDRESS), null));
                }
            });
        } else {
            orderView.showMessage(CacheUtil.getInstance().getTranslations(Text.UNABLE_TO_SAVE_ADDRESS));
        }
    }

    @Override
    public void bookmarkAddress(com.skylinedynamics.solosdk.api.models.objects.Address address) {
        orderView.updateAddAddress(address, null);
    }

    @Override
    public void getOrder(String id) {
        OrdersApiCall.getInstance().getOrder(id, new ApiRequestListener() {

            @Override
            public void onSuccess(Object o) {
                if (o instanceof JSONObject) {

                    try {
                        JSONObject jsonObject = (JSONObject) o;
                        JSONObject data = jsonObject.getJSONObject("data");

                        order = OrderDetails.parse(data);
                        orderStatus = order.getOrderStatus();

                        AblyUtil ablyUtil = AblyUtil.Companion.getInstance();
                        ablyUtil.subscribeToOrderStatusUpdate(order.getId(), OrderPresenter.this::onOrderStatusUpdate);

                        getOrderStatuses();
                        getOrderFeedbackTopics();

                        if (driver == null && orderStatus != null && orderStatus.getSequence() >= OrderStatusType.DELIVERY_IN_PROGRESS.getValue() && orderStatus.getSequence() <= OrderStatusType.ARRIVED_AT_CUSTOMER.getValue()) {
                            getDriver();
                        }

                        orderView.showOrder(order);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        orderView.showError(CacheUtil.getInstance().getTranslations(Text.RETRIEVING_ORDER_DETAILS_ERROR));
                    }

                } else {
                    orderView.showError(CacheUtil.getInstance().getTranslations(Text.RETRIEVING_ORDER_DETAILS_ERROR));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                orderView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.RETRIEVING_ORDER_DETAILS_ERROR), null));
            }
        });
    }

    @Override
    public void getStore(com.skylinedynamics.solosdk.api.models.objects.Address addressNew) {

        String latitude = "";
        String longitude = "";

        if (addressNew != null) {
            latitude = addressNew.getAttributes().getLat();
            longitude = addressNew.getAttributes().getLongt();
        } else {
            latitude = String.valueOf(address.getLatitude());
            longitude = String.valueOf(address.getLongitude());
        }

        LocationApiCall.getInstance().getNearestStoreOrderOptions(latitude, longitude, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    Object data = object.get("data");

                    if (data instanceof JSONObject) {
                        store = Store.parseStore((JSONObject) data);
                        CacheUtil.getInstance().setOrderStore(store);
                        selectedAddress = addressNew;
                        checkUnavailableMenuItems();
                    } else if (data instanceof JSONArray) {
                        JSONArray array = (JSONArray) data;
                        if (array.length() > 0) {
                            store = Store.parseStore(array.getJSONObject(0));
                            CacheUtil.getInstance().setOrderStore(store);
                            selectedAddress = addressNew;
                            checkUnavailableMenuItems();
                        } else {
                            orderView.showError(CacheUtil.getInstance().getTranslations(Text.WE_DO_NOT_DELIVER_TO_YOUR_AREA));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    orderView.showError(CacheUtil.getInstance().getTranslations(Text.WE_DO_NOT_DELIVER_TO_YOUR_AREA));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                orderView.showError(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(Text.WE_DO_NOT_DELIVER_TO_YOUR_AREA)));
            }
        });

    }

    @Override
    public int getStorePosition(boolean isDineIn, String id) {
        if (isDineIn) {
            for (Store store : dineInStores) {
                if (store.getId().equals(id)) {
                    return dineInStores.indexOf(store);
                }
            }
        } else {
            for (Store store : pickupCurbsideStores) {
                if (store.getId().equals(id)) {
                    return pickupCurbsideStores.indexOf(store);
                }
            }
        }
        return -1;
    }

    @Override
    public Store getStore(boolean isDineIn, int position) {
        if (isDineIn) {
            if (position < dineInStores.size()) {
                return dineInStores.get(position);
            } else {
                return null;
            }
        } else if (position < pickupCurbsideStores.size()) {
            return pickupCurbsideStores.get(position);
        }
        return null;
    }

    @Override
    public void getStoreDetails() {
        LocationApiCall.getInstance().getStoreDetails(String.valueOf(order.getLocationId()), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                if (o instanceof JSONObject) {

                    try {
                        JSONObject jsonObject = (JSONObject) o;
                        JSONObject data = jsonObject.getJSONObject("data");

                        Store store = Store.parseStore(data);
                        getLocation(store);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        orderView.showError(CacheUtil.getInstance().getTranslations(Text.RETRIEVING_ORDER_DETAILS_ERROR));
                    }
                } else {
                    orderView.showError(CacheUtil.getInstance().getTranslations(Text.RETRIEVING_ORDER_DETAILS_ERROR));
                }

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                orderView.showError(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(Text.RETRIEVING_ORDER_DETAILS_ERROR)));
            }
        });
    }

    @Override
    public void getLocation(Store store) {

        LatLng originPosition = null;
        LatLng destinationPosition = null;

        OrderType orderType = OrderType.NONE;

        switch (order.getDeliveryOption().toLowerCase()) {

            case "to-go":
            case "pickup":
            case "امسك":
                orderType = OrderType.PICKUP;
            case "dine-in":
            case "eat-in":
            case "أكلت داخل":
                if (orderType == OrderType.NONE) {
                    orderType = OrderType.DINE_IN;
                }
                if (location != null) {
                    originPosition = new LatLng(location.getLatitude(), location.getLongitude());
                }
                if (store != null) {
                    destinationPosition = new LatLng(store.getAttributes().getLat(), store.getAttributes().getLng());
                }
                break;

            case "deliver":
            case "ايصال":
                orderType = OrderType.DELIVERY;
                if (store != null) {
                    originPosition = new LatLng(store.getAttributes().getLat(), store.getAttributes().getLng());
                }
                destinationPosition = new LatLng(order.getCustomerLat(), order.getCustomerLng());
                break;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        if (originPosition != null) {
            builder.include(originPosition);
        }

        if (destinationPosition != null) {
            builder.include(destinationPosition);
        }

        orderView.updateMap(orderType, originPosition, destinationPosition, builder.build());
    }

    @Override
    public void getDriver() {
        OrdersApiCall.getInstance().getDriver(order.getId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                if (o instanceof JSONObject) {

                    try {
                        JSONObject jsonObject = (JSONObject) o;
                        JSONArray data = jsonObject.getJSONArray("data");

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject dataObject = data.getJSONObject(i);
                            Employee employee = Employee.parse(dataObject);
                            if (employee.getRoles().contains("driver")) {
                                driver = employee;
                                getDriverBearing();
                            }
                        }


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
            }
        });
    }

    @Override
    public void getDriverBearing() {
        OrdersApiCall.getInstance().getDriverBearing(driver.getId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                try {
                    JSONObject bearingObject = new JSONObject(o.toString());
                    JSONObject dataObject = bearingObject.getJSONObject("data");
                    JSONObject attributesObject = dataObject.getJSONObject("attributes");

                    LatLng bearing = new LatLng(attributesObject.getDouble("lat"), attributesObject.getDouble("long"));

                    orderView.showDriver(bearing);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
            }
        });
    }

    @Override
    public void unsubscribeAbly() {
        AblyUtil.Companion.getInstance().detachAll();
    }

    @Override
    public void getOrderStatuses() {

        OrdersApiCall.getInstance().getOrderStatuses(order.getId(), new ApiRequestListener() {

            @Override
            public void onSuccess(Object o) {
                if (o instanceof JSONObject) {

                    try {
                        JSONObject object = (JSONObject) o;
                        JSONArray dataArray = object.getJSONArray("data");

                        orderStatuses.clear();

                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject orderStatusObject = dataArray.getJSONObject(i);
                            OrderStatus orderStatus = OrderStatus.parse(orderStatusObject);
                            orderStatuses.add(orderStatus);
                        }

                        Collections.sort(orderStatuses, new Comparator<OrderStatus>() {
                            @Override
                            public int compare(OrderStatus orderStatus, OrderStatus t1) {
                                return orderStatus.getSequence() - t1.getSequence();
                            }
                        });

                        orderView.showOrderStatuses(orderStatuses);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                orderView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
            }
        });
    }

    @Override
    public void onBindTimeLineViewAtPosition(int position, TimeLineViewHolder timeLineViewHolder) {
        timeLineViewHolder.setTimeLine(orderStatuses.get(position));
    }

    @Override
    public int getTimeLineCount() {
        return orderStatuses.size();
    }

    @Override
    public void cancelOrder() {
        if (order != null) {
            OrdersApiCall.getInstance().cancelOrder(order.getId(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    orderView.showMessage(CacheUtil.getInstance().getTranslations(Text.ORDER_CANCELLED));
                    orderView.showOrders();

                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    orderView.showMessage(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.CANCEL_ORDER_NOT_ALLOWED), null));
                }
            });
        }
    }

    @Override
    public void getOrderFeedbackTopics() {
        if (order != null) {
            OrdersApiCall.getInstance().getOrderFeedbackTopics(order.getId(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    ratings.clear();
                    try {
                        JSONObject jsonObject = new JSONObject(o.toString());
                        JSONArray dataArray = jsonObject.optJSONArray("data");

                        if (dataArray != null) {
                            ratings.addAll(GsonHelper.gsonFinal().fromJson(dataArray.toString(), new TypeToken<List<Rating>>() {
                            }.getType()));
                        }

                        getOrderFeedbacks();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.onError(e.getMessage());
                    }

                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    ratings.clear();
                    orderView.showRatings(order.getOrderStatus(), ratings);
                }
            });
        }
    }

    @Override
    public void getOrderFeedbacks() {
        OrdersApiCall.getInstance().getOrderFeedbacks(order.getId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject jsonObject = new JSONObject(o.toString());
                    JSONArray dataArray = jsonObject.optJSONArray("data");
                    JSONArray includedArray = jsonObject.optJSONArray("included");

                    List<OrderFeedback> orderFeedbacks = new ArrayList<>();

                    if (dataArray != null) {
                        orderFeedbacks.addAll(GsonHelper.gsonFinal().fromJson(dataArray.toString(), new TypeToken<List<OrderFeedback>>() {
                        }.getType()));
                    }

                    if (orderFeedbacks.size() > 0) {

                        OrderFeedback orderFeedback = orderFeedbacks.get(orderFeedbacks.size() - 1);

                        if (includedArray != null) {

                            for (int i = 0; i < includedArray.length(); i++) {
                                Rating rating = GsonHelper.gsonFinal().fromJson(includedArray.getJSONObject(i).toString(), new TypeToken<Rating>() {
                                }.getType());
                                if (orderFeedback.getRatingIds().contains(rating.getId())) {
                                    for (Rating ratingTopic : ratings) {
                                        if (rating.getAttributes().getTopic().equalsIgnoreCase(ratingTopic.getAttributes().getName())) {
                                            ratingTopic.getAttributes().setRating(rating.getAttributes().getRating());
                                            break;
                                        }
                                    }
                                }
                            }

                        }

                    }

                    orderView.showRatings(order.getOrderStatus(), ratings);

                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e.getMessage());
                }

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                orderView.showRatings(order.getOrderStatus(), ratings);
            }
        });
    }

    @Override
    public List<Rating> getRatings() {
        return ratings;
    }

    @Override
    public void sendOrderFeedback(List<Rating> ratings) {
        this.ratings = ratings;
        Customer customer = AuthUtil.getInstance().getCustomer();
        OrderFeedbackBody orderFeedbackBody = new OrderFeedbackBody(customer.getAttributes().getFirstName() + " " + customer.getAttributes().getLastName(), customer.getAttributes().getEmail(), customer.getAttributes().getMobile(), "Order Feedback", "Order Feedback", this.ratings);
        OrdersApiCall.getInstance().sendOrderFeedback(order.getId(), orderFeedbackBody, null, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                orderView.showMessage(CacheUtil.getInstance().getTranslations(Text.ORDER_RATING_SENT));
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                orderView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.RETRIEVING_ORDER_DETAILS_ERROR), null));
            }
        });
    }

    @Override
    public void getNewFusedLocation(Location location) {
        hasLocation = true;
        fusedLocationProvider.disconnect();
        this.location = location;
        CacheUtil.getInstance().setOrderLatitude(String.valueOf(location.getLatitude()));
        CacheUtil.getInstance().setOrderLongitude(String.valueOf(location.getLongitude()));
        CacheUtil.getInstance().setOrderDeliveryLatitude(String.valueOf(location.getLatitude()));
        CacheUtil.getInstance().setOrderDeliveryLongitude(String.valueOf(location.getLongitude()));
        getStores(1);
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        orderView.showLocation(false, latLng, getAddress(latLng));
    }

    @Override
    public void removeMukafaaRedemption() {
        String transactionId = CacheUtil.getInstance().getMukafaaTransactionId();
        if (transactionId != null && !transactionId.isEmpty()) {
            CacheUtil.getInstance().clearMukafaa();
            CustomersApiCall.getInstance().deleteRedemption(Values.MUKAFAA_PROVIDER, transactionId, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {

                }

                @Override
                public void onError(Object o) {

                }
            });
        }
    }

    @Override
    public void removeCurbside() {
        CacheUtil.getInstance().setCurbsideCar(null);
    }

    @Override
    public void getDiffMenuCdn(com.skylinedynamics.solosdk.api.models.objects.Address address) {
        AppDatabase appDatabase = AppDatabase.getInstance();
        String applicationId = CacheUtil.getInstance().getApplication().getId();
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                appDatabase.menuCategoryDao().deleteByApplication(applicationId);
                appDatabase.menuItemDao().deleteByApplication(applicationId);
                appDatabase.modifierGroupDao().deleteByApplication(applicationId);
                appDatabase.modifierItemDao().deleteByApplication(applicationId);
                appDatabase.ingredientDao().deleteByApplication(applicationId);
                appDatabase.allergenDao().deleteByApplication(applicationId);
                appDatabase.menuCategoryEntityDao().deleteByApplication(applicationId);
                appDatabase.menuItemEntityDao().deleteByApplication(applicationId);
                String url = MenuUtil.getDiffMenuCdnUrl();
                UtilitiesApiCall.getInstance().getMenuCategoriesWithHeaders(url, new ApiRequestWithHeadersListener() {
                    @Override
                    public void onSuccess(Headers headers, Object o) {
                        try {
                            CacheUtil.getInstance().setReloadDifferentMenu(true);
                            if (headers.names().contains("last-modified")) {
                                CacheUtil.getInstance().setMenuCdnLastModified(headers.get("last-modified"));
                            }
                            String objectString = o.toString();
                            JSONObject object = new JSONObject(objectString);
                            cachedCountryMenu(objectString);
                            parseMenu(object, address);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            this.onError("");
                        }
                    }

                    @Override
                    public void onError(Object o) {
                        AppLogger.printRetrofitError(o.toString());
                        orderView.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                    }
                });
            }
        });
    }

    @Override
    public String getMenuCategory(String menuItemId) {
        String applicationId = CacheUtil.getInstance().getApplication().getId();
        AppDatabase appDatabase = AppDatabase.getInstance();
        MenuItemEntity menuItemEntity = appDatabase.menuItemEntityDao().findById(applicationId, menuItemId);
        if (menuItemEntity != null) {
            return menuItemEntity.getMenuCategoryId();
        }
        return "";
    }

    @Override
    public void parseMenu(JSONObject data, com.skylinedynamics.solosdk.api.models.objects.Address address) {
        Executor executor = Executors.newSingleThreadExecutor();

        AppDatabase appDatabase = AppDatabase.getInstance();

        executor.execute(new Runnable() {
            @Override
            public void run() {

                try {
                    String applicationId = CacheUtil.getInstance().getApplication().getId();

                    JSONArray dataArray = data.getJSONArray("data");
                    List<MenuCategoryItemResponse> menuCategoryItemsResponse = GsonHelper.gsonFinal().fromJson(dataArray.toString(), new TypeToken<ArrayList<MenuCategoryItemResponse>>() {
                    }.getType());

                    JSONObject included = data.getJSONObject("included");

                    JSONArray allergensArray = new JSONArray();
                    if (included.has("allergens") && !included.isNull("allergens")) {
                        allergensArray = included.getJSONArray("allergens");
                    }
                    for (int index = 0; index < allergensArray.length(); index++) {
                        JSONObject allergenObject = allergensArray.getJSONObject(index);
                        String allergenId = String.valueOf(allergenObject.getLong("id"));
                        AllergenDb allergenDb = new AllergenDb();
                        allergenDb.applicationId = applicationId;
                        allergenDb.itemAllergenId = allergenId;
                        allergenDb.id = allergenId;
                        allergenDb.data = allergenObject.toString();
                        appDatabase.allergenDao().insertAll(allergenDb);
                    }

                    JSONArray ingredientsArray = new JSONArray();
                    if (included.has("ingredients") && !included.isNull("ingredients")) {
                        ingredientsArray = included.getJSONArray("ingredients");
                    }
                    for (int index = 0; index < ingredientsArray.length(); index++) {
                        JSONObject ingredientObject = ingredientsArray.getJSONObject(index);
                        String ingredientId = String.valueOf(ingredientObject.getLong("id"));
                        IngredientDb ingredientDb = new IngredientDb();
                        ingredientDb.applicationId = applicationId;
                        ingredientDb.itemIngredientId = ingredientId;
                        ingredientDb.id = ingredientId;
                        ingredientDb.data = ingredientObject.toString();
                        appDatabase.ingredientDao().insertAll(ingredientDb);
                    }

                    JSONArray modifierGroupsArray = new JSONArray();
                    if (included.has("modifierGroups") && !included.isNull("modifierGroups")) {
                        modifierGroupsArray = included.getJSONArray("modifierGroups");
                    }
                    for (int index = 0; index < modifierGroupsArray.length(); index++) {
                        JSONObject modifierGroupObject = modifierGroupsArray.getJSONObject(index);
                        JSONObject modifierGroupAttributesObject = modifierGroupObject.getJSONObject("attributes");

                        String code = "";
                        if (modifierGroupAttributesObject.getString("code") != null) {
                            code = modifierGroupAttributesObject.getString("code").trim().toLowerCase();
                        }
                        String type = "";
                        if (modifierGroupAttributesObject.getString("type") != null) {
                            type = modifierGroupAttributesObject.getString("type").trim().toLowerCase();
                        }
                        String label = "";
                        if (modifierGroupAttributesObject.getString("label") != null) {
                            label = modifierGroupAttributesObject.getString("label").trim().toLowerCase();
                        }

                        ModifierGroupType modifierGroupType = ModifierGroupType.OPTIONS;
                        if (code.equals("sizes") || type.equals("sizes") || label.equals("sizes")) {
                            modifierGroupType = ModifierGroupType.SIZES;
                        }
                        ModifierGroupDb modifierGroupDb = new ModifierGroupDb();
                        String modifierGroupId = String.valueOf(modifierGroupObject.getLong("id"));
                        modifierGroupDb.itemGroupId = modifierGroupId;
                        modifierGroupDb.id = modifierGroupId;
                        modifierGroupDb.applicationId = applicationId;
                        modifierGroupDb.type = modifierGroupType.getValue();
                        modifierGroupDb.data = modifierGroupObject.toString();
                        appDatabase.modifierGroupDao().insertAll(modifierGroupDb);
                    }

                    JSONArray modifiersArray = new JSONArray();
                    if (included.has("modifiers") && !included.isNull("modifiers")) {
                        modifiersArray = included.getJSONArray("modifiers");
                    }
                    for (int index = 0; index < modifiersArray.length(); index++) {
                        JSONObject modifierItemObject = modifiersArray.getJSONObject(index);
                        String modifierItemId = String.valueOf(modifierItemObject.getLong("id"));
                        ModifierItemDb modifierItemDb = new ModifierItemDb();
                        modifierItemDb.applicationId = applicationId;
                        modifierItemDb.itemGroupModifierId = modifierItemId;
                        modifierItemDb.id = modifierItemId;
                        modifierItemDb.data = modifierItemObject.toString();
                        appDatabase.modifierItemDao().insertAll(modifierItemDb);
                    }

                    ArrayList<MenuItemDb> menuItemDbs = new ArrayList<>();

                    for (int index = 0; index < dataArray.length(); index++) {

                        JSONObject menuCategoryItemObject = dataArray.getJSONObject(index);
                        JSONObject menuCategoryItemAttributesObject = menuCategoryItemObject.getJSONObject("attributes");
                        JSONObject menuCategoryObject = menuCategoryItemAttributesObject.getJSONObject("category");
                        JSONArray menuItemsArray = menuCategoryItemAttributesObject.getJSONArray("items");

                        for (int menuItemIndex = 0; menuItemIndex < menuItemsArray.length(); menuItemIndex++) {
                            JSONObject menuItemObject = menuItemsArray.getJSONObject(menuItemIndex);
                            MenuItemDb menuItemDb = new MenuItemDb();
                            String menuItemId = String.valueOf(menuItemObject.getLong("id"));
                            menuItemDb.id = menuItemId;
                            menuItemDb.applicationId = applicationId;
                            menuItemDb.name = menuItemObject.getJSONObject("name").getString(LocaleUtil.getInstance().getLanguage());
                            menuItemDb.menuCategoryId = String.valueOf(menuCategoryObject.getLong("id"));
                            String menuItemDbData = menuItemObject.toString();
                            menuItemDb.data = menuItemDbData;
                            //TODO: Use file cache only for Lorenzo for now
                            if (BuildUtil.soloAppKey().equalsIgnoreCase("7r5YQwmSgZCtM0ux")) {
                                orderView.saveToCache(menuItemId, menuItemDbData);
                            }
                            menuItemDbs.add(menuItemDb);
                        }

                    }

                    try {
                        appDatabase.menuItemDao().insertAll(menuItemDbs.toArray(new MenuItemDb[menuItemDbs.size()]));
                    } catch (ArrayIndexOutOfBoundsException e) {
                    }

                    for (MenuCategoryItemResponse menuCategoryItem : menuCategoryItemsResponse) {
                        MenuCategoryEntity menuCategoryEntity = menuCategoryItem.getAttributes().getMenuCategory();
                        menuCategoryEntity.setApplicationId(applicationId);
                        if (!MenuUtil.isExcluded(menuCategoryEntity.getExcludedApps())) {
                            appDatabase.menuCategoryEntityDao().insertAll(menuCategoryEntity);
                            String categoryImageUrl = menuCategoryItem.getAttributes().getMenuCategory().getImage();
                            if (categoryImageUrl == null || categoryImageUrl.equalsIgnoreCase("null") || categoryImageUrl.isEmpty() || categoryImageUrl.toLowerCase().contains("default-image.png")) {
                                categoryImageUrl = Values.DEFAULT_IMAGE_URL;
                            } else {
                                categoryImageUrl = StringUtil.getCloudflareUrl(Values.MENU_IMAGE_SIZE, Values.MENU_IMAGE_SIZE, categoryImageUrl);
                            }
                            orderView.preloadImage(categoryImageUrl, RequestOptions.centerInsideTransform());

                            for (MenuItemEntity menuItem : menuCategoryItem.getAttributes().getMenuItems()) {
                                menuItem.setApplicationId(applicationId);
                                menuItem.setMenuCategoryId(menuCategoryItem.getAttributes().getMenuCategory().getId());
                                if (!MenuUtil.isExcluded(menuItem.getExcludedApps())) {
                                    appDatabase.menuItemEntityDao().insertAll(menuItem);
                                    String itemImageUrl = menuItem.getImage();
                                    if (itemImageUrl == null || itemImageUrl.equalsIgnoreCase("null") || itemImageUrl.isEmpty() || itemImageUrl.toLowerCase().contains("default-image.png")) {
                                        itemImageUrl = Values.DEFAULT_IMAGE_URL;
                                    } else {
                                        itemImageUrl = StringUtil.getCloudflareUrl(Values.MENU_IMAGE_SIZE, Values.MENU_IMAGE_SIZE, itemImageUrl);
                                    }
                                    orderView.preloadImage(itemImageUrl, RequestOptions.centerCropTransform());
                                }
                            }
                        }

                    }

                    CacheUtil.getInstance().setMenuCached(new Date().getTime());
                    if (BuildConfig.MULTI_COUNTRY_ENABLED) {
                        String countryApplicationId = BuildConfig.APPLICATION_ID;
                        SupportedCountry country = CacheUtil.getInstance().getCountry();
                        if (country != null) {
                            countryApplicationId = country.applicationId;
                        }
                        CacheUtil.getInstance().setCountryCachedTime(countryApplicationId, new Date().getTime());
                    }
                    orderView.diffMenuSaved(address);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }

    private void cachedCountryMenu(String menu) {
        if (BuildConfig.MULTI_COUNTRY_ENABLED) {
            String applicationId = BuildConfig.APPLICATION_ID;
            SupportedCountry country = CacheUtil.getInstance().getCountry();
            if (country != null) {
                applicationId = country.applicationId;
            }
            CacheUtil.getInstance().setCountryMenu(applicationId, menu);
            CacheUtil.getInstance().setCountryCachedTime(applicationId, System.currentTimeMillis());
        }
    }

    @Override
    public void setSelectedAddress(@NonNull com.skylinedynamics.solosdk.api.models.objects.Address address) {
        this.selectedAddress = address;
    }

    private Unit onOrderStatusUpdate(Message message) {
        getOrderStatuses();
        try {
            JSONObject jsonObject = new JSONObject(message.data.toString());
            OrderData orderData = OrderData.parse(jsonObject.getJSONObject("order"));
            orderStatus = orderData.getOrderStatus();
            orderView.showStatus(orderData.getOrderStatus());
            orderView.showRatings(orderData.getOrderStatus(), ratings);

            if (driver == null && orderStatus != null && orderStatus.getSequence() >= OrderStatusType.DELIVERY_IN_PROGRESS.getValue() && orderStatus.getSequence() <= OrderStatusType.ARRIVED_AT_CUSTOMER.getValue()) {
                getDriver();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Unit.INSTANCE;
    }

}
