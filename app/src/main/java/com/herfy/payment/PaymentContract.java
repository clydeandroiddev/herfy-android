package com.herfy.payment;

import android.location.Geocoder;
import android.util.Pair;

import androidx.activity.result.ActivityResultLauncher;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.gson.JsonObject;
import com.moneyhash.sdk.android.ResultType;
import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.PaymentType;
import com.skylinedynamics.solosdk.api.models.objects.PrayerTime;
import com.skylinedynamics.solosdk.api.models.objects.ServiceCharge;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.herfy.summary.OrderSummaryMenuItemViewHolder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

public interface PaymentContract {

    interface View extends BaseView<Presenter> {

        void showPaymentTypes(Set<PaymentType> paymentTypes);

        void showPaymentTypes(ArrayList<PaymentType> paymentTypes);

        void setStartName(String startName);

        void setStartAddress(String startAddress);

        void setEndName(String endName);

        void setEndAddress(String endAddress);

        void updateMap(OrderType orderType, LatLng startLocation, LatLng endLocation, LatLngBounds bounds);

        void updateTime(Calendar calendar);

        void selectPaymentTypeCash();

        void showValidateOrderError(List<String> errors);

        void sendOrder();

        void initializeTap(String orderId, double total);

        void showCheckoutPaymentForm(String orderId);

        void showInputCvvDialog(String orderId);

        void showCardFailed();

        void showMessage(String message);

        void showError(String error);

        void showCompleted(String orderId, OrderType orderType);

        void setSchedule();

        void dismissDialogs();

        void setTax(boolean inclusive, CharSequence taxAmount);

        void setPriceDetails(double subtotalPrice, double discount, double totalPrice, double deliveryCharge, double pointsSarValue, int pointsEarnable, int pointsUsed, ArrayList<ServiceCharge> charges);

        void setMenuItem(MenuItem menuItem);

        void logFirebaseEvent(String event, String orderTotalPrice);

        void showPrayerTime(PrayerTime prayerTime);

        void showErrorPrayerTime();

        ActivityResultLauncher<kotlin.Pair<String, ResultType>> moneyHashContract();

        void setOrderId(String orderId);

        void showCardDialogList(String orderId);

        void firebaseECommercePurchaseEvent(List<MenuItem> orderedItem, double total);

        void firebaseAddPaymentInfoEvent();

        void firebaseApplyPromotionEvent(String promotionId, String promotionName, List<MenuItem> orderedItems);

        void updateMenuItems();

        void showMoyasarSheet(String orderId, double validatedTotal, String menuItems, String apiKey);

        void showOrderType(Boolean checkout);

        void showSelectedPayment();

        void insufficientLoyaltyPoints();
    }

    interface Presenter extends BasePresenter {

        void getPrayerTime();

        void getPaymentTypes();

        void getStore(Geocoder geocoder);

        void getAddresses(Geocoder geocoder);

        void getLocation(Geocoder geocoder);

        double getMenuItemTotalPrice(boolean isUpsell, MenuItem menuItem, int modifierGroupPosition);

        double getMenuItemsTotalPrice();

        double getTotalPrice();

        @Nullable Double getValidatedSubtotal();

        @Nullable Double getValidatedTotal();

        double getCashPresented();

        void setCashPresented(double cashPresented);

        void scheduleOrder(Calendar originalCalendar, Calendar calendar, boolean checkingOnly);

        void getStoreOpen(boolean scheduleOrder, String date);

        Pair<JsonObject, HashMap<String, String>> getOrderBody(boolean validate, Store store, String scheduledTime, boolean fromOrder, int loyaltyPoints, @Nullable JsonObject validatedJsonObject);

        void validateOrder(boolean checkout, Store store, String scheduledTime, int loyaltyPoints, boolean isShowCardFailed);

        void sendOrder(Store store, String scheduledTime, String jObect, int loyaltyPoints);

        void verifyTap(boolean success, String chargeId);

        void cancelOrder();

        void sendUpsells();

        void createUpsells(List<MenuItem> upsells);

        void generateCheckoutToken(String orderId);

        void setMukafaaTransactionId(String mukafaaTransactionId);

        void onBindMenuItemViewAtPosition(int position, OrderSummaryMenuItemViewHolder orderSummaryMenuItemViewHolder);

        int getMenuItemsCount();

        void getLoyalty();

        boolean canProceedViaFullPoints();

        void setLoyaltyPointsUsed(int loyaltyPointsUsed);

        void fetchCustomerCards();

        boolean hasMukafaaTransactionId();

        String getMukafaaTransactionId();

        void removeMukafaa();

        String getValidatedBody();

        @Nullable Double getValidatedDeliveryCharge();

        void removeMukafaaRedemption();

        void setMukafaaTransactionAmount(double amount);

        double getMukafaaTransactionAmount();

        boolean isMukafaaEnabled();

        String getOrderId();

        String getPaymentProvider();

        void setFirebasePromotionEvent();

        List<MenuItem> getMenuItemsList();

        void setDevicePromotion();

    }

}
