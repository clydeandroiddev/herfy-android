package com.herfy.checkout_temp;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.maps.android.SphericalUtil;
import com.herfy.BuildConfig;
import com.herfy.R;
import com.herfy.analytics.FacebookAppEventUtil;
import com.herfy.analytics.FirebaseECommerceEventUtil;
import com.herfy.base.BaseActivity;
//import com.herfy.constants.Text;
//import com.herfy.constants.Values;
import com.herfy.checkout.databinding.ActivityCheckoutPageBinding;
import com.herfy.decorator.BeforeDateDecorator;
import com.herfy.decorator.OneDayDecorator;
import com.herfy.firebase.FirebaseService;
import com.herfy.history.adapters.OrderServiceChargeRecyclerViewAdapter;
import com.herfy.main.MainActivity;
import com.herfy.order.views.OrderTypeActivity;
import com.herfy.payment.PaymentContract;
import com.herfy.payment.PaymentPresenter;
import com.herfy.payment.savedcards.CVVDialogAlert;
import com.herfy.payment.savedcards.SaveDialogAlert;
import com.herfy.payment.savedcards.SavedCardsDialog;
import com.herfy.payment.views.CustomCheckoutDialog;
import com.herfy.payment.views.MoyasarActivity;
import com.herfy.payment.views.OrderDialogFragment;
import com.herfy.ratings.AppRatingBroadcastReceiver;
import com.herfy.util.ApiLogLevel;
import com.herfy.util.ApiLogUtil;
import com.herfy.util.AppLogger;
import com.herfy.util.CurrencyUtils;
import com.herfy.util.ShipBookLogger;
import com.herfy.util.StringUtil;
import com.herfy.util.ViewUtil;
import com.herfy.utils.constants.Text;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.BuildUtil;
import com.herfy.utils.helper.CacheUtil;
import com.moneyhash.sdk.android.ResultType;
import com.moneyhash.sdk.android.payment.PaymentResultContract;
import com.moneyhash.sdk.android.payment.PaymentStatus;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.OrdersApiCall;
import com.skylinedynamics.solosdk.api.models.objects.Concept;
import com.skylinedynamics.solosdk.api.models.objects.Customer;
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard;
import com.skylinedynamics.solosdk.api.models.objects.Loyalty;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.PaymentType;
import com.skylinedynamics.solosdk.api.models.objects.PrayerTime;
import com.skylinedynamics.solosdk.api.models.objects.ServiceCharge;
import com.skylinedynamics.solosdk.api.models.objects.Store;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import company.tap.gosellapi.internal.api.callbacks.GoSellError;
import company.tap.gosellapi.internal.api.models.Authorize;
import company.tap.gosellapi.internal.api.models.Charge;
import company.tap.gosellapi.internal.api.models.Token;
import company.tap.gosellapi.open.controllers.SDKSession;
import company.tap.gosellapi.open.delegate.SessionDelegate;
import company.tap.gosellapi.open.models.CardsList;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import kotlin.Pair;

public class CheckoutActivity extends BaseActivity implements PaymentContract.View, SessionDelegate, OnDateSelectedListener {

    private ActivityCheckoutPageBinding binding;
    FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap mMap;
    private Location currentLocation;

    boolean isBranchImageAvailable = false;
    boolean mukafaaSelected = false;
    CustomCheckoutDialog dialogCheckout;
    private PaymentContract.Presenter paymentPresenter;
    private OneDayDecorator oneDayDecorator;
    private BeforeDateDecorator beforeDateDecorator;
    private Date maxDate;
    private Date minDate;
    private boolean isViewLoaded;
    private Date selectedDate;
    private String daySelected;
    private String goodDaySelected;
    private Date goodSelectedDate;
    private String goodSelectedDateString;
    private boolean isRemove = true;
    private SDKSession tapSdkSession;
    private boolean isScheduled;
    private String orderId;
    private OrderType orderType;
    private final int OTP_REQUEST = 100;
    private int loyaltypointsInput = 0;
    private SaveDialogAlert saveDialogAlert;
    private SavedCardsDialog savedCardsDialog;
    private CVVDialogAlert cvvDialogAlert;
    private double passedTotalFromCart;
    private double totalValue = 0.0;

    private boolean isPaymentSet = false;
    private boolean isCashPaymentAvailable = false;
    private boolean isCardPaymentAvailable = false;
    private boolean isCODPaymentAvailable = false;
    private OrderServiceChargeRecyclerViewAdapter orderServiceChargeRecyclerViewAdapter;

    FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

    private CompositeDisposable disposable = new CompositeDisposable();
    private BehaviorSubject<String> _textInput = BehaviorSubject.create();
    private Flowable<String> textInput = _textInput.toFlowable(BackpressureStrategy.LATEST);
    private TextWatcher pointsInSarTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            _textInput.onNext(editable.toString());
        }
    };

    boolean isFromCheckout = false;

    private ActivityResultLauncher moyasarActivityResultLauncher;

    private boolean isShowCardFailed = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivityCheckoutPageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        popupUpsellBackStack();
        passedTotalFromCart = getIntent().getDoubleExtra("totalPrice", 0.0);
        paymentPresenter = new PaymentPresenter(this);
        initData();
        paymentPresenter.getLoyalty();
        paymentPresenter.start();
        paymentPresenter.getPaymentTypes();
        moyasarActivityResultLauncher = registerForActivityResult( new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode() == RESULT_OK){
                            Intent intent = result.getData();
                            if(intent != null){
                                if(intent.hasExtra(MoyasarActivity.EXTRA_MOYASAR_ID)){
                                    if(!intent.getStringExtra(MoyasarActivity.EXTRA_MOYASAR_ID).equalsIgnoreCase("failed") && !intent.getStringExtra(MoyasarActivity.EXTRA_MOYASAR_ID).equalsIgnoreCase("canceled")){
                                        paymentPresenter.sendUpsells();
                                    }else {
                                        String errorMessage = intent.getStringExtra(MoyasarActivity.EXTRA_MOYASAR_STATUS);
                                        String orderId = intent.getStringExtra(MoyasarActivity.EXTRA_ORDER_ID);
                                        if(orderId != null && errorMessage != null && !errorMessage.equals("cancelled")){
                                            ShipBookLogger.logError(BuildConfig.TAG + " - Order ID: " + orderId + " -> " + paymentPresenter.getPaymentProvider() + " Card Payment Failed (Reason: Moyasar " + errorMessage + ")");
                                            ApiLogUtil.sendOrderLog(ApiLogLevel.ERROR, orderId, paymentPresenter.getPaymentProvider(), "Card Payment Failed (Reason: Moyasar " + errorMessage + ")");
                                        }
                                        isShowCardFailed = true;
                                    }
                                }
                            }
                        }else {
                            isShowCardFailed = true;
                        }
                    }
                });

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void popupUpsellBackStack() {
        if (getIntent().getBooleanExtra("fromUpsellPage", false)) {
            try {
                if (MainActivity.Companion.getCurrentMainActivity() != null) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> MainActivity.Companion.getCurrentMainActivity().popUpsell(), 300);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BuildUtil.isEkfc()) {
            showLoadingDialog();
        }

        if(isFromCheckout){
            isFromCheckout = false;
            return;
        }

        paymentPresenter.validateOrder(false, CacheUtil.getInstance().getOrderStore(), null, 0,isShowCardFailed);
        paymentPresenter.fetchCustomerCards();
        showSelectedPyament();
        isShowCardFailed = false;
    }

    private void initData() {
        isScheduled = getIntent().getBooleanExtra("isScheduled", false);
        if (isScheduled) {
            goodSelectedDateString = getIntent().getStringExtra("goodSelectedDate");
            goodDaySelected = getIntent().getStringExtra("goodDaySelected");
        }
    }

    @Override
    public void setPresenter(PaymentContract.Presenter presenter) {
        paymentPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, @Nullable String eventMetricName, double eventMetricValue) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
    }

    private void showMap() {
        double storeLat = CacheUtil.getInstance().getOrderStore().getAttributes().getLat();
        double storeLon = CacheUtil.getInstance().getOrderStore().getAttributes().getLng();
        LatLng storeLocation = new LatLng(storeLat,storeLon);
        LayoutInflater inflater = (LayoutInflater )getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        MarkerOptions markerOptions = new MarkerOptions();
        if (orderType == OrderType.DELIVERY) {
            double lat = Double.parseDouble(CacheUtil.getInstance().getOrderAddress().getAttributes().getLat());
            double lon = Double.parseDouble(CacheUtil.getInstance().getOrderAddress().getAttributes().getLongt());
            LatLng addressLocation = new LatLng(lat,lon);

            BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(
                    this, R.drawable.ic_address_location);
            if (bitmapDrawable != null) {
                Bitmap bitmap = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), 110, 110, true);
                Canvas canvas = new Canvas(bitmap);
                Paint paint = new Paint();
                ColorFilter filter = new PorterDuffColorFilter(
                        Color.parseColor(CacheUtil.getInstance().getColorMain()),
                        PorterDuff.Mode.SRC_IN
                );
                paint.setColorFilter(filter);
                canvas.drawBitmap(bitmap, 0f, 0f, paint);
                markerOptions.position(addressLocation).icon(BitmapDescriptorFactory.fromBitmap(bitmap));
            }
        } else {
            markerOptions.position(storeLocation);
            View view = inflater.inflate(R.layout.marker_store_new, null);
            BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(ViewUtil.createDrawableFromView(
                    this, view));
            markerOptions.icon(icon);
            markerOptions.anchor(0.5f, 1f);
        }





