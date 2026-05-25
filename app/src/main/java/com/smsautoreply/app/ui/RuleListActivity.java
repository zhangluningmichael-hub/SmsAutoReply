package com.smsautoreply.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smsautoreply.app.R;
import com.smsautoreply.app.adapter.RuleAdapter;
import com.smsautoreply.app.db.AppDatabase;
import com.smsautoreply.app.db.RuleEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RuleListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RuleAdapter adapter;
    private AppDatabase database;
    private View emptyView;

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri != null) doExport(uri);
            });

    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) doImport(uri);
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rule_list);

        database = AppDatabase.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_rules);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_export) {
                exportLauncher.launch("SmsAutoReply_rules.json");
                return true;
            } else if (id == R.id.action_import) {
                importLauncher.launch(new String[]{"application/json", "*/*"});
                return true;
            }
            return false;
        });

        recyclerView = findViewById(R.id.recycler_rules);
        emptyView = findViewById(R.id.empty_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RuleAdapter(this);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener((rule, position) -> {
            Intent intent = new Intent(RuleListActivity.this, RuleEditActivity.class);
            intent.putExtra("rule_id", rule.getId());
            startActivity(intent);
        });

        adapter.setOnItemToggleListener((rule, enabled, position) -> {
            AppDatabase.databaseWriteExecutor.execute(() ->
                    database.ruleDao().updateEnabled(rule.getId(), enabled));
        });

        adapter.setOnItemDeleteListener((rule, position) -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("确认删除")
                    .setMessage("确定要删除规则「" + rule.getName() + "」吗？此操作不可撤销。")
                    .setPositiveButton("删除", (dialog, which) ->
                            AppDatabase.databaseWriteExecutor.execute(() ->
                                    database.ruleDao().delete(rule)))
                    .setNegativeButton("取消", null)
                    .show();
        });

        database.ruleDao().getAllRules().observe(this, rules -> {
            adapter.setRules(rules);
            boolean empty = rules == null || rules.isEmpty();
            emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        findViewById(R.id.fab_add).setOnClickListener(v ->
                startActivity(new Intent(RuleListActivity.this, RuleEditActivity.class)));
    }

    // =========== 导出规则 ===========

    private void doExport(Uri uri) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<RuleEntity> rules = database.ruleDao().getAllRulesSync();
                JSONArray arr = new JSONArray();
                for (RuleEntity r : rules) {
                    JSONObject obj = new JSONObject();
                    obj.put("name", r.getName() != null ? r.getName() : "");
                    obj.put("enabled", r.isEnabled());
                    obj.put("priority", r.getPriority());
                    obj.put("keyword_match_mode", r.getKeywordMatchMode() != null ? r.getKeywordMatchMode() : "");
                    obj.put("keyword", r.getKeyword() != null ? r.getKeyword() : "");
                    obj.put("sender_match_mode", r.getSenderMatchMode() != null ? r.getSenderMatchMode() : "");
                    obj.put("sender_pattern", r.getSenderPattern() != null ? r.getSenderPattern() : "");
                    obj.put("action", r.getAction() != null ? r.getAction() : "");
                    obj.put("reply_content", r.getReplyContent() != null ? r.getReplyContent() : "");
                    obj.put("forward_number", r.getForwardNumber() != null ? r.getForwardNumber() : "");
                    arr.put(obj);
                }
                JSONObject root = new JSONObject();
                root.put("version", 1);
                root.put("export_time", System.currentTimeMillis());
                root.put("rules", arr);

                byte[] data = root.toString(2).getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    if (os != null) {
                        os.write(data);
                        os.flush();
                    }
                }
                runOnUiThread(() -> Toast.makeText(this,
                        "导出成功，共 " + rules.size() + " 条规则", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "导出失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    // =========== 导入规则 ===========

    private void doImport(Uri uri) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) {
                    runOnUiThread(() -> Toast.makeText(this, "无法读取文件", Toast.LENGTH_SHORT).show());
                    return;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                JSONObject root = new JSONObject(sb.toString());
                JSONArray arr = root.getJSONArray("rules");

                int imported = 0;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    RuleEntity rule = new RuleEntity();
                    rule.setName(obj.optString("name", ""));
                    rule.setEnabled(obj.optBoolean("enabled", true));
                    rule.setPriority(obj.optInt("priority", 0));
                    rule.setKeywordMatchMode(optStringNull(obj, "keyword_match_mode"));
                    rule.setKeyword(optStringNull(obj, "keyword"));
                    rule.setSenderMatchMode(optStringNull(obj, "sender_match_mode"));
                    rule.setSenderPattern(optStringNull(obj, "sender_pattern"));
                    rule.setAction(optStringNull(obj, "action"));
                    rule.setReplyContent(optStringNull(obj, "reply_content"));
                    rule.setForwardNumber(optStringNull(obj, "forward_number"));
                    rule.setId(0); // 确保是新插入
                    database.ruleDao().insert(rule);
                    imported++;
                }
                final int count = imported;
                runOnUiThread(() -> Toast.makeText(this,
                        "导入成功，共 " + count + " 条规则", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "导入失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private String optStringNull(JSONObject obj, String key) {
        String val = obj.optString(key, "");
        return val.isEmpty() ? null : val;
    }
}
