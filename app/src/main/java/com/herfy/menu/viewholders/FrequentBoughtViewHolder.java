package com.herfy.menu.viewholders;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;
import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.util.PriceFormatter;
import com.herfy.utils.helper.LocaleUtil;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.Favorite;
import com.skylinedynamics.solosdk.api.models.objects.Ingredient;
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemAttributes;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderHistory;
import com.skylinedynamics.solosdk.api.models.objects.Store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
      MenuItemAttributes menuItemAttributes = menuItem.getAttributes();
        name.setText(menuItemAttributes.getName());
        txtModifierDescription.setText(menuItem.getDescription(LocaleUtil.getInstance().getLanguage()));
        txtModifierPrice.setText(PriceFormatter.INSTANCE.formatPriceWithLocale(menuItemAttributes.getPrice(),menuItemAttributes.getName()));
        txtCalories.setText(menuItem.getCalories());
    }

}
