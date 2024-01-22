package com.herfy.menu.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.menu.viewholders.IngredientViewHolder;

public class IngredientsRecyclerViewAdapter extends RecyclerView.Adapter<IngredientViewHolder> {

    private Context mContext;

    private final MenuContract.Presenter menuPresenter;

    private IngredientViewHolder.OnCheckedChanged onCheckedChanged;

    public void setOnCheckedChanged(IngredientViewHolder.OnCheckedChanged onCheckedChanged) {
        this.onCheckedChanged = onCheckedChanged;
    }

    public IngredientsRecyclerViewAdapter(Context context, MenuContract.Presenter menuPresenter) {
        this.mContext = context;
        this.menuPresenter = menuPresenter;
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        IngredientViewHolder ingredientViewHolder = new IngredientViewHolder(mContext, menuPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ingredient, parent, false));
        ingredientViewHolder.setOnCheckedChanged(onCheckedChanged);
        return ingredientViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
         menuPresenter.onBindIngredientViewAtPosition(position, holder);
    }

    @Override
    public int getItemCount() {
        return menuPresenter.getIngredientsCount();
    }
}
