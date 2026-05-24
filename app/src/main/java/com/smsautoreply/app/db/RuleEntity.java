package com.smsautoreply.app.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 规则实体类
 * 存储短信自动回复/转发的规则
 */
@Entity(tableName = "rules")
public class RuleEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /** 规则名称 */
    @ColumnInfo(name = "name")
    private String name;

    /** 是否启用 */
    @ColumnInfo(name = "enabled")
    private boolean enabled;

    /** 优先级（排序用，值越小优先级越高） */
    @ColumnInfo(name = "priority")
    private int priority;

    /** 关键词匹配模式：contains(包含), equals(等于), starts_with(开头), regex(正则) */
    @ColumnInfo(name = "keyword_match_mode")
    private String keywordMatchMode;

    /** 关键词内容 */
    @ColumnInfo(name = "keyword")
    private String keyword;

    /** 号码匹配模式：exact(精确), starts_with(开头), contains(包含), any(任意号码) */
    @ColumnInfo(name = "sender_match_mode")
    private String senderMatchMode;

    /** 匹配的发送者号码模式 */
    @ColumnInfo(name = "sender_pattern")
    private String senderPattern;

    /** 动作：reply(回复), forward(转发), both(回复+转发) */
    @ColumnInfo(name = "action")
    private String action;

    /** 回复内容 */
    @ColumnInfo(name = "reply_content")
    private String replyContent;

    /** 转发目标号码 */
    @ColumnInfo(name = "forward_number")
    private String forwardNumber;

    // Getters and Setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getKeywordMatchMode() {
        return keywordMatchMode;
    }

    public void setKeywordMatchMode(String keywordMatchMode) {
        this.keywordMatchMode = keywordMatchMode;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getSenderMatchMode() {
        return senderMatchMode;
    }

    public void setSenderMatchMode(String senderMatchMode) {
        this.senderMatchMode = senderMatchMode;
    }

    public String getSenderPattern() {
        return senderPattern;
    }

    public void setSenderPattern(String senderPattern) {
        this.senderPattern = senderPattern;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReplyContent() {
        return replyContent;
    }

    public void setReplyContent(String replyContent) {
        this.replyContent = replyContent;
    }

    public String getForwardNumber() {
        return forwardNumber;
    }

    public void setForwardNumber(String forwardNumber) {
        this.forwardNumber = forwardNumber;
    }
}
