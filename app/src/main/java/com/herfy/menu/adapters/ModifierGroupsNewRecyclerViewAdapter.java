package com.herfy.menu.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.menu.viewholders.BaseModifierItemViewHolder;
import com.herfy.menu.viewholders.ModifierNewGroupViewHolder;

import java.util.HashMap;

public class ModifierGroupsNewRecyclerViewAdapter extends RecyclerView.Adapter<ModifierNewGroupViewHolder> {

    private Context context;

    private final MenuContract.Presenter menuPresenter;
    private HashMap<Integer, Boolean> showModifierItemPrice = new HashMap<>();
    private HashMap<Integer, Boolean> shouldShowModifierGroupCheckbox = new HashMap<>();

    private HashMap<Integer, Boolean> shouldShowQuantity = new HashMap<>();
    BaseModifierItemViewHolder.OnAction onAction;

    public void setOnAction(BaseModifierItemViewHolder.OnAction  onAction) {
        this.onAction = onAction;
    }
    public ModifierGroupsNewRecyclerViewAdapter(Context context, MenuContract.Presenter menuPresenter, HashMap<Integer, Boolean> showModifierItemPrice,
                                                HashMap<Integer, Boolean> shouldShowModifierGroupCheckbox) {
        this.context = context;
        this.menuPresenter = menuPresenter;
        this.showModifierItemPrice = showModifierItemPrice;
        this.shouldShowModifierGroupCheckbox = shouldShowModifierGroupCheckbox;
    }

    @NonNull
    @Override
    public ModifierNewGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ModifierNewGroupViewHolder modifierGroupViewHolder = new ModifierNewGroupViewHolder(context, menuPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_modifier_group_new, parent, false));
        modifierGroupViewHolder.setOnAction(onAction);
        return modifierGroupViewHolder;


    }

    @Override
    public void onBindViewHolder(@NonNull ModifierNewGroupViewHolder holder, int position) {
        menuPresenter.onBindModifierGroupViewAtPosition(position, holder, showModifierItemPrice.get(position),shouldShowModifierGroupCheckbox.get(position),shouldShowQuantity);
    }

    @Override
    public int getItemCount() {
        return menuPresenter.getModifierGroupsCount();
    }

    public void showModifierItemPrice(boolean show, int position) {
        showModifierItemPrice.put(position, show);
        notifyItemChanged(position);
    }
    public void shouldShowCheckBox(boolean show, int position) {
        shouldShowModifierGroupCheckbox.put(position, show);
        notifyItemChanged(position);
    }
    public void shouldShowQuantity(boolean show, int position) {
        shouldShowQuantity.put(position, show);
        notifyItemChanged(position);
    }
}
