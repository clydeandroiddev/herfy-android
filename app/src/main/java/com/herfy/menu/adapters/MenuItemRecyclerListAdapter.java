package com.herfy.menu.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.dinuscxj.pullzoom.PullZoomRecyclerView;
import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.menu.viewholders.MenuItemDetailsViewHolder;
import com.herfy.menu.viewholders.MenuItemImageViewHolder;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;

public class MenuItemRecyclerListAdapter extends RecyclerListAdapter {

    private MenuContract.Presenter menuPresenter;

    public MenuItemRecyclerListAdapter(Context context, PullZoomRecyclerView recyclerView, MenuContract.Presenter menuPresenter) {
        this.menuPresenter = menuPresenter;
        addViewType(TYPE_HEADER, new ViewHolderFactory<MenuItemImageViewHolder>() {
            @Override
            public MenuItemImageViewHolder onCreateViewHolder(ViewGroup parent) {
                return new MenuItemImageViewHolder(LayoutInflater.from(context).inflate(R.layout.item_menu_item_image, parent, false), context, recyclerView, menuPresenter);
            }
        });
        addViewType(MenuItem.class, new ViewHolderFactory<MenuItemDetailsViewHolder>() {
            @Override
            public MenuItemDetailsViewHolder onCreateViewHolder(ViewGroup parent) {
                return new MenuItemDetailsViewHolder(LayoutInflater.from(context).inflate(R.layout.item_menu_item_details, parent, false), context, menuPresenter);
            }
        });
    }


    @Override
    public Object getItem(int position) {
        if (position == 0) {
            return ITEM_HEADER;
        }
        return menuPresenter.getMenuItem();
    }

    @Override
    public int getItemCount() {
        return 2;
    }

}
