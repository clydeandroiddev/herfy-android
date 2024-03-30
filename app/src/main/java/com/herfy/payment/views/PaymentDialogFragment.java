package com.herfy.payment.views;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.TextKeyListener;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.button.MaterialButton;
import com.herfy.utils.helper.CacheUtil;
import com.moneyhash.sdk.android.ResultType;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.base.BaseDialogFragment;
import com.herfy.utils.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.payment.PaymentContract;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.PaymentType;
import com.skylinedynamics.solosdk.api.models.objects.PrayerTime;
import com.skylinedynamics.solosdk.api.models.objects.ServiceCharge;
import com.herfy.util.CurrencyUtils;
import com.herfy.util.StringUtil;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import kotlin.Pair;

public class PaymentDialogFragment extends BaseDialogFragment implements PaymentContract.View {

    private PaymentContract.Presenter paymentPresenter;

    private OnSubmit onSubmit;

    public interface OnSubmit {
        void onSubmit(Dialog dialog, double cashPresented);
    }

    @BindView(R.id.close) ImageButton close;

    @BindView(R.id.title) TextView title;

    @BindView(R.id.amount) EditText amount;

    @BindView(R.id.exact) MaterialButton exact;
    @BindView(R.id.submit) MaterialButton submit;

    public static PaymentDialogFragment newInstance(PaymentContract.Presenter paymentPresenter, OnSubmit onSubmit) {
        PaymentDialogFragment paymentDialogFragment = new PaymentDialogFragment();
        paymentDialogFragment.paymentPresenter = paymentPresenter;
        paymentDialogFragment.onSubmit = onSubmit;
        return paymentDialogFragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialog_payment, container, false);
        ButterKnife.bind(this, view);
        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        setupFonts();
        setupTranslations();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                TypedValue typedValue = new TypedValue();
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                dialog.getWindow().setStatusBarColor(typedValue.data);
            }
        }
    }

    @Override
    public void setPresenter(PaymentContract.Presenter presenter) {
        paymentPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        if(getActivity() != null) {
            ((BaseActivity) getActivity()).logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
        }
    }

    @Override
    public void setCartExpiry() {

    }

    @Override
    public void setupViews() {

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        TextWatcher amountTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                amount.removeTextChangedListener(this);
                amount.setText(StringUtil.localize(amount.getText().toString()));
                amount.setSelection(editable.length());
                amount.addTextChangedListener(this);
            }
        };

        amount.addTextChangedListener(amountTextWatcher);

        amount.setText("");
        if(paymentPresenter != null) {
            boolean hasPromotion = CacheUtil.getInstance().hasPromotion();
            if (paymentPresenter.getCashPresented() == 0) {
                amount.append(CurrencyUtils.Companion.getFormattedAmount(paymentPresenter.getValidatedTotal(), hasPromotion));
            } else {
                amount.append(CurrencyUtils.Companion.getFormattedAmount(paymentPresenter.getCashPresented(), hasPromotion));
            }
        }

        amount.setKeyListener(new DigitsKeyListener());

        exact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                amount.setKeyListener(new TextKeyListener(TextKeyListener.Capitalize.NONE, false));
                amount.setText("");
                boolean hasPromotion = CacheUtil.getInstance().hasPromotion();
                amount.append(CurrencyUtils.Companion.getFormattedAmount(paymentPresenter.getValidatedTotal(), hasPromotion));
                amount.setKeyListener(new DigitsKeyListener());
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
                    double cashPresented = numberFormat.parse(StringUtil.toEnglish(amount.getText().toString().trim())).doubleValue();

                    if(cashPresented < paymentPresenter.getValidatedTotal()) {
                        Toast.makeText(getActivity(), CacheUtil.getInstance().getTranslations(Text.AMOUNT_LESS_THAN_TOTAL), Toast.LENGTH_SHORT).show();
                    } else {
                        onSubmit.onSubmit(getDialog(), cashPresented);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), CacheUtil.getInstance().getTranslations(Text.INVALID_AMOUNT), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    public void setupFonts() {

        title.setTypeface(FontHandler.getSemiboldTypeface());
        amount.setTypeface(FontHandler.getSemiboldTypeface());
        exact.setTypeface(FontHandler.getSemiboldTypeface());
        submit.setTypeface(FontHandler.getSemiboldTypeface());

    }

    @Override
    public void setupTranslations() {

        title.setText(CacheUtil.getInstance().getTranslations(Text.AMOUNT_DRIVER_MESSAGE));
        exact.setText(CacheUtil.getInstance().getTranslations(Text.EXACT_AMOUNT));
        submit.setText(CacheUtil.getInstance().getTranslations(Text.OK));

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

   /* @Override
    public void initializeHyperPay(CheckoutSettings settings, String checkoutId) {

    }
*/
    @Override
    public void showCheckoutPaymentForm(String orderId) {

    }

    @Override
    public void showCardFailed() {

    }

    @Override
    public void showMessage(String message) {

    }

    @Override
    public void showError(String error) {

    }

    @Override
    public void showCompleted(String orderId, OrderType orderType) {

    }

    @Override
    public void setSchedule() {

    }

    @Override
    public void dismissDialogs() {

    }

    @Override
    public void setTax(boolean inclusive, CharSequence taxAmount) {

    }

    @Override
    public void setPriceDetails(double subtotalPrice, double discount, double totalPrice, double deliveryCharge, double pointsSarValue, int pointsEarnable, int pointsUsed, ArrayList<ServiceCharge> charges) {

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
        return null;
    }

    @Override
    public void setOrderId(String orderId) {

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
    public void showSelectedPayment() {

    }

}
