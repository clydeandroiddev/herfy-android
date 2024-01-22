package com.herfy.account.views;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.button.MaterialButton;
import com.herfy.R;
import com.herfy.account.AccountContract;
import com.herfy.account.AccountPresenter;
import com.herfy.base.BaseActivity;
import com.herfy.constants.Text;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.herfy.util.CacheUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChangePasswordActivity extends BaseActivity implements AccountContract.View {

    private AccountContract.Presenter accountPresenter;

    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.back)
    MaterialButton back;
    @BindView(R.id.action)
    MaterialButton action;
    @BindView(R.id.cart)
    ConstraintLayout cart;
    @BindView(R.id.confirm)
    MaterialButton confirm;
    @BindView(R.id.current_password_label)
    TextView currentPasswordLabel;
    @BindView(R.id.new_password_label)
    TextView newPasswordLabel;
    @BindView(R.id.confirm_new_password_label)
    TextView confirmNewPasswordLabel;
    @BindView(R.id.confirm_new_password)
    EditText confirmNewPassword;
    @BindView(R.id.new_password)
    EditText newPassword;
    @BindView(R.id.current_password)
    EditText currentPassword;
    @BindView(R.id.current_password_error)
    TextView currentPasswordError;
    @BindView(R.id.new_password_error)
    TextView newPasswordError;
    @BindView(R.id.confirm_new_password_error)
    TextView confirmNewPasswordError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        ButterKnife.bind(this);
        accountPresenter = new AccountPresenter(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        accountPresenter.start();
        accountPresenter.getPassword();
    }

    @Override
    public void setPresenter(AccountContract.Presenter presenter) {
        accountPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
    }

    @Override
    public void setupViews() {
        back.setVisibility(View.VISIBLE);
        cart.setVisibility(View.GONE);
        action.setVisibility(View.GONE);
        back.setOnClickListener(view -> onBackPressed());
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoadingDialog();
                accountPresenter.updatePassword(currentPassword.getText().toString(), newPassword.getText().toString(), confirmNewPassword.getText().toString());
            }
        });
    }

    @Override

    public void setupFonts() {
    }

    @Override
    public void setupTranslations() {
        back.setText(CacheUtil.getInstance().getTranslations(Text.BACK));
        title.setText(CacheUtil.getInstance().getTranslations(Text.CHANGE_PASSWORD).toUpperCase());
        confirm.setText(CacheUtil.getInstance().getTranslations("confirm_update_caps", "CONFIRM UPDATE").toUpperCase());
        confirmNewPasswordLabel.setText(CacheUtil.getInstance().getTranslations("confirm_new_password", "CONFIRM NEW PASSWORD").toUpperCase());
        newPasswordLabel.setText(CacheUtil.getInstance().getTranslations("new_password", "NEW PASSWORD").toUpperCase());
        currentPasswordLabel.setText(CacheUtil.getInstance().getTranslations("current_password", "CURRENT PASSWORD").toUpperCase());
    }

    @Override
    public void showCustomer(String firstName, String lastName, String gender, String phoneNumber, String email, String provider) {

    }

    @Override
    public void showDateOfBirth(Date birthDate, String date) {

    }

    @Override
    public void showEmail(boolean show) {
    }

    @Override
    public void showPassword(boolean show) {
    }

    @Override
    public void showErrorFirstName(String error) {

    }

    @Override
    public void showErrorLastName(String error) {

    }

    @Override
    public void showErrorPhoneNumber(String error) {

    }

    @Override
    public void showErrorEmail(String error) {

    }

    @Override
    public void showErrorNewPassword(String error) {
        try {
            if (error.isEmpty()) {
                newPasswordError.setVisibility(View.GONE);
            } else {
                dismissDialogs();
                newPasswordError.setText(error);
                newPasswordError.setVisibility(View.VISIBLE);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void showErrorConfirmPassword(String error) {
        try {
            if (error.isEmpty()) {
                currentPasswordError.setVisibility(View.GONE);
            } else {
                dismissDialogs();
                currentPasswordError.setText(error);
                currentPasswordError.setVisibility(View.VISIBLE);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void showErrorConfirmNewPassword(String error) {
        dismissDialogs();
        try {
            if (error.isEmpty()) {
                confirmNewPasswordError.setVisibility(View.GONE);
            } else {
                dismissDialogs();
                confirmNewPasswordError.setText(error);
                confirmNewPasswordError.setVisibility(View.VISIBLE);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void showPasswordUpdated() {
        dismissDialogs();
        try {
            Toast.makeText(this, CacheUtil.getInstance().getTranslations(Text.PASSWORD_UPDATED, "Password Updated"), Toast.LENGTH_SHORT).show();
            onBackPressed();
            finish();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void showError(String error) {
        dismissDialogs();
        showAlertDialog("", error);
    }

    @Override
    public void showAccount() {

    }

    @Override
    public void showLocations(ArrayList<Address> locations) {

    }

    @Override
    public void hideLocations() {

    }

    @Override
    public void setLocation(Address address) {

    }

    @Override
    public void showMessage(String message) {

    }

    @Override
    public void accountDeleted() {

    }

    @Override
    public void showCustomerCards() {

    }

    @Override
    public void showRevelRewardPoints(Double pointValue) {

    }

}
