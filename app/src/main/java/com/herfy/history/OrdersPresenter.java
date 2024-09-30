package com.herfy.history;

import android.location.Geocoder;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.Place;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.herfy.database.AppDatabase;
import com.herfy.database.entities.AllergenDb;
import com.herfy.database.entities.IngredientDb;
import com.herfy.database.entities.MenuItemDb;
import com.herfy.database.entities.ModifierGroupDb;
import com.herfy.database.entities.ModifierItemDb;
import com.herfy.history.viewholders.OrderMenuItemViewHolder;
import com.herfy.history.viewholders.OrderViewHolder;
import com.herfy.history.viewholders.TimeLineViewHolder;
import com.herfy.util.AppLogger;
import com.herfy.utils.constants.Text;
import com.herfy.utils.constants.Values;
import com.herfy.utils.handler.ErrorHandler;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.BuildUtil;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.utils.helper.ably.AblyUtil;
import com.herfy.utils.menu.MenuUtil;
import com.location.FusedLocationProvider;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.calls.LocationApiCall;
import com.skylinedynamics.solosdk.api.calls.MenuApiCall;
import com.skylinedynamics.solosdk.api.calls.OrdersApiCall;
import com.skylinedynamics.solosdk.api.models.kotlin.DeliveryDriver;
import com.skylinedynamics.solosdk.api.models.kotlin.LyveDriver;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.Allergen;
import com.skylinedynamics.solosdk.api.models.objects.AllergenCdn;
import com.skylinedynamics.solosdk.api.models.objects.Application;
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.Customer;
import com.skylinedynamics.solosdk.api.models.objects.Ingredient;
import com.skylinedynamics.solosdk.api.models.objects.IngredientCdn;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsTransaction;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemCombo;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupCdn;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupType;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItemCdn;
import com.skylinedynamics.solosdk.api.models.objects.OrderData;
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails;
import com.skylinedynamics.solosdk.api.models.objects.OrderFeedback;
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatusType;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Payment;
import com.skylinedynamics.solosdk.api.models.objects.Payments;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfig;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfigRule;
import com.skylinedynamics.solosdk.api.models.objects.combo.ComboItem;
import com.skylinedynamics.solosdk.api.models.objects.combo.ComboItemMain;
import com.skylinedynamics.solosdk.api.models.objects.curbside.CustomerCar;
import com.skylinedynamics.solosdk.api.models.objects.order.OrderIngredient;
import com.skylinedynamics.solosdk.api.models.objects.order.OrderModifierItem;
import com.skylinedynamics.solosdk.api.models.requestbodies.OrderFeedbackBody;
import com.skylinedynamics.solosdk.util.GsonHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
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
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import io.ably.lib.types.Message;
import kotlin.Unit;

public class OrdersPresenter implements OrdersContract.Presenter, FusedLocationProvider.LocationResultCallback {

    public boolean hasLocation = false;

    private boolean fromAbly;

    private boolean isLocationUpdateSubscribed = false;
    private Location location;
    private FusedLocationProvider fusedLocationProvider;
    private final OrdersContract.View ordersView;

    private boolean newestToOldest = true;
    private HashMap<String, Integer> ordersMap = new HashMap<>();
    private ArrayList<OrderHistory> orders = new ArrayList<>();
    private OrderDetails order = null;
    private ArrayList<OrderStatus> orderStatuses = new ArrayList<>();
    private Store store = null;
    private Address address = null;
    private ArrayList<MenuItem> menuItems = new ArrayList<>();
    private int menuItemsCount = 0;
    private LyveDriver driver = null;
    private List<ComboItemMain> comboItems = new ArrayList<>();
    private boolean isDone = false;

    private List<Rating> ratings = new ArrayList<>();

    private int orderSequence = -1;

    public OrdersPresenter(OrdersContract.View ordersView) {
        this.ordersView = ordersView;
        this.ordersView.setPresenter(this);
    }

