package com.herfy.main;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.webkit.URLUtil;

import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.herfy.analytics.kochava.KochavaTrackerUtil;
import com.herfy.database.AppDatabase;
import com.herfy.utils.constants.Text;
import com.herfy.database.entities.AllergenDb;
import com.herfy.database.entities.IngredientDb;
import com.herfy.database.entities.MenuCategoryDb;
import com.herfy.database.entities.MenuItemDb;
import com.herfy.database.entities.ModifierGroupDb;
import com.herfy.database.entities.ModifierItemDb;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.BuildUtil;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.helper.ably.AblyUtil;
import com.herfy.utils.menu.MenuUtil;
import com.kochava.tracker.events.EventType;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.ApiRequestWithHeadersListener;
import com.skylinedynamics.solosdk.api.calls.ApplicationsApiCall;
import com.skylinedynamics.solosdk.api.calls.CmsApiCall;
import com.skylinedynamics.solosdk.api.calls.CouponsApiCall;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.calls.IntegrationsApiCall;
import com.skylinedynamics.solosdk.api.calls.NotificationApiCall;
import com.skylinedynamics.solosdk.api.calls.OrdersApiCall;
import com.skylinedynamics.solosdk.api.calls.UtilitiesApiCall;
import com.skylinedynamics.solosdk.api.models.objects.AllergenCdn;
import com.skylinedynamics.solosdk.api.models.objects.Application;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.Concept;
import com.skylinedynamics.solosdk.api.models.objects.CustomFieldObject;
import com.skylinedynamics.solosdk.api.models.objects.Customer;
import com.skylinedynamics.solosdk.api.models.objects.IngredientCdn;
import com.skylinedynamics.solosdk.api.models.objects.Integration;
import com.skylinedynamics.solosdk.api.models.objects.Loyalty;
import com.skylinedynamics.solosdk.api.models.objects.MenuCategoryItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupCdn;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupType;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItemCdn;
import com.skylinedynamics.solosdk.api.models.objects.OrderData;
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory;
import com.skylinedynamics.solosdk.api.models.objects.OrderStatusType;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.PaymentType;
import com.skylinedynamics.solosdk.api.models.objects.Promotion;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.Slider;
import com.skylinedynamics.solosdk.api.models.objects.cms.CmsPage;
import com.skylinedynamics.solosdk.api.models.requestbodies.OrderFeedbackBody;
import com.skylinedynamics.solosdk.util.GsonHelper;
import com.herfy.util.AppLogger;
import com.herfy.utils.helper.DeviceUtil;
import com.herfy.utils.handler.ErrorHandler;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.utils.analytics.MixpanelUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.ably.lib.types.Message;
import kotlin.Unit;
import okhttp3.Headers;

public class MainPresenter implements MainContract.Presenter {

    private MainContract.View mainView;

    private OrderHistory order;

    private List<Rating> ratings = new ArrayList<>();

    public MainPresenter(MainContract.View mainView) {
        this.mainView = mainView;
        this.mainView.setPresenter(this);
    }

    @Override
    public void start() {
        mainView.setupViews();
        mainView.setupFonts();
        mainView.setupTranslations();
        getCartQuantity();
    }

    @Override
    public void getCartQuantity() {
        mainView.setCartQuantity(CacheUtil.getInstance().getCartQuantity());
    }

