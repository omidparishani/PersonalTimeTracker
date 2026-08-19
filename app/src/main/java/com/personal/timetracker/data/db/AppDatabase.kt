package com.personal.timetracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.personal.timetracker.data.dao.AttendanceDao
import com.personal.timetracker.data.dao.HolidayDao
import com.personal.timetracker.data.dao.SettingsDao
import com.personal.timetracker.data.dao.TaskDao
import com.personal.timetracker.data.dao.TaskLogDao
import com.personal.timetracker.data.entity.AttendanceEntity
import com.personal.timetracker.data.entity.HolidayEntity
import com.personal.timetracker.data.entity.SettingsEntity
import com.personal.timetracker.data.entity.TaskEntity
import com.personal.timetracker.data.entity.TaskLogEntity

@Database(
    entities = [AttendanceEntity::class, TaskEntity::class, TaskLogEntity::class, SettingsEntity::class, HolidayEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
    abstract fun taskDao(): TaskDao
    abstract fun taskLogDao(): TaskLogDao
    abstract fun settingsDao(): SettingsDao
    abstract fun holidayDao(): HolidayDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf(
                    "ALTER TABLE settings ADD COLUMN notifEnabled INTEGER NOT NULL DEFAULT 1",
                    "ALTER TABLE settings ADD COLUMN notifMinutesBefore INTEGER NOT NULL DEFAULT 30",
                    "ALTER TABLE settings ADD COLUMN notifTitle TEXT NOT NULL DEFAULT ''",
                    "ALTER TABLE settings ADD COLUMN notifBody TEXT NOT NULL DEFAULT ''",
                    "ALTER TABLE settings ADD COLUMN biometricEnabled INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE settings ADD COLUMN workLat REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE settings ADD COLUMN workLng REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE settings ADD COLUMN workRadiusMeters REAL NOT NULL DEFAULT 150",
                    "ALTER TABLE settings ADD COLUMN geoAutoCheckIn INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE settings ADD COLUMN geoAlertOnly INTEGER NOT NULL DEFAULT 1"
                ).forEach { try { db.execSQL(it) } catch (_: Exception) {} }
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN requiredMinutes INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN remainingMinutes INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN status TEXT NOT NULL DEFAULT 'new'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN runStartedAt TEXT")
                } catch (_: Exception) {}
                // migrate old duration column into remaining if exists - skip safely
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS task_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        startTime TEXT,
                        endTime TEXT,
                        duration INTEGER NOT NULL DEFAULT 0,
                        note TEXT,
                        createdAt TEXT NOT NULL
                    )"""
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE settings ADD COLUMN geoAutoCheckOut INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE attendance ADD COLUMN overtimeDuration INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE settings ADD COLUMN autoBackupEnabled INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE settings ADD COLUMN autoBackupIntervalHours INTEGER NOT NULL DEFAULT 24")
                } catch (_: Exception) {}
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE settings ADD COLUMN weeklyRequiredMinutes INTEGER NOT NULL DEFAULT 2775")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE settings ADD COLUMN thursdayWorking INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE settings ADD COLUMN thursdayMinutes INTEGER NOT NULL DEFAULT 300")
                } catch (_: Exception) {}
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS holidays (
                        date TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL DEFAULT ''
                    )"""
                )
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "personal_time_tracker.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "INSERT OR IGNORE INTO settings (id, startWorkTime, endWorkTime, flexibleMinutes, minimumWorkMinutes, isDarkMode, themeColor, projects) VALUES (1,'09:00','17:00',30,480,0,-10983104,'')"
                        )
                    }
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "INSERT OR IGNORE INTO settings (id, startWorkTime, endWorkTime, flexibleMinutes, minimumWorkMinutes, isDarkMode, themeColor, projects) VALUES (1,'09:00','17:00',30,480,0,-10983104,'')"
                        )
                    }
                })
                .build()
        }
    }
}
