package com.smsautoreply.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            return;
        }

        try {
            Bundle bundle = intent.getExtras();
            if (bundle == null) return;

            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null || pdus.length == 0) return;

            StringBuilder fullMessage = new StringBuilder();
            String sender = "";

            for (Object pdu : pdus) {
                SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                if (sms == null) continue;

                if (sender.isEmpty()) {
                    sender = sms.getOriginatingAddress();
                }
                fullMessage.append(sms.getMessageBody());
            }

            if (sender == null || sender.isEmpty()) return;

            Log.d(TAG, "收到短信 from " + sender + ": " + fullMessage.toString());

            // 用 RuleEngine 处理
            try {
                RuleEngine engine = new RuleEngine(context);
                engine.processIncomingSms(sender, fullMessage.toString());
            } catch (Exception e) {
                Log.e(TAG, "RuleEngine process failed", e);
            }

        } catch (Exception e) {
            Log.e(TAG, "SmsReceiver error", e);
        }
    }
}