//        MarkerOptions markerOptions = new MarkerOptions();
//        markerOptions.position(storeLocation);


        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.checkout_map);
        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                mMap = googleMap;
                mMap.getUiSettings().setZoomControlsEnabled(false);
                mMap.getUiSettings().setCompassEnabled(false);
                mMap.getUiSettings().setMapToolbarEnabled(false);
                mMap.setMyLocationEnabled(true);

                if (orderType == OrderType.DELIVERY) {
                    mMap.addMarker(markerOptions);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerOptions.getPosition(),13f));
                } else {
                    mMap.addMarker(markerOptions);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerOptions.getPosition(), 13f));
                }
            }
        });

        if (orderType != OrderType.DELIVERY) {
            Task<Location> task = fusedLocationProviderClient.getLastLocation();
            task.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.addPolyline((new PolylineOptions()).add(storeLocation, myLocation).width(10f).color(Color.parseColor("#0064AC")));
                        showCurveLine(storeLocation, myLocation, 0.5f);
                    }
                }
            });
        }
    }

    private void showCurveLine(LatLng storeLocation, LatLng currentLocation, double k) {
        double distanceBetween = SphericalUtil.computeDistanceBetween(storeLocation, currentLocation);
        double heading = SphericalUtil.computeDistanceBetween(storeLocation, currentLocation);

        //midpoint position
        LatLng p = SphericalUtil.computeOffset(storeLocation, distanceBetween *0.5, heading);

        double x = (1 - k * k) * distanceBetween * 0.5 / (2 * k);
        double r = (1 + k * k) * distanceBetween * 0.5 / (2 * k);

        LatLng c = SphericalUtil.computeOffset(p, x, heading + 90.0);

        PolygonOptions polygon = new PolygonOptions();

        double h1 = SphericalUtil.computeHeading(c, storeLocation);
        double h2 = SphericalUtil.computeHeading(c, currentLocation);

        int numpoints = 200;
        double step = (h2 - h1) / numpoints;

        ArrayList<LatLng> temp = new ArrayList<LatLng>();

        for (int i=0; i < numpoints; i++) {
            LatLng latLng = SphericalUtil.computeOffset(c, r, h1 + i * step);
            polygon.add(latLng);
            temp.add(latLng);

        }

        for (int i = temp.size() - 1; i >= 1; i--) {
            polygon.add(temp.get(i));
        }

//        for (int i=0; i < numpoints; i++) {
//            LatLng pi = SphericalUtil.computeOffset(c, r, h1 + i * step);
//            options.add(pi);
//        }

//        mMap.addPolyline(options.width(10).color(Color.BLACK).geodesic(false).pattern(pattern));
//        mMap.addPolyline(options.width(10).color(Color.BLACK).geodesic(false));
        mMap.addPolygon(polygon);
        temp.clear();
    }

    @Override
    public void setupViews() {
        orderType = CacheUtil.getInstance().getOrderType();
        showMap();
//        rvOrderSummary.setLayoutManager(new LinearLayoutManager(this));
//        menuItemsRecyclerViewAdapter = new OrderSummaryItemRecyclerViewAdapter(paymentPresenter);
//        rvOrderSummary.setAdapter(menuItemsRecyclerViewAdapter);


//        tvChangeOrderType.setOnClickListener( new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showOrderType(true);
//            }
//        });

       /* ivShowOrders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rvOrderSummary.getVisibility() == View.VISIBLE) {
                    rvOrderSummary.setVisibility(View.GONE);
                } else {
                    rvOrderSummary.setVisibility(View.VISIBLE);
                }
            }
        });*/

        binding.layoutPayment.tvEditPaymentMethod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPaymentMethod();
            }
        });

        Store orderStore = CacheUtil.getInstance().getOrderStore();
        if (orderStore != null && orderStore.getAttributes() != null) {
            String branchImage = orderStore.getAttributes().getImage();
            if (branchImage != null) {
                isBranchImageAvailable = !branchImage.isEmpty();
            }
        }
        /*if (isScheduled) {
            schedLayout.setVisibility(View.VISIBLE);
            scheduleText.setText(TimeUtil.properSchedDate(goodDaySelected + " " + goodSelectedDateString));
        } else {
            schedLayout.setVisibility(View.GONE);
        }*/

     /*   String tableDineInNumber = CacheUtil.getInstance().getDineInTableNumber();
        if (tableDineInNumber != null && !tableDineInNumber.isEmpty()) {
            tableNumberLayout.setVisibility(View.VISIBLE);
            tableNumber.setText(tableDineInNumber);
        }*/

//        slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

        binding.tvMobileNumber.setText(AuthUtil.getInstance().getCustomer().getAttributes().getMobile());

        binding.toolbarLayout.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        if (CacheUtil.getInstance().getOrderStore() != null) {
            paymentPresenter.validateOrder(false, CacheUtil.getInstance().getOrderStore(), null, 0, false);
        }


        if (orderType == OrderType.DELIVERY) {
            /*showLoadingDialog();
            Geocoder geocoder = new Geocoder(PaymentActivity.this, LocaleUtil.getInstance().isEnglish() ? new Locale("en", "us") : new Locale("ar", "sa"));
            paymentPresenter.getStore(geocoder);*/

            if (CacheUtil.getInstance().getOrderAddress() != null) {
                binding.tvBranchName.setText(CacheUtil.getInstance().getOrderAddress().getAttributes().getLine1());
            }
            binding.grpDeliveryNote.setVisibility(View.VISIBLE);
            binding.tvDeliveryInstruction.setText(CacheUtil.getInstance().getOrderAddress().getAttributes().getInstructions());
        } else {
            /*Geocoder geocoder = new Geocoder(PaymentActivity.this, LocaleUtil.getInstance().isEnglish() ? new Locale("en", "us") : new Locale("ar", "sa"));
            paymentPresenter.getAddresses(geocoder);*/
            if (CacheUtil.getInstance().getOrderStore() != null) {
                binding.tvBranchName.setText(CacheUtil.getInstance().getOrderStore().getAttributes().getName());
            }
            binding.grpDeliveryNote.setVisibility(View.GONE);
        }

        switch (CacheUtil.getInstance().getOrderType()) {
            case PICKUP:
                binding.tvOrderType.setText(CacheUtil.getInstance().getTranslations(Text.PICKUP));
                binding.ivOrderType.setImageResource(com.herfy.checkout.R.drawable.ic_herfy_pickup);
                break;
            case DINE_IN:
                binding.tvOrderType.setText(CacheUtil.getInstance().getTranslations(Text.DINE_IN));
                binding.ivOrderType.setImageResource(com.herfy.checkout.R.drawable.ic_dine_in);
                break;
            case DELIVERY:
                binding.tvOrderType.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERY_ADDRESS, "Delivery Address"));
                binding.ivOrderType.setImageResource(com.herfy.checkout.R.drawable.ic_herfy_delivery);
                break;
            case CURBSIDE:
                binding.tvOrderType.setText(CacheUtil.getInstance().getTranslations(Text.CURBSIDE_PICKUP, "Curbside"));
                binding.ivOrderType.setImageResource(com.herfy.checkout.R.drawable.ic_pickup_curbside);
                break;
        }

//        removeSched.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                isRemove = true;
//                schedTime.setText("");
//                if (CacheUtil.getInstance().getTranslations(Text.PLACE_ORDER_NOW, "PLACE ORDER").equalsIgnoreCase("CHOOSE METHOD")) {
//                    placeOrder.setText("PLACE ORDER");
//                } else {
//                    placeOrder.setText(CacheUtil.getInstance().getTranslations(Text.PLACE_ORDER_NOW, "PLACE ORDER NOW"));
//                }
//                removeLayout.setVisibility(View.GONE);
//                removeLabel.setVisibility(View.GONE);
//                noSchedLayout.setVisibility(View.VISIBLE);
//                spacingTop.setVisibility(View.GONE);
//                withSchedLayout.setVisibility(View.GONE);
//            }
//        });
//
//        cancelButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
//            }
//        });

