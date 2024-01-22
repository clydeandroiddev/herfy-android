package com.herfy.notification;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.herfy.notification.views.NotificationViewHolder;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.notification.NotifData;
import com.skylinedynamics.solosdk.api.models.objects.notification.Notification;

import java.util.LinkedList;

import javax.annotation.Nullable;

public interface NotificationContract {

    interface View extends BaseView<Presenter> {

        void showNotifications(LinkedList<Notification> notifications, int pageCount);

        void setupNotification(Notification notification);

        void showNotificationDetails(NotifData notifData);

        void orderDeleted();

        void showNotifError();

        void showDigitalCoupon(Campaign digitalCoupon);

        void showError(String message);

    }

    interface Presenter extends BasePresenter {

        void getNotifications(int page);

        void getUnreadNotification();

        void markAllReadNotification();

        void clearAllNotif();

        void getNotification(String id);

        void deleteNotif(String notifId);

        void onBindNotificationViewAtPosition(int position, NotificationViewHolder notificationViewHolder);

        int getNotificationsCount();

        LinkedList<Notification> getNotificationList();

        @Nullable Notification getNotification(int position);

        void selectNotif(Notification notification);

        int getUnreadNotifCount();

        int getTotalNotifPageCount();

        void getDigitalCoupons(String digitalCouponId);

    }
}
