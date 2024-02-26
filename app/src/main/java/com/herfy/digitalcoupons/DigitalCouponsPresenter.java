package com.herfy.digitalcoupons;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.herfy.model.CampaignToAdd;
import com.herfy.utils.constants.Text;
import com.herfy.digitalcoupons.views.DigitalCouponViewHolder;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.CouponsApiCall;
import com.skylinedynamics.solosdk.api.calls.MenuApiCall;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.Concept;
import com.skylinedynamics.solosdk.api.models.objects.Customer;
import com.skylinedynamics.solosdk.api.models.objects.Ingredient;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Promotion;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfig;
import com.skylinedynamics.solosdk.util.GsonHelper;
import com.herfy.util.AppLogger;
import com.herfy.utils.analytics.MixpanelUtil;
import com.herfy.utils.handler.ErrorHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class DigitalCouponsPresenter implements DigitalCouponsContract.Presenter {

    private final DigitalCouponsContract.View digitalCouponsView;

    private ArrayList<Campaign> digitalCoupons = new ArrayList<>();

    private Campaign digitalCoupon = null;

    public DigitalCouponsPresenter(DigitalCouponsContract.View digitalCouponsView) {
        this.digitalCouponsView = digitalCouponsView;
        this.digitalCouponsView.setPresenter(this);
    }

    @Override
    public void start() {
        digitalCouponsView.setupViews();
        digitalCouponsView.setupFonts();
        digitalCouponsView.setupTranslations();
    }

    @Override
    public void getDigitalCoupons() {

        CouponsApiCall.getInstance().getGuestCampaigns(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    digitalCoupons.clear();
                    digitalCoupons = GsonHelper.gsonFinal().fromJson(object.getJSONArray("data").toString(), new TypeToken<ArrayList<Campaign>>() {
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
                        try {
                            for (Campaign digitalCoupon : digitalCoupons) {
                                //if (digitalCoupon.getAttributes().isEligible()) {
                                if (digitalCoupon.getPromotion() != null) {
                                    if (digitalCoupon.getPromotion().getAttributes() != null) {
                                        if (digitalCoupon.getPromotion().getAttributes().getType().equalsIgnoreCase("delivery")) {
                                            if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
                                                filteredDigitalCoupons.add(digitalCoupon);
                                            }
                                        } else {
                                            filteredDigitalCoupons.add(digitalCoupon);
                                        }
                                    }
                                }
                                //}
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        digitalCoupons.clear();
                        digitalCoupons.addAll(filteredDigitalCoupons);
                    }

                    digitalCouponsView.showDigitalCoupons(digitalCoupons);

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
    public void setDigitalCoupon(String value) {
        try {
            JSONObject object = new JSONObject(value);
            digitalCoupon = Campaign.parseCoupon(object);
            if(object.has("promotion") && !object.isNull("promotion")) {
                Promotion promotion = GsonHelper.gsonFinal().fromJson(object.getJSONObject("promotion").toString(), Promotion.class);
                if(promotion != null) {
                    digitalCoupon.setPromotion(promotion);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            digitalCoupon = null;
            digitalCouponsView.showError(CacheUtil.getInstance().getTranslations(Text.COUPON_DEFAULT_ERROR), true);
        }
    }

    @Override
    public Campaign getDigitalCoupon() {
        return digitalCoupon;
    }

    @Override
    public void onBindDigitalCouponViewAtPosition(int position, DigitalCouponViewHolder digitalCouponViewHolder) {
        digitalCouponViewHolder.showDigitalCoupon(digitalCoupons.get(position));
    }

    @Override
    public int getDigitalCouponsCount() {
        return digitalCoupons.size();
    }

    @Override
    public double getMenuItemTotalPrice(MenuItem menuItem) {
        String orderStoreId = null;
        if (CacheUtil.getInstance().getOrderStore() != null) {
            orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
        }

        MenuItem trueMenuItem = menuItem;
        double totalPrice = trueMenuItem.getPrice(orderStoreId);

        int comboIndex = 0;
        int trueIndex = 0;
        if (trueMenuItem.getSizes().getModifierItems().size() > 0) {
            if(!trueMenuItem.isPromo()) {
                totalPrice = 0.0;
            }
            for (ModifierItem modifierItem : trueMenuItem.getSizes().getModifierItems()) {
                if (modifierItem.getAttributes().isSelected()) {
                    totalPrice += modifierItem.getAttributes().getPrice();
                    trueIndex = comboIndex;
                    break;
                }
                comboIndex++;
            }
        }
        for (ModifierItem modifierItem : trueMenuItem.getDrinks().getModifierItems()) {
            if (modifierItem.getAttributes().isSelected()) {
                totalPrice += modifierItem.getAttributes().getPrice();
                break;
            }
        }
        for (ModifierItem modifierItem : trueMenuItem.getFries().getModifierItems()) {
            if (modifierItem.getAttributes().isSelected()) {
                totalPrice += modifierItem.getAttributes().getPrice();
                break;
            }
        }
        for (Ingredient ingredient : trueMenuItem.getIngredients()) {
            if (ingredient.getAttributes().isSelected()) {
                totalPrice += ingredient.getAttributes().getPrice();
            }
        }
        for (ModifierGroup modifierGroup : trueMenuItem.getModifierGroups()) {
            VisibilityConfig visibilityConfig = modifierGroup.getAttributes().getVisibilityConfig();
            if (visibilityConfig == null || !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
                for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                    if (modifierItem.getAttributes().isSelected()) {
                        totalPrice += modifierItem.getAttributes().getPrice() * modifierItem.getAttributes().getQuantity();
                    }
                }
            }
        }

        try {
            if (menuItem.isCombo) {
                for (MenuItemComponents components : menuItem.combo.get(trueIndex).components) {
                    for (ComponentModifierItem item : components.items) {
                        if (item.selected) {
                            if (!menuItem.isSandwichOnly)
                                totalPrice += item.price;
                            if (!item.modifierGroups.isEmpty()) {
                                if (components.name.getEnglish().toLowerCase().contains("sandwich") || components.name.getFrench().toLowerCase().contains("sandwich")) {
                                    int getIndex = getYourWayModifier(item.modifierGroups);
                                    if (getIndex != -1) {
                                        for (int i = 0; i < item.modifierGroups.get(getIndex).getModifierItems().size(); i++) {
                                            if (item.modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().isSelected()) {
                                                totalPrice += item.modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().getPrice();
                                            }
                                        }
                                    }
                                } else if ((!menuItem.isSandwichOnly && (!components.name.getEnglish().toLowerCase().contains("sandwich") && !components.name.getFrench().toLowerCase().contains("sandwich"))) || ((!components.name.getEnglish().toLowerCase().contains("sandwich") && !components.name.getFrench().toLowerCase().contains("sandwich")) && menuItem.customFieldObjectMap.isNotItemable)) {
                                    for (ModifierGroup modifierGroup : item.modifierGroups) {
                                        for (int i = 0; i < modifierGroup.getModifierItems().size(); i++) {
                                            if (modifierGroup.getModifierItems().get(i).getAttributes().isSelected()) {
                                                totalPrice += modifierGroup.getModifierItems().get(i).getAttributes().getPrice();
                                            }
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
        //totalPrice *= menuItem.getAttributes().getQuantity();
        return totalPrice;
    }

    private int getYourWayModifier(ArrayList<ModifierGroup> modifierGroups) {
        for (int i = 0; i < modifierGroups.size(); i++) {
            if (modifierGroups.get(i).getAttributes().customFieldObjectMap.label.equalsIgnoreCase("yourway")) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public double getMenuItemsTotalPrice() {
        double subtotalPrice = 0.0;
        for (MenuItem menuItem : CacheUtil.getInstance().getCart()) {
            subtotalPrice += getMenuItemTotalPrice(menuItem) * menuItem.getAttributes().getQuantity();
        }
        return subtotalPrice;
    }

    @Override
    public void validateDigitalCoupon() {

        if(digitalCoupon != null && CacheUtil.getInstance().getCartQuantity() > 0) {

            Customer customer = AuthUtil.getInstance().getCustomer();
            Concept concept = CacheUtil.getInstance().getConcept();
            Store store = CacheUtil.getInstance().getOrderStore();
            ArrayList<MenuItem> menuItems = CacheUtil.getInstance().getCart();

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

//        double discount = 0;
//        if (CacheUtil.getInstance().getCoupon() != null && CacheUtil.getInstance().getPromotion() != null) {
//            Promotion promotion = CacheUtil.getInstance().getPromotion();
//            if (promotion.getAttributes().getType().equalsIgnoreCase("discount")) {
//                if (promotion.getAttributes().getDiscountType().equalsIgnoreCase("fixed")) {
//                    discount = promotion.getAttributes().getDiscountValue();
//                } else if (promotion.getAttributes().getDiscountType().equalsIgnoreCase("percentage")) {
//                    discount = subtotal * (promotion.getAttributes().getDiscountValue() / 100.0f);
//                    if (promotion.getAttributes().getDiscountMax() != -1 && discount > promotion.getAttributes().getDiscountMax()) {
//                        discount = promotion.getAttributes().getDiscountMax();
//                    }
//                }
//            }
//            JsonArray coupons = new JsonArray();
//            coupons.add(CacheUtil.getInstance().getCoupon().getAttributes().getCode());
//            orderBody.add("coupons", coupons);
//        }

            double discount = 0;

            //if ((CacheUtil.getInstance().getCampaign() != null && CacheUtil.getInstance().getCampaignPromotion() != null) || (CacheUtil.getInstance().getCoupon() != null && CacheUtil.getInstance().getPromotion() != null)) {
//        Promotion promotion;
//        if (digitalCoupon != null) {
//            promotion = digitalCoupon.getPromotion();
//            if (promotion.getAttributes().getType().equalsIgnoreCase("discount")) {
//                if (promotion.getAttributes().getDiscountType().equalsIgnoreCase("fixed")) {
//                    discount = promotion.getAttributes().getDiscountValue();
//                } else if (promotion.getAttributes().getDiscountType().equalsIgnoreCase("percentage")) {
//                    discount = subtotal * (promotion.getAttributes().getDiscountValue() / 100.0f);
//                    if (promotion.getAttributes().getDiscountMax() != null && promotion.getAttributes().getDiscountMax() != -1 && discount > promotion.getAttributes().getDiscountMax()) {
//                        discount = promotion.getAttributes().getDiscountMax();
//                    }
//                }
//            }
//        }
            //}

            if (deliveryCharge != 0) {
                boolean hideDiscount = false;
                try {
                    if (digitalCoupon.getPromotion() != null && digitalCoupon.getPromotion().getAttributes().getType().equals("delivery")) {
                        if (digitalCoupon.getPromotion().getAttributes().getDiscountType().equals("percentage")) {
                            if (digitalCoupon.getPromotion().getAttributes().getDiscountValue() >= 100) {
                                hideDiscount = true;
                            } else {
                                deliveryCharge = deliveryCharge * (digitalCoupon.getPromotion().getAttributes().getDiscountValue() / 100);
                            }
                        } else {
                            if (digitalCoupon.getPromotion().getAttributes().getDiscountValue() >= deliveryCharge) {
                                hideDiscount = true;
                            } else {
                                deliveryCharge = deliveryCharge - digitalCoupon.getPromotion().getAttributes().getDiscountValue();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (hideDiscount)
                    deliveryCharge = 0;

            }

            double discountedSubtotal = subtotal - discount;

            if (discountedSubtotal < 0) {
                discountedSubtotal = 0;
                discount = subtotal;
            }

            orderBody.addProperty("discount", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", discount)));

            boolean validDelivery = CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY && (CacheUtil.getInstance().getOrderAddress() != null || CacheUtil.getInstance().getOrderDeliveryAddress() != null);
            boolean validPickup = (CacheUtil.getInstance().getOrderType() == OrderType.PICKUP || CacheUtil.getInstance().getOrderType() == OrderType.DINE_IN || CacheUtil.getInstance().getOrderType() == OrderType.CURBSIDE) && CacheUtil.getInstance().getOrderStore() != null;

            //if (validDelivery || validPickup) {

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
                    orderBody.addProperty("delivery-charge", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", deliveryCharge)));
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
                case NONE:
                    deliveryCharge = 0.0;
                    break;
            }

            //}

            double vatAmount = 0;
//        if (concept.getVatType().equalsIgnoreCase("exclusive")) {
//            vatAmount = (discountedSubtotal + deliveryCharge) * (concept.getVatRate() / 100.0);
//            orderBody.addProperty("vat-amount", Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", vatAmount)));
//        }

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
                    if (visibilityConfig == null || !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
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
                        JSONObject jsonObject = new JSONObject(o.toString());
                        if(jsonObject.has("error") && !jsonObject.isNull("error")) {
                            digitalCouponsView.showError(jsonObject.getString("error"), false);
                        } else {
                            if (digitalCoupon.getPromotion() != null && digitalCoupon.getPromotion().getAttributes().getType().equalsIgnoreCase("item")) {
                                getCampaignPromotionItem();
                            } else {
                                MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_APPLY_PROMO);
                                CacheUtil.getInstance().setCampaign(digitalCoupon);
                                CacheUtil.getInstance().setCampaignPromotion(digitalCoupon.getPromotion());
                                CacheUtil.getInstance().setCampaignPromotionMenuItem(null);
                                CacheUtil.getInstance().setCampaignToAdd(null);
                                digitalCouponsView.showCart();
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
                    digitalCouponsView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.YOU_ARE_NOT_ELIGIBLE_TO_CLAIM_THE_PROMOTION, "You are not eligible to claim the promotion."), null), false);
                }
            });
        } else {
            digitalCouponsView.showError(CacheUtil.getInstance().getTranslations(Text.PLEASE_ADD_ITEMS_TO_YOUR_CART, "Please add items to your cart to avail this offer"), false);
        }

    }

    @Override
    public void getCampaignPromotionItem() {

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

                            MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_APPLY_PROMO);

                            CacheUtil.getInstance().setCampaign(digitalCoupon);
                            CacheUtil.getInstance().setCampaignPromotion(digitalCoupon.getPromotion());

                            if (menuItem.isMustBeCustomized() || menuItem.isCombo) {
                                CacheUtil.getInstance().setCampaignPromotionMenuItem(null);
                                CampaignToAdd campaignToAdd = new CampaignToAdd(
                                    digitalCoupon,
                                    digitalCoupon.getPromotion(),
                                    menuItem
                                );
                                CacheUtil.getInstance().setCampaignToAdd(campaignToAdd);
                            } else {
                                CacheUtil.getInstance().setCampaignPromotionMenuItem(menuItem);
                            }

                            digitalCouponsView.showCart();

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
                    digitalCouponsView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.YOU_ARE_NOT_ELIGIBLE_TO_CLAIM_THE_PROMOTION, "You are not eligible to claim the promotion."), null), false);
                }
            });
        } else {
            digitalCouponsView.showError(CacheUtil.getInstance().getTranslations(Text.YOU_ARE_NOT_ELIGIBLE_TO_CLAIM_THE_PROMOTION, "You are not eligible to claim the promotion."), false);
        }
    }

}
