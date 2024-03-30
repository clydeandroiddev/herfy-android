package com.herfy.menu;

import android.widget.ImageView;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.herfy.menu.viewholders.BaseModifierItemViewHolder;
import com.herfy.menu.viewholders.FrequentBoughtViewHolder;
import com.herfy.menu.viewholders.IngredientViewHolder;
import com.herfy.menu.viewholders.MenuCategoryViewHolder;
import com.herfy.menu.viewholders.MenuItemViewHolder;
import com.herfy.menu.viewholders.ModifierItemViewHolder;
import com.herfy.menu.viewholders.ModifierNewGroupViewHolder;
import com.herfy.menu.viewholders.ModifierItemViewHolderNew;
import com.herfy.menu.viewholders.SizeViewHolder;
import com.herfy.newmenu.viewholder.MenuCouponViewHolder;
import com.herfy.newmenu.viewholder.MenuFavoriteViewHolder;
import com.herfy.newmenu.viewholder.MenuPopularViewHolder;
import com.herfy.newmenu.viewholder.MenuRecentViewHolder;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.Favorite;
import com.skylinedynamics.solosdk.api.models.objects.Ingredient;
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory;
import com.skylinedynamics.solosdk.api.models.objects.MenuCategoryItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupType;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.skylinedynamics.solosdk.api.models.objects.TimedEvent;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

public interface MenuContract {

    interface View extends BaseView<Presenter> {

        void showStore(Store store);

        void navigateToMenuItemDetails(MenuCategory menuCategory, MenuItem menuItem);

        void showStoreOpen(boolean open, String message);
        void showFrequentBoughtItems(List<MenuItem> frequentBoughtItems);
        void showAddress(String address);

        void reloadMenu();

        void showMenuCategories(boolean refresh, boolean isFromInitialLoad, List<MenuCategory> menuCategories);

        void selectMenuCategory(int position, MenuCategory menuCategory);

        void selectMenuItem(@Nullable MenuCategory menuCategory, @Nullable MenuItem menuItem);

        void setMenuCategory(MenuCategory menuCategory, boolean selected);

        void setMenuItemDetails(MenuItem menuItem);

        void setMenuItem(MenuCategory menuCategory, MenuItem menuItem);


        void setFavorite(MenuItem menuItem);

        void showSearchedMenuItems(ArrayList<MenuItem> searchedMenuItems);

        void hideSearchedMenuItems(MenuCategory menuCategory);

        void setSizes(MenuItem menuItem);

        void setModifierItem(ModifierGroup modifierGroup, ModifierItem modifierItem, int position);

        void setModifierItem(int modifierGroupPosition, ModifierGroup modifierGroup, int modifierItemPosition, ModifierItem modifierItem, boolean showModifierItemPrice);

        void setIngredient(Ingredient ingredient);

        void setModifierGroup(int position, ModifierGroup modifierGroup, boolean showModifierItemPrice, boolean showShowModifierCheckBox);

        void setMenuItemQuantity(int quantity);

        void setMenuItemTotalPrice(double totalPrice,double menuItemPrice);

        void selectSize();

        void showOrderType();

        void addCartItem(MenuItem menuItem, boolean isShouldBackPressed);

        void addCartItem(MenuItem menuItem, ImageView view, boolean isShouldBackPressed);

        void updateCartItem();

        void showMessage(String message);

        void updateFavoriteByPosition(int position,MenuItem menuItem);

        void showError(String error);

        void showHideModifierItemPrice(boolean show, int modifierGroupPosition);
        void showModifierSelectionIndicator(boolean hasAtleastOneSelectedItem, int modifierGroupPosition);

        void showFavorites(boolean fromAddRemove, ArrayList<Favorite> favorites);

        void setFavorite(Favorite favorite, ArrayList<String> outOfStockLocations, double discount, double specialPrice);

        void showMenuItem(String categoryId, String menuItemId);

        void showOrders(boolean newestToOldest, ArrayList<OrderHistory> orders);

        void setOrder(OrderHistory order);

        void selectOrder(OrderHistory order);

        void showBestSellers(ArrayList<MenuItem> menuItems);

        void showRecentOrders(ArrayList<MenuItem> menuItems);

        void showLatestOffers(List<Campaign> latestOffers);

        void showDigitalCoupon(Campaign digitalCoupon);

        void setCoupon(Campaign campaign);

        void setBestSeller(MenuItem menuItem, ArrayList<String> outOfStockLocations, double discount, double specialPrice);

