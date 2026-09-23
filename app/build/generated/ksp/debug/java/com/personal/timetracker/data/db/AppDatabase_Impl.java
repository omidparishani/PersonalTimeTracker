package com.personal.timetracker.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.personal.timetracker.data.dao.AttendanceDao;
import com.personal.timetracker.data.dao.AttendanceDao_Impl;
import com.personal.timetracker.data.dao.HolidayDao;
import com.personal.timetracker.data.dao.HolidayDao_Impl;
import com.personal.timetracker.data.dao.JiraFavoriteDao;
import com.personal.timetracker.data.dao.JiraFavoriteDao_Impl;
import com.personal.timetracker.data.dao.JiraIssueDao;
import com.personal.timetracker.data.dao.JiraIssueDao_Impl;
import com.personal.timetracker.data.dao.JiraStatusDao;
import com.personal.timetracker.data.dao.JiraStatusDao_Impl;
import com.personal.timetracker.data.dao.JiraWorklogDao;
import com.personal.timetracker.data.dao.JiraWorklogDao_Impl;
import com.personal.timetracker.data.dao.SettingsDao;
import com.personal.timetracker.data.dao.SettingsDao_Impl;
import com.personal.timetracker.data.dao.TaskDao;
import com.personal.timetracker.data.dao.TaskDao_Impl;
import com.personal.timetracker.data.dao.TaskLogDao;
import com.personal.timetracker.data.dao.TaskLogDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile AttendanceDao _attendanceDao;

  private volatile TaskDao _taskDao;

  private volatile TaskLogDao _taskLogDao;

  private volatile SettingsDao _settingsDao;

  private volatile HolidayDao _holidayDao;

  private volatile JiraFavoriteDao _jiraFavoriteDao;

  private volatile JiraIssueDao _jiraIssueDao;

  private volatile JiraWorklogDao _jiraWorklogDao;

  private volatile JiraStatusDao _jiraStatusDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(14) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `attendance` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `entryTime` TEXT NOT NULL, `exitTime` TEXT, `duration` INTEGER NOT NULL, `leaveDuration` INTEGER NOT NULL, `overtimeDuration` INTEGER NOT NULL, `status` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `jiraNumber` TEXT, `projectName` TEXT NOT NULL, `taskTitle` TEXT NOT NULL, `description` TEXT, `requiredMinutes` INTEGER NOT NULL, `remainingMinutes` INTEGER NOT NULL, `status` TEXT NOT NULL, `isRunning` INTEGER NOT NULL, `runStartedAt` TEXT, `createdAt` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `task_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `taskId` INTEGER NOT NULL, `date` TEXT NOT NULL, `startTime` TEXT, `endTime` TEXT, `duration` INTEGER NOT NULL, `note` TEXT, `createdAt` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `settings` (`id` INTEGER NOT NULL, `startWorkTime` TEXT NOT NULL, `endWorkTime` TEXT NOT NULL, `flexibleMinutes` INTEGER NOT NULL, `minimumWorkMinutes` INTEGER NOT NULL, `isDarkMode` INTEGER NOT NULL, `themeColor` INTEGER NOT NULL, `projects` TEXT NOT NULL, `notifEnabled` INTEGER NOT NULL, `notifMinutesBefore` INTEGER NOT NULL, `notifTitle` TEXT NOT NULL, `notifBody` TEXT NOT NULL, `biometricEnabled` INTEGER NOT NULL, `workLat` REAL NOT NULL, `workLng` REAL NOT NULL, `workRadiusMeters` REAL NOT NULL, `geoAutoCheckIn` INTEGER NOT NULL, `geoAlertOnly` INTEGER NOT NULL, `geoAutoCheckOut` INTEGER NOT NULL, `weeklyRequiredMinutes` INTEGER NOT NULL, `thursdayWorking` INTEGER NOT NULL, `thursdayMinutes` INTEGER NOT NULL, `autoBackupEnabled` INTEGER NOT NULL, `autoBackupIntervalHours` INTEGER NOT NULL, `jiraEnabled` INTEGER NOT NULL, `jiraBaseUrl` TEXT NOT NULL, `jiraToken` TEXT NOT NULL, `jiraFilterStatuses` TEXT NOT NULL, `jiraFilterProjects` TEXT NOT NULL, `jiraProjectCatalog` TEXT NOT NULL, `autoBackupDir` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `holidays` (`date` TEXT NOT NULL, `title` TEXT NOT NULL, PRIMARY KEY(`date`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `jira_favorites` (`issueKey` TEXT NOT NULL, `summary` TEXT NOT NULL, `projectKey` TEXT NOT NULL, `projectName` TEXT NOT NULL, `note` TEXT, `addedAt` TEXT NOT NULL, PRIMARY KEY(`issueKey`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `jira_issues` (`issueKey` TEXT NOT NULL, `summary` TEXT NOT NULL, `description` TEXT, `projectKey` TEXT NOT NULL, `projectName` TEXT NOT NULL, `statusId` TEXT NOT NULL, `statusName` TEXT NOT NULL, `statusCategory` TEXT NOT NULL, `priorityName` TEXT NOT NULL, `issueTypeName` TEXT NOT NULL, `assigneeName` TEXT, `reporterName` TEXT, `labels` TEXT NOT NULL, `requiredMinutes` INTEGER NOT NULL, `remainingMinutes` INTEGER NOT NULL, `timeSpentMinutes` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `isAssignedToMe` INTEGER NOT NULL, `jiraUpdated` TEXT, `cachedAt` TEXT NOT NULL, PRIMARY KEY(`issueKey`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `jira_worklogs` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `remoteId` TEXT, `issueKey` TEXT NOT NULL, `date` TEXT NOT NULL, `started` TEXT, `durationMinutes` INTEGER NOT NULL, `comment` TEXT, `authorName` TEXT, `syncStatus` TEXT NOT NULL, `cachedAt` TEXT NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_jira_worklogs_issueKey` ON `jira_worklogs` (`issueKey`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_jira_worklogs_date` ON `jira_worklogs` (`date`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_jira_worklogs_syncStatus` ON `jira_worklogs` (`syncStatus`)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_jira_worklogs_remoteId` ON `jira_worklogs` (`remoteId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `jira_statuses` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `categoryKey` TEXT NOT NULL, `categoryName` TEXT NOT NULL, `cachedAt` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'dad5d216ee618c0a491473116b2a2e98')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `attendance`");
        db.execSQL("DROP TABLE IF EXISTS `tasks`");
        db.execSQL("DROP TABLE IF EXISTS `task_logs`");
        db.execSQL("DROP TABLE IF EXISTS `settings`");
        db.execSQL("DROP TABLE IF EXISTS `holidays`");
        db.execSQL("DROP TABLE IF EXISTS `jira_favorites`");
        db.execSQL("DROP TABLE IF EXISTS `jira_issues`");
        db.execSQL("DROP TABLE IF EXISTS `jira_worklogs`");
        db.execSQL("DROP TABLE IF EXISTS `jira_statuses`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsAttendance = new HashMap<String, TableInfo.Column>(8);
        _columnsAttendance.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("entryTime", new TableInfo.Column("entryTime", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("exitTime", new TableInfo.Column("exitTime", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("duration", new TableInfo.Column("duration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("leaveDuration", new TableInfo.Column("leaveDuration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("overtimeDuration", new TableInfo.Column("overtimeDuration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendance.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAttendance = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAttendance = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAttendance = new TableInfo("attendance", _columnsAttendance, _foreignKeysAttendance, _indicesAttendance);
        final TableInfo _existingAttendance = TableInfo.read(db, "attendance");
        if (!_infoAttendance.equals(_existingAttendance)) {
          return new RoomOpenHelper.ValidationResult(false, "attendance(com.personal.timetracker.data.entity.AttendanceEntity).\n"
                  + " Expected:\n" + _infoAttendance + "\n"
                  + " Found:\n" + _existingAttendance);
        }
        final HashMap<String, TableInfo.Column> _columnsTasks = new HashMap<String, TableInfo.Column>(11);
        _columnsTasks.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("jiraNumber", new TableInfo.Column("jiraNumber", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("projectName", new TableInfo.Column("projectName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("taskTitle", new TableInfo.Column("taskTitle", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("requiredMinutes", new TableInfo.Column("requiredMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("remainingMinutes", new TableInfo.Column("remainingMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("isRunning", new TableInfo.Column("isRunning", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("runStartedAt", new TableInfo.Column("runStartedAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("createdAt", new TableInfo.Column("createdAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTasks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTasks = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTasks = new TableInfo("tasks", _columnsTasks, _foreignKeysTasks, _indicesTasks);
        final TableInfo _existingTasks = TableInfo.read(db, "tasks");
        if (!_infoTasks.equals(_existingTasks)) {
          return new RoomOpenHelper.ValidationResult(false, "tasks(com.personal.timetracker.data.entity.TaskEntity).\n"
                  + " Expected:\n" + _infoTasks + "\n"
                  + " Found:\n" + _existingTasks);
        }
        final HashMap<String, TableInfo.Column> _columnsTaskLogs = new HashMap<String, TableInfo.Column>(8);
        _columnsTaskLogs.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTaskLogs.put("taskId", new TableInfo.Column("taskId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTaskLogs.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTaskLogs.put("startTime", new TableInfo.Column("startTime", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTaskLogs.put("endTime", new TableInfo.Column("endTime", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTaskLogs.put("duration", new TableInfo.Column("duration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTaskLogs.put("note", new TableInfo.Column("note", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTaskLogs.put("createdAt", new TableInfo.Column("createdAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTaskLogs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTaskLogs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTaskLogs = new TableInfo("task_logs", _columnsTaskLogs, _foreignKeysTaskLogs, _indicesTaskLogs);
        final TableInfo _existingTaskLogs = TableInfo.read(db, "task_logs");
        if (!_infoTaskLogs.equals(_existingTaskLogs)) {
          return new RoomOpenHelper.ValidationResult(false, "task_logs(com.personal.timetracker.data.entity.TaskLogEntity).\n"
                  + " Expected:\n" + _infoTaskLogs + "\n"
                  + " Found:\n" + _existingTaskLogs);
        }
        final HashMap<String, TableInfo.Column> _columnsSettings = new HashMap<String, TableInfo.Column>(31);
        _columnsSettings.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("startWorkTime", new TableInfo.Column("startWorkTime", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("endWorkTime", new TableInfo.Column("endWorkTime", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("flexibleMinutes", new TableInfo.Column("flexibleMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("minimumWorkMinutes", new TableInfo.Column("minimumWorkMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("isDarkMode", new TableInfo.Column("isDarkMode", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("themeColor", new TableInfo.Column("themeColor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("projects", new TableInfo.Column("projects", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("notifEnabled", new TableInfo.Column("notifEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("notifMinutesBefore", new TableInfo.Column("notifMinutesBefore", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("notifTitle", new TableInfo.Column("notifTitle", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("notifBody", new TableInfo.Column("notifBody", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("biometricEnabled", new TableInfo.Column("biometricEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("workLat", new TableInfo.Column("workLat", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("workLng", new TableInfo.Column("workLng", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("workRadiusMeters", new TableInfo.Column("workRadiusMeters", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("geoAutoCheckIn", new TableInfo.Column("geoAutoCheckIn", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("geoAlertOnly", new TableInfo.Column("geoAlertOnly", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("geoAutoCheckOut", new TableInfo.Column("geoAutoCheckOut", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("weeklyRequiredMinutes", new TableInfo.Column("weeklyRequiredMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("thursdayWorking", new TableInfo.Column("thursdayWorking", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("thursdayMinutes", new TableInfo.Column("thursdayMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("autoBackupEnabled", new TableInfo.Column("autoBackupEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("autoBackupIntervalHours", new TableInfo.Column("autoBackupIntervalHours", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("jiraEnabled", new TableInfo.Column("jiraEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("jiraBaseUrl", new TableInfo.Column("jiraBaseUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("jiraToken", new TableInfo.Column("jiraToken", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("jiraFilterStatuses", new TableInfo.Column("jiraFilterStatuses", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("jiraFilterProjects", new TableInfo.Column("jiraFilterProjects", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("jiraProjectCatalog", new TableInfo.Column("jiraProjectCatalog", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettings.put("autoBackupDir", new TableInfo.Column("autoBackupDir", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSettings = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSettings = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSettings = new TableInfo("settings", _columnsSettings, _foreignKeysSettings, _indicesSettings);
        final TableInfo _existingSettings = TableInfo.read(db, "settings");
        if (!_infoSettings.equals(_existingSettings)) {
          return new RoomOpenHelper.ValidationResult(false, "settings(com.personal.timetracker.data.entity.SettingsEntity).\n"
                  + " Expected:\n" + _infoSettings + "\n"
                  + " Found:\n" + _existingSettings);
        }
        final HashMap<String, TableInfo.Column> _columnsHolidays = new HashMap<String, TableInfo.Column>(2);
        _columnsHolidays.put("date", new TableInfo.Column("date", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHolidays.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHolidays = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHolidays = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoHolidays = new TableInfo("holidays", _columnsHolidays, _foreignKeysHolidays, _indicesHolidays);
        final TableInfo _existingHolidays = TableInfo.read(db, "holidays");
        if (!_infoHolidays.equals(_existingHolidays)) {
          return new RoomOpenHelper.ValidationResult(false, "holidays(com.personal.timetracker.data.entity.HolidayEntity).\n"
                  + " Expected:\n" + _infoHolidays + "\n"
                  + " Found:\n" + _existingHolidays);
        }
        final HashMap<String, TableInfo.Column> _columnsJiraFavorites = new HashMap<String, TableInfo.Column>(6);
        _columnsJiraFavorites.put("issueKey", new TableInfo.Column("issueKey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraFavorites.put("summary", new TableInfo.Column("summary", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraFavorites.put("projectKey", new TableInfo.Column("projectKey", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraFavorites.put("projectName", new TableInfo.Column("projectName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraFavorites.put("note", new TableInfo.Column("note", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraFavorites.put("addedAt", new TableInfo.Column("addedAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysJiraFavorites = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesJiraFavorites = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoJiraFavorites = new TableInfo("jira_favorites", _columnsJiraFavorites, _foreignKeysJiraFavorites, _indicesJiraFavorites);
        final TableInfo _existingJiraFavorites = TableInfo.read(db, "jira_favorites");
        if (!_infoJiraFavorites.equals(_existingJiraFavorites)) {
          return new RoomOpenHelper.ValidationResult(false, "jira_favorites(com.personal.timetracker.data.entity.JiraFavoriteEntity).\n"
                  + " Expected:\n" + _infoJiraFavorites + "\n"
                  + " Found:\n" + _existingJiraFavorites);
        }
        final HashMap<String, TableInfo.Column> _columnsJiraIssues = new HashMap<String, TableInfo.Column>(20);
        _columnsJiraIssues.put("issueKey", new TableInfo.Column("issueKey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("summary", new TableInfo.Column("summary", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("projectKey", new TableInfo.Column("projectKey", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("projectName", new TableInfo.Column("projectName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("statusId", new TableInfo.Column("statusId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("statusName", new TableInfo.Column("statusName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("statusCategory", new TableInfo.Column("statusCategory", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("priorityName", new TableInfo.Column("priorityName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("issueTypeName", new TableInfo.Column("issueTypeName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("assigneeName", new TableInfo.Column("assigneeName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("reporterName", new TableInfo.Column("reporterName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("labels", new TableInfo.Column("labels", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("requiredMinutes", new TableInfo.Column("requiredMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("remainingMinutes", new TableInfo.Column("remainingMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("timeSpentMinutes", new TableInfo.Column("timeSpentMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("isFavorite", new TableInfo.Column("isFavorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("isAssignedToMe", new TableInfo.Column("isAssignedToMe", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("jiraUpdated", new TableInfo.Column("jiraUpdated", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraIssues.put("cachedAt", new TableInfo.Column("cachedAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysJiraIssues = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesJiraIssues = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoJiraIssues = new TableInfo("jira_issues", _columnsJiraIssues, _foreignKeysJiraIssues, _indicesJiraIssues);
        final TableInfo _existingJiraIssues = TableInfo.read(db, "jira_issues");
        if (!_infoJiraIssues.equals(_existingJiraIssues)) {
          return new RoomOpenHelper.ValidationResult(false, "jira_issues(com.personal.timetracker.data.entity.JiraIssueCacheEntity).\n"
                  + " Expected:\n" + _infoJiraIssues + "\n"
                  + " Found:\n" + _existingJiraIssues);
        }
        final HashMap<String, TableInfo.Column> _columnsJiraWorklogs = new HashMap<String, TableInfo.Column>(10);
        _columnsJiraWorklogs.put("localId", new TableInfo.Column("localId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraWorklogs.put("remoteId", new TableInfo.Column("remoteId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraWorklogs.put("issueKey", new TableInfo.Column("issueKey", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraWorklogs.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraWorklogs.put("started", new TableInfo.Column("started", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraWorklogs.put("durationMinutes", new TableInfo.Column("durationMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraWorklogs.put("comment", new TableInfo.Column("comment", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraWorklogs.put("authorName", new TableInfo.Column("authorName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraWorklogs.put("syncStatus", new TableInfo.Column("syncStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraWorklogs.put("cachedAt", new TableInfo.Column("cachedAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysJiraWorklogs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesJiraWorklogs = new HashSet<TableInfo.Index>(4);
        _indicesJiraWorklogs.add(new TableInfo.Index("index_jira_worklogs_issueKey", false, Arrays.asList("issueKey"), Arrays.asList("ASC")));
        _indicesJiraWorklogs.add(new TableInfo.Index("index_jira_worklogs_date", false, Arrays.asList("date"), Arrays.asList("ASC")));
        _indicesJiraWorklogs.add(new TableInfo.Index("index_jira_worklogs_syncStatus", false, Arrays.asList("syncStatus"), Arrays.asList("ASC")));
        _indicesJiraWorklogs.add(new TableInfo.Index("index_jira_worklogs_remoteId", true, Arrays.asList("remoteId"), Arrays.asList("ASC")));
        final TableInfo _infoJiraWorklogs = new TableInfo("jira_worklogs", _columnsJiraWorklogs, _foreignKeysJiraWorklogs, _indicesJiraWorklogs);
        final TableInfo _existingJiraWorklogs = TableInfo.read(db, "jira_worklogs");
        if (!_infoJiraWorklogs.equals(_existingJiraWorklogs)) {
          return new RoomOpenHelper.ValidationResult(false, "jira_worklogs(com.personal.timetracker.data.entity.JiraWorklogCacheEntity).\n"
                  + " Expected:\n" + _infoJiraWorklogs + "\n"
                  + " Found:\n" + _existingJiraWorklogs);
        }
        final HashMap<String, TableInfo.Column> _columnsJiraStatuses = new HashMap<String, TableInfo.Column>(5);
        _columnsJiraStatuses.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraStatuses.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraStatuses.put("categoryKey", new TableInfo.Column("categoryKey", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraStatuses.put("categoryName", new TableInfo.Column("categoryName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJiraStatuses.put("cachedAt", new TableInfo.Column("cachedAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysJiraStatuses = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesJiraStatuses = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoJiraStatuses = new TableInfo("jira_statuses", _columnsJiraStatuses, _foreignKeysJiraStatuses, _indicesJiraStatuses);
        final TableInfo _existingJiraStatuses = TableInfo.read(db, "jira_statuses");
        if (!_infoJiraStatuses.equals(_existingJiraStatuses)) {
          return new RoomOpenHelper.ValidationResult(false, "jira_statuses(com.personal.timetracker.data.entity.JiraStatusEntity).\n"
                  + " Expected:\n" + _infoJiraStatuses + "\n"
                  + " Found:\n" + _existingJiraStatuses);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "dad5d216ee618c0a491473116b2a2e98", "fbc246943608ee241a3e2168fd44076a");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "attendance","tasks","task_logs","settings","holidays","jira_favorites","jira_issues","jira_worklogs","jira_statuses");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `attendance`");
      _db.execSQL("DELETE FROM `tasks`");
      _db.execSQL("DELETE FROM `task_logs`");
      _db.execSQL("DELETE FROM `settings`");
      _db.execSQL("DELETE FROM `holidays`");
      _db.execSQL("DELETE FROM `jira_favorites`");
      _db.execSQL("DELETE FROM `jira_issues`");
      _db.execSQL("DELETE FROM `jira_worklogs`");
      _db.execSQL("DELETE FROM `jira_statuses`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(AttendanceDao.class, AttendanceDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TaskDao.class, TaskDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TaskLogDao.class, TaskLogDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SettingsDao.class, SettingsDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HolidayDao.class, HolidayDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(JiraFavoriteDao.class, JiraFavoriteDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(JiraIssueDao.class, JiraIssueDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(JiraWorklogDao.class, JiraWorklogDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(JiraStatusDao.class, JiraStatusDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public AttendanceDao attendanceDao() {
    if (_attendanceDao != null) {
      return _attendanceDao;
    } else {
      synchronized(this) {
        if(_attendanceDao == null) {
          _attendanceDao = new AttendanceDao_Impl(this);
        }
        return _attendanceDao;
      }
    }
  }

  @Override
  public TaskDao taskDao() {
    if (_taskDao != null) {
      return _taskDao;
    } else {
      synchronized(this) {
        if(_taskDao == null) {
          _taskDao = new TaskDao_Impl(this);
        }
        return _taskDao;
      }
    }
  }

  @Override
  public TaskLogDao taskLogDao() {
    if (_taskLogDao != null) {
      return _taskLogDao;
    } else {
      synchronized(this) {
        if(_taskLogDao == null) {
          _taskLogDao = new TaskLogDao_Impl(this);
        }
        return _taskLogDao;
      }
    }
  }

  @Override
  public SettingsDao settingsDao() {
    if (_settingsDao != null) {
      return _settingsDao;
    } else {
      synchronized(this) {
        if(_settingsDao == null) {
          _settingsDao = new SettingsDao_Impl(this);
        }
        return _settingsDao;
      }
    }
  }

  @Override
  public HolidayDao holidayDao() {
    if (_holidayDao != null) {
      return _holidayDao;
    } else {
      synchronized(this) {
        if(_holidayDao == null) {
          _holidayDao = new HolidayDao_Impl(this);
        }
        return _holidayDao;
      }
    }
  }

  @Override
  public JiraFavoriteDao jiraFavoriteDao() {
    if (_jiraFavoriteDao != null) {
      return _jiraFavoriteDao;
    } else {
      synchronized(this) {
        if(_jiraFavoriteDao == null) {
          _jiraFavoriteDao = new JiraFavoriteDao_Impl(this);
        }
        return _jiraFavoriteDao;
      }
    }
  }

  @Override
  public JiraIssueDao jiraIssueDao() {
    if (_jiraIssueDao != null) {
      return _jiraIssueDao;
    } else {
      synchronized(this) {
        if(_jiraIssueDao == null) {
          _jiraIssueDao = new JiraIssueDao_Impl(this);
        }
        return _jiraIssueDao;
      }
    }
  }

  @Override
  public JiraWorklogDao jiraWorklogDao() {
    if (_jiraWorklogDao != null) {
      return _jiraWorklogDao;
    } else {
      synchronized(this) {
        if(_jiraWorklogDao == null) {
          _jiraWorklogDao = new JiraWorklogDao_Impl(this);
        }
        return _jiraWorklogDao;
      }
    }
  }

  @Override
  public JiraStatusDao jiraStatusDao() {
    if (_jiraStatusDao != null) {
      return _jiraStatusDao;
    } else {
      synchronized(this) {
        if(_jiraStatusDao == null) {
          _jiraStatusDao = new JiraStatusDao_Impl(this);
        }
        return _jiraStatusDao;
      }
    }
  }
}
