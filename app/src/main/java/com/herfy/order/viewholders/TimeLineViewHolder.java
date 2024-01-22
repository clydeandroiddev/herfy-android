package com.herfy.order.viewholders;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;
import com.github.vipulasri.timelineview.TimelineView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.tabs.TabLayout;
import com.herfy.R;
import com.herfy.font.FontHandler;
import com.herfy.order.OrderContract;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.Store;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TimeLineViewHolder extends RecyclerView.ViewHolder implements OrderContract.View {

    private Context context;
    private OrderContract.Presenter orderPresenter;
    @BindView(R.id.timeline) TimelineView timelineView;
    @BindView(R.id.time) TextView time;
    @BindView(R.id.description) TextView description;

    public TimeLineViewHolder(Context context, OrderContract.Presenter orderPresenter, View itemView, int viewType) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        this.context = context;
        this.orderPresenter = orderPresenter;
        timelineView.initLine(viewType);
        setupViews();
        setupFonts();
        setupTranslations();
    }

    @Override
    public void setPresenter(OrderContract.Presenter presenter) {

    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {

    }

    @Override
    public void setCartExpiry() {

    }

    @Override
    public void setupViews() {

    }

    @Override
    public void setupFonts() {
        time.setTypeface(FontHandler.getRegularTypeface());
        description.setTypeface(FontHandler.getRegularTypeface());
    }

    @Override
    public void setupTranslations() {

    }

    @Override
    public void showOrderTypes(Set<OrderType> orderTypes) {

    }

    @Override
    public void createOrderType(OrderType orderType) {

    }

    @Override
    public void selectOrderType(OrderType orderType) {

    }

    @Override
    public TabLayout.Tab createTab(String tag, String text, Drawable icon) {
        return null;
    }

    @Override
    public void initializeLocation() {

    }

    @Override
    public void getLocation() {

    }

    @Override
    public void showLocation(boolean isPlace, LatLng latLng, String address) {

    }

    @Override
    public void showAddresses(ArrayList<Address> bookmarkedAddresses, ArrayList<Address> otherAddresses) {

    }

    @Override
    public void setAddress(Address address, boolean selected) {

    }

    @Override
    public void selectAddressHome(Address address, boolean redownloadMenu) {

    }

    @Override
    public void selectAddressWork(int position) {

    }

    @Override
    public void selectAddressOther(Address address, boolean redownloadMenu) {

    }

    @Override
    public void updateAddAddress(Address updateAddress, android.location.Address saveAddress) {

    }

    @Override
    public void showStores(ArrayList<Store> pickupStores, ArrayList<Store> curbsideStores) {

    }

    @Override
    public void setStore(Store store, boolean selected) {

    }

    @Override
    public void callStore(Store store) {

    }

    @Override
    public void selectStore(int position, Store store, boolean initialLoad) {

    }

    @Override
    public void showStore(boolean markerClicked, int position) {

    }

    @Override
    public void showStore(boolean open, Store store, String error) {

    }

    @Override
    public void showOrderType(OrderType orderType) {

    }

    @Override
    public void selectDelivery() {

    }

    @Override
    public void selectPickup() {

    }

    @Override
    public void selectDineIn() {

    }

    @Override
    public void selectCurbside() {

    }

    @Override
    public void saveAddress(String address) {

    }

    @Override
    public void saveAddress(Address address) {

    }

    @Override
    public void updateAddress(String address) {

    }

    @Override
    public void showUnavailableMenuItems() {

    }

    @Override
    public void deleteUnavailableItems() {

    }

    @Override
    public void showPhoneNumberError(String error) {

    }

    @Override
    public void showError(String error) {

    }

    @Override
    public void updateMap(OrderType orderType, LatLng startLocation, LatLng endLocation, LatLngBounds bounds) {

    }

    @Override
    public void showOrder(OrderDetails order) {

    }

    @Override
    public void showOrderStatuses(ArrayList<OrderStatus> orderStatuses) {

    }


    @Override
    public void setTimeLine(OrderStatus orderStatus) {
        if(orderStatus.getTime() != null && !orderStatus.getTime().equalsIgnoreCase("null")) {
            try {
                String dateValue = orderStatus.getTime().replace("T", " ") + " GMT";
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault());
                simpleDateFormat.setTimeZone(TimeZone.getDefault());
                Date date = simpleDateFormat.parse(dateValue);

                SimpleDateFormat toSimpleDateFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                toSimpleDateFormat.setTimeZone(TimeZone.getDefault());
                String dateStr = toSimpleDateFormat.format(date);

                time.setText(dateStr);

            } catch (Exception e) {
                e.printStackTrace();
                time.setText("");
            }
        } else {
            time.setText("");
        }
        description.setText(orderStatus.getDescription());

        if(orderStatus.isDone()) {
            timelineView.setMarker(ContextCompat.getDrawable(context, R.drawable.marker_timeline_done));
            timelineView.setStartLineColor(Color.parseColor("#12C735"), getItemViewType());
            timelineView.setEndLineColor(Color.parseColor("#12C735"), getItemViewType());
            time.setTextColor(Color.parseColor("#000000"));
            description.setTextColor(Color.parseColor("#12C735"));
        } else {
            timelineView.setMarker(ContextCompat.getDrawable(context, R.drawable.marker_timeline_pending));
            timelineView.setStartLineColor(Color.parseColor("#C6C6C6"), getItemViewType());
            timelineView.setEndLineColor(Color.parseColor("#C6C6C6"), getItemViewType());
            time.setTextColor(Color.parseColor("#C6C6C6"));
            description.setTextColor(Color.parseColor("#C6C6C6"));
        }
    }

    @Override
    public void showStatus(OrderStatus orderStatus) {

    }

    @Override
    public void showDriver(LatLng latLng) {

    }

    @Override
    public void updateDriver(LatLng latLng) {

    }

    @Override
    public void showRatings(OrderStatus orderStatus, List<Rating> ratings) {

    }

    @Override
    public void showOrders() {

    }

    @Override
    public void enableConfirmButton(boolean enable, String storeName) {

    }

    @Override
    public void saveToCache(String key, String value) {
        
    }

    @Override
    public void preloadImage(String imageUrl, RequestOptions requestOptions) {

    }

    @Override
    public void diffMenuSaved(Address address) {

    }

    @Override
    public void showPromptChangeOrderType(Address address) {

    }

    @Override
    public void showMinimumDeliveryForAreaMessage(String error) {

    }

    @Override
    public void showMessage(String message) {

    }
}

