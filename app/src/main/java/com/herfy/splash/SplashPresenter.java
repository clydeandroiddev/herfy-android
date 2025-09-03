package com.herfy.splash;

import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.herfy.BuildConfig;
import com.herfy.database.AppDatabase;
import com.herfy.database.entities.AllergenDb;
import com.herfy.database.entities.IngredientDb;
import com.herfy.database.entities.MenuCategoryEntity;
import com.herfy.database.entities.MenuItemDb;
import com.herfy.database.entities.MenuItemEntity;
import com.herfy.database.entities.ModifierGroupDb;
import com.herfy.database.entities.ModifierItemDb;
import com.herfy.database.responses.MenuCategoryItemResponse;
import com.herfy.model.SupportedCountry;
import com.herfy.util.AppLogger;
import com.herfy.util.StringUtil;
import com.herfy.util.hmac.HmacUtil;
import com.herfy.utils.constants.Text;
import com.herfy.utils.constants.Values;
import com.herfy.utils.handler.ErrorHandler;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.BuildUtil;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.helper.DeviceUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.utils.menu.MenuUtil;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.ApiRequestWithHeadersListener;
import com.skylinedynamics.solosdk.api.SoloAuth;
import com.skylinedynamics.solosdk.api.calls.ApplicationsApiCall;
import com.skylinedynamics.solosdk.api.calls.CmsApiCall;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.calls.IntegrationsApiCall;
import com.skylinedynamics.solosdk.api.calls.UtilitiesApiCall;
import com.skylinedynamics.solosdk.api.models.objects.AndroidForceUpdate;
import com.skylinedynamics.solosdk.api.models.objects.Application;
import com.skylinedynamics.solosdk.api.models.objects.Concept;
import com.skylinedynamics.solosdk.api.models.objects.ConsumerData;
import com.skylinedynamics.solosdk.api.models.objects.Customer;
import com.skylinedynamics.solosdk.api.models.objects.ForceUpdate;
import com.skylinedynamics.solosdk.api.models.objects.Integration;
import com.skylinedynamics.solosdk.api.models.objects.Loyalty;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTierV2;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyV2;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupType;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.PaymentType;
import com.skylinedynamics.solosdk.api.models.objects.Slide;
import com.skylinedynamics.solosdk.api.models.objects.Slider;
import com.skylinedynamics.solosdk.api.models.objects.Tier;
import com.skylinedynamics.solosdk.api.models.objects.area.Area;
import com.skylinedynamics.solosdk.api.models.objects.area.City;
import com.skylinedynamics.solosdk.util.GsonHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.reactivex.Maybe;
import io.reactivex.disposables.CompositeDisposable;
import okhttp3.Headers;

public class SplashPresenter implements SplashContract.Presenter {

    private final SplashContract.View splashView;

    private FirebaseRemoteConfig firebaseRemoteConfig;

    private boolean recreate;

    private int errorCounter = 0;

    private ArrayList<Slide> slides = new ArrayList<>();
    private int slideCount = 0;
    private int slidePosition = 0;
    private CompositeDisposable disposables = new CompositeDisposable();

    public SplashPresenter(SplashContract.View splashView) {
        this.splashView = splashView;
        this.splashView.setPresenter(this);
    }

    @Override
    public void start() {
        splashView.setupViews();
        splashView.setupFonts();
        splashView.setupTranslations();
        setupRemoteConfig();
        splashView.checkIfRooted();
    }

    @Override
    public void setRecreate(boolean recreate) {
        this.recreate = recreate;
    }

