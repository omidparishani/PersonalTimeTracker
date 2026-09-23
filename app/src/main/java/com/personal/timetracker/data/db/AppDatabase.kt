package com.personal.timetracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.personal.timetracker.data.dao.AttendanceDao
import com.personal.timetracker.data.dao.HolidayDao
import com.personal.timetracker.data.dao.JiraFavoriteDao
import com.personal.timetracker.data.dao.JiraIssueDao
import com.personal.timetracker.data.dao.JiraWorklogDao
import com.personal.timetracker.data.dao.JiraStatusDao
import com.personal.timetracker.data.dao.SettingsDao
import com.personal.timetracker.data.dao.TaskDao
import com.personal.timetracker.data.dao.TaskLogDao
import com.personal.timetracker.data.entity.AttendanceEntity
import com.personal.timetracker.data.entity.HolidayEntity
import com.personal.timetracker.data.entity.JiraFavoriteEntity
import com.personal.timetracker.data.entity.JiraIssueCacheEntity
import com.personal.timetracker.data.entity.JiraWorklogCacheEntity
import com.personal.timetracker.data.entity.JiraStatusEntity
import com.personal.timetracker.data.entity.SettingsEntity
import com.personal.timetracker.data.entity.TaskEntity
import com.personal.timetracker.data.entity.TaskLogEntity

@Database(
    entities = [
        AttendanceEntity::class, TaskEntity::class, TaskLogEntity::class, SettingsEntity::class, HolidayEntity::class,
        JiraFavoriteEntity::class, JiraIssueCacheEntity::class, JiraWorklogCacheEntity::class, JiraStatusEntity::class
    ],
    version = 14,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
    abstract fun taskDao(): TaskDao
    abstract fun taskLogDao(): TaskLogDao
    abstract fun settingsDao(): SettingsDao
    abstract fun holidayDao(): HolidayDao
    abstract fun jiraFavoriteDao(): JiraFavoriteDao
    abstract fun jiraIssueDao(): JiraIssueDao
    abstract fun jiraWorklogDao(): JiraWorklogDao
    abstract fun jiraStatusDao(): JiraStatusDao

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

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE settings ADD COLUMN jiraEnabled INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE settings ADD COLUMN jiraBaseUrl TEXT NOT NULL DEFAULT 'https://jira.demisco.com'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE settings ADD COLUMN jiraToken TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
            }
        }

        
        
        
        
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE settings ADD COLUMN jiraProjectCatalog TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // حذف تکراری‌های worklog بر اساس remoteId (نگه‌داشتن کوچک‌ترین localId)
                try {
                    db.execSQL("""
                        DELETE FROM jira_worklogs WHERE localId IN (
                            SELECT a.localId FROM jira_worklogs a
                            INNER JOIN jira_worklogs b
                              ON a.remoteId IS NOT NULL AND a.remoteId = b.remoteId
                             AND a.localId > b.localId
                        )
                    """.trimIndent())
                } catch (_: Exception) {}
                try {
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_jira_worklogs_remoteId ON jira_worklogs(remoteId)")
                } catch (_: Exception) {}
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE settings ADD COLUMN jiraFilterProjects TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE settings ADD COLUMN autoBackupDir TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE settings ADD COLUMN jiraFilterStatuses TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS jira_issues (
                    issueKey TEXT NOT NULL PRIMARY KEY,
                    summary TEXT NOT NULL DEFAULT '',
                    description TEXT,
                    projectKey TEXT NOT NULL DEFAULT '',
                    projectName TEXT NOT NULL DEFAULT '',
                    statusId TEXT NOT NULL DEFAULT '',
                    statusName TEXT NOT NULL DEFAULT '',
                    statusCategory TEXT NOT NULL DEFAULT '',
                    priorityName TEXT NOT NULL DEFAULT '',
                    issueTypeName TEXT NOT NULL DEFAULT '',
                    assigneeName TEXT,
                    reporterName TEXT,
                    labels TEXT NOT NULL DEFAULT '',
                    requiredMinutes INTEGER NOT NULL DEFAULT 0,
                    remainingMinutes INTEGER NOT NULL DEFAULT 0,
                    timeSpentMinutes INTEGER NOT NULL DEFAULT 0,
                    isFavorite INTEGER NOT NULL DEFAULT 0,
                    isAssignedToMe INTEGER NOT NULL DEFAULT 0,
                    jiraUpdated TEXT,
                    cachedAt TEXT NOT NULL DEFAULT ''
                )""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS jira_worklogs (
                    localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    remoteId TEXT,
                    issueKey TEXT NOT NULL,
                    date TEXT NOT NULL,
                    started TEXT,
                    durationMinutes INTEGER NOT NULL DEFAULT 0,
                    comment TEXT,
                    authorName TEXT,
                    syncStatus TEXT NOT NULL DEFAULT 'synced',
                    cachedAt TEXT NOT NULL DEFAULT ''
                )""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_jira_worklogs_issueKey ON jira_worklogs(issueKey)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_jira_worklogs_date ON jira_worklogs(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_jira_worklogs_syncStatus ON jira_worklogs(syncStatus)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS jira_statuses (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    categoryKey TEXT NOT NULL DEFAULT '',
                    categoryName TEXT NOT NULL DEFAULT '',
                    cachedAt TEXT NOT NULL DEFAULT ''
                )""")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS jira_favorites (
                        issueKey TEXT NOT NULL PRIMARY KEY,
                        summary TEXT NOT NULL DEFAULT '',
                        projectKey TEXT NOT NULL DEFAULT '',
                        projectName TEXT NOT NULL DEFAULT '',
                        note TEXT,
                        addedAt TEXT NOT NULL
                    )"""
                )
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
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
