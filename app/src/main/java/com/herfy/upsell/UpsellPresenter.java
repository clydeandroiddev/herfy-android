package com.herfy.upsell;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.herfy.constants.Values;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.CouponsApiCall;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.calls.MenuApiCall;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.Concept;
import com.skylinedynamics.solosdk.api.models.objects.Customer;
import com.skylinedynamics.solosdk.api.models.objects.Ingredient;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemAttributes;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderData;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Promotion;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.skylinedynamics.solosdk.api.models.objects.Translated;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfig;
import com.skylinedynamics.solosdk.util.GsonHelper;
import com.herfy.util.AppLogger;
import com.herfy.util.AuthUtil;
import com.herfy.util.CacheUtil;
import com.herfy.util.MenuUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class UpsellPresenter implements UpsellContract.Presenter {

    private final UpsellContract.View upsellView;

    private ArrayList<MenuItem> upsells = new ArrayList<>();
    private ArrayList<MenuItem> menuItems = new ArrayList<>();
    private List<Campaign> digitalCoupons = new ArrayList<>();

    public UpsellPresenter(UpsellContract.View upsellView) {
        this.upsellView = upsellView;
        this.upsellView.setPresenter(this);
    }

    @Override
    public void start() {
        upsellView.setupViews();
        upsellView.setupFonts();
        upsellView.setupTranslations();
    }

    @Override
    public boolean checkIfHasNoUpsellSelected() {
        int size = upsells.size();
        for (int i = 0; i < size; i++) {
            if (upsells.get(i).getAttributes().getQuantity() > 0) {
                return false;
            }
        }
        return true;
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
                        ArrayList<ModifierGroup> modGroups = new ArrayList<>();
                        ArrayList<ModifierItem> modItems = new ArrayList<>();
                        if (jsonObject.has("included") && !jsonObject.isNull("included")) {
                            JSONArray includedArray = jsonObject.getJSONArray("included");
                            for (int i = 0; i < includedArray.length(); i++) {
                                JSONObject inclObj = includedArray.getJSONObject(i);
                                if ("modifier".equals(inclObj.getString("type"))) {
                                    ModifierItem mod = GsonHelper.gsonFinal().fromJson(inclObj.toString(), new TypeToken<ModifierItem>() {
                                    }.getType());
                                    String name = mod.getAttributes().getName();
                                    Translated translated = new Translated();
                                    translated.setEnglish(name);
                                    translated.setArabic(name);
                                    translated.setFrench(name);
                                    mod.getAttributes().setTranslatedName(translated);
                                    modItems.add(mod);
                                } else if ("modifier-group".equals(inclObj.getString("type"))) {
                                    ModifierGroup group = GsonHelper.gsonFinal().fromJson(inclObj.toString(), new TypeToken<ModifierGroup>() {
                                    }.getType());
                                    modGroups.add(group);
                                }
                            }
                        }
                        upsells = GsonHelper.gsonFinal().fromJson(dataArray.toString(), new TypeToken<ArrayList<MenuItem>>() {
                        }.getType());
                        for (MenuItem upsell : upsells) {
                            upsell.getAttributes().setQuantity(0);
                            LinkedList<ModifierGroup> upsellGroups = new LinkedList<>();
                            for (ModifierGroup group : upsell.getRelationships().getModifierGroups().getModifierGroups()) {
                                for (ModifierGroup inclGroup : modGroups) {
                                    if (group.getId().equals(inclGroup.getId())) {
                                        try {
                                            ArrayList<ModifierItem> inclModItems = inclGroup.getRelationships().getModifiers().getModifierItems();
                                            LinkedList<ModifierItem> upsellMods = new LinkedList<>();
                                            for (ModifierItem inclModItem : inclModItems) {
                                                for (ModifierItem modItem : modItems) {
                                                    if (inclModItem.getId().equals(modItem.getId())) {
                                                        upsellMods.add(modItem);
                                                    }
                                                }
                                            }
                                            try {
                                                for (int x = 0; x < inclGroup.getAttributes().getMinimum() && upsellMods.size() > x; x++) {
                                                    upsellMods.get(x).getAttributes().setSelected(true);
                                                }
                                            } catch (ArrayIndexOutOfBoundsException e) {
                                            }
                                            inclGroup.setModifierItems(upsellMods);
                                        } catch (NullPointerException e) {
                                        }
                                        if ("sizes".equals(inclGroup.getAttributes().getCode())) {
                                            if (!inclGroup.getModifierItems().isEmpty())
                                                inclGroup.getModifierItems().get(0).getAttributes().setSelected(true);
                                            upsell.setSizes(inclGroup);
                                        } else {
                                            upsellGroups.add(inclGroup);
                                        }
                                    }
                                }
                            }
                            upsell.setModifierGroups(upsellGroups);
                            upsell.toCdn(CacheUtil.getOrderStoreId());
                        }
                        Collections.sort(upsells, new Comparator<MenuItem>() {
                            @Override
                            public int compare(MenuItem menuItem1, MenuItem menuItem2) {
                                return menuItem1.getAttributes().getDisplayOrder() - menuItem2.getAttributes().getDisplayOrder();
                            }
                        });

                        upsellView.showUpsells(upsells);
                        upsellView.setUpsellTotalPrice(0);

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
                upsells = new ArrayList<>();
                upsellView.showUpsells(upsells);
            }
        });
    }

    @Override
    public void onBindUpsellViewAtPosition(int position, UpsellViewHolder upsellViewHolder) {
        upsellViewHolder.setUpsell(upsells.get(position));
    }

    @Override
    public int getUpsellsCount() {
        return upsells.size();
    }

    @Override
    public void changeQuantity(int position, boolean add) {
        MenuItem menuItem = upsells.get(position);
        if (!add && menuItem.getAttributes().getQuantity() != 0) {
            menuItem.getAttributes().setQuantity(menuItem.getAttributes().getQuantity() - 1);
            menuItem.getAttributes().setUpsellQuantity(menuItem.getAttributes().getUpsellQuantity() - 1);
        } else if (add) {
            menuItem.getAttributes().setQuantity(menuItem.getAttributes().getQuantity() + 1);
            menuItem.getAttributes().setUpsellQuantity(menuItem.getAttributes().getUpsellQuantity() + 1);
        }
        upsells.set(position, menuItem);
        upsellView.showUpsells(upsells);
        double totalPrice = 0;
        for (MenuItem upsellItem : upsells) {
            totalPrice += upsellItem.getAttributes().getPrice(CacheUtil.getOrderStoreId()) * upsellItem.getAttributes().getQuantity();
        }
        upsellView.setUpsellTotalPrice(totalPrice);
    }

    @Override
    public void changeToZero(int position) {
        MenuItem menuItem = upsells.get(position);
        menuItem.getAttributes().setQuantity(0);
        menuItem.getAttributes().setUpsellQuantity(0);
        upsells.set(position, menuItem);
        upsellView.showUpsells(upsells);
        double totalPrice = 0;
        for (MenuItem upsellItem : upsells) {
            totalPrice += upsellItem.getAttributes().getPrice(CacheUtil.getOrderStoreId()) * upsellItem.getAttributes().getQuantity();
        }
        upsellView.setUpsellTotalPrice(totalPrice);
    }

    @Override
    public void continueCheckout() {
        HashMap<String, String> eventAttributes = new HashMap<>();
        double price = 0;
        boolean hasAdded = false;
        for (MenuItem upsell : upsells) {
            if (upsell.getAttributes().getQuantity() > 0) {
                hasAdded = true;
                eventAttributes.put("Item Name", upsell.getAttributes().getName());
                price += upsell.getAttributes().getPrice(CacheUtil.getOrderStoreId());
                upsell.getAttributes().setUpsell(true);
                upsellView.logFirebaseEvent("add_to_cart_event");
                removeMukafaaRedemption();
                Values.fromUpsell = true;
                CacheUtil.getInstance().addToCart(upsell);
            }
        }
        upsellView.logEvent("Upsell", eventAttributes, "Price", price);
        if (Values.NO_PROMOTIONS) {
            upsellView.continueCheckout();
        } else {
            if (hasAdded) {
                if (CacheUtil.getInstance().getCampaign() != null) {
                    validateDigitalCoupon(CacheUtil.getInstance().getCampaign());
                } else {
                    upsellView.continueCheckout();
                }
            } else {
                upsellView.continueCheckout();
            }
        }
    }

    @Override
    public double getMenuItemsTotalPrice() {
        double subtotalPrice = 0.0;
        for (MenuItem menuItem : menuItems) {
            subtotalPrice += getMenuItemTotalPrice(menuItem) * menuItem.getAttributes().getQuantity();
        }
        return subtotalPrice;
    }


    @Override
    public double getMenuItemTotalPrice(MenuItem menuItem) {
        double totalPrice = menuItem.getAttributes().getPrice(CacheUtil.getOrderStoreId());
        if (menuItem.getSizes().getModifierItems().size() > 0) {
            if(!menuItem.isPromo()) {
                totalPrice = 0.0;
            }
            for (ModifierItem modifierItem : menuItem.getSizes().getModifierItems()) {
                if (modifierItem.getAttributes().isSelected()) {
                    totalPrice += modifierItem.getAttributes().getPrice();
                    break;
                }
            }
        }
        for (ModifierItem modifierItem : menuItem.getDrinks().getModifierItems()) {
            if (modifierItem.getAttributes().isSelected()) {
                totalPrice += modifierItem.getAttributes().getPrice();
                break;
            }
        }
        for (ModifierItem modifierItem : menuItem.getFries().getModifierItems()) {
            if (modifierItem.getAttributes().isSelected()) {
                totalPrice += modifierItem.getAttributes().getPrice();
                break;
            }
        }
        for (Ingredient ingredient : menuItem.getIngredients()) {
            if (ingredient.getAttributes().isSelected()) {
                totalPrice += ingredient.getAttributes().getPrice();
            }
        }
        for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
            VisibilityConfig visibilityConfig = modifierGroup.getAttributes().getVisibilityConfig();
            if (visibilityConfig == null || visibilityConfig.getInitialValue() == null || !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
                for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                    if (modifierItem.getAttributes().isSelected()) {
                        totalPrice += modifierItem.getAttributes().getPrice() * modifierItem.getAttributes().getQuantity();
                    }
                }
            }

        }
        //totalPrice *= menuItem.getAttributes().getQuantity();
        return totalPrice;
    }


    @Override
    public void getDigitalCoupons(boolean checkout) {
        if (AuthUtil.getInstance().isSignedIn()) {

            boolean validDelivery = CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY && CacheUtil.getInstance().getOrderAddress() != null;
            boolean validPickup = (CacheUtil.getInstance().getOrderType() == OrderType.PICKUP || CacheUtil.getInstance().getOrderType() == OrderType.DINE_IN || CacheUtil.getInstance().getOrderType() == OrderType.CURBSIDE) && CacheUtil.getInstance().getOrderStore() != null;

            Customer customer = AuthUtil.getInstance().getCustomer();
            Concept concept = CacheUtil.getInstance().getConcept();
            Store store = CacheUtil.getInstance().getOrderStore();
            List<MenuItem> menuItems = CacheUtil.getInstance().getCart();

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
            orderBody.addProperty("customer", customer.getId());

            orderBody.addProperty("source", "Android");
            orderBody.addProperty("notes", CacheUtil.getInstance().getCartNotes());
            orderBody.addProperty("subtotal", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", subtotal)));

            double discount = 0;
            String couponCode = CacheUtil.getInstance().getCouponCode();
            if (couponCode != null) {
                orderBody.addProperty("coupon-code", couponCode);
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
                        if (CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().size() == 3 || CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().contains(OrderType.DINE_IN)) {
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
                            orderBody.addProperty("car-id", CacheUtil.getInstance().getCurbsideCar().id);
                        }
                        orderBody.addProperty("type", "curbside");
                        break;
                }
            }

            double vatAmount = 0;
            if (concept.getVatType().equalsIgnoreCase("exclusive")) {
                vatAmount = (discountedSubtotal + deliveryCharge) * (concept.getVatRate() / 100.0);
                orderBody.addProperty("vat-amount", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", vatAmount)));
            }

            double total = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", discountedSubtotal + deliveryCharge + vatAmount));
            if (total < 0) {
                total = 0;
            }

            orderBody.addProperty("total", total);

            JsonArray items = new JsonArray();

            for (MenuItem menuItem : menuItems) {

                JsonObject item = new JsonObject();
                JsonArray modifiers = new JsonArray();
                JsonArray ingredients = new JsonArray();

                item.addProperty("id", menuItem.getId());
                item.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", menuItem.getAttributes().getPrice(CacheUtil.getOrderStoreId()))));
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

            if (validDelivery || validPickup) {

                CouponsApiCall.getInstance().getCampaignsCancelable(orderBody, new ApiRequestListener() {
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

                                List<Campaign> filteredDigitalCoupons = new ArrayList<>();
                                for (Campaign digitalCoupon : digitalCoupons) {
                                    if (digitalCoupon.getAttributes().isEligible()) {
                                        if (digitalCoupon.getPromotion() != null && digitalCoupon.getPromotion().getAttributes().getType().equalsIgnoreCase("delivery")) {
                                            if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
                                                filteredDigitalCoupons.add(digitalCoupon);
                                            }
                                        } else {
                                            filteredDigitalCoupons.add(digitalCoupon);
                                        }
                                    }
                                }

                                digitalCoupons.clear();
                                digitalCoupons.addAll(filteredDigitalCoupons);
                            }

                            String digitalCouponString = new Gson().toJson(digitalCoupons);

                            if (digitalCoupons.size() > 0) {
                                int selected = -1;
                                if (CacheUtil.getInstance().getCampaign() != null) {
                                    for (int i = 0; i < digitalCoupons.size(); i++) {
                                        if (digitalCoupons.get(i).getId().equalsIgnoreCase(CacheUtil.getInstance().getCampaign().getId())) {
                                            selected = i;
                                            break;
                                        }
                                    }
                                }
                                if (CacheUtil.getInstance().getCartQuantity() > 0) {
                                    if (CacheUtil.getInstance().getCampaign() != null) {
                                        //    upsellView.showDigitalCoupons(checkout, digitalCoupons, selected);
                                        // } else {
                                        validateDigitalCoupon(CacheUtil.getInstance().getCampaign());
                                    }
//                                    if (!CartUtil.isSamePromoResponse(digitalCoupons)) {
//                                        upsellView.showDigitalCoupons(checkout, digitalCoupons, selected);
//                                    } else {
//                                        if (CacheUtil.getInstance().getCampaign() != null) {
//                                        //    upsellView.showDigitalCoupons(checkout, digitalCoupons, selected);
//                                       // } else {
//                                            validateDigitalCoupon(CacheUtil.getInstance().getCampaign());
//                                        }
//                                    }
                                    CacheUtil.getInstance().setDigitalCoupons(digitalCouponString);
                                }
                            } else {
                                upsellView.upsellPromotionSucess(false);
                                this.onError("");
                            }

                        } catch (JSONException ex) {
                            upsellView.upsellPromotionSucess(false);
                            ex.printStackTrace();
                            this.onError("");
                        }
                    }

                    @Override
                    public void onError(Object o) {
                        upsellView.upsellPromotionSucess(false);
                    }
                });
            }
        }
    }

    @Override
    public void validateDigitalCoupon(Campaign digitalCoupon) {

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
        String couponCode = CacheUtil.getInstance().getCouponCode();
        if (couponCode != null) {
            orderBody.addProperty("coupon-code", couponCode);
        }

        double discountedSubtotal = subtotal - discount;

        if (discountedSubtotal < 0) {
            discountedSubtotal = 0;
            discount = subtotal;
        }

        orderBody.addProperty("discount", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", discount)));

        boolean validDelivery = CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY && CacheUtil.getInstance().getOrderAddress() != null;
        boolean validPickup = (CacheUtil.getInstance().getOrderType() == OrderType.PICKUP || CacheUtil.getInstance().getOrderType() == OrderType.DINE_IN || CacheUtil.getInstance().getOrderType() == OrderType.CURBSIDE) && CacheUtil.getInstance().getOrderStore() != null;

        if (validDelivery || validPickup) {

            switch (CacheUtil.getInstance().getOrderType()) {
                case PICKUP:
                    deliveryCharge = 0.0;
                    if (CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().size() == 3 || CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().contains(OrderType.DINE_IN)) {
                        orderBody.addProperty("type", "to-go");
                    } else {
                        orderBody.addProperty("type", "pickup");
                    }
                    orderBody.addProperty("location", store.getId());
                    break;
                case DINE_IN:
                    deliveryCharge = 0.0;
                    orderBody.addProperty("type", "eat-in");
                    orderBody.addProperty("location", store.getId());
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
                    if (CacheUtil.getInstance().getCurbsideCar() != null) {
                        orderBody.addProperty("location", CacheUtil.getInstance().getOrderStore().getId());
                        orderBody.addProperty("car-id", CacheUtil.getInstance().getCurbsideCar().id);
                        orderBody.addProperty("type", "curbside");
                    }
                    break;
            }

        }

        double vatAmount = 0;
        if (concept.getVatType().equalsIgnoreCase("exclusive")) {
            vatAmount = (discountedSubtotal + deliveryCharge) * (concept.getVatRate() / 100.0);
            orderBody.addProperty("vat-amount", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", vatAmount)));
        }

        double total = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", discountedSubtotal + deliveryCharge + vatAmount));
        if (total < 0) {
            total = 0;
        }

        orderBody.addProperty("total", total);

        JsonArray items = new JsonArray();

        for (MenuItem menuItem : menuItems) {

            JsonObject item = new JsonObject();
            JsonArray modifiers = new JsonArray();
            JsonArray ingredients = new JsonArray();

            item.addProperty("id", menuItem.getId());
            item.addProperty("price", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", menuItem.getAttributes().getPrice(CacheUtil.getOrderStoreId()))));
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

        CouponsApiCall.getInstance().validatePromotion(orderBody, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    CacheUtil.getInstance().setCampaign(digitalCoupon);
                    CacheUtil.getInstance().setCampaignPromotion(digitalCoupon.getPromotion());
                    CacheUtil.getInstance().setCampaignPromotionMenuItem(null);
                    CacheUtil.getInstance().setCampaignToAdd(null);

                    OrderData orderData = OrderData.parse(new JSONObject(o.toString()));
                    if (orderData.getTotal() < 0) {
                        orderData.setTotal(0);
                    }

                    JSONObject jsonObject = new JSONObject(o.toString());
                    if (CacheUtil.getInstance().getCartQuantity() > 0) {
                        try {
                            String items = jsonObject.isNull("items") ? "" : jsonObject.optString("items");
                            List<MenuItem> itemsList = GsonHelper.gsonFinal().fromJson(items, new TypeToken<List<MenuItem>>() {
                            }.getType());
                            int size = itemsList.size();
                            for (int i = 0; i < size; i++) {
                                if (itemsList.get(i).getIsPromotion()) {
                                    MenuItem mItem = itemsList.get(i);
                                    MenuItemAttributes attributes = new MenuItemAttributes();
                                    attributes.setImage(digitalCoupon.getAttributes().getImageUri());
                                    attributes.setName(digitalCoupon.getPromotion().getAttributes().getItemName());
                                    attributes.setPrice(digitalCoupon.getPromotion().getAttributes().getItemPrice());
                                    String description = "";
                                    if (digitalCoupon.getAttributes().getDescription() != null) {
                                        if (!digitalCoupon.getAttributes().getDescription().isEmpty()) {
                                            description = digitalCoupon.getAttributes().getDescription();
                                        } else {
                                            description = digitalCoupon.getPromotion().getAttributes().getName();
                                        }
                                    } else {
                                        description = digitalCoupon.getPromotion().getAttributes().getName();
                                    }
                                    attributes.setDescription(description);
                                    mItem.setAttributes(attributes);
                                    CacheUtil.getInstance().setCampaignPromotionMenuItem(mItem);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        upsellView.upsellPromotionSucess(true);

                    }

                } catch (JSONException ex) {
                    upsellView.upsellPromotionSucess(false);
                    ex.printStackTrace();
                    this.onError("");
                }

            }

            @Override
            public void onError(Object o) {
                upsellView.upsellPromotionSucess(false);
            }
        });
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
    public int getUpsellsList() {
        return upsells.size();
    }

    @Override
    public ArrayList<MenuItem> getCurrentUpsells() {
        return upsells;
    }

    @Override
    public void getUpsellTotalPrice() {
        double totalPrice = 0;
        for (MenuItem upsellItem : upsells) {
            totalPrice += upsellItem.getAttributes().getPrice(CacheUtil.getOrderStoreId()) * upsellItem.getAttributes().getQuantity();
        }
        upsellView.setUpsellTotalPrice(totalPrice);
    }

    @Override
    public void goToItemDetails(int position, MenuItem upsell) {
        upsellView.goToItemDetails(position, upsell);
    }

}
