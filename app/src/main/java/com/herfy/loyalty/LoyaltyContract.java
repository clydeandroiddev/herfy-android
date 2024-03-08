package com.herfy.loyalty;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.herfy.loyalty.objects.ListItem;
import com.herfy.model.SortingCategory;
import com.skylinedynamics.solosdk.api.models.objects.Loyalty;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsPosRedemption;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransaction;
import com.skylinedynamics.solosdk.api.models.objects.Tier;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public interface LoyaltyContract {

    interface View extends BaseView<Presenter> {

        void showLoyalty(Loyalty loyalty, String name);

        void showLoyaltyError(String message);

        void showLoyaltyPointsHistory(boolean newestToOldest,  ArrayList<LoyaltyTransaction> loyaltyTransactions);

        void setLoyaltyHistory(LoyaltyTransaction loyaltyTransaction);

        void showNextTier(Tier tier, int totalAccumulatedPoints, int pointsUntilNextTier, int startingPointsOfNextTier);

        void showQRPage(LoyaltyPointsPosRedemption loyalty);

        void showQrData(String data);

        void showSortingCategories(ArrayList<SortingCategory> sortingCategories);

        void updateSortingCategory(SortingCategory category);

        void updateTransactions();
    }

    interface Presenter extends BasePresenter, HistorySortAdapter.CategoryListener {

        void getLoyalty();

        void getCustomerLoyalty();

        void getCustomerDetails();

        void getLoyaltyHistoryTransaction();

        void getLoyaltyHistoryTransaction2(int page);

        void onBindLoyaltyHistoryViewAtPosition(int position, LoyaltyPointsViewHolder loyaltyPointsViewHolder);

        int getLoyaltyPointsHistoryCount();

        List<Tier> getTiers();

        void sortHistory();

        void cleanup();

        void redeemPoints(int points);

        ArrayList<ListItem> getConsolidatedList();

        void getSortingCategories();
    }

}
