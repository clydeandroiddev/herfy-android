package com.herfy.payment.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.checkout.tokenization.model.TokenDetails;
import com.github.florent37.singledateandtimepicker.SingleDateAndTimePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.moneyhash.sdk.android.ResultType;
import com.moneyhash.sdk.android.payment.PaymentResultContract;
import com.moneyhash.sdk.android.payment.PaymentStatus;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.herfy.BuildConfig;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.browser.BrowserActivity;
import com.herfy.data.constants.Text;
import com.herfy.data.constants.Values;
import com.herfy.decorator.BeforeDateDecorator;
import com.herfy.decorator.OneDayDecorator;
import com.herfy.firebase.FirebaseService;
import com.herfy.font.FontHandler;
import com.herfy.history.adapters.OrderServiceChargeRecyclerViewAdapter;
import com.herfy.main.MainActivity;
import com.herfy.mukafaa_otp.MukafaaOTPActivity;
import com.herfy.payment.PaymentContract;
import com.herfy.payment.PaymentPresenter;
import com.herfy.payment.savedcards.CVVDialogAlert;
import com.herfy.payment.savedcards.SaveDialogAlert;
import com.herfy.payment.savedcards.SavedCardsDialog;
import com.herfy.ratings.AppRatingBroadcastReceiver;
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
import com.herfy.summary.OrderSummaryMenuItemRecyclerViewAdapter;
import com.herfy.util.ApiLogLevel;
import com.herfy.util.ApiLogUtil;
import com.herfy.util.AppLogger;
import com.herfy.util.AuthUtil;
import com.herfy.util.BuildUtil;
import com.herfy.util.CacheUtil;
import com.herfy.util.CurrencyUtils;
import com.herfy.util.EnvUtil;
import com.herfy.analytics.FacebookAppEventUtil;
import com.herfy.analytics.FirebaseECommerceEventUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.ShipBookLogger;
import com.herfy.util.StringUtil;
import com.herfy.util.TimeUtil;
import com.herfy.util.ViewUtil;
import com.herfy.views.DynamicTheme;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONException;
import org.json.JSONObject;
import org.threeten.bp.LocalDate;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import company.tap.gosellapi.GoSellSDK;
import company.tap.gosellapi.internal.api.callbacks.GoSellError;
import company.tap.gosellapi.internal.api.models.Authorize;
import company.tap.gosellapi.internal.api.models.Charge;
import company.tap.gosellapi.internal.api.models.PhoneNumber;
import company.tap.gosellapi.internal.api.models.Token;
import company.tap.gosellapi.open.controllers.SDKSession;
import company.tap.gosellapi.open.controllers.ThemeObject;
import company.tap.gosellapi.open.delegate.SessionDelegate;
import company.tap.gosellapi.open.enums.AppearanceMode;
import company.tap.gosellapi.open.enums.TransactionMode;
import company.tap.gosellapi.open.models.CardsList;
import company.tap.gosellapi.open.models.Reference;
import company.tap.gosellapi.open.models.TapCurrency;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

public class PaymentActivity extends BaseActivity implements PaymentContract.View, SessionDelegate, OnDateSelectedListener {

    @BindView(R.id.sliding_panel)
    SlidingUpPanelLayout slidingPanel;
    @BindView(R.id.back)
    MaterialButton back;
    @BindView(R.id.cart)
    ConstraintLayout cart;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.order_type_label)
    AppCompatTextView orderTypeLabel;
    @BindView(R.id.order_type_location)
    AppCompatTextView orderTypeLocation;
    @BindView(R.id.choose_method_label)
    AppCompatTextView chooseMethodLabel;

    MaterialCardView cashPayment;
    AppCompatTextView cashLabel;
    MaterialCardView cardPayment;
    AppCompatTextView cardName;
    MaterialCardView cardOrderTypePayment;
    AppCompatTextView cardOnName;
    @BindView(R.id.remove_layout)
    ConstraintLayout removeLayout;
    @BindView(R.id.remove_label)
    AppCompatTextView removeLabel;
    @BindView(R.id.spacing_top)
    View spacingTop;
    @BindView(R.id.with_sched_layout)
    CardView withSchedLayout;
    @BindView(R.id.remove_sched)
    ImageView removeSched;
    @BindView(R.id.sched_time)
    AppCompatTextView schedTime;
    @BindView(R.id.no_sched_layout)
    LinearLayout noSchedLayout;
    @BindView(R.id.sched_order_label)
    AppCompatTextView schedOrderLabel;
    @BindView(R.id.place_order)
    MaterialButton placeOrder;
    // sliding panel
    @BindView(R.id.calendarView)
    MaterialCalendarView widget;
    @BindView(R.id.date_picker)
    SingleDateAndTimePicker datePicker;
    @BindView(R.id.sliding_title)
    AppCompatTextView slidingTitle;
    @BindView(R.id.pick_a_day_label)
    AppCompatTextView pickADayLabel;
    @BindView(R.id.pick_a_time_label)
    AppCompatTextView pickATimeLabel;
    @BindView(R.id.cancel_button)
    AppCompatTextView cancelButton;
    @BindView(R.id.confirm_button)
    MaterialButton confirmButton;
    @BindView(R.id.stubImage)
    ImageView stubImage;
    @BindView(R.id.menu_items_recycler_view)
    RecyclerView menuItemsRecyclerView;
    @BindView(R.id.items_label)
    TextView itemsLabel;
    @BindView(R.id.items_quantity_label)
    TextView itemsQuantityLabel;
    @BindView(R.id.price_label)
    TextView priceLabel;
    @BindView(R.id.subtotal_label)
    TextView subtotalLabel;
    @BindView(R.id.subtotal)
    TextView subtotalText;
    @BindView(R.id.discount_label)
    TextView discountLabel;
    @BindView(R.id.discount)
    TextView discountText;
    @BindView(R.id.delivery_label)
    TextView deliveryLabel;
    @BindView(R.id.delivery)
    TextView deliveryText;
    @BindView(R.id.total_label)
    TextView totalLabel;
    @BindView(R.id.total)
    TextView total;
    @BindView(R.id.branch_label)
    TextView branchLabel;
    @BindView(R.id.branch)
    TextView branch;
    @BindView(R.id.type_label)
    TextView typeLabel;
    @BindView(R.id.type)
    TextView type;
    @BindView(R.id.sched_layout)
    CardView schedLayout;
    @BindView(R.id.sched_label)
    TextView schedLabel;
    @BindView(R.id.schedule)
    TextView scheduleText;
    @BindView(R.id.vat_layout)
    LinearLayout vatLayout;
    @BindView(R.id.delivery_layout)
    LinearLayout deliveryLayout;
    @BindView(R.id.discount_layout)
    LinearLayout discountLayout;
    @BindView(R.id.financial_card)
    MaterialCardView financialCard;
    @BindView(R.id.financial_container)
    ConstraintLayout financialContainer;
    @BindView(R.id.financial_label)
    TextView financialLabel;
    @BindView(R.id.financial)
    TextView financialValue;
    /**
     * Loyalty views
     */
    @BindView(R.id.use_loyalty_points_label)
    AppCompatTextView useLoyaltyPointsLabel;
    @BindView(R.id.loyalty_points_additional_info)
    AppCompatTextView loyaltyPointsAdditionalInfo;
    @BindView(R.id.loyalty_points_payment_container)
    MaterialCardView loyaltyPointsPaymentContainer;
    @BindView(R.id.loyalty_points)
    ConstraintLayout loyaltyPointsPayment;
    @BindView(R.id.pointsLayout)
    ConstraintLayout pointsLayout;
    @BindView(R.id.loyalty_label)
    TextView loyaltyLabel;
    @BindView(R.id.how_much_label)
    TextView howMuchLabel;
    @BindView(R.id.available_label)
    TextView availableLabel;
    @BindView(R.id.btn_use_all)
    MaterialButton btnUseAll;
    @BindView(R.id.et_points_input)
    TextInputEditText etPointsInput;
    @BindView(R.id.loyalty_image_selected)
    ImageView loyaltyImageSelected;
    @BindView(R.id.redeemed_points_layout)
    LinearLayout redeemedPointsLayout;
    @BindView(R.id.redeemed_points_bold)
    TextView redeemedPointsBold;
    @BindView(R.id.redeemed_points_label)
    TextView redeemedPointsLabel;
    @BindView(R.id.redeemed_points)
    TextView redeemedPoints;
    @BindView(R.id.table_number_label)
    TextView tableNumberLabel;
    @BindView(R.id.table_number)
    TextView tableNumber;
    @BindView(R.id.table_number_layout)
    MaterialCardView tableNumberLayout;
    //    @BindView(R.id.card_mukafaa_image_selected)
