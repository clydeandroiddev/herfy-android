package com.herfy.summary;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.payment.PaymentContract;

public class OrderSummaryMenuItemRecyclerViewAdapter extends RecyclerView.Adapter<OrderSummaryMenuItemViewHolder> {

    private PaymentContract.Presenter paymentPresenter;

    public OrderSummaryMenuItemRecyclerViewAdapter(PaymentContract.Presenter paymentPresenter) {
        this.paymentPresenter = paymentPresenter;
    }

    @Override
    public OrderSummaryMenuItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new OrderSummaryMenuItemViewHolder(paymentPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_menu_summary, parent, false));
    }

    @Override
    public void onBindViewHolder(OrderSummaryMenuItemViewHolder holder, int position) {
        paymentPresenter.onBindMenuItemViewAtPosition(position, holder);
    }

    @Override
    public int getItemCount() {
        return paymentPresenter.getMenuItemsCount();
    }

}
