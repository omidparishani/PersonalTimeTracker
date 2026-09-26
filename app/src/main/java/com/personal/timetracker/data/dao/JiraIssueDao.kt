package com.personal.timetracker.data.dao

/**
 * توضیح فایل: DAO اتاق: JiraIssueDao.kt
 * بسته: com.personal.timetracker.data.dao
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

import androidx.room.*
import com.personal.timetracker.data.entity.JiraIssueCacheEntity
import kotlinx.coroutines.flow.Flow

/**
 * کش Issueهای جیرا.
 */
@Dao
interface JiraIssueDao {
    /**
     * مشاهده همه ردیف‌ها به‌صورت Flow.
     */
    @Query("SELECT * FROM jira_issues ORDER BY jiraUpdated DESC, cachedAt DESC")
    fun observeAll(): Flow<List<JiraIssueCacheEntity>>

    @Query("SELECT * FROM jira_issues WHERE isAssignedToMe = 1 ORDER BY jiraUpdated DESC")
    fun observeAssigned(): Flow<List<JiraIssueCacheEntity>>

    @Query("SELECT * FROM jira_issues WHERE isFavorite = 1 ORDER BY cachedAt DESC")
    fun observeFavorites(): Flow<List<JiraIssueCacheEntity>>

    @Query("SELECT * FROM jira_issues WHERE statusCategory != 'done' ORDER BY jiraUpdated DESC")
    fun observeOpen(): Flow<List<JiraIssueCacheEntity>>

    @Query("""
        SELECT * FROM jira_issues
        WHERE issueKey LIKE '%' || :q || '%'
           OR summary LIKE '%' || :q || '%'
           OR projectKey LIKE '%' || :q || '%'
           OR statusName LIKE '%' || :q || '%'
        ORDER BY jiraUpdated DESC
    """)
    /**
     * جستجو با متن.
     */
    fun search(q: String): Flow<List<JiraIssueCacheEntity>>

    /**
     * یافتن با کلید یکتا.
     */
    @Query("SELECT * FROM jira_issues WHERE issueKey = :key LIMIT 1")
    suspend fun getByKey(key: String): JiraIssueCacheEntity?

    /**
     * خواندن یک‌باره همه ردیف‌ها.
     */
    @Query("SELECT * FROM jira_issues ORDER BY jiraUpdated DESC")
    suspend fun getAllOnce(): List<JiraIssueCacheEntity>

    /**
     * درج یا جایگزینی در صورت تعارض.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: JiraIssueCacheEntity)

    /**
     * upsert دسته‌ای.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<JiraIssueCacheEntity>)

    /**
     * تنظیم پرچم علاقه‌مندی در کش Issue.
     */
    @Query("UPDATE jira_issues SET isFavorite = :fav WHERE issueKey = :key")
    suspend fun setFavorite(key: String, fav: Boolean)

    /**
     * حذف ردیف.
     */
    @Delete
    suspend fun delete(item: JiraIssueCacheEntity)

    /**
     * حذف با کلید.
     */
    @Query("DELETE FROM jira_issues WHERE issueKey = :key")
    suspend fun deleteByKey(key: String)
}
