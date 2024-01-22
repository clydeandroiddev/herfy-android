package com.herfy.auth.views;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.daimajia.androidanimations.library.Techniques;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.LoggingBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.herfy.R;
import com.herfy.auth.AuthContract;
import com.herfy.auth.AuthPresenter;
import com.herfy.base.BaseActivity;
import com.herfy.constants.Text;
import com.herfy.constants.Values;
import com.herfy.main.MainActivity;
import com.herfy.order.views.OrderTypeActivity;
import com.skylinedynamics.solosdk.api.models.objects.AllowedCountries;
import com.herfy.util.AppLogger;
import com.herfy.util.BuildUtil;
import com.herfy.util.CacheUtil;
import com.herfy.analytics.FacebookAppEventUtil;
import com.herfy.util.StringUtil;
import com.herfy.verification.CodeBroadcastReceiver;
import com.herfy.verification.views.CodeDialogFragment;
import com.herfy.verification.views.VerifyCodeDialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GuestActivity extends BaseActivity implements AuthContract.View, FacebookCallback<LoginResult> {

    private AuthContract.Presenter authPresenter;

    private boolean orderHistory = false;
    private boolean cart = false;
    private boolean orderType = false;
    private boolean favoriteHome = false;
    private boolean favoriteMenuItem = false;
    private boolean curbside = false;

    @BindView(R.id.back)
    Button back;
    @BindView(R.id.guest)
    Button guest;
    @BindView(R.id.google)
    Button google;
//    @BindView(R.id.facebook) Button facebook;

    @BindView(R.id.tvTitle)
    TextView title;
    @BindView(R.id.first_name_label)
    TextView firstNameLabel;
    @BindView(R.id.first_name_error)
    TextView firstNameError;
    @BindView(R.id.phone_number_label)
    TextView phoneNumberLabel;
    @BindView(R.id.phone_number_prefix)
    Spinner phoneNumberPrefix;
    @BindView(R.id.phone_number_error)
    TextView phoneNumberError;
    @BindView(R.id.or)
    TextView or;

    @BindView(R.id.first_name_container)
    CardView firstNameContainer;
    @BindView(R.id.phone_number_container)
    CardView phoneNumberContainer;

    @BindView(R.id.first_name)
    EditText firstName;
    @BindView(R.id.phone_number)
    EditText phoneNumber;

    private CallbackManager facebookCallbackManager;

    private GoogleSignInClient googleSignInClient;

    private CodeDialogFragment codeDialogFragment;
    private VerifyCodeDialogFragment verifyCodeDialogFragment;

    private CodeBroadcastReceiver codeBroadcastReceiver;

    private ActivityResultLauncher<Intent> googleLauncher;
    private ActivityResultLauncher<Intent> codeLauncher;

    private ArrayList<AllowedCountries> allowedCountries = new ArrayList<>();
    private String selectedDialCode = "";
    private int mobileFormatIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest);
        ButterKnife.bind(this);
        authPresenter = new AuthPresenter(this);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.containsKey("curbside")) {
                curbside = bundle.getBoolean("curbside");
            }
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
        }

        googleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result != null && result.getData() != null) {
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            try {
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                if (account != null) {
                                    showLoadingDialog();
                                    authPresenter.signInGoogle(account.getEmail(), account.getIdToken());
                                } else {
                                    showMessage(CacheUtil.getInstance().getTranslations(Text.GOOGLE_ERROR));
                                }
                            } catch (ApiException e) {
                                e.printStackTrace();
                                showMessage(CacheUtil.getInstance().getTranslations(Text.GOOGLE_ERROR));
                            }
                        }
                    }
                });

        codeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result != null && result.getData() != null) {
                            try {
                                String message = result.getData().getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE);
                                if (message != null) {
                                    String otpLength = String.valueOf(CacheUtil.getInstance().getConcept().getOtpLength());
                                    Pattern p = Pattern.compile(String.format("\\b\\d{%s}\\b", otpLength));
                                    Matcher m = p.matcher(message);
                                    String codeValue = "";
                                    while (m.find()) {
                                        codeValue = m.group(0);
                                    }
                                    if (verifyCodeDialogFragment != null) {
                                        verifyCodeDialogFragment.setCode(codeValue);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

        FacebookSdk.setIsDebugEnabled(true);
        FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        authPresenter.start();

        facebookCallbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(facebookCallbackManager, this);

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildUtil.googleInClient(GuestActivity.this))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Values.REQUEST_SMS_CONSENT && resultCode == RESULT_OK) {
            String message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE);
            if (message != null) {
                Pattern p = Pattern.compile("\\b\\d{6}\\b");
                Matcher m = p.matcher(message);
                String codeValue = "";
                while (m.find()) {
                    codeValue = m.group(0);
                }
                if (codeDialogFragment != null) {
                    codeDialogFragment.setCode(codeValue);
                }
            }
        } else if (requestCode == Values.PROVIDER_GOOGLE) {

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    authPresenter.signInGoogle(account.getEmail(), account.getIdToken());
                } else {
                    showAlertDialog("", CacheUtil.getInstance().getTranslations(Text.GOOGLE_ERROR));
                }
            } catch (ApiException e) {
                e.printStackTrace();
                AppLogger.error(e.getMessage());
                showAlertDialog("", CacheUtil.getInstance().getTranslations(Text.GOOGLE_ERROR));
            }

        } else {
            facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (codeBroadcastReceiver != null) {
            unregisterReceiver(codeBroadcastReceiver);
        }
        if(googleLauncher != null) {
            googleLauncher = null;
        }
        if(codeLauncher != null) {
            codeLauncher = null;
        }
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

        if (CacheUtil.getInstance().getConcept().getSocialLoginOption().contains("google")) {
            or.setVisibility(View.VISIBLE);
            google.setVisibility(View.VISIBLE);
        } else {
            or.setVisibility(View.GONE);
            google.setVisibility(View.GONE);
        }

        animateSpringView(Techniques.SlideInUp, 300, back, 8);
        animateSpringView(Techniques.SlideInUp, 300, title, 8);
        animateSpringView(Techniques.SlideInUp, 350, firstNameLabel, 8);
        animateSpringView(Techniques.SlideInUp, 400, firstNameContainer, 8);
        animateSpringView(Techniques.SlideInUp, 450, phoneNumberLabel, 8);
        animateSpringView(Techniques.SlideInUp, 500, phoneNumberContainer, 8);
        animateSpringView(Techniques.SlideInUp, 550, guest, 8);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        allowedCountries.addAll(CacheUtil.getInstance().getConcept().getAllowedCountries());

        if(selectedDialCode.isEmpty()) selectedDialCode = allowedCountries.get(0).getDialCode();
        if(mobileFormatIndex == -1 && allowedCountries.size() > 0) mobileFormatIndex = 0;

        phoneNumberPrefix.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String prefix = allowedCountries.get(position).getDialCode();
                selectedDialCode = StringUtil.localize(prefix.contains("+") ? prefix : "+" + prefix);
                mobileFormatIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        AllowedCountriesAdapter adapter = new AllowedCountriesAdapter(this, allowedCountries);
        phoneNumberPrefix.setAdapter(adapter);

        phoneNumber.setFilters(new InputFilter[]{new InputFilter.LengthFilter(13 - selectedDialCode.replace("+", "").length())});

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

        phoneNumber.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    guest.performClick();
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(phoneNumber.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        guest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoadingDialog();

                authPresenter.guest(
                        firstName.getText().toString().trim(),
                        StringUtil.toEnglish(selectedDialCode + phoneNumber.getText().toString().trim()),
                        mobileFormatIndex
                );
            }
        });

