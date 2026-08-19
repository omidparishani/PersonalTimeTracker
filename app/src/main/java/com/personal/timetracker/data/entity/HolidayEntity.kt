package com.personal.timetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A single non-working (holiday) date — either entered manually or fetched from an
 *  online source. Marking a date here removes its work requirement from attendance
 *  calculations without creating a leave/undertime penalty. */
@Entity(tableName = "holidays")
data class HolidayEntity(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val title: String = ""
)
