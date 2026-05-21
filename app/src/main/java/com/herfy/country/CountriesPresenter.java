package com.herfy.country;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.bumptech.glide.request.RequestOptions;
import com.google.gson.reflect.TypeToken;
import com.herfy.database.AppDatabase;
import com.herfy.database.entities.*;
import com.herfy.database.responses.MenuCategoryItemResponse;
import com.herfy.model.SupportedCountry;
import com.herfy.util.AppLogger;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.StringUtil;
import com.herfy.utils.menu.MenuUtil;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.ApiRequestWithHeadersListener;
import com.skylinedynamics.solosdk.api.calls.ApplicationsApiCall;
import com.skylinedynamics.solosdk.api.calls.CmsApiCall;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.calls.IntegrationsApiCall;
import com.skylinedynamics.solosdk.api.calls.UtilitiesApiCall;
import com.skylinedynamics.solosdk.api.models.objects.Application;
import com.skylinedynamics.solosdk.api.models.objects.Concept;
import com.skylinedynamics.solosdk.api.models.objects.Integration;
import com.skylinedynamics.solosdk.api.models.objects.Loyalty;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupType;
import com.skylinedynamics.solosdk.api.models.objects.Slider;
import com.skylinedynamics.solosdk.util.GsonHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.Headers;

public class CountriesPresenter implements CountriesContract.Presenter {

    private int CACHED_MINUTES_FIVE = 300000;
    private SupportedCountry oldCountry;
    private long CURRENT_TIME_WHEN_FETCHED = 0;
    private long CURRENT_TIME_STORED = 0;
    private CountriesContract.View countriesView;

    public CountriesPresenter(CountriesContract.View countriesView) {
        this.countriesView = countriesView;
        this.countriesView.setPresenter(this);
    }

    @Override
    public void start() {

    }

    public void cachedApplicationChecker(String objectString) {
        if (objectString.isEmpty()) {
            callApplication();
        } else {
            applicationFetch(objectString);
        }
    }

