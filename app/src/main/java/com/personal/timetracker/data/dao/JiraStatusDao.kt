package com.personal.timetracker.data.dao

/**
 * توضیح فایل: DAO اتاق: JiraStatusDao.kt
 * بسته: com.personal.timetracker.data.dao
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

import androidx.room.*
import com.personal.timetracker.data.entity.JiraStatusEntity
import kotlinx.coroutines.flow.Flow

/**
 * کش وضعیت‌های جیرا.
 */
@Dao
interface JiraStatusDao {
    /**
     * مشاهده همه ردیف‌ها به‌صورت Flow.
     */
    @Query("SELECT * FROM jira_statuses ORDER BY categoryKey, name")
    fun observeAll(): Flow<List<JiraStatusEntity>>

    /**
     * خواندن یک‌باره همه ردیف‌ها.
     */
    @Query("SELECT * FROM jira_statuses ORDER BY categoryKey, name")
    suspend fun getAllOnce(): List<JiraStatusEntity>

    /**
     * upsert دسته‌ای.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<JiraStatusEntity>)

    @Query("DELETE FROM jira_statuses")
    suspend fun clear()
}
