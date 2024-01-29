package com.herfy.menu.viewholders;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.util.CacheUtil;
import com.herfy.util.LocaleUtil;
import com.herfy.util.StringUtil;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ModifierItemOptionViewHolder extends BaseModifierItemViewHolder {
    @BindView(R.id.check)
    ImageView checkBox;
    @BindView(R.id.txtModifierItemName)
    TextView modifierItemName;
    @BindView(R.id.txtmodifierPrice)
    TextView modifierPrice;




    @BindView(R.id.check_layout)
    FrameLayout checkLayout;


    public ModifierItemOptionViewHolder(Context context, MenuContract.Presenter menuPresenter, View itemView) {
        super(itemView);

        ButterKnife.bind(this, itemView);
    }



    @Override
    public ImageView getModifierImageView() {
        return null;
    }

    @Override
    public TextView getModifierNameTextView() {
        return modifierItemName;
    }

    @Override
    public TextView getModifierPriceTextView() {
        return modifierPrice;
    }

    @Override
    public ImageView getCheckBox() {
        return checkBox;
    }

    @Override
    public void setAction(OnAction onAction) {
        this.action = onAction;
        }


    @Override
    public void setModifierItem(int modifierGroupPosition, ModifierGroup modifierGroup, int modifierItemPosition, ModifierItem modifierItem, boolean showModifierItemPrice) {
        getModifierNameTextView().setText(Html.fromHtml(modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage())), TextView.BufferType.SPANNABLE);
        if (modifierItem.getAttributes().isSelected()) {
            checkBox.setImageResource(R.drawable.modifier_item_selected);
        } else {
            checkBox.setImageResource(R.drawable.modifier_item_unselected);
        }
        checkLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (action != null && modifierItem.getAttributes().getMinimum() != 1) {
                    if (modifierItem.getAttributes().isSelected()) {
                        action.onCheckedChanged(false, modifierGroupPosition, modifierGroup, modifierItemPosition, modifierItem);
                    } else {
                        action.onCheckedChanged(true, modifierGroupPosition, modifierGroup, modifierItemPosition, modifierItem);
                    }
                }
            }
        });
       double modifierPrice = modifierItem.getAttributes().getPrice();
        getModifierPriceTextView().setText(modifierPrice == 0
                ? CacheUtil.getInstance().getTranslations("free_caps", "Free")
                : StringUtil.getFormattedPrice(modifierPrice));
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
