package com.herfy.order.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.order.OrderContract;
import com.herfy.order.viewholders.StoreViewHolder;

public class StoresRecyclerViewAdapter extends RecyclerView.Adapter<StoreViewHolder> {

    private final OrderContract.Presenter orderPresenter;

    private boolean isDineIn = false;
    private int selected = -1;

    public StoresRecyclerViewAdapter(OrderContract.Presenter orderPresenter, boolean isDineIn) {
        this.orderPresenter = orderPresenter;
        this.isDineIn = isDineIn;
    }

    @NonNull
    @Override
    public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new StoreViewHolder(orderPresenter, isDineIn, LayoutInflater.from(parent.getContext()).inflate(com.herfy.ordertype.R.layout.item_stores_pickup, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull StoreViewHolder holder, int position) {
        orderPresenter.onBindStoreViewAtPosition(isDineIn, position, holder, position == selected);
    }

    @Override
    public int getItemCount() {
        if (orderPresenter.getStoresCount(isDineIn) > 0) {
            return orderPresenter.getStoresCount(isDineIn) + 3;
        }
        return 0;
    }

    public int getSelected() {
        return selected;
    }

    public void setSelected(int selected) {
        this.selected = selected;
        notifyDataSetChanged();
    }

}
