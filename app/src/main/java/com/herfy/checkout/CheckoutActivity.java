package com.herfy.checkout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.herfy.BuildConfig;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.main.MainActivity;
import com.herfy.util.CacheUtil;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CheckoutActivity extends BaseActivity implements CheckoutContract.View {

    private CheckoutContract.Presenter checkoutPresenter;

    private boolean payment = false;
    private String order = "";

    @BindView(R.id.container) ConstraintLayout container;

    @BindView(R.id.back) ImageButton back;

    @BindView(R.id.title) TextView title;
    @BindView(R.id.message) TextView message;
    @BindView(R.id.thank_you) TextView thankYou;
    @BindView(R.id.cancel) TextView cancel;

    @BindView(R.id.thank_you_container) LinearLayout thankYouContainer;

    @BindView(R.id.view) Button view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
        ButterKnife.bind(this);
        checkoutPresenter = new CheckoutPresenter(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkoutPresenter.start();

        Bundle bundle = getIntent().getExtras();

        if(bundle != null && bundle.containsKey("payment") && bundle.containsKey("order")) {
            payment = bundle.getBoolean("payment");
            order = bundle.getString("order");
        }

    }

    @Override
    public void onBackPressed() {

        if(payment) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            super.onBackPressed();
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void setPresenter(CheckoutContract.Presenter presenter) {
        checkoutPresenter = presenter;
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

        boolean isWaybackBurgers = BuildConfig.FLAVOR.equals("wayback_burgers") || CacheUtil.getInstance().getConceptKey().equals("CGqbTzpyKa9");

        thankYouContainer.setVisibility(isWaybackBurgers ? View.VISIBLE : View.GONE);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

    }

    @Override
    public void setupFonts() {
        message.setTypeface(FontHandler.getMediumTypeface());
        thankYou.setTypeface(FontHandler.getMediumTypeface());
        cancel.setTypeface(FontHandler.getMediumTypeface());
    }

    @Override
    public void setupTranslations() {
        title.setText(CacheUtil.getInstance().getTranslations(Text.ORDER_PLACED));
        message.setText(CacheUtil.getInstance().getTranslations(Text.ORDER_PLACED_MESSAGE));
        thankYou.setText(CacheUtil.getInstance().getTranslations(Text.THANK_FOR_YOUR_ORDER, "Thank you for your order!"));
        cancel.setText(CacheUtil.getInstance().getTranslations(Text.CANCEL_ORDER_NOT_ALLOWED, "We appreciate wonderful customers like you. Kindly take note that we don't allow order cancellation at this time."));
        view.setText(CacheUtil.getInstance().getTranslations(Text.VIEW_ORDER_STATUS));
    }

}
