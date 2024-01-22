package com.herfy.account.views;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.herfy.R;
import com.herfy.account.AccountContract;
import com.herfy.account.AccountPresenter;
import com.herfy.account.adapters.HintAdapter;
import com.herfy.account.adapters.LocationsRecyclerViewAdapter;
import com.herfy.account.adapters.AccountDeleteSavedCardsAdapter;
import com.herfy.auth.views.AuthActivity;
import com.herfy.base.BaseActivity;
import com.herfy.constants.Text;
import com.herfy.payment.savedcards.DeleteCardDialog;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.SoloAuth;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard;
import com.herfy.util.AppLogger;
import com.herfy.util.AuthUtil;
import com.herfy.util.BuildUtil;
import com.herfy.util.CacheUtil;
import com.herfy.analytics.MixpanelUtil;
import com.herfy.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class EditProfileActivity extends BaseActivity implements AccountContract.View {

    private AccountContract.Presenter accountPresenter;

    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.back)
    MaterialButton back;
    @BindView(R.id.cart)
    ConstraintLayout cart;

    @BindView(R.id.first_name_label)
    TextView firstNameLabel;
    @BindView(R.id.last_name_label)
    TextView lastNameLabel;
    @BindView(R.id.date_of_birth_label)
    TextView dateOfBirthLabel;
    @BindView(R.id.date_of_birth)
    TextView dateOfBirth;
    @BindView(R.id.gender_label)
    TextView genderLabel;
    @BindView(R.id.username_label)
    TextView emailAddressLabel;
    @BindView(R.id.phone_number_label)
    TextView phoneNumberLabel;
    @BindView(R.id.first_name_error)
    TextView firstNameError;
    @BindView(R.id.last_name_error)
    TextView lastNameError;
    @BindView(R.id.phone_number_error)
    TextView phoneNumberError;
    @BindView(R.id.email_error) TextView emailError;
    @BindView(R.id.new_password_error)
    TextView newPasswordError;
    @BindView(R.id.confirm_password_error)
    TextView confirmPasswordError;

    @BindView(R.id.first_name)
    EditText firstName;
    @BindView(R.id.last_name)
    EditText lastName;
    @BindView(R.id.phone_number)
    EditText phoneNumber;
    @BindView(R.id.username)
    EditText email;
    @BindView(R.id.new_password)
    EditText newPassword;
    @BindView(R.id.confirm_password)
    EditText confirmPassword;

    @BindView(R.id.gender_container)
    LinearLayout genderContainer;
    @BindView(R.id.password_container)
    LinearLayout passwordContainer;

    @BindView(R.id.gender_spinner)
    Spinner genderSpinner;

    @BindView(R.id.username_container)
    CardView emailContainer;

    @BindView(R.id.update)
    MaterialButton update;
    @BindView(R.id.delete)
    MaterialButton delete;

    @BindView(R.id.bookmarked_addresses_label)
    TextView addressLabel;
    @BindView(R.id.rv_bookmarked_addresses)
    RecyclerView rvAddresses;

    @BindView(R.id.saved_cards_label)
    TextView savedCardsLabel;
    @BindView(R.id.rv_saved_cards)
    RecyclerView rvSavedCards;
    private AccountDeleteSavedCardsAdapter accountSavedCardsAdapter;
    private List<CustomerCard> cards = new ArrayList<>();
    private DeleteCardDialog deleteCardDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        ButterKnife.bind(this);
        accountPresenter = new AccountPresenter(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        accountPresenter.start();
        accountPresenter.getPassword();
        accountPresenter.getLocationsFromExtra(getIntent().getExtras().getString("locations", ""));
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

        back.setOnClickListener(view -> onBackPressed());

        genderContainer.setOnClickListener(view -> {
            genderSpinner.performClick();
        });

        String key = CacheUtil.getInstance().getConceptKey();
        if (key.equalsIgnoreCase("CQcFAwgLDA0")) {
            emailAddressLabel.setVisibility(View.GONE);
            emailContainer.setVisibility(View.GONE);
        }

        confirmPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    update.performClick();
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(confirmPassword.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoadingDialog();
                accountPresenter.save(firstName.getText().toString().trim(), lastName.getText().toString().trim(), dateOfBirth.getText().toString(), (String) genderSpinner.getSelectedItem(), StringUtil.toEnglish(phoneNumber.getText().toString().trim().replace("+", "")), AuthUtil.getInstance().getCustomer().getAttributes().getEmail(), newPassword.getText().toString().trim(), confirmPassword.getText().toString().trim());
            }
        });

        if (accountSavedCardsAdapter == null) {
            accountSavedCardsAdapter = new AccountDeleteSavedCardsAdapter(this, cards, (maker, position) -> {
                if (deleteCardDialog != null) {
                    deleteCardDialog.dismiss();
                }

                deleteCardDialog = DeleteCardDialog.newInstance(new DeleteCardDialog.OnDeleteAction() {
                    @Override
                    public void onClose() {
                        deleteCardDialog.dismiss();
                    }

                    @Override
                    public void onProceed() {
                        deleteCardDialog.dismiss();
                        showLoadingDialog();
                        CustomersApiCall.getInstance().deleteCard(AuthUtil.getInstance().getCustomer().getId(), maker.getId() + "",
                                new ApiRequestListener() {
                                    @Override
                                    public void onSuccess(Object o) {
                                        dismissDialogs();
                                        cards.remove(position);
                                        accountSavedCardsAdapter.notifyDataSetChanged();
                                        if (cards.isEmpty()) {
                                            CacheUtil.getInstance().setCustomerCards(null);
                                            savedCardsLabel.setVisibility(View.GONE);
                                        } else {
                                            CacheUtil.getInstance().setCustomerCards(new Gson().toJson(cards));
                                            savedCardsLabel.setVisibility(View.VISIBLE);
                                        }
                                    }

                                    @Override
                                    public void onError(Object o) {
                                        dismissDialogs();
                                        AppLogger.printRetrofitError(o);
                                        if (o instanceof JSONObject) {
                                            try {
                                                JSONObject object = (JSONObject) o;
                                                Object error = object.get("error");
                                                String detail = CacheUtil.getInstance().getTranslations(Text.SORRY_ERROR_PAYMENT);
                                                if (error instanceof JSONObject) {
                                                    detail = ((JSONObject) error).getString("detail");
                                                } else if (error instanceof JSONArray) {
                                                    JSONObject errorObject = ((JSONArray) error).getJSONObject(0);
                                                    detail = errorObject.getString("detail");
                                                }
                                                showAlertDialog("", detail);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                                showAlertDialog("", CacheUtil.getInstance().getTranslations(Text.SORRY_ERROR_PAYMENT));
                                            }
                                        } else {
                                            showAlertDialog("", CacheUtil.getInstance().getTranslations(Text.SORRY_ERROR_PAYMENT));
                                        }
                                    }
                                });
                    }
                });
                deleteCardDialog.setCancelable(false);
                deleteCardDialog.show(getSupportFragmentManager(), DeleteCardDialog.class.toString());
            });
        }
        rvSavedCards.setAdapter(accountSavedCardsAdapter);
        setupCards();
    }

    @Override
    public void setupFonts() {
    }

    private void setupCards() {
        cards.clear();
        cards.addAll(CacheUtil.getInstance().getCustomerCards());
        if (cards.isEmpty()) {
            savedCardsLabel.setVisibility(View.GONE);
        } else {
            savedCardsLabel.setVisibility(View.VISIBLE);
        }
        accountSavedCardsAdapter.notifyDataSetChanged();
    }

    @Override
    public void setupTranslations() {
        back.setText(CacheUtil.getInstance().getTranslations(Text.BACK));
        title.setText(CacheUtil.getInstance().getTranslations(Text.ACCOUNT));
        firstNameLabel.setText(CacheUtil.getInstance().getTranslations(Text.FIRST_NAME));
        lastNameLabel.setText(CacheUtil.getInstance().getTranslations(Text.LAST_NAME, "LAST NAME"));
        dateOfBirthLabel.setText(CacheUtil.getInstance().getTranslations(Text.DATE_OF_BIRTH_CAPS, "DATE OF BIRTH"));
        genderLabel.setText(CacheUtil.getInstance().getTranslations(Text.GENDER_CAPS, "GENDER"));
        emailAddressLabel.setText(CacheUtil.getInstance().getTranslations(Text.EMAIL_ADDRESS));
        phoneNumberLabel.setText(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER));
        firstName.setHint(CacheUtil.getInstance().getTranslations(Text.ENTER_FIRST_NAME));
        lastName.setHint(CacheUtil.getInstance().getTranslations(Text.ENTER_LAST_NAME));
        phoneNumber.setHint(CacheUtil.getInstance().getTranslations(Text.ENTER_PHONE_NUMBER_CAPS));
        email.setHint(CacheUtil.getInstance().getTranslations(Text.ENTER_EMAIL));
        newPassword.setHint(CacheUtil.getInstance().getTranslations(Text.ENTER_NEW_PASSWORD));
        confirmPassword.setHint(CacheUtil.getInstance().getTranslations(Text.CONFIRM_NEW_PASSWORD));
        addressLabel.setText(CacheUtil.getInstance().getTranslations(Text.BOOKMARKED_ADDRESSES_CAPS));
        update.setText(CacheUtil.getInstance().getTranslations(Text.UPDATE_ACCOUNT_CAPS, "UPDATE"));
        delete.setText(CacheUtil.getInstance().getTranslations(Text.DELETE_ACCOUNT_CAPS, "DELETE ACCOUNT"));
        savedCardsLabel.setText(CacheUtil.getInstance().getTranslations(Text.SAVED_CARDS, "SAVED CARDS").toUpperCase());
    }

    @Override
    public void showCustomer(String firstNameValue, String lastNameValue, String gender, String phoneNumberValue, String emailValue, String providerValue) {
        firstName.append(firstNameValue);
        lastName.append(lastNameValue);

        List<String> genderOptions = accountPresenter.getGenderOptions();
        HintAdapter genderAdapter = new HintAdapter(this, R.layout.selected_account_spinner, genderOptions);
        genderAdapter.setDropDownViewResource(R.layout.item_account_spinner);
        genderSpinner.setAdapter(genderAdapter);
        genderSpinner.setSelection(accountPresenter.getSelectedGender(gender, genderOptions));
        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                TextView tv = (TextView) view;
                tv.setTextColor(position == 0 ? lastName.getCurrentHintTextColor() : Color.BLACK);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        String key = CacheUtil.getInstance().getConceptKey();
        if (key.equalsIgnoreCase("CQcFAwgLDA0")) {
            phoneNumberValue = CacheUtil.getInstance().getConcept().getDialingCode() + phoneNumberValue.substring(1);
            email.setEnabled(true);
            phoneNumber.setEnabled(false);
        } else {
            email.setEnabled(false);
//            phoneNumber.setEnabled(true);
        }
        phoneNumber.append(StringUtil.localize(phoneNumberValue));
        email.append(emailValue);

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

        boolean isGuest = !AuthUtil.getInstance().getCustomer().getAttributes().getAccountType().equalsIgnoreCase("full");
        delete.setVisibility(isGuest ? View.GONE : View.VISIBLE);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeleteAccountDialog dialog = DeleteAccountDialog.newInstance(new DeleteAccountDialog.OnAction() {
                    @Override
                    public void onClose() {

                    }

                    @Override
                    public void onDelete() {
                        showLoadingDialog();
                        accountPresenter.deleteAccount();
                    }
                });
                dialog.setCancelable(true);
                dialog.show(getSupportFragmentManager(), DeleteAccountDialog.class.toString());
            }
        });

    }

    @Override
    public void showDateOfBirth(Date birthDate, String date) {
        Calendar calendar = Calendar.getInstance();
        if(date != null && !date.isEmpty()) {
            calendar.setTime(birthDate);
            dateOfBirth.setText(date);
            dateOfBirth.setTextColor(Color.BLACK);
        }  else {
            calendar.setTime(new Date());
            dateOfBirth.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_DATE_OF_BIRTH, "Select date of birth"));
            dateOfBirth.setTextColor(lastName.getCurrentHintTextColor());
        }
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (datePicker, year, month, day) -> {},
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMaxDate(new Date().getTime());
        datePickerDialog.setButton(
                DatePickerDialog.BUTTON_POSITIVE,
                CacheUtil.getInstance().getTranslations(Text.SELECT, "SELECT"),
                (dialogInterface, i) -> {
                    DatePicker datePicker = datePickerDialog.getDatePicker();
                    calendar.set(Calendar.YEAR, datePicker.getYear());
                    calendar.set(Calendar.MONTH, datePicker.getMonth());
                    calendar.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
                    dateOfBirth.setTextColor(Color.BLACK);
                    dateOfBirth.setText(sdf.format(calendar.getTime()));
                    dialogInterface.dismiss();
                }
        );
        datePickerDialog.setButton(
                DatePickerDialog.BUTTON_NEGATIVE,
                CacheUtil.getInstance().getTranslations(Text.CANCEL, "CANCEL"),
                (dialogInterface, i) -> dialogInterface.dismiss()
        );

        dateOfBirth.setOnClickListener(view -> datePickerDialog.show());
    }

    @Override
    public void showEmail(boolean show) {
        //emailContainer.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showPassword(boolean show) {
//        passwordContainer.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showErrorFirstName(String error) {
        if (error.isEmpty()) {
            firstNameError.setVisibility(View.GONE);
        } else {
            firstNameError.setText(error);
            firstNameError.setVisibility(View.VISIBLE);
            dismissDialogs();
        }
    }

    @Override
    public void showErrorLastName(String error) {
        if (error.isEmpty()) {
            lastNameError.setVisibility(View.GONE);
        } else {
            lastNameError.setText(error);
            lastNameError.setVisibility(View.VISIBLE);
            dismissDialogs();
        }
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
    public void showErrorEmail(String error) {
        if(error.isEmpty()) {
            emailError.setVisibility(View.GONE);
        } else {
            emailError.setText(error);
            emailError.setVisibility(View.VISIBLE);
            dismissDialogs();
        }
    }

    @Override
    public void showErrorNewPassword(String error) {
        if (error.isEmpty()) {
            newPasswordError.setVisibility(View.GONE);
        } else {
            newPasswordError.setText(error);
            newPasswordError.setVisibility(View.VISIBLE);
            dismissDialogs();
        }
    }

    @Override
    public void showErrorConfirmPassword(String error) {
        if (error.isEmpty()) {
            confirmPasswordError.setVisibility(View.GONE);
        } else {
            confirmPasswordError.setText(error);
            confirmPasswordError.setVisibility(View.VISIBLE);
            dismissDialogs();
        }
    }

    @Override
    public void showErrorConfirmNewPassword(String error) {

    }

    @Override
    public void showPasswordUpdated() {

    }

    @Override
    public void showError(String error) {
        showAlertDialog("", error);
    }

    @Override
    public void showAccount() {
        try {
            dismissDialogs();
            Toast.makeText(this, CacheUtil.getInstance().getTranslations(Text.PROFILE_UPDATED), Toast.LENGTH_SHORT).show();
            onBackPressed();
            finish();
        } catch (NullPointerException e) {
        }
    }

    @Override
    public void showLocations(ArrayList<Address> locations) {

        if (locations.size() > 0) {
            addressLabel.setVisibility(View.VISIBLE);
            LocationsRecyclerViewAdapter addressesAdapter = new LocationsRecyclerViewAdapter(accountPresenter, true);
            rvAddresses.setAdapter(addressesAdapter);
            rvAddresses.setVisibility(View.VISIBLE);
        } else {
            addressLabel.setVisibility(View.GONE);
            rvAddresses.setVisibility(View.GONE);
        }
        dismissDialogs();

    }

    @Override
    public void hideLocations() {
        addressLabel.setVisibility(View.GONE);
        rvAddresses.setVisibility(View.GONE);
    }

    @Override
    public void setLocation(Address address) {

    }

    @Override
    public void showMessage(String message) {

    }

    @Override
    public void accountDeleted() {
        dismissDialogs();
        AccountDialogFragment fragment = AccountDialogFragment.newInstance();
        fragment.show(getSupportFragmentManager(), AccountDialogFragment.class.getSimpleName());
        try {
            new Handler().postDelayed(() -> {
                LoginManager.getInstance().logOut();
                GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(BuildUtil.googleInClient(EditProfileActivity.this))
                        .requestEmail()
                        .build();
                GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(EditProfileActivity.this, googleSignInOptions);
                googleSignInClient.signOut();
                CacheUtil.getInstance().clearOrder();
                SoloAuth.getInstance().logout();
                MixpanelUtil.Companion.logout();
                AuthUtil.getInstance().setCustomer(null);
                Intent intent = new Intent(this, AuthActivity.class);
                intent.putExtra("from_delete", true);
                startActivity(intent);
                finish();
            }, 2300);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void showCustomerCards() {

    }

    @Override
    public void showRevelRewardPoints(Double pointValue) {

    }

}
