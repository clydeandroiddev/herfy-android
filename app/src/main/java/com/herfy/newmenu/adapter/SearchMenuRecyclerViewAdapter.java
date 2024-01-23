package com.herfy.newmenu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.newmenu.viewholder.SearchMenuViewHolder;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;

import java.util.ArrayList;

public class SearchMenuRecyclerViewAdapter extends RecyclerView.Adapter<SearchMenuViewHolder> {

    private Context mContext;
    private final MenuContract.Presenter menuPresenter;
    private ArrayList<MenuItem> searchedMenuItems;

    public SearchMenuRecyclerViewAdapter(Context context, MenuContract.Presenter menuPresenter) {
        this.mContext = context;
        this.menuPresenter = menuPresenter;
    }

    public void updateMenuList(ArrayList<MenuItem> searchedMenuItems) {
        this.searchedMenuItems = searchedMenuItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchMenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SearchMenuViewHolder(mContext, menuPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_item_new, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SearchMenuViewHolder holder, int position) {
        if (position >= 0 && position < searchedMenuItems.size()) {
            holder.setMenuItem(null, searchedMenuItems.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return searchedMenuItems.size();
    }

}

