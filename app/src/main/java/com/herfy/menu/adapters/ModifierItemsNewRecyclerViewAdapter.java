package com.herfy.menu.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.menu.viewholders.BaseModifierItemViewHolder;
import com.herfy.menu.viewholders.ImageDescriptionItemModifierHolder;
import com.herfy.menu.viewholders.ImageItemModifierHolder;
import com.herfy.menu.viewholders.ModifierItemOptionViewHolder;
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

    private int ITEM_WITH_IMAGE_AND_DESCRIPTION_VIEW_TYPE = 1 ;
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
        View view = createViewByType(viewType, parent);
        if (viewType == ITEM_WITH_IMAGE_AND_DESCRIPTION_VIEW_TYPE) {
            return new ImageDescriptionItemModifierHolder(mContext, menuPresenter, view);
        } else if (viewType == ITEM_WITHOUT_DESCRIPTION_VIEW_TYPE) {
            ImageItemModifierHolder holder = new ImageItemModifierHolder(mContext, menuPresenter, view);
            holder.setAction(onAction);
            return holder;
        } else if (viewType == ITEM_MODIFIER_OPTIONS_VIEW_TYPE) {
            ModifierItemOptionViewHolder holder = new ModifierItemOptionViewHolder(mContext, menuPresenter, view);
            holder.setAction(onAction);
            return holder;
        } else {
            throw new UnsupportedOperationException("Invalid view type " + viewType);
        }
    }
    public void updateModifierMenuItem(List<ModifierItem> modifierItems) {
        this.modifierItems = modifierItems;
        notifyDataSetChanged();
    }
    public void updateModifierItemByPosition(int modifierGroupPosition) {
        notifyItemChanged(modifierGroupPosition);
    }
    @Override
    public void onBindViewHolder(@NonNull BaseModifierItemViewHolder holder, int position) {
        menuPresenter.onBindModifierItemViewAtPosition(modifierGroupPosition, modifierGroup, showModifierItemPrice, position, holder);
    }


    private View createViewByType(int viewType, ViewGroup parent) {
        int layoutResourceId = getLayoutResourceIdByType(viewType);
        return inflateView(layoutResourceId, parent);
    }

    private int getLayoutResourceIdByType(int viewType) {
        if (viewType == ITEM_WITH_IMAGE_AND_DESCRIPTION_VIEW_TYPE) {
            return R.layout.item_modifier_item_with_description;
        } else if (viewType == ITEM_WITHOUT_DESCRIPTION_VIEW_TYPE) {
            return R.layout.item_modifier_item_without_description;
        } else if (viewType == ITEM_MODIFIER_OPTIONS_VIEW_TYPE) {
            return R.layout.item_modifier_without_image_and_description;
        } else {
            throw new UnsupportedOperationException("Invalid view type " + viewType);
        }
    }

    private View inflateView(int layoutResourceId, ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(layoutResourceId, parent, false);
    }


    @Override
    public int getItemViewType(int position) {
        ModifierGroupType modifierGroupType = menuPresenter.getModifierGroupTypeByPosition(position,modifierGroup);
        return determineModifierItemViewType(modifierGroupType);
    }

    private int determineModifierItemViewType(ModifierGroupType type) {
        int modifierItemSize = menuPresenter.getModifierItemsCount(modifierGroup);

        if (isSizeModifierItem(type, modifierItemSize)) {
            return ITEM_WITH_IMAGE_AND_DESCRIPTION_VIEW_TYPE;
        } else if (isDrinkModifierItem(type, modifierItemSize)) {
            return ITEM_WITHOUT_DESCRIPTION_VIEW_TYPE;
        } else if (isOptionModifierItem(type, modifierItemSize)) {
            return ITEM_MODIFIER_OPTIONS_VIEW_TYPE;
        } else {
            return HIDDEN_VIEW_TYPE;
        }
    }

    private boolean isSizeModifierItem(ModifierGroupType type, int modifierItemSize) {
        return (modifierItemSize == 2 || modifierItemSize == 3) && type == ModifierGroupType.SIZES;
    }

    private boolean isDrinkModifierItem(ModifierGroupType type, int modifierItemSize) {
        return modifierItemSize > 3 && type == ModifierGroupType.DRINKS;
    }

    private boolean isOptionModifierItem(ModifierGroupType type, int modifierItemSize) {
        return type == ModifierGroupType.OPTIONS && modifierItemSize >= 3;
    }


    @Override
    public int getItemCount() {
        return menuPresenter.getModifierItemsCount(modifierGroup);
    }

}
