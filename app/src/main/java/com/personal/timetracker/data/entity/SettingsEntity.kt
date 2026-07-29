package com.personal.timetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
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
    val geoAlertOnly: Boolean = true
)
