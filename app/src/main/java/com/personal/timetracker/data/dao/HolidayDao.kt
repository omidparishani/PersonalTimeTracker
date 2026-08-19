package com.personal.timetracker.data.dao

import androidx.room.*
import com.personal.timetracker.data.entity.HolidayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {

    @Query("SELECT * FROM holidays ORDER BY date ASC")
    fun observeAll(): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays ORDER BY date ASC")
    suspend fun getAllOnce(): List<HolidayEntity>

    @Query("SELECT * FROM holidays WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getInRange(start: String, end: String): List<HolidayEntity>

    @Query("SELECT COUNT(*) FROM holidays WHERE date = :date")
    suspend fun countForDate(date: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HolidayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HolidayEntity>)

    @Delete
    suspend fun delete(item: HolidayEntity)

    @Query("DELETE FROM holidays WHERE date = :date")
    suspend fun deleteByDate(date: String)
}
