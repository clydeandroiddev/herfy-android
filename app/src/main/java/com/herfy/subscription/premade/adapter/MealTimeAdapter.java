package com.herfy.subscription.premade.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.utils.constants.Text;
import com.herfy.subscription.premade.models.StubPreMadeModel;
import com.herfy.util.StringUtil;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.theme.DynamicTheme;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MealTimeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface MealTimeItemListener {
        void select(StubPreMadeModel menu, int position);

        void setTime(StubPreMadeModel menu, int position);

        void setAddress(StubPreMadeModel menu, int position);
    }

    private MealTimeItemListener listener;

    private List<StubPreMadeModel> menus;
    private Context context;
    private boolean isSingleMode;

    public MealTimeAdapter(Context context, List<StubPreMadeModel> menus, MealTimeItemListener listener) {
        this.menus = menus;
        this.context = context;
        this.listener = listener;
    }

    public class ViewHolderData extends RecyclerView.ViewHolder {

        @BindView(R.id.meal_time)
        AppCompatTextView mealTime;
        @BindView(R.id.price_meal_time)
        AppCompatTextView priceMealTime;
        @BindView(R.id.top_layout)
        ConstraintLayout topLayout;
        @BindView(R.id.pickup_label)
        AppCompatTextView pickupLabel;
        @BindView(R.id.delivery_label)
        AppCompatTextView deliveryLabel;
        @BindView(R.id.type_time)
        MaterialCardView typeTime;
        @BindView(R.id.type_time_label)
        AppCompatTextView typeTimeLabel;
        @BindView(R.id.type_time_label_image)
        ImageView typeTimeLabelImage;
        @BindView(R.id.type_address)
        MaterialCardView typeAddress;
        @BindView(R.id.type_address_label)
        AppCompatTextView typeAddressLabel;
        @BindView(R.id.type_address_label_image)
        ImageView typeAddressLabelImage;
        @BindView(R.id.together_price)
        AppCompatTextView togetherPrice;
        @BindView(R.id.pickup_label_selection)
        View pickupLabelSelection;
        @BindView(R.id.delivery_label_selection)
        View deliveryLabelSelection;

        public ViewHolderData(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderData(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_meal_plan, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        setupDataView(((ViewHolderData) holder), position);
    }

    @Override
    public int getItemCount() {
        return menus.size();
    }

    private void setupDataView(final ViewHolderData holder, final int position) {

        final StubPreMadeModel menu = menus.get(position);


        if (menu.isTogether) {
            holder.topLayout.setVisibility(View.GONE);
            if (menu.isPickup) {
                holder.togetherPrice.setVisibility(View.GONE);
            } else {
                holder.togetherPrice.setVisibility(View.VISIBLE);
                holder.togetherPrice.setText("+" + StringUtil.getFormattedAmount(menu.togetherMealPrice) + " sr/delivery");
            }
        } else {
            holder.mealTime.setText(menu.mealTitle);
            if (menu.isPickup) {
                holder.priceMealTime.setVisibility(View.GONE);
            } else {
                holder.priceMealTime.setVisibility(View.VISIBLE);
                holder.priceMealTime.setText("+" + StringUtil.getFormattedAmount(menu.mealPrice) + " sr/delivery");
            }
            holder.topLayout.setVisibility(View.VISIBLE);
            holder.togetherPrice.setVisibility(View.GONE);
        }

        holder.pickupLabel.setText(CacheUtil.getInstance().getTranslations(Text.STORE_PICKUP, "STORE PICKUP"));
        holder.deliveryLabel.setText(CacheUtil.getInstance().getTranslations(Text.DELIVERY, "DELIVERY"));

        String colorStr = DynamicTheme.getStringColor(context);

        if (menu.isPickup) {
            holder.pickupLabelSelection.setVisibility(View.VISIBLE);
            holder.pickupLabel.setTextColor(Color.parseColor(colorStr));
            holder.deliveryLabelSelection.setVisibility(View.GONE);
            holder.deliveryLabel.setTextColor(context.getResources().getColor(R.color.silver));
            if (menu.pickupTimeDone) {
                holder.typeTimeLabelImage.setImageResource(R.drawable.ic_order_type_time_and_location_already_set_svg);
                holder.typeTimeLabel.setText(CacheUtil.getInstance().getTranslations(Text.SET_TO, "SET TO") + " " + menu.pickupTime);
            } else {
                holder.typeTimeLabelImage.setImageResource(R.drawable.ic_add_main_dish_snack_drink_svg);
                holder.typeTimeLabel.setText(CacheUtil.getInstance().getTranslations(Text.SET_PICKUP_TIME, "SET PICKUP TIME"));
            }
            if (menu.pickupAddressDone) {
                holder.typeAddressLabelImage.setImageResource(R.drawable.ic_order_type_time_and_location_already_set_svg);
                holder.typeAddressLabel.setText(CacheUtil.getInstance().getTranslations(Text.SET_TO, "SET TO") + " \"" + menu.pickupAddress + "\"");
            } else {
                holder.typeAddressLabelImage.setImageResource(R.drawable.ic_add_main_dish_snack_drink_svg);
                holder.typeAddressLabel.setText(CacheUtil.getInstance().getTranslations(Text.SET_PICKUP_BRANCH, "SET PICKUP BRANCH"));
            }
        } else {
            holder.deliveryLabelSelection.setVisibility(View.VISIBLE);
            holder.deliveryLabel.setTextColor(Color.parseColor(colorStr));
            holder.pickupLabelSelection.setVisibility(View.GONE);
            holder.pickupLabel.setTextColor(context.getResources().getColor(R.color.silver));
            if (menu.deliveryTimeDone) {
                holder.typeTimeLabelImage.setImageResource(R.drawable.ic_order_type_time_and_location_already_set_svg);
                holder.typeTimeLabel.setText(CacheUtil.getInstance().getTranslations(Text.SET_TO, "SET TO") + " " + menu.deliveryTime);
            } else {
                holder.typeTimeLabelImage.setImageResource(R.drawable.ic_add_main_dish_snack_drink_svg);
                holder.typeTimeLabel.setText(CacheUtil.getInstance().getTranslations(Text.SET_DELIVERY_TIME, "SET DELIVERY TIME"));
            }
            if (menu.deliveryAddressDone) {
                holder.typeAddressLabelImage.setImageResource(R.drawable.ic_order_type_time_and_location_already_set_svg);
                holder.typeAddressLabel.setText(CacheUtil.getInstance().getTranslations(Text.SET_TO, "SET TO") + " \"" + menu.deliveryAddress + "\"");
            } else {
                holder.typeAddressLabelImage.setImageResource(R.drawable.ic_add_main_dish_snack_drink_svg);
                holder.typeAddressLabel.setText(CacheUtil.getInstance().getTranslations(Text.SET_DELIVERY_ADDRESS, "SET DELIVERY ADDRESS"));
            }
        }

        holder.typeTime.setOnClickListener(v -> {
            listener.setTime(menu, position);
        });

        holder.typeAddress.setOnClickListener(v -> {
            listener.setAddress(menu, position);
        });

        holder.pickupLabel.setOnClickListener(v -> {
            listener.select(menu, position);
        });

        holder.deliveryLabel.setOnClickListener(v -> {
            listener.select(menu, position);
        });

    }

    public void notifyItemOnly(int position) {
        this.notifyItemChanged(position);
    }
}
