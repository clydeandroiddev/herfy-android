package com.herfy.auth.views;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.herfy.R;
import com.herfy.auth.AuthContract;
import com.herfy.auth.AuthPresenter;
import com.herfy.base.BaseActivity;
import com.herfy.data.constants.Text;
import com.herfy.util.CacheUtil;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ForgotPasswordActivity extends BaseActivity implements AuthContract.View {

    private AuthContract.Presenter authPresenter;

    @BindView(R.id.back) ImageButton back;

    @BindView(R.id.message) TextView message;
    @BindView(R.id.email_error) TextView emailError;

    @BindView(R.id.email) TextInputEditText email;

    @BindView(R.id.submit) MaterialButton submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        ButterKnife.bind(this);
        authPresenter = new AuthPresenter(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        authPresenter.start();

    }

    @Override
    public void setPresenter(AuthContract.Presenter presenter) {
        authPresenter = presenter;
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
                finish();
            }
        });

        email.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    submit.performClick();
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(email.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoadingDialog();
                authPresenter.forgotPassword(email.getText().toString().trim(), -1);
            }
        });

    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {
        message.setText(CacheUtil.getInstance().getTranslations(Text.FORGOT_PASSWORD_MESSAGE));
        email.setHint(CacheUtil.getInstance().getTranslations(Text.ENTER_EMAIL));
        submit.setText(CacheUtil.getInstance().getTranslations(Text.SUBMIT));
    }

    @Override
    public void signInEmail(String email, String password, int mobileFormatIndex) {

    }

    @Override
    public void forgotPassword(String phoneNumber, int mobileFormatIndex) {

    }

    @Override
    public void signInFacebook() {

    }

    @Override
    public void signInGoogle() {

    }

    @Override
    public void signUp(String fullName, String phoneNumber, String email, String password, int index) {

    }

    @Override
    public void showErrorEmail(String error) {
        if(error.isEmpty()){
            emailError.setVisibility(View.GONE);
        } else {
            emailError.setText(error);
            emailError.setVisibility(View.VISIBLE);
            dismissDialogs();
        }
    }

    @Override
    public void showErrorPassword(String error) {

    }

    @Override
    public void showErrorUsername(String error) {

    }

    @Override
    public void showMessage(String message) {
        dismissDialogs();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showError(String error) {
        showAlertDialog("", error, CacheUtil.getInstance().getTranslations(Text.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                onBackPressed();
                finish();
            }
        });
    }

    @Override
    public void showSuccess(String success) {
        dismissDialogs();
        showAlertDialog("", success, CacheUtil.getInstance().getTranslations(Text.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                onBackPressed();
                finish();
            }
        });
    }

    @Override
    public void showErrorFirstName(String error) {

    }

    @Override
    public void showErrorPhoneNumber(String error) {

    }


    @Override
    public void showGuest() {

    }

    @Override
    public void showPhoneNumber(String customerId, String email, String password) {

    }

    @Override
    public void setupCodeBroadcastListener() {

    }

    @Override
    public void showCode(boolean guest, String customerId, String email, String password, String phoneNumber) {

    }

    @Override
    public void showMain() {

    }

    @Override
    public void changeLanguage(String language) {

    }

    @Override
    public void logSigninEvent(String customerId) {

    }

    @Override
    public void logRegistrationEvent(String registrationMethod) {}
}
