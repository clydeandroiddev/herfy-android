package com.herfy.history.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.history.OrdersContract;
import com.herfy.history.viewholders.OrderViewHolder;

public class OrdersRecyclerViewAdapter extends RecyclerView.Adapter<OrderViewHolder> {

    private Context context;

    private final OrdersContract.Presenter ordersPresenter;

    public OrdersRecyclerViewAdapter(Context context, OrdersContract.Presenter ordersPresenter) {
        this.context = context;
        this.ordersPresenter = ordersPresenter;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OrderViewHolder(context, ordersPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_summary, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        ordersPresenter.onBindOrderViewAtPosition(position, holder);
    }

    @Override
    public int getItemCount() {
        return ordersPresenter.getOrdersCount();
    }

}
