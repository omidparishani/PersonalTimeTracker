package com.personal.timetracker.data.dao

import androidx.room.*
import com.personal.timetracker.data.entity.JiraWorklogCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JiraWorklogDao {
    @Query("SELECT * FROM jira_worklogs WHERE issueKey = :key AND syncStatus != 'pending_delete' ORDER BY date DESC, started DESC")
    fun observeByIssue(key: String): Flow<List<JiraWorklogCacheEntity>>

    @Query("SELECT * FROM jira_worklogs WHERE issueKey = :key AND syncStatus != 'pending_delete' ORDER BY date DESC, started DESC")
    suspend fun getByIssueOnce(key: String): List<JiraWorklogCacheEntity>

    @Query("SELECT * FROM jira_worklogs WHERE date = :date AND syncStatus != 'pending_delete' ORDER BY started DESC, localId DESC")
    suspend fun getByDateOnce(date: String): List<JiraWorklogCacheEntity>

    @Query("SELECT * FROM jira_worklogs WHERE date BETWEEN :start AND :end AND syncStatus != 'pending_delete' ORDER BY date ASC, started ASC")
    suspend fun getByRange(start: String, end: String): List<JiraWorklogCacheEntity>

    @Query("SELECT * FROM jira_worklogs WHERE syncStatus != 'synced' ORDER BY localId ASC")
    suspend fun getPending(): List<JiraWorklogCacheEntity>

    @Query("SELECT * FROM jira_worklogs WHERE localId = :id LIMIT 1")
    suspend fun getByLocalId(id: Long): JiraWorklogCacheEntity?

    @Query("SELECT * FROM jira_worklogs WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): JiraWorklogCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: JiraWorklogCacheEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<JiraWorklogCacheEntity>)

    @Update
    suspend fun update(item: JiraWorklogCacheEntity)

    @Delete
    suspend fun delete(item: JiraWorklogCacheEntity)

    @Query("DELETE FROM jira_worklogs WHERE issueKey = :key AND syncStatus = 'synced'")
    suspend fun deleteSyncedForIssue(key: String)

    @Query("DELETE FROM jira_worklogs WHERE remoteId = :remoteId")
    suspend fun deleteByRemoteId(remoteId: String)

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM jira_worklogs WHERE date BETWEEN :start AND :end AND syncStatus != 'pending_delete'")
    suspend fun sumMinutesInRange(start: String, end: String): Int

    /** حذف رکوردهای synced تکراری که remoteId یکسان دارند (نگه‌داشتن کمترین localId) */
    @Query("""
        DELETE FROM jira_worklogs WHERE localId IN (
            SELECT a.localId FROM jira_worklogs a
            INNER JOIN jira_worklogs b
              ON a.remoteId IS NOT NULL AND a.remoteId = b.remoteId
             AND a.localId > b.localId
        )
    """)
    suspend fun dedupeByRemoteId(): Int
}
