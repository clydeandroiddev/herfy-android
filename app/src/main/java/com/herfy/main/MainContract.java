package com.herfy.main;

import com.bumptech.glide.request.RequestOptions;
import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.Rating;
import com.skylinedynamics.solosdk.api.models.objects.cms.CmsPage;

import java.util.List;

public interface MainContract {

    interface View extends BaseView<Presenter> {

        void setCaloriesPage(CmsPage caloriesPage);

        void showCart();

        void showFavorites();

        void setCartQuantity(int quantity);

        void showRatings(List<Rating> ratings);

        void showMessage(String message);

        void refreshOrderAgain();

        void showNewNotifIndicator(int count);

        void proceedRestart();

        void proceedMainRestart();

        void saveToCache(String key, String value);

        void preloadImage(String imageUrl, RequestOptions requestOptions);

        void showDigitalCoupon(Campaign digitalCoupon);

        void dismissDialogs();

        void gateWay();

        void multibrandValidation(boolean proceed);

        void setTermsAndConditions(String url);

    }

    interface Presenter extends BasePresenter {

        void changeLanguage();

        void getCartQuantity();

        void getCaloriesPage();

        void getOrders();

        void getOrderFeedbackTopics();

        void getOrderFeedbacks();

        void sendOrderFeedback(List<Rating> ratings);

        void detachAbly();

        void clearMenuCache();

        void geUnreadNotification();

        void getApplication();

        void getDigitalCoupons(String digitalCouponId);

        void multibrandValidation(boolean proceed);

        void getTermsAndConditions();

    }

}
