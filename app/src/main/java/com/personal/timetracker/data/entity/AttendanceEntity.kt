package com.personal.timetracker.data.entity

/**
 * توضیح فایل: Entity دیتابیس: AttendanceEntity.kt
 * بسته: com.personal.timetracker.data.entity
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance")
/**
 * تردد روزانه یک بازه ورود/خروج.
 */
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,          // yyyy-MM-dd Gregorian for sorting
    val entryTime: String,     // HH:mm
    val exitTime: String? = null,
    val duration: Int = 0,     // minutes
    val leaveDuration: Int = 0,
    val overtimeDuration: Int = 0,
    val status: String = "active" // active | completed
)
