package com.personal.timetracker.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * کش Worklogهای جیرا (= لاگ تسک).
 * remoteId یکتاست وقتی از سرور آمده؛ رکوردهای pending ممکن است remoteId=null داشته باشند.
 */
@Entity(
    tableName = "jira_worklogs",
    indices = [
        Index("issueKey"),
        Index("date"),
        Index("syncStatus"),
        Index(value = ["remoteId"], unique = true)
    ]
)
data class JiraWorklogCacheEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    /** شناسه Worklog روی سرور؛ null تا وقتی سینک نشده */
    val remoteId: String? = null,
    val issueKey: String,
    val date: String,              // yyyy-MM-dd
    val started: String? = null,   // ISO از جیرا
    val durationMinutes: Int = 0,
    val comment: String? = null,
    val authorName: String? = null,
    /** synced | pending_add | pending_update | pending_delete */
    val syncStatus: String = "synced",
    val cachedAt: String = ""
)
