package com.herfy.order.views;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.tabs.TabLayout;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.utils.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.order.OrderContract;
import com.herfy.order.OrderPresenter;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatusType;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.herfy.util.ViewUtil;

import java.text.ParseException;
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

public class OrderStatusActivity extends BaseActivity implements OrderContract.View {

    private OrderContract.Presenter orderPresenter;

    private String orderId = "";

    @BindView(R.id.back) ImageButton back;

    @BindView(R.id.progress) ProgressBar progress;

    @BindView(R.id.container) ConstraintLayout container;

    @BindView(R.id.message) TextView message;
    @BindView(R.id.id_label) TextView idLabel;
    @BindView(R.id.code) TextView code;
    @BindView(R.id.confirmed_time) TextView confirmedTime;
    @BindView(R.id.preparing_time) TextView preparingTime;
    @BindView(R.id.on_the_way_time) TextView onTheWayTime;
    @BindView(R.id.delivered_time) TextView deliveredTime;

    @BindView(R.id.confirmed) MaterialCheckBox confirmed;
    @BindView(R.id.preparing) MaterialCheckBox preparing;
    @BindView(R.id.on_the_way) MaterialCheckBox onTheWay;
    @BindView(R.id.delivered) MaterialCheckBox delivered;
    @BindView(R.id.rating) MaterialCheckBox rating;

