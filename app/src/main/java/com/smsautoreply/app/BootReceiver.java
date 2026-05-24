package com.smsautoreply.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 开机启动广播接收器
 * 延迟启动服务，避免开机时冲突
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            Log.d(TAG, "Boot completed, scheduling service start");

            // 延迟10秒启动服务，避免开机冲突
            new android.os.Handler(context.getMainLooper()).postDelayed(() -> {
                try {
                    Intent serviceIntent = new Intent(context, SmsService.class);
                    context.startService(serviceIntent);
                    Log.d(TAG, "Service started after boot");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start service after boot", e);
                }
            }, 10000);
        }
    }
}
