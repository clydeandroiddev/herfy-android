package com.herfy.order.views;

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
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.cart.views.CartDialogFragment;
import com.herfy.order.OrderContract;
import com.herfy.order.OrderPresenter;
import com.herfy.util.AppLogger;
import com.herfy.utils.constants.Text;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.AnimUtil;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.location.FusedLocationProvider;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderDetails;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatus;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.Store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AddressActivity extends BaseActivity implements OrderContract.View {

    private OrderContract.Presenter orderPresenter;

    private boolean home = false;
    private boolean menu = false;
    private boolean cart = false;
    private boolean permissions = false;

    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.subtitle)
    TextView subtitle;

    @BindView(R.id.back)
    MaterialButton back;

    @BindView(R.id.cart)
    ConstraintLayout cartContainer;

    @BindView(R.id.location)
    FloatingActionButton location;

    @BindView(R.id.use)
    MaterialButton use;

    private GoogleMap googleMap;

    private Marker locationMarker;
    private boolean fromShowLocation = false;

    private boolean fromSubs = false;

    private boolean fromAccount = false;

    private UpdateAddAddressDialogFragment updateAddAddressDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);
        ButterKnife.bind(this);
        orderPresenter = new OrderPresenter(this);
        orderPresenter.setGeocoder(new Geocoder(this, LocaleUtil.getInstance().createLocale()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        showLoadingDialog();
        CacheUtil.getInstance().setOrderType(OrderType.DELIVERY);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.containsKey("fromSubs")) {
                fromSubs = bundle.getBoolean("fromSubs");
            }

            if (bundle.containsKey("fromAccount")) {
                fromAccount = bundle.getBoolean("fromAccount");
            }

            if (bundle.containsKey("home")) {
                home = bundle.getBoolean("home");
            }

            if (bundle.containsKey("menu")) {
                menu = bundle.getBoolean("menu");
            }

            if (bundle.containsKey("cart")) {
                cart = bundle.getBoolean("cart");
            }

            if (bundle.containsKey("fromCart")) {
                cart = bundle.getBoolean("fromCart");
            }
        }

        orderPresenter.start();
        orderPresenter.getAddresses(false);

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
            } else if (requestCode == Values.PERMISSION_CALL) {
                orderPresenter.callStore();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(AnimUtil.inF(), AnimUtil.inS());
        finish();
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//        startActivity(intent);
//        overridePendingTransition(AnimUtil.outF(), AnimUtil.outS());
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
        back.setVisibility(View.VISIBLE);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        cartContainer.setVisibility(View.GONE);

        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.address_map);
        if (supportMapFragment != null) {
            setUpMapLocale();
            supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(@NonNull GoogleMap map) {

                    googleMap = map;
                    googleMap.getUiSettings().setCompassEnabled(false);
                    googleMap.getUiSettings().setZoomControlsEnabled(false);
                    googleMap.getUiSettings().setMapToolbarEnabled(false);
                    googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                        @Override
                        public void onMapLoaded() {
                            showLoadingDialog();
                            initializeLocation();
                        }
                    });

                    googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                        @Override
                        public void onMapClick(@NonNull LatLng latLng) {
                            if (locationMarker != null) {
                                locationMarker.setVisible(true);
                            }
                            orderPresenter.getAddress(latLng);
                            googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                        }
                    });

                    googleMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                        @Override
                        public void onCameraIdle() {
                            if (!fromShowLocation && locationMarker != null) {
                                locationMarker.setVisible(true);
                            } else {
                                fromShowLocation = false;
                            }
                            orderPresenter.getAddress(googleMap.getCameraPosition().target);
                        }
                    });

                }
            });
        }

        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initializeLocation();
            }
        });

        use.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoadingDialog();
                orderPresenter.updateAddAddress();
            }
        });

    }

    private void setUpMapLocale() {
        String languageToLoad = "en";
        if (LocaleUtil.getInstance().getLanguage().equalsIgnoreCase("ar")) {
            languageToLoad = "ar";
        } else if (LocaleUtil.getInstance().getLanguage().equalsIgnoreCase("fr")) {
            languageToLoad = "fr";
        }
        Locale locale = new Locale(languageToLoad);
        Locale.setDefault(locale);
        getBaseContext().getResources().getConfiguration().setLocale(locale);
    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {
        back.setText(CacheUtil.getInstance().getTranslations(Text.BACK, "Back"));
        title.setText(CacheUtil.getInstance().getTranslations(Text.ADD_A_NEW_ADDRESS_CAPS, "ADD A NEW ADDRESS"));
        subtitle.setText(CacheUtil.getInstance().getTranslations(Text.PLEASE_DRAG_THE_MAP, "Please drag the map until the pin is on the correct location on the map"));
        use.setText(CacheUtil.getInstance().getTranslations(Text.USE_THIS_LOCATION_CAPS, "USE THIS LOCATION"));
    }

    @Override
    public void showOrderTypes(Set<OrderType> orderTypes) {

    }

    @Override
    public void addToCartFavorite(MenuItem menuItem) {

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
                            resolvableApiException.startResolutionForResult(AddressActivity.this, Values.PERMISSION_LOCATION);

                        } catch (IntentSender.SendIntentException exception) {
                            dismissDialogs();
                            exception.printStackTrace();

                        }

                    } else if (e.getStatusCode() == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                        dismissDialogs();
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
        markerOptions.visible(false);

        locationMarker = googleMap.addMarker(markerOptions);

        fromShowLocation = true;
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));

        dismissDialogs();
    }

    @Override
    public void showAddresses(ArrayList<Address> bookmarkedAddresses, ArrayList<Address> otherAddresses) {
        dismissDialogs();
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
        dismissDialogs();
        try {
            updateAddAddressDialogFragment = UpdateAddAddressDialogFragment.newInstance(orderPresenter, updateAddress, saveAddress, fromAccount, new UpdateAddAddressDialogFragment.OnAction() {
                @Override
                public void onConfirm(String phoneNumber, String note, String label, boolean isSkip, boolean isExistingAddress) {
                    showLoadingDialog();
                    CacheUtil.shouldDismiss = true;

                    if (isSkip) {
                        updateAddAddressDialogFragment.dismiss();
                        dismissDialogs();
                        return;
                    }

                    if (AuthUtil.getInstance().isSignedIn() || orderPresenter.validateUpdateAddAddress(phoneNumber)) {
                        if (updateAddress != null) {
                            updateAddress.getAttributes().setTelephone(phoneNumber);
                            updateAddress.getAttributes().setInstructions(note);
                            updateAddress.getAttributes().setLabel(label);
                            orderPresenter.updateAddress(false, updateAddress, false);
                            if (isExistingAddress) {
                                orderPresenter.updateAddress(true, updateAddress, isExistingAddress);
                            } else {
                                orderPresenter.updateAddress(false, updateAddress, false);
                            }
                        } else {
                            orderPresenter.addAddress(phoneNumber, label, note, "");
                        }
                    } else {
                        dismissDialogs();
                    }
                }
            });
            updateAddAddressDialogFragment.show(getSupportFragmentManager(), UpdateAddAddressDialogFragment.class.getSimpleName());
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        updateAddress(address);
    }

    @Override
    public void saveAddress(Address address) {
        updateAddress(address.getAttributes().getLine1());
    }

    @Override
    public void updateAddress(String address) {

        if (updateAddAddressDialogFragment != null) {
            updateAddAddressDialogFragment.dismiss();
        }
        dismissDialogs();

        if (fromSubs || fromAccount) {
            setResult(RESULT_OK);
            overridePendingTransition(AnimUtil.inF(), AnimUtil.inS());
        } else {
            HashMap<String, String> eventAttributes = new HashMap<>();
            eventAttributes.put("Delivery", address);
            logEvent("OrderTypeSelection", eventAttributes, null, 0);
            if (cart) {
                Values.fromAddAddressBool = true;
            }
            setResult(RESULT_OK);
            overridePendingTransition(AnimUtil.outF(), AnimUtil.outS());
        }
        finish();
    }

    @Override
    public void logFirebaseEvent(String event) {

    }

    @Override
    public void showUnavailableMenuItems() {
        dismissDialogs();
        CartDialogFragment cartDialogFragment = CartDialogFragment.newInstance(orderPresenter);
        cartDialogFragment.show(getSupportFragmentManager(), CartDialogFragment.class.getSimpleName());
    }

    @Override
    public void deleteUnavailableItems() {
        showMessage(CacheUtil.getInstance().getTranslations(Text.REMOVED_ITEM_CART_SUCCESSFULLY, "Item removed from cart"));
        orderPresenter.setOrderType();
    }

    @Override
    public void showPhoneNumberError(String error) {
        if (updateAddAddressDialogFragment != null) {
            updateAddAddressDialogFragment.showPhoneNumberError(error);
        }
    }

    @Override
    public void showError(String error) {
        showAlertDialog("", error);
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
        dismissDialogs();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showRatings(OrderStatus orderStatus, List<Rating> ratings) {

    }

    @Override
    public void showOrders() {

    }

    @Override
    public void showMenuItem(String categoryId, String menuItemId) {

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
        runOnUiThread(() -> {
                    dismissDialogs();
                    showAlertDialog(
                            "",
                            error,
                            CacheUtil.getInstance().getTranslations(Text.OK),
                            (dialogInterface, i) -> {
                                orderPresenter.setOrderType();
                            }
                    );
                }
        );
    }

    @Override
    public void setCloseTime(String time) {

    }

}