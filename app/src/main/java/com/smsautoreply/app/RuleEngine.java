package com.smsautoreply.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.util.Log;

import com.smsautoreply.app.db.AppDatabase;
import com.smsautoreply.app.db.LogEntity;
import com.smsautoreply.app.db.RuleDao;
import com.smsautoreply.app.db.RuleEntity;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 规则引擎
 * 根据配置的规则匹配短信，执行回复或转发操作
 */
public class RuleEngine {

    private static final String TAG = "RuleEngine";
    private static final String PREFS_NAME = "sms_auto_reply_prefs";
    private static final String KEY_SERVICE_ENABLED = "service_enabled";
    private static final String KEY_WHITELIST_MODE = "whitelist_mode";
    private static final String KEY_WHITELIST_NUMBERS = "whitelist_numbers";
    private static final String KEY_BLACKLIST_NUMBERS = "blacklist_numbers";

    private final Context context;
    private final AppDatabase database;
    private final RuleDao ruleDao;

    public RuleEngine(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(this.context);
        this.ruleDao = database.ruleDao();
    }

    /**
     * 处理收到的短信
     *
     * @param sender  发送者号码
     * @param message 短信内容
     */
    public void processIncomingSms(final String sender, final String message) {
        // 检查服务是否开启
        if (!isServiceEnabled()) {
            Log.d(TAG, "Service is disabled, ignoring SMS");
            return;
        }

        // 检查黑白名单
        if (!isAllowedByFilter(sender)) {
            Log.d(TAG, "Sender " + sender + " is blocked by filter");
            logAction(sender, message, "no_match", "号码被黑白名单过滤");
            return;
        }

        // 在后台线程中处理
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 记录收到短信
            logAction(sender, message, "received", "收到短信");

            // 获取所有启用的规则（同步查询，我们在后台线程上）
            List<RuleEntity> rules = database.ruleDao().getEnabledRulesSync();
            if (rules == null || rules.isEmpty()) {
                logAction(sender, message, "no_match", "无启用的规则");
                return;
            }

            boolean matched = false;
            for (RuleEntity rule : rules) {
                if (matchRule(rule, sender, message)) {
                    matched = true;
                    executeAction(rule, sender, message);
                    break; // 只执行第一个匹配的规则
                }
            }

            if (!matched) {
                logAction(sender, message, "no_match", "无匹配规则");
            }
        });
    }

    /**
     * 判断是否匹配规则
     */
    private boolean matchRule(RuleEntity rule, String sender, String message) {
        // 检查号码是否匹配
        if (!matchSender(rule, sender)) {
            return false;
        }

        // 检查关键词是否匹配
        if (!matchKeyword(rule, message)) {
            return false;
        }

        return true;
    }

    /**
     * 号码匹配
     */
    private boolean matchSender(RuleEntity rule, String sender) {
        String mode = rule.getSenderMatchMode();
        String pattern = rule.getSenderPattern();

        if (pattern == null || pattern.isEmpty()) {
            // 号码模式为空表示匹配任意号码
            return true;
        }

        if (sender == null) return false;

        switch (mode != null ? mode : "any") {
            case "exact":
                return sender.equals(pattern);
            case "starts_with":
                return sender.startsWith(pattern);
            case "contains":
                return sender.contains(pattern);
            case "any":
            default:
                return true;
        }
    }

    /**
     * 关键词匹配
     */
    private boolean matchKeyword(RuleEntity rule, String message) {
        String mode = rule.getKeywordMatchMode();
        String keyword = rule.getKeyword();

        if (keyword == null || keyword.isEmpty()) {
            // 关键词为空表示匹配任意内容
            return true;
        }

        if (message == null) return false;

        switch (mode != null ? mode : "contains") {
            case "contains":
                return message.contains(keyword);
            case "equals":
                return message.equals(keyword);
            case "starts_with":
                return message.startsWith(keyword);
            case "regex":
                try {
                    return Pattern.compile(keyword).matcher(message).find();
                } catch (PatternSyntaxException e) {
                    Log.e(TAG, "Invalid regex pattern: " + keyword, e);
                    return false;
                }
            default:
                return message.contains(keyword);
        }
    }

    /**
     * 执行规则的回复/转发动作
     */
    private void executeAction(RuleEntity rule, String sender, String originalMessage) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String action = rule.getAction() != null ? rule.getAction() : "reply";
            String forwardTarget = rule.getForwardNumber(); // 可以是手机号或Webhook地址

            switch (action) {
                case "reply":
                    sendReply(smsManager, sender, rule.getReplyContent());
                    logAction(sender, originalMessage, "replied",
                            "回复内容: " + rule.getReplyContent());
                    sendNotification("已自动回复", "已回复 " + sender + ": " + rule.getReplyContent());
                    break;

                case "forward":
                    if (forwardTarget != null && forwardTarget.startsWith("http")) {
                        // 钉钉 Webhook 地址
                        pushToDingTalk(forwardTarget, "📩 **短信转发**\n发件人: " + sender + "\n内容: " + originalMessage);
                        logAction(sender, originalMessage, "forwarded",
                                "推送到钉钉: " + forwardTarget);
                        sendNotification("已推送到钉钉", "已将短信推送到钉钉群");
                    } else {
                        // 普通短信转发
                        sendSmsForward(smsManager, forwardTarget, sender, originalMessage);
                        logAction(sender, originalMessage, "forwarded",
                                "转发到号码: " + forwardTarget);
                        sendNotification("已自动转发", "已将短信转发到 " + forwardTarget);
                    }
                    break;

                case "dingtalk":
                    pushToDingTalk(forwardTarget, "📩 **短信转发**\n发件人: " + sender + "\n内容: " + originalMessage);
                    logAction(sender, originalMessage, "forwarded",
                            "推送到钉钉");
                    sendNotification("已推送到钉钉", "已将短信推送到钉钉群");
                    break;

                case "reply_dingtalk":
                    sendReply(smsManager, sender, rule.getReplyContent());
                    pushToDingTalk(forwardTarget,
                            "✅ **已自动回复**\n发件人: " + sender + "\n原始短信: " + originalMessage
                                    + "\n回复内容: " + rule.getReplyContent());
                    logAction(sender, originalMessage, "both",
                            "回复内容: " + rule.getReplyContent() + "，推送到钉钉");
                    sendNotification("已回复并推送钉钉", "已回复 " + sender + " 并推送到钉钉群");
                    break;

                case "both":
                    sendReply(smsManager, sender, rule.getReplyContent());
                    if (forwardTarget != null && forwardTarget.startsWith("http")) {
                        // 钉钉
                        pushToDingTalk(forwardTarget,
                                "✅ **已回复并转发**\n发件人: " + sender + "\n原始短信: " + originalMessage
                                        + "\n回复内容: " + rule.getReplyContent());
                        logAction(sender, originalMessage, "both",
                                "回复内容: " + rule.getReplyContent() + "，推送到钉钉");
                        sendNotification("已回复并推送钉钉", "已回复 " + sender + " 并推送到钉钉群");
                    } else {
                        // 短信转发
                        sendSmsForward(smsManager, forwardTarget, sender, originalMessage);
                        logAction(sender, originalMessage, "both",
                                "回复内容: " + rule.getReplyContent()
                                        + ", 转发到: " + forwardTarget);
                        sendNotification("已自动回复并转发", "已回复 " + sender + " 并转发到 " + forwardTarget);
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing action for rule: " + rule.getName(), e);
            logAction(sender, originalMessage, "error", "执行失败: " + e.getMessage());
        }
    }

    /**
     * 发送回复短信
     */
    private void sendReply(SmsManager smsManager, String destination, String content) {
        if (content == null || content.isEmpty()) return;
        smsManager.sendTextMessage(destination, null, content, null, null);
    }

    /**
     * 短信转发（到目标号码）
     */
    private void sendSmsForward(SmsManager smsManager, String forwardNumber, String originalSender, String originalContent) {
        if (forwardNumber == null || forwardNumber.isEmpty()) return;
        String forwardMessage = "来自 [" + originalSender + "] 的短信:\n" + originalContent;
        smsManager.sendTextMessage(forwardNumber, null, forwardMessage, null, null);
    }

    /**
     * 记录操作日志
     */
    private void logAction(String sender, String message, String actionType, String detail) {
        LogEntity log = new LogEntity();
        log.setTimestamp(System.currentTimeMillis());
        log.setSender(sender);
        log.setMessage(message);
        log.setActionType(actionType);
        log.setDetail(detail);

        database.logDao().insert(log);
    }

    /**
     * 发送通知
     */
    private void sendNotification(String title, String content) {
        NotificationHelper.sendNotification(context, title, content);
    }

    /**
     * 检查是否通过黑白名单过滤
     */
    private boolean isAllowedByFilter(String sender) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean whitelistMode = prefs.getBoolean(KEY_WHITELIST_MODE, false);
        String whitelistStr = prefs.getString(KEY_WHITELIST_NUMBERS, "");
        String blacklistStr = prefs.getString(KEY_BLACKLIST_NUMBERS, "");

        // 检查黑名单
        if (!blacklistStr.isEmpty()) {
            String[] blacklist = blacklistStr.split(",");
            for (String num : blacklist) {
                if (num.trim().equals(sender) || sender.startsWith(num.trim())) {
                    return !whitelistMode; // 黑名单模式：不在白名单中就阻止
                }
            }
        }

        // 检查白名单
        if (whitelistMode && !whitelistStr.isEmpty()) {
            String[] whitelist = whitelistStr.split(",");
            for (String num : whitelist) {
                if (num.trim().equals(sender) || sender.startsWith(num.trim())) {
                    return true;
                }
            }
            return false; // 白名单模式且不在白名单中
        }

        return true; // 默认允许
    }

    // === 设置相关方法 ===

    public boolean isServiceEnabled() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SERVICE_ENABLED, true);
    }

    public void setServiceEnabled(boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply();
    }

    public boolean isWhitelistMode() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_WHITELIST_MODE, false);
    }

    public void setWhitelistMode(boolean whitelistMode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_WHITELIST_MODE, whitelistMode).apply();
    }

    public String getWhitelistNumbers() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_WHITELIST_NUMBERS, "");
    }

    public void setWhitelistNumbers(String numbers) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_WHITELIST_NUMBERS, numbers).apply();
    }

    public String getBlacklistNumbers() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_BLACKLIST_NUMBERS, "");
    }

    public void setBlacklistNumbers(String numbers) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_BLACKLIST_NUMBERS, numbers).apply();
    }

    // === 推送到钉钉 ===

    /**
     * 推送到钉钉群机器人
     */
    private void pushToDingTalk(String webhook, String text) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                URL url = new URL(webhook);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                String json = "{\"msgtype\":\"text\",\"text\":{\"content\":\"" + escapeJson(text) + "\"}}";
                byte[] data = json.getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(data.length);
                OutputStream os = conn.getOutputStream();
                os.write(data);
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                Log.d(TAG, "钉钉推送结果: HTTP " + code);
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "钉钉推送失败", e);
            }
        });
    }

    /**
     * 转义 JSON 字符串中的特殊字符
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