    @Override
    public void getCaloriesPage() {
        CmsApiCall.getInstance().getPageByLabel("calories", new ApiRequestListener() {
            @Override
            public void onSuccess(Object obj) {
                try {
                    JSONObject jsonObject = new JSONObject(obj.toString());
                    JSONArray data = jsonObject.optJSONArray("data");
                    if (data != null && data.length() > 0) {
                        CmsPage caloriesPage = new CmsPage().parse(data.getJSONObject(0));
                        String url = caloriesPage.getAttributes().getUrl();
                        if (url != null && !url.isEmpty() && URLUtil.isValidUrl(url)) {
                            mainView.setCaloriesPage(caloriesPage);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Object error) {
                AppLogger.printRetrofitError(error);
            }
        });
    }

    @Override
    public void getOrders() {
        if (AuthUtil.getInstance().isSignedIn()) {
            Customer customer = AuthUtil.getInstance().getCustomer();
            CustomersApiCall.getInstance().getOrders(customer.getId(), "1", "1", new ApiRequestListener() {

                @Override
                public void onSuccess(Object o) {
                    try {
                        JSONObject object = new JSONObject(o.toString());
                        JSONArray array = object.optJSONArray("data");

                        if (array != null && array.length() > 0) {
                            order = OrderHistory.parse(array.getJSONObject(0));
                            CacheUtil.getInstance().setLastOrderId(order.getId());

                            if (order.getSequence() <= OrderStatusType.FINISHED.getValue()) {
                                if (order.getSequence() >= OrderStatusType.ARRIVED_AT_CUSTOMER.getValue() && order.getSequence() <= OrderStatusType.FINISHED.getValue()) {
                                    if (CacheUtil.getInstance().getOrderRatings()) {
                                        getOrderFeedbackTopics();
                                    }
                                    mainView.refreshOrderAgain();
                                } else if (order.getSequence() < OrderStatusType.FINISHED.getValue()) {
                                    AblyUtil ablyUtil = AblyUtil.Companion.getInstance();
                                    ablyUtil.subscribeToOrderStatusUpdate(
                                        order.getId(),
                                        MainPresenter.this::onOrderStatusUpdate
                                    );
                                }
                            }

                        }


                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.onError(e.getMessage());
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
    public void getOrderFeedbackTopics() {

        if (order != null) {

            OrdersApiCall.getInstance().getOrderFeedbackTopics(order.getId(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    ratings.clear();
                    try {
                        JSONObject jsonObject = new JSONObject(o.toString());
                        JSONArray dataArray = jsonObject.optJSONArray("data");

                        if (dataArray != null) {
                            ratings.addAll(GsonHelper.gsonFinal().fromJson(dataArray.toString(), new TypeToken<List<Rating>>() {
                            }.getType()));
                        }

                        if (ratings.size() > 0) {
                            getOrderFeedbacks();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.onError(e.getMessage());
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
    public void getOrderFeedbacks() {
        if (order != null) {
            OrdersApiCall.getInstance().getOrderFeedbacks(order.getId(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    try {
                        JSONObject jsonObject = new JSONObject(o.toString());
                        JSONArray dataArray = jsonObject.optJSONArray("data");

                        if (dataArray != null && dataArray.length() == 0) {
                            CacheUtil.getInstance().setOrderRatings(false);
                            mainView.showRatings(ratings);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.onError(e.getMessage());
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
    public void sendOrderFeedback(List<Rating> ratings) {
        this.ratings = ratings;
        Customer customer = AuthUtil.getInstance().getCustomer();
        OrderFeedbackBody orderFeedbackBody = new OrderFeedbackBody(customer.getAttributes().getFirstName() + " " + customer.getAttributes().getLastName(), customer.getAttributes().getEmail(), customer.getAttributes().getMobile(), "Order Feedback", "Order Feedback", this.ratings);
        OrdersApiCall.getInstance().sendOrderFeedback(order.getId(), orderFeedbackBody, null, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                mainView.showMessage(CacheUtil.getInstance().getTranslations(Text.ORDER_RATING_SENT));
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                mainView.showMessage(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.RETRIEVING_ORDER_DETAILS_ERROR), null));
            }
        });
    }

    @Override
    public void detachAbly() {
        AblyUtil.Companion.getInstance().detachAll();
    }

    @Override
    public void clearMenuCache() {
        AppDatabase appDatabase = AppDatabase.getInstance();
        appDatabase.menuCategoryDao().deleteAll();
        appDatabase.menuItemDao().deleteAll();
        appDatabase.modifierGroupDao().deleteAll();
        appDatabase.modifierItemDao().deleteAll();
        appDatabase.ingredientDao().deleteAll();
        appDatabase.allergenDao().deleteAll();
        appDatabase.storeDao().deleteAll();
    }

    @Override
    public void geUnreadNotification() {
        if(AuthUtil.getInstance().isSignedIn()) {
            NotificationApiCall.getInstance().getUnreadNotifications(new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    try {
                        JSONObject jObject = new JSONObject(o.toString());
                        int newNotifCount = jObject.getJSONObject("meta").getJSONObject("pagination").getInt("total");
                        mainView.showNewNotifIndicator(newNotifCount);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Object o) {
                    mainView.showNewNotifIndicator(0);
                }
            });
        }
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
                            mainView.preloadImage(loyalty.getAttributes().getLoyaltyTier().getImageUrl(), RequestOptions.centerInsideTransform());
                        }
                    }
                    Loyalty loyalty = GsonHelper.gsonFinal().fromJson(data.toString(), Loyalty.class);
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


    @Override
    public void getApplication() {
        ApplicationsApiCall.getInstance().getApplication(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject jsonObject = new JSONObject(o.toString());
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
                                    MixpanelUtil.Companion.registerSuperProperties();
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

                    getSliders();

                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                    mainView.proceedRestart();
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o.toString());
                mainView.proceedRestart();
            }
        });
    }

    @Override
    public void getDigitalCoupons(String digitalCouponId) {
        CouponsApiCall.getInstance().getGuestCampaigns(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    ArrayList<Campaign> digitalCoupons = GsonHelper.gsonFinal().fromJson(object.getJSONArray("data").toString(), new TypeToken<ArrayList<Campaign>>() {
                    }.getType());

                    if (object.has("included")) {

                        ArrayList<Promotion> promotions = GsonHelper.gsonFinal().fromJson(object.getJSONArray("included").toString(), new TypeToken<ArrayList<Promotion>>() {
                        }.getType());
                        HashMap<String, Promotion> promotionsHashMap = new HashMap<>();

                        for (Promotion promotion : promotions) {
                            promotionsHashMap.put(promotion.getId(), promotion);
                        }

                        for (Campaign digitalCoupon : digitalCoupons) {
                            digitalCoupon.setPromotion(promotionsHashMap.get(digitalCoupon.getAttributes().getPromotionId()));
                        }

                        for (Campaign digitalCoupon : digitalCoupons) {
                            if (digitalCoupon.getId().equalsIgnoreCase(digitalCouponId)) {
                                mainView.showDigitalCoupon(digitalCoupon);
                                break;
                            }
                        }

                    }

                } catch (JSONException ex) {
                    ex.printStackTrace();
                    this.onError("");
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
            }
        });
    }

    @Override
    public void multibrandValidation(boolean proceed) {
        if(AuthUtil.getInstance().isSignedIn()) {
            JsonObject orderBody = new JsonObject();
            Customer customer = AuthUtil.getInstance().getCustomer();
            orderBody.addProperty("customer", customer.getId());
            CouponsApiCall.getInstance().validatePromotion(orderBody, new ApiRequestListener() {
                @Override
                public void onSuccess(Object obj) {
                    if(proceed) {
                        mainView.dismissDialogs();
                        mainView.gateWay();
                    }
                }

                @Override
                public void onError(Object error) {
                    AppLogger.printRetrofitError(error);
                    if(proceed) {

                    }
                }
            });
        }
    }

    @Override
    public void getFaqs() {
        CmsApiCall.getInstance().getPageByLabel("FAQ", new ApiRequestListener() {
            @Override
            public void onSuccess(Object obj) {
                try {
                    JSONObject jsonObject = new JSONObject(obj.toString());
                    JSONArray data = jsonObject.optJSONArray("data");
                    if (data != null && data.length() > 0) {
                        CmsPage termsAndConditionsPage = new CmsPage().parse(data.getJSONObject(0));
                        String content = termsAndConditionsPage.getAttributes().getUrl();
                        if (content != null && !content.isEmpty()) {
                            mainView.setFaqs(content);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Object error) {
                AppLogger.printRetrofitError(error);
            }
        });
    }

    @Override
    public void getAboutUs() {
        CmsApiCall.getInstance().getPageByLabel("About Us", new ApiRequestListener() {
            @Override
            public void onSuccess(Object obj) {
                try {
                    JSONObject jsonObject = new JSONObject(obj.toString());
                    JSONArray data = jsonObject.optJSONArray("data");
                    if (data != null && data.length() > 0) {
                        CmsPage termsAndConditionsPage = new CmsPage().parse(data.getJSONObject(0));
                        String content = termsAndConditionsPage.getAttributes().getUrl();
                        if (content != null && !content.isEmpty()) {
                            mainView.setAboutUs(content);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Object error) {
                AppLogger.printRetrofitError(error);
            }
        });
    }

    @Override
    public void getTermsAndConditions() {
        CmsApiCall.getInstance().getPageByLabel("Terms and Conditions", new ApiRequestListener() {
            @Override
            public void onSuccess(Object obj) {
                try {
                    JSONObject jsonObject = new JSONObject(obj.toString());
                    JSONArray data = jsonObject.optJSONArray("data");
                    if (data != null && data.length() > 0) {
                        CmsPage termsAndConditionsPage = new CmsPage().parse(data.getJSONObject(0));
                        String content = termsAndConditionsPage.getAttributes().getUrl();
                        if (content != null && !content.isEmpty()) {
                            mainView.setTermsAndConditions(content);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Object error) {
                AppLogger.printRetrofitError(error);
            }
        });
    }

    public void getSliders() {
        CmsApiCall.getInstance().getSliders(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
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

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o.toString());
                CacheUtil.getInstance().setMidSliderId("");
                proceed();
            }
        });
    }

    public void proceed() {
        CacheUtil.getInstance().setUpsell(true);
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
        new Handler(Looper.getMainLooper()).post(this::getMenuCdn);
    }

    public void parseMenu(JSONObject data) {
        Executor executor = Executors.newSingleThreadExecutor();
        AppDatabase appDatabase = AppDatabase.getInstance();

        try {
            JSONArray array = data.getJSONArray("data");
            List<MenuCategoryItem> menuCategoryItems = GsonHelper.gsonFinal().fromJson(array.toString(), new TypeToken<ArrayList<MenuCategoryItem>>() {
            }.getType());

            JSONObject included = data.getJSONObject("included");

            JSONArray allergensArray = new JSONArray();
            if (included.has("allergens") && !included.isNull("allergens")) {
                allergensArray = included.getJSONArray("allergens");
            }
            List<AllergenCdn> allergens = GsonHelper.gsonFinal().fromJson(allergensArray.toString(), new TypeToken<ArrayList<AllergenCdn>>() {
            }.getType());
            for (AllergenCdn allergen : allergens) {
                AllergenDb allergenDb = new AllergenDb();
                allergenDb.itemAllergenId = allergen.getId();
                allergenDb.id = allergen.getId();
                allergenDb.data = new Gson().toJson(allergen);
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        appDatabase.allergenDao().insertAll(allergenDb);
                    }
                });
            }

            JSONArray ingredientsArray = new JSONArray();
            if (included.has("ingredients") && !included.isNull("ingredients")) {
                ingredientsArray = included.getJSONArray("ingredients");
            }
            List<IngredientCdn> ingredients = GsonHelper.gsonFinal().fromJson(ingredientsArray.toString(), new TypeToken<ArrayList<IngredientCdn>>() {
            }.getType());
            for (IngredientCdn ingredient : ingredients) {
                IngredientDb ingredientDb = new IngredientDb();
                ingredientDb.itemIngredientId = ingredient.getId();
                ingredientDb.id = ingredient.getId();
                ingredientDb.data = new Gson().toJson(ingredient);
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        appDatabase.ingredientDao().insertAll(ingredientDb);
                    }
                });
            }

            JSONArray modifierGroupsArray = new JSONArray();
            if (included.has("modifierGroups") && !included.isNull("modifierGroups")) {
                modifierGroupsArray = included.getJSONArray("modifierGroups");
            }
            List<ModifierGroupCdn> modifierGroups = GsonHelper.gsonFinal().fromJson(modifierGroupsArray.toString(), new TypeToken<ArrayList<ModifierGroupCdn>>() {
            }.getType());
            for (ModifierGroupCdn modifierGroup : modifierGroups) {
                String cObject = modifierGroup.getAttributes().customField.toString().replace(" ", "");
                try {
                    if (!String.valueOf(cObject.charAt(0)).equals("[")) {
                        modifierGroup.getAttributes().customFieldObjectMap = GsonHelper.gsonFinal().fromJson(cObject, CustomFieldObject.class);
                    }
                } catch (Exception e) {
                    String[] separated = cObject.split(",");
                    for (int i = 0; i < separated.length; i++) {
                        String[] inner = separated[i].replace("{", "").replace("}", "").split("=");
                        if (inner.length == 2) {
                            if (inner[0].equalsIgnoreCase("top_bun")) {
                                modifierGroup.getAttributes().customFieldObjectMap.topBun = inner[1];
                            } else if (inner[0].equalsIgnoreCase("bottom_bun")) {
                                modifierGroup.getAttributes().customFieldObjectMap.bottomBun = inner[1];
                            } else if (inner[0].equalsIgnoreCase("is-ingredient")) {
                                modifierGroup.getAttributes().customFieldObjectMap.isIngredient = inner[1];
                            } else if (inner[0].equalsIgnoreCase("modifier-group")) {
                                modifierGroup.getAttributes().customFieldObjectMap.modifierGroup = inner[1];
                            } else if (inner[0].equalsIgnoreCase("label")) {
                                modifierGroup.getAttributes().customFieldObjectMap.label = inner[1];
                            } else if (inner[0].equalsIgnoreCase("category-highlight")) {
                                modifierGroup.getAttributes().customFieldObjectMap.categoryHighlight = inner[1].equalsIgnoreCase("true");
                            } else if (inner[0].equalsIgnoreCase("cannot-switch-meal-sandwich")) {
                                modifierGroup.getAttributes().customFieldObjectMap.isNotItemable = inner[1].equalsIgnoreCase("true");
                            } else if (inner[0].equalsIgnoreCase("pattys")) {
                                modifierGroup.getAttributes().customFieldObjectMap.patty = inner[1];
                            } else if (inner[0].equalsIgnoreCase("disable-pos-android")) {
                                modifierGroup.getAttributes().customFieldObjectMap.disablePosAndroid = inner[1];
                            } else if (inner[0].equalsIgnoreCase("kiosk_address_id")) {
                                modifierGroup.getAttributes().customFieldObjectMap.kioskAddressId = inner[1];
                            }
                        }
                    }
                    try {
                        int index = cObject.indexOf("patty=[");
                        index = index + 6;
                        StringBuilder builder = new StringBuilder();
                        for (int i = index; i < cObject.length(); i++) {
                            builder.append(cObject.charAt(i));
                            if (Character.toString(cObject.charAt(i)).equalsIgnoreCase("]")) {
                                break;
                            }
                        }
                        String newString = builder.toString().replace("[", "").replace("]", "");
                        String[] newSeparated = newString.split(",");
                        ArrayList<String> list = new ArrayList();
                        for (int a = 0; a < newSeparated.length; a++) {
                            list.add(newSeparated[a]);
                        }
                        modifierGroup.getAttributes().customFieldObjectMap.patty = new Gson().toJson(list);
                    } catch (Exception ie) {
                        ie.printStackTrace();
                    }
                    e.printStackTrace();
                }
                String code = "";
                if (modifierGroup.getAttributes().getCode() != null) {
                    code = modifierGroup.getAttributes().getCode().trim().toLowerCase();
                }
                String type = "";
                if (modifierGroup.getAttributes().getType() != null) {
                    type = modifierGroup.getAttributes().getType().trim().toLowerCase();
                }
                String label = "";
                if (modifierGroup.getAttributes().getLabel() != null) {
                    label = modifierGroup.getAttributes().getLabel().trim().toLowerCase();
                }

                if (code.equals("sizes") || type.equals("sizes") || label.equals("sizes")) {
                    modifierGroup.setType(ModifierGroupType.SIZES);
                } else {
                    modifierGroup.setType(ModifierGroupType.OPTIONS);
                }
                ModifierGroupDb modifierGroupDb = new ModifierGroupDb();
                modifierGroupDb.itemGroupId = modifierGroup.getId();
                modifierGroupDb.id = modifierGroup.getId();
                modifierGroupDb.type = modifierGroup.getType().getValue();
                modifierGroupDb.data = new Gson().toJson(modifierGroup);
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        appDatabase.modifierGroupDao().insertAll(modifierGroupDb);
                    }
                });

            }

            JSONArray modifiersArray = new JSONArray();
            if (included.has("modifiers") && !included.isNull("modifiers")) {
                modifiersArray = included.getJSONArray("modifiers");
            }
            List<ModifierItemCdn> modifiers = GsonHelper.gsonFinal().fromJson(modifiersArray.toString(), new TypeToken<ArrayList<ModifierItemCdn>>() {
            }.getType());
            for (ModifierItemCdn modifier : modifiers) {
                ModifierItemDb modifierItemDb = new ModifierItemDb();
                modifierItemDb.itemGroupModifierId = modifier.getId();
                modifierItemDb.id = modifier.getId();
                modifierItemDb.data = new Gson().toJson(modifier);
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        appDatabase.modifierItemDao().insertAll(modifierItemDb);
                    }
                });
            }

            ArrayList<MenuCategoryDb> menuCategoryDbs = new ArrayList<>();
            ArrayList<MenuItemDb> menuItemDbs = new ArrayList<>();

            for (MenuCategoryItem menuCategoryItem : menuCategoryItems) {

                MenuCategoryDb menuCategoryDb = new MenuCategoryDb();
                menuCategoryDb.id = menuCategoryItem.getAttributes().getMenuCategory().getId();
                menuCategoryDb.data = new Gson().toJson(menuCategoryItem.getAttributes().getMenuCategory());
                menuCategoryDbs.add(menuCategoryDb);

                for (MenuItem menuItem : menuCategoryItem.getAttributes().getMenuItems()) {
                    String cObject = menuItem.customField.toString().replace(" ", "");
                    try {
                        if (!String.valueOf(cObject.charAt(0)).equals("[")) {
                            menuItem.customFieldObjectMap = GsonHelper.gsonFinal().fromJson(cObject, CustomFieldObject.class);
                        }
                    } catch (Exception e) {
                        String[] separated = cObject.split(",");
                        for (int i = 0; i < separated.length; i++) {
                            String[] inner = separated[i].replace("{", "").replace("}", "").split("=");
                            if (inner.length == 2) {
                                if (inner[0].equalsIgnoreCase("top_bun")) {
                                    menuItem.customFieldObjectMap.topBun = inner[1];
                                } else if (inner[0].equalsIgnoreCase("bottom_bun")) {
                                    menuItem.customFieldObjectMap.bottomBun = inner[1];
                                } else if (inner[0].equalsIgnoreCase("is-ingredient")) {
                                    menuItem.customFieldObjectMap.isIngredient = inner[1];
                                } else if (inner[0].equalsIgnoreCase("modifier-group")) {
                                    menuItem.customFieldObjectMap.modifierGroup = inner[1];
                                } else if (inner[0].equalsIgnoreCase("label")) {
                                    menuItem.customFieldObjectMap.label = inner[1];
                                } else if (inner[0].equalsIgnoreCase("category-highlight")) {
                                    menuItem.customFieldObjectMap.categoryHighlight = inner[1].equalsIgnoreCase("true");
                                } else if (inner[0].equalsIgnoreCase("cannot-switch-meal-sandwich")) {
                                    menuItem.customFieldObjectMap.isNotItemable = inner[1].equalsIgnoreCase("true");
                                } else if (inner[0].equalsIgnoreCase("pattys")) {
                                    menuItem.customFieldObjectMap.patty = inner[1];
                                } else if (inner[0].equalsIgnoreCase("disable-pos-android")) {
                                    menuItem.customFieldObjectMap.disablePosAndroid = inner[1];
                                } else if (inner[0].equalsIgnoreCase("kiosk_address_id")) {
                                    menuItem.customFieldObjectMap.kioskAddressId = inner[1];
                                }else if (inner[0].equalsIgnoreCase("nutritional-info-image")) {
                                    menuItem.customFieldObjectMap.nutritionInfoImageUrl = inner[1];
                                }
                            }
                        }
                        try {
                            int index = cObject.indexOf("patty=[");
                            index = index + 6;
                            StringBuilder builder = new StringBuilder();
                            for (int i = index; i < cObject.length(); i++) {
                                builder.append(cObject.charAt(i));
                                if (Character.toString(cObject.charAt(i)).equalsIgnoreCase("]")) {
                                    break;
                                }
                            }
                            String newString = builder.toString().replace("[", "").replace("]", "");
                            String[] newSeparated = newString.split(",");
                            ArrayList<String> list = new ArrayList();
                            for (int a = 0; a < newSeparated.length; a++) {
                                list.add(newSeparated[a]);
                            }
                            menuItem.customFieldObjectMap.patty = new Gson().toJson(list);
                        } catch (Exception ie) {
                            ie.printStackTrace();
                        }
                        e.printStackTrace();
                    }
                    MenuItemDb menuItemDb = new MenuItemDb();
                    menuItemDb.id = menuItem.getId();
                    menuItemDb.name = menuItem.getName(LocaleUtil.getInstance().getLanguage());
                    menuItemDb.menuCategoryId = menuCategoryDb.id;
                    String menuItemDbData = new Gson().toJson(menuItem);
                    menuItemDb.data = menuItemDbData;
                    //TODO: Use file cache only for Lorenzo for now
                    if (BuildUtil.soloAppKey().equalsIgnoreCase("7r5YQwmSgZCtM0ux")) {
                        mainView.saveToCache(menuItem.getId(), menuItemDbData);
                    }
                    menuItemDbs.add(menuItemDb);
                }

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            appDatabase.menuCategoryDao().insertAll(menuCategoryDbs.toArray(new MenuCategoryDb[menuCategoryDbs.size()]));
                        } catch (ArrayIndexOutOfBoundsException e) {
                        }
                        try {
                            appDatabase.menuItemDao().insertAll(menuItemDbs.toArray(new MenuItemDb[menuItemDbs.size()]));
                        } catch (ArrayIndexOutOfBoundsException e) {
                        }
                    }
                });

            }

            CacheUtil.getInstance().setMenuCached(new Date().getTime());
            mainView.proceedMainRestart();

        } catch (JSONException e) {
            e.printStackTrace();
            mainView.proceedRestart();
        }

    }

