package com.herfy.menu.viewholders;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.menu.adapters.ModifierItemsNewRecyclerViewAdapter;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.Favorite;
import com.skylinedynamics.solosdk.api.models.objects.Ingredient;
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SizeViewHolder extends RecyclerView.ViewHolder implements MenuContract.View {

    private Context context;

    private MenuContract.Presenter menuPresenter;

    //@BindView(R.id.card) MaterialCardView card;

    @BindView(R.id.container) ConstraintLayout container;

    @BindView(R.id.check) CheckBox check;

    @BindView(R.id.name) TextView name;

    @BindView(R.id.price) TextView price;

    private SizeViewHolder.OnAction onAction;

    public interface OnAction {
        void onCheckedChanged(ModifierItem modifierItem);
    }

    public void setOnAction(SizeViewHolder.OnAction onAction) {
        this.onAction = onAction;
    }

    public SizeViewHolder(Context context, MenuContract.Presenter menuPresenter, View itemView) {

        super(itemView);

        ButterKnife.bind(this, itemView);

        this.context = context;
        this.menuPresenter = menuPresenter;

        setupViews();
        setupFonts();
        setupTranslations();

    }

    @Override
    public void setPresenter(MenuContract.Presenter presenter) {

    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {

    }

    @Override
    public void setCartExpiry() {

    }

    @Override
    public void setupViews() {



    }

    public void setupFonts() {

        //name.setTypeface(FontHandler.getSemiboldTypeface());

    }

    @Override
    public void setupTranslations() {

    }

    @Override
    public void showStore(Store store) {

    }
    @Override
    public void navigateToMenuItemDetails(MenuCategory menuCategory, MenuItem menuItem) {

    }

    @Override
    public void showStoreOpen(boolean open, String message) {

    }

    @Override
    public void showAddress(String address) {

    }

    @Override
    public void reloadMenu() {

    }

    @Override
    public void showMenuCategories(boolean refresh, boolean isFromInitialLoad, List<MenuCategory> menuCategories) {

    }

    @Override
    public void selectMenuCategory(int position, MenuCategory menuCategory) {

    }

    @Override
    public void selectMenuItem(@Nullable MenuCategory menuCategory, @Nullable MenuItem menuItem) {

    }

    @Override
    public void setMenuCategory(MenuCategory menuCategory, boolean selected) {

    }

    @Override
    public void setMenuItemDetails(MenuItem menuItem) {

    }

    @Override
    public void setMenuItem(MenuCategory menuCategory, MenuItem menuItem) {

    }

    @Override
    public void setFavorite(MenuItem menuItem) {

    }

    @Override
    public void showSearchedMenuItems(ArrayList<MenuItem> searchedMenuItems) {

    }

    @Override
    public void hideSearchedMenuItems(MenuCategory menuCategory) {

    }

    @Override
    public void setSizes(MenuItem menuItem) {

    }

    @Override
    public void setModifierItem(ModifierGroup modifierGroup, ModifierItem modifierItem, int position) {

    }
    public void showFrequentBoughtItems(List<MenuItem> frequentBoughtItems) {

    }
    @Override
    public void setModifierItem(int modifierGroupPosition, ModifierGroup modifierGroup, int modifierItemPosition, ModifierItem modifierItem, boolean showModifierItemPrice) {

        check.setChecked(modifierItem.getAttributes().isSelected());

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!modifierItem.getAttributes().isSelected()) {
                    check.toggle();
                    if(onAction != null) {
                        onAction.onCheckedChanged(modifierItem);
                    }
                }


                //check.toggle();
                //menuPresenter.selectSize(modifierItem.getId());
            }
        });

        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //menuPresenter.selectSize(modifierItem.getId());
                if(!modifierItem.getAttributes().isSelected()) {
                    check.toggle();
                    if(onAction != null) {
                        onAction.onCheckedChanged(modifierItem);
                    }
                }
            }
        });

        /*if (modifierItem.getAttributes().isSelected()) {
            card.setStrokeColor(ViewUtil.getColorMain(context));
            card.setStrokeWidth(ViewUtil.dpToPx(context, 2));
        } else {
            card.setStrokeColor(ContextCompat.getColor(context, android.R.color.transparent));
            card.setStrokeWidth(0);
        }*/

        name.setText(modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));
        price.setText(StringUtil.getFormattedPrice(modifierItem.getAttributes().getPrice()));
        /*container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menuPresenter.selectSize(modifierItem.getId());
            }
        });*/
    }

    @Override
    public void setIngredient(Ingredient ingredient) {

    }

    @Override
    public void setModifierGroup(int position, ModifierGroup modifierGroup, boolean showModifierItemPrice, boolean showShowModifierCheckBox, HashMap<Integer, ModifierItemsNewRecyclerViewAdapter> modifierItemsRecyclerViewAdapterNewHashMap) {

    }

    @Override
    public void setMenuItemQuantity(int quantity) {

    }

    @Override
    public void setMenuItemTotalPrice(double totalPrice, double menuItemPrice) {

    }

    @Override
    public void selectSize() {

    }

    @Override
    public void showOrderType() {

    }

    @Override
    public void addCartItem(MenuItem menuItem, boolean isShouldBackPressed, int menuAddedToCartPosition) {

    }

    @Override
    public void addCartItem(MenuItem menuItem, ImageView view, boolean isShouldBackPressed) {

    }

    @Override
    public void updateCartItem() {

    }

    @Override
    public void showMessage(String message) {

    }

    @Override
    public void updateFavoriteByPosition(int position, MenuItem menuItem) {

    }

    @Override
    public void showError(String error) {

    }

    @Override
    public void showHideModifierItemPrice(boolean show, int modifierGroupPosition) {

    }

    @Override
    public void showModifierSelectionIndicator(boolean hasAtleastOneSelectedItem, int modifierGroupPosition, HashMap<String, Integer> modifierGroupIndexVisibility) {

    }
    @Override
    public void showFavorites(boolean fromAddRemove, ArrayList<Favorite> favorites) {

    }

    @Override
    public void setFavorite(Favorite favorite, ArrayList<String> outOfStockLocations, double discount, double specialPrice) {

    }

    @Override
    public void showMenuItem(String categoryId, String menuItemId) {

    }

    @Override
    public void showOrders(boolean newestToOldest, ArrayList<OrderHistory> orders) {

    }

    @Override
    public void setOrder(OrderHistory order) {

    }

    @Override
    public void selectOrder(OrderHistory order) {

    }

    @Override
    public void showBestSellers(ArrayList<MenuItem> menuCategories) {

    }

    @Override
    public void showLatestOffers(List<Campaign> latestOffers) {

    }

    @Override
    public void showDigitalCoupon(Campaign digitalCoupon) {

    }

    @Override
    public void setCoupon(Campaign campaign) {

    }

    @Override
    public void setBestSeller(MenuItem menuItem, ArrayList<String> outOfStockLocation, double discount, double specialPrice) {

    }

    @Override
    public void setRecentOrder(MenuItem menuItem, ArrayList<String> outOfStockLocation, double discount, double specialPrice) {

    }

    @Override
    public void logFirebaseEvent(String event) {

    }

    @Override
    public void saveToCache(String key, String value) {

    }

    @Override
    public String getFromCache(String key) {
        return "";
    }

    @Override
    public void showLoader() {

    }

    @Override
    public void updateComponents(int componentPosition, List<ComponentModifierItem> componentModifierItems, int previous, int selected) {

    }

    @Override
    public void updateComponentModifierGroups(int componentPosition, List<ComponentModifierItem> componentModifierItems, int selected) {

    }

    @Override
    public void updateOrderTypeContainer(boolean isPreselected) {

    }

    @Override
    public void logAddToWishlistEvent(MenuItem menuItem) {

    }

    @Override
    public void setOriginalMenuItemValue(String value) {

    }

    @Override
    public void showRecentOrders(ArrayList<MenuItem> menuItems) {

    }

    @Override
    public void setMenuCategoryForAnalytics(MenuItem menuItem, MenuCategory menuCategory, double price, int quantity) {

    }

    @Override
    public void updateFloatingTotalPrice() {

    }

}