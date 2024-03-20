package com.herfy.menu.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.productpagedetails.databinding.ItemModifierDrinksTypeBinding;
import com.herfy.productpagedetails.databinding.ItemModifierOptionTypeBinding;
import com.herfy.productpagedetails.ui.viewholder.BaseModifierItemViewHolder;
import com.herfy.productpagedetails.ui.viewholder.ImageItemModifierHolder;
import com.herfy.productpagedetails.ui.viewholder.ModifierItemOptionViewHolder;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupType;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;

import java.util.List;

public class ModifierItemsNewRecyclerViewAdapter extends RecyclerView.Adapter<BaseModifierItemViewHolder> {

    private Context mContext;

    private final MenuContract.Presenter menuPresenter;

    private int modifierGroupPosition;
    private ModifierGroup modifierGroup;
    private boolean showModifierItemPrice;
    private List<ModifierItem> modifierItems;

    private int ITEM_MODIFIER_OPTION_TYPE = 1 ;
    private int ITEM_WITHOUT_DESCRIPTION_VIEW_TYPE = 2;
    private int ITEM_MODIFIER_OPTIONS_VIEW_TYPE = 3 ;
    private int HIDDEN_VIEW_TYPE = 4;
    private BaseModifierItemViewHolder.OnAction onAction;
    private boolean dataUpdatedFlag = false;


    public ModifierItemsNewRecyclerViewAdapter(Context context, MenuContract.Presenter menuPresenter, int modifierGroupPosition, ModifierGroup modifierGroup, boolean showModifierItemPrice) {
        this.mContext = context;
        this.menuPresenter = menuPresenter;
        this.modifierGroupPosition = modifierGroupPosition;
        this.modifierGroup = modifierGroup;
        this.showModifierItemPrice = showModifierItemPrice;
    }


    public void setOnAction(BaseModifierItemViewHolder.OnAction onAction) {
        this.onAction = onAction;
    }

    @NonNull
    @Override
    public BaseModifierItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == ITEM_MODIFIER_OPTION_TYPE) {
            ItemModifierOptionTypeBinding binding = ItemModifierOptionTypeBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
            ModifierItemOptionViewHolder modifierTypeOption = new  ModifierItemOptionViewHolder(binding);
            modifierTypeOption.setAction(onAction);
            return modifierTypeOption;
        } else if (viewType == ITEM_WITHOUT_DESCRIPTION_VIEW_TYPE) {
            ItemModifierDrinksTypeBinding binding = ItemModifierDrinksTypeBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
            ImageItemModifierHolder holder = new ImageItemModifierHolder(binding);
            holder.setAction(onAction);
            return holder;
        }  else {
            throw new UnsupportedOperationException("Invalid view type " + viewType);
        }
    }
    @Override
    public void onBindViewHolder(@NonNull BaseModifierItemViewHolder holder, int position) {
        menuPresenter.onBindModifierItemViewAtPosition(modifierGroupPosition, modifierGroup, showModifierItemPrice, position, holder);
    }

    @Override
    public int getItemViewType(int position) {
        ModifierGroupType modifierGroupType = menuPresenter.getModifierGroupTypeByPosition(position,modifierGroup);
        return determineModifierItemViewType(modifierGroupType);
    }

    private int determineModifierItemViewType(ModifierGroupType type) {
        int modifierItemSize = menuPresenter.getModifierItemsCount(modifierGroup);
        if (isOptionModifierItem(type, modifierItemSize)) {
            return ITEM_MODIFIER_OPTION_TYPE;
        } else if (isDrinkModifierItem(type, modifierItemSize)) {
            return ITEM_WITHOUT_DESCRIPTION_VIEW_TYPE;
        }  else {
            return HIDDEN_VIEW_TYPE;
        }
    }

    private boolean isDrinkModifierItem(ModifierGroupType type, int modifierItemSize) {
        return modifierItemSize > 3 && type == ModifierGroupType.DRINKS;
    }

    private boolean isOptionModifierItem(ModifierGroupType type, int modifierItemSize) {
        return type == ModifierGroupType.OPTIONS && modifierItemSize >= 1;
    }


    @Override
    public int getItemCount() {
        return menuPresenter.getModifierItemsCount(modifierGroup);
    }

}
