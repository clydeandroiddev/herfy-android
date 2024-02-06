package com.herfy.menu.adapters;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.Translated;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.StringUtil;
import com.herfy.util.ViewUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ComboItemListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public int promoWidth = 200;
    private List<ComponentModifierItem> componentModifierItem;
    private Activity context;
    private MenuContract.Presenter presenter;
    private int itemsPosition;
    public String sizeId;

    public ComboItemListAdapter(Activity context, List<ComponentModifierItem> componentModifierItem, MenuContract.Presenter presenter, int itemPosition) {
        this.componentModifierItem = componentModifierItem;
        this.context = context;
        this.presenter = presenter;
        this.itemsPosition = itemPosition;
    }

    @Override
    public int getItemCount() {
        return componentModifierItem.size();
    }

    public class ViewHolderData extends RecyclerView.ViewHolder {

        @BindView(R.id.mainCard)
        CardView mainCard;
        @BindView(R.id.imageModifierItem)
        ImageView imageModifierItem;
        @BindView(R.id.mealSizeLabel)
        TextView mealSizeLabel;
        @BindView(R.id.checkSelected)
        ImageView checkSelected;

        public ViewHolderData(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

    }

    private void setupDataView(final ViewHolderData holder, final int position) {
        final ComponentModifierItem cItem = componentModifierItem.get(position);

        int width = ViewUtil.getSideWidth(context);
        int height = width + (width / 7);

        holder.mainCard.getLayoutParams().width = width;
        holder.mainCard.getLayoutParams().height = height;

        holder.imageModifierItem.getLayoutParams().width = width;
        holder.imageModifierItem.getLayoutParams().height = (int) (height * 0.6);

        if (cItem.selected) {
            String text = "";
            if (cItem.price != 0) {
                String color = CacheUtil.getInstance().getColorMain();
                text = "(" + "<font color='" + color + "'>+" + StringUtil.getFormattedPrice(cItem.price) + "</font>" + ")";
            }
            holder.mealSizeLabel.setText(Html.fromHtml(translated(LocaleUtil.getInstance().getLanguage(), cItem.name) + " " + text), TextView.BufferType.SPANNABLE);
            holder.checkSelected.setVisibility(View.VISIBLE);
        } else {
            holder.mealSizeLabel.setText(translated(LocaleUtil.getInstance().getLanguage(), cItem.name));
            holder.checkSelected.setVisibility(View.GONE);
        }

        if (cItem.image != null && !cItem.image.contains("default-image.png")) {
            String imageUrl = cItem.image;
            imageUrl = StringUtil.getCloudflareUrl(Values.MENU_IMAGE_SIZE, Values.MENU_IMAGE_SIZE, imageUrl);
            Glide.with(context).load(imageUrl)
                    .skipMemoryCache(false)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .apply(RequestOptions.centerInsideTransform())
                    .placeholder(R.drawable.imagepicker_image_placeholder)
                    .error(R.drawable.imagepicker_image_placeholder)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            holder.imageModifierItem.setVisibility(View.VISIBLE);
                            holder.imageModifierItem.setImageResource(R.drawable.imagepicker_image_placeholder);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            holder.imageModifierItem.setVisibility(View.VISIBLE);
                            return false;
                        }
                    }).into(holder.imageModifierItem);

        } else {
            holder.imageModifierItem.setImageResource(R.drawable.imagepicker_image_placeholder);
        }

        holder.mainCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!cItem.selected) {
                    cItem.selected = true;
                    presenter.updateComponents(cItem, itemsPosition, position);
                }
            }
        });
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderData(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_common_item, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        setupDataView((ViewHolderData) holder, position);
    }

    public String translated(String language, Translated name) {
        if (language.equalsIgnoreCase("en-us")) {
            return name.getEnglish();
        } else {
            return language.equalsIgnoreCase("fr") ? name.getFrench() : name.getArabic();
        }
    }

    public void updateComponents(List<ComponentModifierItem> componentModifierItems, int previous, int selected) {
        this.componentModifierItem = componentModifierItems;
        notifyItemChanged(previous);
        notifyItemChanged(selected);
    }

}