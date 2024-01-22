package com.herfy.newmenu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.newmenu.viewholder.MenuRecentViewHolder;

public class MenuRecentOrderRecyclerViewAdapter extends RecyclerView.Adapter<MenuRecentViewHolder> {

    private Context context;

    private final MenuContract.Presenter menuPresenter;
    private boolean isFromSearch;

    public MenuRecentOrderRecyclerViewAdapter(Context context, MenuContract.Presenter menuPresenter) {
        this.context = context;
        this.menuPresenter = menuPresenter;
    }

    @NonNull
    @Override
    public MenuRecentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MenuRecentViewHolder(context, menuPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_item_new_home, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MenuRecentViewHolder holder, int position) {
        menuPresenter.onBindRecentOrderViewAtPosition(position, holder);
    }

    @Override
    public int getItemCount() {
        return menuPresenter.recentOrdersCount();
    }

}
