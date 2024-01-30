package com.herfy.menu;

import android.util.Pair;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.herfy.BuildConfig;
import com.herfy.base.BaseView;
import com.herfy.data.AppDatabase;
import com.herfy.data.constants.Text;
import com.herfy.data.constants.Values;
import com.herfy.country.SupportedCountry;
import com.herfy.data.entities.AllergenDb;
import com.herfy.data.entities.IngredientDb;
import com.herfy.data.entities.MenuCategoryDb;
import com.herfy.data.entities.MenuCategoryEntity;
import com.herfy.data.entities.MenuItemDb;
import com.herfy.data.entities.MenuItemEntity;
import com.herfy.data.entities.ModifierGroupDb;
import com.herfy.data.entities.ModifierItemDb;
import com.herfy.data.responses.MenuCategoryItemResponse;
import com.herfy.menu.viewholders.IngredientViewHolder;
import com.herfy.menu.viewholders.MenuCategoryViewHolder;
import com.herfy.menu.viewholders.MenuItemViewHolder;
import com.herfy.menu.viewholders.ModifierGroupViewHolder;
import com.herfy.menu.viewholders.ModifierItemViewHolder;
import com.herfy.menu.viewholders.ModifierItemViewHolderNew;
import com.herfy.menu.viewholders.SizeViewHolder;
import com.herfy.newmenu.viewholder.MenuCouponViewHolder;
import com.herfy.newmenu.viewholder.MenuFavoriteViewHolder;
import com.herfy.newmenu.viewholder.MenuPopularViewHolder;
import com.herfy.newmenu.viewholder.MenuRecentViewHolder;
import com.herfy.promotions.data.CampaignToAdd;
import com.herfy.promotions.data.CouponToAdd;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.ApiRequestWithHeadersListener;
import com.skylinedynamics.solosdk.api.calls.ApplicationsApiCall;
import com.skylinedynamics.solosdk.api.calls.CouponsApiCall;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.calls.LocationApiCall;
import com.skylinedynamics.solosdk.api.calls.MenuApiCall;
import com.skylinedynamics.solosdk.api.calls.ReportsApiCall;
import com.skylinedynamics.solosdk.api.calls.UtilitiesApiCall;
import com.skylinedynamics.solosdk.api.models.Pagination;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.skylinedynamics.solosdk.api.models.objects.Allergen;
import com.skylinedynamics.solosdk.api.models.objects.AllergenCdn;
import com.skylinedynamics.solosdk.api.models.objects.ApplicationAttributes;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.Concept;
import com.skylinedynamics.solosdk.api.models.objects.Customer;
import com.skylinedynamics.solosdk.api.models.objects.Facility;
import com.skylinedynamics.solosdk.api.models.objects.Favorite;
import com.skylinedynamics.solosdk.api.models.objects.Ingredient;
import com.skylinedynamics.solosdk.api.models.objects.IngredientCdn;
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory;
import com.skylinedynamics.solosdk.api.models.objects.MenuCategoryItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemCombo;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupCdn;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupType;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItemCdn;
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Promotion;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.skylinedynamics.solosdk.api.models.objects.TimedEvent;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfig;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfigRule;
import com.skylinedynamics.solosdk.api.models.requestbodies.AddFavoriteItemBody;
import com.skylinedynamics.solosdk.util.BooleanTypeAdapter;
import com.skylinedynamics.solosdk.util.GsonHelper;
import com.herfy.util.AppLogger;
import com.herfy.util.AuthUtil;
import com.herfy.util.BuildUtil;
import com.herfy.util.CacheUtil;
import com.herfy.util.ErrorHandler;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.MenuUtil;
import com.herfy.analytics.MixpanelUtil;
import com.herfy.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import io.reactivex.Observable;
import okhttp3.Headers;

public class MenuPresenter implements MenuContract.Presenter {

    private MenuContract.View menuView;

    private List<MenuCategory> menuCategories = new ArrayList<>();
    private ArrayList<MenuCategory> loadedMenuCategories = new ArrayList<>();
    private ArrayList<MenuCategory> preloadedMenuCategories = new ArrayList<>();
    private ArrayList<MenuItem> searchedMenuItems = new ArrayList<>();
    private int menuCategoryItems = 0;

    private boolean isRefreshing = true;
    private ArrayList<MenuCategory> refreshMenuCategories = new ArrayList<>();
    private int refreshMenuCategoryItems = 0;

    private MenuCategory menuCategory = new MenuCategory();
    private MenuItem menuItem = new MenuItem();
    private MenuItem itemById = new MenuItem();
    private ImageView currentImageViewToAnimate = null;
    private ArrayList<MenuItem> bestSellers = new ArrayList<>();
    private ArrayList<MenuItem> recentOrders = new ArrayList<>();
    private List<Campaign> digitalCoupons = new ArrayList<>();

    private ArrayList<Favorite> excludedFavItem = new ArrayList<>();
    private ArrayList<MenuItem> excludedPopularItem = new ArrayList<>();
    private ArrayList<MenuItem> excludedRecentOrder = new ArrayList<>();
    private ArrayList<Favorite> fave = new ArrayList<>();
    private String orderStoreId = null;
    private String itemNote = "";

    public String selectedSizeId = "";
    //private LinkedHashMap<String, ModifierGroup> menuItemModifierGroups = new LinkedHashMap<>();

    private ArrayList<Favorite> favorites = new ArrayList<>();

    private ArrayList<OrderHistory> orders = new ArrayList<>();

    private boolean newestToOldest = true;

    private int menuItemComboIndex = 0;

    private boolean isFromCartEdit = false;
    private HashMap<String, Integer> selectedComponents = new HashMap();

    private int downloadMenuRetries = 0;

    private boolean isExcluded = false;

    private boolean isStoreOrderTypePreselected = false;
    public List<MenuCategory> excludedMenu = new ArrayList<>();
    private HashMap<String, HashSet<MenuCategory>> excludedCategories = new HashMap<>();

    public MenuPresenter(BaseView baseView) {

        menuView = (MenuContract.View) baseView;
        menuView.setPresenter(this);

    }

    @Override
    public void start() {

        menuView.setupViews();
        menuView.setupFonts();
        menuView.setupTranslations();

    }

    @Override
    public void getFavorites(boolean fromAddRemove, int page) {
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
                    Set<String> favoriteIds = CacheUtil.getInstance().getFavorites();
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject favoriteObject = data.getJSONObject(i);
                        Favorite favorite = Favorite.parseFavorites(favoriteObject);
                        fave.add(favorite);
                        favoriteIds.add(favorite.getAttributes().getItemId());
                    }

                    CacheUtil.getInstance().setFavorites(favoriteIds);

