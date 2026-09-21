package com.personal.timetracker.data.dao

import androidx.room.*
import com.personal.timetracker.data.entity.JiraStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JiraStatusDao {
    @Query("SELECT * FROM jira_statuses ORDER BY categoryKey, name")
    fun observeAll(): Flow<List<JiraStatusEntity>>

    @Query("SELECT * FROM jira_statuses ORDER BY categoryKey, name")
    suspend fun getAllOnce(): List<JiraStatusEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<JiraStatusEntity>)

    @Query("DELETE FROM jira_statuses")
    suspend fun clear()
}
