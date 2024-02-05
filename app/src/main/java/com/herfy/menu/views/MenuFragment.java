package com.herfy.menu.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.herfy.R;
import com.herfy.base.BaseFragment;
import com.herfy.utils.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.main.MainActivity;
import com.herfy.menu.MenuContract;
import com.herfy.menu.MenuPresenter;
import com.herfy.menu.adapters.MenuCategoriesRecyclerViewAdapter;
import com.herfy.menu.adapters.MenuItemsRecyclerViewAdapter;
import com.herfy.order.views.OrderTypeActivity;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.*;
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
import com.herfy.util.ViewUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MenuFragment extends BaseFragment implements MenuContract.View {

    private MenuContract.Presenter menuPresenter;

    @BindView(R.id.order_type)
    CardView orderType;
    @BindView(R.id.search_container)
    CardView searchContainer;

    @BindView(R.id.order_type_icon)
    ImageView orderTypeIcon;
    @BindView(R.id.open_closed_icon)
    ImageView openClosedIcon;

    @BindView(R.id.search_icon)
    ImageButton searchIcon;

    @BindView(R.id.order_type_label)
    TextView orderTypeLabel;
    @BindView(R.id.close)
    TextView close;
    @BindView(R.id.open_closed)
    TextView openClosed;
    @BindView(R.id.store_message)
    TextView storeMessage;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.empty)
    TextView empty;

    @BindView(R.id.search)
    EditText search;

    @BindView(R.id.store_container)
    ConstraintLayout storeContainer;

    //@BindView(R.id.progress) ProgressBar progress;
    @BindView(R.id.menu_categories_shimmer)
    ShimmerFrameLayout menuCategoriesShimmer;
    @BindView(R.id.menu_items_shimmer)
    ShimmerFrameLayout menuItemsShimmer;

    @BindView(R.id.menu_categories_recycler_view)
    RecyclerView menuCategoriesRecyclerView;
    @BindView(R.id.menu_items_recycler_view)
    RecyclerView menuItemsRecyclerView;

    private MenuCategoriesRecyclerViewAdapter menuCategoriesRecyclerViewAdapter;
    private MenuItemsRecyclerViewAdapter menuItemsRecyclerViewAdapter;

    public MenuFragment() {

    }

    public static MenuFragment newInstance() {
        return new MenuFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        menuPresenter = new MenuPresenter(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        ButterKnife.bind(this, view);

        setRetainInstance(true);

        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        menuPresenter.start();
        menuPresenter.getMenuCategories(false, false, false, false, false, null, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        update();
    }

    @Override
    public void update() {
        super.update();

        if (storeContainer != null) {
            storeContainer.setVisibility(View.GONE);
        }
        if (title != null) {
            title.setVisibility(View.VISIBLE);
        }
        if (orderType != null) {
            menuPresenter.getStoreOpen();
            switch (CacheUtil.getInstance().getOrderType()) {
                case PICKUP:
                    if (getActivity() != null) {
                        menuPresenter.getStore();
                        orderTypeIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.order_type_pickup));
                        orderTypeIcon.setVisibility(View.VISIBLE);
                        if (CacheUtil.getInstance().getOrderStore() != null) {
                            orderTypeLabel.setText(CacheUtil.getInstance().getOrderStore().getAttributes().getName());
                        } else {
                            orderTypeIcon.setVisibility(View.GONE);
                            orderTypeLabel.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE));
                        }
                    }
                    break;
                case DINE_IN:
                    if (getActivity() != null) {
                        menuPresenter.getStore();
                        orderTypeIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.order_type_dine_in));
                        orderTypeIcon.setVisibility(View.VISIBLE);
                        if (CacheUtil.getInstance().getOrderStore() != null) {
                            orderTypeLabel.setText(CacheUtil.getInstance().getOrderStore().getAttributes().getName());
                        } else {
                            orderTypeIcon.setVisibility(View.GONE);
                            orderTypeLabel.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE));
                        }
                    }
                    break;
                case DELIVERY:
                    if (getActivity() != null) {
                        menuPresenter.getAddress();
                        orderTypeIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.order_type_delivery));
                        orderTypeIcon.setVisibility(View.VISIBLE);
                        if (CacheUtil.getInstance().getOrderAddress() != null) {
                            orderTypeLabel.setText(CacheUtil.getInstance().getOrderAddress().getAttributes().getLine1());
                        } else if (CacheUtil.getInstance().getOrderDeliveryAddress() != null) {
                            orderTypeLabel.setText(CacheUtil.getInstance().getOrderDeliveryAddress());
                        } else {
                            orderTypeIcon.setVisibility(View.GONE);
                            orderTypeLabel.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE));
                        }
                    }
                    break;
                case NONE:
                    orderTypeIcon.setVisibility(View.GONE);
                    orderTypeLabel.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE));
                    break;

            }
        }

        /*if(menuItemsRecyclerViewAdapter != null) {
            menuItemsRecyclerViewAdapter.notifyDataSetChanged();
        }

        if(menuPresenter != null && searchContainer.getVisibility() != View.VISIBLE) {
            menuPresenter.refreshMenuCategories();
        }*/

        if (CacheUtil.getInstance().getFavoriteMenuCategory() != null && CacheUtil.getInstance().getFavoriteMenuItem() != null) {

            menuPresenter.setMenuCategory(CacheUtil.getInstance().getFavoriteMenuCategory());
            menuPresenter.setMenuItem(CacheUtil.getInstance().getFavoriteMenuItem());
            setMenuItem(menuPresenter.getMenuCategory(), menuPresenter.getMenuItem());
            CacheUtil.getInstance().setFavoriteMenuCategory(null);
            CacheUtil.getInstance().setFavoriteMenuItem(null);

            ((MainActivity) getActivity()).showLoadingDialog();
            menuPresenter.addRemoveFavorite(true);

        }

    }

    @Override
    public void setPresenter(MenuContract.Presenter presenter) {
        menuPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        if (getActivity() != null) {
            ((MainActivity) getActivity()).logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
        }
    }

    @Override
    public void setCartExpiry() {
        if (getActivity() != null) {
            ((MainActivity) getActivity()).setCartExpiry();
        }
    }

    private LinearLayoutManager layoutManager;

    @Override
    public void setupViews() {

        /*top.post(new Runnable() {
            @Override
            public void run() {
                CoordinatorLayout.LayoutParams layoutParams = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(0, top.getHeight(), 0, 0);

                menuCategoriesContainer.setLayoutParams(layoutParams);
            }
        });*/

        orderType.setRadius(ViewUtil.dpToPx(getActivity(), 10));
        orderType.setCardBackgroundColor(Color.parseColor(String.format("#33%06X", (0xFFFFFF & ViewUtil.getColorMain(getActivity())))));

        orderType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), OrderTypeActivity.class);
                intent.putExtra("home", false);
                intent.putExtra("menu", true);
                intent.putExtra("cart", false);
                intent.putExtra("payment", false);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }
        });

        orderTypeIcon.setColorFilter(ContextCompat.getColor(getActivity(), R.color.primary_text));

        searchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                orderType.setVisibility(View.INVISIBLE);
                storeContainer.setVisibility(View.GONE);
                title.setVisibility(View.VISIBLE);
                searchIcon.setVisibility(View.INVISIBLE);
                searchContainer.setVisibility(View.VISIBLE);
                close.setVisibility(View.VISIBLE);
            }
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() > 0) {
                    menuItemsRecyclerView.setVisibility(View.GONE);
                    menuCategoriesRecyclerViewAdapter.setSelected(-1);
                    menuPresenter.clearSearchedMenuItems();
                    menuItemsRecyclerViewAdapter.updateMenuCategory(null);
                    menuItemsShimmer.setVisibility(View.VISIBLE);
                    menuPresenter.search(editable.toString());
                }
            }
        });

        search.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                orderType.setVisibility(View.VISIBLE);
                menuPresenter.getStoreOpen();
                searchIcon.setVisibility(View.VISIBLE);
                search.setText("");
                searchContainer.setVisibility(View.GONE);
                close.setVisibility(View.GONE);
                menuPresenter.hideSearchedMenuItems();
            }
        });

        /*DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;

        CollapsingToolbarLayout.LayoutParams layoutParams = new CollapsingToolbarLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, width / 2);
        slider.setLayoutParams(layoutParams);*/

        layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        menuCategoriesRecyclerView.setLayoutManager(layoutManager);
        menuItemsRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));

    }

    @Override
    public void setupFonts() {
        orderTypeLabel.setTypeface(FontHandler.getSemiboldTypeface());
        close.setTypeface(FontHandler.getSemiboldTypeface());
        openClosed.setTypeface(FontHandler.getSemiboldTypeface());
        storeMessage.setTypeface(FontHandler.getMediumTypeface());
        title.setTypeface(FontHandler.getExtraboldTypeface());
        search.setTypeface(FontHandler.getRegularTypeface());
    }

    @Override
    public void setupTranslations() {
        orderTypeLabel.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE));
        close.setText(CacheUtil.getInstance().getTranslations(Text.CLOSE));
        title.setText(CacheUtil.getInstance().getTranslations(Text.WHAT_WOULD_YOU_LIKE_TO_EAT));
        empty.setText(CacheUtil.getInstance().getTranslations(Text.ZERO_RESULTS, "0 results"));
        search.setHint(CacheUtil.getInstance().getTranslations(Text.SEARCH_HERE));
    }

    @Override
    public void showStore(Store store) {
        if (orderType != null) {
            switch (CacheUtil.getInstance().getOrderType()) {
                case PICKUP:
                    if (getActivity() != null && CacheUtil.getInstance().getOrderStore() != null) {
                        orderTypeIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.order_type_pickup));
                        orderTypeIcon.setVisibility(View.VISIBLE);
                        orderTypeLabel.setText(CacheUtil.getInstance().getOrderStore().getAttributes().getName());
                    } else {
                        orderTypeIcon.setVisibility(View.GONE);
                        orderTypeLabel.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE));
                    }
                    break;
                case DINE_IN:
                    if (getActivity() != null && CacheUtil.getInstance().getOrderStore() != null) {
                        orderTypeIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.order_type_dine_in));
                        orderTypeIcon.setVisibility(View.VISIBLE);
                        orderTypeLabel.setText(CacheUtil.getInstance().getOrderStore().getAttributes().getName());
                    } else {
                        orderTypeIcon.setVisibility(View.GONE);
                        orderTypeLabel.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE));
                    }
                    break;
                case NONE:
                    orderTypeIcon.setVisibility(View.GONE);
                    orderTypeLabel.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE));
                    break;

            }
        }

    }

    @Override
    public void showStoreOpen(boolean open, String message) {
        if (message.isEmpty()) {
            title.setVisibility(View.VISIBLE);
            storeContainer.setVisibility(View.GONE);
        } else {
            if (open) {
                openClosedIcon.setColorFilter(Color.parseColor("#227D26"));
                openClosed.setText(CacheUtil.getInstance().getTranslations(Text.OPEN_CAPS, "OPEN"));
                openClosed.setTextColor(Color.parseColor("#227D26"));
                storeMessage.setText(message);
                storeMessage.setVisibility(View.VISIBLE);
            } else {
                openClosedIcon.setColorFilter(Color.parseColor("#EC0E00"));
                openClosed.setText(CacheUtil.getInstance().getTranslations(Text.CLOSED_CAPS, "CLOSED"));
                openClosed.setTextColor(Color.parseColor("#EC0E00"));
                storeMessage.setVisibility(View.GONE);
            }
            title.setVisibility(View.GONE);
            storeContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void showAddress(String address) {
        if (orderType != null) {
            switch (CacheUtil.getInstance().getOrderType()) {
                case DELIVERY:
                    if (getActivity() != null) {
                        orderTypeIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.order_type_delivery));
                        orderTypeIcon.setVisibility(View.VISIBLE);
                        orderTypeLabel.setText(address);
                    } else {
                        orderTypeIcon.setVisibility(View.GONE);
                        orderTypeLabel.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE));
                    }
                    break;
                case NONE:
                    orderTypeIcon.setVisibility(View.GONE);
                    orderTypeLabel.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE));
                    break;

            }
        }
    }

    @Override
    public void reloadMenu() {

    }

    @Override
    public void showMenuCategories(boolean refresh, boolean isFromInitialLoad, List<MenuCategory> menuCategories) {

        if (!refresh) {

            menuCategoriesRecyclerViewAdapter = new MenuCategoriesRecyclerViewAdapter(getActivity(), menuPresenter);
            menuCategoriesRecyclerView.setAdapter(menuCategoriesRecyclerViewAdapter);

            menuItemsRecyclerViewAdapter = new MenuItemsRecyclerViewAdapter(getActivity(), menuCategories.get(0), menuPresenter);
            menuItemsRecyclerView.setAdapter(menuItemsRecyclerViewAdapter);

        } else {
            menuCategoriesRecyclerViewAdapter.notifyDataSetChanged();
            if (menuCategoriesRecyclerViewAdapter.getSelected() != -1 && menuCategoriesRecyclerViewAdapter.getSelected() < menuCategories.size()) {
                menuItemsRecyclerViewAdapter.updateMenuCategory(menuCategories.get(menuCategoriesRecyclerViewAdapter.getSelected()));
            } else {
                menuItemsRecyclerViewAdapter.updateMenuCategory(menuCategories.get(0));
            }
        }

        //progress.setVisibility(View.GONE);
        menuCategoriesShimmer.setVisibility(View.INVISIBLE);
        menuItemsShimmer.setVisibility(View.GONE);

        if (!menuItemsRecyclerView.hasNestedScrollingParent(ViewCompat.TYPE_NON_TOUCH)) {
            menuItemsRecyclerView.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        }

        if (!CacheUtil.getInstance().getSliderMenuCategory().isEmpty()) {
            menuPresenter.selectMenuCategory(CacheUtil.getInstance().getSliderMenuCategory());
            CacheUtil.getInstance().setSliderMenuCategory("");
        }

    }

    @Override
    public void selectMenuCategory(int position, MenuCategory menuCategory) {

        HashMap<String, String> eventAttributes = new HashMap<>();
        eventAttributes.put("Category ID", menuCategory.getId());
        eventAttributes.put("Category Name", menuCategory.getAttributes().getName());

        logEvent("CategoryTap", eventAttributes, null, 0);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;

        menuCategoriesRecyclerViewAdapter.setSelected(position);
        layoutManager.scrollToPositionWithOffset(position, (width / 2) - (ViewUtil.dpToPx(getActivity(), 120) / 2));
        menuItemsRecyclerView.setVisibility(View.VISIBLE);
        menuItemsRecyclerViewAdapter.updateMenuCategory(menuCategory);

    }

    @Override
    public void selectMenuItem(@Nullable MenuCategory menuCategory, @Nullable MenuItem menuItem) {
        if (searchContainer.getVisibility() == View.VISIBLE) {
            HashMap<String, String> eventAttributes = new HashMap<>();
            eventAttributes.put("Item ID", menuItem.getId());
            eventAttributes.put("Item Name", menuItem.getAttributes().getName());
            logEvent("ItemSearch", eventAttributes, null, 0);
        }
        /*Intent intent = new Intent(getActivity(), MenuItemActivity.class);
        intent.putExtra("menu_category_id", menuCategory.getId());
        intent.putExtra("menu_item_id", menuItem.getId());
        startActivity(intent);*/
        //((MainActivity) getActivity()).showMenuItem(menuCategory, menuItem);

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
        if (menuItemsRecyclerViewAdapter != null) {
            menuItemsRecyclerViewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void showSearchedMenuItems(ArrayList<MenuItem> searchedMenuItems) {
        menuCategoriesRecyclerViewAdapter.setSelected(-1);
        menuItemsRecyclerViewAdapter.updateMenuCategory(null);
        menuItemsShimmer.setVisibility(View.GONE);
        if (searchedMenuItems.size() > 0) {
            empty.setVisibility(View.GONE);
            menuItemsRecyclerView.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.VISIBLE);
            menuItemsRecyclerView.setVisibility(View.GONE);
        }
    }

    @Override
    public void hideSearchedMenuItems(MenuCategory menuCategory) {
        menuCategoriesRecyclerViewAdapter.setSelected(0);
        menuCategoriesRecyclerView.scrollToPosition(0);
        menuItemsRecyclerView.setVisibility(View.VISIBLE);
        menuItemsRecyclerViewAdapter.updateMenuCategory(menuCategory);
        //progress.setVisibility(View.GONE);
        menuItemsShimmer.setVisibility(View.GONE);
        empty.setVisibility(View.GONE);
        menuPresenter.refreshMenuCategories();
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
        Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(100);
            }
        }

        ((MainActivity) getActivity()).dismissDialogs();
        Toast.makeText(getActivity(), CacheUtil.getInstance().getTranslations(Text.ITEM_ADDED_TO_CART), Toast.LENGTH_SHORT).show();
        //((MainActivity) getActivity()).setCartQuantity(CacheUtil.getInstance().getCartQuantity());
    }

    @Override
    public void addCartItem(MenuItem menuItem, ImageView view) {

    }

    @Override
    public void updateCartItem() {

    }

    @Override
    public void showMessage(String message) {
        ((MainActivity) getActivity()).dismissDialogs();
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showError(String error) {
        ((MainActivity) getActivity()).dismissDialogs();
        ((MainActivity) getActivity()).showAlertDialog("", error);
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
        if(Values.sDeepLinkUtm != null) {
            try {
                Activity activity = getActivity();
                if(activity != null) {
                    FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());
                    firebaseAnalytics.logEvent(event, Values.sDeepLinkUtm);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void saveToCache(String key, String value) {
        CacheHelper.save(requireContext(), key, value);
    }

    @Override
    public String getFromCache(String key) {
        return CacheHelper.retrieve(requireContext(), key);
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
