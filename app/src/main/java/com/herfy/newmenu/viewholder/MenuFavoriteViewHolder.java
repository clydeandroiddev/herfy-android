package com.herfy.newmenu.viewholder;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.style.StrikethroughSpan;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
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
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.auth.views.SignInActivity;
import com.herfy.utils.constants.Text;
import com.herfy.firebase.FirebaseService;
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
import com.skylinedynamics.solosdk.api.models.objects.FavoriteModifier;
import com.skylinedynamics.solosdk.api.models.objects.Ingredient;
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.herfy.analytics.FacebookAppEventUtil;
import com.herfy.analytics.FirebaseECommerceEventUtil;
import com.herfy.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MenuFavoriteViewHolder extends RecyclerView.ViewHolder implements MenuContract.View {

    private Context context;

    private MenuContract.Presenter menuPresenter;
    private boolean isSignedIn;

    @BindView(R.id.loader)
    ShimmerFrameLayout loader;

    @BindView(R.id.image_item)
    ImageView image;

    @BindView(R.id.item_name)
    TextView name;

    @BindView(R.id.item_price)
    TextView price;

    @BindView(R.id.container)
    MaterialCardView container;

    @BindView(R.id.menuItemBottomButtons)
    ConstraintLayout menuItemBottomButtons;

    //@BindView(R.id.card_fav) FloatingActionButton cardFav;
    @BindView(R.id.favorite_card)
    FrameLayout favoriteCard;
    @BindView(R.id.favorite_container)
    RelativeLayout favoriteContainer;
    @BindView(R.id.favorite_icon)
    ImageView favoriteIcon;

    @BindView(R.id.add)
    TextView addToCart;

    @BindView(R.id.ivSoldout)
    ImageView ivSoldout;
    @BindView(R.id.soldout)
    TextView soldout;
    @BindView(R.id.old_price)
    TextView oldPrice;

    public MenuFavoriteViewHolder(Context context, MenuContract.Presenter menuPresenter, View itemView) {
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

    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {
        addToCart.setText(CacheUtil.getInstance().getTranslations(Text.ADD_TO_BAG, "Add to Bag"));
    }

    @Override
    public void setFavorite(Favorite favorite, ArrayList<String> outOfStockLocations, double discount, double specialPrice) {
        MenuItem menuItem = MenuUtil.getFavoriteMenuItem(favorite);

        String imageUrl = favorite.getAttributes().getImageUri();
        if (imageUrl == null || imageUrl.equalsIgnoreCase("null") || imageUrl.isEmpty() || imageUrl.toLowerCase().contains("default-image.png")) {
            imageUrl = Values.DEFAULT_IMAGE_URL;
        }

        Glide.with(context).load(imageUrl).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.centerInsideTransform()).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                loader.setVisibility(View.GONE);
                image.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                loader.setVisibility(View.GONE);
                image.setVisibility(View.VISIBLE);
                return false;
            }
        }).into(image);

        name.setText(favorite.getAttributes().getName());

        price.setText(StringUtil.getFormattedPrice(specialPrice));

        StringBuilder mods = new StringBuilder();
        List<FavoriteModifier> modifiers = favorite.getAttributes().getModifiers();
        for (FavoriteModifier m : modifiers) {
            if (!m.getName().isEmpty()) {
                if (TextUtils.isEmpty(mods)) {
                    mods.append(m.getName());
                } else {
                    mods.append(", ").append(m.getName());
                }
            }
        }

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuPresenter.getFavoriteItem(favorite);
                FirebaseECommerceEventUtil.Companion.selectItemEvent(context, menuItem, null, favorite.getAttributes().getPrice());
            }
        });

        switch (CacheUtil.getInstance().getApplication().getAttributes().getHomeScreenDesign()) {
            case "option-1":
                price.setTypeface(FontHandler.getMediumTypeface());
                menuItemBottomButtons.setVisibility(View.GONE);
                container.setStrokeWidth(0);
                container.setBackground(null);
                break;
            case "option-2":
                price.setTypeface(FontHandler.getMediumTypeface());
                menuItemBottomButtons.setVisibility(View.GONE);
                container.setStrokeWidth(1);
                break;
            case "option-3":
                price.setTypeface(FontHandler.getExtraboldTypeface());
                menuItemBottomButtons.setVisibility(View.VISIBLE);
                container.setStrokeWidth(1);
                break;
            default:
                price.setTypeface(FontHandler.getMediumTypeface());
                menuItemBottomButtons.setVisibility(View.GONE);
                container.setStrokeWidth(0);
                container.setBackground(null);
                break;
        }

        setFavorite(menuItem);

        favoriteContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menuPresenter.setMenuItem(menuItem);
                MenuCategory menuCategory = menuPresenter.getMenuCategoryFromSearch(menuItem);
                if (isSignedIn) {
                    ((MainActivity) context).showLoadingDialog();
                    if (CacheUtil.getInstance().getFavorites().contains(menuItem.getId())) {
                        favoriteIcon.setImageResource(R.drawable.ic_heart_empty);
                        favoriteIcon.setColorFilter(Color.parseColor("#CCCCCC"));
                    } else {
                        favoriteIcon.setImageResource(R.drawable.ic_heart);
                        favoriteIcon.setColorFilter(Color.parseColor("#F22424"));
                    }
                    menuPresenter.addRemoveFavorite(false);
                } else if (menuCategory != null) {
                    CacheUtil.getInstance().setFavoriteMenuCategory(menuCategory);
                    CacheUtil.getInstance().setFavoriteMenuItem(menuItem);
                    Intent intent = new Intent(context, SignInActivity.class);
                    intent.putExtra("favorite_home", true);
                    context.startActivity(intent);
                }
            }
        });

        if (CacheUtil.getInstance().getApplication().getAttributes().getMenuOnly()) {
            addToCart.setBackgroundResource(R.drawable.background_oval_disabled);
        } else {
            addToCart.setBackgroundResource(R.drawable.background_oval);
        }

        addToCart.setOnClickListener(v -> {
            if (CacheUtil.getInstance().getApplication().getAttributes().getMenuOnly()) {
                FirebaseService.newNotification(CacheUtil.getInstance().getTranslations(Text.MENU_ONLY_MESSAGE, "Sorry, ordering is not available at the moment"), addToCart.getContext());
                return;
            }
            menuPresenter.addCartMenuItem(image, menuItem, favorite.getAttributes().getPrice(), 1);
        });

        if (discount > 0) {
            SpannedString formattedOldPrice = StringUtil.getFormattedPrice(discount);
            SpannableString oldPriceValue = new SpannableString(formattedOldPrice);
            oldPriceValue.setSpan(new StrikethroughSpan(),0, formattedOldPrice.length(),0);
            oldPrice.setText(oldPriceValue);
            oldPrice.setVisibility(View.VISIBLE);

        } else {
            oldPrice.setText(null);
            oldPrice.setVisibility(View.GONE);
        }

        if (CacheUtil.getOrderStoreId() != null && outOfStockLocations.contains(CacheUtil.getOrderStoreId())) {
            ivSoldout.setVisibility(View.VISIBLE);
            soldout.setVisibility(View.VISIBLE);
            soldout.setText(CacheUtil.getInstance().getTranslations(Text.SOLD_OUT_CAPS, "SOLD OUT"));
            image.setColorFilter(ContextCompat.getColor(context, R.color.soldout));
            addToCart.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.disabled_tint)));
            addToCart.setEnabled(false);
        } else {
            ivSoldout.setVisibility(View.GONE);
            soldout.setVisibility(View.GONE);
            addToCart.setEnabled(true);
            image.setColorFilter(ContextCompat.getColor(context, R.color.transparent));
            addToCart.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_tint)));
        }
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
    public void setMenuItem(MenuCategory menuCategory, MenuItem menuItem) {

    }

    @Override
    public void setFavorite(MenuItem menuItem) {
        if (isSignedIn && CacheUtil.getInstance().getFavorites().contains(menuItem.getId())) {
            favoriteIcon.setImageResource(R.drawable.ic_heart);
            favoriteIcon.setColorFilter(Color.parseColor("#F22424"));
            //cardFav.setImageResource(R.drawable.ic_heart);
            //cardFav.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F22424")));
        } else {
            favoriteIcon.setImageResource(R.drawable.ic_heart_empty);
            favoriteIcon.setColorFilter(Color.parseColor("#CCCCCC"));
            //cardFav.setImageResource(R.drawable.ic_heart_empty);
            //cardFav.setImageTintList(ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
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
    public void setIngredient(Ingredient ingredient) {

    }

    @Override
    public void setModifierGroup(int position, ModifierGroup modifierGroup, boolean showModifierItemPrice, boolean showShowModifierCheckBox) {

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

    }

    @Override
    public void showError(String error) {

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
    public void setBestSeller(MenuItem menuItem, ArrayList<String> outOfStockLocations, double discount, double specialPrice) {

    }

    @Override
    public void setRecentOrder(MenuItem menuItem, ArrayList<String> outOfStockLocations, double discount, double specialPrice) {

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
        FacebookAppEventUtil.Companion.addToWishlistEvent(context,menuItem);
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