    public void applicationFetch(String objectString) {
        try {
            JSONObject jsonObject = new JSONObject(objectString);
            JSONObject data = jsonObject.getJSONObject("data");
            Application application = GsonHelper.gsonFinal().fromJson(data.toString(), new TypeToken<Application>() {
            }.getType());
            CacheUtil.getInstance().setApplication(application);
            getIntegrations();
            if (AuthUtil.getInstance().isSignedIn()) {
                getLoyalty();
            }
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
                        } else if (included.getJSONObject(i).optString("type").equalsIgnoreCase("popup")) {
                            JSONObject popupObj = included.getJSONObject(i);
                            String label = popupObj.optJSONObject("attributes") != null ? popupObj.getJSONObject("attributes").optString("label") : "";
                            if (label.equalsIgnoreCase("Capillary") || label.toLowerCase().contains("capillary")) {
                                CacheUtil.getInstance().setCapillaryPopup(popupObj);
                            }
                            CacheUtil.getInstance().setPopup(popupObj);
                        }
                    }
                } else {
                    countriesView.proceedRestart();
                }
            } else {
                countriesView.proceedRestart();
            }

            getSliders();

        } catch (JSONException e) {
            e.printStackTrace();
            countriesView.proceedRestart();
        }
    }

    public void callApplication() {
        ApplicationsApiCall.getInstance().getApplication(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                SupportedCountry country = CacheUtil.getInstance().getCountry();
                CacheUtil.getInstance().setCountryApplication(country.applicationId, o.toString());
                applicationFetch(o.toString());
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o.toString());
                countriesView.proceedRestart();
            }
        });
    }

    @Override
    public boolean isCacheExpired(@Nullable SupportedCountry country) {
        if(country != null) {
            long currentTime = System.currentTimeMillis();
            long cachedTime = CacheUtil.getInstance().getCountryCachedTime(country.applicationId);
            return (currentTime - cachedTime) >= CACHED_MINUTES_FIVE;
        }
        return true;
    }

    @Override
    public void getApplication() {
        SupportedCountry country = CacheUtil.getInstance().getCountry();
        CURRENT_TIME_WHEN_FETCHED = System.currentTimeMillis();
        CURRENT_TIME_STORED = CacheUtil.getInstance().getCountryCachedTime(country.applicationId);
        if ((CURRENT_TIME_WHEN_FETCHED - CURRENT_TIME_STORED) < CACHED_MINUTES_FIVE) {
            cachedApplicationChecker(CacheUtil.getInstance().getCountryApplication(country.applicationId));
        } else {
            callApplication();
        }
    }

    @Override
    public void setOldCountry(SupportedCountry country) {
        oldCountry = country;
    }

    public void cachedSlidersChecker(String objectString) {
        if (objectString.isEmpty()) {
            callSliders();
        } else {
            slidersFetch(objectString);
        }
    }

    private void slidersFetch(String objectString) {
        try {
            JSONObject object = new JSONObject(objectString);
            if (object.has("data") && !object.isNull("data")) {
                JSONArray data = object.getJSONArray("data");
                for (int i = 0; i < data.length(); i++) {
                    Slider slider = Slider.parse(data.getJSONObject(i));
                    if (slider.getLabel().equalsIgnoreCase("Mid Banner")) {
                        CacheUtil.getInstance().setMidSliderId(slider.getId());
                        break;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            CacheUtil.getInstance().setMidSliderId("");
        }
        proceed();
    }

    public void callSliders() {
        CmsApiCall.getInstance().getSliders(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                SupportedCountry country = CacheUtil.getInstance().getCountry();
                CacheUtil.getInstance().setCountrySlider(country.applicationId, o.toString());
                slidersFetch(o.toString());
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o.toString());
                CacheUtil.getInstance().setMidSliderId("");
                proceed();
            }
        });
    }

    public void getSliders() {
        SupportedCountry country = CacheUtil.getInstance().getCountry();
        if ((CURRENT_TIME_WHEN_FETCHED - CURRENT_TIME_STORED) < CACHED_MINUTES_FIVE) {
            cachedSlidersChecker(CacheUtil.getInstance().getCountrySlider(country.applicationId));
        } else {
            callSliders();
        }
    }

    public void proceed() {
        CacheUtil.getInstance().clearOrder();
        CacheUtil.getInstance().clearMukafaa();
        AppDatabase appDatabase = AppDatabase.getInstance();
        String applicationId = CacheUtil.getInstance().getApplication().getId();
        if (!appDatabase.menuCategoryEntityDao().hasByApplication(applicationId) || (CURRENT_TIME_WHEN_FETCHED - CURRENT_TIME_STORED) >= CACHED_MINUTES_FIVE) {
            getMenuCdn();
        } else {
            countriesView.proceedRestart();
        }
    }

    public void cachedMenuChecker(String objectString) {
        if (objectString.isEmpty()) {
            callMenu();
        } else {
            menuFetch(objectString);
        }
    }

    private void menuFetch(String objectString) {
        try {
            JSONObject object = new JSONObject(objectString);
            parseMenu(object);
        } catch (JSONException e) {
            e.printStackTrace();
            countriesView.proceedRestart();
        }
    }

    public void callMenu() {
        String url = MenuUtil.getMenuCdnUrl();
        UtilitiesApiCall.getInstance().getMenuCategoriesWithHeaders(url, new ApiRequestWithHeadersListener() {
            @Override
            public void onSuccess(Headers headers, Object o) {
                if(headers.names().contains("last-modified")) {
                    CacheUtil.getInstance().setMenuCdnLastModified(headers.get("last-modified"));
                }
                SupportedCountry country = CacheUtil.getInstance().getCountry();
                CacheUtil.getInstance().setCountryMenu(country.applicationId, o.toString());
                menuFetch(o.toString());
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o.toString());
                countriesView.proceedRestart();
            }
        });
    }

    public void getMenuCdn() {
        SupportedCountry country = CacheUtil.getInstance().getCountry();
        cachedMenuChecker(CacheUtil.getInstance().getCountryMenu(country.applicationId));
    }

    public void parseMenu(JSONObject data) {
        Executor executor = Executors.newSingleThreadExecutor();
        AppDatabase appDatabase = AppDatabase.getInstance();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {

                    String applicationId = CacheUtil.getInstance().getApplication().getId();

                    appDatabase.menuCategoryDao().deleteByApplication(applicationId);
                    appDatabase.menuItemDao().deleteByApplication(applicationId);
                    appDatabase.modifierGroupDao().deleteByApplication(applicationId);
                    appDatabase.modifierItemDao().deleteByApplication(applicationId);
                    appDatabase.ingredientDao().deleteByApplication(applicationId);
                    appDatabase.allergenDao().deleteByApplication(applicationId);
                    appDatabase.menuCategoryEntityDao().deleteByApplication(applicationId);
                    appDatabase.menuItemEntityDao().deleteByApplication(applicationId);

                    JSONArray dataArray = data.getJSONArray("data");
                    List<MenuCategoryItemResponse> menuCategoryItemsResponse = GsonHelper.gsonFinal().fromJson(dataArray.toString(), new TypeToken<ArrayList<MenuCategoryItemResponse>>() {
                    }.getType());

                    JSONObject included = data.getJSONObject("included");

                    JSONArray allergensArray = new JSONArray();
                    if (included.has("allergens") && !included.isNull("allergens")) {
                        allergensArray = included.getJSONArray("allergens");
                    }
                    for (int index = 0; index < allergensArray.length(); index++) {
                        JSONObject allergenObject = allergensArray.getJSONObject(index);
                        AllergenDb allergenDb = new AllergenDb();
                        allergenDb.applicationId = applicationId;
                        allergenDb.itemAllergenId = String.valueOf(allergenObject.getLong("id"));
                        allergenDb.id = String.valueOf(allergenObject.getLong("id"));
                        allergenDb.data = allergenObject.toString();
                        appDatabase.allergenDao().insertAll(allergenDb);
                    }

                    JSONArray ingredientsArray = new JSONArray();
                    if (included.has("ingredients") && !included.isNull("ingredients")) {
                        ingredientsArray = included.getJSONArray("ingredients");
                    }
                    for (int index = 0; index < ingredientsArray.length(); index++) {
                        JSONObject ingredientObject = ingredientsArray.getJSONObject(index);
                        IngredientDb ingredientDb = new IngredientDb();
                        ingredientDb.applicationId = applicationId;
                        ingredientDb.itemIngredientId = String.valueOf(ingredientObject.getLong("id"));
                        ingredientDb.id = String.valueOf(ingredientObject.getLong("id"));
                        ingredientDb.data = ingredientObject.toString();
                        appDatabase.ingredientDao().insertAll(ingredientDb);
                    }

                    JSONArray modifierGroupsArray = new JSONArray();
                    if (included.has("modifierGroups") && !included.isNull("modifierGroups")) {
                        modifierGroupsArray = included.getJSONArray("modifierGroups");
                    }
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
                    for (int index = 0; index < modifiersArray.length(); index++) {
                        JSONObject modifierItemObject = modifiersArray.getJSONObject(index);
                        ModifierItemDb modifierItemDb = new ModifierItemDb();
                        modifierItemDb.applicationId = applicationId;
                        modifierItemDb.itemGroupModifierId = String.valueOf(modifierItemObject.getLong("id"));
                        modifierItemDb.id = String.valueOf(modifierItemObject.getLong("id"));
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
                            menuItemDb.id = String.valueOf(menuItemObject.getLong("id"));
                            menuItemDb.applicationId = applicationId;
                            menuItemDb.name = menuItemObject.getJSONObject("name").getString(LocaleUtil.getInstance().getLanguage());
                            menuItemDb.menuCategoryId = String.valueOf(menuCategoryObject.getLong("id"));
                            menuItemDb.data = menuItemObject.toString();
                            menuItemDbs.add(menuItemDb);
                        }

                    }

                    try {
                        appDatabase.menuItemDao().insertAll(menuItemDbs.toArray(new MenuItemDb[menuItemDbs.size()]));
                    } catch (ArrayIndexOutOfBoundsException e) {
                    }

                    for (MenuCategoryItemResponse menuCategoryItem : menuCategoryItemsResponse) {

                        MenuCategoryEntity menuCategoryEntity = menuCategoryItem.getAttributes().getMenuCategory();
                        menuCategoryEntity.setApplicationId(applicationId);
                        if (!MenuUtil.isExcluded(menuCategoryEntity.getExcludedApps())) {
                            appDatabase.menuCategoryEntityDao().insertAll(menuCategoryEntity);
                            String categoryImageUrl = menuCategoryItem.getAttributes().getMenuCategory().getImage();
                            if (categoryImageUrl == null || categoryImageUrl.equalsIgnoreCase("null") || categoryImageUrl.isEmpty() || categoryImageUrl.toLowerCase().contains("default-image.png")) {
                                categoryImageUrl = Values.DEFAULT_IMAGE_URL;
                            } else {
                                categoryImageUrl = StringUtil.getCloudflareUrl(Values.MENU_IMAGE_SIZE, Values.MENU_IMAGE_SIZE, categoryImageUrl);
                            }
                            countriesView.preloadImage(categoryImageUrl, RequestOptions.centerInsideTransform());

                            for (MenuItemEntity menuItem : menuCategoryItem.getAttributes().getMenuItems()) {
                                menuItem.setApplicationId(applicationId);
                                menuItem.setMenuCategoryId(menuCategoryItem.getAttributes().getMenuCategory().getId());
                                if (!MenuUtil.isExcluded(menuItem.getExcludedApps())) {
                                    appDatabase.menuItemEntityDao().insertAll(menuItem);
                                    String itemImageUrl = menuItem.getImage();
                                    if (itemImageUrl == null || itemImageUrl.equalsIgnoreCase("null") || itemImageUrl.isEmpty() || itemImageUrl.toLowerCase().contains("default-image.png")) {
                                        itemImageUrl = Values.DEFAULT_IMAGE_URL;
                                    } else {
                                        itemImageUrl = StringUtil.getCloudflareUrl(Values.MENU_IMAGE_SIZE, Values.MENU_IMAGE_SIZE, itemImageUrl);
                                    }
                                    countriesView.preloadImage(itemImageUrl, RequestOptions.centerCropTransform());
                                }
                            }
                        }
                    }

                    CacheUtil.getInstance().setMenuCached(new Date().getTime());
                    SupportedCountry country = CacheUtil.getInstance().getCountry();
                    CacheUtil.getInstance().setCountryCachedTime(country.applicationId, System.currentTimeMillis());
                    countriesView.proceedRestart();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }

    public void getIntegrations() {
        if(AuthUtil.getInstance().isSignedIn()) {
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

    public void getLoyalty() {
        CustomersApiCall.getInstance().getCustomerLoyalty(AuthUtil.getInstance().getCustomer().getId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    JSONObject data = object.getJSONObject("data");
                    if (data.getJSONObject("attributes").optJSONObject("tier") == null || data.getJSONObject("attributes").get("tier") instanceof JSONObject) {
                        Loyalty loyalty = GsonHelper.gsonFinal().fromJson(data.toString(), Loyalty.class);
                        if (loyalty != null && loyalty.getAttributes().getLoyaltyTier() != null && !TextUtils.isEmpty(loyalty.getAttributes().getLoyaltyTier().getImageUrl())) {
                            countriesView.preloadImage(loyalty.getAttributes().getLoyaltyTier().getImageUrl(), RequestOptions.centerInsideTransform());
                        }
                    }
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
}
