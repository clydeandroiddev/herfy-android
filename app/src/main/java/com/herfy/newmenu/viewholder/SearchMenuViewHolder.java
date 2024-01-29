package com.herfy.newmenu.viewholder;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.herfy.R;
import com.herfy.auth.views.SignInActivity;
import com.herfy.data.constants.Text;
import com.herfy.data.constants.Values;
import com.herfy.firebase.FirebaseService;
import com.herfy.main.MainActivity;
import com.herfy.menu.MenuContract;
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
import com.herfy.util.AuthUtil;
import com.herfy.util.CacheUtil;
import com.herfy.analytics.FacebookAppEventUtil;
import com.herfy.util.LocaleUtil;
import com.herfy.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SearchMenuViewHolder extends RecyclerView.ViewHolder implements MenuContract.View {

    private Context context;

    private MenuContract.Presenter menuPresenter;

    @BindView(R.id.image_item)
    ImageView itemImage;
    //@BindView(R.id.card_fav) FloatingActionButton cardFav;
    @BindView(R.id.favorite_card)
    FrameLayout favoriteCard;
    @BindView(R.id.favorite_container)
    RelativeLayout favoriteContainer;
    @BindView(R.id.favorite_icon)
    ImageView favoriteIcon;
    /*@BindView(R.id.heart_image)
    ImageView heartImage;*/
    @BindView(R.id.item_name)
    AppCompatTextView itemName;
    @BindView(R.id.add_to_bag)
    AppCompatButton addToBag;
    @BindView(R.id.item_price)
    AppCompatTextView itemPrice;
    @BindView(R.id.item_description)
    AppCompatTextView itemDescription;
    @BindView(R.id.container)
    ConstraintLayout container;

    public SearchMenuViewHolder(Context context, MenuContract.Presenter menuPresenter, View itemView) {
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
        addToBag.setText(CacheUtil.getInstance().getTranslations(Text.ADD_TO_BAG, "Add to Bag"));
    }

    @Override
    public void showStore(Store store) {

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
    public void setIngredient(Ingredient ingredient) {

    }

    @Override
    public void setModifierGroup(int position, ModifierGroup modifierGroup, boolean showModifierItemPrice) {

    }

    @Override
    public void setMenuItemQuantity(int quantity) {

    }

    @Override
    public void setMenuItemTotalPrice(double totalPrice) {

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
        /*((HomeActivity) context).dismissDialogs();
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();*/
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
    public void showFavorites(boolean fromAddRemove, ArrayList<Favorite> favorites) {

    }

    @Override
    public void setFavorite(Favorite favorite, ArrayList<String> outOfStockLocations, double discount, double specialPrice) {

    }

    @Override
    public void setMenuItem(MenuCategory menuCategory, MenuItem menuItem) {

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuPresenter.selectMenuItem(menuCategory, menuItem);
            }
        });

        itemImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuPresenter.selectMenuItem(menuCategory, menuItem);
            }
        });

        String imageUrl = menuItem.getImage();
        if (imageUrl == null || imageUrl.equalsIgnoreCase("null") || imageUrl.isEmpty() || imageUrl.toLowerCase().contains("default-image.png")) {
            imageUrl = Values.DEFAULT_IMAGE_URL;
        } else {
            imageUrl = StringUtil.getCloudflareUrl(Values.MENU_IMAGE_SIZE, Values.MENU_IMAGE_SIZE, imageUrl);
        }

        Glide.with(context).load(imageUrl).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.centerCropTransform()).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                itemImage.setVisibility(View.INVISIBLE);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                itemImage.setVisibility(View.VISIBLE);
                return false;
            }
        }).into(itemImage);

        String description = menuItem.getDescription(LocaleUtil.getInstance().getLanguage());
        if (description != null && !description.isEmpty() && !description.equalsIgnoreCase("null")) {
            itemDescription.setText(description);
        } else {
            itemDescription.setText("");
        }
        String orderStoreId = null;
        if (CacheUtil.getInstance().getOrderStore() != null) {
            orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
        }
        double price = menuItem.getPrice(orderStoreId);
        itemPrice.setText(StringUtil.getFormattedPrice(price));
        if (price > 0) {
            itemPrice.setVisibility(View.VISIBLE);
        } else {
            itemPrice.setVisibility(View.INVISIBLE);
        }
        itemName.setText(menuItem.getName(LocaleUtil.getInstance().getLanguage()));

        favoriteCard.setVisibility(AuthUtil.getInstance().isSignedIn() && !menuItem.isCombo ? View.VISIBLE : View.INVISIBLE);
        setFavorite(menuItem);

        favoriteContainer.setOnClickListener(view -> {
            MenuCategory category = menuPresenter.getMenuCategoryFromSearch(menuItem);
            if (category != null) {
                menuPresenter.setMenuCategory(category);
                menuPresenter.setMenuItem(menuItem);
                if (AuthUtil.getInstance().isSignedIn()) {
                    ((MainActivity) context).showLoadingDialog();
                    if (CacheUtil.getInstance().getFavorites().contains(menuItem.getId())) {
                        favoriteIcon.setColorFilter(Color.parseColor("#DEDEDE"));
                    } else {
                        favoriteIcon.setColorFilter(Color.parseColor("#F22424"));
                    }
                    menuPresenter.addRemoveFavorite(false);
                } else {
                    CacheUtil.getInstance().setFavoriteMenuCategory(menuPresenter.getMenuCategory());
                    CacheUtil.getInstance().setFavoriteMenuItem(menuPresenter.getMenuItem());
                    Intent intent = new Intent(context, SignInActivity.class);
                    intent.putExtra("favorite_home", true);
                    context.startActivity(intent);
                }
            }
        });

        if (CacheUtil.getInstance().getApplication().getAttributes().getMenuOnly()) {
            addToBag.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.disabled_tint)));
        } else {
            addToBag.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_tint)));
        }

        addToBag.setOnClickListener(v -> {
            if (CacheUtil.getInstance().getApplication().getAttributes().getMenuOnly()) {
                FirebaseService.newNotification(CacheUtil.getInstance().getTranslations(Text.MENU_ONLY_MESSAGE, "Sorry, ordering is not available at the moment"), addToBag.getContext());
                return;
            }
            menuPresenter.addCartMenuItem(itemImage, menuItem, price, 1);
        });
    }

    @Override
    public void setFavorite(MenuItem menuItem) {
        if (AuthUtil.getInstance().isSignedIn() && CacheUtil.getInstance().getFavorites().contains(menuItem.getId())) {
            favoriteIcon.setColorFilter(Color.parseColor("#F22424"));
        } else {
            favoriteIcon.setColorFilter(Color.parseColor("#DEDEDE"));
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