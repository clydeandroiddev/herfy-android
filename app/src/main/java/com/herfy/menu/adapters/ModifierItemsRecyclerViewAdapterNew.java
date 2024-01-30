package com.herfy.menu.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.menu.viewholders.ModifierItemViewHolderNew;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;

public class ModifierItemsRecyclerViewAdapterNew extends RecyclerView.Adapter<ModifierItemViewHolderNew> {

    private Context mContext;

    private final MenuContract.Presenter menuPresenter;

    private ModifierGroup modifierGroup;

    private ModifierItemViewHolderNew.OnAction onAction;

    public void setOnAction(ModifierItemViewHolderNew.OnAction onAction) {
        this.onAction = onAction;
    }

    public ModifierItemsRecyclerViewAdapterNew(Context context, MenuContract.Presenter menuPresenter, ModifierGroup modifierGroup) {
        this.mContext = context;
        this.menuPresenter = menuPresenter;
        this.modifierGroup = modifierGroup;
    }

    @NonNull
    @Override
    public ModifierItemViewHolderNew onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ModifierItemViewHolderNew modifierItemViewHolder = new ModifierItemViewHolderNew(mContext, menuPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_modifier_group_new, parent, false));
        modifierItemViewHolder.setOnAction(onAction);
        return modifierItemViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ModifierItemViewHolderNew holder, int position) {
        menuPresenter.onBindModifierItemViewAtPositionNew(modifierGroup, position, holder);
    }

    @Override
    public int getItemCount() {
        return menuPresenter.getModifierItemsCount(modifierGroup);
    }
}
