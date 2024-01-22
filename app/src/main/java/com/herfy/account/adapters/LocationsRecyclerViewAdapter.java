package com.herfy.account.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.account.AccountContract;
import com.herfy.account.LocationViewHolder;

public class LocationsRecyclerViewAdapter extends RecyclerView.Adapter<LocationViewHolder> {

    private final AccountContract.Presenter accountPresenter;
    private final boolean isEdit;

    public LocationsRecyclerViewAdapter(AccountContract.Presenter locationPresenter, boolean isEdit) {
        this.accountPresenter = locationPresenter;
        this.isEdit = isEdit;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new LocationViewHolder(accountPresenter, isEdit, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_account_addresses, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
         accountPresenter.onBindLocationItemViewAtPosition(position, holder);
    }

    @Override
    public int getItemCount() {
        return accountPresenter.getBookmarkLocationsCount();
    }

}
