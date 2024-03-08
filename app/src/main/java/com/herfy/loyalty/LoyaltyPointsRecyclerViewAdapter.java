package com.herfy.loyalty;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;

import com.herfy.loyalty.objects.DateItem;
import com.herfy.loyalty.objects.ListItem;

public class LoyaltyPointsRecyclerViewAdapter extends RecyclerView.Adapter {

    private Context context;
    private final LoyaltyContract.Presenter pointsPresenter;

    public LoyaltyPointsRecyclerViewAdapter(Context context, LoyaltyContract.Presenter presenter) {
        this.context = context;
        this.pointsPresenter = presenter;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        switch (viewType) {
            case ListItem.TYPE_LOYALTY:
                viewHolder = new LoyaltyPointsViewHolder(context, pointsPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loyalty_history, parent, false));
                break;
            case ListItem.TYPE_DATE:
                viewHolder =new DateViewHolder(context, pointsPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_date, parent, false));
                break;
        }
        return viewHolder;
    }



    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        switch (viewHolder.getItemViewType()) {
            case ListItem.TYPE_LOYALTY:
                pointsPresenter.onBindLoyaltyHistoryViewAtPosition(position,(LoyaltyPointsViewHolder) viewHolder);
                break;
            case ListItem.TYPE_DATE:
                DateItem dateItem = (DateItem)  pointsPresenter.getConsolidatedList().get(position);
                DateViewHolder dateViewHolder = (DateViewHolder) viewHolder;
                dateViewHolder.tvDate.setText(dateItem.getDate());
                break;
        }
    }


    @Override
    public int getItemCount() {
        return pointsPresenter.getConsolidatedList().size();
    }

    @Override
    public int getItemViewType(int position) {
        return pointsPresenter.getConsolidatedList().get(position).getType();
    }
}
