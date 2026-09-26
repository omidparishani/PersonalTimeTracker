package com.personal.timetracker.data.dao

/**
 * توضیح فایل: DAO اتاق: HolidayDao.kt
 * بسته: com.personal.timetracker.data.dao
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

import androidx.room.*
import com.personal.timetracker.data.entity.HolidayEntity
import kotlinx.coroutines.flow.Flow

/**
 * دسترسی Room به تعطیلات.
 */
@Dao
interface HolidayDao {

    /**
     * مشاهده همه ردیف‌ها به‌صورت Flow.
     */
    @Query("SELECT * FROM holidays ORDER BY date ASC")
    fun observeAll(): Flow<List<HolidayEntity>>

    /**
     * خواندن یک‌باره همه ردیف‌ها.
     */
    @Query("SELECT * FROM holidays ORDER BY date ASC")
    suspend fun getAllOnce(): List<HolidayEntity>

    @Query("SELECT * FROM holidays WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getInRange(start: String, end: String): List<HolidayEntity>

    @Query("SELECT COUNT(*) FROM holidays WHERE date = :date")
    suspend fun countForDate(date: String): Int

    /**
     * درج ردیف جدید.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HolidayEntity)

    /**
     * درج دسته‌ای.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HolidayEntity>)

    /**
     * حذف ردیف.
     */
    @Delete
    suspend fun delete(item: HolidayEntity)

    /**
     * حذف با تاریخ.
     */
    @Query("DELETE FROM holidays WHERE date = :date")
    suspend fun deleteByDate(date: String)
}
