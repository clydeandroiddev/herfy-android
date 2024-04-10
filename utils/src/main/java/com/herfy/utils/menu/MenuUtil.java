package com.herfy.utils.menu;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.herfy.utils.constants.Text;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.utils.helper.StringUtil;
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.CustomFieldObject;
import com.skylinedynamics.solosdk.api.models.objects.Favorite;
import com.skylinedynamics.solosdk.api.models.objects.FavoriteIngredients;
import com.skylinedynamics.solosdk.api.models.objects.FavoriteModifier;
import com.skylinedynamics.solosdk.api.models.objects.Ingredient;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupCdn;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.OrderTypeMenus;
import com.skylinedynamics.solosdk.api.models.objects.TimedEvent;
import com.skylinedynamics.solosdk.api.models.objects.Translated;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfig;
import com.skylinedynamics.solosdk.util.GsonHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class MenuUtil {

    public static String getMenuCdnUrl() {
        String url = CacheUtil.getInstance().getApplication().getAttributes().getMenuRef();
        long timestamp = System.currentTimeMillis() / 1000L;
        return url + "?timestamp=" + timestamp;
    }

    public static String getDiffMenuCdnUrl() {
        OrderType orderType = CacheUtil.getInstance().getOrderType();

        String url = "";

        List<OrderTypeMenus> orderTypeMenus = CacheUtil.getInstance().getApplication().getAttributes().getOrderTypeMenus();

        if (CacheUtil.getInstance().isSeparatedMenus()) {
            boolean isPickup = CacheUtil.getInstance().pickupTypes();
            String differentMenuId = "";
            for (OrderTypeMenus menus : orderTypeMenus) {
                if (isPickup && (
                        Objects.requireNonNull(menus.getType()).equalsIgnoreCase("pickup") ||
                                Objects.requireNonNull(menus.getType()).equalsIgnoreCase("eat-in") ||
                                Objects.requireNonNull(menus.getType()).equalsIgnoreCase("drive-thru")
                )) {
                    differentMenuId = menus.getId();
                    url = menus.getMenuUri();
                    break;
                } else if (orderType == OrderType.DELIVERY && Objects.requireNonNull(menus.getType()).equalsIgnoreCase("deliver")) {
                    differentMenuId = menus.getId();
                    url = menus.getMenuUri();
                    break;
                } else if (orderType == OrderType.DINE_IN && Objects.requireNonNull(menus.getType()).equalsIgnoreCase("dine-in")) {
                    differentMenuId = menus.getId();
                    url = menus.getMenuUri();
                    break;
                }
            }
            CacheUtil.getInstance().setDifferentMenuId(differentMenuId);
        }

        if (url.isEmpty()) {
            url = CacheUtil.getInstance().getApplication().getAttributes().getMenuRef();
        }

        long timestamp = System.currentTimeMillis() / 1000L;
        return url + "?timestamp=" + timestamp;
    }

    public static String getDiffMenuId() {
        OrderType orderType = CacheUtil.getInstance().getOrderType();

        String menuId = "";

        List<OrderTypeMenus> orderTypeMenus = CacheUtil.getInstance().getApplication().getAttributes().getOrderTypeMenus();

        if (CacheUtil.getInstance().isSeparatedMenus()) {
            boolean isPickup = CacheUtil.getInstance().pickupTypes();
            for (OrderTypeMenus menus : orderTypeMenus) {
                if (isPickup && (
                        Objects.requireNonNull(menus.getType()).equalsIgnoreCase("pickup") ||
                                Objects.requireNonNull(menus.getType()).equalsIgnoreCase("eat-in") ||
                                Objects.requireNonNull(menus.getType()).equalsIgnoreCase("drive-thru")
                )) {
                    menuId = menus.getId();
                    break;
                } else if (orderType == OrderType.DELIVERY && Objects.requireNonNull(menus.getType()).equalsIgnoreCase("deliver")) {
                    menuId = menus.getId();
                    break;
                } else if (orderType == OrderType.DINE_IN && Objects.requireNonNull(menus.getType()).equalsIgnoreCase("dine-in")) {
                    menuId = menus.getId();
                    break;
                }
            }
        }

        if (menuId.isEmpty()) {
            menuId = CacheUtil.getInstance().getMenuId();
        }
        return menuId;
    }

    public static boolean isItemAvailable(TimedEvent timedEvent, String current) {

        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        day = day - 1;
        TimedEvent.Event event = timedEvent.getEventMap().get(day);

        if (event != null) {
            try {
                SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
                timeSdf.setTimeZone(TimeZone.getTimeZone("GMT+0"));
                SimpleDateFormat currentTimeSdf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
                currentTimeSdf.setTimeZone(TimeZone.getTimeZone("GMT+3"));

                Date startDate = dateSdf.parse(timedEvent.getFromDate());
                Date endDate = dateSdf.parse(timedEvent.getToDate());
                Date currentDate = dateSdf.parse(current.substring(0, current.length() - 3));

                Date fromTime = timeSdf.parse(event.getFrom());
                Date toTime = timeSdf.parse(event.getTo());
                Date currentTime = currentTimeSdf.parse(current.substring(11));

                if (currentDate != null && currentTime != null) {
                    if (currentDate.equals(startDate) || currentDate.equals(endDate) || (currentDate.after(startDate) && currentDate.before(endDate))) {
                        if ((timedEvent.getIsActive() == 1 && (currentTime.equals(fromTime) || currentTime.equals(toTime) || (currentTime.after(fromTime) && currentTime.before(toTime)))) || (timedEvent.getIsActive() == 1 && (currentTime.equals(fromTime) || currentTime.equals(toTime) || (currentTime.before(fromTime) && currentTime.after(toTime))))) {
                            return true;
                        }
                    }
                }

            } catch (ParseException | NullPointerException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static String getTimedEventError(TimedEvent timedEvent) {
        String note = CacheUtil.getInstance().getTranslations(Text.TIME_EVENT_AVAILABLE);
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        day = day - 1;
        TimedEvent.Event event = timedEvent.getEventMap().get(day);

        if (event != null) {
            String fromDate = timedEvent.getFromDate();
            String toDate = timedEvent.getToDate();
            String from = event.getFrom();
            String to = event.getTo();
            SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            SimpleDateFormat api = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
            api.setTimeZone(TimeZone.getTimeZone("GMT+0"));

            try {
                Date startDate = dateSdf.parse(fromDate);
                Date endDate = dateSdf.parse(toDate);
                Date f = api.parse(from);
                Date t = api.parse(to);
                SimpleDateFormat date = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);
                SimpleDateFormat phone = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
                phone.setTimeZone(TimeZone.getDefault());
                fromDate = date.format(startDate);
                toDate = date.format(endDate);
                from = phone.format(f);
                to = phone.format(t);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            String fromDateString = StringUtil.localize(String.format(Locale.getDefault(), "%s", fromDate));
            String toDateString = StringUtil.localize(String.format(Locale.getDefault(), "%s", toDate));
            String fromString = String.format(Locale.getDefault(), "%s", from);
            String toString = String.format(Locale.getDefault(), "%s", to);

            if (!LocaleUtil.getInstance().isEnglish()) {
                fromString = StringUtil.localize(fromString).replace("AM", CacheUtil.getInstance().getTranslations(Text.AM));
                fromString = StringUtil.localize(fromString).replace("PM", CacheUtil.getInstance().getTranslations(Text.PM));
                toString = StringUtil.localize(toString).replace("AM", CacheUtil.getInstance().getTranslations(Text.AM));
                toString = StringUtil.localize(toString).replace("PM", CacheUtil.getInstance().getTranslations(Text.PM));
            }

            if (timedEvent.getIsActive() == 1) {
                note = CacheUtil.getInstance().getTranslations(Text.TIME_EVENT_AVAILABLE).replace("(a)", fromDateString).replace("(b)", toDateString).replace("(x)", fromString).replace("(y)", toString);
            } else {
                note = CacheUtil.getInstance().getTranslations(Text.TIME_EVENT_UNAVAILABLE, "Sorry, this item is not available from (a) to (b) during these hours only (x) to (y)").replace("(a)", fromDateString).replace("(b)", toDateString).replace("(x)", fromString).replace("(y)", toString);
            }

        }
        return note;
    }

    public static MenuItem getFavoriteMenuItem(Favorite favorite) {

        MenuItem menuItem = new MenuItem();

        menuItem.setId(favorite.getId());
        menuItem.getAttributes().setName(favorite.getAttributes().getName());
        menuItem.getAttributes().setImage(favorite.getAttributes().getImageUri());
        menuItem.getAttributes().setPrice(favorite.getAttributes().getPrice());
        menuItem.getAttributes().setQuantity(1);

        for (FavoriteModifier favoriteModifier : favorite.getAttributes().getModifiers()) {
            ModifierItem modifierItem = new ModifierItem();
            modifierItem.setId(String.valueOf(favoriteModifier.getId()));
            String name = favoriteModifier.getName();
            Translated translated = new Translated();
            translated.setEnglish(name);
            translated.setArabic(name);
            translated.setFrench(name);
            modifierItem.getAttributes().setName(favoriteModifier.getName());
            modifierItem.getAttributes().setTranslatedName(translated);
            modifierItem.getAttributes().setPrice(favoriteModifier.getPrice());
            modifierItem.getAttributes().setQuantity(favoriteModifier.getQuantity());
            List<ModifierItem> items = new ArrayList<>(menuItem.getModifierItems());
            items.add(modifierItem);
            menuItem.setModifierItems(items);
        }

        for (FavoriteIngredients favoriteIngredient : favorite.getAttributes().getIngredientsList()) {
            Ingredient ingredient = new Ingredient();
            ingredient.setId(String.valueOf(favoriteIngredient.getIngredientId()));
            ingredient.getAttributes().setName(favoriteIngredient.getName());
            ingredient.getAttributes().setPrice(favoriteIngredient.getPrice());
            ingredient.getAttributes().setQuantity(favoriteIngredient.getQuantity());
            List<Ingredient> items = new ArrayList<>(menuItem.getIngredients());
            items.add(ingredient);
            menuItem.setIngredients(items);
        }

        return menuItem;

    }

    public static MenuItem processMenuItemCustomObject(MenuItem menuItem) {
        String cObject = new Gson().toJson(menuItem.customField).replace(" ", "");
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
                    } else if (inner[0].equalsIgnoreCase("nutritional-info-image")) {
                        menuItem.customFieldObjectMap.nutritionInfoImageUrl = inner[1];
                    } else if (inner[0].equalsIgnoreCase("nutritional-info-story")) {
                        menuItem.customFieldObjectMap.nutritionInfoStory = inner[1];
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
        return menuItem;
    }

    public static ModifierGroupCdn processModifierGroupCustomObject(ModifierGroupCdn modifierGroupCdn) {
        String cObject = modifierGroupCdn.getAttributes().customField.toString().replace(" ", "");
        try {
            if (!String.valueOf(cObject.charAt(0)).equals("[")) {
                modifierGroupCdn.getAttributes().customFieldObjectMap = GsonHelper.gsonFinal().fromJson(cObject, CustomFieldObject.class);
            }
        } catch (Exception e) {
            String[] separated = cObject.split(",");
            for (int i = 0; i < separated.length; i++) {
                String[] inner = separated[i].replace("{", "").replace("}", "").split("=");
                if (inner.length == 2) {
                    if (inner[0].equalsIgnoreCase("top_bun")) {
                        modifierGroupCdn.getAttributes().customFieldObjectMap.topBun = inner[1];
                    } else if (inner[0].equalsIgnoreCase("bottom_bun")) {
                        modifierGroupCdn.getAttributes().customFieldObjectMap.bottomBun = inner[1];
                    } else if (inner[0].equalsIgnoreCase("is-ingredient")) {
                        modifierGroupCdn.getAttributes().customFieldObjectMap.isIngredient = inner[1];
                    } else if (inner[0].equalsIgnoreCase("modifier-group")) {
                        modifierGroupCdn.getAttributes().customFieldObjectMap.modifierGroup = inner[1];
                    } else if (inner[0].equalsIgnoreCase("label")) {
                        modifierGroupCdn.getAttributes().customFieldObjectMap.label = inner[1];
                    } else if (inner[0].equalsIgnoreCase("category-highlight")) {
                        modifierGroupCdn.getAttributes().customFieldObjectMap.categoryHighlight = inner[1].equalsIgnoreCase("true");
                    } else if (inner[0].equalsIgnoreCase("cannot-switch-meal-sandwich")) {
                        modifierGroupCdn.getAttributes().customFieldObjectMap.isNotItemable = inner[1].equalsIgnoreCase("true");
                    } else if (inner[0].equalsIgnoreCase("pattys")) {
                        modifierGroupCdn.getAttributes().customFieldObjectMap.patty = inner[1];
                    } else if (inner[0].equalsIgnoreCase("disable-pos-android")) {
                        modifierGroupCdn.getAttributes().customFieldObjectMap.disablePosAndroid = inner[1];
                    } else if (inner[0].equalsIgnoreCase("kiosk_address_id")) {
                        modifierGroupCdn.getAttributes().customFieldObjectMap.kioskAddressId = inner[1];
                    } else if (inner[0].equalsIgnoreCase("nutritional-info-image")) {
                        modifierGroupCdn.getAttributes().customFieldObjectMap.nutritionInfoImageUrl = inner[1];
                    } else if (inner[0].equalsIgnoreCase("nutritional-info-image")) {
                        modifierGroupCdn.getAttributes().customFieldObjectMap.nutritionInfoStory = inner[1];
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
                modifierGroupCdn.getAttributes().customFieldObjectMap.patty = new Gson().toJson(list);
            } catch (Exception ie) {
                ie.printStackTrace();
            }
            e.printStackTrace();
        }
        return modifierGroupCdn;
    }

    public static boolean isExcluded(List<String> integerSet) {
        try {
            if (integerSet.size() > 0) {
                int id = Integer.parseInt(CacheUtil.getInstance().getApplication().getId());
                return integerSet.contains(String.valueOf(id));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getCaloriesText(@Nullable String caloriesValue, @Nullable Integer calorieCount) {
        String caloriesText = "";
        if (calorieCount != null && calorieCount != 0) {
            caloriesText = calorieCount.toString();
        } else if(caloriesValue != null && !caloriesValue.isEmpty()) {
            caloriesText = caloriesValue;
        }
        return CacheUtil.getInstance().getTranslations(Text.CALORIES_KCAL, "(x) Kcal").replace("(x)", caloriesText);
    }

    public static double getMenuItemTotalPrice(MenuItem item) {

        String orderStoreId = null;
        int counter = 0;
        int comboIndex = 0;
        if (CacheUtil.getInstance().getOrderStore() != null) {
            orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
        }

        double totalPrice = item.getPrice(orderStoreId);
        if (!item.getSizes().getModifierItems().isEmpty()) {
            if(!item.getIsPromotion()) {
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
                            if (!item.isSandwichOnly)
                                totalPrice += componentModifierItem.price;
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

        return totalPrice;
    }

    public static boolean isCustomizeable(MenuItem item) {
        return item.isCustomizable() || item.isMustBeCustomized() || item.isCombo;
    }
     public static int modifierItemQuantityCount(ModifierGroup modifierGroup) {
        int counter = 0;
        for (ModifierItem item : modifierGroup.getModifierItems()) {
            if (item.getAttributes().isSelected()) {
                counter += item.getAttributes().getQuantity();
            }
        }
        return counter;
    }
     public static boolean isAboveMaxModifierRequired(ModifierGroup modifierGroup,int selectedItems) {
        return selectedItems >= modifierGroup.getAttributes().getMaximum();
    }
    public static boolean shouldShowQuantity(ModifierGroup modifierGroup, ModifierItem modifierItem) {
        return modifierGroup.getAttributes().getMaximum() > 1 &&
                modifierItem.getAttributes().getMaximum() > 1;
    }

    public static boolean areModifiersSame(ModifierItem currentModifier, ModifierItem selectedModifier) {
        return Objects.equals(currentModifier.getId(), selectedModifier.getId());
    }
}
