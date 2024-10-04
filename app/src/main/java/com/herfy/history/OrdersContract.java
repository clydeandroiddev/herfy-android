package com.herfy.history;

import android.location.Geocoder;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.herfy.history.viewholders.OrderMenuItemViewHolder;
import com.herfy.history.viewholders.OrderViewHolder;
import com.herfy.history.viewholders.TimeLineViewHolder;
import com.location.FusedLocationProvider;
import com.skylinedynamics.solosdk.api.models.kotlin.DeliveryDriver;
import com.skylinedynamics.solosdk.api.models.kotlin.LyveDriver;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsTransaction;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails;
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.Store;

import java.util.ArrayList;
import java.util.List;

public interface OrdersContract {

    interface View extends BaseView<Presenter> {

        void showLocation(boolean isPlace, LatLng latLng, String address);

        void showOrders(boolean newestToOldest, ArrayList<OrderHistory> orders);

        void setOrder(OrderHistory order);

        void updateOrder(int position);

        void selectOrder(OrderHistory order);

        void showError(String error);

        void showOrder(OrderDetails order, @Nullable Store store, @Nullable Address address);

        void showOrderStatuses(ArrayList<OrderStatus> orderStatuses, boolean isDone);

        void showMenuItems(OrderDetails order, ArrayList<MenuItem> menuItems);

        void setTimeLine(boolean isCancelled, OrderStatus orderStatus, boolean noDriver);

        void setMenuItem(MenuItem menuItem);

        void showCart();

        void updateStatus(OrderDetails orderDetails, OrderStatus orderStatus);

        void showDriver(@Nullable LatLng customerLatLng, @Nullable LatLng driverLatLng, @Nullable Integer eta);

        void updateDriver(DeliveryDriver appDriver);

        void cancel();

        void showRatings(OrderStatus orderStatus, List<Rating> ratings);

        void showMessage(String message);

        void customerArrivedSuccess();

        void logFirebaseEvent(String event);

        @Nullable
        String getFromCache(String key);

        void showWhatsApp(String mobileNumber);

    }

    interface Presenter extends BasePresenter {
        void setGeocoder(Geocoder geocoder);

        void getLocation(FusedLocationProvider fusedLocationProvider);

        void getOrders();

        LyveDriver getDriverDetail();

        void onBindOrderViewAtPosition(int position, OrderViewHolder orderViewHolder);

        int getOrdersCount();

        void selectOrder(OrderHistory order);

        void sortOrders();

        void onBindMenuItemViewAtPosition(int position, OrderMenuItemViewHolder orderMenuViewHolder);

        void getOrder(String id, int position, boolean statusUpdate, boolean reorder);

        void getStore(boolean reorder);

        void getAddress(boolean reorder);

        void getOrderStatuses();

        void onBindTimeLineViewAtPosition(int orderPostion, int position, TimeLineViewHolder timeLineViewHolder, boolean noDriver);

        int getTimeLineCount(int position);

        int getMenuItemsCount();

        int getSequence();

        void setSequence(int sequence);

        OrderDetails getOrder();

        void getDriver();

        void customerArrived(String orderId);

        void getDriverBearing(String employeeId);

        void detachAbly();

        void getOrderFeedbackTopics();

        void getOrderFeedbackTopics(String orderId);

        void getOrderFeedbacks(String orderId);

        void getOrderFeedbacks();

        List<Rating> getRatings();

        void sendOrderFeedback(List<Rating> ratings);

        void sendOrderFeedback(List<Rating> ratings, String orderId, String otherFeedback);

        void cancel();

        void checkReorder();

        void reorder();

        void getModifiers(String menuCategoryId, MenuItem menuItem);

        void getIngredients(String menuCategoryId, MenuItem menuItem);

        boolean isMukafaaEnabled();

        void removeMukafaaRedemption();

        boolean getFromAbly();

        void setFromAblyToFalse();

        LoyaltyPointsTransaction getMukafaaTransaction();

        ArrayList<LoyaltyPointsTransaction> getLoyaltyPointsTransaction();

        LoyaltyPointsTransaction getSoloLoyaltyTransaction();

        List<MenuItem> getOrderItems();

        ArrayList<OrderHistory> getOrderHistories();

        void setOrders(ArrayList<OrderHistory> orders);
    }

}