    public void getMenuCdn() {
        String applicationId = CacheUtil.getInstance().getApplication().getId();
        AppDatabase appDatabase = AppDatabase.getInstance();
        if (!appDatabase.menuCategoryEntityDao().hasByApplication(applicationId)) {
            String url = MenuUtil.getMenuCdnUrl();
            UtilitiesApiCall.getInstance().getMenuCategoriesWithHeaders(url, new ApiRequestWithHeadersListener() {
                @Override

                public void onSuccess(Headers headers, Object o) {
                    try {
                        if(headers.names().contains("last-modified")) {
                            CacheUtil.getInstance().setMenuCdnLastModified(headers.get("last-modified"));
                        }
                        JSONObject object = new JSONObject(o.toString());
                        parseMenu(object);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.onError("");
                    }
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o.toString());
                    mainView.proceedRestart();
                }
            });
        } else {
            appDatabase.menuCategoryDao().deleteByApplication(applicationId);
            appDatabase.menuItemDao().deleteByApplication(applicationId);
            appDatabase.modifierGroupDao().deleteByApplication(applicationId);
            appDatabase.modifierItemDao().deleteByApplication(applicationId);
            appDatabase.ingredientDao().deleteByApplication(applicationId);
            appDatabase.allergenDao().deleteByApplication(applicationId);
            appDatabase.storeDao().deleteByApplication(applicationId);
            appDatabase.facilityDao().deleteByApplication(applicationId);
            appDatabase.menuCategoryEntityDao().deleteByApplication(applicationId);
            appDatabase.menuItemEntityDao().deleteByApplication(applicationId);
            getMenuCdn();
        }
    }

    @Override
    public void changeLanguage() {

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

            UtilitiesApiCall.getInstance().postIdentify(AuthUtil.getInstance().getFirebase(), uniqueID, customerMobileNumber, customerId,
                    new ApiRequestListener() {
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

    private Unit onOrderStatusUpdate(Message message) {
        try {
            JSONObject jsonObject = new JSONObject(message.data.toString());
            OrderData orderData = OrderData.parse(jsonObject.getJSONObject("order"));

            if (orderData.getOrderStatus().getSequence() >= OrderStatusType.ARRIVED_AT_CUSTOMER.getValue()) {
                if (orderData.getOrderStatus().getSequence() <= OrderStatusType.FINISHED.getValue()) {
                    if (CacheUtil.getInstance().getOrderRatings()) {
                        getOrderFeedbackTopics();
                    }
                    mainView.refreshOrderAgain();
                }
                detachAbly();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Unit.INSTANCE;
    }

}
