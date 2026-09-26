package com.personal.timetracker.data.dao

/**
 * توضیح فایل: DAO اتاق: SettingsDao.kt
 * بسته: com.personal.timetracker.data.dao
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

import androidx.room.*
import com.personal.timetracker.data.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * خواندن/نوشتن تنظیمات.
 */
@Dao
interface SettingsDao {
    /**
     * جریان زنده داده از دیتابیس (Flow).
     */
    @Query("SELECT * FROM settings WHERE id = 1")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun get(): SettingsEntity?

    /**
     * درج یا جایگزینی در صورت تعارض.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SettingsEntity)

    /**
     * حذف همه.
     */
    @Query("DELETE FROM settings")
    suspend fun deleteAll()
}
