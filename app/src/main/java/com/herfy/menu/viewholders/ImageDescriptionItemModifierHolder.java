package com.herfy.menu.viewholders;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.textview.MaterialTextView;
import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.util.LocaleUtil;
import com.herfy.util.StringUtil;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ImageDescriptionItemModifierHolder extends BaseModifierItemViewHolder {
    private Context context;

    private MenuContract.Presenter menuPresenter;

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
    @BindView(R.id.clickable_layout)
    LinearLayout clickableLayout;
    @BindView(R.id.modifierDescription)
    MaterialTextView modifierDescription;
    private OnAction onAction;



    public void setOnAction(OnAction onAction) {
        this.onAction = onAction;
    }

    public ImageDescriptionItemModifierHolder(Context context, MenuContract.Presenter menuPresenter, View itemView) {

        super(itemView);

        ButterKnife.bind(this, itemView);

        this.context = context;
        this.menuPresenter = menuPresenter;

    }


    @Override
    ImageView getModifierImageView() {
        return image;
    }

    @Override
    TextView getModifierNameTextView() {
        return modifierNameAndPrice;
    }

    @Override
    TextView getModifierPriceTextView() {
        return txtModifierPrice;
    }

    @Override
    ImageView getCheckBox() {
        return check;
    }

    @Override
    public void setAction(OnAction onAction) {

    }

    @Override
    public void setModifierItem(int modifierGroupPosition, ModifierGroup modifierGroup, int modifierItemPosition, ModifierItem modifierItem, boolean showModifierItemPrice) {
        if (modifierItem.getAttributes().isSelected()) {
            check.setImageResource(R.drawable.modifier_item_selected);
        } else {
            check.setImageResource(R.drawable.modifier_item_unselected);
        }

        checkLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onAction != null && modifierItem.getAttributes().getMinimum() != 1) {
                    if (modifierItem.getAttributes().isSelected()) {
                        onAction.onCheckedChanged(false, modifierGroupPosition, modifierGroup, modifierItemPosition, modifierItem);
                    } else {
                        onAction.onCheckedChanged(true, modifierGroupPosition, modifierGroup, modifierItemPosition, modifierItem);
                    }
                }
            }
        });
        String price;
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
        //hamburgini

            modifierNameAndPrice.setText(Html.fromHtml(modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage())), TextView.BufferType.SPANNABLE);
            txtModifierPrice.setText(price);
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



    public static boolean isProbablyArabic(String s) {
        for (int i = 0; i < s.length(); ) {
            int c = s.codePointAt(i);
            if (c >= 0x0600 && c <= 0x06E0)
                return true;
            i += Character.charCount(c);
        }
        return false;
    }

}