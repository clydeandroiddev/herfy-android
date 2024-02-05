package com.herfy.newmenu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.newmenu.NewMenuTabListener;
import com.herfy.newmenu.TabModel;
import com.herfy.newmenu.views.MenuItemsFragment;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewMenuViewPagerAdapter extends FragmentStateAdapter {

    //private ArrayList<MenuCategory> menuCategories = new ArrayList<>();
    private MenuContract.Presenter presenter;
    private Context context;
    private NewMenuTabListener listener;
    private Map<Integer, MenuItemsFragment> fragmentList = new HashMap();
    private List<MenuCategory> menuCategories;
    public int selectedPosition = 0;

    public NewMenuViewPagerAdapter(@NonNull Fragment fragment, Context context, NewMenuTabListener listener) {
        super(fragment);
        this.context = context;
        this.listener = listener;
    }

    public NewMenuViewPagerAdapter(@NonNull FragmentManager fm, Lifecycle lifecycle, Context context, NewMenuTabListener listener) {
        super(fm, lifecycle);
        this.context = context;
        this.listener = listener;
    }

    /*@Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        //return menuCategories.get(position).getAttributes().getName();
        return presenter.getMenuCategoryByPosition(position).getAttributes().getName();
    }

    @Override
    public @NotNull Fragment getItem(int position) {
        MenuItemsFragment fragment = new MenuItemsFragment();
        //fragment.newInstance(menuCategories.get(position), presenter, listener);
        fragment.newInstance(context, position, presenter, listener);
        return fragment;
    }

    @Override
    public int getCount() {
        //return menuCategories.size();
        return presenter.getMenuCategoriesCount();
    }*/

    public void onResume() {
        for (Map.Entry<Integer, MenuItemsFragment> integerMenuItemsFragmentEntry : fragmentList.entrySet()) {
            integerMenuItemsFragmentEntry.getValue().onResume();
        }
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        //return null;
        MenuItemsFragment fragment = fragmentList.get(position);
        if (fragment == null) {
            fragment = new MenuItemsFragment();
            fragmentList.put(position, fragment);
        }
        fragment.newInstance(position, presenter, listener);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return presenter.getMenuCategoriesCount();
    }

    public void updatePresenter(MenuContract.Presenter presenter) {
        this.presenter = presenter;
    }

    public void updateCategories(List<MenuCategory> menuCategories) {
        boolean isDiff = false;
        // Deep equals
        for (int i = 0; i < menuCategories.size(); i++) {
            if (this.menuCategories == null || this.menuCategories.size() <= i || !this.menuCategories.get(i).getId().equals(menuCategories.get(i).getId())) {
                isDiff = true;
            }
            if(this.menuCategories != null && !this.menuCategories.isEmpty()) {
                if(!this.menuCategories.contains(menuCategories.get(i))) {
                    isDiff = true;
                    break;
                }
            }
        }

        if (isDiff) {
            if (this.menuCategories != null) {
                this.menuCategories.clear();
                this.menuCategories.addAll(menuCategories);
            } else {
                this.menuCategories = menuCategories;
            }
            notifyDataSetChanged();
        }
    }

    public TabModel getTabView(int position, boolean selected) {
        View view = null;
        if (!CacheUtil.getInstance().getApplication().getAttributes().getSettings().categoryDisplay.equalsIgnoreCase(Values.IMAGE_TEXT)) {
            view = LayoutInflater.from(context).inflate(R.layout.tab_category_old_text, null);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.tab_category_old, null);
        }
        CardView cardView = view.findViewById(R.id.category_image_card);
        cardView.setTag("" + position);
        TextView tabTitle = view.findViewById(R.id.tab_title);
        MenuCategory menuCategory = presenter.getMenuCategoryByPosition(position);
        String imageUrl = null;
        if(menuCategory != null) {
            imageUrl = menuCategory.getImage();
            tabTitle.setText(menuCategory.getName(LocaleUtil.getInstance().getLanguage()));
        }
        if (selected) {
            tabTitle.setTextColor(context.getResources().getColor(R.color.cod_gray));
        } else {
            tabTitle.setTextColor(context.getResources().getColor(R.color.silver_chalice));
        }
        ImageView tabImageView = view.findViewById(R.id.tab_image);

        if (imageUrl == null || imageUrl.equalsIgnoreCase("null") || imageUrl.isEmpty() || imageUrl.toLowerCase().contains("default-image.png")) {
            imageUrl = Values.DEFAULT_IMAGE_URL;
            Glide.with(context).load(R.color.transparent)
                    .skipMemoryCache(false)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transform(new CenterCrop(), new RoundedCorners(context.getResources().getDimensionPixelSize(R.dimen.category_icon_rounded_corner)))
                    .into(tabImageView);
        } else {
            imageUrl = StringUtil.getCloudflareUrl(Values.MENU_IMAGE_SIZE, Values.MENU_IMAGE_SIZE, imageUrl);
            Glide.with(context).load(imageUrl)
                    .skipMemoryCache(false)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transform(new CenterCrop(), new RoundedCorners(context.getResources().getDimensionPixelSize(R.dimen.category_icon_rounded_corner)))
                    .into(tabImageView);
        }

        TabModel model = new TabModel();
        model.card = cardView;
        model.tab = view;
        return model;

    }

    public void updateFragment(int position) {
        if (position < fragmentList.size() && fragmentList.get(position) != null) {
            fragmentList.get(position).onResume();
        }
    }
}