//        confirmButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
//                try {
//                    Calendar originalCalendar = Calendar.getInstance();
//                    Calendar calendar = Calendar.getInstance();
//                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.ENGLISH);
//                    calendar.setTime(sdf.parse(daySelected + " " + TimeUtil.dateToHourOnly(selectedDate)));
//                    paymentPresenter.scheduleOrder(originalCalendar, calendar, true);
//                } catch (ParseException pe) {
//                    pe.printStackTrace();
//                    int minSchedule = CacheUtil.getInstance().getConcept().getDefaultScheduleDeliveryTime();
//                    if (minSchedule == 0) {
//                        minSchedule = 1;
//                    }
//                    showMessage(CacheUtil.getInstance().getTranslations(Text.MINIMUM_SCHEDULED_ORDER, "Unable to process order, scheduled order is allowed minimum of (x) min from current time.").replace("(x)", "" + minSchedule));
//                    dismissDialogs();
//                }
//            }
//        });

        binding.placeOrder.setOnClickListener(view -> {
            boolean canProceedViaFullPoints = paymentPresenter.canProceedViaFullPoints();
            if (canProceedViaFullPoints || CacheUtil.getInstance().getOrderPayment() != PaymentType.NONE) {
                if (!isScheduled) {
                    showLoadingDialog();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
                    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
                    paymentPresenter.getPrayerTime();
                    paymentPresenter.getStoreOpen(false, simpleDateFormat.format(new Date()));

//                    String branch;
//                    LatLng latLng;
//                    String imageUrl;
//                    int promisedTime;
//                    if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
//                        promisedTime = CacheUtil.getInstance().getOrderStore().getAttributes().getPromisedTimeDelta().getDelivery();
//                        branch = (CacheUtil.getInstance().getOrderAddress().getAttributes().getLine1());
//                        latLng = new LatLng(Double.parseDouble(CacheUtil.getInstance().getOrderAddress().getAttributes().getLat()), Double.parseDouble(CacheUtil.getInstance().getOrderAddress().getAttributes().getLongt()));
//                        imageUrl = null;
//                    } else {
//                        promisedTime = CacheUtil.getInstance().getOrderStore().getAttributes().getPromisedTimeDelta().getPickup();
//                        branch = (CacheUtil.getInstance().getOrderStore().getAttributes().getName());
//                        latLng = new LatLng(CacheUtil.getInstance().getOrderStore().getAttributes().getLat(), CacheUtil.getInstance().getOrderStore().getAttributes().getLng());
//                        imageUrl = CacheUtil.getInstance().getOrderStore().getAttributes().getImage();
//                    }
//                    paymentOrderTimeDialog = PaymentOrderTimeDialog.newInstance(CacheUtil.getInstance().getOrderType(), promisedTime, branch, imageUrl, latLng, new PaymentOrderTimeDialog.OnAction() {
//                        @Override
//                        public void close() {
//                            paymentOrderTimeDialog.dismiss();
//                        }
//
//                        @Override
//                        public void confirm() {

//                        }
//                    });
//                    paymentOrderTimeDialog.setCancelable(false);
//                    paymentOrderTimeDialog.show(getSupportFragmentManager(), CarMakerDialog.class.toString());
                } else {
                    try {
                        showLoadingDialog();
                        Calendar originalCalendar = Calendar.getInstance();
                        Calendar calendar = Calendar.getInstance();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.ENGLISH);
                        calendar.setTime(sdf.parse(goodDaySelected + " " + goodSelectedDateString));
                        paymentPresenter.getPrayerTime();
                        paymentPresenter.scheduleOrder(originalCalendar, calendar, false);
                    } catch (ParseException pe) {
                        pe.printStackTrace();
                        int minSchedule = CacheUtil.getInstance().getConcept().getDefaultScheduleDeliveryTime();
                        if (minSchedule == 0) {
                            minSchedule = 1;
                        }
                        showMessage(CacheUtil.getInstance().getTranslations(Text.MINIMUM_SCHEDULED_ORDER, "Unable to process order, scheduled order is allowed minimum of (x) min from current time.").replace("(x)", "" + minSchedule));
                        dismissDialogs();
                    }
                }
                firebaseAddPaymentInfoEvent();

            } else {
                Toast.makeText(CheckoutActivity.this, CacheUtil.getInstance().getTranslations(Text.SELECT_PAYMENT_METHOD), Toast.LENGTH_SHORT).show();
            }
        });

        binding.layoutLoyalty.loyaltySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    binding.layoutLoyalty.layoutLoyaltyPoints.setVisibility(View.VISIBLE);
                } else {
                    binding.layoutLoyalty.layoutLoyaltyPoints.setVisibility(View.GONE);
                    paymentPresenter.setLoyaltyPointsUsed(0);
                    binding.layoutLoyalty.etPoints.setText("");
                    loyaltypointsInput = 0;
                    paymentPresenter.validateOrder(false, CacheUtil.getInstance().getOrderStore(), null, 0,false);
                }
            }
        });
        /*loyaltyPointsPayment.setOnClickListener(view -> {
            if (!loyaltyPointsPayment.isSelected()) {
                loyaltyPointsPayment.setSelected(true);
                loyaltyPointsPaymentContainer.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
                pointsLayout.setVisibility(View.VISIBLE);
            } else {
                loyaltyPointsPayment.setSelected(false);
                loyaltyPointsPaymentContainer.setStrokeColor(Color.TRANSPARENT);
                pointsLayout.setVisibility(View.GONE);
                paymentPresenter.setLoyaltyPointsUsed(0);
                etLoyaltyPoints.setText("");
                loyaltypointsInput = 0;
                paymentPresenter.validateOrder(false, CacheUtil.getInstance().getOrderStore(), null, 0,false);
            }
        });

        pointsLayout.setOnClickListener(v -> {
        });*/

        Loyalty loyalty = AuthUtil.getInstance().getCustomerLoyalty();
        if (loyalty.getAttributes() != null) {
            firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String loyaltySettings = firebaseRemoteConfig.getString("loyalty_settings");

                    if (loyaltySettings != null && loyaltySettings.equals("full")) {
                        if (loyalty.getAttributes().getCurrentBalanceAmount() >= paymentPresenter.getMenuItemsTotalPrice()) {
                            binding.layoutLoyalty.loyaltySwitch.setEnabled(true);
                            binding.layoutLoyalty.loyaltyPointsPaymentContainer.setEnabled(true);
                        } else {
                            binding.layoutLoyalty.loyaltySwitch.setEnabled(false);
                            binding.layoutLoyalty.loyaltyPointsPaymentContainer.setEnabled(false);
                            binding.layoutLoyalty.loyaltyPointsPaymentContainer.setCardBackgroundColor(Color.parseColor("#d5d5d5"));
                        }
                    } else {
                        binding.layoutLoyalty.loyaltySwitch.setEnabled(true);
                        binding.layoutLoyalty.loyaltyPointsPaymentContainer.setEnabled(true);
                    }
                }
            });

            if ((int) loyalty.getAttributes().getCurrentBalanceAmount() == loyalty.getAttributes().getCurrentBalanceAmount()) {
                binding.layoutLoyalty.tvLoyaltyPoints.setText(CacheUtil.getInstance().getTranslations(Text.LOYALTY_POINTS_PAYMENT_LABEL, "(x) Points ((y))").replace("(x)", String.valueOf(AuthUtil.getInstance().getCustomerLoyalty().getAttributes().getCurrentBalance())).replace("(y)", CurrencyUtils.Companion.updateCurrency(loyalty.getAttributes().getCurrentBalanceAmount(), true)));
                //loyaltyLabel.setText(StringUtil.localize(String.valueOf(AuthUtil.getInstance().getCustomerLoyalty().getAttributes().getCurrentBalance()))+" Points ("+StringUtil.localize(String.valueOf(loyalty.getAttributes().getCurrentBalanceAmount())) +")");
            } else {
                binding.layoutLoyalty.tvLoyaltyPoints.setText(CacheUtil.getInstance().getTranslations(Text.LOYALTY_POINTS_PAYMENT_LABEL, "(x) Points ((y))").replace("(x)", StringUtil.getFormattedAmount(AuthUtil.getInstance().getCustomerLoyalty().getAttributes().getCurrentBalance())).replace("(y)", CurrencyUtils.Companion.updateCurrency(loyalty.getAttributes().getCurrentBalanceAmount(), true)));
                //loyaltyLabel.setText(StringUtil.getFormattedAmount(AuthUtil.getInstance().getCustomerLoyalty().getAttributes().getCurrentBalance())+" Points ("+StringUtil.getFormattedAmount(loyalty.getAttributes().getCurrentBalanceAmount()) +")");
            }
        }

        disposable.add(textInput.debounce(300, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.io()).subscribe(pointsText -> {
            runOnUiThread(() -> {
                if (pointsText.length() > 0) {
                    double userSetPoints = Double.parseDouble(StringUtil.toEnglish(pointsText));
                    double currentBalance = AuthUtil.getInstance().getCustomerLoyalty().getAttributes().getCurrentBalanceAmount();
                    double pointConversion = Double.parseDouble(AuthUtil.getInstance().getCustomerLoyalty().getAttributes().getPointConversion());
                    double points = loyalty.getAttributes().getCurrentBalanceAmount() > paymentPresenter.getMenuItemsTotalPrice() ? paymentPresenter.getMenuItemsTotalPrice() * pointConversion : loyalty.getAttributes().getCurrentBalance();

                    binding.layoutLoyalty.etPoints.removeTextChangedListener(pointsInSarTextWatcher);
                    if (userSetPoints >= currentBalance) {
//                        btnUseAll.performClick();
                    } else {
                        binding.layoutLoyalty.etPoints.setText(StringUtil.localize(binding.layoutLoyalty.etPoints.getText().toString()));
                    }


                    binding.layoutLoyalty.etPoints.setSelection(binding.layoutLoyalty.etPoints.getText().toString().length());
                    binding.layoutLoyalty.etPoints.addTextChangedListener(pointsInSarTextWatcher);
                    //showLoadingDialog();
                    loyaltypointsInput = Integer.parseInt(binding.layoutLoyalty.etPoints.getText().toString());
                    paymentPresenter.validateOrder(false, CacheUtil.getInstance().getOrderStore(), null, loyaltypointsInput, false);
                } else {
                    binding.layoutLoyalty.etPoints.setSelection(0);
                    paymentPresenter.setLoyaltyPointsUsed(0);
                }
            });
        }, onError -> {

        }));

        binding.layoutLoyalty.etPoints.addTextChangedListener(pointsInSarTextWatcher);

       /* noSchedLayout.setOnClickListener(view -> {
            showPanel();
        });

        withSchedLayout.setOnClickListener(view -> {
            showPanel();
        });*/

        boolean isDelivery = false;
        Customer customer = AuthUtil.getInstance().getCustomer();
        boolean isSignedIn = AuthUtil.getInstance().isSignedIn();
        boolean isFullSignedIn = (isSignedIn && !customer.getAttributes().getAccountType().equalsIgnoreCase("guest"));

       /* switch (CacheUtil.getInstance().getOrderType()) {
            case PICKUP:
                type.setText(CacheUtil.getInstance().getTranslations(Text.PICKUP));
                orderTypeLabel2.setText(CacheUtil.getInstance().getTranslations(Text.PICKUP_AT));
                break;
            case DINE_IN:
                type.setText(CacheUtil.getInstance().getTranslations(Text.DINE_IN));
                orderTypeLabel2.setText(CacheUtil.getInstance().getTranslations(Text.DINE_IN_AT));
                break;
            case DELIVERY:
                isDelivery = true;
                type.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERY));
                orderTypeLabel2.setText(CacheUtil.getInstance().getTranslations(Text.DELIVER_TO));
                break;
            case CURBSIDE:
                type.setText(CacheUtil.getInstance().getTranslations(Text.CURBSIDE_PICKUP, "Curbside"));
                orderTypeLabel2.setText(CacheUtil.getInstance().getTranslations(Text.CURBSIDE_PICKUP_FROM_CAPS, "CURBSIDE PICKUP FROM").toUpperCase());
                break;
        }*/

        try {
            Set<PaymentType> paymentTypes = CacheUtil.getInstance().getApplication().getAttributes().getDeliverPaymentTypes();
            if (!isDelivery) {
                paymentTypes = CacheUtil.getInstance().getApplication().getAttributes().getPickupPaymentTypes();
//                tvBranchName.setText(CacheUtil.getInstance().getOrderStore().getAttributes().getName());
//                tvBranchAddress.setText(CacheUtil.getInstance().getOrderStore().getAttributes().getCountry());
            } else {
             //   tvBranchName.setText(CacheUtil.getInstance().getOrderAddress().getAttributes().getLine1());
//                tvBranchAddress.setText(CacheUtil.getInstance().getOrderAddress().getAttributes().getLine1());
            }

            //just for testing

            binding.layoutLoyalty.loyaltyPointsPaymentContainer.setVisibility(View.VISIBLE);
           /* if (isFullSignedIn && paymentTypes.contains(PaymentType.LOYALTY) && passedTotalFromCart > 0) {
                loyaltyPointsPaymentContainer.setVisibility(View.VISIBLE);
//                useLoyaltyPointsLabel.setVisibility(View.VISIBLE);
//                loyaltyPointsAdditionalInfo.setVisibility(View.VISIBLE);
            }*/

           /* branchImageContainer.setVisibility(View.VISIBLE);
            branchContainer.setVisibility(View.GONE);
            orderTypeContainer.setVisibility(View.GONE);

            if (isBranchImageAvailable) {
                branchImageContainer.setVisibility(View.VISIBLE);
                branchContainer.setVisibility(View.GONE);
                orderTypeContainer.setVisibility(View.GONE);
                Glide.with(this).load(CacheUtil.getInstance().getOrderStore().getAttributes().getImage()).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.centerCropTransform()).into(imgBranch);
            } else {
                branchImageContainer.setVisibility(View.GONE);
                branchContainer.setVisibility(View.VISIBLE);
                orderTypeContainer.setVisibility(View.VISIBLE);
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }

        //total.setText(StringUtil.getFormattedPrice(paymentPresenter.getTotalPrice()));

       /* Concept concept = CacheUtil.getInstance().getConcept();
        if (concept != null) {
            String vatNumber = concept.getVatNumber();
            String vatCertificateUrl = concept.getVatCertificateUrl();
            if (vatNumber != null && !vatNumber.isEmpty() && vatCertificateUrl != null && !vatCertificateUrl.isEmpty()) {
                financialValue.setText(vatNumber);
                financialContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (vatCertificateUrl.contains(".pdf")) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                            browserIntent.setDataAndType(Uri.parse(vatCertificateUrl), "application/pdf");
                            Intent chooser = Intent.createChooser(browserIntent, CacheUtil.getInstance().getTranslations(Text.VAT_NO_CAPS, "VAT NO."));
                            startActivity(chooser);
                        } else {
                            Intent intent = new Intent(CheckoutActivity.this, BrowserActivity.class);
                            intent.putExtra("title", CacheUtil.getInstance().getTranslations(Text.VAT_NO_CAPS, "VAT NO."));
                            intent.putExtra("url", vatCertificateUrl);
                            startActivity(intent);
                        }
                    }
                });
                financialCard.setVisibility(View.VISIBLE);
            } else {
                financialCard.setVisibility(View.GONE);
            }
        } else {
            financialCard.setVisibility(View.GONE);
        }*/

        /*tvMukafaaRemove.setOnClickListener(view -> {
            paymentPresenter.removeMukafaa();
            cardMukafaaContainer.performClick();
        });

        cardMukafaaContainer.setOnClickListener(view -> {
            if (!paymentPresenter.hasMukafaaTransactionId()) {
                CacheUtil.getInstance().setOrderPayment(PaymentType.NONE);
                mukafaaSelected = !mukafaaSelected;
                selectMukafaa(mukafaaSelected, true);
            }
        });

        requestOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String number = getNumber();
                if (number == null || number.isEmpty()) {
                    showMessage(CacheUtil.getInstance().getTranslations(Text.MOBILE_OR_CIC_REQUIRED, "Please input either your Mobile Number or Loyalty CIC Number to request an OTP."));
                } else if (!phoneNumber.getText().toString().isEmpty() && !StringUtil.isValidMobileNumber(number)) {
                    showMessage(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_INVALID));
                } else {
                    Intent intent = new Intent(PaymentActivity.this, MukafaaOTPActivity.class);
                    intent.putExtra("number", number);
                    intent.putExtra("total", String.valueOf(paymentPresenter.getValidatedTotal()));
                    intent.putExtra("cart", paymentPresenter.getValidatedBody());
                    intent.putExtra("delivery_charge", paymentPresenter.getValidatedDeliveryCharge());
                    startActivityForResult(intent, OTP_REQUEST);

                }
            }
        });

        if (paymentPresenter.hasMukafaaTransactionId()) {
            if (!mukafaaSelected) {
                mukafaaSelected = true;
                mukafaaPayment.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
                mukafaaNumbers.setVisibility(View.GONE);
                requestOTP.setVisibility(View.GONE);
                placeOrder.setVisibility(View.VISIBLE);
            }
        }*/
    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {
        binding.toolbarLayout.title.setText(CacheUtil.getInstance().getTranslations(Text.CHECKOUT, "Checkout"));
        binding.layoutPayment.tvEditPaymentMethod.setText(CacheUtil.getInstance().getTranslations(Text.EDIT, "Edit").toUpperCase());
        binding.tvEditMobileNumber.setText(CacheUtil.getInstance().getTranslations(Text.EDIT, "Edit").toUpperCase());
        binding.tvDeliveryInstructionLabel.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERY_INSTRUCTION, "Delivery Instruction").toUpperCase());
