package com.personal.timetracker.data.entity

/**
 * توضیح فایل: Entity دیتابیس: JiraFavoriteEntity.kt
 * بسته: com.personal.timetracker.data.entity
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Issueهایی که به کاربر assign نیستند ولی می‌خواهد سریع به آن‌ها لاگ بزند
 * (مثلاً Daily، جلسات، پشتیبانی و ...).
 */
@Entity(tableName = "jira_favorites")
/**
 * کلاس JiraFavoriteEntity
 */
data class JiraFavoriteEntity(
    @PrimaryKey val issueKey: String,
    val summary: String = "",
    val projectKey: String = "",
    val projectName: String = "",
    val note: String? = null,          // یادداشت شخصی کاربر
    val addedAt: String                // yyyy-MM-dd HH:mm
)
