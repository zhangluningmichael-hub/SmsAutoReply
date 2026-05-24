package com.smsautoreply.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smsautoreply.app.R;
import com.smsautoreply.app.adapter.RuleAdapter;
import com.smsautoreply.app.db.AppDatabase;
import com.smsautoreply.app.db.RuleEntity;

public class RuleListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RuleAdapter adapter;
    private AppDatabase database;
    private View emptyView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rule_list);

        database = AppDatabase.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_rules);
        emptyView = findViewById(R.id.empty_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RuleAdapter(this);
        recyclerView.setAdapter(adapter);

        // 点击编辑规则
        adapter.setOnItemClickListener((rule, position) -> {
            Intent intent = new Intent(RuleListActivity.this, RuleEditActivity.class);
            intent.putExtra("rule_id", rule.getId());
            startActivity(intent);
        });

        // 开关切换
        adapter.setOnItemToggleListener((rule, enabled, position) -> {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                database.ruleDao().updateEnabled(rule.getId(), enabled);
            });
        });

        // 删除规则（带二次确认）
        adapter.setOnItemDeleteListener((rule, position) -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("确认删除")
                    .setMessage("确定要删除规则「" + rule.getName() + "」吗？此操作不可撤销。")
                    .setPositiveButton("删除", (dialog, which) -> {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            database.ruleDao().delete(rule);
                        });
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        // 加载数据
        database.ruleDao().getAllRules().observe(this, rules -> {
            adapter.setRules(rules);
            boolean empty = rules == null || rules.isEmpty();
            emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        // 添加按钮
        findViewById(R.id.fab_add).setOnClickListener(v -> {
            startActivity(new Intent(RuleListActivity.this, RuleEditActivity.class));
        });
    }
}
