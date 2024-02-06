package com.herfy.menu.viewholders;

import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.imageview.ShapeableImageView;
import com.herfy.R;
import com.herfy.utils.constants.*;
import com.herfy.menu.MenuContract;
import com.herfy.productpagedetails.ui.viewholder.util.ArabicUtils;
import com.herfy.utils.helper.*;
import com.herfy.util.ModifierProcessor;
import com.herfy.util.ModifierProcessorCallback;
import com.herfy.util.StringUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupAttributes;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ModifierItemOptionViewHolder extends BaseModifierItemViewHolder implements ModifierProcessorCallback {
    @BindView(R.id.checkBox)
    ImageView check;
    private Context context;
    @BindView(R.id.radioButton)
    ImageView radioButton;
    @BindView(R.id.check_layout)
    FrameLayout checkLayout;

    @BindView(R.id.txtModifierItemName)
    TextView modifierNameAndPrice;
    @BindView(R.id.txtmodifierPrice)
    TextView txtModifierPrice;
    @BindView(R.id.clickable_layout)
    LinearLayout clickableLayout;
    @BindView(R.id.quantityUp)
    ImageButton plus;
    @BindView(R.id.modifierImage)
    ShapeableImageView modifierOptionImage;
    private ModifierProcessor modifierProcessor;
    @BindView(R.id.quantity_container)
    ConstraintLayout quantityContainer;

    @BindView(R.id.quantity) TextView quantity;






    public ModifierItemOptionViewHolder(Context context, MenuContract.Presenter menuPresenter, View itemView) {
        super(itemView);
        modifierProcessor = new ModifierProcessor(this);
        this.context = context;
        ButterKnife.bind(this, itemView);
    }



    @Override
    public ImageView getModifierImageView() {
        return null;
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
        ModifierGroupAttributes modifierGroupAttributes = modifierGroup.getAttributes();

        radioButton.setVisibility(modifierGroupAttributes.getMaximum() == 1 && modifierGroupAttributes.getMinimum() == 1 ? View.VISIBLE : View.GONE);
        check.setVisibility(modifierGroup.getAttributes().getMaximum() > 1 ? View.VISIBLE : View.GONE);
        if (modifierItem.getAttributes().isSelected()) {
            radioButton.setImageResource(R.drawable.modifier_radio_item_selected);
        }else {
            radioButton.setImageResource(R.drawable.modifier_item_radio_unselected);
        }
        if (modifierItem.getAttributes().isSelected()) {

            check.setImageResource(R.drawable.modifier_item_selected);
        } else {
            check.setImageResource(R.drawable.modifier_item_unselected);
        }
        quantity.setText(StringUtil.localize(String.valueOf(modifierItem.getAttributes().getQuantity())));
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
        clickableLayout.setOnClickListener(new View.OnClickListener() {
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
        plus.setFocusable(modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum());
        plus.setClickable(modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum());
        String imageUrl = modifierItem.getAttributes().getImage();
        if (imageUrl != null && !imageUrl.equalsIgnoreCase("null") && !imageUrl.isEmpty() && !imageUrl.toLowerCase().contains("default-image.png")) {
            Glide.with(context).load(imageUrl).apply(RequestOptions.centerCropTransform()).placeholder(R.drawable.imagepicker_image_placeholder).diskCacheStrategy(DiskCacheStrategy.ALL).into(modifierOptionImage);
            modifierOptionImage.setVisibility(View.VISIBLE);
        } else {
            modifierOptionImage.setVisibility(View.GONE);
        }
        if (modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum()) {
            plus.setImageResource(R.drawable.ic_add);
        }

        quantityContainer.setVisibility(modifierGroup.getAttributes().
                getMaximum() > 1 && modifierItem.getAttributes().
                getMaximum() > 1 ? View.VISIBLE : View.GONE);
        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (action != null) {
                    if (modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum()) {
                        action.onQuantityChanged(true, modifierGroupPosition, modifierGroup, modifierItemPosition, modifierItem);
                    } else {
                        Toast.makeText(itemView.getContext(), CacheUtil.getInstance().getTranslations(Text.MODIFIER_MAXIMUM_LIMIT), Toast.LENGTH_SHORT).show();
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
            modifierNameAndPrice.setText(Html.fromHtml(name),  TextView.BufferType.SPANNABLE);
        } else {
            if (ArabicUtils.INSTANCE.isArabic(name)) {
                modifierNameAndPrice.setText(Html.fromHtml(name), TextView.BufferType.SPANNABLE);
            } else {

                String modifiedText = text + " " + name;
                modifierNameAndPrice.setText(Html.fromHtml(modifiedText), TextView.BufferType.SPANNABLE);
            }
        }

        txtModifierPrice.setText(formattedPrice);
    }
}
