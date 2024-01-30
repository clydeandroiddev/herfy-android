package com.herfy.menu.viewholders;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.productpagedetails.ui.viewholder.util.ArabicUtils;
import com.herfy.util.CacheUtil;
import com.herfy.util.LocaleUtil;
import com.herfy.util.ModifierProcessor;
import com.herfy.util.ModifierProcessorCallback;
import com.herfy.util.StringUtil;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ModifierItemOptionViewHolder extends BaseModifierItemViewHolder implements ModifierProcessorCallback {
    @BindView(R.id.check)
    ImageView checkBox;
    @BindView(R.id.txtModifierItemName)
    TextView modifierItemName;
    @BindView(R.id.txtmodifierPrice)
    TextView modifierPrice;




    @BindView(R.id.check_layout)
    FrameLayout checkLayout;
    private ModifierProcessor modifierProcessor;


    public ModifierItemOptionViewHolder(Context context, MenuContract.Presenter menuPresenter, View itemView) {
        super(itemView);
        modifierProcessor = new ModifierProcessor(this);
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
      modifierProcessor.processModifier(modifierItem);
        if (modifierItem.getAttributes().isSelected()) {
            checkBox.setImageResource(R.drawable.size_selected);
        } else {
            checkBox.setImageResource(R.drawable.size_unselected);
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

    @Override
    public void onModifierProcessed(@NonNull String name, @NonNull String formattedPrice, @NonNull String text) {
        setModifierNameAndPrice(name,formattedPrice,text);
    }
    private void setModifierNameAndPrice(String name, String formattedPrice,String text) {
        if (LocaleUtil.getInstance().isEnglish() || LocaleUtil.getInstance().isFrench()) {
            modifierItemName.setText(Html.fromHtml(name),  TextView.BufferType.SPANNABLE);
        } else {
            if (ArabicUtils.INSTANCE.isArabic(name)) {
                modifierItemName.setText(Html.fromHtml(name), TextView.BufferType.SPANNABLE);
            } else {

                String modifiedText = text + " " + name;
                modifierItemName.setText(Html.fromHtml(modifiedText), TextView.BufferType.SPANNABLE);
            }
        }

        modifierPrice.setText(formattedPrice);
    }
}
