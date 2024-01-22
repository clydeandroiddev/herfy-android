package com.herfy.menu.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.menu.viewholders.MenuItemViewHolder;
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory;

public class MenuItemsRecyclerViewAdapter extends RecyclerView.Adapter<MenuItemViewHolder> {

    private Context mContext;

    private final MenuContract.Presenter menuPresenter;

    private MenuCategory menuCategory;

    public MenuItemsRecyclerViewAdapter(Context context, MenuCategory menuCategory, MenuContract.Presenter menuPresenter) {
        this.mContext = context;
        this.menuPresenter = menuPresenter;
        this.menuCategory = menuCategory;
    }

    @NonNull
    @Override
    public MenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MenuItemViewHolder(mContext, menuPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MenuItemViewHolder holder, int position) {
         menuPresenter.onBindMenuItemViewAtPosition(menuCategory, position, holder);
    }

    @Override
    public int getItemCount() {
        return menuPresenter.getMenuItemsCount(menuCategory);
    }

    public void updateMenuCategory(MenuCategory menuCategory) {
        this.menuCategory = menuCategory;
        notifyDataSetChanged();
    }

}
