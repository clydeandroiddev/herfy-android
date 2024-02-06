package com.herfy.ratings;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.herfy.base.BaseActivity;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.Concept;

public class AppRatingBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ActivityManager.RunningAppProcessInfo process = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(process);
        boolean isInBackground = process.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
        Concept concept = CacheUtil.getInstance().getConcept();
        if(concept != null) {
            long promptRatingSeconds = CacheUtil.getInstance().getConcept().getPromptRatingSeconds();
            if(promptRatingSeconds > 0) {
                if(isInBackground) {
                    CacheUtil.getInstance().setAppReview(true);
                } else if(BaseActivity.currentActivity != null) {
                    BaseActivity.currentActivity.showAppReview();
                }
            }
        }
    }

}
