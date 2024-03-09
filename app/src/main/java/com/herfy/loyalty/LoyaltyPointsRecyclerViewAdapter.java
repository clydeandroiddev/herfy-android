package com.herfy.loyalty;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;


public class LoyaltyPointsRecyclerViewAdapter extends RecyclerView.Adapter<LoyaltyPointsViewHolder> {

    private Context context;
    private final LoyaltyHistoryContract.Presenter pointsPresenter;

    public LoyaltyPointsRecyclerViewAdapter(Context context, LoyaltyHistoryContract.Presenter presenter) {
        this.context = context;
        this.pointsPresenter = presenter;
    }

    @NonNull
    @Override
    public LoyaltyPointsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new LoyaltyPointsViewHolder(context, pointsPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loyalty_history, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull LoyaltyPointsViewHolder holder, int position) {
        pointsPresenter.onBindLoyaltyHistoryViewAtPosition(position, holder);
    }



    @Override
    public int getItemCount() {
        return pointsPresenter.transactionListCount();
    }
}
