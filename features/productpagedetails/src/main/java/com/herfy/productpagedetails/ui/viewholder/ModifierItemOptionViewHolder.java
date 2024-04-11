package com.herfy.productpagedetails.ui.viewholder;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.herfy.utils.constants.Text;
import com.herfy.utils.helper.CacheUtil;

import com.herfy.productpagedetails.R;
import com.herfy.productpagedetails.databinding.ItemModifierOptionTypeBinding;
import com.herfy.utils.helper.ArabicUtils;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.utils.helper.ModifierProcessor;
import com.herfy.utils.helper.ModifierProcessorCallback;
import com.herfy.utils.helper.StringUtil;
import com.herfy.utils.menu.MenuUtil;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupAttributes;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class ModifierItemOptionViewHolder extends BaseModifierItemViewHolder implements ModifierProcessorCallback {
    ImageView check;
    ImageView radioButton;
    FrameLayout checkLayout;
    TextView modifierNameAndPrice;
    TextView txtModifierPrice;
    LinearLayout clickableLayout;
    MaterialCardView modifierItemCardView;
    ImageButton plus;
    ShapeableImageView modifierOptionImage;
    ConstraintLayout quantityContainer;
    TextView quantity;
    ConstraintLayout quantityDownLayout;
    MaterialCardView minus;
    private Context context;
    private ModifierProcessor modifierProcessor;
    private ModifierItem currentSelectedItem;
    private boolean isQuantityLayoutVisible = false;

    private HashMap<Integer, Boolean> showShowQuantity = new HashMap<>();

    private static HashMap<String, Integer> clickCountMap = new HashMap<>();
    private static HashMap<String,Integer> subtractMap = new HashMap<>();

    public ModifierItemOptionViewHolder(ItemModifierOptionTypeBinding binding){
        super(binding.getRoot());
        this.context = binding.getRoot().getContext();
        this.check = binding.checkBox;
        this.radioButton = binding.radioButton;
        this.checkLayout = binding.checkLayout;
        this.modifierNameAndPrice = binding.txtModifierItemName;
        this.txtModifierPrice = binding.txtmodifierPrice;
        this.clickableLayout = binding.clickableLayout;
        this.modifierItemCardView = binding.optionCardView;
        this.plus = binding.quantityUp;
        this.modifierOptionImage = binding.image;
        this.quantity = binding.quantity;
        this.quantityDownLayout = binding.quantityDownLayout;
        this.minus = binding.quantityDown;
        this.quantityContainer = binding.quantityContainer;
        modifierProcessor = new ModifierProcessor(this);
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
        boolean isModifierItemSelected = modifierItem.getAttributes().isSelected();
        radioButton.setVisibility(modifierGroupAttributes.getMaximum() == 1 ? View.VISIBLE : View.GONE);
        check.setVisibility(modifierGroup.getAttributes().getMaximum() > 1 ? View.VISIBLE : View.GONE);


        if (isModifierItemSelected) {
            modifierItemCardView.setBackgroundResource(R.drawable.modifier_item_bg_selected_state);
            radioButton.setImageResource(R.drawable.modifier_radio_item_selected);
            check.setImageResource(R.drawable.modifier_item_checked_selected);
        } else {
            modifierItemCardView.setBackgroundResource(R.drawable.modifier_item_bg_unselected_state);
            radioButton.setImageResource(R.drawable.modifier_item_radio_unselected);
            check.setImageResource(R.drawable.modifier_item_unselected);
        }
        boolean isQuantityShouldShow = (modifierGroup.getAttributes().getMaximum() > 1 &&
                modifierItem.getAttributes().isSelected() &&
                modifierItem.getAttributes().getMaximum() > 1);

        quantityDownLayout.setVisibility(isQuantityShouldShow ? View.VISIBLE : View.GONE);
        plus.setFocusable(modifierItem.getAttributes().getQuantity() < modifierGroup.getAttributes().getMaximum());
        plus.setClickable(modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum());
        if (clickCountMap.size() >= 1 && modifierItem.getAttributes().getQuantity() == 1 && modifierGroup.getAttributes().getMaximum() > 1 || modifierItem.getAttributes().isSelected()) {
            Integer clickCount = clickCountMap.get(modifierItem.getId());
            if (clickCount != null && clickCount >= 0) {
                clickCountMap.remove(modifierItem.getId());
            }
        }

        quantity.setText(StringUtil.localize(String.valueOf("0" + modifierItem.getAttributes().getQuantity())));
        if (clickCountMap.size() >= 0) {
            Integer clickCount = clickCountMap.get(modifierItem.getId());
            if (clickCount != null) {
                boolean shouldShowQuantityLayout = modifierGroup.getAttributes().getMaximum() > 1 &&
                        modifierItem.getAttributes().getMaximum() > 1 && clickCount >= 1;
                quantityDownLayout.setVisibility(shouldShowQuantityLayout ? View.VISIBLE : View.GONE);
            }
        }
        checkLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (action != null)  {
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
        String imageUrl = modifierItem.getAttributes().getImage();
        if (imageUrl != null && !imageUrl.equalsIgnoreCase("null") && !imageUrl.isEmpty() && !imageUrl.toLowerCase().contains("default-image.png")) {
            Glide.with(context).load(imageUrl).apply(RequestOptions.centerCropTransform()).placeholder(R.drawable.imagepicker_image_placeholder).diskCacheStrategy(DiskCacheStrategy.ALL).into(modifierOptionImage);
            modifierOptionImage.setVisibility(View.VISIBLE);
        } else {
            modifierOptionImage.setVisibility(View.GONE);
        }
        if (modifierItem.getAttributes().hasModifierItemExceedMaximum() && modifierItem.getAttributes().hasUserInteractedWithModifierItem()) {
            boolean shouldShowQuantityLayout = MenuUtil.shouldShowQuantity(modifierGroup, modifierItem);
            if (shouldShowQuantityLayout) {
                plus.setEnabled(true);
                plus.setImageResource(R.drawable.quantity_up_blue);
                modifierItemCardView.setEnabled(true);

            }
        } else {
                boolean shouldShowQuantityLayout = MenuUtil.shouldShowQuantity(modifierGroup, modifierItem);
                if (shouldShowQuantityLayout) {
                    plus.setEnabled(false);
                    plus.setImageResource(R.drawable.quantity_up_blue_gray);
                    modifierItemCardView.setEnabled(false);
                }
        }


        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String modifierItemId = modifierItem.getId();

                if (modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum()) {
                    clickCountMap.put(modifierItemId, 0);
                    boolean shouldShowQuantityLayout = MenuUtil.shouldShowQuantity(modifierGroup,modifierItem) && modifierItem.getAttributes().isSelected() && clickCountMap.size() == 0;
                    quantityDownLayout.setVisibility(shouldShowQuantityLayout ? View.VISIBLE : View.GONE);


                }

                Integer clickCount = clickCountMap.get(modifierItemId);

                clickCount = clickCount == null ? 1 : clickCount + 1;
                clickCountMap.put(modifierItemId, clickCount);
                if (action != null && clickCountMap.containsKey(modifierItemId) && clickCount >= 1 && modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum()) {
                    action.onQuantityChanged(true, modifierGroupPosition, modifierGroup, modifierItemPosition, modifierItem, quantityDownLayout.getVisibility() == View.VISIBLE, clickCount);
                }else {
                    int selectedItems = MenuUtil.modifierItemQuantityCount(modifierGroup);
                    if (MenuUtil.isAboveMaxModifierRequired(modifierGroup,selectedItems)) {
                        for (ModifierItem eachItems : modifierGroup.getModifierItems()) {
                            if (!Objects.equals(eachItems.getId(), modifierItem.getId()) &&  !eachItems.getAttributes().isSelected()) {
                                eachItems.getAttributes().setModifierItemInteraction(false);
                                eachItems.getAttributes().setModifierItemQuantityExceedsMaximum(false);
                            }
                        }
                    }
                    Toast.makeText(itemView.getContext(), CacheUtil.getInstance().getTranslations(Text.MODIFIER_MAXIMUM_LIMIT), Toast.LENGTH_SHORT).show();
                }
            }
        });
        minus.setFocusable(modifierItem.getAttributes().getQuantity() != 1);
        minus.setClickable(modifierItem.getAttributes().getQuantity() != 1);
        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (action != null && (modifierItem.getAttributes().getQuantity() > 1 || modifierItem.getAttributes().getMinimum() != 1)) {
                    action.onQuantityChanged(false, modifierGroupPosition, modifierGroup, modifierItemPosition, modifierItem, quantityDownLayout.getVisibility() == View.VISIBLE, 0);
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
    public void onModifierProcessed(@NotNull String name, @NotNull String formattedPrice) {
        setModifierNameAndPrice(name,formattedPrice);
    }
    private void setModifierNameAndPrice(String name, String formattedPrice) {
        if (LocaleUtil.getInstance().isEnglish() || LocaleUtil.getInstance().isFrench()) {
            modifierNameAndPrice.setText(Html.fromHtml(name),  TextView.BufferType.SPANNABLE);
        } else {
            if (ArabicUtils.INSTANCE.isArabic(name)) {
                modifierNameAndPrice.setText(Html.fromHtml(name), TextView.BufferType.SPANNABLE);
            } else {

                modifierNameAndPrice.setText(Html.fromHtml(name), TextView.BufferType.SPANNABLE);
            }
        }

        txtModifierPrice.setText(formattedPrice);
    }
}
