package com.herfy.subscription.premade.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.subscription.premade.models.DeliveryTime;
import com.herfy.utils.theme.DynamicTheme;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DeliveryTimeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface DeliveryTimeListener {
        void selectDeliveryTime(DeliveryTime time, int position);
    }

    private DeliveryTimeListener listener;

    private List<DeliveryTime> menus;
    private Context context;
    public int selectedTime = -1;

    public DeliveryTimeAdapter(Context context, List<DeliveryTime> menus, DeliveryTimeListener listener) {
        this.menus = menus;
        this.context = context;
        this.listener = listener;
    }

    public class ViewHolderData extends RecyclerView.ViewHolder {

        @BindView(R.id.time_main)
        MaterialCardView timeMain;
        @BindView(R.id.check_image)
        ImageView checkImage;
        @BindView(R.id.indicator_image)
        ImageView indicatorImage;
        @BindView(R.id.time)
        AppCompatTextView time;

        public ViewHolderData(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderData(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_time_delivery, parent, false));
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

        final DeliveryTime menu = menus.get(position);

        holder.time.setText(menu.time);

        if (menu.isAvailable) {
            String colorStr = DynamicTheme.getStringColor(context);
            holder.time.setTextColor(Color.parseColor(colorStr));
            holder.indicatorImage.setImageResource(R.drawable.ic_ellipse_green);
            holder.timeMain.setOnClickListener(v -> {
                listener.selectDeliveryTime(menu, position);
            });
        } else {
            holder.time.setTextColor(context.getResources().getColor(R.color.silver));
            holder.indicatorImage.setImageResource(R.drawable.ic_ellipse_red);
            holder.timeMain.setOnClickListener(null);
        }

        if (selectedTime == position) {
            holder.checkImage.setImageResource(R.drawable.ic_picked_payment_type_svg);
        } else {
            holder.checkImage.setImageResource(R.drawable.ic_rectangle_47);
        }


    }

}

