package com.personal.timetracker.data.dao

import androidx.room.*
import com.personal.timetracker.data.entity.JiraFavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JiraFavoriteDao {
    @Query("SELECT * FROM jira_favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<JiraFavoriteEntity>>

    @Query("SELECT * FROM jira_favorites ORDER BY addedAt DESC")
    suspend fun getAllOnce(): List<JiraFavoriteEntity>

    @Query("SELECT * FROM jira_favorites WHERE issueKey = :key LIMIT 1")
    suspend fun getByKey(key: String): JiraFavoriteEntity?

    @Query("SELECT COUNT(*) FROM jira_favorites WHERE issueKey = :key")
    suspend fun exists(key: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: JiraFavoriteEntity)

    @Delete
    suspend fun delete(item: JiraFavoriteEntity)

    @Query("DELETE FROM jira_favorites WHERE issueKey = :key")
    suspend fun deleteByKey(key: String)
}
