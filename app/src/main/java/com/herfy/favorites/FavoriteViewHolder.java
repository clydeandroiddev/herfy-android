package com.herfy.favorites;

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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
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
import com.herfy.base.BaseActivity;
import com.herfy.utils.constants.Text;
import com.herfy.firebase.FirebaseService;
import com.herfy.font.FontHandler;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.Favorite;
import com.skylinedynamics.solosdk.api.models.objects.FavoriteModifier;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.StringUtil;
import com.herfy.views.SlantedTextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FavoriteViewHolder extends RecyclerView.ViewHolder implements FavoritesContract.View {

    private FavoritesContract.Presenter favoritesPresenter;

    //@BindView(R.id.card_fav) FloatingActionButton heart;
    @BindView(R.id.favorite_card)
    FrameLayout favoriteCard;
    @BindView(R.id.favorite_container)
    RelativeLayout favoriteContainer;
    @BindView(R.id.favorite_icon)
    ImageView favoriteIcon;

    @BindView(R.id.image_item)
    ImageView image;

    @BindView(R.id.progress)
    ProgressBar progress;

    @BindView(R.id.item_name)
    TextView name;
    @BindView(R.id.item_price)
    TextView price;
    @BindView(R.id.item_description)
    TextView extras;

    @BindView(R.id.add_to_bag)
    AppCompatButton add;

    @BindView(R.id.stvSoldout)
    SlantedTextView stvSoldout;

    @BindView(R.id.old_price)
    TextView oldPrice;

    @BindView(R.id.discount_indicator)
    ImageView discountIndicator;

    public FavoriteViewHolder(FavoritesContract.Presenter favoritesPresenter, View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        this.favoritesPresenter = favoritesPresenter;
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
        add.setText(CacheUtil.getInstance().getTranslations(Text.ADD_TO_BAG, "Add to Bag"));
    }

    @Override
    public void showLoadingDialog() {

    }

    @Override
    public void showFavorites(ArrayList<Favorite> favorites) {

    }

    @Override
    public void setFavorite(Favorite favorite, ArrayList<String> outOfStockLocationList, double discount, double specialPrice) {

        favoriteIcon.setColorFilter(Color.parseColor("#F22424"));

        favoriteContainer.setOnClickListener(view -> {
            ((BaseActivity) itemView.getContext()).showLoadingDialog();
            favoriteIcon.setColorFilter(Color.parseColor("#DEDEDE"));
            favoritesPresenter.deleteFavorite(favorite);
        });

        String imageUrl = favorite.getAttributes().getImageUri();
        if (imageUrl == null || imageUrl.equalsIgnoreCase("null") || imageUrl.isEmpty() || imageUrl.toLowerCase().contains("default-image.png")) {
            imageUrl = Values.DEFAULT_IMAGE_URL;
        }

        Glide.with(itemView.getContext()).load(imageUrl).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.centerCropTransform()).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                progress.setVisibility(View.GONE);
                image.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                progress.setVisibility(View.GONE);
                image.setVisibility(View.VISIBLE);
                return false;
            }
        }).into(image);

        name.setText(favorite.getAttributes().getName());

        price.setText(StringUtil.getFormattedPrice(specialPrice));

        if (LocaleUtil.getInstance().isArabic()) {
            stvSoldout.setMode(SlantedTextView.MODE_LEFT);
        } else stvSoldout.setMode(SlantedTextView.MODE_RIGHT);

        if (discount > 0.0) {
            SpannedString formattedOldPrice = StringUtil.getFormattedPrice(discount);
            SpannableString oldPriceValue = new SpannableString(formattedOldPrice);
            oldPriceValue.setSpan(new StrikethroughSpan(),0, formattedOldPrice.length(),0);
            oldPrice.setText(formattedOldPrice);
            oldPrice.setVisibility(View.VISIBLE);
            discountIndicator.setVisibility(View.VISIBLE);
        } else {
            oldPrice.setText(null);
            oldPrice.setVisibility(View.GONE);
            discountIndicator.setVisibility(View.GONE);
        }

        if (CacheUtil.getOrderStoreId() != null && outOfStockLocationList.contains(CacheUtil.getOrderStoreId())) {
            stvSoldout.setText(CacheUtil.getInstance().getTranslations(Text.SOLD_OUT_CAPS, "SOLD OUT"));
            stvSoldout.setVisibility(View.VISIBLE);
            image.setColorFilter(ContextCompat.getColor(image.getContext(), R.color.soldout));
            add.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(add.getContext(), R.color.disabled_tint)));
            add.setEnabled(false);
        } else {
            stvSoldout.setVisibility(View.GONE);
            add.setBackgroundResource(R.drawable.background_add_to_bag);
            add.setEnabled(true);
        }

        if (CacheUtil.getInstance().getApplication().getAttributes().getMenuOnly()) {
            add.setBackgroundResource(R.drawable.background_add_to_bag_disabled);
        } else {
            if (CacheUtil.getOrderStoreId() != null && !outOfStockLocationList.contains(CacheUtil.getOrderStoreId()))
                add.setBackgroundResource(R.drawable.background_add_to_bag);
        }

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CacheUtil.getInstance().getApplication().getAttributes().getMenuOnly()) {
                    FirebaseService.newNotification(CacheUtil.getInstance().getTranslations(Text.MENU_ONLY_MESSAGE, "Sorry, ordering is not available at the moment"), add.getContext());
                    return;
                }
                favoritesPresenter.addToCartFavorite(favorite, image);
            }
        });

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

        extras.setText(mods);

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