//    ImageView mukafaaImageSelected;
    @BindView(R.id.card_mukafaa)
    MaterialCardView mukafaaPayment;
    @BindView(R.id.card_tv_mukafaa_bank)
    AppCompatTextView cardTvMukafaaBank;
    @BindView(R.id.card_tv_mukafaa_rewards)
    AppCompatTextView cardTvMukafaaRewards;
    @BindView(R.id.disclaimer)
    AppCompatTextView disclaimer;
    @BindView(R.id.phone_number_label)
    AppCompatTextView phoneNumberLbl;
    @BindView(R.id.phone_number)
    EditText phoneNumber;
    @BindView(R.id.or)
    AppCompatTextView or;
    @BindView(R.id.loyalty_number_lbl)
    AppCompatTextView loyaltyNumberLbl;
    @BindView(R.id.loyalty_number)
    EditText loyaltyNumber;
    //    @BindView(R.id.subtotal)
//    TextView subtotal;
    @BindView(R.id.mukafaa_number)
    ConstraintLayout mukafaaNumbers;
    @BindView(R.id.mukafaa_layout)
    LinearLayout mukafaaLayout;
    @BindView(R.id.mukafaa_label)
    TextView mukafaaLabel;
    @BindView(R.id.mukafaa_remove)
    TextView mukafaaRemove;
    @BindView(R.id.mukafaa_sar)
    TextView mukafaaSar;
    @BindView(R.id.request_otp)
    MaterialButton requestOTP;
    @BindView(R.id.rvServiceCharge)
    RecyclerView rvServiceCharge;

    @BindView(R.id.img_branch)
    ImageView imgBranch;
    @BindView(R.id.order_type_label_2)
    TextView orderTypeLabel2;
    @BindView(R.id.branchvalue)
    TextView branchvalue2;
    @BindView(R.id.branch_image_container)
    MaterialCardView branchImageContainer;
    @BindView(R.id.branch_container)
    MaterialCardView branchContainer;
    @BindView(R.id.order_type_container)
    MaterialCardView orderTypeContainer;
    @BindView(R.id.card_mukafaa_container)
    ConstraintLayout cardMukafaaContainer;

    @BindView(R.id.payment_type_container)
    LinearLayout paymentTypeContainer;

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
    private OrderSummaryMenuItemRecyclerViewAdapter menuItemsRecyclerViewAdapter;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_new);
        ButterKnife.bind(this);
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
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
    }

    @Override
    public void setupViews() {
        menuItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        menuItemsRecyclerViewAdapter = new OrderSummaryMenuItemRecyclerViewAdapter(paymentPresenter);
        menuItemsRecyclerView.setAdapter(menuItemsRecyclerViewAdapter);
        Store orderStore = CacheUtil.getInstance().getOrderStore();
        if (orderStore != null && orderStore.getAttributes() != null) {
            String branchImage = orderStore.getAttributes().getImage();
            if (branchImage != null) {
                isBranchImageAvailable = !branchImage.isEmpty();
            }
        }
        if (isScheduled) {
            schedLayout.setVisibility(View.VISIBLE);
            scheduleText.setText(TimeUtil.properSchedDate(goodDaySelected + " " + goodSelectedDateString));
        } else {
            schedLayout.setVisibility(View.GONE);
        }

        String tableDineInNumber = CacheUtil.getInstance().getDineInTableNumber();
        if (tableDineInNumber != null && !tableDineInNumber.isEmpty()) {
            tableNumberLayout.setVisibility(View.VISIBLE);
            tableNumber.setText(tableDineInNumber);
        }

        back.setVisibility(View.VISIBLE);
        cart.setVisibility(View.GONE);

        slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        if (CacheUtil.getInstance().getOrderStore() != null) {
            paymentPresenter.validateOrder(false, CacheUtil.getInstance().getOrderStore(), null, 0, false);
        }

        orderType = CacheUtil.getInstance().getOrderType();

        if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
            /*showLoadingDialog();
            Geocoder geocoder = new Geocoder(PaymentActivity.this, LocaleUtil.getInstance().isEnglish() ? new Locale("en", "us") : new Locale("ar", "sa"));
            paymentPresenter.getStore(geocoder);*/
            if (CacheUtil.getInstance().getOrderAddress() != null) {
                orderTypeLocation.setText(CacheUtil.getInstance().getOrderAddress().getAttributes().getLine1());
            }
        } else {
            /*Geocoder geocoder = new Geocoder(PaymentActivity.this, LocaleUtil.getInstance().isEnglish() ? new Locale("en", "us") : new Locale("ar", "sa"));
            paymentPresenter.getAddresses(geocoder);*/
            if (CacheUtil.getInstance().getOrderStore() != null) {
                orderTypeLocation.setText(CacheUtil.getInstance().getOrderStore().getAttributes().getName());
            }
        }

        switch (CacheUtil.getInstance().getOrderType()) {
            case PICKUP:
                orderTypeLabel.setText(CacheUtil.getInstance().getTranslations(Text.PICKUP));
                break;
            case DINE_IN:
                orderTypeLabel.setText(CacheUtil.getInstance().getTranslations(Text.DINE_IN));
                break;
            case DELIVERY:
                orderTypeLabel.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERY));
                branchLabel.setText(CacheUtil.getInstance().getTranslations(Text.ADDRESS, "ADDRESS").toUpperCase());
                break;
            case CURBSIDE:
                orderTypeLabel.setText(CacheUtil.getInstance().getTranslations(Text.CURBSIDE_PICKUP, "Curbside"));
                break;
        }

        removeSched.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRemove = true;
                schedTime.setText("");
                if (CacheUtil.getInstance().getTranslations(Text.PLACE_ORDER_NOW, "PLACE ORDER NOW").equalsIgnoreCase("CHOOSE METHOD")) {
                    placeOrder.setText("PLACE ORDER NOW");
                } else {
                    placeOrder.setText(CacheUtil.getInstance().getTranslations(Text.PLACE_ORDER_NOW, "PLACE ORDER NOW"));
                }
                removeLayout.setVisibility(View.GONE);
                removeLabel.setVisibility(View.GONE);
                noSchedLayout.setVisibility(View.VISIBLE);
                spacingTop.setVisibility(View.GONE);
                withSchedLayout.setVisibility(View.GONE);
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
            }
        });

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                try {
                    Calendar originalCalendar = Calendar.getInstance();
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.ENGLISH);
                    calendar.setTime(sdf.parse(daySelected + " " + TimeUtil.dateToHourOnly(selectedDate)));
                    paymentPresenter.scheduleOrder(originalCalendar, calendar, true);
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
        });

        placeOrder.setOnClickListener(view -> {
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
                Toast.makeText(PaymentActivity.this, CacheUtil.getInstance().getTranslations(Text.SELECT_PAYMENT_METHOD), Toast.LENGTH_SHORT).show();
            }
        });

        loyaltyPointsPayment.setOnClickListener(view -> {
            if (!loyaltyPointsPayment.isSelected()) {
                loyaltyPointsPayment.setSelected(true);
                loyaltyPointsPaymentContainer.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
                pointsLayout.setVisibility(View.VISIBLE);
            } else {
                loyaltyPointsPayment.setSelected(false);
                loyaltyPointsPaymentContainer.setStrokeColor(Color.TRANSPARENT);
                pointsLayout.setVisibility(View.GONE);
                paymentPresenter.setLoyaltyPointsUsed(0);
                etPointsInput.setText("");
                loyaltypointsInput = 0;
                paymentPresenter.validateOrder(false, CacheUtil.getInstance().getOrderStore(), null, 0,false);
            }
        });

        pointsLayout.setOnClickListener(v -> {
        });

        Loyalty loyalty = AuthUtil.getInstance().getCustomerLoyalty();
        if (loyalty.getAttributes() != null) {
            firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String loyaltySettings = firebaseRemoteConfig.getString("loyalty_settings");

                    if (loyaltySettings != null && loyaltySettings.equals("full")) {
                        if (loyalty.getAttributes().getCurrentBalanceAmount() >= paymentPresenter.getMenuItemsTotalPrice()) {
                            loyaltyPointsPayment.setEnabled(true);
                            loyaltyPointsPaymentContainer.setEnabled(true);
                        } else {
                            loyaltyPointsPayment.setEnabled(false);
                            loyaltyPointsPaymentContainer.setEnabled(false);
                            loyaltyPointsPaymentContainer.setCardBackgroundColor(Color.parseColor("#d5d5d5"));
                        }
                    } else {
                        loyaltyPointsPayment.setEnabled(true);
                        loyaltyPointsPaymentContainer.setEnabled(true);
                    }
                }
            });

            if ((int) loyalty.getAttributes().getCurrentBalanceAmount() == loyalty.getAttributes().getCurrentBalanceAmount()) {
                loyaltyLabel.setText(CacheUtil.getInstance().getTranslations(Text.LOYALTY_POINTS_PAYMENT_LABEL, "(x) Points ((y))").replace("(x)", String.valueOf(AuthUtil.getInstance().getCustomerLoyalty().getAttributes().getCurrentBalance())).replace("(y)", CurrencyUtils.Companion.updateCurrency(loyalty.getAttributes().getCurrentBalanceAmount(), true)));
                //loyaltyLabel.setText(StringUtil.localize(String.valueOf(AuthUtil.getInstance().getCustomerLoyalty().getAttributes().getCurrentBalance()))+" Points ("+StringUtil.localize(String.valueOf(loyalty.getAttributes().getCurrentBalanceAmount())) +")");
            } else {
                loyaltyLabel.setText(CacheUtil.getInstance().getTranslations(Text.LOYALTY_POINTS_PAYMENT_LABEL, "(x) Points ((y))").replace("(x)", StringUtil.getFormattedAmount(AuthUtil.getInstance().getCustomerLoyalty().getAttributes().getCurrentBalance())).replace("(y)", CurrencyUtils.Companion.updateCurrency(loyalty.getAttributes().getCurrentBalanceAmount(), true)));
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

                    etPointsInput.removeTextChangedListener(pointsInSarTextWatcher);
                    if (userSetPoints >= currentBalance) {
                        btnUseAll.performClick();
                    } else {
                        etPointsInput.setText(StringUtil.localize(etPointsInput.getText().toString()));
                    }


                    etPointsInput.setSelection(etPointsInput.getText().toString().length());
                    etPointsInput.addTextChangedListener(pointsInSarTextWatcher);
                    //showLoadingDialog();
                    loyaltypointsInput = Integer.parseInt(etPointsInput.getText().toString());
                    paymentPresenter.validateOrder(false, CacheUtil.getInstance().getOrderStore(), null, loyaltypointsInput, false);
                } else {
                    etPointsInput.setSelection(0);
                    paymentPresenter.setLoyaltyPointsUsed(0);
                }
            });
        }, onError -> {

        }));

        etPointsInput.addTextChangedListener(pointsInSarTextWatcher);

        noSchedLayout.setOnClickListener(view -> {
            showPanel();
        });

        withSchedLayout.setOnClickListener(view -> {
            showPanel();
        });

        boolean isDelivery = false;
        Customer customer = AuthUtil.getInstance().getCustomer();
        boolean isSignedIn = AuthUtil.getInstance().isSignedIn();
        boolean isFullSignedIn = (isSignedIn && !customer.getAttributes().getAccountType().equalsIgnoreCase("guest"));

        switch (CacheUtil.getInstance().getOrderType()) {
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
        }

        try {
            Set<PaymentType> paymentTypes = CacheUtil.getInstance().getApplication().getAttributes().getDeliverPaymentTypes();
            if (!isDelivery) {
                paymentTypes = CacheUtil.getInstance().getApplication().getAttributes().getPickupPaymentTypes();
                branch.setText(CacheUtil.getInstance().getOrderStore().getAttributes().getName());
                branchvalue2.setText(CacheUtil.getInstance().getOrderStore().getAttributes().getName());
            } else {
                branch.setText(CacheUtil.getInstance().getOrderAddress().getAttributes().getLine1());
                branchvalue2.setText(CacheUtil.getInstance().getOrderAddress().getAttributes().getLine1());
            }

            if (isFullSignedIn && paymentTypes.contains(PaymentType.LOYALTY) && passedTotalFromCart > 0) {
                loyaltyPointsPaymentContainer.setVisibility(View.VISIBLE);
                useLoyaltyPointsLabel.setVisibility(View.VISIBLE);
                loyaltyPointsAdditionalInfo.setVisibility(View.VISIBLE);
            }

            branchImageContainer.setVisibility(View.VISIBLE);
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //total.setText(StringUtil.getFormattedPrice(paymentPresenter.getTotalPrice()));

        Concept concept = CacheUtil.getInstance().getConcept();
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
                            Intent intent = new Intent(PaymentActivity.this, BrowserActivity.class);
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
        }

        mukafaaRemove.setOnClickListener(view -> {
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
        }


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OTP_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                String transactionId = data.getStringExtra("transaction_id");
                String alrajhiAmount = data.getStringExtra("alrajhi_amount");
                paymentPresenter.setMukafaaTransactionId(transactionId);
                paymentPresenter.validateOrder(false, CacheUtil.getInstance().getOrderStore(), null, loyaltypointsInput,false);
                mukafaaPayment.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
                mukafaaNumbers.setVisibility(View.GONE);
                requestOTP.setVisibility(View.GONE);
                placeOrder.setVisibility(View.VISIBLE);
                if (CacheUtil.getInstance().getOrderPayment() == PaymentType.NONE &&
                        paymentPresenter.getValidatedTotal() == Float.parseFloat(alrajhiAmount) &&
                        loyaltypointsInput <= 0 &&
                        CacheUtil.getInstance().getOrderType() != OrderType.DELIVERY) {
                    cashPayment.performClick();
                    placeOrder.performClick();
                }
            }
        }
    }

    private String getNumber() {
        String pnum = phoneNumber.getText().toString();
        String lnum = loyaltyNumber.getText().toString();
        if (pnum != null && !pnum.isEmpty()) {
            return StringUtil.toEnglish(pnum);
        } else if (lnum != null && !lnum.isEmpty()) {
            return StringUtil.toEnglish(lnum);
        }
        return null;
    }

    private void selectMukafaa(boolean show, boolean isButtonClicked) {
        mukafaaPayment.setStrokeColor(show ? Color.parseColor(CacheUtil.getInstance().getColorMain()) : Color.TRANSPARENT);
        placeOrder.setVisibility(show ? View.GONE : View.VISIBLE);
        requestOTP.setVisibility(show ? View.VISIBLE : View.GONE);
        mukafaaNumbers.setVisibility(show ? View.VISIBLE : View.GONE);

        if (!paymentPresenter.hasMukafaaTransactionId()) {
            if (isCashPaymentAvailable && isButtonClicked)
                cashPayment.setStrokeColor(Color.TRANSPARENT);
            if (isCODPaymentAvailable && isButtonClicked)
                cardOrderTypePayment.setStrokeColor(Color.TRANSPARENT);
            if (isCardPaymentAvailable && isButtonClicked)
                cardPayment.setStrokeColor(Color.TRANSPARENT);
        }
    }

    private void showPanel() {
        if (CacheUtil.getInstance().getOrderPayment() == PaymentType.NONE) {
            Toast.makeText(PaymentActivity.this, CacheUtil.getInstance().getTranslations(Text.SELECT_PAYMENT_METHOD), Toast.LENGTH_SHORT).show();
        } else {
            if (isRemove) {
                widget.setSelectedDate(LocalDate.now());
                setUpDatePicker();
            }
            slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        }
    }

    private void setUpDatePicker() {

        Calendar c = new GregorianCalendar();
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        maxDate = c.getTime();
        Calendar minTime = Calendar.getInstance();
        minDate = minTime.getTime();

        if (selectedDate == null) {
            selectedDate = minDate;
        }

        datePicker.addOnDateChangedListener((displayed, date) -> {
            selectedDate = datePicker.getDate();
        });

        datePicker.setCustomLocale(new Locale(LocaleUtil.getInstance().getLanguage()));
        datePicker.setIsAmPm(true);
        datePicker.setDisplayDays(false);
        datePicker.setSelectorColor(android.R.color.white);
        datePicker.setDisplayYears(false);
        datePicker.setDefaultDate(minDate);
        datePicker.setTextSize(getResources().getDimensionPixelSize(R.dimen.font_size_16sp));
        datePicker.setDisplayMonths(false);
        datePicker.setTextColor(this.getResources().getColor(R.color.silver_chalice));
        datePicker.setSelectedTextColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
        datePicker.setTypeface(FontHandler.getMediumTypeface());
//        datePicker.setMaxDate(maxDate);
//        datePicker.setMinDate(minDate);

    }

    @Override
    public void setupFonts() {
    }

    @Override
    public void setupTranslations() {
        back.setText(CacheUtil.getInstance().getTranslations(Text.BACK, "Back"));
        title.setText(CacheUtil.getInstance().getTranslations(Text.ORDER_SUMMARY, "ORDER SUMMARY").toUpperCase());
        chooseMethodLabel.setText(CacheUtil.getInstance().getTranslations(Text.CHOOSE_METHOD, "CHOOSE METHOD"));
        if (cashLabel != null)
            cashLabel.setText(CacheUtil.getInstance().getTranslations(Text.CASH));
        pickADayLabel.setText(CacheUtil.getInstance().getTranslations(Text.PICK_A_DAY, "PICK A DAY").toUpperCase());
        pickATimeLabel.setText(CacheUtil.getInstance().getTranslations(Text.PICK_A_TIME, "PICK A TIME").toUpperCase());
        cancelButton.setText(CacheUtil.getInstance().getTranslations(Text.CANCEL, "CANCEL").toUpperCase());
        confirmButton.setText(CacheUtil.getInstance().getTranslations(Text.CONFIRM_CAPS, "CONFIRM").toUpperCase());
        if (CacheUtil.getInstance().getTranslations(Text.PLACE_ORDER_NOW, "PLACE ORDER NOW").equalsIgnoreCase("CHOOSE METHOD")) {
            placeOrder.setText("PLACE ORDER NOW");
        } else {
            placeOrder.setText(CacheUtil.getInstance().getTranslations(Text.PLACE_ORDER_NOW, "PLACE ORDER NOW"));
        }
        schedOrderLabel.setText(CacheUtil.getInstance().getTranslations(Text.SCHEDULER_ORDER));
        slidingTitle.setText(CacheUtil.getInstance().getTranslations(Text.SCHEDULER_ORDER).toUpperCase());
        removeLabel.setText(CacheUtil.getInstance().getTranslations(Text.YOU_CAN_REMOVE_THE_SCHEDULE, "You can remove the schedule by pressing on the “x” button here"));
        back.setText(CacheUtil.getInstance().getTranslations(Text.BACK, "Back"));
        itemsLabel.setText(CacheUtil.getInstance().getTranslations(Text.ORDERED_ITEMS_CAPS, "ORDERED ITEMS"));
        itemsQuantityLabel.setText(CacheUtil.getInstance().getTranslations(Text.ITEMS_QUANTITY_CAPS, "ITEMS & QUANTITY"));
        priceLabel.setText(CacheUtil.getInstance().getTranslations(Text.PRICE_CAPS, "PRICE"));
        subtotalLabel.setText(CacheUtil.getInstance().getTranslations(Text.SUBTOTAL));
        discountLabel.setText(CacheUtil.getInstance().getTranslations(Text.PROMO_DISCOUNT));
        deliveryLabel.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERY));
