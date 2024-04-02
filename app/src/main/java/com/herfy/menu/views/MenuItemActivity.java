package com.herfy.menu.views;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.dinuscxj.pullzoom.PullZoomRecyclerView;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.herfy.R;
import com.herfy.auth.views.AuthActivity;
import com.herfy.base.BaseActivity;
import com.herfy.utils.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.main.MainActivity;
import com.herfy.menu.MenuContract;
import com.herfy.menu.MenuPresenter;
import com.herfy.menu.adapters.MenuItemRecyclerListAdapter;
import com.herfy.order.views.OrderTypeActivity;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.AuthUtil;
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
import com.herfy.util.CacheHelper;
import com.herfy.util.StringUtil;
import com.herfy.util.ViewUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MenuItemActivity extends BaseActivity implements MenuContract.View {

    private MenuContract.Presenter menuPresenter;

    private int cartMenuItem = -1;
    private boolean fromFavorite = false;

    @BindView(R.id.navigation)
    View navigation;

    //@BindView(R.id.progress) ProgressBar progress;

    @BindView(R.id.back)
    ImageButton back;
    @BindView(R.id.favorite)
    ImageButton favorite;

    @BindView(R.id.back_container)
    RelativeLayout backContainer;

    @BindView(R.id.concept_image)
    ImageView conceptImage;

    @BindView(R.id.overlay)
    View overlay;
    @BindView(R.id.image_shimmer)
    ShimmerFrameLayout imageShimmer;
    @BindView(R.id.details_shimmer)
    ShimmerFrameLayout detailsShimmer;

    @BindView(R.id.recycler_view)
    PullZoomRecyclerView recyclerView;

    @BindView(R.id.quantity)
    TextView quantity;
    @BindView(R.id.add)
    TextView add;
    @BindView(R.id.total)
    TextView total;

    @BindView(R.id.footer)
    ConstraintLayout footer;
    @BindView(R.id.total_container)
    ConstraintLayout totalContainer;

    @BindView(R.id.minus)
    FloatingActionButton minus;
    @BindView(R.id.plus)
    FloatingActionButton plus;

    private LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_item);
        ButterKnife.bind(this);
        menuPresenter = new MenuPresenter(this);

    }

    @Override
    public void onBackPressed() {
        if (!fromFavorite) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra("menu", cartMenuItem == -1);
            intent.putExtra("cart", cartMenuItem != -1);
            startActivity(intent);
        } else {
            super.onBackPressed();
        }
        finish();
    }

    @Override
    protected void onResume() {

        super.onResume();

        menuPresenter.start();

        if (CacheUtil.getInstance().getFavoriteMenuCategory() != null && CacheUtil.getInstance().getFavoriteMenuItem() != null) {

            menuPresenter.setMenuCategory(CacheUtil.getInstance().getFavoriteMenuCategory());
            menuPresenter.setMenuItem(CacheUtil.getInstance().getFavoriteMenuItem());
            setMenuItem(menuPresenter.getMenuCategory(), menuPresenter.getMenuItem());
            CacheUtil.getInstance().setFavoriteMenuCategory(null);
            CacheUtil.getInstance().setFavoriteMenuItem(null);

            showLoadingDialog();
            menuPresenter.addRemoveFavorite(true);

        } else {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {

                if (bundle.containsKey("favorite")) {
                    fromFavorite = bundle.getBoolean("favorite");

                }

                if (bundle.containsKey("order_type") && bundle.getBoolean("order_type")) {

                    menuPresenter.addCartItem(true, true);

                } else if (bundle.containsKey("menu_category_id") && bundle.containsKey("menu_item_id")) {

                    String menuCategoryId = bundle.getString("menu_category_id");
                    String menuItemId = bundle.getString("menu_item_id");

                    menuPresenter.setMenuCategory(menuCategoryId);
                    menuPresenter.getMenuItem(false, menuItemId);

                } else if (bundle.containsKey("cart_menu_item")) {

                    cartMenuItem = bundle.getInt("cart_menu_item");
                    List<MenuItem> menuItems = CacheUtil.getInstance().getCart();
                    if (menuItems != null && cartMenuItem < menuItems.size()) {
                        MenuItem menuItem = menuItems.get(cartMenuItem);
                        menuPresenter.setMenuCategory(menuItem);
                        menuPresenter.setMenuItem(menuItem);
                        setMenuItem(menuPresenter.getMenuCategory(), menuPresenter.getMenuItem());
                    }

                }

            }
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void setPresenter(MenuContract.Presenter presenter) {
        menuPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
    }

    @Override
    public void setupViews() {

        back.setVisibility(View.INVISIBLE);
        favorite.setVisibility(AuthUtil.getInstance().isSignedIn() ? View.VISIBLE : View.INVISIBLE);

        backContainer.setVisibility(View.VISIBLE);

        backContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        favorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AuthUtil.getInstance().isSignedIn()) {
                    showLoadingDialog();
                    menuPresenter.addRemoveFavorite(false);
                } else {
                    CacheUtil.getInstance().setFavoriteMenuCategory(menuPresenter.getMenuCategory());
                    CacheUtil.getInstance().setFavoriteMenuItem(menuPresenter.getMenuItem());
                    Intent intent = new Intent(MenuItemActivity.this, AuthActivity.class);
                    intent.putExtra("favorite_menu_item", true);
                    startActivity(intent);
                }
            }
        });

        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.parseColor("#AAB2B2B2"), Color.parseColor("#00F2F2F2")});
        overlay.setBackground(gradientDrawable);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        plus.getDrawable().setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);

        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuPresenter.changeMenuItemQuantity(true);
            }
        });

        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuPresenter.changeMenuItemQuantity(false);
            }
        });

        totalContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cartMenuItem >= 0) {
                    menuPresenter.updateCartItem(false, cartMenuItem);
                } else {
                    showLoadingDialog();
                    menuPresenter.getCartMenuItem();
                }

            }
        });

    }

    @Override
    public void setupFonts() {
        add.setTypeface(FontHandler.getMediumTypeface());
        total.setTypeface(FontHandler.getMediumTypeface());
    }

    @Override
    public void setupTranslations() {

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
    public void setMenuItem(MenuCategory menuCategory, MenuItem item) {

        HashMap<String, String> eventAttributes = new HashMap<>();
        eventAttributes.put("Item ID", item.getId());
        eventAttributes.put("Item Name", item.getAttributes().getName());

        logEvent("ItemView", eventAttributes, null, 0);

        favorite.setVisibility(AuthUtil.getInstance().isSignedIn() ? View.VISIBLE : View.INVISIBLE);

        setFavorite(item);

        recyclerView.setAdapter(new MenuItemRecyclerListAdapter(this, recyclerView, menuPresenter));

        /*recyclerView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (layoutManager.findFirstVisibleItemPosition() == 0) {
                    back.setImageResource(R.drawable.back_white);
                    conceptImage.setVisibility(View.VISIBLE);
                    favorite.setVisibility(View.VISIBLE);
                    overlay.setVisibility(View.VISIBLE);
                } else {
                    back.setImageResource(R.drawable.back_black);
                    conceptImage.setVisibility(View.GONE);
                    favorite.setVisibility(View.INVISIBLE);
                    overlay.setVisibility(View.GONE);
                }
            }
        });*/

        String orderStoreId = null;
        if (CacheUtil.getInstance().getOrderStore() != null) {
            orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
        }
        total.setText(StringUtil.getFormattedPrice(item.getAttributes().getQuantity() * item.getPrice(orderStoreId)));

        footer.setVisibility(View.VISIBLE);

        setMenuItemQuantity(item.getAttributes().getQuantity());
        menuPresenter.getMenuItemTotalPrice(menuPresenter.getMenuItem(), -1);

        if (cartMenuItem >= 0) {
            add.setText(CacheUtil.getInstance().getTranslations(Text.UPDATE));
        } else {
            add.setText(CacheUtil.getInstance().getTranslations(Text.ADD));
        }

        //progress.setVisibility(View.GONE);

        imageShimmer.setVisibility(View.GONE);
        detailsShimmer.setVisibility(View.GONE);

    }

    @Override
    public void setFavorite(MenuItem menuItem) {
        if (AuthUtil.getInstance().isSignedIn() && CacheUtil.getInstance().getFavorites().contains(menuItem.getId())) {
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
    public void setIngredient(Ingredient ingredient) {

    }

    @Override
    public void setModifierGroup(int position, ModifierGroup modifierGroup, boolean showModifierItemPrice, boolean showShowModifierCheckBox) {

    }

    @Override
    public void setMenuItemQuantity(int quantityValue) {
        if (quantityValue == 1) {
            minus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.secondary_text)));
            minus.getDrawable().setColorFilter(ContextCompat.getColor(this, R.color.primary_text), PorterDuff.Mode.SRC_IN);
        } else {
            minus.setBackgroundTintList(ColorStateList.valueOf(ViewUtil.getColorMain(this)));
            minus.getDrawable().setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
        }
        minus.setClickable(quantityValue > 1);
        quantity.setText(StringUtil.localize(String.valueOf(quantityValue)));
    }

    @Override
    public void setMenuItemTotalPrice(double totalPrice) {
        total.setText(StringUtil.getFormattedPrice(totalPrice));
    }

    @Override
    public void selectSize() {

    }

    @Override
    public void showOrderType() {
        Intent intent = new Intent(this, OrderTypeActivity.class);
        intent.putExtra("home", false);
        intent.putExtra("menu", false);
        intent.putExtra("cart", false);
        intent.putExtra("payment", false);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    public void addCartItem(MenuItem menuItem) {

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(100);
            }
        }

        dismissDialogs();
        Toast.makeText(this, CacheUtil.getInstance().getTranslations(Text.ITEM_ADDED_TO_CART, "Item added to cart"), Toast.LENGTH_SHORT).show();
        if (!fromFavorite) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("order_type", true);
            intent.putExtra("home", false);
            intent.putExtra("menu", true);
            intent.putExtra("cart", false);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
        finish();
    }

    @Override
    public void addCartItem(MenuItem menuItem, ImageView view) {

    }

    @Override
    public void updateCartItem() {
        dismissDialogs();
        Toast.makeText(this, CacheUtil.getInstance().getTranslations(Text.ITEM_UPDATED, "Item updated"), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("home", false);
        intent.putExtra("menu", false);
        intent.putExtra("cart", true);
        startActivity(intent);
        finish();
    }

    @Override
    public void showMessage(String message) {
        dismissDialogs();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showError(String error) {
        dismissDialogs();
        showAlertDialog("", error, CacheUtil.getInstance().getTranslations(Text.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                onBackPressed();
            }
        });
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
        if (Values.sDeepLinkUtm != null) {
            FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this);
            firebaseAnalytics.logEvent(event, Values.sDeepLinkUtm);
        }
    }

    @Override
    public void saveToCache(String key, String value) {
        CacheHelper.save(this, key, value);
    }

    @Override
    public String getFromCache(String key) {
        return CacheHelper.retrieve(this, key);
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
