package com.herfy.upsell;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.cart.views.CartPromoDialogFragment;
import com.herfy.checkout_temp.CheckoutActivity;
import com.herfy.utils.constants.Text;
import com.herfy.payment.views.PaymentActivity;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.herfy.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class UpsellActivity extends BaseActivity implements UpsellContract.View {

    private UpsellContract.Presenter upsellPresenter;
    private CartPromoDialogFragment digitalCouponsDialogFragment;

    @BindView(R.id.back)
    MaterialButton back;
    @BindView(R.id.cart)
    ConstraintLayout cart;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.upsell_list)
    RecyclerView recyclerView;
    @BindView(R.id.upsell_title)
    AppCompatTextView upsellTitle;
    @BindView(R.id.upsell_sub_title)
    AppCompatTextView upsellSubTitle;
    @BindView(R.id.place_order)
    MaterialButton placeOrder;
    @BindView(R.id.total_label)
    TextView totalLabel;
    @BindView(R.id.total_price)
    TextView total;
    @BindView(R.id.total_vat_label)
    TextView vat;

    private String goodSelectedDateString;
    private boolean isScheduled;
    private String goodDaySelected;

    private boolean toSkip = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upsell_new);
        ButterKnife.bind(this);
        upsellPresenter = new UpsellPresenter(this);
        initData();
    }

    private void initData() {
        isScheduled = getIntent().getBooleanExtra("isScheduled", false);
        if (isScheduled) {
            goodSelectedDateString = getIntent().getStringExtra("goodSelectedDate");
            goodDaySelected = getIntent().getStringExtra("goodDaySelected");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        CacheUtil.getInstance().setUpsell(false);
        upsellPresenter.start();
        showLoadingDialog();
        upsellPresenter.getUpsells();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void setPresenter(UpsellContract.Presenter presenter) {
        upsellPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
    }

    @Override
    public void setupViews() {

        cart.setVisibility(View.GONE);
        back.setVisibility(View.VISIBLE);

        if (CacheUtil.getInstance().getConcept().getVatType().equalsIgnoreCase("inclusive")) {
            vat.setVisibility(View.VISIBLE);
        } else {
            vat.setVisibility(View.GONE);
        }

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        placeOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (toSkip) {
                    dismissDialogs();
//                    Intent intent = new Intent(UpsellActivity.this, PaymentActivity.class); todo
                    Intent intent = new Intent(UpsellActivity.this, CheckoutActivity.class);
                    if (isScheduled) {
                        intent.putExtra("isScheduled", true);
                        intent.putExtra("goodSelectedDate", goodSelectedDateString);
                        intent.putExtra("goodDaySelected", goodDaySelected);
                    }
                    intent.putExtra("totalPrice", getIntent().getDoubleExtra("totalPrice", 0.0));
                    startActivity(intent);
                    finish();
                } else {
                    showLoadingDialog();
                    upsellPresenter.continueCheckout();
                }
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {
        back.setText(CacheUtil.getInstance().getTranslations(Text.BACK, "Back"));
        totalLabel.setText(CacheUtil.getInstance().getTranslations(Text.TOTAL_LABEL, "Total:"));
        vat.setText(CacheUtil.getInstance().getTranslations(Text.TOTAL_INCLUSIVE_VAT, "(Total is inclusive of VAT)"));
        placeOrder.setText(CacheUtil.getInstance().getTranslations(Text.SKIP_THIS, "SKIP THIS").toUpperCase());
        title.setText(CacheUtil.getInstance().getTranslations(Text.RECOMMENDED, "RECOMMENDED").toUpperCase());
        upsellTitle.setText(CacheUtil.getInstance().getTranslations(Text.WOULD_YOU_LIKE_TO_ADD_THESE, "Would you like to add these?").toUpperCase());
        upsellSubTitle.setText(CacheUtil.getInstance().getTranslations(Text.TAP_ON_A_MENU_ITEM_IF_YOU_WANT_TO_ADD_IT_TO_YOUR_CART, "Tap on a menu item if you want to add it to your cart").toUpperCase());
    }

    @Override
    public void showUpsells(ArrayList<MenuItem> upsells) {
        toSkip = upsellPresenter.checkIfHasNoUpsellSelected();
        if (toSkip) {
            placeOrder.setText(CacheUtil.getInstance().getTranslations(Text.SKIP_THIS, "SKIP THIS").toUpperCase());
        } else {
            placeOrder.setText(CacheUtil.getInstance().getTranslations(Text.ADD_AND_CONTINUE, "ADD AND CONTINUE").toUpperCase());
        }
        dismissDialogs();
        if (upsells.size() > 0) {
            //UpsellRecyclerViewAdapter adapter = new UpsellRecyclerViewAdapter(upsellPresenter);
            //recyclerView.setAdapter(adapter);
        } else {
            placeOrder.performClick();
        }
    }

    @Override
    public void setUpsell(MenuItem upsell) {

    }

    @Override
    public void setUpsellTotalPrice(double totalPrice) {
        total.setText(StringUtil.getFormattedPrice(totalPrice));
    }

    @Override
    public void continueCheckout() {
        toSkip = true;
        placeOrder.performClick();
    }

    @Override
    public void showDigitalCoupons(boolean checkout, List<Campaign> digitalCoupons, int selected) {
        dismissDialogs();
        /*digitalCouponsDialogFragment = CartPromoDialogFragment.newInstance(selected, digitalCoupons, new CartPromoDialogFragment.OnAction() {
            @Override
            public void onClose() {
                continueCheckout();
            }

            @Override
            public void onAvail(int position) {
                digitalCouponsDialogFragment.dismiss();
                if (CacheUtil.getInstance().getCoupon() != null) {
                    showPromptAlertDialog("", CacheUtil.getInstance().getTranslations(Text.ONE_DISCOUNT_ONLY_MESSAGE, "You can only apply one discount at a time. Currently applied discount will be removed"), CacheUtil.getInstance().getTranslations(Text.YES_CAPS), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            showLoadingDialog();
                            upsellPresenter.validateDigitalCoupon(digitalCoupons.get(position));
                        }
                    }, CacheUtil.getInstance().getTranslations(Text.NO_CAPS), (dialogInterface, i) -> dialogInterface.dismiss());
                } else {
                    showLoadingDialog();
                    upsellPresenter.validateDigitalCoupon(digitalCoupons.get(position));
                }
            }
        });
        digitalCouponsDialogFragment.setCancelable(false);
        digitalCouponsDialogFragment.show(getSupportFragmentManager(), CartPromoDialogFragment.class.toString());*/
    }

    @Override
    public void upsellPromotionSucess(boolean success) {
        dismissDialogs();
        continueCheckout();
    }

    @Override
    public void logFirebaseEvent(String event) {
        if (Values.sDeepLinkUtm != null) {
            FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this);
            firebaseAnalytics.logEvent(event, Values.sDeepLinkUtm);
        }
    }

    @Override
    public void goToItemDetails(int position, MenuItem upsell) {

    }
}
