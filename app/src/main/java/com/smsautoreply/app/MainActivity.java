package com.smsautoreply.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.smsautoreply.app.ui.LogActivity;
import com.smsautoreply.app.ui.RuleListActivity;
import com.smsautoreply.app.ui.SettingsActivity;

/**
 * 主界面 - 卡片式菜单布局
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationHelper.createNotificationChannel(this);

        MaterialCardView cardRules = findViewById(R.id.card_rules);
        MaterialCardView cardLogs = findViewById(R.id.card_logs);
        MaterialCardView cardSettings = findViewById(R.id.card_settings);
        TextView tvServiceStatus = findViewById(R.id.tv_service_status);

        cardRules.setOnClickListener(v ->
            startActivity(new Intent(this, RuleListActivity.class)));
        cardLogs.setOnClickListener(v ->
            startActivity(new Intent(this, LogActivity.class)));
        cardSettings.setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));

        // 更新服务状态显示
        RuleEngine engine = new RuleEngine(this);
        boolean running = engine.isServiceEnabled();
        tvServiceStatus.setText(running ? "● 服务运行中" : "● 服务已停止");
        tvServiceStatus.setTextColor(running ? 0xFFA5D6A7 : 0xFFEF9A9A);

        // 延迟启动 SmsService（前台服务，降低被杀概率）
        findViewById(android.R.id.content).postDelayed(() -> {
            try {
                Intent svc = new Intent(this, SmsService.class);
                startForegroundService(svc);
                Log.d(TAG, "SmsService started silently");
                tvServiceStatus.setText("● 服务运行中");
                tvServiceStatus.setTextColor(0xFFA5D6A7);
            } catch (Exception e) {
                Log.e(TAG, "SmsService start failed (safe)", e);
            }
        }, 2000);
    }
}
