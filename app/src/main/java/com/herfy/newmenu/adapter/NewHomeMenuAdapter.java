package com.herfy.newmenu.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
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

import com.facebook.shimmer.ShimmerFrameLayout;
import com.herfy.R;
import com.herfy.utils.constants.Values;
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.StringUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NewHomeMenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public int selected = -1;
    private NewHomeMenuAdapterListener listener;

    private List<MenuCategory> menuCategories;

    private Context context;

    public NewHomeMenuAdapter(Context context, List<MenuCategory> menuCategories, NewHomeMenuAdapterListener listener) {
        this.menuCategories = menuCategories;
        this.context = context;
        this.listener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderData(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.tab_category, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        setupDataView(((ViewHolderData) holder), position);
    }

    @Override
    public int getItemCount() {
        return menuCategories.size();
    }

    private void setupDataView(final ViewHolderData holder, final int position) {
        final MenuCategory menuCategory = menuCategories.get(position);

        String imageUrl = menuCategory.getImage();
        if (imageUrl == null || imageUrl.equalsIgnoreCase("null") || imageUrl.isEmpty() || imageUrl.toLowerCase().contains("default-image.png")) {
            imageUrl = Values.DEFAULT_IMAGE_URL;
        } else {
            imageUrl = StringUtil.getCloudflareUrl(Values.MENU_IMAGE_SIZE, Values.MENU_IMAGE_SIZE, imageUrl);
        }

        Glide.with(context).load(imageUrl)
                .skipMemoryCache(false)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .apply(RequestOptions.centerCropTransform())
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        holder.loader.setVisibility(View.GONE);
                        holder.itemImage.setVisibility(View.VISIBLE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        holder.loader.setVisibility(View.GONE);
                        holder.itemImage.setVisibility(View.VISIBLE);
                        return false;
                    }
                }).into(holder.itemImage);

       /* if (position == selected) {
            holder.itemName.setTextColor(context.getResources().getColor(R.color.selectedCatColor));
        } else {
            holder.itemName.setTextColor(context.getResources().getColor(R.color.cod_gray));
        }*/

        holder.itemName.setText(menuCategory.getName(LocaleUtil.getInstance().getLanguage()));

        holder.mainItem.setOnClickListener(v -> {
            listener.selectItem(menuCategory, position);
        });

    }

    public interface NewHomeMenuAdapterListener {
        void selectItem(MenuCategory menuCategory, int position);
    }

    public class ViewHolderData extends RecyclerView.ViewHolder {

        @BindView(R.id.category_image_card)
        CardView mainItem;
        @BindView(R.id.loader)
        ShimmerFrameLayout loader;
        @BindView(R.id.tab_image) ImageView itemImage;
        @BindView(R.id.tab_title)
        TextView itemName;

        public ViewHolderData(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

    }

    public void updateMenuCategories(List<MenuCategory> menuCategories) {
        this.menuCategories = menuCategories;
        notifyDataSetChanged();
    }

}
