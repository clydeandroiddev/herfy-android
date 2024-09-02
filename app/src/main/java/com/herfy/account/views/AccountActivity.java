package com.herfy.account.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.herfy.R;
import com.herfy.account.AccountContract;
import com.herfy.account.AccountPresenter;
import com.herfy.account.adapters.LocationsRecyclerViewAdapter;
import com.herfy.account.adapters.AccountSavedCardsAdapter;
import com.herfy.base.BaseActivity;
import com.herfy.utils.constants.Text;
import com.herfy.main.MainActivity;
import com.herfy.order.views.AddressActivity;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.BuildUtil;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.SoloAuth;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard;
import com.herfy.utils.helper.AnimUtil;
import com.herfy.util.StringUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AccountActivity extends BaseActivity implements AccountContract.View {

    private AccountContract.Presenter accountPresenter;

    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.back)
    MaterialButton back;
    @BindView(R.id.action)
    MaterialButton action;
    @BindView(R.id.cart)
    ConstraintLayout cart;
    @BindView(R.id.your_details_label)
    TextView details;
    @BindView(R.id.bookmarked_addresses_label)
    TextView bookmarkedAddressesLabel;

    @BindView(R.id.full_name)
    TextView fullName;
    @BindView(R.id.phone_number)
    TextView phoneNumber;
    @BindView(R.id.email)
    TextView email;

    @BindView(R.id.log_out)
    MaterialButton logOut;
    @BindView(R.id.add_address)
    MaterialButton addAddress;

    @BindView(R.id.saved_cards_label)
    TextView savedCardsLabel;

    @BindView(R.id.rv_bookmarked_addresses)
    RecyclerView rvAddresses;
    @BindView(R.id.rv_saved_cards)
    RecyclerView rvSavedCards;

    @BindView(R.id.change_password)
    MaterialButton changePassword;

    @BindView(R.id.revel_reward_point)
    TextView revelRewardsPoint;

    @BindView(R.id.points_label)
    TextView pointsLabel;

    private boolean isFromLogOut = false;
    private AccountSavedCardsAdapter accountSavedCardsAdapter;
    private List<CustomerCard> cards = new ArrayList<>();

    FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        ButterKnife.bind(this);
        accountPresenter = new AccountPresenter(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showLoadingDialog();
        accountPresenter.start();
        accountPresenter.getPassword();
        accountPresenter.fetchCustomerCards();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("home", false);
        intent.putExtra("menu", true);
        intent.putExtra("cart", false);
        intent.putExtra("settings", false);

        if (isFromLogOut) {
            intent.putExtra("isFromLogOut", true);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(AnimUtil.outF(), AnimUtil.outS());
        finish();
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
        action.setVisibility(View.VISIBLE);

        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(task ->{
            boolean showRevelReward = firebaseRemoteConfig.getBoolean("show_revel_rewards");
            if(showRevelReward){
                accountPresenter.getRevelRewardPoints();
            }
        });

        back.setOnClickListener(view -> onBackPressed());

        action.setOnClickListener(view -> {
            showLoadingDialog();
            Intent intent = new Intent(AccountActivity.this, EditProfileActivity.class);
            intent.putExtra("locations", accountPresenter.getLocationsAsString());
            startActivity(intent);
        });

        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginManager.getInstance().logOut();
                GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(BuildUtil.googleInClient(AccountActivity.this))
                        .requestEmail()
                        .build();
                GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(AccountActivity.this, googleSignInOptions);
                googleSignInClient.signOut();
                accountPresenter.removeMukafaaRedemption();
                CacheUtil.getInstance().clearOrder();
                SoloAuth.getInstance().logout();
                AuthUtil.getInstance().setCustomer(null);
                isFromLogOut = true;
                onBackPressed();
            }
        });

        changePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AccountActivity.this, ChangePasswordActivity.class);
                startActivity(intent);
                overridePendingTransition(AnimUtil.inF(), AnimUtil.inS());
            }
        });

        addAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AccountActivity.this, AddressActivity.class);
                intent.putExtra("fromAccount", true);
                startActivity(intent);
                overridePendingTransition(AnimUtil.inF(), AnimUtil.inS());
            }
        });

        if (accountSavedCardsAdapter == null) {
            accountSavedCardsAdapter = new AccountSavedCardsAdapter(cards);
        }
        rvSavedCards.setAdapter(accountSavedCardsAdapter);

    }

    @Override
    public void setupFonts() {
    }

    @Override
    public void setupTranslations() {
        back.setText(CacheUtil.getInstance().getTranslations(Text.BACK));
        title.setText(CacheUtil.getInstance().getTranslations(Text.ACCOUNT));
        action.setText(CacheUtil.getInstance().getTranslations(Text.EDIT_CAPS));
        changePassword.setText(CacheUtil.getInstance().getTranslations(Text.CHANGE_PASSWORD, "CHANGE PASSWORD").toUpperCase());
        details.setText(CacheUtil.getInstance().getTranslations(Text.YOUR_DETAILS_CAPS, "YOUR DETAILS"));
        logOut.setText(CacheUtil.getInstance().getTranslations(Text.LOGOUT_CAPS));
        bookmarkedAddressesLabel.setText(CacheUtil.getInstance().getTranslations(Text.BOOKMARKED_ADDRESSES_CAPS, "BOOKMARKED ADDRESSES"));
        addAddress.setText(CacheUtil.getInstance().getTranslations(Text.ADD_AN_ADDRESS_CAPS, "ADD AN ADDRESS"));
        savedCardsLabel.setText(CacheUtil.getInstance().getTranslations(Text.SAVED_CARDS, "SAVED CARDS").toUpperCase());
    }

    @Override
    public void showCustomer(String firstName, String lastName, String gender, String phoneNumberValue, String emailValue, String providerValue) {
        fullName.setText(firstName + " " + lastName);
        //For Burgerizzr
        if(BuildUtil.soloAppKey().equalsIgnoreCase("SeBwUS2G9SEnkwty")) {
            phoneNumberValue = CacheUtil.getInstance().getConcept().getDialingCode() + phoneNumberValue.substring(1);
            email.setVisibility(View.GONE);
        }
        phoneNumber.setText(StringUtil.localize(phoneNumberValue));
        email.setText(emailValue);
//        provider.setText(providerValue);
//        provider.setVisibility(providerValue.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void showDateOfBirth(Date birthDate, String date) {

    }

    @Override
    public void showEmail(boolean show) {
        email.setVisibility(show && !BuildUtil.soloAppKey().equalsIgnoreCase("SeBwUS2G9SEnkwty") ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showPassword(boolean show) {
        changePassword.setVisibility(show ? View.VISIBLE : View.GONE);
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

    }

    @Override
    public void showErrorConfirmPassword(String error) {

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

    }

    @Override
    public void showLocations(ArrayList<Address> locations) {
        if (locations.size() > 0) {
            LocationsRecyclerViewAdapter addressesAdapter = new LocationsRecyclerViewAdapter(accountPresenter, false);
            rvAddresses.setAdapter(addressesAdapter);
            rvAddresses.setVisibility(View.VISIBLE);
//            if (CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes() != null && CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().contains(OrderType.DELIVERY)) {
//                addAddress.setVisibility(View.VISIBLE);
//            } else {
//                addAddress.setVisibility(View.GONE);
//            }
        } else {
            // addAddress.setVisibility(View.GONE);
            rvAddresses.setVisibility(View.GONE);
        }

        dismissDialogs();

    }

    @Override
    public void hideLocations() {

    }

    @Override
    public void setLocation(Address address) {

    }

    @Override
    public void showMessage(String message) {
        dismissDialogs();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void accountDeleted() {

    }

    @Override
    public void showCustomerCards() {
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
    public void showRevelRewardPoints(Double pointValue) {
        revelRewardsPoint.setVisibility(View.VISIBLE);
        pointsLabel.setVisibility(View.VISIBLE);
        revelRewardsPoint.setText(String.valueOf(pointValue));
        pointsLabel.setText(CacheUtil.getInstance().getTranslations(Text.POINTS, "points"));
    }

    @Override
    public void showCard(boolean isDefault) {
    }

}
