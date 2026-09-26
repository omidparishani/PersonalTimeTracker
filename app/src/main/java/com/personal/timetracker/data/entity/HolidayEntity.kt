package com.personal.timetracker.data.entity

/**
 * توضیح فایل: Entity دیتابیس: HolidayEntity.kt
 * بسته: com.personal.timetracker.data.entity
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A single non-working (holiday) date — either entered manually or fetched from an
 *  online source. Marking a date here removes its work requirement from attendance
 *  calculations without creating a leave/undertime penalty. */
@Entity(tableName = "holidays")
/**
 * یک روز تعطیل رسمی/شرکتی.
 */
data class HolidayEntity(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val title: String = ""
)
