package com.herfy.digitalcoupons.views;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.utils.constants.Text;
import com.herfy.digitalcoupons.DigitalCouponsContract;
import com.herfy.digitalcoupons.DigitalCouponsRecyclerViewAdapter;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DigitalCouponViewHolder extends RecyclerView.ViewHolder implements DigitalCouponsContract.View {

    @BindView(R.id.container) LinearLayout container;
    @BindView(R.id.card) MaterialCardView card;
    @BindView(R.id.image) ImageView image;
    @BindView(R.id.avail) MaterialButton avail;

    private DigitalCouponsContract.Presenter digitalCouponsPresenter;

    private DigitalCouponsRecyclerViewAdapter.OnAction onAction;

    public DigitalCouponViewHolder(DigitalCouponsContract.Presenter digitalCouponsPresenter, View itemView, DigitalCouponsRecyclerViewAdapter.OnAction onAction) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        this.digitalCouponsPresenter = digitalCouponsPresenter;
        this.onAction = onAction;
        setupViews();
        setupFonts();
        setupTranslations();
    }

    @Override
    public void setPresenter(DigitalCouponsContract.Presenter presenter) {

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

    }

    @Override
    public void setupTranslations() {
        avail.setText(CacheUtil.getInstance().getTranslations(Text.AVAIL_NOW_CAPS, "AVAIL NOW"));
    }

    @Override
    public void showCart() {

    }

    @Override
    public void showDigitalCoupons(ArrayList<Campaign> digitalCoupons) {

    }

    @Override
    public void showDigitalCoupon(Campaign digitalCoupon) {
        card.setTransitionName(digitalCoupon.getId());

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAction.view(digitalCoupon, card);
            }
        });

        String imageUrl = digitalCoupon.getAttributes().getImageUri();
        if (imageUrl != null && !imageUrl.equalsIgnoreCase("null") && !imageUrl.isEmpty() && !imageUrl.toLowerCase().contains("default-image.png")) {
            Glide.with(itemView.getContext()).load(imageUrl).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.centerInsideTransform()).listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    //holder.digitalImage.setVisibility(View.INVISIBLE);
                    //holder.progresssImage.setVisibility(View.GONE);
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    //holder.digitalImage.setVisibility(View.VISIBLE);
                    //holder.progresssImage.setVisibility(View.GONE);
                    return false;
                }
            }).into(image);
        }

        avail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAction.avail(digitalCoupon);
            }
        });
    }

    @Override
    public void showError(String error, boolean back) {

    }

}