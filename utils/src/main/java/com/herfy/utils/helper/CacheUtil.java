package com.herfy.utils.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.herfy.model.CampaignToAdd;
import com.herfy.model.CouponToAdd;
import com.herfy.model.Popup;
import com.herfy.model.SupportedCountry;
import com.herfy.model.SupportedCountryResponse;
import com.herfy.utils.BuildConfig;
import com.herfy.utils.analytics.MixpanelUtil;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.Application;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.Concept;
import com.skylinedynamics.solosdk.api.models.objects.ConsumerData;
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard;
import com.skylinedynamics.solosdk.api.models.objects.Favorite;
import com.skylinedynamics.solosdk.api.models.objects.Ingredient;
import com.skylinedynamics.solosdk.api.models.objects.Integration;
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.PaymentType;
import com.skylinedynamics.solosdk.api.models.objects.Promotion;
import com.skylinedynamics.solosdk.api.models.objects.Slide;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfig;
import com.skylinedynamics.solosdk.api.models.objects.area.City;
import com.skylinedynamics.solosdk.api.models.objects.area.CityList;
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarColor;
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarMaker;
import com.skylinedynamics.solosdk.api.models.objects.curbside.CustomerCar;
import com.skylinedynamics.solosdk.util.GsonHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CacheUtil {

    private static CacheUtil instance;

    private SharedPreferences cacheSharedPreferences;


    private static final String CACHE = "Cache";

    private static final String LANGUAGE = "Language";
    private static final String ONBOARDING = "Onboarding";
    private static final String TRANSLATIONS = "Translations";
    private static final String APPLICATION = "Application";
    private static final String INTEGRATION = "Integration";
    private static final String COLOR_MAIN = "ColorMain";
    private static final String SLIDER_ID = "SliderId";
    private static final String MID_SLIDER_ID = "MidSliderId";
    private static final String SLIDES = "Slides";

    private static final String CITIES = "CitiesList";
    private static final String CUSTOMER_CARDS = "CustomerCards";

    private static final String IS_FREQUENTLY_QUANTITY_SHOW = "FrequentlyQuantity";
    private static final String FREQUENTLY_QUANTITY_ID = "FrequentlyID";
    private static final String CUSTOMER_CARD = "CustomerCard";
    private static final String MID_SLIDES = "MidSlides";
    private static final String MENU_ID = "MenuId";
    private static final String CONCEPT_KEY = "Concept_Key";
    private static final String APPLICATION_KEY = "Application_Key";
    private static final String MODE_KEY = "MODE_KEY";
    private static final String CONCEPT = "Concept";
    private static final String MENU_CACHED = "MenuCached";
    private static final String MENU_CATEGORIES = "MenuCategories";
    private static final String PROMOTIONS_SET = "PromotionsSet";
    private static final String SLIDER_MENU_CATEGORY = "SliderMenuCategory";
    private static final String CART_MENU_ITEMS = "CartMenuItems";
    private static final String CART_NOTES = "CartNotes";
    private static final String UPSELL = "Upsell";
    private static final String ORDER_TYPE = "OrderType";
    private static final String ORDER_LATITUDE = "OrderLatitude";
    private static final String ORDER_LONGITUDE = "OrderLongitude";
    private static final String ORDER_STORE = "OrderStore";
    private static final String ORDER_ADDRESS = "OrderAddress";
    private static final String ORDER_DELIVERY_LATITUDE = "OrderDeliveryLatitude";
    private static final String ORDER_DELIVERY_LONGITUDE = "OrderDeliveryLongitude";
    private static final String ORDER_DELIVERY_ADDRESS = "OrderDeliveryAddress";
    private static final String ORDER_DELIVERY_TELEPHONE = "OrderDeliveryTelephone";
    private static final String ORDER_DELIVERY_LABEL = "OrderDeliveryLabel";
    private static final String ORDER_DELIVERY_INSTRUCTIONS = "OrderDeliveryInstructions";
    private static final String ORDER_PAYMENT = "OrderPayment";
    private static final String NOTIFICATION_ID = "NotificationId";
    private static final String FAVORITES = "Favorites";
    private static final String FAVORITE_MENU_CATEGORY = "FavoriteMenuCategory";
    private static final String FAVORITE_MENU_ITEM = "FavoriteMenuItem";
    private static final String COUPON_CODE = "CouponCode";
    private static final String COUPON_NAME = "CouponName";
    private static final String COUPON_MENU_ITEM = "CouponMenuItem";
    private static final String COUPON_TO_ADD = "CouponToAdd";
    private static final String CART_EXPIRED = "CartExpired";
    private static final String CAMPAIGN = "CampaignPromo";
    private static final String CAMPAIGN_PROMOTION = "CampaignPromotion";
    private static final String CURBSIDE_CAR = "CurbsideCar";
    private static final String CAMPAIGN_PROMOTION_MENU_ITEM = "CampaignPromotionMenuItem";
    private static final String CAMPAIGN_TO_ADD = "CampaignToAdd";
    private static final String DIGITAL_COUPONS = "DigitalCoupons";
    private static final String ORDER_RATINGS = "OrderRatings";
    private static final String LOYALTY_POINTS_EARNABLE = "LoyaltyPointsEarnable";
    private static final int MODE_QUICK = 1;
    private static final int MODE_SUBS = 2;
    private int loyaltyPoints;
    private static final String LATEST_ORDER = "LatestOrder";
    private static final String LATEST_MESSAGE = "LatestMessage";
    private static final String LATEST_DRIVER_NUMBER = "LatestDriverNumber";
    private static final String DINE_IN_TABLE_NUMBER = "dineInTableNumber";
    private static final String MUKAFAA_TRANSACTION_ID = "MukafaaTransactionId";
    private static final String MUKAFAA_TRANSACTION_AMOUNT = "MukafaaTransactionAmount";
    private static final String APP_REVIEW = "AppReview";
    private static final String SIGNIN_USERNAME = "SigninUsername";
    private static final String PRESELECTED_BRANCH = "PreselectedBranch";
    private static final String IS_AUTHCREDENTIAL_MIGRATED = "isAuthCredentialMigrated";
    private static final String IS_CONTACT_US_ENABLED = "isContactUsEnabled";
    private static final String MENU_CDN_LAST_MODIFIED = "MenuCdnLastModified";
    private static final String RELOAD_DIFFERENT_MENU = "reload_different_menu";
    private static final String DIFFERENT_MENU_ID = "different_menu_id";
    private static final String CONSUMER_DATA = "consumer_data";
    private static final String TOTAL_ACCUMULATED_POINTS = "totalAccumulatedPoints";
    private static final String POINTS_UNTIL_NEXT_TIER = "pointsUntilNextTier";
    private static final String STARTING_POINTS_OF_NEXT_TIER = "startingPointsOfNextTier";

    private static final String CART_QUANTITY = "CartQuantity";
    private String COUNTRY = "Country";
    private String SUPPORT_CARD_TYPES = "SupportCardTypes";

    private String LAST_ORDER_ID = "lastOrderId";

    private static final String VEHICLE_BRANDS = "VehicleBrands";

    private static final String MAP_AREA = "MapArea";
    private static final String VEHICLE_COLOR = "VehicleColor";

    private static final String FAVOURITE = "FAVOURITE";
    private static final String PROMISED_TIME = "promised_time";

    private CacheUtil(Context context) {

        cacheSharedPreferences = context.getSharedPreferences(CACHE, Context.MODE_PRIVATE);

    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new CacheUtil(context);
        }
    }

    public static synchronized CacheUtil getInstance() {
        return instance;
    }

    public SharedPreferences getSharedPrefs() {
        return cacheSharedPreferences;
    }

    public boolean getLanguage() {
        return cacheSharedPreferences.getBoolean(LANGUAGE, false);
    }

    public void setLanguage(boolean language) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putBoolean(LANGUAGE, language);
        editor.apply();

    }

    public boolean getOnboarding() {
        return cacheSharedPreferences.getBoolean(ONBOARDING, false);
    }

    public void setOnboarding(boolean onboarding) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putBoolean(ONBOARDING, onboarding);
        editor.apply();

    }

    public boolean ifHasTranslations() {
        String translations = cacheSharedPreferences.getString(TRANSLATIONS, null);
        return translations != null;
    }

    public void saveTranslations(String translations) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(TRANSLATIONS, translations);
        editor.apply();

    }

    public String getTranslations(String key) {

        String translations = cacheSharedPreferences.getString(TRANSLATIONS, null);

        if (translations != null) {
            try {
                JSONObject translationsObject = new JSONObject(translations);
                if (translationsObject.has(key)) {
                    JSONObject text = translationsObject.getJSONObject(key);
                    if (text != null) {
                        if (LocaleUtil.getInstance().isEnglish()) {
                            return text.getString("en_US");
                        } else if (LocaleUtil.getInstance().isFrench()) {
                            return text.getString("fr");
                        } else {
                            return text.getString("ar_SA");
                        }
                    }
                } else {
                    return key;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return key;

    }

    public String getArabicTranslations(String key, String defaultValue) {

        String translations = cacheSharedPreferences.getString(TRANSLATIONS, null);

        if (translations != null) {
            try {
                JSONObject translationsObject = new JSONObject(translations);
                if (translationsObject.has(key)) {
                    JSONObject text = translationsObject.getJSONObject(key);
                    if (text != null) {
                        return text.getString("ar_SA");
                    }
                } else {
                    return defaultValue;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return defaultValue;

    }

    public String getTranslations(String key, String defaultValue) {

        String translations = cacheSharedPreferences.getString(TRANSLATIONS, null);

        if (translations != null) {
            try {
                JSONObject translationsObject = new JSONObject(translations);
                if (translationsObject.has(key)) {
                    JSONObject text = translationsObject.getJSONObject(key);
                    if (text != null) {
                        if (LocaleUtil.getInstance().isEnglish()) {
                            return text.getString("en_US");
                        } else if (LocaleUtil.getInstance().isFrench()) {
                            return text.getString("fr");
                        } else {
                            return text.getString("ar_SA");
                        }
                    }
                } else {
                    return defaultValue;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return defaultValue;

    }

    public void setApplication(Application application) {

        String applicationValue = new Gson().toJson(application);

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(APPLICATION, applicationValue);
        editor.apply();

    }

    public Application getApplication() {

        String applicationValue = cacheSharedPreferences.getString(APPLICATION, null);

        if (applicationValue != null) {
            return GsonHelper.gsonFinal().fromJson(applicationValue, new TypeToken<Application>() {
            }.getType());
        }

        return null;
    }

    public void setIntegration(Integration integration) {

        String integrationValue = new Gson().toJson(integration);

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(INTEGRATION, integrationValue);
        editor.apply();

    }

    public Integration getIntegration() {

        String integrationValue = cacheSharedPreferences.getString(INTEGRATION, null);

        if (integrationValue != null) {
            return GsonHelper.gsonFinal().fromJson(integrationValue, new TypeToken<Integration>() {
            }.getType());
        }

        return null;
    }

    public void setSliderId(String key, String id) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(key, id);
        editor.apply();
    }

    public String getSliderId(String key) {
        return cacheSharedPreferences.getString(key, "");
    }

    public void setSliderId(String id) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(SLIDER_ID, id);
        editor.apply();
    }

    public String getSliderId() {
        return cacheSharedPreferences.getString(SLIDER_ID, "");
    }

    public void setMidSliderId(String id) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(MID_SLIDER_ID, id);
        editor.apply();
    }

    public String getMidSliderId() {
        return cacheSharedPreferences.getString(MID_SLIDER_ID, "");
    }

    public void setSlides(String slides) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(SLIDES, slides);
        editor.apply();
    }

    public ArrayList<Slide> getSlides() {
        String slides = cacheSharedPreferences.getString(SLIDES, null);
        if (slides != null) {
            return GsonHelper.gsonFinal().fromJson(slides, new TypeToken<ArrayList<Slide>>() {
            }.getType());
        }
        return new ArrayList<>();
    }

    public void setCities(String cities) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(CITIES, cities);
        editor.apply();
    }

    public List<City> getCities() {
        String cities = cacheSharedPreferences.getString(CITIES, null);
        if (cities != null) {
            return GsonHelper.gsonFinal().fromJson(cities, new TypeToken<List<City>>() {
            }.getType());
        }
        return new ArrayList<>();
    }

    public void setCustomerCards(String cards) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(CUSTOMER_CARDS, cards);
        editor.apply();
    }

    public void setFrequentlyQuantityShouldShow(boolean isQuantityShouldShow, String menuId) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putBoolean(IS_FREQUENTLY_QUANTITY_SHOW, isQuantityShouldShow);
        editor.putString(FREQUENTLY_QUANTITY_ID,menuId);
        editor.apply();
    }
    public Pair<Boolean, String> getFrequentlyQuantityShouldShow() {
        boolean isQuantityShouldShow = cacheSharedPreferences.getBoolean(IS_FREQUENTLY_QUANTITY_SHOW, false);
        String menuId = cacheSharedPreferences.getString(FREQUENTLY_QUANTITY_ID, "");
        return new Pair<>(isQuantityShouldShow, menuId);
    }
    public List<CustomerCard> getCustomerCards() {
        String cards = cacheSharedPreferences.getString(CUSTOMER_CARDS, null);
        if (cards != null) {
            return GsonHelper.gsonFinal().fromJson(cards, new TypeToken<List<CustomerCard>>() {
            }.getType());
        }
        return new ArrayList<>();
    }

    public void setCustomCard(String customCard) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(CUSTOMER_CARD, customCard);
        editor.apply();
    }

    public CustomerCard getCustomCard() {
        String customCardJson = cacheSharedPreferences.getString(CUSTOMER_CARD, null);
        if (customCardJson != null) {
            return GsonHelper
                    .gsonFinal()
                    .fromJson(customCardJson, CustomerCard.class);
        }
        return null;
    }

    public void setMidSlides(String slides) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(MID_SLIDES, slides);
        editor.apply();

    }

    public ArrayList<Slide> getMidSlides() {

        String slides = cacheSharedPreferences.getString(MID_SLIDES, null);

        if (slides != null) {
            return GsonHelper.gsonFinal().fromJson(slides, new TypeToken<ArrayList<Slide>>() {
            }.getType());
        }

        return new ArrayList<>();
    }

    public void setMenuId(String id) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(MENU_ID, id);
        editor.apply();
    }

    public String getMenuId() {
        return cacheSharedPreferences.getString(MENU_ID, "");
    }

    public void setUpMode(int mode) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putInt(MODE_KEY, mode);
        editor.apply();
    }

    public int getMode() {
        return cacheSharedPreferences.getInt(MODE_KEY, 0);
    }

    public void setApplicationKey(String key) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(APPLICATION_KEY, key);
        editor.apply();
    }

    public String getApplicationKey() {
        return cacheSharedPreferences.getString(APPLICATION_KEY, BuildConfig.SOLO_APP);

    }

    public void setConceptKey(String key) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(CONCEPT_KEY, key);
        editor.apply();
    }

    public String getConceptKey() {
        return cacheSharedPreferences.getString(CONCEPT_KEY, BuildConfig.SOLO_CONCEPT);

    }

    public void setDigitalCoupons(String digitalCoupons) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(DIGITAL_COUPONS, digitalCoupons);
        editor.apply();

    }

    public String getDigitalCoupons() {
        return cacheSharedPreferences.getString(DIGITAL_COUPONS, "");
    }

    public void setConcept(Concept concept) {

        String conceptValue = new Gson().toJson(concept);

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(CONCEPT, conceptValue);
        editor.apply();

    }

    public Concept getConcept() {

        String conceptValue = cacheSharedPreferences.getString(CONCEPT, null);

        if (conceptValue != null) {
            return GsonHelper.gsonFinal().fromJson(conceptValue, Concept.class);
        }

        return null;
    }

    public void setPopup(JSONObject popup) throws JSONException {
        Popup lastPopup = getPopup();
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();

        if (popup == null && lastPopup != null) {
            editor.putString("LAST_POPUP", new Gson().toJson(lastPopup));
        }

        if (popup == null) {
            editor.remove("POPUP");
            editor.apply();
            return;
        }

        editor.putString("POPUP", popup.getJSONObject("attributes").toString());
        editor.apply();
    }

    public Popup getPopup() {
        String value = cacheSharedPreferences.getString("POPUP", null);
        if (value != null) {
            Popup popup = GsonHelper.gsonFinal().fromJson(value, Popup.class);
            Popup lastPopup = GsonHelper.gsonFinal().fromJson(cacheSharedPreferences.getString("LAST_POPUP", "{}"), Popup.class);
            if (popup.equals(lastPopup)) {
                return null;
            }

            return popup;
        }

        return null;
    }

    public void setMenuCached(long menuCached) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putLong(MENU_CACHED, menuCached);
        editor.apply();
    }

    public long getMenuCached() {
        return cacheSharedPreferences.getLong(MENU_CACHED, 0L);
    }

    public void setColorMain(String colorMain) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(COLOR_MAIN, colorMain);
        editor.apply();
    }

    public String getColorMain() {
        //return cacheSharedPreferences.getString(COLOR_MAIN, "#F26224");
        return cacheSharedPreferences.getString(COLOR_MAIN, "#000000");
    }

    public void setMenuCategories(HashMap<String, Pair<MenuCategory, Set<String>>> menuCategories) {

        try {
            String menuCategoriesValue = new Gson().toJson(menuCategories);

            SharedPreferences.Editor editor = cacheSharedPreferences.edit();
            editor.putString(MENU_CATEGORIES, menuCategoriesValue);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public HashMap<String, Pair<MenuCategory, Set<String>>> getMenuCategories() {

        String menuCategories = cacheSharedPreferences.getString(MENU_CATEGORIES, null);


        try {
            if (menuCategories != null) {
                return GsonHelper.gsonFinal().fromJson(menuCategories, new TypeToken<HashMap<String, Pair<MenuCategory, Set<String>>>>() {
                }.getType());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        return new HashMap<>();
    }

    public void setSliderMenuCategory(String id) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(SLIDER_MENU_CATEGORY, id);
        editor.apply();
    }

    public String getSliderMenuCategory() {
        return cacheSharedPreferences.getString(SLIDER_MENU_CATEGORY, "");
    }


    public void setLastOrderId(String lastOrderId) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(LAST_ORDER_ID, lastOrderId);
        editor.apply();

    }

    public String getLastOrderId() {
        return cacheSharedPreferences.getString(LAST_ORDER_ID, "");
    }

    public void clearLocationOnly() {
        setOrderType(OrderType.NONE);
        setOrderLatitude(null);
        setOrderLongitude(null);
        setOrderStore(null);
        setCurbsideCar(null);
        setOrderAddress(null);
        setOrderDeliveryAddress(null);
        setOrderDeliveryLatitude(null);
        setOrderDeliveryLongitude(null);
        setOrderDeliveryTelephone(null);
        setOrderDeliveryLabel(null);
        setOrderDeliveryInstructions(null);
    }

    public void clearOrder() {
        setCart(new ArrayList<>());
        setCartNotes("");
        setUpsell(true);
        setOrderType(OrderType.NONE);
        setOrderLatitude(null);
        setOrderLongitude(null);
        setOrderStore(null);
        setOrderAddress(null);
        setCurbsideCar(null);
        setOrderDeliveryAddress(null);
        setOrderDeliveryLatitude(null);
        setOrderDeliveryLongitude(null);
        setOrderDeliveryTelephone(null);
        setOrderDeliveryLabel(null);
        setOrderDeliveryInstructions(null);
//        setOrderPayment(PaymentType.NONE);
        setCouponCode(null);
        setCouponName(null);
        setCouponMenuItem(null);
        setCouponToAdd(null);
        setCampaign(null);
        setDigitalCoupons("");
        setCampaignPromotion(null);
        setCampaignPromotionMenuItem(null);
        setCampaignToAdd(null);
        setCustomerCards(null);
        setCustomCard(null);
        setConsumerData(null);
        setLastOrderId(null);
    }

    public void setCart(ArrayList<MenuItem> menuItems) {

        String cartMenuItems = new Gson().toJson(menuItems);
        setCartQuantity(menuItems);
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(CART_MENU_ITEMS, cartMenuItems);
        editor.apply();

    }

    public void setCartQuantity(ArrayList<MenuItem> menuItems) {
        int quantity = 0;
        for (MenuItem menuItem : menuItems) {
            quantity += menuItem.getAttributes().getQuantity();
        }
        MenuItem couponMenuItem = getCouponMenuItem();
        Campaign campaign = getCampaign();
        Promotion promotion = getCampaignPromotion();
        MenuItem campaignPromotionMenuItem = getCampaignPromotionMenuItem();

        if (getCouponCode() != null && couponMenuItem != null) {
            quantity += couponMenuItem.getAttributes().getQuantity();
        }

        if (campaign != null && promotion != null && campaignPromotionMenuItem != null) {
            quantity += campaignPromotionMenuItem.getAttributes().getQuantity();
        }

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putInt(CART_QUANTITY, quantity);
        editor.apply();
    }

    public void setColors(ArrayList<CarColor> menuItems) {
        String cartMenuItems = new Gson().toJson(menuItems);
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(VEHICLE_COLOR, cartMenuItems);
        editor.apply();
    }

    public ArrayList<CarColor> getColors() {
        String cartMenuItems = cacheSharedPreferences.getString(VEHICLE_COLOR, null);
        if (cartMenuItems != null) {
            return GsonHelper.gsonFinal().fromJson(cartMenuItems, new TypeToken<ArrayList<CarColor>>() {
            }.getType());
        }
        return new ArrayList<>();
    }

    public void setAreas(String items) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(MAP_AREA, items);
        editor.apply();
    }

    public CityList getAreas() {
        String cartMenuItems = cacheSharedPreferences.getString(MAP_AREA, null);
        if (cartMenuItems != null) {
            return GsonHelper.gsonFinal().fromJson(cartMenuItems, CityList.class);
        }
        return new CityList();
    }

    public void setBrands(ArrayList<CarMaker> menuItems) {
        String cartMenuItems = new Gson().toJson(menuItems);
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(VEHICLE_BRANDS, cartMenuItems);
        editor.apply();
    }

    public ArrayList<CarMaker> getBrands() {
        String cartMenuItems = cacheSharedPreferences.getString(VEHICLE_BRANDS, null);
        if (cartMenuItems != null) {
            return GsonHelper.gsonFinal().fromJson(cartMenuItems, new TypeToken<ArrayList<CarMaker>>() {
            }.getType());
        }
        return new ArrayList<>();
    }

    public ArrayList<MenuItem> getCart() {

        String cartMenuItems = cacheSharedPreferences.getString(CART_MENU_ITEMS, null);

        if (cartMenuItems != null) {
            ArrayList<MenuItem> cart = GsonHelper.gsonFinal().fromJson(cartMenuItems, new TypeToken<ArrayList<MenuItem>>() {
            }.getType());
            /*if (CacheUtil.getInstance().getCoupon() != null && CacheUtil.getInstance().getPromotion() != null && CacheUtil.getInstance().getPromotionMenuItem() != null) {
                cart.add(CacheUtil.getInstance().getPromotionMenuItem());
            }*/
            return cart;
        }

        return new ArrayList<>();
    }


    private Pair<Boolean, ArrayList<MenuItem>> compareMenuItem(int position, MenuItem menuItem) {
        boolean same = false;

        ArrayList<MenuItem> menuItems = new ArrayList<>();
        String cartMenuItems = cacheSharedPreferences.getString(CART_MENU_ITEMS, null);
        if (cartMenuItems != null) {
            menuItems = GsonHelper.gsonFinal().fromJson(cartMenuItems, new TypeToken<ArrayList<MenuItem>>() {
            }.getType());
        }

        for (MenuItem cartMenuItem : menuItems) {
            if (position != menuItems.indexOf(cartMenuItem)) {
                if (Objects.equals(cartMenuItem.getId(), menuItem.getId()) && menuItem.isFavorite()) {
                    cartMenuItem.setFavorite(true);
                }
                if (menuItem.getId().equals(cartMenuItem.getId())) {
                    int sizeIndex = 0;
                    int trueIndex = 0;
                    if (menuItem.getSizes().getModifierItems().size() == 0 && cartMenuItem.getSizes().getModifierItems().size() == 0) {
                        same = true;
                    } else {
                        for (ModifierItem modifierItem : menuItem.getSizes().getModifierItems()) {
                            if (modifierItem.getAttributes().isSelected()) {
                                for (ModifierItem cartModifierItem : cartMenuItem.getSizes().getModifierItems()) {
                                    if (cartModifierItem.getAttributes().isSelected()) {
                                        if (cartModifierItem.getId().equals(modifierItem.getId())) {
                                            same = true;
                                            trueIndex = sizeIndex;
                                            break;
                                        }
                                    }
                                    sizeIndex++;
                                }
                            }
                        }
                    }

                    if (same) {
                        same = false;
                        if (menuItem.getDrinks().getModifierItems().size() == 0 && cartMenuItem.getDrinks().getModifierItems().size() == 0) {
                            same = true;
                        } else {
                            for (ModifierItem modifierItem : menuItem.getDrinks().getModifierItems()) {
                                if (modifierItem.getAttributes().isSelected()) {
                                    for (ModifierItem cartModifierItem : cartMenuItem.getDrinks().getModifierItems()) {
                                        if (cartModifierItem.getAttributes().isSelected()) {
                                            if (cartModifierItem.getId().equals(modifierItem.getId())) {
                                                same = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (same) {
                        same = false;
                        if (menuItem.getFries().getModifierItems().size() == 0 && cartMenuItem.getFries().getModifierItems().size() == 0) {
                            same = true;
                        } else {
                            for (ModifierItem modifierItem : menuItem.getFries().getModifierItems()) {
                                if (modifierItem.getAttributes().isSelected()) {
                                    for (ModifierItem cartModifierItem : cartMenuItem.getFries().getModifierItems()) {
                                        if (cartModifierItem.getAttributes().isSelected()) {
                                            if (cartModifierItem.getId().equals(modifierItem.getId())) {
                                                same = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }

                    if (same) {
                        same = false;
                        if (menuItem.getModifierGroups().size() == 0 && cartMenuItem.getModifierGroups().size() == 0) {
                            same = true;
                        } else {

                            HashMap<String, ModifierItem> menuItemSelectedModifierItems = new HashMap<>();
                            HashMap<String, ModifierItem> cartMenuItemSelectedModifierItems = new HashMap<>();

                            for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                                VisibilityConfig visibilityConfig = modifierGroup.getAttributes().getVisibilityConfig();
                                if (visibilityConfig == null || visibilityConfig.getInitialValue() == null || !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
                                    for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                                        if (modifierItem.getAttributes().isSelected()) {
                                            menuItemSelectedModifierItems.put(modifierItem.getId(), modifierItem);
                                        }
                                    }
                                }
                            }

                            for (ModifierGroup modifierGroup : cartMenuItem.getModifierGroups()) {
                                VisibilityConfig visibilityConfig = modifierGroup.getAttributes().getVisibilityConfig();
                                if (visibilityConfig == null || visibilityConfig.getInitialValue() == null || !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
                                    for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                                        if (modifierItem.getAttributes().isSelected()) {
                                            cartMenuItemSelectedModifierItems.put(modifierItem.getId(), modifierItem);
                                        }
                                    }
                                }
                            }

                            same = menuItemSelectedModifierItems.size() == cartMenuItemSelectedModifierItems.size() && menuItemSelectedModifierItems.keySet().containsAll(cartMenuItemSelectedModifierItems.keySet()) && cartMenuItemSelectedModifierItems.keySet().containsAll(menuItemSelectedModifierItems.keySet());

                            if (same) {
                                for (String id : menuItemSelectedModifierItems.keySet()) {
                                    ModifierItem menuModifierItem = menuItemSelectedModifierItems.get(id);
                                    ModifierItem cartModifierItem = cartMenuItemSelectedModifierItems.get(id);

                                    same = menuModifierItem.getAttributes().getQuantity() == cartModifierItem.getAttributes().getQuantity();
                                    if (!same) {
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (same) {
                        same = false;
                        if (menuItem.getIngredients().size() == 0 && cartMenuItem.getIngredients().size() == 0) {
                            same = true;
                        } else {

                            Set<String> menuItemRemovedIngredients = new HashSet<>();
                            Set<String> cartMenuItemRemovedIngredients = new HashSet<>();

                            for (Ingredient ingredient : menuItem.getIngredients()) {
                                if (!ingredient.getAttributes().isSelected()) {
                                    menuItemRemovedIngredients.add(ingredient.getId());
                                }
                            }

                            for (Ingredient ingredient : cartMenuItem.getIngredients()) {
                                if (!ingredient.getAttributes().isSelected()) {
                                    cartMenuItemRemovedIngredients.add(ingredient.getId());
                                }
                            }

                            same = menuItemRemovedIngredients.size() == cartMenuItemRemovedIngredients.size() && menuItemRemovedIngredients.containsAll(cartMenuItemRemovedIngredients) && cartMenuItemRemovedIngredients.containsAll(menuItemRemovedIngredients);

                        }

                    }

                    try {
                        if (menuItem.isCombo && cartMenuItem.isCombo) {
                            if (same) {
                                boolean sameIsFalseAlready = false;
                                if (menuItem.combo.size() == cartMenuItem.combo.size()) {
                                    if (!menuItem.combo.get(trueIndex).components.isEmpty() && !cartMenuItem.combo.get(trueIndex).components.isEmpty()) {
                                        if (menuItem.combo.get(trueIndex).components.size() == cartMenuItem.combo.get(trueIndex).components.size()) {
                                            int size = menuItem.combo.get(trueIndex).components.size();
                                            for (int i = 0; i < size; i++) {
                                                if (!menuItem.combo.get(trueIndex).components.get(i).items.isEmpty() && !cartMenuItem.combo.get(trueIndex).components.get(i).items.isEmpty()) {
                                                    if (menuItem.combo.get(trueIndex).components.get(i).items.size() == cartMenuItem.combo.get(trueIndex).components.get(i).items.size()) {
                                                        int iSize = menuItem.combo.get(trueIndex).components.get(i).items.size();
                                                        for (int a = 0; a < iSize; a++) {
                                                            if (menuItem.combo.get(trueIndex).components.get(i).items.get(a).selected && cartMenuItem.combo.get(trueIndex).components.get(i).items.get(a).selected) {
                                                                if (menuItem.combo.get(trueIndex).components.get(i).items.get(a).id.equalsIgnoreCase(cartMenuItem.combo.get(trueIndex).components.get(i).items.get(a).id)) {
                                                                    if (!menuItem.combo.get(trueIndex).components.get(i).items.get(a).modifierGroups.isEmpty() && !cartMenuItem.combo.get(trueIndex).components.get(i).items.get(a).modifierGroups.isEmpty()) {
                                                                        if (menuItem.combo.get(trueIndex).components.get(i).items.get(a).modifierGroups.size() == cartMenuItem.combo.get(trueIndex).components.get(i).items.get(a).modifierGroups.size()) {
                                                                            int mGSize = menuItem.combo.get(trueIndex).components.get(i).items.get(a).modifierGroups.size();
                                                                            for (int x = 0; x < mGSize; x++) {
                                                                                Set<String> menuItemSelected = new HashSet<>();
                                                                                Set<String> cartMenuItemSelected = new HashSet<>();
                                                                                List<ModifierItem> menuItems1 = new ArrayList<>();
                                                                                List<ModifierItem> menuItems2 = new ArrayList<>();
                                                                                for (int b = 0; b < cartMenuItem.combo.get(trueIndex).components.get(i).items.get(a).modifierGroups.get(x).getModifierItems().size(); b++) {
                                                                                    ModifierItem menuModifierItem = menuItem.combo.get(trueIndex).components.get(i).items.get(a).modifierGroups.get(x).getModifierItems().get(b);
                                                                                    ModifierItem cartMenuModifierItem = cartMenuItem.combo.get(trueIndex).components.get(i).items.get(a).modifierGroups.get(x).getModifierItems().get(b);
                                                                                    if (menuModifierItem.getAttributes().isSelected() && menuModifierItem.getAttributes().getQuantity() != 0) {
                                                                                        menuItemSelected.add(menuModifierItem.getId());
                                                                                        menuItems1.add(menuModifierItem);
                                                                                    }
                                                                                    if (cartMenuModifierItem.getAttributes().isSelected() && cartMenuModifierItem.getAttributes().getQuantity() != 0) {
                                                                                        cartMenuItemSelected.add(cartMenuModifierItem.getId());
                                                                                        menuItems2.add(cartMenuModifierItem);
                                                                                    }
                                                                                }
                                                                                if (!sameIsFalseAlready) {
                                                                                    same = menuItemSelected.size() == cartMenuItemSelected.size() && menuItemSelected.containsAll(cartMenuItemSelected);
                                                                                    if (!same) {
                                                                                        sameIsFalseAlready = true;
                                                                                    } else {
                                                                                        for (int h = 0; h < menuItems1.size(); h++) {
                                                                                            boolean theSame = true;
                                                                                            ModifierItem itemM = menuItems1.get(h);
                                                                                            for (int l = 0; l < menuItems2.size(); l++) {
                                                                                                if (itemM.getAttributes().getQuantity() != menuItems2.get(l).getAttributes().getQuantity()) {
                                                                                                    theSame = false;
                                                                                                    break;
                                                                                                }
                                                                                            }
                                                                                            if (!theSame) {
                                                                                                same = false;
                                                                                                sameIsFalseAlready = true;
                                                                                                break;
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        } else {
                                                                            same = false;
                                                                            break;
                                                                        }
                                                                    } else {
                                                                        if (cartMenuItem.combo.get(trueIndex).components.get(i).items.get(a).modifierGroups.size() != menuItem.combo.get(trueIndex).components.get(i).items.get(a).modifierGroups.size()) {
                                                                            same = false;
                                                                            break;
                                                                        }
                                                                    }
                                                                } else {
                                                                    same = false;
                                                                    break;
                                                                }
                                                            } else {
                                                                if (menuItem.combo.get(trueIndex).components.get(i).items.get(a).selected && !cartMenuItem.combo.get(trueIndex).components.get(i).items.get(a).selected) {
                                                                    same = false;
                                                                    break;
                                                                }
                                                                if (!menuItem.combo.get(trueIndex).components.get(i).items.get(a).selected && cartMenuItem.combo.get(trueIndex).components.get(i).items.get(a).selected) {
                                                                    same = false;
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        same = false;
                                                        break;
                                                    }
                                                } else {
                                                    if (cartMenuItem.combo.get(trueIndex).components.get(i).items.size() != menuItem.combo.get(trueIndex).components.get(i).items.size()) {
                                                        same = false;
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            same = false;
                                        }
                                    }
                                } else {
                                    same = false;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

                if (same) {
                    int quantity = cartMenuItem.getAttributes().getQuantity() + menuItem.getAttributes().getQuantity();
                    menuItems.get(menuItems.indexOf(cartMenuItem)).getAttributes().setQuantity(quantity);

                    if (cartMenuItem.getAttributes().isUpsell()) {
                        if (!cartMenuItem.getAttributes().isUpsell()) {
                            cartMenuItem.getAttributes().setUpsellQuantity(menuItem.getAttributes().getQuantity());
                        } else {
                            cartMenuItem.getAttributes().setUpsellQuantity(cartMenuItem.getAttributes().getUpsellQuantity() + menuItem.getAttributes().getQuantity());
                        }
                        cartMenuItem.getAttributes().setUpsell(true);
                    }

                    break;
                }
            }

        }

        return new Pair<>(same, menuItems);
    }

    public void addToCart(MenuItem menuItem) {

        Pair<Boolean, ArrayList<MenuItem>> pair = compareMenuItem(-1, menuItem);

        if (!pair.first) {
            MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_ADD_TO_CART);
            pair.second.add(menuItem);
        }

        if (!menuItem.getAttributes().getNotes().isEmpty() && pair.second.size() == 1) {
            pair.second.get(0).getAttributes().setNotes(menuItem.getAttributes().getNotes());
        }

        String updatedCartMenuItems = new Gson().toJson(pair.second);
        setCartQuantity(pair.second);
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(CART_MENU_ITEMS, updatedCartMenuItems);
        editor.apply();
    }

    public void updateCart(int position, MenuItem menuItem) {
        clearMukafaa();
        Pair<Boolean, ArrayList<MenuItem>> pair = compareMenuItem(position, menuItem);

        pair.second.remove(position);
        if (!pair.first) {
            pair.second.add(position, menuItem);
        }

        String updatedCartMenuItems = new Gson().toJson(pair.second);
        setCartQuantity(pair.second);
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(CART_MENU_ITEMS, updatedCartMenuItems);
        editor.apply();

    }

    public int getCartQuantity() {

        String cartMenuItems = cacheSharedPreferences.getString(CART_MENU_ITEMS, null);

        if (cartMenuItems != null) {
            try {
                List<MenuItem> menuItems = GsonHelper.gsonFinal().fromJson(cartMenuItems, new TypeToken<ArrayList<MenuItem>>() {
                }.getType());
                int quantity = 0;
                for (MenuItem menuItem : menuItems) {
                    quantity += menuItem.getAttributes().getQuantity();
                }
                if (CacheUtil.getInstance().getCouponCode() != null && CacheUtil.getInstance().getCouponMenuItem() != null) {
                    quantity += CacheUtil.getInstance().getCouponMenuItem().getAttributes().getQuantity();
                }
                if (CacheUtil.getInstance().getCampaign() != null && CacheUtil.getInstance().getCampaignPromotion() != null && CacheUtil.getInstance().getCampaignPromotionMenuItem() != null) {
                    quantity += CacheUtil.getInstance().getCampaignPromotionMenuItem().getAttributes().getQuantity();
                }
                return quantity;
            } catch (Exception e) {
                e.printStackTrace();
                setCart(new ArrayList<>());
            }
        }

        return 0;
    }

    public String getCartNotes() {

        return cacheSharedPreferences.getString(CART_NOTES, "");

    }

    public void setCartNotes(String notes) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(CART_NOTES, notes);
        editor.apply();

    }

    public boolean getUpsell() {

        return cacheSharedPreferences.getBoolean(UPSELL, true);

    }

    public void setUpsell(boolean upsell) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putBoolean(UPSELL, upsell);
        editor.apply();

    }

    public void setOrderType(OrderType orderType) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putInt(ORDER_TYPE, orderType.getValue());
        editor.apply();

    }

    public Boolean pickupTypes() {
        return (CacheUtil.getInstance().getOrderType() == OrderType.PICKUP || CacheUtil.getInstance().getOrderType() == OrderType.DRIVE_THRU || CacheUtil.getInstance().getOrderType() == OrderType.DINE_IN || CacheUtil.getInstance().getOrderType() == OrderType.CURBSIDE);
    }

    public OrderType getOrderType() {
        int orderType = cacheSharedPreferences.getInt(ORDER_TYPE, OrderType.NONE.getValue());
        switch (orderType) {
            case 1:
                return OrderType.PICKUP;
            case 2:
                return OrderType.DINE_IN;
            case 3:
                return OrderType.DELIVERY;
            case 4:
                return OrderType.CURBSIDE;
            case 5:
                return OrderType.DRIVE_THRU;
            default:
                return OrderType.NONE;
        }

    }

    public void setOrderLatitude(String latitude) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(ORDER_LATITUDE, latitude);
        editor.apply();

    }

    public String getOrderLatitude() {

        return cacheSharedPreferences.getString(ORDER_LATITUDE, "");

    }

    public void setOrderLongitude(String longitude) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(ORDER_LONGITUDE, longitude);
        editor.apply();

    }

    public String getOrderLongitude() {

        return cacheSharedPreferences.getString(ORDER_LONGITUDE, "");

    }

    public void setOrderStore(Store store) {
        if (getCampaign() != null && store != null) {
            Log.e("ADADADADADADAD","ADADADADAD:" + store.getAttributes().getName());
            JsonObject campaignBody = new JsonParser().parse(new Gson().toJson(getCampaign())).getAsJsonObject();
            campaignBody.addProperty("location", store.getId());
            //setCampaign(campaignBody.toString());
        }
        String orderStoreValue = new Gson().toJson(store);
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(ORDER_STORE, orderStoreValue);
        editor.commit();

    }

    public Store getOrderStore() {

        String orderStoreValue = cacheSharedPreferences.getString(ORDER_STORE, null);

        if (orderStoreValue != null) {
            return GsonHelper.gsonFinal().fromJson(orderStoreValue, new TypeToken<Store>() {
            }.getType());
        }

        return null;
    }

    public void setOrderAddress(Address address) {

        String orderAddressValue = new Gson().toJson(address);

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(ORDER_ADDRESS, orderAddressValue);
        editor.apply();

    }

    public Address getOrderAddress() {

        String orderAddressValue = cacheSharedPreferences.getString(ORDER_ADDRESS, null);

        if (orderAddressValue != null) {
            return GsonHelper.gsonFinal().fromJson(orderAddressValue, new TypeToken<Address>() {
            }.getType());
        }

        return null;
    }

    public void setOrderDeliveryLatitude(String latitude) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(ORDER_DELIVERY_LATITUDE, latitude);
        editor.apply();

    }

    public String getOrderDeliveryLatitude() {

        return cacheSharedPreferences.getString(ORDER_DELIVERY_LATITUDE, null);

    }

    public void setOrderDeliveryLongitude(String longitude) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(ORDER_DELIVERY_LONGITUDE, longitude);
        editor.apply();

    }

    public String getOrderDeliveryLongitude() {

        return cacheSharedPreferences.getString(ORDER_DELIVERY_LONGITUDE, null);

    }

    public void setOrderDeliveryAddress(String address) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(ORDER_DELIVERY_ADDRESS, address);
        editor.apply();

    }

    public String getOrderDeliveryAddress() {

        return cacheSharedPreferences.getString(ORDER_DELIVERY_ADDRESS, null);

    }

    public void setOrderDeliveryTelephone(String telephone) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(ORDER_DELIVERY_TELEPHONE, telephone);
        editor.apply();

    }

    public String getOrderDeliveryTelephone() {

        return cacheSharedPreferences.getString(ORDER_DELIVERY_TELEPHONE, null);

    }

    public void setOrderDeliveryLabel(String label) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(ORDER_DELIVERY_LABEL, label);
        editor.apply();

    }

    public String getOrderDeliveryLabel() {

        return cacheSharedPreferences.getString(ORDER_DELIVERY_LABEL, null);

    }

    public void setOrderDeliveryInstructions(String label) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(ORDER_DELIVERY_INSTRUCTIONS, label);
        editor.apply();

    }

    public String getOrderDeliveryInstructions() {

        return cacheSharedPreferences.getString(ORDER_DELIVERY_INSTRUCTIONS, null);

    }

    public void setOrderPayment(PaymentType paymentType) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(ORDER_PAYMENT, paymentType.getValue());
        editor.apply();
    }

    public PaymentType getOrderPayment() {

        String paymentType = cacheSharedPreferences.getString(ORDER_PAYMENT, PaymentType.NONE.getValue());

        if (paymentType.equalsIgnoreCase(PaymentType.CASH.getValue())) {
            return PaymentType.CASH;
        } else if (paymentType.equalsIgnoreCase(PaymentType.CARD.getValue())) {
            return PaymentType.CARD;
        } else if (paymentType.equalsIgnoreCase(PaymentType.CARD_ON_DELIVERY.getValue())) {
            return PaymentType.CARD_ON_DELIVERY;
        } else if (paymentType.equalsIgnoreCase(PaymentType.CARD_ON_PICKUP.getValue())) {
            return PaymentType.CARD_ON_PICKUP;
//        } else if (paymentType.equalsIgnoreCase(PaymentType.ALRAJHI.getValue())) {
//            return PaymentType.ALRAJHI;
//        } else if (paymentType.equalsIgnoreCase(PaymentType.LOYALTY.getValue())) {
//            return PaymentType.LOYALTY;
        } else {
            return PaymentType.NONE;
        }

    }

    public void setNotificationId(int id) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putInt(NOTIFICATION_ID, id);
        editor.apply();
    }

    public int getNotificationId() {
        return cacheSharedPreferences.getInt(NOTIFICATION_ID, 0);
    }

    public void setFavorites(Set<String> favorites) {

        String favoritesValue = new Gson().toJson(favorites);

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(FAVORITES, favoritesValue);
        editor.commit();

    }

    public void saveFavoriteToAddToCart(Favorite favorite) {
        String favoriteJson = new Gson().toJson(favorite);
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(FAVOURITE, favoriteJson);
        editor.apply(); // Don't forget to apply changes
    }

    public Favorite retrieveFavoriteToAddToCart() {
        String favoriteJson = cacheSharedPreferences.getString(FAVOURITE, null);

        if (favoriteJson != null) {
            return GsonHelper.gsonFinal().fromJson(favoriteJson, new TypeToken<Favorite>() {
            }.getType());
        }

        return null;
    }

    public Set<String> getFavorites() {

        String favoritesValue = cacheSharedPreferences.getString(FAVORITES, null);

        if (favoritesValue != null) {
            return GsonHelper.gsonFinal().fromJson(favoritesValue, new TypeToken<Set<String>>() {
            }.getType());
        }

        return new HashSet<>();
    }

    public void setFavoriteMenuCategory(MenuCategory menuCategory) {

        String favoriteMenuCategoryValue = new Gson().toJson(menuCategory);

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(FAVORITE_MENU_CATEGORY, favoriteMenuCategoryValue);
        editor.apply();

    }

    public MenuCategory getFavoriteMenuCategory() {

        String favoriteMenuCategoryValue = cacheSharedPreferences.getString(FAVORITE_MENU_CATEGORY, null);

        if (favoriteMenuCategoryValue != null) {
            return GsonHelper.gsonFinal().fromJson(favoriteMenuCategoryValue, new TypeToken<MenuCategory>() {
            }.getType());
        }

        return null;
    }

    public void setFavoriteMenuItem(MenuItem menuItem) {

        String favoriteMenuItemValue = new Gson().toJson(menuItem);

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(FAVORITE_MENU_ITEM, favoriteMenuItemValue);
        editor.apply();

    }

    public MenuItem getFavoriteMenuItem() {

        String favoriteMenuItemValue = cacheSharedPreferences.getString(FAVORITE_MENU_ITEM, null);

        if (favoriteMenuItemValue != null) {
            return GsonHelper.gsonFinal().fromJson(favoriteMenuItemValue, new TypeToken<MenuItem>() {
            }.getType());
        }

        return null;
    }

    public void setCouponCode(String couponCode) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(COUPON_CODE, couponCode);
        editor.apply();
    }

    public String getCouponCode() {
        return cacheSharedPreferences.getString(COUPON_CODE, null);
    }

    public void setCouponName(String couponName) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(COUPON_NAME, couponName);
        editor.apply();
    }

    public String getCouponName() {
        return cacheSharedPreferences.getString(COUPON_NAME, null);
    }

    public void setCampaign(Campaign campaign) {
        String campaignValue = new Gson().toJson(campaign);
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(CAMPAIGN, campaignValue);
        editor.commit();
    }

    public Campaign getCampaign() {

        String couponValue = cacheSharedPreferences.getString(CAMPAIGN, null);

        if (couponValue != null) {
            return GsonHelper.gsonFinal().fromJson(couponValue, new TypeToken<Campaign>() {
            }.getType());
        }

        return null;
    }

    public void setCampaignPromotion(Promotion promotion) {

        String promotionValue = new Gson().toJson(promotion);

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(CAMPAIGN_PROMOTION, promotionValue);
        editor.commit();

    }

    public Promotion getCampaignPromotion() {

        String promotionValue = cacheSharedPreferences.getString(CAMPAIGN_PROMOTION, null);

        if (promotionValue != null) {
            return GsonHelper.gsonFinal().fromJson(promotionValue, new TypeToken<Promotion>() {
            }.getType());
        }

        return null;
    }

    public void setCurbsideCar(CustomerCar customerCar) {
        String customerCarValue = new Gson().toJson(customerCar);
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(CURBSIDE_CAR, customerCarValue);
        editor.apply();
    }

    public CustomerCar getCurbsideCar() {
        String customerCarValue = cacheSharedPreferences.getString(CURBSIDE_CAR, null);
        if (customerCarValue != null) {
            return GsonHelper.gsonFinal().fromJson(customerCarValue, new TypeToken<CustomerCar>() {
            }.getType());
        }
        return null;
    }

    public void setCampaignPromotionMenuItem(MenuItem menuItem) {

        String promotionMenuItemValue = new Gson().toJson(menuItem);
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(CAMPAIGN_PROMOTION_MENU_ITEM, promotionMenuItemValue);
        editor.commit();

    }

    public CampaignToAdd getCampaignToAdd() {

        String campaignToAddValue = cacheSharedPreferences.getString(CAMPAIGN_TO_ADD, null);

        if (campaignToAddValue != null) {
            return GsonHelper.gsonFinal().fromJson(campaignToAddValue, new TypeToken<CampaignToAdd>() {
            }.getType());
        }

        return null;
    }

    public void setCampaignToAdd(CampaignToAdd campaignToAdd) {

        String campaignToAddValue = new Gson().toJson(campaignToAdd);
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(CAMPAIGN_TO_ADD, campaignToAddValue);
        editor.commit();

    }

    public boolean hasDeliveryPromotion() {
        Campaign campaign = CacheUtil.getInstance().getCampaign();
        try {
            if (campaign != null) {
                if (campaign.getPromotion().getAttributes().getType().equals("delivery")) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public MenuItem getCampaignPromotionMenuItem() {

        String promotionMenuItemValue = cacheSharedPreferences.getString(CAMPAIGN_PROMOTION_MENU_ITEM, null);

        if (promotionMenuItemValue != null) {
            return GsonHelper.gsonFinal().fromJson(promotionMenuItemValue, new TypeToken<MenuItem>() {
            }.getType());
        }

        return null;
    }

    public void setCouponMenuItem(MenuItem menuItem) {

        String promotionMenuItemValue = new Gson().toJson(menuItem);

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(COUPON_MENU_ITEM, promotionMenuItemValue);
        editor.apply();

    }

    public MenuItem getCouponMenuItem() {

        String couponMenuItemValue = cacheSharedPreferences.getString(COUPON_MENU_ITEM, null);

        if (couponMenuItemValue != null) {
            return GsonHelper.gsonFinal().fromJson(couponMenuItemValue, new TypeToken<MenuItem>() {
            }.getType());
        }

        return null;
    }

    public CouponToAdd getCouponToAdd() {

        String couponToAddValue = cacheSharedPreferences.getString(COUPON_TO_ADD, null);

        if (couponToAddValue != null) {
            return GsonHelper.gsonFinal().fromJson(couponToAddValue, new TypeToken<CouponToAdd>() {
            }.getType());
        }

        return null;
    }

    public void setCouponToAdd(CouponToAdd couponToAdd) {

        String couponToAddValue = new Gson().toJson(couponToAdd);

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(COUPON_TO_ADD, couponToAddValue);
        editor.apply();

    }

    public boolean getCartExpired() {

        return cacheSharedPreferences.getBoolean(CART_EXPIRED, false);

    }

    public void setCartExpired(boolean cartExpired) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putBoolean(CART_EXPIRED, cartExpired);
        editor.apply();

    }

    public boolean getOrderRatings() {

        return cacheSharedPreferences.getBoolean(ORDER_RATINGS, true);

    }

    public void setOrderRatings(boolean orderRatings) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putBoolean(ORDER_RATINGS, orderRatings);
        editor.apply();

    }

    public void setLoyaltyPointsEarnable(String orderId, int points) {
        cacheSharedPreferences.edit().putInt(LOYALTY_POINTS_EARNABLE + "_" + orderId, points).commit();
    }

    public int getLoyaltyPointsEarnable(String orderId) {
        return cacheSharedPreferences.getInt(LOYALTY_POINTS_EARNABLE + "_" + orderId, 0);
    }

    public String getLatestOrder() {
        return cacheSharedPreferences.getString(LATEST_ORDER, "");
    }

    public void setLatestOrder(String id) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(LATEST_ORDER, id);
        editor.apply();
    }

    public String getLatestDriverNumber() {
        return cacheSharedPreferences.getString(LATEST_DRIVER_NUMBER, "");
    }

    public void setLatestDriverNumber(String number) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(LATEST_DRIVER_NUMBER, number);
        editor.apply();
    }

    public String getLatestMessage() {
        return cacheSharedPreferences.getString(LATEST_MESSAGE, "");
    }

    public void setLatestMessage(String id) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(LATEST_MESSAGE, id);
        editor.apply();
    }

    public static String getOrderStoreId() {
        String orderStoreId = null;
        if (CacheUtil.getInstance().getOrderStore() != null) {
            orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
        }

        return orderStoreId;
    }

    public static List<SupportedCountry> getSupportedCountries(String applicationId, Context context) {
        JSONObject jsonObject = JsonAssetUtil.getJsonObjectAsset(context, applicationId.replace(".", "_") + ".json");
        if (jsonObject == null) {
            jsonObject = JsonAssetUtil.getJsonObjectAsset(context, "SupportedCountry.json");
        }
        if (jsonObject != null) {
            SupportedCountryResponse response = GsonUtil.getGson(jsonObject, SupportedCountryResponse.class);
            return response.data;
        }
        return Collections.emptyList();
    }

    public SupportedCountry getCountry() {
        String countryValue = cacheSharedPreferences.getString(COUNTRY, null);
        if (countryValue != null) {
            return GsonHelper.gsonFinal().fromJson(countryValue, new TypeToken<SupportedCountry>() {
            }.getType());
        }
        return null;
    }

    public void setCountry(SupportedCountry country) {
        String countryValue = new Gson().toJson(country);
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(COUNTRY, countryValue);
        editor.apply();
    }

    public void setDineInTableNumber(String tableNumber) {
        cacheSharedPreferences.edit().putString(DINE_IN_TABLE_NUMBER, tableNumber).commit();
    }

    public String getDineInTableNumber() {
        return cacheSharedPreferences.getString(DINE_IN_TABLE_NUMBER, "");
    }

    // TODO: DO NOT USE, THIS IS ONLY TEMPORARY!!!
    @Deprecated
    public static boolean shouldDismiss = false;

    public void setMukafaaTransactionId(String id) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(MUKAFAA_TRANSACTION_ID, id);
        editor.apply();
    }

    public String getMukafaaTransactionId() {
        return cacheSharedPreferences.getString(MUKAFAA_TRANSACTION_ID, "");
    }

    public void setMukafaaTransactionAmount(double amount) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putFloat(MUKAFAA_TRANSACTION_AMOUNT, (float) amount);
        editor.apply();
    }

    public double getMukafaaTransactionAmount() {
        return cacheSharedPreferences.getFloat(MUKAFAA_TRANSACTION_AMOUNT, 0);
    }

    public void clearMukafaa() {
        setMukafaaTransactionAmount(0);
        setMukafaaTransactionId("");
    }

    public boolean getAppReview() {
        return cacheSharedPreferences.getBoolean(APP_REVIEW, false);
    }

    public void setAppReview(boolean appReview) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putBoolean(APP_REVIEW, appReview);
        editor.apply();

    }

    public String getSigninUsername() {
        return cacheSharedPreferences.getString(SIGNIN_USERNAME, "");
    }

    public void setSigninUsername(String signinUsername) {

        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(SIGNIN_USERNAME, signinUsername);
        editor.apply();

    }

    public long getCountryCachedTime(String applicationId) {
        return cacheSharedPreferences.getLong(applicationId.replace(".", "_") + "_cached_time", 0L);
    }

    public void setCountryCachedTime(String applicationId, long menuCached) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putLong(applicationId.replace(".", "_") + "_cached_time", menuCached);
        editor.apply();
    }

    public String getCountryApplication(String applicationId) {
        return cacheSharedPreferences.getString(applicationId.replace(".", "_") + "_application", "");
    }

    public void setCountryApplication(String applicationId, String application) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(applicationId.replace(".", "_") + "_application", application);
        editor.apply();
    }

    public String getCountrySlider(String applicationId) {
        return cacheSharedPreferences.getString(applicationId.replace(".", "_") + "_slider", "");
    }

    public void setCountrySlider(String applicationId, String slider) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(applicationId.replace(".", "_") + "_slider", slider);
        editor.apply();
    }

    public String getCountryMenu(String applicationId) {
        return cacheSharedPreferences.getString(applicationId.replace(".", "_") + "_menu", "");
    }

    public void setCountryMenu(String applicationId, String menu) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(applicationId.replace(".", "_") + "_menu", menu);
        editor.apply();
    }

    public boolean hasPromotion() {
        return getCouponCode() != null || getCampaign() != null;
    }

    public boolean getPreselectedBranch() {
        return cacheSharedPreferences.getBoolean(PRESELECTED_BRANCH, false);
    }

    public void setPreselectedBranch(boolean preselectedBranch) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putBoolean(PRESELECTED_BRANCH, preselectedBranch);
        editor.apply();
    }

    public Set<String> getSupportCardTypes() {
        Set<String> supportedCardTypes = new HashSet<>();
        try {
            supportedCardTypes = new Gson().fromJson(cacheSharedPreferences.getString(SUPPORT_CARD_TYPES, "[]"), new TypeToken<Set<String>>() {
            }.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return supportedCardTypes;
    }

    public void setSupportCardTypes(String supportCardTypes) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(SUPPORT_CARD_TYPES, supportCardTypes);
        editor.apply();
    }

    public boolean getIsAuthCredentialMigrated() {
        return cacheSharedPreferences.getBoolean(IS_AUTHCREDENTIAL_MIGRATED, false);
    }

    public void setIsAuthCredentialMigrated(boolean authCredentialMigrated) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putBoolean(IS_AUTHCREDENTIAL_MIGRATED, authCredentialMigrated);
        editor.apply();
    }

    public boolean getIsContactUsEnabled() {
        return cacheSharedPreferences.getBoolean(IS_CONTACT_US_ENABLED, false);
    }

    public void setIsContactUsEnabled(boolean isContactUsEnabled) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putBoolean(IS_CONTACT_US_ENABLED, isContactUsEnabled);
        editor.apply();
    }

    public String getMenuCdnLastModified() {
        return cacheSharedPreferences.getString(MENU_CDN_LAST_MODIFIED, "");
    }

    public void setMenuCdnLastModified(String menuCdnLastModified) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(MENU_CDN_LAST_MODIFIED, menuCdnLastModified);
        editor.apply();
    }

    public boolean getReloadDifferentMenu() {
        return isSeparatedMenus() && cacheSharedPreferences.getBoolean(RELOAD_DIFFERENT_MENU, false);
    }

    public void setReloadDifferentMenu(boolean reloadDifferentMenu) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putBoolean(RELOAD_DIFFERENT_MENU, reloadDifferentMenu);
        editor.apply();
    }

    public String getDifferentMenuId() {
        return cacheSharedPreferences.getString(DIFFERENT_MENU_ID, "");
    }

    public void setDifferentMenuId(String differentMenuId) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(DIFFERENT_MENU_ID, differentMenuId);
        editor.apply();
    }


    public boolean isSeparatedMenus() {
        Application application = getApplication();
        if (application == null) return false;
        application.getAttributes();
        return !application.getAttributes().getOrderTypeMenus().isEmpty();
    }

    public void setConsumerData(String data) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(CONSUMER_DATA, data);
        editor.apply();
    }

    public ConsumerData getConsumerData() {
        ConsumerData data = new ConsumerData();
        String consumerdata = cacheSharedPreferences.getString(CONSUMER_DATA, null);
        if (consumerdata != null && !consumerdata.isEmpty()) {
            data = GsonHelper.gsonFinal().fromJson(consumerdata, ConsumerData.class);
        }
        return data;
    }

    public void setTotalAccumulatedPoints(int totalAccumulatedPoints) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putInt(TOTAL_ACCUMULATED_POINTS, totalAccumulatedPoints);
        editor.apply();
    }

    public int getTotalAccumulatedPoints() {
        return cacheSharedPreferences.getInt(TOTAL_ACCUMULATED_POINTS, 0);
    }

    public void setStartingPointsOfNextTier(int startingPointsOfNextTier) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putInt(STARTING_POINTS_OF_NEXT_TIER, startingPointsOfNextTier);
        editor.apply();
    }

    public int getStartingPointsOfNextTier() {
        return cacheSharedPreferences.getInt(STARTING_POINTS_OF_NEXT_TIER, 0);
    }

    public void setPointsUntilNextTier(int pointsUntilNextTier) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putInt(POINTS_UNTIL_NEXT_TIER, pointsUntilNextTier);
        editor.apply();
    }

    public int getPointsUntilNextTier() {
        return cacheSharedPreferences.getInt(POINTS_UNTIL_NEXT_TIER, 0);
    }

    public void setPromisedTime(String promisedTime) {
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putString(PROMISED_TIME, promisedTime);
        editor.apply();
    }

    public String getPromisedTime() {
        return cacheSharedPreferences.getString(PROMISED_TIME, "");
    }
}
