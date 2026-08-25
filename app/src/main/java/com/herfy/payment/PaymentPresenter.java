package com.herfy.payment;

import android.location.Geocoder;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.herfy.BuildConfig;
import com.herfy.analytics.kochava.KochavaEventUtil;
import com.herfy.analytics.kochava.KochavaTrackerUtil;
import com.herfy.summary.OrderSummaryMenuItemViewHolder;
import com.herfy.util.AppLogger;
import com.herfy.util.ShipBookLogger;
import com.herfy.util.StringUtil;
import com.herfy.utils.constants.Text;
import com.herfy.utils.constants.Values;
import com.herfy.utils.handler.ErrorHandler;
import com.herfy.utils.helper.ApiLogLevel;
import com.herfy.utils.helper.ApiLogUtil;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.BuildUtil;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.utils.helper.TimeUtil;
import com.kochava.tracker.events.EventType;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.CouponsApiCall;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.calls.LocationApiCall;
import com.skylinedynamics.solosdk.api.calls.OrdersApiCall;
import com.skylinedynamics.solosdk.api.calls.PrayerTimesApiCall;
import com.skylinedynamics.solosdk.api.calls.UtilitiesApiCall;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.Application;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.Concept;
import com.skylinedynamics.solosdk.api.models.objects.Customer;
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard;
import com.skylinedynamics.solosdk.api.models.objects.Ingredient;
import com.skylinedynamics.solosdk.api.models.objects.Loyalty;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyV2;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderData;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.PaymentType;
import com.skylinedynamics.solosdk.api.models.objects.PrayerTime;
import com.skylinedynamics.solosdk.api.models.objects.Promotion;
import com.skylinedynamics.solosdk.api.models.objects.ServiceCharge;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfig;
import com.skylinedynamics.solosdk.util.GsonHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentPresenter implements PaymentContract.Presenter {

    private final PaymentContract.View paymentView;

    private double cashPresented = 0;

    List<MenuItem> menuItems = new ArrayList<>();
    private String orderId;
    private OrderType orderType;
    private double orderTotalPrice;

    private Store storeSelected;

    private double validatedPromoItemPrice = 0.0;
    private int validatedPromoItemQuantity = 0;
    private double validatedSubtotal = 0.0;
    private double validatedDiscount = 0.0;
    private double validatedRound;
    private Double validatedDeliveryCharge = 0.0;
    private ArrayList<ServiceCharge> validatedCharges = new ArrayList<>();
    private Double validatedTotal = 0.0;
    private int loyaltyPointsUsed;
    private int pointsEarnable;
    private String mukafaaTransactionId;
    private JsonObject validatedBody = null;
    private double vatAmountFromResponse;
    List<MenuItem> orderedMenuItem = new ArrayList<>();
    List<MenuItem> promoItems = new ArrayList<>();

    public PaymentPresenter(PaymentContract.View paymentView) {
        this.paymentView = paymentView;

        this.paymentView.setPresenter(this);
        menuItems = CacheUtil.getInstance().getCart();
        try {
            if (CacheUtil.getInstance().getCouponMenuItem() != null) {
                MenuItem promo = CacheUtil.getInstance().getCouponMenuItem();
                promo.setPromo(true);
                menuItems.add(promo);
            }
            if (CacheUtil.getInstance().getCampaignPromotionMenuItem() != null) {
                MenuItem promo = CacheUtil.getInstance().getCampaignPromotionMenuItem();
                promo.setPromo(true);
                menuItems.add(promo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        mukafaaTransactionId = CacheUtil.getInstance().getMukafaaTransactionId();
        paymentView.setupTranslations();
        paymentView.setupFonts();
        storeSelected = CacheUtil.getInstance().getOrderStore();
    }

    @Override
    public void getPrayerTime() {
        PrayerTimesApiCall.getInstance().getPrayerTimeNew(String.valueOf(storeSelected.getAttributes().getLng()), String.valueOf(storeSelected.getAttributes().getLat()), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    PrayerTime prayerTime = GsonHelper.gsonFinal().fromJson(o.toString(), PrayerTime.class);
                    paymentView.showPrayerTime(prayerTime);
                } catch (Exception e) {
                    e.printStackTrace();
                    this.onError(e);
                }
            }

            @Override
            public void onError(Object o) {
            }
        });
    }

    @Override
    public void getPaymentTypes() {
        Application application = CacheUtil.getInstance().getApplication();

        PaymentType previousPaymentType = CacheUtil.getInstance().getOrderPayment();

        ArrayList<PaymentType> paymentTypes = new ArrayList<>();

        if (getTotalPrice() == 0) {
            paymentTypes.add(PaymentType.CASH);
        } else {
            if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
                paymentTypes.addAll(application.getAttributes().getDeliverPaymentTypes());
            } else {
                paymentTypes.addAll(application.getAttributes().getPickupPaymentTypes());
            }
            double maximumOrderAmountCash = CacheUtil.getInstance().getConcept().getMaximumOrderAmountCash();
            if (maximumOrderAmountCash > 0 && getTotalPrice() > maximumOrderAmountCash) {
                paymentTypes.remove(PaymentType.CASH);
            }
        }


        if (paymentTypes.contains(previousPaymentType)) {
            paymentTypes.remove(previousPaymentType);
            paymentTypes.add(0, previousPaymentType);
        }
        paymentView.showPaymentTypes(paymentTypes);
    }

    @Override
    public void getStore(Geocoder geocoder) {
        LocationApiCall.getInstance().getClosestStoreToAddress(AuthUtil.getInstance().getCustomer().getId(), CacheUtil.getInstance().getOrderAddress().getId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                try {
                    JSONObject jsonObject = new JSONObject(o.toString());
                    Store store = Store.parseStore(jsonObject.getJSONObject("data"));
                    CacheUtil.getInstance().setOrderStore(store);
                    getLocation(geocoder);
                    validateOrder(false, store, null, loyaltyPointsUsed, false);
                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                paymentView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.SENDING_ORDER_ERROR), null));
            }
        });
    }

    @Override
    public void getAddresses(Geocoder geocoder) {

        Store store = CacheUtil.getInstance().getOrderStore();

        if (store != null && StringUtil.hasOrderLatLong() && (CacheUtil.getInstance().pickupTypes())) {
            LatLng originPosition = new LatLng(Double.parseDouble(CacheUtil.getInstance().getOrderLatitude()), Double.parseDouble(CacheUtil.getInstance().getOrderLongitude()));
            LatLng destinationPosition = new LatLng(store.getAttributes().getLat(), store.getAttributes().getLng());
            paymentView.setStartName(AuthUtil.getInstance().getCustomer().getAttributes().getFirstName() + " " + AuthUtil.getInstance().getCustomer().getAttributes().getLastName());
            try {
                List<android.location.Address> addresses = geocoder.getFromLocation(originPosition.latitude, originPosition.longitude, 1);
                android.location.Address address = (addresses.isEmpty() ? null : addresses.get(0));
                if (address != null) {
                    paymentView.setStartAddress(address.getAddressLine(0));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            paymentView.setEndName(store.getAttributes().getName());
            try {
                List<android.location.Address> addresses = geocoder.getFromLocation(destinationPosition.latitude, destinationPosition.longitude, 1);
                android.location.Address address = (addresses.isEmpty() ? null : addresses.get(0));
                if (address != null) {
                    paymentView.setEndAddress(address.getAddressLine(0));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    @Override
    public void getLocation(Geocoder geocoder) {
        LatLng originPosition = null;
        LatLng destinationPosition = null;

        Store store = CacheUtil.getInstance().getOrderStore();

        OrderType orderType = CacheUtil.getInstance().getOrderType();

        if (store != null && CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
            originPosition = new LatLng(store.getAttributes().getLat(), store.getAttributes().getLng());
            paymentView.setStartName(store.getAttributes().getName());

            try {
                List<android.location.Address> addresses = geocoder.getFromLocation(originPosition.latitude, originPosition.longitude, 1);
                android.location.Address address = (addresses.isEmpty() ? null : addresses.get(0));
                if (address != null) {
                    paymentView.setStartAddress(address.getAddressLine(0));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        Address address = CacheUtil.getInstance().getOrderAddress();
        if (address != null) {
            destinationPosition = new LatLng(Double.parseDouble(address.getAttributes().getLat()), Double.parseDouble(address.getAttributes().getLongt()));
            paymentView.setEndName(AuthUtil.getInstance().getCustomer().getAttributes().getFirstName() + " " + AuthUtil.getInstance().getCustomer().getAttributes().getLastName());
            paymentView.setEndAddress(address.getAttributes().getLine1());
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        if (originPosition != null) {
            builder.include(originPosition);
        }

        if (destinationPosition != null) {
            builder.include(destinationPosition);
        }

        paymentView.updateMap(orderType, originPosition, destinationPosition, builder.build());

    }

    @Override
    public double getMenuItemTotalPrice(boolean isUpsell, MenuItem item, int modifierGroupPosition) {
        String orderStoreId = null;
        int counter = 0;
        int comboIndex = 0;
        if (CacheUtil.getInstance().getOrderStore() != null) {
            orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
        }
        double price = item.getPrice(orderStoreId);
        double totalPrice = (price == 0.0) ? item.getAttributes().getPrice() : price;
        if (item.isPromo()) {
            totalPrice = item.getAttributes().getPrice();
        }

        if (BuildUtil.isEkfc() && item.getTotal() != 0.0) {
            totalPrice = item.getTotal();
        } else {
            if (item.getSizes().getModifierItems().size() > 0) {
                if (!item.isPromo()) {
                    totalPrice = 0.0;
                }
                for (ModifierItem modifierItem : item.getSizes().getModifierItems()) {
                    if (modifierItem.getAttributes().isSelected()) {
                        totalPrice += modifierItem.getAttributes().getPrice();
                        comboIndex = counter;
                        break;
                    }
                    counter++;
                }

                if (item.isCombo && item.isPromo()) {
                    totalPrice = item.getAttributes().getPrice();
                }
            }
            for (ModifierItem modifierItem : item.getDrinks().getModifierItems()) {
                if (modifierItem.getAttributes().isSelected()) {
                    totalPrice += modifierItem.getAttributes().getPrice();
                    break;
                }
            }
            for (ModifierItem modifierItem : item.getFries().getModifierItems()) {
                if (modifierItem.getAttributes().isSelected()) {
                    totalPrice += modifierItem.getAttributes().getPrice();
                    break;
                }
            }
            for (ModifierGroup modifierGroup : item.getModifierGroups()) {
                VisibilityConfig visibilityConfig = modifierGroup.getAttributes().getVisibilityConfig();
                if (visibilityConfig == null || visibilityConfig.getInitialValue() == null || !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
                    int selectedFreeModifier = 0;
                    for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                        if (modifierItem.getAttributes().isSelected()) {
                            if (modifierGroup.getAttributes().getFreeModifiersCount() > 0 && selectedFreeModifier < modifierGroup.getAttributes().getFreeModifiersCount()) {
                                int totalSelectedFreeModifier = selectedFreeModifier + modifierItem.getAttributes().getQuantity();
                                if (totalSelectedFreeModifier < modifierGroup.getAttributes().getFreeModifiersCount()) {
                                    selectedFreeModifier += modifierItem.getAttributes().getQuantity();
                                } else {
                                    selectedFreeModifier = modifierGroup.getAttributes().getFreeModifiersCount();
                                    totalPrice += modifierItem.getAttributes().getPrice() * (totalSelectedFreeModifier - modifierGroup.getAttributes().getFreeModifiersCount());
                                }
                            } else {
                                totalPrice += modifierItem.getAttributes().getPrice() * modifierItem.getAttributes().getQuantity();
                            }
                        }
                    }
                }
            }
            for (Ingredient ingredient : item.getIngredients()) {
                if (ingredient.getAttributes().isSelected()) {
                    totalPrice += ingredient.getAttributes().getPrice();
                }
            }

            try {
                if (item.isCombo) {
                    for (MenuItemComponents components : item.combo.get(comboIndex).components) {
                        for (ComponentModifierItem componentModifierItem : components.items) {
                            if (componentModifierItem.selected) {
                                if (!item.isSandwichOnly)
                                    totalPrice += componentModifierItem.price * componentModifierItem.quantity;
                                if (!componentModifierItem.modifierGroups.isEmpty()) {
                                    for (ModifierGroup modifierGroup : componentModifierItem.modifierGroups) {
                                        for (int i = 0; i < modifierGroup.getModifierItems().size(); i++) {
                                            if (modifierGroup.getModifierItems().get(i).getAttributes().isSelected()) {
                                                totalPrice += modifierGroup.getModifierItems().get(i).getAttributes().getPrice() * modifierGroup.getModifierItems().get(i).getAttributes().getQuantity();
                                            }
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
        }

        if (!isUpsell || item.isPromo() || item.getAttributes().getUpsellQuantity() > item.getAttributes().getQuantity()) {
            totalPrice *= item.getAttributes().getQuantity();
        } else {
            totalPrice *= item.getAttributes().getUpsellQuantity();
        }

        return totalPrice;
    }

    @Override
    public double getMenuItemsTotalPrice() {
        ArrayList<MenuItem> cartMenuItems = CacheUtil.getInstance().getCart();
        double totalPrice = 0.0;

        for (MenuItem menuItem : cartMenuItems) {
            totalPrice += getMenuItemTotalPrice(false, menuItem, -1);
        }

        try {
            if (CacheUtil.getInstance().getCouponMenuItem() != null) {
                totalPrice += getMenuItemTotalPrice(false, CacheUtil.getInstance().getCouponMenuItem(), -1);
            }
            if (CacheUtil.getInstance().getCampaignPromotionMenuItem() != null) {
                totalPrice += CacheUtil.getInstance().getCampaign().getPromotion().getAttributes().getItemPrice();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return totalPrice;
    }

    @Override
    public double getTotalPrice() {
        double subtotal = getMenuItemsTotalPrice();

        double discount = 0;
        double promoPrice = 0;
        boolean followDelivery = false;
        if (CacheUtil.getInstance().getCampaign() != null && CacheUtil.getInstance().getCampaignPromotion() != null) {
            Promotion promotion = CacheUtil.getInstance().getCampaignPromotion();
            if (promotion.getAttributes().getType().equalsIgnoreCase("discount")) {
                if (promotion.getAttributes().getDiscountType().equalsIgnoreCase("fixed")) {
                    discount = promotion.getAttributes().getDiscountValue();
                } else if (promotion.getAttributes().getDiscountType().equalsIgnoreCase("percentage")) {
                    discount = subtotal * (promotion.getAttributes().getDiscountValue() / 100.0f);
                    if (promotion.getAttributes().getDiscountMax() != null && promotion.getAttributes().getDiscountMax() != -1 && discount > promotion.getAttributes().getDiscountMax()) {
                        discount = promotion.getAttributes().getDiscountMax();
                    }
                }
            } else {
                Campaign campaign = CacheUtil.getInstance().getCampaign();
                if (campaign != null) {
                    if (campaign.getPromotion().getAttributes().getType().equalsIgnoreCase("item")) {
                        //promoPrice = campaign.getPromotion().getAttributes().getItemPrice();
                    } else if (campaign.getPromotion().getAttributes().getType().equalsIgnoreCase("delivery")) {
                        followDelivery = true;
                    }
                }
            }
        }

        Concept concept = CacheUtil.getInstance().getConcept();

        orderType = CacheUtil.getInstance().getOrderType();

        double deliveryCharge = 0;

        if (orderType == null || orderType == OrderType.DELIVERY) {
            deliveryCharge = concept.getDeliveryCharge();
            try {
                double minimumOrderAmountFreeDelivery = concept.getMinimumOrderAmountFreeDelivery();
                if (minimumOrderAmountFreeDelivery != 0 && subtotal >= minimumOrderAmountFreeDelivery) {
                    deliveryCharge = concept.getSpecialDeliveryPrice();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (followDelivery) {
            deliveryCharge = 0;
        }

        double discountedSubtotal = subtotal - discount;

        if (discountedSubtotal < 0) {
            discountedSubtotal = 0;
        }

        double vatAmount = 0;
        double vatAmountToDisplay = 0;

        if (concept.getVatType() != null && concept.getVatType().equalsIgnoreCase("exclusive")) {
            vatAmount = (discountedSubtotal + deliveryCharge) * (concept.getVatRate() / 100.0);
            vatAmountToDisplay = (discountedSubtotal + deliveryCharge) * (concept.getVatRate() / 100.0);
        } else {
            double sum = (discountedSubtotal + deliveryCharge);
            vatAmountToDisplay = sum - (sum / ((concept.getVatRate() / 100.0) + 1));
        }

        double total = discountedSubtotal + deliveryCharge + vatAmount + promoPrice;

        if (loyaltyPointsUsed > 0) {
            total -= Math.min(loyaltyPointsUsed / Double.parseDouble(String.valueOf(AuthUtil.getInstance().getCustomerLoyalty().getAttributes().getLoyaltyTier().getRedemptionRatio())), getMenuItemsTotalPrice());
        }

        if (total < 0) {
            total = 0;
        }

        if (vatAmountToDisplay <= 0) {
            vatAmountToDisplay = 0;
        }

        boolean isInclusive = false;

        if (concept.getVatType() != null && concept.getVatType().equalsIgnoreCase("inclusive")) {
            isInclusive = concept.getVatType().equalsIgnoreCase("inclusive");
        }
        paymentView.setTax(isInclusive, StringUtil.getFormattedPrice(vatAmountToDisplay));

        return Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", total));
    }

    @Override
    public @Nullable Double getValidatedSubtotal() {
        double promoItemPrice = 0.0;
//        double subtotalForDisplay = validatedSubtotal - (validatedPromoItemPrice * validatedPromoItemQuantity);
//        if (CacheUtil.getInstance().getCouponMenuItem() != null) {
//            promoItemPrice = getMenuItemTotalPrice(false, CacheUtil.getInstance().getCouponMenuItem(), -1);
//        } else if (CacheUtil.getInstance().getCampaignPromotionMenuItem() != null) {
//            promoItemPrice = getMenuItemTotalPrice(false, CacheUtil.getInstance().getCampaignPromotionMenuItem(), -1);
//        }
//        subtotalForDisplay += promoItemPrice;
        return validatedSubtotal;
    }

    @Override
    public @Nullable Double getValidatedTotal() {
        double promoItemPrice = 0.0;
//        double totalForDisplay = validatedTotal - (validatedPromoItemPrice * validatedPromoItemQuantity);
//        if (CacheUtil.getInstance().getCouponMenuItem() != null) {
//            promoItemPrice = getMenuItemTotalPrice(false, CacheUtil.getInstance().getCouponMenuItem(), -1);
//        } else if (CacheUtil.getInstance().getCampaignPromotionMenuItem() != null) {
//            promoItemPrice = getMenuItemTotalPrice(false, CacheUtil.getInstance().getCampaignPromotionMenuItem(), -1);
//        }
//        totalForDisplay += promoItemPrice;
        return validatedTotal;
    }

    @androidx.annotation.Nullable
    @Override
    public Double getValidateDeliveryCharge() {
        return validatedDeliveryCharge;
    }

    @Override
    public double getCashPresented() {
        return cashPresented;
    }

    @Override
    public void setCashPresented(double cashPresented) {
        this.cashPresented = cashPresented;
    }

    @Override
    public void scheduleOrder(Calendar originalCalendar, Calendar calendar, boolean checkingOnly) {

        int minSchedule = CacheUtil.getInstance().getConcept().getDefaultScheduleDeliveryTime();
        int maxSchedule = CacheUtil.getInstance().getConcept().getMaxDaysAllowedInAdvanceOrder();

        if (minSchedule == 0) {
            minSchedule = 15;
        }

        if (maxSchedule == 0) {
            maxSchedule = 1;
        }

        Calendar calendarMinimum = Calendar.getInstance();
        calendarMinimum.setTime(originalCalendar.getTime());
        calendarMinimum.add(Calendar.MINUTE, minSchedule);

        Calendar calendarMaxDays = Calendar.getInstance();
        calendarMaxDays.setTime(originalCalendar.getTime());
        calendarMaxDays.add(Calendar.DAY_OF_MONTH, maxSchedule);

        calendar.set(Calendar.SECOND, 59);

        if (calendar.before(calendarMinimum) && !calendar.equals(calendarMinimum)) {
            paymentView.showMessage(CacheUtil.getInstance().getTranslations(Text.MINIMUM_SCHEDULED_ORDER, "Unable to process order, scheduled order is allowed minimum of (x) min from current time.").replace("(x)", "" + minSchedule));
        } else if (calendar.after(calendarMaxDays)) {
            if (maxSchedule == 1) {
                paymentView.showMessage(CacheUtil.getInstance().getTranslations(Text.SCHEDULED_ORDER_MAXIMUM));
            } else {
                paymentView.showMessage(CacheUtil.getInstance().getTranslations(Text.MAXIMUM_SCHEDULED_ORDER, "Unable to process order, scheduled order is allowed maximum of (x) days from current time.").replace("(x)", "" + maxSchedule));
            }
        } else {
            if (checkingOnly) {
                paymentView.setSchedule();
            } else {
                paymentView.sendOrder();
                Calendar nowCalendar = Calendar.getInstance();
                nowCalendar.setTime(new Date());
                nowCalendar.add(Calendar.MINUTE, 15);
                nowCalendar.add(Calendar.SECOND, 30);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
                String scheduledTime = TimeUtil.convertTimeUtc(dateFormat.format(calendar.getTime()));
                if (calendar.equals(calendarMinimum)) {
                    scheduledTime = TimeUtil.convertTimeUtc(dateFormat.format(nowCalendar.getTime()));
                }
                getStoreOpen(true, scheduledTime);
            }
        }
    }

    @Override
    public void getStoreOpen(boolean scheduleOrder, String date) {
        if (CacheUtil.getInstance().getOrderStore() != null) {
            LocationApiCall.getInstance().isOpen(CacheUtil.getInstance().getOrderStore().getId(), date, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {

                    try {
                        JSONObject object = (JSONObject) o;
                        JSONObject data = object.getJSONObject("data");

                        OrderType orderType = CacheUtil.getInstance().getOrderType();
                        Store openStore = Store.parseStore(data);
                        if ((orderType == OrderType.DELIVERY && openStore.getAttributes().getOpenDeliver()) || openStore.getAttributes().getOpenPickup()) {

                            if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY && openStore.getAttributes().getDeliveryEnabled() == 0) {
                                paymentView.showError(CacheUtil.getInstance().getTranslations(Text.WE_DO_NOT_DELIVER_TO_YOUR_AREA));
                            } else if (CacheUtil.getInstance().getOrderType() != OrderType.DELIVERY && openStore.getAttributes().getPickupEnabled() == 0) {
                                paymentView.showError(CacheUtil.getInstance().getTranslations(Text.LOCATION_PICKUP_DISABLED, "Your selected branch is no longer accepting orders. Please select another branch."));
                            } else {
                                validateOrder(true, CacheUtil.getInstance().getOrderStore(), scheduleOrder ? date : null, loyaltyPointsUsed, false);
                            }

                        } else {
                            paymentView.showError(CacheUtil.getInstance().getTranslations(Text.STORE_IS_CLOSED));
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.onError(e);
                    }
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    paymentView.showError(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(Text.STORE_IS_CLOSED)));
                }
            });
        }
    }

    public boolean checkIfHasCashPaymentType(PaymentType paymentType) {
        Application application = CacheUtil.getInstance().getApplication();
        if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
            Set<PaymentType> paymentTypesDelivery = application.getAttributes().getDeliverPaymentTypes();
            return paymentTypesDelivery.contains(paymentType);
        } else {
            Set<PaymentType> paymentTypesDelivery = application.getAttributes().getPickupPaymentTypes();
            return paymentTypesDelivery.contains(paymentType);
        }
    }

    @Override
    public Pair<JsonObject, HashMap<String, String>> getOrderBody(boolean validate, Store store, String scheduledTime, boolean fromOrder, int loyaltyPoints, @Nullable JsonObject validatedJsonObject) {

        Customer customer = AuthUtil.getInstance().getCustomer();
        Concept concept = CacheUtil.getInstance().getConcept();
        menuItems = CacheUtil.getInstance().getCart();
        orderedMenuItem = CacheUtil.getInstance().getCart();

        try {
            if (CacheUtil.getInstance().getCouponMenuItem() != null) {
                MenuItem promo = CacheUtil.getInstance().getCouponMenuItem();
                promo.setPromo(true);
                menuItems.add(promo);
            }
            if (CacheUtil.getInstance().getCampaignPromotionMenuItem() != null) {
                MenuItem promo = CacheUtil.getInstance().getCampaignPromotionMenuItem();
                promo.setPromo(true);
                menuItems.add(promo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        double subtotal = getMenuItemsTotalPrice();

        if (!validate) {
            subtotal = getValidatedSubtotal();
        }

        double deliveryCharge = concept.getDeliveryCharge();

        try {
            double minimumOrderAmountFreeDelivery = concept.getMinimumOrderAmountFreeDelivery();
            if (minimumOrderAmountFreeDelivery != 0 && subtotal >= minimumOrderAmountFreeDelivery) {
                deliveryCharge = concept.getSpecialDeliveryPrice();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!validate) {
            deliveryCharge = validatedDeliveryCharge;
        }

        JsonObject orderBody = new JsonObject();

        if (customer.getId() != null) {
            orderBody.addProperty("customer", customer.getId());
        }

        if (scheduledTime != null) {
            orderBody.addProperty("scheduled-time", scheduledTime);
        }

        if (mukafaaTransactionId != null && !mukafaaTransactionId.isEmpty()) {
            JsonObject loyalty = new JsonObject();
            loyalty.addProperty("provider", Values.MUKAFAA_PROVIDER);
            loyalty.addProperty("transaction-id", mukafaaTransactionId);
            orderBody.add("loyalty", loyalty);
        }

        if (loyaltyPoints > 0) {
            orderBody.addProperty("points", loyaltyPoints);
        }

        orderBody.addProperty("source", "Android");
        orderBody.addProperty("notes", CacheUtil.getInstance().getCartNotes());
        orderBody.addProperty("subtotal", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", subtotal)));

        double discount = 0;
        double promoPrice = 0;
        boolean followDelivery = false;
        double followDeliveryCharge = 0;
        boolean discountZero = false;

        if (CacheUtil.getInstance().getCouponCode() != null) {
            JsonArray coupons = new JsonArray();
            coupons.add(CacheUtil.getInstance().getCouponCode());
            orderBody.add("coupons", coupons);
        }

        if (CacheUtil.getInstance().getCampaign() != null && CacheUtil.getInstance().getCampaignPromotion() != null) {
            orderBody.addProperty("campaign-id", CacheUtil.getInstance().getCampaign().getId());
        }

        if (!validate) {
            discount = validatedDiscount;
        }

        double discountedSubtotal = subtotal - discount;
        if (discountedSubtotal < 0) {
            discountedSubtotal = 0;
            discount = subtotal;
        }

        if (!validate) {
            discount = validatedDiscount;
        }

        orderBody.addProperty("discount", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", discount)));

        if (!validate && validatedJsonObject != null) {
            if (validatedJsonObject.has("loyalty-points-earnable")) {
                orderBody.add("loyalty-points-earnable", validatedJsonObject.get("loyalty-points-earnable"));
            }
            if (validatedJsonObject.has("location")) {
                orderBody.add("location", validatedJsonObject.get("location"));
            }
            if (validatedJsonObject.has("delivery-charge")) {
                orderBody.add("delivery-charge", validatedJsonObject.get("delivery-charge"));
            }
            if (validatedJsonObject.has("charges")) {
                orderBody.add("charges", validatedJsonObject.get("charges"));
            }
            if (validatedJsonObject.has("taxes")) {
                orderBody.add("taxes", validatedJsonObject.get("taxes"));
            }
            if (validatedJsonObject.has("taxes")) {
                orderBody.add("taxes", validatedJsonObject.get("taxes"));
            }
            if (validatedJsonObject.has("discount-display")) {
                orderBody.add("discount-display", validatedJsonObject.get("discount-display"));
            }
            if (validatedJsonObject.has("round")) {
                orderBody.add("round", validatedJsonObject.get("round"));
            }
            if (validatedJsonObject.has("vat-amount")) {
                orderBody.add("vat-amount", validatedJsonObject.get("vat-amount"));
            }
        }

        switch (CacheUtil.getInstance().getOrderType()) {
            case DRIVE_THRU:
                deliveryCharge = 0.0;
                orderBody.addProperty("type", "drive-thru");
                if (store != null) {
                    orderBody.addProperty("location", store.getId());
                }
                break;
            case PICKUP:
                deliveryCharge = 0.0;
                if (CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().contains(OrderType.DINE_IN)) {
                    orderBody.addProperty("type", "to-go");
                } else {
                    orderBody.addProperty("type", "pickup");
                }
                if (store != null) {
                    orderBody.addProperty("location", store.getId());
                }
                break;
            case DINE_IN:
                deliveryCharge = 0.0;
                orderBody.addProperty("type", "eat-in");
                if (store != null) {
                    orderBody.addProperty("location", store.getId());
                    orderBody.addProperty("table-label", CacheUtil.getInstance().getDineInTableNumber());
                }
                break;
            case DELIVERY:
                orderBody.addProperty("type", "deliver");
                if (validate) {
                    orderBody.addProperty("delivery-charge", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", deliveryCharge)));
                }
                //orderBody.addProperty("delivery-charge", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", deliveryCharge)));
                Address address = CacheUtil.getInstance().getOrderAddress();
                if (address != null) {
                    orderBody.addProperty("address", address.getId());
                }
                break;
            case CURBSIDE:
                deliveryCharge = 0.0;
                if (store != null) {
                    orderBody.addProperty("location", CacheUtil.getInstance().getOrderStore().getId());
                }
                if (CacheUtil.getInstance().getCurbsideCar() != null) {
                    orderBody.addProperty("car-id", CacheUtil.getInstance().getCurbsideCar().id);
                }
                orderBody.addProperty("type", "curbside");
                break;
        }

        if (followDelivery) {
            deliveryCharge = followDeliveryCharge;
        }

//        double vatAmount = vatAmountFromResponse;
        if (validate && concept.getVatType() != null && concept.getVatType().equalsIgnoreCase("exclusive") && vatAmountFromResponse > 0) {
//            vatAmount = (discountedSubtotal + deliveryCharge) * (concept.getVatRate() / 100.0);
//            if (discountZero) {
//                vatAmount = 0;
//            }
            orderBody.addProperty("vat-amount", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", vatAmountFromResponse)));
        }

//        double total = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", discountedSubtotal + deliveryCharge + vatAmount + promoPrice));
        double total = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", discountedSubtotal + deliveryCharge + validatedTotal + promoPrice));

        if (total < 0) {
            total = 0;
        }

        if (!validate) {
            total = getValidatedTotal();
        }

        orderBody.addProperty("total", total);

        PaymentType orderPayment = CacheUtil.getInstance().getOrderPayment();

        if (canProceedViaFullPoints()) {
            JsonObject payment = new JsonObject();
            payment.addProperty("method", "cash");
            payment.addProperty("amount", total);
            payment.addProperty("cash-presented", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", cashPresented)));
            orderBody.add("payment", payment);
        } else if (orderPayment != PaymentType.NONE && orderPayment != PaymentType.CARD) {
            if (checkIfHasCashPaymentType(orderPayment)) {
                JsonObject payment = new JsonObject();
                payment.addProperty("method", orderPayment.getValue());
                payment.addProperty("amount", total);
                payment.addProperty("cash-presented", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", cashPresented)));
                orderBody.add("payment", payment);
            } else {
                CacheUtil.getInstance().setOrderPayment(PaymentType.NONE);
            }
        }

        JsonArray items = new JsonArray();

        HashMap<String, String> eventAttributes = new HashMap<>();

        String orderStoreId = null;
        if (CacheUtil.getInstance().getOrderStore() != null) {
            orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
        }

        for (MenuItem menuItem : menuItems) {

            //if (validate) {

            eventAttributes.put("Item Name", menuItem.getAttributes().getName());
            eventAttributes.put("Item Quantity", String.valueOf(menuItem.getAttributes().getQuantity()));

            JsonObject item = new JsonObject();
            JsonArray modifiers = new JsonArray();
            JsonArray ingredients = new JsonArray();
            item.addProperty("id", menuItem.getId());
            item.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", menuItem.getPrice(orderStoreId))));
            item.addProperty("quantity", menuItem.getAttributes().getQuantity());
            item.addProperty("is-reorder", menuItem.getAttributes().isReorder());
            item.addProperty("is-promotion", menuItem.isPromo());
            item.addProperty("notes", menuItem.getAttributes().getNotes());

            int comboIndex = 0;
            int trueIndex = 0;
            for (ModifierItem size : menuItem.getSizes().getModifierItems()) {
                if (size.getAttributes().isSelected()) {
                    JsonObject modifier = new JsonObject();
                    modifier.addProperty("id", size.getId());
                    modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", size.getAttributes().getPrice())));
                    modifier.addProperty("quantity", size.getAttributes().getQuantity());
                    modifiers.add(modifier);
                    eventAttributes.put("Modifier Name", size.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                    eventAttributes.put("Modifier Quantity", String.valueOf(size.getAttributes().getQuantity()));
                    trueIndex = comboIndex;
                }
                comboIndex++;
            }

            for (ModifierItem drink : menuItem.getDrinks().getModifierItems()) {
                if (drink.getAttributes().isSelected()) {
                    JsonObject modifier = new JsonObject();
                    modifier.addProperty("id", drink.getId());
                    modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", drink.getAttributes().getPrice())));
                    modifier.addProperty("quantity", drink.getAttributes().getQuantity());
                    modifiers.add(modifier);
                    eventAttributes.put("Modifier Name", drink.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                    eventAttributes.put("Modifier Quantity", String.valueOf(drink.getAttributes().getQuantity()));
                }
            }

            for (ModifierItem fries : menuItem.getFries().getModifierItems()) {
                if (fries.getAttributes().isSelected()) {
                    JsonObject modifier = new JsonObject();
                    modifier.addProperty("id", fries.getId());
                    modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", fries.getAttributes().getPrice())));
                    modifier.addProperty("quantity", fries.getAttributes().getQuantity());
                    modifiers.add(modifier);
                    eventAttributes.put("Modifier Name", fries.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                    eventAttributes.put("Modifier Quantity", String.valueOf(fries.getAttributes().getQuantity()));
                }
            }

            for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                VisibilityConfig visibilityConfig = modifierGroup.getAttributes().getVisibilityConfig();
                if (visibilityConfig == null || visibilityConfig.getInitialValue() == null || !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
                    int selectedFreeModifier = 0;
                    for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                        if (modifierItem.getAttributes().isSelected()) {

                            JsonObject freeModifier = null;
                            JsonObject modifier = null;

                            if (modifierGroup.getAttributes().getFreeModifiersCount() > 0 && selectedFreeModifier < modifierGroup.getAttributes().getFreeModifiersCount()) {
                                int totalSelectedFreeModifier = selectedFreeModifier + modifierItem.getAttributes().getQuantity();
                                if (totalSelectedFreeModifier <= modifierGroup.getAttributes().getFreeModifiersCount()) {
                                    selectedFreeModifier += modifierItem.getAttributes().getQuantity();
                                    freeModifier = new JsonObject();
                                    freeModifier.addProperty("id", modifierItem.getId());
                                    freeModifier.addProperty("price", 0);
                                    freeModifier.addProperty("quantity", modifierItem.getAttributes().getQuantity());
                                } else {
                                    selectedFreeModifier = modifierGroup.getAttributes().getFreeModifiersCount();

                                    freeModifier = new JsonObject();
                                    freeModifier.addProperty("id", modifierItem.getId());
                                    freeModifier.addProperty("price", 0);
                                    freeModifier.addProperty("quantity", modifierItem.getAttributes().getQuantity() - (totalSelectedFreeModifier - modifierGroup.getAttributes().getFreeModifiersCount()));

                                    modifier = new JsonObject();
                                    modifier.addProperty("id", modifierItem.getId());
                                    modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", modifierItem.getAttributes().getPrice())));
                                    modifier.addProperty("quantity", totalSelectedFreeModifier - modifierGroup.getAttributes().getFreeModifiersCount());
                                }
                            } else {
                                modifier = new JsonObject();
                                modifier.addProperty("id", modifierItem.getId());
                                modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", modifierItem.getAttributes().getPrice())));
                                modifier.addProperty("quantity", modifierItem.getAttributes().getQuantity());
                            }

                            if (freeModifier != null) {
                                modifiers.add(freeModifier);
                            }

                            if (modifier != null) {
                                modifiers.add(modifier);
                            }

                            eventAttributes.put("Modifier Name", modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                            eventAttributes.put("Modifier Quantity", String.valueOf(modifierItem.getAttributes().getQuantity()));
                        }
                    }
                }

            }

            for (ModifierItem modifierItem : menuItem.getModifierItems()) {
                JsonObject modifier = new JsonObject();
                modifier.addProperty("id", modifierItem.getId());
                modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", modifierItem.getAttributes().getPrice())));
                modifier.addProperty("quantity", modifierItem.getAttributes().getQuantity());
                modifiers.add(modifier);
                eventAttributes.put("Modifier Name", modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                eventAttributes.put("Modifier Quantity", String.valueOf(modifierItem.getAttributes().getQuantity()));
            }

            for (Ingredient ingredient : menuItem.getIngredients()) {
                if (!ingredient.getAttributes().isSelected()) {
                    JsonObject ingredientObject = new JsonObject();
                    ingredientObject.addProperty("id", ingredient.getId());
                    ingredientObject.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", ingredient.getAttributes().getPrice())));
                    ingredientObject.addProperty("quantity", ingredient.getAttributes().getQuantity());
                    ingredients.add(ingredientObject);
                    eventAttributes.put("Ingredient Name", ingredient.getAttributes().getName());
                    eventAttributes.put("Ingredient Quantity", String.valueOf(ingredient.getAttributes().getQuantity()));
                }
            }

            try {
                if (menuItem.isCombo) {
                    JsonArray comboItems = new JsonArray();
                    for (MenuItemComponents components : menuItem.combo.get(trueIndex).components) {
                        for (ComponentModifierItem compoItem : components.items) {
                            if (compoItem.selected) {
                                boolean proceed = true;
                                if (proceed) {
                                    JsonObject comboItemObject = new JsonObject();
                                    comboItemObject.addProperty("id", compoItem.id);
                                    comboItemObject.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", compoItem.price)));
                                    comboItemObject.addProperty("quantity", compoItem.quantity);
                                    comboItemObject.add("ingredients", new JsonArray());
                                    JsonArray modifierCombos = new JsonArray();
                                    if (!compoItem.modifierGroups.isEmpty()) {
                                        for (ModifierGroup modifierGroup : compoItem.modifierGroups) {
                                            for (int i = 0; i < modifierGroup.getModifierItems().size(); i++) {
                                                if (modifierGroup.getModifierItems().get(i).getAttributes().isSelected()) {
                                                    JsonObject comoModItemObject = new JsonObject();
                                                    comoModItemObject.addProperty("id", modifierGroup.getModifierItems().get(i).getId());
                                                    comoModItemObject.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", modifierGroup.getModifierItems().get(i).getAttributes().getPrice())));
                                                    comoModItemObject.addProperty("quantity", modifierGroup.getModifierItems().get(i).getAttributes().getQuantity());
                                                    modifierCombos.add(comoModItemObject);
                                                }
                                            }
                                        }
                                    }
                                    comboItemObject.add("modifiers", modifierCombos);
                                    comboItems.add(comboItemObject);
                                }
                            }
                        }
                    }
                    item.add("combo-items", comboItems);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            item.add("ingredients", ingredients);
            item.add("modifiers", modifiers);
            items.add(item);

            //}

        }

        orderBody.add("items", items);

        return new Pair(orderBody, eventAttributes);
    }

    @Override
    public void validateOrder(boolean checkout, Store store, String scheduledTime, int loyaltyPoints, boolean isShowCardFailed) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            setLoyaltyPointsUsed(loyaltyPoints);

            CouponsApiCall.getInstance().validatePromotion(getOrderBody(true, store, scheduledTime, false, loyaltyPoints, null).first, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
//                double vatAmount = 0;
                    try {
                        JSONObject jsonObject = (JSONObject) o;
                        JSONArray itemsArray = jsonObject.getJSONArray("items");
                        boolean hasChanged = false;
                        for (int i = 0; i < itemsArray.length(); i++) {
                            JSONObject itemObject = itemsArray.getJSONObject(i);
                            MenuItem item = GsonHelper.gsonFinal().fromJson(itemObject.toString(), MenuItem.class);
                            validatedPromoItemPrice = 0.0;
                            validatedPromoItemQuantity = 0;
                            for (MenuItem menuItem : menuItems) {
                                if (item.getIsPromotion()) {
                                    validatedPromoItemPrice = item.getPrice();
                                    validatedPromoItemQuantity = item.getQuantity();
                                }
                                if (!item.getIsPromotion() && menuItem.getId().equals(item.getId())) {
                                    if (BuildUtil.isEkfc() && (item.getTotal() != item.getPrice() || getMenuItemTotalPrice(false, menuItems.get(menuItems.indexOf(menuItem)), -1) != item.getTotal())) {
                                        menuItems.get(menuItems.indexOf(menuItem)).setTotal(item.getTotal());
                                    } else {
                                        menuItems.get(menuItems.indexOf(menuItem)).setTotal(0);
                                        menuItems.get(menuItems.indexOf(menuItem)).setPrice(item.getPrice(null));
                                        menuItems.get(menuItems.indexOf(menuItem)).setPrice(item.getPrice(null));
                                    }
                                    hasChanged = true;
                                }
                            }
                        }

                        if (hasChanged) {
                            ArrayList<MenuItem> items = new ArrayList<>();
                            for (MenuItem item : menuItems) {
                                if (!item.isPromo()) {
                                    items.add(item);
                                }
                            }
                            CacheUtil.getInstance().setCart(items);
                            paymentView.updateMenuItems();
                        }

                        OrderData orderData = OrderData.parse(jsonObject);

                        if (jsonObject.has("loyalty-points-earnable")) {
                            pointsEarnable = jsonObject.getInt("loyalty-points-earnable");
                        }

                        validatedSubtotal = orderData.getSubtotal();
                        validatedDiscount = orderData.getDiscount();
                        validatedRound = orderData.getRound();
                        validatedDeliveryCharge = orderData.getDeliverCharge();
                        validatedCharges = orderData.getCharges();
                        vatAmountFromResponse = orderData.getVatAmount();
                        validatedTotal = orderData.getTotal();

                        if (jsonObject.has("loyalty")) {
                            JSONObject loyalty = jsonObject.getJSONObject("loyalty");
                            if (loyalty.has("amount")) {
                                setMukafaaTransactionAmount(loyalty.getDouble("amount"));
                            }
                        }

                        double pointsSarValue = 0.0;

                        if (loyaltyPoints > 0) {
                            pointsSarValue = loyaltyPoints * Double.parseDouble(String.valueOf(AuthUtil.getInstance().getCustomerLoyalty().getAttributes().getLoyaltyTier().getRedemptionRatio()));// / Double.parseDouble(AuthUtil.getInstance().getCustomerLoyalty().getAttributes().getPointConversion());
                        }
                        com.skylinedynamics.solosdk.util.AppLogger.longError("AAAAAAAAAAAAA pointsSarValue " + pointsSarValue);
                        double discountValue = orderData.getDiscount() > 0.0 ? orderData.getDiscount() : orderData.getDiscountDisplay();

                        boolean hasPromoItem = CacheUtil.getInstance().getCouponMenuItem() != null || CacheUtil.getInstance().getCampaignPromotionMenuItem() != null;

                        if (jsonObject.has("error") && !jsonObject.isNull("error")) {
                            ErrorHandler.getErrorCode(jsonObject, s -> {
                                if (s.equals("1213")) {
                                    loyaltyPointsUsed = 0;
                                }
                                return null;
                            });
                            paymentView.showError(jsonObject.getString("error"));
                        } else if (checkout) {
                            if (!hasPromoItem) {
                                sendOrder(store, scheduledTime, o.toString(), loyaltyPoints);
                            } else {
                                validatedBody = getOrderBody(false, store, scheduledTime, false, loyaltyPoints, JsonParser.parseString(jsonObject.toString()).getAsJsonObject()).first;
                                sendOrder(store, scheduledTime, validatedBody.toString(), loyaltyPoints);
                            }
                        } else {
                            if (isShowCardFailed) {
                                paymentView.showCardFailed();
                            }
                        }
                        paymentView.setPriceDetails(getValidatedSubtotal(), discountValue, getValidatedTotal(), validatedDeliveryCharge, pointsSarValue, pointsEarnable, loyaltyPoints, orderData.getCharges());

                        if (!hasPromoItem) {
                            validatedBody = (JsonObject) new JsonParser().parse(o.toString());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    if (o instanceof JSONObject) {
                        try {
                            JSONObject object = (JSONObject) o;
                            if (object.has("error") && !object.isNull("error")) {
                                ErrorHandler.getErrorCode(object, s -> {
                                    if (s.equals("1213")) {
                                        loyaltyPointsUsed = 0;
                                        double currentValidatedTotal = getValidatedSubtotal() - validatedDiscount;
                                        paymentView.setPriceDetails(getValidatedSubtotal(), validatedDiscount, currentValidatedTotal, validatedDeliveryCharge, 0, 0, loyaltyPointsUsed, new ArrayList<>());
                                    }
                                    return null;
                                });
                                paymentView.showError(ErrorHandler.getErrorMessage(object, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
                            } else if (object.has("messages") && !object.isNull("messages")) {
                                Object messages = object.get("messages");
                                if (messages instanceof JSONObject) {
                                    JSONObject message = (JSONObject) messages;
                                    List<String> errorList = new ArrayList<>();
                                    Iterator<String> keys = message.keys();
                                    while (keys.hasNext()) {
                                        String key = keys.next();
                                        if (message.get(key) instanceof JSONArray) {
                                            JSONArray array = (JSONArray) message.get(key);
                                            Type type = new TypeToken<List<String>>() {
                                            }.getType();
                                            List<String> error = GsonHelper.gsonFinal().fromJson(array.toString(), type);
                                            errorList.addAll(error);
                                        }
                                    }
                                    paymentView.showValidateOrderError(errorList);
                                }
                            } else {
                                paymentView.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            paymentView.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                        }
                    } else {
                        paymentView.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                    }
                }
            });
        });
    }

    @Override
    public void sendOrder(Store store, String scheduledTime, String object, int loyaltyPoints) {

        Pair<JsonObject, HashMap<String, String>> orderBody = getOrderBody(false, store, scheduledTime, true, loyaltyPoints, null);
        JsonObject finalObject = GsonHelper.gsonFinal().fromJson(object, JsonObject.class);
        OrdersApiCall.getInstance().sendOrder(finalObject, new ApiRequestListener() {

            @Override
            public void onSuccess(Object o) {
                if (o instanceof JSONObject) {

                    try {

                        JSONObject jsonObject = (JSONObject) o;
                        JSONObject dataObject = jsonObject.getJSONObject("data");
                        orderId = dataObject.getString("id");
                        orderType = CacheUtil.getInstance().getOrderType();
                        orderTotalPrice = getTotalPrice();
                        PaymentType paymentType = CacheUtil.getInstance().getOrderPayment();
                        paymentView.logEvent("ItemPurchased", orderBody.second, "Price", validatedTotal);
                        paymentView.logFirebaseEvent("purchase_event", String.valueOf(validatedTotal));
                        paymentView.firebaseECommercePurchaseEvent(orderedMenuItem, validatedTotal);
                        setFirebasePromotionEvent();
                        CacheUtil.getInstance().setDineInTableNumber("");
                        CacheUtil.getInstance().setLoyaltyPointsEarnable(orderId, pointsEarnable);

                        Store store = CacheUtil.getInstance().getOrderStore();

                        if (store != null && store.getAttributes() != null && store.getAttributes().getPromisedTimeDelta() != null) {
                            String promisedTime = store.getAttributes().getPromisedTimeDelta().getPickup().toString();
                            if (orderType == OrderType.DELIVERY) {
                                promisedTime = store.getAttributes().getPromisedTimeDelta().getDelivery().toString();
                            }
                            CacheUtil.getInstance().setPromisedTime(promisedTime);
                        }

                        paymentView.setOrderId(orderId);
                        KochavaTrackerUtil.trackEventWithData(EventType.PURCHASE.getEventName(), KochavaEventUtil.Companion.purchaseObj(orderTotalPrice, menuItems));
                        ApiLogUtil.sendOrderLog(ApiLogLevel.INFO, orderId, paymentType.getValue(), "Order Successful");

                        if (paymentType != PaymentType.CARD || validatedTotal <= 0) {
                            sendUpsells();
                        } else {
                            String gateway = CacheUtil.getInstance().getApplication().getAttributes().getPaymentGateway();
                            switch (gateway) {
                                case "tap":
                                    paymentView.initializeTap(orderId, validatedTotal);
                                    break;
                                case "payfort":
                                    return;
                                case "checkout":
                                case "foodics-online":
                                    callPaymentSession(orderId);
                                    break;
                                case "moneyhash":
                                case "money-hash":
                                    paymentView.showError("Payment gateway not found");
                                    break;
                                case "moyasar":
                                    getMoyasarKey();
                                    break;
                                default:
                                    paymentView.showError("Payment gateway not found");
                                    break;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        paymentView.showError(CacheUtil.getInstance().getTranslations(Text.SORRY_ERROR_PAYMENT));
                    }

                } else {
                    paymentView.showError(CacheUtil.getInstance().getTranslations(Text.SORRY_ERROR_PAYMENT));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                paymentView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.SORRY_ERROR_PAYMENT), null));
            }
        });

    }

    private void callPaymentSession(String orderId) {
        OrdersApiCall.getInstance().checkoutSession(orderId, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                if (o instanceof JSONObject jsonObject) {
                    try {
                        String id = jsonObject.getString("id");
                        String token = jsonObject.getString("token");
                        String secret = jsonObject.getString("secret");
                        paymentView.showCheckoutPaymentForm(orderId, id, token, secret);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        paymentView.showError(CacheUtil.getInstance().getTranslations(Text.SORRY_ERROR_PAYMENT));
                    }
                } else {
                    paymentView.showError(CacheUtil.getInstance().getTranslations(Text.SORRY_ERROR_PAYMENT));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                paymentView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.SORRY_ERROR_PAYMENT), null));
            }
        });

    }

    @Override
    public void verifyTap(boolean success, String chargeId) {
        OrdersApiCall.getInstance().verifyTap(Integer.parseInt(orderId), chargeId, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                if (success) {
                    ShipBookLogger.logInfo(BuildConfig.TAG + " - Order ID: " + orderId + " -> Verify Tap Success");
                    sendUpsells();
                } else {
                    ShipBookLogger.logError(BuildConfig.TAG + " - Order ID: " + orderId + " -> Verify Tap Failed");
                    paymentView.showCardFailed();
                }
            }

            @Override
            public void onError(Object o) {
                ShipBookLogger.logError(BuildConfig.TAG + " - Order ID: " + orderId + " -> Verify Tap Error");
                if (o instanceof JSONObject) {
                    JSONObject object = (JSONObject) o;
                    ApiLogUtil.sendOrderLog(ApiLogLevel.ERROR, orderId, getPaymentProvider(), "Card Payment Failed (Reason: " + object + ")");
                } else {
                    ApiLogUtil.sendOrderLog(ApiLogLevel.ERROR, orderId, getPaymentProvider(), "Card Payment Failed (Reason: Verify Tap Error)");
                }
                AppLogger.printRetrofitError(o);
                paymentView.showCardFailed();
            }
        });
    }

    @Override
    public void fetchCustomerCards() {
        CustomersApiCall.getInstance().getSavedCards(AuthUtil.getInstance().getCustomer().getId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject jsonObject = new JSONObject(o.toString());
                    List<CustomerCard> cards = GsonHelper.gsonFinal().fromJson(jsonObject.getJSONArray("data").toString(), new TypeToken<List<CustomerCard>>() {
                    }.getType());
                    CacheUtil.getInstance().setCustomerCards(new Gson().toJson(cards));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Object o) {
            }
        });
    }

    @Override
    public void cancelOrder() {
        OrdersApiCall.getInstance().cancelOrder(orderId, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                paymentView.dismissDialogs();
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
            }
        });
    }

    @Override
    public void sendUpsells() {
        removeMukafaaRedemption();
        setDevicePromotion();
        CacheUtil.getInstance().clearOrder();
        List<MenuItem> upsells = new ArrayList<>();
        for (MenuItem menuItem : menuItems) {
            if (menuItem.getAttributes().isUpsell()) {
                upsells.add(menuItem);
            }
        }
        if (upsells.size() > 0) {
            createUpsells(upsells);
        } else {
            CacheUtil.getInstance().setOrderRatings(true);
            orderType = CacheUtil.getInstance().getOrderType();
            paymentView.showCompleted(orderId, orderType);
        }
    }

    @Override
    public void createUpsells(List<MenuItem> upsells) {

        JsonObject body = new JsonObject();

        double total = 0.0;

        JsonArray items = new JsonArray();

        for (MenuItem upsell : upsells) {
            JsonObject item = new JsonObject();
            item.addProperty("id", Integer.valueOf(upsell.getId()));
            item.addProperty("quantity", Math.min(upsell.getAttributes().getUpsellQuantity(), upsell.getAttributes().getQuantity()));
            total += getMenuItemTotalPrice(true, upsell, -1);
            items.add(item);
        }

        body.addProperty("value", total);
        body.add("items", items);

        OrdersApiCall.getInstance().createOrderUpsell(orderId, body, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                orderType = CacheUtil.getInstance().getOrderType();
                paymentView.showCompleted(orderId, orderType);
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                orderType = CacheUtil.getInstance().getOrderType();
                paymentView.showCompleted(orderId, orderType);
            }
        });
    }

    @Override
    public void generateCheckoutToken(String orderId) {

    }

    @Override
    public void onBindMenuItemViewAtPosition(int position, OrderSummaryMenuItemViewHolder orderSummaryMenuItemViewHolder) {
        orderSummaryMenuItemViewHolder.setMenuItem(menuItems.get(position));
    }

    @Override
    public int getMenuItemsCount() {
        return menuItems.size();
    }

    @Override
    public void getLoyalty() {
        CustomersApiCall.getInstance().getLoyaltyCustomerV2(AuthUtil.getInstance().getCustomer().getId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    JSONObject data = object.getJSONObject("data");
                    JSONObject attributes = data.getJSONObject("attributes");
                   /* Object tier = attributes.get("tier");
                    if(!(tier instanceof JSONObject)) {
                        attributes.put("tier", new LoyaltyTier().toJSON());
                        data.put("attributes", attributes);
                    }*/


                    LoyaltyV2 loyalty = GsonHelper.gsonFinal().fromJson(data.toString(), LoyaltyV2.class);
                    AuthUtil.getInstance().setCustomerLoyalty(loyalty);

//                    CustomersApiCall.getInstance().getLoyaltyTier(loyalty.getAttributes().getLoyaltyTier().getId(), new ApiRequestListener() {
//                        @Override
//                        public void onSuccess(Object o) {
//                            try{
//                                JSONObject object = new JSONObject(o.toString());
//                                JSONObject data = object.getJSONObject("data");
//                                Tier loyalty = GsonHelper.gsonFinal().fromJson(data.toString(), Tier.class);
//                                AuthUtil.getInstance().setCustomerLoyaltyTier(loyalty);
//                            }catch (JSONException ex){
//                                ex.printStackTrace();
//                            }
//                        }
//
//                        @Override
//                        public void onError(Object o) {
//
//                        }
//                    });


                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                }

                paymentView.setupViews();
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                paymentView.setupViews();
            }
        });
    }

    @Override
    public boolean canProceedViaFullPoints() {
        return loyaltyPointsUsed > 0 && validatedTotal <= 0;
    }

    @Override
    public void setLoyaltyPointsUsed(int loyaltyPointsUsed) {
        this.loyaltyPointsUsed = loyaltyPointsUsed;
    }


    @Override
    public void setMukafaaTransactionId(String mukafaaTransactionId) {
        this.mukafaaTransactionId = mukafaaTransactionId;
        CacheUtil.getInstance().setMukafaaTransactionId(mukafaaTransactionId);
    }

    @Override
    public boolean hasMukafaaTransactionId() {
        return mukafaaTransactionId != null && !mukafaaTransactionId.isEmpty();
    }

    @Override
    public String getMukafaaTransactionId() {
        return mukafaaTransactionId;
    }

    @Override
    public void removeMukafaa() {
        removeMukafaaRedemption();
    }

    @Override
    public String getValidatedBody() {
        if (validatedBody != null) {
            return validatedBody.toString();
        }
        return "";
    }

    @Override
    public @Nullable Double getValidatedDeliveryCharge() {
        return validatedDeliveryCharge;
    }

    @Override
    public void removeMukafaaRedemption() {
        String transactionId = CacheUtil.getInstance().getMukafaaTransactionId();
        if (transactionId != null && !transactionId.isEmpty()) {
            CacheUtil.getInstance().clearMukafaa();
            mukafaaTransactionId = "";
            CustomersApiCall.getInstance().deleteRedemption(Values.MUKAFAA_PROVIDER, transactionId, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    validateOrder(false, CacheUtil.getInstance().getOrderStore(), null, loyaltyPointsUsed, false);
                }

                @Override
                public void onError(Object o) {

                }
            });
        }
    }

    @Override
    public void setMukafaaTransactionAmount(double amount) {
        CacheUtil.getInstance().setMukafaaTransactionAmount(amount);
    }

    @Override
    public double getMukafaaTransactionAmount() {
        double mukafaaTransactionAmount = CacheUtil.getInstance().getMukafaaTransactionAmount();
        if (mukafaaTransactionAmount < 0) mukafaaTransactionAmount = 0;

        return mukafaaTransactionAmount;
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
    public String getOrderId() {
        return orderId;
    }

    @Override
    public String getPaymentProvider() {
        String gateway = CacheUtil.getInstance().getApplication().getAttributes().getPaymentGateway();
        switch (gateway) {
            case "tap":
                return "Tap";
            case "payfort":
                return "Payfort";
            case "checkout":
                return "Checkout";
            case "foodics-online":
                return "Foodics Online";
            case "moneyhash":
            case "money-hash":
                return "Money Hash";
            case "moyasar":
                return "Moyasar";
            default:
                return "Unknown";
        }
    }

    @Override
    public void setFirebasePromotionEvent() {
        if (CacheUtil.getInstance().getCouponCode() != null) {
            String promoId = CacheUtil.getInstance().getCouponCode();
            String promoName = CacheUtil.getInstance().getCouponName();
            paymentView.firebaseApplyPromotionEvent(promoId, promoName, orderedMenuItem);
        }

        if (CacheUtil.getInstance().getCampaign() != null && CacheUtil.getInstance().getCampaignPromotion() != null) {
            String promoId = CacheUtil.getInstance().getCampaign().getId();
            String promoName = CacheUtil.getInstance().getCampaign().getAttributes().getName();
            paymentView.firebaseApplyPromotionEvent(promoId, promoName, orderedMenuItem);
        }
    }

    @Override
    public List<MenuItem> getMenuItemsList() {
        return menuItems;
    }

    @Override
    public void setDevicePromotion() {
        Promotion promotion = CacheUtil.getInstance().getCampaignPromotion();
        if (promotion != null) {
            ArrayList<String> deviceUsedPromotions = CacheUtil.getInstance().getDeviceUsedPromotions();
            deviceUsedPromotions.add(promotion.getId());
            CacheUtil.getInstance().setDeviceUsedPromotions(new Gson().toJson(deviceUsedPromotions));
        }
    }

    public void getMoyasarKey() {
        UtilitiesApiCall.getInstance().getMoyasarKey(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                if (o instanceof JSONObject) {
                    try {
                        JSONObject obj = (JSONObject) o;
                        if (obj.has("data")) {
                            JSONObject data = obj.getJSONObject("data");
                            if (data.has("option_value")) {
                                String apiKey = data.getString("option_value");
                                paymentView.showMoyasarSheet(orderId, validatedTotal, GsonHelper.gsonFinal().toJson(menuItems), apiKey);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        paymentView.showError(CacheUtil.getInstance().getTranslations(Text.SORRY_ERROR_PAYMENT));
                    }
                }
            }

            @Override
            public void onError(Object o) {
                if (o instanceof JSONObject) {
                    try {
                        JSONObject object = (JSONObject) o;
                        ApiLogUtil.sendMessageLog(ApiLogLevel.ERROR, object.toString());
                        Object error = object.get("error");
                        String detail = CacheUtil.getInstance().getTranslations(Text.SORRY_ERROR_PAYMENT);
                        if (error instanceof JSONObject) {
                            detail = ((JSONObject) error).getString("detail");
                        } else if (error instanceof JSONArray) {
                            JSONObject errorObject = ((JSONArray) error).getJSONObject(0);
                            detail = errorObject.getString("detail");
                        }
                        paymentView.showError(detail);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        paymentView.showError(CacheUtil.getInstance().getTranslations(Text.SORRY_ERROR_PAYMENT));
                    }
                } else {
                    paymentView.showError(CacheUtil.getInstance().getTranslations(Text.SORRY_ERROR_PAYMENT));
                }
            }
        });
    }
}
