package com.herfy.order.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.vipulasri.timelineview.TimelineView;
import com.herfy.R;
import com.herfy.order.OrderContract;
import com.herfy.order.viewholders.TimeLineViewHolder;

public class TimeLineAdapter extends RecyclerView.Adapter<TimeLineViewHolder> {

    private Context context;

    private OrderContract.Presenter orderPresenter;

    public TimeLineAdapter(Context context, OrderContract.Presenter orderPresenter) {
        this.context = context;
        this.orderPresenter = orderPresenter;
    }

    @NonNull
    @Override
    public TimeLineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TimeLineViewHolder(context, orderPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timeline, parent, false), viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeLineViewHolder holder, int position) {
        orderPresenter.onBindTimeLineViewAtPosition(position, holder);
    }

    @Override
    public int getItemCount() {
        return orderPresenter.getTimeLineCount();
    }

    @Override
    public int getItemViewType(int position) {
        return TimelineView.getTimeLineViewType(position, getItemCount());
    }

}
