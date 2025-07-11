package com.herfy.cart.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.util.StringUtil;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.order.OrderModifierItem;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CartModifierPriceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>  {


    private List<ModifierItem> listOfOrderModifierItem;
    private Context context;

    public CartModifierPriceAdapter(Context context, List<ModifierItem> listOfOrderModifierItem) {
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
        return new CartModifierPriceAdapter.ModifierItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                com.herfy.cart.R.layout.item_cart_modifiers, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ModifierItem modifierItem = listOfOrderModifierItem.get(position);
        setupDataView((CartModifierPriceAdapter.ModifierItemViewHolder) holder, modifierItem);
    }

    @Override
    public int getItemCount() {
        return listOfOrderModifierItem.size();
    }

    private void setupDataView(final ModifierItemViewHolder holder,final ModifierItem modifierItem) {
        bindModifierPrice(holder, modifierItem);
        bindModifierLabel(holder, modifierItem);
    }

    private void bindModifierPrice(ModifierItemViewHolder holder, ModifierItem modifierItem) {
        String formattedPrice = String.valueOf(StringUtil.getFormattedPrice(modifierItem.getAttributes().getPrice()));
        holder.modifierPrice.setText(formattedPrice);
    }

    private void bindModifierLabel(ModifierItemViewHolder holder, ModifierItem modifierItem) {
        String modifierName = modifierItem.getAttributes().getName();
        holder.txtModifierLabel.setText(context.getString(R.string.modifier_name_with_quantity_text,modifierName,String.valueOf(modifierItem.getAttributes().getQuantity())));
    }


}