//        tvChangeOrderType.setText(CacheUtil.getInstance().getTranslations(Text.CHANGE, "Change").toUpperCase());
        binding.layoutLoyalty.tvPointsLabel.setText(CacheUtil.getInstance().getTranslations(Text.POINTS, "Points").toUpperCase());
        binding.layoutLoyalty.tvSarAmountLabel.setText(CacheUtil.getInstance().getTranslations(Text.SAR_AMOUNT, "SAR Amount").toUpperCase());
        binding.layoutPaymentSummary.tvDiscountLabel.setText(CacheUtil.getInstance().getTranslations(Text.PROMO_DISCOUNT));
        binding.layoutLoyalty.tvLoyaltyPointsLabel.setText(CacheUtil.getInstance().getTranslations(Text.LOYALTY_USE_POINTS,"Use Loyalty Points"));



        OrderType orderType = CacheUtil.getInstance().getOrderType();
        if(orderType == OrderType.DELIVERY) {
            binding.tvTimeLabel.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERY_TIME,"Delivery Time"));
            String mins = CacheUtil.getInstance().getOrderStore().getAttributes().getPromisedTimeDelta().getDelivery().toString()+" "+CacheUtil.getInstance().getTranslations("mins", "mins");
            binding.tvWithin.setText(CacheUtil.getInstance().getTranslations(Text.WITHIN, "Within "+mins));
        } else {
            binding.tvTimeLabel.setText(CacheUtil.getInstance().getTranslations(Text.PICKUP_TIME,"Pickup Time"));
            String mins = CacheUtil.getInstance().getOrderStore().getAttributes().getPromisedTimeDelta().getPickup().toString()+" "+CacheUtil.getInstance().getTranslations("mins", "mins");
            binding.tvWithin.setText(CacheUtil.getInstance().getTranslations(Text.WITHIN, "Within "+mins));
        }
