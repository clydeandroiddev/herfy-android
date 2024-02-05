package com.herfy.cart.receivers;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.herfy.base.BaseActivity;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;

import java.util.ArrayList;

public class CartExpiryBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        ActivityManager.RunningAppProcessInfo process = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(process);
        boolean isInBackground = process.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;

        if (CacheUtil.getInstance().getCartQuantity() != 0) {
            if (isInBackground) {
                removeMukafaaRedemption();
                CacheUtil.getInstance().setCart(new ArrayList<>());
                CacheUtil.getInstance().setUpsell(true);
                CacheUtil.getInstance().setCouponCode(null);
                CacheUtil.getInstance().setCouponName(null);
                CacheUtil.getInstance().setCouponMenuItem(null);
                CacheUtil.getInstance().setCouponToAdd(null);
            } else if (BaseActivity.currentActivity != null) {
                BaseActivity.currentActivity.clearCart();
            }
        }
    }

    private void removeMukafaaRedemption() {
        String transactionId = CacheUtil.getInstance().getMukafaaTransactionId();
        if (transactionId != null && !transactionId.isEmpty()) {
            CacheUtil.getInstance().clearMukafaa();
            CustomersApiCall.getInstance().deleteRedemption(Values.MUKAFAA_PROVIDER, transactionId, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {

                }

                @Override
                public void onError(Object o) {

                }
            });
        }
    }

}
