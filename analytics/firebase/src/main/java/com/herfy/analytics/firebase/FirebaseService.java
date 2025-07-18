package com.herfy.analytics.firebase;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.capillary.hydra.android.sdk.FcmConfig;
import com.capillary.hydra.android.sdk.HydraAPI;
import com.capillary.hydra.android.sdk.HydraConfig;
import com.capillary.hydra.android.sdk.NotificationConfig;
import com.capillary.hydra.android.sdk.exceptions.HydraConfigurationException;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import com.herfy.utils.constants.Navigation;
import com.herfy.utils.helper.*;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.UtilitiesApiCall;
import com.herfy.utils.helper.LocaleUtil;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class FirebaseService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {

        super.onNewToken(token);



        AuthUtil.getInstance().setFirebase(token);

        String uniqueID = AuthUtil.getInstance().getUniqueId();

        if (uniqueID.isEmpty()) {
            uniqueID = UUID.randomUUID().toString();
            AuthUtil.getInstance().setUniqueId(uniqueID);
        }

        String customerId = null;
        String customerMobileNumber = null;
        if (AuthUtil.getInstance().isSignedIn()) {
            customerId = AuthUtil.getInstance().getCustomer().getId();
            String key = CacheUtil.getInstance().getConceptKey();
            if(key.equalsIgnoreCase("CQcFAwgLDA0")) {
                customerMobileNumber = CacheUtil.getInstance().getConcept().getDialingCode() + AuthUtil.getInstance().getCustomer().getAttributes().getMobile().substring(1);
            } else {
                customerMobileNumber = AuthUtil.getInstance().getCustomer().getAttributes().getMobile();
            }
        }

        UtilitiesApiCall.getInstance().postIdentify(token, uniqueID, customerMobileNumber, customerId,
                new ApiRequestListener() {
                    @Override
                    public void onSuccess(Object object) {
                        DeviceUtil.save(object.toString());
                        if (AuthUtil.getInstance().isSignedIn() && !AuthUtil.getInstance().getDeviceId().isEmpty()) {

                            UtilitiesApiCall.getInstance().changeLanguage(AuthUtil.getInstance().getDeviceId(), LocaleUtil.getInstance().getLanguage(), new ApiRequestListener() {
                                @Override
                                public void onSuccess(Object obj) {
                                    DeviceUtil.save(obj.toString());
                                }

                                @Override
                                public void onError(Object error) {
                                    AppLogger.printRetrofitError(error);
                                }
                            });

                        }
                    }

                    @Override
                    public void onError(Object error) {
                        AppLogger.printRetrofitError(error);
                    }
                });

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {

        super.onTaskRemoved(rootIntent);

        startService(new Intent(this, FirebaseService.class));

    }

    @Override
    public void onMessageReceived(@NotNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getNotification() != null) {

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                NotificationChannel channel = notificationManager.getNotificationChannel(getResources().getString(R.string.app_name));

                if (channel == null) {

                    channel = new NotificationChannel(getResources().getString(R.string.app_name), getResources().getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
                    channel.setDescription(getResources().getString(R.string.app_name));
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .build();
                    channel.setSound(defaultSoundUri, audioAttributes);
                    channel.enableLights(true);
                    channel.enableVibration(true);
                    channel.setLightColor(Color.parseColor(String.format("#FF%06X", (0xFFFFFF & ViewUtil.getColorMain(this)))));
                    channel.setShowBadge(true);
                    channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                    channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

                    notificationManager.createNotificationChannel(channel);

                }

            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), getResources().getString(R.string.app_name));

            builder.setSmallIcon(R.drawable.notification);

            Bitmap bitmap = null;
            if(remoteMessage.getNotification().getImageUrl() != null) {
                FutureTarget<Bitmap> futureTarget = Glide.with(this)
                        .asBitmap()
                        .load(remoteMessage.getNotification().getImageUrl())
                        .submit();
                try {
                    bitmap = futureTarget.get();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if(bitmap != null) {
                builder.setLargeIcon(bitmap);
                builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(bitmap));
            } else{
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText(remoteMessage.getNotification().getBody()));
                builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.splash));
            }

            builder.setContentTitle(remoteMessage.getNotification().getTitle());
            builder.setContentText(remoteMessage.getNotification().getBody());

            builder.setSound(defaultSoundUri)
                    .setAutoCancel(true)
                    .setLights(Color.parseColor(String.format("#FF%06X", (0xFFFFFF & ViewUtil.getColorMain(this)))), 200, 200)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

            Intent mainIntent = new Intent();
                    mainIntent.setClassName(this, Navigation.SPLASH_ACTIVITY);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            String deepLink = remoteMessage.getData().get("deep-link");
            if (deepLink != null && !deepLink.isEmpty()) {
                String[] values = deepLink.split("/");
                if(values.length >= 2) {
                    deepLink = "https://" + BuildConfig.SOLO_APP.toLowerCase() + ".page.link/" + values[1].replace("-", "/");
                }
                DeepLinkUtil.parse(deepLink);
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            builder.setContentIntent(pendingIntent);

            int id = getNextNotificationId();

            if (notificationManager != null) {

                notificationManager.notify(String.valueOf(id), id, builder.build());

            }


/*            NotificationConfig notificationConfig = new NotificationConfig(
                    R.drawable.notification,
                    R.color.light_red,
                    true
            );

            HydraConfig hydraConfig = null;
            try {
                hydraConfig = new HydraConfig.Builder(getApplicationContext(),"rnu-282033",
                        "","","","","",
                        false,new FcmConfig(true),notificationConfig,false,0)
                        .build();
            } catch (HydraConfigurationException e) {
              e.printStackTrace();
            }

            HydraAPI.Companion.instanceWithConfig(getApplicationContext(), hydraConfig);*/

        }

    }

    public static void newNotification(String body, Context context) {
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(context.getResources().getString(R.string.app_name));
            if (channel == null) {
                channel = new NotificationChannel(context.getResources().getString(R.string.app_name), context.getResources().getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
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
        builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.splash));
        builder.setContentTitle(context.getResources().getString(R.string.app_name));
        builder.setContentText(body);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setSound(defaultSoundUri)
                .setAutoCancel(true)
                .setLights(Color.parseColor(String.format("#FF%06X", (0xFFFFFF & ViewUtil.getColorMain(context)))), 200, 200)
                .setPriority(Notification.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        Intent mainIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);
        int id = CacheUtil.getInstance().getNotificationId();
        if (id == 0) {
            id = 1;
        }
        CacheUtil.getInstance().setNotificationId((id + 1) % Integer.MAX_VALUE);
        if (notificationManager != null) {
            notificationManager.notify(String.valueOf(id), id, builder.build());
        }
    }

    private int getNextNotificationId() {
        int id = CacheUtil.getInstance().getNotificationId();
        if (id == 0) {
            id = 1;
        }
        CacheUtil.getInstance().setNotificationId((id + 1) % Integer.MAX_VALUE);
        return id;
    }
}