                    if (page == pagination.getTotalPages()) {
                        favorites.clear();
                        favorites.addAll(fave);
                        fave.clear();
                        excludedFavorites();
                        if (CacheUtil.getOrderStoreId() == null) {
                            menuView.showFavorites(fromAddRemove, favorites);
                        } else {
                            menuView.showFavorites(fromAddRemove, excludedFavorites());
                        }
                    } else {
                        getFavorites(fromAddRemove, page + 1);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                if (CacheUtil.getOrderStoreId() == null) {
                    menuView.showFavorites(fromAddRemove, favorites);
                } else {
                    menuView.showFavorites(fromAddRemove, excludedFavorites());
                }

            }
        });
    }

    @Override
    public void resetMenuItem() {
        menuView.setMenuItem(menuCategory, menuItem);
    }

    @Override
    public int modifierItemQuantityCount(@NonNull List<ModifierItem> modifiers) {
        int counter = 0;
        for (ModifierItem modifierItem : modifiers) {
            if (modifierItem.getAttributes().isSelected())
                counter += modifierItem.getAttributes().getQuantity();
        }
        return counter;
    }

    @Override
    public void updateComponentsFromYourWay(ModifierItem modifierItem, int position, int selectedModifierIndex, int getIndex, boolean isAdd) {
        List<ModifierItem> modifierItem1s = menuItem.combo.get(menuItemComboIndex).components.get(position).items.get(selectedModifierIndex).modifierGroups.get(getIndex).getModifierItems();
        for (int i = 0; i < modifierItem1s.size(); i++) {
            if (modifierItem1s.get(i).getId().equals(modifierItem.getId())) {
                if (isAdd) {
                    menuItem.combo.get(menuItemComboIndex).components.get(position).items.get(selectedModifierIndex).modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().setQuantity(1);
                    menuItem.combo.get(menuItemComboIndex).components.get(position).items.get(selectedModifierIndex).modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().setSelected(true);
                } else {
                    menuItem.combo.get(menuItemComboIndex).components.get(position).items.get(selectedModifierIndex).modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().setSelected(false);
                    menuItem.combo.get(menuItemComboIndex).components.get(position).items.get(selectedModifierIndex).modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().setQuantity(1);
                }
                break;
            }
        }
        menuView.setMenuItem(menuCategory, menuItem);
    }

    @Override
    public void updateComponentsFromYourWayQuantityCheck(ModifierGroup modifierGroup, ModifierItem modifierItem, int position, int selectedModifierIndex, int getIndex, boolean isAdd) {
        try {
            List<ModifierItem> modifierItem1s = menuItem.combo.get(menuItemComboIndex).components.get(position).items.get(selectedModifierIndex).modifierGroups.get(getIndex).getModifierItems();
            if (isAdd && modifierGroup.getAttributes().getMaximum() == 1) {
                for (int i = 0; i < modifierItem1s.size(); i++) {
                    if (modifierItem1s.get(i).getId().equals(modifierItem.getId())) {
                        menuItem.combo.get(menuItemComboIndex).components.get(position).items.get(selectedModifierIndex).modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().setQuantity(1);
                        menuItem.combo.get(menuItemComboIndex).components.get(position).items.get(selectedModifierIndex).modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().setSelected(true);
                    } else {
                        menuItem.combo.get(menuItemComboIndex).components.get(position).items.get(selectedModifierIndex).modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().setQuantity(0);
                        menuItem.combo.get(menuItemComboIndex).components.get(position).items.get(selectedModifierIndex).modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().setSelected(false);
                    }
                }
            } else {
                for (int i = 0; i < modifierItem1s.size(); i++) {
                    if (modifierItem1s.get(i).getId().equals(modifierItem.getId())) {
                        if (isAdd) {
                            menuItem.combo.get(menuItemComboIndex).components.get(position).items.get(selectedModifierIndex).modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().setQuantity(1);
                            menuItem.combo.get(menuItemComboIndex).components.get(position).items.get(selectedModifierIndex).modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().setSelected(true);
                        } else {
                            menuItem.combo.get(menuItemComboIndex).components.get(position).items.get(selectedModifierIndex).modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().setSelected(false);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //getMenuItemTotalPrice(menuItem, -1);
        menuView.updateComponentModifierGroups(position, menuItem.combo.get(menuItemComboIndex).components.get(position).items, selectedModifierIndex);
    }

    @Override
    public void updateComponentsFromYourWayQuantity(ModifierItem modifierItem, int position, int selectedModifierIndex, int getIndex, boolean isAdd, int quantity) {
        List<ModifierItem> modifierItem1s = menuItem.combo.get(menuItemComboIndex).components.get(position).items.get(selectedModifierIndex).modifierGroups.get(getIndex).getModifierItems();
        for (int i = 0; i < modifierItem1s.size(); i++) {
            if (modifierItem1s.get(i).getId().equals(modifierItem.getId())) {
                menuItem.combo.get(menuItemComboIndex).components.get(position).items.get(selectedModifierIndex).modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().setQuantity(quantity);
                menuItem.combo.get(menuItemComboIndex).components.get(position).items.get(selectedModifierIndex).modifierGroups.get(getIndex).getModifierItems().get(i).getAttributes().setSelected(quantity != 0);
                break;
            }
        }
        //getMenuItemTotalPrice(menuItem, -1);
        menuView.updateComponentModifierGroups(position, menuItem.combo.get(menuItemComboIndex).components.get(position).items, selectedModifierIndex);
    }

    @Nullable
    String getMenuItemData(AppDatabase appDatabase, String id) {
        String data = "";
        try {
            //TODO: Use file cache only for Lorenzo for now
            if (BuildUtil.soloAppKey().equalsIgnoreCase("7r5YQwmSgZCtM0ux")) {
                data = menuView.getFromCache(id);
            } else {
                String applicationId = CacheUtil.getInstance().getApplication().getId();
                MenuItemDb menuItemDb = appDatabase.menuItemDao().findByApplicationAndId(applicationId, id);
                if (menuItemDb != null) {
                    data = menuItemDb.data;
                } else {
                    data = menuView.getFromCache(id);
                }
            }
        } catch (Exception e) {
            data = menuView.getFromCache(id);
        }
        return data;
    }

    @Override
    public MenuItem getMenuItemSingle(String id) {
        AppDatabase appDatabase = AppDatabase.getInstance();
        String menuItemData = getMenuItemData(appDatabase, id);
        if (menuItemData != null && !menuItemData.isEmpty()) {
            try {
                String applicationId = CacheUtil.getInstance().getApplication().getId();
                MenuItem menuItemNew = MenuUtil.processMenuItemCustomObject(GsonHelper.gsonFinal().fromJson(menuItemData, MenuItem.class));

                LinkedList<Allergen> allergens = new LinkedList<>();
                for (AllergenDb allergenDb : appDatabase.allergenDao().findAllByApplicationAndIds(applicationId, menuItemNew.getAllergensIds().toArray(new String[menuItemNew.getAllergensIds().size()]))) {
                    AllergenCdn allergenCdn = GsonHelper.gsonFinal().fromJson(allergenDb.data, AllergenCdn.class);
                    allergens.add(AllergenCdn.convert(allergenCdn, LocaleUtil.getInstance().getLanguage()));
                }

                // TODO: prefer FP rather than side effects, also make Allergen Comparable
                Collections.sort(allergens, new Comparator<Allergen>() {
                    @Override
                    public int compare(Allergen allergen1, Allergen allergen2) {
                        return allergen1.getAttributes().getDisplayOrder() - allergen2.getAttributes().getDisplayOrder();
                    }
                });
                menuItemNew.setAllergens(allergens);

                LinkedList<Ingredient> ingredients = new LinkedList<>();
                ArrayList<String> ingredientIds = new ArrayList<>();
                for (Ingredient ingredient : menuItemNew.getIngredientIds()) {
                    ingredientIds.add(ingredient.getId());
                }
                for (IngredientDb ingredientDb : appDatabase.ingredientDao().findAllByApplicationAndIds(applicationId, ingredientIds.toArray(new String[ingredientIds.size()]))) {
                    IngredientCdn ingredientCdn = GsonHelper.gsonFinal().fromJson(ingredientDb.data, IngredientCdn.class);
                    ingredients.add(IngredientCdn.convert(ingredientCdn, LocaleUtil.getInstance().getLanguage()));
                }

                // TODO: prefer FP rather than side effects, also make Ingredient Comparable
                Collections.sort(ingredients, new Comparator<Ingredient>() {
                    @Override
                    public int compare(Ingredient ingredient1, Ingredient ingredient2) {
                        return ingredient1.getAttributes().getDisplayOrder() - ingredient2.getAttributes().getDisplayOrder();
                    }
                });
                menuItemNew.setIngredients(ingredients);

                ModifierGroup sizes = new ModifierGroup();
                LinkedList<ModifierGroup> modifierGroups = new LinkedList<>();
                HashMap<String, ModifierGroup> modifierGroupIds = new HashMap<>();
                for (ModifierGroup modifierGroup : menuItemNew.getModifierGroupIds()) {
                    modifierGroupIds.put(modifierGroup.getId(), modifierGroup);
                }
                for (ModifierGroupDb modifierGroupDb : appDatabase.modifierGroupDao().findAllByApplicationAndIds(applicationId, modifierGroupIds.keySet().toArray(new String[modifierGroupIds.size()]))) {
                    ModifierGroupCdn modifierGroupCdn = MenuUtil.processModifierGroupCustomObject(GsonHelper.gsonFinal().fromJson(modifierGroupDb.data, ModifierGroupCdn.class));
                    LinkedList<ModifierItem> modifierItems = new LinkedList<>();
                    for (ModifierItemDb modifierItemDb : appDatabase.modifierItemDao().findAllByApplicationAndIds(applicationId, modifierGroupCdn.getAttributes().getModifiers().toArray(new String[modifierGroupCdn.getAttributes().getModifiers().size()]))) {
                        ModifierItemCdn modifierItemCdn = GsonHelper.gsonFinal().fromJson(modifierItemDb.data, ModifierItemCdn.class);
                        ModifierItem modifierItem = ModifierItemCdn.convert(modifierItemCdn, LocaleUtil.getInstance().getLanguage());
                        modifierItem.getAttributes().setSelected(modifierItem.getAttributes().getMinimum() == 1);
                        modifierItems.add(modifierItem);
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
                    int size = modifierItems.size();
                    ArrayList<ModifierItem> modifierItemsFinal = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        boolean found = false;
                        for (int a = 0; a < modifierGroup.getExcludeModifiers().size(); a++) {
                            if (modifierItems.get(i).getId().equalsIgnoreCase(String.valueOf(modifierGroup.getExcludeModifiers().get(a)))) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            modifierItemsFinal.add(modifierItems.get(i));
                        }
                    }
                    modifierGroup.setModifierItems(modifierItems);
                    if (modifierGroupDb.type == ModifierGroupType.SIZES.getValue()) {
                        sizes = modifierGroup;
                    } else {
                        modifierGroups.add(modifierGroup);
                    }
                }
                menuItemNew.setSizes(sizes);
                Collections.sort(modifierGroups, new Comparator<ModifierGroup>() {
                    @Override
                    public int compare(ModifierGroup modifierGroup1, ModifierGroup modifierGroup2) {
                        return modifierGroup1.getAttributes().getDisplayOrder() - modifierGroup2.getAttributes().getDisplayOrder();
                    }
                });
                menuItemNew.setModifierGroups(modifierGroups);

                LinkedList<ModifierGroup> filteredModifierGroups = new LinkedList<>();

                for (ModifierGroup modifierGroup : menuItemNew.getModifierGroups()) {
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

                if (menuItemNew.getSizes().getModifierItems().size() > 0) {
                    menuItemNew.getSizes().getModifierItems().get(0).getAttributes().setSelected(true);
                }

                menuItemNew.setModifierGroups(filteredModifierGroups);

                for (ModifierGroup modifierGroup : menuItemNew.getModifierGroups()) {
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

                VisibilityConfig visibilityConfig = menuItemNew.getSizes().getAttributes().getVisibilityConfig();
                if (visibilityConfig != null) {
                    List<VisibilityConfigRule> rules = visibilityConfig.getRules();
                    if (rules != null && rules.size() > 0) {
                        VisibilityConfigRule rule = rules.get(0);
                        if (rule.getTargetType() != null && rule.getTargetType().equalsIgnoreCase("modifier-group")) {
                            for (ModifierGroup modGroup : menuItemNew.getModifierGroups()) {
                                if (!menuItemNew.getSizes().getId().equals(modGroup.getId()) && rule.getTargetId().equals(modGroup.getId())) {
                                    if (rule.getTargetIs() != null && rule.getTargetIs().equalsIgnoreCase("selected")) {
                                        int selected = 0;
                                        for (ModifierItem modItem : modGroup.getModifierItems()) {
                                            if (modItem.getAttributes().isSelected()) {
                                                selected += 1;
                                            }
                                        }
                                        if (selected > 0) {
                                            menuItemNew.getSizes().getAttributes().getVisibilityConfig().setInitialValue("");
                                        } else {
                                            menuItemNew.getSizes().getAttributes().getVisibilityConfig().setInitialValue("hidden");
                                        }
                                    }
                                }

                            }
                        }

                    }
                }

                for (ModifierGroup group : menuItemNew.getModifierGroups()) {
                    visibilityConfig = group.getAttributes().getVisibilityConfig();
                    if (visibilityConfig != null) {
                        List<VisibilityConfigRule> rules = visibilityConfig.getRules();
                        if (rules != null && rules.size() > 0) {
                            VisibilityConfigRule rule = rules.get(0);
                            if (rule.getTargetType() != null) {
                                if (rule.getTargetType().equalsIgnoreCase("modifier-group")) {
                                    if (!group.getId().equals(menuItemNew.getSizes().getId()) && rule.getTargetId().equals(menuItemNew.getSizes().getId())) {
                                        if (rule.getTargetIs() != null && rule.getTargetIs().equalsIgnoreCase("selected")) {
                                            int selected = 0;
                                            for (ModifierItem modItem : menuItemNew.getSizes().getModifierItems()) {
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

                                    for (ModifierGroup modGroup : menuItemNew.getModifierGroups()) {
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
                                    for (ModifierItem modItem : menuItemNew.getSizes().getModifierItems()) {
                                        if (modItem.getAttributes().isSelected()) {
                                            selectedModifiers.add(modItem.getId());
                                            break;
                                        }
                                    }

                                    for (ModifierGroup modGroup : menuItemNew.getModifierGroups()) {
                                        if (!group.getId().equals(modGroup.getId())) {
                                            for (ModifierItem modItem : modGroup.getModifierItems()) {
                                                if (modItem.getAttributes().isSelected()) {
                                                    selectedModifiers.add(modItem.getId());
                                                }
                                            }
                                        }
                                    }

                                    //for (ModifierGroup modGroup : menuItemNew.getModifierGroups()) {
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
                return menuItemNew;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;

    }

    @Override
    public boolean isAboveMax(ModifierGroup group, int count) {
        return ((group.getAttributes().getMaximum() - 1) < count);
    }

    @Override
    public void refresh() {
        menuView.setMenuItem(menuCategory, menuItem);
    }

    @Override
    public void assignComboIndex(int comboIndex) {
        menuItemComboIndex = comboIndex;
    }

    @Override
    public void assignMenuItem(MenuItem item) {
        menuItem = item;
    }

    @Override
    public ArrayList<MenuItemComponents> filterComponents(ArrayList<MenuItemComponents> components) {
        ArrayList<MenuItemComponents> components1 = new ArrayList<>();
        for (int i = 0; i < components.size(); i++) {
            if (!components.get(i).items.isEmpty()) {
                components1.add(components.get(i));
            }
        }
        return components1;
    }

    @Override
    public void updateComponents(ComponentModifierItem componentModifierItem, int componentsPosition, int itemPosition) {
        menuItem.combo.get(menuItemComboIndex).components.get(componentsPosition).items.set(itemPosition, componentModifierItem);
        String componentId = menuItem.combo.get(menuItemComboIndex).components.get(componentsPosition).id;
        int previousSelected = 0;
        if (selectedComponents.containsKey(componentId)) {
            previousSelected = selectedComponents.get(componentId);
            if (previousSelected < menuItem.combo.get(menuItemComboIndex).components.get(componentsPosition).items.size()) {
                menuItem.combo.get(menuItemComboIndex).components.get(componentsPosition).items.get(previousSelected).selected = false;
            }
        }
        selectedComponents.put(componentId, itemPosition);
        /*int size = menuItem.combo.get(menuItemComboIndex).components.get(componentsPosition).items.size();
        for (int i = 0; i < size; i++) {
            if (i != itemPosition) {
                menuItem.combo.get(menuItemComboIndex).components.get(componentsPosition).items.get(i).selected = false;
            }
        }
        menuView.setMenuItem(menuCategory, menuItem);*/
        menuView.updateComponents(componentsPosition, menuItem.combo.get(menuItemComboIndex).components.get(componentsPosition).items, previousSelected, itemPosition);
    }

    @Override
    public void onBindFavoriteViewAtPosition(int position, MenuFavoriteViewHolder favoriteViewHolder) {
        ArrayList<String> outOfStockLocations = new ArrayList<String>();
        AppDatabase appDatabase = AppDatabase.getInstance();

        String menuItemData = "";
        if (CacheUtil.getOrderStoreId() != null && !excludedFavItem.isEmpty() && excludedFavItem.size() >= position) {
            if (excludedFavItem.size() == 1 && position == 1) {
                menuItemData = getMenuItemData(appDatabase, excludedFavItem.get(0).getAttributes().getItemId());
            } else {
                menuItemData = getMenuItemData(appDatabase, excludedFavItem.get(position).getAttributes().getItemId());
            }
        } else {
            menuItemData = getMenuItemData(appDatabase, favorites.get(position).getAttributes().getItemId());
        }
        if (menuItemData != null && !menuItemData.isEmpty()) {
            try {
                itemById = new Gson().fromJson(menuItemData, MenuItem.class);
                outOfStockLocations.addAll(itemById.getOutOfStockLocations());

                if (CacheUtil.getOrderStoreId() == null) {
                    favoriteViewHolder.setFavorite(favorites.get(position), outOfStockLocations, itemById.getListPrice(), itemById.getPrice(null));
                } else {
                    orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
                    favoriteViewHolder.setFavorite(excludedFavItem.get(position), outOfStockLocations, itemById.getListPrice(), itemById.getPrice(orderStoreId));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<Favorite> excludedFavorites() {
        if (CacheUtil.getOrderStoreId() != null) {
            excludedFavItem.clear();
            String orderId = "";
            if (CacheUtil.getOrderStoreId() != null) {
                orderId = CacheUtil.getOrderStoreId();
            }
            AppDatabase appDatabase = AppDatabase.getInstance();
            for (Favorite fav : favorites) {
                String menuItemData = getMenuItemData(appDatabase, fav.getAttributes().getItemId());
                if (menuItemData != null && !menuItemData.isEmpty()) {
                    try {
                        itemById = GsonHelper.gsonFinal().fromJson(menuItemData, MenuItem.class);
                        if (!itemById.getExcludedLocations().contains(orderId)) {
                            excludedFavItem.add(fav);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return excludedFavItem;
    }

    public ArrayList<MenuItem> excludedPopularItem() {
        if (CacheUtil.getOrderStoreId() != null) {
            excludedPopularItem.clear();
            String orderId = "";
            if (CacheUtil.getOrderStoreId() != null) {
                orderId = CacheUtil.getOrderStoreId();
            }
            AppDatabase appDatabase = AppDatabase.getInstance();
            for (MenuItem item : bestSellers) {
                String menuItemData = getMenuItemData(appDatabase, item.getId());
                if (menuItemData != null && !menuItemData.isEmpty()) {
                    try {
                        itemById = new Gson().fromJson(menuItemData, MenuItem.class);
                        if (!itemById.getExcludedLocations().contains(orderId)) {
                            excludedPopularItem.add(item);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return excludedPopularItem;
    }

    public ArrayList<MenuItem> excludedRecentOrder() {
        if (CacheUtil.getOrderStoreId() != null) {
            excludedRecentOrder.clear();
            String orderId = "";
            if (CacheUtil.getOrderStoreId() != null) {
                orderId = CacheUtil.getOrderStoreId();
            }
            AppDatabase appDatabase = AppDatabase.getInstance();
            for (MenuItem item : recentOrders) {
                String menuItemData = getMenuItemData(appDatabase, item.getId());
                if (menuItemData != null && !menuItemData.isEmpty()) {
                    try {
                        itemById = GsonHelper.gsonFinal().fromJson(menuItemData, MenuItem.class);
                        if (!itemById.getExcludedLocations().contains(orderId)) {
                            excludedRecentOrder.add(item);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return excludedRecentOrder;
    }

    @Override
    public void onBindPopularViewAtPosition(int position, MenuPopularViewHolder popularViewHolder) {
        ArrayList<String> outOfStockLocationList = new ArrayList<String>();
        AppDatabase appDatabase = AppDatabase.getInstance();
        String menuItemData = "";
        if (CacheUtil.getOrderStoreId() != null && !excludedPopularItem.isEmpty() && excludedPopularItem.size() >= position) {
            if (excludedPopularItem.size() == 1 && position == 1) {
                menuItemData = getMenuItemData(appDatabase, excludedPopularItem.get(0).getId());
            } else {
                menuItemData = getMenuItemData(appDatabase, excludedPopularItem.get(position).getId());
            }
        } else {
            menuItemData = getMenuItemData(appDatabase, bestSellers.get(position).getId());
        }

        if (menuItemData != null && !menuItemData.isEmpty()) {
            try {
                itemById = GsonHelper.gsonFinal().fromJson(menuItemData, MenuItem.class);
                outOfStockLocationList.addAll(itemById.getOutOfStockLocations());

                if (CacheUtil.getOrderStoreId() == null) {
                    popularViewHolder.setBestSeller(bestSellers.get(position), outOfStockLocationList, itemById.getListPrice(), itemById.getPrice(null));
                } else {
                    orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
                    popularViewHolder.setBestSeller(excludedPopularItem.get(position), outOfStockLocationList, itemById.getListPrice(), itemById.getPrice(orderStoreId));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onBindDigitalCouponsViewAtPosition(int position, MenuCouponViewHolder couponViewHolder) {
        couponViewHolder.setCoupon(digitalCoupons.get(position));
    }

    @Override
    public void onBindRecentOrderViewAtPosition(int position, MenuRecentViewHolder orderViewHolder) {
        ArrayList<String> outOfStockLocationList = new ArrayList<String>();
        AppDatabase appDatabase = AppDatabase.getInstance();
        String menuItemData = "";
        if (CacheUtil.getOrderStoreId() == null || excludedRecentOrder.size() == 0) {
            menuItemData = getMenuItemData(appDatabase, recentOrders.get(position).getId());
        } else {
            if (excludedRecentOrder.size() == 1 && position == 1) {
                menuItemData = getMenuItemData(appDatabase, excludedRecentOrder.get(0).getId());
            } else {
                menuItemData = getMenuItemData(appDatabase, excludedRecentOrder.get(position).getId());
            }
        }
        if (menuItemData != null && !menuItemData.isEmpty()) {
            try {
                itemById = GsonHelper.gsonFinal().fromJson(menuItemData, MenuItem.class);
                outOfStockLocationList.addAll(itemById.getOutOfStockLocations());

                if (CacheUtil.getOrderStoreId() == null) {
                    orderViewHolder.setRecentOrder(recentOrders.get(position), outOfStockLocationList, itemById.getListPrice(), itemById.getPrice(null));
                } else {
                    orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
                    orderViewHolder.setRecentOrder(excludedRecentOrder.get(position), outOfStockLocationList, itemById.getListPrice(), itemById.getPrice(orderStoreId));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public int getFavoritesCount() {
        if (CacheUtil.getOrderStoreId() != null) {
            return excludedFavorites().size();
        } else return favorites.size();
    }

    @Override
    public ArrayList<Favorite> getFavorites() {
        if (CacheUtil.getOrderStoreId() != null) {
            return excludedFavorites();
        } else return favorites;
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
                            menuView.showStore(newStore);
                            getStoreOpen();
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
                    menuView.showStore(store);
                }
            });


        }
    }

    @Override
    public void getStoreOpen() {
        if (CacheUtil.getInstance().getOrderStore() != null) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
            LocationApiCall.getInstance().isOpen(CacheUtil.getInstance().getOrderStore().getId(), simpleDateFormat.format(new Date()), new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {

                    try {
                        JSONObject object = (JSONObject) o;
                        JSONObject data = object.getJSONObject("data");

                        OrderType orderType = CacheUtil.getInstance().getOrderType();
                        Store openStore = Store.parseStore(data);
                        if ((orderType == OrderType.DELIVERY && openStore.getAttributes().getOpenDeliver()) || openStore.getAttributes().getOpenPickup()) {

                            menuView.showStoreOpen(true, StringUtil.getClosingHours(openStore));

                        } else {
                            menuView.showStoreOpen(false, CacheUtil.getInstance().getTranslations(Text.CLOSED, "Closed"));
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.onError(e);
                    }
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    menuView.showStoreOpen(false, "");
                }
            });

        } else {
            menuView.showStoreOpen(false, "");
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

                            menuView.showAddress(address.getAttributes().getLine1());

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
                    menuView.showAddress(address.getAttributes().getLine1());
                }
            });
        } else if (CacheUtil.getInstance().getOrderDeliveryAddress() != null) {
            menuView.showAddress(CacheUtil.getInstance().getOrderDeliveryAddress());
        }
    }

    private void prepareMenuCDN(AppDatabase appDatabase, String menuCategoryId, String menuItemId) {
        String applicationId = CacheUtil.getInstance().getApplication().getId();
        List<MenuCategoryItem> menuCategoryItems = new ArrayList<>();
        for (MenuCategoryEntity menuCategoryEntity : appDatabase.menuCategoryEntityDao().getAllByApplication(applicationId)) {
            MenuCategoryItem menuCategoryItem = new MenuCategoryItem();
            menuCategoryItem.getAttributes().setMenuCategory(MenuCategoryEntity.Companion.toDomain(menuCategoryEntity));
            menuCategoryItem.getAttributes().setMenuItems(MenuItemEntity.Companion.toDomain(appDatabase.menuItemEntityDao().findAllByMenuCategoryId(applicationId, menuCategoryEntity.getId())));
            menuCategoryItems.add(menuCategoryItem);
        }
        prepareMenu(menuCategoryId, menuItemId, menuCategoryItems);
    }

    @Override
    public void getMenuCdn(boolean fromForceLocation, String menuCategoryId, String menuItemId) {

        AppDatabase appDatabase = AppDatabase.getInstance();

        if (downloadMenuRetries <= 3) {

            Executor executor = Executors.newSingleThreadExecutor();

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    String applicationId = CacheUtil.getInstance().getApplication().getId();
                    boolean isMenuSeparate = fromForceLocation && CacheUtil.getInstance().isSeparatedMenus();
                    boolean isEkfc = BuildUtil.isEkfc();
                    if (!isMenuSeparate && !isEkfc && appDatabase.menuCategoryEntityDao().hasByApplication(applicationId) && (new Date().getTime() - CacheUtil.getInstance().getMenuCached()) < (1000 * 60 * Values.CACHE_EXPIRY_MINUTES)) {
                        getMenuFromDatabase(menuCategoryId, menuItemId);
                    } else {
                        downloadMenuRetries++;
                        String url = MenuUtil.getDiffMenuCdnUrl();
                        UtilitiesApiCall.getInstance().getMenuCategoriesWithHeaders(url, new ApiRequestWithHeadersListener() {
                            @Override
                            public void onSuccess(Headers headers, Object o) {
                                downloadMenuRetries = 0;
                                try {
                                    String previous = CacheUtil.getInstance().getMenuCdnLastModified();
                                    String lastModified = "";
                                    if(headers.names().contains("last-modified")) {
                                        lastModified = headers.get("last-modified");
                                    }
                                    if(!appDatabase.menuCategoryEntityDao().hasByApplication(applicationId) || !isEkfc || (isEkfc && !previous.equalsIgnoreCase(lastModified))) {
                                        CacheUtil.getInstance().setMenuCdnLastModified(lastModified);
                                        appDatabase.menuCategoryDao().deleteByApplication(applicationId);
                                        appDatabase.menuItemDao().deleteByApplication(applicationId);
                                        appDatabase.modifierGroupDao().deleteByApplication(applicationId);
                                        appDatabase.modifierItemDao().deleteByApplication(applicationId);
                                        appDatabase.ingredientDao().deleteByApplication(applicationId);
                                        appDatabase.allergenDao().deleteByApplication(applicationId);
                                        appDatabase.menuCategoryEntityDao().deleteByApplication(applicationId);
                                        appDatabase.menuItemEntityDao().deleteByApplication(applicationId);
                                        JSONObject object = new JSONObject(o.toString());
                                        if (BuildConfig.MULTI_COUNTRY_ENABLED) {
                                            SupportedCountry country = CacheUtil.getInstance().getCountry();
                                            if (country != null) {
                                                CacheUtil.getInstance().setCountryMenu(country.applicationId, o.toString());
                                            }
                                        }
                                        parseMenu(menuCategoryId, menuItemId, object);
                                    } else {
                                        getMenuFromDatabase(menuCategoryId, menuItemId);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    if (!appDatabase.menuCategoryEntityDao().hasByApplication(applicationId)) {
                                        getMenuCdn(fromForceLocation, menuCategoryId, menuItemId);
                                    } else {
                                        prepareMenuCDN(appDatabase, menuCategoryId, menuItemId);
                                    }
                                }
                            }

                            @Override
                            public void onError(Object o) {
                                AppLogger.printRetrofitError(o.toString());
                                if (o instanceof JSONObject) {
                                    try {
                                        JSONObject object = (JSONObject) o;
                                        Object error = object.get("error");
                                        if (!appDatabase.menuCategoryEntityDao().hasByApplication(applicationId)) {
                                            menuView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
                                        } else {
                                            prepareMenuCDN(appDatabase, menuCategoryId, menuItemId);
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        if (!appDatabase.menuCategoryEntityDao().hasByApplication(applicationId)) {
                                            //menuView.showError(CacheUtil.getInstance().getTranslations(Text.NO_MENU_FOUND, "No menu found"));
                                            getMenuCdn(fromForceLocation, menuCategoryId, menuItemId);
                                        } else {
                                            prepareMenuCDN(appDatabase, menuCategoryId, menuItemId);
                                        }
                                    }
                                } else {
                                    if (!appDatabase.menuCategoryEntityDao().hasByApplication(applicationId)) {
                                        //menuView.showError(CacheUtil.getInstance().getTranslations(Text.NO_MENU_FOUND, "No menu found"));
                                        getMenuCdn(fromForceLocation, menuCategoryId, menuItemId);
                                    } else {
                                        prepareMenuCDN(appDatabase, menuCategoryId, menuItemId);
                                    }
                                }
                            }
                        });
                    }
                }
            });

        } else {
            if (!appDatabase.menuCategoryEntityDao().hasByApplication(CacheUtil.getInstance().getApplication().getId())) {
                menuView.showError(CacheUtil.getInstance().getTranslations(Text.NO_MENU_FOUND, "No menu found"));
            } else {
                prepareMenuCDN(appDatabase, menuCategoryId, menuItemId);
            }
        }

    }

    private void getMenuFromDatabase(String menuCategoryId, String menuItemId) {
        AppDatabase appDatabase = AppDatabase.getInstance();
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                String applicationId = CacheUtil.getInstance().getApplication().getId();
                List<MenuCategoryItem> menuCategoryItems = new ArrayList<>();
                for (MenuCategoryEntity menuCategoryDb : appDatabase.menuCategoryEntityDao().getAllByApplication(applicationId)) {
                    MenuCategory menuCategory = MenuCategoryEntity.toDomain(menuCategoryDb);
                    List<MenuItem> menuItems = new ArrayList<>();
                    for (MenuItemEntity menuItemDb : appDatabase.menuItemEntityDao().findAllByMenuCategoryId(applicationId, menuCategory.getId())) {
                        MenuItem menuItem = MenuItemEntity.toDomain(menuItemDb);
                        menuItems.add(menuItem);
                    }
                    MenuCategoryItem menuCategoryItem = new MenuCategoryItem();
                    menuCategoryItem.getAttributes().setMenuCategory(menuCategory);
                    menuCategoryItem.getAttributes().setMenuItems(menuItems);
                    menuCategoryItems.add(menuCategoryItem);
                }
                prepareMenu(menuCategoryId, menuItemId, menuCategoryItems);
            }
        });
    }

    @Override
    public void updateCart(boolean forceUpdate) {
        if(CacheUtil.getInstance().getCartQuantity() != 0 && (forceUpdate || BuildUtil.isEkfc())) {
            Executor executor = Executors.newSingleThreadExecutor();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    ArrayList<MenuItem> items = new ArrayList<>();
                    for(MenuItem cartItem : CacheUtil.getInstance().getCart()) {
                        AppDatabase appDatabase = AppDatabase.getInstance();
                        String data = getMenuItemData(appDatabase, cartItem.getId());
                        if (data != null && !data.isEmpty()) {
                            MenuItem item = MenuUtil.processMenuItemCustomObject(GsonHelper.gsonFinal().fromJson(data, MenuItem.class));
                            cartItem.setPrices(item.getPrices());
                            if(CacheUtil.getInstance().getOrderStore() != null) {
                                cartItem.setPrice(item.getPrice(CacheUtil.getOrderStoreId()));
                            } else {
                                cartItem.setPrice(item.getPrice());
                            }
                            if(!item.getModifierGroupIds().isEmpty()) {
                                String applicationId = CacheUtil.getInstance().getApplication().getId();
                                HashMap<String, ModifierGroup> modifierGroupIds = new HashMap<>();
                                for (ModifierGroup modifierGroup : item.getModifierGroupIds()) {
                                    modifierGroupIds.put(modifierGroup.getId(), modifierGroup);
                                }
                                HashMap<String, Double> modifierItemPrices = new HashMap<>();
                                for (ModifierGroupDb modifierGroupDb : appDatabase.modifierGroupDao().findAllByApplicationAndIds(applicationId, modifierGroupIds.keySet().toArray(new String[modifierGroupIds.size()]))) {
                                    ModifierGroupCdn modifierGroupCdn = MenuUtil.processModifierGroupCustomObject(GsonHelper.gsonFinal().fromJson(modifierGroupDb.data, ModifierGroupCdn.class));
                                    for (ModifierItemDb modifierItemDb : appDatabase.modifierItemDao().findAllByApplicationAndIds(applicationId, modifierGroupCdn.getAttributes().getModifiers().toArray(new String[modifierGroupCdn.getAttributes().getModifiers().size()]))) {
                                        ModifierItemCdn modifierItemCdn = GsonHelper.gsonFinal().fromJson(modifierItemDb.data, ModifierItemCdn.class);
                                        ModifierItem modItem = ModifierItemCdn.convert(modifierItemCdn, LocaleUtil.getInstance().getLanguage());
                                        modifierItemPrices.put(modItem.getId(), modItem.getPrice());
                                    }
                                }
                                for(ModifierItem modItem : cartItem.getSizes().getModifierItems()) {
                                    if(modifierItemPrices.containsKey(modItem.getId())) {
                                        modItem.setPrice(modifierItemPrices.get(modItem.getId()));
                                    }
                                }
                                for(ModifierGroup modGroup : cartItem.getModifierGroups()) {
                                    for(ModifierItem modItem : modGroup.getModifierItems()) {
                                        if(modifierItemPrices.containsKey(modItem.getId())) {
                                            modItem.setPrice(modifierItemPrices.get(modItem.getId()));
                                        }
                                    }
                                }
                            }
                        }
                        items.add(cartItem);
                    }
                    CacheUtil.getInstance().setCart(items);
                    menuView.updateFloatingTotalPrice();
                }
            });
        }
    }

    private LinkedList<MenuItem> getCategoryMenuItems(boolean storeSelected, MenuCategory category, AppDatabase appDatabase) {
        LinkedList<MenuItem> menuItems = new LinkedList<>();
        if (!storeSelected || !category.getExcludedLocations().contains(CacheUtil.getInstance().getOrderStore().getId())) {
            //TODO: Use file cache only for Lorenzo for now
            if (BuildUtil.soloAppKey().equalsIgnoreCase("7r5YQwmSgZCtM0ux")) {
                menuItems = getCategoryMenuItemIds(category, appDatabase);
            } else {
                try {
                    /*List<MenuItemDb> menuItemDbs = appDatabase.menuItemDao().findAllByMenuCategoryId(category.getId());
                    for (MenuItemDb menuItemDb : menuItemDbs) {
                        MenuItem menuItem = GsonHelper.gsonFinal().fromJson(menuItemDb.data, MenuItem.class);
                        menuItems.add(menuItem);
                    }*/
                    MenuItemEntity.Companion.toDomain(appDatabase.menuItemEntityDao().findAllByMenuCategoryId(CacheUtil.getInstance().getApplication().getId(), menuCategory.getId()));
                } catch (Exception e) {
                    menuItems = getCategoryMenuItemIds(category, appDatabase);
                }
            }
        }
        return menuItems;
    }

    private LinkedList<MenuItem> getCategoryMenuItemIds(MenuCategory category, AppDatabase appDatabase) {
        LinkedList<MenuItem> menuItems = new LinkedList<>();
        List<String> ids = appDatabase.menuItemDao().findAllIdsByMenuCategoryId(category.getId());
        for (String id : ids) {
            String data = menuView.getFromCache(id);
            if (data != null && !data.isEmpty()) {
                MenuItem menuItem = GsonHelper.gsonFinal().fromJson(data, MenuItem.class);
                menuItems.add(menuItem);
            }
        }
        return menuItems;
    }

    @Override
    public void parseMenu(String menuCategoryId, String menuItemId, JSONObject data) {
        boolean storeSelected = CacheUtil.getInstance().getOrderStore() != null;

        Executor executor = Executors.newSingleThreadExecutor();

        menuCategoryItems = 0;
        menuCategories.clear();
        excludedMenu.clear();
        excludedCategories.clear();

        AppDatabase appDatabase = AppDatabase.getInstance();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {

                    String applicationId = CacheUtil.getInstance().getApplication().getId();

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
                                menuView.saveToCache(menuItemId, menuItemDbData);
                            }
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
                            MenuCategory menuCategory = MenuCategoryEntity.toDomain(menuCategoryEntity);
                            if (!menuCategory.getExcludedLocations().isEmpty()) {
                                for (String excluded : menuCategory.getExcludedLocations()) {
                                    if (excludedCategories.containsKey(excluded)) {
                                        excludedCategories.get(excluded).add(menuCategory);
                                    } else {
                                        HashSet<MenuCategory> set = new HashSet<>();
                                        set.add(menuCategory);
                                        excludedCategories.put(excluded, set);
                                    }
                                }
                            }
                            LinkedList<MenuItem> menuItems = new LinkedList<>();

                            for (MenuItemEntity menuItem : menuCategoryItem.getAttributes().getMenuItems()) {
                                menuItem.setApplicationId(applicationId);
                                menuItem.setMenuCategoryId(menuCategoryItem.getAttributes().getMenuCategory().getId());
                                if (!MenuUtil.isExcluded(menuItem.getExcludedApps())) {
                                    appDatabase.menuItemEntityDao().insertAll(menuItem);
                                    menuItems.add(MenuItemEntity.toDomain(menuItem));
                                }
                            }

                            if (menuCategory.getEnabled() == 1 && menuItems.size() > 0) {
                                Collections.sort(menuItems, new Comparator<MenuItem>() {
                                    @Override
                                    public int compare(MenuItem menuItem1, MenuItem menuItem2) {
                                        return menuItem1.getDisplayOrder() - menuItem2.getAttributes().getDisplayOrder();
                                    }
                                });
                                menuCategory.setMenuItems(menuItems);
                                menuCategories.add(menuCategory);
                                if (!storeSelected || !menuCategory.getExcludedLocations().contains(CacheUtil.getInstance().getOrderStore().getId())) {
                                    excludedMenu.add(menuCategory);
                                }
                            }
                            Collections.sort(excludedMenu, new Comparator<MenuCategory>() {
                                @Override
                                public int compare(MenuCategory menuCategory1, MenuCategory menuCategory2) {
                                    return menuCategory1.getDisplayOrder() - menuCategory2.getDisplayOrder();
                                }
                            });
                        }
                    }

                    CacheUtil.getInstance().setMenuCached(new Date().getTime());
                    if (BuildConfig.MULTI_COUNTRY_ENABLED) {
                        SupportedCountry country = CacheUtil.getInstance().getCountry();
                        if (country != null) {
                            CacheUtil.getInstance().setCountryCachedTime(country.applicationId, new Date().getTime());
                        }
                    }

                    HashSet<MenuCategory> hashSet = new HashSet<MenuCategory>(menuCategories);
                    menuCategories.clear();
                    menuCategories.addAll(hashSet);

                    updateCart(false);
                    menuView.showMenuCategories(false, false, excludedMenu);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    public void prepareMenu(String menuCategoryId, String menuItemId, List<MenuCategoryItem> menuCategoryItems) {
        boolean storeSelected = CacheUtil.getInstance().getOrderStore() != null;

        this.menuCategoryItems = 0;
        menuCategories.clear();
        excludedMenu.clear();
        excludedCategories.clear();

        for (MenuCategoryItem menuCategoryItem : menuCategoryItems) {
            MenuCategory menuCategory = menuCategoryItem.getAttributes().getMenuCategory();
            if (!menuCategory.getExcludedLocations().isEmpty()) {
                for (String excluded : menuCategory.getExcludedLocations()) {
                    if (excludedCategories.containsKey(excluded)) {
                        excludedCategories.get(excluded).add(menuCategory);
                    } else {
                        HashSet<MenuCategory> set = new HashSet<>();
                        set.add(menuCategory);
                        excludedCategories.put(excluded, set);
                    }
                }
            }
            LinkedList<MenuItem> menuItems = new LinkedList<>(menuCategoryItem.getAttributes().getMenuItems());
            menuCategory.setMenuItems(menuItems);
            if (menuCategory.getEnabled() == 1 && menuCategoryItem.getAttributes().getMenuItems().size() > 0) {
                LinkedList<MenuItem> filteredMenuItems = new LinkedList<>();
                for (MenuItem menuItem : menuItems) {
                    if (menuItem.getEnabled() == 1 && (!storeSelected || !menuItem.getExcludedLocations().contains(CacheUtil.getInstance().getOrderStore().getId()))) {
                        filteredMenuItems.add(menuItem);
                    }
                }
                if (filteredMenuItems.size() > 0) {
                    Collections.sort(filteredMenuItems, new Comparator<MenuItem>() {
                        @Override
                        public int compare(MenuItem menuItem1, MenuItem menuItem2) {
                            return menuItem1.getDisplayOrder() - menuItem2.getDisplayOrder();
                        }
                    });
                    menuCategory.setMenuItems(filteredMenuItems);
                    menuCategories.add(menuCategory);
                    if (!storeSelected || !menuCategory.getExcludedLocations().contains(CacheUtil.getInstance().getOrderStore().getId())) {
                        excludedMenu.add(menuCategory);
                    }
                }

            }

        }

        if (menuCategoryId == null) {

            Collections.sort(excludedMenu, new Comparator<MenuCategory>() {
                @Override
                public int compare(MenuCategory menuCategory1, MenuCategory menuCategory2) {
                    return menuCategory1.getDisplayOrder() - menuCategory2.getDisplayOrder();
                }
            });
            HashSet<MenuCategory> hashSet = new HashSet<MenuCategory>(menuCategories);
            menuCategories.clear();
            menuCategories.addAll(hashSet);
            menuView.showMenuCategories(false, false, excludedMenu);
            isRefreshing = false;

        } else {
            boolean found = false;
            for (MenuCategory menuCategory : excludedMenuCategories(menuCategories)) {
                if (menuCategoryId.equals(menuCategory.getId())) {
                    found = true;
                    setMenuCategory(menuCategory);
                    getMenuItem(false, menuItemId);
                    break;
                }
            }
            if (!found) {
                menuView.showError(CacheUtil.getInstance().getTranslations(Text.ITEM_NO_LONGER_AVAILABLE));
            }
        }

        downloadMenuRetries = 0;

    }

    @Override
    public List<MenuCategory> getMenuCategories() {
        return excludedMenuCategories(menuCategories);
    }

    @Override
    public void getMenuCategories(boolean fromForceLocation, boolean fromHomeScreen, boolean fromDeeplink, boolean forceUseCache, boolean isFromInitialLoad, String menuCategoryId, String menuItemId) {
        menuCategoryItems = 0;
        menuCategories.clear();
        excludedMenu.clear();
        excludedCategories.clear();

        AppDatabase appDatabase = AppDatabase.getInstance();

        String applicationId = CacheUtil.getInstance().getApplication().getId();

        String homeScreenDesign =
                CacheUtil.getInstance().getApplication().getAttributes().getHomeScreenDesign();
        boolean isDefaultHomeScreenDesign = homeScreenDesign.equalsIgnoreCase("default");
        boolean isMenuSeparate = (fromForceLocation ||
                (!isDefaultHomeScreenDesign && fromHomeScreen)) &&
                CacheUtil.getInstance().isSeparatedMenus();

        if (!isMenuSeparate && !BuildUtil.isEkfc() && !fromDeeplink && appDatabase.menuCategoryEntityDao().hasByApplication(applicationId) && (forceUseCache || (new Date().getTime() - CacheUtil.getInstance().getMenuCached()) < (1000 * 60 * Values.CACHE_EXPIRY_MINUTES))) {
            Executor executor = Executors.newSingleThreadExecutor();

            executor.execute(new Runnable() {
                @Override
                public void run() {

                    boolean storeSelected = CacheUtil.getInstance().getOrderStore() != null;

                    List<MenuCategoryEntity> menuCategoryDbs = appDatabase.menuCategoryEntityDao().getAllByApplication(applicationId);

                    for (MenuCategoryEntity menuCategoryDb : menuCategoryDbs) {
                        MenuCategory category = MenuCategoryEntity.Companion.toDomain(menuCategoryDb);
                        if (!category.getExcludedLocations().isEmpty()) {
                            for (String excluded : category.getExcludedLocations()) {
                                if (excludedCategories.containsKey(excluded)) {
                                    excludedCategories.get(excluded).add(category);
                                } else {
                                    HashSet<MenuCategory> set = new HashSet<>();
                                    set.add(category);
                                    excludedCategories.put(excluded, set);
                                }
                            }
                        }
                        if (category.getEnabled() == 1) {
                            List<MenuItemEntity> menuItemDbs = appDatabase.menuItemEntityDao().findAllByMenuCategoryId(applicationId, category.getId());
                            if (menuItemDbs.size() > 0) {
                                category.setMenuItems(MenuItemEntity.Companion.toDomain(menuItemDbs));
                                menuCategories.add(category);
                                if (!storeSelected || !category.getExcludedLocations().contains(CacheUtil.getInstance().getOrderStore().getId())) {
                                    excludedMenu.add(category);
                                }
                            }
                        }
                    }

                    Collections.sort(excludedMenu, new Comparator<MenuCategory>() {
                        @Override
                        public int compare(MenuCategory menuCategory1, MenuCategory menuCategory2) {
                            return menuCategory1.getDisplayOrder() - menuCategory2.getDisplayOrder();
                        }
                    });

                    HashSet<MenuCategory> hashSet = new HashSet<MenuCategory>(menuCategories);
                    menuCategories.clear();
                    menuCategories.addAll(hashSet);
                    menuView.showMenuCategories(false, isFromInitialLoad, excludedMenu);

                    try {
                        HashMap<String, Pair<MenuCategory, Set<String>>> menuCategoriesHashMap = new HashMap<>();
                        for (MenuCategory menuCategory : menuCategories) {
                            Set<String> menuCategoryItemIds = new HashSet<>();
                            LinkedList<MenuItem> categoryMenuItems = getCategoryMenuItems(false, menuCategory, appDatabase);
                            for (MenuItem menuItem : categoryMenuItems) {
                                menuCategoryItemIds.add(menuItem.getId());
                            }
                            Pair<MenuCategory, Set<String>> pair = new Pair<>(menuCategory, menuCategoryItemIds);
                            menuCategoriesHashMap.put(menuCategory.getId(), pair);
                        }
                        CacheUtil.getInstance().setMenuCategories(menuCategoriesHashMap);
                    } catch (ConcurrentModificationException e) {
                        e.printStackTrace();
                    }

                    isRefreshing = false;

                }
            });

        } else {
            appDatabase.menuCategoryDao().deleteByApplication(applicationId);
            appDatabase.menuItemDao().deleteByApplication(applicationId);
            appDatabase.modifierGroupDao().deleteByApplication(applicationId);
            appDatabase.modifierItemDao().deleteByApplication(applicationId);
            appDatabase.ingredientDao().deleteByApplication(applicationId);
            appDatabase.allergenDao().deleteByApplication(applicationId);
            appDatabase.menuCategoryEntityDao().deleteByApplication(applicationId);
            appDatabase.menuItemEntityDao().deleteByApplication(applicationId);
            getMenuCdn(fromForceLocation, menuCategoryId, menuItemId);
        }

    }

    @Override
    public void getMenuCategoryItems(int position, String categoryId) {

        MenuApiCall.getInstance().getMenuCategoryItemsAllDetails(categoryId, "1", CacheUtil.getInstance().getMenuId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                try {

                    JSONObject object = new JSONObject(o.toString());
                    JSONArray array = object.getJSONArray("data");

                    LinkedList<MenuItem> menuItems = GsonHelper.gsonFinal().fromJson(array.toString(), new TypeToken<LinkedList<MenuItem>>() {
                    }.getType());

                    // New code
                    LinkedList<MenuItem> removeDisabledItems = new LinkedList<>();
                    for (MenuItem menuItem : menuItems) {
                        if (menuItem.getAttributes().getEnabled() != 0) {
                            removeDisabledItems.add(menuItem);
                        }
                    }
                    menuItems = removeDisabledItems;
                    // above code is new

                    if (CacheUtil.getInstance().getOrderStore() != null) {
                        LinkedList<MenuItem> filteredMenuItems = new LinkedList<>();
                        for (MenuItem menuItem : menuItems) {
                            if(!menuItem.getExcludedLocations().contains(CacheUtil.getOrderStoreId())) {
                                filteredMenuItems.add(menuItem);
                            }
                        }
                        menuItems = filteredMenuItems;
                    }

                    List<MenuCategory> menuCat = excludedMenuCategories(menuCategories);
                    if (position < menuCat.size()) {
                        menuCat.get(position).setMenuItems(menuItems);
                        menuCategoryItems++;
                        if (menuCategoryItems == menuCat.size()) {
                            ArrayList<MenuCategory> filteredMenuCategories = new ArrayList<>();
                            for (MenuCategory menuCategory : menuCat) {
                                if (menuCategory.getMenuItems().size() > 0) {
                                    filteredMenuCategories.add(menuCategory);
                                }
                            }
                            menuCat = filteredMenuCategories;
                            HashMap<String, Pair<MenuCategory, Set<String>>> menuCategoriesHashMap = new HashMap<>();
                            for (MenuCategory menuCategory : menuCat) {
                                Set<String> menuCategoryItemIds = new HashSet<>();
                                for (MenuItem menuItem : menuCategory.getMenuItems()) {
                                    menuCategoryItemIds.add(menuItem.getId());
                                }
                                Pair<MenuCategory, Set<String>> pair = new Pair<>(menuCategory, menuCategoryItemIds);
                                menuCategoriesHashMap.put(menuCategory.getId(), pair);
                            }
                            CacheUtil.getInstance().setMenuCategories(menuCategoriesHashMap);
                            HashSet<MenuCategory> hashSet = new HashSet<MenuCategory>(menuCategories);
                            menuCategories.clear();
                            menuCategories.addAll(hashSet);
                            menuView.showMenuCategories(false, false, menuCat);
                            isRefreshing = false;
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
            }
        });
    }

    @Override
    public void refreshMenuCategories() {
        if (!isRefreshing) {
            isRefreshing = true;
            refreshMenuCategories.clear();
            refreshMenuCategoryItems = 0;
            MenuApiCall.getInstance().getMenuCategories(CacheUtil.getInstance().getMenuId(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    try {
                        JSONObject object = new JSONObject(o.toString());
                        JSONArray array = object.getJSONArray("data");
                        refreshMenuCategories = GsonHelper.gsonFinal().fromJson(array.toString(), new TypeToken<ArrayList<MenuCategory>>() {
                        }.getType());

                        if (CacheUtil.getInstance().getOrderStore() != null) {
                            ArrayList<MenuCategory> enabledMenuCategories = new ArrayList<>();
                            for (MenuCategory menuCategory : refreshMenuCategories) {
                                if (!menuCategory.getLocationIds().contains(CacheUtil.getInstance().getOrderStore().getId())) {
                                    enabledMenuCategories.add(menuCategory);
                                }
                            }
                            refreshMenuCategories = enabledMenuCategories;
                        }

                        Collections.sort(refreshMenuCategories, new Comparator<MenuCategory>() {
                            @Override
                            public int compare(MenuCategory menuCategory1, MenuCategory menuCategory2) {
                                return menuCategory1.getAttributes().getDisplayOrder() - menuCategory2.getAttributes().getDisplayOrder();
                            }
                        });

                        for (int i = 0; i < refreshMenuCategories.size(); i++) {

                            refreshMenuCategoryItems(i, refreshMenuCategories.get(i).getId());

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
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
    public void refreshMenuCategoryItems(int position, String categoryId) {
        MenuApiCall.getInstance().getMenuCategoryItems(categoryId, "1", CacheUtil.getInstance().getMenuId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                try {

                    JSONObject object = new JSONObject(o.toString());
                    JSONArray array = object.getJSONArray("data");

                    LinkedList<MenuItem> menuItems = GsonHelper.gsonFinal().fromJson(array.toString(), new TypeToken<LinkedList<MenuItem>>() {
                    }.getType());

                    if (CacheUtil.getInstance().getOrderStore() != null) {
                        LinkedList<MenuItem> filteredMenuItems = new LinkedList<>();
                        for (MenuItem menuItem : menuItems) {
                            filteredMenuItems.add(menuItem);
                        }
                        menuItems = filteredMenuItems;
                    }

                    if (position < refreshMenuCategories.size()) {
                        refreshMenuCategories.get(position).setMenuItems(menuItems);

                        refreshMenuCategoryItems++;

                        if (refreshMenuCategoryItems == refreshMenuCategories.size()) {

                            ArrayList<MenuCategory> filteredMenuCategories = new ArrayList<>();
                            for (MenuCategory menuCategory : refreshMenuCategories) {
                                if (menuCategory.getMenuItems().size() > 0) {
                                    filteredMenuCategories.add(menuCategory);
                                }
                            }
                            refreshMenuCategories = filteredMenuCategories;

                            HashMap<String, Pair<MenuCategory, Set<String>>> menuCategoriesHashMap = new HashMap<>();
                            for (MenuCategory menuCategory : refreshMenuCategories) {
                                Set<String> menuCategoryItemIds = new HashSet<>();
                                for (MenuItem menuItem : menuCategory.getMenuItems()) {
                                    menuCategoryItemIds.add(menuItem.getId());
                                }
                                Pair<MenuCategory, Set<String>> pair = new Pair<>(menuCategory, menuCategoryItemIds);
                                menuCategoriesHashMap.put(menuCategory.getId(), pair);
                            }

                            CacheUtil.getInstance().setMenuCategories(menuCategoriesHashMap);

                            menuCategories.clear();
                            menuCategories.addAll(refreshMenuCategories);
                            menuView.showMenuCategories(true, false, excludedMenuCategories(menuCategories));
                            isRefreshing = false;
                        }

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
            }
        });
    }

    public void reloadCategories() {

    }

    @Override
    public void reloadMenu() {
        if (CacheUtil.getInstance().getOrderStore() != null) {
            isRefreshing = true;

            AppDatabase appDatabase = AppDatabase.getInstance();
            ArrayList<MenuCategory> savedMenuCategories = new ArrayList<>();
            List<MenuCategoryDb> menuCategoryDbs = appDatabase.menuCategoryDao().getAll();
            // TODO: PERFORMANCE ISSUE!!!
            for (MenuCategoryDb menuCategoryDb : menuCategoryDbs) {
                MenuCategory menuCategory = GsonHelper.gsonFinal().fromJson(menuCategoryDb.data, MenuCategory.class);
                LinkedList<MenuItem> menuItems = getCategoryMenuItems(false, menuCategory, appDatabase);
                Collections.sort(menuItems, new Comparator<MenuItem>() {
                    @Override
                    public int compare(MenuItem menuItem1, MenuItem menuItem2) {
                        return menuItem1.getDisplayOrder() - menuItem2.getDisplayOrder();
                    }
                });
                menuCategory.setMenuItems(menuItems);
                savedMenuCategories.add(menuCategory);
            }

            ArrayList<MenuCategory> enabledMenuCategories = new ArrayList<>();
            for (MenuCategory menuCategory : savedMenuCategories) {
                if (menuCategory.getEnabled() == 1 && !menuCategory.getExcludedLocations().contains(CacheUtil.getInstance().getOrderStore().getId())) {
                    LinkedList<MenuItem> enabledMenuItems = new LinkedList<>();
                    for (MenuItem menuItem : menuCategory.getMenuItems()) {
                        enabledMenuItems.add(menuItem);
                    }
                    Collections.sort(enabledMenuItems, new Comparator<MenuItem>() {
                        @Override
                        public int compare(MenuItem menuItem1, MenuItem menuItem2) {
                            return menuItem1.getDisplayOrder() - menuItem2.getDisplayOrder();
                        }
                    });
                    menuCategory.setMenuItems(enabledMenuItems);
                    enabledMenuCategories.add(menuCategory);
                }
            }
            ArrayList<MenuCategory> filteredMenuCategories = new ArrayList<>();
            for (MenuCategory menuCategory : enabledMenuCategories) {
                if (menuCategory.getMenuItems().size() > 0) {
                    filteredMenuCategories.add(menuCategory);
                }
            }
            menuCategories.clear();
            menuCategories.addAll(filteredMenuCategories);
            Collections.sort(menuCategories, new Comparator<MenuCategory>() {
                @Override
                public int compare(MenuCategory menuCategory1, MenuCategory menuCategory2) {
                    return menuCategory1.getDisplayOrder() - menuCategory2.getDisplayOrder();
                }
            });

            HashMap<String, Pair<MenuCategory, Set<String>>> menuCategoriesHashMap = new HashMap<>();
            for (MenuCategory menuCategory : menuCategories) {
                Set<String> menuCategoryItemIds = new HashSet<>();
                for (MenuItem menuItem : menuCategory.getMenuItems()) {
                    menuCategoryItemIds.add(menuItem.getId());
                }
                Pair<MenuCategory, Set<String>> pair = new Pair<>(menuCategory, menuCategoryItemIds);
                menuCategoriesHashMap.put(menuCategory.getId(), pair);
            }

            CacheUtil.getInstance().setMenuCategories(menuCategoriesHashMap);
            menuView.showMenuCategories(false, false, excludedMenuCategories(menuCategories));
            isRefreshing = false;
        }
    }

    @Override
    public void onBindMenuCategoryViewAtPosition(int position, MenuCategoryViewHolder menuCategoryViewHolder, boolean selected) {
        if (!excludedMenu.isEmpty()) {
            menuCategoryViewHolder.setMenuCategory(excludedMenu.get(position), selected);
        } else {
            menuCategoryViewHolder.setMenuCategory(excludedMenuCategories(menuCategories).get(position), selected);
        }
    }

    private List<MenuCategory> excludedMenuCategories(List<MenuCategory> menuCategories) {
        this.excludedMenu.clear();
        if (CacheUtil.getOrderStoreId() != null && !excludedCategories.isEmpty() && excludedCategories.containsKey(CacheUtil.getOrderStoreId())) {
            List<MenuCategory> categories = new ArrayList<>(menuCategories);
            categories.removeAll(excludedCategories.get(CacheUtil.getOrderStoreId()));
            this.excludedMenu.addAll(categories);
        } else {
            this.excludedMenu.addAll(menuCategories);
        }
        Collections.sort(excludedMenu, new Comparator<MenuCategory>() {
            @Override
            public int compare(MenuCategory menuCategory1, MenuCategory menuCategory2) {
                return menuCategory1.getDisplayOrder() - menuCategory2.getDisplayOrder();
            }
        });
        return excludedMenu;
    }

    @Override
    public int getMenuCategoriesCount() {
        if (!excludedMenu.isEmpty()) {
            return excludedMenu.size();
        }
        return excludedMenuCategories(menuCategories).size();
    }

    @Override
    public int getSearchItemsSize() {
        return searchedMenuItems.size();
    }

    @Override
    public void selectMenuCategory(String menuCategoryId) {
        List<MenuCategory> menu = excludedMenuCategories(menuCategories);
        Collections.sort(menu, new Comparator<MenuCategory>() {
            @Override
            public int compare(MenuCategory menuCategory1, MenuCategory menuCategory2) {
                return menuCategory1.getDisplayOrder() - menuCategory2.getDisplayOrder();
            }
        });
        for (MenuCategory menuCategory : menu) {
            if (menuCategory.getId().equalsIgnoreCase(menuCategoryId)) {
                this.menuCategory = menuCategory;
                menuView.selectMenuCategory(menu.indexOf(menuCategory), menuCategory);
                break;
            }
        }
    }

    @Override
    public void selectMenuCategory(int position, MenuCategory meuCategory) {
        menuView.selectMenuCategory(position, meuCategory);
    }

    @Override
    public MenuCategory getMenuCategory() {
        return menuCategory;
    }

    @Override
    public void showMenuItem(String menuItemId) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                String applicationId = CacheUtil.getInstance().getApplication().getId();
                AppDatabase appDatabase = AppDatabase.getInstance();
                MenuItemEntity menuItemEntity = appDatabase.menuItemEntityDao().findById(applicationId, menuItemId);
                if (menuItemEntity != null) {
                    String categoryId = menuItemEntity.getMenuCategoryId();
                    if (categoryId != null && !categoryId.isEmpty()) {
                        menuView.showMenuItem(categoryId, menuItemId);
                    }
                }
            }
        });
    }

    @Override
    public void setMenuCategory(MenuCategory menuCategory) {
        this.menuCategory = menuCategory;
    }

    @Override
    public void setMenuCategory(String menuCategoryId) {
        MenuCategory menuCategory = new MenuCategory();
        menuCategory.setId(menuCategoryId);
        this.menuCategory = menuCategory;
    }

    @Override
    public void setMenuCategory(MenuItem menuItem) {

        boolean found = false;
        for (MenuCategory menuCategory : excludedMenuCategories(menuCategories)) {
            if (!found) {
                for (MenuItem menuCategoryItem : menuCategory.getMenuItems()) {
                    if (menuCategoryItem.getId().equals(menuItem.getId())) {
                        this.menuCategory = menuCategory;
                        found = true;
                        break;
                    }
                }
            } else {
                break;
            }
        }
    }

    @Override
    public MenuItem getMenuItem() {
        return menuItem;
    }

    @Override
    public void getMenuItem(boolean addToBag, String menuItemId) {

        Executor executor = Executors.newSingleThreadExecutor();

        AppDatabase appDatabase = AppDatabase.getInstance();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                String data = getMenuItemData(appDatabase, menuItemId);
                if (data != null && !data.isEmpty()) {
                    String applicationId = CacheUtil.getInstance().getApplication().getId();
                    menuItem = MenuUtil.processMenuItemCustomObject(GsonHelper.gsonFinal().fromJson(data, MenuItem.class));

                    if (menuCategory == null) {
                        MenuItemEntity menuItemEntity = appDatabase.menuItemEntityDao().findById(applicationId, menuItemId);
                        setMenuCategory(menuItemEntity.getMenuCategoryId());
                    }

                    if (menuItem.isCombo) {
                        menuView.showLoader();
                    }

                    menuView.setMenuItemDetails(menuItem);

                    LinkedList<Allergen> allergens = new LinkedList<>();
                    for (AllergenDb allergenDb : appDatabase.allergenDao().findAllByApplicationAndIds(applicationId, menuItem.getAllergensIds().toArray(new String[menuItem.getAllergensIds().size()]))) {
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
                    for (Ingredient ingredient : menuItem.getIngredientIds()) {
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
                    for (ModifierGroup modifierGroup : menuItem.getModifierGroupIds()) {
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
                        List<String> excludedItemsByLocationId = Observable.fromArray(modifierGroupIds.get(modifierGroupCdn.getId()))
                                .map(modifierGroup -> {
                                    if (orderStore == null) return new ArrayList<ModifierItem>();
                                    // if group contains store id in included location, add all modifiers within it to the excluded items
                                    // else check modifier item excluded location if it contains store id
                                    if (Observable.fromIterable(modifierGroup.getExcludedLocations())
                                            .map(Object::toString)
                                            .any(excludedLocation -> excludedLocation.equals(orderStore.getId()))
                                            .blockingGet()) {
                                        return modifierGroup.getModifiers();
                                    } else {
                                        return Observable.fromIterable(modifierGroup.getModifierItems())
                                                .filter(modifierItem -> Observable.fromIterable(modifierItem.getAttributes().getExcludedLocations())
                                                        .any(item -> item.toString().equals(orderStore.getId()))
                                                        .blockingGet())
                                                .toList()
                                                .blockingGet();
                                    }
                                })
                                .flatMap(Observable::fromIterable)
                                .map(ModifierItem::getId)
                                .toList()
                                .blockingGet();

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
                            boolean isExcluded = orderStore != null && Observable.fromIterable(modifierItem.getAttributes().getExcludedLocations())
                                    .any(item -> item.toString().equals(orderStore.getId()))
                                    .blockingGet();
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

                    prepareMenuItem(addToBag);

                } else {
                    menuView.showError(CacheUtil.getInstance().getTranslations(Text.ITEM_NO_LONGER_AVAILABLE));
                }
            }
        });

    }

    @Override
    public void prepareMenuItem(boolean addToBag) {

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

        if (!addToBag) {
            if (menuItem.isCombo && !menuItem.combo.isEmpty()) {
                setupComponents();
            } else {
                menuView.setOriginalMenuItemValue(new Gson().toJson(menuItem));
                menuView.setMenuItem(menuCategory, menuItem);
            }
        } else {
            boolean invalid = false;

            ArrayList<ModifierGroup> visibleModifierGroups = new ArrayList<>();

            for (ModifierGroup group : menuItem.getModifierGroups()) {
                visibilityConfig = group.getAttributes().getVisibilityConfig();
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
                    menuView.showMessage(CacheUtil.getInstance().getTranslations(Text.MODIFIER_MINIMUM_LIMIT, "Minimum number of extras (x) not reached for (y)").replace("(x)", String.valueOf(modifierGroup.getAttributes().getMinimum())).replace("(y)", modifierGroup.getAttributes().getName()));
                    invalid = true;
                    break;
                }
            }

            if(!invalid) {
                invalid = checkComboModifiers();
            }

            if (!invalid) {
                addCartItem(false, false);
            } else {
                menuView.selectMenuItem(menuCategory, menuItem);
            }
        }
    }

    private boolean checkComboModifiers() {
        boolean invalid = false;
        if(menuItem.isCombo) {
            int comboIndex = 0;
            int trueIndex = 0;
            for (ModifierItem size : menuItem.getSizes().getModifierItems()) {
                if (size.getAttributes().isSelected()) {
                    trueIndex = comboIndex;
                }
                comboIndex++;
            }
            if (menuItem.isCombo) {
                for (MenuItemComponents components : menuItem.combo.get(trueIndex).components) {
                    if(invalid) {
                        break;
                    }
                    for (ComponentModifierItem item : components.items) {
                        if(invalid) {
                            break;
                        }
                        if (item.selected) {
                            if (!item.modifierGroups.isEmpty()) {
                                for (ModifierGroup modifierGroup : item.modifierGroups) {
                                    int selected = 0;
                                    for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                                        if (modifierItem.getAttributes().isSelected()) {
                                            selected += modifierItem.getAttributes().getQuantity();
                                        }
                                    }
                                    if (selected < modifierGroup.getAttributes().getMinimum()) {
                                        menuView.showMessage(CacheUtil.getInstance().getTranslations(Text.MODIFIER_MINIMUM_LIMIT, "Minimum number of extras (x) not reached for (y)").replace("(x)", String.valueOf(modifierGroup.getAttributes().getMinimum())).replace("(y)", modifierGroup.getAttributes().getName()));
                                        invalid = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return invalid;
    }

    public void setupComponents() {
        selectedComponents.clear();
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                ArrayList<MenuItemComponents> menuItemComponents = new ArrayList<>();
                for (MenuItemCombo combo : menuItem.combo) {
                    menuItemComponents.clear();
                    menuItemComponents.addAll(combo.components);
                    for (int componentIndex = 0; componentIndex < menuItemComponents.size(); componentIndex++) {
                        MenuItemComponents components = menuItemComponents.get(componentIndex);
                        if (!components.items.isEmpty()) {
                            try {
                                Collections.sort(combo.components.get(componentIndex).items, (menuItem1, menuItem2) -> menuItem1.displayOrder - menuItem2.displayOrder);
                                for (ComponentModifierItem compoItem : components.items) {
                                    int compoItemIndex = components.items.indexOf(compoItem);
                                    LinkedList<ModifierGroup> modifierGroups = new LinkedList<>();
                                    HashMap<String, ModifierGroup> modifierGroupIds = new HashMap<>();
                                    for (ModifierGroup modifierGroup : compoItem.modifierGroupsMap) {
                                        modifierGroupIds.put(modifierGroup.getId(), modifierGroup);
                                    }
                                    AppDatabase appDatabase = AppDatabase.getInstance();
                                    String applicationId = CacheUtil.getInstance().getApplication().getId();
                                    for (ModifierGroupDb modifierGroupDb : appDatabase.modifierGroupDao().findAllByApplicationAndIds(applicationId, modifierGroupIds.keySet().toArray(new String[modifierGroupIds.size()]))) {
                                        if (modifierGroupDb.type != ModifierGroupType.SIZES.getValue()) {
                                            ModifierGroupCdn modifierGroupCdn = MenuUtil.processModifierGroupCustomObject(GsonHelper.gsonFinal().fromJson(modifierGroupDb.data, ModifierGroupCdn.class));
                                            ModifierGroup mapModifierGroup = modifierGroupIds.get(modifierGroupCdn.getId());
                                            HashSet<String> excludeModifiers = new HashSet();
                                            if(mapModifierGroup != null && mapModifierGroup.getExcludeModifiers() != null && !mapModifierGroup.getExcludeModifiers().isEmpty()) {
                                                for (Integer excludeModifierId : mapModifierGroup.getExcludeModifiers()) {
                                                    excludeModifiers.add(excludeModifierId.toString());
                                                }
                                            }
                                            LinkedList<ModifierItem> modifierItems = new LinkedList<>();
                                            for (ModifierItemDb modifierItemDb : appDatabase.modifierItemDao().findAllByApplicationAndIds(applicationId, modifierGroupCdn.getAttributes().getModifiers().toArray(new String[modifierGroupCdn.getAttributes().getModifiers().size()]))) {
                                                if(!excludeModifiers.contains(modifierItemDb.id)) {
                                                    ModifierItemCdn modifierItemCdn = GsonHelper.gsonFinal().fromJson(modifierItemDb.data, ModifierItemCdn.class);
                                                    ModifierItem modifierItem = ModifierItemCdn.convert(modifierItemCdn, LocaleUtil.getInstance().getLanguage());
                                                    modifierItem.getAttributes().setSelected(modifierItem.getAttributes().getMinimum() == 1);
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
                                            if (mapModifierGroup != null) {
                                                modifierGroup.getAttributes().setMinimum(mapModifierGroup.getMin());
                                                modifierGroup.getAttributes().setMaximum(mapModifierGroup.getMax());
                                                modifierGroup.getAttributes().setDisplayOrder(mapModifierGroup.getDisplayOrder());
                                                modifierGroup.getAttributes().setFreeModifiersCount(mapModifierGroup.getFreeModifiersCount());
                                                modifierGroup.setDefaultModifiers(new ArrayList<>(mapModifierGroup.getDefaultModifiers()));
                                                modifierGroup.getAttributes().setVisibilityConfig(mapModifierGroup.getVisibilityConfig());
                                            }
                                            int size = modifierItems.size();
                                            ArrayList<ModifierItem> modifierItemsFinal = new ArrayList<>();
                                            for (int i = 0; i < size; i++) {
                                                boolean found = false;
                                                for (int a = 0; a < modifierGroup.getExcludeModifiers().size(); a++) {
                                                    if (modifierItems.get(i).getId().equalsIgnoreCase(String.valueOf(modifierGroup.getExcludeModifiers().get(a)))) {
                                                        found = true;
                                                        break;
                                                    }
                                                }
                                                if (!found) {
                                                    modifierItemsFinal.add(modifierItems.get(i));
                                                }
                                            }

                                            if (modifierItemsFinal.size() > 0) {

                                                Set<String> defaultModifiers = modifierGroup.getDefaultModifiers();
                                                if (modifierItemsFinal.size() > 0 && modifierGroup.getAttributes().getMinimum() > 0) {
                                                    if (defaultModifiers.size() > 0) {
                                                        int selected = 0;
                                                        for (int i = 0; i < modifierItemsFinal.size(); i++) {
                                                            ModifierItem modifierItem = modifierItemsFinal.get(i);
                                                            if (selected < modifierGroup.getAttributes().getMaximum() && defaultModifiers.contains(modifierItem.getId())) {
                                                                modifierItemsFinal.get(i).getAttributes().setSelected(true);
                                                                selected++;
                                                            }
                                                        }
                                                    } else if (modifierGroup.getAttributes().getMinimum() == 1) {
                                                        modifierItemsFinal.get(0).getAttributes().setSelected(true);
                                                    }
                                                }

                                                modifierGroup.setModifierItems(modifierItemsFinal);
                                                modifierGroups.add(modifierGroup);
                                            }
                                        }
                                    }
                                    Collections.sort(modifierGroups, new Comparator<ModifierGroup>() {
                                        @Override
                                        public int compare(ModifierGroup modifierGroup1, ModifierGroup modifierGroup2) {
                                            return modifierGroup1.getAttributes().getDisplayOrder() - modifierGroup2.getAttributes().getDisplayOrder();
                                        }
                                    });
                                    //compoItem.modifierGroups = new ArrayList<>(modifierGroups);
                                    combo.components.get(componentIndex).items.get(compoItemIndex).modifierGroups = new ArrayList<>(modifierGroups);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                menuView.setOriginalMenuItemValue(new Gson().toJson(menuItem));
                menuView.setMenuItem(menuCategory, menuItem);
            }
        });

    }

    public ArrayList<ModifierGroup> getModifierGroups(int itemId) {
        AppDatabase appDatabase = AppDatabase.getInstance();
        String menuItemData = getMenuItemData(appDatabase, String.valueOf(itemId));
        if (menuItemData != null && !menuItemData.isEmpty()) {
            try {
                String applicationId = CacheUtil.getInstance().getApplication().getId();
                MenuItem menuItemNew = MenuUtil.processMenuItemCustomObject(GsonHelper.gsonFinal().fromJson(menuItemData, MenuItem.class));
                LinkedList<Allergen> allergens = new LinkedList<>();
                for (AllergenDb allergenDb : appDatabase.allergenDao().findAllByApplicationAndIds(applicationId, menuItemNew.getAllergensIds().toArray(new String[menuItemNew.getAllergensIds().size()]))) {
                    AllergenCdn allergenCdn = GsonHelper.gsonFinal().fromJson(allergenDb.data, AllergenCdn.class);
                    allergens.add(AllergenCdn.convert(allergenCdn, LocaleUtil.getInstance().getLanguage()));
                }
                Collections.sort(allergens, new Comparator<Allergen>() {
                    @Override
                    public int compare(Allergen allergen1, Allergen allergen2) {
                        return allergen1.getAttributes().getDisplayOrder() - allergen2.getAttributes().getDisplayOrder();
                    }
                });
                menuItemNew.setAllergens(allergens);

                LinkedList<Ingredient> ingredients = new LinkedList<>();
                ArrayList<String> ingredientIds = new ArrayList<>();
                for (Ingredient ingredient : menuItemNew.getIngredientIds()) {
                    ingredientIds.add(ingredient.getId());
                }
                for (IngredientDb ingredientDb : appDatabase.ingredientDao().findAllByApplicationAndIds(applicationId, ingredientIds.toArray(new String[ingredientIds.size()]))) {
                    IngredientCdn ingredientCdn = GsonHelper.gsonFinal().fromJson(ingredientDb.data, IngredientCdn.class);
                    ingredients.add(IngredientCdn.convert(ingredientCdn, LocaleUtil.getInstance().getLanguage()));
                }
                Collections.sort(ingredients, new Comparator<Ingredient>() {
                    @Override
                    public int compare(Ingredient ingredient1, Ingredient ingredient2) {
                        return ingredient1.getAttributes().getDisplayOrder() - ingredient2.getAttributes().getDisplayOrder();
                    }
                });
                menuItemNew.setIngredients(ingredients);

                ModifierGroup sizes = new ModifierGroup();
                LinkedList<ModifierGroup> modifierGroups = new LinkedList<>();
                HashMap<String, ModifierGroup> modifierGroupIds = new HashMap<>();
                for (ModifierGroup modifierGroup : menuItemNew.getModifierGroupIds()) {
                    modifierGroupIds.put(modifierGroup.getId(), modifierGroup);
                }
                for (ModifierGroupDb modifierGroupDb : appDatabase.modifierGroupDao().findAllByApplicationAndIds(applicationId, modifierGroupIds.keySet().toArray(new String[modifierGroupIds.size()]))) {
                    ModifierGroupCdn modifierGroupCdn = MenuUtil.processModifierGroupCustomObject(GsonHelper.gsonFinal().fromJson(modifierGroupDb.data, ModifierGroupCdn.class));
                    LinkedList<ModifierItem> modifierItems = new LinkedList<>();
                    for (ModifierItemDb modifierItemDb : appDatabase.modifierItemDao().findAllByApplicationAndIds(applicationId, modifierGroupCdn.getAttributes().getModifiers().toArray(new String[modifierGroupCdn.getAttributes().getModifiers().size()]))) {
                        ModifierItemCdn modifierItemCdn = GsonHelper.gsonFinal().fromJson(modifierItemDb.data, ModifierItemCdn.class);
                        ModifierItem modifierItem = ModifierItemCdn.convert(modifierItemCdn, LocaleUtil.getInstance().getLanguage());
                        modifierItem.getAttributes().setSelected(modifierItem.getAttributes().getMinimum() == 1);
                        modifierItems.add(modifierItem);
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
                    int size = modifierItems.size();
                    ArrayList<ModifierItem> modifierItemsFinal = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        boolean found = false;
                        for (int a = 0; a < modifierGroup.getExcludeModifiers().size(); a++) {
                            if (modifierItems.get(i).getId().equalsIgnoreCase(String.valueOf(modifierGroup.getExcludeModifiers().get(a)))) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            modifierItemsFinal.add(modifierItems.get(i));
                        }
                    }
                    modifierGroup.setModifierItems(modifierItemsFinal);
                    if (modifierGroupDb.type == ModifierGroupType.SIZES.getValue()) {
                        sizes = modifierGroup;
                    } else {
                        modifierGroups.add(modifierGroup);
                    }
                }
                menuItemNew.setSizes(sizes);
                Collections.sort(modifierGroups, new Comparator<ModifierGroup>() {
                    @Override
                    public int compare(ModifierGroup modifierGroup1, ModifierGroup modifierGroup2) {
                        return modifierGroup1.getAttributes().getDisplayOrder() - modifierGroup2.getAttributes().getDisplayOrder();
                    }
                });
                menuItemNew.setModifierGroups(modifierGroups);

                LinkedList<ModifierGroup> filteredModifierGroups = new LinkedList<>();

                for (ModifierGroup modifierGroup : menuItemNew.getModifierGroups()) {
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

                if (menuItemNew.getSizes().getModifierItems().size() > 0) {
                    menuItemNew.getSizes().getModifierItems().get(0).getAttributes().setSelected(true);
                }

                menuItemNew.setModifierGroups(filteredModifierGroups);

                for (ModifierGroup modifierGroup : menuItemNew.getModifierGroups()) {
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

                VisibilityConfig visibilityConfig = menuItemNew.getSizes().getAttributes().getVisibilityConfig();
                if (visibilityConfig != null) {
                    List<VisibilityConfigRule> rules = visibilityConfig.getRules();
                    if (rules != null && rules.size() > 0) {
                        VisibilityConfigRule rule = rules.get(0);
                        if (rule.getTargetType() != null && rule.getTargetType().equalsIgnoreCase("modifier-group")) {
                            for (ModifierGroup modGroup : menuItemNew.getModifierGroups()) {
                                if (!menuItemNew.getSizes().getId().equals(modGroup.getId()) && rule.getTargetId().equals(modGroup.getId())) {
                                    if (rule.getTargetIs() != null && rule.getTargetIs().equalsIgnoreCase("selected")) {
                                        int selected = 0;
                                        for (ModifierItem modItem : modGroup.getModifierItems()) {
                                            if (modItem.getAttributes().isSelected()) {
                                                selected += 1;
                                            }
                                        }
                                        if (selected > 0) {
                                            menuItemNew.getSizes().getAttributes().getVisibilityConfig().setInitialValue("");
                                        } else {
                                            menuItemNew.getSizes().getAttributes().getVisibilityConfig().setInitialValue("hidden");
                                        }
                                    }
                                }

                            }
                        }

                    }
                }

                for (ModifierGroup group : menuItemNew.getModifierGroups()) {
                    visibilityConfig = group.getAttributes().getVisibilityConfig();
                    if (visibilityConfig != null) {
                        List<VisibilityConfigRule> rules = visibilityConfig.getRules();
                        if (rules != null && rules.size() > 0) {
                            VisibilityConfigRule rule = rules.get(0);
                            if (rule.getTargetType() != null) {
                                if (rule.getTargetType().equalsIgnoreCase("modifier-group")) {
                                    if (!group.getId().equals(menuItemNew.getSizes().getId()) && rule.getTargetId().equals(menuItemNew.getSizes().getId())) {
                                        if (rule.getTargetIs() != null && rule.getTargetIs().equalsIgnoreCase("selected")) {
                                            int selected = 0;
                                            for (ModifierItem modItem : menuItemNew.getSizes().getModifierItems()) {
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

                                    for (ModifierGroup modGroup : menuItemNew.getModifierGroups()) {
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
                                    for (ModifierItem modItem : menuItemNew.getSizes().getModifierItems()) {
                                        if (modItem.getAttributes().isSelected()) {
                                            selectedModifiers.add(modItem.getId());
                                            break;
                                        }
                                    }

                                    for (ModifierGroup modGroup : menuItemNew.getModifierGroups()) {
                                        if (!group.getId().equals(modGroup.getId())) {
                                            for (ModifierItem modItem : modGroup.getModifierItems()) {
                                                if (modItem.getAttributes().isSelected()) {
                                                    selectedModifiers.add(modItem.getId());
                                                }
                                            }
                                        }
                                    }

                                    //for (ModifierGroup modGroup : menuItemNew.getModifierGroups()) {
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
                return new ArrayList<>(menuItemNew.getModifierGroups());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new ArrayList<>();
    }

    public boolean checkIfNoItemSelected(MenuItemComponents component, ArrayList<ComponentModifierItem> items) {
        int size = items.size();
        for (int i = 0; i < size; i++) {
            if (items.get(i).selected) {
                selectedComponents.put(component.id, i);
                return false;
            }
        }
        return true;
    }


    @Override
    public ArrayList<MenuItemComponents> componentsSelectFirst(ArrayList<MenuItemComponents> components) {
        int size = components.size();
        for (int i = 0; i < size; i++) {
            if (!components.get(i).items.isEmpty()) {
                if (!isFromCartEdit || checkIfNoItemSelected(components.get(i), components.get(i).items)) {
                    components.get(i).items.get(0).selected = true;
                    selectedComponents.put(components.get(i).id, 0);
                }
            }
        }
        return components;
    }

    @Override
    public void setMenuItem(MenuItem menuItem) {

        this.menuItem = menuItem;

    }

    @Override
    public void onBindMenuItemViewAtPosition(MenuCategory menuCategory, int position, MenuItemViewHolder menuItemViewHolder) {
        if (menuCategory != null) {
            menuItemViewHolder.setMenuItem(menuCategory, menuCategory.getMenuItems().get(position));
        } else {
            menuItemViewHolder.setMenuItem(null, searchedMenuItems.get(position));
        }
    }

    @Override
    public int getMenuItemsCount(MenuCategory menuCategory) {
        if (menuCategory != null) {
            return menuCategory.getMenuItems().size();
        } else {
            return searchedMenuItems.size();
        }
    }

    @Override
    public void selectMenuItem(MenuCategory menuCategory, MenuItem menuItem) {
        if (menuCategory == null) {
            for (MenuCategory category : excludedMenuCategories(menuCategories)) {
                for (MenuItem item : category.getMenuItems()) {
                    if (item.getId().equals(menuItem.getId())) {
                        menuCategory = category;
                        break;
                    }
                }
                if (menuCategory != null) {
                    break;
                }
            }
        }
        menuView.selectMenuItem(menuCategory, menuItem);
    }

    @Override
    public void clearSearchedMenuItems() {
        searchedMenuItems.clear();
    }

    @Override
    public void hideSearchedMenuItems() {
        menuView.hideSearchedMenuItems(menuCategories.get(0));
    }

    ArrayList<MenuItem> getMenuItemDataByName(AppDatabase appDatabase, String name) {
        ArrayList<MenuItem> data = new ArrayList<MenuItem>();
        try {
            //TODO: Use file cache only for Lorenzo for now
            if (BuildUtil.soloAppKey().equalsIgnoreCase("7r5YQwmSgZCtM0ux")) {
                //  data.add(menuView.getFromCache(name)) ;
            } else {
                for (MenuItemDb menuItemDb : appDatabase.menuItemDao().findByName(name)) {
                    MenuItem item = GsonHelper.gsonFinal().fromJson(menuItemDb.data, MenuItem.class);
                    if (item.getAttributes().getEnabled() != 0) {
                        data.add(item);
                    }
                }
            }
        } catch (Exception e) {
            //data = menuView.getFromCache(name);
        }
        return data;
    }

    @Override
    public void search(String query) {
        MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_SEARCH_ITEM);
        String storeId = CacheUtil.getOrderStoreId();
        AppDatabase appDatabase = AppDatabase.getInstance();
        ArrayList<MenuItem> menuItemData = getMenuItemDataByName(appDatabase, "%" + query + "%");
        if (menuItemData != null && !menuItemData.isEmpty()) {
            try {
                searchedMenuItems.clear();
                for (MenuItem searchMenuItem : menuItemData) {
                    if (storeId == null || (storeId != null && !searchMenuItem.getExcludedLocations().contains(storeId))) {
                        searchedMenuItems.add(searchMenuItem);
                    }

                }
                menuView.showSearchedMenuItems(searchedMenuItems);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            MenuApiCall.getInstance().searchItems(CacheUtil.getInstance().getMenuId(), query, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {

                    try {

                        JSONObject object = new JSONObject(o.toString());
                        JSONArray array = object.getJSONArray("data");

                        searchedMenuItems.clear();
                        ArrayList<MenuItem> searchedMenuItemsList = GsonHelper.gsonFinal().fromJson(array.toString(), new TypeToken<ArrayList<MenuItem>>() {
                        }.getType());

                        Boolean isExcluded = false;

                        for (MenuItem searchMenuItem : searchedMenuItemsList) {

                            if (storeId != null) {
                                String menuItemData = getMenuItemData(appDatabase, searchMenuItem.getId());
                                if (menuItemData != null && !menuItemData.isEmpty()) {
                                    try {
                                        itemById = GsonHelper.gsonFinal().fromJson(menuItemData, MenuItem.class);
                                        if (itemById.getExcludedLocations().contains(storeId)) {
                                            isExcluded = true;
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            if (searchMenuItem.getAttributes().getEnabled() != 0 && !isExcluded) {
                                searchMenuItem.toCdn(CacheUtil.getOrderStoreId());
                                searchedMenuItems.add(searchMenuItem);
                            }
                        }

                        menuView.showSearchedMenuItems(searchedMenuItems);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    menuView.hideSearchedMenuItems(menuCategories.get(0));
                }
            });
        }
    }

    @Override
    public String selectedSizeId() {
        return selectedSizeId;
    }

    @Override
    public void setSelectedSizeId(String selectedSize) {
        selectedSizeId = selectedSize;
    }

    @Override
    public MenuItem correctSizeModifier(String id) {
        String lastSelectedSizeId = selectedSizeId();
        int size = menuItem.getSizes().getModifierItems().size();
        for (int i = 0; i < size; i++) {
            ModifierItem item = menuItem.getSizes().getModifierItems().get(i);
            if (item.getId().equalsIgnoreCase(id)) {
                menuItem.getSizes().getModifierItems().get(i).getAttributes().setSelected(true);
            } else {
                menuItem.getSizes().getModifierItems().get(i).getAttributes().setSelected(false);
            }
        }
        String menuItemToCopy = "";
        if (menuItem.isCombo) {
            for (int i = 0; i < menuItem.combo.size(); i++) {
                if (menuItem.combo.get(i).id.equalsIgnoreCase(lastSelectedSizeId)) {
                    menuItemToCopy = new Gson().toJson(menuItem.combo.get(i).components);
                    for (int a = 0; a < menuItem.combo.get(i).components.size(); a++) {
                        for (int x = 0; x < menuItem.combo.get(i).components.get(a).items.size(); x++) {
                            menuItem.combo.get(i).components.get(a).items.get(x).selected = false;
                            for (int y = 0; y < menuItem.combo.get(i).components.get(a).items.get(x).modifierGroups.size(); y++) {
                                for (int z = 0; z < menuItem.combo.get(i).components.get(a).items.get(x).modifierGroups.get(y).getModifierItems().size(); z++) {
                                    menuItem.combo.get(i).components.get(a).items.get(x).modifierGroups.get(y).getModifierItems().get(z).selected = false;
                                    menuItem.combo.get(i).components.get(a).items.get(x).modifierGroups.get(y).getModifierItems().get(z).setQuantity(0);
                                }
                            }
                        }
                    }
                }
            }
        }
        Type userListType = new TypeToken<ArrayList<MenuItemComponents>>() {
        }.getType();
        ArrayList<MenuItemComponents> mens = GsonHelper.gsonFinal().fromJson(menuItemToCopy, userListType);
        if (menuItem.isCombo) {
            for (int i = 0; i < menuItem.combo.size(); i++) {
                if (menuItem.combo.get(i).id.equalsIgnoreCase(id)) {
                    for (int a = 0; a < menuItem.combo.get(i).components.size(); a++) {
                        for (int m = 0; m < mens.size(); m++) {
                            if (menuItem.combo.get(i).components.get(a).id.equalsIgnoreCase(mens.get(m).id)) {
                                if (mens.get(m).id.equalsIgnoreCase(menuItem.combo.get(i).components.get(a).id)) {
                                    for (int x = 0; x < menuItem.combo.get(i).components.get(a).items.size(); x++) {
                                        for (int bb = 0; bb < mens.get(m).items.size(); bb++) {
                                            if (mens.get(m).items.get(bb).id.equalsIgnoreCase(menuItem.combo.get(i).components.get(a).items.get(x).id)) {
                                                menuItem.combo.get(i).components.get(a).items.get(x).selected = mens.get(m).items.get(bb).selected;
                                                for (int cc = 0; cc < mens.get(m).items.get(bb).modifierGroups.size(); cc++) {
                                                    for (int zz = 0; zz < menuItem.combo.get(i).components.get(a).items.get(x).modifierGroups.size(); zz++) {
                                                        try {
                                                            if (mens.get(m).items.get(bb).modifierGroups.get(cc).getId().equalsIgnoreCase(menuItem.combo.get(i).components.get(a).items.get(x).modifierGroups.get(zz).getId())) {
                                                                for (int ff = 0; ff < mens.get(m).items.get(bb).modifierGroups.get(cc).getModifierItems().size(); ff++) {
                                                                    for (int gg = 0; gg < menuItem.combo.get(i).components.get(a).items.get(x).modifierGroups.get(zz).getModifierItems().size(); gg++) {
                                                                        if (mens.get(m).items.get(bb).modifierGroups.get(cc).getModifierItems().get(ff).getId().equalsIgnoreCase(
                                                                                menuItem.combo.get(i).components.get(a).items.get(x).modifierGroups.get(zz).getModifierItems().get(gg).getId())) {
                                                                            menuItem.combo.get(i).components.get(a).items.get(x).modifierGroups.get(zz).getModifierItems().get(gg).getAttributes().setSelected(mens.get(m).items.get(bb).modifierGroups.get(cc).getModifierItems().get(ff).getAttributes().isSelected());
                                                                            menuItem.combo.get(i).components.get(a).items.get(x).modifierGroups.get(zz).getModifierItems().get(gg).getAttributes().setQuantity(mens.get(m).items.get(bb).modifierGroups.get(cc).getModifierItems().get(ff).getAttributes().getQuantity());
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return menuItem;
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
    public void changeMenuItemQuantity(boolean add) {
        if (!add && menuItem.getAttributes().getQuantity() != 1) {
            menuItem.getAttributes().setQuantity(menuItem.getAttributes().getQuantity() - 1);
        } else if (add) {
            menuItem.getAttributes().setQuantity(menuItem.getAttributes().getQuantity() + 1);
        }
        menuView.setMenuItemQuantity(menuItem.getAttributes().getQuantity());
        getMenuItemTotalPrice(menuItem, -1);
    }

    @Override
    public void getModifiers() {

        MenuApiCall.getInstance().getModifierGroups(CacheUtil.getInstance().getMenuId(), menuCategory.getId(), menuItem.getId(), new ApiRequestListener() {
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

                        if (menuItem.getSizes().getModifierItems().size() > 0) {
                            menuItem.getSizes().getModifierItems().get(0).getAttributes().setSelected(true);
                        }

                        for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
                            if (modifierGroup.getAttributes().getMinimum() == 1 && modifierGroup.getModifierItems().size() > 0) {
                                modifierGroup.getModifierItems().get(0).getAttributes().setSelected(true);
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
                                        break;
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
                                if (rule.getTargetType() != null && rule.getTargetType().equalsIgnoreCase("modifier-group")) {

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
                                }

                            }
                        }
                    }

                    getIngredients();

                } catch (JSONException e) {
                    e.printStackTrace();
                    getIngredients();
                }

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                getIngredients();
            }
        });

    }

    @Override
    public void getIngredients() {

        MenuApiCall.getInstance().getCategoryItemIngredients(CacheUtil.getInstance().getMenuId(), menuCategory.getId(), menuItem.getId(), "1", new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {

                try {

                    JSONObject object = new JSONObject(o.toString());

                    menuItem.setIngredients(GsonHelper.gsonFinal().fromJson(object.getJSONArray("data").toString(), new TypeToken<ArrayList<Ingredient>>() {
                    }.getType()));

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                menuView.setMenuItem(menuCategory, menuItem);

                menuView.setMenuItemQuantity(menuItem.getAttributes().getQuantity());
                getMenuItemTotalPrice(menuItem, -1);

            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                menuView.setMenuItem(menuCategory, menuItem);

                menuView.setMenuItemQuantity(menuItem.getAttributes().getQuantity());
                getMenuItemTotalPrice(menuItem, -1);
            }
        });

    }

    @Override
    public void selectSize(String modifierItemId) {
        for (ModifierItem modifierItem : menuItem.getSizes().getModifierItems()) {
            modifierItem.getAttributes().setSelected(modifierItem.getId().equals(modifierItemId));
        }
        updateModifierVisibility();
        menuView.selectSize();
        getMenuItemTotalPrice(menuItem, -1);
    }

    @Override
    public void selectIngredient(boolean isSelected, Ingredient ingredient) {
        ingredient.getAttributes().setSelected(!isSelected);
        getMenuItemTotalPrice(menuItem, -1);
    }

    @Override
    public void selectModifierItem(boolean isSelected, int modifierGroupPosition, ModifierGroup modifierGroup, int modifierItemPosition, ModifierItem modifierItem) {
        if (modifierGroup.getAttributes().getMaximum() == 1) {
            if (modifierGroup.getAttributes().getMinimum() == 1) {
                isSelected = true;
            }
            ModifierGroup group = menuItem.getModifierGroups().get(modifierGroupPosition);
            for (ModifierItem item : group.getModifierItems()) {
                item.getAttributes().setSelected(isSelected && item.getId().equals(modifierItem.getId()));
            }
        } else {
            int selectedItems = 0;
            ModifierGroup group = menuItem.getModifierGroups().get(modifierGroupPosition);
            if (group != null) {
                for (ModifierItem item : group.getModifierItems()) {
                    if (item.getAttributes().isSelected()) {
                        selectedItems += item.getAttributes().getQuantity();
                    }
                }
                if (isSelected && selectedItems >= modifierGroup.getAttributes().getMaximum()) {
                    menuView.showMessage(CacheUtil.getInstance().getTranslations(Text.MODIFIER_MAXIMUM_LIMIT));
                } else {
                    ModifierItem item = group.getModifierItems().get(modifierItemPosition);
                    if (item.getId().equals(modifierItem.getId())) {
                        item.getAttributes().setSelected(isSelected);
                        if (!isSelected) {
                            item.getAttributes().setQuantity(1);
                        }
                    }
                }
            }
        }

        updateModifierVisibility();

        getMenuItemTotalPrice(menuItem, modifierGroupPosition);
    }

    @Override
    public void updateModifierVisibility() {
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
                            break;
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
                            for (ModifierGroup modGroup : menuItem.getModifierGroups()) {
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
                                    break;
                                }

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

    }

    @Override
    public void changeModifierItemQuantity(boolean add, int modifierGroupPosition, ModifierGroup modifierGroup, int modifierItemPosition, ModifierItem modifierItem) {
        if (!add) {
            ModifierGroup group = menuItem.getModifierGroups().get(modifierGroupPosition);
            //for (ModifierGroup group : menuItem.getModifierGroups()) {
            //if (group.getId().equals(modifierGroup.getId())) {
            ModifierItem item = group.getModifierItems().get(modifierItemPosition);
            //for (ModifierItem item : group.getModifierItems()) {
            //if (item.getId().equals(modifierItem.getId())) {
            if (modifierItem.getAttributes().getQuantity() == 1) {
                item.getAttributes().setSelected(false);
            } else {
                item.getAttributes().setQuantity(modifierItem.getAttributes().getQuantity() - 1);
            }
            //break;
            //}
            //}
            //break;
            //}
            //}
        } else {
            int selectedItems = 0;
            ModifierGroup group = menuItem.getModifierGroups().get(modifierGroupPosition);
            //for (ModifierGroup group : menuItem.getModifierGroups()) {
            //if (group.getId().equals(modifierGroup.getId())) {
            for (ModifierItem item : group.getModifierItems()) {
                if (item.getAttributes().isSelected()) {
                    selectedItems += item.getAttributes().getQuantity();
                }
            }
            //break;
            //}
            //}
            if (selectedItems >= modifierGroup.getAttributes().getMaximum()) {
                menuView.showMessage(CacheUtil.getInstance().getTranslations(Text.MODIFIER_MAXIMUM_LIMIT));
            } else {
                //for (ModifierGroup group : menuItem.getModifierGroups()) {
                //if (group.getId().equals(modifierGroup.getId())) {
                ModifierItem item = group.getModifierItems().get(modifierItemPosition);
                //for (ModifierItem item : group.getModifierItems()) {
                if (item.getId().equals(modifierItem.getId())) {
                    item.getAttributes().setQuantity(modifierItem.getAttributes().getQuantity() + 1);
                    //break;
                }
                //}
                //break;
                //}
                //}
            }
        }

        getMenuItemTotalPrice(menuItem, modifierGroupPosition);
    }

    @Override
    public void onBindSizeViewAtPosition(int position, SizeViewHolder sizeViewHolder) {
        sizeViewHolder.setModifierItem(0, menuItem.getSizes(), position, menuItem.getSizes().getModifierItems().get(position), true);
    }

    @Override
    public int getSizesCount() {
        return menuItem.getSizes().getModifierItems().size();
    }

    @Override
    public void onBindIngredientViewAtPosition(int position, IngredientViewHolder ingredientViewHolder) {
        ingredientViewHolder.setIngredient(menuItem.getIngredients().get(position));
    }

    @Override
    public int getIngredientsCount() {
        return menuItem.getIngredients().size();
    }

    @Override
    public void onBindModifierGroupViewAtPosition(int position, ModifierGroupViewHolder modifierGroupViewHolder, boolean showModifierItemPrice) {
       /* ModifierGroup modifierGroup = menuItemModifierGroups.get(menuItem.getModifierGroups().get(position).getId());
        if(modifierGroup != null) {*/
        modifierGroupViewHolder.setModifierGroup(position, menuItem.getModifierGroups().get(position), menuItem.getModifierGroups().get(position).getAttributes().getFreeModifiersCount() == 0 || showModifierItemPrice);
        //}
    }

    @Override
    public int getModifierGroupsCount() {
        return menuItem.getModifierGroups().size();
    }

    @Override
    public int getModifierGroupIndex(ModifierGroup modifierGroup) {

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

        for (ModifierGroup group : visibleModifierGroups) {
            if (group.getId().equalsIgnoreCase(modifierGroup.getId())) {
                return visibleModifierGroups.indexOf(group);
            }
        }

        return -1;
    }

    @Override
    public int getModifierGroupSelectedModifierItemsCount(ModifierGroup modifierGroup) {

        int selectedItems = 0;
        for (ModifierItem item : modifierGroup.getModifierItems()) {
            if (item.getAttributes().isSelected()) {
                selectedItems += item.getAttributes().getQuantity();
            }
        }

        return selectedItems;
    }

    @Override
    public void onBindModifierItemViewAtPositionNew(ModifierGroup modifierGroup, int position, ModifierItemViewHolderNew modifierItemViewHolder) {
        modifierItemViewHolder.setModifierItem(modifierGroup, modifierGroup.getModifierItems().get(position), position);
    }

    @Override
    public void onBindModifierItemViewAtPosition(int modifierGroupPosition, ModifierGroup modifierGroup, boolean showModifierItemPrice, int position, ModifierItemViewHolder modifierItemViewHolder) {
        modifierItemViewHolder.setModifierItem(modifierGroupPosition, modifierGroup, position, modifierGroup.getModifierItems().get(position), showModifierItemPrice);
    }

    @Override
    public int getModifierItemsCount(ModifierGroup modifierGroup) {
        if (modifierGroup != null) {
            return modifierGroup.getModifierItems().size();
        } else {
            int count = 0;
            for (ModifierGroup modifierGroup1 : menuItem.getModifierGroups()) {
                count += modifierGroup1.getModifierItems().size();
            }
            return count;
        }
    }

    @Override
    public void addRemoveFavorite(boolean add) {
        if (add || !CacheUtil.getInstance().getFavorites().contains(menuItem.getId())) {
            CustomersApiCall.getInstance().addItemToFavorite(AuthUtil.getInstance().getCustomer().getId(), AddFavoriteItemBody.getCdn(LocaleUtil.getInstance().getLanguage(), menuItem), new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {

                    MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_FAVORITE_ITEM);

                    getFavorites(true, 1);

                    Set<String> favorites = CacheUtil.getInstance().getFavorites();
                    favorites.add(menuItem.getId());
                    CacheUtil.getInstance().setFavorites(favorites);

                    HashMap<String, String> eventAttributes = new HashMap<>();
                    eventAttributes.put("Item ID", menuItem.getId());
                    eventAttributes.put("Item Name", menuItem.getAttributes().getName());

                    menuView.logEvent("AddToFavorites", eventAttributes, null, 0);
                    menuView.logAddToWishlistEvent(menuItem);

                    menuView.setFavorite(menuItem);

                    menuView.showMessage(CacheUtil.getInstance().getTranslations(Text.ITEM_ADDED_TO_FAVORITES));

                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    menuView.showMessage(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.ERROR_ADD_TO_FAVORITES), null));
                }
            });
        } else {
            CustomersApiCall.getInstance().deleteItemFromFavorites(AuthUtil.getInstance().getCustomer().getId(), menuItem.getId(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {

                    getFavorites(true, 1);

                    Set<String> favorites = CacheUtil.getInstance().getFavorites();
                    favorites.remove(menuItem.getId());
                    CacheUtil.getInstance().setFavorites(favorites);

                    menuView.setFavorite(menuItem);

                    menuView.showMessage(CacheUtil.getInstance().getTranslations(Text.ITEM_REMOVED_FROM_FAVORITES));

                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    menuView.showMessage(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.ERROR_REMOVE_FROM_FAVORITES), null));
                }
            });
        }
    }

    @Override
    public void getCartMenuItem() {

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
                menuView.showMessage(CacheUtil.getInstance().getTranslations(Text.MODIFIER_MINIMUM_LIMIT, "Minimum number of extras (x) not reached for (y)").replace("(x)", String.valueOf(modifierGroup.getAttributes().getMinimum())).replace("(y)", modifierGroup.getAttributes().getName()));
                invalid = true;
                break;
            }
        }

        if(!invalid) {
            invalid = checkComboModifiers();
        }

        if (!invalid) {
            menuItem.getAttributes().setNotes(itemNote);
            addCartItem(false, false);
        }
    }

    @Override
    public void addCartMenuItem() {

        if (menuCategory == null) {
            for (String menuCategoryId : CacheUtil.getInstance().getMenuCategories().keySet()) {
                if (CacheUtil.getInstance().getMenuCategories().get(menuCategoryId) != null && CacheUtil.getInstance().getMenuCategories().get(menuCategoryId).second.contains(menuItem.getId())) {
                    menuCategory = CacheUtil.getInstance().getMenuCategories().get(menuCategoryId).first;
                }
            }
        }

        if (menuItem.isMustBeCustomized() || menuItem.isCombo) {
            if (menuCategory == null) {
                for (MenuCategory category : menuCategories) {
                    for (MenuItem item : category.getMenuItems()) {
                        if (item.getId().equals(menuItem.getId())) {
                            menuCategory = category;
                            break;
                        }
                    }
                    if (menuCategory != null) {
                        break;
                    }
                }
            }
            menuView.selectMenuItem(menuCategory, menuItem);
        } else {
            getMenuItem(true, menuItem.getId());
        }
    }

    @Override
    public void addCartMenuItem(ImageView view, MenuItem menuItem, double price, int quantity) {
        currentImageViewToAnimate = view;
        addMenuItemToCartWithAnalytics(menuItem, price, quantity);
    }

    @Override
    public void getTimedEvent() {
        MenuApiCall.getInstance().callTimedEvents(CacheUtil.getInstance().getMenuId(), new ApiRequestListener() {

            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    JSONArray dataArray = object.getJSONArray("data");
                    if (dataArray.length() > 0) {
                        getConcept(TimedEvent.parse(dataArray.getJSONObject(0)));
                    } else {
                        addCartItem(true, false);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);

                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                addCartItem(true, false);
            }
        }, menuItem.getId());
    }

    @Override
    public void getConcept(TimedEvent timedEvent) {

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
                                        addCartItem(true, false);
                                    } else {
                                        menuView.showError(MenuUtil.getTimedEventError(timedEvent));
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
                menuView.showMessage(ErrorHandler.getErrorMessage(o, "", CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)));
            }
        });

    }

    @Override
    public void addCartItem(boolean add, boolean skip) {
        String orderStoreId = null;
        if (CacheUtil.getInstance().getOrderStore() != null) {
            orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
        }
        Store store = CacheUtil.getInstance().getOrderStore();

        if (store != null && menuItem.getDisabledStores().containsKey(store.getId())) {
            menuView.showError(CacheUtil.getInstance().getTranslations(Text.LOCATION_AVAILABLE).replace("(x)", store.getAttributes().getName()));
        } else {

            HashMap<String, String> eventAttributes = new HashMap<>();
            eventAttributes.put("Item Name", menuItem.getName(LocaleUtil.getInstance().getLanguage()));
            eventAttributes.put("Item Quantity", String.valueOf(menuItem.getAttributes().getQuantity()));

            double totalPrice = menuItem.getPrice(orderStoreId);

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
                    int selectedFreeModifier = 0;
                    for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                        if (modifierItem.getAttributes().isSelected()) {
                            eventAttributes.put("Modifier Name", modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
                            eventAttributes.put("Modifier Quantity", String.valueOf(modifierItem.getAttributes().getQuantity()));
                            if (modifierGroup.getAttributes().getFreeModifiersCount() > 0 && selectedFreeModifier < modifierGroup.getAttributes().getFreeModifiersCount()) {
                                selectedFreeModifier += modifierItem.getAttributes().getQuantity();
                            } else {
                                totalPrice += modifierItem.getAttributes().getPrice() * modifierItem.getAttributes().getQuantity();
                            }
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
            menuView.logEvent("AddToCart", eventAttributes, "Price", totalPrice);
            menuView.logFirebaseEvent("add_to_cart_event");
            int cartCount = CacheUtil.getInstance().getCartQuantity();
            menuItem.isSandwichOnly = false;
            removeMukafaaRedemption();
            CacheUtil.getInstance().addToCart(menuItem);
            if (cartCount == 0) {
                menuView.setCartExpiry();
            }
            if (currentImageViewToAnimate != null) {
                menuView.addCartItem(menuItem, currentImageViewToAnimate);
                currentImageViewToAnimate = null;
            } else {
                menuView.addCartItem(menuItem);
            }
        }

    }

    @Override
    public void addMenuItemToCart(String menuItemId) {
        AppDatabase appDatabase = AppDatabase.getInstance();
        String applicationId = CacheUtil.getInstance().getApplication().getId();
        MenuItemEntity menuItemEntity = appDatabase.menuItemEntityDao().findById(applicationId, menuItemId);
        if (menuItemEntity != null) {
            MenuCategoryEntity menuCategoryEntity = appDatabase.menuCategoryEntityDao().findById(applicationId, menuItemEntity.getMenuCategoryId());
            if (menuCategoryEntity != null) {
                menuCategory = MenuCategoryEntity.Companion.toDomain(menuCategoryEntity);
            }
            MenuItemDb menuItemDb = appDatabase.menuItemDao().findByApplicationAndId(applicationId, menuItemId);
            if (menuItemDb != null) {
                menuItem = GsonHelper.gsonFinal().fromJson(menuItemDb.data, MenuItem.class);
            }
        }

        if (menuCategory != null && menuItem != null) {
            addCartMenuItem();
        }
    }

    @Override
    public void addMenuItemToCartWithAnalytics(MenuItem item, double price, int quantity) {
        AppDatabase appDatabase = AppDatabase.getInstance();
        String applicationId = CacheUtil.getInstance().getApplication().getId();
        MenuItemEntity menuItemEntity = appDatabase.menuItemEntityDao().findById(applicationId, item.getId());
        if (menuItemEntity != null) {
            MenuCategoryEntity menuCategoryEntity = appDatabase.menuCategoryEntityDao().findById(applicationId, menuItemEntity.getMenuCategoryId());
            if (menuCategoryEntity != null) {
                menuCategory = MenuCategoryEntity.Companion.toDomain(menuCategoryEntity);
            }
            MenuItemDb menuItemDb = appDatabase.menuItemDao().findByApplicationAndId(applicationId, item.getId());
            if (menuItemDb != null) {
                menuItem = GsonHelper.gsonFinal().fromJson(menuItemDb.data, MenuItem.class);
            }
        }

        if (menuCategory != null && menuItem != null) {
            menuView.setMenuCategoryForAnalytics(menuItem, menuCategory, price, quantity);
            addCartMenuItem();
        }
    }

    @Override
    public void updateCartItem(boolean isPromo, int position) {

        boolean invalid = false;

        for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
            int selected = 0;
            for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                if (modifierItem.getAttributes().isSelected()) {
                    selected += modifierItem.getAttributes().getQuantity();
                }
            }
            if (selected < modifierGroup.getAttributes().getMinimum()) {
                menuView.showMessage(CacheUtil.getInstance().getTranslations(Text.MODIFIER_MINIMUM_LIMIT, "Minimum number of extras (x) not reached for (y)").replace("(x)", String.valueOf(modifierGroup.getAttributes().getMinimum())).replace("(y)", modifierGroup.getAttributes().getName()));
                invalid = true;
                break;
            }
        }

        if(!invalid) {
            invalid = checkComboModifiers();
        }

        if (!invalid) {
            menuItem.getAttributes().setNotes(itemNote);
            if(!isPromo) {
                CacheUtil.getInstance().updateCart(position, menuItem);
            } else {
                if(CacheUtil.getInstance().getCouponToAdd() != null) {
                    CouponToAdd couponToAdd = CacheUtil.getInstance().getCouponToAdd();
                    CacheUtil.getInstance().setCouponCode(couponToAdd.getCouponCode());
                    CacheUtil.getInstance().setCouponName(couponToAdd.getCouponName());
                    CacheUtil.getInstance().setCouponMenuItem(menuItem);
                    CacheUtil.getInstance().setCouponToAdd(null);
                } else if(CacheUtil.getInstance().getCampaignToAdd() != null) {
                    CampaignToAdd campaignToAdd = CacheUtil.getInstance().getCampaignToAdd();
                    CacheUtil.getInstance().setCampaign(campaignToAdd.getCampaign());
                    CacheUtil.getInstance().setCampaignPromotion(campaignToAdd.getPromotion());
                    CacheUtil.getInstance().setCampaignPromotionMenuItem(menuItem);
                    CacheUtil.getInstance().setCampaignToAdd(null);
                } else if(CacheUtil.getInstance().getCouponCode() != null &&
                        CacheUtil.getInstance().getCouponMenuItem() != null) {
                    CacheUtil.getInstance().setCouponMenuItem(menuItem);
                } else if(CacheUtil.getInstance().getCampaign() != null &&
                        CacheUtil.getInstance().getCampaignPromotionMenuItem() != null) {
                    CacheUtil.getInstance().setCampaignPromotionMenuItem(menuItem);
                }
            }
            menuView.updateCartItem();
        }

    }

    @Override
    public MenuCategory getMenuCategoryFromSearch(MenuItem menuItem) {
        List<MenuCategory> categories = excludedMenuCategories(menuCategories);
        int categoryId = menuItem.getAttributes().getCategoryId();
        if (categoryId != 0) {
            for (MenuCategory category : categories) {
                if (String.valueOf(categoryId).equals(category.getId())) {
                    return category;
                }
            }
        } else {
            for (MenuCategory category : categories) {
                for (MenuItem item : category.getMenuItems()) {
                    if (item.getId().equals(menuItem.getId())) {
                        return category;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public MenuCategory getMenuCategoryByPosition(int position) {
        if (!excludedMenu.isEmpty() && position < excludedMenu.size()) {
            return excludedMenu.get(position);
        } else if (!menuCategories.isEmpty()) {
            List<MenuCategory> excludedCategories = excludedMenuCategories(menuCategories);
            int size = excludedCategories.size();
            if(size != 0) {
                if (size > position) {
                    return excludedCategories.get(position);
                } else {
                    return excludedCategories.get(size - 1);
                }
            }
        }
        return null;
    }

    @Override
    public void showHideModifierItemPrice() {
        for (ModifierGroup modifierGroup : menuItem.getModifierGroups()) {
            VisibilityConfig visibilityConfig = modifierGroup.getAttributes().getVisibilityConfig();
            if (visibilityConfig == null || visibilityConfig.getInitialValue() == null || !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
                int modifierGroupPosition = menuItem.getModifierGroups().indexOf(modifierGroup);
                if (modifierGroup.getAttributes().getFreeModifiersCount() == 0) {
                    menuView.showHideModifierItemPrice(true, modifierGroupPosition);
                } else {
                    int selectedFreeModifier = 0;

                    for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                        if (modifierItem.getAttributes().isSelected()) {
                            if (modifierGroup.getAttributes().getFreeModifiersCount() > 0 && selectedFreeModifier < modifierGroup.getAttributes().getFreeModifiersCount()) {
                                int totalSelectedFreeModifier = selectedFreeModifier + modifierItem.getAttributes().getQuantity();
                                if (totalSelectedFreeModifier < modifierGroup.getAttributes().getFreeModifiersCount()) {
                                    if (modifierGroupPosition >= 0) {
                                        menuView.showHideModifierItemPrice(false, modifierGroupPosition);
                                    }
                                    selectedFreeModifier += modifierItem.getAttributes().getQuantity();
                                } else {
                                    if (modifierGroupPosition >= 0) {
                                        menuView.showHideModifierItemPrice(true, modifierGroupPosition);
                                    }
                                    selectedFreeModifier = modifierGroup.getAttributes().getFreeModifiersCount();
                                }
                            }
                        }
                    }

                    if (selectedFreeModifier == 0 && modifierGroup.getAttributes().getFreeModifiersCount() > 0 && modifierGroupPosition >= 0 && modifierGroupPosition == menuItem.getModifierGroups().indexOf(modifierGroup)) {
                        menuView.showHideModifierItemPrice(false, modifierGroupPosition);
                    }
                }
            }
        }
    }

    @Override
    public void getFavoriteItem(Favorite favorite) {
        MenuItem favoriteMenuItem = MenuUtil.getFavoriteMenuItem(favorite);

        menuView.showMenuItem(favorite.getCategoryId(), favoriteMenuItem.getId());
    }

    @Override
    public void getOrders() {
        Customer customer = AuthUtil.getInstance().getCustomer();
        CustomersApiCall.getInstance().getOrders(customer.getId(), "1", "10", new ApiRequestListener() {

            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    JSONArray array = object.getJSONArray("data");

                    orders = new ArrayList<>();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject orderObject = array.getJSONObject(i);
                        OrderHistory order = OrderHistory.parse(orderObject);
                        orders.add(order);
                    }

                    Collections.sort(orders, new Comparator<OrderHistory>() {
                        @Override
                        public int compare(OrderHistory o1, OrderHistory o2) {

                            try {

                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
                                Date date1 = simpleDateFormat.parse(o1.getCreatedAt());
                                Date date2 = simpleDateFormat.parse(o2.getCreatedAt());
                                if (newestToOldest) {
                                    return date2.compareTo(date1);
                                } else {
                                    return date1.compareTo(date2);
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            return 0;
                        }
                    });

                    menuView.showOrders(newestToOldest, orders);


                } catch (JSONException e) {
                    e.printStackTrace();
                    orders = new ArrayList<>();
                    menuView.showOrders(newestToOldest, orders);
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                orders = new ArrayList<>();
                menuView.showOrders(newestToOldest, orders);
            }
        });
    }

    @Override
    public int getOrdersCount() {
        return orders.size();
    }

    @Override
    public void selectOrder(OrderHistory order) {
        menuView.selectOrder(order);
    }


    @Override
    public double getMenuItemsTotalPrice() {
        double subtotalPrice = 0.0;
        for (MenuItem menuItem : CacheUtil.getInstance().getCart()) {
            subtotalPrice += getMenuItemTotalPrice(menuItem, -1) * menuItem.getAttributes().getQuantity();
        }
        return subtotalPrice;
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
        if (item.getSizes().getModifierItems().size() > 0) {
            if(!menuItem.getIsPromotion()) {
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

                if (modifierGroup.getAttributes().getFreeModifiersCount() == 0 && modifierGroupPosition >= 0 && modifierGroupPosition == menuItem.getModifierGroups().indexOf(modifierGroup)) {
                    menuView.showHideModifierItemPrice(true, modifierGroupPosition);
                }

                int selectedFreeModifier = 0;
                for (ModifierItem modifierItem : modifierGroup.getModifierItems()) {
                    if (modifierItem.getAttributes().isSelected()) {
                        if (modifierGroup.getAttributes().getFreeModifiersCount() > 0 && selectedFreeModifier < modifierGroup.getAttributes().getFreeModifiersCount()) {
                            int totalSelectedFreeModifier = selectedFreeModifier + modifierItem.getAttributes().getQuantity();
                            if (totalSelectedFreeModifier < modifierGroup.getAttributes().getFreeModifiersCount()) {
                                if (modifierGroup.getAttributes().getFreeModifiersCount() > 0 && modifierGroupPosition >= 0 && modifierGroupPosition == menuItem.getModifierGroups().indexOf(modifierGroup)) {
                                    menuView.showHideModifierItemPrice(false, modifierGroupPosition);
                                }
                                selectedFreeModifier += modifierItem.getAttributes().getQuantity();
                            } else {
                                if (modifierGroup.getAttributes().getFreeModifiersCount() > 0 && modifierGroupPosition >= 0 && modifierGroupPosition == menuItem.getModifierGroups().indexOf(modifierGroup)) {
                                    menuView.showHideModifierItemPrice(true, modifierGroupPosition);
                                }
                                selectedFreeModifier = modifierGroup.getAttributes().getFreeModifiersCount();
                                totalPrice += modifierItem.getAttributes().getPrice() * (totalSelectedFreeModifier - modifierGroup.getAttributes().getFreeModifiersCount());
                            }
                        } else {
                            totalPrice += modifierItem.getAttributes().getPrice() * modifierItem.getAttributes().getQuantity();
                        }
                    }
                }

                if (selectedFreeModifier == 0 && modifierGroup.getAttributes().getFreeModifiersCount() > 0 && modifierGroupPosition >= 0 && modifierGroupPosition == menuItem.getModifierGroups().indexOf(modifierGroup)) {
                    menuView.showHideModifierItemPrice(false, modifierGroupPosition);
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

        menuView.setMenuItemTotalPrice(totalPrice * item.getAttributes().getQuantity());
        return totalPrice;
    }

    @Override
    public void getGuestDigitalCoupons() {

        CouponsApiCall.getInstance().getGuestCampaigns(new ApiRequestListener() {
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

                    menuView.showLatestOffers(digitalCoupons);

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
    public void selectDigitalCoupon(int position) {
        if (position < digitalCoupons.size()) {
            menuView.showDigitalCoupon(digitalCoupons.get(position));
        }
    }

    @Override
    public void getBestSeller() {
        MenuApiCall.getInstance().bestSellers(CacheUtil.getInstance().getMenuId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    JSONArray array = object.getJSONArray("data");
                    bestSellers.clear();
                    bestSellers = GsonHelper.gsonFinal().fromJson(array.toString(), new TypeToken<ArrayList<MenuItem>>() {
                    }.getType());
                    excludedPopularItem();
                    if (CacheUtil.getOrderStoreId() == null) {
                        menuView.showBestSellers(bestSellers);
                    } else {
                        menuView.showBestSellers(excludedPopularItem);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
            }
        });
    }

    @Override
    public ArrayList<MenuItem> getBestSellers() {
        if (CacheUtil.getOrderStoreId() != null) {
            return excludedPopularItem();
        } else return bestSellers;
    }

    @Override
    public List<Campaign> getLatestOffersList() {
        return digitalCoupons;
    }

    @Override
    public int bestSellersCount() {
        if (CacheUtil.getOrderStoreId() != null) {
            return excludedPopularItem().size();
        } else return bestSellers.size();
    }

    @Override
    public void getLatestOffers() {
        CouponsApiCall.getInstance().getGuestCampaigns(new ApiRequestListener() {
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
                            // if (digitalCoupon.getAttributes().isEligible()) {
                            if (digitalCoupon.getPromotion() != null && digitalCoupon.getPromotion().getAttributes().getType().equalsIgnoreCase("delivery")) {
                                if (CacheUtil.getInstance().getOrderType() == OrderType.DELIVERY) {
                                    filteredDigitalCoupons.add(digitalCoupon);
                                }
                            } else {
                                filteredDigitalCoupons.add(digitalCoupon);
                            }
                            //}
                        }

                        digitalCoupons.clear();
                        digitalCoupons.addAll(filteredDigitalCoupons);
                        menuView.showLatestOffers(digitalCoupons);
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
    public int latestOffersCount() {
        return digitalCoupons.size();
    }

    @Override
    public void getRecentOrder() {
        ReportsApiCall.getInstance().previouslyOrdered(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    JSONArray array = object.getJSONArray("data");
                    recentOrders.clear();
                    recentOrders = GsonHelper.gsonFinal().fromJson(array.toString(), new TypeToken<ArrayList<MenuItem>>() {
                    }.getType());
                    excludedRecentOrder();
                    if (CacheUtil.getOrderStoreId() == null) {
                        menuView.showRecentOrders(recentOrders);
                    } else {
                        menuView.showRecentOrders(excludedRecentOrder);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
            }
        });
    }

    @Override
    public ArrayList<MenuItem> getRecentOrders() {
        if (CacheUtil.getOrderStoreId() != null) {
            return excludedRecentOrder();
        } else return recentOrders;
    }

    @Override
    public int recentOrdersCount() {
        if (CacheUtil.getOrderStoreId() != null) {
            return excludedRecentOrder().size();
        } else return recentOrders.size();
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
    public void setFromCartEdit(boolean isFromCartEdit) {
        this.isFromCartEdit = isFromCartEdit;
    }


    @Override
    public void setPreselectedBranch() {
        ApplicationAttributes applicationAttributes = CacheUtil.getInstance().getApplication().getAttributes();
        if (applicationAttributes.getForceLocation() == 0 &&
                applicationAttributes.getOrderTypes().size() == 1 &&
                !applicationAttributes.getOrderTypes().contains(OrderType.DELIVERY)) {

            LocationApiCall.getInstance().getStoreLocations("1", new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    try {
                        JSONObject object = new JSONObject(o.toString());
                        JSONArray array = object.getJSONArray("data");
                        if (array.length() == 1) {
                            Store store = Store.parseStore(array.getJSONObject(0));
                            if (object.has("included") && !object.isNull("included")) {
                                ArrayList<Facility> facilities = GsonHelper.gsonFinal().fromJson(object.getJSONArray("included").toString(), new TypeToken<ArrayList<Facility>>() {
                                }.getType());
                                for (Facility facility : facilities) {
                                    if (store.getFacilitiesIds().contains(facility.getId())) {
                                        List<Facility> items = new ArrayList<>(store.getFacilities());
                                        items.add(facility);
                                        store.setFacilities(items);
                                        break;
                                    }
                                }
                            }
                            OrderType preselectedOrderType = CacheUtil.getInstance().getApplication().getAttributes().getOrderTypes().iterator().next();
                            if (preselectedOrderType.equals(OrderType.PICKUP) && store.getAttributes().getPickupEnabled() == 1) {
                                CacheUtil.getInstance().setOrderStore(store);
                                CacheUtil.getInstance().setOrderType(preselectedOrderType);
                                isStoreOrderTypePreselected = true;
                                CacheUtil.getInstance().setPreselectedBranch(true);
                                menuView.updateOrderTypeContainer(true);
                            } else if (preselectedOrderType.equals(OrderType.DINE_IN)) {
                                CacheUtil.getInstance().setOrderStore(store);
                                CacheUtil.getInstance().setOrderType(preselectedOrderType);
                                isStoreOrderTypePreselected = true;
                                CacheUtil.getInstance().setPreselectedBranch(true);
                                menuView.updateOrderTypeContainer(true);
                            } else {
                                isStoreOrderTypePreselected = false;
                                CacheUtil.getInstance().setPreselectedBranch(false);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                }
            });
        } else {
            isStoreOrderTypePreselected = false;
            CacheUtil.getInstance().setPreselectedBranch(false);
            if (applicationAttributes.getForceLocation() == 1) {
                CacheUtil.getInstance().setOrderStore(null);
                CacheUtil.getInstance().setOrderType(OrderType.NONE);
            }
        }

    }

    @Override
    public void setItemNote(String note) {
        itemNote = note;
    }

    @Override
    public boolean isPreselectedBranch() {
        return isStoreOrderTypePreselected;
    }

}
