package com.personal.timetracker.data.dao

/**
 * توضیح فایل: DAO اتاق: AttendanceDao.kt
 * بسته: com.personal.timetracker.data.dao
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

import androidx.room.*
import com.personal.timetracker.data.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

/**
 * دسترسی Room به جدول تردد.
 */
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
    /**
     * مشاهده رکوردهای یک تاریخ خاص.
     */
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
    /**
     * خواندن یک‌باره رکوردهای یک روز.
     */
    suspend fun getByDateOnce(date: String): List<AttendanceEntity>

    /**
     * خواندن رکوردها در بازه تاریخ.
     */
    @Query("SELECT * FROM attendance WHERE date BETWEEN :start AND :end ORDER BY date, entryTime")
    suspend fun getByRange(start: String, end: String): List<AttendanceEntity>

    /**
     * رکورد فعال فعلی.
     */
    @Query("SELECT * FROM attendance WHERE exitTime IS NULL AND status = 'active' ORDER BY id DESC LIMIT 1")
    suspend fun getActive(): AttendanceEntity?

    /**
     * مشاهده رکورد فعال (مثلاً ورود بدون خروج).
     */
    @Query("SELECT * FROM attendance WHERE exitTime IS NULL AND status = 'active' ORDER BY id DESC LIMIT 1")
    fun observeActive(): Flow<AttendanceEntity?>

    /**
     * درج ردیف جدید.
     */
    @Insert
    suspend fun insert(item: AttendanceEntity): Long

    /**
     * به‌روزرسانی ردیف.
     */
    @Update
    suspend fun update(item: AttendanceEntity)

    /**
     * حذف ردیف.
     */
    @Delete
    suspend fun delete(item: AttendanceEntity)

    /**
     * حذف همه.
     */
    @Query("DELETE FROM attendance")
    suspend fun deleteAll()

    /**
     * تعداد ردیف‌ها.
     */
    @Query("SELECT COUNT(*) FROM attendance")
    suspend fun count(): Int

    @Query("SELECT COALESCE(SUM(duration),0) FROM attendance WHERE date BETWEEN :start AND :end")
    suspend fun sumWorked(start: String, end: String): Int

    @Query("SELECT COALESCE(SUM(leaveDuration),0) FROM attendance WHERE date BETWEEN :start AND :end")
    suspend fun sumLeave(start: String, end: String): Int

    @Query("SELECT COUNT(*) FROM attendance WHERE date BETWEEN :start AND :end")
    suspend fun countInRange(start: String, end: String): Int
}