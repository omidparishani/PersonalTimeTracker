package com.personal.timetracker.data.entity

/**
 * توضیح فایل: Entity دیتابیس: JiraStatusEntity.kt
 * بسته: com.personal.timetracker.data.entity
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

import androidx.room.Entity
import androidx.room.PrimaryKey

/** وضعیت‌های معتبر workflow که از API شرکت واکشی شده */
@Entity(tableName = "jira_statuses")
/**
 * کش یک وضعیت workflow جیرا.
 */
data class JiraStatusEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryKey: String = "",  // new | indeterminate | done
    val categoryName: String = "",
    val cachedAt: String = ""
)