//        taxLabel.setText(CacheUtil.getInstance().getTranslations(Text.TAX));
        totalLabel.setText(CacheUtil.getInstance().getTranslations(Text.TOTAL_LABEL));
        schedLabel.setText(CacheUtil.getInstance().getTranslations(Text.SCHEDULE, "SCHEDULE").toUpperCase());
        branchLabel.setText(CacheUtil.getInstance().getTranslations(Text.BRANCH_CAPS, "BRANCH"));
        typeLabel.setText(CacheUtil.getInstance().getTranslations(Text.ORDER_TYPE_CAPS, "ORDER TYPE"));
        financialLabel.setText(CacheUtil.getInstance().getTranslations(Text.VAT_NO_CAPS, "VAT NO."));
        availableLabel.setText(CacheUtil.getInstance().getTranslations(Text.AVAILABLE_LOYALTY_POINTS, "Available Loyalty Points"));
        btnUseAll.setText(CacheUtil.getInstance().getTranslations(Text.USE_ALL_CAPS, "USE ALL"));
        howMuchLabel.setText(CacheUtil.getInstance().getTranslations(Text.REDEEM_AMOUNT_IN, "REDEEM AMOUNT IN (X)").replace("(X)",CacheUtil.getInstance().getTranslations(CacheUtil.getInstance().getConcept().getCurrencyCode())));
        etPointsInput.setHint(CacheUtil.getInstance().getTranslations(Text.LOYALTY_USE_AMOUNT_INPUT_HINT, "Enter amount here"));
        loyaltyPointsAdditionalInfo.setText(CacheUtil.getInstance().getTranslations(Text.LOYALTY_LEAVE_BLANK, "Leave this blank if you don't wanna use your points"));
        useLoyaltyPointsLabel.setText(CacheUtil.getInstance().getTranslations(Text.LOYALTY_USE_POINTS, "USE LOYALTY POINTS"));
        tableNumberLabel.setText(CacheUtil.getInstance().getTranslations(Text.TABLE_NUMBER_CAPS, "TABLE NUMBER"));
        requestOTP.setText(CacheUtil.getInstance().getTranslations(Text.REQUEST_OTP, "REQUEST OTP"));
        cardTvMukafaaBank.setText(CacheUtil.getInstance().getTranslations(Text.MUKAFAA_BANK, "Al Rajhi Bank"));
        cardTvMukafaaRewards.setText(CacheUtil.getInstance().getTranslations(Text.MUKAFAA_REWARDS, "(Mokafa’a Rewards)"));
        disclaimer.setText(Html.fromHtml(CacheUtil.getInstance().getTranslations(Text.MUKAFAA_DISCLAIMER, "<b>Disclaimer</b>: The user has to be a registered mobile number with Mokafa'a Al Rajhi Program, otherwise the redemption will not be successful.")));
        phoneNumberLbl.setText(CacheUtil.getInstance().getTranslations(Text.MOBILE_NUMBER, "Mobile Number"));
        or.setText(CacheUtil.getInstance().getTranslations(Text.OR, "OR"));
        loyaltyNumberLbl.setText(CacheUtil.getInstance().getTranslations(Text.LOYALTY_CIC_NUMBER, "Loyalty CIC Number"));
        itemsLabel.setText(CacheUtil.getInstance().getTranslations(Text.ORDERED_ITEMS_CAPS, "ORDERED ITEMS"));
        itemsQuantityLabel.setText(CacheUtil.getInstance().getTranslations(Text.ITEMS_QUANTITY_CAPS, "ITEMS & QUANTITY"));
        priceLabel.setText(CacheUtil.getInstance().getTranslations(Text.PRICE_CAPS, "PRICE"));
        subtotalLabel.setText(CacheUtil.getInstance().getTranslations(Text.SUBTOTAL));
        discountLabel.setText(CacheUtil.getInstance().getTranslations(Text.PROMO_DISCOUNT));
        mukafaaLabel.setText(CacheUtil.getInstance().getTranslations(Text.AL_RAJHI_MOKAFAA, "Al Rajhi Mokafa'a"));
        mukafaaRemove.setText(CacheUtil.getInstance().getTranslations(Text.REMOVE_CAPS, "Remove"));
        deliveryLabel.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERY));
