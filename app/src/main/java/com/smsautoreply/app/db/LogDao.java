package com.smsautoreply.app.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * 日志数据访问对象
 */
@Dao
public interface LogDao {

    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    LiveData<List<LogEntity>> getAllLogs();

    @Query("SELECT * FROM logs WHERE action_type IN ('replied','forwarded','both','error') ORDER BY timestamp DESC")
    LiveData<List<LogEntity>> getActionLogs();

    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<LogEntity>> getRecentLogs(int limit);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(LogEntity log);

    @Delete
    void delete(LogEntity log);

    @Query("DELETE FROM logs")
    void deleteAll();

    @Query("DELETE FROM logs WHERE timestamp < :timestamp")
    void deleteOlderThan(long timestamp);
}
