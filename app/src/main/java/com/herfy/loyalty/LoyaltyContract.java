package com.herfy.loyalty;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsPosRedemption;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTierV2;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransaction;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyV2;

import java.util.ArrayList;
import java.util.List;

public interface LoyaltyContract {

    interface View extends BaseView<Presenter> {

        void showLoyalty(LoyaltyV2 loyalty, String name);

        void showLoyaltyError(String message);

        void showLoyaltyPointsHistory(boolean newestToOldest,  ArrayList<LoyaltyTransaction> loyaltyTransactions);

        void setLoyaltyHistory(LoyaltyTransaction loyaltyTransaction);

        void showNextTier(LoyaltyTierV2 tier, int totalAccumulatedPoints, int pointsUntilNextTier, int startingPointsOfNextTier, boolean isMaxTier, LoyaltyV2 loyalty);

        void showQRPage(LoyaltyPointsPosRedemption loyalty);

        void showQrData(String data);

        void showTiers(LoyaltyV2 loyalty);
    }

    interface Presenter extends BasePresenter {
        void getLoyalty();
        void getCustomerLoyalty();
        List<LoyaltyTierV2> getTiers();
        void redeemPoints(int points);
        void setTiers(List<LoyaltyTierV2> tiers);


    }

}
