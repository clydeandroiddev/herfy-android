package com.herfy.mukafaa_otp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.button.MaterialButton;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.data.constants.Text;
import com.herfy.util.CacheUtil;
import com.herfy.util.StringUtil;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MukafaaOTPActivity extends BaseActivity implements MukafaaOTPContract.View {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

//    @BindView(R.id.logo)
//    ImageView logo;

    @BindView(R.id.back)
    MaterialButton back;
    @BindView(R.id.cart)
    ConstraintLayout cart;
    @BindView(R.id.title)
    TextView title;

    //    @BindView(R.id.label)
//    TextView label;
    @BindView(R.id.otp_label)
    TextView otpLabel;
    @BindView(R.id.otp)
    EditText otp;
    @BindView(R.id.amount_label)
    TextView amountLabel;
    @BindView(R.id.amount)
    EditText amount;
    @BindView(R.id.sar_label)
    TextView sarLabel;
    @BindView(R.id.submit)
    Button submit;
    @BindView(R.id.resend_container)
    LinearLayout resendContainer;
    @BindView(R.id.resend_message)
    TextView resendMessage;
    @BindView(R.id.resend)
    TextView resend;
    @BindView(R.id.count_container)
    LinearLayout countContainer;
    @BindView(R.id.count_message)
    TextView countMessage;
    @BindView(R.id.count)
    TextView count;

    private AlertDialog loadingDialog;

    private MukafaaOTPContract.Presenter otpPresenter;
    private InvalidOTPDialog invalidOTPDialog;

    private CountDownTimer countDownTimer;

    private TextWatcher otpTextWatcher;
    private TextWatcher amountTextWatcher;
    private int amountCount = -1;
    private float total = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mukafaa_otp);
        ButterKnife.bind(this);
        otpPresenter = new MukafaaOTPPresenter(this, getIntent().getStringExtra("number"), getIntent().getStringExtra("cart"));
        otpPresenter.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void setPresenter(MukafaaOTPContract.Presenter presenter) {
        otpPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {

    }

    @Override
    public void setupViews() {
        total = Float.parseFloat(getIntent().getStringExtra("total"));
        double deliveryCharge = getIntent().getDoubleExtra("delivery_charge", 0);
        double totalMinusDC = total - deliveryCharge;
        if (total > 0) {
            amount.setText(StringUtil.localize(String.format("%.2f", totalMinusDC)));
        }

        title.setVisibility(View.VISIBLE);

        back.setVisibility(View.VISIBLE);
        back.setOnClickListener(view -> onBackPressed());
        cart.setVisibility(View.GONE);

        amountTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() > 0) {
                    String str = amount.getText().toString();
                    amount.setOnKeyListener((v, keyCode, event) -> {
                        if (keyCode == KeyEvent.KEYCODE_DEL) {
                            amountCount--;
                            InputFilter[] fArray = new InputFilter[1];
                            fArray[0] = new InputFilter.LengthFilter(100);
                            amount.setFilters(fArray);
                        }
                        return false;
                    });
                    char t = str.charAt(editable.length() - 1);
                    if (t == '.') {
                        amountCount = 0;
                    }
                    if (amountCount >= 0) {
                        if (amountCount == 2) {
                            InputFilter[] fArray = new InputFilter[1];
                            fArray[0] = new InputFilter.LengthFilter(editable.length());
                            amount.setFilters(fArray);
                        }
                        amountCount++;
                    }
                }

                amount.removeTextChangedListener(this);
                try {
                    amount.setText(StringUtil.localize(amount.getText().toString()));
                    int length = editable.length();
                    amount.setSelection(length);
                } catch (Exception e) {
                }
                amount.addTextChangedListener(this);
            }
        };
        amount.addTextChangedListener(amountTextWatcher);

        otpTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                otp.removeTextChangedListener(this);
                try {
                    otp.setText(StringUtil.localize(otp.getText().toString()));
                    int length = editable.length();
                    otp.setSelection(length);
                } catch (Exception e) {
                }
                otp.addTextChangedListener(this);
            }
        };
        otp.addTextChangedListener(otpTextWatcher);


        resend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                otpPresenter.sendOtp();
            }
        });


        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String o = StringUtil.toEnglish(otp.getText().toString());
                String a = StringUtil.toEnglish(amount.getText().toString());
                if (o.isEmpty()) {
                    otp.requestFocus();
                    showMessage(CacheUtil.getInstance().getTranslations(Text.OTP_REQUIRED, "OTP is required"));
                } else if (a.isEmpty()) {
                    amount.requestFocus();
                    showMessage(CacheUtil.getInstance().getTranslations(Text.AMOUNT_REQUIRED, "Amount is required"));
                } else {
                    otpPresenter.verify(o, a);
                }
            }
        });

    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {
        title.setText(CacheUtil.getInstance().getTranslations(Text.OTP_PAGE, "OTP Page"));
        back.setText(CacheUtil.getInstance().getTranslations(Text.BACK, "Back"));
//        label.setText(CacheUtil.getInstance().getTranslations(Text.OTP_SENT_MESSAGE, "We have sent you a one-time password which will expire in 1 minute."));
        otpLabel.setText(CacheUtil.getInstance().getTranslations(Text.OTP, "OTP"));
        amountLabel.setText(CacheUtil.getInstance().getTranslations(Text.AMOUNT, "Amount"));
        sarLabel.setText(CacheUtil.getInstance().getTranslations(Text.SAR, "SAR"));
        resendMessage.setText(CacheUtil.getInstance().getTranslations(Text.RESEND_OTP_MESSAGE, "Didn’t receive OTP? Resend by clicking"));
        resend.setText(CacheUtil.getInstance().getTranslations(Text.RESEND_OTP_HERE, "here"));
        countMessage.setText(CacheUtil.getInstance().getTranslations(Text.YOU_CAN_RESEND_THE_CODE, "You can resend the code in"));
        count.setText(CacheUtil.getInstance().getTranslations(Text.RESEND_SECONDS, "(x) seconds").replace("(x)", StringUtil.localize("60")));
        submit.setText(CacheUtil.getInstance().getTranslations(Text.SUBMIT_OTP, "SUBMIT OTP"));
    }

    @Override
    public void showLoadingDialog() {

        dismissDialogs();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        builder.setView(getLayoutInflater().inflate(R.layout.loading, null));

        loadingDialog = builder.create();

        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        loadingDialog.show();

    }

    @Override
    public void hideLoadingDialog() {

        try {

            if (loadingDialog != null && loadingDialog.isShowing()) {

                loadingDialog.dismiss();

            }

        } catch (Exception e) {

            e.printStackTrace();

        }

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
        dismissDialogs();
        showAlertDialog("", error);
    }

    @Override
    public void showCode() {

    }

    @Override
    public void showVerified() {

    }

    @Override
    public void showInvalidOTPDialog(boolean closePageOnClose) {
        String message = CacheUtil.getInstance().getTranslations(Text.INVALID_OTP, "Sorry, you have entered an Invalid OTP. Please re-enter OTP.");
        showInvalidOTPDialog(message, closePageOnClose);
    }

    @Override
    public void showInvalidOTPDialog(String message, boolean closePageOnClose) {
        invalidOTPDialog = InvalidOTPDialog.newInstance(message, new InvalidOTPDialog.OnAction() {
            @Override
            public void onClose() {
                invalidOTPDialog.dismiss();
                if (closePageOnClose) {
                    closePage();
                }
            }
        });
        invalidOTPDialog.setCancelable(true);
        invalidOTPDialog.show(getSupportFragmentManager(), InvalidOTPDialog.class.toString());
    }

    @Override
    public void closePage() {
        finish();
    }

    @Override
    public void closePageWithTransactionID(String transactionId, String amount) {
        Intent intent = new Intent();
        intent.putExtra("transaction_id", transactionId);
        intent.putExtra("alrajhi_amount", amount);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    public void setCountdown(long millis) {
//        if (countDownTimer != null)
//            countDownTimer.cancel();
//
//        label.setVisibility(View.VISIBLE);
//        String message = CacheUtil.getInstance().getTranslations(Text.OTP_SENT_MESSAGE, "We have sent you a one-time password which will expire in (x).");
//
//        countDownTimer = new CountDownTimer(millis, 1000) {
//            public void onTick(long millisUntilFinished) {
//                String lbl = message.replace("(x)", otpPresenter.getFormattedTimeForCountdown(millisUntilFinished));
//                label.setText(lbl);
//            }
//
//            public void onFinish() {
//                closePage();
//            }
//        };
//        countDownTimer.start();

    }
}

