package com.herfy.upsell;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.style.StrikethroughSpan;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.data.constants.Values;
import com.herfy.font.FontHandler;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.herfy.util.CacheUtil;
import com.herfy.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class UpsellViewHolder extends RecyclerView.ViewHolder implements UpsellContract.View {

    private UpsellContract.Presenter upsellPresenter;

    @BindView(R.id.container)
    MaterialCardView container;

    @BindView(R.id.inner_main)
    ConstraintLayout mainInner;

    @BindView(R.id.plus)
    FrameLayout plus;
    @BindView(R.id.minus)
    FrameLayout minus;

    @BindView(R.id.image)
    ImageView image;

    @BindView(R.id.quantity)
    TextView quantity;
    @BindView(R.id.name)
    TextView name;
    @BindView(R.id.total_price)
    TextView price;
    @BindView(R.id.old_price)
    TextView oldPrice;
    @BindView(R.id.discount_indicator)
    ImageView discountIndicator;

    @BindView(R.id.quantity_layout)
    FrameLayout quantityLayout;

    public UpsellViewHolder(UpsellContract.Presenter upsellPresenter, View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        this.upsellPresenter = upsellPresenter;
        setupViews();
        setupFonts();
        setupTranslations();
    }

    @Override
    public void setPresenter(UpsellContract.Presenter presenter) {

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

    @Override
    public void setupFonts() {

        quantity.setTypeface(FontHandler.getSemiboldTypeface());
        name.setTypeface(FontHandler.getSemiboldTypeface());
        price.setTypeface(FontHandler.getSemiboldTypeface());

    }

    @Override
    public void setupTranslations() {

    }

    @Override
    public void showUpsells(ArrayList<MenuItem> upsells) {

    }

    @Override
    public void setUpsell(MenuItem upsell) {

        container.setOnClickListener(v -> {
            if (upsell.isMustBeCustomized()) {
                upsellPresenter.goToItemDetails(getAdapterPosition(), upsell);
            } else {
                if (upsell.getAttributes().getQuantity() != 0) {
                    upsellPresenter.changeToZero(getAdapterPosition());
                } else {
                    upsellPresenter.changeQuantity(getAdapterPosition(), true);
                }
            }
        });

        if (upsell.isMustBeCustomized()) {
            quantityLayout.setVisibility(View.GONE);
            container.setStrokeColor(ContextCompat.getColor(itemView.getContext(), android.R.color.transparent));
        } else {
            if (upsell.getAttributes().getQuantity() != 0) {
                quantityLayout.setVisibility(View.VISIBLE);
                container.setStrokeColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
            } else {
                quantityLayout.setVisibility(View.GONE);
                container.setStrokeColor(ContextCompat.getColor(itemView.getContext(), android.R.color.transparent));
            }
        }

        if (upsell.getAttributes().getListPrice() > 0) {
            SpannedString formattedOldPrice = StringUtil.getFormattedPrice(upsell.getAttributes().getListPrice());
            SpannableString oldPricevalue = new SpannableString(formattedOldPrice);
            oldPricevalue.setSpan(new StrikethroughSpan(), 0, formattedOldPrice.length(), 0);
            oldPrice.setText(oldPricevalue);
            discountIndicator.setVisibility(View.VISIBLE);
            oldPrice.setVisibility(View.VISIBLE);
        } else {
            discountIndicator.setVisibility(View.GONE);
            oldPrice.setVisibility(View.GONE);
        }

        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                upsellPresenter.changeQuantity(getAdapterPosition(), true);
            }
        });

        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                upsellPresenter.changeQuantity(getAdapterPosition(), false);
            }
        });

        String imageUrl = upsell.getAttributes().getImage();
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
                image.setVisibility(View.VISIBLE);
                return false;
            }
        }).into(image);

        quantity.setText(StringUtil.localize(String.valueOf(upsell.getAttributes().getQuantity())));
        name.setText(upsell.getAttributes().getName());
        String orderStoreId = null;
        if (CacheUtil.getInstance().getOrderStore() != null) {
            orderStoreId = CacheUtil.getInstance().getOrderStore().getId();
        }
        price.setText(StringUtil.getFormattedPrice(upsell.getAttributes().getPrice(orderStoreId)));

    }

    @Override
    public void setUpsellTotalPrice(double totalPrice) {

    }

    @Override
    public void continueCheckout() {

    }

    @Override
    public void showDigitalCoupons(boolean checkout, List<Campaign> digitalCoupons, int selected) {

    }

    @Override
    public void upsellPromotionSucess(boolean success) {

    }

    @Override
    public void logFirebaseEvent(String event) {

    }

    @Override
    public void goToItemDetails(int position, MenuItem upsell) {

    }

}