package com.herfy.newmenu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.newmenu.viewholder.MenuCouponViewHolder;

public class MenuCouponRecyclerViewAdapter extends RecyclerView.Adapter<MenuCouponViewHolder> {

    private Context context;

    private final MenuContract.Presenter menuPresenter;
    private boolean isFromSearch;

    public MenuCouponRecyclerViewAdapter(Context context, MenuContract.Presenter menuPresenter) {
        this.context = context;
        this.menuPresenter = menuPresenter;
    }

    @NonNull
    @Override
    public MenuCouponViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MenuCouponViewHolder(context, menuPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_digital_coupon_home, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MenuCouponViewHolder holder, int position) {
        menuPresenter.onBindDigitalCouponsViewAtPosition(position, holder);
    }

    @Override
    public int getItemCount() {
        return menuPresenter.latestOffersCount();
    }

}
