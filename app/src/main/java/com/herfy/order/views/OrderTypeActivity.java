package com.herfy.order.views;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.cart.views.CartDialogFragment;
import com.herfy.font.FontHandler;
import com.herfy.main.MainActivity;
import com.herfy.main.NavigationActivity;
import com.herfy.order.OrderContract;
import com.herfy.order.OrderPresenter;
import com.herfy.order.adapters.AddressesRecyclerViewAdapter;
import com.herfy.order.adapters.StoresRecyclerViewAdapter;
import com.herfy.util.AppLogger;
import com.herfy.util.StringUtil;
import com.herfy.util.ViewUtil;
import com.herfy.utils.constants.Text;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.AnimUtil;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.views.NestedScrollableViewHelper;
import com.location.FusedLocationProvider;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
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

public class OrderTypeActivity extends BaseActivity implements OrderContract.View {

    private OrderContract.Presenter orderPresenter;

    private boolean home = false;
    private boolean menu = false;
    private boolean cart = false;
    private boolean permissions = false;

    @BindView(R.id.close) ImageButton close;

    @BindView(R.id.message) TextView message;
    @BindView(R.id.current_location_label) TextView currentLocationLabel;
    @BindView(R.id.current_location) TextView currentLocation;
    @BindView(R.id.add_address) TextView addAddress;
    @BindView(R.id.home_addresses_label) TextView homeAddressesLabel;
    @BindView(R.id.work_addresses_label) TextView workAddressesLabel;
    @BindView(R.id.other_addresses_label) TextView otherAddressesLabel;
    @BindView(R.id.addresses_continue) TextView addressesContinue;
    @BindView(R.id.stores_continue) TextView storesContinue;
    //@BindView(R.id.name) TextView storeName;
    //@BindView(R.id.distance) TextView storeDistance;
    //@BindView(R.id.telephone) TextView storeTelephone;
    //@BindView(R.id.hours) TextView storeHours;
    //@BindView(R.id.open_now) TextView storeOpenNow;
    //@BindView(R.id.show_facility) TextView storeShowFacility;
    //@BindView(R.id.store_select) TextView storeSelect;

    @BindView(R.id.tab_layout) TabLayout tabLayout;

    @BindView(R.id.addresses_nested_scroll_view) NestedScrollView addressesNestedScrollView;

    @BindView(R.id.sliding_panel) SlidingUpPanelLayout slidingPanel;

    //@BindView(R.id.map_list_view) LinearLayout mapListView;
    //@BindView(R.id.facilities) LinearLayout storeFacilities;

    //@BindView(R.id.map_view) MaterialButton mapView;
    //@BindView(R.id.list_view) MaterialButton listView;
    @BindView(R.id.confirm_location) MaterialButton confirmLocation;

    @BindView(R.id.map_container) ConstraintLayout mapContainer;
    //@BindView(R.id.stores_container) RelativeLayout storesContainer;
    //@BindView(R.id.store_details) ConstraintLayout storeDetails;

    //@BindView(R.id.store_close) RelativeLayout storeClose;
    @BindView(R.id.delivery_location) RelativeLayout deliveryLocation;

    @BindView(R.id.marker) ImageView marker;
    @BindView(R.id.current_location_icon) ImageView currentLocationIcon;
    @BindView(R.id.search_icon) ImageView searchIcon;
    @BindView(R.id.clear) ImageView clear;
    //@BindView(R.id.telephone_icon) ImageView storeTelephoneIcon;
    //@BindView(R.id.hours_icon) ImageView storeHoursIcon;
    //@BindView(R.id.distance_icon) ImageView storeDistanceIcon;
    //@BindView(R.id.open_now_icon) ImageView storeOpenNowIcon;

    @BindView(R.id.home_addresses_recycler_view) RecyclerView homeAddressesRecyclerView;
    @BindView(R.id.work_addresses_recycler_view) RecyclerView workAddressesRecyclerView;
    @BindView(R.id.other_addresses_recycler_view) RecyclerView otherAddressesRecyclerView;
    @BindView(R.id.stores_recycler_view) RecyclerView storesRecyclerView;

    @BindView(R.id.current_location_container) MaterialCardView currentLocationContainer;
    @BindView(R.id.add_address_container) MaterialCardView addAddressContainer;

    @BindView(R.id.search_container) CardView searchContainer;

    @BindView(R.id.search) AutoCompleteTextView search;

    private GoogleMap map;

    private boolean mapMoved = true;
    private boolean mapZoom = true;

    //private Marker mapMarker;
    private Marker locationMarker;
    private ArrayList<Marker> storeMarkers = new ArrayList<>();

