package com.herfy.menu.viewholders;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
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
import com.herfy.utils.helper.CacheUtil;
import com.nex3z.flowlayout.FlowLayout;
import com.herfy.R;
import com.herfy.utils.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.menu.MenuContract;
import com.herfy.menu.adapters.IngredientsRecyclerViewAdapter;
import com.herfy.menu.adapters.ModifierGroupsNewRecyclerViewAdapter;
import com.herfy.menu.adapters.RecyclerListAdapter;
import com.herfy.menu.adapters.SizesRecyclerViewAdapter;
import com.skylinedynamics.solosdk.api.models.objects.Allergen;
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
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.ViewUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MenuItemDetailsViewHolder extends RecyclerListAdapter.ViewHolder<MenuItem> implements MenuContract.View {

    private Context context;

    private MenuContract.Presenter menuPresenter;

    @BindView(R.id.nested_scroll_view) NestedScrollView nestedScrollView;

    @BindView(R.id.shimmer) ShimmerFrameLayout shimmer;

    @BindView(R.id.card) CardView card;

    @BindView(R.id.name) TextView name;
    @BindView(R.id.calories) TextView calories;
    @BindView(R.id.description) TextView description;
    @BindView(R.id.ingredients_label) TextView ingredientsLabel;

    @BindView(R.id.allergens_container) FlowLayout allergensContainer;

    @BindView(R.id.sizes_container) LinearLayout sizesContainer;

    @BindView(R.id.sizes_recycler_view) RecyclerView sizesRecyclerView;
    @BindView(R.id.modifier_groups_recycler_view) RecyclerView modifierGroupsRecyclerView;
    @BindView(R.id.ingredients_recycler_view) RecyclerView ingredientsRecyclerView;

    private SizesRecyclerViewAdapter sizesAdapter;
    private ModifierGroupsNewRecyclerViewAdapter modifierGroupsAdapter;
    private IngredientsRecyclerViewAdapter ingredientsAdapter;

    public MenuItemDetailsViewHolder(@NonNull View view, Context context, MenuContract.Presenter menuPresenter) {
        super(view);
        ButterKnife.bind(this, view);
        this.context = context;
        this.menuPresenter = menuPresenter;
        setupViews();
        setupFonts();
        setupTranslations();
    }

    @Override
    public void bind(MenuItem item, int position) {

        shimmer.setVisibility(View.GONE);

        name.setText(item.getName(LocaleUtil.getInstance().getLanguage()));

        String caloriesValue = item.getCalories();
        int calorieCount = item.getCalorieCount();
        if (caloriesValue != null && !caloriesValue.isEmpty() && !caloriesValue.trim().equals("null") && !caloriesValue.trim().equals("0")) {
            calories.setText(CacheUtil.getInstance().getTranslations(Text.CALORIES) + " " + caloriesValue);
            calories.setVisibility(View.VISIBLE);
        } else if(calorieCount != 0) {
            calories.setText(CacheUtil.getInstance().getTranslations(Text.CALORIES) + " " + calorieCount);
            calories.setVisibility(View.VISIBLE);
        } else {
            calories.setVisibility(View.GONE);
        }

        allergensContainer.setRtl(LocaleUtil.getInstance().isArabic());
        allergensContainer.removeAllViews();

        for (Allergen allergen : item.getAllergens()) {
            View allergenView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.item_allergen, allergensContainer, false);
            ImageView allergenIcon = allergenView.findViewById(R.id.icon);

            String iconValue = allergen.getAttributes().getIcon(LocaleUtil.getInstance().getLanguage());

            if(iconValue == null || iconValue.equalsIgnoreCase("null") || iconValue.isEmpty() || iconValue.toLowerCase().contains("default-image.png")) {

                allergenIcon.setImageResource(R.drawable.allergen);
                allergenIcon.setVisibility(View.GONE);

            } else {

                Glide.with(context).load(iconValue).apply(RequestOptions.centerCropTransform()).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.ALL).listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        allergenIcon.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        allergenIcon.setVisibility(View.VISIBLE);
                        return false;
                    }
                }).into(allergenIcon);

            }

            TextView allergenName = allergenView.findViewById(R.id.value);
            allergenName.setTypeface(FontHandler.getRegularTypeface());
            allergenName.setText(allergen.getAttributes().getName().getValue(LocaleUtil.getInstance().getLanguage()));
            allergensContainer.addView(allergenView);
        }

        String descriptionValue = item.getAttributes().getDescription();
        if(descriptionValue == null || descriptionValue.isEmpty() || descriptionValue.equals("null") ) {
            description.setVisibility(View.GONE);
        } else {
            description.setText(descriptionValue);
            description.setVisibility(View.VISIBLE);
        }

        setSizes(item);

        /*if(menuPresenter.getDrinksCount() > 0) {
            drinksLabel.setText(menuPresenter.getDrinks().getAttributes().getName());

            drinksAdapter = new DrinksRecyclerViewAdapter(context, menuPresenter);
            drinksAdapter.setOnCheckedChanged(new ModifierItemViewHolder.OnCheckedChanged() {
                @Override
                public void onCheckedChanged(boolean isSelected, ModifierGroup modifierGroup, ModifierItem modifierItem) {
                    menuPresenter.selectDrinks(isSelected, modifierItem);
                    drinksAdapter.notifyDataSetChanged();
                }
            });

            drinksRecyclerView.setAdapter(drinksAdapter);

            drinksLabel.setVisibility(View.VISIBLE);
            drinksRecyclerView.setVisibility(View.VISIBLE);

        } else {
            drinksLabel.setVisibility(View.GONE);
            drinksRecyclerView.setVisibility(View.GONE);
        }*/

        /*if(menuPresenter.getFriesCount() > 0) {
            friesLabel.setText(menuPresenter.getFries().getAttributes().getName());

            friesAdapter = new FriesRecyclerViewAdapter(context, menuPresenter);
            friesAdapter.setOnCheckedChanged(new ModifierItemViewHolder.OnCheckedChanged() {
                @Override
                public void onCheckedChanged(boolean isSelected, ModifierGroup modifierGroup, ModifierItem modifierItem) {
                    menuPresenter.selectFries(isSelected, modifierItem);
                    friesAdapter.notifyDataSetChanged();
                }
            });

            friesRecyclerView.setAdapter(friesAdapter);

            friesLabel.setVisibility(View.VISIBLE);
            friesRecyclerView.setVisibility(View.VISIBLE);
        } else {
            friesLabel.setVisibility(View.GONE);
            friesRecyclerView.setVisibility(View.GONE);
        }*/

        if(menuPresenter.getModifierGroupsCount() > 0) {

            HashMap<Integer, Boolean> showModifierItemPrice = new HashMap<>();
            for(ModifierGroup group : item.getModifierGroups()) {
                showModifierItemPrice.put(item.getModifierGroups().indexOf(group), group.getAttributes().getFreeModifiersCount() == 0);
            }

            modifierGroupsAdapter = new ModifierGroupsNewRecyclerViewAdapter(context, menuPresenter, showModifierItemPrice,calculateShouldShowModifierCheckbox(item.getModifierGroups()));


//            modifierGroupsAdapter.setOnAction(new ModifierItemViewHolder.OnAction() {
//                @Override
//                public void onCheckedChanged(boolean isSelected, int modifierGroupPosition, ModifierGroup modifierGroup, int modifierItemPosition, ModifierItem modifierItem) {
//                    menuPresenter.selectModifierItem(isSelected, modifierGroupPosition, modifierGroup, modifierItemPosition, modifierItem);
//                    setSizes(item);
//                    modifierGroupsAdapter.notifyDataSetChanged();
//                }
//
//                @Override
//                public void onQuantityChanged(boolean add, int modifierGroupPosition, ModifierGroup modifierGroup, int modifierItemPosition, ModifierItem modifierItem) {
//                    menuPresenter.changeModifierItemQuantity(add, modifierGroupPosition, modifierGroup, modifierItemPosition, modifierItem);
//                    setSizes(item);
//                    modifierGroupsAdapter.notifyDataSetChanged();
//                }
//            });

            modifierGroupsRecyclerView.setAdapter(modifierGroupsAdapter);

            modifierGroupsRecyclerView.setVisibility(View.VISIBLE);

        } else {
            modifierGroupsRecyclerView.setVisibility(View.GONE);
        }

        if(menuPresenter.getIngredientsCount() > 0) {
            ingredientsLabel.setText(CacheUtil.getInstance().getTranslations(Text.INGREDIENTS));

            ingredientsAdapter = new IngredientsRecyclerViewAdapter(context, menuPresenter);
            ingredientsAdapter.setOnCheckedChanged(new IngredientViewHolder.OnCheckedChanged() {
                @Override
                public void onCheckedChanged(boolean isSelected, Ingredient ingredient) {
                    menuPresenter.selectIngredient(isSelected, ingredient);
                    ingredientsAdapter.notifyDataSetChanged();
                }
            });

            ingredientsRecyclerView.setAdapter(ingredientsAdapter);

            ingredientsLabel.setVisibility(View.VISIBLE);
            ingredientsRecyclerView.setVisibility(View.VISIBLE);
        } else {
            ingredientsLabel.setVisibility(View.GONE);
            ingredientsRecyclerView.setVisibility(View.GONE);
        }

        card.setVisibility(View.VISIBLE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                nestedScrollView.scrollBy(0, 1000);
                modifierGroupsRecyclerView.smoothScrollToPosition(2);
            }
        }, 200);

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

        sizesRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

        //drinksRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        //friesRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        modifierGroupsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        ingredientsRecyclerView.setLayoutManager(new LinearLayoutManager(context));

    }

    @Override
    public void setupFonts() {
        name.setTypeface(FontHandler.getSemiboldTypeface());
    }

    @Override
    public void setupTranslations() {

    }

    @Override
    public void showStore(Store store) {

    }

    @Override
    public void showStoreOpen(boolean open, String messag) {

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

        boolean showSizes = true;

        VisibilityConfig visibilityConfig = menuItem.getSizes().getAttributes().getVisibilityConfig();
        if (visibilityConfig != null && visibilityConfig.getInitialValue() != null && visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
            showSizes = false;
        }

        if(showSizes) {
            sizesAdapter = new SizesRecyclerViewAdapter(context, menuPresenter);
            sizesRecyclerView.setAdapter(sizesAdapter);

            sizesContainer.removeAllViews();
            if (menuItem.getSizes().getModifierItems().size() > 1) {

                if (menuItem.getSizes().getModifierItems().size() <= 3) {

                    for (ModifierItem modifierItem : menuItem.getSizes().getModifierItems()) {
                        View view = LayoutInflater.from(context).inflate(R.layout.item_size_fixed, sizesContainer, false);
                        view.setTag(modifierItem.getId());
                        MaterialCardView card = view.findViewById(R.id.card);
                        if (modifierItem.getAttributes().isSelected()) {
                            card.setStrokeColor(ViewUtil.getColorMain(context));
                            card.setStrokeWidth(ViewUtil.dpToPx(context, 2));
                        } else {
                            card.setStrokeColor(ContextCompat.getColor(context, android.R.color.transparent));
                            card.setStrokeWidth(0);
                        }

                        ConstraintLayout container = view.findViewById(R.id.container);
                        container.setTag(modifierItem.getId());
                        container.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                for (int i = 0; i < sizesContainer.getChildCount(); i++) {
                                    View childView = sizesContainer.getChildAt(i);
                                    MaterialCardView childCard = childView.findViewById(R.id.card);
                                    if (childView.getTag().equals(view.getTag())) {
                                        childCard.setStrokeColor(ViewUtil.getColorMain(context));
                                        childCard.setStrokeWidth(ViewUtil.dpToPx(context, 2));
                                    } else {
                                        childCard.setStrokeColor(ContextCompat.getColor(context, android.R.color.transparent));
                                        childCard.setStrokeWidth(0);
                                    }
                                }
                                menuPresenter.selectSize((String) view.getTag());
                                sizesAdapter.notifyDataSetChanged();
                            }
                        });

                        TextView name = view.findViewById(R.id.name);
                        name.setTypeface(FontHandler.getSemiboldTypeface());
                        name.setText(modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()));

                        sizesContainer.addView(view);
                    }
                    sizesContainer.setVisibility(View.VISIBLE);
                    sizesRecyclerView.setVisibility(View.GONE);

                } else {

                    sizesAdapter.notifyDataSetChanged();

                    sizesContainer.setVisibility(View.GONE);
                    sizesRecyclerView.setVisibility(View.VISIBLE);
                }

            } else {
                sizesContainer.setVisibility(View.GONE);
            }

        } else {
            sizesContainer.setVisibility(View.GONE);
        }
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
    public HashMap<Integer, Boolean> calculateShouldShowModifierCheckbox(List<ModifierGroup> modifierGroups) {
        HashMap<Integer, Boolean> shouldShowModifierCheckbox = new HashMap<>();

        for (int index = 0; index < modifierGroups.size(); index++) {
            ModifierGroup group = modifierGroups.get(index);
            boolean anySelected = false;
            for (ModifierItem item : group.getModifierItems()) {
                if (item.getAttributes().isSelected()) {
                    anySelected = true;
                    break;
                }
            }
            shouldShowModifierCheckbox.put(index, anySelected);
        }

        return shouldShowModifierCheckbox;
    }
}
