package com.herfy.newmenu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.newmenu.viewholder.MenuPopularViewHolder;

public class MenuPopularRecyclerViewAdapter extends RecyclerView.Adapter<MenuPopularViewHolder> {

    private Context context;

    private final MenuContract.Presenter menuPresenter;
    private boolean isFromSearch;

    public MenuPopularRecyclerViewAdapter(Context context, MenuContract.Presenter menuPresenter) {
        this.context = context;
        this.menuPresenter = menuPresenter;
    }

    @NonNull
    @Override
    public MenuPopularViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MenuPopularViewHolder(context, menuPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_item_new_home, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MenuPopularViewHolder holder, int position) {
        menuPresenter.onBindPopularViewAtPosition(position, holder);
    }

    @Override
    public int getItemCount() {
        return menuPresenter.bestSellersCount();
    }

}
