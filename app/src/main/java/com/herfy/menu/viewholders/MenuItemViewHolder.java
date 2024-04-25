package com.herfy.menu.viewholders;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.herfy.R;
import com.herfy.auth.views.AuthActivity;
import com.herfy.components.Amount;
import com.herfy.components.AmountTextView;
import com.herfy.menu.adapters.ModifierItemsNewRecyclerViewAdapter;
import com.herfy.utils.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.main.MainActivity;
import com.herfy.menu.MenuContract;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.menu.MenuUtil;
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
import com.herfy.analytics.facebook.FacebookAppEventUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MenuItemViewHolder extends RecyclerView.ViewHolder implements MenuContract.View {

    private Context context;

    private MenuContract.Presenter menuPresenter;

    private boolean isSignedIn;

    @BindView(R.id.container)
    ConstraintLayout container;

    @BindView(R.id.image)
    ImageView image;

    //@BindView(R.id.progress) ProgressBar progress;

    @BindView(R.id.shimmer)
    ShimmerFrameLayout shimmer;

    @BindView(R.id.price)
    AmountTextView price;

    @BindView(R.id.name)
    TextView name;
    @BindView(R.id.calories)
    TextView calories;
    @BindView(R.id.add)
    TextView add;

    @BindView(R.id.favorite)
    ImageButton favorite;

    public MenuItemViewHolder(Context context, MenuContract.Presenter menuPresenter, View itemView) {

        super(itemView);

        ButterKnife.bind(this, itemView);

        this.context = context;
        this.menuPresenter = menuPresenter;

        isSignedIn = AuthUtil.getInstance().isSignedIn();
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

    @SuppressLint("UnsafeOptInUsageError")
    public void setupFonts() {

        price.amountTextView().setTypeface(FontHandler.getSemiboldTypeface());
        price.currencyTextView().setTypeface(FontHandler.getMediumTypeface());
        name.setTypeface(FontHandler.getSemiboldTypeface());
        calories.setTypeface(FontHandler.getSemiboldTypeface());
        add.setTypeface(FontHandler.getMediumTypeface());

    }

    @Override
    public void setupTranslations() {
        add.setText(CacheUtil.getInstance().getTranslations(Text.ADD_TO_CART));
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
    public void showFrequentBoughtItems(List<MenuItem> frequentBoughtItems) {

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
        /*((HomeActivity) context).dismissDialogs();
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();*/
    }

    @Override
    public void updateFavoriteByPosition(int position, MenuItem menuItem) {

    }

    @Override
    public void showError(String error) {
        /*((HomeActivity) context).dismissDialogs();
        ((HomeActivity) context).showAlertDialog("", error);*/
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

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public void setMenuItem(MenuCategory menuCategory, MenuItem menuItem) {

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuPresenter.selectMenuItem(menuCategory, menuItem);
            }
        });

        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuPresenter.selectMenuItem(menuCategory, menuItem);
            }
        });

        String imageUrl = menuItem.getAttributes().getImage();
        if (imageUrl == null || imageUrl.equalsIgnoreCase("null") || imageUrl.isEmpty() || imageUrl.toLowerCase().contains("default-image.png")) {
            imageUrl = Values.DEFAULT_IMAGE_URL;
        } else {
            imageUrl = StringUtil.getCloudflareUrl(Values.MENU_IMAGE_SIZE, Values.MENU_IMAGE_SIZE, imageUrl);
        }

        Glide.with(context).load(imageUrl).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.centerCropTransform()).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                shimmer.setVisibility(View.GONE);
                image.setVisibility(View.INVISIBLE);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                shimmer.setVisibility(View.GONE);
                image.setVisibility(View.VISIBLE);
                return false;
            }
        }).into(image);

        String orderStoreId = null;
if (CacheUtil.getInstance().getOrderStore() != null) {
    orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
}
        price.setAmount(new Amount(menuItem.getPrice(orderStoreId), "", LocaleUtil.getLocale()));
        name.setText(menuItem.getName(LocaleUtil.getInstance().getLanguage()));

        String caloriesValue = menuItem.getCalories();
        int calorieCount = menuItem.getCalorieCount();
        if (caloriesValue != null && !caloriesValue.isEmpty() && !caloriesValue.trim().equals("null") && !caloriesValue.trim().equals("0")) {
            calories.setText(MenuUtil.getCaloriesText(caloriesValue, calorieCount));
            calories.setVisibility(View.VISIBLE);
        } else if (calorieCount != 0) {
            calories.setText(MenuUtil.getCaloriesText(caloriesValue, calorieCount));
            calories.setVisibility(View.VISIBLE);
        } else {
            calories.setVisibility(View.INVISIBLE);
        }

        favorite.setVisibility(isSignedIn && !menuItem.isCombo ? View.VISIBLE : View.INVISIBLE);

        setFavorite(menuItem);

        favorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menuPresenter.setMenuCategory(menuCategory);
                menuPresenter.setMenuItem(menuItem);
                if (isSignedIn) {
                    ((MainActivity) context).showLoadingDialog();
                    menuPresenter.addRemoveFavorite(false);
                } else {
                    CacheUtil.getInstance().setFavoriteMenuCategory(menuPresenter.getMenuCategory());
                    CacheUtil.getInstance().setFavoriteMenuItem(menuPresenter.getMenuItem());
                    Intent intent = new Intent(context, AuthActivity.class);
                    intent.putExtra("favorite_home", true);
                    context.startActivity(intent);
                }
            }
        });

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) itemView.getContext()).showLoadingDialog();
                menuPresenter.setMenuCategory(menuCategory);
                menuPresenter.setMenuItem(menuItem);
                menuPresenter.addCartMenuItem();
            }
        });

    }

    @Override
    public void setFavorite(MenuItem menuItem) {
        if (isSignedIn && CacheUtil.getInstance().getFavorites().contains(menuItem.getId())) {
            favorite.setImageResource(R.drawable.favorite_selected);
        } else {
            favorite.setImageResource(R.drawable.favorite_unselected);
        }
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
        FacebookAppEventUtil.Companion.addToWishlistEvent(context, menuItem);
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