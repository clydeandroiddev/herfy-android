package com.herfy.cart;

import android.annotation.SuppressLint;
import android.os.Handler;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.herfy.BuildConfig;
import com.herfy.cart.views.CartMenuItemViewHolder;
import com.herfy.model.CampaignToAdd;
import com.herfy.model.CouponToAdd;
import com.herfy.utils.constants.Text;
import com.herfy.database.AppDatabase;
import com.herfy.database.entities.AllergenDb;
import com.herfy.database.entities.IngredientDb;
import com.herfy.database.entities.MenuCategoryEntity;
import com.herfy.database.entities.MenuItemDb;
import com.herfy.database.entities.MenuItemEntity;
import com.herfy.database.entities.ModifierGroupDb;
import com.herfy.database.entities.ModifierItemDb;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.BuildUtil;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.ApplicationsApiCall;
import com.skylinedynamics.solosdk.api.calls.CouponsApiCall;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.calls.IntegrationsApiCall;
import com.skylinedynamics.solosdk.api.calls.LocationApiCall;
import com.skylinedynamics.solosdk.api.calls.MenuApiCall;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.Allergen;
import com.skylinedynamics.solosdk.api.models.objects.AllergenCdn;
import com.skylinedynamics.solosdk.api.models.objects.Application;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.Concept;
import com.skylinedynamics.solosdk.api.models.objects.Customer;
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard;
import com.skylinedynamics.solosdk.api.models.objects.Ingredient;
import com.skylinedynamics.solosdk.api.models.objects.IngredientCdn;
import com.skylinedynamics.solosdk.api.models.objects.Integration;
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemAttributes;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupCdn;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupType;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItemCdn;
import com.skylinedynamics.solosdk.api.models.objects.OrderData;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Promotion;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfig;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfigRule;
import com.skylinedynamics.solosdk.api.models.requestbodies.CreateCustomerAddressBody;
import com.skylinedynamics.solosdk.util.GsonHelper;
import com.herfy.util.ApiLogLevel;
import com.herfy.util.ApiLogUtil;
import com.herfy.util.AppLogger;
import com.herfy.util.ErrorHandler;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.MenuUtil;
import com.herfy.utils.analytics.MixpanelUtil;
import com.herfy.util.StringUtil;
import com.herfy.util.TimeUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import okhttp3.ResponseBody;
import retrofit2.Call;

public class CartPresenter implements CartContract.Presenter {

    private final CartContract.View cartView;

    private ArrayList<MenuItem> menuItems = new ArrayList<>();
    private int menuItemsCount = 0;
    private HashMap<String, MenuItem> disabledMenuItems = new HashMap<>();
    private HashMap<String, MenuItem> unavailableMenuItems = new HashMap<>();

    private String digitalCouponsResponse = "";
    private List<Campaign> digitalCoupons = new ArrayList<>();
    private Call<ResponseBody> digitalCall = null;

    private HashMap<Integer, String> notes = new HashMap<>();
    private Disposable validatePromotionDisposable;
    private Disposable validateDigitalCouponDisposable;
    private double validatedPromoItemPrice = 0.0;
    private int validatedPromoItemQuantity = 0;
    private OrderData validatedOrderData = null;

    public CartPresenter(CartContract.View cartView) {
        this.digitalCall = null;
        this.cartView = cartView;
        this.cartView.setPresenter(this);
    }

