package com.herfy.verification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

public class CodeBroadcastReceiver extends BroadcastReceiver {

    public interface CodeBroadcastReceiverListener {
        void onReceived(Intent intent);
        void onTimeout();
    }

    public CodeBroadcastReceiverListener codeBroadcastReceiverListener;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            Status smsRetrieverStatus = (Status) extras.get(SmsRetriever.EXTRA_STATUS);

            switch (smsRetrieverStatus.getStatusCode()) {
                case CommonStatusCodes.SUCCESS:
                    if(codeBroadcastReceiverListener != null) {
                        codeBroadcastReceiverListener.onReceived(extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT));
                    }
                    break;
                case CommonStatusCodes.TIMEOUT:
                    if(codeBroadcastReceiverListener != null) {
                        codeBroadcastReceiverListener.onTimeout();
                    }
                    break;
            }
        }

    }

    public void setCodeBroadcastReceiverListener(CodeBroadcastReceiverListener codeBroadcastReceiverListener) {
        this.codeBroadcastReceiverListener = codeBroadcastReceiverListener;
    }

}
