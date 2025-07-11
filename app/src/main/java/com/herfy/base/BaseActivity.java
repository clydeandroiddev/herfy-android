package com.herfy.base;

import android.Manifest;
import android.animation.Animator;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.herfy.R;
import com.herfy.cart.receivers.CartExpiryBroadcastReceiver;
import com.herfy.cart.receivers.CartNotificationBroadcastReceiver;
import com.herfy.checkout.CheckoutActivity;
import com.herfy.checkout.ChoosePaymentActivity;
import com.herfy.payment.views.MoyasarActivity;
import com.herfy.utils.constants.Text;
import com.herfy.main.MainActivity;
import com.herfy.main.NavigationActivity;
import com.herfy.mukafaa_otp.MukafaaOTPActivity;
import com.herfy.payment.views.PaymentActivity;
import com.herfy.utils.constants.Values;
import com.herfy.utils.event.AddToCartEvent;
import com.herfy.utils.helper.BuildUtil;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.theme.DynamicTheme;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.herfy.summary.OrderSummaryActivity;
import com.herfy.upsell.UpsellActivity;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.ReviewManagerUtil;
import com.herfy.util.StringUtil;
import com.herfy.util.ViewUtil;
import com.herfy.util.receivers.InAppRatingBroadcastReceivers;
import com.herfy.util.receivers.NetworkConnection;
import com.herfy.views.NetworkConnectivityDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class BaseActivity extends AppCompatActivity implements BaseListeners {

    public static BaseActivity currentActivity;

    private AlertDialog alertDialog;
    private AlertDialog forceUpdateDialog;
    private AlertDialog loadingDialog;
    private android.app.AlertDialog progressDialog;
    private AlertDialog promoDialog;
    private AlertDialog deleteCartDialog;
    public static AlertDialog cartAlertDialog;
    private String driverNumber;
    private String chatMessage = "";
    public static BaseActivity currentBaseMainActivity;
    private boolean initialLoad = true;
    private boolean isDisconnected = false;
    private boolean isNetworkAvailable;

    private NetworkConnectivityDialog networkConnectivityDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        initBase();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Configuration overrideConfiguration = new Configuration(newBase.getResources().getConfiguration());
        overrideConfiguration.fontScale = 1.0f;
        applyOverrideConfiguration(overrideConfiguration);
        super.attachBaseContext(newBase);
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentActivity = this;
        currentBaseMainActivity = this;
        Intent intent = new Intent(this, CartNotificationBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
        ImageView conceptImage = findViewById(R.id.concept_image);
        if (conceptImage != null) {
            conceptImage.setImageResource(R.drawable.header);
            conceptImage.setImageResource(R.drawable.splash);
        }
        if (BuildUtil.soloAppKey().equalsIgnoreCase("7r5YQwmSgZCtM0ux")) {
            TextView title = findViewById(R.id.title);
            if (title != null) {
                title.setVisibility(View.GONE);
            }
            TextView mainTitleText = findViewById(R.id.mainTitleText);
            if (mainTitleText != null) {
                mainTitleText.setVisibility(View.GONE);
            }
            ImageView topLogo = findViewById(R.id.topLogo);
            if (topLogo != null) {
                topLogo.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        currentActivity = null;
        dismissDialogs();
        if (CacheUtil.getInstance().getConcept() != null) {
            Long abandonCartAlert = CacheUtil.getInstance().getConcept().getAbandonedCartAlertValue();
            if (abandonCartAlert != null && abandonCartAlert > 0 && CacheUtil.getInstance().getCartQuantity() != 0) {
                Intent intent = new Intent(this, CartNotificationBroadcastReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (abandonCartAlert * 60L * 1_000L), pendingIntent);
                        }
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (abandonCartAlert * 60L * 1_000L), pendingIntent);
                    }
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    protected void initBase() {

        if (!(this instanceof MainActivity)) {
            initialLoad = false;
        }

        observeNetworkConnection();

        new DynamicTheme(this);

        // Fetch the color from CacheUtil and handle potential nulls
        String defaultColor = "#" + DynamicTheme.DEFAULT_PRIMARY_COLOR;
        String colorMain = CacheUtil.getInstance().getColorMain();
        if (colorMain == null || colorMain.equalsIgnoreCase("null") || colorMain.isEmpty() || colorMain.length() != 7) {
            colorMain = defaultColor;
        }

        setStatusBarColor(DynamicTheme.getDefaultPrimaryColor());

        try {
            setDynamicTheme(colorMain);
        } catch (Exception e) {
            e.printStackTrace();
            setDynamicTheme(defaultColor);
        }

        LocaleUtil.getInstance().setLocale(this);
        Locale locale = LocaleUtil.getInstance().createDefaultLocale();

        Locale.setDefault(locale);
        final Configuration cfg = new Configuration();
        cfg.locale = locale;
        getResources().updateConfiguration(cfg, null);

        ViewUtil.overrideFonts(findViewById(android.R.id.content));
    }

    private void setDynamicTheme(String colorMain) {
        int red = Integer.valueOf(colorMain.substring(1, 3), 16);
        int green = Integer.valueOf(colorMain.substring(3, 5), 16);
        int blue = Integer.parseInt(colorMain.substring(5, 7), 16);

        DynamicTheme.setNewThemeColor(this, red, green, blue);

        LocaleUtil.getInstance().setLocale(this);
        Locale locale = LocaleUtil.getInstance().createDefaultLocale();

        Locale.setDefault(locale);
        final Configuration cfg = new Configuration();
        cfg.locale = locale;
        getResources().updateConfiguration(cfg, null);

        ViewUtil.overrideFonts(findViewById(android.R.id.content));
    }

    public void setStatusBarColor(int statusBarColor) {
        try {
            Window window = getWindow();
            if(window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) { // Android 15+
                    window.getDecorView().setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                        @NonNull
                        @Override
                        public WindowInsets onApplyWindowInsets(@NonNull View view, @NonNull WindowInsets insets) {
                            Insets statusBarInsets = insets.getInsets(WindowInsets.Type.statusBars());
                            view.setBackgroundColor(statusBarColor);
                            view.setPadding(0, statusBarInsets.top, 0, 0);
                            return insets;
                        }
                    });
                } else {
                    // For Android 14 and below
                    window.setStatusBarColor(statusBarColor);
                }
                if (!ViewUtil.isColorDark(statusBarColor)) {
                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isNetworkAvailable() {
        return isNetworkAvailable;
    }

    public void observeNetworkConnection() {
        NetworkConnection networkConnection = new NetworkConnection(getApplicationContext());

        networkConnection.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                try {
                    FragmentManager manager = getSupportFragmentManager();
                    if (isCanProceedFragmentTransaction(manager)) {
                        if (networkConnectivityDialog != null)
                            networkConnectivityDialog.dismissAllowingStateLoss();
                    }

                    isNetworkAvailable = aBoolean;

                    if (initialLoad) {
                        initialLoad = false;
                        return;
                    }

                    if (!aBoolean) {
                        isDisconnected = true;
                    }

                    if (isDisconnected) {
                        dismissDialogs();
                        networkConnectivityDialog = NetworkConnectivityDialog.Companion.newInstance(aBoolean);
                        networkConnectivityDialog.setCancelable(false);
                        networkConnectivityDialog.show(getSupportFragmentManager(), null);

                        if (aBoolean) {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    networkConnectivityDialog.dismissAllowingStateLoss();
                                }
                            }, 5000);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();

                }
            }
        });
    }

    public void logEventAttributesMetric(String eventName, HashMap<String, String> eventAttributes, @Nullable String eventMetricName, double eventMetricValue) {

        Trace trace = FirebasePerformance.getInstance().newTrace(eventName);
        trace.start();

        if (eventAttributes == null) {
            eventAttributes = new HashMap<>();
        }

        trace.putAttribute("ConceptID", CacheUtil.getInstance().getConcept().getId());

        for (String eventAttributeName : eventAttributes.keySet()) {

            String eventAttributeValue = eventAttributes.get(eventAttributeName);

            if (eventAttributeValue != null) {
                trace.putAttribute(eventAttributeName.replace(" ", ""), eventAttributeValue);
            }
        }

        if (eventMetricName != null && !eventMetricName.isEmpty()) {
            trace.putAttribute(eventMetricName, String.valueOf(eventMetricValue));
        }

        trace.stop();

    }

    public void showAlertDialog(String title, String message) {

        try {

            dismissDialogs();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setCancelable(true);

            builder.setTitle(title);
            builder.setMessage(message);

            alertDialog = builder.create();

            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, CacheUtil.getInstance().getTranslations(Text.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });

            alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {

                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
                    if (!CacheUtil.getInstance().getColorMain().equalsIgnoreCase("#FFFFFF")) {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ViewUtil.getColorMain(BaseActivity.this));
                    } else {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(BaseActivity.this, R.color.primary_text));
                    }

                }
            });

            if (!isFinishing() && !isDestroyed()) {
                alertDialog.show();
            }


        } catch (Exception ex) {

            ex.printStackTrace();

        }

    }

    public void showAlertDialog(String title, String message, String neutral, DialogInterface.OnClickListener neutralOnClickListener) {

        try {

            dismissDialogs();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setCancelable(true);

            builder.setTitle(title);
            builder.setMessage(message);

            alertDialog = builder.create();

            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, neutral, neutralOnClickListener);

            alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
                    if (!CacheUtil.getInstance().getColorMain().equalsIgnoreCase("#FFFFFF")) {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ViewUtil.getColorMain(BaseActivity.this));
                    } else {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(BaseActivity.this, R.color.primary_text));
                    }
                }
            });

            if (!isFinishing() && !isDestroyed()) {
                alertDialog.show();
            }

        } catch (Exception ex) {

            ex.printStackTrace();

        }

    }

    private boolean isCanProceedFragmentTransaction(FragmentManager fragmentManager) {
        return !isFinishing() && !fragmentManager.isStateSaved();
    }

    private void dismissDialog(Dialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

    }

    private boolean isDialogVisible(Dialog dialog) {
        return dialog != null && dialog.isShowing();
    }

    public void showForceUpdateDialog(String title, String message, String positive, DialogInterface.OnClickListener positiveOnClickListener, String negative, DialogInterface.OnClickListener negativeOnClickListener) {
        dismissDialogs();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(title);
        builder.setMessage(message);
        forceUpdateDialog = builder.create();
        if (!positive.equals("")) {
            forceUpdateDialog.setButton(AlertDialog.BUTTON_POSITIVE, positive, positiveOnClickListener);
        }
        if (!negative.equals("")) {
            forceUpdateDialog.setButton(AlertDialog.BUTTON_NEGATIVE, negative, negativeOnClickListener);
        }
        forceUpdateDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                if (!positive.equals("")) {
                    forceUpdateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
                    if (!CacheUtil.getInstance().getColorMain().equalsIgnoreCase("#FFFFFF")) {
                        forceUpdateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ViewUtil.getColorMain(BaseActivity.this));
                    } else {
                        forceUpdateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(BaseActivity.this, R.color.primary_text));
                    }
                }
                if (!negative.equals("")) {
                    forceUpdateDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
                    forceUpdateDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(BaseActivity.this, R.color.secondary_text));
                }
            }
        });
        forceUpdateDialog.show();
    }

    public void showAlertDialog(String title, String message, String positive, DialogInterface.OnClickListener positiveOnClickListener, String negative, DialogInterface.OnClickListener negativeOnClickListener) {


        dismissDialogs();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setCancelable(true);

        builder.setTitle(title);
        builder.setMessage(message);

        alertDialog = builder.create();

        if (!positive.equals("")) {

            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, positive, positiveOnClickListener);

        }

        if (!negative.equals("")) {

            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, negative, negativeOnClickListener);

        }

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {

                if (!positive.equals("")) {

                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
                    if (!CacheUtil.getInstance().getColorMain().equalsIgnoreCase("#FFFFFF")) {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ViewUtil.getColorMain(BaseActivity.this));
                    } else {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(BaseActivity.this, R.color.primary_text));
                    }
                }

                if (!negative.equals("")) {

                    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
                    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(BaseActivity.this, R.color.secondary_text));

                }

            }
        });

        if (!isFinishing() && !isDestroyed()) {
            alertDialog.show();
        }

    }

    public void showLoadingDialog() {

        try {
            dismissDialogs();
            View view = getLayoutInflater().inflate(com.herfy.common.R.layout.loading, null);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setView(view);

            ImageView ivLoader = view.findViewById(com.herfy.common.R.id.ivLoader);
            Glide.with(this)
                    .asGif()
                    .load(com.herfy.commonstyles.R.drawable.loader)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(ivLoader);

            loadingDialog = builder.create();
            loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            loadingDialog.show();

        } catch (Exception ex) {

            ex.printStackTrace();

        }

    }

    public void dismissDialogs() {

        try {
            FragmentManager manager = getSupportFragmentManager();
            //  Ensure the activity is in a valid state for fragment transactions to prevent IllegalStateException
            if (isCanProceedFragmentTransaction(manager)) {
                if (isDialogVisible(alertDialog)) {
                    dismissDialog(alertDialog);
                }

                if (isDialogVisible(forceUpdateDialog)) {
                    dismissDialog(forceUpdateDialog);
                }

                if (isDialogVisible(loadingDialog)) {
                    dismissDialog(loadingDialog);

                }

                if (isDialogVisible(progressDialog)) {
                    dismissDialog(progressDialog);

                }
            }

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    public void setCartExpiry() {
        try {
            int cartExpiry = CacheUtil.getInstance().getConcept().getCartExpiryMinutes();
            if (cartExpiry > 0) {
                Intent intent = new Intent(this, CartExpiryBroadcastReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (cartExpiry * 60 * 1000), pendingIntent);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startInAppRatingCountdown() {
        try {
            int appRatingTime = CacheUtil.getInstance().getConcept().getPromptRatingSeconds();
            if (appRatingTime > 0) {
                Intent intent = new Intent(this, InAppRatingBroadcastReceivers.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (appRatingTime * 1000), pendingIntent);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearCart() {
        if (currentActivity != null && CacheUtil.getInstance().getCartQuantity() != 0) {
            if (currentActivity instanceof UpsellActivity || currentActivity instanceof CheckoutActivity || currentActivity instanceof ChoosePaymentActivity || currentActivity instanceof PaymentActivity || currentActivity instanceof OrderSummaryActivity || currentActivity instanceof MukafaaOTPActivity || currentActivity instanceof MoyasarActivity) {
                CacheUtil.getInstance().setCartExpired(true);
            } else {
                removeMukafaaRedemption();
                CacheUtil.getInstance().setCart(new ArrayList<>());
                CacheUtil.getInstance().setUpsell(true);
                CacheUtil.getInstance().setCouponCode(null);
                CacheUtil.getInstance().setCouponName(null);
                CacheUtil.getInstance().setCouponMenuItem(null);
                CacheUtil.getInstance().setCouponToAdd(null);
                CacheUtil.getInstance().setCampaign(null);
                CacheUtil.getInstance().setCampaignPromotion(null);
                CacheUtil.getInstance().setCampaignPromotionMenuItem(null);
                CacheUtil.getInstance().setCampaignToAdd(null);

                dismissDialogs();

                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setCancelable(false);

                builder.setTitle("");
                builder.setMessage(CacheUtil.getInstance().getTranslations(Text.ORDER_SESSION_EXPIRED, "Order session has expired"));

                AlertDialog cartAlertDialog = builder.create();

                cartAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, CacheUtil.getInstance().getTranslations(Text.CONTINUE_LABEL, "Continue"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (currentActivity instanceof NavigationActivity) {
                            ((NavigationActivity) currentActivity).cartExpired();
                        } else if (currentActivity instanceof MainActivity) {
                            ((MainActivity) currentActivity).cartExpired();
                        } else {
                            Intent intent = new Intent(BaseActivity.this, MainActivity.class);
                            intent.putExtra("home", true);
                            intent.putExtra("menu", true);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finishAffinity();
                        }
                    }
                });

                cartAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        cartAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
                        if (!CacheUtil.getInstance().getColorMain().equalsIgnoreCase("#FFFFFF")) {
                            cartAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ViewUtil.getColorMain(BaseActivity.this));
                        } else {
                            cartAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(BaseActivity.this, R.color.primary_text));
                        }
                    }
                });

                cartAlertDialog.show();
            }
        }
    }

    public void showAppReview() {
        try {
            if (currentActivity != null &&
                    !currentActivity.isDestroyed() &&
                    !currentActivity.isFinishing() &&
                    CacheUtil.getInstance().getInAppReviewEnable() &&
                    !CacheUtil.getInstance().getAppReview()) {
                new ReviewManagerUtil(currentActivity).showReviewDialog();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showInAppRating() {
        new ReviewManagerUtil(this).showReviewDialog();
    }

    public void animateView(Techniques technique, long duration, android.view.View view) {
        YoYo.with(technique).duration(duration).repeat(0).pivot(YoYo.CENTER_PIVOT, YoYo.CENTER_PIVOT).interpolate(new AccelerateDecelerateInterpolator()).withListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        }).playOn(view);

    }

    public void animateView(Techniques technique, long duration, android.view.View view, Animator.AnimatorListener animatorListener) {
        YoYo.with(technique).duration(duration).repeat(0).pivot(YoYo.CENTER_PIVOT, YoYo.CENTER_PIVOT).interpolate(new AccelerateDecelerateInterpolator()).withListener(animatorListener).playOn(view);

    }

    public void animateSpringView(Techniques technique, long duration, android.view.View view, int offset) {

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                float finalPosition = view.getY();
                YoYo.with(technique).duration(duration).repeat(0).pivot(YoYo.CENTER_PIVOT, YoYo.CENTER_PIVOT).interpolate(new AccelerateDecelerateInterpolator()).withListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        view.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        SpringAnimation springAnimation = new SpringAnimation(view, SpringAnimation.Y);
                        SpringForce spring = new SpringForce();
                        spring.setFinalPosition(finalPosition + ViewUtil.dpToPx(BaseActivity.this, offset));
                        spring.setStiffness(SpringForce.STIFFNESS_LOW);
                        spring.setDampingRatio(SpringForce.DAMPING_RATIO_HIGH_BOUNCY);
                        springAnimation.setSpring(spring);
                        springAnimation.start();

                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                }).playOn(view);

            }
        });

    }

    public void animateSpringView(Techniques technique, long duration, android.view.View view, float finalPosition) {

        YoYo.with(technique).duration(duration).repeat(0).pivot(YoYo.CENTER_PIVOT, YoYo.CENTER_PIVOT).interpolate(new AccelerateDecelerateInterpolator()).withListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                SpringAnimation springAnimation = new SpringAnimation(view, SpringAnimation.Y);
                SpringForce spring = new SpringForce();
                spring.setFinalPosition(finalPosition);
                spring.setStiffness(SpringForce.STIFFNESS_LOW);
                spring.setDampingRatio(SpringForce.DAMPING_RATIO_HIGH_BOUNCY);
                springAnimation.setSpring(spring);
                springAnimation.start();

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        }).playOn(view);

    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void showPromptAlertDialog(String title, String message, String positive, DialogInterface.OnClickListener positiveOnClickListener, String negative, DialogInterface.OnClickListener negativeOnClickListener) {
        dismissPromptDialog();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(title);
        builder.setMessage(message);
        promoDialog = builder.create();
        if (!positive.equals("")) {
            promoDialog.setButton(AlertDialog.BUTTON_POSITIVE, positive, positiveOnClickListener);
        }

        if (!negative.equals("")) {
            promoDialog.setButton(AlertDialog.BUTTON_NEGATIVE, negative, negativeOnClickListener);
        }

        promoDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                if (!positive.equals("")) {
                    promoDialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
                    if (!CacheUtil.getInstance().getColorMain().equalsIgnoreCase("#FFFFFF")) {
                        promoDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ViewUtil.getColorMain(BaseActivity.this));
                    } else {
                        promoDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(BaseActivity.this, R.color.primary_text));
                    }
                }

                if (!negative.equals("")) {
                    promoDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
                    promoDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(BaseActivity.this, R.color.secondary_text));
                }
            }
        });
        promoDialog.show();
    }

    public void showDeleteAlertDialog(String title, String message, String positive, DialogInterface.OnClickListener positiveOnClickListener, String negative, DialogInterface.OnClickListener negativeOnClickListener) {
        dismissDeleteCartDialog();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(title);
        builder.setMessage(message);
        deleteCartDialog = builder.create();
        if (!positive.equals("")) {
            deleteCartDialog.setButton(AlertDialog.BUTTON_POSITIVE, positive, positiveOnClickListener);
        }

        if (!negative.equals("")) {
            deleteCartDialog.setButton(AlertDialog.BUTTON_NEGATIVE, negative, negativeOnClickListener);
        }

        deleteCartDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                if (!positive.equals("")) {
                    deleteCartDialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
                    if (!CacheUtil.getInstance().getColorMain().equalsIgnoreCase("#FFFFFF")) {
                        deleteCartDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ViewUtil.getColorMain(BaseActivity.this));
                    } else {
                        deleteCartDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(BaseActivity.this, R.color.primary_text));
                    }
                }

                if (!negative.equals("")) {
                    deleteCartDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
                    deleteCartDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(BaseActivity.this, R.color.secondary_text));
                }
            }
        });
        deleteCartDialog.show();
    }

    public void dismissDeleteCartDialog() {
        try {
            FragmentManager manager = getSupportFragmentManager();
            if (isCanProceedFragmentTransaction(manager)) {
                if (deleteCartDialog != null && deleteCartDialog.isShowing()) {
                    deleteCartDialog.dismiss();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void dismissPromptDialog() {
        try {
            FragmentManager manager = getSupportFragmentManager();
            if (isCanProceedFragmentTransaction(manager)) {
                if (promoDialog != null && promoDialog.isShowing()) {
                    promoDialog.dismiss();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected void proceedToCallUser() {
        try {
            String driveNumber = driverNumber;
            if (StringUtil.isNullOrEmpty(driveNumber)) {
                driveNumber = CacheUtil.getInstance().getLatestDriverNumber();
            }
            if (driveNumber != null) {
                if (!driveNumber.isEmpty()) {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    callIntent.setData(Uri.parse("tel:" + driveNumber));
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    startActivity(callIntent);
                } else {
                    Toast.makeText(this, CacheUtil.getInstance().getTranslations(Text.INVALID_DRIVER_NUMBER, "Sorry, the mobile number of the driver is not valid."), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, CacheUtil.getInstance().getTranslations(Text.INVALID_DRIVER_NUMBER, "Sorry, the mobile number of the driver is not valid."), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeMukafaaRedemption() {
        String transactionId = CacheUtil.getInstance().getMukafaaTransactionId();
        if (transactionId != null && !transactionId.isEmpty()) {
            CacheUtil.getInstance().clearMukafaa();
            CustomersApiCall.getInstance().deleteRedemption(Values.MUKAFAA_PROVIDER, transactionId, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {

                }

                @Override
                public void onError(Object o) {

                }
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AddToCartEvent event) {
        setCartExpiry();
    }

}
