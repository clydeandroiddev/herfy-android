package com.herfy.verification.views;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.mukesh.OtpView;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.constants.Text;
import com.herfy.constants.Values;
import com.herfy.font.FontHandler;
import com.herfy.util.AppLogger;
import com.herfy.util.CacheUtil;
import com.herfy.util.StringUtil;
import com.herfy.verification.CodeBroadcastReceiver;
import com.herfy.verification.VerificationContract;
import com.herfy.verification.VerificationPresenter;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CodeActivity extends BaseActivity implements VerificationContract.View {

    private VerificationContract.Presenter verificationPresenter;

    private boolean orderHistory = false;
    private boolean cart = false;
    private boolean orderType = false;
    private boolean favoriteHome = false;
    private boolean favoriteMenuItem = false;
    private boolean guest = false;
    private String customerId = "";
    private String email = "";
    private String password = "";
    private String phoneNumberValue = "";

    @BindView(R.id.back) ImageButton back;

    @BindView(R.id.title) TextView title;
    @BindView(R.id.enter) TextView enter;
    @BindView(R.id.sent) TextView sent;
    @BindView(R.id.phone_number) TextView phoneNumber;
    @BindView(R.id.count_message) TextView countMessage;
    @BindView(R.id.count) TextView count;
    @BindView(R.id.resend_message) TextView resendMessage;
    @BindView(R.id.resend) TextView resend;

    @BindView(R.id.code) OtpView code;

    @BindView(R.id.count_container) LinearLayout countContainer;

    @BindView(R.id.verify) MaterialButton verify;

    private CodeBroadcastReceiver codeBroadcastReceiver;

    private CountDownTimer countDownTimer;
    private long countDownTime = 60000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code);
        ButterKnife.bind(this);
        verificationPresenter = new VerificationPresenter(this);

        codeBroadcastReceiver = new CodeBroadcastReceiver();
        codeBroadcastReceiver.setCodeBroadcastReceiverListener(new CodeBroadcastReceiver.CodeBroadcastReceiverListener() {
            @Override
            public void onReceived(Intent intent) {
                if(intent != null) {
                    try {
                        startActivityForResult(intent, Values.REQUEST_SMS_CONSENT);
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onTimeout() {
                AppLogger.error("CodeBroadcastReceiver onTimeout");
            }
        });

        IntentFilter intentFilter = new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION);
        registerReceiver(codeBroadcastReceiver, intentFilter, SmsRetriever.SEND_PERMISSION, null);

        Task<Void> task = SmsRetriever.getClient(this).startSmsUserConsent(null);
        task.addOnSuccessListener(aVoid -> {});
        task.addOnFailureListener(e -> AppLogger.error(e.getMessage()));

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {

            if(bundle.containsKey("order_history")) {
                orderHistory = bundle.getBoolean("order_history");
            }

            if(bundle.containsKey("cart")) {
                cart = bundle.getBoolean("cart");
            }

            if(bundle.containsKey("order_type")) {
                orderType = bundle.getBoolean("order_type");
            }

            if(bundle.containsKey("favorite_home")) {
                favoriteHome = bundle.getBoolean("favorite_home");
            }

            if(bundle.containsKey("favorite_menu_item")) {
                favoriteMenuItem = bundle.getBoolean("favorite_menu_item");
            }

            if(bundle.containsKey("guest")) {
                guest = bundle.getBoolean("guest");
            }

            if(bundle.containsKey("customer_id")) {
                customerId = bundle.getString("customer_id");
            }

            if(bundle.containsKey("email")) {
                email = bundle.getString("email");
            }

            if(bundle.containsKey("password")) {
                password = bundle.getString("password");
            }

            if(bundle.containsKey("phone_number")) {
                phoneNumberValue = bundle.getString("phone_number");
            }

        }



    }

    @Override
    protected void onResume() {
        super.onResume();
        verificationPresenter.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Values.REQUEST_SMS_CONSENT && resultCode == RESULT_OK) {
            String message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE);
            if(message != null) {
                Pattern p = Pattern.compile("\\b\\d{6}\\b");
                Matcher m = p.matcher(message);
                String codeValue = "";
                while (m.find()) {
                    codeValue = m.group(0);
                }
                code.setText(codeValue);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(codeBroadcastReceiver != null) {
            unregisterReceiver(codeBroadcastReceiver);
        }
        //unregisterReceiver(smsVerificationReceiver);
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
            public void onClick(View v) {
                onBackPressed();
            }
        });

        phoneNumber.setText(StringUtil.localize(phoneNumberValue));

        TextWatcher phoneNumberTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                code.removeTextChangedListener(this);
                code.setText(StringUtil.localize(code.getText().toString()));
                code.setSelection(editable.length());
                code.addTextChangedListener(this);
            }
        };

        code.addTextChangedListener(phoneNumberTextWatcher);

        countDownTimer = new CountDownTimer(countDownTime, 1000) {
            public void onTick(long millisUntilFinished) {
                countDownTime = millisUntilFinished;
                count.setText(CacheUtil.getInstance().getTranslations(Text.RESEND_SECONDS, "(x) seconds").replace("(x)", StringUtil.localize(String.valueOf(millisUntilFinished/ 1000))));
            }
            public void onFinish() {
                countDownTime = 60000;
                countContainer.setVisibility(View.GONE);
                resend.setTextColor(ContextCompat.getColor(CodeActivity.this, R.color.primary_text));
                resend.setFocusable(true);
                resend.setClickable(true);
            }
        }.start();
        countContainer.setVisibility(View.VISIBLE);

        verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoadingDialog();
                if(!guest) {
                    verificationPresenter.verify(customerId, email, password, StringUtil.toEnglish(phoneNumber.getText().toString().trim().replace("+", "")), StringUtil.toEnglish(code.getText().toString().trim()));
                } else {
                    verificationPresenter.guest(StringUtil.toEnglish(phoneNumber.getText().toString().trim().replace("+", "")), StringUtil.toEnglish(code.getText().toString().trim()));
                }
            }
        });

        resend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(countContainer.getVisibility() == View.GONE) {
                    Intent intent = new Intent(CodeActivity.this, PhoneNumberActivity.class);
                    intent.putExtra("order_history", orderHistory);
                    intent.putExtra("cart", cart);
                    intent.putExtra("order_type", orderType);
                    intent.putExtra("favorite_home", favoriteHome);
                    intent.putExtra("favorite_menu_item", favoriteMenuItem);
                    intent.putExtra("customer_id", customerId);
                    intent.putExtra("phone_number", phoneNumberValue);
                    intent.putExtra("email", email);
                    intent.putExtra("password", password);
                    startActivity(intent);
                }
            }
        });
        resend.setFocusable(false);
        resend.setClickable(false);

    }

    @Override
    public void setupFonts() {
        phoneNumber.setTypeface(FontHandler.getMediumTypeface());
        code.setTypeface(FontHandler.getMediumTypeface());
        countMessage.setTypeface(FontHandler.getMediumTypeface());
        count.setTypeface(FontHandler.getSemiboldTypeface());
        resendMessage.setTypeface(FontHandler.getMediumTypeface());
        resend.setTypeface(FontHandler.getSemiboldTypeface());
    }

    @Override
    public void setupTranslations() {
        title.setText(CacheUtil.getInstance().getTranslations(Text.PHONE_VERIFICATION));
        enter.setText(CacheUtil.getInstance().getTranslations(Text.ENTER_VERIFICATION_CODE));
        sent.setText(CacheUtil.getInstance().getTranslations(Text.VERIFICATION_CODE_SENT, "A verification code is sent to your number"));
        verify.setText(CacheUtil.getInstance().getTranslations(Text.VERIFY_NUMBER));
        countMessage.setText(CacheUtil.getInstance().getTranslations(Text.YOU_CAN_RESEND_THE_CODE, "You can resend the code in"));
        count.setText(CacheUtil.getInstance().getTranslations(Text.RESEND_SECONDS, "(x) seconds").replace("(x)", StringUtil.localize("60")));
        resendMessage.setText(CacheUtil.getInstance().getTranslations(Text.RESEND_VERIFICATION_CODE, "Didn't get the code?"));
        resend.setText(CacheUtil.getInstance().getTranslations(Text.RESEND, "Resend"));
    }

    @Override
    public void setCode(String code) {

    }

    @Override
    public void showErrorCode(String error) {

    }

    @Override
    public void showErrorPhoneNumber(String error) {

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
        dismissDialogs();
        Toast.makeText(this, CacheUtil.getInstance().getTranslations(Text.VERIFICATION_CODE_SENT, "A verification code is sent to your number"), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showVerified() {

        Intent intent = new Intent(CodeActivity.this, VerifiedActivity.class);
        intent.putExtra("order_history", orderHistory);
        intent.putExtra("cart", cart);
        intent.putExtra("order_type", orderType);
        intent.putExtra("favorite_home", favoriteHome);
        intent.putExtra("favorite_menu_item", favoriteMenuItem);
        startActivity(intent);
        finish();

    }

}
