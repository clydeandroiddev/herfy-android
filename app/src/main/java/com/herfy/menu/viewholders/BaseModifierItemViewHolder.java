package com.herfy.menu.viewholders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;

import java.util.HashMap;

public abstract class BaseModifierItemViewHolder extends RecyclerView.ViewHolder {
    public BaseModifierItemViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    abstract ImageView getModifierImageView();
    abstract TextView getModifierNameTextView();
    abstract TextView getModifierPriceTextView();
    abstract ImageView getCheckBox();
     protected OnAction action;
    public void setAction(OnAction onAction) {
        this.action = onAction;
    }
    public abstract void setModifierItem(int modifierGroupPosition, ModifierGroup modifierGroup, int modifierItemPosition, ModifierItem modifierItem, boolean showModifierItemPrice, HashMap<Integer, Boolean> shouldShowQuantity);
    public interface OnAction {
        void onCheckedChanged(boolean isSelected, int modifierGroupPosition, ModifierGroup modifierGroup, int modifierItemPosition, ModifierItem modifierItem);

        void onQuantityChanged(boolean add, int modifierGroupPosition, ModifierGroup modifierGroup, int modifierItemPosition, ModifierItem modifierItem, boolean isQuantityLayoutShow);
        void onQuantityChangedWithoutVisible(boolean add, int modifierGroupPosition, ModifierGroup modifierGroup, int modifierItemPosition, ModifierItem modifierItem);
    }

}