    @Override
    public void setAreas() {
        UtilitiesApiCall.getInstance().getTranslations("https://cdn.getsolo.io/content/herfycityandarea.json", new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    Gson gson = new Gson();
                    Type type = new TypeToken<Map<String, ArrayList<Area>>>() {
                    }.getType();
                    Map<String, ArrayList<Area>> cityMap = gson.fromJson(o.toString(), type);
                    ArrayList<City> cities = new ArrayList<>();
                    for (Map.Entry<String, ArrayList<Area>> entry : cityMap.entrySet()) {
                        String city = entry.getKey();
                        City city1 = new City();
                        city1.setName(city);
                        city1.setId(city);
                        city1.setAreas(entry.getValue());
                        cities.add(city1);
                    }
                    CacheUtil.getInstance().setCities(gson.toJson(cities));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Object o) {
            }
        });
    }

    @Override
    public void checkTranslations() {

        long timestamp = System.currentTimeMillis() / 1000L;
        String url = CacheUtil.getInstance().getApplication().getAttributes().getDefaultContentUri() + "?timestamp=" + timestamp;

        disposables.add(getTranslationsObservable(url).map(o -> {
            try {
                JSONObject data = new JSONObject(o.toString());
                return data;
            } catch (JSONException e) {
                e.printStackTrace();
                throw new Exception("Cannot find translation");
            }
        }).flatMap(defaultTranslations -> getTranslationsObservable(url).map(newTranslation -> {
            JSONObject overwritingTranslations = new JSONObject(newTranslation.toString());
            JSONObject mergedTranslations = new JSONObject();
            JSONObject[] translations = new JSONObject[]{defaultTranslations, overwritingTranslations};
            for (JSONObject translation : translations) {
                Iterator it = translation.keys();
                while (it.hasNext()) {
                    String key = (String) it.next();
                    mergedTranslations.put(key, translation.get(key));
                }
            }
            return mergedTranslations;
        }).onErrorResumeNext(source -> {
            source.onSuccess(defaultTranslations);
        })).subscribe(o -> {
            if (o != null) {
                JSONObject data = new JSONObject(o.toString());
                saveTranslations(data.toString());
            }
        }, error -> {
            AppLogger.printRetrofitError(error.toString());
            if (!CacheUtil.getInstance().ifHasTranslations()) {
                saveTranslations(splashView.getDefaultTranslations());
            }
        }));

    }

    public Maybe<Object> getTranslationsObservable(String url) {
        return Maybe.create(e -> {
            UtilitiesApiCall.getInstance().getTranslations(url, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    e.onSuccess(o);
                    e.onComplete();
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o.toString());
                    if (o instanceof Exception) {
                        e.onError((Exception) o);
                    } else if (o instanceof Throwable) {
                        e.onError((Throwable) o);
                    } else {
                        e.onError(new Throwable("unhandled_exception"));
                    }

                    e.onComplete();
                }
            });
        });
    }

    @Override
    public void getTranslations(String url, ApiRequestListener listener) {
        UtilitiesApiCall.getInstance().getTranslations(url, listener);
    }

    @Override
    public void saveTranslations(String translations) {
        CacheUtil.getInstance().saveTranslations(translations);
    }

    public void setupRemoteConfig() {
        if (!BuildConfig.DISABLE_SPLASH_ANIMATION) {
            splashView.animateLogo(true, null);
        }
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(0L).build();
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        getRemoteConfig();
    }

    @Override
    public void getRemoteConfig() {
        new Handler().post(() -> {
            firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(new OnCompleteListener<Boolean>() {

                @Override
                public void onComplete(@NonNull Task<Boolean> task) {

                    if (task.isSuccessful()) {

                        try {

                            CacheUtil.getInstance().setCapillaryHydraConfig(firebaseRemoteConfig.getString("capillary_hydra_sdk_config"));
                            

                            String inAppReviewEnableKey = "android_enable_in_app_review";
                            boolean inAppReviewEnable = firebaseRemoteConfig.getBoolean(inAppReviewEnableKey);
                            CacheUtil.getInstance().setInAppReviewEnable(inAppReviewEnable);

                            String isContactUsEnabledKey = "is_contact_us_enabled";
                            boolean isContactUsEnabled = firebaseRemoteConfig.getBoolean(isContactUsEnabledKey);
                            CacheUtil.getInstance().setIsContactUsEnabled(isContactUsEnabled);

                            String languageVideoKey = "language_video";
                            String languageVideoURL = firebaseRemoteConfig.getString(languageVideoKey);
                            try {
                                JSONObject jsonObject = new JSONObject(languageVideoURL);
                                String androidUrl = jsonObject.getString("android");
                                if (androidUrl.isEmpty()) {
                                    androidUrl = Values.LANGUAGE_BACKGROUND_VIDEO_URL;
                                }
                                CacheUtil.getInstance().setLanguageStreamingURL(androidUrl);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            String supportCardTypesKey = "support_card_types";
                            String supportCardTypesValue = firebaseRemoteConfig.getString(supportCardTypesKey);

                            if (!supportCardTypesValue.isEmpty()) {
                                JSONArray jsonArray = new JSONArray(supportCardTypesValue);
                                CacheUtil.getInstance().setSupportCardTypes(supportCardTypesValue);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }
            });
        });
    }

    @Override
    public void setSharedProject(FirebaseApp sharedFirebaseApp) {

        FirebaseRemoteConfig sharedRemoteConfig = FirebaseRemoteConfig.getInstance(sharedFirebaseApp);

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0L)
                .build();
        sharedRemoteConfig.setConfigSettingsAsync(configSettings);

        new Handler().post(() -> {
            sharedRemoteConfig.fetchAndActivate().addOnCompleteListener(new OnCompleteListener<Boolean>() {

                @Override
                public void onComplete(@NonNull Task<Boolean> task) {

                    if (task.isSuccessful()) {

                        String sharedStringRemoteConfigKey = HmacUtil.getSharedStringRemoteConfigKey();
                        String sharedConfigString = sharedRemoteConfig.getString(sharedStringRemoteConfigKey);
                        CacheUtil.getInstance().setSharedStringConfig(sharedConfigString);
                        CacheUtil.getInstance().setCapillaryHydraConfig(sharedRemoteConfig.getString("capillary_hydra_sdk_config"));


                    }

                    splashView.init();

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    splashView.init();
                }
            }).addOnCanceledListener(new OnCanceledListener() {
                @Override
                public void onCanceled() {
                    splashView.init();
                }
            });
        });
    }

    @Override
    public void sendLanguage() {

        if (AuthUtil.getInstance().isSignedIn() && !AuthUtil.getInstance().getDeviceId().isEmpty()) {

            UtilitiesApiCall.getInstance().changeLanguage(AuthUtil.getInstance().getDeviceId(), LocaleUtil.getInstance().getLanguage(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object obj) {
                    DeviceUtil.save(obj.toString());
                }

                @Override
                public void onError(Object error) {
                    AppLogger.printRetrofitError(error);
                }
            });

        } else if (!AuthUtil.getInstance().getFirebase().isEmpty()) {
            String uniqueID = AuthUtil.getInstance().getUniqueId();

            if (uniqueID.isEmpty()) {
                uniqueID = UUID.randomUUID().toString();
                AuthUtil.getInstance().setUniqueId(uniqueID);
            }

            String customerId = null;
            String customerMobileNumber = null;
            if (AuthUtil.getInstance().isSignedIn()) {
                customerId = AuthUtil.getInstance().getCustomer().getId();
                customerMobileNumber = AuthUtil.getInstance().getCustomer().getAttributes().getMobile();
            }

            UtilitiesApiCall.getInstance().postIdentify(AuthUtil.getInstance().getFirebase(), uniqueID, customerMobileNumber, customerId, new ApiRequestListener() {
                @Override
                public void onSuccess(Object object) {
                    DeviceUtil.save(object.toString());
                    if (AuthUtil.getInstance().isSignedIn() && !AuthUtil.getInstance().getDeviceId().isEmpty()) {

                        UtilitiesApiCall.getInstance().changeLanguage(AuthUtil.getInstance().getDeviceId(), LocaleUtil.getInstance().getLanguage(), new ApiRequestListener() {
                            @Override
                            public void onSuccess(Object obj) {
                                DeviceUtil.save(obj.toString());
                            }

                            @Override
                            public void onError(Object error) {
                                AppLogger.printRetrofitError(error);
                            }
                        });

                    }
                }

                @Override
                public void onError(Object error) {
                    AppLogger.printRetrofitError(error);
                }
            });

        }

    }

    private void cacheCountryApplication(String application) {
        if (BuildConfig.MULTI_COUNTRY_ENABLED) {
            String applicationId = BuildConfig.APPLICATION_ID;
            SupportedCountry country = CacheUtil.getInstance().getCountry();
            if (country != null) {
                applicationId = country.applicationId;
            }
            CacheUtil.getInstance().setCountryApplication(applicationId, application);
        }
    }

    private void cacheCountrySliders(String sliders) {
        if (BuildConfig.MULTI_COUNTRY_ENABLED) {
            String applicationId = BuildConfig.APPLICATION_ID;
            SupportedCountry country = CacheUtil.getInstance().getCountry();
            if (country != null) {
                applicationId = country.applicationId;
            }
            CacheUtil.getInstance().setCountrySlider(applicationId, sliders);
        }
    }

    private void cachedCountryMenu(String menu) {
        if (BuildConfig.MULTI_COUNTRY_ENABLED) {
            String applicationId = BuildConfig.APPLICATION_ID;
            SupportedCountry country = CacheUtil.getInstance().getCountry();
            if (country != null) {
                applicationId = country.applicationId;
            }
            CacheUtil.getInstance().setCountryMenu(applicationId, menu);
            CacheUtil.getInstance().setCountryCachedTime(applicationId, System.currentTimeMillis());
        }
    }

    @Override
    public void getApplication() {
        ApplicationsApiCall.getInstance().getApplication(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    errorCounter = 0;
                    String object = o.toString();
                    JSONObject jsonObject = new JSONObject(object);
                    cacheCountryApplication(object);
                    JSONObject data = jsonObject.getJSONObject("data");
                    Application application = GsonHelper.gsonFinal().fromJson(data.toString(), new TypeToken<Application>() {
                    }.getType());

                    CacheUtil.getInstance().setApplication(application);

                    checkTranslations();

                    getIntegrations();
                    if (AuthUtil.getInstance().isSignedIn()) {
                        getLoyalty();
                    }
                    getLoyaltyTiers();

                    CacheUtil.getInstance().setColorMain(application.getAttributes().getTheme().getPrimaryColor());
                    CacheUtil.getInstance().setSliderId(String.valueOf(application.getAttributes().getSliderId()));
                    CacheUtil.getInstance().setMenuId(String.valueOf(application.getAttributes().getDefaultMenuId()));

                    if (jsonObject.has("included") && !jsonObject.isNull("included")) {
                        JSONArray included = jsonObject.getJSONArray("included");
                        if (included.length() > 0) {
                            for (int i = 0; i < included.length(); i++) {
                                if (included.getJSONObject(i).optString("type").equalsIgnoreCase("concept")) {
                                    Concept concept = new Concept();
                                    concept = concept.parse(included.getJSONObject(i));
                                    CacheUtil.getInstance().setConcept(concept);
                                    getConsumerData();
                                    String conceptLogo = concept.getLogo();
                                    if (BuildConfig.FEATURE_PHASE >= 1.3f && conceptLogo != null && !conceptLogo.isEmpty()) {
                                        splashView.preloadImage(conceptLogo, RequestOptions.centerInsideTransform());
                                    }
                                } else if (included.getJSONObject(i).optString("type").equalsIgnoreCase("popup")) {
                                    CacheUtil.getInstance().setPopup(included.getJSONObject(i));
                                }
                            }
                        } else {
                            this.onError("");
                        }
                    } else {
                        this.onError("");
                    }

                    try {
                        ForceUpdate forceUpdate = application.getAttributes().getForceUpdate();
                        if (forceUpdate != null && forceUpdate.getAndroid() != null) {
                            AndroidForceUpdate androidForceUpdate = forceUpdate.getAndroid();
                            String version = androidForceUpdate.getVersion();
                            if (androidForceUpdate.getNeedUpdate() && !version.isEmpty() && !version.equalsIgnoreCase("null") && Integer.parseInt(version.replace(".", "")) > Integer.parseInt(BuildConfig.VERSION_NAME.replace(".", ""))) {
                                if (LocaleUtil.getInstance().isEnglish() || LocaleUtil.getInstance().isFrench()) {
                                    splashView.showForceUpdate(androidForceUpdate.getMessageEnglish(), androidForceUpdate.getUrl());
                                } else {
                                    splashView.showForceUpdate(androidForceUpdate.getMessageArabic(), androidForceUpdate.getUrl());
                                }
                                return;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    getSliders();
                    //getSlider();
                    //proceed();

                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o.toString());
                errorCounter++;
                if (errorCounter >= 1) {
                    splashView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null), true);
                } else {
                    getApplication();
                }
            }
        });
    }

    public void getConsumerData() {
        Customer customer = AuthUtil.getInstance().getCustomer();
        if (customer != null) {
            CacheUtil.getInstance().setConsumerData(null);
            CustomersApiCall.getInstance().getConsumerData(customer.getId(), "desc", 1, new ApiRequestListener() {
                @Override
                public void onSuccess(Object obj) {
                    if (obj instanceof JSONObject) {
                        try {
                            ConsumerData data = GsonHelper.gsonFinal().fromJson(obj.toString(), new TypeToken<ConsumerData>() {
                            }.getType());
                            CacheUtil.getInstance().setConsumerData(obj.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                    }
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                }
            });
        }

    }

    @Override
    public void getIntegrations() {
        if (AuthUtil.getInstance().isSignedIn()) {
            String paymentGateway = CacheUtil.getInstance().getApplication().getAttributes().getPaymentGateway();
            if (paymentGateway.equalsIgnoreCase("checkout") || paymentGateway.equalsIgnoreCase("foodics-online")) {
                IntegrationsApiCall.getInstance().getKeys(paymentGateway, new ApiRequestListener() {
                    @Override
                    public void onSuccess(Object o) {
                        try {
                            JSONObject object = new JSONObject(o.toString());
                            if (object.has("data") && !object.isNull("data")) {
                                Integration integration = GsonHelper.gsonFinal().fromJson(object.getJSONObject("data").toString(), Integration.class);
                                CacheUtil.getInstance().setIntegration(integration);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Object o) {
                        AppLogger.printRetrofitError(o.toString());
                    }
                });
            }
        }
    }

    @Override
    public void getSliders() {
        CmsApiCall.getInstance().getSliders(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    String objectString = o.toString();
                    JSONObject object = new JSONObject(objectString);
                    cacheCountrySliders(objectString);
                    if (object.has("data") && !object.isNull("data")) {
                        JSONArray data = object.getJSONArray("data");
                        for (int i = 0; i < data.length(); i++) {
                            Slider slider = Slider.parse(data.getJSONObject(i));
                            String label = slider.getLabel();
                            if (label != null) {
                                String key = "";
                                if (label.equalsIgnoreCase(Values.HOME_PAGE_TOP_BANNER_LABEL)) {
                                    key = Values.HOME_PAGE_TOP_BANNER_LABEL;
                                } else if (label.equalsIgnoreCase(Values.HOME_PAGE_BOTTOM_BANNER_LABEL)) {
                                    key = Values.HOME_PAGE_BOTTOM_BANNER_LABEL;
                                } else if (label.equalsIgnoreCase(Values.CATEGORY_PAGE_TOP_BANNER_LABEL)) {
                                    key = Values.CATEGORY_PAGE_TOP_BANNER_LABEL;
                                }
                                if (label.equalsIgnoreCase(Values.CATER_HERFY_BANNER)) {
                                    CacheUtil.getInstance().setSliderId(Values.CATER_HERFY_BANNER, slider.getId());
                                }
                                if (label.equalsIgnoreCase(Values.LOGIN_BANNER)) {
                                    CacheUtil.getInstance().setSliderId(Values.LOGIN_BANNER, slider.getId());
                                }
                                if (!key.isEmpty()) {
                                    CacheUtil.getInstance().setSliderId(key, slider.getId());
                                }
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    CacheUtil.getInstance().setSliderId(Values.CATER_HERFY_BANNER, "");
                    CacheUtil.getInstance().setSliderId(Values.HOME_PAGE_TOP_BANNER_LABEL, "");
                    CacheUtil.getInstance().setSliderId(Values.HOME_PAGE_BOTTOM_BANNER_LABEL, "");
                    CacheUtil.getInstance().setSliderId(Values.CATEGORY_PAGE_TOP_BANNER_LABEL, "");
                }

                //getSlider(false);
                proceed();
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o.toString());
                CacheUtil.getInstance().setMidSliderId("");
                //getSlider(false);
                proceed();
            }
        });
    }

    @Override
    public void getSlider(boolean mid) {

        String sliderId = CacheUtil.getInstance().getSliderId();
        if (mid) {
            sliderId = CacheUtil.getInstance().getMidSliderId();
        }

        if (!sliderId.isEmpty()) {
            CmsApiCall.getInstance().getSlider(sliderId, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {

                    slides.clear();

                    try {

                        JSONObject object = new JSONObject(o.toString());

                        JSONArray dataArray = object.getJSONArray("data");

                        for (int i = 0; i < dataArray.length(); i++) {

                            JSONObject slideObject = dataArray.getJSONObject(i);

                            Slide slide = Slide.parse(slideObject);

                            slides.add(slide);

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (slides.size() > 0) {
                        if (!mid) {
                            CacheUtil.getInstance().setSlides(new Gson().toJson(slides));
                        } else {
                            CacheUtil.getInstance().setMidSlides(new Gson().toJson(slides));
                        }
                        splashView.cacheSliders(mid, slides);
                    } else if (!mid) {
                        getSlider(true);
                    } else {
                        proceed();
                    }

                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    if (slides.size() > 0) {
                        splashView.cacheSliders(mid, slides);
                    } else if (!mid) {
                        getSlider(true);
                    } else {
                        proceed();
                    }
                }
            });
        } else if (!mid) {
            getSlider(true);
        } else {
            proceed();
        }
    }

    @Override
    public Slide getSlide(int position) {
        if (position < slides.size()) {
            return slides.get(position);
        }
        return null;
    }

    @Override
    public void getMenuCdn() {

        AppDatabase appDatabase = AppDatabase.getInstance();
        String applicationId = CacheUtil.getInstance().getApplication().getId();
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                appDatabase.menuCategoryDao().deleteByApplication(applicationId);
                String url = MenuUtil.getMenuCdnUrl();
                UtilitiesApiCall.getInstance().getMenuCategoriesWithHeaders(url, new ApiRequestWithHeadersListener() {
                    @Override
                    public void onSuccess(Headers headers, Object o) {
                        try {
                            if (headers.names().contains("last-modified")) {
                                CacheUtil.getInstance().setMenuCdnLastModified(headers.get("last-modified"));
                            }
                            String objectString = o.toString();
                            JSONObject object = new JSONObject(objectString);
                            cachedCountryMenu(objectString);
                            parseMenu(object);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            this.onError("");
                        }
                    }

                    @Override
                    public void onError(Object o) {
                        AppLogger.printRetrofitError(o.toString());
                        splashView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null), false);
                    }
                });
            }
        });

    }

    @Override
    public void parseMenu(JSONObject data) {
        Executor executor = Executors.newSingleThreadExecutor();

        AppDatabase appDatabase = AppDatabase.getInstance();

        executor.execute(new Runnable() {
            @Override
            public void run() {

                try {
                    String applicationId = CacheUtil.getInstance().getApplication().getId();

                    JSONArray dataArray = data.getJSONArray("data");

                    List<MenuCategoryItemResponse> menuCategoryItemsResponse = GsonHelper.gsonFinal().fromJson(dataArray.toString(), new TypeToken<ArrayList<MenuCategoryItemResponse>>() {
                    }.getType());

                    List<MenuCategoryEntity> menuCategoryEntities = new ArrayList<>();
                    List<MenuItemEntity> menuItemEntities = new ArrayList<>();
                    for (MenuCategoryItemResponse menuCategoryItem : menuCategoryItemsResponse) {
                        MenuCategoryEntity menuCategoryEntity = menuCategoryItem.getAttributes().getMenuCategory();
                        menuCategoryEntity.setApplicationId(applicationId);
                        if (!MenuUtil.isExcluded(menuCategoryEntity.getExcludedApps())) {
                            menuCategoryEntities.add(menuCategoryEntity);
                            String categoryImageUrl = menuCategoryItem.getAttributes().getMenuCategory().getImage();
                            if (categoryImageUrl == null || categoryImageUrl.equalsIgnoreCase("null") || categoryImageUrl.isEmpty() || categoryImageUrl.toLowerCase().contains("default-image.png")) {
                                categoryImageUrl = Values.DEFAULT_IMAGE_URL;
                            } else {
                                categoryImageUrl = StringUtil.getCloudflareUrl(Values.MENU_IMAGE_SIZE, Values.MENU_IMAGE_SIZE, categoryImageUrl);
                            }
                            splashView.preloadImage(categoryImageUrl, RequestOptions.centerInsideTransform());

                            for (MenuItemEntity menuItem : menuCategoryItem.getAttributes().getMenuItems()) {
                                menuItem.setApplicationId(applicationId);
                                menuItem.setMenuCategoryId(menuCategoryItem.getAttributes().getMenuCategory().getId());
                                if (!MenuUtil.isExcluded(menuItem.getExcludedApps())) {
                                    menuItemEntities.add(menuItem);
                                    String itemImageUrl = menuItem.getImage();
                                    if (itemImageUrl == null || itemImageUrl.equalsIgnoreCase("null") || itemImageUrl.isEmpty() || itemImageUrl.toLowerCase().contains("default-image.png")) {
                                        itemImageUrl = Values.DEFAULT_IMAGE_URL;
                                    } else {
                                        itemImageUrl = StringUtil.getCloudflareUrl(Values.MENU_IMAGE_SIZE, Values.MENU_IMAGE_SIZE, itemImageUrl);
                                    }
                                    splashView.preloadImage(itemImageUrl, RequestOptions.centerCropTransform());
                                }
                            }
                        }

                    }

                    if (!menuItemEntities.isEmpty()) {
                        appDatabase.menuItemEntityDao().deleteByApplication(applicationId);
                        appDatabase.menuItemEntityDao().insertAll(menuItemEntities.toArray(new MenuItemEntity[menuCategoryEntities.size()]));
                    }

                    if (!menuCategoryEntities.isEmpty()) {
                        appDatabase.menuCategoryEntityDao().deleteByApplication(applicationId);
                        appDatabase.menuCategoryEntityDao().insertAll(menuCategoryEntities.toArray(new MenuCategoryEntity[menuCategoryEntities.size()]));
                    }

                    JSONObject included = data.getJSONObject("included");

                    JSONArray allergensArray = new JSONArray();
                    if (included.has("allergens") && !included.isNull("allergens")) {
                        allergensArray = included.getJSONArray("allergens");
                    }
                    appDatabase.allergenDao().deleteByApplication(applicationId);
                    for (int index = 0; index < allergensArray.length(); index++) {
                        JSONObject allergenObject = allergensArray.getJSONObject(index);
                        String allergenId = String.valueOf(allergenObject.getLong("id"));
                        AllergenDb allergenDb = new AllergenDb();
                        allergenDb.applicationId = applicationId;
                        allergenDb.itemAllergenId = allergenId;
                        allergenDb.id = allergenId;
                        allergenDb.data = allergenObject.toString();
                        appDatabase.allergenDao().insertAll(allergenDb);
                    }

                    JSONArray ingredientsArray = new JSONArray();
                    if (included.has("ingredients") && !included.isNull("ingredients")) {
                        ingredientsArray = included.getJSONArray("ingredients");
                    }
                    appDatabase.ingredientDao().deleteByApplication(applicationId);
                    for (int index = 0; index < ingredientsArray.length(); index++) {
                        JSONObject ingredientObject = ingredientsArray.getJSONObject(index);
                        String ingredientId = String.valueOf(ingredientObject.getLong("id"));
                        IngredientDb ingredientDb = new IngredientDb();
                        ingredientDb.applicationId = applicationId;
                        ingredientDb.itemIngredientId = ingredientId;
                        ingredientDb.id = ingredientId;
                        ingredientDb.data = ingredientObject.toString();
                        appDatabase.ingredientDao().insertAll(ingredientDb);
                    }

                    JSONArray modifierGroupsArray = new JSONArray();
                    if (included.has("modifierGroups") && !included.isNull("modifierGroups")) {
                        modifierGroupsArray = included.getJSONArray("modifierGroups");
                    }
                    appDatabase.modifierGroupDao().deleteByApplication(applicationId);
                    for (int index = 0; index < modifierGroupsArray.length(); index++) {
                        JSONObject modifierGroupObject = modifierGroupsArray.getJSONObject(index);
                        JSONObject modifierGroupAttributesObject = modifierGroupObject.getJSONObject("attributes");

                        String code = "";
                        if (modifierGroupAttributesObject.getString("code") != null) {
                            code = modifierGroupAttributesObject.getString("code").trim().toLowerCase();
                        }
                        String type = "";
                        if (modifierGroupAttributesObject.getString("type") != null) {
                            type = modifierGroupAttributesObject.getString("type").trim().toLowerCase();
                        }
                        String label = "";
                        if (modifierGroupAttributesObject.getString("label") != null) {
                            label = modifierGroupAttributesObject.getString("label").trim().toLowerCase();
                        }

                        ModifierGroupType modifierGroupType = ModifierGroupType.OPTIONS;
                        if (code.equals("sizes") || type.equals("sizes") || label.equals("sizes")) {
                            modifierGroupType = ModifierGroupType.SIZES;
                        }
                        ModifierGroupDb modifierGroupDb = new ModifierGroupDb();
                        String modifierGroupId = String.valueOf(modifierGroupObject.getLong("id"));
                        modifierGroupDb.itemGroupId = modifierGroupId;
                        modifierGroupDb.id = modifierGroupId;
                        modifierGroupDb.applicationId = applicationId;
                        modifierGroupDb.type = modifierGroupType.getValue();
                        modifierGroupDb.data = modifierGroupObject.toString();
                        appDatabase.modifierGroupDao().insertAll(modifierGroupDb);
                    }

                    JSONArray modifiersArray = new JSONArray();
                    if (included.has("modifiers") && !included.isNull("modifiers")) {
                        modifiersArray = included.getJSONArray("modifiers");
                    }
                    appDatabase.modifierItemDao().deleteByApplication(applicationId);
                    for (int index = 0; index < modifiersArray.length(); index++) {
                        JSONObject modifierItemObject = modifiersArray.getJSONObject(index);
                        String modifierItemId = String.valueOf(modifierItemObject.getLong("id"));
                        ModifierItemDb modifierItemDb = new ModifierItemDb();
                        modifierItemDb.applicationId = applicationId;
                        modifierItemDb.itemGroupModifierId = modifierItemId;
                        modifierItemDb.id = modifierItemId;
                        modifierItemDb.data = modifierItemObject.toString();
                        appDatabase.modifierItemDao().insertAll(modifierItemDb);
                    }

                    ArrayList<MenuItemDb> menuItemDbs = new ArrayList<>();

                    for (int index = 0; index < dataArray.length(); index++) {

                        JSONObject menuCategoryItemObject = dataArray.getJSONObject(index);
                        JSONObject menuCategoryItemAttributesObject = menuCategoryItemObject.getJSONObject("attributes");
                        JSONObject menuCategoryObject = menuCategoryItemAttributesObject.getJSONObject("category");
                        JSONArray menuItemsArray = menuCategoryItemAttributesObject.getJSONArray("items");

                        for (int menuItemIndex = 0; menuItemIndex < menuItemsArray.length(); menuItemIndex++) {
                            JSONObject menuItemObject = menuItemsArray.getJSONObject(menuItemIndex);
                            MenuItemDb menuItemDb = new MenuItemDb();
                            String menuItemId = String.valueOf(menuItemObject.getLong("id"));
                            menuItemDb.id = menuItemId;
                            menuItemDb.applicationId = applicationId;
                            menuItemDb.name = menuItemObject.getJSONObject("name").getString(LocaleUtil.getInstance().getLanguage());
                            menuItemDb.menuCategoryId = String.valueOf(menuCategoryObject.getLong("id"));
                            String menuItemDbData = menuItemObject.toString();
                            menuItemDb.data = menuItemDbData;
                            //TODO: Use file cache only for Lorenzo for now
                            if (BuildUtil.soloAppKey().equalsIgnoreCase("7r5YQwmSgZCtM0ux")) {
                                splashView.saveToCache(menuItemId, menuItemDbData);
                            }
                            menuItemDbs.add(menuItemDb);
                        }

                    }

                    try {
                        appDatabase.menuItemDao().deleteByApplication(applicationId);
                        appDatabase.menuItemDao().insertAll(menuItemDbs.toArray(new MenuItemDb[menuItemDbs.size()]));
                    } catch (ArrayIndexOutOfBoundsException e) {
                    }

                    CacheUtil.getInstance().setMenuCached(new Date().getTime());
                    if (BuildConfig.MULTI_COUNTRY_ENABLED) {
                        String countryApplicationId = BuildConfig.APPLICATION_ID;
                        SupportedCountry country = CacheUtil.getInstance().getCountry();
                        if (country != null) {
                            countryApplicationId = country.applicationId;
                        }
                        CacheUtil.getInstance().setCountryCachedTime(countryApplicationId, new Date().getTime());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }

    @Override
    public void proceed() {

        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                AppDatabase appDatabase = AppDatabase.getInstance();
                appDatabase.storeDao().deleteAll();
                appDatabase.facilityDao().deleteAll();
            }
        });

        CacheUtil.getInstance().setUpsell(true);
        CacheUtil.getInstance().setDifferentMenuId("");
        if (!CacheUtil.getInstance().isSeparatedMenus() || CacheUtil.getInstance().getCartQuantity() == 0) {
            CacheUtil.getInstance().setOrderType(OrderType.NONE);
            CacheUtil.getInstance().setOrderStore(null);
            CacheUtil.getInstance().setCurbsideCar(null);
            CacheUtil.getInstance().setOrderAddress(null);
            CacheUtil.getInstance().setOrderDeliveryAddress(null);
            CacheUtil.getInstance().setOrderDeliveryLatitude(null);
            CacheUtil.getInstance().setOrderDeliveryLongitude(null);
            CacheUtil.getInstance().setOrderDeliveryTelephone(null);
            CacheUtil.getInstance().setOrderDeliveryLabel(null);
            CacheUtil.getInstance().setOrderDeliveryInstructions(null);
        }
        CacheUtil.getInstance().setOrderPayment(PaymentType.NONE);
        CacheUtil.getInstance().setCouponCode(null);
        CacheUtil.getInstance().setCouponName(null);
        CacheUtil.getInstance().setCouponMenuItem(null);
        CacheUtil.getInstance().setCouponToAdd(null);
        CacheUtil.getInstance().setCampaign(null);
        CacheUtil.getInstance().setDigitalCoupons("");
        CacheUtil.getInstance().setCampaignPromotion(null);
        CacheUtil.getInstance().setCampaignPromotionMenuItem(null);
        CacheUtil.getInstance().setCampaignToAdd(null);
        CacheUtil.getInstance().setOrderRatings(true);
        CacheUtil.getInstance().setPreselectedBranch(false);
        CacheUtil.getInstance().setLastOrderId(null);

        getMenuCdn();

        proceedNext();

    }

    public void getLoyalty() {
        CustomersApiCall.getInstance().getLoyaltyCustomerV2(AuthUtil.getInstance().getCustomer().getId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    JSONObject data = object.getJSONObject("data");
                    if (data.getJSONObject("loyalty_profile").optJSONObject("current_tier") == null || data.getJSONObject("loyalty_profile").get("current_tier") instanceof JSONObject) {
                        LoyaltyV2 loyalty = GsonHelper.gsonFinal().fromJson(data.toString(), LoyaltyV2.class);
                        if (loyalty != null && loyalty.getAttributes().getLoyaltyTier() != null && !TextUtils.isEmpty(loyalty.getAttributes().getLoyaltyTier().getImageUrl())) {
                            splashView.preloadImage(loyalty.getAttributes().getLoyaltyTier().getImageUrl(), RequestOptions.centerInsideTransform());
                        }
                    }
                    LoyaltyV2 loyalty = GsonHelper.gsonFinal().fromJson(data.toString(), LoyaltyV2.class);
                    AuthUtil.getInstance().setCustomerLoyalty(loyalty);
                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
            }
        });
    }

    public void changeLanguage() {
        CacheUtil.getInstance().setLanguage(true);
        if (AuthUtil.getInstance().isSignedIn() && !AuthUtil.getInstance().getDeviceId().isEmpty()) {
            UtilitiesApiCall.getInstance().changeLanguage(AuthUtil.getInstance().getDeviceId(), LocaleUtil.getInstance().getLanguage(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object obj) {
                    splashView.proceed(CacheUtil.getInstance().getLanguage(), CacheUtil.getInstance().getOnboarding(), CacheUtil.getInstance().getApplication().getAttributes().getForceLocation() != 0);
                }

                @Override
                public void onError(Object error) {
                    splashView.proceed(CacheUtil.getInstance().getLanguage(), CacheUtil.getInstance().getOnboarding(), CacheUtil.getInstance().getApplication().getAttributes().getForceLocation() != 0);
                    AppLogger.printRetrofitError(error);
                }
            });

        } else if (!AuthUtil.getInstance().getFirebase().isEmpty()) {
            String uniqueID = AuthUtil.getInstance().getUniqueId();



            if (uniqueID.isEmpty()) {
                uniqueID = UUID.randomUUID().toString();
                AuthUtil.getInstance().setUniqueId(uniqueID);
            }

            String customerId = null;
            String customerMobileNumber = null;
            if (AuthUtil.getInstance().isSignedIn()) {
                customerId = AuthUtil.getInstance().getCustomer().getId();
                String key = CacheUtil.getInstance().getConceptKey();
                if (key.equalsIgnoreCase("CQcFAwgLDA0")) {
                    customerMobileNumber = CacheUtil.getInstance().getConcept().getDialingCode() + AuthUtil.getInstance().getCustomer().getAttributes().getMobile().substring(1);
                } else {
                    customerMobileNumber = AuthUtil.getInstance().getCustomer().getAttributes().getMobile();
                }
            }

            UtilitiesApiCall.getInstance().postIdentify(AuthUtil.getInstance().getFirebase(), uniqueID, customerMobileNumber, customerId, new ApiRequestListener() {
                @Override
                public void onSuccess(Object object) {
                    DeviceUtil.save(object.toString());
                    if (AuthUtil.getInstance().isSignedIn() && !AuthUtil.getInstance().getDeviceId().isEmpty()) {
                        UtilitiesApiCall.getInstance().changeLanguage(AuthUtil.getInstance().getDeviceId(), LocaleUtil.getInstance().getLanguage(), new ApiRequestListener() {
                            @Override
                            public void onSuccess(Object obj) {
                                DeviceUtil.save(obj.toString());
                                splashView.proceed(CacheUtil.getInstance().getLanguage(), CacheUtil.getInstance().getOnboarding(), CacheUtil.getInstance().getApplication().getAttributes().getForceLocation() != 0);
                            }

                            @Override
                            public void onError(Object error) {
                                splashView.proceed(CacheUtil.getInstance().getLanguage(), CacheUtil.getInstance().getOnboarding(), CacheUtil.getInstance().getApplication().getAttributes().getForceLocation() != 0);
                                AppLogger.printRetrofitError(error);
                            }
                        });
                    } else {
                        splashView.proceed(CacheUtil.getInstance().getLanguage(), CacheUtil.getInstance().getOnboarding(), CacheUtil.getInstance().getApplication().getAttributes().getForceLocation() != 0);
                    }
                }

                @Override
                public void onError(Object error) {
                    splashView.proceed(CacheUtil.getInstance().getLanguage(), CacheUtil.getInstance().getOnboarding(), CacheUtil.getInstance().getApplication().getAttributes().getForceLocation() != 0);
                    AppLogger.printRetrofitError(error);
                }
            });

        }

    }

    @Override
    public void removeMukafaaRedemption() {
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

    @Override
    public void getLoyaltyTiers() {
        CustomersApiCall.getInstance().getLoyaltyTiers(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    JSONArray tiersArray = object.getJSONArray("data");
                    Type type = new TypeToken<List<LoyaltyTierV2>>() {}.getType();
                    List<LoyaltyTierV2> tiers = GsonHelper.gsonFinal().fromJson(tiersArray.toString(), type);
                    if (tiers != null && !tiers.isEmpty()) {
                        CacheUtil.getInstance().setLoyaltyTiers(new ArrayList<>(tiers));
                        for (LoyaltyTierV2 tier : tiers) {
                            splashView.preloadImage(tier.getImageUrl(), RequestOptions.noTransformation());
                        }
                    }
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void onError(Object o) {
            }
        });

    }

    public void proceedNext() {
        boolean forceLocation = false;
        if (CacheUtil.getInstance().getApplication().getAttributes().getForceLocation() != 0) {
            forceLocation = true;
        }

        if (CacheUtil.getInstance().isSeparatedMenus() && CacheUtil.getInstance().getCartQuantity() == 0) {
            forceLocation = false;
        }

        String primaryLanguage = CacheUtil.getInstance().getConcept().getPrimaryLanguage();
        String secondaryLanguage = CacheUtil.getInstance().getConcept().getSecondaryLanguage();

        if (secondaryLanguage == null || secondaryLanguage.isEmpty()) {
            boolean language = CacheUtil.getInstance().getLanguage();
            LocaleUtil.getInstance().setLanguage(primaryLanguage.split("-")[0]);
            SoloAuth.getInstance().setLanguage(primaryLanguage);
            CacheUtil.getInstance().setLanguage(true);
            if (!language) {
                changeLanguage();
            } else {
                splashView.proceed(CacheUtil.getInstance().getLanguage(), CacheUtil.getInstance().getOnboarding(), forceLocation);
            }
        } else {
            splashView.proceed(CacheUtil.getInstance().getLanguage(), CacheUtil.getInstance().getOnboarding(), forceLocation);
        }
    }


    public String inputStreamToString(InputStream inputStream) {
        try {
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes, 0, bytes.length);
            String json = new String(bytes);
            return json;
        } catch (IOException e) {
            return null;
        }

    }



}
