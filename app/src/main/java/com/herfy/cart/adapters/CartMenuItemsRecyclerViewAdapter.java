package com.herfy.cart.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.cart.CartContract;
import com.herfy.cart.views.CartMenuItemViewHolder;

public class CartMenuItemsRecyclerViewAdapter extends RecyclerView.Adapter<CartMenuItemViewHolder> {

    private Context mContext;

    private final CartContract.Presenter cartPresenter;

    public CartMenuItemsRecyclerViewAdapter(Context context, CartContract.Presenter cartPresenter) {
        this.mContext = context;
        this.cartPresenter = cartPresenter;
    }

    @NonNull
    @Override
    public CartMenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CartMenuItemViewHolder(mContext, cartPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CartMenuItemViewHolder holder, int position) {
         cartPresenter.onBindMenuItemViewAtPosition(position, holder);
    }

    @Override
    public int getItemCount() {
        return cartPresenter.getMenuItemsCount();
    }

}
