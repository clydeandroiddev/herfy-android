package com.herfy.favorites;

import android.widget.ImageView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.herfy.utils.constants.Text;
import com.herfy.database.AppDatabase;
import com.herfy.database.entities.MenuItemDb;
import com.herfy.database.entities.MenuItemEntity;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.BuildUtil;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.ApplicationsApiCall;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.calls.MenuApiCall;
import com.skylinedynamics.solosdk.api.models.Pagination;
import com.skylinedynamics.solosdk.api.models.objects.Allergen;
import com.skylinedynamics.solosdk.api.models.objects.AllergenAttributes;
import com.skylinedynamics.solosdk.api.models.objects.Concept;
import com.skylinedynamics.solosdk.api.models.objects.Favorite;
import com.skylinedynamics.solosdk.api.models.objects.Ingredient;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupType;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.skylinedynamics.solosdk.api.models.objects.TimedEvent;
import com.skylinedynamics.solosdk.api.models.objects.Translated;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfig;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfigRule;
import com.skylinedynamics.solosdk.util.BooleanTypeAdapter;
import com.skylinedynamics.solosdk.util.GsonHelper;
import com.herfy.util.AppLogger;
import com.herfy.util.ErrorHandler;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.MenuUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class FavoritesPresenter implements FavoritesContract.Presenter {

    private final FavoritesContract.View favoritesView;

    private ArrayList<Favorite> favorites = new ArrayList<>();
    private ArrayList<Favorite> searchFavorites = new ArrayList<>();

    ImageView imageView;
    private boolean fromSearch;
    private String currentSearchText;
    private MenuItem itemById = new MenuItem();
    private MenuItemDb menuItemDb = new MenuItemDb();
    private double listPrice = 0.0;
    private String orderStoreId = null;
    private String id = "";

    public FavoritesPresenter(FavoritesContract.View favoritesView) {
        this.favoritesView = favoritesView;
        this.favoritesView.setPresenter(this);
    }

    @Override
    public void start() {
        favoritesView.setupViews();
        favoritesView.setupFonts();
        favoritesView.setupTranslations();
    }

    @Override
    public void getFavorites(int page) {
        CustomersApiCall.getInstance().getAllCustomerFavoriteItems(AuthUtil.getInstance().getCustomer().getId(), String.valueOf(page), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());

                    Pagination pagination = Pagination.parse(object);
                    if (page == 1) {
                        favorites.clear();
                    }

                    JSONArray data = object.getJSONArray("data");
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject favorite = data.getJSONObject(i);
                        favorites.add(Favorite.parseFavorites(favorite));
                    }

                    if (page == pagination.getTotalPages()) {
                        favoritesView.showFavorites(favorites);
                    } else {
                        getFavorites(page + 1);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                favoritesView.showFavorites(favorites);
            }
        });
    }

    @Override
    public void searchFavorites(String text) {
        searchFavorites = new ArrayList<>();
        currentSearchText = text;
        int size = favorites.size();
        if (!text.isEmpty()) {
            for (int i = 0; i < size; i++) {
                if (favorites.get(i).getAttributes().getName().toLowerCase().contains(text.toLowerCase())) {
                    searchFavorites.add(favorites.get(i));
                }
            }
            favoritesView.showSearchFavorites(searchFavorites);
        }
    }


    @Override
    public void onBindFavoriteViewAtPosition(int position, FavoriteViewHolder favoriteViewHolder) {
        ArrayList<String> outOfStockLocationList = new ArrayList<String>();
        AppDatabase appDatabase = AppDatabase.getInstance();

        String id = "";
        if (fromSearch) {
            id = searchFavorites.get(position).getAttributes().getItemId();
        } else {
            id = favorites.get(position).getAttributes().getItemId();
        }

        String data = "";

        //TODO: Use file cache only for Lorenzo for now
        if (BuildUtil.soloAppKey().equalsIgnoreCase("7r5YQwmSgZCtM0ux")) {
            data = favoritesView.getFromCache(id);
        } else {
            String applicationId = CacheUtil.getInstance().getApplication().getId();
            MenuItemDb menuItemDb = appDatabase.menuItemDao().findByApplicationAndId(applicationId, id);
            if (menuItemDb != null) {
                data = menuItemDb.data;
            }
        }
        if (data != null && !data.isEmpty()) {
            try {
                itemById = GsonHelper.gsonFinal().fromJson(data, MenuItem.class);
                outOfStockLocationList.addAll(itemById.getOutOfStockLocations());
                listPrice = itemById.getListPrice();
                if (CacheUtil.getInstance().getOrderStore() != null) {
                    orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
                }


                if (fromSearch) {
                    favoriteViewHolder.setFavorite(searchFavorites.get(position), outOfStockLocationList, listPrice, itemById.getPrice(orderStoreId));
                } else {
                    favoriteViewHolder.setFavorite(favorites.get(position), outOfStockLocationList, listPrice, itemById.getPrice(orderStoreId));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            listPrice = 0.0;
            favoritesView.showError(CacheUtil.getInstance().getTranslations(Text.ITEM_NO_LONGER_AVAILABLE));
        }


    }
    @Override
    public void setToSearch(boolean isFromSearch) {
        fromSearch = isFromSearch;
    }

    @Override
    public int getFavoritesCount() {
        if (fromSearch) {
            return searchFavorites.size();
        } else {
            return favorites.size();
        }
    }

    @Override
    public void deleteFavorite(Favorite favorite) {
        CustomersApiCall.getInstance().deleteItemFromFavorites(AuthUtil.getInstance().getCustomer().getId(), favorite.getItemId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                Set<String> favoritesSet = CacheUtil.getInstance().getFavorites();
                favoritesSet.remove(favorite.getItemId());
                CacheUtil.getInstance().setFavorites(favoritesSet);

                favorites.remove(favorite);
                if (fromSearch) {
                    searchFavorites(currentSearchText);
                } else {
                    favoritesView.showFavorites(favorites);
                }
                favoritesView.showMessage(CacheUtil.getInstance().getTranslations(Text.ITEM_REMOVED_FROM_FAVORITES));

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);

                if (o instanceof JSONObject) {

                    try {
                        JSONObject object = (JSONObject) o;
                        Object error = object.get("error");
                        String detail = CacheUtil.getInstance().getTranslations(Text.ERROR_REMOVE_FROM_FAVORITES);
                        if (error instanceof JSONObject) {
                            detail = ((JSONObject) error).getString("detail");
                        } else if (error instanceof JSONArray) {
                            JSONObject errorObject = ((JSONArray) error).getJSONObject(0);
                            detail = errorObject.getString("detail");
                        }
                        favoritesView.showMessage(detail);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        favoritesView.showMessage(CacheUtil.getInstance().getTranslations(Text.ERROR_REMOVE_FROM_FAVORITES));
                    }

                } else {
                    favoritesView.showMessage(CacheUtil.getInstance().getTranslations(Text.ERROR_REMOVE_FROM_FAVORITES));
                }
            }
        });
    }

    @Override
    public void addToCartFavorite(Favorite favorite, ImageView view) {
        imageView = view;
        favoritesView.showLoadingDialog();

        addCartItem(favorite);

        /*MenuApiCall.getInstance().getItemDetails(CacheUtil.getInstance().getMenuId(), favorite.getAttributes().getItemId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    JSONArray dataArray = object.getJSONArray("data");
                    if (dataArray.length() > 0) {
                        MenuItem menuItem = GsonHelper.gsonFinal().fromJson(dataArray.getJSONObject(0).toString(), MenuItem.class);

                        MenuItem cartMenuItem = GsonHelper.gsonFinal().fromJson(dataArray.getJSONObject(0).toString(), MenuItem.class);

                        boolean disable = false;
                        if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
                            disable = menuItem.getAttributes().getDisableForDelivery() == 1;
                        } else if (CacheUtil.getInstance().getOrderType() == OrderType.PICKUP || CacheUtil.getInstance().getOrderType() == OrderType.DINE_IN) {
                            disable = menuItem.getAttributes().getDisableForPickup() == 1;
                        }

                        if (disable) {
                            favoritesView.showMessage(CacheUtil.getInstance().getTranslations(Text.ITEM_NO_LONGER_AVAILABLE));
                        } else {
                            getTimedEvent(favorite, menuItem);
                        }
                    } else {
                        favoritesView.showMessage(CacheUtil.getInstance().getTranslations(Text.ITEM_NO_LONGER_AVAILABLE));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    favoritesView.showMessage(CacheUtil.getInstance().getTranslations(Text.ITEM_NO_LONGER_AVAILABLE));
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                if (o instanceof JSONObject) {
                    try {
                        JSONObject object = (JSONObject) o;
                        Object error = object.get("error");
                        String detail = CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED);
                        if (error instanceof JSONObject) {
                            detail = ((JSONObject) error).getString("detail");
                        } else if (error instanceof JSONArray) {
                            JSONObject errorObject = ((JSONArray) error).getJSONObject(0);
                            detail = errorObject.getString("detail");
                        }
                        favoritesView.showMessage(detail);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        favoritesView.showMessage(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                    }
                } else {
                    favoritesView.showMessage(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                }
            }
        });*/
    }

    @Override
    public void getTimedEvent(Favorite favorite, MenuItem menuItem) {
        addCartItem(favorite);
//        MenuApiCall.getInstance().callTimedEvents(CacheUtil.getInstance().getMenuId(), new ApiRequestListener() {
//
//            @Override
//            public void onSuccess(Object o) {
//                try {
//                    JSONObject object = new JSONObject(o.toString());
//                    JSONArray dataArray = object.getJSONArray("data");
//                    if (dataArray.length() > 0) {
//                        getConcept(favorite, TimedEvent.parse(dataArray.getJSONObject(0)));
//                    } else {
//                        addCartItem(favorite);
//                    }
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                    this.onError(e);
//                }
//            }
//
//            @Override
//            public void onError(Object o) {
//                AppLogger.printRetrofitError(o);
//                addCartItem(favorite);
//            }
//        }, menuItem.getId());
    }

    @Override
    public void getConcept(Favorite favorite, TimedEvent timedEvent) {

        ApplicationsApiCall.getInstance().getApplication(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {

                    JSONObject jsonObject = new JSONObject(o.toString());

                    if (jsonObject.has("included") && !jsonObject.isNull("included")) {
                        JSONArray included = jsonObject.getJSONArray("included");
                        if (included.length() > 0) {
                            // TODO: Inject this
                            GsonBuilder builder = new GsonBuilder()
                                    .setPrettyPrinting()
                                    .registerTypeAdapter(boolean.class, new BooleanTypeAdapter());
                            Gson gson = builder.create();
//                            Concept concept = GsonHelper.gsonFinal().fromJson(data.toString(), new TypeToken<Concept>() {}.getType());
                            for (int i = 0; i < included.length(); i++) {
                                if (included.getJSONObject(i).optString("type").equalsIgnoreCase("concept")) {
                                    Concept concept = gson.fromJson(included.getJSONObject(i).getJSONObject("attributes").toString(), Concept.class);
                                    concept.setId(included.getJSONObject(i).getString("id"));

                                    if (MenuUtil.isItemAvailable(timedEvent, concept.getCurrentTime())) {
                                        addCartItem(favorite);
                                    } else {
                                        favoritesView.showError(MenuUtil.getTimedEventError(timedEvent));
                                    }
                                }
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
                favoritesView.showError(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)));
            }
        });

    }

    @Override
    public void addCartItem(Favorite favorite) {

        MenuItem favoriteMenuItem = MenuUtil.getFavoriteMenuItem(favorite);

        HashMap<String, ModifierItem> favoriteModifiers = new HashMap<>();
        for (ModifierItem modifierItem : favoriteMenuItem.getModifierItems()) {
            favoriteModifiers.put(modifierItem.getId(), modifierItem);
        }

        HashMap<String, Ingredient> favoriteIngredients = new HashMap<>();
        for (Ingredient ingredient : favoriteMenuItem.getIngredients()) {
            favoriteIngredients.put(ingredient.getId(), ingredient);
        }

        MenuApiCall.getInstance().getAllItemDetails(CacheUtil.getInstance().getMenuId(), String.valueOf(favoriteMenuItem.getId()), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    String categoryId;
                    JSONObject object = new JSONObject(o.toString());
                    JSONArray dataArray = object.getJSONArray("data");

                    if (dataArray.length() > 0) {

                        MenuItem menuItem = GsonHelper.gsonFinal().fromJson(dataArray.getJSONObject(0).toString(), MenuItem.class);
                        menuItem.toCdn(CacheUtil.getOrderStoreId());

                        try {
                            menuItem.isCombo = dataArray.getJSONObject(0).getJSONObject("attributes").getBoolean("is-combo");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        categoryId = getMenuCategory(menuItem.getId());

                        if (menuItem.getAttributes().getEnabled() == 0) {
                            favoritesView.showError(CacheUtil.getInstance().getTranslations(Text.ITEM_NO_LONGER_AVAILABLE));
                        } else if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY && menuItem.getAttributes().getDisableForDelivery() == 1) {
                            favoritesView.showError(CacheUtil.getInstance().getTranslations(Text.ITEM_NOT_AVAILABLE_FOR_DELIVERY, "(x) is not available for Delivery.").replace("(x)", menuItem.getAttributes().getName()));
                        } else if ((CacheUtil.getInstance().getOrderType() == OrderType.PICKUP || CacheUtil.getInstance().getOrderType() == OrderType.DINE_IN) && menuItem.getAttributes().getDisableForPickup() == 1) {
                            favoritesView.showError(CacheUtil.getInstance().getTranslations(Text.ITEM_NOT_AVAILABLE_FOR_PICKUP, "(x) is not available for Pickup.").replace("(x)", menuItem.getAttributes().getName()));
                        } else {

                            if (object.has("included") && !object.isNull("included")) {
                                JSONArray included = object.getJSONArray("included");

                                for (int i = 0; i < included.length(); i++) {
                                    Store store = Store.parseStore(included.getJSONObject(i));
                                    menuItem.getDisabledStores().put(store.getId(), store);
                                }

                            }

                            if (object.has("included") && !object.isNull("included")) {
                                JSONArray included = object.getJSONArray("included");

                                for (int i = 0; i < included.length(); i++) {

                                    JSONObject includedObject = included.getJSONObject(i);

                                    if (includedObject.has("id") && !includedObject.isNull("id") && includedObject.has("type") && !includedObject.isNull("type")) {
                                        String includedId = includedObject.getString("id");
                                        String includedType = includedObject.getString("type");

                                        if (includedType.equalsIgnoreCase("modifier-group")) {

                                            if (menuItem.getModifierGroupsIds().contains(includedId)) {

                                                ModifierGroup modifierGroup = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<ModifierGroup>() {
                                                }.getType());

                                                if (modifierGroup.getAttributes().getEnabled() == 1) {
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
                                                        menuItem.setSizes(modifierGroup);
                                                    } else {
                                                        modifierGroup.setType(ModifierGroupType.OPTIONS);
                                                        List<ModifierGroup> items = new ArrayList<>(menuItem.getModifierGroups());
                                                        items.add(modifierGroup);
                                                        menuItem.setModifierGroups(items);
                                                    }

                                                }

                                            }

                                        } else if (includedType.equalsIgnoreCase("ingredient")) {
                                            if (menuItem.getIngredientsIds().contains(includedId)) {
                                                Ingredient ingredient = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<Ingredient>() {
                                                }.getType());
                                                if (favoriteIngredients.containsKey(ingredient.getId())) {
                                                    ingredient.getAttributes().setSelected(false);
                                                }
                                                if (ingredient.getAttributes().getEnabled() == 1) {
                                                    List<Ingredient> items = new ArrayList<>(menuItem.getIngredients());
                                                    items.add(ingredient);
                                                    menuItem.setIngredients(items);
                                                }
                                            }
                                        } else if (includedType.equalsIgnoreCase("allergen")) {
                                            if (menuItem.getAllergenIds().contains(includedId)) {
                                                Allergen allergen = Allergen.parse(includedObject);
                                                if (allergen.getEnabled() == 1) {
                                                    List<Allergen> items = new ArrayList<>(menuItem.getAllergens());
                                                    AllergenAttributes attributes = new AllergenAttributes();
                                                    Translated name = new Translated();
                                                    name.setArabic(allergen.getName());
                                                    name.setEnglish(allergen.getName());
                                                    name.setFrench(allergen.getName());
                                                    attributes.setName(name);
                                                    Translated icon = new Translated();
                                                    icon.setArabic(allergen.getIcon());
                                                    icon.setEnglish(allergen.getIcon());
                                                    icon.setFrench(allergen.getIcon());
                                                    attributes.setIcon(icon);
                                                    attributes.setEnabled(allergen.getEnabled());
                                                    allergen.setAttributes(attributes);
                                                    items.add(allergen);
                                                    menuItem.setAllergens(items);
                                                }
                                            }
                                        } else if (includedType.equalsIgnoreCase("location")) {
                                            if (menuItem.getLocationIds().contains(includedId)) {
                                                Store store = Store.parseStore(included.getJSONObject(i));
                                                menuItem.getDisabledStores().put(store.getId(), store);
                                            }
                                        }

                                    }
                                }

                                for (int i = 0; i < included.length(); i++) {

                                    JSONObject includedObject = included.getJSONObject(i);

                                    if (includedObject.has("id") && !includedObject.isNull("id") && includedObject.has("type") && !includedObject.isNull("type")) {
                                        String includedId = includedObject.getString("id");
                                        String includedType = includedObject.getString("type");

                                        if (includedType.equalsIgnoreCase("modifier")) {

                                            ModifierItem modifierItem = GsonHelper.gsonFinal().fromJson(includedObject.toString(), new TypeToken<ModifierItem>() {
                                            }.getType());

                                            String name = modifierItem.getAttributes().getName();
                                            Translated translated = new Translated();
                                            translated.setEnglish(name);
                                            translated.setArabic(name);
                                            translated.setFrench(name);
                                            modifierItem.getAttributes().setTranslatedName(translated);

                                            if (favoriteModifiers.containsKey(modifierItem.getId())) {
                                                modifierItem.getAttributes().setSelected(true);
                                                modifierItem.getAttributes().setQuantity(favoriteModifiers.get(modifierItem.getId()).getAttributes().getQuantity());
                                            }

                                            if (modifierItem.getAttributes().getEnabled() == 1) {
                                                if (menuItem.getSizes().getModifierItemsIds().contains(includedId)) {
                                                    List<ModifierItem> items = new ArrayList<>(menuItem.getSizes().getModifierItems());
                                                    items.add(modifierItem);
                                                    menuItem.getSizes().setModifierItems(items);
                                                } else {
                                                    for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                                                        if (modifierGroup.getModifierItemsIds().contains(includedId)) {
                                                            List<ModifierItem> items = new ArrayList<>(modifierGroup.getModifierItems());
                                                            items.add(modifierItem);
                                                            modifierGroup.setModifierItems(items);
                                                        }
                                                    }
                                                }

                                            }

                                        }
                                    }
                                }

                                LinkedList<ModifierGroup> filteredModifierGroups = new LinkedList<>();

                                for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                                    if (modifierGroup.getModifierItems().size() > 0) {
                                        filteredModifierGroups.add(modifierGroup);
                                    }
                                }

                                Collections.sort(filteredModifierGroups, new Comparator<ModifierGroup>() {
                                    @Override
                                    public int compare(ModifierGroup modifierGroup1, ModifierGroup modifierGroup2) {
                                        return modifierGroup1.getAttributes().getDisplayOrder() - modifierGroup2.getAttributes().getDisplayOrder();
                                    }
                                });

                                menuItem.setModifierGroups(filteredModifierGroups);

                                if (menuItem.getSizes().getModifierItems().size() > 0) {
                                    menuItem.getSizes().getModifierItems().get(0).getAttributes().setSelected(true);
                                }

                                for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                                    if (modifierGroup.getAttributes().getMinimum() == 1 && modifierGroup.getModifierItems().size() > 0) {
                                        modifierGroup.getModifierItems().get(0).getAttributes().setSelected(true);
                                    }
                                }
                                VisibilityConfig visibilityConfig = menuItem.getSizes().getAttributes().getVisibilityConfig();
                                if (visibilityConfig != null) {
                                    List<VisibilityConfigRule> rules = visibilityConfig.getRules();
                                    if (rules != null && rules.size() > 0) {
                                        VisibilityConfigRule rule = rules.get(0);
                                        if (rule.getTargetType() != null) {
                                            if (rule.getTargetType().equalsIgnoreCase("modifier-group")) {
                                                for (ModifierGroup modGroup : menuItem.getModifierGroups()) {
                                                    if (!menuItem.getSizes().getAttributes().getId().equals(modGroup.getAttributes().getId()) && rule.getTargetId().equals(modGroup.getAttributes().getId())) {
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
                                                        break;
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
                                                    if (!group.getAttributes().getId().equals(menuItem.getSizes().getAttributes().getId()) && rule.getTargetId().equals(menuItem.getSizes().getAttributes().getId())) {
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
                                                        break;
                                                    }

                                                    for (ModifierGroup modGroup : menuItem.getModifierGroups()) {
                                                        if (!group.getAttributes().getId().equals(modGroup.getAttributes().getId()) && rule.getTargetId().equals(modGroup.getAttributes().getId())) {
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

                            }

                            Store store = CacheUtil.getInstance().getOrderStore();
                            if (store != null && menuItem.getDisabledStores().containsKey(store.getId())) {
                                favoritesView.showMessage(CacheUtil.getInstance().getTranslations(Text.LOCATION_AVAILABLE).replace("(x)", store.getAttributes().getName()));
                            } else if (menuItem.getAttributes().isMustBeCustomized()) {
                                favoritesView.showMenuItem(categoryId, menuItem.getId());
                            } else if (menuItem.isCombo) {
                                favoritesView.showMenuItem(categoryId, menuItem.getId());
                            } else {
                                boolean invalid = false;
                                ArrayList<ModifierGroup> visibleModifierGroups = new ArrayList<>();

                                for (ModifierGroup group : menuItem.getModifierGroups()) {
                                    VisibilityConfig visibilityConfig = group.getAttributes().getVisibilityConfig();
                                    if (visibilityConfig != null && visibilityConfig.getInitialValue() != null) {
                                        if (!visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
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
                                    if (selected < modifierGroup.getAttributes().getMinimum()) {
                                        invalid = true;
                                        break;
                                    }
                                }

                                if (invalid) {
                                    favoritesView.showMenuItem(categoryId, menuItem.getId());
                                } else {

                                    HashMap<String, String> eventAttributes = new HashMap<>();
                                    eventAttributes.put("Item Name", menuItem.getAttributes().getName());
                                    eventAttributes.put("Item Quantity", String.valueOf(menuItem.getAttributes().getQuantity()));

                                    double totalPrice = menuItem.getAttributes().getPrice(CacheUtil.getOrderStoreId());

                                    for (ModifierItem modifierItem : menuItem.getSizes().getModifierItems()) {
                                        if (modifierItem.getAttributes().isSelected()) {
                                            eventAttributes.put("Modifier Name", modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                                            eventAttributes.put("Modifier Quantity", String.valueOf(modifierItem.getAttributes().getQuantity()));
                                            totalPrice += modifierItem.getAttributes().getPrice();
                                        }
                                    }

                                    for (ModifierItem modifierItem : menuItem.getDrinks().getModifierItems()) {
                                        if (modifierItem.getAttributes().isSelected()) {
                                            eventAttributes.put("Modifier Name", modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                                            eventAttributes.put("Modifier Quantity", String.valueOf(modifierItem.getAttributes().getQuantity()));
                                            totalPrice += modifierItem.getAttributes().getPrice();
                                        }
                                    }

                                    for (ModifierItem modifierItem : menuItem.getFries().getModifierItems()) {
                                        if (modifierItem.getAttributes().isSelected()) {
                                            eventAttributes.put("Modifier Name", modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                                            eventAttributes.put("Modifier Quantity", String.valueOf(modifierItem.getAttributes().getQuantity()));
                                            totalPrice += modifierItem.getAttributes().getPrice();
                                        }
                                    }

                                    for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                                        VisibilityConfig visibilityConfig = modifierGroup.getAttributes().getVisibilityConfig();
                                        if (visibilityConfig == null || visibilityConfig.getInitialValue() == null || !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
                                            for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                                                if (modifierItem.getAttributes().isSelected()) {
                                                    eventAttributes.put("Modifier Name", modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                                                    eventAttributes.put("Modifier Quantity", String.valueOf(modifierItem.getAttributes().getQuantity()));
                                                    totalPrice += modifierItem.getAttributes().getPrice() * modifierItem.getAttributes().getQuantity();
                                                }
                                            }
                                        }
                                    }

                                    for (ModifierItem modifierItem : menuItem.getModifierItems()) {
                                        eventAttributes.put("Modifier Name", modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                                        eventAttributes.put("Modifier Quantity", String.valueOf(modifierItem.getAttributes().getQuantity()));
                                        totalPrice += modifierItem.getAttributes().getPrice();
                                    }

                                    for (Ingredient ingredient : menuItem.getIngredients()) {
                                        if (!ingredient.getAttributes().isSelected()) {
                                            eventAttributes.put("Ingredient Name", ingredient.getAttributes().getName());
                                            eventAttributes.put("Ingredient Quantity", String.valueOf(ingredient.getAttributes().getQuantity()));
                                            totalPrice += ingredient.getAttributes().getPrice();
                                        }
                                    }

                                    totalPrice += menuItem.getAttributes().getQuantity();

                                    favoritesView.logEvent("AddToCartFromFavorites", eventAttributes, "Price", totalPrice);
                                    favoritesView.logFirebaseEvent("add_to_cart_event");

                                    menuItem.getAttributes().setReorder(true);

                                    int cartCount = CacheUtil.getInstance().getCartQuantity();
                                    removeMukafaaRedemption();
                                    CacheUtil.getInstance().addToCart(menuItem);
                                    if (cartCount == 0) {
                                        favoritesView.setCartExpiry();
                                    }
                                    favoritesView.addToCartFavorite(menuItem, imageView);
                                }

                            }
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
                favoritesView.showError(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(Text.ITEM_NO_LONGER_AVAILABLE)));
            }
        });

    }

    @Override
    public void getModifiers(Favorite favorite, String menuCategoryId, MenuItem menuItem) {
        MenuApiCall.getInstance().getModifierGroups(CacheUtil.getInstance().getMenuId(), menuCategoryId, menuItem.getId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {

                    JSONObject object = new JSONObject(o.toString());

                    ArrayList<ModifierGroup> modifierGroups = GsonHelper.gsonFinal().fromJson(object.getJSONArray("data").toString(), new TypeToken<ArrayList<ModifierGroup>>() {
                    }.getType());

                    for (ModifierGroup modifierGroup : modifierGroups) {

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

                        if (menuItem.getSizes().getType() == ModifierGroupType.OPTIONS && (code.equals("sizes") || type.equals("sizes") || label.equals("sizes"))) {
                            modifierGroup.setType(ModifierGroupType.SIZES);
                            menuItem.setSizes(modifierGroup);
                        } else {
                            modifierGroup.setType(ModifierGroupType.OPTIONS);
                            List<ModifierGroup> items = new ArrayList<>(menuItem.getModifierGroups());
                            items.add(modifierGroup);
                            menuItem.setModifierGroups(items);
                        }
                    }

                    if (object.has("included")) {

                        ArrayList<ModifierItem> modifierItems = GsonHelper.gsonFinal().fromJson(object.getJSONArray("included").toString(), new TypeToken<ArrayList<ModifierItem>>() {
                        }.getType());

                        for (ModifierItem modifierItem : modifierItems) {
                            if (menuItem.getSizes().getModifierItemsIds().contains(modifierItem.getId())) {
                                List<ModifierItem> items = new ArrayList<>(menuItem.getSizes().getModifierItems());
                                items.add(modifierItem);
                                menuItem.getSizes().setModifierItems(items);
                            } else {
                                for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                                    if (modifierGroup.getModifierItemsIds().contains(modifierItem.getId())) {
                                        List<ModifierItem> items = new ArrayList<>(modifierGroup.getModifierItems());
                                        items.add(modifierItem);
                                        modifierGroup.setModifierItems(items);
                                        break;
                                    }
                                }
                            }
                        }

                    }

                    LinkedList<ModifierGroup> filteredModifierGroups = new LinkedList<>();

                    for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                        if (modifierGroup.getModifierItems().size() > 0) {
                            filteredModifierGroups.add(modifierGroup);
                        }
                    }

                    Collections.sort(filteredModifierGroups, new Comparator<ModifierGroup>() {
                        @Override
                        public int compare(ModifierGroup modifierGroup1, ModifierGroup modifierGroup2) {
                            return modifierGroup1.getAttributes().getDisplayOrder() - modifierGroup2.getAttributes().getDisplayOrder();
                        }
                    });

                    menuItem.setModifierGroups(filteredModifierGroups);

                    if (menuItem.getSizes().getModifierItems().size() > 0) {
                        menuItem.getSizes().getModifierItems().get(0).getAttributes().setSelected(true);
                    }

                    for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                        if (modifierGroup.getAttributes().getMinimum() == 1 && modifierGroup.getModifierItems().size() > 0) {
                            modifierGroup.getModifierItems().get(0).getAttributes().setSelected(true);
                        }
                    }

                    /*MenuItem favoriteMenuItem = MenuUtil.getFavoriteMenuItem(favorite);

                    Set<String> selectedModifierItemIds = favoriteMenuItem.getModifierItemsIds();
                    ArrayList<ModifierItem> selectedModifierItems = favoriteMenuItem.getModifierItems();

                    for(ModifierItem modifierItem : menuItem.getSizes().getModifierItems()) {
                        if(selectedModifierItemIds.contains(modifierItem.getId())) {
                            modifierItem.getAttributes().setSelected(true);
                            for(ModifierItem selectedModifierItem : selectedModifierItems) {
                                if(selectedModifierItem.getId().equalsIgnoreCase(modifierItem.getId())) {
                                    modifierItem.getAttributes().setQuantity(selectedModifierItem.getAttributes().getQuantity());
                                    break;
                                }
                            }
                        }
                    }

                    for(ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                        for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                            if (selectedModifierItemIds.contains(modifierItem.getId())) {
                                modifierItem.getAttributes().setSelected(true);
                                for (ModifierItem selectedModifierItem : selectedModifierItems) {
                                    if (selectedModifierItem.getId().equalsIgnoreCase(modifierItem.getId())) {
                                        modifierItem.getAttributes().setQuantity(selectedModifierItem.getAttributes().getQuantity());
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    for(ModifierGroup group : menuItem.getModifierGroups()) {
                        VisibilityConfig visibilityConfig = group.getAttributes().getVisibilityConfig();
                        if(visibilityConfig != null) {
                            List<VisibilityConfigRule> rules = visibilityConfig.getRules();
                            if(rules != null && rules.size() > 0) {
                                VisibilityConfigRule rule = rules.get(0);
                                if(rule.getTargetType() != null && rule.getTargetType().equalsIgnoreCase("modifier-group")) {
                                    for (ModifierGroup modGroup : menuItem.getModifierGroups()) {
                                        if (!group.getId().equals(modGroup.getId()) && rule.getTargetId().equals(modGroup.getId())) {
                                            if(rule.getTargetIs() != null && rule.getTargetIs().equalsIgnoreCase("selected")) {
                                                int selected = 0;
                                                for(ModifierItem modItem : modGroup.getModifierItems()) {
                                                    if(modItem.getAttributes().isSelected()) {
                                                        selected += 1;
                                                    }
                                                }
                                                if(selected > 0) {
                                                    group.getAttributes().getVisibilityConfig().setInitialValue("");
                                                } else {
                                                    group.getAttributes().getVisibilityConfig().setInitialValue("hidden");
                                                }
                                            }
                                            break;
                                        }

                                    }
                                }

                            }
                        }
                    }*/

                    getIngredients(favorite, menuCategoryId, menuItem);

                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                }

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                favoritesView.showError(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)));
            }
        });
    }

    @Override
    public void getIngredients(Favorite favorite, String menuCategoryId, MenuItem menuItem) {
        MenuApiCall.getInstance().getCategoryItemIngredients(CacheUtil.getInstance().getMenuId(), menuCategoryId, menuItem.getId(), "1", new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                try {

                    JSONObject object = new JSONObject(o.toString());

                    menuItem.setIngredients(GsonHelper.gsonFinal().fromJson(object.getJSONArray("data").toString(), new TypeToken<ArrayList<Ingredient>>() {
                    }.getType()));

                    /*MenuItem favoriteMenuItem = MenuUtil.getFavoriteMenuItem(favorite);

                    Set<String> selectedIngredientIds = favoriteMenuItem.getIngredientsIds();
                    ArrayList<Ingredient> selectedIngredients = favoriteMenuItem.getIngredients();

                    for(Ingredient ingredient : menuItem.getIngredients()) {
                        if(selectedIngredientIds.contains(ingredient.getId())) {
                            ingredient.getAttributes().setSelected(true);
                            for(Ingredient selectedIngredient : selectedIngredients) {
                                if(selectedIngredient.getId().equalsIgnoreCase(ingredient.getId())) {
                                    ingredient.getAttributes().setQuantity(selectedIngredient.getAttributes().getQuantity());
                                    break;
                                }
                            }
                        }
                    }*/

                    Store store = CacheUtil.getInstance().getOrderStore();
                    if (store != null && menuItem.getDisabledStores().containsKey(store.getId())) {
                        favoritesView.showMessage(CacheUtil.getInstance().getTranslations(Text.LOCATION_AVAILABLE).replace("(x)", store.getAttributes().getName()));
                    } else {

                        boolean invalid = false;

                        for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                            int selected = 0;
                            for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                                if (modifierItem.getAttributes().isSelected()) {
                                    selected += modifierItem.getAttributes().getQuantity();
                                }
                            }
                            if (selected < modifierGroup.getAttributes().getMinimum()) {
                                invalid = true;
                                break;
                            }
                        }

                        if (invalid) {
                            favoritesView.showMenuItem(menuCategoryId, menuItem.getId());
                        } else {

                            HashMap<String, String> eventAttributes = new HashMap<>();
                            eventAttributes.put("Item Name", menuItem.getAttributes().getName());
                            eventAttributes.put("Item Quantity", String.valueOf(menuItem.getAttributes().getQuantity()));

                            double totalPrice = menuItem.getAttributes().getPrice(CacheUtil.getOrderStoreId());

                            for (ModifierItem modifierItem : menuItem.getSizes().getModifierItems()) {
                                if (modifierItem.getAttributes().isSelected()) {
                                    eventAttributes.put("Modifier Name", modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                                    eventAttributes.put("Modifier Quantity", String.valueOf(modifierItem.getAttributes().getQuantity()));
                                    totalPrice += modifierItem.getAttributes().getPrice();
                                }
                            }

                            for (ModifierItem modifierItem : menuItem.getDrinks().getModifierItems()) {
                                if (modifierItem.getAttributes().isSelected()) {
                                    eventAttributes.put("Modifier Name", modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                                    eventAttributes.put("Modifier Quantity", String.valueOf(modifierItem.getAttributes().getQuantity()));
                                    totalPrice += modifierItem.getAttributes().getPrice();
                                }
                            }

                            for (ModifierItem modifierItem : menuItem.getFries().getModifierItems()) {
                                if (modifierItem.getAttributes().isSelected()) {
                                    eventAttributes.put("Modifier Name", modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                                    eventAttributes.put("Modifier Quantity", String.valueOf(modifierItem.getAttributes().getQuantity()));
                                    totalPrice += modifierItem.getAttributes().getPrice();
                                }
                            }

                            for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                                VisibilityConfig visibilityConfig = modifierGroup.getAttributes().getVisibilityConfig();
                                if (visibilityConfig == null || visibilityConfig.getInitialValue() == null || !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
                                    for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                                        if (modifierItem.getAttributes().isSelected()) {
                                            eventAttributes.put("Modifier Name", modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                                            eventAttributes.put("Modifier Quantity", String.valueOf(modifierItem.getAttributes().getQuantity()));
                                            totalPrice += modifierItem.getAttributes().getPrice() * modifierItem.getAttributes().getQuantity();
                                        }
                                    }
                                }
                            }

                            for (ModifierItem modifierItem : menuItem.getModifierItems()) {
                                eventAttributes.put("Modifier Name", modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                                eventAttributes.put("Modifier Quantity", String.valueOf(modifierItem.getAttributes().getQuantity()));
                                totalPrice += modifierItem.getAttributes().getPrice();
                            }

                            for (Ingredient ingredient : menuItem.getIngredients()) {
                                if (!ingredient.getAttributes().isSelected()) {
                                    eventAttributes.put("Ingredient Name", ingredient.getAttributes().getName());
                                    eventAttributes.put("Ingredient Quantity", String.valueOf(ingredient.getAttributes().getQuantity()));
                                    totalPrice += ingredient.getAttributes().getPrice();
                                }
                            }

                            totalPrice += menuItem.getAttributes().getQuantity();

                            favoritesView.logEvent("AddToCartFromFavorites", eventAttributes, "Price", totalPrice);
                            favoritesView.logFirebaseEvent("add_to_cart_event");

                            int cartCount = CacheUtil.getInstance().getCartQuantity();
                            removeMukafaaRedemption();
                            CacheUtil.getInstance().addToCart(menuItem);
                            if (cartCount == 0) {
                                favoritesView.setCartExpiry();
                            }

                            favoritesView.addToCartFavorite(menuItem, imageView);
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
                favoritesView.showError(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)));
            }
        });
    }

    @Override
    public String getMenuCategory(String menuItemId) {
        String applicationId = CacheUtil.getInstance().getApplication().getId();
        AppDatabase appDatabase = AppDatabase.getInstance();
        MenuItemEntity menuItemEntity = appDatabase.menuItemEntityDao().findById(applicationId, menuItemId);
        if(menuItemEntity != null) {
            return menuItemEntity.getMenuCategoryId();
        }
        return "";
    }

    @Override
    public ArrayList<Favorite> getFavorites() {
        return favorites;
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
}