    @BindView(R.id.track) MaterialButton track;
    @BindView(R.id.rate) MaterialButton rate;
    @BindView(R.id.cancel) MaterialButton cancel;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_status);
        ButterKnife.bind(this);
        orderPresenter = new OrderPresenter(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        orderPresenter.start();

        Bundle bundle = getIntent().getExtras();

        if(bundle != null && bundle.containsKey("order_id")) {

            orderId = bundle.getString("order_id");
            orderPresenter.getOrder(orderId);

        } else {
            showError(CacheUtil.getInstance().getTranslations(Text.RETRIEVING_ORDER_DETAILS_ERROR));
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onBackPressed() {
        if(handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        super.onBackPressed();
        finish();
    }

    @Override
    public void setPresenter(OrderContract.Presenter presenter) {
        orderPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
    }

    @Override
    public void setupViews() {

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        track.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(OrderStatusActivity.this, OrderTrackActivity.class);
                intent.putExtra("order_id", orderId);
                startActivity(intent);
            }
        });

        rate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(OrderStatusActivity.this);
                View dialogView = getLayoutInflater().inflate(R.layout.bottom_sheet_dialog_rate, null);

                TextView title = dialogView.findViewById(R.id.title);
                TextView delivery = dialogView.findViewById(R.id.delivery);
                TextView quality = dialogView.findViewById(R.id.quality);
                TextView food = dialogView.findViewById(R.id.food);
                TextView other = dialogView.findViewById(R.id.other);
                RatingBar deliveryRating = dialogView.findViewById(R.id.delivery_rating);
                RatingBar qualityRating = dialogView.findViewById(R.id.quality_rating);
                RatingBar foodRating = dialogView.findViewById(R.id.food_rating);
                RatingBar otherRating = dialogView.findViewById(R.id.food_rating);
                ImageButton close = dialogView.findViewById(R.id.close);
                Button submit = dialogView.findViewById(R.id.submit);

                title.setTypeface(FontHandler.getMediumTypeface());
                delivery.setTypeface(FontHandler.getRegularTypeface());
                quality.setTypeface(FontHandler.getRegularTypeface());
                food.setTypeface(FontHandler.getRegularTypeface());
                other.setTypeface(FontHandler.getRegularTypeface());
                submit.setTypeface(FontHandler.getRegularTypeface());

                title.setText(CacheUtil.getInstance().getTranslations(Text.RATE_YOUR_EXPERIENCE));
                delivery.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERY_SPEED));
                quality.setText(CacheUtil.getInstance().getTranslations(Text.QUALITY_OF_FOOD));
                food.setText(CacheUtil.getInstance().getTranslations(Text.FOOD_AS_DESCRIBED));
                other.setText(CacheUtil.getInstance().getTranslations(Text.OTHER));
                submit.setText(CacheUtil.getInstance().getTranslations(Text.SUBMIT_RATING));

                close.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomSheetDialog.dismiss();
                    }
                });

                submit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bottomSheetDialog.dismiss();
                    }
                });

               bottomSheetDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                BottomSheetDialog d = (BottomSheetDialog) dialog;
                                FrameLayout bottomSheet = d.findViewById(R.id.design_bottom_sheet);
                                BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
                                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                            }
                        },0);
                    }
                });

                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();*/

            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

    }

    @Override
    public void setupFonts() {
        message.setTypeface(FontHandler.getMediumTypeface());
        code.setTypeface(FontHandler.getSemiboldTypeface());
        confirmed.setTypeface(FontHandler.getSemiboldTypeface());
        preparing.setTypeface(FontHandler.getSemiboldTypeface());
        onTheWay.setTypeface(FontHandler.getSemiboldTypeface());
        delivered.setTypeface(FontHandler.getSemiboldTypeface());
        rating.setTypeface(FontHandler.getSemiboldTypeface());
    }

    @Override
    public void setupTranslations() {
        message.setText(CacheUtil.getInstance().getTranslations(Text.ORDER_STATUS));
        idLabel.setText(CacheUtil.getInstance().getTranslations(Text.YOUR_ORDER_ID));
        track.setText(CacheUtil.getInstance().getTranslations(Text.LIVE_TRACKING));
        confirmed.setText(CacheUtil.getInstance().getTranslations(Text.ORDER_CONFIRMED));
        confirmedTime.setText(CacheUtil.getInstance().getTranslations(Text.ON_PROGRESS));
        preparing.setText(CacheUtil.getInstance().getTranslations(Text.PREPARING_FOOD));
        preparingTime.setText(CacheUtil.getInstance().getTranslations(Text.ON_PROGRESS));
        onTheWay.setText(CacheUtil.getInstance().getTranslations(Text.FOOD_ON_THE_WAY));
        onTheWayTime.setText(CacheUtil.getInstance().getTranslations(Text.ON_PROGRESS));
        delivered.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERED_TO_YOU));
        deliveredTime.setText(CacheUtil.getInstance().getTranslations(Text.ON_PROGRESS));
        rating.setText(CacheUtil.getInstance().getTranslations(Text.RATING));
        rate.setText(CacheUtil.getInstance().getTranslations(Text.RATE_NOW));
        cancel.setText(CacheUtil.getInstance().getTranslations(Text.CANCEL_ORDER));
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
    public void updateAddAddress(@Nullable Address updateAddress, @Nullable android.location.Address saveAddress) {

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
        dismissDialogs();
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        onBackPressed();
        finish();
    }

    @Override
    public void updateMap(OrderType orderType, LatLng startLocation, LatLng endLocation, LatLngBounds bounds) {

    }

    @Override
    public void showOrder(OrderDetails order) {

        dismissDialogs();

        if(!order.getCode().isEmpty()) {
            code.setText(order.getCode());
        } else {
            code.setText(order.getId());
        }

        orderPresenter.getOrderStatuses();

    }

    @Override
    public void showOrderStatuses(ArrayList<OrderStatus> orderStatuses) {
        if(orderStatuses.size() > 0) {

            for(OrderStatus orderStatus : orderStatuses) {

                showStatus(orderStatus);

            }

            OrderStatus latestOrderStatus = orderStatuses.get(0);

            if (latestOrderStatus.getSequence() < OrderStatusType.ARRIVED_AT_CUSTOMER.getValue()) {

                if(handler != null) {
                    handler.removeCallbacksAndMessages(null);
                }

                handler = new Handler();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        orderPresenter.getOrderStatuses();
                    }
                }, 60000);

            }

        }

        progress.setVisibility(View.GONE);

        container.setVisibility(View.VISIBLE);

        cancel.setVisibility(View.VISIBLE);
    }

    @Override
    public void setTimeLine(OrderStatus orderStatus) {

    }

    @Override
    public void showStatus(OrderStatus orderStatus) {

        SimpleDateFormat fromSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
        fromSimpleDateFormat.setTimeZone(TimeZone.getDefault());
        SimpleDateFormat toSimpleDateFormat = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
        toSimpleDateFormat.setTimeZone(TimeZone.getDefault());

        if(orderStatus.getSequence() == OrderStatusType.POSTED.getValue()) {
            try {
                Date date = fromSimpleDateFormat.parse(orderStatus.getTime());
                confirmed.setChecked(true);
                confirmed.setTextColor(ViewUtil.getColorMain(this));
                confirmedTime.setText(toSimpleDateFormat.format(date));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        if(orderStatus.getSequence() == OrderStatusType.IN_KITCHEN.getValue()) {

            if(!confirmed.isChecked()) {
                confirmed.setChecked(true);
                confirmed.setTextColor(ViewUtil.getColorMain(this));
                confirmedTime.setText("");
            }

            try {
                Date date = fromSimpleDateFormat.parse(orderStatus.getTime());
                preparing.setChecked(true);
                preparing.setTextColor(ViewUtil.getColorMain(this));
                preparingTime.setText(toSimpleDateFormat.format(date));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        if(orderStatus.getSequence() == OrderStatusType.DELIVERY_IN_PROGRESS.getValue()) {

            if(!confirmed.isChecked()) {
                confirmed.setChecked(true);
                confirmed.setTextColor(ViewUtil.getColorMain(this));
                confirmedTime.setText("");
            }

            if(!preparing.isChecked()) {
                preparing.setChecked(true);
                preparing.setTextColor(ViewUtil.getColorMain(this));
                preparingTime.setText("");
            }

            try {
                Date date = fromSimpleDateFormat.parse(orderStatus.getTime());

                onTheWay.setChecked(true);
                onTheWay.setTextColor(ViewUtil.getColorMain(this));
                onTheWayTime.setText(toSimpleDateFormat.format(date));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        if(orderStatus.getSequence() >= OrderStatusType.ARRIVED_AT_CUSTOMER.getValue()) {

            if(!confirmed.isChecked()) {
                confirmed.setChecked(true);
                confirmed.setTextColor(ViewUtil.getColorMain(this));
                confirmedTime.setText("");
            }

            if(!preparing.isChecked()) {
                preparing.setChecked(true);
                preparing.setTextColor(ViewUtil.getColorMain(this));
                preparingTime.setText("");
            }

            if(!onTheWay.isChecked()) {
                onTheWay.setChecked(true);
                onTheWay.setTextColor(ViewUtil.getColorMain(this));
                onTheWay.setText("");
            }

            try {
                Date date = fromSimpleDateFormat.parse(orderStatus.getTime());
                delivered.setChecked(true);
                delivered.setTextColor(ViewUtil.getColorMain(this));
                deliveredTime.setText(toSimpleDateFormat.format(date));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            rating.setChecked(true);
            rating.setTextColor(ViewUtil.getColorMain(this));
            rate.setTextColor(ContextCompat.getColor(this, R.color.white));
            rate.setBackgroundColor(ViewUtil.getColorMain(this));

            ColorStateList colorStateList = new ColorStateList(new int[][] {
                    new int[] {-android.R.attr.state_pressed},
                    new int[] { android.R.attr.state_pressed}}, new int[] {
                    ViewUtil.getColorMain(this),
                    ViewUtil.getColorMain(this)

            });

            rate.setStrokeColor(colorStateList);
            rate.setClickable(true);

            cancel.setBackgroundColor(ContextCompat.getColor(this, R.color.secondary_text));
            cancel.setClickable(false);

        }

    }

    @Override
    public void showDriver(LatLng latLng) {

    }

    @Override
    public void updateDriver(LatLng latLng) {

    }

    @Override
    public void showMessage(String message) {

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

}
