package com.herfy.auth.views;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.herfy.R;
import com.herfy.auth.AuthContract;
import com.herfy.auth.AuthPresenter;
import com.herfy.base.BaseActivity;
import com.herfy.data.constants.Text;
import com.herfy.data.constants.Values;
import com.herfy.font.FontHandler;
import com.herfy.main.MainActivity;
import com.herfy.order.views.OrderTypeActivity;
import com.herfy.util.BuildUtil;
import com.herfy.util.CacheUtil;
import com.herfy.analytics.FirebaseECommerceEventUtil;
import com.herfy.verification.views.CodeActivity;
import com.herfy.verification.views.PhoneNumberActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SignInActivity extends BaseActivity implements AuthContract.View, FacebookCallback<LoginResult> {

    private AuthContract.Presenter authPresenter;

    private boolean orderHistory = false;
    private boolean cart = false;
    private boolean orderType = false;
    private boolean favoriteHome = false;
    private boolean favoriteMenuItem = false;

    @BindView(R.id.back) ImageButton back;

    @BindView(R.id.message) TextView message;
    @BindView(R.id.email_error) TextView emailError;
    @BindView(R.id.password_error) TextView passwordError;
    @BindView(R.id.or_guest) TextView orGuest;
    @BindView(R.id.or_social) TextView orSocial;
    @BindView(R.id.social) TextView social;
    @BindView(R.id.facebook_label) TextView facebookLabel;
    @BindView(R.id.google_label) TextView googleLabel;
    @BindView(R.id.account) TextView account;
    @BindView(R.id.sign_up) TextView signUp;
    @BindView(R.id.forgot_password) TextView forgotPassword;

    @BindView(R.id.email) TextInputEditText email;
    @BindView(R.id.password) TextInputEditText password;

    @BindView(R.id.sign_in) MaterialButton signIn;
    @BindView(R.id.guest) MaterialButton guest;

    @BindView(R.id.facebook_container) ConstraintLayout facebook;
    @BindView(R.id.google_container) ConstraintLayout google;

    private CallbackManager facebookCallbackManager;

    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        ButterKnife.bind(this);
        authPresenter = new AuthPresenter(this);

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
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        authPresenter.start();

        facebookCallbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(facebookCallbackManager, this);
        LoginManager.getInstance().setLoginBehavior(LoginBehavior.WEB_ONLY);

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildUtil.googleInClient(SignInActivity.this))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Values.PROVIDER_GOOGLE) {

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if(account != null) {
                    showLoadingDialog();
                    authPresenter.signInGoogle(account.getEmail(), account.getIdToken());
                } else {
                    showLoadingDialog();
                }
            } catch (ApiException e) {
                e.printStackTrace();
                showMessage(CacheUtil.getInstance().getTranslations(Text.GOOGLE_ERROR));
            }

        } else {
            facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        CacheUtil.getInstance().setFavoriteMenuCategory(null);
        CacheUtil.getInstance().setFavoriteMenuItem(null);
        if(orderType) {
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();
        } else {
            super.onBackPressed();
            finish();
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

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        password.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    signIn.performClick();
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(password.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoadingDialog();
                authPresenter.signIn(email.getText().toString().trim(), password.getText().toString().trim(), -1);
            }
        });

        guest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignInActivity.this, GuestActivity.class);
                intent.putExtra("order_history", orderHistory);
                intent.putExtra("cart", cart);
                intent.putExtra("order_type", orderType);
                intent.putExtra("favorite_home", favoriteHome);
                intent.putExtra("favorite_menu_item", favoriteMenuItem);
                startActivity(intent);
            }
        });

        facebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoginManager.getInstance().logOut();
                showLoadingDialog();
                FacebookSdk.setApplicationId(getString(R.string.facebook_app_id));
                Collection<String> permissions = Arrays.asList("public_profile", "email");
                LoginManager.getInstance().logInWithReadPermissions(SignInActivity.this, permissions);
            }
        });

        google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googleSignInClient.signOut();
                showLoadingDialog();
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, Values.PROVIDER_GOOGLE);
            }
        });

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
                intent.putExtra("order_history", orderHistory);
                intent.putExtra("cart", cart);
                intent.putExtra("order_type", orderType);
                intent.putExtra("favorite_home", favoriteHome);
                intent.putExtra("favorite_menu_item", favoriteMenuItem);
                startActivity(intent);
            }
        });

        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignInActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    public void setupFonts() {
        message.setTypeface(FontHandler.getSemiboldTypeface());
        signIn.setTypeface(FontHandler.getSemiboldTypeface());
        facebookLabel.setTypeface(FontHandler.getSemiboldTypeface());
        googleLabel.setTypeface(FontHandler.getSemiboldTypeface());
    }

    @Override
    public void setupTranslations() {
        message.setText(CacheUtil.getInstance().getTranslations(Text.SIGN_IN_MESSAGE));
        orGuest.setText(CacheUtil.getInstance().getTranslations(Text.OR));
        orSocial.setText(CacheUtil.getInstance().getTranslations(Text.OR));
        social.setText(CacheUtil.getInstance().getTranslations(Text.SIGN_IN_SOCIAL_ACCOUNT));
        email.setHint(CacheUtil.getInstance().getTranslations(Text.EMAIL_ADDRESS));
        password.setHint(CacheUtil.getInstance().getTranslations(Text.PASSWORD));
        signIn.setText(CacheUtil.getInstance().getTranslations(Text.SIGN_IN));
        guest.setText(CacheUtil.getInstance().getTranslations(Text.CONTINUE_GUEST, "Continue as Guest"));
        account.setText(CacheUtil.getInstance().getTranslations(Text.DONT_HAVE_ACCOUNT));
        signUp.setText(CacheUtil.getInstance().getTranslations(Text.SIGN_UP));
        forgotPassword.setText(CacheUtil.getInstance().getTranslations(Text.FORGOT_PASSWORD));
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
        if(error.isEmpty()){
            passwordError.setVisibility(View.GONE);
        } else {
            passwordError.setText(error);
            passwordError.setVisibility(View.VISIBLE);
            dismissDialogs();
        }
    }

    @Override
    public void showErrorUsername(String error) {
        if(error.isEmpty()){
            emailError.setVisibility(View.GONE);
        } else {
            emailError.setText(error);
            emailError.setVisibility(View.VISIBLE);
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
    public void showSuccess(String success) {

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
        dismissDialogs();
        Intent intent = new Intent(this, PhoneNumberActivity.class);
        intent.putExtra("order_history", orderHistory);
        intent.putExtra("cart", cart);
        intent.putExtra("order_type", orderType);
        intent.putExtra("favorite_home", favoriteHome);
        intent.putExtra("favorite_menu_item", favoriteMenuItem);
        intent.putExtra("customer_id", customerId);
        intent.putExtra("email", email);
        intent.putExtra("password", password);
        startActivity(intent);
    }

    @Override
    public void setupCodeBroadcastListener() {

    }

    @Override
    public void showCode(boolean guest, String customerId, String email, String password, String phoneNumber) {
        dismissDialogs();
        Intent intent = new Intent(this, CodeActivity.class);
        intent.putExtra("order_history", orderHistory);
        intent.putExtra("cart", cart);
        intent.putExtra("order_type", orderType);
        intent.putExtra("favorite_home", favoriteHome);
        intent.putExtra("favorite_menu_item", favoriteMenuItem);
        intent.putExtra("guest", guest);
        intent.putExtra("customer_id", customerId);
        intent.putExtra("email", email);
        intent.putExtra("password", password);
        intent.putExtra("phone_number", phoneNumber);
        startActivity(intent);
    }

    @Override
    public void showMain() {
        dismissDialogs();
        Toast.makeText(this, CacheUtil.getInstance().getTranslations(Text.SIGNED_IN), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        if(!orderType) {
            intent.putExtra("cart", cart);
            /*if(orderHistory) {
                intent = new Intent(this, OrdersActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            } else*/ if(favoriteHome) {
                intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            } else if(favoriteMenuItem) {
                //intent = new Intent(this, MenuItemActivity.class);
                intent = new Intent(this, MainActivity.class);
                intent.putExtra("menu", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            } else {
                intent.putExtra("home", true);
                intent.putExtra("menu", true);
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
        FirebaseECommerceEventUtil.Companion.signInEvent(this,customerId);
    }

    @Override
    public void logRegistrationEvent(String registrationMethod) {}

    @Override
    public void onSuccess(LoginResult loginResult) {

        AccessToken accessToken = loginResult.getAccessToken();
        String token = accessToken.getToken();

        GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {

            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                if(object.has("email")) {

                    try {
                        showLoadingDialog();
                        authPresenter.signInFacebook(object.getString("email"), token);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showMessage(CacheUtil.getInstance().getTranslations(Text.FACEBOOK_ERROR));
                    }

                } else {
                    showMessage(CacheUtil.getInstance().getTranslations(Text.FACEBOOK_ERROR));
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
        showMessage(CacheUtil.getInstance().getTranslations(Text.FACEBOOK_ERROR));
    }

    @Override
    public void onError(FacebookException error) {
        dismissDialogs();
        showMessage(error.getMessage());
    }

}
