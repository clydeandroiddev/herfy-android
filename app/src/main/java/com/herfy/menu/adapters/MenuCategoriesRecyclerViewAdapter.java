package com.herfy.menu.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.menu.viewholders.MenuCategoryViewHolder;

public class MenuCategoriesRecyclerViewAdapter extends RecyclerView.Adapter<MenuCategoryViewHolder> {

    private Context context;

    private final MenuContract.Presenter menuPresenter;

    private int selected = 0;

    public MenuCategoriesRecyclerViewAdapter(Context context, MenuContract.Presenter menuPresenter) {
        this.context = context;
        this.menuPresenter = menuPresenter;
    }

    @NonNull
    @Override
    public MenuCategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MenuCategoryViewHolder(context, menuPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_category, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MenuCategoryViewHolder holder, int position) {
         menuPresenter.onBindMenuCategoryViewAtPosition(position, holder, position == selected);
    }

    @Override
    public int getItemCount() {
        return menuPresenter.getMenuCategoriesCount();
    }

    public void setSelected(int selected) {
        this.selected = selected;
        notifyDataSetChanged();
    }

    public int getSelected() {
        return selected;
    }

}
