package com.herfy.cart.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.utils.constants.Text;
import com.herfy.ext.PromotionRulesExtKt;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.herfy.util.StringUtil;
import com.herfy.views.SlantedTextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CartPromoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface CartPromoListener {
        void select(Campaign campaign, int position);
    }

    private CartPromoListener listener;

    private List<Campaign> campaigns = new ArrayList<>();
    private Context context;
    public int selected = -1;
    public int imageSize = 200;
    private double total;

    public CartPromoAdapter(Context context, List<Campaign> campaigns, CartPromoListener listener) {
        this.campaigns = campaigns;
        this.context = context;
        this.listener = listener;
    }

    public class ViewHolderData extends RecyclerView.ViewHolder {

        @BindView(R.id.card)
        MaterialCardView card;
        @BindView(R.id.digital_image)
        ImageView digitalImage;
        @BindView(R.id.digital_name)
        TextView digitalName;
        @BindView(R.id.digital_price)
        TextView digitalPrice;
        //@BindView(R.id.digital_desc) TextView digitalDesc;
        @BindView(R.id.progress_image)
        ProgressBar progress;
        @BindView(R.id.coverImage)
        ImageView coverImage;
        @BindView(R.id.ineligble)
        SlantedTextView ineligble;

        public ViewHolderData(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderData(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_digital_coupon, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        setupDataView(((ViewHolderData) holder), position);
    }

    @Override
    public int getItemCount() {
        if(campaigns != null) {
            return campaigns.size();
        }
        return 0;
    }

    private void setupDataView(final ViewHolderData holder, final int position) {

        final Campaign campaign = campaigns.get(position);

        if (selected == position) {
            holder.card.setStrokeWidth(2);
        } else {
            holder.card.setStrokeWidth(0);
        }

        if (campaign.getPromotion() != null && campaign.getPromotion().getAttributes().getType().equalsIgnoreCase("item")) {
            holder.digitalPrice.setVisibility(View.VISIBLE);
            if (campaign.getPromotion().getAttributes().getItemPrice() == 0) {
                holder.digitalPrice.setText(CacheUtil.getInstance().getTranslations(Text.FREE, "FREE").toUpperCase());
            } else {
                holder.digitalPrice.setText(
                        "+" +
                                StringUtil.getDigitalCouponPrice(campaign.getPromotion().getAttributes().getItemPrice(), false)
                                + " " + CacheUtil.getInstance().getTranslations(Text.ONLY, "ONLY").toUpperCase()
                );
            }
        } else {
            holder.digitalPrice.setText("");
            holder.digitalPrice.setVisibility(View.INVISIBLE);
        }

        if(campaign.getAttributes().isEligible() && campaign.getPromotion() != null && PromotionRulesExtKt.isMet(campaign.getPromotion().getAttributes().getRules())) {
            holder.ineligble.setVisibility(View.GONE);
            holder.coverImage.setVisibility(View.GONE);
            holder.card.setOnClickListener(v -> listener.select(campaign, position));
        } else {
            holder.ineligble.setText(CacheUtil.getInstance().getTranslations(Text.INELIGIBLE, "Ineligible"));
            holder.coverImage.setVisibility(View.VISIBLE);
            holder.ineligble.setVisibility(View.VISIBLE);
        }

        holder.digitalName.setText(campaign.getAttributes().getName());

        String imageUrl = campaign.getAttributes().getImageUri();
        if (imageUrl == null || imageUrl.equalsIgnoreCase("null") || imageUrl.isEmpty() || imageUrl.toLowerCase().contains("default-image.png")) {
            imageUrl = Values.DEFAULT_IMAGE_URL;
        }

        Glide.with(context).load(imageUrl).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.ALL).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                holder.digitalImage.setVisibility(View.INVISIBLE);
                holder.progress.setVisibility(View.VISIBLE);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                holder.digitalImage.setVisibility(View.VISIBLE);
                holder.progress.setVisibility(View.GONE);
                return false;
            }
        }).into(holder.digitalImage);
    }

    public void updateCampaigns(int selected, List<Campaign> campaigns) {
        this.selected = selected;
        this.campaigns = campaigns;
        notifyDataSetChanged();
    }

}
