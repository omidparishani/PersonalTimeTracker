package com.personal.timetracker.data.dao

/**
 * توضیح فایل: DAO اتاق: TaskDao.kt
 * بسته: com.personal.timetracker.data.dao
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

import androidx.room.*
import com.personal.timetracker.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * CRUD و مشاهده تسک‌های محلی.
 */
@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TaskEntity>>

    /**
     * خواندن یک‌باره همه ردیف‌ها.
     */
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<TaskEntity>

    /**
     * رکورد فعال فعلی.
     */
    @Query("SELECT * FROM tasks WHERE status != 'done' ORDER BY createdAt DESC")
    fun getActive(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY createdAt DESC")
    fun getByStatus(status: String): Flow<List<TaskEntity>>

    /**
     * یافتن با شناسه عددی.
     */
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE isRunning = 1 LIMIT 1")
    suspend fun getRunning(): TaskEntity?

    @Query("SELECT * FROM tasks WHERE isRunning = 1 LIMIT 1")
    fun observeRunning(): Flow<TaskEntity?>

    /**
     * جستجو با متن.
     */
    @Query("SELECT * FROM tasks WHERE jiraNumber LIKE '%' || :q || '%' OR projectName LIKE '%' || :q || '%' OR taskTitle LIKE '%' || :q || '%' OR description LIKE '%' || :q || '%' ORDER BY createdAt DESC")
    fun search(q: String): Flow<List<TaskEntity>>

    /**
     * درج ردیف جدید.
     */
    @Insert
    suspend fun insert(item: TaskEntity): Long

    /**
     * به‌روزرسانی ردیف.
     */
    @Update
    suspend fun update(item: TaskEntity)

    /**
     * حذف ردیف.
     */
    @Delete
    suspend fun delete(item: TaskEntity)

    /**
     * حذف همه.
     */
    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    /**
     * تعداد ردیف‌ها.
     */
    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun count(): Int

    @Query("SELECT projectName, SUM(requiredMinutes - remainingMinutes) as total FROM tasks GROUP BY projectName ORDER BY total DESC")
    suspend fun projectSummary(): List<ProjectSum>

    @Query("SELECT jiraNumber, SUM(requiredMinutes - remainingMinutes) as total FROM tasks WHERE jiraNumber IS NOT NULL AND jiraNumber != '' GROUP BY jiraNumber ORDER BY total DESC")
    suspend fun jiraSummary(): List<JiraSum>
}

data class ProjectSum(val projectName: String, val total: Int)
data class JiraSum(val jiraNumber: String, val total: Int, val taskTitle: String? = null)