//        facebook.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                LoginManager.getInstance().logOut();
//                showLoadingDialog();
//                FacebookSdk.setApplicationId(getString(R.string.facebook_app_id));
//                Collection<String> permissions = Arrays.asList("public_profile", "email");
//                LoginManager.getInstance().logInWithReadPermissions(GuestActivity.this, permissions);
//            }
//        });

        google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googleSignInClient.signOut();
                showLoadingDialog();
                Intent signInIntent = googleSignInClient.getSignInIntent();
                //startActivityForResult(signInIntent, Values.PROVIDER_GOOGLE);
                googleLauncher.launch(signInIntent);
            }
        });

    }

    @Override
    public void setupFonts() {
        /*message.setTypeface(FontHandler.getSemiboldTypeface());
        personalInfo.setTypeface(FontHandler.getSemiboldTypeface());
        facebookLabel.setTypeface(FontHandler.getSemiboldTypeface());
        googleLabel.setTypeface(FontHandler.getSemiboldTypeface());*/
    }

    @Override
    public void setupTranslations() {
        title.setText(CacheUtil.getInstance().getTranslations(Text.LOG_IN_AS_GUEST_CAPS));
        back.setText(CacheUtil.getInstance().getTranslations(Text.BACK));
        firstName.setHint(CacheUtil.getInstance().getTranslations(Text.ENTER_YOUR_FIRSTNAME_AND_LASTNAME, "Enter your first name and last name"));
        firstNameLabel.setText(CacheUtil.getInstance().getTranslations(Text.FULL_NAME));
        phoneNumberLabel.setText(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER));
        guest.setText(CacheUtil.getInstance().getTranslations(Text.SIGN_IN_AS_GUEST, "SIGN IN AS GUEST"));
        or.setText(CacheUtil.getInstance().getTranslations(Text.OR));
        google.setText(CacheUtil.getInstance().getTranslations(Text.SIGN_IN_WITH_GOOGLE));
