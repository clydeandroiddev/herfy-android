package com.herfy.splash;

import android.Manifest;
import android.animation.Animator;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.capillary.hydra.android.sdk.FcmConfig;
import com.capillary.hydra.android.sdk.HydraAPI;
import com.capillary.hydra.android.sdk.HydraConfig;
import com.capillary.hydra.android.sdk.utils.LogType;
import com.capillary.hydra.firebase.HydraFirebaseAPI;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.crashlytics.internal.common.CommonUtils;
import com.google.gson.Gson;
import com.herfy.BuildConfig;
import com.herfy.R;
import com.herfy.animation.ActivitySwitcher;
import com.herfy.base.BaseActivity;
import com.herfy.base.BaseApplication;
import com.herfy.country.CountriesActivity;
import com.herfy.language.LanguageNewActivity;
import com.herfy.main.MainActivity;
import com.herfy.util.AppLogger;
import com.herfy.util.obfuscator.Keychain;
import com.herfy.util.obfuscator.ObfuscatorKt;
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
import com.herfy.utils.theme.DynamicTheme;
import com.skylinedynamics.solosdk.api.SoloApi;
import com.skylinedynamics.solosdk.api.SoloAuth;
import com.skylinedynamics.solosdk.api.models.objects.Slide;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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

    @BindView(R.id.lv_splash)
    LottieAnimationView lvSplash;

    private boolean fromChangeLanguage = false;
    private boolean concepts = false;
    private boolean recreate = false;
    private boolean router = false;

    private boolean proceedMain;
    private int sliderCount = 0;
    private int sliderPosition = 0;
    private MediaController mediaControls;

    private boolean videoCompleted = false;

    private boolean isFetchingDone = false;
    private boolean isNotificationPermissionShowing = false;


    private final ActivityResultLauncher<String> notificationsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                isNotificationPermissionShowing = false;
                if(isFetchingDone){
                    boolean forceLocation = false;
                    if (CacheUtil.getInstance().getApplication().getAttributes().getForceLocation() != 0) {
                        forceLocation = true;
                    }

                    if (CacheUtil.getInstance().isSeparatedMenus() && CacheUtil.getInstance().getCartQuantity() == 0) {
                        forceLocation = false;
                    }
                    checkAlarmPermission();
                    proceed(CacheUtil.getInstance().getLanguage(), CacheUtil.getInstance().getOnboarding(), forceLocation);
                }else {
                    checkAlarmPermission();
                }

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

        boolean fromChangeLanguage = getIntent().getBooleanExtra("fromChangeLanguage", false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !fromChangeLanguage) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                isNotificationPermissionShowing = true;
                notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                checkAlarmPermission();
            }
        }

        if(!AuthUtil.getInstance().getFirebase().isEmpty() && AuthUtil.getInstance().isSignedIn()) {
            //HYDRA SIGN IN EVENT
            HydraAPI.Companion.getDefaultInstance(getApplicationContext()).
                    pushUserSignin(
                            AuthUtil.getInstance().getCustomer().getAttributes().getMobile(),
                            AuthUtil.getInstance().getCustomer().getAttributes().getFirstName(),
                            AuthUtil.getInstance().getCustomer().getAttributes().getLastName(),
                            AuthUtil.getInstance().getCustomer().getAttributes().getEmail(),
                            AuthUtil.getInstance().getCustomer().getAttributes().getMobile(),
                            Collections.emptyMap());

            HydraFirebaseAPI.getInstance().passPushToken(getApplicationContext(), AuthUtil.getInstance().getFirebase());

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSharedProject();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        closeApp();
    }

    @Override
    public void init() {
        setStatusBarColor(DynamicTheme.getDefaultPrimaryColor());
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.containsKey("fromChangeLanguage")) {
                fromChangeLanguage = bundle.getBoolean("fromChangeLanguage");
                if (fromChangeLanguage && (!BuildUtil.splashVideoApp() && !BuildUtil.splashGIFApp())) {
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
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                intent.putExtra("fromChangeLanguage", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                if (!BuildUtil.splashVideoApp() && !BuildUtil.splashGIFApp()) {
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                } else {
                    if(!BuildUtil.splashGIFApp()){
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
    public void checkIfRooted() {
        if (CommonUtils.isRooted()) {
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
        new Handler(getMainLooper()).post(() -> {
            if(!isNotificationPermissionShowing){
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
                    if (BuildConfig.DISABLE_SPLASH_ANIMATION || (BuildUtil.splashBackgroundApp() || BuildUtil.splashGIFApp()) || videoCompleted) {
                        proceedNext(false, finalIntent);
                    } else {
                        animateLogo(false, finalIntent);
                    }
                }
            }
        });
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
            lvSplash.setVisibility(View.GONE);
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
        } else if (BuildUtil.splashGIFApp()){
            vvSplash.setVisibility(View.GONE);
            image.setVisibility(View.INVISIBLE);
            background.setVisibility(View.INVISIBLE);
            lvSplash.setVisibility(View.VISIBLE);
            lvSplash.setAnimation(R.raw.splash_gif);
            lvSplash.requestFocus();
            lvSplash.playAnimation();
            lvSplash.addAnimatorListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(@NonNull Animator animator) {
                }

                @Override
                public void onAnimationEnd(@NonNull Animator animator) {
                    videoCompleted = true;
                    Bundle bundle = getIntent().getExtras();
                    if (bundle != null) {
                        if (bundle.containsKey("fromChangeLanguage")) {
                            boolean fromChangeLanguage = bundle.getBoolean("fromChangeLanguage");
                            if(fromChangeLanguage){
                                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                                intent.putExtra("fromChangeLanguage", true);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                startActivity(intent);
                                overridePendingTransition(0, 0);
                                finish();

                            }
                        }
                    }

                }

                @Override
                public void onAnimationCancel(@NonNull Animator animator) {
                }

                @Override
                public void onAnimationRepeat(@NonNull Animator animator) {
                }
            });
        }else {
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
    public void getSharedProject() {
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setProjectId(ObfuscatorKt.reveal(Keychain.INSTANCE.getSHARED_PROJECT_ID()))
                .setApplicationId(ObfuscatorKt.reveal(Keychain.INSTANCE.getSHARED_PROJECT_APPLICATION_ID()))
                .setApiKey(ObfuscatorKt.reveal(Keychain.INSTANCE.getSHARED_PROJECT_API_KEY()))
                .build();

        FirebaseApp sharedProject = null;
        boolean hasBeenInitialized = false;
        String packageName = ObfuscatorKt.reveal(Keychain.INSTANCE.getSHARED_PROJECT_PACKAGE_NAME());
        List<FirebaseApp> firebaseApps = FirebaseApp.getApps(this);
        for (FirebaseApp app : firebaseApps) {
            if (app.getName().equals(packageName)) {
                hasBeenInitialized = true;
                sharedProject = app;
                break;
            }
        }
        if (!hasBeenInitialized) {
            sharedProject = FirebaseApp.initializeApp(this, options, packageName);
        }
        splashPresenter.setSharedProject(sharedProject);
    }

    @Override
    public void initializeSoloApi(String url) {
        SoloApi.init(this, url, BuildUtil.soloAppKey(), CacheUtil.getInstance().getConceptKey());
        SoloAuth.getInstance().setLanguage(LocaleUtil.getInstance().getLanguageSymbol());
        SoloApi.getInstance().setDebug(BuildConfig.BUILD_TYPE.equals("debug"));
    }

    @Override
    public void showError(String error, boolean closeApp) {
        if(!closeApp) {
            showAlertDialog("", error);
        } else {
            showAlertDialog("", error, CacheUtil.getInstance().getTranslations(Text.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    closeApp();
                }
            });
        }
    }

    private void closeApp() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
                boolean isArabic = LocaleUtil.getInstance().isArabic();
                String title;
                String message;
                String okay;
                String cancel;
                if(isArabic) {
                    title = getResources().getString(R.string.enable_cart_notifications_title_ar);
                    message = getResources().getString(R.string.enable_cart_notifications_message_ar);
                    okay = getResources().getString(R.string.okay_ar);
                    cancel = getResources().getString(R.string.cancel_ar);
                } else {
                    title = getResources().getString(R.string.enable_cart_notifications_title_en);
                    message = getResources().getString(R.string.enable_cart_notifications_message_en);
                    okay = getResources().getString(R.string.okay_en);
                    cancel = getResources().getString(R.string.cancel_en);
                }
                showAlertDialog(
                    title,
                    message,
                    okay,
                    (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    },
                    cancel,
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

    @Override
    public void initializeHydraSdk() {
        String baseUrl = CacheUtil.getInstance().getCapillaryBaseUrl();
        String publicKey = CacheUtil.getInstance().getCapillarySslPublicKey();

        if (baseUrl != null && !baseUrl.isEmpty() && publicKey != null && !publicKey.isEmpty()) {
            ((BaseApplication) getApplicationContext()).initializeHydraSdk(getApplicationContext());
        }

    }
}
