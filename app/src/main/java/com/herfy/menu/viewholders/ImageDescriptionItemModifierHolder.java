package com.herfy.menu.viewholders;

import android.content.Context;
import android.text.Html;
import android.text.SpannedString;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.textview.MaterialTextView;
import com.herfy.R;
import com.herfy.constants.Text;
import com.herfy.menu.MenuContract;
import com.herfy.util.CacheUtil;
import com.herfy.util.LocaleUtil;
import com.herfy.util.ModifierProcessor;
import com.herfy.util.ModifierProcessorCallback;
import com.herfy.util.StringUtil;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ImageDescriptionItemModifierHolder extends BaseModifierItemViewHolder implements ModifierProcessorCallback {
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
    @BindView(R.id.quantityUp)
    ImageButton plus;
    private OnAction onAction;
    private ModifierProcessor modifierProcessor;


    public void setOnAction(OnAction onAction) {
        this.onAction = onAction;
    }

    public ImageDescriptionItemModifierHolder(Context context, MenuContract.Presenter menuPresenter, View itemView) {

        super(itemView);

        ButterKnife.bind(this, itemView);
        modifierProcessor = new ModifierProcessor(this);
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
        modifierProcessor.processModifier(modifierItem);
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
        plus.setFocusable(modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum());
        plus.setClickable(modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum());

        if (modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum()) {
            plus.setImageResource(R.drawable.ic_add);
        }

        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onAction != null) {
                    if (modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum()) {
                        onAction.onQuantityChanged(true, modifierGroupPosition, modifierGroup, modifierItemPosition, modifierItem);
                    } else {
                        Toast.makeText(itemView.getContext(), CacheUtil.getInstance().getTranslations(Text.MODIFIER_MAXIMUM_LIMIT), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        }


    private void setModifierNameAndPrice(String name, String formattedPrice,String text) {
        if (LocaleUtil.getInstance().isEnglish() || LocaleUtil.getInstance().isFrench()) {

            modifierNameAndPrice.setText(name);
        } else {
            if (isProbablyArabic(name)) {
                modifierNameAndPrice.setText(Html.fromHtml(name), TextView.BufferType.SPANNABLE);
            } else {
                String modifiedText = text + " " + name;
                modifierNameAndPrice.setText(Html.fromHtml(modifiedText), TextView.BufferType.SPANNABLE);
            }
        }


        txtModifierPrice.setText(formattedPrice);
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
        setModifierNameAndPrice(name, formattedPrice, text);
    }
}