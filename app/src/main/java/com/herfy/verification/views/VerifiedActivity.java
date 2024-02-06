package com.herfy.verification.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.utils.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.main.MainActivity;
import com.herfy.menu.views.MenuItemActivity;
import com.herfy.order.views.OrderTypeActivity;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.verification.VerificationContract;
import com.herfy.verification.VerificationPresenter;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VerifiedActivity extends BaseActivity implements VerificationContract.View {

    private VerificationContract.Presenter verificationPresenter;

    @BindView(R.id.message) TextView message;

    @BindView(R.id.explore) Button explore;

    private boolean orderHistory = false;
    private boolean cart = false;
    private boolean orderType = false;
    private boolean favoriteHome = false;
    private boolean favoriteMenuItem = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verified);
        ButterKnife.bind(this);
        verificationPresenter = new VerificationPresenter(this);

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
        verificationPresenter.start();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        if (!orderType) {
            intent.putExtra("cart", cart);
            /*if(orderHistory) {
                intent = new Intent(this, OrdersActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            } else*/
            if (favoriteHome) {
                intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            } else if (favoriteMenuItem) {
                intent = new Intent(this, MenuItemActivity.class);
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
    public void setPresenter(VerificationContract.Presenter presenter) {
        verificationPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
    }

    @Override
    public void setupViews() {
        explore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    @Override
    public void setupFonts() {
        message.setTypeface(FontHandler.getMediumTypeface());
    }

    @Override
    public void setupTranslations() {
        message.setText(CacheUtil.getInstance().getTranslations(Text.VERIFIED));
        explore.setText(CacheUtil.getInstance().getTranslations(Text.EXPLORE_MENU));
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

    }

    @Override
    public void showError(String error) {

    }

    @Override
    public void showResendError(String error) {

    }

    @Override
    public void showCode() {

    }

    @Override
    public void showVerified() {

    }

}
