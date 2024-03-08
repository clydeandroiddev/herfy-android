package com.herfy.order.views;

import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.herfy.BuildConfig;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.utils.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.order.OrderContract;
import com.herfy.order.OrderPresenter;
import com.herfy.order.adapters.TimeLineAdapter;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatusType;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.herfy.util.AppLogger;
import com.herfy.util.LatLngInterpolator;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.MarkerAnimation;
import com.herfy.util.StringUtil;
import com.herfy.util.ViewUtil;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public abstract class OrderTrackActivity extends BaseActivity implements OrderContract.View {

    private OrderContract.Presenter orderPresenter;

    private boolean permissions = false;

    private String orderId = "";

    @BindView(R.id.sliding_panel)
    SlidingUpPanelLayout slidingPanel;

    @BindView(R.id.back)
    ImageButton back;

    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.id_label)
    TextView idLabel;
    @BindView(R.id.code)
    TextView code;
    @BindView(R.id.tracking_status)
    TextView trackingStatus;
    /*@BindView(R.id.confirmed_time) TextView confirmedTime;
    @BindView(R.id.preparing_time) TextView preparingTime;
    @BindView(R.id.on_the_way_time) TextView onTheWayTime;
    @BindView(R.id.delivered_time) TextView deliveredTime;

    @BindView(R.id.confirmed) MaterialCheckBox confirmed;
    @BindView(R.id.preparing) MaterialCheckBox preparing;
    @BindView(R.id.on_the_way) MaterialCheckBox onTheWay;
    @BindView(R.id.delivered) MaterialCheckBox delivered;
    @BindView(R.id.rating) MaterialCheckBox rating;

    @BindView(R.id.delivered_view) View deliveredView;*/

    @BindView(R.id.cancel)
    MaterialButton cancel;
    //@BindView(R.id.rate) MaterialButton rate;

    @BindView(R.id.order_statuses_recycler_view)
    RecyclerView orderStatusesRecyclerView;

    private GoogleMap map;

    private Marker startMarker;
    private Marker endMarker;
    private Marker driverMarker;

    private Handler handler;

    private boolean mapMoved = false;

    private TimeLineAdapter timeLineAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_track);
        ButterKnife.bind(this);
        orderPresenter = new OrderPresenter(this);
        orderPresenter.setGeocoder(new Geocoder(this, LocaleUtil.getInstance().createLocale()));

    }

    @Override
    protected void onResume() {
        super.onResume();
        orderPresenter.start();

        Bundle bundle = getIntent().getExtras();

        if (bundle != null && bundle.containsKey("order_id")) {

            showLoadingDialog();
            orderId = bundle.getString("order_id");
            orderPresenter.getOrder(orderId);

        } else {
            showError(CacheUtil.getInstance().getTranslations(Text.RETRIEVING_ORDER_DETAILS_ERROR));
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Values.PERMISSION_LOCATION) {
            getLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == Values.PERMISSION_LOCATION) {
                getLocation();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        orderPresenter.unsubscribeAbly();
    }

    @Override
    public void onBackPressed() {
        if (handler != null) {
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

        slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (supportMapFragment != null) {
            supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {

                    map = googleMap;

                    map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                        @Override
                        public void onCameraIdle() {
                            if (mapMoved) {
                                mapMoved = false;
                                if (map.getCameraPosition().zoom > 12) {
                                    map.moveCamera(CameraUpdateFactory.zoomTo(12.0f));
                                }
                            }
                        }
                    });

                    initializeLocation();

                    slidingPanel.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
                        @Override
                        public void onPanelSlide(View panel, float slideOffset) {

                        }

                        @Override
                        public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {

                            switch (newState) {

                                case ANCHORED:
                                    map.setPadding(0, 0, 0, ViewUtil.dpToPx(OrderTrackActivity.this, 360));
                                    break;

                                case COLLAPSED:
                                    map.setPadding(0, 0, 0, ViewUtil.dpToPx(OrderTrackActivity.this, 80));
                                    break;

                            }
                        }
                    });

                }
            });
        }

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoadingDialog();
                orderPresenter.cancelOrder();
            }
        });

        orderStatusesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        timeLineAdapter = new TimeLineAdapter(this, orderPresenter);
        orderStatusesRecyclerView.setAdapter(timeLineAdapter);

        /*rate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RatingsBottomSheetDialogFragment ratingsBottomSheetDialogFragment = RatingsBottomSheetDialogFragment.newInstance(new RatingsBottomSheetDialogFragment.OnSubmit() {
                    @Override
                    public void onSubmit(Dialog dialog, List<Rating> ratings) {
                        dialog.dismiss();
                        showProgressDialog(CacheUtil.getInstance().getTranslations(Text.LOADING));
                        orderPresenter.sendOrderFeedback(ratings);
                    }
                }, orderPresenter.getRatings());

                ratingsBottomSheetDialogFragment.show(getSupportFragmentManager(), RatingsBottomSheetDialogFragment.class.getSimpleName());

            }
        });*/

    }

    @Override
    public void setupFonts() {
        code.setTypeface(FontHandler.getSemiboldTypeface());
        trackingStatus.setTypeface(FontHandler.getMediumTypeface());
        /*confirmed.setTypeface(FontHandler.getSemiboldTypeface());
        preparing.setTypeface(FontHandler.getSemiboldTypeface());
        onTheWay.setTypeface(FontHandler.getSemiboldTypeface());
        delivered.setTypeface(FontHandler.getSemiboldTypeface());
        rating.setTypeface(FontHandler.getSemiboldTypeface());*/
    }

    @Override
    public void setupTranslations() {
        title.setText(CacheUtil.getInstance().getTranslations(Text.ORDER_TRACKING));
        idLabel.setText(CacheUtil.getInstance().getTranslations(Text.YOUR_ORDER_ID));
        /*confirmed.setText(CacheUtil.getInstance().getTranslations(Text.ORDER_CONFIRMED));
        confirmedTime.setText(CacheUtil.getInstance().getTranslations(Text.ON_PROGRESS));
        preparing.setText(CacheUtil.getInstance().getTranslations(Text.PREPARING_FOOD));
        preparingTime.setText(CacheUtil.getInstance().getTranslations(Text.ON_PROGRESS));
        onTheWay.setText(CacheUtil.getInstance().getTranslations(Text.FOOD_ON_THE_WAY));
        onTheWayTime.setText(CacheUtil.getInstance().getTranslations(Text.ON_PROGRESS));
        delivered.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERED_TO_YOU));
        deliveredTime.setText(CacheUtil.getInstance().getTranslations(Text.ON_PROGRESS));
        rating.setText(CacheUtil.getInstance().getTranslations(Text.RATING));*/
        cancel.setText(CacheUtil.getInstance().getTranslations(Text.CANCEL_ORDER));
        trackingStatus.setText(CacheUtil.getInstance().getTranslations(Text.TRACKING_STATUS, "Tracking status"));
        //rate.setText(CacheUtil.getInstance().getTranslations(Text.RATE_NOW));

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

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(60000);
        locationRequest.setFastestInterval(60000);
        locationRequest.setSmallestDisplacement(0.0f);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder locationSettingsRequestBuilder = new LocationSettingsRequest.Builder();
        locationSettingsRequestBuilder.addLocationRequest(locationRequest);
        locationSettingsRequestBuilder.setAlwaysShow(true);

        LocationServices.getSettingsClient(this).checkLocationSettings(locationSettingsRequestBuilder.build()).addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {

                try {

                    task.getResult(ApiException.class);

                    getLocation();

                } catch (ApiException e) {

                    e.printStackTrace();

                    if (e.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {

                        try {

                            ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                            resolvableApiException.startResolutionForResult(OrderTrackActivity.this, Values.PERMISSION_LOCATION);

                        } catch (IntentSender.SendIntentException exception) {

                            exception.printStackTrace();

                        }

                    } else if (e.getStatusCode() == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                        AppLogger.error("Settings change unavailable");
                    }

                }

            }
        });
    }

    @Override
    public void getLocation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            orderPresenter.getLocation(new FusedLocationProvider(this));
        } else {
            permissions = true;
            CacheUtil.getInstance().setOrderLatitude(String.valueOf(Values.DEFAULT_LATITUDE));
            CacheUtil.getInstance().setOrderLongitude(String.valueOf(Values.DEFAULT_LONGITUDE));
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, Values.PERMISSION_LOCATION);
        }
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

        if (startMarker != null) {
            startMarker.remove();
        }

        if (endMarker != null) {
            endMarker.remove();
        }

        if (map != null) {

            if (startLocation != null) {

                MarkerOptions startMarkerOptions = new MarkerOptions();
                startMarkerOptions.position(startLocation);
                startMarkerOptions.anchor(0.25f, 0.5f);

                switch (orderType) {
                    case PICKUP:
                    case DRIVE_THRU:
                    case EAT_IN:
                    case DINE_IN:
                        startMarkerOptions.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(ViewUtil.vectorDrawableToBitmap(this, R.drawable.address_home), 95, 95, true)));
                        break;
                    case DELIVERY:
                        startMarkerOptions.icon(ViewUtil.vectorDrawableToBitmapDescriptor(this, R.drawable.marker_start));
                        break;
                }

                startMarker = map.addMarker(startMarkerOptions);

            }

            if (endLocation != null) {

                MarkerOptions endMarkerOptions = new MarkerOptions();
                endMarkerOptions.position(endLocation);
                endMarkerOptions.anchor(0.5f, 0.9f);

                switch (orderType) {
                    case PICKUP:
                    case DRIVE_THRU:
                    case EAT_IN:
                    case DINE_IN:
                        endMarkerOptions.icon(ViewUtil.vectorDrawableToBitmapDescriptor(this, R.drawable.marker_start));
                        break;
                    case DELIVERY:
                        endMarkerOptions.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(ViewUtil.vectorDrawableToBitmap(this, R.drawable.address_home), 95, 95, true)));
                        break;
                }

                endMarker = map.addMarker(endMarkerOptions);

            }

            mapMoved = true;
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120));

            dismissDialogs();

        }

    }

    @Override
    public void showOrder(OrderDetails order) {

        String displayId = order.getDisplayId();

        if (displayId == null || displayId.isEmpty() || displayId.equalsIgnoreCase("null")) {
            displayId = order.getCode();
        }

        if (displayId == null || displayId.isEmpty() || displayId.equalsIgnoreCase("null")) {
            displayId = order.getId();
        }

        code.setText(StringUtil.localize(displayId));

        //orderPresenter.getStatuses();
        //orderPresenter.getStoreDetails(new String(Base64.decode(getResources().getString(R.string.google_maps_api_key_encoded), Base64.DEFAULT)));

        boolean isWaybackBurgers = BuildConfig.FLAVOR.equals("wayback_burgers") || CacheUtil.getInstance().getConceptKey().equals("CGqbTzpyKa9");
        cancel.setVisibility(!isWaybackBurgers && order.isCanCancel() && order.getOrderStatus().getSequence() < OrderStatusType.FINISHED.getValue() ? View.VISIBLE : View.GONE);

    }

    @Override
    public void showOrderStatuses(ArrayList<OrderStatus> orderStatuses) {

        if (orderStatuses.size() > 0) {
            timeLineAdapter.notifyDataSetChanged();
            trackingStatus.setVisibility(View.VISIBLE);
            orderStatusesRecyclerView.setVisibility(View.VISIBLE);
        } else {
            trackingStatus.setVisibility(View.GONE);
            orderStatusesRecyclerView.setVisibility(View.GONE);
        }

        /*if(orderStatuses.size() > 0) {

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

        }*/

        slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);

        map.setPadding(0, 0, 0, ViewUtil.dpToPx(OrderTrackActivity.this, 360));

        orderPresenter.getStoreDetails();

    }

    @Override
    public void setTimeLine(OrderStatus orderStatus) {

    }

    @Override
    public void showStatus(OrderStatus orderStatus) {
        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat fromSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                fromSimpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
                SimpleDateFormat toSimpleDateFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                toSimpleDateFormat.setTimeZone(TimeZone.getDefault());

                if(orderStatus.getSequence() == OrderStatusType.POSTED.getValue()) {
                    try {
                        Date date = fromSimpleDateFormat.parse(orderStatus.getTime());
                        confirmed.setChecked(true);
                        confirmed.setTextColor(ViewUtil.getColorMain(OrderTrackActivity.this));
                        confirmedTime.setText(toSimpleDateFormat.format(date));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                if(orderStatus.getSequence() == OrderStatusType.IN_KITCHEN.getValue()) {

                    if(!confirmed.isChecked()) {
                        confirmed.setChecked(true);
                        confirmed.setTextColor(ViewUtil.getColorMain(OrderTrackActivity.this));
                        confirmedTime.setText("");
                    }

                    try {
                        Date date = fromSimpleDateFormat.parse(orderStatus.getTime());
                        preparing.setChecked(true);
                        preparing.setTextColor(ViewUtil.getColorMain(OrderTrackActivity.this));
                        preparingTime.setText(toSimpleDateFormat.format(date));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                if(orderStatus.getSequence() == OrderStatusType.DELIVERY_IN_PROGRESS.getValue()) {

                    if(!confirmed.isChecked()) {
                        confirmed.setChecked(true);
                        confirmed.setTextColor(ViewUtil.getColorMain(OrderTrackActivity.this));
                        confirmedTime.setText("");
                    }

                    if(!preparing.isChecked()) {
                        preparing.setChecked(true);
                        preparing.setTextColor(ViewUtil.getColorMain(OrderTrackActivity.this));
                        preparingTime.setText("");
                    }

                    try {
                        Date date = fromSimpleDateFormat.parse(orderStatus.getTime());

                        onTheWay.setChecked(true);
                        onTheWay.setTextColor(ViewUtil.getColorMain(OrderTrackActivity.this));
                        onTheWayTime.setText(toSimpleDateFormat.format(date));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                if(orderStatus.getSequence() >= OrderStatusType.ARRIVED_AT_CUSTOMER.getValue() && orderStatus.getSequence() <= OrderStatusType.FINISHED.getValue()) {

                    if(!confirmed.isChecked()) {
                        confirmed.setChecked(true);
                        confirmed.setTextColor(ViewUtil.getColorMain(OrderTrackActivity.this));
                        confirmedTime.setText("");
                    }

                    if(!preparing.isChecked()) {
                        preparing.setChecked(true);
                        preparing.setTextColor(ViewUtil.getColorMain(OrderTrackActivity.this));
                        preparingTime.setText("");
                    }

                    if(!onTheWay.isChecked()) {
                        onTheWay.setChecked(true);
                        onTheWay.setTextColor(ViewUtil.getColorMain(OrderTrackActivity.this));
                        onTheWay.setText("");
                    }

                    try {
                        Date date = fromSimpleDateFormat.parse(orderStatus.getTime());
                        delivered.setChecked(true);
                        delivered.setTextColor(ViewUtil.getColorMain(OrderTrackActivity.this));
                        deliveredTime.setText(toSimpleDateFormat.format(date));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    rating.setChecked(true);
                    rating.setTextColor(ViewUtil.getColorMain(OrderTrackActivity.this));
                    rate.setTextColor(ContextCompat.getColor(OrderTrackActivity.this, R.color.white));
                    rate.setBackgroundColor(ViewUtil.getColorMain(OrderTrackActivity.this));

                    ColorStateList colorStateList = new ColorStateList(new int[][] {
                            new int[] {-android.R.attr.state_pressed},
                            new int[] { android.R.attr.state_pressed}}, new int[] {
                            ViewUtil.getColorMain(OrderTrackActivity.this),
                            ViewUtil.getColorMain(OrderTrackActivity.this)

                    });

                    rate.setStrokeColor(colorStateList);
                    rate.setEnabled(true);

                    cancel.setVisibility(View.GONE);

                }
            }
        });*/

    }

    @Override
    public void showDriver(LatLng latLng) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (driverMarker != null) {
                    driverMarker.remove();
                }

                if (map != null) {

                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.anchor(0.5f, 0.5f);

                    BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(OrderTrackActivity.this, R.drawable.marker_location);
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), 95, 95, true)));

                    driverMarker = map.addMarker(markerOptions);

                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    builder.include(latLng);

                    if (startMarker != null) {
                        builder.include(startMarker.getPosition());
                    }

                    if (endMarker != null) {
                        builder.include(endMarker.getPosition());
                    }

                    if (startMarker == null && endMarker == null) {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
                    } else {
                        map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
                    }

                }
            }
        });

    }

    @Override
    public void updateDriver(LatLng latLng) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (driverMarker != null) {
                    LatLngInterpolator interpolator = new LatLngInterpolator.Linear();
                    MarkerAnimation.animateMarker(driverMarker, latLng, interpolator);
                }
            }
        });

    }

    @Override
    public void showMessage(String message) {
        dismissDialogs();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showRatings(OrderStatus orderStatus, List<Rating> ratings) {
        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (orderStatus.getSequence() >= OrderStatusType.ARRIVED_AT_CUSTOMER.getValue() && orderStatus.getSequence() <= OrderStatusType.FINISHED.getValue() && ratings.size() > 0) {
                    deliveredView.setVisibility(View.VISIBLE);
                    rating.setVisibility(View.VISIBLE);
                    rate.setVisibility(View.VISIBLE);
                    rate.setEnabled(true);
                } else {
                    deliveredView.setVisibility(View.GONE);
                    rating.setVisibility(View.GONE);
                    rate.setVisibility(View.GONE);
                    rate.setEnabled(false);
                }

            }
        });*/
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

}
