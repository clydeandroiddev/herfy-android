package com.herfy.history.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.history.OrdersContract;
import com.herfy.history.viewholders.OrderMenuItemViewHolder;

public class OrderMenuItemsRecyclerViewAdapter extends RecyclerView.Adapter<OrderMenuItemViewHolder> {

    private Context mContext;

    private final OrdersContract.Presenter ordersPresenter;

    public OrderMenuItemsRecyclerViewAdapter(Context context, OrdersContract.Presenter ordersPresenter) {
        this.mContext = context;
        this.ordersPresenter = ordersPresenter;
    }

    @NonNull
    @Override
    public OrderMenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OrderMenuItemViewHolder(mContext, ordersPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_menu_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull OrderMenuItemViewHolder holder, int position) {
         ordersPresenter.onBindMenuItemViewAtPosition(position, holder);
    }

    @Override
    public int getItemCount() {
        return ordersPresenter.getMenuItemsCount();
    }

}
