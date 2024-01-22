package com.herfy.verification.views;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.util.CacheUtil;
import com.herfy.util.StringUtil;
import com.herfy.verification.VerificationContract;
import com.herfy.verification.VerificationPresenter;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PhoneNumberActivity extends BaseActivity implements VerificationContract.View {

    private VerificationContract.Presenter verificationPresenter;

    private boolean orderHistory = false;
    private boolean cart = false;
    private boolean orderType = false;
    private boolean favoriteHome = false;
    private boolean favoriteMenuItem = false;
    private String customerId = "";
    private String phoneNumberValue = "";
    private String email = "";
    private String password = "";

    @BindView(R.id.back)
    ImageButton back;

    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.enter)
    TextView enter;
    @BindView(R.id.phone_number_error)
    TextView phoneNumberError;
    @BindView(R.id.send)
    TextView send;

    @BindView(R.id.phone_number)
    EditText phoneNumber;

    @BindView(R.id.verify)
    MaterialButton verify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_number);
        ButterKnife.bind(this);
        verificationPresenter = new VerificationPresenter(this);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {

            if (bundle.containsKey("order_history")) {
                orderHistory = bundle.getBoolean("order_history");
            }

            if (bundle.containsKey("cart")) {
                cart = bundle.getBoolean("cart");
            }

            if (bundle.containsKey("order_type")) {
                orderType = bundle.getBoolean("order_type");
            }

            if (bundle.containsKey("favorite_home")) {
                favoriteHome = bundle.getBoolean("favorite_home");
            }

            if (bundle.containsKey("favorite_menu_item")) {
                favoriteMenuItem = bundle.getBoolean("favorite_menu_item");
            }

            if (bundle.containsKey("customer_id")) {
                customerId = bundle.getString("customer_id");
            }

            if (bundle.containsKey("phone_number")) {
                phoneNumberValue = bundle.getString("phone_number");
            }

            if (bundle.containsKey("email")) {
                email = bundle.getString("email");
            }

            if (bundle.containsKey("password")) {
                password = bundle.getString("password");
            }

        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        verificationPresenter.start();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void setPresenter(VerificationContract.Presenter presenter) {
        verificationPresenter = presenter;
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

        phoneNumber.setText("");
        phoneNumber.append(StringUtil.localize(phoneNumberValue));

        TextWatcher phoneNumberTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                phoneNumber.removeTextChangedListener(this);
                phoneNumber.setText(StringUtil.localize(phoneNumber.getText().toString()));
                phoneNumber.setSelection(editable.length());
                phoneNumber.addTextChangedListener(this);
            }
        };

        phoneNumber.addTextChangedListener(phoneNumberTextWatcher);

        verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoadingDialog();
                verificationPresenter.send(customerId, StringUtil.toEnglish(phoneNumber.getText().toString().trim().replace("+", "")));
            }
        });
    }

    @Override
    public void setupFonts() {
        phoneNumber.setTypeface(FontHandler.getRegularTypeface());
    }

    @Override
    public void setupTranslations() {
        title.setText(CacheUtil.getInstance().getTranslations(Text.PHONE_VERIFICATION));
        enter.setText(CacheUtil.getInstance().getTranslations(Text.ENTER_PHONE_NUMBER_CAPS));
        send.setText(CacheUtil.getInstance().getTranslations(Text.SEND_CODE_TO_NUMBER));
        verify.setText(CacheUtil.getInstance().getTranslations(Text.SEND_VERIFICATION_CODE));
    }

    @Override
    public void setCode(String code) {

    }

    @Override
    public void showErrorCode(String error) {

    }

    @Override
    public void showErrorPhoneNumber(String error) {
        if (error.isEmpty()) {
            phoneNumberError.setVisibility(View.GONE);
        } else {
            phoneNumberError.setText(error);
            phoneNumberError.setVisibility(View.VISIBLE);
            dismissDialogs();
        }
    }

    @Override
    public void showMessage(String message) {
        dismissDialogs();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showError(String error) {
        showAlertDialog("", error);
    }

    @Override
    public void showResendError(String error) {

    }

    @Override
    public void showCode() {

        Intent intent = new Intent(this, CodeActivity.class);
        intent.putExtra("order_history", orderHistory);
        intent.putExtra("cart", cart);
        intent.putExtra("order_type", orderType);
        intent.putExtra("favorite_home", favoriteHome);
        intent.putExtra("favorite_menu_item", favoriteMenuItem);
        intent.putExtra("customer_id", customerId);
        intent.putExtra("email", email);
        intent.putExtra("password", password);
        intent.putExtra("phone_number", phoneNumber.getText().toString().trim());
        startActivity(intent);

    }

    @Override
    public void showVerified() {

    }

}

