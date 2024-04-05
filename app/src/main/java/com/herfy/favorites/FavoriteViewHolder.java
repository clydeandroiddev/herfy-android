package com.herfy.favorites;


import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.android.material.textview.MaterialTextView;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.utils.constants.Text;
import com.herfy.firebase.FirebaseService;
import com.herfy.font.FontHandler;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.utils.menu.MenuUtil;
import com.skylinedynamics.solosdk.api.models.objects.Favorite;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.herfy.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FavoriteViewHolder extends RecyclerView.ViewHolder implements FavoritesContract.View {

    private FavoritesContract.Presenter favoritesPresenter;

    @BindView(R.id.favorite_icon)
    ImageView favoriteIcon;

    @BindView(R.id.image_item)
    ImageView image;

    @BindView(R.id.txtModifierItemName)
    MaterialTextView name;
    @BindView(R.id.txtFavouritePrice)
    MaterialTextView price;
    @BindView(R.id.txtFavouriteDescription)
    MaterialTextView extras;

    @BindView(R.id.tvCalories)
    TextView tvCalories;

    @BindView(R.id.icAdd)
    ImageView add;
    @BindView(R.id.favouriteQuantityLayout)
    ConstraintLayout favouriteQuantityLayout;

    @BindView(R.id.txtQuantity)

    TextView txtQuantity;

    @BindView(R.id.layoutCalories)
    LinearLayout layoutCalories;

    private FavoritesAction action;

    public FavoriteViewHolder(FavoritesContract.Presenter favoritesPresenter, View itemView,FavoritesAction action) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        this.favoritesPresenter = favoritesPresenter;
        this.action = action;
        setupViews();
        setupFonts();
        setupTranslations();
    }

    @Override
    public void setPresenter(FavoritesContract.Presenter presenter) {

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
        name.setTypeface(FontHandler.getSemiboldTypeface());
        price.setTypeface(FontHandler.getSemiboldTypeface());
    }

    @Override
    public void setupTranslations() {
//        add.setText(CacheUtil.getInstance().getTranslations(Text.ADD_TO_BAG, "Add to Bag"));
    }


    @Override
    public void setIsLoading(boolean isLoading) {

    }

    @Override
    public void showFavorites(ArrayList<Favorite> favorites) {

    }

    @Override
    public void setFavorite(Favorite favorite, ArrayList<String> outOfStockLocationList, double discount, double specialPrice) {
        int quantity = getQuantityByFavourites(favorite);
        favouriteQuantityLayout.setVisibility(quantity >= 1 ? View.VISIBLE : View.GONE);
        txtQuantity.setText(String.valueOf(quantity));
        add.setVisibility(quantity == 0 ? View.VISIBLE : View.GONE);
        favoriteIcon.setOnClickListener(view -> {
            ((BaseActivity) itemView.getContext()).showLoadingDialog();
//            favoriteIcon.setColorFilter(Color.parseColor("#DEDEDE"));
            favoritesPresenter.deleteFavorite(favorite);
        });
            String caloriesValue = favorite.getAttributes().getCalories();
            Integer calorieCount = favorite.getAttributes().getCalorieCount();
         if (calorieCount != null && calorieCount != 0) {
            tvCalories.setVisibility(View.VISIBLE);
             layoutCalories.setVisibility(View.VISIBLE);
            tvCalories.setText(String.valueOf(calorieCount.intValue()));
        } else if (caloriesValue != null && !caloriesValue.isEmpty() && !caloriesValue.trim().equals("null") && !caloriesValue.trim().equals("0")) {
             layoutCalories.setVisibility(View.VISIBLE);
            tvCalories.setVisibility(View.VISIBLE);
            tvCalories.setText(caloriesValue);
        } else {
             layoutCalories.setVisibility(View.GONE);
            tvCalories.setVisibility(View.GONE);
            tvCalories.setText("");
        }
        String imageUrl = favorite.getAttributes().getImageUri();
        if (imageUrl == null || imageUrl.equalsIgnoreCase("null") || imageUrl.isEmpty() || imageUrl.toLowerCase().contains("default-image.png")) {
            imageUrl = Values.DEFAULT_IMAGE_URL;
        }
        Glide.with(itemView.getContext()).load(imageUrl).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.centerCropTransform()).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                image.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
//                progress.setVisibility(View.GONE);
                image.setVisibility(View.VISIBLE);
                return false;
            }
        }).into(image);

        name.setText(favorite.getAttributes().getName());

        price.setText(StringUtil.getFormattedPrice(favorite.getAttributes().getPrice()));

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CacheUtil.getInstance().getApplication().getAttributes().getMenuOnly()) {
                    FirebaseService.newNotification(CacheUtil.getInstance().getTranslations(Text.MENU_ONLY_MESSAGE, "Sorry, ordering is not available at the moment"), add.getContext());
                    return;
                }
                action.onFavouriteAddToCart(favorite);
            }
        });
        favouriteQuantityLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CacheUtil.getInstance().getApplication().getAttributes().getMenuOnly()) {
                    FirebaseService.newNotification(CacheUtil.getInstance().getTranslations(Text.MENU_ONLY_MESSAGE, "Sorry, ordering is not available at the moment"), add.getContext());
                    return;
                }
                action.onFavouriteAddToCart(favorite);
            }
        });
        String description = favorite.getAttributes().getDescription();
        if (!description.isEmpty()) {
            extras.setText(favorite.getAttributes().getDescription());
        }



    }
    public int getQuantityByFavourites(Favorite favorite) {
        int quantity = 0;
        ArrayList<MenuItem> cart = CacheUtil.getInstance().getCart();
        for (MenuItem item : cart) {
            if (Objects.equals(String.valueOf(item.getId()), favorite.getId()) && item.isFavorite()) {
                quantity = item.getAttributes().getQuantity();
            }
        }

        return quantity;
    }
    @Override
    public void showMenuItem(String categoryId, String menuItemId) {

    }

    @Override
    public void addToCartFavorite(MenuItem menuItem, ImageView view) {

    }

    @Override
    public void showMessage(String message) {

    }

    @Override
    public void showError(String error) {

    }

    @Override
    public void showSearchFavorites(ArrayList<Favorite> favorites) {

    }

    @Override
    public void logFirebaseEvent(String event) {

    }

    @Override
    public @Nullable String getFromCache(String key) {
        return null;
    }

}