package com.smsautoreply.app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * 后台短信服务
 * 普通Service（非前台Service），负责保持进程存活
 * Android 16 + vivo OriginOS 无法使用前台服务
 */
public class SmsService extends Service {

    private static final String TAG = "SmsService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SmsService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SmsService onStartCommand");
        // 如果进程被杀死，自动重启
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
