package com.herfy.menu.viewholders;


import static com.herfy.menu.viewholders.ModifierItemOptionViewHolder.isProbablyArabic;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.ModifierProcessor;
import com.herfy.util.ModifierProcessorCallback;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import butterknife.BindView;
import butterknife.ButterKnife;
public class ImageItemModifierHolder extends BaseModifierItemViewHolder implements ModifierProcessorCallback {
    @BindView(R.id.check)
    ImageView check;
    @BindView(R.id.check_layout)
    FrameLayout checkLayout;

    @BindView(R.id.image)
    ImageView image;

    @BindView(R.id.txtModifierItemName)
    TextView modifierNameAndPrice;
    @BindView(R.id.txtmodifierPrice)
    TextView txtModifierPrice;
    private OnAction action;
    private ModifierProcessor modifierProcessor;
    @Nullable
    LinearLayout clickableLayout;
    public ImageItemModifierHolder(Context context, MenuContract.Presenter menuPresenter, View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
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
    public void onModifierProcessed(@NonNull String name, @NonNull String formattedPrice, @NonNull String text) {
        setModifierNameAndPrice(name,formattedPrice,text);
    }
    private void setModifierNameAndPrice(String name, String formattedPrice,String text) {
        if (LocaleUtil.getInstance().isEnglish() || LocaleUtil.getInstance().isFrench()) {
            modifierNameAndPrice.setText(Html.fromHtml(name),  TextView.BufferType.SPANNABLE);
        } else {
            if (isProbablyArabic(name)) {
                modifierNameAndPrice.setText(Html.fromHtml(name), TextView.BufferType.SPANNABLE);
            } else {
                String modifiedText = text + " " + name;
                modifierNameAndPrice.setText(Html.fromHtml(modifiedText), TextView.BufferType.SPANNABLE);
            }
        }

        // Set the price text in all cases
        txtModifierPrice.setText(formattedPrice);
    }

}


