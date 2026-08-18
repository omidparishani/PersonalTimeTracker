package com.personal.timetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance")
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
