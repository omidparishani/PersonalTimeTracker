package com.personal.timetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** کش محلی Issueهای جیرا — منبع اصلی «تسک» در اپ */
@Entity(tableName = "jira_issues")
data class JiraIssueCacheEntity(
    @PrimaryKey val issueKey: String,
    val summary: String = "",
    val description: String? = null,
    val projectKey: String = "",
    val projectName: String = "",
    val statusId: String = "",
    val statusName: String = "",
    val statusCategory: String = "", // new | indeterminate | done
    val priorityName: String = "",
    val issueTypeName: String = "",
    val assigneeName: String? = null,
    val reporterName: String? = null,
    val labels: String = "", // comma-separated
    val requiredMinutes: Int = 0,
    val remainingMinutes: Int = 0,
    val timeSpentMinutes: Int = 0,
    val isFavorite: Boolean = false,
    val isAssignedToMe: Boolean = false,
    val jiraUpdated: String? = null,
    val cachedAt: String = ""
)
