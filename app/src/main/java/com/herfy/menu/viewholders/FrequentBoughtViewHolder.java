package com.herfy.menu.viewholders;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;
import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.util.StringUtil;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemAttributes;

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
    @BindView(R.id.txtCalories)
    MaterialTextView txtCalories;

    @BindView(R.id.icFavorite)
    ImageView icFavorite;

    @BindView(R.id.quantityDown)
    ImageButton addToCart;

    private Context context;

    public FrequentBoughtViewHolder(Context context, MenuContract.Presenter menuPresenter, @NonNull View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);

        this.context = context;
        this.menuPresenter = menuPresenter;

    }

    public void setFrequentBoughtMenuItem(MenuItem menuItem) {
        String imageUrl = menuItem.getAttributes().getImage();
        if (imageUrl != null && !imageUrl.equalsIgnoreCase("null") && !imageUrl.isEmpty() && !imageUrl.toLowerCase().contains("default-image.png")) {
            Glide.with(context).load(imageUrl).apply(RequestOptions.centerCropTransform()).placeholder(R.drawable.imagepicker_image_placeholder).diskCacheStrategy(DiskCacheStrategy.ALL).into(image);
            image.setVisibility(View.VISIBLE);
        } else {
            image.setVisibility(View.GONE);
        }
        icFavorite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                       menuPresenter.toggleFavorites(menuItem.isFavorite(),menuItem,getAbsoluteAdapterPosition());
                    }
                });

        int calorieCount = menuItem.getCalorieCount();
        String caloriesValue = menuItem.getCalories();

        if (calorieCount != 0) {
            txtCalories.setVisibility(View.VISIBLE);
            txtCalories.setText(String.valueOf(calorieCount));
        } else if (caloriesValue != null && !caloriesValue.isEmpty() && !caloriesValue.trim().equals("null") && !caloriesValue.trim().equals("0")) {
            txtCalories.setVisibility(View.VISIBLE);
            txtCalories.setText(caloriesValue);
        } else {
            txtCalories.setVisibility(View.GONE);
            txtCalories.setText("");
        }
            if (AuthUtil.getInstance().isSignedIn()) {
                Log.d("FAFA","Is favorite " + menuItem.isFavorite());
                if (menuItem.isFavorite()) {
                    Log.d("FAFA","Contains bai");
                    icFavorite.setImageResource(R.drawable.ic_favorite_filled);
                } else {
                    icFavorite.setImageResource(R.drawable.ic_favorite_empty);
                }
                icFavorite.setVisibility(View.VISIBLE);
            } else {
                icFavorite.setVisibility(View.GONE);
            }

        addToCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuPresenter.addMenuItemToCart(menuItem.getId());
            }
        });

      MenuItemAttributes menuItemAttributes = menuItem.getAttributes();
        name.setText(menuItemAttributes.getName());
        txtModifierDescription.setText(menuItem.getDescription(LocaleUtil.getInstance().getLanguage()));
        txtModifierPrice.setText(StringUtil.getFormattedPrice((menuItemAttributes.getPrice())));
    }

}
