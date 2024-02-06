package com.herfy.loyalty;

import static com.herfy.utils.constants.Text.TOTAL_POINTS_MAX_OUT;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.utils.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.Customer;
import com.skylinedynamics.solosdk.api.models.objects.Loyalty;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsPosRedemption;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransaction;
import com.skylinedynamics.solosdk.api.models.objects.Tier;
import com.herfy.utils.helper.AnimUtil;
import com.herfy.util.StringUtil;
import com.herfy.util.TimeUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoyaltyActivity extends BaseActivity implements LoyaltyContract.View, RedeemLoyaltyDialog.OnAction, CustomerLoyaltyDialog.OnAction {

    private LoyaltyContract.Presenter loyaltyPresenter;

    private String POINTS = "points";
    private String VALUE = "value";
    private String CODE = "code";
    private String EXPIRES_AT = "expires-at";
    private String QR_DATA = "qr-data";

    @BindView(R.id.back)
    MaterialButton back;
    @BindView(R.id.btnHistory)
    MaterialButton btnHistory;

    @BindView(R.id.btnRedeem)
    MaterialButton btnRedeem;

    @BindView(R.id.btnLearnMore)
    TextView btnLearnMore;

    @BindView(R.id.v_tier_card)
    MaterialCardView tierCard;
    @BindView(R.id.iv_card)
    ImageView card;
    @BindView(R.id.tv_tier)
    TextView tier;
    @BindView(R.id.tv_card_number_label)
    TextView cardNumberLabel;
    @BindView(R.id.iv_default_art)
    ImageView ivDefaultArt;
    @BindView(R.id.tv_card_number)
    TextView cardNumber;
    @BindView(R.id.tv_customer_name)
    TextView customerName;

    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.points)
    TextView tvPoints;
    @BindView(R.id.points_currency)
    TextView tvPointsLabel;
    @BindView(R.id.total_points)
    TextView tvTotalPoints;
    @BindView(R.id.amount)
    TextView tvSarValue;
    @BindView(R.id.amount_currency)
    TextView tvSarLabel;
    @BindView(R.id.amount_label)
    TextView tvCurrencyEquivalence;
    @BindView(R.id.progress_label)
    TextView progressLabel;
    @BindView(R.id.cart)
    ConstraintLayout cart;
    @BindView(R.id.progress)
    ProgressBar progress;
    @BindView(R.id.main_layout)
    ConstraintLayout mainLayout;

    @BindView(R.id.qr_img_toolbar)
    ImageView qrImageToolbar;

    private RedeemLoyaltyDialog redeemLoyaltyDialog;

    private CustomerLoyaltyDialog customerLoyaltyDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loyalty);
        ButterKnife.bind(this);
        loyaltyPresenter = new LoyaltyPresenter(this);


        if(AuthUtil.getInstance().getCustomerLoyalty()!=null){
            Loyalty loyalty = AuthUtil.getInstance().getCustomerLoyalty();
            showLoyalty(loyalty, AuthUtil.getInstance().getCustomer().getAttributes().getFirstName() + " " + AuthUtil.getInstance().getCustomer().getAttributes().getLastName());
        }

        if(AuthUtil.getInstance().getCustomerLoyaltyTier()!=null){
            Tier tier = AuthUtil.getInstance().getCustomerLoyaltyTier();
            showNextTier(tier,CacheUtil.getInstance().getTotalAccumulatedPoints(),CacheUtil.getInstance().getPointsUntilNextTier(), CacheUtil.getInstance().getStartingPointsOfNextTier());
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        loyaltyPresenter.start();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isNetworkAvailable()) {
                    if(AuthUtil.getInstance().getCustomerLoyalty()==null){
                        checkLoading();
                    }
                    loyaltyPresenter.getLoyalty();
                    loyaltyPresenter.getCustomerLoyalty();


                }
            }
        }, 350);
    }

    private void checkLoading() {
        try {
            if (redeemLoyaltyDialog != null || customerLoyaltyDialog != null) {
                if (redeemLoyaltyDialog != null) {
                    if (!redeemLoyaltyDialog.isVisible()) {
                        showLoadingDialog();
                    }
                }
                if (customerLoyaltyDialog != null) {
                    if (!customerLoyaltyDialog.isVisible()) {
                        showLoadingDialog();
                    }
                }
            } else {
                showLoadingDialog();
            }
        } catch (Exception e) {
            showLoadingDialog();
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(AnimUtil.outF(), AnimUtil.outS());
    }

    @Override
    public void setPresenter(LoyaltyContract.Presenter presenter) {
        loyaltyPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
    }

    @Override
    public void setupViews() {
        cart.setVisibility(View.GONE);
        back.setVisibility(View.VISIBLE);
        back.setIconTint(ContextCompat.getColorStateList(this, R.color.transparent));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoyaltyActivity.this, LoyaltyPointsHistory.class);
                startActivityForResult(intent, 1111);
            }
        });

    }

    @Override
    public void setupFonts() {
        tier.setTypeface(FontHandler.getMediumTypeface());
        cardNumberLabel.setTypeface(FontHandler.getSemiboldTypeface());
        cardNumber.setTypeface(FontHandler.getSemiboldTypeface());
        customerName.setTypeface(FontHandler.getMediumTypeface());
        tvPoints.setTypeface(FontHandler.getMediumTypeface());
        tvPointsLabel.setTypeface(FontHandler.getMediumTypeface());
        tvTotalPoints.setTypeface(FontHandler.getSemiboldTypeface());
        tvSarValue.setTypeface(FontHandler.getMediumTypeface());
        tvSarLabel.setTypeface(FontHandler.getMediumTypeface());
        tvCurrencyEquivalence.setTypeface(FontHandler.getSemiboldTypeface());
//        progressLabel.setTypeface(FontHandler.getMediumTypeface());
//        btnHistory.setTypeface(FontHandler.getSemiboldTypeface());
    }

    @Override
    public void setupTranslations() {
        back.setText(CacheUtil.getInstance().getTranslations(Text.CLOSE, "Close"));
        title.setText(CacheUtil.getInstance().getTranslations(Text.LOYALTY_PAGE, "LOYALTY"));
        tvCurrencyEquivalence.setText(CacheUtil.getInstance().getTranslations(Text.CURRENCY_EQUIVALENCE, "(x) Equivalence").replace("(x)", CacheUtil.getInstance().getTranslations(CacheUtil.getInstance().getConcept().getCurrencyCode().toUpperCase())));
        btnLearnMore.setText(CacheUtil.getInstance().getTranslations(Text.LEARN_ABOUT_THIS_POINTS_LABEL, "Learn about points"));
        tvPointsLabel.setText(CacheUtil.getInstance().getTranslations(Text.PTS_LABEL, "PTS").replace("(x)",""));
        btnHistory.setText(CacheUtil.getInstance().getTranslations(Text.POINTS_HISTORY, "POINTS HISTORY"));
        tvSarLabel.setText(CacheUtil.getInstance().getTranslations(CacheUtil.getInstance().getConcept().getCurrencyCode().toLowerCase()));
        tvTotalPoints.setText(CacheUtil.getInstance().getTranslations(Text.LOYALTY_TOTAL_POINTS, "Total number of points"));
        cardNumberLabel.setText(CacheUtil.getInstance().getTranslations(Text.LOYALTY_MOBILE_NUMBER_LABEL, "MOBILE NUMBER"));
        btnRedeem.setText(CacheUtil.getInstance().getTranslations(Text.REDEEM_IN_STORE, "REDEEM IN STORE"));
    }


    @Override
    public void showNextTier(Tier tier, int totalAccumulatedPoints, int pointsUntilNextTier, int startingPointsOfNextTier) {
        AuthUtil.getInstance().setCustomerLoyaltyTier(tier);
        CacheUtil.getInstance().setTotalAccumulatedPoints(totalAccumulatedPoints);
        CacheUtil.getInstance().setPointsUntilNextTier(pointsUntilNextTier);
        CacheUtil.getInstance().setStartingPointsOfNextTier(startingPointsOfNextTier);
        for(Tier thisTier : loyaltyPresenter.getTiers()){
            Glide.with(this).load(thisTier.getImageUrl()).preload();
        }

        if (pointsUntilNextTier <= 0) {
            if (tier == null) {
                progress.setMax(100);
                progress.setProgress(0);
                progressLabel.setText("You are now a Tier member");
            } else {
                progress.setMax(100);
                progress.setProgress(100);
                progressLabel.setText(CacheUtil.getInstance().getTranslations(TOTAL_POINTS_MAX_OUT, "You have maxed out on our tier levels"));
            }

        } else {
            progress.setMax(startingPointsOfNextTier);
            progress.setProgress(totalAccumulatedPoints);
            progressLabel.setText(HtmlCompat.fromHtml(StringUtil.localize(CacheUtil.getInstance().getTranslations(Text.LOYALTY_POINTS_UNTIL_TIER, "(x) points till you reach (y) Tier").replace("(x)", String.valueOf(pointsUntilNextTier)).replace("(y)", "<b>" + tier.getName().substring(0, 1).toUpperCase() + tier.getName().substring(1).toLowerCase())) + "</b>", HtmlCompat.FROM_HTML_MODE_LEGACY));
        }
        dismissDialogs();
    }

    @Override
    public void showQRPage(LoyaltyPointsPosRedemption loyalty) {
        if (loyalty != null) {
            cancel();
            Intent intent = new Intent(LoyaltyActivity.this, LoyaltyQrActivity.class);
            intent.putExtra(POINTS, loyalty.getPoints());
            intent.putExtra(VALUE, loyalty.getValue());
            intent.putExtra(CODE, loyalty.getCode());
            loyalty.getExpiresAt();
            if (!loyalty.getExpiresAt().isEmpty()) {
                String expiresAt = String.valueOf(TimeUtil.getTimeDifferenceInMillis(loyalty.getExpiresAt()));
                intent.putExtra(EXPIRES_AT, expiresAt);
            }
            intent.putExtra(QR_DATA, new Gson().toJson(loyalty.getQrData()));
            startActivity(intent);
        } else {
            showLoyaltyError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
        }
    }

    @Override
    public void showQrData(String data) {
        qrImageToolbar.setVisibility(View.VISIBLE);
        Log.e("ADADADADAD", "ADADAD1:" + data);
        qrImageToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customerLoyaltyDialog = CustomerLoyaltyDialog.Companion.newInstance(data);
                customerLoyaltyDialog.setCancelable(true);
                customerLoyaltyDialog.show(getSupportFragmentManager(), CustomerLoyaltyDialog.class.toString());
            }
        });
    }

    @Override
    public void showLoyalty(Loyalty loyalty, String name) {

        btnLearnMore.setOnClickListener(v -> {
            if (loyalty == null) return;
            String currentTier;
            if (loyalty.getAttributes().getLoyaltyTier() == null) {
                currentTier = loyaltyPresenter.getTiers().get(0).getName();
            } else {
                currentTier = loyalty.getAttributes().getLoyaltyTier().getName();
            }
            LearnPointsDialog learnPointsDialog = LearnPointsDialog.newInstance(loyalty,currentTier, loyaltyPresenter.getTiers());
            learnPointsDialog.setCancelable(true);
            learnPointsDialog.show(getSupportFragmentManager(), LearnPointsDialog.class.toString());
        });

        if (loyalty != null && loyalty.getAttributes().getLoyaltyTier() != null && !TextUtils.isEmpty(loyalty.getAttributes().getLoyaltyTier().getImageUrl())) {
            ivDefaultArt.setVisibility(View.GONE);

            Glide.with(this).load(loyalty.getAttributes().getLoyaltyTier().getImageUrl()).preload();

            Glide.with(this).load(loyalty.getAttributes().getLoyaltyTier().getImageUrl()).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.centerInsideTransform()).addListener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    tierCard.setCardBackgroundColor(Color.parseColor("#5730BF"));
                    tier.setTextColor(ContextCompat.getColor(LoyaltyActivity.this, R.color.white));
                    cardNumberLabel.setTextColor(ContextCompat.getColor(LoyaltyActivity.this, R.color.white));
                    cardNumber.setTextColor(ContextCompat.getColor(LoyaltyActivity.this, R.color.white));
                    customerName.setTextColor(ContextCompat.getColor(LoyaltyActivity.this, R.color.white));
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    new Thread(() -> {
                        // allow image to load into target
                        SystemClock.sleep(200);
                        runOnUiThread(() -> {
                            card.buildDrawingCache();
                            int brightness = calculateBrightness(card.getDrawingCache(), 1);

                            if (brightness > 255 / 2) {
                                tier.setTextColor(ContextCompat.getColor(LoyaltyActivity.this, R.color.black));
                                cardNumberLabel.setTextColor(ContextCompat.getColor(LoyaltyActivity.this, R.color.black));
                                cardNumber.setTextColor(ContextCompat.getColor(LoyaltyActivity.this, R.color.black));
                                customerName.setTextColor(ContextCompat.getColor(LoyaltyActivity.this, R.color.black));
                            }
                        });
                    }).start();
                    return false;
                }
            }).into(card);
        } else {
            ivDefaultArt.setVisibility(View.VISIBLE);
            tierCard.setCardBackgroundColor(Color.parseColor("#5730BF"));
        }

        if (loyalty != null && loyalty.getAttributes().getLoyaltyTier() == null) {
            if (loyaltyPresenter.getTiers() != null && loyaltyPresenter.getTiers().size() > 0) {
                tier.setText(loyaltyPresenter.getTiers().get(0).getName());
            }
        } else {
            tier.setText(loyalty.getAttributes().getLoyaltyTier().getName());
        }

        Customer customer = AuthUtil.getInstance().getCustomer();
        cardNumber.setText(customer.getAttributes().getMobile());
        customerName.setText(name);
        DecimalFormat formatter = new DecimalFormat("#,###");
        tvSarValue.setText(StringUtil.localize(formatter.format(loyalty.getAttributes().getCurrentBalanceAmount())));
        tvPoints.setText(StringUtil.localize(formatter.format(loyalty.getAttributes().getCurrentBalance())));

        if (loyalty.getAttributes().getCurrentBalance() > 0) {
            btnRedeem.setVisibility(View.VISIBLE);
            btnRedeem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (loyalty.getAttributes().getCurrentBalanceAmount() > 0) {
                        redeemLoyaltyDialog = RedeemLoyaltyDialog.Companion.newInstance(loyalty);
                        redeemLoyaltyDialog.setCancelable(true);
                        redeemLoyaltyDialog.show(getSupportFragmentManager(), RedeemLoyaltyDialog.class.toString());
                    } else {
                        showAlertDialog("", CacheUtil.getInstance().getTranslations("not_enough_balance_amount_message", "Please make sure you have at least 1 (x) to redeem").replace("(x)", CacheUtil.getInstance().getTranslations(CacheUtil.getInstance().getConcept().getCurrencyCode().toLowerCase())), CacheUtil.getInstance().getTranslations(Text.OK), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                    }
                }
            });
        } else {
            btnRedeem.setVisibility(View.GONE);
        }

        dismissDialogs();
    }

    public int calculateBrightness(android.graphics.Bitmap bitmap, int skipPixel) {
        int R = 0;
        int G = 0;
        int B = 0;
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        int n = 0;
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < pixels.length; i += skipPixel) {
            int color = pixels[i];
            R += Color.red(color);
            G += Color.green(color);
            B += Color.blue(color);
            n++;
        }
        return (R + B + G) / (n * 3);
    }

    @Override
    public void showLoyaltyError(String message) {
        showAlertDialog("", message, CacheUtil.getInstance().getTranslations(Text.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                onBackPressed();
            }
        });
    }

    @Override
    public void showLoyaltyPointsHistory(boolean newestToOldest, ArrayList<LoyaltyTransaction> loyaltyTransactions) {

    }

    @Override
    public void setLoyaltyHistory(LoyaltyTransaction loyaltyTransaction) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 1024) {
            onBackPressed();
        }
    }

    @Override
    public void navigate(int points) {
        showLoadingDialog();
        loyaltyPresenter.redeemPoints(points);
    }

    @Override
    public void cancel() {
        if (redeemLoyaltyDialog == null) {
            return;
        }
        redeemLoyaltyDialog.dismiss();
        redeemLoyaltyDialog = null;
    }

    @Override
    public void closeQR() {
        if (customerLoyaltyDialog == null) {
            return;
        }
        customerLoyaltyDialog.dismiss();
        customerLoyaltyDialog = null;
    }
}