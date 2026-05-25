package com.smsautoreply.app;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * 前台短信服务
 * 以前台服务方式运行，降低被系统杀死的概率
 */
public class SmsService extends Service {

    private static final String TAG = "SmsService";
    private static final int FOREGROUND_NOTIFICATION_ID = 1002;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SmsService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SmsService onStartCommand");

        // 启动前台服务，显示持久通知降低被杀概率
        Notification notification = NotificationHelper.buildServiceNotification(this, "正在监控短信...");
        startForeground(FOREGROUND_NOTIFICATION_ID, notification);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SmsService destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
