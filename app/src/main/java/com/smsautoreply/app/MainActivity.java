package com.smsautoreply.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.smsautoreply.app.ui.LogActivity;
import com.smsautoreply.app.ui.RuleListActivity;
import com.smsautoreply.app.ui.SettingsActivity;

/**
 * 主界面
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationHelper.createNotificationChannel(this);

        Button btnRules = findViewById(R.id.btn_rules);
        Button btnLogs = findViewById(R.id.btn_logs);
        Button btnSettings = findViewById(R.id.btn_settings);

        btnRules.setOnClickListener(v ->
            startActivity(new Intent(this, RuleListActivity.class)));
        btnLogs.setOnClickListener(v ->
            startActivity(new Intent(this, LogActivity.class)));
        btnSettings.setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));

        // 延迟启动 SmsService（静默，不阻塞 UI）
        findViewById(android.R.id.content).postDelayed(() -> {
            try {
                Intent svc = new Intent(this, SmsService.class);
                startService(svc);
                Log.d(TAG, "SmsService started silently");
            } catch (Exception e) {
                Log.e(TAG, "SmsService start failed (safe)", e);
            }
        }, 2000);
    }
}
