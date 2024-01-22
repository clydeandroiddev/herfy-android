package com.herfy.digitalcoupons;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.digitalcoupons.views.DigitalCouponViewHolder;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;

public class DigitalCouponsRecyclerViewAdapter extends RecyclerView.Adapter<DigitalCouponViewHolder> {

    private final DigitalCouponsContract.Presenter digitalCouponsPresenter;

    private OnAction onAction;

    public DigitalCouponsRecyclerViewAdapter(DigitalCouponsContract.Presenter digitalCouponsPresenter, OnAction onAction) {
        this.digitalCouponsPresenter = digitalCouponsPresenter;
        this.onAction = onAction;
    }

    @NonNull
    @Override
    public DigitalCouponViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DigitalCouponViewHolder(digitalCouponsPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_digital_coupons, parent, false), onAction);
    }

    public interface OnAction {
        void view(Campaign campaign, MaterialCardView card);
        void avail(Campaign campaign);
    }

    @Override
    public void onBindViewHolder(@NonNull DigitalCouponViewHolder holder, int position) {
        digitalCouponsPresenter.onBindDigitalCouponViewAtPosition(position, holder);
    }

    @Override
    public int getItemCount() {
        return digitalCouponsPresenter.getDigitalCouponsCount();
    }

}