    @Override
    public void start() {
        cartView.setupViews();
        cartView.setupFonts();
        cartView.setupTranslations();
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
    public void getApplication(boolean showUpsell) {
        ApplicationsApiCall.getInstance().getApplication(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject jsonObject = new JSONObject(o.toString());
                    JSONObject data = jsonObject.getJSONObject("data");
                    Application application = GsonHelper.gsonFinal().fromJson(data.toString(), new TypeToken<Application>() {
                    }.getType());
                    CacheUtil.getInstance().setApplication(application);
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
                                    CacheUtil.getInstance().setPopup(included.getJSONObject(i));
                                }
                            }
                        }
                    }
                    if (!showUpsell) {
                        cartView.showPayment();
                    } else {
                        cartView.showUpsell();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    if (!showUpsell) {
                        cartView.showPayment();
                    } else {
                        cartView.showUpsell();
                    }
                }
            }

            @Override
            public void onError(Object o) {
                if (!showUpsell) {
                    cartView.showPayment();
                } else {
                    cartView.showUpsell();
                }
            }
        });
    }

    @Override
    public void getConcept() {

        ApplicationsApiCall.getInstance().getApplication(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {

                    JSONObject jsonObject = new JSONObject(o.toString());

                    if (jsonObject.has("included") && !jsonObject.isNull("included")) {
                        JSONArray included = jsonObject.getJSONArray("included");
                        if (included.length() > 0) {
                            for (int i = 0; i < included.length(); i++) {
                                if (included.getJSONObject(i).optString("type").equalsIgnoreCase("concept")) {
                                    Concept concept = new Concept();
                                    concept = concept.parse(included.getJSONObject(i));
                                    CacheUtil.getInstance().setConcept(concept);
                                }
                            }
                            showCoupon();
                        } else {
                            this.onError("");
                        }
                    } else {
                        this.onError("");
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                showCoupon();
            }
        });

        /*ConceptsService.INSTANCE.getConcept(CacheUtil.getInstance().getConceptKey(), new Callback<SoloResponse<SoloResponseData<Concept>>>() {
            @Override
            public void onResponse(Call<SoloResponse<SoloResponseData<Concept>>> call, Response<SoloResponse<SoloResponseData<Concept>>> response) {
                if (response.isSuccessful()) {
                    Concept concept = response.body().getData().getAttributes();
                    concept.setId(response.body().getData().getId());
                    CacheUtil.getInstance().setConcept(concept);
                    getFinancials();
                }
                showCoupon();
            }

            @Override
            public void onFailure(Call<SoloResponse<SoloResponseData<Concept>>> call, Throwable t) {
                AppLogger.printRetrofitError(t);
                showCoupon();
            }
        });*/
    }

    @Override
    public List<Campaign> getCampaignList() {
        return null;
    }

    @Override
    public void getMenuItems() {
        menuItems = CacheUtil.getInstance().getCart();
        if (CacheUtil.getInstance().getConcept().isCartItemsNotesEnabled()) {
            notes.clear();
            for (int i = 0; i < menuItems.size(); i++) {
                notes.put(i, menuItems.get(i).getAttributes().getNotes());
            }
        }
        cartView.showMenuItems(menuItems, false, false);
    }

    @Override
    public void fetchCustomerCards() {
        if (AuthUtil.getInstance().isSignedIn()) {
            CustomersApiCall.getInstance().getSavedCards(AuthUtil.getInstance().getCustomer().getId(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    try {
                        JSONObject jsonObject = new JSONObject(o.toString());
                        List<CustomerCard> cards = GsonHelper.gsonFinal().fromJson(jsonObject.getJSONArray("data").toString(), new TypeToken<List<CustomerCard>>() {
                        }.getType());
                        CacheUtil.getInstance().setCustomerCards(new Gson().toJson(cards));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Object o) {
                }
            });
        }
    }

    @Override
    public void validateOrder(boolean isCouponValidate, boolean checkout, String scheduledTime) {

        Customer customer = AuthUtil.getInstance().getCustomer();
        Concept concept = CacheUtil.getInstance().getConcept();
        menuItems = CacheUtil.getInstance().getCart();
        double subtotal = getMenuItemsTotalPrice();
        if (!checkout) {
            cartView.validatingOrder(subtotal);
        }
        double deliveryCharge = concept.getDeliveryCharge();

        try {
            double minimumOrderAmountFreeDelivery = concept.getMinimumOrderAmountFreeDelivery();
            if (minimumOrderAmountFreeDelivery != 0 && subtotal >= minimumOrderAmountFreeDelivery) {
                deliveryCharge = concept.getSpecialDeliveryPrice();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonObject orderBody = new JsonObject();
        if (AuthUtil.getInstance().isSignedIn()) {
            orderBody.addProperty("customer", customer.getId());
        }

        if (checkout) {
            if (scheduledTime != null) {
                orderBody.addProperty("scheduled-time", scheduledTime);
            }
        }
        String mukafaaTransactionId = CacheUtil.getInstance().getMukafaaTransactionId();
        if (mukafaaTransactionId != null && !mukafaaTransactionId.isEmpty()) {
            JsonObject loyalty = new JsonObject();
            loyalty.addProperty("provider", Values.MUKAFAA_PROVIDER);
            loyalty.addProperty("transaction-id", mukafaaTransactionId);
            orderBody.add("loyalty", loyalty);
        }

        /*if (CacheUtil.getOrderStoreId() != null) {
            orderBody.addProperty("location", CacheUtil.getOrderStoreId());
        }*/
        orderBody.addProperty("source", "Android");
        orderBody.addProperty("notes", CacheUtil.getInstance().getCartNotes());
        orderBody.addProperty("subtotal", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", subtotal)));

        double discount = 0;
        double promoPrice = 0;
        boolean followDelivery = false;
        boolean discountZero = false;

        String couponCode = CacheUtil.getInstance().getCouponCode();
        if (couponCode != null && !couponCode.isEmpty()) {
            orderBody.addProperty("coupon-code", couponCode);
        }

        if (CacheUtil.getInstance().getCampaign() != null && CacheUtil.getInstance().getCampaignPromotion() != null) {
            orderBody.addProperty("campaign-id", CacheUtil.getInstance().getCampaign().getId());
        }

        /*if (discountZero) {
            discount = 0;
            Campaign camp = CacheUtil.getInstance().getCampaign();
            if (camp != null) {
                if (deliveryCharge != 0) {
                    boolean hideDiscount = false;
                    try {
                        if (camp.getPromotion().getAttributes().getType().equals("delivery")) {
                            if (camp.getPromotion().getAttributes().getDiscountType().equals("percentage")) {
                                if (camp.getPromotion().getAttributes().getDiscountValue() >= 100) {
                                    hideDiscount = true;
                                } else {
                                    deliveryCharge = deliveryCharge * (camp.getPromotion().getAttributes().getDiscountValue() / 100);
                                }
                            } else {
                                if (camp.getPromotion().getAttributes().getDiscountValue() >= deliveryCharge) {
                                    hideDiscount = true;
                                } else {
                                    deliveryCharge = deliveryCharge - camp.getPromotion().getAttributes().getDiscountValue();
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (hideDiscount)
                        deliveryCharge = 0;
                }
            }
        }*/

        switch (CacheUtil.getInstance().getOrderType()) {
            case PICKUP:
                deliveryCharge = 0.0;
                if (CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().contains(OrderType.DINE_IN)) {
                    orderBody.addProperty("type", "to-go");
                } else {
                    orderBody.addProperty("type", "pickup");
                }
                if (CacheUtil.getInstance().getOrderStore() != null) {
                    orderBody.addProperty("location", CacheUtil.getInstance().getOrderStore().getId());
                }
                break;
            case DRIVE_THRU:
                deliveryCharge = 0.0;
                orderBody.addProperty("type", "drive-thru");
                if (CacheUtil.getInstance().getOrderStore() != null) {
                    orderBody.addProperty("location", CacheUtil.getInstance().getOrderStore().getId());
                }
                break;
            case EAT_IN:
            case DINE_IN:
                deliveryCharge = 0.0;
                orderBody.addProperty("type", "eat-in");
                if (CacheUtil.getInstance().getOrderStore() != null) {
                    orderBody.addProperty("location", CacheUtil.getInstance().getOrderStore().getId());
                }
                break;
            case DELIVERY:
                orderBody.addProperty("type", "deliver");
                String orderStoreId = CacheUtil.getOrderStoreId();
                Address address = CacheUtil.getInstance().getOrderAddress();
                if (address != null) {
                    orderBody.addProperty("address", address.getId());
                }
                break;
            case CURBSIDE:
                deliveryCharge = 0.0;
                if (CacheUtil.getInstance().getOrderStore() != null) {
                    orderBody.addProperty("location", CacheUtil.getInstance().getOrderStore().getId());
                }
                if (CacheUtil.getInstance().getCurbsideCar() != null) {
                    orderBody.addProperty("type", "curbside");
                    orderBody.addProperty("car-id", CacheUtil.getInstance().getCurbsideCar().id);
                }
                break;
            case NONE:
                deliveryCharge = 0.0;
                break;
        }

        double discountedSubtotal = subtotal - discount;
        if (discountedSubtotal < 0) {
            discountedSubtotal = 0;
            discount = subtotal;
        }

        if (followDelivery) {
            deliveryCharge = 0;
        }

        orderBody.addProperty("discount", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", discount)));

        double vatAmount = 0;
//        if (concept.getVatType() != null && concept.getVatType().equalsIgnoreCase("exclusive")) {
//            vatAmount = (discountedSubtotal + deliveryCharge) * (concept.getVatRate() / 100.0);
//            if (discountZero) {
//                vatAmount = 0;
//            }
//            orderBody.addProperty("vat-amount", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", vatAmount)));
//        }

        double total = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", discountedSubtotal + deliveryCharge + vatAmount + promoPrice));
        if (total < 0) {
            total = 0;
        }

        orderBody.addProperty("total", total);

        JsonArray items = new JsonArray();
        String orderStoreId = CacheUtil.getOrderStoreId();

        for (MenuItem menuItem : menuItems) {

            if (!menuItem.isPromo()) {

                JsonObject item = new JsonObject();
                JsonArray modifiers = new JsonArray();
                JsonArray ingredients = new JsonArray();
                item.addProperty("id", menuItem.getId());
                item.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", menuItem.getPrice(orderStoreId))));
                item.addProperty("quantity", menuItem.getAttributes().getQuantity());
                item.addProperty("is-reorder", menuItem.getAttributes().isReorder());
                item.addProperty("is-promotion", menuItem.isPromo());
                item.addProperty("notes", menuItem.getAttributes().getNotes());

                int comboIndex = 0;
                int trueIndex = 0;
                for (ModifierItem size : menuItem.getSizes().getModifierItems()) {
                    if (size.getAttributes().isSelected()) {
                        JsonObject modifier = new JsonObject();
                        modifier.addProperty("id", size.getId());
                        modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", size.getAttributes().getPrice())));
                        modifier.addProperty("quantity", size.getAttributes().getQuantity());
                        modifiers.add(modifier);
                        trueIndex = comboIndex;
                    }
                    comboIndex++;
                }

                for (ModifierItem drink : menuItem.getDrinks().getModifierItems()) {
                    if (drink.getAttributes().isSelected()) {
                        JsonObject modifier = new JsonObject();
                        modifier.addProperty("id", drink.getId());
                        modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", drink.getAttributes().getPrice())));
                        modifier.addProperty("quantity", drink.getAttributes().getQuantity());
                        modifiers.add(modifier);
                    }
                }

                for (ModifierItem fries : menuItem.getFries().getModifierItems()) {
                    if (fries.getAttributes().isSelected()) {
                        JsonObject modifier = new JsonObject();
                        modifier.addProperty("id", fries.getId());
                        modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", fries.getAttributes().getPrice())));
                        modifier.addProperty("quantity", fries.getAttributes().getQuantity());
                        modifiers.add(modifier);
                    }
                }

                for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                    VisibilityConfig visibilityConfig = modifierGroup.getAttributes().getVisibilityConfig();
                    if (visibilityConfig == null || visibilityConfig.getInitialValue() == null || !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
                        int selectedFreeModifier = 0;
                        for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                            if (modifierItem.getAttributes().isSelected()) {

                                JsonObject freeModifier = null;
                                JsonObject modifier = null;

                                if (modifierGroup.getAttributes().getFreeModifiersCount() > 0 && selectedFreeModifier < modifierGroup.getAttributes().getFreeModifiersCount()) {
                                    int totalSelectedFreeModifier = selectedFreeModifier + modifierItem.getAttributes().getQuantity();
                                    if (totalSelectedFreeModifier <= modifierGroup.getAttributes().getFreeModifiersCount()) {
                                        selectedFreeModifier += modifierItem.getAttributes().getQuantity();
                                        freeModifier = new JsonObject();
                                        freeModifier.addProperty("id", modifierItem.getId());
                                        freeModifier.addProperty("price", 0);
                                        freeModifier.addProperty("quantity", modifierItem.getAttributes().getQuantity());
                                    } else {
                                        selectedFreeModifier = modifierGroup.getAttributes().getFreeModifiersCount();

                                        freeModifier = new JsonObject();
                                        freeModifier.addProperty("id", modifierItem.getId());
                                        freeModifier.addProperty("price", 0);
                                        freeModifier.addProperty("quantity", modifierItem.getAttributes().getQuantity() - (totalSelectedFreeModifier - modifierGroup.getAttributes().getFreeModifiersCount()));

                                        modifier = new JsonObject();
                                        modifier.addProperty("id", modifierItem.getId());
                                        modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", modifierItem.getAttributes().getPrice())));
                                        modifier.addProperty("quantity", totalSelectedFreeModifier - modifierGroup.getAttributes().getFreeModifiersCount());
                                    }
                                } else {
                                    modifier = new JsonObject();
                                    modifier.addProperty("id", modifierItem.getId());
                                    modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", modifierItem.getAttributes().getPrice())));
                                    modifier.addProperty("quantity", modifierItem.getAttributes().getQuantity());
                                }

                                if (freeModifier != null) {
                                    modifiers.add(freeModifier);
                                }

                                if (modifier != null) {
                                    modifiers.add(modifier);
                                }

                            }
                        }
                    }

                }

                for (ModifierItem modifierItem : menuItem.getModifierItems()) {
                    JsonObject modifier = new JsonObject();
                    modifier.addProperty("id", modifierItem.getId());
                    modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", modifierItem.getAttributes().getPrice())));
                    modifier.addProperty("quantity", modifierItem.getAttributes().getQuantity());
                    modifiers.add(modifier);
                }

                for (Ingredient ingredient : menuItem.getIngredients()) {
                    if (!ingredient.getAttributes().isSelected()) {
                        JsonObject ingredientObject = new JsonObject();
                        ingredientObject.addProperty("id", ingredient.getId());
                        ingredientObject.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", ingredient.getAttributes().getPrice())));
                        ingredientObject.addProperty("quantity", ingredient.getAttributes().getQuantity());
                        ingredients.add(ingredientObject);
                    }
                }

                try {
                    if (menuItem.isCombo) {
                        JsonArray comboItems = new JsonArray();
                        for (MenuItemComponents components : menuItem.combo.get(trueIndex).components) {
                            for (ComponentModifierItem compoItem : components.items) {
                                if (compoItem.selected) {
                                    JsonObject comboItemObject = new JsonObject();
                                    comboItemObject.addProperty("id", compoItem.id);
                                    comboItemObject.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", compoItem.price)));
                                    comboItemObject.addProperty("quantity", 1);
                                    comboItemObject.add("ingredients", new JsonArray());
                                    JsonArray modifierCombos = new JsonArray();
                                    if (!compoItem.modifierGroups.isEmpty()) {
                                        for (ModifierGroup modifierGroup : compoItem.modifierGroups) {
                                            for (int i = 0; i < modifierGroup.getModifierItems().size(); i++) {
                                                if (modifierGroup.getModifierItems().get(i).getAttributes().isSelected()) {
                                                    JsonObject comoModItemObject = new JsonObject();
                                                    comoModItemObject.addProperty("id", modifierGroup.getModifierItems().get(i).getId());
                                                    comoModItemObject.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", modifierGroup.getModifierItems().get(i).getAttributes().getPrice())));
                                                    comoModItemObject.addProperty("quantity", modifierGroup.getModifierItems().get(i).getAttributes().getQuantity());
                                                    modifierCombos.add(comoModItemObject);
                                                }
                                            }
                                        }
                                    }
                                    comboItemObject.add("modifiers", modifierCombos);
                                    comboItems.add(comboItemObject);
                                }
                            }
                        }
                        item.add("combo-items", comboItems);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                item.add("ingredients", ingredients);
                item.add("modifiers", modifiers);
                items.add(item);

            }
        }

        orderBody.add("items", items);

        // Cancel older calls
//        if (digitalCall != null && digitalCall.isExecuted()) digitalCall.cancel();
        if (validatePromotionDisposable != null) validatePromotionDisposable.dispose();
        String finalOrderStoreId = orderStoreId;
        validatePromotionDisposable = validatePromotionObservable(orderBody, checkout, isCouponValidate).subscribe(onNext -> {
            boolean isCouponError = false;
            boolean isCouponPromoItem = false;
            try {
                JSONObject jsonObject = (JSONObject) onNext;

                JSONArray itemsArray = jsonObject.getJSONArray("items");
                boolean hasChanged = false;
                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject itemObject = itemsArray.getJSONObject(i);
                    MenuItem item = GsonHelper.gsonFinal().fromJson(itemObject.toString(), MenuItem.class);
                    validatedPromoItemPrice = 0.0;
                    validatedPromoItemQuantity = 0;
                    for (MenuItem menuItem : menuItems) {
                        if (item.getIsPromotion()) {
                            validatedPromoItemPrice = item.getPrice();
                            validatedPromoItemQuantity = item.getQuantity();
                        }
                        if (!item.getIsPromotion() && menuItem.getId().equals(item.getId())) {
                            if (BuildUtil.isEkfc() && (item.getTotal() != item.getPrice() || getMenuItemTotalPrice(menuItems.get(menuItems.indexOf(menuItem)), -1) != item.getTotal())) {
                                menuItems.get(menuItems.indexOf(menuItem)).setTotal(item.getTotal());
                            } else {
                                menuItem.setPrice(item.getPrice(null));
                                menuItem.getAttributes().setPrice(item.getPrice(null));
                            }
                            hasChanged = true;
                        }
                    }
                }

                if (hasChanged) {
                    CacheUtil.getInstance().setCart(menuItems);
                    cartView.updateMenuItems(menuItems);
                }
                double mokafaaAmount = 0;
                if (jsonObject.has("loyalty")) {
                    JSONObject loyalty = jsonObject.getJSONObject("loyalty");
                    if (loyalty.has("amount")) {
                        mokafaaAmount = loyalty.getDouble("amount");
                        setMukafaaTransactionAmount(loyalty.getDouble("amount"));
                    }
                }

                boolean isInclusive = false;

                if (concept.getVatType() != null && concept.getVatType().equalsIgnoreCase("inclusive")) {
                    isInclusive = concept.getVatType().equalsIgnoreCase("inclusive");
                }

                boolean hasError = jsonObject.has("error") && !jsonObject.isNull("error");
                boolean hasPromotion = !hasError && CacheUtil.getInstance().hasPromotion();

                OrderData orderData = OrderData.parse(jsonObject);
                cartView.setTax(isInclusive, StringUtil.getFormattedPrice(orderData.getVatAmount()), orderData.getVatAmount());
                if (orderData.getDiscount() > 0) {
                    cartView.setOriginalPrice(hasPromotion, orderData.getSubtotal() + orderData.getDeliverCharge());
                }
                showPrice(hasPromotion, orderData, mokafaaAmount);

                if (hasError) {
                    //if (isCouponValidate) {
                    isCouponError = true;
                    CacheUtil.getInstance().setCouponCode(null);
                    CacheUtil.getInstance().setCouponName(null);
                    CacheUtil.getInstance().setCouponMenuItem(null);
                    CacheUtil.getInstance().setCouponToAdd(null);
                    cartView.removeCoupon();
                    digitalCoupons.clear();
                    CacheUtil.getInstance().setDigitalCoupons("");
                    CacheUtil.getInstance().setCampaign(null);
                    CacheUtil.getInstance().setCampaignPromotion(null);
                    CacheUtil.getInstance().setCampaignPromotionMenuItem(null);
                    CacheUtil.getInstance().setCampaignToAdd(null);
                    cartView.showDigitalCoupon(null);
                    removeMukafaaRedemption();
                    //}
                    cartView.showPromoError(jsonObject.getString("error"));
                } else {

                    if (CacheUtil.getInstance().getCouponCode() != null && orderData.getPromotionName() != null || orderData.getPromotionName() == null && !orderData.getPromotionName().isEmpty()) {
                        if (isCouponValidate) {
                            MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_APPLY_PROMO);
                        }
                        CacheUtil.getInstance().setCouponName(orderData.getPromotionName());
                        boolean isFreeItem = false;
                        for (int i = 0; i < itemsArray.length(); i++) {
                            JSONObject itemObject = itemsArray.getJSONObject(i);
                            MenuItem item = GsonHelper.gsonFinal().fromJson(itemObject.toString(), MenuItem.class);
                            if (item.getIsPromotion()) {
                                isFreeItem = true;
                                if (isCouponValidate || CacheUtil.getInstance().getCouponMenuItem() == null) {
                                    getCouponMenuItem(item.getId(), item.getQuantity(), item.getPrice(), null, null);
                                } else {
                                    String couponName = CacheUtil.getInstance().getCouponName();
                                    if (couponName != null) {
                                        cartView.showCoupon(couponCode, couponName);
                                        cartView.addPromotionItem(CacheUtil.getInstance().getCouponMenuItem());
                                    }
                                }
                                break;
                            }
                        }
                        if (!isFreeItem) {
                            showCoupon();
                        }
                    } else {
                        CacheUtil.getInstance().setCouponCode(null);
                        CacheUtil.getInstance().setCouponName(null);
                    }
                    if (checkout) {
                        if (CacheUtil.getInstance().getUpsell()) {
                            getUpsells();
                        } else {
                            //put cartPresenter.showPayment in this method
                            getApplication(false);
                        }
                    } else {
                        cartView.dismissProgressDialog();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, onError -> {
        });

    }

    private void showPrice(boolean hasPromotion, OrderData orderData, double mokafaaAmount) {
        double promoItemPrice = 0.0;
        double subtotalForDisplay = orderData.getSubtotal() - (validatedPromoItemPrice * validatedPromoItemQuantity);
        double totalForDisplay = orderData.getTotal() - (validatedPromoItemPrice * validatedPromoItemQuantity);
        if (CacheUtil.getInstance().getCouponMenuItem() != null) {
            promoItemPrice = getMenuItemTotalPrice(CacheUtil.getInstance().getCouponMenuItem(), -1);
        } else if (CacheUtil.getInstance().getCampaignPromotionMenuItem() != null) {
            promoItemPrice = getMenuItemTotalPrice(CacheUtil.getInstance().getCampaignPromotionMenuItem(), -1);
        }
        subtotalForDisplay += (promoItemPrice * validatedPromoItemQuantity);
        totalForDisplay += (promoItemPrice * validatedPromoItemQuantity);
        cartView.setTotalPrice(hasPromotion, subtotalForDisplay, orderData.getDiscountDisplay(), totalForDisplay, CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY, orderData.getDeliverCharge(), orderData.getCharges(), mokafaaAmount);
    }

    // TODO: remove side effects
    public Observable<Object> validatePromotionObservable(JsonObject orderBody, Boolean checkout, Boolean isCouponValidate) {
        return Observable.create(subscriber -> {
            CouponsApiCall.getInstance().validatePromotion(orderBody, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    subscriber.onNext(o);
                    subscriber.onComplete();
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    cartView.dismissProgressDialog();
                    cartView.setTotalPrice(CacheUtil.getInstance().hasPromotion(), getMenuItemsTotalPrice(), 0.0, getMenuItemsTotalPrice(), CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY, 0.0, new ArrayList<>(), 0.0);
                    if (checkout || isCouponValidate) {
                        if (o instanceof JSONObject) {
                            try {
                                JSONObject object = (JSONObject) o;
                                if (object.has("error") && !object.isNull("error")) {
                                    cartView.showError(ErrorHandler.getErrorMessage(object, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
                                } else if (object.has("messages") && !object.isNull("messages")) {
                                    Object messages = object.get("messages");
                                    if (messages instanceof JSONObject) {
                                        JSONObject message = (JSONObject) messages;
                                        List<String> errorList = new ArrayList<>();
                                        Iterator<String> keys = message.keys();
                                        while (keys.hasNext()) {
                                            String key = keys.next();
                                            if (message.get(key) instanceof JSONArray) {
                                                JSONArray array = (JSONArray) message.get(key);
                                                Type type = new TypeToken<List<String>>() {
                                                }.getType();
                                                List<String> error = GsonHelper.gsonFinal().fromJson(array.toString(), type);
                                                errorList.addAll(error);
                                            }
                                        }
                                        cartView.showValidateOrderError(errorList);
                                    }
                                } else {
                                    cartView.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                                }
                                sendLog(ApiLogLevel.ERROR, object.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                                cartView.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                            }
                        } else {
                            cartView.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                        }
                    }
                }
            });
        });
    }

    @Override
    public void getStore() {
        Store store = CacheUtil.getInstance().getOrderStore();
        if (store != null) {

            LocationApiCall.getInstance().getStoreDetails(store.getId(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    if (o instanceof JSONObject) {
                        try {
                            JSONObject jsonObject = (JSONObject) o;
                            JSONObject data = jsonObject.getJSONObject("data");

                            Store newStore = Store.parseStore(data);
                            CacheUtil.getInstance().setOrderStore(newStore);

                            cartView.showStore(newStore);

                        } catch (JSONException e) {
                            e.printStackTrace();
                            this.onError(e);
                        }
                    } else {
                        this.onError("");
                    }
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    cartView.showStore(store);
                }
            });
        }
    }

    @Override
    public void getAddress() {
        Address address = CacheUtil.getInstance().getOrderAddress();
        if (AuthUtil.getInstance().isSignedIn() && address != null) {
            CustomersApiCall.getInstance().getCustomerAddress(AuthUtil.getInstance().getCustomer().getId(), address.getId(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {

                    if (o instanceof JSONObject) {

                        try {
                            JSONObject jsonObject = (JSONObject) o;
                            JSONObject data = jsonObject.getJSONObject("data");

                            Address newAddress = Address.parseAddress(data);
                            CacheUtil.getInstance().setOrderAddress(newAddress);

                            cartView.showAddress(address.getAttributes().getLine1());

                        } catch (JSONException e) {
                            e.printStackTrace();
                            this.onError(e);
                        }
                    } else {
                        this.onError("");
                    }

                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    cartView.showAddress(address.getAttributes().getLine1());
                }
            });
        } else if (CacheUtil.getInstance().getOrderDeliveryAddress() != null) {
            cartView.showAddress(CacheUtil.getInstance().getOrderDeliveryAddress());
        }
    }

    @Override
    public void onBindMenuItemViewAtPosition(int position, CartMenuItemViewHolder cartMenuItemViewHolder) {
        cartMenuItemViewHolder.setMenuItem(menuItems.get(position));
    }

    @Override
    public int getMenuItemsCount() {
        return menuItems.size();
    }

    @Override
    public double getMenuItemTotalPrice(MenuItem item, int modifierGroupPosition) {
        String orderStoreId = null;
        int counter = 0;
        int comboIndex = 0;
        if (CacheUtil.getInstance().getOrderStore() != null) {
            orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
        }

        double totalPrice = item.getPrice(orderStoreId);
        if (BuildUtil.isEkfc() && item.getTotal() != 0) {
            totalPrice = item.getTotal();
        } else {
            if (item.getSizes().getModifierItems().size() > 0) {
                if (!item.isPromo()) {
                    totalPrice = 0.0;
                }
                for (ModifierItem modifierItem : item.getSizes().getModifierItems()) {
                    if (modifierItem.getAttributes().isSelected()) {
                        totalPrice += modifierItem.getAttributes().getPrice();
                        comboIndex = counter;
                        break;
                    }
                    counter++;
                }
            }
            for (ModifierItem modifierItem : item.getDrinks().getModifierItems()) {
                if (modifierItem.getAttributes().isSelected()) {
                    totalPrice += modifierItem.getAttributes().getPrice();
                    break;
                }
            }
            for (ModifierItem modifierItem : item.getFries().getModifierItems()) {
                if (modifierItem.getAttributes().isSelected()) {
                    totalPrice += modifierItem.getAttributes().getPrice();
                    break;
                }
            }
            for (ModifierGroup modifierGroup : item.getModifierGroups()) {
                VisibilityConfig visibilityConfig = modifierGroup.getAttributes().getVisibilityConfig();
                if (visibilityConfig == null || visibilityConfig.getInitialValue() == null || !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {

                    int selectedFreeModifier = 0;
                    for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                        if (modifierItem.getAttributes().isSelected()) {
                            if (modifierGroup.getAttributes().getFreeModifiersCount() > 0 && selectedFreeModifier < modifierGroup.getAttributes().getFreeModifiersCount()) {
                                int totalSelectedFreeModifier = selectedFreeModifier + modifierItem.getAttributes().getQuantity();
                                if (totalSelectedFreeModifier < modifierGroup.getAttributes().getFreeModifiersCount()) {
                                    selectedFreeModifier += modifierItem.getAttributes().getQuantity();
                                } else {
                                    selectedFreeModifier = modifierGroup.getAttributes().getFreeModifiersCount();
                                    totalPrice += modifierItem.getAttributes().getPrice() * (totalSelectedFreeModifier - modifierGroup.getAttributes().getFreeModifiersCount());
                                }
                            } else {
                                totalPrice += modifierItem.getAttributes().getPrice() * modifierItem.getAttributes().getQuantity();
                            }
                        }
                    }

                }
            }
            for (Ingredient ingredient : item.getIngredients()) {
                if (ingredient.getAttributes().isSelected()) {
                    totalPrice += ingredient.getAttributes().getPrice();
                }
            }

            try {
                if (item.isCombo) {
                    for (MenuItemComponents components : item.combo.get(comboIndex).components) {
                        for (ComponentModifierItem componentModifierItem : components.items) {
                            if (componentModifierItem.selected) {
                                if (!item.isSandwichOnly) totalPrice += componentModifierItem.price;
                                if (!componentModifierItem.modifierGroups.isEmpty()) {
                                    for (ModifierGroup modifierGroup : componentModifierItem.modifierGroups) {
                                        for (int i = 0; i < modifierGroup.getModifierItems().size(); i++) {
                                            if (modifierGroup.getModifierItems().get(i).getAttributes().isSelected()) {
                                                totalPrice += (modifierGroup.getModifierItems().get(i).getAttributes().getPrice() * modifierGroup.getModifierItems().get(i).getAttributes().getQuantity());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        return totalPrice;
    }

    @Override
    public double getMenuItemsTotalPrice() {
        double subtotalPrice = 0.0;
        for (MenuItem menuItem : menuItems) {
            subtotalPrice += getMenuItemTotalPrice(menuItem, -1) * menuItem.getAttributes().getQuantity();
        }
        return subtotalPrice;
    }

    @Override
    public void getTotalPrice(boolean validate, boolean isCouponValidate) {
        validateOrder(isCouponValidate, false, null);
    }

    @Override
    public void changeQuantity(int position, boolean add) {

        if (add) {
            MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_INCREASE_ITEM_QUANTITY_CART);
        } else {
            MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_DECREASE_ITEM_QUANTITY_CART);
        }

        MenuItem menuItem = menuItems.get(position);
        boolean addingItem = false;

        if (!add && menuItem.getAttributes().getQuantity() != 1) {
            menuItem.getAttributes().setQuantity(menuItem.getAttributes().getQuantity() - 1);
            if (menuItem.getAttributes().isUpsell() && menuItem.getAttributes().getUpsellQuantity() != 1) {
                menuItem.getAttributes().setUpsellQuantity(menuItem.getAttributes().getUpsellQuantity() - 1);
            }
        } else if (add) {
            addingItem = true;
            menuItem.getAttributes().setQuantity(menuItem.getAttributes().getQuantity() + 1);
            if (menuItem.getAttributes().isUpsell()) {
                menuItem.getAttributes().setUpsellQuantity(menuItem.getAttributes().getUpsellQuantity() + 1);
            }
        }

        removeMukafaaRedemption();
        CacheUtil.getInstance().setCart(menuItems);

        getTotalPrice(Values.VALIDATE_QUANTITY, false);
        cartView.refreshMenuItems(position, true);

        if (!Values.NO_PROMOTIONS) {
            if (CacheUtil.getInstance().getCampaign() != null)
                validateDigitalCoupon(false, CacheUtil.getInstance().getCampaign());
        }
    }

    @Override
    public void deleteCartItem(boolean delete, int position) {
        if (!delete) {
            cartView.deleteCartItem(position);
        } else if (position >= 0 && position < menuItems.size()) {
            MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_REMOVE_FROM_CART);
            if (menuItems.get(position).isPromo()) {
                removeCoupon();
            } else {
                MenuItem removeItem = menuItems.get(position);
                cartView.removeItemEvent(removeItem, getMenuItemTotalPrice(menuItems.get(position), -1) * removeItem.getAttributes().getQuantity());
                menuItems.remove(position);
                removeMukafaaRedemption();
                CacheUtil.getInstance().setCart(menuItems);
                if (menuItems.size() == 0) {
                    CacheUtil.getInstance().setUpsell(true);
                    removeCoupon();
                    digitalCoupons.clear();
                    CacheUtil.getInstance().setDigitalCoupons("");
                    CacheUtil.getInstance().setCampaign(null);
                    CacheUtil.getInstance().setCampaignPromotion(null);
                    CacheUtil.getInstance().setCampaignPromotionMenuItem(null);
                    cartView.showDigitalCoupon(null);
                    cartView.setTotalPrice(false, 0, 0, 0, CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY, 0, new ArrayList<>(), 0);
                } else {
                    cartView.refreshMenuItems(-1, true);
                    getTotalPrice(true, true);
                }

            }
        }
    }

    @Override
    public void editCartItem(int position) {
        MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_EDIT_ITEM_FROM_CART);
        cartView.editCartItem(position);
    }

    @Override
    public void getCoupon(String couponCode) {
        if (!couponCode.isEmpty()) {
            CacheUtil.getInstance().setCouponCode(couponCode);
            validateOrder(true, false, null);
            /*CouponsApiCall.getInstance().getCoupon(couponCode, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    try {
                        JSONObject object = new JSONObject(o.toString());
                        if (object.has("data") && !object.isNull("data")) {
                            JSONArray dataArray = object.getJSONArray("data");
                            if (dataArray.length() > 0) {
                                if (object.has("included") && !object.isNull("included")) {
                                    JSONArray includedArray = object.getJSONArray("included");
                                    if (includedArray.length() > 0) {
                                        Coupon coupon = GsonHelper.gsonFinal().fromJson(dataArray.getJSONObject(0).toString(), new TypeToken<Coupon>() {
                                        }.getType());
                                        Promotion promotion = GsonHelper.gsonFinal().fromJson(includedArray.getJSONObject(0).toString(), new TypeToken<Promotion>() {
                                        }.getType());

                                        CacheUtil.getInstance().setCoupon(coupon);
                                        CacheUtil.getInstance().setPromotion(promotion);

                                        String type = promotion.getAttributes().getType().toLowerCase();
                                        if (type.equalsIgnoreCase("item") && promotion.getAttributes().getItemId() != null) {
                                            getPromotionMenuItem();
                                        } else if (type.equalsIgnoreCase("discount")) {
                                            validateOrder(true, false, null);
                                        } else {
                                            validateOrder(true, false, null);
                                        }
                                    } else {
                                        this.onError("");
                                    }
                                } else {
                                    this.onError("");
                                }
                            } else {
                                this.onError("");
                            }
                        } else {
                            this.onError("");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.onError(e);
                    }
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    Coupon coupon = new Coupon();
                    coupon.getAttributes().setCode(couponCode);
                    CacheUtil.getInstance().setCoupon(coupon);
                    validateOrder(true, false, null);
                }
            });*/
        } else {
            cartView.showMessage(CacheUtil.getInstance().getTranslations(Text.PLEASE_INPUT_A_COUPON_CODE_FIRST, "Coupon code is required"));
        }
    }

    @Override
    public void showCoupon() {
        String couponCode = CacheUtil.getInstance().getCouponCode();
        String couponName = CacheUtil.getInstance().getCouponName();
        if (couponCode != null && couponName != null) {
            cartView.showCoupon(couponCode, couponName);
        } else {
            cartView.removeCoupon();
        }
    }

    @Override
    public void getCouponMenuItem(String menuItemId, int quantity, double price, @Nullable MenuItemAttributes attributes, @Nullable Campaign campaign) {
        MenuApiCall.getInstance().getItemDetails(menuItemId, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    JSONArray dataArray = object.getJSONArray("data");
                    if (dataArray.length() > 0) {
                        MenuItem menuItem = GsonHelper.gsonFinal().fromJson(dataArray.getJSONObject(0).toString(), MenuItem.class);
                        menuItem.setPromo(true);
                        if (quantity > 0) {
                            menuItem.getAttributes().setQuantity(quantity);
                        } else {
                            menuItem.getAttributes().setQuantity(1);
                        }
                        menuItem.getAttributes().setPrice(price);
                        getMenuItem(menuItem, attributes, campaign);
                    } else {
                        this.onError("");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                removeCoupon();
                cartView.showError(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(Text.COUPON_DEFAULT_ERROR, "Sorry but the coupon code you have entered is invalid. It may have already expired or the coupon is erroneous.")));
            }
        });
    }


    private MenuItem promoMenuItem = new MenuItem();

    @Nullable
    String getMenuItemData(AppDatabase appDatabase, String id) {
        String data = "";
        try {
            //TODO: Use file cache only for Lorenzo for now
            if (BuildUtil.soloAppKey().equalsIgnoreCase("7r5YQwmSgZCtM0ux")) {
                data = cartView.getFromCache(id);
            } else {
                String applicationId = CacheUtil.getInstance().getApplication().getId();
                MenuItemDb menuItemDb = appDatabase.menuItemDao().findByApplicationAndId(applicationId, id);
                data = menuItemDb.data;
            }
        } catch (Exception e) {
            data = cartView.getFromCache(id);
        }
        return data;
    }

    public void getMenuItem(MenuItem menuItem, @Nullable MenuItemAttributes attributes, @Nullable Campaign campaign) {
        Executor executor = Executors.newSingleThreadExecutor();

        AppDatabase appDatabase = AppDatabase.getInstance();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                String data = getMenuItemData(appDatabase, menuItem.getId());
                if (data != null && !data.isEmpty()) {
                    String applicationId = CacheUtil.getInstance().getApplication().getId();
                    promoMenuItem = MenuUtil.processMenuItemCustomObject(GsonHelper.gsonFinal().fromJson(data, MenuItem.class));
                    if (attributes != null && campaign != null) {
                        menuItem.getAttributes().setPrice(attributes.getPrice());
                        menuItem.getAttributes().setQuantity(attributes.getQuantity());
                    }
                    LinkedList<Allergen> allergens = new LinkedList<>();
                    for (AllergenDb allergenDb : appDatabase.allergenDao().findAllByApplicationAndIds(applicationId, promoMenuItem.getAllergensIds().toArray(new String[promoMenuItem.getAllergensIds().size()]))) {
                        AllergenCdn allergenCdn = GsonHelper.gsonFinal().fromJson(allergenDb.data, AllergenCdn.class);
                        allergens.add(AllergenCdn.convert(allergenCdn, LocaleUtil.getInstance().getLanguage()));
                    }
                    Collections.sort(allergens, new Comparator<Allergen>() {
                        @Override
                        public int compare(Allergen allergen1, Allergen allergen2) {
                            return allergen1.getAttributes().getDisplayOrder() - allergen2.getAttributes().getDisplayOrder();
                        }
                    });
                    menuItem.setAllergens(allergens);
                    LinkedList<Ingredient> ingredients = new LinkedList<>();
                    HashMap<String, Ingredient> ingredientIds = new HashMap<>();
                    for (Ingredient ingredient : promoMenuItem.getIngredientIds()) {
                        ingredientIds.put(ingredient.getIngredientId(), ingredient);
                    }
                    for (IngredientDb ingredientDb : appDatabase.ingredientDao().findAllByApplicationAndIds(applicationId, ingredientIds.keySet().toArray(new String[ingredientIds.size()]))) {
                        IngredientCdn ingredientCdn = GsonHelper.gsonFinal().fromJson(ingredientDb.data, IngredientCdn.class);
                        Ingredient ingredient = IngredientCdn.convert(ingredientCdn, LocaleUtil.getInstance().getLanguage());
                        ingredient.setId(ingredientIds.get(ingredientCdn.getId()).getId());
                        ingredients.add(ingredient);
                    }
                    Collections.sort(ingredients, new Comparator<Ingredient>() {
                        @Override
                        public int compare(Ingredient ingredient1, Ingredient ingredient2) {
                            return ingredient1.getAttributes().getDisplayOrder() - ingredient2.getAttributes().getDisplayOrder();
                        }
                    });
                    menuItem.setIngredients(ingredients);

                    //menuItemModifierGroups.clear();

                    ModifierGroup sizes = new ModifierGroup();
                    LinkedList<ModifierGroup> modifierGroups = new LinkedList<>();
                    HashMap<String, ModifierGroup> modifierGroupIds = new HashMap<>();
                    HashMap<String, Integer> modifierGroupDisplayOrders = new HashMap<>();
                    for (ModifierGroup modifierGroup : promoMenuItem.getModifierGroupIds()) {
                        modifierGroupIds.put(modifierGroup.getId(), modifierGroup);
                        //modifierGroupDisplayOrders.put(modifierGroup.getId(), menuItem.getModifierGroupIds().indexOf(modifierGroup));
                    }
                    for (ModifierGroupDb modifierGroupDb : appDatabase.modifierGroupDao().findAllByApplicationAndIds(applicationId, modifierGroupIds.keySet().toArray(new String[modifierGroupIds.size()]))) {
                        ModifierGroupCdn modifierGroupCdn = MenuUtil.processModifierGroupCustomObject(GsonHelper.gsonFinal().fromJson(modifierGroupDb.data, ModifierGroupCdn.class));
                        LinkedList<ModifierItem> modifierItems = new LinkedList<>();
                        ArrayList<ModifierItem> modifierGroupModifierItems = modifierGroupIds.get(modifierGroupCdn.getId()).getModifiers();
                        ArrayList<String> modifierGroupModifierItemIds = new ArrayList<>();
                        ArrayList<Integer> excludeModifierItems = modifierGroupIds.get(modifierGroupCdn.getId()).getExcludeModifiers();
                        Set<String> excludedModifierItems = new HashSet<>();
                        for (Integer exclude : excludeModifierItems) {
                            excludedModifierItems.add(String.valueOf(exclude));
                        }

                        // Exclude by location
                        Store orderStore = CacheUtil.getInstance().getOrderStore();
                        List<String> excludedItemsByLocationId = Observable.fromArray(modifierGroupIds.get(modifierGroupCdn.getId())).map(modifierGroup -> {
                            if (orderStore == null) return new ArrayList<ModifierItem>();
                            // if group contains store id in included location, add all modifiers within it to the excluded items
                            // else check modifier item excluded location if it contains store id
                            if (Observable.fromIterable(modifierGroup.getExcludedLocations()).map(Object::toString).any(excludedLocation -> excludedLocation.equals(orderStore.getId())).blockingGet()) {
                                return modifierGroup.getModifiers();
                            } else {
                                return Observable.fromIterable(modifierGroup.getModifierItems()).filter(modifierItem -> Observable.fromIterable(modifierItem.getAttributes().getExcludedLocations()).any(item -> item.toString().equals(orderStore.getId())).blockingGet()).toList().blockingGet();
                            }
                        }).flatMap(Observable::fromIterable).map(ModifierItem::getId).toList().blockingGet();

                        excludedModifierItems.addAll(excludedItemsByLocationId);

                        HashMap<String, Integer> modifierGroupModifierItemDisplayOrders = new HashMap<>();
                        for (ModifierItem modifierGroupModifierItem : modifierGroupModifierItems) {
                            if (!excludedModifierItems.contains(modifierGroupModifierItem.getId())) {
                                modifierGroupModifierItemIds.add(modifierGroupModifierItem.getId());
                                modifierGroupModifierItemDisplayOrders.put(modifierGroupModifierItem.getId(), modifierGroupModifierItems.indexOf(modifierGroupModifierItem));
                            }
                        }
                        for (ModifierItemDb modifierItemDb : appDatabase.modifierItemDao().findAllByApplicationAndIds(applicationId, modifierGroupModifierItemIds.toArray(new String[modifierGroupModifierItemIds.size()]))) {
                            ModifierItemCdn modifierItemCdn = GsonHelper.gsonFinal().fromJson(modifierItemDb.data, ModifierItemCdn.class);
                            ModifierItem modifierItem = ModifierItemCdn.convert(modifierItemCdn, LocaleUtil.getInstance().getLanguage());
                            modifierItem.getAttributes().setSelected(modifierItem.getAttributes().getMinimum() == 1);
                            if (modifierGroupModifierItemDisplayOrders.containsKey(modifierItem.getId()) && modifierGroupModifierItemDisplayOrders.get(modifierItem.getId()) != null) {
                                modifierItem.getAttributes().setDisplayOrder(modifierGroupModifierItemDisplayOrders.get(modifierItem.getId()));
                            }
                            boolean isExcluded = orderStore != null && Observable.fromIterable(modifierItem.getAttributes().getExcludedLocations()).any(item -> item.toString().equals(orderStore.getId())).blockingGet();
                            if (!isExcluded) {
                                modifierItems.add(modifierItem);
                            }
                        }
                        Collections.sort(modifierItems, new Comparator<ModifierItem>() {
                            @Override
                            public int compare(ModifierItem modifierItem1, ModifierItem modifierItem2) {
                                return modifierItem1.getAttributes().getDisplayOrder() - modifierItem2.getAttributes().getDisplayOrder();
                            }
                        });
                        ModifierGroup modifierGroup = ModifierGroupCdn.convert(modifierGroupCdn, LocaleUtil.getInstance().getLanguage());
                        ModifierGroup mapModifierGroup = modifierGroupIds.get(modifierGroup.getId());
                        if (mapModifierGroup != null) {
                            modifierGroup.getAttributes().setMinimum(mapModifierGroup.getMin());
                            modifierGroup.getAttributes().setMaximum(mapModifierGroup.getMax());
                            modifierGroup.getAttributes().setDisplayOrder(mapModifierGroup.getDisplayOrder());
                            modifierGroup.getAttributes().setFreeModifiersCount(mapModifierGroup.getFreeModifiersCount());
                            modifierGroup.setDefaultModifiers(new ArrayList<>(mapModifierGroup.getDefaultModifiers()));
                            modifierGroup.getAttributes().setVisibilityConfig(mapModifierGroup.getVisibilityConfig());
                        }
                    /*if(modifierGroupDisplayOrders.containsKey(modifierGroup.getId()) && modifierGroupDisplayOrders.get(modifierGroup.getId()) != null) {
                        modifierGroup.getAttributes().setDisplayOrder(modifierGroupDisplayOrders.get(modifierGroup.getId()));
                    }*/
                        modifierGroup.setModifierItems(modifierItems);

                        //hamburgini ingredients handling
                        if (BuildConfig.SOLO_CONCEPT == "1") {

                            for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                                for (String modifierId : modifierGroup.getDefaultModifiers()) {
                                    if (modifierItem.getId().equalsIgnoreCase(modifierId)) {
                                        modifierItem.getAttributes().setQuantity(1);
                                        modifierItem.getAttributes().setSelected(true);
                                    }
                                }
                            }

                        }


                        if (modifierGroupDb.type == ModifierGroupType.SIZES.getValue()) {
                            sizes = modifierGroup;
                        } else {
                            //menuItemModifierGroups.put(modifierGroup.getId(), modifierGroup);
                            modifierGroups.add(modifierGroup);
                        }
                    }
                    menuItem.setSizes(sizes);
                    Collections.sort(modifierGroups, new Comparator<ModifierGroup>() {
                        @Override
                        public int compare(ModifierGroup modifierGroup1, ModifierGroup modifierGroup2) {
                            return modifierGroup1.getAttributes().getDisplayOrder() - modifierGroup2.getAttributes().getDisplayOrder();
                        }
                    });

                    menuItem.setModifierGroups(modifierGroups);
                    prepareMenuItem(menuItem, campaign);

                } else {
                    String orderStoreId = null;
                    if (CacheUtil.getInstance().getOrderStore() != null) {
                        orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
                    }
                    menuItem.toCdn(orderStoreId);
                    menuItem.setPromo(true);
                    CacheUtil.getInstance().setCouponMenuItem(menuItem);
//                    menuView.showError(CacheUtil.getInstance().getTranslations(Text.ITEM_NO_LONGER_AVAILABLE));
                }
            }
        });

    }

    public void prepareMenuItem(MenuItem menuItem, @Nullable Campaign campaign) {
        LinkedList<ModifierGroup> filteredModifierGroups = new LinkedList<>();

        for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
            if (modifierGroup.getModifierItems().size() > 0) {
                filteredModifierGroups.add(modifierGroup);
            }
        }

        if (menuItem.getSizes().getModifierItems().size() > 0) {
            menuItem.getSizes().getModifierItems().get(0).getAttributes().setSelected(true);
        }

        menuItem.setModifierGroups(filteredModifierGroups);

        for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
            Set<String> defaultModifiers = modifierGroup.getDefaultModifiers();
            if (modifierGroup.getModifierItems().size() > 0 && modifierGroup.getAttributes().getMinimum() > 0) {
                if (defaultModifiers.size() > 0) {
                    int selected = 0;
                    for (int i = 0; i < modifierGroup.getModifierItems().size(); i++) {
                        ModifierItem modifierItem = modifierGroup.getModifierItems().get(i);
                        if (selected < modifierGroup.getAttributes().getMaximum() && defaultModifiers.contains(modifierItem.getId())) {
                            modifierGroup.getModifierItems().get(i).getAttributes().setSelected(true);
                            selected++;
                        }
                    }
                } else if (modifierGroup.getAttributes().getMinimum() == 1) {
                    modifierGroup.getModifierItems().get(0).getAttributes().setSelected(true);
                }
            }

        }

        VisibilityConfig visibilityConfig = menuItem.getSizes().getAttributes().getVisibilityConfig();
        if (visibilityConfig != null) {
            List<VisibilityConfigRule> rules = visibilityConfig.getRules();
            if (rules != null && rules.size() > 0) {
                VisibilityConfigRule rule = rules.get(0);
                if (rule.getTargetType() != null && rule.getTargetType().equalsIgnoreCase("modifier-group")) {
                    for (ModifierGroup modGroup : menuItem.getModifierGroups()) {
                        if (!menuItem.getSizes().getId().equals(modGroup.getId()) && rule.getTargetId().equals(modGroup.getId())) {
                            if (rule.getTargetIs() != null && rule.getTargetIs().equalsIgnoreCase("selected")) {
                                int selected = 0;
                                for (ModifierItem modItem : modGroup.getModifierItems()) {
                                    if (modItem.getAttributes().isSelected()) {
                                        selected += 1;
                                    }
                                }
                                if (selected > 0) {
                                    menuItem.getSizes().getAttributes().getVisibilityConfig().setInitialValue("");
                                } else {
                                    menuItem.getSizes().getAttributes().getVisibilityConfig().setInitialValue("hidden");
                                }
                            }
                        }

                    }
                }

            }
        }

        for (ModifierGroup group : menuItem.getModifierGroups()) {
            visibilityConfig = group.getAttributes().getVisibilityConfig();
            if (visibilityConfig != null) {
                List<VisibilityConfigRule> rules = visibilityConfig.getRules();
                if (rules != null && rules.size() > 0) {
                    VisibilityConfigRule rule = rules.get(0);
                    if (rule.getTargetType() != null) {
                        if (rule.getTargetType().equalsIgnoreCase("modifier-group")) {
                            if (!group.getId().equals(menuItem.getSizes().getId()) && rule.getTargetId().equals(menuItem.getSizes().getId())) {
                                if (rule.getTargetIs() != null && rule.getTargetIs().equalsIgnoreCase("selected")) {
                                    int selected = 0;
                                    for (ModifierItem modItem : menuItem.getSizes().getModifierItems()) {
                                        if (modItem.getAttributes().isSelected()) {
                                            selected += 1;
                                        }
                                    }
                                    if (selected > 0) {
                                        group.getAttributes().getVisibilityConfig().setInitialValue("");
                                    } else {
                                        group.getAttributes().getVisibilityConfig().setInitialValue("hidden");
                                    }
                                }
                            }

                            for (ModifierGroup modGroup : menuItem.getModifierGroups()) {
                                if (!group.getId().equals(modGroup.getId()) && rule.getTargetId().equals(modGroup.getId())) {
                                    if (rule.getTargetIs() != null && rule.getTargetIs().equalsIgnoreCase("selected")) {
                                        int selected = 0;
                                        for (ModifierItem modItem : modGroup.getModifierItems()) {
                                            if (modItem.getAttributes().isSelected()) {
                                                selected += 1;
                                            }
                                        }
                                        if (selected > 0) {
                                            group.getAttributes().getVisibilityConfig().setInitialValue("");
                                        } else {
                                            group.getAttributes().getVisibilityConfig().setInitialValue("hidden");
                                        }
                                    }
                                    break;
                                }

                            }

                        } else if (rule.getTargetType().equalsIgnoreCase("modifier")) {
                            Set<String> selectedModifiers = new HashSet<>();
                            for (ModifierItem modItem : menuItem.getSizes().getModifierItems()) {
                                if (modItem.getAttributes().isSelected()) {
                                    selectedModifiers.add(modItem.getId());
                                    break;
                                }
                            }

                            for (ModifierGroup modGroup : menuItem.getModifierGroups()) {
                                if (!group.getId().equals(modGroup.getId())) {
                                    for (ModifierItem modItem : modGroup.getModifierItems()) {
                                        if (modItem.getAttributes().isSelected()) {
                                            selectedModifiers.add(modItem.getId());
                                        }
                                    }
                                }
                            }

                            //for (ModifierGroup modGroup : menuItem.getModifierGroups()) {
                            //if (!group.getId().equals(modGroup.getId())) {
                            if (rule.getTargetId() != null && selectedModifiers.contains(rule.getTargetId())) {
                                if (rule.getTargetIs() != null && rule.getTargetIs().equalsIgnoreCase("selected")) {
                                    group.getAttributes().getVisibilityConfig().setInitialValue("");
                                } else {
                                    group.getAttributes().getVisibilityConfig().setInitialValue("hidden");
                                }
                            } else if (!rule.getTargetId().isEmpty()) {
                                group.getAttributes().getVisibilityConfig().setInitialValue("hidden");
                            }
                            //}
                            //}

                        }

                    }
                }
            }
        }


        ArrayList<ModifierGroup> visibleModifierGroups = new ArrayList<>();

        for (ModifierGroup group : menuItem.getModifierGroups()) {
            visibilityConfig = group.getAttributes().getVisibilityConfig();
            if (visibilityConfig != null) {
                if (visibilityConfig.getInitialValue() != null && !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
                    visibleModifierGroups.add(group);
                }
            } else {
                visibleModifierGroups.add(group);
            }
        }

        for (ModifierGroup modifierGroup : visibleModifierGroups) {
            int selected = 0;
            for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                if (modifierItem.getAttributes().isSelected()) {
                    selected += modifierItem.getAttributes().getQuantity();
                }
            }
        }

        String orderStoreId = null;
        if (CacheUtil.getInstance().getOrderStore() != null) {
            orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
        }
        menuItem.toCdn(orderStoreId);
        menuItem.setPromo(true);
        if (campaign == null) {
            if (menuItem.isMustBeCustomized() || menuItem.isCombo) {
                CouponToAdd couponToAdd = new CouponToAdd(CacheUtil.getInstance().getCouponCode(), CacheUtil.getInstance().getCouponName(), menuItem);
                CacheUtil.getInstance().setCouponToAdd(couponToAdd);
                CacheUtil.getInstance().setCouponCode(null);
                CacheUtil.getInstance().setCouponName(null);
            } else {
                CacheUtil.getInstance().setCouponMenuItem(menuItem);
                String couponCode = CacheUtil.getInstance().getCouponCode();
                String couponName = CacheUtil.getInstance().getCouponName();
                if (couponCode != null && couponName != null) {
                    cartView.showCoupon(couponCode, couponName);
                    cartView.addPromotionItem(menuItem);
                }
            }
        } else {
            if (menuItem.isMustBeCustomized() || menuItem.isCombo) {
                CampaignToAdd campaignToAdd = new CampaignToAdd(CacheUtil.getInstance().getCampaign(), CacheUtil.getInstance().getCampaignPromotion(), menuItem);
                CacheUtil.getInstance().setCampaignToAdd(campaignToAdd);
                CacheUtil.getInstance().setCampaign(null);
                CacheUtil.getInstance().setCampaignPromotion(null);
            } else {
                CacheUtil.getInstance().setCampaignPromotionMenuItem(menuItem);
            }
            cartView.showDigitalCoupon(campaign);
        }
        if (validatedOrderData != null) {
            showPrice(true, validatedOrderData, 0);
        }
        cartView.showPromotionItem();
    }

    @Override
    public void removeCoupon() {
        CacheUtil.getInstance().setCouponCode(null);
        CacheUtil.getInstance().setCouponName(null);
        CacheUtil.getInstance().setCouponMenuItem(null);
        CacheUtil.getInstance().setCouponToAdd(null);
        if (validatePromotionDisposable != null) validatePromotionDisposable.dispose();
        if (validateDigitalCouponDisposable != null) validateDigitalCouponDisposable.dispose();
//        if (digitalCall != null && digitalCall.isExecuted()) digitalCall.cancel();
        menuItems = CacheUtil.getInstance().getCart();
        cartView.showMenuItems(menuItems, true, true);
        cartView.removeCoupon();
        removeMukafaa();
    }

    @Override
    public void proceed(String scheduleTime) {
        if (CacheUtil.getInstance().getCartQuantity() > 0) {
            if (AuthUtil.getInstance().isSignedIn()) {
                OrderType orderType = CacheUtil.getInstance().getOrderType();
                if (orderType != OrderType.NONE && (((orderType == OrderType.PICKUP || orderType == OrderType.DINE_IN || orderType == OrderType.CURBSIDE) && CacheUtil.getInstance().getOrderStore() != null) || (orderType == OrderType.DELIVERY && (CacheUtil.getInstance().getOrderAddress() != null || CacheUtil.getInstance().getOrderDeliveryAddress() != null)))) {
                    if (orderType == OrderType.PICKUP || orderType == OrderType.DINE_IN || orderType == OrderType.CURBSIDE || (orderType == OrderType.DELIVERY && CacheUtil.getInstance().getOrderAddress() != null)) {
                        if (CacheUtil.getOrderStoreId() != null) {
                            if (!CacheUtil.getInstance().getConcept().getAllowAdvanceOrder()) {
                                scheduleTime = null;
                            }
                            getStoreOpen(scheduleTime);
                        } else {
                            validateOrder(false, true, scheduleTime);
                        }
                    } else if (CacheUtil.getInstance().getOrderDeliveryAddress() != null) {
                        getAddresses(false);
                    } else {
                        cartView.showOrderType(true);
                    }
                } else {
                    cartView.showOrderType(true);
                }
            } else {
                cartView.showSignIn();
            }
        } else {
            cartView.showMessage(CacheUtil.getInstance().getTranslations(Text.CART_IS_EMPTY));
        }
    }

    @Override
    public void getStoreOpen(String scheduleTime) {
        String newScheduleTime = scheduleTime;
        if (scheduleTime == null || scheduleTime.isEmpty()) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
            newScheduleTime = simpleDateFormat.format(new Date());
        }
        LocationApiCall.getInstance().isOpen(CacheUtil.getOrderStoreId(), newScheduleTime, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                try {
                    JSONObject object = (JSONObject) o;
                    JSONObject data = object.getJSONObject("data");

                    OrderType orderType = CacheUtil.getInstance().getOrderType();
                    Store openStore = Store.parseStore(data);
                    if ((orderType == OrderType.DELIVERY && openStore.getAttributes().getOpenDeliver()) || openStore.getAttributes().getOpenPickup()) {
                        validateOrder(false, true, scheduleTime);
                    } else {
                        cartView.showError(CacheUtil.getInstance().getTranslations(Text.STORE_IS_CLOSED));
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                cartView.showError(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(Text.STORE_IS_CLOSED)));
            }
        });
    }

    @SuppressLint("CheckResult")
    @Override
    public void checkUnavailableMenuItems() {
        if (CacheUtil.getInstance().getOrderStore() != null) {
            String orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
            if (orderStoreId != null) {
                Observable.fromIterable(menuItems).filter(item -> Observable.fromIterable(item.getModifierGroups()).filter(modifierGroup -> {
                    // filter only items with a selected modifier
                    return Observable.fromIterable(modifierGroup.getModifierItems()).any(modifierItem -> modifierItem.getAttributes().isSelected()).blockingGet();
                }).any(modifierGroup -> {
                    boolean isModifierGroupDisabledForLocation = Observable.fromIterable(modifierGroup.getAttributes().getExcludedLocations()).map(Object::toString).any(modifierGroupExcludedLocation -> modifierGroupExcludedLocation.equals(orderStoreId)).blockingGet();

                    boolean modifierGroupContainsModifierItemThatIsDisabledForLocation = Observable.fromIterable(modifierGroup.getModifierItems()).filter(modifierItem -> modifierItem.getAttributes().isSelected()).any(modifierItem -> Observable.fromIterable(modifierItem.getExcludedLocations()).map(Object::toString).any(modifierItemExcludedLocation -> modifierItemExcludedLocation.equals(orderStoreId)).blockingGet()).blockingGet();
                    return isModifierGroupDisabledForLocation || modifierGroupContainsModifierItemThatIsDisabledForLocation;
                }).blockingGet()).forEach(menuItem -> {
                    unavailableMenuItems.put(menuItem.getId(), menuItem);
                });

                Executor executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    String applicationId = CacheUtil.getInstance().getApplication().getId();
                    AppDatabase appDatabase = AppDatabase.getInstance();
                    for (MenuItem item : menuItems) {
                        if (!item.isPromo()) {
                            MenuItemEntity menuItemEntity = appDatabase.menuItemEntityDao().findById(applicationId, item.getId());
                            if (menuItemEntity != null) {
                                boolean isUnavailable = false;
                                MenuCategoryEntity menuCategoryEntity = appDatabase.menuCategoryEntityDao().findById(applicationId, menuItemEntity.getMenuCategoryId());
                                if (menuCategoryEntity != null) {
                                    MenuCategory menuCategory = MenuCategoryEntity.Companion.toDomain(menuCategoryEntity);
                                    if (menuCategory.getExcludedLocations().contains(orderStoreId)) {
                                        isUnavailable = true;
                                        unavailableMenuItems.put(item.getId(), item);
                                    }
                                }
                                if (!isUnavailable) {
                                    MenuItem menuItem = MenuItemEntity.Companion.toDomain(menuItemEntity);
                                    boolean disable = false;
                                    if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
                                        disable = menuItem.getDisableForDelivery() == 1;
                                    } else if (CacheUtil.getInstance().getOrderType() == OrderType.PICKUP || CacheUtil.getInstance().getOrderType() == OrderType.DINE_IN || CacheUtil.getInstance().getOrderType() == OrderType.CURBSIDE) {
                                        disable = menuItem.getDisableForPickup() == 1;
                                    }
                                    if ((disable || menuItem.getExcludedLocations().contains(orderStoreId) || menuItem.getOutOfStockLocations().contains(orderStoreId))) {
                                        unavailableMenuItems.put(item.getId(), item);
                                    }
                                }
                            }
                        }
                    }
                    if (unavailableMenuItems.size() > 0) {
                        cartView.showUnavailableMenuItems();
                    } else if (CacheUtil.getInstance().getUpsell()) {
                        getUpsells();
                    } else {
                        getApplication(false);
                    }
                });
            }
        }
    }

    @Override
    public String getUnavailableMenuItemsMessage() {
        String message = CacheUtil.getInstance().getTranslations(Text.DISABLED_ITEMS_DELIVERY);
        if (CacheUtil.getInstance().getOrderType() != OrderType.DELIVERY) {
            message = CacheUtil.getInstance().getTranslations(Text.DISABLED_ITEMS_PICKUP);
        }
        if (disabledMenuItems.size() > 0) {
            message = CacheUtil.getInstance().getTranslations(Text.DISABLED_ITEMS, "Sorry, the items listed below are no longer available. Kindly remove them to proceed to checkout.");
        }
        return message;
    }

    @Override
    public ArrayList<MenuItem> getUnavailableMenuItems() {
        if (disabledMenuItems.size() > 0) {
            return new ArrayList<>(disabledMenuItems.values());
        }
        return new ArrayList<>(unavailableMenuItems.values());
    }

    @Override
    public ArrayList<MenuItem> getMenuItemsList() {
        return menuItems;
    }

    @Override
    public void deleteUnavailableItems() {
        Set<String> disabledItemIds = new HashSet<>(disabledMenuItems.keySet());
        Set<String> unavailableItemIds = new HashSet<>(unavailableMenuItems.keySet());

        ArrayList<MenuItem> updatedCartItems = new ArrayList<>();
        if (disabledItemIds.size() > 0) {
            for (MenuItem menuItem : menuItems) {
                if (!disabledItemIds.contains(menuItem.getId())) {
                    updatedCartItems.add(menuItem);
                }
            }
        } else {
            for (MenuItem menuItem : menuItems) {
                if (!unavailableItemIds.contains(menuItem.getId())) {
                    updatedCartItems.add(menuItem);
                }
            }
        }

        removeMukafaaRedemption();
        CacheUtil.getInstance().setCart(updatedCartItems);
        menuItems = updatedCartItems;
        if (menuItems.size() == 0) {
            CacheUtil.getInstance().setUpsell(true);
        }
        cartView.deleteUnavailableItems(CacheUtil.getInstance().getCartQuantity());

    }

    @Override
    public void getAddresses(boolean fromSave) {

        if (AuthUtil.getInstance().isSignedIn()) {
            CustomersApiCall.getInstance().getCustomerAddresses(AuthUtil.getInstance().getCustomer().getId(), "1", new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    try {
                        JSONObject object = new JSONObject(o.toString());
                        JSONArray array = object.getJSONArray("data");
                        ArrayList<Address> addresses = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            addresses.add(com.skylinedynamics.solosdk.api.models.objects.Address.parseAddress(array.getJSONObject(i)));
                        }

                        if (fromSave) {
                            if (addresses.size() > 0) {
                                Address address = addresses.get(0);
                                CacheUtil.getInstance().setOrderAddress(address);
                                if (CacheUtil.getInstance().getUpsell()) {
                                    getUpsells();
                                } else {
                                    //put cartPresenter.showPayment in this method
                                    getApplication(false);
                                }
                            } else {
                                cartView.showMessage(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                            }
                        } else {

                            for (Address address : addresses) {
                                if (address.getAttributes().getLat().equals(CacheUtil.getInstance().getOrderDeliveryLatitude()) && address.getAttributes().getLongt().equals(CacheUtil.getInstance().getOrderDeliveryLongitude())) {
                                    CacheUtil.getInstance().setOrderAddress(address);
                                    break;
                                }
                            }
                            //unsync call causing duplicate address being save
                            new Handler().postDelayed(() -> {
                                if (CacheUtil.getInstance().getOrderAddress() != null) {
                                    if (CacheUtil.getInstance().getUpsell()) {
                                        getUpsells();
                                    } else {
                                        //put cartPresenter.showPayment in this method
                                        getApplication(false);
                                    }
                                } else if (CacheUtil.getInstance().getOrderDeliveryAddress() != null) {
                                    addAddress();
                                } else {
                                    cartView.showMessage(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                                }
                            }, 1500);

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.onError(e.getMessage());
                    }

                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    cartView.showMessage(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
                }
            });
        }

    }

    @Override
    public void updateAddress(Address address) {

    }

    @Override
    public void addAddress() {

        CreateCustomerAddressBody createCustomerAddressBody = new CreateCustomerAddressBody("", CacheUtil.getInstance().getOrderDeliveryLabel(), "", "", "", "", "", CacheUtil.getInstance().getOrderDeliveryAddress(), Double.valueOf(CacheUtil.getInstance().getOrderDeliveryLatitude()), Double.valueOf(CacheUtil.getInstance().getOrderDeliveryLongitude()), CacheUtil.getInstance().getOrderDeliveryTelephone(), CacheUtil.getInstance().getOrderDeliveryInstructions(), "");

        CustomersApiCall.getInstance().addCustomerAddress(createCustomerAddressBody, AuthUtil.getInstance().getCustomer().getId(), "1", new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                getAddresses(true);
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                cartView.showMessage(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
            }
        });

    }

    @Override
    public void getUpsells() {
        String menuId = MenuUtil.getDiffMenuId();
        MenuApiCall.getInstance().getUpsells(menuId, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                if (o instanceof JSONObject) {

                    try {
                        JSONObject jsonObject = (JSONObject) o;
                        JSONArray dataArray = jsonObject.getJSONArray("data");
                        ArrayList<MenuItem> upsells = GsonHelper.gsonFinal().fromJson(dataArray.toString(), new TypeToken<ArrayList<MenuItem>>() {
                        }.getType());

                        if (upsells != null && upsells.size() > 0) {

                            for (MenuItem upsell : upsells) {
                                upsell.getAttributes().setQuantity(0);
                            }
                            Collections.sort(upsells, new Comparator<MenuItem>() {
                                @Override
                                public int compare(MenuItem menuItem1, MenuItem menuItem2) {
                                    return menuItem1.getAttributes().getDisplayOrder() - menuItem2.getAttributes().getDisplayOrder();
                                }
                            });
                            //put cartPresenter.showPayment in this method
                            getApplication(true);
                        } else {
                            //put cartPresenter.showPayment in this method
                            getApplication(false);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.onError(e);
                    }

                } else {
                    this.onError("");
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                //put cartPresenter.showPayment in this method
                getApplication(false);
            }
        });
    }

    @Override
    public void showDigitalCoupons(boolean show) {
        //cartView.showProgressDialog();
        getDigitalCoupons(show, true);
    }

    @Override
    public int getDigitalCouponsCount() {
        return digitalCoupons.size();
    }

    @Override
    public void selectDigitalCoupon(int position) {
        cartView.selectDigitalCoupon(position);
    }

    @Override
    public void validateDigitalCoupon(boolean fromApply, Campaign digitalCoupon) {

        Customer customer = AuthUtil.getInstance().getCustomer();
        Concept concept = CacheUtil.getInstance().getConcept();
        Store store = CacheUtil.getInstance().getOrderStore();
        menuItems = CacheUtil.getInstance().getCart();

        double subtotal = getMenuItemsTotalPrice();
        double deliveryCharge = concept.getDeliveryCharge();

        try {
            double minimumOrderAmountFreeDelivery = concept.getMinimumOrderAmountFreeDelivery();
            if (minimumOrderAmountFreeDelivery != 0 && subtotal >= minimumOrderAmountFreeDelivery) {
                deliveryCharge = concept.getSpecialDeliveryPrice();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonObject orderBody = new JsonObject();
        if (customer != null) {
            orderBody.addProperty("customer", customer.getId());
        }

        orderBody.addProperty("campaign-id", digitalCoupon.getId());

        orderBody.addProperty("source", "Android");
        orderBody.addProperty("notes", CacheUtil.getInstance().getCartNotes());
        orderBody.addProperty("subtotal", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", subtotal)));

        double discount = 0;
        double discountedSubtotal = subtotal - discount;

        if (discountedSubtotal < 0) {
            discountedSubtotal = 0;
            discount = subtotal;
        }

        orderBody.addProperty("discount", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", discount)));

        switch (CacheUtil.getInstance().getOrderType()) {
            case PICKUP:
                deliveryCharge = 0.0;
//                if (CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().size() == 3 || CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().contains(OrderType.DINE_IN)) {
                if (CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().contains(OrderType.DINE_IN)) {
                    orderBody.addProperty("type", "to-go");
                } else {
                    orderBody.addProperty("type", "pickup");
                }
                if (store != null) {
                    orderBody.addProperty("location", store.getId());
                }
                break;
            case DINE_IN:
                deliveryCharge = 0.0;
                orderBody.addProperty("type", "eat-in");
                if (store != null) {
                    orderBody.addProperty("location", store.getId());
                }
                break;
            case DELIVERY:
                orderBody.addProperty("type", "deliver");
                //orderBody.addProperty("delivery-charge", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", deliveryCharge)));
                Address address = CacheUtil.getInstance().getOrderAddress();
                if (address != null) {
                    orderBody.addProperty("address", address.getId());
                }
                break;
            case CURBSIDE:
                deliveryCharge = 0.0;
                if (CacheUtil.getInstance().getOrderStore() != null) {
                    orderBody.addProperty("location", CacheUtil.getInstance().getOrderStore().getId());
                }
                if (CacheUtil.getInstance().getCurbsideCar() != null) {
                    orderBody.addProperty("type", "curbside");
                    orderBody.addProperty("car-id", CacheUtil.getInstance().getCurbsideCar().id);
                }
                break;
            case NONE:
                deliveryCharge = 0.0;
                break;
        }

        double vatAmount = 0;

        double total = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", discountedSubtotal + deliveryCharge + vatAmount));
        if (total < 0) {
            total = 0;
        }

        orderBody.addProperty("total", total);

        JsonArray items = new JsonArray();
        String orderStoreId = null;
        if (CacheUtil.getInstance().getOrderStore() != null) {
            orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
        }

        for (MenuItem menuItem : menuItems) {

            JsonObject item = new JsonObject();
            JsonArray modifiers = new JsonArray();
            JsonArray ingredients = new JsonArray();

            item.addProperty("id", menuItem.getId());
            item.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", menuItem.getPrice(orderStoreId))));
            item.addProperty("quantity", menuItem.getAttributes().getQuantity());
            item.addProperty("is-reorder", menuItem.getAttributes().isReorder());
            item.addProperty("is-promotion", menuItem.isPromo());
            item.addProperty("notes", menuItem.getAttributes().getNotes());

            for (ModifierItem size : menuItem.getSizes().getModifierItems()) {
                if (size.getAttributes().isSelected()) {
                    JsonObject modifier = new JsonObject();
                    modifier.addProperty("id", size.getId());
                    modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", size.getAttributes().getPrice())));
                    modifier.addProperty("quantity", size.getAttributes().getQuantity());
                    modifiers.add(modifier);
                }
            }

            for (ModifierItem drink : menuItem.getDrinks().getModifierItems()) {
                if (drink.getAttributes().isSelected()) {
                    JsonObject modifier = new JsonObject();
                    modifier.addProperty("id", drink.getId());
                    modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", drink.getAttributes().getPrice())));
                    modifier.addProperty("quantity", drink.getAttributes().getQuantity());
                    modifiers.add(modifier);
                }
            }

            for (ModifierItem fries : menuItem.getFries().getModifierItems()) {
                if (fries.getAttributes().isSelected()) {
                    JsonObject modifier = new JsonObject();
                    modifier.addProperty("id", fries.getId());
                    modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", fries.getAttributes().getPrice())));
                    modifier.addProperty("quantity", fries.getAttributes().getQuantity());
                    modifiers.add(modifier);
                }
            }

            for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                VisibilityConfig visibilityConfig = modifierGroup.getAttributes().getVisibilityConfig();
                if (visibilityConfig == null || visibilityConfig.getInitialValue() == null || !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
                    for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                        if (modifierItem.getAttributes().isSelected()) {
                            JsonObject modifier = new JsonObject();
                            modifier.addProperty("id", modifierItem.getId());
                            modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", modifierItem.getAttributes().getPrice())));
                            modifier.addProperty("quantity", modifierItem.getAttributes().getQuantity());
                            modifiers.add(modifier);
                        }
                    }
                }

            }

            for (ModifierItem modifierItem : menuItem.getModifierItems()) {
                JsonObject modifier = new JsonObject();
                modifier.addProperty("id", modifierItem.getId());
                modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", modifierItem.getAttributes().getPrice())));
                modifier.addProperty("quantity", modifierItem.getAttributes().getQuantity());
                modifiers.add(modifier);
            }

            for (Ingredient ingredient : menuItem.getIngredients()) {
                if (!ingredient.getAttributes().isSelected()) {
                    JsonObject ingredientObject = new JsonObject();
                    ingredientObject.addProperty("id", ingredient.getId());
                    ingredientObject.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", ingredient.getAttributes().getPrice())));
                    ingredientObject.addProperty("quantity", ingredient.getAttributes().getQuantity());
                    ingredients.add(ingredientObject);
                }
            }
            item.add("ingredients", ingredients);
            item.add("modifiers", modifiers);
            items.add(item);
        }
        orderBody.add("items", items);

//        if (digitalCall != null && digitalCall.isExecuted()) digitalCall.cancel();
        if (validateDigitalCouponDisposable != null) validateDigitalCouponDisposable.dispose();
        String finalOrderStoreId = orderStoreId;
        validateDigitalCouponDisposable = validateDigitalCouponObservable(orderBody).subscribe(onNext -> {
            try {

                JSONObject jsonObject = new JSONObject(onNext.toString());

                if (jsonObject.has("error") && !jsonObject.isNull("error")) {
                    digitalCoupons.clear();
                    CacheUtil.getInstance().setDigitalCoupons("");
                    CacheUtil.getInstance().setCampaign(null);
                    CacheUtil.getInstance().setCampaignPromotion(null);
                    CacheUtil.getInstance().setCampaignPromotionMenuItem(null);
                    CacheUtil.getInstance().setCampaignToAdd(null);
                    cartView.showDigitalCoupon(null);
                    removeCoupon();
                    cartView.showPromoError(jsonObject.getString("error"));
                } else if (fromApply) {
                    MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_APPLY_PROMO);
                    CacheUtil.getInstance().setCampaign(digitalCoupon);
                    CacheUtil.getInstance().setCampaignPromotion(digitalCoupon.getPromotion());
                    CacheUtil.getInstance().setCampaignPromotionMenuItem(null);
                    CacheUtil.getInstance().setCampaignToAdd(null);

                    OrderData orderData = OrderData.parse(new JSONObject(onNext.toString()));
                    if (orderData.getTotal() < 0) {
                        orderData.setTotal(0);
                    }
                    boolean isPromoItem = false;
                    if (CacheUtil.getInstance().getCartQuantity() > 0) {
                        try {
                            String cartItems = jsonObject.isNull("items") ? "" : jsonObject.optString("items");
                            List<MenuItem> itemsList = GsonHelper.gsonFinal().fromJson(cartItems, new TypeToken<List<MenuItem>>() {
                            }.getType());
                            int size = itemsList.size();
                            validatedPromoItemPrice = 0.0;
                            validatedPromoItemQuantity = 0;
                            for (int i = 0; i < size; i++) {
                                if (itemsList.get(i).getIsPromotion()) {
                                    isPromoItem = true;
                                    MenuItem mItem = itemsList.get(i);
                                    validatedPromoItemPrice = mItem.getPrice();
                                    validatedPromoItemQuantity = mItem.getQuantity();
                                    MenuItemAttributes attributes = new MenuItemAttributes();
                                    attributes.setPrice(digitalCoupon.getPromotion().getAttributes().getItemPrice());
                                    if (digitalCoupon.getPromotion() != null && digitalCoupon.getPromotion().getAttributes().getItemQuantity() > 0) {
                                        attributes.setQuantity(digitalCoupon.getPromotion().getAttributes().getItemQuantity());
                                    } else {
                                        attributes.setQuantity(1);
                                    }
                                    getCouponMenuItem(mItem.getId(), attributes.getQuantity(), attributes.getPrice(), attributes, digitalCoupon);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        cartView.setTax(concept.getVatType().equalsIgnoreCase("inclusive"), StringUtil.getFormattedPrice(orderData.getVatAmount()), orderData.getVatAmount());
                        if (orderData.getDiscount() > 0) {
                            cartView.setOriginalPrice(true, orderData.getSubtotal() + orderData.getDeliverCharge());
                        }

                        if (!isPromoItem) {
                            cartView.setTotalPrice(true, orderData.getSubtotal(), orderData.getDiscountDisplay(), orderData.getTotal(), CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY, orderData.getDeliverCharge(), orderData.getCharges(), 0);
                            cartView.showDigitalCoupon(digitalCoupon);
                        } else {
                            validatedOrderData = orderData;
                        }
                    } else {
                        digitalCoupons.clear();
                        CacheUtil.getInstance().setDigitalCoupons("");
                        CacheUtil.getInstance().setCampaign(null);
                        CacheUtil.getInstance().setCampaignPromotion(null);
                        CacheUtil.getInstance().setCampaignPromotionMenuItem(null);
                        CacheUtil.getInstance().setCampaignToAdd(null);
                        cartView.showDigitalCoupon(null);
                        removeCoupon();
                    }
                } else {
                    OrderData orderData = OrderData.parse(new JSONObject(onNext.toString()));
                    if (orderData.getTotal() < 0) {
                        orderData.setTotal(0);
                    }
                    cartView.setTax(concept.getVatType().equalsIgnoreCase("inclusive"), StringUtil.getFormattedPrice(orderData.getVatAmount()), orderData.getVatAmount());
                    if (orderData.getDiscount() > 0) {
                        cartView.setOriginalPrice(true, orderData.getSubtotal() + orderData.getDeliverCharge());
                    }
                    showPrice(true, orderData, 0);
                    cartView.showDigitalCoupon(digitalCoupon);
                }

            } catch (JSONException ex) {
                validateDigitalCouponError(ex);
            }
        });
    }

    private void validateDigitalCouponError(Object couponError) {
        if (couponError instanceof JSONException) {
            ((JSONException) couponError).printStackTrace();
        } else {
            AppLogger.printRetrofitError(couponError);
            cartView.showDigitalCoupon(null);
            if (couponError instanceof JSONObject) {
                try {
                    JSONObject object = (JSONObject) couponError;
                    Object error = object.get("error");
                    String detail = CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED);
                    if (error instanceof JSONObject) {
                        detail = ((JSONObject) error).getString("detail");
                    } else if (error instanceof JSONArray) {
                        JSONObject errorObject = ((JSONArray) error).getJSONObject(0);
                        detail = errorObject.getString("detail");
                    }
                    cartView.showError(detail);
                } catch (JSONException e) {
                    e.printStackTrace();
                    cartView.showError(CacheUtil.getInstance().getTranslations(Text.NO_INTERNET_ERROR, "Sorry we can't process your request at the moment. Please try again later."));
                }

            } else {
                cartView.showError(CacheUtil.getInstance().getTranslations(Text.NO_INTERNET_ERROR, "Sorry we can't process your request at the moment. Please try again later."));
            }
        }
    }


    // TODO: remove side effects
    public Observable<Object> validateDigitalCouponObservable(JsonObject orderBody) {
        return Observable.create(subscriber -> {
            CouponsApiCall.getInstance().validatePromotion(orderBody, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    subscriber.onNext(o);
                    subscriber.onComplete();
                }

                @Override
                public void onError(Object o) {
                    validateDigitalCouponError(o);
                }
            });

        });
    }

    @Override
    public void selectPromo(int position) {

    }

    @Override
    public void getDigitalCoupons(boolean show, boolean fromButtonClick) {

        if (CacheUtil.getInstance().getCartQuantity() > 0) {
            boolean validDelivery = CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY && (CacheUtil.getInstance().getOrderAddress() != null || CacheUtil.getInstance().getOrderDeliveryAddress() != null);
            boolean validPickup = (CacheUtil.getInstance().getOrderType() == OrderType.PICKUP || CacheUtil.getInstance().getOrderType() == OrderType.DINE_IN || CacheUtil.getInstance().getOrderType() == OrderType.CURBSIDE) && CacheUtil.getInstance().getOrderStore() != null;

            Customer customer = AuthUtil.getInstance().getCustomer();
            Concept concept = CacheUtil.getInstance().getConcept();
            Store store = CacheUtil.getInstance().getOrderStore();
            menuItems = CacheUtil.getInstance().getCart();

            double subtotal = getMenuItemsTotalPrice();
            double deliveryCharge = concept.getDeliveryCharge();

            try {
                double minimumOrderAmountFreeDelivery = concept.getMinimumOrderAmountFreeDelivery();
                if (minimumOrderAmountFreeDelivery != 0 && subtotal >= minimumOrderAmountFreeDelivery) {
                    deliveryCharge = concept.getSpecialDeliveryPrice();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            JsonObject orderBody = new JsonObject();
            if (AuthUtil.getInstance().isSignedIn()) {
                orderBody.addProperty("customer", customer.getId());
            }

            orderBody.addProperty("source", "Android");
            orderBody.addProperty("notes", CacheUtil.getInstance().getCartNotes());
            orderBody.addProperty("subtotal", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", subtotal)));

            double discount = 0;

            String couponCode = CacheUtil.getInstance().getCouponCode();
            if (couponCode != null && !couponCode.isEmpty()) {
                orderBody.addProperty("coupon-code", couponCode);
            }

            if (CacheUtil.getInstance().getCampaign() != null && CacheUtil.getInstance().getCampaignPromotion() != null) {
                orderBody.addProperty("campaign-id", CacheUtil.getInstance().getCampaign().getId());
            }

            double discountedSubtotal = subtotal - discount;
            if (discountedSubtotal < 0) {
                discountedSubtotal = 0;
                discount = subtotal;
            }

            orderBody.addProperty("discount", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", discount)));

            if (validDelivery || validPickup) {
                switch (CacheUtil.getInstance().getOrderType()) {
                    case PICKUP:
                        deliveryCharge = 0.0;
//                        if (CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().size() == 3 || CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().contains(OrderType.DINE_IN)) {
                        if (CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().contains(OrderType.DINE_IN)) {
                            orderBody.addProperty("type", "to-go");
                        } else {
                            orderBody.addProperty("type", "pickup");
                        }
                        if (store != null) {
                            orderBody.addProperty("location", store.getId());
                        }
                        break;
                    case DINE_IN:
                        deliveryCharge = 0.0;
                        orderBody.addProperty("type", "eat-in");
                        if (store != null) {
                            orderBody.addProperty("location", store.getId());
                        }
                        break;
                    case DELIVERY:
                        orderBody.addProperty("type", "deliver");
                        //orderBody.addProperty("delivery-charge", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", deliveryCharge)));
                        Address address = CacheUtil.getInstance().getOrderAddress();
                        if (address != null) {
                            orderBody.addProperty("address", address.getId());
                        }
                        break;
                    case CURBSIDE:
                        deliveryCharge = 0.0;
                        if (CacheUtil.getInstance().getOrderStore() != null) {
                            orderBody.addProperty("location", CacheUtil.getInstance().getOrderStore().getId());
                        }
                        if (CacheUtil.getInstance().getCurbsideCar() != null) {
                            orderBody.addProperty("type", "curbside");
                            orderBody.addProperty("car-id", CacheUtil.getInstance().getCurbsideCar().id);
                        }
                        break;
                    case NONE:
                        deliveryCharge = 0.0;
                        break;
                }
            }

            double vatAmount = 0;

//            if (concept.getVatType() != null && concept.getVatType().equalsIgnoreCase("exclusive")) {
//                vatAmount = (discountedSubtotal + deliveryCharge) * (concept.getVatRate() / 100.0);
//                orderBody.addProperty("vat-amount", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", vatAmount)));
//            }

            double total = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", discountedSubtotal + deliveryCharge + vatAmount));
            if (total < 0) {
                total = 0;
            }

            orderBody.addProperty("total", total);

            JsonArray items = new JsonArray();
            String orderStoreId = CacheUtil.getOrderStoreId();
            for (MenuItem menuItem : menuItems) {

                JsonObject item = new JsonObject();
                JsonArray modifiers = new JsonArray();
                JsonArray ingredients = new JsonArray();

                item.addProperty("id", menuItem.getId());
                item.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", menuItem.getPrice(orderStoreId))));
                item.addProperty("quantity", menuItem.getAttributes().getQuantity());
                item.addProperty("is-reorder", menuItem.getAttributes().isReorder());
                item.addProperty("is-promotion", menuItem.isPromo());
                item.addProperty("notes", menuItem.getAttributes().getNotes());

                for (ModifierItem size : menuItem.getSizes().getModifierItems()) {
                    if (size.getAttributes().isSelected()) {
                        JsonObject modifier = new JsonObject();
                        modifier.addProperty("id", size.getId());
                        modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", size.getAttributes().getPrice())));
                        modifier.addProperty("quantity", size.getAttributes().getQuantity());
                        modifiers.add(modifier);
                    }
                }

                for (ModifierItem drink : menuItem.getDrinks().getModifierItems()) {
                    if (drink.getAttributes().isSelected()) {
                        JsonObject modifier = new JsonObject();
                        modifier.addProperty("id", drink.getId());
                        modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", drink.getAttributes().getPrice())));
                        modifier.addProperty("quantity", drink.getAttributes().getQuantity());
                        modifiers.add(modifier);
                    }
                }

                for (ModifierItem fries : menuItem.getFries().getModifierItems()) {
                    if (fries.getAttributes().isSelected()) {
                        JsonObject modifier = new JsonObject();
                        modifier.addProperty("id", fries.getId());
                        modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", fries.getAttributes().getPrice())));
                        modifier.addProperty("quantity", fries.getAttributes().getQuantity());
                        modifiers.add(modifier);
                    }
                }

                for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                    VisibilityConfig visibilityConfig = modifierGroup.getAttributes().getVisibilityConfig();
                    if (visibilityConfig == null || visibilityConfig.getInitialValue() == null || !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
                        for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                            if (modifierItem.getAttributes().isSelected()) {
                                JsonObject modifier = new JsonObject();
                                modifier.addProperty("id", modifierItem.getId());
                                modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", modifierItem.getAttributes().getPrice())));
                                modifier.addProperty("quantity", modifierItem.getAttributes().getQuantity());
                                modifiers.add(modifier);
                            }
                        }
                    }

                }

                for (ModifierItem modifierItem : menuItem.getModifierItems()) {
                    JsonObject modifier = new JsonObject();
                    modifier.addProperty("id", modifierItem.getId());
                    modifier.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", modifierItem.getAttributes().getPrice())));
                    modifier.addProperty("quantity", modifierItem.getAttributes().getQuantity());
                    modifiers.add(modifier);
                }

                for (Ingredient ingredient : menuItem.getIngredients()) {
                    if (!ingredient.getAttributes().isSelected()) {
                        JsonObject ingredientObject = new JsonObject();
                        ingredientObject.addProperty("id", ingredient.getId());
                        ingredientObject.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", ingredient.getAttributes().getPrice())));
                        ingredientObject.addProperty("quantity", ingredient.getAttributes().getQuantity());
                        ingredients.add(ingredientObject);
                    }
                }
                item.add("ingredients", ingredients);
                item.add("modifiers", modifiers);
                items.add(item);
            }
            orderBody.add("items", items);

            if (digitalCall != null && digitalCall.isExecuted()) digitalCall.cancel();
            digitalCall = CouponsApiCall.getInstance().getCampaignsCancelableAll(orderBody, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    try {
                        JSONObject object = new JSONObject(o.toString());
                        digitalCoupons.clear();
                        digitalCoupons = GsonHelper.gsonFinal().fromJson(object.getJSONArray("data").toString(), new TypeToken<List<Campaign>>() {
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

                        }

                        String digitalCouponString = new Gson().toJson(digitalCoupons);

                        if (digitalCoupons.size() > 0) {
                            if (digitalCoupons.size() == 1) {
                                Promotion promotion = digitalCoupons.get(0).getPromotion();
                                if (promotion != null && promotion.getAttributes().getAutoApply() != null && promotion.getAttributes().getAutoApply()) {
                                    if (CacheUtil.getInstance().getCampaign() == null) {
                                        if (digitalCoupons.get(0).getAttributes().isEligible()) {
                                            if (fromButtonClick) {
                                                cartView.showDigitalCoupons(false, digitalCoupons, 0);
                                            } else {
                                                availDigitalCoupon(0);
                                            }
                                        } else if (show && CacheUtil.getInstance().getCampaign() == null) {
                                            cartView.showDigitalCoupons(false, digitalCoupons, -1);
                                        }
                                        CacheUtil.getInstance().setDigitalCoupons(digitalCouponString);
                                    } else {
                                        if (CacheUtil.getInstance().getCartQuantity() > 0) {
                                            int selected = -1;
                                            if (CacheUtil.getInstance().getCampaign() != null) {
                                                for (int i = 0; i < digitalCoupons.size(); i++) {
                                                    if (digitalCoupons.get(i).getId().equalsIgnoreCase(CacheUtil.getInstance().getCampaign().getId())) {
                                                        selected = i;
                                                        break;
                                                    }
                                                }
                                            }

                                            if (show) {
                                                cartView.showDigitalCoupons(false, digitalCoupons, selected);
                                            }
                                            if (CacheUtil.getInstance().getCampaign() != null)
                                                validateDigitalCoupon(false, CacheUtil.getInstance().getCampaign());

                                            CacheUtil.getInstance().setDigitalCoupons(digitalCouponString);
                                            cartView.showDigitalCoupon(CacheUtil.getInstance().getCampaign());
                                        } else {
                                            CacheUtil.getInstance().setDigitalCoupons("");
                                            CacheUtil.getInstance().setCampaign(null);
                                            CacheUtil.getInstance().setCampaignPromotion(null);
                                            CacheUtil.getInstance().setCampaignPromotionMenuItem(null);
                                            CacheUtil.getInstance().setCampaignToAdd(null);
                                            cartView.showDigitalCoupon(null);
                                        }
                                    }
                                } else if (CacheUtil.getInstance().getCartQuantity() > 0) {
                                    int selected = -1;
                                    if (CacheUtil.getInstance().getCampaign() != null) {
                                        for (int i = 0; i < digitalCoupons.size(); i++) {
                                            if (digitalCoupons.get(i).getId().equalsIgnoreCase(CacheUtil.getInstance().getCampaign().getId())) {
                                                selected = i;
                                                break;
                                            }
                                        }
                                    }

                                    if (show) {
                                        cartView.showDigitalCoupons(false, digitalCoupons, selected);
                                    }
                                    if (CacheUtil.getInstance().getCampaign() != null)
                                        validateDigitalCoupon(false, CacheUtil.getInstance().getCampaign());

                                    CacheUtil.getInstance().setDigitalCoupons(digitalCouponString);
                                    cartView.showDigitalCoupon(CacheUtil.getInstance().getCampaign());
                                } else {
                                    CacheUtil.getInstance().setDigitalCoupons("");
                                    CacheUtil.getInstance().setCampaign(null);
                                    CacheUtil.getInstance().setCampaignPromotion(null);
                                    CacheUtil.getInstance().setCampaignPromotionMenuItem(null);
                                    CacheUtil.getInstance().setCampaignToAdd(null);
                                    cartView.showDigitalCoupon(null);
                                }
                            } else {
                                boolean proceedBottom = true;
                                int selected = -1;
                                if (CacheUtil.getInstance().getCampaign() != null) {
                                    for (int i = 0; i < digitalCoupons.size(); i++) {
                                        if (digitalCoupons.get(i).getId().equalsIgnoreCase(CacheUtil.getInstance().getCampaign().getId())) {
                                            selected = i;
                                            break;
                                        }
                                    }
                                } else {
                                    int counter = 0;
                                    int position = -1;
                                    List<Campaign> dummy = new ArrayList<>();
                                    for (int i = 0; i < digitalCoupons.size(); i++) {
                                        Promotion promotion = digitalCoupons.get(i).getPromotion();
                                        if (promotion != null && promotion.getAttributes().getAutoApply() != null && promotion.getAttributes().getAutoApply()) {
                                            if (digitalCoupons.get(i).getAttributes().isEligible()) {
                                                dummy.add(digitalCoupons.get(i));
                                                counter++;
                                                position = i;
                                            }
                                        }
                                    }
                                    if (counter >= 1) {
                                        proceedBottom = false;
                                        if (counter == 1) {
                                            if (fromButtonClick) {
                                                cartView.showDigitalCoupons(false, digitalCoupons, position);
                                            } else {
                                                availDigitalCoupon(position);
                                            }
                                        } else {
                                            cartView.showDigitalCoupons(false, digitalCoupons, position);
                                        }
                                    }
                                }

                                if (proceedBottom) {
                                    if (CacheUtil.getInstance().getCartQuantity() > 0) {
                                        if (show && (fromButtonClick || digitalCoupons.size() > 0)) {
                                            cartView.showDigitalCoupons(false, digitalCoupons, selected);
                                        }
                                        if (CacheUtil.getInstance().getCampaign() != null)
                                            validateDigitalCoupon(false, CacheUtil.getInstance().getCampaign());

                                        CacheUtil.getInstance().setDigitalCoupons(digitalCouponString);
                                        cartView.showDigitalCoupon(CacheUtil.getInstance().getCampaign());
                                    } else {
                                        CacheUtil.getInstance().setCampaign(null);
                                        CacheUtil.getInstance().setCampaignPromotion(null);
                                        CacheUtil.getInstance().setCampaignPromotionMenuItem(null);
                                        CacheUtil.getInstance().setCampaignToAdd(null);
                                        cartView.showDigitalCoupon(null);
                                    }
                                }
                            }
                        } else {
                            CacheUtil.getInstance().setDigitalCoupons(digitalCouponString);
                            this.onError("");
                        }

                    } catch (JSONException ex) {
                        ex.printStackTrace();
                        this.onError("");
                    }

                    cartView.refreshDigitalCoupons();

                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    CacheUtil.getInstance().setCampaign(null);
                    CacheUtil.getInstance().setCampaignPromotion(null);
                    CacheUtil.getInstance().setCampaignPromotionMenuItem(null);
                    CacheUtil.getInstance().setCampaignToAdd(null);
                    cartView.showDigitalCoupon(null);
                    if (show && (fromButtonClick || digitalCoupons.size() > 0)) {
                        cartView.showDigitalCoupons(false, digitalCoupons, -1);
                    }
                }
            });
        }
    }

    @Override
    public void availDigitalCoupon(int position) {

        if (CacheUtil.getInstance().getCouponCode() != null) {
            cartView.showMessage(CacheUtil.getInstance().getTranslations(Text.REMOVE_COUPON_MESSAGE, "Please remove coupon code first before applying another promo."));
        } else if (position != -1 && position < digitalCoupons.size()) {
            Campaign digitalCoupon = digitalCoupons.get(position);
            removeMukafaaRedemption();

            if (digitalCoupon.getPromotion() != null && digitalCoupon.getPromotion().getAttributes().getType().equalsIgnoreCase("item")) {

                Promotion promotion = digitalCoupon.getPromotion();
                if (promotion != null) {
                    MenuApiCall.getInstance().getItemDetails(promotion.getAttributes().getItemId(), new ApiRequestListener() {
                        @Override
                        public void onSuccess(Object o) {
                            try {
                                JSONObject object = new JSONObject(o.toString());
                                JSONArray dataArray = object.getJSONArray("data");
                                if (dataArray.length() > 0) {

                                    MenuItem menuItem = GsonHelper.gsonFinal().fromJson(dataArray.getJSONObject(0).toString(), MenuItem.class);
                                    menuItem.setPromo(true);
                                    if (promotion.getAttributes().getItemQuantity() > 0) {
                                        menuItem.getAttributes().setQuantity(promotion.getAttributes().getItemQuantity());
                                    } else {
                                        menuItem.getAttributes().setQuantity(1);
                                    }
                                    String orderStoreId = null;
                                    if (CacheUtil.getInstance().getOrderStore() != null) {
                                        orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
                                    }
                                    menuItem.getAttributes().setPrice(promotion.getAttributes().getItemPrice());
                                    menuItem.toCdn(orderStoreId);
                                    CacheUtil.getInstance().setCampaignPromotionMenuItem(menuItem);

                                    validateDigitalCoupon(true, digitalCoupon);

                                } else {
                                    this.onError("");
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                this.onError(e);
                            }
                        }

                        @Override
                        public void onError(Object o) {
                            AppLogger.printRetrofitError(o);
                            cartView.showDigitalCoupon(null);
                            cartView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
                        }
                    });

                }

            } else {
                validateDigitalCoupon(true, digitalCoupon);
            }


        }
    }

    @Override
    public void availDigitalCouponCampaign(Campaign campaign) {
        for (int i = 0; i < digitalCoupons.size(); i++) {
            if (digitalCoupons.get(i).getId().equalsIgnoreCase(campaign.getId())) {
                availDigitalCoupon(i);
                break;
            }
        }
    }

    @Override
    public List<Campaign> getDigitalCouponsList() {
        return digitalCoupons;
    }

    @Override
    public int getDigitalCouponsSelected() {
        int selected = -1;
        if (CacheUtil.getInstance().getCampaign() != null) {
            for (int i = 0; i < digitalCoupons.size(); i++) {
                if (digitalCoupons.get(i).getId().equalsIgnoreCase(CacheUtil.getInstance().getCampaign().getId())) {
                    selected = i;
                    break;
                }
            }
        }
        return selected;
    }


    @Override
    public void scheduleOrder(Calendar originalCalendar, Calendar calendar, boolean checkingOnly) {
        int minSchedule = CacheUtil.getInstance().getConcept().getDefaultScheduleDeliveryTime();
        int maxSchedule = CacheUtil.getInstance().getConcept().getMaxDaysAllowedInAdvanceOrder();

        if (minSchedule == 0) {
            minSchedule = 15;
        }

        if (maxSchedule == 0) {
            maxSchedule = 1;
        }

        Calendar calendarMinimum = Calendar.getInstance();
        calendarMinimum.setTime(originalCalendar.getTime());
        calendarMinimum.add(Calendar.MINUTE, minSchedule);

        Calendar calendarMaxDays = Calendar.getInstance();
        calendarMaxDays.setTime(originalCalendar.getTime());
        calendarMaxDays.add(Calendar.DAY_OF_MONTH, maxSchedule);
        calendarMaxDays.set(Calendar.SECOND, 59);

        calendar.set(Calendar.SECOND, 59);

        if (calendar.before(calendarMinimum) && !calendar.equals(calendarMinimum)) {
            cartView.showMessage(CacheUtil.getInstance().getTranslations(Text.MINIMUM_SCHEDULED_ORDER, "Unable to process order, scheduled order is allowed minimum of (x) min from current time.").replace("(x)", "" + minSchedule));
        } else if (calendar.after(calendarMaxDays) && calendar != calendarMaxDays) {
            if (maxSchedule == 1) {
                cartView.showMessage(CacheUtil.getInstance().getTranslations(Text.SCHEDULED_ORDER_MAXIMUM));
            } else {
                cartView.showMessage(CacheUtil.getInstance().getTranslations(Text.MAXIMUM_SCHEDULED_ORDER, "Unable to process order, scheduled order is allowed maximum of (x) days from current time.").replace("(x)", "" + maxSchedule));
            }
        } else {
            Calendar nowCalendar = Calendar.getInstance();
            nowCalendar.setTime(new Date());
            nowCalendar.add(Calendar.MINUTE, 15);
            nowCalendar.add(Calendar.SECOND, 30);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            String scheduledTime = TimeUtil.convertTimeUtc(dateFormat.format(calendar.getTime()));
            if (calendar.equals(minSchedule)) {
                scheduledTime = TimeUtil.convertTimeUtc(dateFormat.format(nowCalendar.getTime()));
            }
            cartView.setSchedule(true, scheduledTime);

        }
    }

    @Override
    public String getNotes(int position) {
        if (notes.containsKey(position)) {
            return notes.get(position);
        }
        return "";
    }

    @Override
    public void addNotes(int position, String notesValue) {
        notes.put(position, notesValue);
        ArrayList<MenuItem> cartItems = CacheUtil.getInstance().getCart();
        cartItems.get(position).getAttributes().setNotes(notesValue);
        CacheUtil.getInstance().setCart(cartItems);
    }

    @Override
    public void removeMukafaaRedemption() {
        String transactionId = CacheUtil.getInstance().getMukafaaTransactionId();
        if (transactionId != null && !transactionId.isEmpty()) {
            CacheUtil.getInstance().clearMukafaa();
            cartView.removeMokafaaView();
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
    public double getMukafaaTransactionAmount() {
        double mukafaaTransactionAmount = CacheUtil.getInstance().getMukafaaTransactionAmount();
        if (mukafaaTransactionAmount < 0) mukafaaTransactionAmount = 0;

        return mukafaaTransactionAmount;
    }

    @Override
    public void removeMukafaa() {
        removeMukafaaRedemption();
        validateOrder(false, false, null);
    }

    @Override
    public void setMukafaaTransactionAmount(double amount) {
        CacheUtil.getInstance().setMukafaaTransactionAmount(amount);
    }

    @Override
    public void sendLog(ApiLogLevel level, String message) {
        ApiLogUtil.sendMessageLog(level, message);
    }

}
