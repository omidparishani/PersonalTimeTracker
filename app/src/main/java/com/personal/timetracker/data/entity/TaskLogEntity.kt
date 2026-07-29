package com.personal.timetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One work log entry for a task on a specific day */
@Entity(tableName = "task_logs")
data class TaskLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val date: String,          // yyyy-MM-dd
    val startTime: String?,    // HH:mm or datetime
    val endTime: String?,
    val duration: Int = 0,     // minutes
    val note: String? = null,
    val createdAt: String
)
