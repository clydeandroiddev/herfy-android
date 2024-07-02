package com.herfy.history.viewholders;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.github.vipulasri.timelineview.TimelineView;
import com.google.android.gms.maps.model.LatLng;
import com.herfy.R;
import com.herfy.font.FontHandler;
import com.herfy.history.OrdersContract;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.kotlin.DeliveryDriver;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails;
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.Store;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TimeLineViewHolder extends RecyclerView.ViewHolder implements OrdersContract.View {

    private Context context;

    @BindView(R.id.timeline)
    TimelineView timelineView;
    @BindView(R.id.timelineDummy)
    TimelineView dummyTimeline;
    @BindView(R.id.chatDriverLabel)
    TextView chatDriverLabel;
    @BindView(R.id.driverChatButton)
    ConstraintLayout driverChatButton;
    @BindView(R.id.driverChat)
    ConstraintLayout driverChat;
    @BindView(R.id.time)
    TextView time;
    @BindView(R.id.description)
    TextView description;
    private OrdersContract.Presenter ordersPresenter;
    private String orderId;

    public TimeLineViewHolder(Context context, OrdersContract.Presenter ordersPresenter, View itemView, int viewType, String orderId) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        this.context = context;
        this.orderId = orderId;
        this.ordersPresenter = ordersPresenter;
        timelineView.initLine(viewType);
        setupViews();
        setupFonts();
        setupTranslations();
    }

    @Override
    public void setPresenter(OrdersContract.Presenter presenter) {

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
    public void showLocation(boolean isPlace, LatLng latLng, String address) {

    }

    @Override
    public void showOrders(boolean newestToOldest, ArrayList<OrderHistory> orders) {

    }

    @Override
    public void setOrder(OrderHistory order) {

    }

    @Override
    public void updateOrder(int position) {

    }

    @Override
    public void selectOrder(OrderHistory order) {

    }

    @Override
    public void showError(String error) {

    }

    @Override
    public void showOrder(OrderDetails order, Store store, Address address) {

    }

    @Override
    public void showOrderStatuses(ArrayList<OrderStatus> orderStatuses, boolean isDone) {

    }

    @Override
    public void showMenuItems(OrderDetails order, ArrayList<MenuItem> menuItems) {

    }

    @Override
    public void setTimeLine(boolean isCancelled, OrderStatus orderStatus, boolean noDriver) {

        if (orderStatus.getTime() != null && !orderStatus.getTime().equalsIgnoreCase("null") && !orderStatus.getTime().isEmpty()) {
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

        driverChat.setVisibility(View.GONE);
        driverChatButton.setVisibility(View.GONE);

        if (orderStatus.isDone()) {
            timelineView.setMarker(ContextCompat.getDrawable(context, !isCancelled ? R.drawable.ic_marker_timeline_done : R.drawable.ic_marker_timeline_cancelled));
            timelineView.setStartLineColor(Color.parseColor(!isCancelled ? CacheUtil.getInstance().getColorMain() : "#F22424"), getItemViewType());
            timelineView.setEndLineColor(Color.parseColor(!isCancelled ? CacheUtil.getInstance().getColorMain() : "#F22424"), getItemViewType());
            time.setTextColor(Color.parseColor("#141414"));
            description.setTextColor(Color.parseColor(!isCancelled ? CacheUtil.getInstance().getColorMain() : "#F22424"));
        } else {
            timelineView.setMarker(ContextCompat.getDrawable(context, R.drawable.ic_marker_timeline_pending));
            timelineView.setStartLineColor(Color.parseColor("#B5B5B5"), getItemViewType());
            timelineView.setEndLineColor(Color.parseColor("#B5B5B5"), getItemViewType());
            time.setTextColor(Color.parseColor("#B5B5B5"));
            description.setTextColor(Color.parseColor("#B5B5B5"));
        }

    }

    @Override
    public void setMenuItem(MenuItem menuItem) {

    }

    @Override
    public void showCart() {

    }

    @Override
    public void updateStatus(OrderDetails orderDetails, OrderStatus orderStatus) {

    }

    @Override
    public void showDriver(LatLng customerLatLng, LatLng driverLatLng, int eta) {

    }

    @Override
    public void updateDriver(DeliveryDriver appDriver) {

    }

    @Override
    public void cancel() {

    }

    @Override
    public void showRatings(OrderStatus orderStatus, List<Rating> ratings) {

    }

    @Override
    public void showMessage(String message) {

    }

    @Override
    public void customerArrivedSuccess() {

    }

    @Override
    public void logFirebaseEvent(String event) {

    }

    @Override
    public String getFromCache(String key) {
        return "";
    }

    @Override
    public void showWhatsApp(String mobileNumber) {

    }
}

