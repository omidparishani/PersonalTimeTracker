package com.personal.timetracker.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.personal.timetracker.data.db.AppDatabase
import com.personal.timetracker.data.entity.AttendanceEntity
import com.personal.timetracker.data.entity.SettingsEntity
import com.personal.timetracker.data.entity.TaskEntity
import com.personal.timetracker.data.entity.TaskLogEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BackupHelper {
    suspend fun exportJson(context: Context): File {
        val db = AppDatabase.get(context)
        val attendance = db.attendanceDao().getByRange("2000-01-01", "2100-12-31")
        val tasks = db.taskDao().getAllOnce()
        val logs = db.taskLogDao().getByRange("2000-01-01", "2100-12-31")
        val settings = db.settingsDao().get()

        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", TimeUtils.nowDateTime())
        root.put("app", "Personal Time Tracker")

        val attArr = JSONArray()
        attendance.forEach {
            attArr.put(JSONObject().apply {
                put("date", it.date)
                put("entryTime", it.entryTime)
                put("exitTime", it.exitTime)
                put("duration", it.duration)
                put("leaveDuration", it.leaveDuration)
                put("overtimeDuration", it.overtimeDuration)
                put("status", it.status)
            })
        }
        root.put("attendance", attArr)

        val taskArr = JSONArray()
        tasks.forEach {
            taskArr.put(JSONObject().apply {
                put("id", it.id)
                put("jiraNumber", it.jiraNumber)
                put("projectName", it.projectName)
                put("taskTitle", it.taskTitle)
                put("description", it.description)
                put("requiredMinutes", it.requiredMinutes)
                put("remainingMinutes", it.remainingMinutes)
                put("status", it.status)
                put("isRunning", false)
                put("createdAt", it.createdAt)
            })
        }
        root.put("tasks", taskArr)

        val logArr = JSONArray()
        logs.forEach {
            logArr.put(JSONObject().apply {
                put("taskId", it.taskId)
                put("date", it.date)
                put("startTime", it.startTime)
                put("endTime", it.endTime)
                put("duration", it.duration)
                put("note", it.note)
                put("createdAt", it.createdAt)
            })
        }
        root.put("task_logs", logArr)

        if (settings != null) {
            root.put("settings", JSONObject().apply {
                put("startWorkTime", settings.startWorkTime)
                put("endWorkTime", settings.endWorkTime)
                put("flexibleMinutes", settings.flexibleMinutes)
                put("minimumWorkMinutes", settings.minimumWorkMinutes)
                put("isDarkMode", settings.isDarkMode)
                put("themeColor", settings.themeColor)
                put("projects", settings.projects)
                put("notifEnabled", settings.notifEnabled)
                put("notifMinutesBefore", settings.notifMinutesBefore)
                put("notifTitle", settings.notifTitle)
                put("notifBody", settings.notifBody)
                put("biometricEnabled", settings.biometricEnabled)
                put("workLat", settings.workLat)
                put("workLng", settings.workLng)
                put("workRadiusMeters", settings.workRadiusMeters)
                put("geoAutoCheckIn", settings.geoAutoCheckIn)
                put("geoAlertOnly", settings.geoAlertOnly)
            })
        }

        val dir = File(context.cacheDir, "backup").apply { mkdirs() }
        val file = File(dir, "ptt_backup_${System.currentTimeMillis()}.json")
        file.writeText(root.toString(2))
        return file
    }

    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "اشتراک پشتیبان"))
    }

    suspend fun restoreJson(context: Context, json: String) {
        val db = AppDatabase.get(context)
        val root = JSONObject(json)
        db.clearAllTables()

        val attendance = root.optJSONArray("attendance")
        if (attendance != null) {
            for (i in 0 until attendance.length()) {
                val o = attendance.getJSONObject(i)
                db.attendanceDao().insert(
                    AttendanceEntity(
                        date = o.getString("date"),
                        entryTime = o.getString("entryTime"),
                        exitTime = if (o.isNull("exitTime")) null else o.getString("exitTime"),
                        duration = o.optInt("duration", 0),
                        leaveDuration = o.optInt("leaveDuration", 0),
                        overtimeDuration = o.optInt("overtimeDuration", 0),
                        status = o.optString("status", "completed")
                    )
                )
            }
        }

        val tasks = root.optJSONArray("tasks")
        if (tasks != null) {
            for (i in 0 until tasks.length()) {
                val o = tasks.getJSONObject(i)
                val req = o.optInt("requiredMinutes", o.optInt("duration", 0))
                val rem = o.optInt("remainingMinutes", req)
                db.taskDao().insert(
                    TaskEntity(
                        jiraNumber = if (o.isNull("jiraNumber")) null else o.optString("jiraNumber", null),
                        projectName = o.getString("projectName"),
                        taskTitle = o.getString("taskTitle"),
                        description = if (o.isNull("description")) null else o.optString("description", null),
                        requiredMinutes = req,
                        remainingMinutes = rem,
                        status = o.optString("status", "new"),
                        isRunning = false,
                        createdAt = o.optString("createdAt", TimeUtils.nowDateTime())
                    )
                )
            }
        }

        val logs = root.optJSONArray("task_logs")
        if (logs != null) {
            for (i in 0 until logs.length()) {
                val o = logs.getJSONObject(i)
                db.taskLogDao().insert(
                    TaskLogEntity(
                        taskId = o.getLong("taskId"),
                        date = o.getString("date"),
                        startTime = if (o.isNull("startTime")) null else o.optString("startTime", null),
                        endTime = if (o.isNull("endTime")) null else o.optString("endTime", null),
                        duration = o.optInt("duration", 0),
                        note = if (o.isNull("note")) null else o.optString("note", null),
                        createdAt = o.optString("createdAt", TimeUtils.nowDateTime())
                    )
                )
            }
        }

        val s = root.optJSONObject("settings")
        if (s != null) {
            db.settingsDao().upsert(
                SettingsEntity(
                    startWorkTime = s.optString("startWorkTime", "09:00"),
                    endWorkTime = s.optString("endWorkTime", "17:00"),
                    flexibleMinutes = s.optInt("flexibleMinutes", 30),
                    minimumWorkMinutes = s.optInt("minimumWorkMinutes", 480),
                    isDarkMode = s.optBoolean("isDarkMode", false),
                    themeColor = s.optInt("themeColor", 0xFF1565C0.toInt()),
                    projects = s.optString("projects", ""),
                    notifEnabled = s.optBoolean("notifEnabled", true),
                    notifMinutesBefore = s.optInt("notifMinutesBefore", 30),
                    notifTitle = s.optString("notifTitle", "یادآوری پایان کار"),
                    notifBody = s.optString("notifBody", "زمان پایان کار نزدیک است"),
                    biometricEnabled = s.optBoolean("biometricEnabled", false),
                    workLat = s.optDouble("workLat", 0.0),
                    workLng = s.optDouble("workLng", 0.0),
                    workRadiusMeters = s.optDouble("workRadiusMeters", 150.0).toFloat(),
                    geoAutoCheckIn = s.optBoolean("geoAutoCheckIn", false),
                    geoAlertOnly = s.optBoolean("geoAlertOnly", true)
                )
            )
        }
    }

    suspend fun clearAll(context: Context, includeSettings: Boolean) {
        val db = AppDatabase.get(context)
        db.attendanceDao().deleteAll()
        db.taskDao().deleteAll()
        db.taskLogDao().deleteAll()
        if (includeSettings) {
            db.settingsDao().upsert(SettingsEntity())
        }
    }
}
