package com.herfy.notification;

import androidx.annotation.Nullable;

import com.google.gson.reflect.TypeToken;
import com.herfy.database.AppDatabase;
import com.herfy.database.entities.MenuItemEntity;
import com.herfy.utils.constants.Text;
import com.herfy.notification.views.NotificationViewHolder;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.CouponsApiCall;
import com.skylinedynamics.solosdk.api.calls.MenuApiCall;
import com.skylinedynamics.solosdk.api.calls.NotificationApiCall;
import com.skylinedynamics.solosdk.api.models.objects.Allergen;
import com.skylinedynamics.solosdk.api.models.objects.AllergenAttributes;
import com.skylinedynamics.solosdk.api.models.objects.Campaign;
import com.skylinedynamics.solosdk.api.models.objects.Ingredient;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroupType;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.OrderType;
import com.skylinedynamics.solosdk.api.models.objects.Promotion;
import com.skylinedynamics.solosdk.api.models.objects.Store;
import com.skylinedynamics.solosdk.api.models.objects.Translated;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfig;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfigRule;
import com.skylinedynamics.solosdk.api.models.objects.notification.NotifData;
import com.skylinedynamics.solosdk.api.models.objects.notification.Notification;
import com.skylinedynamics.solosdk.util.GsonHelper;
import com.herfy.util.AppLogger;
import com.herfy.utils.handler.ErrorHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class NotificationPresenter implements  NotificationContract.Presenter {

    private LinkedList<Notification> notifications = new LinkedList<>();
    private NotifData notifData = new NotifData();
    private final NotificationContract.View notificationView;
    private int unreadNotifCount = 0;
    private int totalNotifPageCount = 0;
    private ArrayList<Campaign> digitalCoupons = new ArrayList<>();

    public NotificationPresenter(NotificationContract.View notificationView) {
        this.notificationView = notificationView;
        this.notificationView.setPresenter(this);
    }


    @Override
    public void start() {
        notificationView.setupTranslations();
        notificationView.setupViews();
        notificationView.setupFonts();
    }

    @Override
    public void getNotifications(int page) {
        if(page == 1) {
            notificationView.setIsLoading(true);
        }
        NotificationApiCall.getInstance().getNotifications(String.valueOf(page), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    if (page == 1) {
                        notifications.clear();
                    }
                    JSONObject jObject = new JSONObject(o.toString());
                    JSONArray jArray =jObject.getJSONArray("data");
                    totalNotifPageCount = jObject.getJSONObject("meta").getJSONObject("pagination").getInt("total_pages");
                    notifications.addAll(GsonHelper.gsonFinal().fromJson(jArray.toString(), new TypeToken<LinkedList<Notification>>(){}.getType()));
                    notificationView.setIsLoading(false);
                    notificationView.showNotifications(notifications, totalNotifPageCount);
//                    if (page == totalNotifPageCount) {
//                        notificationView.showNotifications(notifications);
//                    } else {
//                        getNotifications(page + 1);
//                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Object o) {
                notificationView.showNotifications(notifications, totalNotifPageCount);
            }
        });


    }

    @Override
    public void getUnreadNotification() {
        NotificationApiCall.getInstance().getUnreadNotifications(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject jObject = new JSONObject(o.toString());
                    unreadNotifCount = jObject.getJSONObject("meta").getInt("total");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Object o) {

            }
        });
    }

    @Override
    public void markAllReadNotification() {

    }

    @Override
    public void clearAllNotif() {
        NotificationApiCall.getInstance().clearAllReadNotifications(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                getNotifications(1);
            }

            @Override
            public void onError(Object o) {

            }
        });

    }

    @Override
    public void getNotification(String id) {
        NotificationApiCall.getInstance().getNotification(id, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject jObject = new JSONObject(o.toString());
                    notifData = GsonHelper.gsonFinal().fromJson(jObject.getJSONObject("data").getJSONObject("attributes").toString(), new TypeToken<NotifData>(){}.getType());
                    notificationView.showNotificationDetails(notifData);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Object o) {

            }
        });
    }

    @Override
    public void deleteNotif(String notifId) {
        NotificationApiCall.getInstance().deleteNotification(notifId, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                notificationView.orderDeleted();
            }

            @Override
            public void onError(Object o) {
                notificationView.orderDeleted();
            }
        });
    }

    @Override
    public int getUnreadNotifCount() {
        return unreadNotifCount;
    }

    @Override
    public int getTotalNotifPageCount() {
        return totalNotifPageCount;
    }

    @Override
    public void getDigitalCoupons(String digitalCouponId) {
        if(digitalCoupons.isEmpty()) {
            CouponsApiCall.getInstance().getGuestCampaigns(new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    try {
                        JSONObject object = new JSONObject(o.toString());
                        digitalCoupons = GsonHelper.gsonFinal().fromJson(object.getJSONArray("data").toString(), new TypeToken<ArrayList<Campaign>>() {
                        }.getType());

                        if(!digitalCoupons.isEmpty()) {

                            if (object.has("included")) {

                                ArrayList<Promotion> promotions = GsonHelper.gsonFinal().fromJson(object.getJSONArray("included").toString(), new TypeToken<ArrayList<Promotion>>() {
                                }.getType());
                                HashMap<String, Promotion> promotionsHashMap = new HashMap<>();

                                for (Promotion promotion : promotions) {
                                    promotionsHashMap.put(promotion.getId(), promotion);
                                }

                                for (Campaign digitalCoupon : digitalCoupons) {
                                    digitalCoupon.setPromotion(promotionsHashMap.get(digitalCoupon.getAttributes().getPromotionId()));
                                }

                                boolean found = false;
                                for (Campaign digitalCoupon : digitalCoupons) {
                                    if (digitalCoupon.getId().equalsIgnoreCase(digitalCouponId)) {
                                        found = true;
                                        notificationView.showDigitalCoupon(digitalCoupon);
                                        break;
                                    }
                                }

                                if(!found) {
                                    notificationView.showError(CacheUtil.getInstance().getTranslations(Text.COUPON_DEFAULT_ERROR, "Invalid coupon"));
                                }

                            }
                        } else {
                            notificationView.showError(CacheUtil.getInstance().getTranslations(Text.COUPON_DEFAULT_ERROR, "Invalid coupon"));
                        }

                    } catch (JSONException ex) {
                        ex.printStackTrace();
                        this.onError("");
                    }
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    String detail = ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null);
                    notificationView.showError(detail);
                }
            });
        } else {
            boolean found = false;
            for (Campaign digitalCoupon : digitalCoupons) {
                if (digitalCoupon.getId().equalsIgnoreCase(digitalCouponId)) {
                    found = true;
                    notificationView.showDigitalCoupon(digitalCoupon);
                    break;
                }
            }
            if(!found) {
                digitalCoupons.clear();
                getDigitalCoupons(digitalCouponId);
            }
        }
    }

    @Override
    public void markNotificationAsRead(int position, String notificationId) {
        NotificationApiCall.getInstance().markNotificationAsRead(notificationId, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                if(position < notifications.size()) {
                    notifications.get(position).getNotifData().setReadAt((new Date()).toString());
                    notificationView.markNotificationAsRead(position);
                }
            }

            @Override
            public void onError(Object o) {

            }
        });
    }

    @Override
    public void getMenuItem(String menuItemId) {
        MenuApiCall.getInstance().getAllItemDetails(CacheUtil.getInstance().getMenuId(), menuItemId, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    JSONArray dataArray = object.getJSONArray("data");

                    if (dataArray.length() > 0) {
                        MenuItem menuItem = GsonHelper.gsonFinal().fromJson(dataArray.getJSONObject(0).toString(), MenuItem.class);
                        menuItem.toCdn(CacheUtil.getOrderStoreId());

                        try {
                            menuItem.isCombo = dataArray.getJSONObject(0).getJSONObject("attributes").getBoolean("is-combo");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        String categoryId = getMenuCategory(menuItem.getId());
                        notificationView.goToItemDetails(categoryId, menuItem);

                    } else {
                        this.onError("");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
            }
        });

    }

    private String getMenuCategory(String menuItemId) {
        String applicationId = CacheUtil.getInstance().getApplication().getId();
        AppDatabase appDatabase = AppDatabase.getInstance();
        MenuItemEntity menuItemEntity = appDatabase.menuItemEntityDao().findById(applicationId, menuItemId);
        if (menuItemEntity != null) {
            return menuItemEntity.getMenuCategoryId();
        }
        return "";
    }

    @Override
    public void onBindNotificationViewAtPosition(int position, NotificationViewHolder notificationViewHolder) {
        notificationViewHolder.setupNotification(notifications.get(position));
    }

    @Override
    public int getNotificationsCount() {
        return notifications.size();
    }

    @Override
    public LinkedList<Notification> getNotificationList() {
        return notifications;
    }

    @Nullable
    @Override
    public Notification getNotification(int position) {
        if(position < notifications.size())  {
            return notifications.get(position);
        }
        return null;
    }

    @Override
    public void selectNotif(Notification notification) {

    }
}
