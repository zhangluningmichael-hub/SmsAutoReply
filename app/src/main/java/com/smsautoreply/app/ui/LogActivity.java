package com.smsautoreply.app.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.smsautoreply.app.R;
import com.smsautoreply.app.db.AppDatabase;
import com.smsautoreply.app.db.LogEntity;

import java.util.ArrayList;
import java.util.List;

public class LogActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> logItems = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        listView = findViewById(R.id.list_logs);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, logItems);
        listView.setAdapter(adapter);

        // 设置Toolbar支持菜单
        toolbar.inflateMenu(R.menu.menu_log);

        // 加载日志 - 只显示执行了自动操作的记录
        AppDatabase db = AppDatabase.getInstance(this);
        db.logDao().getActionLogs().observe(this, logs -> {
            logItems.clear();
            if (logs != null) {
                for (LogEntity log : logs) {
                    String actionLabel;
                    switch (log.getActionType()) {
                        case "replied": actionLabel = "✅ 已回复"; break;
                        case "forwarded": actionLabel = "↗️ 已转发"; break;
                        case "both": actionLabel = "✅↗️ 已回复并转发"; break;
                        case "error": actionLabel = "❌ 执行失败"; break;
                        default: actionLabel = log.getActionType();
                    }
                    logItems.add("[" + actionLabel + "] " + log.getSender() + ": " + log.getMessage());
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_clear_logs) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase.getInstance(this).logDao().deleteAll();
                runOnUiThread(() -> {
                    Toast.makeText(this, "日志已清空", Toast.LENGTH_SHORT).show();
                    logItems.clear();
                    adapter.notifyDataSetChanged();
                });
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
