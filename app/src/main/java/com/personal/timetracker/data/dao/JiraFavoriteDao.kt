package com.personal.timetracker.data.dao

/**
 * توضیح فایل: DAO اتاق: JiraFavoriteDao.kt
 * بسته: com.personal.timetracker.data.dao
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

import androidx.room.*
import com.personal.timetracker.data.entity.JiraFavoriteEntity
import kotlinx.coroutines.flow.Flow

/**
 * علاقه‌مندی‌های جیرا.
 */
@Dao
interface JiraFavoriteDao {
    /**
     * مشاهده همه ردیف‌ها به‌صورت Flow.
     */
    @Query("SELECT * FROM jira_favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<JiraFavoriteEntity>>

    /**
     * خواندن یک‌باره همه ردیف‌ها.
     */
    @Query("SELECT * FROM jira_favorites ORDER BY addedAt DESC")
    suspend fun getAllOnce(): List<JiraFavoriteEntity>

    /**
     * یافتن با کلید یکتا.
     */
    @Query("SELECT * FROM jira_favorites WHERE issueKey = :key LIMIT 1")
    suspend fun getByKey(key: String): JiraFavoriteEntity?

    /**
     * آیا کلید وجود دارد (معمولاً 0/1).
     */
    @Query("SELECT COUNT(*) FROM jira_favorites WHERE issueKey = :key")
    suspend fun exists(key: String): Int

    /**
     * درج یا جایگزینی در صورت تعارض.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: JiraFavoriteEntity)

    /**
     * حذف ردیف.
     */
    @Delete
    suspend fun delete(item: JiraFavoriteEntity)

    /**
     * حذف با کلید.
     */
    @Query("DELETE FROM jira_favorites WHERE issueKey = :key")
    suspend fun deleteByKey(key: String)
}