        void setRecentOrder(MenuItem menuItem, ArrayList<String> outOfStockLocations, double discount, double specialPrice);

        void logFirebaseEvent(String event);

        void saveToCache(String key, String value);

        @Nullable String getFromCache(String key);

        void showLoader();

        void updateComponents(int componentPosition, List<ComponentModifierItem> componentModifierItems, int previous, int selected);

        void updateComponentModifierGroups(int componentPosition, List<ComponentModifierItem> componentModifierItems, int selected);

        void updateOrderTypeContainer(boolean isPreselected);

        void logAddToWishlistEvent(MenuItem menuItem);

        void setOriginalMenuItemValue(String value);

        void setMenuCategoryForAnalytics(MenuItem menuItem, MenuCategory menuCategory, double price, int quantity);

        void updateFloatingTotalPrice();

    }

    interface Presenter extends BasePresenter {
        public void updateModifierGroupSelectionIndicator(Boolean hasSelectedModifierItem, int modifierGroupPosition);
        MenuItem getMenuItemSingle(String id);

        void getFavorites(boolean fromAddRemove, int page);

        ArrayList<MenuItemComponents> componentsSelectFirst(ArrayList<MenuItemComponents> components);

        void resetMenuItem();

        int modifierItemQuantityCount(List<ModifierItem> modifiers);

        void updateComponentsFromYourWay(ModifierItem modifierItem, int position, int selectedModifierIndex, int getIndex,  boolean isAdd);

        void updateComponentsFromYourWayQuantityCheck(ModifierGroup modifierGroup, ModifierItem modifierItem, int position, int selectedModifierIndex, int getIndex,  boolean isAdd);

        void updateComponentsFromYourWayQuantity(ModifierItem modifierItem, int position, int selectedModifierIndex, int getIndex,  boolean isAdd, int quantity);

        boolean isAboveMax(ModifierGroup group, int count);

        void refresh();

        void assignComboIndex(int comboIndex);

        String selectedSizeId();

        MenuItem correctSizeModifier(String id);

        ArrayList<MenuItemComponents> filterComponents(ArrayList<MenuItemComponents> components);

        void setSelectedSizeId(String selectedSizeId);

        void assignMenuItem(MenuItem item);

        void updateComponents(ComponentModifierItem componentModifierItem, int componentsPosition, int itemPosition);

        void onBindFavoriteViewAtPosition(int position, MenuFavoriteViewHolder favoriteViewHolder);

        void onBindPopularViewAtPosition(int position, MenuPopularViewHolder popularViewHolder);

        void onBindDigitalCouponsViewAtPosition(int position, MenuCouponViewHolder couponViewHolder);

        void onBindFrequentBoughtItemPosition(int position, FrequentBoughtViewHolder viewHolder);

        void onBindRecentOrderViewAtPosition(int position, MenuRecentViewHolder orderViewHolder);

        int getFavoritesCount();

        ArrayList<Favorite> getFavorites();

        void getStore();

        void getStoreOpen();

        void getAddress();

        void getMenuCdn(boolean fromForceLocation, String menuCategoryId, String menuItemId);

        void parseMenu(String menuCategoryId, String menuItemId, JSONObject data);

        void prepareMenu(String menuCategoryId, String menuItemId, List<MenuCategoryItem> menuCategoryItems);

        List<MenuCategory> getMenuCategories();

        void getMenuCategories(boolean fromForceLocation, boolean fromHomeScreen, boolean fromDeeplink, boolean forceUseCache, boolean isFromInitialLoad,  String menuCategoryId, String menuItemId);

        void getMenuCategoryItems(int position, String categoryId);

        void refreshMenuCategories();

        void refreshMenuCategoryItems(int position, String categoryId);

        void reloadMenu();

        void onBindMenuCategoryViewAtPosition(int position, MenuCategoryViewHolder menuCategoryViewHolder, boolean selected);

        int getMenuCategoriesCount();
        int getFrequentBoughtItemCount();

        int getSearchItemsSize();

        void selectMenuCategory(int position, MenuCategory meuCategory);

        MenuCategory getMenuCategory();

        void showMenuItem(String menuItemId);

        void setMenuCategory(MenuCategory menuCategory);

        void setMenuCategory(String menuCategoryId);

        void setMenuCategory(MenuItem menuItem);

        void selectMenuCategory(String menuCategoryId);

        MenuItem getMenuItem();

        void getMenuItem(boolean addToBag, String menuItemId);

        void getMenuItem(boolean addToBag, String menuItemId, boolean isUpsell);

