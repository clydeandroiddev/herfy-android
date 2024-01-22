package com.herfy.addresses.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.addresses.AddressesContract;
import com.herfy.addresses.viewholders.AddressViewHolder;

public class AddressesRecyclerViewAdapter extends RecyclerView.Adapter<AddressViewHolder> {

    private Context context;

    private final AddressesContract.Presenter addressesPresenter;

    private boolean home = false;
    private boolean work = false;

    private int selected = -1;

    public AddressesRecyclerViewAdapter(Context context, AddressesContract.Presenter addressesPresenter, boolean home, boolean work) {
        this.context = context;
        this.addressesPresenter = addressesPresenter;
        this.home = home;
        this.work = work;
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AddressViewHolder(context, addressesPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        addressesPresenter.onBindAddressItemViewAtPosition(position, holder, home, work, selected == position);
    }

    @Override
    public int getItemCount() {
        return addressesPresenter.getAddressesCount(home, work);
    }

    public void setSelected(int selected) {
        this.selected = selected;
        notifyDataSetChanged();
    }

    public int getSelected() {
        return selected;
    }

}
