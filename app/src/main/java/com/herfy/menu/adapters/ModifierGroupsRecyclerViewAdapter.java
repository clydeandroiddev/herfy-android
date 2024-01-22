package com.herfy.menu.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.menu.viewholders.ModifierGroupViewHolder;
import com.herfy.menu.viewholders.ModifierItemViewHolder;

import java.util.HashMap;

public class ModifierGroupsRecyclerViewAdapter extends RecyclerView.Adapter<ModifierGroupViewHolder> {

    private Context context;

    private final MenuContract.Presenter menuPresenter;
    private HashMap<Integer, Boolean> showModifierItemPrice = new HashMap<>();

    private ModifierItemViewHolder.OnAction onAction;

    public void setOnAction(ModifierItemViewHolder.OnAction onAction) {
        this.onAction = onAction;
    }

    public ModifierGroupsRecyclerViewAdapter(Context context, MenuContract.Presenter menuPresenter, HashMap<Integer, Boolean> showModifierItemPrice) {
        this.context = context;
        this.menuPresenter = menuPresenter;
        this.showModifierItemPrice = showModifierItemPrice;
    }

    @NonNull
    @Override
    public ModifierGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        ModifierGroupViewHolder modifierGroupViewHolder = new ModifierGroupViewHolder(context, menuPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_modifier_group, parent, false));
        modifierGroupViewHolder.setOnAction(onAction);
        return modifierGroupViewHolder;


    }

    @Override
    public void onBindViewHolder(@NonNull ModifierGroupViewHolder holder, int position) {
        menuPresenter.onBindModifierGroupViewAtPosition(position, holder, showModifierItemPrice.get(position));
    }

    @Override
    public int getItemCount() {
        return menuPresenter.getModifierGroupsCount();
    }

    public void showModifierItemPrice(boolean show, int position) {
        showModifierItemPrice.put(position, show);
        notifyItemChanged(position);
    }

}