//        taxLabel.setText(CacheUtil.getInstance().getTranslations(Text.TAX_RATE, "(x)% VAT").replace("(x)", StringUtil.localize(String.valueOf(CacheUtil.getInstance().getConcept().getVatRate()))));
//        summaryTotalLabel.setText(CacheUtil.getInstance().getTranslations(Text.TOTAL));
    }

    @Override
    public void showPaymentTypes(Set<PaymentType> paymentTypes) {

    }

    @Override
    public void showPaymentTypes(ArrayList<PaymentType> paymentTypes) {
        chooseMethodLabel.setVisibility(View.VISIBLE);
        mukafaaPayment.setVisibility(paymentTypes.contains(PaymentType.ALRAJHI) ? View.VISIBLE : View.GONE);

        if (!isPaymentSet) {
            isPaymentSet = true;
            PaymentType previousPaymentType = CacheUtil.getInstance().getOrderPayment();
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            for (PaymentType paymentType : paymentTypes) {
                if (paymentType == PaymentType.CASH) {

                    isCashPaymentAvailable = true;
                    View view = inflater.inflate(R.layout.item_payment_type_cash, null);
                    paymentTypeContainer.addView(view);
                    cashLabel = view.findViewById(R.id.cash_label);
                    cashPayment = view.findViewById(R.id.cash_payment);
                    cashPayment.setOnClickListener(v -> selectPaymentTypeCash());
                } else if (paymentType == PaymentType.CARD) {
                    isCardPaymentAvailable = true;
                    View view = inflater.inflate(R.layout.item_payment_type_card, null);
                    cardName = view.findViewById(R.id.card_name);
                    setupCardImagesAndLabel(
                            view.findViewById(R.id.card_mada),
                            view.findViewById(R.id.card_visa),
                            view.findViewById(R.id.card_master_card),
                            cardName
                    );

                    paymentTypeContainer.addView(view);
                    cardPayment = view.findViewById(R.id.card_payment);

                    cardPayment.setOnClickListener(v -> {
                        CacheUtil.getInstance().setOrderPayment(PaymentType.CARD);
                        if (isCashPaymentAvailable) cashPayment.setStrokeColor(Color.TRANSPARENT);
                        if (isCODPaymentAvailable)
                            cardOrderTypePayment.setStrokeColor(Color.TRANSPARENT);
                        cardPayment.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
                        paymentPresenter.setCashPresented(0);

                        if (paymentPresenter.hasMukafaaTransactionId()) {
                            mukafaaPayment.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
                        } else {
                            mukafaaPayment.setStrokeColor(Color.TRANSPARENT);
                            mukafaaNumbers.setVisibility(View.GONE);
                            mukafaaSelected = false;
                            requestOTP.setVisibility(View.GONE);
                            placeOrder.setVisibility(View.VISIBLE);
                        }
                    });
                } else if (paymentType == PaymentType.CARD_ON_DELIVERY || paymentType == PaymentType.CARD_ON_PICKUP) {
                    isCODPaymentAvailable = true;
                    View view = inflater.inflate(R.layout.item_payment_type_cod_cop, null);
                    setupCardImagesAndLabel(
                            view.findViewById(R.id.card_on_mada),
                            view.findViewById(R.id.card_on_visa),
                            view.findViewById(R.id.card_on_master_card),
                            null
                    );
                    paymentTypeContainer.addView(view);
                    cardOnName = view.findViewById(R.id.card_on_name);
                    cardOrderTypePayment = view.findViewById(R.id.card_order_type_payment);
                    cardOnName.setText(CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY ? CacheUtil.getInstance().getTranslations(Text.CARD_ON_DELIVERY) : CacheUtil.getInstance().getTranslations(Text.CARD_ON_PICKUP));
                    cardOrderTypePayment.setOnClickListener(v -> {
                        if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
                            CacheUtil.getInstance().setOrderPayment(PaymentType.CARD_ON_DELIVERY);
                        } else {
                            CacheUtil.getInstance().setOrderPayment(PaymentType.CARD_ON_PICKUP);
                        }
                        cardOrderTypePayment.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
                        if (isCashPaymentAvailable) cashPayment.setStrokeColor(Color.TRANSPARENT);
                        if (isCardPaymentAvailable) cardPayment.setStrokeColor(Color.TRANSPARENT);
                        paymentPresenter.setCashPresented(0);

                        if (paymentPresenter.hasMukafaaTransactionId()) {
                            mukafaaPayment.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
                        } else {
                            mukafaaPayment.setStrokeColor(Color.TRANSPARENT);
                            mukafaaNumbers.setVisibility(View.GONE);
                            mukafaaSelected = false;
                            requestOTP.setVisibility(View.GONE);
                            placeOrder.setVisibility(View.VISIBLE);
                        }
                    });
                }

            }

            boolean isDelivery = false;
            Customer customer = AuthUtil.getInstance().getCustomer();
            boolean isSignedIn = AuthUtil.getInstance().isSignedIn();
            boolean isFullSignedIn = (isSignedIn && !customer.getAttributes().getAccountType().equalsIgnoreCase("guest"));

            if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) isDelivery = true;

            try {
                if (isDelivery) {
                    if (isFullSignedIn && CacheUtil.getInstance().getApplication().getAttributes().getDeliverPaymentTypes().contains(PaymentType.LOYALTY) && passedTotalFromCart > 0) {
                        loyaltyPointsPaymentContainer.setVisibility(View.VISIBLE);
                        useLoyaltyPointsLabel.setVisibility(View.VISIBLE);
                        loyaltyPointsAdditionalInfo.setVisibility(View.VISIBLE);
                    }

                    branch.setText(CacheUtil.getInstance().getOrderAddress().getAttributes().getLine1());
                } else {
                    if (isFullSignedIn && CacheUtil.getInstance().getApplication().getAttributes().getPickupPaymentTypes().contains(PaymentType.LOYALTY) && passedTotalFromCart > 0) {
                        loyaltyPointsPaymentContainer.setVisibility(View.VISIBLE);
                        useLoyaltyPointsLabel.setVisibility(View.VISIBLE);
                        loyaltyPointsAdditionalInfo.setVisibility(View.VISIBLE);
                    }

                    branch.setText(CacheUtil.getInstance().getOrderStore().getAttributes().getName());

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (paymentPresenter.hasMukafaaTransactionId()) {
                if (!mukafaaSelected) {
                    mukafaaSelected = true;
                    mukafaaPayment.setStrokeColor(R.attr.colorPrimary);
                    mukafaaNumbers.setVisibility(View.GONE);

                    requestOTP.setVisibility(View.GONE);
                    placeOrder.setVisibility(View.VISIBLE);
                }
            }

            if (cashPayment != null && previousPaymentType == PaymentType.CASH) {
                cashPayment.performClick();
            } else if (cardPayment != null && previousPaymentType == PaymentType.CARD) {
                cardPayment.performClick();
            } else if (previousPaymentType == PaymentType.CARD_ON_DELIVERY || previousPaymentType == PaymentType.CARD_ON_PICKUP) {
                cardOrderTypePayment.performClick();
            }

            setupTranslations();
        }
    }

    private void setupCardImagesAndLabel(ImageView mada, ImageView visa, ImageView mastercard, @Nullable TextView label) {
        boolean madaOnly = false;
        if (CacheUtil.getInstance().getSupportCardTypes().isEmpty()) {
            mada.setVisibility(View.VISIBLE);
            visa.setVisibility(View.VISIBLE);
            mastercard.setVisibility(View.VISIBLE);
        } else {
            for (String supportCardType : CacheUtil.getInstance().getSupportCardTypes()) {
                if (supportCardType.equalsIgnoreCase("mada")) {
                    madaOnly = CacheUtil.getInstance().getSupportCardTypes().size() == 1;
                    mada.setVisibility(View.VISIBLE);
                } else if (supportCardType.equalsIgnoreCase("visa")) {
                    madaOnly = false;
                    visa.setVisibility(View.VISIBLE);
                } else if (supportCardType.equalsIgnoreCase("mastercard")) {
                    madaOnly = false;
                    mastercard.setVisibility(View.VISIBLE);
                }
            }
        }
        if (label != null) {
            if (madaOnly) {
                label.setText(CacheUtil.getInstance().getTranslations(Text.MADA_BANK_CARD, "mada bank Card"));
            } else {
                label.setText(CacheUtil.getInstance().getTranslations(Text.CREDIT_CARD_MADA_BANK_CARD));
            }
        }
    }

    @Override
    public void setStartName(String startNameValue) {
        //startName.setText(startNameValue);
    }

    @Override
    public void setStartAddress(String startAddressValue) {
        // startAddress.setText(startAddressValue);
    }

    @Override
    public void setEndName(String endNameValue) {
        //endName.setText(endNameValue);
    }

    @Override
    public void setEndAddress(String endAddressValue) {
        orderTypeLocation.setText(endAddressValue);
    }

    @Override
    public void updateMap(OrderType orderType, LatLng startLocation, LatLng endLocation, LatLngBounds bounds) {
        dismissDialogs();
    }

    @Override
    public void updateTime(Calendar calendar) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("h:mm", Locale.ENGLISH);
        // time.setText(StringUtil.localize(simpleDateFormat.format(calendar.getTime())) + " " + CacheUtil.getInstance().getTranslations(calendar.get(Calendar.AM_PM) == Calendar.AM ? Text.AM : Text.PM));
        /*if(calendar.get(Calendar.HOUR) == 0) {
            hour.setText(StringUtil.localize("12"));
        } else {
            hour.setText(StringUtil.localize(String.valueOf(calendar.get(Calendar.HOUR))));
        }
        int minuteValue = calendar.get(Calendar.MINUTE);
        if(minuteValue >= 10) {
            minute.setText(StringUtil.localize(String.valueOf(calendar.get(Calendar.MINUTE))));
        } else {
            minute.setText(StringUtil.localize("0" + calendar.get(Calendar.MINUTE)));
        }
        amPm.setText(CacheUtil.getInstance().getTranslations(calendar.get(Calendar.AM_PM) == Calendar.AM ? Text.AM : Text.PM));*/
    }

    @Override
    public void selectPaymentTypeCash() {
        CacheUtil.getInstance().setOrderPayment(PaymentType.CASH);
        cashPayment.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));

        if (isCODPaymentAvailable) cardOrderTypePayment.setStrokeColor(Color.TRANSPARENT);
        if (isCardPaymentAvailable) cardPayment.setStrokeColor(Color.TRANSPARENT);

        if (paymentPresenter.hasMukafaaTransactionId()) {
            mukafaaPayment.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
        } else {
            mukafaaPayment.setStrokeColor(Color.TRANSPARENT);
            mukafaaSelected = false;
            mukafaaNumbers.setVisibility(View.GONE);
            requestOTP.setVisibility(View.GONE);
            placeOrder.setVisibility(View.VISIBLE);
        }
        if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY && paymentPresenter.getValidatedTotal() != 0) {
            PaymentDialogFragment paymentDialogFragment = PaymentDialogFragment.newInstance(paymentPresenter, new PaymentDialogFragment.OnSubmit() {
                @Override
                public void onSubmit(Dialog dialog, double cashPresented) {
                    dialog.dismiss();
                    paymentPresenter.setCashPresented(cashPresented);
                }
            });
            paymentDialogFragment.show(getSupportFragmentManager(), PaymentDialogFragment.class.getSimpleName());
        }
    }

    @Override
    public void showValidateOrderError(List<String> errors) {
        dismissDialogs();
        StringBuilder builder = new StringBuilder();
        builder.append("");
        for (int i = 0; i < errors.size(); i++) {
            builder.append("- ");
            builder.append(errors.get(i));
            if ((i + 1) != errors.size()) {
                builder.append("\n\n");
            }
        }
        showAlertDialog("", builder.toString());
    }

    @Override
    public void sendOrder() {
        slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
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

    @Override
    public void showCheckoutPaymentForm(String orderId) {

        /*Intent intent = new Intent(PaymentActivity.this, CustomCheckoutActivity.class);
        intent.putExtra(CustomCheckoutActivity.EXTRA_ORDER_ID , orderId);
        intent.putExtra(CustomCheckoutActivity.EXTRA_PAYMENT_PROVIDER, paymentPresenter.getPaymentProvider());
        checkoutActivityLauncher.launch(intent);
        isFromCheckout = true;*/
        if (isCanProceedFragmentTransaction(getSupportFragmentManager())) {

            dialogCheckout = new CustomCheckoutDialog(this);
            dialogCheckout.setOnActionSelectedListener(new CustomCheckoutDialog.OnActionSelectedListener() {
                @Override
                public void onFormSubmit() {
                    showProgressDialog(CacheUtil.getInstance().getTranslations(Text.LOADING));
                }

                @Override
                public void onTokenGenerated(@Nullable TokenDetails response) {
                    //dialogCheckout.clear();
                    showLoadingDialog();
                    saveDialogAlert = SaveDialogAlert.newInstance(new SaveDialogAlert.OnSavedAction() {
                        @Override
                        public void onClose() {
                            saveDialogAlert.dismissAllowingStateLoss();
                            checkTokenGenerated(response.getToken(), null, false, orderId);
                        }

                        @Override
                        public void onProceed() {
                            saveDialogAlert.dismissAllowingStateLoss();
                            checkTokenGenerated(response.getToken(), null, true, orderId);
                        }
                    });
                    saveDialogAlert.setCancelable(false);
                    saveDialogAlert.show(getSupportFragmentManager(), SaveDialogAlert.class.toString());
                }

                @Override
                public void onError(@Nullable String error) {
                    String reason = "CardTokenisationFail";
                    if (error != null) {
                        reason = error;
                    }
                    ShipBookLogger.logError(BuildConfig.TAG + " - Order ID: " + orderId + " -> " + paymentPresenter.getPaymentProvider() + " Card Payment Failed (Reason: " + reason + ")");
                    ApiLogUtil.sendOrderLog(ApiLogLevel.ERROR, orderId, paymentPresenter.getPaymentProvider(), "Card Payment Failed (Reason: " + reason + ")");
                    dismissDialogs();
                }

                @Override
                public void on3DSSuccess() {
                    ShipBookLogger.logInfo(BuildConfig.TAG + " - Order ID: " + orderId + " -> " + paymentPresenter.getPaymentProvider() + " Card Payment Success");
                    paymentPresenter.sendUpsells();
                    dialogCheckout.dismissAllowingStateLoss();
                }

                @Override
                public void on3DSFailure(String error) {
                    ShipBookLogger.logError(BuildConfig.TAG + " - Order ID: " + orderId + " -> " + paymentPresenter.getPaymentProvider() + " Card Payment Failed (Reason: on3DSFailure " + error + ")");
                    ApiLogUtil.sendOrderLog(ApiLogLevel.ERROR, orderId, paymentPresenter.getPaymentProvider(), "Card Payment Failed (Reason: on3DSFailure " + error + ")");
                    showCardFailed();
                }

                @Override
                public void onBackPressed() {
                    ApiLogUtil.sendOrderLog(ApiLogLevel.INFO, orderId, paymentPresenter.getPaymentProvider(), "Card Payment onBackPressed");
                    ApiLogUtil.sendOrderLog(ApiLogLevel.WARNING, orderId, paymentPresenter.getPaymentProvider(), "Card Payment onBackPressed");
                    dialogCheckout.dismissAllowingStateLoss();
                }
            });
        }

      /*  if (dialogCheckout.getDialog().getWindow() != null) {
            dialogCheckout.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }*/

        dismissDialogs();
        dialogCheckout.show(getSupportFragmentManager(), CustomCheckoutDialog.class.getSimpleName());

    }

    @Override
    public void initializeTap(String orderId, double total) {

        if (!BuildConfig.BUILD_TYPE.equalsIgnoreCase("release")) {
            GoSellSDK.init(this, BuildUtil.tapTest(), getPackageName());
        } else {
            GoSellSDK.init(this, BuildUtil.tapLive(), getPackageName());
        }

        GoSellSDK.setLocale(LocaleUtil.getInstance().getLanguageCode());

        ThemeObject.getInstance().setAppearanceMode(AppearanceMode.FULLSCREEN_MODE).setSdkLanguage(LocaleUtil.getInstance().getLanguageCode()).setHeaderFont(FontHandler.getMediumTypeface()).setHeaderTextColor(ContextCompat.getColor(this, R.color.white)).setHeaderTextSize(18).setHeaderBackgroundColor(ViewUtil.getColorMain(this)).setCardInputTextColor(ContextCompat.getColor(this, R.color.black)).setCardInputInvalidTextColor(ContextCompat.getColor(this, R.color.red)).setCardInputPlaceholderTextColor(ContextCompat.getColor(this, R.color.gray)).setSaveCardSwitchOffThumbTint(ContextCompat.getColor(this, R.color.french_gray_new)).setSaveCardSwitchOnThumbTint(ContextCompat.getColor(this, R.color.vibrant_green)).setSaveCardSwitchOffTrackTint(ContextCompat.getColor(this, R.color.french_gray)).setSaveCardSwitchOnTrackTint(ContextCompat.getColor(this, R.color.vibrant_green_pressed)).setScanIconDrawable(ContextCompat.getDrawable(this, R.drawable.btn_card_scanner_normal)).setPayButtonResourceId(R.drawable.btn_pay_selector).setPayButtonFont(FontHandler.getRegularTypeface()).setPayButtonDisabledTitleColor(ContextCompat.getColor(this, R.color.white)).setPayButtonEnabledTitleColor(ContextCompat.getColor(this, R.color.white)).setPayButtonTextSize(14).setPayButtonLoaderVisible(true).setPayButtonSecurityIconVisible(true).setDialogTextColor(ContextCompat.getColor(this, R.color.black1)).setDialogTextSize(14);

        if (tapSdkSession == null) {
            tapSdkSession = new SDKSession();
        }

        String dialingCode = CacheUtil.getInstance().getConcept().getDialingCode().replace("+", "");

        tapSdkSession.addSessionDelegate(this);
        tapSdkSession.instantiatePaymentDataSource();
        tapSdkSession.setTransactionCurrency(new TapCurrency(CacheUtil.getInstance().getConcept().getCurrencyCode()));
        Customer customer = AuthUtil.getInstance().getCustomer();
        PhoneNumber phoneNumber = new PhoneNumber(dialingCode, customer.getAttributes().getMobile().replace("+", "").replaceFirst(dialingCode, ""));
        tapSdkSession.setCustomer(new company.tap.gosellapi.open.models.Customer.CustomerBuilder(null).email(customer.getAttributes().getEmail()).firstName(customer.getAttributes().getFirstName()).lastName(customer.getAttributes().getLastName()).metadata(customer.getId()).phone(phoneNumber).build());
        tapSdkSession.setAmount(new BigDecimal(total));
        Reference reference = new Reference(getPackageName(), "tap", "card", customer.getId(), System.currentTimeMillis() + " _ " + orderId, orderId);
        tapSdkSession.setPaymentReference(reference);
        tapSdkSession.isUserAllowedToSaveCard(false);
        tapSdkSession.isRequires3DSecure(true);
        tapSdkSession.setTransactionMode(TransactionMode.PURCHASE);
        tapSdkSession.setPostURL(EnvUtil.getEnvironmentUrl() + "webhook/tap/feedback");
        tapSdkSession.start(this);

    }

    @Override
    public void showCardFailed() {
        if (cashPayment != null && cashPayment.getVisibility() == View.VISIBLE) {
            showAlertDialog("", CacheUtil.getInstance().getTranslations(Text.CARD_PAYMENT_FAILED_PAY_WITH_CASH, "Card payment failed. Do you want to Pay with Cash instead?"), CacheUtil.getInstance().getTranslations(Text.YES_CAPS), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (dialogCheckout != null) {
                        dialogCheckout.dismiss();
                    }
                    selectPaymentTypeCash();
                }
            }, CacheUtil.getInstance().getTranslations(Text.NO_CAPS), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        } else {
            showError(CacheUtil.getInstance().getTranslations(Text.CARD_FAILED));
        }
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
    public void showCompleted(String orderId, OrderType orderTypeTrue) {
        dismissDialogs();
        this.orderId = orderId;
        continueOrder();
    }

    @Override
    public void setSchedule() {
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (!fragmentManager.isDestroyed()) {
                OrderDialogFragment fragment = OrderDialogFragment.newInstance(true);
                fragment.show(fragmentManager, OrderDialogFragment.class.getSimpleName());
            }
            String colorStr = DynamicTheme.getStringColor(PaymentActivity.this);
            String start = CacheUtil.getInstance().getTranslations(Text.ORDER_SCHEDULED_FOR, "Order scheduled for:");
            String at = CacheUtil.getInstance().getTranslations(Text.AT, "at");
            String dayOnly = TimeUtil.dateToMonthAndDayOnly(daySelected);
            String timeOnly = TimeUtil.dateToHourOnly(selectedDate);
            goodDaySelected = daySelected;
            goodSelectedDate = selectedDate;
            String day = "<font color=" + colorStr + ">" + dayOnly + "</font>";
            String time = "<font color=" + colorStr + ">" + timeOnly + "</font>";
            schedTime.setText(Html.fromHtml(start + " " + day + " " + at + " " + time));
            removeLayout.setVisibility(View.VISIBLE);
            removeLabel.setVisibility(View.VISIBLE);
            noSchedLayout.setVisibility(View.GONE);
            spacingTop.setVisibility(View.VISIBLE);
            withSchedLayout.setVisibility(View.VISIBLE);
            placeOrder.setText(CacheUtil.getInstance().getTranslations(Text.PLACE_SCHEDULED_ORDER, "PLACE SCHEDULED ORDER"));
            isRemove = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        subtotalText.setText(CurrencyUtils.Companion.updateCurrency(subtotalPrice, true, hasPromotion));

        if (discount != 0) {
            discountLayout.setVisibility(View.VISIBLE);
            discountText.setText(CurrencyUtils.Companion.updateCurrency(discount, true, hasPromotion, discount > 0));
        } else {
            discountLayout.setVisibility(View.GONE);
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
//                    discountLayout.setVisibility(View.GONE);
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
            rvServiceCharge.setLayoutManager(linearLayoutManager);
            orderServiceChargeRecyclerViewAdapter = new OrderServiceChargeRecyclerViewAdapter(charges);
            rvServiceCharge.setAdapter(orderServiceChargeRecyclerViewAdapter);
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
        double mukafaaAmount = paymentPresenter.getMukafaaTransactionAmount();
        discount = discount - mukafaaAmount;
        discountText.setText(CurrencyUtils.Companion.updateCurrency(discount, true, hasPromotion, discount > 0));
        discountLayout.setVisibility(discount != 0 ? View.VISIBLE : View.GONE);

        if (mukafaaAmount > 0) {
            if (paymentPresenter.hasMukafaaTransactionId() && paymentPresenter.isMukafaaEnabled()) {
                String amnt = String.valueOf(StringUtil.getFormattedPrice(mukafaaAmount));
                if (!"0".equals(amnt) && !amnt.contains("-")) amnt = "-" + amnt;
                mukafaaSar.setText(amnt);

                mukafaaLayout.setVisibility(View.VISIBLE);
            } else {
                mukafaaLayout.setVisibility(View.GONE);
            }
        } else {
            mukafaaLayout.setVisibility(View.GONE);
            selectMukafaa(false, false);
        }

        if (pointsUsed != 0) {
            redeemedPointsLayout.setVisibility(View.VISIBLE);

            String redeemedText = CacheUtil.getInstance().getTranslations(Text.REDEEMED, "redeemed");
            String color = CacheUtil.getInstance().getColorMain();

            String ptsRedeemed = CacheUtil.getInstance().getTranslations(Text.PTS_LABEL, "(x) pts").replace("(x)", String.valueOf(pointsUsed));

            redeemedPointsBold.setText(HtmlCompat.fromHtml("<font color=" + color + ">" + ptsRedeemed + "</font> ", HtmlCompat.FROM_HTML_MODE_LEGACY));
            redeemedPointsLabel.setText(redeemedText);

            redeemedPoints.setText(CurrencyUtils.Companion.updateCurrency(Math.min(pointsSarValue, paymentPresenter.getMenuItemsTotalPrice()), true, hasPromotion, true));
        } else {
            redeemedPointsLayout.setVisibility(View.GONE);
        }

        if (totalPrice == 0 && pointsUsed > 0) {

            if (deliveryCharge <= 0) {
                chooseMethodLabel.setVisibility(View.GONE);
                if (cardOrderTypePayment != null) cardOrderTypePayment.setVisibility(View.GONE);
                if (cardPayment != null) cardPayment.setVisibility(View.GONE);
                mukafaaPayment.setVisibility(View.GONE);
                if (cashPayment != null) cashPayment.setVisibility(View.GONE);
                loyaltyPointsPaymentContainer.setVisibility(View.VISIBLE);
                useLoyaltyPointsLabel.setVisibility(View.VISIBLE);
                loyaltyPointsAdditionalInfo.setVisibility(View.VISIBLE);
                if (mukafaaAmount > 0) {
                    mukafaaPayment.setVisibility(View.VISIBLE);
                }
                selectPaymentTypeCash();
            } else if (subtotalPrice == pointsSarValue && deliveryCharge > 0) {
                if (cardOrderTypePayment != null) cardOrderTypePayment.setVisibility(View.VISIBLE);
                if (cardPayment != null) cardPayment.setVisibility(View.VISIBLE);
                mukafaaPayment.setVisibility(View.VISIBLE);
                if (cashPayment != null) cashPayment.setVisibility(View.VISIBLE);
            }
        } else {
            paymentPresenter.getPaymentTypes();
            if (totalPrice == deliveryCharge && mukafaaAmount == 0) {
                mukafaaPayment.setVisibility(View.GONE);
            }
            if (cardOrderTypePayment != null) cardOrderTypePayment.setVisibility(View.VISIBLE);
            if (cardPayment != null) cardPayment.setVisibility(View.VISIBLE);
            if (cashPayment != null) cashPayment.setVisibility(View.VISIBLE);
        }

        total.setText(CurrencyUtils.Companion.updateCurrency(totalPrice, true, CacheUtil.getInstance().hasPromotion()));
        totalValue = totalPrice;


        Loyalty loyalty = AuthUtil.getInstance().getCustomerLoyalty();
        double finalDiscount = discount;
        double finalTotal;
        if (mukafaaAmount > 0) {
            finalTotal = subtotalPrice - mukafaaAmount;
        } else {
            finalTotal = subtotalPrice;
        }

        btnUseAll.setOnClickListener(v -> {



            //double points = loyalty.getAttributes().getCurrentBalanceAmount() > (finalTotal - finalDiscount) ? (finalTotal - finalDiscount) * Double.parseDouble(AuthUtil.getInstance().getCustomerLoyalty().getAttributes().getPointConversion()) : loyalty.getAttributes().getCurrentBalance();
            double currentBalance = loyalty.getAttributes().getCurrentBalanceAmount() > (finalTotal - finalDiscount) ? (finalTotal - finalDiscount)  : loyalty.getAttributes().getCurrentBalanceAmount();

            // If points = 3.4 for example, make it 4 points
         /*   if ((int) points < points) {
                points += 1;
            }*/
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

        });


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
                    Intent intent = new Intent(PaymentActivity.this, MainActivity.class);
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
    public ActivityResultLauncher<kotlin.Pair<String, ResultType>> moneyHashContract() {
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
        menuItemsRecyclerViewAdapter.notifyDataSetChanged();
    }

    @Override
    public void showMoyasarSheet(String orderId, double validatedTotal, String menuItems, String apiKey) {
        new Handler().post(() -> {
                    Intent intent = new Intent(PaymentActivity.this,MoyasarActivity.class);
                    intent.putExtra(MoyasarActivity.EXTRA_ORDER_ID, orderId);
                    intent.putExtra(MoyasarActivity.EXTRA_AMOUNT, validatedTotal);
                    intent.putExtra(MoyasarActivity.EXTRA_MENU_ITEMS, menuItems);
                    intent.putExtra(MoyasarActivity.EXTRA_API_KEY, apiKey);
                    moyasarActivityResultLauncher.launch(intent);
                }
        );

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
        showCardFailed();
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

    @OnClick(R.id.panel)
    void panel() {

    }

    @Override
    public void onDateSelected(@NonNull MaterialCalendarView materialCalendarView, @NonNull CalendarDay calendarDay, boolean b) {
        oneDayDecorator.setDate(calendarDay.getDate());
        daySelected = calendarDay.getDate().toString();
        widget.invalidateDecorators();
    }
}
