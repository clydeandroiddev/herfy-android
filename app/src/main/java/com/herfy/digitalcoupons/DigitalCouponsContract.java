package com.herfy.digitalcoupons;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.herfy.digitalcoupons.views.DigitalCouponViewHolder;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;

import java.util.ArrayList;

public interface DigitalCouponsContract {

    interface View extends BaseView<Presenter> {

        void showCart();

        void showDigitalCoupons(ArrayList<Campaign> digitalCoupons);

        void showDigitalCoupon(Campaign digitalCoupon);

        void showError(String error, boolean back);

    }

    interface Presenter extends BasePresenter {

        void getDigitalCoupons();

        void setDigitalCoupon(String value);

        Campaign getDigitalCoupon();

        void onBindDigitalCouponViewAtPosition(int position, DigitalCouponViewHolder digitalCouponViewHolder);

        int getDigitalCouponsCount();

        double getMenuItemTotalPrice(MenuItem menuItem);

        double getMenuItemsTotalPrice();

        void validateDigitalCoupon();

        void getCampaignPromotionItem();

    }

}
