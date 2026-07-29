package com.personal.timetracker.data.dao

import androidx.room.*
import com.personal.timetracker.data.entity.TaskLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskLogDao {
    @Query("SELECT * FROM task_logs WHERE taskId = :taskId ORDER BY date DESC, id DESC")
    fun getByTask(taskId: Long): Flow<List<TaskLogEntity>>

    @Query("SELECT * FROM task_logs WHERE taskId = :taskId ORDER BY date DESC, id DESC")
    suspend fun getByTaskOnce(taskId: Long): List<TaskLogEntity>

    @Query("SELECT * FROM task_logs WHERE date = :date ORDER BY id DESC")
    fun getByDate(date: String): Flow<List<TaskLogEntity>>

    @Query("SELECT * FROM task_logs WHERE date = :date ORDER BY id DESC")
    suspend fun getByDateOnce(date: String): List<TaskLogEntity>

    @Query("SELECT * FROM task_logs WHERE date BETWEEN :start AND :end ORDER BY date")
    suspend fun getByRange(start: String, end: String): List<TaskLogEntity>

    @Insert
    suspend fun insert(item: TaskLogEntity): Long

    @Update
    suspend fun update(item: TaskLogEntity)

    @Delete
    suspend fun delete(item: TaskLogEntity)

    @Query("DELETE FROM task_logs")
    suspend fun deleteAll()

    @Query("SELECT COALESCE(SUM(duration),0) FROM task_logs WHERE date = :date")
    suspend fun sumByDate(date: String): Int

    @Query("SELECT COALESCE(SUM(duration),0) FROM task_logs WHERE taskId = :taskId")
    suspend fun sumByTask(taskId: Long): Int
}
