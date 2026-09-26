package com.personal.timetracker.data.dao

/**
 * توضیح فایل: DAO اتاق: TaskLogDao.kt
 * بسته: com.personal.timetracker.data.dao
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

import androidx.room.*
import com.personal.timetracker.data.entity.TaskLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * لاگ‌های زمانی محلی.
 */
@Dao
interface TaskLogDao {
    @Query("SELECT * FROM task_logs WHERE taskId = :taskId ORDER BY date DESC, id DESC")
    fun getByTask(taskId: Long): Flow<List<TaskLogEntity>>

    @Query("SELECT * FROM task_logs WHERE taskId = :taskId ORDER BY date DESC, id DESC")
    suspend fun getByTaskOnce(taskId: Long): List<TaskLogEntity>

    @Query("SELECT * FROM task_logs WHERE date = :date ORDER BY id DESC")
    fun getByDate(date: String): Flow<List<TaskLogEntity>>

    /**
     * خواندن یک‌باره رکوردهای یک روز.
     */
    @Query("SELECT * FROM task_logs WHERE date = :date ORDER BY id DESC")
    suspend fun getByDateOnce(date: String): List<TaskLogEntity>

    /**
     * خواندن رکوردها در بازه تاریخ.
     */
    @Query("SELECT * FROM task_logs WHERE date BETWEEN :start AND :end ORDER BY date")
    suspend fun getByRange(start: String, end: String): List<TaskLogEntity>

    /**
     * خواندن یک‌باره همه ردیف‌ها.
     */
    @Query("SELECT * FROM task_logs ORDER BY date DESC, id DESC")
    suspend fun getAllOnce(): List<TaskLogEntity>

    /**
     * درج ردیف جدید.
     */
    @Insert
    suspend fun insert(item: TaskLogEntity): Long

    /**
     * به‌روزرسانی ردیف.
     */
    @Update
    suspend fun update(item: TaskLogEntity)

    /**
     * حذف ردیف.
     */
    @Delete
    suspend fun delete(item: TaskLogEntity)

    /**
     * حذف همه.
     */
    @Query("DELETE FROM task_logs")
    suspend fun deleteAll()

    @Query("SELECT COALESCE(SUM(duration),0) FROM task_logs WHERE date = :date")
    suspend fun sumByDate(date: String): Int

    @Query("SELECT COALESCE(SUM(duration),0) FROM task_logs WHERE taskId = :taskId")
    suspend fun sumByTask(taskId: Long): Int
}
