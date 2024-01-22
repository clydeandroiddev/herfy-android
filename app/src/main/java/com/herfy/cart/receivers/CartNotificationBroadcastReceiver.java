package com.herfy.cart.receivers;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.herfy.R;
import com.herfy.constants.Text;
import com.herfy.main.MainActivity;
import com.herfy.util.BuildUtil;
import com.herfy.util.CacheUtil;
import com.herfy.analytics.FirebaseECommerceEventUtil;
import com.herfy.util.ViewUtil;

public class CartNotificationBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        ActivityManager.RunningAppProcessInfo process = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(process);
        boolean isInBackground = process.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
        Long abandonCartAlert = CacheUtil.getInstance().getConcept().getAbandonedCartAlertValue();
        if(isInBackground && abandonCartAlert != null && abandonCartAlert > 0 && CacheUtil.getInstance().getCartQuantity() != 0) {

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                NotificationChannel channel = notificationManager.getNotificationChannel(context.getResources().getString(R.string.app_name));

                if (channel == null) {

                    channel = new NotificationChannel(context.getResources().getString(R.string.app_name), context.getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
                    channel.setDescription(context.getResources().getString(R.string.app_name));
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .build();
                    channel.setSound(defaultSoundUri, audioAttributes);
                    channel.enableLights(true);
                    channel.enableVibration(true);
                    channel.setLightColor(Color.parseColor(String.format("#FF%06X", (0xFFFFFF & ViewUtil.getColorMain(context)))));
                    channel.setShowBadge(true);
                    channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                    channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

                    notificationManager.createNotificationChannel(channel);

                }

            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, context.getResources().getString(R.string.app_name));

            builder.setSmallIcon(R.drawable.notification);

            String appKey = BuildUtil.soloAppKey();
            if (appKey.equalsIgnoreCase("UKFQD1wwUBev4PRP") || appKey.equalsIgnoreCase("dYJux5BNAwTbEwEE")) {
                builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_large));
            } else {
                builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.splash));
            }
            FirebaseECommerceEventUtil.Companion.abandonedCartEvent(context, CacheUtil.getInstance().getCart());
            builder.setContentTitle(context.getResources().getString(R.string.app_name));

            String message = CacheUtil.getInstance().getTranslations(Text.ABANDON_ALERT_MESSAGE, "We noticed that you added one or more items to your cart, but didn't continue to checkout. When you are ready, simply visit your cart to complete your order.");
            builder.setContentText(message);

            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setSound(defaultSoundUri)
                    .setAutoCancel(true)
                    .setLights(Color.parseColor(String.format("#FF%06X", (0xFFFFFF & ViewUtil.getColorMain(context)))), 200, 200)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

            Intent mainIntent = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            mainIntent.putExtra("home", false);
            mainIntent.putExtra("menu", false);
            mainIntent.putExtra("cart", true);
            mainIntent.putExtra("settings", false);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            builder.setContentIntent(pendingIntent);

            int id = getNextNotificationId();

            if (notificationManager != null) {

                notificationManager.notify(String.valueOf(id), id, builder.build());

            }

        }
    }

    private int getNextNotificationId() {

        int id = CacheUtil.getInstance().getNotificationId();

        if (id == 0) {
            id = 1;
        }

        CacheUtil.getInstance().setNotificationId((id + 1) % Integer.MAX_VALUE );

        return id;
    }

}
