package com.herfy.summary;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.BuildUtil;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.helper.EnvUtil;
import com.moneyhash.sdk.android.ResultType;
import com.moneyhash.sdk.android.payment.PaymentResultContract;
import com.herfy.BuildConfig;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.utils.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.payment.PaymentContract;
import com.herfy.payment.PaymentPresenter;
import com.herfy.payment.views.OrderDialogFragment;
import com.herfy.payment.views.PaymentDialogFragment;
import com.skylinedynamics.solosdk.api.models.objects.Customer;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.PaymentType;
import com.skylinedynamics.solosdk.api.models.objects.PrayerTime;
import com.skylinedynamics.solosdk.api.models.objects.ServiceCharge;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.StringUtil;
import com.herfy.utils.helper.TimeUtil;
import com.herfy.util.ViewUtil;

import java.math.BigDecimal;
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

import butterknife.BindView;
import butterknife.ButterKnife;
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
import kotlin.Pair;

public class OrderSummaryActivity extends BaseActivity implements PaymentContract.View, SessionDelegate {

    @BindView(R.id.back)
    MaterialButton back;
    @BindView(R.id.cart)
    ConstraintLayout cart;
    @BindView(R.id.confirm)
    MaterialButton confirm;
    @BindView(R.id.menu_items_recycler_view)
    RecyclerView menuItemsRecyclerView;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.details_label)
    TextView detailsLabel;
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
    @BindView(R.id.tax_label)
    TextView taxLabel;
    @BindView(R.id.tax)
    TextView tax;
    @BindView(R.id.total_label)
    TextView totalLabel;
    @BindView(R.id.total)
    TextView total;
    @BindView(R.id.payment_label)
    TextView paymentLabel;
    @BindView(R.id.payment_method_label)
    TextView paymentMethodLabel;
    @BindView(R.id.payment_method)
    TextView paymentMethodText;
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
    @BindView(R.id.redeemed_points_layout)
    LinearLayout redeemedPointsLayout;
    @BindView(R.id.redeemed_points_label)
    TextView redeemedPointsLabel;
    @BindView(R.id.redeemed_points)
    TextView redeemedPoints;
    @BindView(R.id.v_earn_points_card)
    MaterialCardView vEarnPointsCard;
    @BindView(R.id.tv_points)
    TextView tvPoints;
    @BindView(R.id.table_number_layout)
    MaterialCardView tableNumberLayout;
    @BindView(R.id.table_number_label)
    TextView tableNumberLabel;
    @BindView(R.id.table_number)
    TextView tableNumber;

    private boolean isScheduled;
    private String scheduledDate;
    private double cashPresented;
    private boolean hasCash;
    private SDKSession tapSdkSession;

    private ArrayList<MenuItem> items;

    private PaymentContract.Presenter paymentPresenter;

    private OrderSummaryMenuItemRecyclerViewAdapter menuItemsRecyclerViewAdapter;

    public OrderSummaryActivity() {
        items = new ArrayList<>();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_summary);
        ButterKnife.bind(this);
        paymentPresenter = new PaymentPresenter(this);
        paymentPresenter.start();
        paymentPresenter.getPaymentTypes();
    }

    @Override
    public void setupViews() {

        isScheduled = getIntent().getBooleanExtra("isScheduled", false);
        cashPresented = getIntent().getDoubleExtra("cashPresented", 0d);
        if (isScheduled) {
            scheduledDate = getIntent().getStringExtra("scheduledDate");
            schedLayout.setVisibility(View.VISIBLE);
            scheduleText.setText(TimeUtil.properSchedDate(scheduledDate));
        } else {
            schedLayout.setVisibility(View.GONE);
        }

        paymentPresenter.setCashPresented(cashPresented);

        back.setVisibility(View.VISIBLE);
        back.setOnClickListener(view -> onBackPressed());
        cart.setVisibility(View.GONE);

        menuItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        menuItemsRecyclerViewAdapter = new OrderSummaryMenuItemRecyclerViewAdapter(paymentPresenter);
        menuItemsRecyclerView.setAdapter(menuItemsRecyclerViewAdapter);

        items.addAll(CacheUtil.getInstance().getCart());

        confirm.setOnClickListener(v -> {
            showLoadingDialog();
            if (!isScheduled) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
                paymentPresenter.getStoreOpen(false, simpleDateFormat.format(new Date()));
            } else {
                try {
                    Calendar originalCalendar = Calendar.getInstance();
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.ENGLISH);
                    calendar.setTime(sdf.parse(scheduledDate));
                    paymentPresenter.scheduleOrder(originalCalendar, calendar, false);
                } catch (ParseException pe) {
                    pe.printStackTrace();
                    int minSchedule = CacheUtil.getInstance().getConcept().getDefaultScheduleDeliveryTime();
                    if (minSchedule == 0) {
                        minSchedule = 15;
                    }
                    showMessage(CacheUtil.getInstance().getTranslations(Text.MINIMUM_SCHEDULED_ORDER, "Unable to process order, scheduled order is allowed minimum of (x) min from current time.").replace("(x)", "" + minSchedule));
                    dismissDialogs();
                }
            }
        });

        setPaymentMethodTextLabel();

        switch (CacheUtil.getInstance().getOrderType()) {
            case EAT_IN:
            case DRIVE_THRU:
            case PICKUP:
                type.setText(CacheUtil.getInstance().getTranslations(Text.PICKUP));
                break;
            case DINE_IN:
                type.setText(CacheUtil.getInstance().getTranslations(Text.DINE_IN));
                break;
            case DELIVERY:
                type.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERY));
                break;
        }

        try {
            branch.setText(CacheUtil.getInstance().getOrderStore().getAttributes().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!CacheUtil.getInstance().getDineInTableNumber().isEmpty()) {
            tableNumberLayout.setVisibility(View.VISIBLE);
            tableNumber.setText(CacheUtil.getInstance().getDineInTableNumber());
        }
    }

    private void setPaymentMethodTextLabel() {
        String payment = CacheUtil.getInstance().getOrderPayment().getValue();

        if (payment.equalsIgnoreCase(PaymentType.CASH.getValue())) {
            paymentMethodText.setText(CacheUtil.getInstance().getTranslations(Text.CASH));
        } else if (payment.equalsIgnoreCase(PaymentType.CARD.getValue())) {
            /*if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
                paymentMethodText.setText(CacheUtil.getInstance().getTranslations(Text.CARD_ON_DELIVERY));
            } else {
                paymentMethodText.setText(CacheUtil.getInstance().getTranslations(Text.CARD_ON_PICKUP));
            }*/
            paymentMethodText.setText(CacheUtil.getInstance().getTranslations(Text.ONLINE_CARD_PAYMENT, "Online Card Payment"));
        } else if (payment.equalsIgnoreCase(PaymentType.CARD_ON_DELIVERY.getValue())) {
            paymentMethodText.setText(CacheUtil.getInstance().getTranslations(Text.CARD_ON_DELIVERY));
        } else if (payment.equalsIgnoreCase(PaymentType.APPLE_PAY.getValue())) {
            paymentMethodText.setText(CacheUtil.getInstance().getTranslations(Text.APPLE_PAY));
        }
    }

    @Override
    public void setupTranslations() {
        title.setText(CacheUtil.getInstance().getTranslations(Text.ORDER_SUMMARY, "ORDER SUMMARY"));
        back.setText(CacheUtil.getInstance().getTranslations(Text.BACK, "Back"));
        detailsLabel.setText(CacheUtil.getInstance().getTranslations(Text.ORDER_DETAILS));
        itemsLabel.setText(CacheUtil.getInstance().getTranslations(Text.ORDERED_ITEMS_CAPS, "ORDERED ITEMS"));
        itemsQuantityLabel.setText(CacheUtil.getInstance().getTranslations(Text.ITEMS_QUANTITY_CAPS, "ITEMS & QUANTITY"));
        priceLabel.setText(CacheUtil.getInstance().getTranslations(Text.PRICE_CAPS, "PRICE"));
        subtotalLabel.setText(CacheUtil.getInstance().getTranslations(Text.SUBTOTAL));
        subtotalLabel.setText(CacheUtil.getInstance().getTranslations(Text.SUBTOTAL));
        discountLabel.setText(CacheUtil.getInstance().getTranslations(Text.PROMO_DISCOUNT));
        deliveryLabel.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERY));
        taxLabel.setText(CacheUtil.getInstance().getTranslations(Text.TAX));
        confirm.setText(CacheUtil.getInstance().getTranslations(Text.CONFIRM_CAPS, "CONFIRM"));
        totalLabel.setText(CacheUtil.getInstance().getTranslations(Text.TOTAL_LABEL));
        schedLabel.setText(CacheUtil.getInstance().getTranslations(Text.SCHEDULE, "SCHEDULE").toUpperCase());
        paymentLabel.setText(CacheUtil.getInstance().getTranslations(Text.PAYMENT_DETAILS_CAPS, "PAYMENT DETAILS"));
        paymentMethodLabel.setText(CacheUtil.getInstance().getTranslations(Text.PAYMENT_METHOD_CAPS));
        branchLabel.setText(CacheUtil.getInstance().getTranslations(Text.BRANCH_CAPS, "BRANCH"));
        typeLabel.setText(CacheUtil.getInstance().getTranslations(Text.ORDER_TYPE_CAPS, "ORDER TYPE"));
        tableNumberLabel.setText(CacheUtil.getInstance().getTranslations(Text.TABLE_NUMBER_CAPS, "TABLE NUMBER"));
    }

    @Override
    public void showPaymentTypes(Set<PaymentType> paymentTypes) {
        hasCash = paymentTypes.contains(PaymentType.CASH);
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
        CacheUtil.getInstance().setOrderPayment(PaymentType.CASH);
        setPaymentMethodTextLabel();
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

    }

    @Override
    public void initializeTap(String orderId, double total) {

        if (!BuildConfig.BUILD_TYPE.equalsIgnoreCase("release")) {
            GoSellSDK.init(this, BuildUtil.tapTest(), getPackageName());
        } else {
            GoSellSDK.init(this, BuildUtil.tapLive(), getPackageName());
        }

        GoSellSDK.setLocale(LocaleUtil.getInstance().getLanguageCode());

        ThemeObject.getInstance()
                .setAppearanceMode(AppearanceMode.FULLSCREEN_MODE)
                .setSdkLanguage(LocaleUtil.getInstance().getLanguageCode())
                .setHeaderFont(FontHandler.getMediumTypeface())
                .setHeaderTextColor(ContextCompat.getColor(this, R.color.white))
                .setHeaderTextSize(18)
                .setHeaderBackgroundColor(ViewUtil.getColorMain(this))
                .setCardInputTextColor(ContextCompat.getColor(this, R.color.black))
                .setCardInputInvalidTextColor(ContextCompat.getColor(this, R.color.red))
                .setCardInputPlaceholderTextColor(ContextCompat.getColor(this, R.color.gray))
                .setSaveCardSwitchOffThumbTint(ContextCompat.getColor(this, R.color.french_gray_new))
                .setSaveCardSwitchOnThumbTint(ContextCompat.getColor(this, R.color.vibrant_green))
                .setSaveCardSwitchOffTrackTint(ContextCompat.getColor(this, R.color.french_gray))
                .setSaveCardSwitchOnTrackTint(ContextCompat.getColor(this, R.color.vibrant_green_pressed))
                .setScanIconDrawable(ContextCompat.getDrawable(this, R.drawable.btn_card_scanner_normal))
                .setPayButtonResourceId(R.drawable.btn_pay_selector)
                .setPayButtonFont(FontHandler.getRegularTypeface())
                .setPayButtonDisabledTitleColor(ContextCompat.getColor(this, R.color.white))
                .setPayButtonEnabledTitleColor(ContextCompat.getColor(this, R.color.white))
                .setPayButtonTextSize(14)
                .setPayButtonLoaderVisible(true)
                .setPayButtonSecurityIconVisible(true)
                .setDialogTextColor(ContextCompat.getColor(this, R.color.black1))
                .setDialogTextSize(14);

        if (tapSdkSession == null) {
            tapSdkSession = new SDKSession();
        }

        String dialingCode = CacheUtil.getInstance().getConcept().getDialingCode().replace("+", "");

        tapSdkSession.addSessionDelegate(this);
        tapSdkSession.instantiatePaymentDataSource();
        tapSdkSession.setTransactionCurrency(new TapCurrency(CacheUtil.getInstance().getConcept().getCurrencySymbol()));
        Customer customer = AuthUtil.getInstance().getCustomer();
        PhoneNumber phoneNumber = new PhoneNumber(dialingCode, customer.getAttributes().getMobile().replace("+", "").replaceFirst(dialingCode, ""));
        tapSdkSession.setCustomer(new company.tap.gosellapi.open.models.Customer.CustomerBuilder(null).email(customer.getAttributes().getEmail()).firstName(customer.getAttributes().getFirstName()).lastName(customer.getAttributes().getLastName()).metadata(customer.getId()).phone(phoneNumber).build());
        tapSdkSession.setAmount(new BigDecimal(total));
        Reference reference = new Reference(getPackageName(), "tap", "card", orderId, System.currentTimeMillis() + " _ " + orderId, orderId);
        tapSdkSession.setPaymentReference(reference);
        tapSdkSession.isUserAllowedToSaveCard(false);
        tapSdkSession.isRequires3DSecure(true);
        tapSdkSession.setTransactionMode(TransactionMode.PURCHASE);
        tapSdkSession.setPostURL(EnvUtil.getEnvironmentUrl() + "webhook/tap/feedback");
        tapSdkSession.start(this);
    }


    @Override
    public void showCheckoutPaymentForm(String orderId) {

    }

    @Override
    public void showCardFailed() {
        if (hasCash) {
            showAlertDialog("", CacheUtil.getInstance().getTranslations(Text.CARD_PAYMENT_FAILED_PAY_WITH_CASH, "Card payment failed. Do you want to Pay with Cash instead?"), CacheUtil.getInstance().getTranslations(Text.YES_CAPS), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
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
    public void showCompleted(String orderId, OrderType orderType) {
        try {
            dismissDialogs();
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (!fragmentManager.isDestroyed()) {
                OrderDialogFragment fragment = OrderDialogFragment.newInstance(false);
                fragment.show(fragmentManager, OrderDialogFragment.class.getSimpleName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setSchedule() {

    }

    @Override
    public void setTax(boolean inclusive, CharSequence taxAmount) {
        vatLayout.setVisibility(inclusive ? View.GONE : View.VISIBLE);
        tax.setText(taxAmount);
        taxLabel.setText(CacheUtil.getInstance().getConcept().getVatRate() + "% " + CacheUtil.getInstance().getTranslations(Text.VAT, "VAT"));
    }

    @Override
    public void setPriceDetails(double subtotalPrice, double discount, double totalPrice, double deliveryCharge, double pointsSarValue, int pointsEarnable, int pointsUsed, ArrayList<ServiceCharge> charges) {
        subtotalText.setText(StringUtil.getFormattedPrice(subtotalPrice));

        if (deliveryCharge != 0) {
            deliveryLayout.setVisibility(View.VISIBLE);
            deliveryText.setText(StringUtil.getFormattedPrice(deliveryCharge));
        } else {
            deliveryLayout.setVisibility(View.GONE);
        }

        if (discount != 0) {
            discountLayout.setVisibility(View.VISIBLE);
            discountText.setText("-" + StringUtil.getFormattedPrice(discount));
        } else {
            discountLayout.setVisibility(View.GONE);
        }

        if (pointsSarValue != 0) {
            redeemedPointsLayout.setVisibility(View.VISIBLE);
            redeemedPointsLabel.setText(pointsUsed + " pts redeemed");
            redeemedPoints.setText("-" + StringUtil.getFormattedPrice(pointsSarValue));
        } else {
            redeemedPointsLayout.setVisibility(View.GONE);
        }

        if (totalPrice == 0) {
            vEarnPointsCard.setVisibility(View.GONE);
        }

        total.setText(StringUtil.getFormattedPrice(totalPrice));
    }

    @Override
    public void setMenuItem(MenuItem menuItem) {

    }

    @Override
    public void logFirebaseEvent(String event, String orderTotalPrice) {

    }

    @Override
    public void showPrayerTime(PrayerTime prayerTime) {

    }

    @Override
    public void showErrorPrayerTime() {

    }

    @Override
    public ActivityResultLauncher<Pair<String, ResultType>> moneyHashContract() {
        return registerForActivityResult((ActivityResultContract) (new PaymentResultContract()), (ActivityResultCallback) (result) -> {
        });
    }

    @Override
    public void setOrderId(String orderId) {
        int pointsEarnable = CacheUtil.getInstance().getLoyaltyPointsEarnable(orderId);
        tvPoints.setText(CacheUtil.getInstance()
                .getTranslations(Text.EARNABLE_POINTS, "You will earn (x) once your order is completed.")
                .replace("(x)", String.valueOf(pointsEarnable))
        );
    }

    @Override
    public void showCardDialogList(String orderId) {

    }

    @Override
    public void firebaseECommercePurchaseEvent(List<MenuItem> orderedItem, double total) {

    }

    @Override
    public void firebaseAddPaymentInfoEvent() {

    }

    @Override
    public void firebaseApplyPromotionEvent(String promotionId, String promotionName, List<MenuItem> orderedItems) {

    }

    @Override
    public void updateMenuItems() {

    }

    @Override
    public void showMoyasarSheet(String orderId, double validatedTotal, String menuItems, String apiKey) {

    }

    @Override
    public void showOrderType(Boolean checkout) {

    }

    @Override
    public void showSelectedPyament() {

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
    public void setupFonts() {

    }

    @Override
    public void paymentSucceed(@NonNull Charge charge) {
        paymentPresenter.verifyTap(true, charge.getId());
    }

    @Override
    public void paymentFailed(@Nullable Charge charge) {
        if (charge != null) {
            paymentPresenter.verifyTap(false, charge.getId());
        } else {
            showCardFailed();
        }
    }

    @Override
    public void authorizationSucceed(@NonNull Authorize authorize) {

    }

    @Override
    public void authorizationFailed(Authorize authorize) {

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

    }

    @Override
    public void sessionIsStarting() {

    }

    @Override
    public void sessionHasStarted() {

    }

    @Override
    public void sessionCancelled() {

    }

    @Override
    public void sessionFailedToStart() {

    }

    @Override
    public void invalidCardDetails() {

    }

    @Override
    public void backendUnknownError(String message) {

    }

    @Override
    public void invalidTransactionMode() {

    }

    @Override
    public void invalidCustomerID() {

    }

    @Override
    public void userEnabledSaveCardOption(boolean saveCardEnabled) {

    }

    @Override
    public void cardTokenizedSuccessfully(@NonNull Token token, boolean saveCardEnabled) {

    }
}