//        facebook.setText(CacheUtil.getInstance().getTranslations(Text.SIGN_IN_WITH_FACEBOOK));
    }

    @Override
    public void showGuest() {

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
        showAlertDialog("", error);
    }

    @Override
    public void showSuccess(String success) {

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
    }

    @Override
    public void showPhoneNumber(String customerId, String email, String password) {
        dismissDialogs();
        if (codeDialogFragment != null) {
            codeDialogFragment.dismiss();
        }
        codeBroadcastReceiver = new CodeBroadcastReceiver();
        codeBroadcastReceiver.setCodeBroadcastReceiverListener(new CodeBroadcastReceiver.CodeBroadcastReceiverListener() {
            @Override
            public void onReceived(Intent intent) {
                if (intent != null) {
                    try {
                        codeLauncher.launch(intent);
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
        task.addOnSuccessListener(aVoid -> {
        });
        task.addOnFailureListener(e -> AppLogger.error(e.getMessage()));

        Bundle args = new Bundle();
        args.putBoolean("guest", false);
        args.putBoolean("order_history", orderHistory);
        args.putBoolean("cart", cart);
        args.putBoolean("order_type", orderType);
        args.putBoolean("favorite_home", favoriteHome);
        args.putBoolean("favorite_menu_item", favoriteMenuItem);
        args.putString("customer_id", customerId);
        args.putString("email", email);
        args.putString("password", password);
        args.putString("phone_number", "");

        codeDialogFragment = CodeDialogFragment.newInstance();
        codeDialogFragment.setArguments(args);
        codeDialogFragment.show(getSupportFragmentManager(), CodeDialogFragment.class.getSimpleName());
    }

    @Override
    public void setupCodeBroadcastListener() {

    }

    @Override
    public void showCode(boolean guest, String customerId, String email, String password, String phoneNumber) {
        dismissDialogs();
        if (codeDialogFragment == null || codeDialogFragment.getDialog() == null || !codeDialogFragment.getDialog().isShowing()) {

            codeBroadcastReceiver = new CodeBroadcastReceiver();
            codeBroadcastReceiver.setCodeBroadcastReceiverListener(new CodeBroadcastReceiver.CodeBroadcastReceiverListener() {
                @Override
                public void onReceived(Intent intent) {
                    if (intent != null) {
                        try {
                            codeLauncher.launch(intent);
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
            task.addOnSuccessListener(aVoid -> {
            });
            task.addOnFailureListener(e -> AppLogger.error(e.getMessage()));

        } else {
            codeDialogFragment.dismiss();
        }

        Bundle args = new Bundle();
        args.putBoolean("guest", guest);
        args.putBoolean("order_history", orderHistory);
        args.putBoolean("cart", cart);
        args.putBoolean("order_type", orderType);
        args.putBoolean("favorite_home", favoriteHome);
        args.putBoolean("favorite_menu_item", favoriteMenuItem);
        args.putString("customer_id", customerId);
        args.putString("email", email);
        args.putString("password", password);
        args.putString("phone_number", phoneNumber);

        verifyCodeDialogFragment = VerifyCodeDialogFragment.newInstance();
        verifyCodeDialogFragment.setArguments(args);
        verifyCodeDialogFragment.show(getSupportFragmentManager(), VerifyCodeDialogFragment.class.getSimpleName());

        if(!authPresenter.getRegistrationMethod().isEmpty()) {
            logRegistrationEvent(authPresenter.getRegistrationMethod());
        }

    }

    @Override
    public void showMain() {
        if (codeDialogFragment != null) {
            codeDialogFragment.dismiss();
        }
        if (verifyCodeDialogFragment != null) {
            verifyCodeDialogFragment.dismiss();
        }
        dismissDialogs();
        Toast.makeText(this, CacheUtil.getInstance().getTranslations(Text.SIGNED_IN), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        if (curbside) {
            setResult(RESULT_OK, new Intent());
            finish();
        } else if (!orderType) {
            intent.putExtra("cart", cart);
            /*if (orderHistory) {
                intent = new Intent(this, OrdersActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            } else*/ if (favoriteHome) {
                intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            } else if (favoriteMenuItem) {
                //intent = new Intent(this, MenuItemActivity.class);
                intent = new Intent(this, MainActivity.class);
                intent.putExtra("menu", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            } else {
                intent.putExtra("home", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }
        } else {
            intent = new Intent(this, OrderTypeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        }
        intent.putExtra("sign_in", true);
        startActivity(intent);
        finish();
    }

    @Override
    public void changeLanguage(String language) {

    }

    @Override
    public void logSigninEvent(String customerId) {

    }

    @Override
    public void logRegistrationEvent(String registrationMethod) {
        FacebookAppEventUtil.Companion.completeRegistrationEvent(this, registrationMethod);
    }

    @Override
    public void onSuccess(LoginResult loginResult) {

        AccessToken accessToken = loginResult.getAccessToken();
        String token = accessToken.getToken();

        GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {

            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                if (object.has("email")) {

                    try {
                        showLoadingDialog();
                        authPresenter.signInFacebook(object.getString("email"), token);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showError(CacheUtil.getInstance().getTranslations(Text.FACEBOOK_ERROR));
                    }

                } else {
                    showError(CacheUtil.getInstance().getTranslations(Text.FACEBOOK_ERROR));
                }
            }

        });

        Bundle bundle = new Bundle();
        bundle.putString("fields", "email");
        request.setParameters(bundle);
        request.executeAsync();

    }

    @Override
    public void onCancel() {
        dismissDialogs();
        showError(CacheUtil.getInstance().getTranslations(Text.FACEBOOK_ERROR));
    }

    @Override
    public void onError(FacebookException error) {
        dismissDialogs();
        showError(error.getMessage());
    }

}