    @Override
    public void start() {
        ordersView.setupTranslations();
        ordersView.setupViews();
        ordersView.setupFonts();
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
    public void getOrders() {
        Customer customer = AuthUtil.getInstance().getCustomer();
        CustomersApiCall.getInstance().getOrdersNew(customer.getId(), "1", "10", new ApiRequestListener() {

            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    JSONArray array = object.getJSONArray("data");

                    ordersMap.clear();
                    orders = new ArrayList<>();

                    for (int x = 0; x < array.length(); x++) {
                        JSONObject orderObject = array.getJSONObject(x);
                        OrderHistory order = OrderHistory.parse(orderObject);
                        ArrayList<String> menItems = new ArrayList<>();
                        if (object.has("included") && !object.isNull("included")) {
                            JSONArray included = object.getJSONArray("included");
                            for (int a = 0; a < included.length(); a++) {
                                JSONObject includedObject = included.getJSONObject(a);
                                if (includedObject.getString("type").equals("order-item")) {
                                    MenuItem menuItem = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<MenuItem>() {
                                    }.getType());
                                    if (menuItem.getRelationships().getRelationshipOrder().links.self.contains(order.getId())) {
                                        menItems.add(menuItem.getAttributes().getImage());
                                    }
                                } else if (includedObject.getString("type").equals("customer-address")) {
                                    Address orderItem = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<Address>() {
                                    }.getType());
                                    if (orderItem.getId().equals(String.valueOf(order.getCustomerAddressId()))) {
                                        order.setLocationName(orderItem.getAttributes().getLine1());
                                    }
                                }
                            }
                        }
                        order.setMenuImages(menItems);
                        ordersMap.put(order.getId(), x);
                        orders.add(order);
                    }

                    Collections.sort(orders, new Comparator<OrderHistory>() {
                        @Override
                        public int compare(OrderHistory o1, OrderHistory o2) {
                            try {
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
                                Date date1 = simpleDateFormat.parse(o1.getCreatedAt());
                                Date date2 = simpleDateFormat.parse(o2.getCreatedAt());
                                if (newestToOldest) {
                                    return date2.compareTo(date1);
                                } else {
                                    return date1.compareTo(date2);
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            return 0;
                        }
                    });

                    ordersView.showOrders(newestToOldest, orders);
                    AblyUtil ablyUtil = AblyUtil.Companion.getInstance();
                    for (OrderHistory orderHistory : orders) {
                        ablyUtil.subscribeToOrderStatusUpdate(orderHistory.getId(), OrdersPresenter.this::onOrderHistoryStatusUpdate);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    ordersMap.clear();
                    orders = new ArrayList<>();
                    ordersView.showOrders(newestToOldest, orders);
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                orders = new ArrayList<>();
                ordersView.showOrders(newestToOldest, orders);
            }
        });
    }

    @Override
    public LyveDriver getDriverDetail() {
        return driver;
    }

    private Unit onOrderHistoryStatusUpdate(Message message) {
        try {
            JSONObject jsonObject = new JSONObject(message.data.toString());
            OrderData orderData = OrderData.parse(jsonObject.getJSONObject("order"));
            String orderId = orderData.getId();
            if (ordersMap.containsKey(orderId) && ordersMap.get(orderId) != null) {
                getOrder(orderId, ordersMap.get(orderId), true, false);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Unit.INSTANCE;
    }

    @Override
    public void onBindOrderViewAtPosition(int position, OrderViewHolder orderViewHolder) {
        orderViewHolder.setOrder(orders.get(position));
    }

    @Override
    public int getOrdersCount() {
        return orders.size();
    }

    @Override
    public void sortOrders() {
        newestToOldest = !newestToOldest;
        Collections.sort(orders, new Comparator<OrderHistory>() {
            @Override
            public int compare(OrderHistory o1, OrderHistory o2) {

                try {

                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
                    Date date1 = simpleDateFormat.parse(o1.getCreatedAt());
                    Date date2 = simpleDateFormat.parse(o2.getCreatedAt());
                    if (newestToOldest) {
                        return date2.compareTo(date1);
                    } else {
                        return date1.compareTo(date2);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });

        ordersView.showOrders(newestToOldest, orders);
    }

    @Override
    public void selectOrder(OrderHistory order) {
        ordersView.selectOrder(order);
    }

    @Override
    public void getOrder(String id, int position, boolean statusUpdate, boolean reorder) {

        OrdersApiCall.getInstance().getOrderNew(id, "items,location,customerAddress,campaign.promotion,payments,loyalty-points-transactions,feedbacks,car,combos", new ApiRequestListener() {

            @Override
            public void onSuccess(Object o) {

                if (o instanceof JSONObject) {

                    try {
                        JSONObject jsonObject = (JSONObject) o;
                        JSONObject data = jsonObject.getJSONObject("data");

                        order = OrderDetails.parse(data);

                        try {
                            if (jsonObject.has("included") && !jsonObject.isNull("included")) {
                                JSONArray included = jsonObject.getJSONArray("included");
                                for (int a = 0; a < included.length(); a++) {
                                    JSONObject includedObject = included.getJSONObject(a);
                                    if (includedObject.getString("type").equals("payments")) {
                                        order.payment = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<Payment>() {
                                        }.getType());
                                    } else if (includedObject.getString("type").equals("customer-car")) {
                                        order.customerCar = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<CustomerCar>() {
                                        }.getType());
                                    }
                                }
                            }

                            if (order.getPayments().getStatusDescription().isEmpty()) {
                                JSONArray dataArr = jsonObject.optJSONArray("included");
                                if (dataArr != null) {
                                    for (int i = 0; i < dataArr.length(); i++) {
                                        JSONObject dataObj = dataArr.optJSONObject(i);
                                        if (dataObj != null) {
                                            if (dataObj.optString("type").equals("payments")) {
                                                JSONObject attributes = dataObj.optJSONObject("attributes");
                                                assert attributes != null;
                                                Payments payments = new Payments(attributes.optString("status"), attributes.optString("status-description"));
                                                order.setPayments(payments);
                                            }
                                        }
                                    }
                                }
                            }

                            JSONArray dataArr = jsonObject.optJSONArray("included");
                            transactions = new ArrayList<>();
                            if (dataArr != null) {
                                for (int i = 0; i < dataArr.length(); i++) {
                                    JSONObject dataObj = dataArr.optJSONObject(i);
                                    if (dataObj != null) {
                                        if (dataObj.getString("type").equals("loyalty-points-transaction")) {
                                            transactions.add(GsonHelper.gsonFinal().fromJson(dataObj.toString(), new TypeToken<LoyaltyPointsTransaction>() {
                                            }.getType()));
                                        }
                                    }

                                }
                            }


                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (!statusUpdate) {
                            if (!reorder) {
                                String orderId = order.getId();
                                subscribeToOrderStatusUpdate(orderId);
                                subscribeToOrderDriverAssign(orderId);
                                getOrderStatuses();
                                getOrderFeedbackTopics();
                            }

                            menuItems = new ArrayList<>();
                            HashMap<String, Double> comboPrices = new HashMap<>();
                            if (jsonObject.has("included") && !jsonObject.isNull("included")) {
                                JSONArray included = jsonObject.getJSONArray("included");
                                for (int i = 0; i < included.length(); i++) {
                                    JSONObject includedObject = included.getJSONObject(i);
                                    String includedType = includedObject.getString("type");
                                    if (includedType.equals("order-item")) {
                                        MenuItem menuItem = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<MenuItem>() {
                                        }.getType());
                                        menuItems.add(menuItem);
                                    } else if (includedType.equals("location")) {
                                        store = Store.parseStore(includedObject);
                                    } else if (includedType.equals("customer-address")) {
                                        address = Address.parseAddress(includedObject);
                                    } else if (includedType.equals("combo")) {
                                        MenuItem combo = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<MenuItem>() {
                                        }.getType());
                                        comboPrices.put(String.valueOf(combo.getAttributes().getItemId()), combo.getAttributes().getFinalPrice());
                                    }
                                }
                                for (MenuItem menuItem : menuItems) {
                                    if(menuItem.getAttributes().isCombo()) {
                                        String menuItemId = String.valueOf(menuItem.getAttributes().getItemId());
                                        if(comboPrices.containsKey(menuItemId)) {
                                            Double price = comboPrices.get(menuItemId);
                                            if(price != null && price != 0.00) {
                                                menuItem.setPrice(price);
                                                menuItem.getAttributes().setPrice(price);
                                            }
                                        }
                                    }
                                    ArrayList<ModifierGroup> modifierGroups = new ArrayList<>();
                                    for (int i = 0; i < included.length(); i++) {
                                        JSONObject includedObject = included.getJSONObject(i);
                                        if (includedObject.getString("type").equals("modifier-group")) {
                                            ModifierGroup modifierGroup = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<ModifierGroup>() {
                                            }.getType());
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
                                            } else {
                                                modifierGroup.setType(ModifierGroupType.OPTIONS);
                                            }
                                            modifierGroups.add(modifierGroup);
                                        } else if (includedObject.getString("type").equals("modifier")) {
                                            OrderModifierItem modifierItem = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<OrderModifierItem>() {
                                            }.getType());

                                            // for modifiers with empty modifier ID like in combo meal
                                            if (modifierItem.getAttributes().getId() == null || modifierItem.getAttributes().getId().isEmpty()) {
                                                modifierItem.getAttributes().setId(modifierItem.getId());
                                            }

                                            if (menuItem.getModifierItemsIds().contains(modifierItem.getId())) {
                                                if (menuItem.getOrderModifierItemsMap().containsKey(modifierItem.getAttributes().getId())) {
                                                    OrderModifierItem modItem = menuItem.getOrderModifierItemsMap().get(modifierItem.getAttributes().getId());
                                                    if (modItem != null) {
                                                        modItem.getAttributes().setQuantity(modItem.getAttributes().getQuantity() + modifierItem.getAttributes().getQuantity());
                                                        menuItem.getOrderModifierItemsMap().put(modifierItem.getAttributes().getId(), modItem);
                                                    }
                                                } else {
                                                    menuItem.getOrderModifierItemsMap().put(modifierItem.getAttributes().getId(), modifierItem);
                                                }
                                            }
                                        } else if (includedObject.getString("type").equals("item-ingredient")) {
                                            OrderIngredient ingredient = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<OrderIngredient>() {
                                            }.getType());
                                            if (menuItem.getIngredientsIds().contains(ingredient.getId())) {
                                                ingredient.getAttributes().setSelected(false);
                                                List<OrderIngredient> items = new ArrayList<>(menuItem.getOrderIngredients());
                                                items.add(ingredient);
                                                menuItem.setOrderIngredients(items);
                                            }
                                        }
                                    }
                                    menuItem.setModifierGroups(modifierGroups);
                                    menuItem.setModifierGroupIds(modifierGroups);
                                    menuItem.setOrderModifierItems(new LinkedList<>(menuItem.getOrderModifierItemsMap().values()));
                                }
                            }
                            if (!reorder) {
                                ordersView.showOrder(order, store, address);
                                ordersView.showMenuItems(order, menuItems);
                            } else {
                                checkReorder();
                            }
                            if (!reorder && driver == null && order.getOrderStatus() != null && order.getOrderStatus().getSequence() >= OrderStatusType.DELIVERY_IN_PROGRESS.getValue() && order.getOrderStatus().getSequence() <= OrderStatusType.ARRIVED_AT_CUSTOMER.getValue()) {
                                getDriver();
                            }
                        } else {
                            if (position >= 0) {
                                if (position < orders.size()) {
                                    OrderStatus orderStatus = order.getOrderStatus();
                                    orders.get(position).setCode(orderStatus.getCode());
                                    orders.get(position).setSequence(orderStatus.getSequence());
                                    orders.get(position).setDescription(orderStatus.getDescription());
                                    ordersView.updateOrder(position);
                                }
                            } else {
                                ordersView.updateStatus(order, order.getOrderStatus());
                                if (driver == null && order.getOrderStatus() != null && order.getOrderStatus().getSequence() >= OrderStatusType.DELIVERY_IN_PROGRESS.getValue() && order.getOrderStatus().getSequence() <= OrderStatusType.ARRIVED_AT_CUSTOMER.getValue()) {
                                    getDriver();
                                }
                                ordersView.showRatings(order.getOrderStatus(), ratings);
                            }

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        if (!statusUpdate) {
                            ordersView.showError(CacheUtil.getInstance().getTranslations(!reorder ? Text.RETRIEVING_ORDER_DETAILS_ERROR : Text.REORDER_ERROR));
                        }
                    }

                } else {
                    if (!statusUpdate) {
                        ordersView.showError(CacheUtil.getInstance().getTranslations(!reorder ? Text.RETRIEVING_ORDER_DETAILS_ERROR : Text.REORDER_ERROR));
                    }
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                if (!statusUpdate) {
                    ordersView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(!reorder ? Text.RETRIEVING_ORDER_DETAILS_ERROR : Text.REORDER_ERROR), null));
                }
            }
        });
    }

    @Override
    public void getStore(boolean reorder) {
        LocationApiCall.getInstance().getStoreDetails(String.valueOf(order.getLocationId()), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                if (o instanceof JSONObject) {

                    try {
                        JSONObject jsonObject = (JSONObject) o;
                        JSONObject data = jsonObject.getJSONObject("data");

                        store = Store.parseStore(data);

                        if (!order.getCustomerAddressId().isEmpty()) {
                            getAddress(reorder);
                        } else if (reorder) {
                            checkReorder();
                        } else {
                            ordersView.showOrder(order, store, address);
                            ordersView.showMenuItems(order, menuItems);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.onError("");
                    }
                } else {
                    this.onError("");
                }

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                store = new Store();
                store.setId(String.valueOf(order.getLocationId()));
                store.getAttributes().setName(order.getLocationName());
                if (!order.getCustomerAddressId().isEmpty()) {
                    getAddress(reorder);
                } else if (reorder) {
                    checkReorder();
                } else {
                    ordersView.showOrder(order, store, address);
                    ordersView.showMenuItems(order, menuItems);
                }
            }
        });
    }

    @Override
    public void getAddress(boolean reorder) {
        CustomersApiCall.getInstance().getCustomerAddress(AuthUtil.getInstance().getCustomer().getId(), order.getCustomerAddressId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                if (o instanceof JSONObject) {

                    try {
                        JSONObject jsonObject = (JSONObject) o;
                        JSONObject data = jsonObject.getJSONObject("data");

                        address = Address.parseAddress(data);

                        if (!reorder) {
                            ordersView.showOrder(order, store, address);
                            ordersView.showMenuItems(order, menuItems);
                        } else {
                            checkReorder();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        ordersView.showError(CacheUtil.getInstance().getTranslations(!reorder ? Text.RETRIEVING_ORDER_DETAILS_ERROR : Text.REORDER_ERROR));
                    }
                } else {
                    ordersView.showError(CacheUtil.getInstance().getTranslations(!reorder ? Text.RETRIEVING_ORDER_DETAILS_ERROR : Text.REORDER_ERROR));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                ordersView.showError(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(!reorder ? Text.RETRIEVING_ORDER_DETAILS_ERROR : Text.REORDER_ERROR)));
            }
        });
    }

    @Override
    public void getOrderStatuses() {
        OrdersApiCall.getInstance().getOrderStatuses(order.getId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object obj) {
                if (obj instanceof JSONObject) {
                    try {

                        JSONObject jsonObject = (JSONObject) obj;
                        JSONArray itemsArray = jsonObject.getJSONArray("data");
                        orderStatuses.clear();
                        for (int i = 0; i < itemsArray.length(); i++) {
                            OrderStatus orderStatus = OrderStatus.parse(itemsArray.getJSONObject(i));
                            orderStatuses.add(orderStatus);
                            if (orderStatus.getSequence() >= OrderStatusType.ARRIVED_AT_CUSTOMER.getValue() && orderStatus.isDone()) {
                                isDone = true;
                            }
                        }
                        Collections.sort(orderStatuses, new Comparator<OrderStatus>() {
                            @Override
                            public int compare(OrderStatus orderStatus, OrderStatus t1) {
                                return orderStatus.getSequence() - t1.getSequence();
                            }
                        });
                        boolean isCancelled = order.getOrderStatus().getSequence() >= OrderStatusType.CANCELLED_BY_CUSTOMER.getValue();
                        if (isCancelled) {
                            orderStatuses = filterOrderStatuses(orderStatuses);
                        }
                        ordersView.showOrderStatuses(orderStatuses, isDone);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.onError(e);
                    }
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o.toString());
                orderStatuses.clear();
                ordersView.showOrderStatuses(orderStatuses, isDone);
            }
        });
    }

    @Override
    public void onBindTimeLineViewAtPosition(int orderPosition, int position, TimeLineViewHolder timeLineViewHolder, boolean noDriver) {
        if (orderPosition == -1) {
            boolean isCancelled = order.getOrderStatus().getSequence() >= OrderStatusType.CANCELLED_BY_CUSTOMER.getValue();
            timeLineViewHolder.setTimeLine(isCancelled, orderStatuses.get(position), noDriver);
        } else {
            boolean isCancelled = orders.get(orderPosition).getSequence() >= OrderStatusType.CANCELLED_BY_CUSTOMER.getValue();
            OrderStatus orderStatus = isCancelled ? filterOrderStatuses(orders.get(orderPosition).getStatuses()).get(position) : orders.get(orderPosition).getStatuses().get(position);
            timeLineViewHolder.setTimeLine(isCancelled, orderStatus, noDriver);
        }
    }

    @Override
    public int getTimeLineCount(int orderPosition) {
        if (orderPosition == -1) {
            return orderStatuses.size();
        } else {
            boolean isCancelled = orders.get(orderPosition).getSequence() >= OrderStatusType.CANCELLED_BY_CUSTOMER.getValue();
            if (isCancelled) {
                return filterOrderStatuses(orders.get(orderPosition).getStatuses()).size();
            } else {
                return orders.get(orderPosition).getStatuses().size();
            }
        }
    }

    private ArrayList<OrderStatus> filterOrderStatuses(List<OrderStatus> orderStatuses) {
        ArrayList<OrderStatus> filteredOrderStatuses = new ArrayList<>();
        for (OrderStatus orderStatus : orderStatuses) {
            if (orderStatus.getTime() != null && !orderStatus.getTime().equalsIgnoreCase("null") && !orderStatus.getTime().isEmpty()) {
                filteredOrderStatuses.add(orderStatus);
            }
        }
        return filteredOrderStatuses;
    }

    @Override
    public void onBindMenuItemViewAtPosition(int position, OrderMenuItemViewHolder orderMenuViewHolder) {
        orderMenuViewHolder.setMenuItem(menuItems.get(position));
    }

    @Override
    public int getMenuItemsCount() {
        return menuItems.size();
    }

    @Override
    public int getSequence() {
        if (order != null) {
            return order.getOrderStatus().getSequence();
        } else {
            return orderSequence;
        }
    }

    @Override
    public void setSequence(int sequence) {
        orderSequence = sequence;
    }

    @Override
    public OrderDetails getOrder() {
        return order;
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
                            JSONObject attributes = dataObject.optJSONObject("attributes");
                            String employeeId = dataObject.getString("id");
                            if (attributes != null) {
                                JSONObject lyveDriverObject = attributes.getJSONObject("lyve-driver");
                                try {
                                    LyveDriver lyveDriver = new Gson().fromJson(lyveDriverObject.toString(), LyveDriver.class);
                                    if (lyveDriver != null) {
                                        driver = lyveDriver;
                                        ordersView.showWhatsApp(driver.getEmployee().getMobile());
                                        LatLng customerLatLng = new LatLng(order.getCustomerLat(), order.getCustomerLng());
                                        LatLng driverLatLng = new LatLng(Double.parseDouble(lyveDriver.getLat()), Double.parseDouble(lyveDriver.getLng()));
                                        ordersView.showDriver(customerLatLng, driverLatLng, lyveDriver.getEta());
                                        if (!isLocationUpdateSubscribed) {
                                            subscribeToOrderDriverLocationUpdate(order.getId());
                                        }
                                    } else {
                                        getDriverBearing(employeeId);
                                    }
                                } catch (Exception e) {
                                    getDriverBearing(employeeId);
                                    e.printStackTrace();
                                }
                            }
                            if (driver == null) {
                                getDriverBearing(employeeId);
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
    public void customerArrived(String orderId) {
        JsonObject body = new JsonObject();
        body.addProperty("order-status", "customer-arrived");
        OrdersApiCall.getInstance().updateOrderStatus(orderId, body, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                ordersView.customerArrivedSuccess();
            }

            @Override
            public void onError(Object o) {
                ordersView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
            }
        });
    }

    @Override
    public void getDriverBearing(String employeeId) {
        OrdersApiCall.getInstance().getDriverBearing(employeeId, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                try {
                    JSONObject bearingObject = new JSONObject(o.toString());
                    JSONObject dataObject = bearingObject.getJSONObject("data");
                    JSONObject attributesObject = dataObject.getJSONObject("attributes");
                    LatLng customerLatLng = new LatLng(order.getCustomerLat(), order.getCustomerLng());
                    LatLng driverLatLng = new LatLng(attributesObject.getDouble("lat"), attributesObject.getDouble("long"));
                    ordersView.showDriver(customerLatLng, driverLatLng, attributesObject.getInt("eta"));
                    if (!isLocationUpdateSubscribed) {
                        subscribeToOrderDriverLocationUpdate(order.getId());
                    }
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
    public void detachAbly() {
        AblyUtil.Companion.getInstance().detachAll();
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
                    ordersView.showRatings(order.getOrderStatus(), ratings);
                }
            });
        }
    }

    @Override
    public void getOrderFeedbackTopics(String orderId) {
        OrdersApiCall.getInstance().getOrderFeedbackTopics(orderId, new ApiRequestListener() {
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

                    getOrderFeedbacks(orderId);

                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e.getMessage());
                }

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                ratings.clear();
                ordersView.showRatings(null, ratings);
            }
        });
    }

    @Override
    public void getOrderFeedbacks(String orderId) {
        OrdersApiCall.getInstance().getOrderFeedbacks(orderId, new ApiRequestListener() {
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

                    ordersView.showRatings(null, ratings);

                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e.getMessage());
                }

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                ordersView.showRatings(null, ratings);
            }
        });
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

                    ordersView.showRatings(order.getOrderStatus(), ratings);

                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e.getMessage());
                }

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                ordersView.showRatings(order.getOrderStatus(), ratings);
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
                ordersView.showMessage(CacheUtil.getInstance().getTranslations(Text.ORDER_RATING_SENT, "Order ratings sent"));
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                ordersView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.RETRIEVING_ORDER_DETAILS_ERROR), null));
            }
        });
    }

    @Override
    public void sendOrderFeedback(List<Rating> ratings, String orderId, String otherFeedback) {
        this.ratings = ratings;
        String feedbackNew = otherFeedback;
        if (feedbackNew.isEmpty()) {
            feedbackNew = "Order Feedback";
        }
        Customer customer = AuthUtil.getInstance().getCustomer();
        String email = customer.getAttributes().getEmail();
        if (email.isEmpty()) {
            email = null;
        }
        OrderFeedbackBody orderFeedbackBody = new OrderFeedbackBody(customer.getAttributes().getFirstName() + " " + customer.getAttributes().getLastName(), email, customer.getAttributes().getMobile(), "Order Feedback", feedbackNew, this.ratings);
        OrdersApiCall.getInstance().sendOrderFeedback(orderId, orderFeedbackBody, null, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                ordersView.showMessage(CacheUtil.getInstance().getTranslations(Text.ORDER_RATING_SENT, "Order ratings sent"));
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                ordersView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.RETRIEVING_ORDER_DETAILS_ERROR), null));
            }
        });
    }

    @Override
    public void cancel() {
        OrdersApiCall.getInstance().cancelOrder(order.getId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                ordersView.showMessage(CacheUtil.getInstance().getTranslations(Text.ORDER_CANCELLED));
                ordersView.cancel();
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                ordersView.showMessage(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(Text.CANCEL_ORDER_NOT_ALLOWED)));
            }
        });
    }

    @Override
    public void checkReorder() {
        OrdersApiCall.getInstance().getOrderNew(order.getId(), "items,campaign.promotion,combo-order,combos", new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject jsonObject = (JSONObject) o;
                    JSONArray included = jsonObject.getJSONArray("included");
                    comboItems.clear();
                    boolean reorderable = true;
                    for (int i = 0; i < included.length(); i++) {
                        JSONObject includedObject = included.getJSONObject(i);
                        String type = includedObject.getString("type");
                        if (type.equalsIgnoreCase("order-item")) {
                            MenuItem menuItem = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<MenuItem>() {
                            }.getType());
                            if (!menuItem.getAttributes().isActive()) {
                                reorderable = false;
                                break;
                            }
                        } else if (type.equalsIgnoreCase("modifier-group")) {
                            ModifierGroup modifierGroup = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<ModifierGroup>() {
                            }.getType());
                            if (modifierGroup.getAttributes().getEnabled() == 0) {
                                reorderable = false;
                                break;
                            }
                        } else if (type.equalsIgnoreCase("modifier")) {
                            ModifierItem modifierItem = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<ModifierItem>() {
                            }.getType());
                            if (modifierItem.getAttributes().getEnabled() == 0) {
                                reorderable = false;
                                break;
                            }
                        } else if (type.equalsIgnoreCase("item-ingredient")) {
                            Ingredient ingredient = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<Ingredient>() {
                            }.getType());
                            if (ingredient.getAttributes().getEnabled() == 0) {
                                reorderable = false;
                                break;
                            }
                        } else if (type.equalsIgnoreCase("combo")) {
                            try {
                                ComboItemMain comboItem = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<ComboItemMain>() {
                                }.getType());
                                comboItems.add(comboItem);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (!reorderable) {
                        this.onError("");
                    } else {
                        reorder();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError("");
                }

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o.toString());
                ordersView.showError(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(Text.REORDER_ERROR, "Some items are no longer available in this order.")));
            }
        });
    }

    @Override
    public void reorder() {

        CacheUtil.getInstance().setOrderStore(store);
        if (order.getDeliveryOption().equalsIgnoreCase("to-go") || order.getDeliveryOption().equalsIgnoreCase("pickup") || order.getDeliveryOption().equalsIgnoreCase("امسك")) {
            CacheUtil.getInstance().setOrderType(OrderType.PICKUP);
        } else if (order.getDeliveryOption().equalsIgnoreCase("dine-in") || order.getDeliveryOption().equalsIgnoreCase("eat-in") || order.getDeliveryOption().equalsIgnoreCase("أكلت داخل")) {
            CacheUtil.getInstance().setOrderType(OrderType.DINE_IN);
        } else if (order.getDeliveryOption().equalsIgnoreCase("deliver") || order.getDeliveryOption().equalsIgnoreCase("ايصال")) {
            CacheUtil.getInstance().setOrderType(OrderType.DELIVERY);
            CacheUtil.getInstance().setOrderAddress(address);
        } else if (order.getDeliveryOption().equalsIgnoreCase("drive-thru")) {
            CacheUtil.getInstance().setOrderType(OrderType.DRIVE_THRU);
        }

        MenuItem reorderMenuItem = menuItems.get(menuItemsCount);
        if (reorderMenuItem.isPromo() || reorderMenuItem.getAttributes().getIsPromotion()) {
            menuItemsCount++;
            if (menuItemsCount < menuItems.size()) {
                reorder();
            } else {
                menuItemsCount = 0;
                if (!CacheUtil.getInstance().getCart().isEmpty()) {
                    ordersView.showCart();
                }
            }
        } else {
            getMenuItem(String.valueOf(reorderMenuItem.getAttributes().getItemId()), reorderMenuItem.getAttributes().getQuantity());
        }

    }

    @Nullable
    String getMenuItemData(AppDatabase appDatabase, String id) {
        String data = "";
        try {
            //TODO: Use file cache only for Lorenzo for now
            if (BuildUtil.soloAppKey().equalsIgnoreCase("7r5YQwmSgZCtM0ux")) {
                data = ordersView.getFromCache(id);
            } else {
                String applicationId = CacheUtil.getInstance().getApplication().getId();
                MenuItemDb menuItemDb = appDatabase.menuItemDao().findByApplicationAndId(applicationId, id);
                data = menuItemDb.data;
            }
        } catch (Exception e) {
            data = ordersView.getFromCache(id);
        }
        return data;
    }

    public void getMenuItem(String menuItemId, int quantity) {

        Executor executor = Executors.newSingleThreadExecutor();
        AppDatabase appDatabase = AppDatabase.getInstance();
        executor.execute(new Runnable() {
            @Override
            public void run() {

                String data = getMenuItemData(appDatabase, menuItemId);

                if (data != null && !data.isEmpty()) {

                    String applicationId = CacheUtil.getInstance().getApplication().getId();

                    MenuItem menuItem = MenuUtil.processMenuItemCustomObject(GsonHelper.gsonFinal().fromJson(data, MenuItem.class));

                    menuItem.getAttributes().setQuantity(quantity);
                    LinkedList<Allergen> allergens = new LinkedList<>();
                    for (AllergenDb allergenDb : appDatabase.allergenDao().findAllByApplicationAndIds(applicationId, menuItem.getAllergensIds().toArray(new String[menuItem.getAllergensIds().size()]))) {
                        AllergenCdn allergenCdn = GsonHelper.gsonFinal().fromJson(allergenDb.data, AllergenCdn.class);
                        allergens.add(AllergenCdn.convert(allergenCdn, LocaleUtil.getInstance().getLanguage()));
                    }
                    Collections.sort(allergens, new Comparator<Allergen>() {
                        @Override
                        public int compare(Allergen allergen1, Allergen allergen2) {
                            return allergen1.getAttributes().getDisplayOrder() - allergen2.getAttributes().getDisplayOrder();
                        }
                    });
                    menuItem.setAllergens(allergens);

                    LinkedList<Ingredient> ingredients = new LinkedList<>();
                    HashMap<String, Ingredient> ingredientIds = new HashMap<>();
                    for (Ingredient ingredient : menuItem.getIngredientIds()) {
                        ingredientIds.put(ingredient.getIngredientId(), ingredient);
                    }
                    for (IngredientDb ingredientDb : appDatabase.ingredientDao().findAllByApplicationAndIds(applicationId, ingredientIds.keySet().toArray(new String[ingredientIds.size()]))) {
                        IngredientCdn ingredientCdn = GsonHelper.gsonFinal().fromJson(ingredientDb.data, IngredientCdn.class);
                        Ingredient ingredient = IngredientCdn.convert(ingredientCdn, LocaleUtil.getInstance().getLanguage());
                        ingredient.setId(ingredientIds.get(ingredientCdn.getId()).getId());
                        ingredients.add(ingredient);
                    }
                    Collections.sort(ingredients, new Comparator<Ingredient>() {
                        @Override
                        public int compare(Ingredient ingredient1, Ingredient ingredient2) {
                            return ingredient1.getAttributes().getDisplayOrder() - ingredient2.getAttributes().getDisplayOrder();
                        }
                    });
                    menuItem.setIngredients(ingredients);

                    //menuItemModifierGroups.clear();

                    ModifierGroup sizes = new ModifierGroup();
                    LinkedList<ModifierGroup> modifierGroups = new LinkedList<>();
                    HashMap<String, ModifierGroup> modifierGroupIds = new HashMap<>();
                    for (ModifierGroup modifierGroup : menuItem.getModifierGroupIds()) {
                        modifierGroupIds.put(modifierGroup.getId(), modifierGroup);
                    }
                    for (ModifierGroupDb modifierGroupDb : appDatabase.modifierGroupDao().findAllByApplicationAndIds(applicationId, modifierGroupIds.keySet().toArray(new String[modifierGroupIds.size()]))) {
                        ModifierGroupCdn modifierGroupCdn = MenuUtil.processModifierGroupCustomObject(GsonHelper.gsonFinal().fromJson(modifierGroupDb.data, ModifierGroupCdn.class));
                        LinkedList<ModifierItem> modifierItems = new LinkedList<>();
                        ArrayList<ModifierItem> modifierGroupModifierItems = modifierGroupIds.get(modifierGroupCdn.getId()).getModifiers();
                        ArrayList<String> modifierGroupModifierItemIds = new ArrayList<>();
                        ArrayList<Integer> excludeModifierItems = modifierGroupIds.get(modifierGroupCdn.getId()).getExcludeModifiers();
                        Set<String> excludedModifierItems = new HashSet<>();
                        for (Integer exclude : excludeModifierItems) {
                            excludedModifierItems.add(String.valueOf(exclude));
                        }
                        for (ModifierItem modifierGroupModifierItem : modifierGroupModifierItems) {
                            if (!excludedModifierItems.contains(modifierGroupModifierItem.getId())) {
                                modifierGroupModifierItemIds.add(modifierGroupModifierItem.getId());
                            }
                        }
                        for (ModifierItemDb modifierItemDb : appDatabase.modifierItemDao().findAllByApplicationAndIds(applicationId, modifierGroupModifierItemIds.toArray(new String[modifierGroupModifierItemIds.size()]))) {
                            ModifierItemCdn modifierItemCdn = GsonHelper.gsonFinal().fromJson(modifierItemDb.data, ModifierItemCdn.class);
                            ModifierItem modifierItem = ModifierItemCdn.convert(modifierItemCdn, LocaleUtil.getInstance().getLanguage());
                            modifierItem.getAttributes().setSelected(modifierItem.getAttributes().getMinimum() == 1);
                            modifierItems.add(modifierItem);
                        }
                        Collections.sort(modifierItems, new Comparator<ModifierItem>() {
                            @Override
                            public int compare(ModifierItem modifierItem1, ModifierItem modifierItem2) {
                                return modifierItem1.getAttributes().getDisplayOrder() - modifierItem2.getAttributes().getDisplayOrder();
                            }
                        });
                        ModifierGroup modifierGroup = ModifierGroupCdn.convert(modifierGroupCdn, LocaleUtil.getInstance().getLanguage());
                        ModifierGroup mapModifierGroup = modifierGroupIds.get(modifierGroup.getId());
                        if (mapModifierGroup != null) {
                            modifierGroup.getAttributes().setMinimum(mapModifierGroup.getMin());
                            modifierGroup.getAttributes().setMaximum(mapModifierGroup.getMax());
                            modifierGroup.getAttributes().setDisplayOrder(mapModifierGroup.getDisplayOrder());
                            modifierGroup.getAttributes().setFreeModifiersCount(mapModifierGroup.getFreeModifiersCount());
                            modifierGroup.setDefaultModifiers(new ArrayList<>(mapModifierGroup.getDefaultModifiers()));
                            modifierGroup.getAttributes().setVisibilityConfig(mapModifierGroup.getVisibilityConfig());
                        }
                        modifierGroup.setModifierItems(modifierItems);
                        if (modifierGroupDb.type == ModifierGroupType.SIZES.getValue()) {
                            sizes = modifierGroup;
                        } else {
                            //menuItemModifierGroups.put(modifierGroup.getId(), modifierGroup);
                            modifierGroups.add(modifierGroup);
                        }
                    }
                    menuItem.setSizes(sizes);

                    Collections.sort(modifierGroups, new Comparator<ModifierGroup>() {
                        @Override
                        public int compare(ModifierGroup modifierGroup1, ModifierGroup modifierGroup2) {
                            return modifierGroup1.getAttributes().getDisplayOrder() - modifierGroup2.getAttributes().getDisplayOrder();
                        }
                    });
                    menuItem.setModifierGroups(modifierGroups);

                    prepareMenuItem(menuItem);

                } else {
                    menuItemsCount++;
                    if (menuItemsCount < menuItems.size()) {
                        reorder();
                    } else {
                        menuItemsCount = 0;
                        ordersView.showCart();
                    }
                }

            }
        });

    }

    public void prepareMenuItem(MenuItem menuItem) {

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

        List<OrderModifierItem> selectedModifierItems = menuItems.get(menuItemsCount).getOrderModifierItems();
        Set<String> selectedModifierItemIds = new HashSet<>();
        for (OrderModifierItem modifierItem : selectedModifierItems) {
            selectedModifierItemIds.add(modifierItem.getAttributes().getId());
        }

        int selectedCount = 0;

        int comboIndex = 0;
        int trueIndex = 0;
        for (ModifierItem modifierItem : menuItem.getSizes().getModifierItems()) {
            boolean selected = selectedModifierItemIds.contains(modifierItem.getId());
            if (selected) {
                selectedCount++;
                trueIndex = comboIndex;
            }
            comboIndex++;
            modifierItem.getAttributes().setSelected(selected);
        }

        if (selectedCount == 0 && menuItem.getSizes().getModifierItems().size() > 0) {
            menuItem.getSizes().getModifierItems().get(0).getAttributes().setSelected(true);
        }

        VisibilityConfig visibilityConfig = menuItem.getSizes().getAttributes().getVisibilityConfig();
        if (visibilityConfig != null) {
            List<VisibilityConfigRule> rules = visibilityConfig.getRules();
            if (rules != null && rules.size() > 0) {
                VisibilityConfigRule rule = rules.get(0);
                if (rule.getTargetType() != null && rule.getTargetType().equalsIgnoreCase("modifier-group")) {
                    for (ModifierGroup modGroup : menuItem.getModifierGroups()) {
                        if (!menuItem.getSizes().getId().equals(modGroup.getId()) && rule.getTargetId().equals(modGroup.getId())) {
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
                            if (!group.getId().equals(menuItem.getSizes().getId()) && rule.getTargetId().equals(menuItem.getSizes().getId())) {
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
                            }

                            for (ModifierGroup modGroup : menuItem.getModifierGroups()) {
                                if (!group.getId().equals(modGroup.getId()) && rule.getTargetId().equals(modGroup.getId())) {
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

                            if (rule.getTargetId() != null && selectedModifiers.contains(rule.getTargetId())) {
                                if (rule.getTargetIs() != null && rule.getTargetIs().equalsIgnoreCase("selected")) {
                                    group.getAttributes().getVisibilityConfig().setInitialValue("");
                                } else {
                                    group.getAttributes().getVisibilityConfig().setInitialValue("hidden");
                                }
                            } else if (!rule.getTargetId().isEmpty()) {
                                group.getAttributes().getVisibilityConfig().setInitialValue("hidden");
                            }
                        }

                    }
                }
            }
        }

        HashMap<String, Integer> selectedCounts = new HashMap<>();

        for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
            selectedCount = 0;
            for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                if (selectedModifierItemIds.contains(modifierItem.getId())) {
                    selectedCount++;
                    modifierItem.getAttributes().setSelected(true);
                    for (OrderModifierItem selectedModifierItem : selectedModifierItems) {
                        if (selectedModifierItem.getAttributes().getId().equalsIgnoreCase(modifierItem.getId())) {
                            modifierItem.getAttributes().setQuantity(selectedModifierItem.getAttributes().getQuantity());
                            break;
                        }
                    }
                }
            }
            selectedCounts.put(modifierGroup.getId(), selectedCount);
        }

        for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
            if (modifierGroup.getModifierItems().size() > 0 && selectedCounts.containsKey(modifierGroup.getId()) && selectedCounts.get(modifierGroup.getId()) < modifierGroup.getAttributes().getMinimum()) {
                for (int i = 0; i < modifierGroup.getAttributes().getMinimum(); i++) {
                    if (i < modifierGroup.getModifierItems().size()) {
                        modifierGroup.getModifierItems().get(i).getAttributes().setSelected(true);
                    }
                }
            }
        }

        Set<String> removedIngredientIds = menuItems.get(menuItemsCount).getIngredientsIds();

        for (Ingredient ingredient : menuItem.getIngredients()) {
            if (removedIngredientIds.contains(ingredient.getId())) {
                ingredient.getAttributes().setSelected(false);
            }
        }

        boolean proceedAddingCombo = false;
        // Reorder Combo Item
        if (menuItem.isCombo) {
            ComboItemMain main = ifItemHasComboItem(menuItem.getId());
            if (main != null) {
                if (!menuItem.combo.isEmpty()) {
                    for (MenuItemCombo combo : menuItem.combo) {
                        if (!combo.components.isEmpty()) {
                            for (MenuItemComponents components : combo.components) {
                                if (!components.items.isEmpty()) {
                                    try {
                                        for (ComponentModifierItem compoItem : components.items) {
                                            List<ComboItem> comboItem = main.attributes.comboItems.stream().filter(it -> Objects.equals(it.id, compoItem.id)).collect(Collectors.toList());
                                            if (!comboItem.isEmpty()) {
                                                compoItem.modifierGroups = compoItem.modifierGroupsMap;
                                                if (!compoItem.modifierGroups.isEmpty()) {
                                                    for (ModifierGroup modifierGroup : compoItem.modifierGroups) {
                                                        modifierGroup.setModifierItems(modifierGroup.getModifiers());
                                                        for (int i = 0; i < modifierGroup.getModifierItems().size(); i++) {
                                                            for (ModifierItem modifierItem : comboItem.get(0).modifiers) {
                                                                if (modifierGroup.getModifierItems().get(i).getId().equals(modifierItem.getId())) {
                                                                    modifierGroup.getModifierItems().get(i).setPrice(modifierItem.getPrice());
                                                                    modifierGroup.getModifierItems().get(i).setQuantity(modifierItem.getQuantity());
                                                                    modifierGroup.getModifierItems().get(i).getAttributes().setSelected(true);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Collections.sort(components.items, (menuItem1, menuItem2) -> menuItem1.displayOrder - menuItem2.displayOrder);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                    try {
                        for (int x = 0; x < menuItem.combo.get(trueIndex).components.size(); x++) {
                            for (int i = 0; i < menuItem.combo.get(trueIndex).components.get(x).items.size(); i++) {
                                int comboItemMainIndex = -1;
                                for (int a = 0; a < main.attributes.comboItems.size(); a++) {
                                    if (menuItem.combo.get(trueIndex).components.get(x).items.get(i).id.equalsIgnoreCase(main.attributes.comboItems.get(a).id)) {
                                        menuItem.combo.get(trueIndex).components.get(x).items.get(i).selected = true;
                                        comboItemMainIndex = a;
                                        break;
                                    }
                                }
                                if (comboItemMainIndex != -1) {
                                    for (int m = 0; m < menuItem.combo.get(trueIndex).components.get(x).items.get(i).modifierGroups.size(); m++) {
                                        for (int mm = 0; mm < menuItem.combo.get(trueIndex).components.get(x).items.get(i).modifierGroups.get(m).getModifierItems().size(); mm++) {
                                            for (int xx = 0; xx < main.attributes.comboItems.get(comboItemMainIndex).modifiers.size(); xx++) {
                                                if (menuItem.combo.get(trueIndex).components.get(x).items.get(i).modifierGroups.get(m).getModifierItems().get(mm).getId().equalsIgnoreCase(main.attributes.comboItems.get(comboItemMainIndex).modifiers.get(xx).getId())) {
                                                    menuItem.combo.get(trueIndex).components.get(x).items.get(i).modifierGroups.get(m).getModifierItems().get(mm).getAttributes().setSelected(true);
                                                    menuItem.combo.get(trueIndex).components.get(x).items.get(i).modifierGroups.get(m).getModifierItems().get(mm).getAttributes().setQuantity(main.attributes.comboItems.get(comboItemMainIndex).modifiers.get(xx).getQuantity());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    proceedAddingCombo = true;
                }
            }
        }


        Store store = CacheUtil.getInstance().getOrderStore();
        if (store == null || !menuItem.getDisabledStores().containsKey(store.getId())) {
            if (!menuItem.isCombo) {
                addMenuItem(menuItem);
            } else if (proceedAddingCombo) {
                addMenuItem(menuItem);
            }
        }

        menuItemsCount++;
        if (menuItemsCount < menuItems.size()) {
            reorder();
        } else {
            menuItemsCount = 0;
            ordersView.showCart();
        }

    }

    private void addMenuItem(MenuItem menuItem) {
        menuItem.getAttributes().setReorder(true);
        int cartCount = CacheUtil.getInstance().getCartQuantity();
        ordersView.logFirebaseEvent("add_to_cart_event");
        CacheUtil.getInstance().addToCart(menuItem);
        removeMukafaaRedemption();
        if (cartCount == 0) {
            ordersView.setCartExpiry();
        }
    }

    private ComboItemMain ifItemHasComboItem(String id) {
        for (ComboItemMain main : comboItems) {
            if (main.attributes != null && main.attributes.itemId != null && main.attributes.itemId.equalsIgnoreCase(id)) {
                return main;
            }
        }
        return null;
    }

    @Override
    public void getModifiers(String menuCategoryId, MenuItem menuItem) {
        MenuApiCall.getInstance().getModifierGroups(CacheUtil.getInstance().getMenuId(), menuCategoryId, menuItem.getId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {

                    JSONObject object = new JSONObject(o.toString());

                    ArrayList<ModifierGroup> modifierGroups = GsonHelper.gsonFinal().fromJson(object.getJSONArray("data").toString(), new TypeToken<ArrayList<ModifierGroup>>() {
                    }.getType());

                    for (ModifierGroup modifierGroup : modifierGroups) {

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

                        if (menuItem.getSizes().getType() == ModifierGroupType.OPTIONS && (code.equals("sizes") || type.equals("sizes") || label.equals("sizes"))) {
                            modifierGroup.setType(ModifierGroupType.SIZES);
                            menuItem.setSizes(modifierGroup);
                        } else {
                            modifierGroup.setType(ModifierGroupType.OPTIONS);
                            List<ModifierGroup> items = new ArrayList<>(menuItem.getModifierGroups());
                            items.add(modifierGroup);
                            menuItem.setModifierGroups(items);
                        }
                    }

                    if (object.has("included")) {
                        ArrayList<ModifierItem> modifierItems = GsonHelper.gsonFinal().fromJson(object.getJSONArray("included").toString(), new TypeToken<ArrayList<ModifierItem>>() {
                        }.getType());
                        for (ModifierItem modifierItem : modifierItems) {
                            if (menuItem.getSizes().getModifierItemsIds().contains(modifierItem.getId())) {
                                List<ModifierItem> items = new ArrayList<>(menuItem.getSizes().getModifierItems());
                                items.add(modifierItem);
                                menuItem.getSizes().setModifierItems(items);
                            } else {
                                for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                                    if (modifierGroup.getModifierItemsIds().contains(modifierItem.getId())) {
                                        List<ModifierItem> items = new ArrayList<>(modifierGroup.getModifierItems());
                                        items.add(modifierItem);
                                        modifierGroup.setModifierItems(items);
                                        break;
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

                    List<OrderModifierItem> selectedModifierItems = menuItems.get(menuItemsCount).getOrderModifierItems();
                    Set<String> selectedModifierItemIds = new HashSet<>();
                    for (OrderModifierItem modifierItem : selectedModifierItems) {
                        selectedModifierItemIds.add(modifierItem.getAttributes().getId());
                    }

                    int selectedCount = 0;

                    for (ModifierItem modifierItem : menuItem.getSizes().getModifierItems()) {
                        boolean selected = selectedModifierItemIds.contains(modifierItem.getId());
                        if (selected) {
                            selectedCount++;
                        }
                        modifierItem.getAttributes().setSelected(selected);
                    }

                    if (selectedCount == 0 && menuItem.getSizes().getModifierItems().size() > 0) {
                        menuItem.getSizes().getModifierItems().get(0).getAttributes().setSelected(true);
                    }

                    HashMap<String, Integer> selectedCounts = new HashMap<>();

                    for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                        selectedCount = 0;
                        for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                            if (selectedModifierItemIds.contains(modifierItem.getId())) {
                                selectedCount++;
                                modifierItem.getAttributes().setSelected(true);
                                for (OrderModifierItem selectedModifierItem : selectedModifierItems) {
                                    if (selectedModifierItem.getAttributes().getId().equalsIgnoreCase(modifierItem.getId())) {
                                        modifierItem.getAttributes().setQuantity(selectedModifierItem.getAttributes().getQuantity());
                                        break;
                                    }
                                }
                            }
                        }
                        selectedCounts.put(modifierGroup.getId(), selectedCount);
                    }

                    for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                        if (modifierGroup.getModifierItems().size() > 0 && selectedCounts.containsKey(modifierGroup.getId()) && selectedCounts.get(modifierGroup.getId()) < modifierGroup.getAttributes().getMinimum()) {
                            for (int i = 0; i < modifierGroup.getAttributes().getMinimum(); i++) {
                                if (i < modifierGroup.getModifierItems().size()) {
                                    modifierGroup.getModifierItems().get(i).getAttributes().setSelected(true);
                                }
                            }
                        }
                    }

                    for (ModifierGroup group : menuItem.getModifierGroups()) {
                        VisibilityConfig visibilityConfig = group.getAttributes().getVisibilityConfig();
                        if (visibilityConfig != null) {
                            List<VisibilityConfigRule> rules = visibilityConfig.getRules();
                            if (rules != null && rules.size() > 0) {
                                VisibilityConfigRule rule = rules.get(0);
                                if (rule.getTargetType() != null && rule.getTargetType().equalsIgnoreCase("modifier-group")) {
                                    for (ModifierGroup modGroup : menuItem.getModifierGroups()) {
                                        if (!group.getId().equals(modGroup.getId()) && rule.getTargetId().equals(modGroup.getId())) {
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
                                }

                            }
                        }
                    }

                    getIngredients(menuCategoryId, menuItem);

                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                }

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                menuItemsCount++;
                if (menuItemsCount < menuItems.size()) {
                    reorder();
                } else {
                    menuItemsCount = 0;
                    ordersView.showCart();
                }
            }
        });
    }

    @Override
    public void getIngredients(String menuCategoryId, MenuItem menuItem) {
        MenuApiCall.getInstance().getCategoryItemIngredients(CacheUtil.getInstance().getMenuId(), menuCategoryId, menuItem.getId(), "1", new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                try {

                    JSONObject object = new JSONObject(o.toString());

                    LinkedList<Ingredient> ingredients = GsonHelper.gsonFinal().fromJson(object.getJSONArray("data").toString(), new TypeToken<LinkedList<Ingredient>>() {
                    }.getType());

                    Set<String> removedIngredientIds = menuItems.get(menuItemsCount).getIngredientsIds();

                    for (Ingredient ingredient : ingredients) {
                        if (removedIngredientIds.contains(ingredient.getId())) {
                            ingredient.getAttributes().setSelected(false);
                        }
                    }

                    menuItem.setIngredients(ingredients);

                    Store store = CacheUtil.getInstance().getOrderStore();
                    if (store == null || !menuItem.getDisabledStores().containsKey(store.getId())) {
                        menuItem.getAttributes().setReorder(true);
                        int cartCount = CacheUtil.getInstance().getCartQuantity();
                        removeMukafaaRedemption();
                        ordersView.logFirebaseEvent("add_to_cart_event");
                        CacheUtil.getInstance().addToCart(menuItem);
                        if (cartCount == 0) {
                            ordersView.setCartExpiry();
                        }
                    }

                    menuItemsCount++;
                    if (menuItemsCount < menuItems.size()) {
                        reorder();
                    } else {
                        menuItemsCount = 0;
                        ordersView.showCart();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                }


            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                menuItemsCount++;
                if (menuItemsCount < menuItems.size()) {
                    reorder();
                } else {
                    menuItemsCount = 0;
                    ordersView.showCart();
                }
            }
        });
    }

    public ArrayList<ModifierGroup> getModifierGroups(int itemId) {
        AppDatabase appDatabase = AppDatabase.getInstance();
        String data = getMenuItemData(appDatabase, String.valueOf(itemId));
        if (data != null && !data.isEmpty()) {
            try {
                String applicationId = CacheUtil.getInstance().getApplication().getId();
                MenuItem menuItemNew = MenuUtil.processMenuItemCustomObject(GsonHelper.gsonFinal().fromJson(data, MenuItem.class));
                LinkedList<Allergen> allergens = new LinkedList<>();
                for (AllergenDb allergenDb : appDatabase.allergenDao().findAllByApplicationAndIds(applicationId, menuItemNew.getAllergensIds().toArray(new String[menuItemNew.getAllergensIds().size()]))) {
                    AllergenCdn allergenCdn = GsonHelper.gsonFinal().fromJson(allergenDb.data, AllergenCdn.class);
                    allergens.add(AllergenCdn.convert(allergenCdn, LocaleUtil.getInstance().getLanguage()));
                }
                Collections.sort(allergens, new Comparator<Allergen>() {
                    @Override
                    public int compare(Allergen allergen1, Allergen allergen2) {
                        return allergen1.getAttributes().getDisplayOrder() - allergen2.getAttributes().getDisplayOrder();
                    }
                });
                menuItemNew.setAllergens(allergens);

                LinkedList<Ingredient> ingredients = new LinkedList<>();
                ArrayList<String> ingredientIds = new ArrayList<>();
                for (Ingredient ingredient : menuItemNew.getIngredientIds()) {
                    ingredientIds.add(ingredient.getId());
                }
                for (IngredientDb ingredientDb : appDatabase.ingredientDao().findAllByApplicationAndIds(applicationId, ingredientIds.toArray(new String[ingredientIds.size()]))) {
                    IngredientCdn ingredientCdn = GsonHelper.gsonFinal().fromJson(ingredientDb.data, IngredientCdn.class);
                    ingredients.add(IngredientCdn.convert(ingredientCdn, LocaleUtil.getInstance().getLanguage()));
                }
                Collections.sort(ingredients, new Comparator<Ingredient>() {
                    @Override
                    public int compare(Ingredient ingredient1, Ingredient ingredient2) {
                        return ingredient1.getAttributes().getDisplayOrder() - ingredient2.getAttributes().getDisplayOrder();
                    }
                });
                menuItemNew.setIngredients(ingredients);

                ModifierGroup sizes = new ModifierGroup();
                LinkedList<ModifierGroup> modifierGroups = new LinkedList<>();
                HashMap<String, ModifierGroup> modifierGroupIds = new HashMap<>();
                for (ModifierGroup modifierGroup : menuItemNew.getModifierGroupIds()) {
                    modifierGroupIds.put(modifierGroup.getId(), modifierGroup);
                }
                for (ModifierGroupDb modifierGroupDb : appDatabase.modifierGroupDao().findAllByApplicationAndIds(applicationId, modifierGroupIds.keySet().toArray(new String[modifierGroupIds.size()]))) {
                    ModifierGroupCdn modifierGroupCdn = MenuUtil.processModifierGroupCustomObject(GsonHelper.gsonFinal().fromJson(modifierGroupDb.data, ModifierGroupCdn.class));
                    LinkedList<ModifierItem> modifierItems = new LinkedList<>();
                    for (ModifierItemDb modifierItemDb : appDatabase.modifierItemDao().findAllByApplicationAndIds(applicationId, modifierGroupCdn.getAttributes().getModifiers().toArray(new String[modifierGroupCdn.getAttributes().getModifiers().size()]))) {
                        ModifierItemCdn modifierItemCdn = GsonHelper.gsonFinal().fromJson(modifierItemDb.data, ModifierItemCdn.class);
                        ModifierItem modifierItem = ModifierItemCdn.convert(modifierItemCdn, LocaleUtil.getInstance().getLanguage());
                        modifierItem.getAttributes().setSelected(modifierItem.getAttributes().getMinimum() == 1);
                        modifierItems.add(modifierItem);
                    }
                    Collections.sort(modifierItems, new Comparator<ModifierItem>() {
                        @Override
                        public int compare(ModifierItem modifierItem1, ModifierItem modifierItem2) {
                            return modifierItem1.getAttributes().getDisplayOrder() - modifierItem2.getAttributes().getDisplayOrder();
                        }
                    });
                    ModifierGroup modifierGroup = ModifierGroupCdn.convert(modifierGroupCdn, LocaleUtil.getInstance().getLanguage());
                    ModifierGroup mapModifierGroup = modifierGroupIds.get(modifierGroup.getId());
                    if (mapModifierGroup != null) {
                        modifierGroup.getAttributes().setMinimum(mapModifierGroup.getMin());
                        modifierGroup.getAttributes().setMaximum(mapModifierGroup.getMax());
                        modifierGroup.setExcludeModifiers(new ArrayList<>(mapModifierGroup.getExcludeModifiers()));
                        modifierGroup.setDefaultModifiers(new ArrayList<>(mapModifierGroup.getDefaultModifiers()));
                    }
                    int size = modifierItems.size();
                    ArrayList<ModifierItem> modifierItemsFinal = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        boolean found = false;
                        for (int a = 0; a < modifierGroup.getExcludeModifiers().size(); a++) {
                            if (modifierItems.get(i).getId().equalsIgnoreCase(String.valueOf(modifierGroup.getExcludeModifiers().get(a)))) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            modifierItemsFinal.add(modifierItems.get(i));
                        }
                    }
                    modifierGroup.setModifierItems(modifierItems);
                    if (modifierGroupDb.type == ModifierGroupType.SIZES.getValue()) {
                        sizes = modifierGroup;
                    } else {
                        modifierGroups.add(modifierGroup);
                    }
                }
                menuItemNew.setSizes(sizes);
                Collections.sort(modifierGroups, new Comparator<ModifierGroup>() {
                    @Override
                    public int compare(ModifierGroup modifierGroup1, ModifierGroup modifierGroup2) {
                        return modifierGroup1.getAttributes().getDisplayOrder() - modifierGroup2.getAttributes().getDisplayOrder();
                    }
                });
                menuItemNew.setModifierGroups(modifierGroups);

                LinkedList<ModifierGroup> filteredModifierGroups = new LinkedList<>();

                for (ModifierGroup modifierGroup : menuItemNew.getModifierGroups()) {
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

                if (menuItemNew.getSizes().getModifierItems().size() > 0) {
                    menuItemNew.getSizes().getModifierItems().get(0).getAttributes().setSelected(true);
                }

                menuItemNew.setModifierGroups(filteredModifierGroups);

                for (ModifierGroup modifierGroup : menuItemNew.getModifierGroups()) {
                    Set<String> defaultModifiers = modifierGroup.getDefaultModifiers();
                    if (modifierGroup.getModifierItems().size() > 0 && modifierGroup.getAttributes().getMinimum() > 0) {
                        if (defaultModifiers.size() > 0) {
                            int selected = 0;
                            for (int i = 0; i < modifierGroup.getModifierItems().size(); i++) {
                                ModifierItem modifierItem = modifierGroup.getModifierItems().get(i);
                                if (selected < modifierGroup.getAttributes().getMaximum() && defaultModifiers.contains(modifierItem.getId())) {
                                    modifierGroup.getModifierItems().get(i).getAttributes().setSelected(true);
                                    selected++;
                                }
                            }
                        } else if (modifierGroup.getAttributes().getMinimum() == 1) {
                            modifierGroup.getModifierItems().get(0).getAttributes().setSelected(true);
                        }
                    }

                }

                VisibilityConfig visibilityConfig = menuItemNew.getSizes().getAttributes().getVisibilityConfig();
                if (visibilityConfig != null) {
                    List<VisibilityConfigRule> rules = visibilityConfig.getRules();
                    if (rules != null && rules.size() > 0) {
                        VisibilityConfigRule rule = rules.get(0);
                        if (rule.getTargetType() != null && rule.getTargetType().equalsIgnoreCase("modifier-group")) {
                            for (ModifierGroup modGroup : menuItemNew.getModifierGroups()) {
                                if (!menuItemNew.getSizes().getId().equals(modGroup.getId()) && rule.getTargetId().equals(modGroup.getId())) {
                                    if (rule.getTargetIs() != null && rule.getTargetIs().equalsIgnoreCase("selected")) {
                                        int selected = 0;
                                        for (ModifierItem modItem : modGroup.getModifierItems()) {
                                            if (modItem.getAttributes().isSelected()) {
                                                selected += 1;
                                            }
                                        }
                                        if (selected > 0) {
                                            menuItemNew.getSizes().getAttributes().getVisibilityConfig().setInitialValue("");
                                        } else {
                                            menuItemNew.getSizes().getAttributes().getVisibilityConfig().setInitialValue("hidden");
                                        }
                                    }
                                }

                            }
                        }

                    }
                }

                for (ModifierGroup group : menuItemNew.getModifierGroups()) {
                    visibilityConfig = group.getAttributes().getVisibilityConfig();
                    if (visibilityConfig != null) {
                        List<VisibilityConfigRule> rules = visibilityConfig.getRules();
                        if (rules != null && rules.size() > 0) {
                            VisibilityConfigRule rule = rules.get(0);
                            if (rule.getTargetType() != null) {
                                if (rule.getTargetType().equalsIgnoreCase("modifier-group")) {
                                    if (!group.getId().equals(menuItemNew.getSizes().getId()) && rule.getTargetId().equals(menuItemNew.getSizes().getId())) {
                                        if (rule.getTargetIs() != null && rule.getTargetIs().equalsIgnoreCase("selected")) {
                                            int selected = 0;
                                            for (ModifierItem modItem : menuItemNew.getSizes().getModifierItems()) {
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
                                    }

                                    for (ModifierGroup modGroup : menuItemNew.getModifierGroups()) {
                                        if (!group.getId().equals(modGroup.getId()) && rule.getTargetId().equals(modGroup.getId())) {
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
                                    for (ModifierItem modItem : menuItemNew.getSizes().getModifierItems()) {
                                        if (modItem.getAttributes().isSelected()) {
                                            selectedModifiers.add(modItem.getId());
                                            break;
                                        }
                                    }

                                    for (ModifierGroup modGroup : menuItemNew.getModifierGroups()) {
                                        if (!group.getId().equals(modGroup.getId())) {
                                            for (ModifierItem modItem : modGroup.getModifierItems()) {
                                                if (modItem.getAttributes().isSelected()) {
                                                    selectedModifiers.add(modItem.getId());
                                                }
                                            }
                                        }
                                    }

                                    //for (ModifierGroup modGroup : menuItemNew.getModifierGroups()) {
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
                return new ArrayList<>(menuItemNew.getModifierGroups());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new ArrayList<>();
    }

    @Override
    public boolean isMukafaaEnabled() {
        boolean isMukafaaEnabledInApp = false;
        Application app = CacheUtil.getInstance().getApplication();
        if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
            isMukafaaEnabledInApp = app.getAttributes().getAllowedPaymentsDeliver().contains("alrajhi");
        } else {
            isMukafaaEnabledInApp = app.getAttributes().getAllowedPaymentsPickup().contains("alrajhi");
        }
        return isMukafaaEnabledInApp;
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
    public boolean getFromAbly() {
        return fromAbly;
    }

    @Override
    public void setFromAblyToFalse() {
        fromAbly = false;
    }

    private ArrayList<LoyaltyPointsTransaction> transactions;

    @Override
    public LoyaltyPointsTransaction getMukafaaTransaction() {
        if (transactions != null) {
            for (LoyaltyPointsTransaction transaction : transactions) {
                if (transaction.getAttributes() != null && transaction.getAttributes().getProvider() != null && Values.MUKAFAA_PROVIDER.equalsIgnoreCase(transaction.getAttributes().getProvider())) {
                    return transaction;
                }
            }
        }
        return null;
    }

    @Override
    public ArrayList<LoyaltyPointsTransaction> getLoyaltyPointsTransaction() {
        return transactions;
    }

    @Override
    public LoyaltyPointsTransaction getSoloLoyaltyTransaction() {
        if (transactions != null) {
            for (LoyaltyPointsTransaction transaction : transactions) {
                if (transaction.getAttributes() != null && transaction.getAttributes().getProvider() != null && Values.LOYALTY_PROVIDER.equalsIgnoreCase(transaction.getAttributes().getProvider()) && transaction.getAttributes().getType().equalsIgnoreCase("redeem")) {
                    return transaction;
                }
            }
        }
        return null;
    }

    @Override
    public List<MenuItem> getOrderItems() {
        return menuItems;
    }

    @Override
    public ArrayList<OrderHistory> getOrderHistories() {
        return orders;
    }

    @Override
    public void setOrders(ArrayList<OrderHistory> orders) {
        this.orders = orders;
    }

    private void subscribeToOrderStatusUpdate(String orderId) {
        AblyUtil ablyUtil = AblyUtil.Companion.getInstance();
        ablyUtil.subscribeToOrderStatusUpdate(orderId, OrdersPresenter.this::onOrderDetailsStatusUpdate);
    }

    private void subscribeToOrderDriverAssign(String orderId) {
        if (order.getDeliveryOption().equalsIgnoreCase("deliver") || order.getDeliveryOption().equalsIgnoreCase("ايصال")) {
            AblyUtil ablyUtil = AblyUtil.Companion.getInstance();
            String driverId = order.getDriverId();
            if (driverId == null || driverId.isEmpty()) {
                ablyUtil.subscribeToOrderDriverAssign(orderId, OrdersPresenter.this::onOrderDriverAssign);
            } else {
                subscribeToOrderDriverLocationUpdate(orderId);
            }
        }
    }

    private void subscribeToOrderDriverLocationUpdate(String orderId) {
        isLocationUpdateSubscribed = true;
        AblyUtil ablyUtil = AblyUtil.Companion.getInstance();
        ablyUtil.subscribeToOrderDriverLocationUpdate(orderId, OrdersPresenter.this::onOrderDriverLocationUpdate);
    }

    private Unit onOrderDetailsStatusUpdate(Message message) {
        fromAbly = true;
        try {
            JSONObject jsonObject = new JSONObject(message.data.toString());
            JSONObject orderObject = jsonObject.optJSONObject("order");
            if(orderObject != null) {
                OrderData orderData = OrderData.parse(jsonObject.getJSONObject("order"));
                OrderStatus orderStatus = orderData.getOrderStatus();
                if (orderStatus != null && orderStatus.getSequence() >= OrderStatusType.DELIVERY_IN_PROGRESS.getValue() && orderStatus.getSequence() <= OrderStatusType.ARRIVED_AT_CUSTOMER.getValue()) {
                    JSONObject lyveDriverObject = orderObject.optJSONObject("lyve-driver");
                    if (lyveDriverObject != null) {
                        LyveDriver lyveDriver = new Gson().fromJson(lyveDriverObject.toString(), LyveDriver.class);
                        if (lyveDriver != null) {
                            driver = lyveDriver;
                            ordersView.showWhatsApp(driver.getEmployee().getMobile());
                            LatLng customerLatLng = new LatLng(order.getCustomerLat(), order.getCustomerLng());
                            LatLng driverLatLng = new LatLng(Double.parseDouble(lyveDriver.getLat()), Double.parseDouble(lyveDriver.getLng()));
                            ordersView.showDriver(customerLatLng, driverLatLng, lyveDriver.getEta());
                        } else {
                           getDriver();
                        }
                    } else {
                        getDriver();
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        getOrderStatuses();
        getOrder(order.getId(), -1, true, false);
        return Unit.INSTANCE;
    }

    private Unit onOrderDriverAssign(Message message) {
        fromAbly = true;
        AblyUtil ablyUtil = AblyUtil.Companion.getInstance();
        ablyUtil.detachToOrderDriverAssign(order.getId());
        getDriver();
        return Unit.INSTANCE;
    }

    private Unit onOrderDriverLocationUpdate(Message message) {
        OrderStatus orderStatus = order.getOrderStatus();
        if (orderStatus != null && orderStatus.getSequence() >= OrderStatusType.DELIVERY_IN_PROGRESS.getValue() && orderStatus.getSequence() <= OrderStatusType.ARRIVED_AT_CUSTOMER.getValue()) {
            try {
                JSONObject jsonObject = new JSONObject(message.data.toString());
                DeliveryDriver appDriver = new Gson().fromJson(jsonObject.toString(), DeliveryDriver.class);
                ordersView.updateDriver(appDriver);
            } catch (JSONException e) {
                getDriver();
                e.printStackTrace();
            }
        }
        return Unit.INSTANCE;
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
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        ordersView.showLocation(false, latLng, getAddressLoc(latLng));
    }


    @Override
    public void setGeocoder(Geocoder geocoder) {
        this.geocoder = geocoder;
    }

    private Geocoder geocoder;

    private android.location.Address addressNew;

    private Place place;

    public String getAddressLoc(LatLng latLng) {
        if (geocoder != null) {
            try {
                List<android.location.Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                addressNew = (addresses.isEmpty() ? null : addresses.get(0));
                if (addressNew != null) {
                    place = null;
                    return addressNew.getAddressLine(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }
}
