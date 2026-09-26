package com.personal.timetracker.data.entity

/**
 * توضیح فایل: Entity دیتابیس: SettingsEntity.kt
 * بسته: com.personal.timetracker.data.entity
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
/**
 * تنظیمات سراسری اپ (معمولاً یک ردیف).
 * شامل: ساعت کاری، شناوری، تم، بیومتریک، مختصات محل کار،
 * قوانین پنج‌شنبه، بکاپ خودکار، و پیکربندی جیرا (URL/توکن/فیلترها).
 */
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val startWorkTime: String = "09:00",
    val endWorkTime: String = "17:00",
    val flexibleMinutes: Int = 30,
    val minimumWorkMinutes: Int = 480,
    val isDarkMode: Boolean = false,
    val themeColor: Int = -10983104, // 0xFF1565C0
    val projects: String = "",
    // notifications
    val notifEnabled: Boolean = true,
    val notifMinutesBefore: Int = 30,
    val notifTitle: String = "یادآوری پایان کار",
    val notifBody: String = "۳۰ دقیقه تا پایان ساعت کاری باقی مانده",
    // biometric
    val biometricEnabled: Boolean = false,
    // workplace location
    val workLat: Double = 0.0,
    val workLng: Double = 0.0,
    val workRadiusMeters: Float = 150f,
    val geoAutoCheckIn: Boolean = false,
    val geoAlertOnly: Boolean = true,
    val geoAutoCheckOut: Boolean = false,
    // weekly work-schedule (شیفت کاری)
    val weeklyRequiredMinutes: Int = 2775, // 46:15 default (Sat-Wed, Thu off)
    val thursdayWorking: Boolean = false,
    val thursdayMinutes: Int = 300, // used only when thursdayWorking = true
    // auto backup
    val autoBackupEnabled: Boolean = false,
    val autoBackupIntervalHours: Int = 24, // 0 = disabled
    // Jira integration
    val jiraEnabled: Boolean = false,
    val jiraBaseUrl: String = "https://jira.demisco.com",
    val jiraToken: String = "",
    /** نام وضعیت‌های انتخاب‌شده برای فیلتر (با کاما). خالی = همه */
    val jiraFilterStatuses: String = "",
    /** کلید/نام پروژه‌های انتخاب‌شده برای فیلتر (با کاما). خالی = همه */
    val jiraFilterProjects: String = "",
    /** کاتالوگ پروژه‌های سازمان (کلیدها با کاما) — از تنظیمات بروزرسانی می‌شود */
    val jiraProjectCatalog: String = "",
    /** مسیر پوشه پشتیبان خودکار؛ خالی = پیش‌فرض PTT_Backups در حافظه اپ */
    val autoBackupDir: String = ""
)
