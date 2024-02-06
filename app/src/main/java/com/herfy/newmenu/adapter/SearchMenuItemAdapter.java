package com.herfy.newmenu.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import com.herfy.main.MainActivity;
import com.herfy.menu.MenuContract;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.herfy.util.StringUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SearchMenuItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public int promoWidth = 200;
    private List<MenuItem> menuItems;
    private Context context;
    private MenuContract.Presenter presenter;

    public SearchMenuItemAdapter(Context context, List<MenuItem> menuItems, MenuContract.Presenter presenter) {
        this.menuItems = menuItems;
        this.context = context;
        this.presenter = presenter;
    }

    private void setupDataView(final ViewHolderData holder, final int position) {

        final MenuItem menuItem = menuItems.get(position);

        holder.image.setOnClickListener(v -> {
            presenter.selectMenuItem(null, menuItem);
        });

        holder.container.setOnClickListener(v -> {
            presenter.selectMenuItem(null, menuItem);
        });

      /*  holder.listMainMenuContainer.getLayoutParams().width = promoWidth;
        holder.listMainMenuContainer.invalidate();

        holder.addToCart.setText(CacheUtil.getInstance().getTranslations("add_to_cart_caps", "ADD TO CART"));
*/
        String imageUrl = menuItem.getAttributes().getImage();
        if (imageUrl == null || imageUrl.equalsIgnoreCase("null") || imageUrl.isEmpty() || imageUrl.toLowerCase().contains("default-image.png")) {
            imageUrl = Values.DEFAULT_IMAGE_URL;
        } else {
            imageUrl = StringUtil.getCloudflareUrl(Values.MENU_IMAGE_SIZE, Values.MENU_IMAGE_SIZE, imageUrl);

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

        holder.price.setText(StringUtil.getFormattedPrice(menuItem.getAttributes().getPrice(CacheUtil.getOrderStoreId())));

        /*holder.addToCart.setOnClickListener(v -> {
            MenuCategory menuCategory = presenter.getMenuCategoryFromSearch(menuItem);
            presenter.setMenuCategory(menuCategory);
            presenter.setMenuItem(menuItem);
            presenter.addCartMenuItem();
        });

        holder.favorite.setOnClickListener(v -> {
            MenuCategory menuCategory = presenter.getMenuCategoryFromSearch(menuItem);
            presenter.setMenuCategory(menuCategory);
            presenter.setMenuItem(menuItem);
            if (AuthUtil.getInstance().isSignedIn()) {
                String message = CacheUtil.getInstance().getTranslations(Text.ITEM_REMOVED_FROM_FAVORITES, "Item removed from favorites");
                if (CacheUtil.getInstance().getFavorites().contains(menuItem.getId())) {
                    //holder.favoriteHeart.setColorFilter(Color.parseColor("#CCCCCC"));
                    holder.favorite.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5F5F5")));
                    holder.favorite.setImageResource(R.drawable.favorite_heart_unselected);
                    holder.favorite.setColorFilter(Color.parseColor("#CCCCCC"));
                } else {
                    message = CacheUtil.getInstance().getTranslations(Text.ITEM_ADDED_TO_FAVORITES, "Item added to favorites");
                    //holder.favoriteHeart.setColorFilter(Color.parseColor("#FF8533"));
                    holder.favorite.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D62300")));
                    holder.favorite.setImageResource(R.drawable.ic_heart);
                    holder.favorite.setColorFilter(Color.WHITE);
                }
                presenter.addRemoveFavorite(false);
                presenter.favoriteMessage(message);
            }
        });

        //holder.favoriteRectangle.setVisibility(AuthUtil.getInstance().isSignedIn() ? View.VISIBLE : View.INVISIBLE);
        //holder.favoriteHeart.setVisibility(AuthUtil.getInstance().isSignedIn() ? View.VISIBLE : View.GONE);
        holder.favorite.setVisibility(AuthUtil.getInstance().isSignedIn() ? View.VISIBLE : View.GONE);

        if (AuthUtil.getInstance().isSignedIn()) {
            //holder.favoriteHeart.setColorFilter(Color.parseColor(CacheUtil.getInstance().getFavorites().contains(menuItem.getId()) ? "#FF8533" : "#CCCCCC"));
            boolean isFavorite = CacheUtil.getInstance().getFavorites().contains(menuItem.getId());
            holder.favorite.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(isFavorite ? "#D62300" : "#F5F5F5")));
            holder.favorite.setImageResource(isFavorite ? R.drawable.ic_heart : R.drawable.favorite_heart_unselected);
            holder.favorite.setColorFilter(isFavorite ? Color.WHITE : Color.parseColor("#CCCCCC"));
        }*/

        holder.name.setText(menuItem.getAttributes().getName());

       /* holder.listMainMenuContainer.setOnClickListener(v -> {
            MenuCategory menuCategory = presenter.getMenuCategoryFromSearch(menuItem);
            presenter.selectMenuItem(menuCategory, menuItem);
        });*/

        holder.favoriteCard.setVisibility(AuthUtil.getInstance().isSignedIn() && !menuItem.isCombo ? View.VISIBLE : View.GONE);
        //setFavorite(menuItem);

        if (AuthUtil.getInstance().isSignedIn() && CacheUtil.getInstance().getFavorites().contains(menuItem.getId())) {
            holder.favoriteIcon.setImageResource(R.drawable.ic_heart);
            holder.favoriteIcon.setColorFilter(Color.parseColor("#F22424"));
            //holder.cardFav.setImageResource(R.drawable.ic_heart);
            //holder.cardFav.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F22424")));
        } else {
            holder.favoriteIcon.setImageResource(R.drawable.ic_heart_empty);
            holder.favoriteIcon.setColorFilter(Color.parseColor("#CCCCCC"));
            //holder.cardFav.setImageResource(R.drawable.ic_heart_empty);
            //holder.cardFav.setImageTintList(ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
        }

        holder.favoriteContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.setMenuItem(menuItem);
                MenuCategory menuCategory =presenter.getMenuCategoryFromSearch(menuItem);
                if (AuthUtil.getInstance().isSignedIn()) {
                    ((MainActivity) context).showLoadingDialog();
                    if (CacheUtil.getInstance().getFavorites().contains(menuItem.getId())) {
                        holder.favoriteIcon.setImageResource(R.drawable.ic_heart_empty);
                        holder.favoriteIcon.setColorFilter(Color.parseColor("#CCCCCC"));
                        //holder.cardFav.setImageResource(R.drawable.ic_heart_empty);
                        //holder.cardFav.setImageTintList(ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
                    } else {
                        holder.favoriteIcon.setImageResource(R.drawable.ic_heart);
                        holder.favoriteIcon.setColorFilter(Color.parseColor("#F22424"));
                        //holder.cardFav.setImageResource(R.drawable.ic_heart);
                        //holder.cardFav.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F22424")));
                    }
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

        holder.addToBag.setOnClickListener(v -> {
            //((MainActivity) itemView.getContext()).showLoadingDialog();
            ///presenter.setMenuCategory(menuCategory);
            presenter.setMenuItem(menuItem);
            //presenter.addCartMenuItem(itemImage);
        });

        holder.menuItemBottomButtons.setVisibility(View.VISIBLE);
        holder.container.setStrokeWidth(1);

        /*switch (CacheUtil.getInstance().getApplication().getAttributes().getHomeScreenDesign()){
            case "option-1":
                holder.menuItemBottomButtons.setVisibility(View.GONE);
                holder.container.setStrokeWidth(0);
                holder.container.setBackground(null);
                break;
            case "option-2":
                holder.menuItemBottomButtons.setVisibility(View.GONE);
                holder.container.setStrokeWidth(1);
                break;
            case "option-3":
                holder.menuItemBottomButtons.setVisibility(View.VISIBLE);
                holder.container.setStrokeWidth(1);
                break;
            default:
                holder.menuItemBottomButtons.setVisibility(View.GONE);
                holder.container.setStrokeWidth(0);
                holder.container.setBackground(null);
                break;
        }*/

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderData(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_menu_item_new_home, parent, false));
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