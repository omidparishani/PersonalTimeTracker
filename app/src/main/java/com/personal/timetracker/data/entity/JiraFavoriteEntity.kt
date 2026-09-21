package com.personal.timetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Issueهایی که به کاربر assign نیستند ولی می‌خواهد سریع به آن‌ها لاگ بزند
 * (مثلاً Daily، جلسات، پشتیبانی و ...).
 */
@Entity(tableName = "jira_favorites")
data class JiraFavoriteEntity(
    @PrimaryKey val issueKey: String,
    val summary: String = "",
    val projectKey: String = "",
    val projectName: String = "",
    val note: String? = null,          // یادداشت شخصی کاربر
    val addedAt: String                // yyyy-MM-dd HH:mm
)