    private AddressesRecyclerViewAdapter homeAddressesAdapter;
    private AddressesRecyclerViewAdapter workAddressesAdapter;
    private AddressesRecyclerViewAdapter otherAddressesAdapter;
    private StoresRecyclerViewAdapter storesRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_type);
        ButterKnife.bind(this);
        orderPresenter = new OrderPresenter(this);

        orderPresenter.setGeocoder(new Geocoder(this, LocaleUtil.getInstance().createLocale()));

    }

    @Override
    protected void onResume() {

        super.onResume();

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {

            if(bundle.containsKey("home")) {
                home = bundle.getBoolean("home");
            }

            if(bundle.containsKey("menu")) {
                menu = bundle.getBoolean("menu");
            }

            if(bundle.containsKey("cart")) {
               cart = bundle.getBoolean("cart");
            }

        }

        orderPresenter.start();

        if(tabLayout != null) {
            if (LocaleUtil.getInstance().isEnglish() || LocaleUtil.getInstance().isFrench()) {
                tabLayout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

            } else {
                tabLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

            }
        }

        if(mapContainer != null) {
            if(LocaleUtil.getInstance().isEnglish() || LocaleUtil.getInstance().isFrench()) {
                mapContainer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            } else {
                mapContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            }
        }

        if(!permissions) {
            getLocation();
        } else {
            permissions = false;
        }

        showOrderType(CacheUtil.getInstance().getOrderType());

        if(AuthUtil.getInstance().isSignedIn()) {
            showLoadingDialog();
            orderPresenter.getAddresses(false);
        } else {
            addressesNestedScrollView.setVisibility(View.GONE);
            addressesContinue.setVisibility(View.GONE);
        }
        try {
            orderPresenter.getStores(1);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        showLoadingDialog();
        orderPresenter.getAddresses(false);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Values.PERMISSION_LOCATION) {
            getLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
        CacheUtil.getInstance().setOrderType(OrderType.NONE);
        CacheUtil.getInstance().setOrderStore(null);
        //if(!splash) {
        dismissDialogs();
        Intent intent;
        if (cart || home || menu) {
            intent = new Intent(this, MainActivity.class);
            intent.putExtra("order_type", home);
            intent.putExtra("cart", cart);
            intent.putExtra("menu", menu);
                /*if(!concepts) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                }*/
        } else {
            intent = new Intent(this, NavigationActivity.class);
            intent.putExtra("order_type", true);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(AnimUtil.outF(), AnimUtil.outS());
        //finish();
        /*} else {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }*/
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

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        tabLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.background));
        tabLayout.removeAllTabs();

        Set<OrderType> supportedOrderTypes = CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes();

        if (supportedOrderTypes.contains(OrderType.DELIVERY)) {
            tabLayout.addTab(createTab("deliver", CacheUtil.getInstance().getTranslations(Text.DELIVERY), ContextCompat.getDrawable(this, R.drawable.order_type_delivery)));
        }

        if (supportedOrderTypes.contains(OrderType.PICKUP)) {
            tabLayout.addTab(createTab("pickup", CacheUtil.getInstance().getTranslations(Text.PICKUP), ContextCompat.getDrawable(this, R.drawable.order_type_pickup)));
        }

        if (supportedOrderTypes.contains(OrderType.DINE_IN)) {
            tabLayout.addTab(createTab("eat-in", CacheUtil.getInstance().getTranslations(Text.DINE_IN), ContextCompat.getDrawable(this, R.drawable.order_type_dine_in)));
        }

        for(int i = 0; i < tabLayout.getTabCount(); i++) {
            int color = Color.WHITE;
            if(i > 0) {
                color = Color.parseColor("#CCCCCC");
            }
            if(tabLayout.getTabAt(i) != null && tabLayout.getTabAt(i).getIcon() != null) {
                tabLayout.getTabAt(i).getIcon().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        }

        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.order_type_map);
        if(supportMapFragment != null) {
            supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {

                    if(map == null) {
                        initializeLocation();
                    }

                    map = googleMap;
                    map.getUiSettings().setCompassEnabled(false);
                    map.getUiSettings().setZoomControlsEnabled(false);
                    map.getUiSettings().setMapToolbarEnabled(false);

                    map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                        @Override
                        public void onMapClick(LatLng latLng) {

                            String currentLocation = orderPresenter.getAddress(latLng);

                            search.setThreshold(1000);
                            search.setText(currentLocation);
                            search.setThreshold(1);
                            search.clearFocus();

                            if(CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY && addressesNestedScrollView.getVisibility() == View.GONE) {
                                searchContainer.setVisibility(View.VISIBLE);
                            } else {
                                searchContainer.setVisibility(View.INVISIBLE);
                            }

                            /*if(mapMarker != null) {
                                mapMarker.remove();
                            }

                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(latLng);

                            mapMarker = map.addMarker(markerOptions);*/

                        }
                    });

                    map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                        @Override
                        public void onCameraIdle() {

                            if(!mapMoved) {

                                mapZoom = false;

                                LatLng latLng = map.getCameraPosition().target;

                                String currentLocation = orderPresenter.getAddress(latLng);

                                search.setThreshold(1000);
                                search.setText(currentLocation);
                                search.setThreshold(1);
                                search.clearFocus();

                                if(CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY && addressesNestedScrollView.getVisibility() == View.GONE) {
                                    searchContainer.setVisibility(View.VISIBLE);
                                } else {
                                    searchContainer.setVisibility(View.INVISIBLE);
                                }
                                /*if(mapMarker != null) {
                                    mapMarker.remove();
                                }

                                MarkerOptions markerOptions = new MarkerOptions();
                                markerOptions.position(latLng);

                                mapMarker = map.addMarker(markerOptions);*/

                            } else {
                                mapMoved = false;
                            }

                        }
                    });

                    map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(Marker marker) {
                            int position = Integer.parseInt(marker.getTitle());
                            storesRecyclerViewAdapter.setSelected(position);
                            storesContinue.setVisibility(position != -1 ? View.VISIBLE : View.GONE);
                            showStore(true, Integer.valueOf(marker.getTitle()));
                            return true;
                        }
                    });

                }
            });
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                String tag = (String) tab.getTag();
                if(tag != null) {
                    if (tag.equalsIgnoreCase("deliver")) {
                        selectDelivery();
                    } else if (tag.equalsIgnoreCase("pickup")) {
                        selectPickup();
                    } else if (tag.equalsIgnoreCase("eat-in")) {
                        selectDineIn();
                    }
                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

                String tag = (String) tab.getTag();
                if(tag != null) {
                    if (tag.equalsIgnoreCase("deliver")) {
                        selectDelivery();
                    }
                }

            }

        });

        currentLocationContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentLocationContainer.getStrokeWidth() == ViewUtil.dpToPx(OrderTypeActivity.this, 2)) {
                    addressesContinue.setVisibility(View.GONE);
                    currentLocationContainer.setStrokeWidth(1);
                    currentLocationContainer.setStrokeColor(Color.parseColor("#BEBEBE"));
                    currentLocationIcon.setImageResource(R.drawable.current_location);
                } else {
                    addressesContinue.setVisibility(View.VISIBLE);
                    currentLocationContainer.setStrokeWidth(ViewUtil.dpToPx(OrderTypeActivity.this, 2));
                    currentLocationContainer.setStrokeColor(ViewUtil.getColorMain(OrderTypeActivity.this));
                    currentLocationIcon.setImageResource(R.drawable.address_selected);
                    homeAddressesAdapter.setSelected(-1);
                    workAddressesAdapter.setSelected(-1);
                    otherAddressesAdapter.setSelected(-1);
                }
            }
        });

        addAddressContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchContainer.setVisibility(View.VISIBLE);
                confirmLocation.setVisibility(View.VISIBLE);
                marker.setVisibility(View.VISIBLE);
                deliveryLocation.setVisibility(View.VISIBLE);
                currentLocationContainer.setStrokeWidth(1);
                currentLocationContainer.setStrokeColor(Color.parseColor("#BEBEBE"));
                currentLocationIcon.setImageResource(R.drawable.current_location);
                homeAddressesAdapter.setSelected(-1);
                workAddressesAdapter.setSelected(-1);
                otherAddressesAdapter.setSelected(-1);
                addressesNestedScrollView.setVisibility(View.GONE);
                addressesContinue.setVisibility(View.GONE);
            }
        });

        homeAddressesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        homeAddressesAdapter = new AddressesRecyclerViewAdapter(this, orderPresenter, true, false);
        homeAddressesRecyclerView.setAdapter(homeAddressesAdapter);

        workAddressesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        workAddressesAdapter = new AddressesRecyclerViewAdapter(this, orderPresenter, false, true);
        workAddressesRecyclerView.setAdapter(workAddressesAdapter);

        otherAddressesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        otherAddressesAdapter = new AddressesRecyclerViewAdapter(this, orderPresenter, false, false);
        otherAddressesRecyclerView.setAdapter(otherAddressesAdapter);

        addressesContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showLoadingDialog();

                if (currentLocationContainer.getStrokeWidth() == ViewUtil.dpToPx(OrderTypeActivity.this, 2)) {

                    showLoadingDialog();
                    orderPresenter.updateAddAddress();

                } else if (homeAddressesAdapter.getSelected() >= 0) {

                    Address address = orderPresenter.getAddressHome(homeAddressesAdapter.getSelected());
                    showLoadingDialog();
                    orderPresenter.getStore(address);

                } else if (workAddressesAdapter.getSelected() >= 0) {

                    Address address = orderPresenter.getAddressWork(workAddressesAdapter.getSelected());
                    showLoadingDialog();
                    orderPresenter.getStore(address);

                } else if (otherAddressesAdapter.getSelected() >= 0) {

                    Address address = orderPresenter.getAddressOther(otherAddressesAdapter.getSelected());
                    showLoadingDialog();
                    orderPresenter.getStore(address);
                }

            }
        });

        storesContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoadingDialog();
                orderPresenter.continueStore(true, storesRecyclerViewAdapter.getSelected(), null);
            }
        });

        slidingPanel.setScrollableViewHelper(new NestedScrollableViewHelper());
        slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        slidingPanel.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                switch (newState) {
                    case HIDDEN:
                        map.setPadding(0, 0, 0, 0);
                        break;
                    case EXPANDED:
                        map.setPadding(0, 0, 0, ViewUtil.dpToPx(OrderTypeActivity.this, 370));
                        RelativeLayout.LayoutParams expandedLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        storesRecyclerView.setLayoutParams(expandedLayoutParams);
                        storesRecyclerView.setPadding(0, 0, 0, 0);
                        break;
                    case ANCHORED:
                        map.setPadding(0, 0, 0, ViewUtil.dpToPx(OrderTypeActivity.this, 260));
                        RelativeLayout.LayoutParams anchoredLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtil.dpToPx(OrderTypeActivity.this, 220));
                        storesRecyclerView.setLayoutParams(anchoredLayoutParams);
                        storesRecyclerView.setPadding(0, 0, 0, 0);
                        break;
                    case COLLAPSED:
                        map.setPadding(0, 0, 0, ViewUtil.dpToPx(OrderTypeActivity.this, 110));
                        RelativeLayout.LayoutParams collapsedLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtil.dpToPx(OrderTypeActivity.this, 130));
                        storesRecyclerView.setLayoutParams(collapsedLayoutParams);
                        storesRecyclerView.setPadding(0, 0, 0, ViewUtil.dpToPx(OrderTypeActivity.this, 50));
                        break;
                }
            }
        });

        /*mapContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                SlidingUpPanelLayout.LayoutParams layoutParams = new SlidingUpPanelLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                layoutParams.setMargins(0, mapContainer.getHeight() / 2, 0, 0);
                storesContainer.setLayoutParams(layoutParams);
                mapContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });*/

        /*mapView.setSelected(false);
        listView.setSelected(true);

        mapView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mapView.setSelected(true);
                listView.setSelected(false);


                mapListView.setVisibility(storesRecyclerViewAdapter.getSelected() != -1 ? View.GONE : View.VISIBLE);
                if(storesRecyclerViewAdapter.getSelected() != -1) {
                    storeDetails.setVisibility(View.VISIBLE);
                }

                deliveryLocation.setVisibility(View.VISIBLE);
                storesRecyclerView.setVisibility(View.GONE);

                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(mapContainer);

                if (storeDetails.getVisibility() == View.VISIBLE) {
                    constraintSet.connect(R.id.delivery_location, ConstraintSet.BOTTOM, R.id.store_details, ConstraintSet.TOP, ViewUtil.dpToPx(OrderTypeActivity.this, 16));
                } else {
                    constraintSet.connect(R.id.delivery_location, ConstraintSet.BOTTOM, R.id.map_list_view, ConstraintSet.TOP, ViewUtil.dpToPx(OrderTypeActivity.this, 16));
                }

                constraintSet.applyTo(mapContainer);

            }
        });

        listView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mapView.setSelected(false);
                listView.setSelected(true);

                deliveryLocation.setVisibility(View.GONE);
                storesRecyclerView.setVisibility(View.VISIBLE);

                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(mapContainer);

                if (storeDetails.getVisibility() == View.VISIBLE) {
                    constraintSet.connect(R.id.delivery_location, ConstraintSet.BOTTOM, R.id.store_details, ConstraintSet.TOP, ViewUtil.dpToPx(OrderTypeActivity.this, 16));
                } else {
                    constraintSet.connect(R.id.delivery_location, ConstraintSet.BOTTOM, R.id.map_list_view, ConstraintSet.TOP, ViewUtil.dpToPx(OrderTypeActivity.this, 16));
                }

                constraintSet.applyTo(mapContainer);

            }
        });

        storeClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                storeDetails.setVisibility(View.GONE);
                mapListView.setVisibility(View.VISIBLE);

                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(mapContainer);
                constraintSet.connect(R.id.delivery_location, ConstraintSet.BOTTOM, R.id.map_list_view, ConstraintSet.TOP, ViewUtil.dpToPx(OrderTypeActivity.this, 16));
                constraintSet.applyTo(mapContainer);

            }
        });

        storeSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });*/

        storesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        storesRecyclerViewAdapter = new StoresRecyclerViewAdapter(orderPresenter, true);
        storesRecyclerView.setAdapter(storesRecyclerViewAdapter);

        deliveryLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoadingDialog();
                mapZoom = true;
                initializeLocation();
            }
        });

        /*search.setThreshold(1);

        PlacesAutoCompleteAdapter placesAdapter = new PlacesAutoCompleteAdapter(this, android.R.layout.simple_list_item_1, orderPresenter.getPlacesClient());

        search.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(search.getWindowToken(), 0);

                showProgressDialog(CacheUtil.getInstance().getTranslations(Text.LOADING));
                orderPresenter.fetchPlace(placesAdapter.getItem(i).placeId.toString());
            }
        });

        search.setAdapter(placesAdapter);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(editable.length() != 0) {
                    searchIcon.setVisibility(View.INVISIBLE);
                    clear.setVisibility(View.VISIBLE);
                } else {
                    searchIcon.setVisibility(View.VISIBLE);
                    clear.setVisibility(View.GONE);
                }
            }
        };
        search.addTextChangedListener(textWatcher);*/

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                search.setText("");
                search.requestFocus();
            }
        });

        confirmLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if(mapMarker != null) {
                    showLoadingDialog();
                    orderPresenter.updateAddAddress();
                //}
            }
        });

    }

    @Override
    public void setupFonts() {

        //title.setTypeface(FontHandler.getSemiboldTypeface());
        message.setTypeface(FontHandler.getSemiboldTypeface());

        currentLocationLabel.setTypeface(FontHandler.getMediumTypeface());
        currentLocation.setTypeface(FontHandler.getMediumTypeface());
        addAddress.setTypeface(FontHandler.getMediumTypeface());
        homeAddressesLabel.setTypeface(FontHandler.getMediumTypeface());
        workAddressesLabel.setTypeface(FontHandler.getMediumTypeface());
        otherAddressesLabel.setTypeface(FontHandler.getMediumTypeface());
        addressesContinue.setTypeface(FontHandler.getMediumTypeface());
        storesContinue.setTypeface(FontHandler.getMediumTypeface());

        //mapView.setTypeface(FontHandler.getMediumTypeface());
        //listView.setTypeface(FontHandler.getMediumTypeface());

        /*storeName.setTypeface(FontHandler.getSemiboldTypeface());
        storeDistance.setTypeface(FontHandler.getMediumTypeface());
        storeTelephone.setTypeface(FontHandler.getMediumTypeface());
        storeHours.setTypeface(FontHandler.getMediumTypeface());
        storeOpenNow.setTypeface(FontHandler.getMediumTypeface());
        storeShowFacility.setTypeface(FontHandler.getRegularTypeface());
        storeSelect.setTypeface(FontHandler.getMediumTypeface());*/

        search.setTypeface(FontHandler.getRegularTypeface());

        confirmLocation.setTypeface(FontHandler.getSemiboldTypeface());

    }

    @Override
    public void setupTranslations() {

        //title.setText(CacheUtil.getInstance().getTranslations(Text.NEW_ORDER));
        message.setText(CacheUtil.getInstance().getTranslations(Text.HOW_TO_GET_FOOD));

        currentLocationLabel.setText(CacheUtil.getInstance().getTranslations(Text.CURRENT_LOCATION));
        addAddress.setText(CacheUtil.getInstance().getTranslations(Text.ADD_ADDRESS));
        homeAddressesLabel.setText(CacheUtil.getInstance().getTranslations(Text.HOME_CAPS));
        workAddressesLabel.setText(CacheUtil.getInstance().getTranslations(Text.WORK));
        otherAddressesLabel.setText(CacheUtil.getInstance().getTranslations(Text.OTHER));
        addressesContinue.setText(CacheUtil.getInstance().getTranslations(Text.CONTINUE_LABEL, "Continue"));
        storesContinue.setText(CacheUtil.getInstance().getTranslations(Text.CONTINUE_LABEL, "Continue"));

        //mapView.setText(CacheUtil.getInstance().getTranslations(Text.MAP_VIEW));
        //listView.setText(CacheUtil.getInstance().getTranslations(Text.LIST_VIEW));

        /*storeOpenNow.setText(CacheUtil.getInstance().getTranslations(Text.OPEN_NOW));
        storeShowFacility.setText(CacheUtil.getInstance().getTranslations(Text.SHOW_FACILITY));
        storeSelect.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_THIS_BRANCH));*/

        //dragLabel.setText(CacheUtil.getInstance().getTranslations(Text.CHOOSE_NEAREST_BRANCH));
        //stores.setText(CacheUtil.getInstance().getTranslations(Text.BRANCH_NAME));

        //deliveryAddressLabel.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERY_ADDRESS));
        //savedLocations.setText(CacheUtil.getInstance().getTranslations(Text.SAVED_DELIVERY_LOCATIONS));

        //search.setHint(CacheUtil.getInstance().getTranslations(Text.TYPE_DELIVERY_LOCATION));

        confirmLocation.setText(CacheUtil.getInstance().getTranslations(Text.CONFIRM_LOCATION));

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
        TabLayout.Tab tab = tabLayout.newTab().setText(text).setIcon(icon).setCustomView(R.layout.tab_item_order_type);
        tab.setTag(tag);

        if (tab.getCustomView() != null) {
            ImageView imageView = tab.getCustomView().findViewById(android.R.id.icon);
            ViewGroup.MarginLayoutParams lp = ((ViewGroup.MarginLayoutParams) imageView.getLayoutParams());
            lp.bottomMargin = 0;
            imageView.requestLayout();
        }

        return tab;
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

                    if(e.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {

                        try {

                            ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                            resolvableApiException.startResolutionForResult(OrderTypeActivity.this, Values.PERMISSION_LOCATION);

                        } catch (IntentSender.SendIntentException exception) {

                            exception.printStackTrace();

                        }

                    } else if(e.getStatusCode() == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                        AppLogger.error("Settings change unavailable");
                    }

                }

            }
        });
    }

    @Override
    public void getLocation() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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

        /*if(mapMarker != null) {
            mapMarker.remove();
        }*/

        MarkerOptions markerOptions;

        if(!isPlace) {

            if (locationMarker != null) {
                locationMarker.remove();
            }

            markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.marker_location);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), 95, 95, true)));
            markerOptions.anchor(0.5f, 0.5f);

            locationMarker = map.addMarker(markerOptions);

            if(!address.isEmpty()) {
                currentLocation.setText(address);
                currentLocationLabel.setVisibility(View.VISIBLE);
                currentLocationContainer.setVisibility(View.VISIBLE);
            } else {
                currentLocationLabel.setVisibility(View.GONE);
                currentLocationContainer.setVisibility(View.GONE);
            }

        }

        //if(tabLayout.getSelectedTabPosition() == 2) {

            /*if(address == null) {

                address = orderPresenter.getAddress(latLng);

            }*/

            search.setThreshold(1000);
            search.setText(address);
            search.setThreshold(1);
            search.clearFocus();

            if(CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY && addressesNestedScrollView.getVisibility() == View.GONE) {
                searchContainer.setVisibility(View.VISIBLE);
            } else {
                searchContainer.setVisibility(View.INVISIBLE);
            }

            /*markerOptions = new MarkerOptions();
            markerOptions.position(latLng);

            mapMarker = map.addMarker(markerOptions);*/

        //}

        mapMoved = true;

        if(!mapZoom) {
            map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        } else {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
        }

        dismissDialogs();

    }

    @Override
    public void showAddresses(ArrayList<Address> bookmarkedAddresses, ArrayList<Address> otherAddresses) {

        /*if(homeAddresses.size() > 0) {
            homeAddressesAdapter.notifyDataSetChanged();
            homeAddressesLabel.setVisibility(View.VISIBLE);
            homeAddressesRecyclerView.setVisibility(View.VISIBLE);
        } else {
            homeAddressesLabel.setVisibility(View.GONE);
            homeAddressesRecyclerView.setVisibility(View.GONE);
        }

        if(workAddresses.size() > 0) {
            workAddressesAdapter.notifyDataSetChanged();
            workAddressesLabel.setVisibility(View.VISIBLE);
            workAddressesRecyclerView.setVisibility(View.VISIBLE);
        } else {
            workAddressesLabel.setVisibility(View.GONE);
            workAddressesRecyclerView.setVisibility(View.GONE);
        }

        if(otherAddresses.size() > 0) {
            otherAddressesAdapter.notifyDataSetChanged();
            otherAddressesLabel.setVisibility(View.VISIBLE);
            otherAddressesRecyclerView.setVisibility(View.VISIBLE);
        } else {
            otherAddressesLabel.setVisibility(View.GONE);
            otherAddressesRecyclerView.setVisibility(View.GONE);
        }

        if(CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY && orderPresenter.getAddressesCount() > 0) {
            searchContainer.setVisibility(View.GONE);
            confirmLocation.setVisibility(View.GONE);
            marker.setVisibility(View.GONE);
            deliveryLocation.setVisibility(View.GONE);
            currentLocationContainer.setStrokeWidth(1);
            currentLocationContainer.setStrokeColor(Color.parseColor("#BEBEBE"));
            currentLocationIcon.setImageResource(R.drawable.current_location);
            homeAddressesAdapter.setSelected(-1);
            workAddressesAdapter.setSelected(-1);
            otherAddressesAdapter.setSelected(-1);
            addressesNestedScrollView.setVisibility(View.VISIBLE);
            if(currentLocationContainer.getStrokeWidth() == ViewUtil.dpToPx(OrderTypeActivity.this, 2) && homeAddressesAdapter.getSelected() != -1 || workAddressesAdapter.getSelected() != -1 || otherAddressesAdapter.getSelected() != -1) {
                addressesContinue.setVisibility(View.VISIBLE);
            } else {
                addressesContinue.setVisibility(View.GONE);
            }
        }*/

        dismissDialogs();
    }

    @Override
    public void setAddress(Address address, boolean selected) {

    }

    @Override
    public void selectAddressHome(Address address, boolean redownloadMenu) {

    }
    @Override
    public void navigateMenuItemDetails(MenuItem menuItem) {

    }

    @Override
    public void selectAddressWork(int position) {
        addressesContinue.setVisibility(View.VISIBLE);
        currentLocationContainer.setStrokeWidth(1);
        currentLocationContainer.setStrokeColor(Color.parseColor("#BEBEBE"));
        currentLocationIcon.setImageResource(R.drawable.current_location);
        if(currentLocationContainer.getVisibility() == View.VISIBLE) {
            currentLocationContainer.setVisibility(View.GONE);
            currentLocationContainer.setVisibility(View.VISIBLE);
        }
        homeAddressesAdapter.setSelected(-1);
        workAddressesAdapter.setSelected(position);
        otherAddressesAdapter.setSelected(-1);
    }

    @Override
    public void selectAddressOther(Address address, boolean redownloadMenu) {

    }

    @Override
    public void updateAddAddress(@Nullable Address updateAddress, @Nullable android.location.Address saveAddress) {
        dismissDialogs();
        /*UpdateAddAddressDialogFragment updateAddAddressDialogFragment = UpdateAddAddressDialogFragment.newInstance(orderPresenter, updateAddress, saveAddress, new UpdateAddAddressDialogFragment.OnSubmit() {
            @Override
            public void onSubmit(String phoneNumber, String label, String note) {
                if(orderPresenter.validateUpdateAddAddress(phoneNumber)) {
                    if (updateAddress != null) {
                        showProgressDialog(CacheUtil.getInstance().getTranslations(Text.LOADING));
                        updateAddress.getAttributes().setTelephone(phoneNumber);
                        updateAddress.getAttributes().setLabel(label);
                        updateAddress.getAttributes().setInstructions(note);
                        orderPresenter.updateAddress(false, updateAddress);
                    } else if (saveAddress != null) {
                        showProgressDialog(CacheUtil.getInstance().getTranslations(Text.LOADING));
                        orderPresenter.setAddress(saveAddress);
                        orderPresenter.addAddress(phoneNumber, label, note);
                    }
                }
            }
        });
        updateAddAddressDialogFragment.show(getSupportFragmentManager(), UpdateAddAddressDialogFragment.class.getSimpleName());*/
    }

    @Override
    public void showStores(ArrayList<Store> pickupStores, ArrayList<Store> curbsideStore) {

        storesRecyclerViewAdapter.notifyDataSetChanged();

        slidingPanel.setPanelState(CacheUtil.getInstance().getOrderType() != OrderType.DELIVERY && pickupStores.size() > 0 ? SlidingUpPanelLayout.PanelState.ANCHORED : SlidingUpPanelLayout.PanelState.HIDDEN);

        if(storeMarkers != null && storeMarkers.size() > 0) {
            for(Marker marker : storeMarkers) {
                marker.remove();
            }
        }

        storeMarkers = new ArrayList<>();

        for(int i = 0; i < pickupStores.size(); i++) {

            Store store = pickupStores.get(i);

            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = inflater.inflate(R.layout.marker_store_new, null);

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.title(String.valueOf(i));
            markerOptions.position(new LatLng(store.getAttributes().getLat(), store.getAttributes().getLng()));
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(ViewUtil.createDrawableFromView(this, view)));
            markerOptions.anchor(0.5f, 1f);

            Marker marker = map.addMarker(markerOptions);
            marker.setVisible(CacheUtil.getInstance().getOrderType() != OrderType.DELIVERY);

            storeMarkers.add(marker);

        }

        /*Concept concept = CacheUtil.getInstance().getConcept();

        if(!concept.getSupportedOrderTypes().contains("deliver") && stores.size() == 1) {
            slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            storesRecyclerViewAdapter.setSelected(0);
            storesContinue.setVisibility(View.VISIBLE);
            //listView.performClick();
        }*/

        dismissDialogs();

    }

    @Override
    public void setStore(Store store, boolean selected) {

    }

    @Override
    public void callStore(Store store) {

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + store.getAttributes().getTelephone().trim().replace(" ", "")));
            startActivity(intent);
        } else {
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, Values.PERMISSION_CALL);
        }

    }

    @Override
    public void selectStore(int position, Store store, boolean initialLoad) {

        storesRecyclerViewAdapter.setSelected(position);

        storesContinue.setVisibility(View.VISIBLE);

        showStore(false, position);

        //CacheUtil.getInstance().setOrderStore(store);

        //onBackPressed();

        /*for(Marker marker : storeMarkers) {
            if(marker.getTag() != null && marker.getTag().equals(store.getId())) {
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(ViewUtil.writeTextOnDrawable(this, R.drawable.marker_store_selected, String.valueOf(storeMarkers.indexOf(marker) + 1))));
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(store.getAttributes().getLat(), store.getAttributes().getLng()), 12f));
            } else {
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(ViewUtil.writeTextOnDrawable(this, R.drawable.marker_store, String.valueOf(storeMarkers.indexOf(marker) + 1))));
            }
        }*/

    }

    @Override
    public void showStore(boolean markerClicked, int position) {
        for(Marker storeMarker : storeMarkers) {

            if(storeMarker.getTitle() != null && storeMarker.getTitle().equals(String.valueOf(position))) {

                Store store = orderPresenter.getStore(true, Integer.valueOf(storeMarker.getTitle()));

                /*storeName.setText(store.getAttributes().getName());

                if(store.getAttributes().getOpen()) {
                    //storeOpenIcon.setVisibility(View.VISIBLE);
                    storeOpenNowIcon.setVisibility(View.VISIBLE);
                    storeOpenNow.setVisibility(View.VISIBLE);
                } else {
                    //storeOpenIcon.setVisibility(View.GONE);
                    storeOpenNowIcon.setVisibility(View.GONE);
                    storeOpenNow.setVisibility(View.GONE);
                }

                String telephone = store.getAttributes().getTelephone();

                if(telephone != null && !telephone.isEmpty()) {
                    storeTelephone.setText(telephone);
                    storeTelephoneIcon.setVisibility(View.VISIBLE);
                    storeTelephone.setVisibility(View.VISIBLE);
                } else {
                    storeTelephoneIcon.setVisibility(View.GONE);
                    storeTelephone.setVisibility(View.GONE);
                }

                String openingHours = StringUtil.getOpeningHours(store);

                if(!openingHours.isEmpty()) {
                    storeHours.setText(openingHours);
                    storeHoursIcon.setVisibility(View.VISIBLE);
                    storeHours.setVisibility(View.VISIBLE);
                } else {
                    storeHoursIcon.setVisibility(View.GONE);
                    storeHours.setVisibility(View.GONE);
                }

                storeTelephone.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        callStore(store);
                    }
                });

                storeShowFacility.setText(CacheUtil.getInstance().getTranslations(Text.SHOW_FACILITY));

                storeShowFacility.setVisibility(View.GONE);
                storeFacilities.setVisibility(View.GONE);

                if(store.getFacilities().size() > 0) {

                    storeFacilities.removeAllViews();

                    for(Facility facility : store.getFacilities()) {

                        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        View facilityView = inflater.inflate(R.layout.item_facility, null);

                        TextView facilityName = facilityView.findViewById(R.id.name);
                        ImageView facilityIcon = facilityView.findViewById(R.id.icon);

                        facilityName.setTypeface(FontHandler.getRegularTypeface());

                        facilityName.setText(facility.getAttributes().getName());

                        Glide.with(OrderTypeActivity.this).load(facility.getAttributes().getIcon()).apply(RequestOptions.centerCropTransform()).into(facilityIcon);

                        storeFacilities.addView(facilityView);

                    }

                    storeShowFacility.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(storeFacilities.getVisibility() == View.VISIBLE) {
                                storeShowFacility.setText(CacheUtil.getInstance().getTranslations(Text.SHOW_FACILITY));
                                storeFacilities.setVisibility(View.GONE);
                            } else {
                                storeShowFacility.setText(CacheUtil.getInstance().getTranslations(Text.HIDE_FACILITY));
                                storeFacilities.setVisibility(View.VISIBLE);
                            }
                        }
                    });

                    storeShowFacility.setVisibility(View.VISIBLE);

                }

                storeSelect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showProgressDialog(CacheUtil.getInstance().getTranslations(Text.LOADING));
                        orderPresenter.continueStore(-1, store);
                    }
                });*/

                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                View view = inflater.inflate(R.layout.marker_store_new, null);

                //ImageView infoWindow = view.findViewById(R.id.info_window);
                TextView distanceText = view.findViewById(R.id.distance);

                distanceText.setTypeface(FontHandler.getRegularTypeface());

                String distance = StringUtil.getDistance(store.getAttributes().getDistance());

                if(!distance.isEmpty()) {

                    //storeDistance.setText(distance);
                    //storeDistanceIcon.setVisibility(View.VISIBLE);
                    //storeDistance.setVisibility(View.VISIBLE);

                    distanceText.setText(distance);
                    //infoWindow.setVisibility(View.VISIBLE);
                    distanceText.setVisibility(View.VISIBLE);

                } else {
                    //storeDistanceIcon.setVisibility(View.GONE);
                    //storeDistance.setVisibility(View.GONE);
                    //infoWindow.setVisibility(View.INVISIBLE);
                    distanceText.setVisibility(View.INVISIBLE);
                }

                storeMarker.setIcon(BitmapDescriptorFactory.fromBitmap(ViewUtil.createDrawableFromView(OrderTypeActivity.this, view)));

                mapMoved = true;
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(storeMarker.getPosition(), 15f));

                /*mapListView.setVisibility(mapView.isSelected() ? View.GONE : View.VISIBLE);
                storeDetails.setVisibility(View.VISIBLE);

                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(mapContainer);
                constraintSet.connect(R.id.delivery_location, ConstraintSet.BOTTOM, R.id.store_details, ConstraintSet.TOP, ViewUtil.dpToPx(OrderTypeActivity.this, 16));
                constraintSet.applyTo(mapContainer);*/

            } else {

                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                View view = inflater.inflate(R.layout.marker_store_new, null);

                //ImageView infoWindow = view.findViewById(R.id.info_window);
                //infoWindow.setVisibility(View.INVISIBLE);

                storeMarker.setIcon(BitmapDescriptorFactory.fromBitmap(ViewUtil.createDrawableFromView(OrderTypeActivity.this, view)));

            }
        }
    }

    @Override
    public void showStore(boolean open, Store store, String error) {
        if(open) {

            HashMap<String, String> eventAttributes = new HashMap<>();

            TabLayout.Tab tab = tabLayout.getTabAt(tabLayout.getSelectedTabPosition());

            if(tab != null) {
                String tag = (String) tab.getTag();
                if(tag != null) {
                    if(tag.equalsIgnoreCase("pickup")) {
                        eventAttributes.put("Pickup", store.getAttributes().getName());
                        CacheUtil.getInstance().setOrderType(OrderType.PICKUP);
                    } else if(tag.equalsIgnoreCase("eat-in")) {
                        eventAttributes.put("DineIn", store.getAttributes().getName());
                        CacheUtil.getInstance().setOrderType(OrderType.DINE_IN);
                    }
                }
            }

            /*switch (tabLayout.getSelectedTabPosition()) {
                case 0:
                    eventAttributes.put("Pickup", store.getAttributes().getName());
                    CacheUtil.getInstance().setOrderType(OrderType.PICKUP);
                    break;
                case 1:
                    eventAttributes.put("DineIn", store.getAttributes().getName());
                    CacheUtil.getInstance().setOrderType(OrderType.DINE_IN);
                    break;
            }*/

            logEvent("OrderTypeSelection", eventAttributes, null, 0);
            logEvent("OrderTypeSelection", eventAttributes, null, 0);
            CacheUtil.getInstance().setOrderStore(store);

            /*if(!splash) {
                onBackPressed();
            } else {*/
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("menu", menu);
                intent.putExtra("cart", cart);
                //if(!concepts) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                /*} else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                }*/
                startActivity(intent);
                //finish();
            //}

        } else {
            showAlertDialog(store.getAttributes().getName(), error);
        }
    }

    @Override
    public void showOrderType(OrderType orderType) {
        switch (orderType) {
            case NONE:
            case DELIVERY:
                for(int i = 0; i < tabLayout.getTabCount(); i++) {
                    TabLayout.Tab tab = tabLayout.getTabAt(i);
                    if(tab != null) {
                        String tag = (String) tab.getTag();

                        if (tag != null && tag.equalsIgnoreCase("deliver")) {
                            tabLayout.selectTab(tabLayout.getTabAt(i));
                            break;
                        }
                    }
                }
                //tabLayout.selectTab(tabLayout.getTabAt(2));
                break;
            case PICKUP:
                for(int i = 0; i < tabLayout.getTabCount(); i++) {
                    TabLayout.Tab tab = tabLayout.getTabAt(i);
                    if(tab != null) {
                        String tag = (String) tab.getTag();

                        if (tag != null && tag.equalsIgnoreCase("pickup")) {
                            tabLayout.selectTab(tabLayout.getTabAt(i));
                            break;
                        }
                    }
                }
                //tabLayout.selectTab(tabLayout.getTabAt(0));
                break;
            case DINE_IN:
                for(int i = 0; i < tabLayout.getTabCount(); i++) {
                    TabLayout.Tab tab = tabLayout.getTabAt(i);
                    if(tab != null) {
                        String tag = (String) tab.getTag();

                        if (tag != null && tag.equalsIgnoreCase("eat-in")) {
                            tabLayout.selectTab(tabLayout.getTabAt(i));
                            break;
                        }
                    }
                }
                //tabLayout.selectTab(tabLayout.getTabAt(1));
                break;
        }
    }

    /*@Override
    public void showSaveAddress(Address address) {
        dismissDialogs();
        SaveAddressDialogFragment saveAddressDialogFragment = SaveAddressDialogFragment.newInstance(new SaveAddressDialogFragment.OnSave() {
            @Override
            public void onSave(Dialog dialog, String phoneNumber, String note) {
                dialog.dismiss();
                if(AuthUtil.getInstance().isSignedIn()) {

                    showProgressDialog(CacheUtil.getInstance().getTranslations(Text.SAVING_ADDRESS));
                    orderPresenter.saveAddress(phoneNumber, note);

                } else {

                    dismissDialogs();

                    showAlertDialog("", CacheUtil.getInstance().getTranslations(Text.YOU_ARE_NOT_SIGNED_IN), CacheUtil.getInstance().getTranslations(Text.SIGN_IN), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //addressesContainer.setVisibility(View.GONE);
                            Intent intent = new Intent(OrderTypeActivity.this, SignInActivity.class);
                            intent.putExtra("order_type", true);
                            startActivity(intent);
                        }
                    }, CacheUtil.getInstance().getTranslations(Text.CANCEL), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                }
            }
        }, address);
        saveAddressDialogFragment.show(getSupportFragmentManager(), SaveAddressDialogFragment.class.getSimpleName());
    }*/

    @Override
    public void selectDelivery() {

        CacheUtil.getInstance().setOrderType(OrderType.DELIVERY);

        for(int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if(tab != null) {
                String tag = (String) tab.getTag();
                if(tag != null && tag.equalsIgnoreCase("deliver")) {
                    tab.getIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                } else {
                    tab.getIcon().setColorFilter(Color.parseColor("#CCCCCC"), PorterDuff.Mode.SRC_IN);
                }
            }
        }

        marker.setVisibility(View.VISIBLE);

        slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

        //mapListView.setVisibility(View.GONE);
        //storeDetails.setVisibility(View.GONE);
        storesRecyclerView.setVisibility(View.GONE);
        storesContinue.setVisibility(View.GONE);

        deliveryLocation.setVisibility(View.VISIBLE);

        for(Marker marker : storeMarkers) {
            marker.setVisible(false);
        }

        /*if(locationMarker != null) {

            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = inflater.inflate(R.layout.marker_location, null);

            TextView locationLabel = view.findViewById(R.id.location_label);
            TextView locationAddress = view.findViewById(R.id.location_address);

            locationLabel.setTypeface(FontHandler.getRegularTypeface());
            locationAddress.setTypeface(FontHandler.getSemiboldTypeface());

            locationLabel.setText(CacheUtil.getInstance().getTranslations(Text.LOCATION));
            locationAddress.setText(CacheUtil.getInstance().getTranslations(Text.LOCATION_ADDRESS_HERE));

            Geocoder geocoder = new Geocoder(this, LocaleUtil.getInstance().isEnglish() ? new Locale("en", "us") : new Locale("ar", "sa"));
            String currentLocation = orderPresenter.getAddress(geocoder, locationMarker.getPosition());

            locationAddress.setText(currentLocation);

            search.setText(currentLocation);

            locationMarker.setIcon(BitmapDescriptorFactory.fromBitmap(ViewUtil.createDrawableFromView(this, view)));
            locationMarker.setAnchor(0.5f, 0.5f);

            CameraUpdateFactory.newLatLng(locationMarker.getPosition());

        }*/

        searchContainer.setVisibility(View.VISIBLE);
        confirmLocation.setVisibility(View.VISIBLE);

        if(AuthUtil.getInstance().isSignedIn() && orderPresenter.getAddressesCount() > 0) {
            searchContainer.setVisibility(View.GONE);
            confirmLocation.setVisibility(View.GONE);
            marker.setVisibility(View.GONE);
            deliveryLocation.setVisibility(View.GONE);
            addressesNestedScrollView.setVisibility(View.VISIBLE);
            if(currentLocationContainer.getStrokeWidth() == ViewUtil.dpToPx(OrderTypeActivity.this, 2) && homeAddressesAdapter.getSelected() != -1 || workAddressesAdapter.getSelected() != -1 || otherAddressesAdapter.getSelected() != -1) {
                addressesContinue.setVisibility(View.VISIBLE);
            } else {
                addressesContinue.setVisibility(View.GONE);
            }

        }

    }

    @Override
    public void selectPickup() {

        CacheUtil.getInstance().setOrderType(OrderType.PICKUP);

        for(int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if(tab != null) {
                String tag = (String) tab.getTag();
                if(tag != null && tag.equalsIgnoreCase("pickup")) {
                    tab.getIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                } else {
                    tab.getIcon().setColorFilter(Color.parseColor("#CCCCCC"), PorterDuff.Mode.SRC_IN);
                }
            }
        }

        marker.setVisibility(View.GONE);
        addressesNestedScrollView.setVisibility(View.GONE);
        addressesContinue.setVisibility(View.GONE);

        slidingPanel.setPanelState(orderPresenter.getStoresCount(true) > 0 ? SlidingUpPanelLayout.PanelState.ANCHORED : SlidingUpPanelLayout.PanelState.HIDDEN);

        //mapListView.setVisibility(View.VISIBLE);
        storesRecyclerView.setVisibility(View.VISIBLE);
        if(storesRecyclerViewAdapter.getSelected() != -1) {
            storesContinue.setVisibility(View.VISIBLE);
        }

        deliveryLocation.setVisibility(View.VISIBLE);

        for(Marker marker : storeMarkers) {
            marker.setVisible(true);
        }

        /*if(locationMarker != null) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.marker_location);
            locationMarker.setIcon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), 95, 95, true)));
            locationMarker.setAnchor(0.5f, 0.5f);
        }*/

        searchContainer.setVisibility(View.INVISIBLE);
        confirmLocation.setVisibility(View.INVISIBLE);

    }

    @Override
    public void selectDineIn() {

        CacheUtil.getInstance().setOrderType(OrderType.DINE_IN);

        for(int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if(tab != null) {
                String tag = (String) tab.getTag();
                if(tag != null && tag.equalsIgnoreCase("eat-in")) {
                    tab.getIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                } else {
                    tab.getIcon().setColorFilter(Color.parseColor("#CCCCCC"), PorterDuff.Mode.SRC_IN);
                }
            }
        }

        marker.setVisibility(View.GONE);
        addressesNestedScrollView.setVisibility(View.GONE);
        addressesContinue.setVisibility(View.GONE);

        slidingPanel.setPanelState(orderPresenter.getStoresCount(true) > 0 ? SlidingUpPanelLayout.PanelState.ANCHORED : SlidingUpPanelLayout.PanelState.HIDDEN);

        //mapListView.setVisibility(View.VISIBLE);
        storesRecyclerView.setVisibility(View.VISIBLE);
        if(storesRecyclerViewAdapter.getSelected() != -1) {
            storesContinue.setVisibility(View.VISIBLE);
        }

        deliveryLocation.setVisibility(View.VISIBLE);

        for(Marker marker : storeMarkers) {
            marker.setVisible(true);
        }

        /*if(locationMarker != null) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.marker_location);
            locationMarker.setIcon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), 95, 95, true)));
            locationMarker.setAnchor(0.5f, 0.5f);
        }*/

        searchContainer.setVisibility(View.INVISIBLE);
        confirmLocation.setVisibility(View.INVISIBLE);

    }

    @Override
    public void selectCurbside() {

    }

    @Override
    public void saveAddress(String address) {

        dismissDialogs();

        HashMap<String, String> eventAttributes = new HashMap<>();
        eventAttributes.put("Delivery", address);
        logEvent("OrderTypeSelection", eventAttributes, null, 0);

        /*if(!splash) {
            onBackPressed();
        } else {*/
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("menu", menu);
            intent.putExtra("cart", cart);
            //if(!concepts) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            /*} else {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }*/
            startActivity(intent);
            //finish();
        //}

    }

    @Override
    public void saveAddress(Address address) {

    }

    @Override
    public void updateAddress(String address) {

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

    }

    @Override
    public void showError(String error) {
        dismissDialogs();
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

    }

    @Override
    public void setCloseTime(String time) {

    }

}
