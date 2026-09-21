package com.personal.timetracker.data.dao

import androidx.room.*
import com.personal.timetracker.data.entity.JiraIssueCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JiraIssueDao {
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
    fun search(q: String): Flow<List<JiraIssueCacheEntity>>

    @Query("SELECT * FROM jira_issues WHERE issueKey = :key LIMIT 1")
    suspend fun getByKey(key: String): JiraIssueCacheEntity?

    @Query("SELECT * FROM jira_issues ORDER BY jiraUpdated DESC")
    suspend fun getAllOnce(): List<JiraIssueCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: JiraIssueCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<JiraIssueCacheEntity>)

    @Query("UPDATE jira_issues SET isFavorite = :fav WHERE issueKey = :key")
    suspend fun setFavorite(key: String, fav: Boolean)

    @Delete
    suspend fun delete(item: JiraIssueCacheEntity)

    @Query("DELETE FROM jira_issues WHERE issueKey = :key")
    suspend fun deleteByKey(key: String)
}
