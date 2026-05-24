package com.smsautoreply.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

/**
 * 通知帮助类
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "sms_auto_reply_channel";
    private static final String CHANNEL_NAME = "短信自动回复";
    private static final String CHANNEL_DESC = "显示短信自动回复和转发的操作通知";
    private static final int NOTIFICATION_ID = 1001;

    private static boolean channelCreated = false;

    /**
     * 创建通知渠道（应在 Application 初始化时调用）
     */
    public static void createNotificationChannel(Context context) {
        if (channelCreated) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);
            channel.setShowBadge(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
        channelCreated = true;
    }

    /**
     * 发送操作通知
     */
    public static void sendNotification(Context context, String title, String content) {
        createNotificationChannel(context);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), notification);
        }
    }

    /**
     * 发送前台服务通知（用于 SmsService）
     */
    public static Notification buildServiceNotification(Context context, String content) {
        createNotificationChannel(context);

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("短信自动回复服务运行中")
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }
}
