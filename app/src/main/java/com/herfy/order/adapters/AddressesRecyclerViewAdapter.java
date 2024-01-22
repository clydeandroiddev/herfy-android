package com.herfy.order.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.order.OrderContract;
import com.herfy.order.viewholders.AddressViewHolder;

public class AddressesRecyclerViewAdapter extends RecyclerView.Adapter<AddressViewHolder> {

    private Context context;

    private final OrderContract.Presenter orderPresenter;

    private boolean home = false;
    private boolean work = false;

    private int selected = -1;

    public AddressesRecyclerViewAdapter(Context context, OrderContract.Presenter orderPresenter, boolean home, boolean work) {
        this.context = context;
        this.orderPresenter = orderPresenter;
        this.home = home;
        this.work = work;
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AddressViewHolder(context, orderPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        orderPresenter.onBindAddressItemViewAtPosition(position, holder, home, work, selected == position);
    }

    @Override
    public int getItemCount() {
        return orderPresenter.getAddressesCount(home, work);
    }

    public void setSelected(int selected) {
        this.selected = selected;
        notifyDataSetChanged();
    }

    public int getSelected() {
        return selected;
    }

}
