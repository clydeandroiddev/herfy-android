package com.herfy.menu.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.menu.viewholders.ModifierItemViewHolder;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;

public class ModifierItemsRecyclerViewAdapter extends RecyclerView.Adapter<ModifierItemViewHolder> {

    private Context mContext;

    private final MenuContract.Presenter menuPresenter;

    private int modifierGroupPosition;
    private ModifierGroup modifierGroup;
    private boolean showModifierItemPrice;

    private ModifierItemViewHolder.OnAction onAction;

    public void setOnAction(ModifierItemViewHolder.OnAction onAction) {
        this.onAction = onAction;
    }

    public ModifierItemsRecyclerViewAdapter(Context context, MenuContract.Presenter menuPresenter, int modifierGroupPosition, ModifierGroup modifierGroup, boolean showModifierItemPrice) {
        this.mContext = context;
        this.menuPresenter = menuPresenter;
        this.modifierGroupPosition = modifierGroupPosition;
        this.modifierGroup = modifierGroup;
        this.showModifierItemPrice = showModifierItemPrice;
    }

    @NonNull
    @Override
    public ModifierItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        ModifierItemViewHolder modifierItemViewHolder = new ModifierItemViewHolder(mContext, menuPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_modifier_item, parent, false));
        modifierItemViewHolder.setOnAction(onAction);

        return modifierItemViewHolder;

    }

    @Override
    public void onBindViewHolder(@NonNull ModifierItemViewHolder holder, int position) {
        menuPresenter.onBindModifierItemViewAtPosition(modifierGroupPosition, modifierGroup, showModifierItemPrice, position, holder);
    }

    @Override
    public int getItemCount() {
        return menuPresenter.getModifierItemsCount(modifierGroup);
    }
}
