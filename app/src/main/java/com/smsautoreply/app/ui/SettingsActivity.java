package com.smsautoreply.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.smsautoreply.app.R;
import com.smsautoreply.app.RuleEngine;
import com.smsautoreply.app.SmsService;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchServiceEnabled;
    private Switch switchWhitelistMode;
    private EditText etWhitelistNumbers;
    private EditText etBlacklistNumbers;
    private Button btnSaveSettings;
    private RuleEngine ruleEngine;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ruleEngine = new RuleEngine(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        switchServiceEnabled = findViewById(R.id.switch_service_enabled);
        switchWhitelistMode = findViewById(R.id.switch_whitelist_mode);
        etWhitelistNumbers = findViewById(R.id.et_whitelist_numbers);
        etBlacklistNumbers = findViewById(R.id.et_blacklist_numbers);
        btnSaveSettings = findViewById(R.id.btn_save_settings);

        loadSettings();

        btnSaveSettings.setOnClickListener(v -> saveSettings());
    }

    private void loadSettings() {
        switchServiceEnabled.setChecked(ruleEngine.isServiceEnabled());
        switchWhitelistMode.setChecked(ruleEngine.isWhitelistMode());
        etWhitelistNumbers.setText(ruleEngine.getWhitelistNumbers());
        etBlacklistNumbers.setText(ruleEngine.getBlacklistNumbers());
    }

    private void saveSettings() {
        boolean serviceEnabled = switchServiceEnabled.isChecked();
        boolean whitelistMode = switchWhitelistMode.isChecked();
        String whitelist = etWhitelistNumbers.getText().toString().trim();
        String blacklist = etBlacklistNumbers.getText().toString().trim();

        ruleEngine.setServiceEnabled(serviceEnabled);
        ruleEngine.setWhitelistMode(whitelistMode);
        ruleEngine.setWhitelistNumbers(whitelist);
        ruleEngine.setBlacklistNumbers(blacklist);

        if (serviceEnabled) {
            startForegroundService(new Intent(this, SmsService.class));
            Toast.makeText(this, "服务已启动", Toast.LENGTH_SHORT).show();
        } else {
            stopService(new Intent(this, SmsService.class));
            Toast.makeText(this, "服务已停止", Toast.LENGTH_SHORT).show();
        }

        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        finish();
    }
}
