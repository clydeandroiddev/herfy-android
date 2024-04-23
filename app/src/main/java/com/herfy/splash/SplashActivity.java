package com.herfy.splash;

import android.Manifest;
import android.app.AlarmManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.crashlytics.internal.common.CommonUtils;
import com.herfy.BuildConfig;
import com.herfy.R;
import com.herfy.animation.ActivitySwitcher;
import com.herfy.base.BaseActivity;
import com.herfy.country.CountriesActivity;
import com.herfy.language.LanguageNewActivity;
import com.herfy.main.MainActivity;
import com.herfy.utils.helper.CacheHelper;
import com.herfy.utils.helper.DeepLinkUtil;
import com.herfy.util.JsonUtil;
import com.herfy.util.StringUtil;
import com.herfy.utils.constants.Text;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.BuildUtil;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.skylinedynamics.solosdk.api.SoloApi;
import com.skylinedynamics.solosdk.api.SoloAuth;
import com.skylinedynamics.solosdk.api.models.objects.Slide;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SplashActivity extends BaseActivity implements SplashContract.View {

    private SplashContract.Presenter splashPresenter;

    @BindView(R.id.container)
    ConstraintLayout container;

    @BindView(R.id.image)
    ImageView image;

    @BindView(R.id.vvSplash)
    VideoView vvSplash;
    @BindView(R.id.background)
    ImageView background;

    private boolean fromChangeLanguage = false;
    private boolean concepts = false;
    private boolean recreate = false;
    private boolean router = false;

    private boolean proceedMain;
    private int sliderCount = 0;
    private int sliderPosition = 0;
    private MediaController mediaControls;

    private boolean videoCompleted = false;

    private final ActivityResultLauncher<String> notificationsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                checkAlarmPermission();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);
        splashPresenter = new SplashPresenter(this);
        splashPresenter.setAreas();
        proceesDeeplink();
        isAuthCredentialMigrated();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                checkAlarmPermission();
            }
        }
        setStatusBarColor();
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.containsKey("fromChangeLanguage")) {
                fromChangeLanguage = bundle.getBoolean("fromChangeLanguage");
                if (fromChangeLanguage && !BuildUtil.splashVideoApp()) {
                    ActivitySwitcher.animationIn(findViewById(R.id.container), getWindowManager());
                }
            }
            if (bundle.containsKey("concepts")) {
                concepts = bundle.getBoolean("concepts");
            }
            if (bundle.containsKey("recreate")) {
                recreate = bundle.getBoolean("recreate");
            }
            router = bundle.getBoolean("router", false);
        }
        if (fromChangeLanguage) {
            setupViews();
            fromChangeLanguage = false;
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    intent.putExtra("fromChangeLanguage", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    if (!BuildUtil.splashVideoApp()) {
                        ActivitySwitcher.animationOut(findViewById(R.id.container), getWindowManager(), new ActivitySwitcher.AnimationListener() {
                            @Override
                            public void onAnimationStarted() {
                                startActivity(intent);
                                overridePendingTransition(0, 0);
                                finish();
                            }

                            @Override
                            public void onAnimationFinished() {

                            }
                        });
                    } else {
                        startActivity(intent);
                        overridePendingTransition(0, 0);
                        finish();
                    }
                }
            }, 500);
        } else {
            if (recreate || BuildConfig.DISABLE_SPLASH_ANIMATION) {
                image.setScaleX(4.0f);
                image.setScaleY(4.0f);
                image.setVisibility(View.VISIBLE);
            }
            splashPresenter.setRecreate(recreate);
            splashPresenter.start();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void checkIfRooted() {
        if (CommonUtils.isRooted(this)) {
            showAlertDialog(
                    "",
                    CacheUtil.getInstance().getTranslations(
                            Text.ROOTED_DEVICE_MESSAGE,
                            "This device appears to be rooted. This application is not supported on rooted devices."
                    ),
                    CacheUtil.getInstance().getTranslations(Text.OK),
                    (dialogInterface, i) -> checkIfRooted()
            );
        } else {
            splashPresenter.sendLanguage();
            splashPresenter.getApplication();
        }
    }

    @Override
    public void recreate() {
        Intent intent = new Intent(this, SplashActivity.class);
        intent.putExtra("concepts", concepts);
        intent.putExtra("recreate", true);
        startActivity(intent);
        finish();
    }

    public void proceesDeeplink() {
        Bundle bundle = getIntent().getExtras();
        if (Values.sDeepLinkId.isEmpty() && bundle != null && bundle.containsKey("deep-link")) {
            String deepLink = bundle.getString("deep-link");
            if (deepLink != null && !deepLink.isEmpty()) {
                String[] values = deepLink.split("/");
                if (values.length >= 2) {
                    deepLink = "https://" + BuildConfig.SOLO_APP.toLowerCase() + ".page.link/" + values[1].replace("-", "/");
                }
                DeepLinkUtil.parse(deepLink);
            }
        } else {
            Values.sDeepLink = "";
            Values.sDeepLinkId = "";
        }
    }

    @Override
    public void animateLogo(boolean zoomIn, Intent intent) {

        if ((!zoomIn || !recreate) && !CacheUtil.getInstance().getConceptKey().equalsIgnoreCase("CGqbTzpyKa9")) {
            AnimationSet animationSet = null;
            Animation animation;
            if (zoomIn) {
                animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoom_in);
            } else {
                animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoom_out);
                if (recreate) {
                    animationSet = new AnimationSet(true);
                    animation = new ScaleAnimation(1f, 0.4f, 1f, 0.4f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    animation.setDuration(1000);
                    animation.setFillAfter(true);
                    animationSet.addAnimation(animation);

                    animation = new AlphaAnimation(1.0f, 0.0f);
                    animation.setDuration(1000);
                    animation.setFillAfter(true);
                    animationSet.addAnimation(animation);

                }
            }
            animation.setFillAfter(true);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    proceedNext(zoomIn, intent);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (!zoomIn) {
                        image.setVisibility(View.INVISIBLE);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            if (animationSet == null) {
                image.startAnimation(animation);
            } else {
                image.startAnimation(animationSet);
            }
        }
    }

    public void proceedNext(boolean zoomIn, Intent intent) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!zoomIn && intent != null) {
                    if (proceedMain && BuildConfig.MULTI_COUNTRY_ENABLED) {
                        Intent countriesIntent = new Intent(SplashActivity.this, CountriesActivity.class);
                        countriesIntent.putExtra("home", true);
                        countriesIntent.putExtra("forceLocation", intent.getBooleanExtra("forceLocation", false));
                        startActivity(countriesIntent);
                        finish();
                    } else {
                        Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(SplashActivity.this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
                        startActivity(intent, bundle);
                        finish();
                    }
                }
            }
        }, 500);
    }

    @Override
    public void proceed(boolean language, boolean onboarding, boolean forceLocation) {

        if (!router) {
            Values.sDeepLinkUtm = null;
        }

        if (!BuildConfig.SUBSCRIPTION_ENABLED) {
            CacheUtil.getInstance().setUpMode(1);
        }

        Intent intent = new Intent(this, MainActivity.class);
        proceedMain = true;
        if (!language) {
            proceedMain = false;
            intent = new Intent(this, LanguageNewActivity.class);
        } else {
            intent.putExtra("home", true);
        }

        intent.putExtra("forceLocation", forceLocation);

        final Intent finalIntent = intent;

        if (CacheUtil.getInstance().getConceptKey().equalsIgnoreCase("CGqbTzpyKa9")) {
            Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(SplashActivity.this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
            startActivity(intent, bundle);
            finish();
        } else {
            if (BuildConfig.DISABLE_SPLASH_ANIMATION || BuildUtil.splashBackgroundApp() || videoCompleted) {
                proceedNext(false, finalIntent);
            } else {
                animateLogo(false, finalIntent);
            }
        }
    }

    @Override
    public void setPresenter(SplashContract.Presenter presenter) {
        splashPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
    }

    @Override
    public void setupViews() {

        container.setBackgroundColor(Color.parseColor(BuildUtil.splashColorApp()));

        if (BuildUtil.splashLogoApp()) {
            if (fromChangeLanguage) {
                image.setScaleX(4.0f);
                image.setScaleY(4.0f);
            }
            image.setVisibility(View.VISIBLE);
        } else {
            image.setVisibility(View.GONE);
        }

        if (BuildUtil.splashBackgroundApp()) {
            if (BuildConfig.SOLO_CONCEPT != "Adjx12OokD0") {
                image.setImageResource(android.R.color.transparent);
            }
            background.setVisibility(View.VISIBLE);
        } else if (BuildUtil.splashVideoApp()) {
            vvSplash.setVisibility(View.VISIBLE);
            image.setVisibility(View.INVISIBLE);
            background.setVisibility(View.INVISIBLE);
            if (mediaControls == null) {
                mediaControls = new MediaController(this);
                mediaControls.setAnchorView(this.vvSplash);
            }
            vvSplash.setVideoURI(Uri.parse("android.resource://" + this.getPackageName() + "/" + R.raw.splash_video));
            vvSplash.requestFocus();
            vvSplash.start();
            vvSplash.setOnCompletionListener(mediaPlayer -> {
                videoCompleted = true;
                boolean forceLocation = false;
                if (CacheUtil.getInstance().getApplication() != null && CacheUtil.getInstance().getApplication().getAttributes().getForceLocation() != 0) {
                    forceLocation = true;
                }
                if (CacheUtil.getInstance().isSeparatedMenus() && CacheUtil.getInstance().getCartQuantity() == 0) {
                    forceLocation = false;
                }
            });
        } else {
            image.setImageResource(BuildUtil.splashImage());
        }

    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {

    }

    @Override
    public void showForceUpdate(String message, String url) {
        showForceUpdateDialog("", message, CacheUtil.getInstance().getTranslations(Text.UPDATE), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (!url.isEmpty()) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } else {
                    String appPackageName = getPackageName();
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, CacheUtil.getInstance().getTranslations(Text.CANCEL), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                finish();
            }
        });

    }

    @Override
    public void getSoloDriver() {
        /*FirebaseOptions options = new FirebaseOptions.Builder()
                .setProjectId(BuildConfig.SOLO_DRIVER_PROJECT_ID)
                .setApplicationId(BuildConfig.SOLO_DRIVER_APPLICATION_ID)
                .setApiKey(BuildConfig.SOLO_DRIVER_API_KEY)
                .build();

        FirebaseApp soloDriver = null;
        boolean hasBeenInitialized = false;
        List<FirebaseApp> firebaseApps = FirebaseApp.getApps(this);
        for (FirebaseApp app : firebaseApps) {
            if (app.getName().equals(BuildConfig.SOLO_DRIVER_PACKAGE_NAME)) {
                hasBeenInitialized = true;
                soloDriver = app;
                break;
            }
        }
        if (!hasBeenInitialized) {
            soloDriver = FirebaseApp.initializeApp(this, options, BuildConfig.SOLO_DRIVER_PACKAGE_NAME);
        }
        splashPresenter.setSoloDriver(soloDriver);*/
    }

    @Override
    public void initializeSoloApi(String url) {
        SoloApi.init(this, url, BuildUtil.soloAppKey(), CacheUtil.getInstance().getConceptKey());
        SoloAuth.getInstance().setLanguage(LocaleUtil.getInstance().getLanguageSymbol());
        SoloApi.getInstance().setDebug(BuildConfig.BUILD_TYPE.equals("debug"));
    }

    @Override
    public void showError(String error) {
        showAlertDialog("", error);
    }

    @Override
    public void cacheSliders(boolean mid, ArrayList<Slide> slides) {
        sliderCount = slides.size();
        sliderPosition = 0;
        if (sliderCount > 0) {
            cacheSlide(mid, slides.get(sliderPosition));
        }
    }

    @Override
    public String getDefaultTranslations() {
        return JsonUtil.getJsonObjectAsset(this, "translations.json").toString();
    }

    @Override
    public void preloadImage(String imageUrl, RequestOptions requestOptions) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isDestroyed() && !isFinishing()) {
                    Glide.with(SplashActivity.this)
                            .downloadOnly()
                            .load(imageUrl)
                            .apply(requestOptions)
                            .submit();
                }
            }
        });
    }

    @Override
    public void saveToCache(String key, String value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CacheHelper.save(getApplicationContext(), key, value);
            }
        });
    }

    public void cacheSlide(boolean mid, Slide slide) {
        if (!isDestroyed() && !isFinishing()) {
            String imageUrl = StringUtil.getCloudflareSliderUrl(Values.SLIDER_IMAGE_WIDTH, Values.SLIDER_IMAGE_HEIGHT, slide.getImageUri());
            Glide.with(SplashActivity.this)
                    .downloadOnly()
                    .listener(new RequestListener<File>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<File> target, boolean isFirstResource) {
                            sliderPosition++;
                            if (sliderPosition < sliderCount) {
                                Slide slider = splashPresenter.getSlide(sliderPosition);
                                if (slider != null) {
                                    cacheSlide(mid, slider);
                                }
                            } else if (!mid) {
                                splashPresenter.getSlider(true);
                            } else {
                                splashPresenter.proceed();
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(File resource, Object model, Target<File> target, DataSource dataSource, boolean isFirstResource) {
                            sliderPosition++;
                            if (sliderPosition < sliderCount) {
                                Slide slider = splashPresenter.getSlide(sliderPosition);
                                if (slider != null) {
                                    cacheSlide(mid, slider);
                                }
                            }
                            if (!mid) {
                                splashPresenter.getSlider(true);
                            } else {
                                splashPresenter.proceed();
                            }
                            return false;
                        }
                    })
                    .load(imageUrl)
                    .submit(Values.SLIDER_IMAGE_WIDTH, Values.SLIDER_IMAGE_HEIGHT);
        }
    }

    private void checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                showAlertDialog(
                        CacheUtil.getInstance().getTranslations(Text.ENABLE_CART_NOTIFICATIONS_TITLE, "Enable Cart Notifications"),
                        CacheUtil.getInstance().getTranslations(Text.ENABLE_CART_NOTIFICATIONS_MESSAGE, "Please enable alarms and reminders for the app to receive cart notifications"),
                        CacheUtil.getInstance().getTranslations(Text.OK),
                        (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        },
                        CacheUtil.getInstance().getTranslations(Text.CANCEL),
                        (dialogInterface, i) -> dialogInterface.dismiss()
                );

            }
        }
    }

    private void isAuthCredentialMigrated() {
        if (!CacheUtil.getInstance().getIsAuthCredentialMigrated()) {
//            LoginManager.getInstance().logOut();
//            GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                    .requestIdToken(BuildUtil.googleInClient(this))
//                    .requestEmail()
//                    .build();
//            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
//            googleSignInClient.signOut();
//            splashPresenter.removeMukafaaRedemption();
//            SoloAuth.getInstance().clearSoloAuthPref();
//            AuthUtil.getInstance().clearAuthUtilPref();
            AuthUtil.getInstance().migrate();
            SoloAuth.getInstance().migrate();
            CacheUtil.getInstance().setIsAuthCredentialMigrated(true);
        }
    }

}
