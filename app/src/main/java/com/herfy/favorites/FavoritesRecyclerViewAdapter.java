package com.herfy.favorites;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;

public class FavoritesRecyclerViewAdapter extends RecyclerView.Adapter<FavoriteViewHolder> {

    private final FavoritesContract.Presenter favoritesPresenter;
    private boolean isFromSearch;

    public FavoritesRecyclerViewAdapter(FavoritesContract.Presenter favoritesPresenter) {
        this.favoritesPresenter = favoritesPresenter;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FavoriteViewHolder(favoritesPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_new_favourite, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        favoritesPresenter.onBindFavoriteViewAtPosition(position, holder);
    }

    @Override
    public int getItemCount() {
        return favoritesPresenter.getFavoritesCount();
    }

}
