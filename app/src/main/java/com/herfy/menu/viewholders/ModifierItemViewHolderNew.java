package com.herfy.menu.viewholders;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.herfy.BuildConfig;
import com.herfy.R;
import com.herfy.menu.adapters.ModifierItemsNewRecyclerViewAdapter;
import com.herfy.utils.constants.Text;
import com.herfy.menu.MenuContract;
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
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ModifierItemViewHolderNew extends RecyclerView.ViewHolder implements MenuContract.View {

    private Context context;

    private MenuContract.Presenter menuPresenter;

    @BindView(R.id.container)
    ConstraintLayout container;

    @BindView(R.id.check)
    ImageView check;

    @BindView(R.id.check_layout)
    FrameLayout checkLayout;

    @BindView(R.id.image)
    ImageView image;

    @BindView(R.id.modifierNameAndPrice)
    TextView modifierNameAndPrice;

    @BindView(R.id.quantity)
    TextView quantity;

    @BindView(R.id.quantity_container)
    ConstraintLayout quantityContainer;

    @BindView(R.id.quantityDown)
    ImageButton quantityDown;

    @BindView(R.id.quantityUp)
    ImageButton quantityUp;

    @BindView(R.id.clickable_layout)
    LinearLayout clickableLayout;

    @Override
    public void showBestSellers(ArrayList<MenuItem> menuCategories) {

    }

    @Override
    public void showRecentOrders(ArrayList<MenuItem> menuItems) {

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

    private OnAction onAction;

    public interface OnAction {
        void onCheckedChanged(boolean isSelected, ModifierGroup modifierGroup, ModifierItem modifierItem, int position);

        void onQuantityChanged(boolean add, ModifierGroup modifierGroup, ModifierItem modifierItem, int position);
    }

    public void setOnAction(OnAction onAction) {
        this.onAction = onAction;
    }

    public ModifierItemViewHolderNew(Context context, MenuContract.Presenter menuPresenter, View itemView) {

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
    public void showFrequentBoughtItems(List<MenuItem> frequentBoughtItems) {

    }
    @Override
    public void setCartExpiry() {

    }

    @Override
    public void setupViews() {

    }

    public void setupFonts() {

        //name.setTypeface(FontHandler.getRegularTypeface());
        //price.setTypeface(FontHandler.getRegularTypeface());
        //quantity.setTypeface(FontHandler.getRegularTypeface());

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
    public void setModifierItem(int modifierGroupPosition, ModifierGroup modifierGroup, int modifierItemPosition, ModifierItem modifierItem, boolean showModifierItemPrice) {

    }

    @Override
    public void setModifierItem(ModifierGroup modifierGroup, ModifierItem modifierItem, int posTrue) {

        if (modifierItem.getAttributes().isSelected()) {
            check.setImageResource(R.drawable.modifier_radio_item_selected);
        } else {
            check.setImageResource(R.drawable.modifier_item_radio_unselected);
        }

        checkLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onAction != null) {
                    if (modifierItem.getAttributes().isSelected()) {
                        onAction.onCheckedChanged(false, modifierGroup, modifierItem, posTrue);
                    } else {
                        onAction.onCheckedChanged(true, modifierGroup, modifierItem, posTrue);
                    }
                }
            }
        });

        clickableLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onAction != null) {
                    if (modifierItem.getAttributes().isSelected()) {
                        onAction.onCheckedChanged(false, modifierGroup, modifierItem, posTrue);
                    } else {
                        onAction.onCheckedChanged(true, modifierGroup, modifierItem, posTrue);
                    }
                }
            }
        });

        String imageUrl = modifierItem.getAttributes().getImage();
        if (imageUrl != null && !imageUrl.equalsIgnoreCase("null") && !imageUrl.isEmpty() && !imageUrl.toLowerCase().contains("default-image.png")) {
            Glide.with(context).load(imageUrl).apply(RequestOptions.centerCropTransform()).placeholder(R.drawable.imagepicker_image_placeholder).diskCacheStrategy(DiskCacheStrategy.ALL).into(image);
            image.setVisibility(View.VISIBLE);
        } else {
            image.setVisibility(View.GONE);
        }

        String price;
        //hamburgini
        if(BuildConfig.SOLO_CONCEPT == "1") {
            price = ("+" + StringUtil.getFormattedPrice(modifierItem.getAttributes().getPrice()));
        }else{
            if (LocaleUtil.getInstance().isArabic()) {
                price = (StringUtil.getFormattedPrice(modifierItem.getAttributes().getPrice()) + "+");
            } else {
                price = ("+" + StringUtil.getFormattedPrice(modifierItem.getAttributes().getPrice()));
            }
        }


        String text = "<font color='#D62300'>" + price + "</font>";
        if (modifierItem.getAttributes().getPrice() <= 0) {
            text = "";
        }
        modifierNameAndPrice.setText(Html.fromHtml(modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()) + " " + text), TextView.BufferType.SPANNABLE);

        quantity.setText(StringUtil.localize(String.valueOf(modifierItem.getAttributes().getQuantity())));

        quantityContainer.setVisibility(modifierGroup.getAttributes().
                getMaximum() > 1 && modifierItem.getAttributes().
                isSelected() && modifierItem.getAttributes().
                getMaximum() > 1 ? View.VISIBLE : View.GONE);

        quantityDown.setClickable(modifierItem.getAttributes().getQuantity() != 1);

        if (modifierItem.getAttributes().getQuantity() != 1) {
            quantityUp.setImageResource(R.drawable.quantity_up_orange);
        } else {
            quantityDown.setImageResource(R.drawable.quantity_down_gray);
        }

        quantityDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onAction != null) {
                    onAction.onQuantityChanged(false, modifierGroup, modifierItem, posTrue);
                }
            }
        });

        quantityUp.setClickable(modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum());

        if (modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum()) {
            quantityUp.setImageResource(R.drawable.quantity_up_orange);
        } else {
            quantityDown.setImageResource(R.drawable.quantity_down_gray);
        }

        quantityUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onAction != null) {
                    if (modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum()) {
                        onAction.onQuantityChanged(true, modifierGroup, modifierItem, posTrue);
                    } else {
                        Toast.makeText(itemView.getContext(), CacheUtil.getInstance().getTranslations(Text.MODIFIER_MAXIMUM_LIMIT), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

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
    public void showError(String error) {

    }
    public void updateFavoriteByPosition(int position, MenuItem menuItem) {

    }
    @Override
    public void showHideModifierItemPrice(boolean show, int modifierGroupPosition) {

    }

    @Override
    public void showModifierSelectionIndicator(boolean hasAtleastOneSelectedItem, int modifierGroupPosition, HashMap<String, Integer> modifierGroupIndexVisibility, int modifierItemPosition) {

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
    public void setMenuCategoryForAnalytics(MenuItem menuItem, MenuCategory menuCategory, double price, int quantity) {

    }

    @Override
    public void updateFloatingTotalPrice() {

    }

}
