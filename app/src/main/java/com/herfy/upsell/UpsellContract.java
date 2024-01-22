package com.herfy.upsell;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;

import java.util.ArrayList;
import java.util.List;

public interface UpsellContract {

    interface View extends BaseView<Presenter> {

        void showUpsells(ArrayList<MenuItem> upsells);

        void setUpsell(MenuItem upsell);

        void setUpsellTotalPrice(double totalPrice);

        void continueCheckout();

        void showDigitalCoupons(boolean checkout, List<Campaign> digitalCoupons, int selected);

        void upsellPromotionSucess(boolean success);

        void logFirebaseEvent(String event);

        void goToItemDetails(int position, MenuItem upsell);

    }

    interface Presenter extends BasePresenter {

        boolean checkIfHasNoUpsellSelected();

        void getUpsells();

        void onBindUpsellViewAtPosition(int position, UpsellViewHolder upsellViewHolder);

        int getUpsellsCount();

        void changeQuantity(int position, boolean add);

        void changeToZero(int position);

        void continueCheckout();

        void getDigitalCoupons(boolean checkout);

        double getMenuItemTotalPrice(MenuItem menuItem);

        double getMenuItemsTotalPrice();

        void validateDigitalCoupon(Campaign campaign);

        void removeMukafaaRedemption();

        int getUpsellsList();

        ArrayList<MenuItem> getCurrentUpsells();

        void getUpsellTotalPrice();

        void goToItemDetails(int position, MenuItem upsell);


    }

}
