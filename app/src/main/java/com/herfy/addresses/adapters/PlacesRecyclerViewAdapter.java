package com.herfy.addresses.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.addresses.AddressesContract;
import com.herfy.addresses.viewholders.PlaceViewHolder;

public class PlacesRecyclerViewAdapter extends RecyclerView.Adapter<PlaceViewHolder> {

    private Context context;

    private final AddressesContract.Presenter addressesPresenter;

    private int selected = -1;

    public PlacesRecyclerViewAdapter(Context context, AddressesContract.Presenter addressesPresenter) {
        this.context = context;
        this.addressesPresenter = addressesPresenter;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PlaceViewHolder(context, addressesPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_place, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
         addressesPresenter.onBindPlaceItemViewAtPosition(position, holder, selected == position);
    }

    @Override
    public int getItemCount() {
        return addressesPresenter.getPlacesCount();
    }

    public void setSelected(int selected) {
        this.selected = selected;
        notifyDataSetChanged();
    }

    public int getSelected() {
        return selected;
    }

}
