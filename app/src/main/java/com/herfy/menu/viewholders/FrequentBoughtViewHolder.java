package com.herfy.menu.viewholders;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;
import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.utils.helper.PriceFormatter;
import com.herfy.utils.constants.Text;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemAttributes;

import java.util.ArrayList;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FrequentBoughtViewHolder extends RecyclerView.ViewHolder{
    private MenuContract.Presenter menuPresenter;
    @BindView(R.id.modifierImage)
    ShapeableImageView image;
    @BindView(R.id.txtModifierItemName)
    MaterialTextView name;

    @BindView(R.id.modifierDescription)
    MaterialTextView txtModifierDescription;
    @BindView(R.id.txtmodifierPrice)
    MaterialTextView txtModifierPrice;
    @BindView(R.id.tvCalories)
    MaterialTextView txtCalories;

    @BindView(R.id.layoutCalories)
    LinearLayout layoutCalories;
    @BindView(R.id.btnFavorite)
    ImageView btnFavorite;

    @BindView(R.id.btnAdd)
    ImageButton addToCart;

    @BindView(R.id.frequentlyParentLayout)
    ConstraintLayout clickableLayout;

    private Context context;

    public FrequentBoughtViewHolder(Context context, MenuContract.Presenter menuPresenter, @NonNull View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);

        this.context = context;
        this.menuPresenter = menuPresenter;

    }

    public void setFrequentBoughtMenuItem(MenuItem menuItem) {
        MenuItemAttributes menuItemAttributes = menuItem.getAttributes();
        String imageUrl = menuItemAttributes.getImage();

        boolean isAlreadyAddedToCart = isFavoriteItemInCart(menuItem);
        System.out.println("Cart item " + isAlreadyAddedToCart);
        addToCart.setVisibility(isAlreadyAddedToCart ? View.GONE : View.VISIBLE);
        if (imageUrl != null && !imageUrl.equalsIgnoreCase("null") && !imageUrl.isEmpty() && !imageUrl.toLowerCase().contains("default-image.png")) {
            Glide.with(context).load(imageUrl).apply(RequestOptions.centerCropTransform()).placeholder(R.drawable.imagepicker_image_placeholder).diskCacheStrategy(DiskCacheStrategy.ALL).into(image);
            image.setVisibility(View.VISIBLE);
        } else {
            image.setVisibility(View.GONE);
        }
        btnFavorite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                       menuPresenter.toggleFavorites(menuItem.isFavorite(),menuItem,getAbsoluteAdapterPosition());
                    }
                });

        String caloriesValue = menuItemAttributes.getCalories();
        int calorieCount = menuItemAttributes.getCalorieCount();
        if (calorieCount != 0) {
            layoutCalories.setVisibility(View.VISIBLE);
            txtCalories.setText(CacheUtil.getInstance().getTranslations(Text.CALORIES_KCAL).replace("(x)",String.valueOf(calorieCount)));
        } else if (caloriesValue != null && !caloriesValue.isEmpty() && !caloriesValue.trim().equals("null") && !caloriesValue.trim().equals("0")) {
            layoutCalories.setVisibility(View.VISIBLE);
            txtCalories.setText(CacheUtil.getInstance().getTranslations(Text.CALORIES_KCAL).replace("(x)",caloriesValue));
        } else {
            layoutCalories.setVisibility(View.GONE);
            txtCalories.setText("");
        }
            if (AuthUtil.getInstance().isSignedIn()) {
                if (menuItem.isFavorite()) {
                    btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                } else {
                    btnFavorite.setImageResource(R.drawable.ic_frequent_favourites);
                }
                btnFavorite.setVisibility(View.VISIBLE);
            } else {
                btnFavorite.setVisibility(View.GONE);
            }

        addToCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuPresenter.addMenuItemToCart(menuItem.getId(), getAbsoluteAdapterPosition());
            }
        });
        clickableLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuPresenter.navigateToMenuItemDetails(menuItem);
            }
        });


        name.setText(menuItemAttributes.getName());
        txtModifierDescription.setText(menuItemAttributes.getDescription());
        txtModifierPrice.setText(PriceFormatter.INSTANCE.formatPriceWithLocale(menuItemAttributes.getPrice(),menuItemAttributes.getName()));
    }

    public boolean isFavoriteItemInCart(MenuItem menuItem) {
        ArrayList<MenuItem> cartItems = CacheUtil.getInstance().getCart();
        Pair<Boolean, String> frequentlyMenuAdded = CacheUtil.getInstance().getFrequentlyQuantityShouldShow();

        boolean isFrequentMenuAdded = frequentlyMenuAdded.first;
        String frequentMenuId = frequentlyMenuAdded.second;

        for (MenuItem cartItem : cartItems) {
            if (isSameMenuItem(cartItem, menuItem) && cartItem.isAddedToCart()) {
                return true;
            }
            if (isFrequentMenuAdded && !frequentMenuId.isEmpty() && Objects.equals(String.valueOf(cartItem.getId()), frequentMenuId)) {
                return true;
            }
        }

        return false;
    }
    private boolean isSameMenuItem(MenuItem item1, MenuItem item2) {
        return Objects.equals(String.valueOf(item1.getId()), item2.getId());
    }
}
