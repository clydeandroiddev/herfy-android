package com.herfy.subscription.branch;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.order.OrderContract;
import com.herfy.order.OrderPresenter;
import com.herfy.order.adapters.StoresRecyclerViewAdapter;
import com.herfy.util.AppLogger;
import com.herfy.util.StringUtil;
import com.herfy.util.ViewUtil;
import com.herfy.utils.constants.Text;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.views.NestedScrollableViewHelper;
import com.location.FusedLocationProvider;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PickupBranchActivity extends BaseActivity implements OrderContract.View {

    private OrderContract.Presenter orderPresenter;
    private StoresRecyclerViewAdapter storesRecyclerViewAdapter;

    private GoogleMap googleMap;

    private boolean permissions = false;

    @BindView(R.id.sliding_panel)
    SlidingUpPanelLayout slidingPanel;
    @BindView(R.id.confirm)
    MaterialButton confirm;
    @BindView(R.id.left_label)
    AppCompatTextView leftLabel;
    @BindView(R.id.title)
    AppCompatTextView title;
    @BindView(R.id.order_type_label)
    AppCompatTextView orderTypeLabel;
    @BindView(R.id.order_type_location)
    AppCompatTextView orderTypeLocation;
    @BindView(R.id.location)
    FloatingActionButton location;
    @BindView(R.id.stores_list)
    RecyclerView storesList;

    private ArrayList<Marker> storeMarkers = new ArrayList<>();
    private String selectedId = "";
    private Marker locationMarker;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pickup_branch);
        ButterKnife.bind(this);
        selectedId = getIntent().getStringExtra("selectedId");
        orderPresenter = new OrderPresenter(this);
        orderPresenter.start();
        try {
            orderPresenter.getStores(1);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.left_layout)
    void back() {
        finish();
    }

    @OnClick(R.id.confirm)
    void confirm() {
        Store store = orderPresenter.getStore(true, storesRecyclerViewAdapter.getSelected());
        Intent intent = getIntent();
        intent.putExtra("selectedId", store.getId());
        intent.putExtra("branchName", store.getAttributes().getName());
        setResult(RESULT_OK, intent);
        finish();
    }

    @OnClick(R.id.location)
    void location() {
        initializeLocation();
    }

    @Override
    public void setupViews() {
        slidingPanel.setScrollableViewHelper(new NestedScrollableViewHelper());

        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.order_type_map);
        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {

                googleMap = map;
                googleMap.getUiSettings().setCompassEnabled(false);
                googleMap.getUiSettings().setZoomControlsEnabled(false);
                googleMap.getUiSettings().setMapToolbarEnabled(false);
                googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        initializeLocation();
                        googleMap.setPadding(0, 0, 0, ViewUtil.dpToPx(PickupBranchActivity.this, 255));
                    }
                });
                googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        int position = Integer.parseInt(marker.getTitle());
                        storesRecyclerViewAdapter.setSelected(position);
                        confirm.setBackgroundColor(position != -1 ? View.VISIBLE : View.GONE);
                        //confirm.getBackground().setColorFilter(position != -1 ? Color.parseColor(CacheUtil.getInstance().getColorMain()) : Color.parseColor("#B5B5B5"), PorterDuff.Mode.SRC_IN);
                        confirm.setBackgroundColor(position != -1 ? Color.parseColor(CacheUtil.getInstance().getColorMain()) : Color.parseColor("#B5B5B5"));
                        confirm.setFocusable(position != -1);
                        confirm.setClickable(position != -1);
                        confirm.setVisibility(position != -1 ? View.VISIBLE : View.GONE);
                        showStore(true, Integer.parseInt(marker.getTitle()));
                        return true;
                    }
                });
            }
        });

        storesList.setLayoutManager(new LinearLayoutManager(this));
        storesRecyclerViewAdapter = new StoresRecyclerViewAdapter(orderPresenter, true);
        storesList.setAdapter(storesRecyclerViewAdapter);

    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {
        leftLabel.setText(CacheUtil.getInstance().getTranslations(Text.CANCEL, "CANCEL"));
        title.setText(CacheUtil.getInstance().getTranslations(Text.PICK_BRANCH, "PICK BRANCH"));
        orderTypeLocation.setText(CacheUtil.getInstance().getTranslations(Text.NO_BRANCH_PICKED, "No branch picked"));
        orderTypeLabel.setText(CacheUtil.getInstance().getTranslations(Text.PICK_UP_FROM, "PICKUP FROM"));
        confirm.setText(CacheUtil.getInstance().getTranslations(Text.CONFIRM_CAPS, "CONFIRM"));
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

        LocationServices.getSettingsClient(this).checkLocationSettings(locationSettingsRequestBuilder.build()).addOnCompleteListener(task -> {
            try {
                task.getResult(ApiException.class);
                getLocation();
            } catch (ApiException e) {
                e.printStackTrace();
                if (e.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                        resolvableApiException.startResolutionForResult(PickupBranchActivity.this, Values.PERMISSION_LOCATION);
                    } catch (IntentSender.SendIntentException exception) {
                        exception.printStackTrace();
                    }
                } else if (e.getStatusCode() == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                    AppLogger.error("Settings change unavailable");
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
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, Values.PERMISSION_LOCATION);
        }
    }

    @Override
    public void showLocation(boolean isPlace, LatLng latLng, String address) {
        location.setImageResource(R.drawable.ic_location_enabled);
        location.setColorFilter(Color.parseColor(CacheUtil.getInstance().getColorMain()));

        if (locationMarker != null) {
            locationMarker.remove();
        }

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.marker_location);
        Bitmap bitmap = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), 110, 110, true);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        ColorFilter filter = new PorterDuffColorFilter(Color.parseColor(CacheUtil.getInstance().getColorMain()), PorterDuff.Mode.SRC_IN);
        paint.setColorFilter(filter);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap));

        markerOptions.anchor(0.5f, 0.5f);

        locationMarker = googleMap.addMarker(markerOptions);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11f));
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

        if (!pickupStores.isEmpty()) {
            storesRecyclerViewAdapter.notifyDataSetChanged();
//            if (slidingPanel.getPanelHeight() != getResources().getDimensionPixelSize(R.dimen.pickup_branch_panel_height)) {
//                slidingPanel.setPanelHeight(getResources().getDimensionPixelSize(R.dimen.pickup_branch_panel_height));
//                expandPanel();
//            }
            if (storeMarkers.isEmpty()) {
                for (int i = 0; i < pickupStores.size(); i++) {
                    Store store = pickupStores.get(i);
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.title(String.valueOf(i));
                    markerOptions.position(new LatLng(store.getAttributes().getLat(), store.getAttributes().getLng()));
                    markerOptions.icon(ViewUtil.vectorDrawableToBitmapDescriptor(this, R.drawable.marker_store_unselected));
                    markerOptions.anchor(0.5f, 1f);
                    Marker marker = googleMap.addMarker(markerOptions);
                    marker.setVisible(CacheUtil.getInstance().getOrderType() != OrderType.DELIVERY);
                    storeMarkers.add(marker);
                }
            }

            if (!selectedId.isEmpty()) {
                int position = orderPresenter.getStorePosition(true, selectedId);
                selectStore(position, orderPresenter.getDineInStores().get(position), true);
            }

        }

    }

    @Override
    public void setStore(Store store, boolean selected) {

    }

    @Override
    public void callStore(Store store) {

    }

    @Override
    public void selectStore(int position, Store store, boolean initialLoad) {
        if(store != null) {
            orderTypeLocation.setText(store.getAttributes().getName());
        }
        storesRecyclerViewAdapter.setSelected(position);
        confirm.setBackgroundColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
        confirm.setFocusable(storesRecyclerViewAdapter.getSelected() != -1);
        confirm.setClickable(storesRecyclerViewAdapter.getSelected() != -1);
        showStore(false, position);
    }

    @Override
    public void showStore(boolean markerClicked, int position) {

        if(markerClicked && storesRecyclerViewAdapter.getSelected() == position) {
            Store branch = orderPresenter.getStore(true, position);
            Uri.Builder directionsBuilder = new Uri.Builder()
                    .scheme("https")
                    .authority("www.google.com")
                    .appendPath("maps")
                    .appendPath("dir")
                    .appendPath("")
                    .appendQueryParameter("api", "1")
                    .appendQueryParameter("travelmode", "driving")
                    .appendQueryParameter("destination", branch.getAttributes().getLat() + "," + branch.getAttributes().getLng());

            if(StringUtil.hasOrderLatLong()) {
                directionsBuilder.appendQueryParameter("origin", CacheUtil.getInstance().getOrderLatitude() + "," + CacheUtil.getInstance().getOrderLongitude());
            }

            startActivity(new Intent(Intent.ACTION_VIEW, directionsBuilder.build()));
        } else {

            for (Marker storeMarker : storeMarkers) {

                if (storeMarker.getTitle() != null && storeMarker.getTitle().equals(String.valueOf(position))) {

                    Store store = orderPresenter.getStore(true, position);

                    LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                    View view = inflater.inflate(R.layout.marker_store_new, null);

                    MaterialButton open = view.findViewById(R.id.open);
                    open.setText(CacheUtil.getInstance().getTranslations(Text.OPEN_IN_GOOGLE_MAPS, "Open in Google Maps"));
                    open.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Uri.Builder directionsBuilder = new Uri.Builder()
                                    .scheme("https")
                                    .authority("www.google.com")
                                    .appendPath("maps")
                                    .appendPath("dir")
                                    .appendPath("")
                                    .appendQueryParameter("api", "1")
                                    .appendQueryParameter("travelmode", "driving")
                                    .appendQueryParameter("destination", store.getAttributes().getLat() + "," + store.getAttributes().getLng());

                            if(StringUtil.hasOrderLatLong()) {
                                directionsBuilder.appendQueryParameter("origin", CacheUtil.getInstance().getOrderLatitude() + "," + CacheUtil.getInstance().getOrderLongitude());
                            }

                            startActivity(new Intent(Intent.ACTION_VIEW, directionsBuilder.build()));
                        }
                    });

                    storeMarker.setIcon(BitmapDescriptorFactory.fromBitmap(ViewUtil.createDrawableFromView(this, view)));
                    storeMarker.setAnchor(0.1f, 1.0f);

                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(storeMarker.getPosition(), 11f));

                } else {

                    storeMarker.setIcon(ViewUtil.vectorDrawableToBitmapDescriptor(this, R.drawable.marker_store_unselected));
                    storeMarker.setAnchor(0.5f, 1f);

                }
            }
        }
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

    @Override
    public void setPresenter(OrderContract.Presenter presenter) {

    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {

    }

}
