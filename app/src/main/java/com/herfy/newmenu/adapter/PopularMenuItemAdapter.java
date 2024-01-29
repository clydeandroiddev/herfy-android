package com.herfy.newmenu.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.auth.views.SignInActivity;
import com.herfy.data.constants.Text;
import com.herfy.data.constants.Values;
import com.herfy.firebase.FirebaseService;
import com.herfy.menu.MenuContract;
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.herfy.util.AuthUtil;
import com.herfy.util.CacheUtil;
import com.herfy.util.StringUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PopularMenuItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public int promoWidth = 200;
    private List<MenuItem> menuItems;
    private Context context;
    private MenuContract.Presenter presenter;
    private boolean isSignedIn;

    public PopularMenuItemAdapter(Context context, List<MenuItem> menuItems, MenuContract.Presenter presenter) {
        this.menuItems = menuItems;
        this.context = context;
        this.presenter = presenter;

        isSignedIn = AuthUtil.getInstance().isSignedIn();
    }

    private void setupDataView(final ViewHolderData holder, final int position) {

        final MenuItem menuItem = menuItems.get(position);

        holder.container.setOnClickListener(v -> {
            presenter.showMenuItem(menuItem.getId());
        });

        String imageUrl = menuItem.getAttributes().getImage();
        if (imageUrl == null || imageUrl.equalsIgnoreCase("null") || imageUrl.isEmpty() || imageUrl.toLowerCase().contains("default-image.png")) {
            imageUrl = Values.DEFAULT_IMAGE_URL;
        } else {
            imageUrl = StringUtil.getCloudflareUrl(Values.MENU_IMAGE_SIZE, Values.MENU_IMAGE_SIZE, imageUrl);
        }

        if (menuItem.getAttributes().getListPrice() > 0) {
            SpannedString formattedOldPrice = StringUtil.getFormattedPrice(menuItem.getListPrice());
            SpannableString oldPriceValue = new SpannableString(formattedOldPrice);
            oldPriceValue.setSpan(new StrikethroughSpan(),0, formattedOldPrice.length(),0);
            holder.oldPrice.setText(oldPriceValue);
            holder.oldPrice.setVisibility(View.VISIBLE);
        } else {
            holder.oldPrice.setVisibility(View.GONE);
        }

        Glide.with(context).load(imageUrl)
                .skipMemoryCache(false)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .apply(RequestOptions.centerInsideTransform())
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        holder.loader.setVisibility(View.GONE);
                        holder.image.setVisibility(View.VISIBLE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        holder.loader.setVisibility(View.GONE);
                        holder.image.setVisibility(View.VISIBLE);
                        return false;
                    }
                }).into(holder.image);

        double price = menuItem.getAttributes().getPrice(CacheUtil.getOrderStoreId());
        holder.price.setText(StringUtil.getFormattedPrice(price));

        holder.addToBag.setOnClickListener(v -> {
            if (CacheUtil.getInstance().getApplication().getAttributes().getMenuOnly()) {
                FirebaseService.newNotification(CacheUtil.getInstance().getTranslations(Text.MENU_ONLY_MESSAGE, "Sorry, ordering is not available at the moment"), holder.itemView.getContext());
                return;
            }
            presenter.addCartMenuItem(holder.image, menuItem, price, 1);
        });

        holder.name.setText(menuItem.getAttributes().getName());

        holder.favoriteCard.setVisibility(isSignedIn && !menuItem.isCombo ? View.VISIBLE : View.GONE);

        if (isSignedIn && CacheUtil.getInstance().getFavorites().contains(menuItem.getId())) {
            holder.favoriteIcon.setImageResource(R.drawable.ic_heart);
            holder.favoriteIcon.setColorFilter(Color.parseColor("#F22424"));
        } else {
            holder.favoriteIcon.setImageResource(R.drawable.ic_heart_empty);
            holder.favoriteIcon.setColorFilter(Color.parseColor("#CCCCCC"));
        }

        holder.favoriteContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.setMenuItem(menuItem);
                MenuCategory menuCategory =presenter.getMenuCategoryFromSearch(menuItem);
				if (isSignedIn) {
					if (CacheUtil.getInstance().getFavorites().contains(menuItem.getId())) {
                        holder.favoriteIcon.setImageResource(R.drawable.ic_heart_empty);
                        holder.favoriteIcon.setColorFilter(Color.parseColor("#CCCCCC"));
					} else {
                        holder.favoriteIcon.setImageResource(R.drawable.ic_heart);
                        holder.favoriteIcon.setColorFilter(Color.parseColor("#F22424"));
					}
					presenter.setMenuItem(menuItem);
					presenter.addRemoveFavorite(false);
				} else if(menuCategory != null) {
					CacheUtil.getInstance().setFavoriteMenuCategory(menuCategory);
					CacheUtil.getInstance().setFavoriteMenuItem(menuItem);
					Intent intent = new Intent(context, SignInActivity.class);
					intent.putExtra("favorite_home", true);
					context.startActivity(intent);
				}
            }
        });

        holder.menuItemBottomButtons.setVisibility(View.VISIBLE);
        holder.container.setStrokeWidth(1);

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderData(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_menu_view_all, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        setupDataView(((ViewHolderData) holder), position);
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    public class ViewHolderData extends RecyclerView.ViewHolder {

        @BindView(R.id.loader)
        ShimmerFrameLayout loader;

        @BindView(R.id.image_item)
        ImageView image;

        @BindView(R.id.item_name)
        TextView name;

        @BindView(R.id.item_price)
        TextView price;

        @BindView(R.id.container)
        MaterialCardView container;

        @BindView(R.id.menuItemBottomButtons)
        ConstraintLayout menuItemBottomButtons;

        @BindView(R.id.add)
        TextView addToBag;

        @BindView(R.id.old_price)
        TextView oldPrice;

        //@BindView(R.id.card_fav) FloatingActionButton cardFav;
        @BindView(R.id.favorite_card) FrameLayout favoriteCard;
        @BindView(R.id.favorite_container) RelativeLayout favoriteContainer;
        @BindView(R.id.favorite_icon) ImageView favoriteIcon;

        public ViewHolderData(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

    }

}
