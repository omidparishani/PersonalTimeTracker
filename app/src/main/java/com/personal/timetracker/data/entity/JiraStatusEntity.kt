package com.personal.timetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** وضعیت‌های معتبر workflow که از API شرکت واکشی شده */
@Entity(tableName = "jira_statuses")
data class JiraStatusEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryKey: String = "",  // new | indeterminate | done
    val categoryName: String = "",
    val cachedAt: String = ""
)
