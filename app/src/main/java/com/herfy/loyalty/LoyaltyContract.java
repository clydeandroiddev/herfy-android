package com.herfy.loyalty;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.skylinedynamics.solosdk.api.models.objects.Loyalty;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsPosRedemption;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransaction;
import com.skylinedynamics.solosdk.api.models.objects.Tier;

import java.util.ArrayList;
import java.util.List;

public interface LoyaltyContract {

    interface View extends BaseView<Presenter> {

        void showLoyalty(Loyalty loyalty, String name);

        void showLoyaltyError(String message);

        void showLoyaltyPointsHistory(boolean newestToOldest,  ArrayList<LoyaltyTransaction> loyaltyTransactions);

        void setLoyaltyHistory(LoyaltyTransaction loyaltyTransaction);

        void showNextTier(Tier tier, int totalAccumulatedPoints, int pointsUntilNextTier, int startingPointsOfNextTier, boolean isMaxTier, Loyalty loyalty);

        void showQRPage(LoyaltyPointsPosRedemption loyalty);

        void showQrData(String data);

        void showTiers(Loyalty loyalty);
    }

    interface Presenter extends BasePresenter {
        void getLoyalty();
        void getCustomerLoyalty();
        List<Tier> getTiers();
        void redeemPoints(int points);
        void setTiers(List<Tier> tiers);


    }

}