//        chooseMethodLabel.setText(CacheUtil.getInstance().getTranslations(Text.CHOOSE_METHOD, "CHOOSE METHOD"));
//        if (cashLabel != null)
//            cashLabel.setText(CacheUtil.getInstance().getTranslations(Text.CASH));
//        pickADayLabel.setText(CacheUtil.getInstance().getTranslations(Text.PICK_A_DAY, "PICK A DAY").toUpperCase());
//        pickATimeLabel.setText(CacheUtil.getInstance().getTranslations(Text.PICK_A_TIME, "PICK A TIME").toUpperCase());
//        cancelButton.setText(CacheUtil.getInstance().getTranslations(Text.CANCEL, "CANCEL").toUpperCase());
//        confirmButton.setText(CacheUtil.getInstance().getTranslations(Text.CONFIRM_CAPS, "CONFIRM").toUpperCase());
        binding.placeOrder.setText(CacheUtil.getInstance().getTranslations(Text.PLACE_ORDER, "Place Order"));

//        schedOrderLabel.setText(CacheUtil.getInstance().getTranslations(Text.SCHEDULER_ORDER));
//        slidingTitle.setText(CacheUtil.getInstance().getTranslations(Text.SCHEDULER_ORDER).toUpperCase());
//        removeLabel.setText(CacheUtil.getInstance().getTranslations(Text.YOU_CAN_REMOVE_THE_SCHEDULE, "You can remove the schedule by pressing on the “x” button here"));
//        itemsLabel.setText(CacheUtil.getInstance().getTranslations(Text.ORDERED_ITEMS_CAPS, "ORDERED ITEMS"));
//        itemsQuantityLabel.setText(CacheUtil.getInstance().getTranslations(Text.ITEMS_QUANTITY_CAPS, "ITEMS & QUANTITY"));
//        priceLabel.setText(CacheUtil.getInstance().getTranslations(Text.PRICE_CAPS, "PRICE"));
        binding.layoutPaymentSummary.tvSubtotalLabel.setText(CacheUtil.getInstance().getTranslations(Text.SUBTOTAL));
        binding.layoutPaymentSummary.tvDiscount.setText(CacheUtil.getInstance().getTranslations(Text.PROMO_DISCOUNT));
//        deliveryLabel.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERY));
//        taxLabel.setText(CacheUtil.getInstance().getTranslations(Text.TAX));
        binding.layoutPaymentSummary.tvTotalLabel.setText(CacheUtil.getInstance().getTranslations(Text.TOTAL_LABEL));
//        schedLabel.setText(CacheUtil.getInstance().getTranslations(Text.SCHEDULE, "SCHEDULE").toUpperCase());
//        branchLabel.setText(CacheUtil.getInstance().getTranslations(Text.BRANCH_CAPS, "BRANCH"));
//        typeLabel.setText(CacheUtil.getInstance().getTranslations(Text.ORDER_TYPE_CAPS, "ORDER TYPE"));
//        financialLabel.setText(CacheUtil.getInstance().getTranslations(Text.VAT_NO_CAPS, "VAT NO."));
//        availableLabel.setText(CacheUtil.getInstance().getTranslations(Text.AVAILABLE_LOYALTY_POINTS, "Available Loyalty Points"));
//        btnUseAll.setText(CacheUtil.getInstance().getTranslations(Text.USE_ALL_CAPS, "USE ALL"));
//        howMuchLabel.setText(CacheUtil.getInstance().getTranslations(Text.REDEEM_AMOUNT_IN, "REDEEM AMOUNT IN (X)").replace("(X)",CacheUtil.getInstance().getTranslations(CacheUtil.getInstance().getConcept().getCurrencyCode())));
//        etPointsInput.setHint(CacheUtil.getInstance().getTranslations(Text.LOYALTY_USE_AMOUNT_INPUT_HINT, "Enter amount here"));
//        loyaltyPointsAdditionalInfo.setText(CacheUtil.getInstance().getTranslations(Text.LOYALTY_LEAVE_BLANK, "Leave this blank if you don't wanna use your points"));
//        useLoyaltyPointsLabel.setText(CacheUtil.getInstance().getTranslations(Text.LOYALTY_USE_POINTS, "USE LOYALTY POINTS"));
//        tableNumberLabel.setText(CacheUtil.getInstance().getTranslations(Text.TABLE_NUMBER_CAPS, "TABLE NUMBER"));
//        requestOTP.setText(CacheUtil.getInstance().getTranslations(Text.REQUEST_OTP, "REQUEST OTP"));
//        cardTvMukafaaBank.setText(CacheUtil.getInstance().getTranslations(Text.MUKAFAA_BANK, "Al Rajhi Bank"));
//        cardTvMukafaaRewards.setText(CacheUtil.getInstance().getTranslations(Text.MUKAFAA_REWARDS, "(Mokafa’a Rewards)"));
//        disclaimer.setText(Html.fromHtml(CacheUtil.getInstance().getTranslations(Text.MUKAFAA_DISCLAIMER, "<b>Disclaimer</b>: The user has to be a registered mobile number with Mokafa'a Al Rajhi Program, otherwise the redemption will not be successful.")));
//        phoneNumberLbl.setText(CacheUtil.getInstance().getTranslations(Text.MOBILE_NUMBER, "Mobile Number"));
//        or.setText(CacheUtil.getInstance().getTranslations(Text.OR, "OR"));
//        loyaltyNumberLbl.setText(CacheUtil.getInstance().getTranslations(Text.LOYALTY_CIC_NUMBER, "Loyalty CIC Number"));
//        itemsLabel.setText(CacheUtil.getInstance().getTranslations(Text.ORDERED_ITEMS_CAPS, "ORDERED ITEMS"));
//        itemsQuantityLabel.setText(CacheUtil.getInstance().getTranslations(Text.ITEMS_QUANTITY_CAPS, "ITEMS & QUANTITY"));
//        priceLabel.setText(CacheUtil.getInstance().getTranslations(Text.PRICE_CAPS, "PRICE"));
        binding.layoutPaymentSummary.tvSubtotalLabel.setText(CacheUtil.getInstance().getTranslations(Text.SUBTOTAL));
        binding.layoutPaymentSummary.tvDiscount.setText(CacheUtil.getInstance().getTranslations(Text.PROMO_DISCOUNT));
