package com.herfy.loyalty;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsPosRedemption;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTierV2;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransaction;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyV2;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DateViewHolder extends RecyclerView.ViewHolder implements LoyaltyContract.View {

    private Context context;
    private LoyaltyHistoryContract.Presenter loyaltyPresenter;

    @BindView(R.id.tv_date) TextView tvDate;


    public DateViewHolder(Context context, LoyaltyHistoryContract.Presenter loyaltyPresenter, View itemView) {
        super(itemView);

        ButterKnife.bind(this, itemView);
        this.context = context;
        this.loyaltyPresenter = loyaltyPresenter;

        setupViews();
        setupFonts();
        setupTranslations();

    }


    @Override
    public void setPresenter(LoyaltyContract.Presenter presenter) {

    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {

    }

    @Override
    public void setCartExpiry() {

    }

    @Override
    public void setupViews() {

    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {

    }

    @Override
    public void showLoyalty(LoyaltyV2 loyalty, String name) {

    }

    @Override
    public void showLoyaltyError(String message) {

    }

    @Override
    public void showLoyaltyPointsHistory(boolean newestToOldest, ArrayList<LoyaltyTransaction> loyaltyTransactions) {

    }

    @Override
    public void setLoyaltyHistory(LoyaltyTransaction loyaltyTransaction) {

    }

    @Override
    public void showNextTier(LoyaltyTierV2 tier, int totalAccumulatedPoints, int pointsUntilNextTier, int startingPointsOfNextTier, boolean isMaxTier, LoyaltyV2 loyalty) {

    }

    @Override
    public void showQRPage(LoyaltyPointsPosRedemption loyalty) {

    }

    @Override
    public void showQrData(String data) {

    }

    @Override
    public void showTiers(LoyaltyV2 loyalty) {

    }
}
