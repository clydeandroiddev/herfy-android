package com.herfy.menu.viewholders;


import static com.herfy.menu.viewholders.ModifierItemOptionViewHolder.isProbablyArabic;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.util.LocaleUtil;
import com.herfy.util.StringUtil;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import butterknife.BindView;
import butterknife.ButterKnife;

public class ImageItemModifierHolder extends BaseModifierItemViewHolder {
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
    @Nullable
    LinearLayout clickableLayout;
    public ImageItemModifierHolder(Context context, MenuContract.Presenter menuPresenter, View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
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
        if (modifierItem.getAttributes().isSelected()) {
            check.setImageResource(R.drawable.modifier_item_selected);
        } else {
            check.setImageResource(R.drawable.modifier_item_unselected);
        }

        String price;
        //hamburgini

            price = String.valueOf(StringUtil.getFormattedPrice(modifierItem.getAttributes().getPrice()));
            if (LocaleUtil.getInstance().isEnglish() || LocaleUtil.getInstance().isFrench()) {
                price = ("+" + StringUtil.getFormattedPrice(modifierItem.getAttributes().getPrice()));
            } else {
                if (isProbablyArabic(modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()))) {
                    price = String.valueOf(StringUtil.getFormattedPriceWithText(modifierItem.getAttributes().getPrice()));
                } else {
                    price = String.valueOf(StringUtil.getFormattedPrice(modifierItem.getAttributes().getPrice()));
                }
            }


        String text = "<font color='#D62300'>" + price + "</font>";
        if (modifierItem.getAttributes().getPrice() <= 0) {
            text = "";
        }

            if (LocaleUtil.getInstance().isEnglish() || LocaleUtil.getInstance().isFrench()) {
                modifierNameAndPrice.setText(Html.fromHtml(modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage())), TextView.BufferType.SPANNABLE);
                txtModifierPrice.setText(price);
            } else {
                if (isProbablyArabic(modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()))) {
                    modifierNameAndPrice.setText(Html.fromHtml(modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage())), TextView.BufferType.SPANNABLE);
                    txtModifierPrice.setText(price);
                } else {

                    modifierNameAndPrice.setText(Html.fromHtml(text + " " + modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage())), TextView.BufferType.SPANNABLE);
                    txtModifierPrice.setText(price);
                }
            }
        }
    }