//        tvMukafaaLabel.setText(CacheUtil.getInstance().getTranslations(Text.AL_RAJHI_MOKAFAA, "Al Rajhi Mokafa'a"));
//        tvMukafaaRemove.setText(CacheUtil.getInstance().getTranslations(Text.REMOVE_CAPS, "Remove"));
//        deliveryLabel.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERY));
//        taxLabel.setText(CacheUtil.getInstance().getTranslations(Text.TAX_RATE, "(x)% VAT").replace("(x)", StringUtil.localize(String.valueOf(CacheUtil.getInstance().getConcept().getVatRate()))));
//        summaryTotalLabel.setText(CacheUtil.getInstance().getTranslations(Text.TOTAL));
    }

    @Override
    public void showPaymentTypes(Set<PaymentType> paymentTypes) {

    }

    @Override
    public void showPaymentTypes(ArrayList<PaymentType> paymentTypes) {

    }

    @Override
    public void setStartName(String startName) {

    }

    @Override
    public void setStartAddress(String startAddress) {

    }

    @Override
    public void setEndName(String endName) {

    }

    @Override
    public void setEndAddress(String endAddress) {

    }

    @Override
    public void updateMap(OrderType orderType, LatLng startLocation, LatLng endLocation, LatLngBounds bounds) {

    }

    @Override
    public void updateTime(Calendar calendar) {

    }

    @Override
    public void selectPaymentTypeCash() {

    }

    @Override
    public void showValidateOrderError(List<String> errors) {

    }

    @Override
    public void sendOrder() {

    }

    @Override
    public void initializeTap(String orderId, double total) {

    }

    @Override
    public void showCheckoutPaymentForm(String orderId) {

    }

    @Override
    public void showCardFailed() {

    }

    @Override
    public void showMessage(String message) {
        dismissDialogs();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showError(String error) {
        dismissDialogs();
        showAlertDialog("", error);
    }

    @Override
    public void showCompleted(String orderId, OrderType orderType) {
        dismissDialogs();
        this.orderId = orderId;
        continueOrder();
    }

    @Override
    public void setSchedule() {
//        try {
//            FragmentManager fragmentManager = getSupportFragmentManager();
//            if (!fragmentManager.isDestroyed()) {
//                OrderDialogFragment fragment = OrderDialogFragment.newInstance(true);
//                fragment.show(fragmentManager, OrderDialogFragment.class.getSimpleName());
//            }
//            String colorStr = DynamicTheme.getStringColor(CheckoutActivity.this);
//            String start = CacheUtil.getInstance().getTranslations(Text.ORDER_SCHEDULED_FOR, "Order scheduled for:");
//            String at = CacheUtil.getInstance().getTranslations(Text.AT, "at");
//            String dayOnly = TimeUtil.dateToMonthAndDayOnly(daySelected);
//            String timeOnly = TimeUtil.dateToHourOnly(selectedDate);
//            goodDaySelected = daySelected;
//            goodSelectedDate = selectedDate;
//            String day = "<font color=" + colorStr + ">" + dayOnly + "</font>";
//            String time = "<font color=" + colorStr + ">" + timeOnly + "</font>";
//            schedTime.setText(Html.fromHtml(start + " " + day + " " + at + " " + time));
//            removeLayout.setVisibility(View.VISIBLE);
//            removeLabel.setVisibility(View.VISIBLE);
//            noSchedLayout.setVisibility(View.GONE);
//            spacingTop.setVisibility(View.VISIBLE);
//            withSchedLayout.setVisibility(View.VISIBLE);
//            placeOrder.setText(CacheUtil.getInstance().getTranslations(Text.PLACE_SCHEDULED_ORDER, "PLACE SCHEDULED ORDER"));
//            isRemove = false;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void setTax(boolean inclusive, CharSequence taxAmount) {
//        vatLayout.setVisibility(View.VISIBLE);
//        if (inclusive) {
//            vatLayout.setVisibility(View.GONE);
//        }
//        tax.setText(taxAmount);
//        taxLabel.setText(CacheUtil.getInstance().getConcept().getVatRate() + "% " + CacheUtil.getInstance().getTranslations(Text.VAT, "VAT"));
    }

    @Override
    public void setPriceDetails(double subtotalPrice, double discount, double totalPrice, double deliveryCharge, double pointsSarValue, int pointsEarnable, int pointsUsed, ArrayList<ServiceCharge> charges) {

        boolean hasPromotion = CacheUtil.getInstance().hasPromotion();
        String originalPrice = String.valueOf(CurrencyUtils.Companion.updateCurrency(subtotalPrice, true, hasPromotion));

// Create a SpannableString to apply underline
        SpannableString spannableString = new SpannableString(originalPrice);

// Calculate the center position of the text
        int centerPosition = originalPrice.length() / 2;

// Apply UnderlineSpan to the center of the text
        spannableString.setSpan(new UnderlineSpan(), centerPosition, centerPosition + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

// Apply the formatted text to the TextView
        binding.tvOriginalPrice.setText(spannableString);
        binding.layoutPaymentSummary.tvSubtotal.setText(CurrencyUtils.Companion.updateCurrency(subtotalPrice, true, hasPromotion));

        if (discount != 0) {
            binding.layoutPaymentSummary.grpDiscount.setVisibility(View.VISIBLE);
            binding.layoutPaymentSummary.tvDiscount.setText(CurrencyUtils.Companion.updateCurrency(discount, true, hasPromotion, discount > 0));
        } else {
            binding.layoutPaymentSummary.grpDiscount.setVisibility(View.GONE);
        }

//        if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
//            if (deliveryCharge != 0) {
//                deliveryText.setText(StringUtil.getFormattedPrice(deliveryCharge));
//            } else {
//                Campaign campaign = CacheUtil.getInstance().getCampaign();
//                boolean hideDiscount = false;
//                try {
//                    if (campaign != null) {
//                        if (campaign.getPromotion().getAttributes().getType().equals("delivery")) {
//                            hideDiscount = true;
//                        }
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                if (hideDiscount) {
//                    grpDiscount.setVisibility(View.GONE);
//                }
//                deliveryText.setText(CacheUtil.getInstance().getTranslations(Text.FREE, "Free"));
//            }
//            deliveryLayout.setVisibility(View.VISIBLE);
//        } else {
//            deliveryLayout.setVisibility(View.GONE);
//        }

        if (charges.size() > 0) {
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this) {
                @Override
                public boolean canScrollVertically() {
                    return false;
                }
            };
            binding.layoutPaymentSummary.rvServiceCharge.setLayoutManager(linearLayoutManager);
            orderServiceChargeRecyclerViewAdapter = new OrderServiceChargeRecyclerViewAdapter(charges);
            binding.layoutPaymentSummary.rvServiceCharge.setAdapter(orderServiceChargeRecyclerViewAdapter);
        }

//        if (deliveryCharge != 0) {
//            deliveryLayout.setVisibility(View.VISIBLE);
//            deliveryText.setText(StringUtil.getFormattedPrice(deliveryCharge));
//        } else {
//            Promotion promotion = CacheUtil.getInstance().getCampaignPromotion();
//            if (promotion != null) {
//                if (promotion.getAttributes().getType().equalsIgnoreCase("delivery")) {
//                    deliveryLayout.setVisibility(View.VISIBLE);
//                    deliveryText.setText(CacheUtil.getInstance().getTranslations(Text.FREE, "Free"));
//                } else {
//                    deliveryLayout.setVisibility(View.GONE);
//                }
//            } else {
//                deliveryLayout.setVisibility(View.GONE);
//            }
//        }


     /*   double mukafaaAmount = paymentPresenter.getMukafaaTransactionAmount();
        discount = discount - mukafaaAmount;*/
        binding.layoutPaymentSummary.tvDiscount.setText(CurrencyUtils.Companion.updateCurrency(discount, true, hasPromotion, discount > 0));
        binding.layoutPaymentSummary.grpDiscount.setVisibility(discount != 0 ? View.VISIBLE : View.GONE);

        /*if (mukafaaAmount > 0) {
            if (paymentPresenter.hasMukafaaTransactionId() && paymentPresenter.isMukafaaEnabled()) {
                String amnt = String.valueOf(StringUtil.getFormattedPrice(mukafaaAmount));
                if (!"0".equals(amnt) && !amnt.contains("-")) amnt = "-" + amnt;
                tvMukafaaSar.setText(amnt);

                grpMukafaa.setVisibility(View.VISIBLE);
            } else {
                grpMukafaa.setVisibility(View.GONE);
            }
        } else {
            grpMukafaa.setVisibility(View.GONE);
            selectMukafaa(false, false);
        }*/

        if (pointsUsed != 0) {
            binding.layoutPaymentSummary.grpRedeemedPoints.setVisibility(View.VISIBLE);

            String redeemedText = CacheUtil.getInstance().getTranslations(Text.REDEEMED, "redeemed");
            String color = CacheUtil.getInstance().getColorMain();

            String ptsRedeemed = CacheUtil.getInstance().getTranslations(Text.PTS_LABEL, "(x) pts").replace("(x)", String.valueOf(pointsUsed));

//            tvRedeemPoints.setText(HtmlCompat.fromHtml("<font color=" + color + ">" + ptsRedeemed + "</font> ", HtmlCompat.FROM_HTML_MODE_LEGACY));
            binding.layoutPaymentSummary.tvRedeemPointsEquiv.setText(CacheUtil.getInstance().getTranslations(Text.LOYALTY_POINTS, "Loyalty poinst (x)").replace("x", String.valueOf(pointsUsed)));

            binding.layoutPaymentSummary.tvRedeemPointsEquiv.setText(CurrencyUtils.Companion.updateCurrency(Math.min(pointsSarValue, paymentPresenter.getMenuItemsTotalPrice()), true, hasPromotion, true));
        } else {
            binding.layoutPaymentSummary.grpRedeemedPoints.setVisibility(View.GONE);
        }

        if (totalPrice == 0 && pointsUsed > 0) {

            if (deliveryCharge <= 0) {
//                chooseMethodLabel.setVisibility(View.GONE);
               /* if (cardOrderTypePayment != null) cardOrderTypePayment.setVisibility(View.GONE);
                if (cardPayment != null) cardPayment.setVisibility(View.GONE);
//                mukafaaPayment.setVisibility(View.GONE);
                if (cashPayment != null) cashPayment.setVisibility(View.GONE);*/
                binding.layoutLoyalty.loyaltyPointsPaymentContainer.setVisibility(View.VISIBLE);
//                useLoyaltyPointsLabel.setVisibility(View.VISIBLE);
//                loyaltyPointsAdditionalInfo.setVisibility(View.VISIBLE);
               /* if (mukafaaAmount > 0) {
                    mukafaaPayment.setVisibility(View.VISIBLE);
                }*/
                selectPaymentTypeCash();
            } else if (subtotalPrice == pointsSarValue && deliveryCharge > 0) {
//                if (cardOrderTypePayment != null) cardOrderTypePayment.setVisibility(View.VISIBLE);
//                if (cardPayment != null) cardPayment.setVisibility(View.VISIBLE);
////                mukafaaPayment.setVisibility(View.VISIBLE);
//                if (cashPayment != null) cashPayment.setVisibility(View.VISIBLE);
            }
        } else {
            paymentPresenter.getPaymentTypes();
           /* if (totalPrice == deliveryCharge && mukafaaAmount == 0) {
                mukafaaPayment.setVisibility(View.GONE);
            }*/
//            if (cardOrderTypePayment != null) cardOrderTypePayment.setVisibility(View.VISIBLE);
//            if (cardPayment != null) cardPayment.setVisibility(View.VISIBLE);
//            if (cashPayment != null) cashPayment.setVisibility(View.VISIBLE);
        }

        binding.tvDiscountedPrice.setText(CurrencyUtils.Companion.updateCurrency(totalPrice, true, CacheUtil.getInstance().hasPromotion()));
        binding.layoutPaymentSummary.tvTotal.setText(CurrencyUtils.Companion.updateCurrency(totalPrice, true, CacheUtil.getInstance().hasPromotion()));
        totalValue = totalPrice;


        Loyalty loyalty = AuthUtil.getInstance().getCustomerLoyalty();
        /*double finalDiscount = discount;
        double finalTotal;
        if (mukafaaAmount > 0) {
            finalTotal = subtotalPrice - mukafaaAmount;
        } else {
            finalTotal = subtotalPrice;
        }*/

       /* btnUseAll.setOnClickListener(v -> {



            //double points = loyalty.getAttributes().getCurrentBalanceAmount() > (finalTotal - finalDiscount) ? (finalTotal - finalDiscount) * Double.parseDouble(AuthUtil.getInstance().getCustomerLoyalty().getAttributes().getPointConversion()) : loyalty.getAttributes().getCurrentBalance();
            double currentBalance = loyalty.getAttributes().getCurrentBalanceAmount() > (finalTotal - finalDiscount) ? (finalTotal - finalDiscount)  : loyalty.getAttributes().getCurrentBalanceAmount();

            // If points = 3.4 for example, make it 4 points
         *//*   if ((int) points < points) {
                points += 1;
            }*//*
            loyaltypointsInput = (int) currentBalance;
            etPointsInput.setText(StringUtil.localize(String.valueOf((int) currentBalance)));

            if (deliveryCharge != 0 && (subtotalPrice - finalDiscount) == 0) {
                Toast toast = Toast.makeText(PaymentActivity.this, CacheUtil.getInstance().getTranslations(Text.POINTS_NOT_APPLICABLE, "Sorry, loyalty points can only be used to pay on purchased items excluding delivery charge."), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM, 0, ViewUtil.dpToPx(PaymentActivity.this, 80));
                toast.show();
            } else if (currentBalance > (subtotalPrice - finalDiscount)) {
                Toast toast = Toast.makeText(PaymentActivity.this, CacheUtil.getInstance().getTranslations(Text.POINTS_MAXIMUM_LIMIT, "Sorry, the maximum points allowed to be used for this transaction is (x).").replace("(x)", String.valueOf((int) currentBalance)), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM, 0, ViewUtil.dpToPx(PaymentActivity.this, 80));
                toast.show();
            }

        });*/
    }

    @Override
    public void setMenuItem(MenuItem menuItem) {

    }

    @Override
    public void logFirebaseEvent(String event, String orderTotalPrice) {
        if (Values.sDeepLinkUtm != null) {
            Bundle utm = Values.sDeepLinkUtm;
            utm.putString("purchase_amount", orderTotalPrice);
            @SuppressLint("MissingPermission") FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this);
            firebaseAnalytics.logEvent(event, utm);
        }
    }

    @SuppressLint("MissingPermission")
    private void continueOrder() {
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (!fragmentManager.isDestroyed()) {
                OrderDialogFragment fragment = OrderDialogFragment.newInstance(false);
                fragment.show(fragmentManager, OrderDialogFragment.class.getSimpleName());
            }
            new Handler().postDelayed(() -> {
                runOnUiThread(() -> {
                    Concept concept = CacheUtil.getInstance().getConcept();
                    if (concept != null) {
                        long promptRatingSeconds = CacheUtil.getInstance().getConcept().getPromptRatingSeconds();
                        if (promptRatingSeconds > 0) {
                            promptRatingSeconds = promptRatingSeconds * 1_000L;
                            Intent intent = new Intent(this, AppRatingBroadcastReceiver.class);
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                            if (alarmManager != null) {
                                alarmManager.cancel(pendingIntent);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || alarmManager.canScheduleExactAlarms()) {
                                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + promptRatingSeconds, pendingIntent);
                                    }
                                } else {
                                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + promptRatingSeconds, pendingIntent);
                                }
                            }
                        }
                    }
                    Intent intent = new Intent(CheckoutActivity.this, MainActivity.class);
                    intent.putExtra("payment", true);
                    intent.putExtra("order_id", orderId);
                    startActivity(intent);
                    finish();
                });

            }, 2300);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void showPrayerTime(PrayerTime prayerTime) {
        if (prayerTime.prayer) {
            FirebaseService.newNotification(CacheUtil.getInstance().getTranslations("prayer_time_notification", "Prayer Time"), this);
        }
    }

    @Override
    public void showErrorPrayerTime() {

    }

    ActivityResultLauncher<kotlin.Pair<String, ResultType>> activityResultLauncher = registerForActivityResult((ActivityResultContract) (new PaymentResultContract()), (ActivityResultCallback) (result) -> {
        if (result == null) return;
        if (result instanceof PaymentStatus.Success) {
            paymentPresenter.sendUpsells();
        } else if (result instanceof PaymentStatus.Redirect) {
            if (((PaymentStatus.Redirect) result).getResult().component2().getStatus().equalsIgnoreCase("SUCCESSFUL")) {
                paymentPresenter.sendUpsells();
            } else {
                showCardFailed();
            }
        } else {
            showCardFailed();
        }
    });

    @Override
    public ActivityResultLauncher<Pair<String, ResultType>> moneyHashContract() {
        return activityResultLauncher;
    }

    @Override
    public void setOrderId(String orderId) {

    }

    @Override
    public void showCardDialogList(String orderId) {
        if (isCanProceedFragmentTransaction(getSupportFragmentManager())) {
            savedCardsDialog = SavedCardsDialog.newInstance(CacheUtil.getInstance().getCustomerCards(), new SavedCardsDialog.OnSavedAction() {
                @Override
                public void onClose() {
                    dismissDialogs();
                    savedCardsDialog.dismissAllowingStateLoss();
                }

                @Override
                public void onEditCards() {
                    if (savedCardsDialog != null) {
                        savedCardsDialog.switchToEditMode();
                    }
                }

                @Override
                public void onUseNewCard() {
                    savedCardsDialog.dismissAllowingStateLoss();
                    showCheckoutPaymentForm(orderId);
                }

                @Override
                public void onAvail(CustomerCard card) {
                    if (card != null) {
                        inputCvvDialog(orderId, card);
                    }
                }
            });
            savedCardsDialog.setCancelable(false);
            // Check if the fragment is still added to the activity
            if (!savedCardsDialog.isAdded()) {
                savedCardsDialog.show(getSupportFragmentManager(), SavedCardsDialog.class.toString());
            }
        }
    }

    private boolean isCanProceedFragmentTransaction(FragmentManager fragmentManager) {
        return !isFinishing() && !fragmentManager.isStateSaved() && !fragmentManager.isDestroyed();
    }

    @Override
    public void firebaseECommercePurchaseEvent(List<MenuItem> orderedItem, double total) {
        FirebaseECommerceEventUtil.Companion.purchaseEvent(this, paymentPresenter.getMenuItemsList(), total);
        FacebookAppEventUtil.Companion.purchaseEvent(this, paymentPresenter.getMenuItemsList(), total);
    }

    @Override
    public void firebaseAddPaymentInfoEvent() {
        try {
            if (this != null) {
                FirebaseECommerceEventUtil.Companion.addPaymentInfoEvent(this, paymentPresenter.getMenuItemsList(), totalValue);
                FacebookAppEventUtil.Companion.addPaymentInfoEvent(this, CacheUtil.getInstance().getOrderPayment().getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void firebaseApplyPromotionEvent(String promotionId, String promotionName, List<MenuItem> orderedItems) {
        FirebaseECommerceEventUtil.Companion.applyPromotionEvent(this, promotionId, promotionName, paymentPresenter.getMenuItemsList());
    }

    @Override
    public void updateMenuItems() {
//        menuItemsRecyclerViewAdapter.notifyDataSetChanged();
    }

    @Override
    public void showMoyasarSheet(String orderId, double validatedTotal, String menuItems, String apiKey) {
        new Handler().post(() -> {
                    Intent intent = new Intent(CheckoutActivity.this,MoyasarActivity.class);
                    intent.putExtra(MoyasarActivity.EXTRA_ORDER_ID, orderId);
                    intent.putExtra(MoyasarActivity.EXTRA_AMOUNT, validatedTotal);
                    intent.putExtra(MoyasarActivity.EXTRA_MENU_ITEMS, menuItems);
                    intent.putExtra(MoyasarActivity.EXTRA_API_KEY, apiKey);
                    moyasarActivityResultLauncher.launch(intent);
                }
        );
    }

    @Override
    public void showOrderType(Boolean checkout) {
        String storeId = CacheUtil.getOrderStoreId();
        Intent intent = new Intent(this, OrderTypeActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("checkout", true);
        startActivity(intent);
    }

    @Override
    public void showSelectedPyament() {
        PaymentType selecetedPayment = CacheUtil.getInstance().getOrderPayment();
        if (selecetedPayment == PaymentType.NONE) {
            binding.layoutPayment.tvSelectedPayment.setVisibility(View.GONE);
            binding.layoutPayment.ivSelectedPayment.setVisibility(View.GONE);
        } else {
            binding.layoutPayment.tvSelectedPayment.setVisibility(View.VISIBLE);
            binding.layoutPayment.ivSelectedPayment.setVisibility(View.VISIBLE);
        }
        if (selecetedPayment == PaymentType.CASH) {
            binding.layoutPayment.tvSelectedPayment.setText(CacheUtil.getInstance().getTranslations(Text.CASH, "Cash"));
            binding.layoutPayment.ivSelectedPayment.setImageResource(R.drawable.ic_payment_cash);
        } else if (selecetedPayment == PaymentType.CARD) {
            binding.layoutPayment.tvSelectedPayment.setText(CacheUtil.getInstance().getTranslations(Text.CARD, "Card"));
            binding.layoutPayment.ivSelectedPayment.setImageResource(R.drawable.ic_payment_type_card);
        } else if (selecetedPayment == PaymentType.CARD_ON_DELIVERY || selecetedPayment == PaymentType.CARD_ON_PICKUP) {
            binding.layoutPayment.tvSelectedPayment.setText(CacheUtil.getInstance().getTranslations(Text.CASH, "Cash"));
            binding.layoutPayment.ivSelectedPayment.setImageResource(R.drawable.ic_payment_cash);
        }
    }

    private void showPaymentMethod() {
        Intent intent = new Intent(this, ChoosePaymentAcitivty.class);
        startActivity(intent);
    }

    private void inputCvvDialog(String orderId, CustomerCard card) {
        cvvDialogAlert = CVVDialogAlert.newInstance(new CVVDialogAlert.OnCVVAction() {
            @Override
            public void onClose() {
                cvvDialogAlert.dismiss();
            }

            @Override
            public void onProceed(String cvv) {
                savedCardsDialog.dismiss();
                cvvDialogAlert.dismiss();
                showLoadingDialog();
                checkTokenGenerated(card.attributes.getToken(), cvv, false, orderId);
            }
        }, card);
        cvvDialogAlert.setCancelable(false);
        cvvDialogAlert.show(getSupportFragmentManager(), CVVDialogAlert.class.toString());
    }


    @Override
    public void onDateSelected(@NonNull MaterialCalendarView materialCalendarView, @NonNull CalendarDay calendarDay, boolean b) {
//        oneDayDecorator.setDate(calendarDay.getDate());
//        daySelected = calendarDay.getDate().toString();
//        widget.invalidateDecorators();
    }

    @Override
    public void paymentSucceed(@NonNull Charge charge) {
        ShipBookLogger.logInfo(BuildConfig.TAG + " - Order ID: " + paymentPresenter.getOrderId() + " -> Tap Card Payment Success");
        paymentPresenter.verifyTap(true, charge.getId());
    }

    @Override
    public void paymentFailed(@Nullable Charge charge) {
        ShipBookLogger.logError(BuildConfig.TAG + " - Order ID: " + paymentPresenter.getOrderId() + " -> Tap Card Payment Failed");
        if (charge != null) {
            ApiLogUtil.sendOrderLog(ApiLogLevel.ERROR, orderId, paymentPresenter.getPaymentProvider(), "Card Payment Failed (Charge: " + new Gson().toJson(charge) + ")");
            paymentPresenter.verifyTap(false, charge.getId());
        } else {
            ApiLogUtil.sendOrderLog(ApiLogLevel.ERROR, orderId, paymentPresenter.getPaymentProvider(), "Card Payment Failed");
            showCardFailed();
        }
    }

    @Override
    public void authorizationSucceed(@NonNull Authorize authorize) {

    }

    @Override
    public void authorizationFailed(Authorize authorize) {
        ApiLogUtil.sendOrderLog(ApiLogLevel.ERROR, orderId, paymentPresenter.getPaymentProvider(), "Card Payment Failed (Reason: authorizationFailed)");
        showCardFailed();
    }

    @Override
    public void cardSaved(@NonNull Charge charge) {

    }

    @Override
    public void cardSavingFailed(@NonNull Charge charge) {

    }

    @Override
    public void cardTokenizedSuccessfully(@NonNull Token token) {

    }

    @Override
    public void savedCardsList(@NonNull CardsList cardsList) {

    }

    @Override
    public void sdkError(@Nullable GoSellError goSellError) {
        ShipBookLogger.logError(BuildConfig.TAG + " - Order ID: " + paymentPresenter.getOrderId() + " -> Tap Card Payment SDK Error");
        if (goSellError != null) {
            ApiLogUtil.sendOrderLog(ApiLogLevel.ERROR, orderId, paymentPresenter.getPaymentProvider(), "Card Payment Failed (Reason: sdkError " + new Gson().toJson(goSellError) + ")");
        } else {
            ApiLogUtil.sendOrderLog(ApiLogLevel.ERROR, orderId, paymentPresenter.getPaymentProvider(), "Card Payment Failed (Reason: sdkError)");
        }
        showCardFailed();
    }

    @Override
    public void sessionIsStarting() {

    }

    @Override
    public void sessionHasStarted() {

    }

    @Override
    public void sessionCancelled() {
        ApiLogUtil.sendOrderLog(ApiLogLevel.INFO, orderId, paymentPresenter.getPaymentProvider(), "Card Payment sessionCancelled");
        ApiLogUtil.sendOrderLog(ApiLogLevel.WARNING, orderId, paymentPresenter.getPaymentProvider(), "Card Payment sessionCancelled");
    }

    @Override
    public void sessionFailedToStart() {
        ApiLogUtil.sendOrderLog(ApiLogLevel.ERROR, orderId, paymentPresenter.getPaymentProvider(), "Card Payment Failed (Reason: sessionFailedToStart)");
        showCardFailed();
    }

    @Override
    public void invalidCardDetails() {
        ApiLogUtil.sendOrderLog(ApiLogLevel.ERROR, orderId, paymentPresenter.getPaymentProvider(), "Card Payment Failed (Reason: invalidCardDetails)");
        showCardFailed();
    }

    @Override
    public void backendUnknownError(String message) {
        ApiLogUtil.sendOrderLog(ApiLogLevel.ERROR, orderId, paymentPresenter.getPaymentProvider(), "Card Payment Failed (Reason: backendUnknownError " + message + ")");
        showCardFailed();
    }

    @Override
    public void invalidTransactionMode() {
        ApiLogUtil.sendOrderLog(ApiLogLevel.ERROR, orderId, paymentPresenter.getPaymentProvider(), "Card Payment Failed (Reason: invalidTransactionMode)");
        showCardFailed();
    }

    @Override
    public void invalidCustomerID() {
        ApiLogUtil.sendOrderLog(ApiLogLevel.ERROR, orderId, paymentPresenter.getPaymentProvider(), "Card Payment Failed (Reason: invalidCustomerID)");
        showCardFailed();
    }

    @Override
    public void userEnabledSaveCardOption(boolean saveCardEnabled) {

    }

    @Override
    public void cardTokenizedSuccessfully(@NonNull Token token, boolean saveCardEnabled) {

    }

    private void checkTokenGenerated(String token, String cvv, boolean isSave, String orderIds) {
        JsonObject body = new JsonObject();
        body.addProperty("order", orderIds);
        body.addProperty("token", token);
        body.addProperty("save-card", isSave);
        if (cvv != null && !cvv.isEmpty()) {
            body.addProperty("cvv", cvv);
        }
        OrdersApiCall.getInstance().checkoutAuthorize(body, new ApiRequestListener() {
            @Override
            public void onSuccess(Object object) {
                dismissDialogs();
                if (object instanceof JSONObject) {
                    try {
                        JSONObject data = (JSONObject) object;
                        boolean isApproved = data.getBoolean("is-approved");
                        if (isApproved) {
                            if(dialogCheckout != null){
                                dialogCheckout.handle3ds(data);
                            }else {
                                checkoutForce(data);
                            }

                        } else {
                            this.onError(CacheUtil.getInstance().getTranslations(Text.CARD_PAYMENT_FAILED_PAY_WITH_CASH));
                        }
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                        this.onError(ex);
                    }
                } else {
                    dismissDialogs();
                    this.onError(CacheUtil.getInstance().getTranslations(Text.CARD_PAYMENT_FAILED_PAY_WITH_CASH));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                if (o instanceof JSONObject) {
                    JSONObject object = (JSONObject) o;
                    ApiLogUtil.sendOrderLog(ApiLogLevel.ERROR, orderId, paymentPresenter.getPaymentProvider(), "Card Payment Failed (Reason: " + object + ")");
                }
                dismissDialogs();
                showCardFailed();
            }
        });
    }

    private void checkoutForce(JSONObject data) {
        /*Intent intent = new Intent(PaymentActivity.this, CustomCheckoutActivity.class);
        intent.putExtra(CustomCheckoutActivity.EXTRA_ORDER_ID , orderId);
        intent.putExtra(CustomCheckoutActivity.EXTRA_PAYMENT_PROVIDER, paymentPresenter.getPaymentProvider());
        intent.putExtra(CustomCheckoutActivity.EXTRA_3DS_DATA, data.toString());
        checkoutActivityLauncher.launch(intent);
        isFromCheckout = true;*/
        showCheckoutPaymentForm(orderId);
        new Handler().postDelayed(() -> dialogCheckout.handle3ds(data), 400);
    }
    private void showPanel() {
//        if (CacheUtil.getInstance().getOrderPayment() == PaymentType.NONE) {
//            Toast.makeText(CheckoutActivity.this, CacheUtil.getInstance().getTranslations(Text.SELECT_PAYMENT_METHOD), Toast.LENGTH_SHORT).show();
//        } else {
//            if (isRemove) {
//                widget.setSelectedDate(LocalDate.now());
//                setUpDatePicker();
//            }
//            slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
//        }
    }
}
