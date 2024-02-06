package com.herfy.newmenu.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.databinding.ItemMenuItemNewBinding;
import com.herfy.menu.MenuContract;
import com.herfy.newmenu.viewholder.NewMenuViewHolder;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class NewMenuRecyclerViewAdapter extends RecyclerView.Adapter<NewMenuViewHolder> implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context mContext;
    private final MenuContract.Presenter menuPresenter;
    private MenuCategory menuCategory;
    private int position = 0;
    private List<MenuItem> menuItems = new ArrayList<>();

    public NewMenuRecyclerViewAdapter(Context context, int position, MenuCategory menuCategory, MenuContract.Presenter menuPresenter) {
        this.mContext = context;
        this.position = position;
        this.menuPresenter = menuPresenter;
        this.menuCategory = menuCategory;
    }

    public void updateItems() {
        notifyDataSetChanged();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equalsIgnoreCase("Favorites")) {
            MenuCategory presenterMenuCategory = menuPresenter.getMenuCategoryByPosition(this.position);
            boolean adapterContainsMenuItemChanged = false;
            if (presenterMenuCategory != null && menuPresenter.getMenuItem() != null) {
                for (MenuItem menuItem : presenterMenuCategory.getMenuItems()) {
                    if (menuItem.getId().equals(menuPresenter.getMenuItem().getId())) {
                        adapterContainsMenuItemChanged = true;
                        break;
                    }
                }
            }
            if (adapterContainsMenuItemChanged) {
                (new Handler()).postDelayed((Runnable) () -> {
                    notifyDataSetChanged();
                }, 250); // Delay to let notifydatasetchange from adapter run before this
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        CacheUtil.getInstance().getSharedPrefs().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        CacheUtil.getInstance().getSharedPrefs().unregisterOnSharedPreferenceChangeListener(this);
        super.onDetachedFromRecyclerView(recyclerView);
    }

    /*public NewMenuRecyclerViewAdapter(Context context, MenuCategory menuCategory, MenuContract.Presenter menuPresenter) {
        this.mContext = context;
        this.menuPresenter = menuPresenter;
        this.menuCategory = menuCategory;
    }*/

    @NonNull
    @Override
    public NewMenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NewMenuViewHolder(mContext, menuPresenter, ItemMenuItemNewBinding.inflate( LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull NewMenuViewHolder holder, int position) {
        /*if (menuCategory != null) {
            holder.setMenuItem(menuCategory, menuCategory.getMenuItems().get(position));
        }*/
        MenuCategory presenterMenuCategory = menuPresenter.getMenuCategoryByPosition(this.position);
        if (presenterMenuCategory != null) {
            if(position < presenterMenuCategory.getMenuItems().size()) {
                if (CacheUtil.getOrderStoreId() == null) {
                    holder.setMenuItem(presenterMenuCategory, presenterMenuCategory.getMenuItems().get(position));
                } else {
                    holder.setMenuItem(presenterMenuCategory, removeItemsWithExcludedLocation(presenterMenuCategory.getMenuItems()).get(position));
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        MenuCategory category = menuPresenter.getMenuCategoryByPosition(position);
        if(category != null) {
            if (CacheUtil.getOrderStoreId() != null) {
                return removeItemsWithExcludedLocation(category.getMenuItems()).size();
            } else {
                return category.getMenuItems().size();
            }
        }
        return 0;
    }

    private List<MenuItem> removeItemsWithExcludedLocation( List<MenuItem> items) {
         if (!menuItems.isEmpty())
             menuItems.clear();

         String orderId = "";
         if(CacheUtil.getOrderStoreId() != null){
             orderId = CacheUtil.getOrderStoreId();
             for(MenuItem menuItem : items) {
                 if(!orderId.isEmpty() && !menuItem.getExcludedLocations().contains(orderId)) {
                     menuItems.add(menuItem);
                 }
             }
         }
         return menuItems;
    }

    public void updateMenuCategory(MenuCategory menuCategory) {
        this.menuCategory = menuCategory;
        notifyDataSetChanged();
    }

}

