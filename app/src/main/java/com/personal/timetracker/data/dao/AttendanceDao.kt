package com.personal.timetracker.data.dao

import androidx.room.*
import com.personal.timetracker.data.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    // مشاهده ترددهای یک روز (Realtime)
    @Query(
        """
        SELECT *
        FROM attendance
        WHERE date = :date
        ORDER BY entryTime ASC
    """
    )
    fun observeByDate(date: String): Flow<List<AttendanceEntity>>

    // دریافت ترددهای یک روز
    @Query(
        """
        SELECT *
        FROM attendance
        WHERE date = :date
        ORDER BY entryTime ASC
    """
    )
    suspend fun getByDateOnce(date: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE date BETWEEN :start AND :end ORDER BY date, entryTime")
    suspend fun getByRange(start: String, end: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE exitTime IS NULL AND status = 'active' ORDER BY id DESC LIMIT 1")
    suspend fun getActive(): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE exitTime IS NULL AND status = 'active' ORDER BY id DESC LIMIT 1")
    fun observeActive(): Flow<AttendanceEntity?>

    @Insert
    suspend fun insert(item: AttendanceEntity): Long

    @Update
    suspend fun update(item: AttendanceEntity)

    @Delete
    suspend fun delete(item: AttendanceEntity)

    @Query("DELETE FROM attendance")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM attendance")
    suspend fun count(): Int

    @Query("SELECT COALESCE(SUM(duration),0) FROM attendance WHERE date BETWEEN :start AND :end")
    suspend fun sumWorked(start: String, end: String): Int

    @Query("SELECT COALESCE(SUM(leaveDuration),0) FROM attendance WHERE date BETWEEN :start AND :end")
    suspend fun sumLeave(start: String, end: String): Int

    @Query("SELECT COUNT(*) FROM attendance WHERE date BETWEEN :start AND :end")
    suspend fun countInRange(start: String, end: String): Int
}