package com.herfy.upsell;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;

public class UpsellRecyclerViewAdapter extends RecyclerView.Adapter<UpsellViewHolder> {

    private final UpsellContract.Presenter upsellPresenter;

    public UpsellRecyclerViewAdapter(UpsellContract.Presenter upsellPresenter) {
        this.upsellPresenter = upsellPresenter;
    }

    @NonNull
    @Override
    public UpsellViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new UpsellViewHolder(upsellPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_upsell_new, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull UpsellViewHolder holder, int position) {
         upsellPresenter.onBindUpsellViewAtPosition(position, holder);
    }

    @Override
    public int getItemCount() {
        return upsellPresenter.getUpsellsCount();
    }

}
