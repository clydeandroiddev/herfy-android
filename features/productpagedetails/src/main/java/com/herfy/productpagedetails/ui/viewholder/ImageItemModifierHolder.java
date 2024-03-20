package com.herfy.productpagedetails.ui.viewholder;


import static com.herfy.productpagedetails.ui.viewholder.ModifierItemOptionViewHolder.isProbablyArabic;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.herfy.productpagedetails.R;
import com.herfy.productpagedetails.databinding.ItemModifierDrinksTypeBinding;
import com.herfy.productpagedetails.databinding.ItemModifierOptionTypeBinding;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.utils.helper.ModifierProcessor;
import com.herfy.utils.helper.ModifierProcessorCallback;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;



public class ImageItemModifierHolder extends BaseModifierItemViewHolder implements ModifierProcessorCallback {
    ImageView check;
    FrameLayout checkLayout;
    ImageView image;

    TextView modifierNameAndPrice;
    TextView txtModifierPrice;
    private OnAction action;
    private ModifierProcessor modifierProcessor;
    private ItemModifierDrinksTypeBinding binding;
    @Nullable
    LinearLayout clickableLayout;
    public ImageItemModifierHolder(ItemModifierDrinksTypeBinding binding){
        super(binding.getRoot());
        this.binding = binding;
        check = this.binding.check;
        checkLayout = this.binding.checkLayout;
        image = this.binding.image;
        modifierNameAndPrice = this.binding.txtModifierItemName;
        txtModifierPrice = this.binding.txtmodifierPrice;
        modifierProcessor = new ModifierProcessor(this);
    }


    @Override
    public ImageView getModifierImageView() {
        return image;
    }

    @Override
    public TextView getModifierNameTextView() {
        return modifierNameAndPrice;
    }

    @Override
    public TextView getModifierPriceTextView() {
        return txtModifierPrice;
    }

    @Override
    public ImageView getCheckBox() {
        return check;
    }

    @Override
    public void setAction(OnAction onAction) {
        this.action = onAction;
    }

    @Override
    public void setModifierItem(int modifierGroupPosition, ModifierGroup modifierGroup, int modifierItemPosition, ModifierItem modifierItem, boolean showModifierItemPrice) {
        modifierProcessor.processModifier(modifierItem);
        if (modifierItem.getAttributes().isSelected()) {
            check.setImageResource(R.drawable.modifier_radio_item_selected);
        } else {
            check.setImageResource(R.drawable.modifier_item_radio_unselected);
        }

    }

    @Override
    public void onModifierProcessed(@NotNull String name, @NotNull String formattedPrice) {
        setModifierNameAndPrice(name,formattedPrice);
    }
    private void setModifierNameAndPrice(String name, String formattedPrice) {
        if (LocaleUtil.getInstance().isEnglish() || LocaleUtil.getInstance().isFrench()) {
            modifierNameAndPrice.setText(Html.fromHtml(name),  TextView.BufferType.SPANNABLE);
        } else {
            if (isProbablyArabic(name)) {
                modifierNameAndPrice.setText(Html.fromHtml(name), TextView.BufferType.SPANNABLE);
            } else {
                modifierNameAndPrice.setText(Html.fromHtml(name), TextView.BufferType.SPANNABLE);
            }
        }

        // Set the price text in all cases
        txtModifierPrice.setText(formattedPrice);
    }

}


