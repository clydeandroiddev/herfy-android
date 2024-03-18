package com.herfy.menu.viewholders;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.herfy.R;
import com.herfy.util.StringUtil;
import com.herfy.utils.constants.*;
import com.herfy.menu.MenuContract;
import com.herfy.menu.adapters.ModifierItemsNewRecyclerViewAdapter;
import com.herfy.utils.helper.CacheUtil;
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
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ModifierNewGroupViewHolder extends RecyclerView.ViewHolder implements MenuContract.View {

    private Context context;

    private MenuContract.Presenter menuPresenter;



    @BindView(R.id.name) TextView name;
    @BindView(R.id.modifierImage)  ImageView modifierImage;

    @BindView(R.id.select) TextView select; // todo uncomment this one ui for select is now final

    @BindView(R.id.recycler_view) RecyclerView recyclerView;
    @BindView(R.id.checkBox) ImageView modifierCheckBox;
    @BindView(R.id.btnMinimum)
    CardView requiredCardView;

    @BindView(R.id.minimum)
    TextView txtMinimum;


    private BaseModifierItemViewHolder.OnAction onAction;

    public void setOnAction(BaseModifierItemViewHolder.OnAction onAction) {
        this.onAction = onAction;
    }

    public ModifierNewGroupViewHolder(Context context, MenuContract.Presenter menuPresenter, View itemView) {

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

    @Override
    public void setModifierItem(int modifierGroupPosition, ModifierGroup modifierGroup, int modifierItemPosition, ModifierItem modifierItem, boolean showModifierItemPrice) {

    }


    @Override
    public void setIngredient(Ingredient ingredient) {

    }

    @Override
    public void setModifierGroup(int position, ModifierGroup modifierGroup, boolean showModifierItemPrice, boolean showShowModifierCheckBox) {
        String imageUrl = modifierGroup.getAttributes().getImage();
        boolean visible = false;
        VisibilityConfig visibilityConfig = modifierGroup.getAttributes().getVisibilityConfig();
        if (visibilityConfig != null) {
            if (visibilityConfig.getInitialValue() != null && !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
                visible = true;
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.GONE);
            }
        } else {
            visible = true;
            recyclerView.setVisibility(View.VISIBLE);
        }

        name.setText(modifierGroup.getAttributes().getName());
        if (imageUrl != null && !imageUrl.equalsIgnoreCase("null") && !imageUrl.isEmpty() && !imageUrl.toLowerCase().contains("default-image.png")) {
            Glide.with(context).load(imageUrl).apply(RequestOptions.centerCropTransform()).placeholder(R.drawable.imagepicker_image_placeholder).diskCacheStrategy(DiskCacheStrategy.ALL).into(modifierImage);
        }
        boolean isModifierGroupRequired  = modifierGroup.getAttributes().getMinimum() >= 1;
        requiredCardView.setVisibility(isModifierGroupRequired ? View.VISIBLE : View.GONE);
        modifierCheckBox.setVisibility(showShowModifierCheckBox ? View.VISIBLE : View.GONE);
        txtMinimum.setText(CacheUtil.getInstance().getTranslations(Text.REQUIRED,"Required"));

        if (visible) {

            int minimum = modifierGroup.getAttributes().getMinimum();
            int maximum = modifierGroup.getAttributes().getMaximum();
            if (minimum > 0) {
                if(maximum > minimum) {
                    select.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_ATLEAST, "(Select at least (x))").replace("(x)", StringUtil.localize(String.valueOf(minimum))));
                    select.setVisibility(View.VISIBLE);
                } else if(maximum == minimum) {
                    select.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_X, "(Select (x))").replace("(x)", StringUtil.localize(String.valueOf(maximum))));
                    select.setVisibility(View.VISIBLE);
                } else {
                    select.setVisibility(View.INVISIBLE);
                }
            } else if (minimum == 0) {
                if(maximum > 0) {
                    select.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_UP_TO_X, "(Select up to (x))").replace("(x)", StringUtil.localize(String.valueOf(maximum))));
                    select.setVisibility(View.VISIBLE);
                } else {
                    select.setVisibility(View.INVISIBLE);
                }
            } else {
                select.setVisibility(View.INVISIBLE);
            }

            //hamburgini hide free modifiers text

//                if(modifierGroup.getFreeModifiersCount() > 0) {
//                    free.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_MODIFIERS_FOR_FREE, "Select (x) modifiers for free").replace("(x)", StringUtil.localize(String.valueOf(modifierGroup.getFreeModifiersCount()))));
//                    free.setVisibility(View.VISIBLE);
//                } else {
//                    free.setVisibility(View.GONE);
//                }
//                if(modifierGroup.getAttributes().getFreeModifiersCount() > 0) {
//                    free.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_MODIFIERS_FOR_FREE, "Select (x) modifiers for free").replace("(x)", StringUtil.localize(String.valueOf(modifierGroup.getAttributes().getFreeModifiersCount()))));
//                    free.setVisibility(View.VISIBLE);
//                } else {
//                    free.setVisibility(View.GONE);
//                }
//            }


            //hamburgini

//            select.setVisibility(View.INVISIBLE);
//            String selectString = "";
//            int minimum = modifierGroup.getAttributes().getMinimum();
//            int maximum = modifierGroup.getAttributes().getMaximum();
//            if (minimum > 0) {
//                if (maximum > minimum) {
//                    selectString = CacheUtil.getInstance().getTranslations(Text.SELECT_ATLEAST, "(Select at least (x))").replace("(x)", StringUtil.localize(String.valueOf(minimum)));
//                } else if (maximum == minimum) {
//                    selectString = CacheUtil.getInstance().getTranslations(Text.SELECT_X, "(Select (x))").replace("(x)", StringUtil.localize(String.valueOf(maximum)));
//
//                } else {
//                    selectString = "";
//                }
//            } else if (minimum == 0) {
//                if (maximum > 0) {
//                    selectString = CacheUtil.getInstance().getTranslations(Text.SELECT_UP_TO_X, "(Select up to (x))").replace("(x)", StringUtil.localize(String.valueOf(maximum)));
//                } else {
//                    selectString = "";
//                }
//            } else {
//                selectString = "";
//            }




            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setNestedScrollingEnabled(false);
            ModifierItemsNewRecyclerViewAdapter modifierItemsNewRecyclerViewAdapter = new ModifierItemsNewRecyclerViewAdapter(context, menuPresenter, position, modifierGroup, showModifierItemPrice);
            modifierItemsNewRecyclerViewAdapter.setOnAction(onAction);
            recyclerView.setAdapter(modifierItemsNewRecyclerViewAdapter);
        }
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
    public void addCartItem(MenuItem menuItem) {

    }

    @Override
    public void addCartItem(MenuItem menuItem, ImageView view) {

    }

    @Override
    public void updateCartItem() {

    }

    @Override
    public void showMessage(String message) {

    }

    public void showFrequentBoughtItems(List<MenuItem> frequentBoughtItems) {

    }

    @Override
    public void showError(String error) {

    }
    public void updateFavoriteByPosition(int position, MenuItem menuItem) {

    }
    @Override
    public void showHideModifierItemPrice(boolean show, int modifierGroupPosition) {

    }

    @Override
    public void showModifierSelectionIndicator(boolean hasAtleastOneSelectedItem, int modifierGroupPosition) {

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
