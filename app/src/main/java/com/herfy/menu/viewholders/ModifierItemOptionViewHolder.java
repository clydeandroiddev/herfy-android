package com.herfy.menu.viewholders;

import android.animation.ValueAnimator;
import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.productpagedetails.ui.viewholder.util.ArabicUtils;
import com.herfy.util.ModifierProcessor;
import com.herfy.util.ModifierProcessorCallback;
import com.herfy.util.StringUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupAttributes;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;

import java.util.HashMap;

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
    @BindView(R.id.optionCardView)
    MaterialCardView modifierItemCardView;
    private ModifierItem currentSelectedItem;
    @BindView(R.id.quantityUp)
    ImageButton plus;
    @BindView(R.id.image)
    ShapeableImageView modifierOptionImage;
    private ModifierProcessor modifierProcessor;

    @BindView(R.id.quantity_container)
    ConstraintLayout quantityContainer;

    @BindView(R.id.quantity) TextView quantity;

    @BindView(R.id.quantityDownLayout)
    ConstraintLayout quantityDownLayout;
    @BindView(R.id.quantityDown)
    MaterialCardView minus;
    private boolean isQuantityLayoutVisible = false;

    private HashMap<Integer, Boolean> showShowQuantity = new HashMap<>();


    public ModifierItemOptionViewHolder(Context context, MenuContract.Presenter menuPresenter, View itemView, HashMap<Integer, Boolean> shouldShowQuantity) {
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
    public void setModifierItem(int modifierGroupPosition, ModifierGroup modifierGroup, int modifierItemPosition, ModifierItem modifierItem, boolean showModifierItemPrice, HashMap<Integer, Boolean> shouldShowQuantity) {
      modifierProcessor.processModifier(modifierItem);
        ModifierGroupAttributes modifierGroupAttributes = modifierGroup.getAttributes();
        radioButton.setVisibility(modifierGroupAttributes.getMaximum() == 1 && modifierGroupAttributes.getMinimum() == 1 ? View.VISIBLE : View.GONE);
        check.setVisibility(modifierGroup.getAttributes().getMaximum() > 1 ? View.VISIBLE : View.GONE);
        if (modifierItem.getAttributes().isSelected()) {
            modifierItemCardView.setBackgroundResource(R.drawable.modifier_item_bg_selected_state);
            radioButton.setImageResource(R.drawable.modifier_radio_item_selected);
            check.setImageResource(R.drawable.modifier_item_checked_selected);

        }else {
            modifierItemCardView.setBackgroundResource(R.drawable.modifier_item_bg_unselected_state);
            radioButton.setImageResource(R.drawable.modifier_item_radio_unselected);
            check.setImageResource(R.drawable.modifier_item_unselected);
        }
        if (modifierGroup.getAttributes().getMaximum() > 1 &&
                modifierItem.getAttributes().isSelected() &&
                modifierItem.getAttributes().getMaximum() > 1) {
            plus.setEnabled(true);
            plus.setImageResource(R.drawable.quantity_up_blue);
        }else {
            plus.setEnabled(false);
            plus.setImageResource(R.drawable.quantity_up_blue_gray);
        }
        quantity.setText(StringUtil.localize(String.valueOf("0" + modifierItem.getAttributes().getQuantity())));

        plus.setEnabled(modifierGroup.getAttributes().getMaximum() > 1 &&
                modifierItem.getAttributes().isSelected() &&
                modifierItem.getAttributes().getMaximum() > 1);
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
                String imageUrl = modifierItem.getAttributes().getImage();
        if (imageUrl != null && !imageUrl.equalsIgnoreCase("null") && !imageUrl.isEmpty() && !imageUrl.toLowerCase().contains("default-image.png")) {
            Glide.with(context).load(imageUrl).apply(RequestOptions.centerCropTransform()).placeholder(R.drawable.imagepicker_image_placeholder).diskCacheStrategy(DiskCacheStrategy.ALL).into(modifierOptionImage);
            modifierOptionImage.setVisibility(View.VISIBLE);
        } else {
            modifierOptionImage.setVisibility(View.GONE);
        }
        if (modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum()) {
            plus.setImageResource(R.drawable.quantity_up_blue);
        }

        // todo commented for now might needed once designer have quantity design
        plus.setFocusable(modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum());
        plus.setClickable(modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum());

        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (action != null) {
                    // Update the visibility of the quantityDownLayout
                    boolean shouldShowQuantityLayout = modifierGroup.getAttributes().getMaximum() > 1 &&
                            modifierItem.getAttributes().isSelected() &&
                            modifierItem.getAttributes().getMaximum() > 1;
                    quantityDownLayout.setVisibility(shouldShowQuantityLayout ? View.VISIBLE : View.GONE);

                    // Update the visibility state of the current quantityDownLayout
                    isQuantityLayoutVisible = shouldShowQuantityLayout;

                    // Check if the clicked item is the same as the currently selected item
                    boolean isSameItemClicked = currentSelectedItem == modifierItem;

                    // If it's not the same item, hide the previous quantityDownLayout and update the currentSelectedItem
                    if (!isSameItemClicked && currentSelectedItem != null) {
                        // Hide the previous quantityDownLayout
                        if (currentSelectedItem != null && isQuantityLayoutVisible) {
                            Log.d("FAFA","Collapse called");
                            collapseView(quantityDownLayout, 300, 150);
                            isQuantityLayoutVisible = false;
                        }
                        Log.d("FAFA","Collapse called BAI");
                        // Update the currentSelectedItem
                        currentSelectedItem = modifierItem;
                    }else {
                        Log.d("FAFA","False man");
                    }

                    // If it's the same item and the layout is already visible, toggle the visibility
                    if (isSameItemClicked && isQuantityLayoutVisible) {
                        isQuantityLayoutVisible = !isQuantityLayoutVisible;
                    }

                    // If the layout should be visible and it's not the same item, expand it
                    if (!isSameItemClicked && shouldShowQuantityLayout) {
                        Log.d("FAFA","Expanse called");
                        expandView(quantityDownLayout, 300, 150);
                        // Call the action to update the quantity after the layout is properly handled
//                        action.onQuantityChanged(true, modifierGroupPosition, modifierGroup, modifierItemPosition, modifierItem);
                        isQuantityLayoutVisible = true;
                    }else {
                        Log.d("FAFA","Not triggered");
                    }

                }else {
                    Log.d("FAFA","False bai ");
                }
            }
        });

        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (action != null && (modifierItem.getAttributes().getQuantity() > 1 || modifierItem.getAttributes().getMinimum() != 1)) {
                    action.onQuantityChanged(false, modifierGroupPosition, modifierGroup, modifierItemPosition, modifierItem);
                }
            }
        });
    }
    public static void expandView(View v, int duration, final int targetWidth) {
        final int prevHeight = v.getWidth();
        v.setVisibility(View.VISIBLE);
        ValueAnimator valueAnimator = ValueAnimator.ofInt(prevHeight, targetWidth);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animatedValue = (int) animation.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
                layoutParams.width = animatedValue;
                v.setLayoutParams(layoutParams);
            }
        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    public static void collapseView(final View v, int duration, final int targetHeight) {
        final int prevHeight = v.getWidth();
        ValueAnimator valueAnimator = ValueAnimator.ofInt(prevHeight, targetHeight);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animatedValue = (int) animation.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
                layoutParams.width = animatedValue;
                v.setLayoutParams(layoutParams);
            }
        });
        valueAnimator.setDuration(duration);
        valueAnimator.start();
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
