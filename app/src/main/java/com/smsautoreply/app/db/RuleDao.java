package com.smsautoreply.app.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * 规则数据访问对象
 */
@Dao
public interface RuleDao {

    @Query("SELECT * FROM rules ORDER BY priority ASC, id DESC")
    LiveData<List<RuleEntity>> getAllRules();

    @Query("SELECT * FROM rules WHERE enabled = 1 ORDER BY priority ASC, id DESC")
    LiveData<List<RuleEntity>> getEnabledRules();

    @Query("SELECT * FROM rules WHERE enabled = 1 ORDER BY priority ASC, id DESC")
    List<RuleEntity> getEnabledRulesSync();

    @Query("SELECT * FROM rules WHERE id = :id")
    RuleEntity getRuleById(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(RuleEntity rule);

    @Update
    void update(RuleEntity rule);

    @Delete
    void delete(RuleEntity rule);

    @Query("DELETE FROM rules WHERE id = :id")
    void deleteById(long id);

    @Query("UPDATE rules SET priority = :priority WHERE id = :id")
    void updatePriority(long id, int priority);

    @Query("UPDATE rules SET enabled = :enabled WHERE id = :id")
    void updateEnabled(long id, boolean enabled);

    @Query("SELECT MAX(priority) FROM rules")
    LiveData<Integer> getMaxPriority();
}
