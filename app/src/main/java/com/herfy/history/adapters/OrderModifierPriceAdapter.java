package com.herfy.history.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.herfy.R;
import com.skylinedynamics.solosdk.api.models.objects.order.OrderModifierItem;
import com.herfy.util.StringUtil;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;

public class OrderModifierPriceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>  {


    private List<OrderModifierItem> listOfOrderModifierItem;
    private Context context;

    public OrderModifierPriceAdapter(Context context,List<OrderModifierItem> listOfOrderModifierItem) {
        this.context = context;
        this.listOfOrderModifierItem = listOfOrderModifierItem;
    }

    public class ModifierItemViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.txtModifiersprice)
        TextView modifierPrice;

        @BindView(R.id.modifierLabel)
        TextView txtModifierLabel;
        public ModifierItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new OrderModifierPriceAdapter.ModifierItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_modifiers_order, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        OrderModifierItem modifierItem = listOfOrderModifierItem.get(position);
        setupDataView((OrderModifierPriceAdapter.ModifierItemViewHolder) holder, modifierItem);
    }

    @Override
    public int getItemCount() {
        return listOfOrderModifierItem.size();
    }

    private void setupDataView(final ModifierItemViewHolder holder,final OrderModifierItem modifierItem) {
        bindModifierPrice(holder, modifierItem);
        bindModifierLabel(holder, modifierItem);
    }

    private void bindModifierPrice(ModifierItemViewHolder holder, OrderModifierItem modifierItem) {
        String formattedPrice = String.valueOf(StringUtil.getFormattedPrice(modifierItem.getAttributes().getPrice()));
        holder.modifierPrice.setText(formattedPrice);
    }

    private void bindModifierLabel(ModifierItemViewHolder holder, OrderModifierItem modifierItem) {
        String modifierName = modifierItem.getAttributes().getName();
        holder.txtModifierLabel.setText(context.getString(R.string.modifier_name_with_quantity_text,modifierName,String.valueOf(modifierItem.getAttributes().getQuantity())));
    }


}
