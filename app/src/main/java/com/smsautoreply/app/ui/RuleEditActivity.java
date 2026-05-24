package com.smsautoreply.app.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.smsautoreply.app.R;
import com.smsautoreply.app.db.AppDatabase;
import com.smsautoreply.app.db.RuleEntity;

import java.util.regex.Pattern;

/**
 * 规则编辑页面 - 新建或编辑规则
 */
public class RuleEditActivity extends AppCompatActivity {

    private EditText etName, etKeyword, etSenderPattern, etReplyContent, etForwardTarget;
    private Spinner spKeywordMode, spSenderMode, spAction;
    private Button btnSave;
    private LinearLayout layoutForward, layoutReply;
    private TextView tvForwardHint;
    private AppDatabase database;
    private long editRuleId = -1;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://.*");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rule_edit);

        database = AppDatabase.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();
        setupSpinners();

        editRuleId = getIntent().getLongExtra("rule_id", -1);

        if (editRuleId != -1) {
            loadRuleData(editRuleId);
        }

        btnSave.setOnClickListener(v -> saveRule());
    }

    private void initViews() {
        etName = findViewById(R.id.et_rule_name);
        etKeyword = findViewById(R.id.et_keyword);
        spKeywordMode = findViewById(R.id.sp_keyword_mode);
        etSenderPattern = findViewById(R.id.et_sender_pattern);
        spSenderMode = findViewById(R.id.sp_sender_mode);
        spAction = findViewById(R.id.sp_action);
        etReplyContent = findViewById(R.id.et_reply_content);
        etForwardTarget = findViewById(R.id.et_forward_target);
        layoutForward = findViewById(R.id.layout_forward);
        layoutReply = findViewById(R.id.layout_reply);
        tvForwardHint = findViewById(R.id.tv_forward_hint);
        btnSave = findViewById(R.id.btn_save);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> ka = ArrayAdapter.createFromResource(this,
                R.array.keyword_match_modes, android.R.layout.simple_spinner_item);
        ka.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spKeywordMode.setAdapter(ka);

        ArrayAdapter<CharSequence> sa = ArrayAdapter.createFromResource(this,
                R.array.sender_match_modes, android.R.layout.simple_spinner_item);
        sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSenderMode.setAdapter(sa);

        ArrayAdapter<CharSequence> aa = ArrayAdapter.createFromResource(this,
                R.array.action_types, android.R.layout.simple_spinner_item);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAction.setAdapter(aa);

        // 监听动作选择，动态切换提示文字
        spAction.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateForwardHint();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /**
     * 根据选择的动作类型，更新回复内容和转发目标的显隐及提示文字
     */
    private void updateForwardHint() {
        String action = getSpinnerValue(spAction, R.array.action_type_values);
        boolean needReply = "reply".equals(action) || "both".equals(action) || "reply_dingtalk".equals(action);
        boolean needForward = "forward".equals(action) || "both".equals(action);
        boolean needDingtalk = "dingtalk".equals(action) || "reply_dingtalk".equals(action);

        // 回复内容区：纯转发/纯钉钉时隐藏
        layoutReply.setVisibility(needReply ? View.VISIBLE : View.GONE);

        // 转发目标区：纯回复时隐藏
        if (needReply && !needForward && !needDingtalk) {
            // 纯回复，隐藏转发目标
            layoutForward.setVisibility(View.GONE);
        } else {
            layoutForward.setVisibility(View.VISIBLE);
            if (needDingtalk) {
                tvForwardHint.setText("填钉钉群机器人 Webhook 地址");
                etForwardTarget.setHint("https://oapi.dingtalk.com/robot/send?access_token=...");
            } else if (needForward) {
                tvForwardHint.setText("填手机号码（纯数字）");
                etForwardTarget.setHint("13800138000");
            }
        }
    }

    private void saveRule() {
        String name = etName.getText().toString().trim();
        String keyword = etKeyword.getText().toString().trim();
        String keywordMode = getSpinnerValue(spKeywordMode, R.array.keyword_match_mode_values);
        String senderPattern = etSenderPattern.getText().toString().trim();
        String senderMode = getSpinnerValue(spSenderMode, R.array.sender_match_mode_values);
        String action = getSpinnerValue(spAction, R.array.action_type_values);
        String replyContent = etReplyContent.getText().toString().trim();
        String forwardTarget = etForwardTarget.getText().toString().trim();

        // 基本校验
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "请输入规则名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(keyword) && TextUtils.isEmpty(senderPattern)) {
            Toast.makeText(this, "请至少填写关键词或号码模式", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean needReply = "reply".equals(action) || "both".equals(action) || "reply_dingtalk".equals(action);
        boolean needForward = "forward".equals(action) || "both".equals(action);
        boolean needDingtalk = "dingtalk".equals(action) || "reply_dingtalk".equals(action);

        if (needReply && TextUtils.isEmpty(replyContent)) {
            Toast.makeText(this, "包含回复的动作必须填写回复内容", Toast.LENGTH_SHORT).show();
            return;
        }

        if (needForward || needDingtalk) {
            if (TextUtils.isEmpty(forwardTarget)) {
                Toast.makeText(this, "请填写转发目标（手机号或钉钉 Webhook）", Toast.LENGTH_SHORT).show();
                return;
            }
            // 对填的内容做简单校验
            if (needForward && !PHONE_PATTERN.matcher(forwardTarget).matches()) {
                Toast.makeText(this, "短信转发目标看起来不是有效的手机号", Toast.LENGTH_SHORT).show();
                return;
            }
            if (needDingtalk && !URL_PATTERN.matcher(forwardTarget).matches()) {
                Toast.makeText(this, "钉钉推送目标看起来不是有效的 Webhook 地址（需以 http 开头）", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        RuleEntity rule = new RuleEntity();
        if (editRuleId != -1) rule.setId(editRuleId);
        rule.setName(name);
        rule.setKeyword(keyword.isEmpty() ? null : keyword);
        rule.setKeywordMatchMode(keywordMode);
        rule.setSenderPattern(senderPattern.isEmpty() ? null : senderPattern);
        rule.setSenderMatchMode(senderMode);
        rule.setAction(action);
        rule.setReplyContent(replyContent);
        rule.setForwardNumber(forwardTarget);
        rule.setEnabled(true);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (editRuleId != -1) {
                RuleEntity old = database.ruleDao().getRuleById(editRuleId);
                if (old != null) {
                    rule.setPriority(old.getPriority());
                    rule.setEnabled(old.isEnabled());
                }
                database.ruleDao().update(rule);
            } else {
                rule.setPriority(0);
                database.ruleDao().insert(rule);
            }
            runOnUiThread(() -> {
                Toast.makeText(RuleEditActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    /**
     * 加载已有规则的数据填充到界面
     */
    private void loadRuleData(long ruleId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            RuleEntity rule = database.ruleDao().getRuleById(ruleId);
            if (rule == null) return;
            runOnUiThread(() -> {
                etName.setText(rule.getName());

                // 设置关键词匹配模式
                String[] keywordValues = getResources().getStringArray(R.array.keyword_match_mode_values);
                for (int i = 0; i < keywordValues.length; i++) {
                    if (keywordValues[i].equals(rule.getKeywordMatchMode())) {
                        spKeywordMode.setSelection(i);
                        break;
                    }
                }
                if (rule.getKeyword() != null) etKeyword.setText(rule.getKeyword());

                // 设置号码匹配模式
                String[] senderValues = getResources().getStringArray(R.array.sender_match_mode_values);
                for (int i = 0; i < senderValues.length; i++) {
                    if (senderValues[i].equals(rule.getSenderMatchMode())) {
                        spSenderMode.setSelection(i);
                        break;
                    }
                }
                if (rule.getSenderPattern() != null) etSenderPattern.setText(rule.getSenderPattern());

                // 设置动作类型
                String[] actionValues = getResources().getStringArray(R.array.action_type_values);
                for (int i = 0; i < actionValues.length; i++) {
                    if (actionValues[i].equals(rule.getAction())) {
                        spAction.setSelection(i);
                        break;
                    }
                }

                if (rule.getReplyContent() != null) etReplyContent.setText(rule.getReplyContent());
                if (rule.getForwardNumber() != null) etForwardTarget.setText(rule.getForwardNumber());

                // 加载完成后更新转发目标提示
                updateForwardHint();
            });
        });
    }

    private String getSpinnerValue(Spinner spinner, int arrayResId) {
        String[] values = getResources().getStringArray(arrayResId);
        int pos = spinner.getSelectedItemPosition();
        return (pos >= 0 && pos < values.length) ? values[pos] : values[0];
    }
}
