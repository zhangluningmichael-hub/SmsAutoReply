package com.smsautoreply.app.ui;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.smsautoreply.app.R;
import com.smsautoreply.app.db.AppDatabase;
import com.smsautoreply.app.db.LogEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogActivity extends AppCompatActivity {

    private ListView listView;
    private LogArrayAdapter adapter;
    private List<LogEntry> logItems = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // 菜单：清空日志
        toolbar.inflateMenu(R.menu.menu_log);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_clear_logs) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase.getInstance(this).logDao().deleteAll();
                    runOnUiThread(() -> Toast.makeText(this, "日志已清空", Toast.LENGTH_SHORT).show());
                });
                return true;
            }
            return false;
        });

        listView = findViewById(R.id.list_logs);
        adapter = new LogArrayAdapter(this, logItems);
        listView.setAdapter(adapter);

        // 加载日志 - 使用自定义适配器配合 item_log.xml
        AppDatabase db = AppDatabase.getInstance(this);
        db.logDao().getActionLogs().observe(this, logs -> {
            logItems.clear();
            if (logs != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());
                for (LogEntity log : logs) {
                    logItems.add(new LogEntry(
                            sdf.format(new Date(log.getTimestamp())),
                            log.getSender(),
                            log.getMessage(),
                            log.getDetail(),
                            log.getActionType()
                    ));
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    // 日志条目数据类
    static class LogEntry {
        String time;
        String sender;
        String message;
        String detail;
        String actionType;

        LogEntry(String time, String sender, String message, String detail, String actionType) {
            this.time = time;
            this.sender = sender;
            this.message = message;
            this.detail = detail;
            this.actionType = actionType;
        }

        String getActionLabel() {
            switch (actionType) {
                case "replied": return "📤 已回复";
                case "forwarded": return "↗️ 已转发";
                case "both": return "🔄 已回复并转发";
                case "error": return "❌ 执行失败";
                default: return "📥 收到";
            }
        }

        int getActionColor() {
            switch (actionType) {
                case "replied":
                case "forwarded":
                case "both": return 0xFF16A34A; // success green
                case "error": return 0xFFDC2626; // error red
                default: return 0xFF1565C0; // primary
            }
        }
    }

    // 自定义适配器
    static class LogArrayAdapter extends android.widget.ArrayAdapter<LogEntry> {
        LogArrayAdapter(LogActivity context, List<LogEntry> items) {
            super(context, 0, items);
        }

        @Override
        public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
            LogEntry entry = getItem(position);
            if (convertView == null) {
                convertView = android.view.LayoutInflater.from(getContext())
                        .inflate(R.layout.item_log, parent, false);
            }

            android.widget.TextView tvTime = convertView.findViewById(R.id.tv_log_time);
            android.widget.TextView tvActionType = convertView.findViewById(R.id.tv_log_action_type);
            android.widget.TextView tvSender = convertView.findViewById(R.id.tv_log_sender);
            android.widget.TextView tvMessage = convertView.findViewById(R.id.tv_log_message);
            android.widget.TextView tvDetail = convertView.findViewById(R.id.tv_log_detail);

            tvTime.setText(entry.time);
            tvActionType.setText(entry.getActionLabel());
            tvActionType.setTextColor(entry.getActionColor());
            tvSender.setText("来自: " + entry.sender);
            tvMessage.setText(entry.message);
            tvDetail.setText(entry.detail);

            return convertView;
        }
    }
}
