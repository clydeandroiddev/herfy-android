package com.herfy.newmenu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.newmenu.viewholder.MenuFavoriteViewHolder;

public class MenuFavoritesRecyclerViewAdapter extends RecyclerView.Adapter<MenuFavoriteViewHolder> {

    private Context context;

    private final MenuContract.Presenter favoritesPresenter;
    private boolean isFromSearch;

    public MenuFavoritesRecyclerViewAdapter(Context context, MenuContract.Presenter favoritesPresenter) {
        this.context = context;
        this.favoritesPresenter = favoritesPresenter;
    }

    @NonNull
    @Override
    public MenuFavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MenuFavoriteViewHolder(context, favoritesPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_item_new_home, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MenuFavoriteViewHolder holder, int position) {
        favoritesPresenter.onBindFavoriteViewAtPosition(position, holder);
    }

    @Override
    public int getItemCount() {
        return favoritesPresenter.getFavoritesCount();
    }

}
