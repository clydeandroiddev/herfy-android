package com.herfy.history.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.vipulasri.timelineview.TimelineView;
import com.herfy.R;
import com.herfy.history.OrdersContract;
import com.herfy.history.viewholders.TimeLineViewHolder;

public class TimeLineAdapter extends RecyclerView.Adapter<TimeLineViewHolder> {

    private Context context;

    private int orderPosition = -1;

    private OrdersContract.Presenter ordersPresenter;
    public boolean noDriver = true;
    public String orderId;

    public TimeLineAdapter(Context context, int orderPosition, OrdersContract.Presenter ordersPresenter, String orderId) {
        this.context = context;
        this.orderPosition = orderPosition;
        this.ordersPresenter = ordersPresenter;
        this.orderId = orderId;
    }

    @NonNull
    @Override
    public TimeLineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TimeLineViewHolder(context, ordersPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timeline, parent, false), viewType, orderId);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeLineViewHolder holder, int position) {
        ordersPresenter.onBindTimeLineViewAtPosition(orderPosition, position, holder, noDriver);
    }

    @Override
    public int getItemCount() {
        return ordersPresenter.getTimeLineCount(orderPosition);
    }

    @Override
    public int getItemViewType(int position) {
        return TimelineView.getTimeLineViewType(position, getItemCount());
    }

}
