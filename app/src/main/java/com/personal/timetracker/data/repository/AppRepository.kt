package com.personal.timetracker.data.repository

import android.content.Context
import android.util.Log
import com.personal.timetracker.data.db.AppDatabase
import com.personal.timetracker.data.dao.JiraSum
import com.personal.timetracker.data.dao.ProjectSum
import com.personal.timetracker.data.entity.AttendanceEntity
import com.personal.timetracker.data.entity.HolidayEntity
import com.personal.timetracker.data.entity.SettingsEntity
import com.personal.timetracker.data.entity.TaskEntity
import com.personal.timetracker.data.entity.TaskLogEntity
import com.personal.timetracker.util.NotifHelper
import com.personal.timetracker.util.TimeCalc
import com.personal.timetracker.util.TimeUtils
import com.personal.timetracker.util.DynamicAppIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.get(context)
    private val attendanceDao = db.attendanceDao()
    private val taskDao = db.taskDao()
    private val taskLogDao = db.taskLogDao()
    private val settingsDao = db.settingsDao()
    private val holidayDao = db.holidayDao()

    fun observeSettings(): Flow<SettingsEntity?> = settingsDao.observe()
    suspend fun getSettings(): SettingsEntity =
        settingsDao.get() ?: SettingsEntity().also { settingsDao.upsert(it) }
    suspend fun saveSettings(s: SettingsEntity) = settingsDao.upsert(s)

    fun observeActive(): Flow<AttendanceEntity?> = attendanceDao.observeActive()
    fun observeToday(): Flow<List<AttendanceEntity>> =
        attendanceDao.observeByDate(TimeUtils.today())

    fun observeAttendance(date: String): Flow<List<AttendanceEntity>> =
        attendanceDao.observeByDate(date)
    suspend fun getByDateOnce(date: String) = attendanceDao.getByDateOnce(date)

    // ---- Holidays ----
    fun observeHolidays(): Flow<List<HolidayEntity>> = holidayDao.observeAll()
    suspend fun getHolidaysOnce() = holidayDao.getAllOnce()
    suspend fun addHoliday(date: String, title: String) = holidayDao.insert(HolidayEntity(date, title))
    suspend fun deleteHoliday(item: HolidayEntity) = holidayDao.delete(item)
    suspend fun isHoliday(date: String): Boolean = holidayDao.countForDate(date) > 0

    /** Required work minutes for [date], accounting for the weekly schedule, whether
     *  Thursday is a working day, and any holiday marked for that date. */
    suspend fun requiredMinutesFor(date: String, settings: SettingsEntity): Int {
        val holiday = isHoliday(date)
        return TimeCalc.requiredMinutesForDate(
            date, settings.weeklyRequiredMinutes, settings.thursdayWorking, settings.thursdayMinutes, holiday
        )
    }

    /**
     * Fetches official Iranian holidays for a Jalali year from GitHub (hasan-ahani/shamsi-holidays).
     * One single HTTP request returns the full year as a JSON array.
     *
     * Format: [ { "date": "1404-01-01", "is_holiday": true, "events": [ { "description": "...", "is_holiday": true } ] } ]
     *
     * Falls back to holidayapi.ir (day-by-day) if GitHub is unreachable.
     */
    suspend fun fetchHolidaysFromInternet(jalaliYear: Int): Int = withContext(Dispatchers.IO) {
        val added = tryFetchFromGitHub(jalaliYear)
        if (added >= 0) return@withContext added
        // fallback: holidayapi.ir month-by-month
        return@withContext tryFetchFromHolidayApiIr(jalaliYear)
    }

    private fun fetchJson(rawUrl: String, timeoutMs: Int = 15000): String? {
        return try {
            val conn = URL(rawUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "PersonalTimeTracker/1.0")
            if (conn.responseCode == 200)
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            else null
        } catch (e: Exception) {
            Log.w("PTT", "fetchJson failed: $rawUrl", e)
            null
        }
    }

    /** Returns number of holidays added, or -1 on failure. */
    private suspend fun tryFetchFromGitHub(jalaliYear: Int): Int {
        val url = "https://raw.githubusercontent.com/hasan-ahani/shamsi-holidays/main/holidays/$jalaliYear.json"
        val body = fetchJson(url) ?: return -1
        return try {
            val arr = org.json.JSONArray(body)
            var added = 0
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                if (!item.optBoolean("is_holiday", false)) continue
                val dateRaw = item.optString("date", "") // "1404-01-01"
                val parts = dateRaw.split("-")
                if (parts.size != 3) continue
                val jy = parts[0].toIntOrNull() ?: continue
                val jm = parts[1].toIntOrNull() ?: continue
                val jd = parts[2].toIntOrNull() ?: continue
                // جمع همه عناوین تعطیل آن روز با هم
                val events = item.optJSONArray("events")
                val titles = buildList {
                    if (events != null) {
                        for (e in 0 until events.length()) {
                            val ev = events.getJSONObject(e)
                            if (ev.optBoolean("is_holiday", false)) {
                                val desc = ev.optString("description", "")
                                if (desc.isNotBlank()) add(desc)
                            }
                        }
                    }
                }
                val title = titles.firstOrNull() ?: "تعطیل رسمی"
                try {
                    val greg = TimeUtils.fromJalali(jy, jm, jd)
                    holidayDao.insert(HolidayEntity(TimeUtils.formatDate(greg), title))
                    added++
                } catch (e: Exception) {
                    Log.w("PTT", "skip invalid $jy/$jm/$jd", e)
                }
            }
            Log.i("PTT", "GitHub holidays: $added added for $jalaliYear")
            added
        } catch (e: Exception) {
            Log.w("PTT", "GitHub holiday parse failed", e)
            -1
        }
    }

    /** Fallback: holidayapi.ir — one request per day in month (only holidays). Returns count or -1. */
    private suspend fun tryFetchFromHolidayApiIr(jalaliYear: Int): Int {
        var added = 0
        var failed = 0
        for (jm in 1..12) {
            val days = TimeUtils.jalaliMonthDays(jalaliYear, jm)
            for (jd in 1..days) {
                val url = "https://holidayapi.ir/jalali/$jalaliYear/$jm/$jd"
                val body = fetchJson(url, 6000)
                if (body == null) { failed++; continue }
                try {
                    val obj = org.json.JSONObject(body)
                    if (!obj.optBoolean("is_holiday", false)) continue
                    val events = obj.optJSONArray("events")
                    val title = buildString {
                        if (events != null) {
                            for (e in 0 until events.length()) {
                                val ev = events.getJSONObject(e)
                                if (ev.optBoolean("is_holiday", false)) {
                                    val d = ev.optString("description", "")
                                    if (d.isNotBlank()) { if (isNotEmpty()) append(" / "); append(d) }
                                }
                            }
                        }
                    }.ifBlank { "تعطیل رسمی" }
                    val greg = TimeUtils.fromJalali(jalaliYear, jm, jd)
                    holidayDao.insert(HolidayEntity(TimeUtils.formatDate(greg), title))
                    added++
                } catch (e: Exception) {
                    Log.w("PTT", "parse err $jalaliYear/$jm/$jd", e)
                }
            }
        }
        if (failed > 100) throw Exception("اتصال به اینترنت برقرار نشد")
        Log.i("PTT", "holidayapi.ir: $added added for $jalaliYear")
        return added
    }

    suspend fun checkIn(date: String = TimeUtils.today(), entryTime: String = TimeUtils.nowTime()) {
        val active = attendanceDao.getActive()
        if (active != null && active.date == date) return
        attendanceDao.insert(AttendanceEntity(date = date, entryTime = entryTime, status = "active"))
        val settings = getSettings()
        if (settings.notifEnabled && date == TimeUtils.today()) {
            val required = requiredMinutesFor(date, settings)
            val end = TimeCalc.applyFlex(
                entryTime, settings.startWorkTime, settings.flexibleMinutes, required
            ).suggestedEnd
            NotifHelper.scheduleWorkEnd(
                appContext, end, settings.notifMinutesBefore,
                settings.notifTitle, settings.notifBody
            )
        }
        try { DynamicAppIcon.syncNow(appContext) } catch (_: Exception) {}
    }

    suspend fun checkOut(exitTime: String = TimeUtils.nowTime()) {
        val active = attendanceDao.getActive() ?: return
        val settings = getSettings()
        val duration = TimeUtils.minutesBetween(active.entryTime, exitTime)

        val required = requiredMinutesFor(active.date, settings)
        val flex = TimeCalc.applyFlex(
            active.entryTime, settings.startWorkTime, settings.flexibleMinutes, required
        )
        val (exitLeave, exitOvertime) = TimeCalc.exitOutcome(exitTime, flex.suggestedEnd)

        var mid = 0
        val dayRecords = attendanceDao.getByDateOnce(active.date)
        val prev = dayRecords.filter { it.id != active.id && it.exitTime != null }.lastOrNull()
        if (prev?.exitTime != null) {
            mid = TimeCalc.midDayLeave(prev.exitTime!!, active.entryTime)
        }
        attendanceDao.update(
            active.copy(
                exitTime = exitTime,
                duration = duration,
                leaveDuration = flex.entryLeaveMinutes + exitLeave + mid,
                overtimeDuration = exitOvertime,
                status = "completed"
            )
        )
        NotifHelper.cancel(appContext)
        try { DynamicAppIcon.syncNow(appContext) } catch (_: Exception) {}
    }

    suspend fun addAttendance(date: String, entry: String, exit: String?) {
        val settings = getSettings()
        if (exit == null) {
            attendanceDao.insert(AttendanceEntity(date = date, entryTime = entry, status = "active"))
        } else {
            val dur = TimeUtils.minutesBetween(entry, exit)
            val required = requiredMinutesFor(date, settings)
            val flex = TimeCalc.applyFlex(entry, settings.startWorkTime, settings.flexibleMinutes, required)
            val (exitLeave, exitOvertime) = TimeCalc.exitOutcome(exit, flex.suggestedEnd)
            attendanceDao.insert(
                AttendanceEntity(
                    date = date, entryTime = entry, exitTime = exit,
                    duration = dur,
                    leaveDuration = flex.entryLeaveMinutes + exitLeave,
                    overtimeDuration = exitOvertime,
                    status = "completed"
                )
            )
        }
        try { DynamicAppIcon.syncNow(appContext) } catch (_: Exception) {}
    }

    suspend fun updateAttendance(item: AttendanceEntity) {
        val settings = getSettings()
        val updated = if (item.exitTime != null) {
            val dur = TimeUtils.minutesBetween(item.entryTime, item.exitTime)
            val required = requiredMinutesFor(item.date, settings)
            val flex = TimeCalc.applyFlex(item.entryTime, settings.startWorkTime, settings.flexibleMinutes, required)
            val (exitLeave, exitOvertime) = TimeCalc.exitOutcome(item.exitTime, flex.suggestedEnd)
            item.copy(
                duration = dur,
                leaveDuration = flex.entryLeaveMinutes + exitLeave,
                overtimeDuration = exitOvertime,
                status = "completed"
            )
        } else item.copy(status = "active")
        attendanceDao.update(updated)
        try { DynamicAppIcon.syncNow(appContext) } catch (_: Exception) {}
    }

    suspend fun deleteAttendance(item: AttendanceEntity) {
        attendanceDao.delete(item)
        try { DynamicAppIcon.syncNow(appContext) } catch (_: Exception) {}
    }

    /**
     * Re-applies the current flex/schedule/holiday rules to every stored attendance record
     * that has an exit time. Useful after changing shift settings, or after upgrading from
     * an older version whose leave/overtime numbers were computed differently. Returns how
     * many records were updated.
     */
    suspend fun recalculateAllAttendance(): Int {
        val settings = getSettings()
        val all = attendanceDao.getByRange("0000-01-01", "9999-12-31")
        var count = 0
        val byDate = all.groupBy { it.date }
        byDate.forEach { (date, recs) ->
            val required = requiredMinutesFor(date, settings)
            var prevExit: String? = null
            recs.sortedBy { it.entryTime }.forEach { r ->
                if (r.exitTime != null) {
                    val dur = TimeUtils.minutesBetween(r.entryTime, r.exitTime)
                    val flex = TimeCalc.applyFlex(r.entryTime, settings.startWorkTime, settings.flexibleMinutes, required)
                    val (exitLeave, exitOvertime) = TimeCalc.exitOutcome(r.exitTime, flex.suggestedEnd)
                    val mid = if (prevExit != null) TimeCalc.midDayLeave(prevExit!!, r.entryTime) else 0
                    attendanceDao.update(
                        r.copy(
                            duration = dur,
                            leaveDuration = flex.entryLeaveMinutes + exitLeave + mid,
                            overtimeDuration = exitOvertime
                        )
                    )
                    count++
                    prevExit = r.exitTime
                }
            }
        }
        try { DynamicAppIcon.syncNow(appContext) } catch (_: Exception) {}
        return count
    }

    // ---- Tasks ----
    fun observeTasks() = taskDao.getAll()
    fun observeActiveTasks() = taskDao.getActive()
    fun observeTasksByStatus(status: String) = taskDao.getByStatus(status)
    fun observeRunningTask() = taskDao.observeRunning()
    fun searchTasks(q: String) = taskDao.search(q)
    suspend fun getTask(id: Long) = taskDao.getById(id)

    suspend fun saveTask(task: TaskEntity): Long {
        return if (task.id == 0L) {
            val remaining = if (task.remainingMinutes > 0) task.remainingMinutes else task.requiredMinutes
            taskDao.insert(task.copy(remainingMinutes = remaining))
        } else {
            taskDao.update(task)
            task.id
        }
    }

    suspend fun deleteTask(task: TaskEntity) = taskDao.delete(task)

    suspend fun startTimer(task: TaskEntity) {
        taskDao.getRunning()?.let { if (it.id != task.id) stopTimer(it) }
        taskDao.update(
            task.copy(
                isRunning = true,
                runStartedAt = TimeUtils.nowDateTime(),
                status = if (task.status == "done") "in_progress" else task.status.ifBlank { "in_progress" }.let {
                    if (it == "new") "in_progress" else it
                }
            )
        )
    }

    suspend fun stopTimer(task: TaskEntity, logDate: String = TimeUtils.today()) {
        if (!task.isRunning || task.runStartedAt == null) return
        val start = try {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).parse(task.runStartedAt!!)
        } catch (_: Exception) { null }
        val mins = if (start != null) {
            ((System.currentTimeMillis() - start.time) / 60000).toInt().coerceAtLeast(1)
        } else 1
        addLog(task, logDate, mins, task.runStartedAt, TimeUtils.nowDateTime())
    }

    /** Manual log for any day */
    suspend fun addLog(
        task: TaskEntity,
        date: String,
        durationMinutes: Int,
        startTime: String? = null,
        endTime: String? = null,
        note: String? = null
    ) {
        val dur = durationMinutes.coerceAtLeast(0)
        taskLogDao.insert(
            TaskLogEntity(
                taskId = task.id,
                date = date,
                startTime = startTime,
                endTime = endTime,
                duration = dur,
                note = note,
                createdAt = TimeUtils.nowDateTime()
            )
        )
        val remaining = (task.remainingMinutes - dur).coerceAtLeast(0)
        val status = when {
            remaining <= 0 -> "done"
            task.status == "new" || task.isRunning -> "in_progress"
            else -> task.status
        }
        taskDao.update(
            task.copy(
                remainingMinutes = remaining,
                status = status,
                isRunning = false,
                runStartedAt = null
            )
        )
    }

    suspend fun getLogsByTask(taskId: Long) = taskLogDao.getByTaskOnce(taskId)
    suspend fun getAllLogsOnce() = taskLogDao.getAllOnce()

    suspend fun updateLog(log: TaskLogEntity) {
        taskLogDao.update(log)
    }

    suspend fun deleteLog(log: TaskLogEntity) {
        taskLogDao.delete(log)
    }

    suspend fun recalculateTask(taskId: Long) {

        val task = taskDao.getById(taskId) ?: return

        val totalLogged = taskLogDao
            .getByTaskOnce(taskId)
            .sumOf { it.duration }

        taskDao.update(
            task.copy(
                remainingMinutes =
                    (task.requiredMinutes - totalLogged)
                        .coerceAtLeast(0),
                status = if (totalLogged >= task.requiredMinutes)
                    "done"
                else
                    task.status
            )
        )
    }
    suspend fun getLogsByDate(date: String) = taskLogDao.getByDateOnce(date)
    fun observeLogsByDate(date: String) = taskLogDao.getByDate(date)
    suspend fun getTasksByDateOnce(date: String): List<TaskEntity> {
        val logs = taskLogDao.getByDateOnce(date)
        val ids = logs.map { it.taskId }.toSet()
        return taskDao.getAllOnce().filter { it.id in ids }
    }

    suspend fun projectSummary() = taskDao.projectSummary()
    suspend fun jiraSummary() = taskDao.jiraSummary()

    /** Same as [projectSummary]/[jiraSummary] but scoped to a date range, based on actual
     *  logged time in that range rather than lifetime task totals — used by Reports so the
     *  bottom charts respect the daily/weekly/monthly filter above them. */
    suspend fun projectSummaryRange(start: String, end: String): List<ProjectSum> {
        val logs = taskLogDao.getByRange(start, end)
        val tasks = taskDao.getAllOnce().associateBy { it.id }
        return logs.groupBy { tasks[it.taskId]?.projectName ?: "بدون پروژه" }
            .map { (proj, list) -> ProjectSum(proj, list.sumOf { it.duration }) }
            .filter { it.total > 0 }
            .sortedByDescending { it.total }
    }

    suspend fun jiraSummaryRange(start: String, end: String): List<JiraSum> {
        val logs = taskLogDao.getByRange(start, end)
        val tasks = taskDao.getAllOnce().associateBy { it.id }
        return logs.mapNotNull { log ->
            val task = tasks[log.taskId] ?: return@mapNotNull null
            val jira = task.jiraNumber
            if (jira.isNullOrBlank()) null else Triple(jira, task.taskTitle, log.duration)
        }.groupBy({ it.first }, { it.third })
            .map { (jira, durations) ->
                val title = tasks.values.firstOrNull { it.jiraNumber == jira }?.taskTitle ?: ""
                JiraSum(jira, durations.sum(), title)
            }
            .sortedByDescending { it.total }
    }

    suspend fun report(start: String, end: String): ReportData {
        val settings = getSettings()
        val days = attendanceDao.getByRange(start, end)
        val byDate = days.groupBy { it.date }

        var worked = 0
        var leave = 0
        var overtime = 0
        var undertime = 0

        // Days with actual attendance records: leave/overtime come straight from the
        // flex-aware per-record fields computed at check-in/out time.
        byDate.forEach { (date, recs) ->
            var dayWork = 0
            recs.forEach { r ->
                if (r.exitTime != null) {
                    dayWork += r.duration
                    leave += r.leaveDuration
                    overtime += r.overtimeDuration
                } else if (r.status == "active") {
                    dayWork += TimeUtils.minutesBetween(
                        r.entryTime, TimeUtils.nowTime()
                    ).coerceAtLeast(0)
                }
            }
            worked += dayWork
        }

        // Days within range that required work but have no attendance record at all count
        // fully as undertime (absence), without double counting holidays/non-working days.
        TimeUtils.datesBetween(start, end).forEach { date ->
            if (date !in byDate.keys) {
                val required = requiredMinutesFor(date, settings)
                if (required > 0) undertime += required
            }
        }

        val logMinutes = taskLogDao.getByRange(start, end)
            .sumOf { it.duration }

        return ReportData(
            worked,
            leave,
            days.size,
            overtime,
            undertime,
            logMinutes
        )
    }

    suspend fun dayBreakdown(start: String, end: String): List<DayBreakdown> {
        val days = attendanceDao.getByRange(start, end)
        val logs = taskLogDao.getByRange(start, end)
        val tasks = taskDao.getAllOnce().associateBy { it.id }
        val byDate = (days.map { it.date } + logs.map { it.date }).toSet().sorted()
        return byDate.map { date ->
            val att = days.filter { it.date == date }
            var dayWork = 0
            var dayLeave = 0
            var dayOvertime = 0
            att.forEach { r ->
                if (r.exitTime != null) {
                    dayWork += r.duration
                    dayLeave += r.leaveDuration
                    dayOvertime += r.overtimeDuration
                } else if (r.status == "active") {
                    dayWork += TimeUtils.minutesBetween(r.entryTime, TimeUtils.nowTime()).coerceAtLeast(0)
                }
            }
            val dayLogs = logs.filter { it.date == date }
            val taskLines = dayLogs.map { log ->
                val t = tasks[log.taskId]
                TaskLogLine(
                    logId = log.id,
                    taskId = log.taskId,
                    taskTitle = t?.taskTitle ?: "تسک #${log.taskId}",
                    jira = t?.jiraNumber,
                    project = t?.projectName,
                    duration = log.duration,
                    note = log.note
                )
            }
            DayBreakdown(
                date = date,
                attendance = att,
                worked = dayWork,
                leave = dayLeave,
                overtime = dayOvertime,
                taskLogs = taskLines
            )
        }
    }
}

data class TaskLogLine(
    val logId: Long,
    val taskId: Long,
    val taskTitle: String,
    val jira: String?,
    val project: String?,
    val duration: Int,
    val note: String?
)

data class DayBreakdown(
    val date: String,
    val attendance: List<AttendanceEntity>,
    val worked: Int,
    val leave: Int,
    val overtime: Int,
    val taskLogs: List<TaskLogLine>
)


data class ReportData(
    val worked: Int,
    val leave: Int,
    val entryCount: Int,
    val overtime: Int,
    val undertime: Int = 0,
    val taskLogMinutes: Int = 0
)