        void prepareMenuItem(boolean addToBag);

        void setMenuItem(MenuItem menuItem);

        void onBindMenuItemViewAtPosition(MenuCategory menuCategory, int position, MenuItemViewHolder menuItemViewHolder);

        int getMenuItemsCount(MenuCategory menuCategory);


        void selectMenuItem(MenuCategory menuCategory, MenuItem menuItem);

        void navigateToMenuItemDetails(MenuItem menuItem);

        void clearSearchedMenuItems();

        void hideSearchedMenuItems();

        void search(String query);

        void changeMenuItemQuantity(boolean add);

        void getModifiers();

        void getIngredients();

        void selectSize(String modifierItemId);

        void selectIngredient(boolean isSelected, Ingredient ingredient);

        void selectModifierItem(boolean isSelected, int modifierGroupPosition, ModifierGroup modifierGroup, int modifierItemPosition, ModifierItem modifierItem);

        void updateModifierVisibility();

        void changeModifierItemQuantity(boolean add, int modifierGroupPosition, ModifierGroup modifierGroup, int modifierItemPosition, ModifierItem modifierItem, boolean isQuantityVisible, Integer clickCount);

        void onBindSizeViewAtPosition(int position, SizeViewHolder sizeViewHolder);

        int getSizesCount();

        void onBindIngredientViewAtPosition(int position, IngredientViewHolder ingredientViewHolder);

        int getIngredientsCount();

        void onBindModifierGroupViewAtPosition(int position, ModifierNewGroupViewHolder modifierGroupViewHolder, boolean showModifierItemPrice, Boolean showShowModifierCheckBox, HashMap<Integer, Boolean> shouldShowQuantity);

        int getModifierGroupsCount();

        List<ModifierGroup> getModifierGroupList();

        int getModifierGroupIndex(ModifierGroup modifierGroup);

        int getModifierGroupSelectedModifierItemsCount(ModifierGroup modifierGroup);

        void onBindModifierItemViewAtPositionNew(ModifierGroup modifierGroup, int position, ModifierItemViewHolderNew modifierItemViewHolderNew);

        void onBindModifierItemViewAtPosition(int modifierGroupPosition, ModifierGroup modifierGroup, boolean showModifierItemPrice, int position, BaseModifierItemViewHolder modifierItemViewHolder);
        void onBindModifierItemViewAtPositionOld(int modifierGroupPosition, ModifierGroup modifierGroup, boolean showModifierItemPrice, int position, ModifierItemViewHolder modifierItemViewHolder);
        ModifierGroupType getModifierGroupTypeByPosition(int position, ModifierGroup modifierGroup);
        int getModifierItemsCount(ModifierGroup modifierGroup);

        void addRemoveFavorite(boolean add);

        void getCartMenuItem();

        void addCartMenuItem();

        void addCartMenuItem(ImageView view, MenuItem menuItem, double price, int quantity);

        void getTimedEvent();

        void getConcept(TimedEvent timedEvent);

        void addCartItem(boolean add, boolean skip, boolean isFrequentAddToCart);

        void addMenuItemToCart(String menuItemId);

        void addMenuItemToCartWithAnalytics(MenuItem menuItem, double price, int quantity);

        void updateCartItem(boolean isPromo, int position);

        MenuCategory getMenuCategoryFromSearch(MenuItem menuItem);

        MenuCategory getMenuCategoryByPosition(int position);

        void showHideModifierItemPrice();

        void getFavoriteItem(Favorite favorite);

        void getOrders();

        int getOrdersCount();

        void selectOrder(OrderHistory order);

        double getMenuItemsTotalPrice();

        double getMenuItemTotalPrice(MenuItem item, int modifierGroupPosition);

        void getGuestDigitalCoupons();

        void getFrequentBoughtItemsByMenuId(int page,String menuId);

        void selectDigitalCoupon(int position);

        List<Campaign> getLatestOffersList();

        void getBestSeller();

        ArrayList<MenuItem> getBestSellers();

        void getRecentOrder();

        ArrayList<MenuItem> getRecentOrders();

        int recentOrdersCount();

        int bestSellersCount();

        void getLatestOffers();

        int latestOffersCount();

        void removeMukafaaRedemption();

        void setFromCartEdit(boolean isFromCartEdit);

        void setPreselectedBranch();

        void setItemNote(String note);

        boolean isPreselectedBranch();

        void updateCart(boolean forceUpdate);

        void toggleFavorites(boolean isAddToFavorite, MenuItem menuItem, int position);


    }

}
