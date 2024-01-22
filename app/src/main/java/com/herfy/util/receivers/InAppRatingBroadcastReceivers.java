package com.herfy.util.receivers;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.herfy.base.BaseActivity;

public class InAppRatingBroadcastReceivers extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ActivityManager.RunningAppProcessInfo process = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(process);
        boolean isInBackground = process.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;

        if(BaseActivity.currentActivity != null) {
            BaseActivity.currentActivity.showInAppRating();
        }
    }
}
