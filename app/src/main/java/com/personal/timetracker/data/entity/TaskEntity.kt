package com.personal.timetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jiraNumber: String? = null,
    val projectName: String,
    val taskTitle: String,
    val description: String? = null,
    /** Estimated / required work in minutes */
    val requiredMinutes: Int = 0,
    /** Remaining minutes (required - logged) */
    val remainingMinutes: Int = 0,
    /** new | in_progress | done */
    val status: String = "new",
    val isRunning: Boolean = false,
    val runStartedAt: String? = null,
    val createdAt: String
)
