package com.herfy.branches;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;

public class BranchesRecyclerViewAdapter extends RecyclerView.Adapter<BranchViewHolder> {

    private final BranchesContract.Presenter branchesPresenter;

    private int selected = -1;

    public BranchesRecyclerViewAdapter(BranchesContract.Presenter branchesPresenter) {
        this.branchesPresenter = branchesPresenter;
    }

    @NonNull
    @Override
    public BranchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BranchViewHolder(branchesPresenter, LayoutInflater.from(parent.getContext()).inflate(com.herfy.ordertype.R.layout.item_stores_pickup, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull BranchViewHolder holder, int position) {
        branchesPresenter.onBindBranchViewAtPosition(position, holder, position == selected);
    }

    @Override
    public int getItemCount() {
        return branchesPresenter.getBranchesCount();
    }

    public int getSelected() {
        return selected;
    }

    public void setSelected(int selected) {
        this.selected = selected;
        notifyDataSetChanged();
    }

}
