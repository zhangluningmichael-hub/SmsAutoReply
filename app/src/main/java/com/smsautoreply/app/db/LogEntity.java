package com.smsautoreply.app.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 日志实体类
 * 记录收到的短信和自动操作
 */
@Entity(tableName = "logs")
public class LogEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /** 时间戳（毫秒） */
    @ColumnInfo(name = "timestamp")
    private long timestamp;

    /** 短信发送者号码 */
    @ColumnInfo(name = "sender")
    private String sender;

    /** 短信内容 */
    @ColumnInfo(name = "message")
    private String message;

    /** 操作类型：received(收到), replied(已回复), forwarded(已转发), no_match(无匹配规则) */
    @ColumnInfo(name = "action_type")
    private String actionType;

    /** 操作详情说明 */
    @ColumnInfo(name = "detail")
    private String detail;

    // Getters and Setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
