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
import com.personal.timetracker.data.entity.JiraFavoriteEntity
import com.personal.timetracker.data.entity.JiraStatusEntity
import com.personal.timetracker.data.entity.JiraWorklogCacheEntity
import com.personal.timetracker.data.entity.JiraIssueCacheEntity
import com.personal.timetracker.jira.JiraService
import com.personal.timetracker.jira.toTaskEntity
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
    private val jiraFavoriteDao = db.jiraFavoriteDao()
    private val jiraIssueDao = db.jiraIssueDao()
    private val jiraWorklogDao = db.jiraWorklogDao()
    private val jiraStatusDao = db.jiraStatusDao()

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

    // ==================== Jira ====================

    fun observeJiraFavorites() = jiraFavoriteDao.observeAll()
    suspend fun getJiraFavorites() = jiraFavoriteDao.getAllOnce()
    suspend fun isJiraFavorite(key: String) = jiraFavoriteDao.exists(key.trim().uppercase()) > 0

    suspend fun addJiraFavorite(
        issueKey: String,
        summary: String = "",
        projectKey: String = "",
        projectName: String = "",
        note: String? = null
    ) {
        jiraFavoriteDao.upsert(
            JiraFavoriteEntity(
                issueKey = issueKey.trim().uppercase(),
                summary = summary,
                projectKey = projectKey,
                projectName = projectName,
                note = note,
                addedAt = TimeUtils.nowDateTime()
            )
        )
    }

    suspend fun removeJiraFavorite(issueKey: String) {
        jiraFavoriteDao.deleteByKey(issueKey.trim().uppercase())
    }

    /** سرویس جیرا از تنظیمات فعلی؛ null اگر پیکربندی نشده */
    suspend fun jiraServiceOrNull(): JiraService? {
        return JiraService.fromSettings(getSettings())
    }

    /** همگام‌سازی Issueهای assign‌شده به تسک‌های محلی */
    suspend fun syncJiraIssues(openOnly: Boolean = true): Result<Int> = withContext(Dispatchers.IO) {
        val service = jiraServiceOrNull()
            ?: return@withContext Result.failure(Exception("جیرا در تنظیمات فعال/پیکربندی نشده"))
        service.fetchAssigned(openOnly = openOnly).map { issues ->
            var count = 0
            val existing = taskDao.getAllOnce()
            val byJira = existing.filter { !it.jiraNumber.isNullOrBlank() }
                .associateBy { it.jiraNumber!!.trim().uppercase() }
            for (issue in issues) {
                val key = issue.key.trim().uppercase()
                val prev = byJira[key]
                val entity = issue.toTaskEntity(
                    existingId = prev?.id ?: 0L,
                    createdAt = prev?.createdAt ?: TimeUtils.nowDateTime()
                ).copy(
                    isRunning = prev?.isRunning ?: false,
                    runStartedAt = prev?.runStartedAt
                )
                saveTask(entity)
                count++
            }
            count
        }
    }

    /**
     * ارسال Worklog به جیرا.
     * null = رد شد (بدون jiraNumber یا بدون پیکربندی)
     */
    suspend fun pushWorklogToJira(
        task: TaskEntity,
        durationMinutes: Int,
        date: String,
        note: String? = null
    ): Result<String>? = withContext(Dispatchers.IO) {
        val jiraKey = task.jiraNumber?.trim().orEmpty()
        if (jiraKey.isEmpty()) return@withContext null
        val service = jiraServiceOrNull() ?: return@withContext null
        val started = JiraService.toJiraStarted(date)
        service.addWorklog(jiraKey, durationMinutes, started, note).map { wl ->
            val rid = wl.id
            if (!rid.isNullOrBlank()) {
                val existing = jiraWorklogDao.getByRemoteId(rid)
                val entity = JiraWorklogCacheEntity(
                    localId = existing?.localId ?: 0,
                    remoteId = rid,
                    issueKey = jiraKey.uppercase(),
                    date = date,
                    started = started,
                    durationMinutes = durationMinutes,
                    comment = note,
                    authorName = wl.author?.displayName ?: wl.author?.name,
                    syncStatus = "synced",
                    cachedAt = TimeUtils.nowDateTime()
                )
                if (existing != null) jiraWorklogDao.update(entity) else jiraWorklogDao.upsert(entity)
            }
            rid ?: ""
        }
    }

    /** ثبت مستقیم Worklog روی هر Issue (assign یا علاقه‌مندی) */
    suspend fun addJiraWorklog(
        issueKey: String,
        durationMinutes: Int,
        date: String,
        note: String? = null,
        timeHHmm: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val service = jiraServiceOrNull()
            ?: return@withContext Result.failure(Exception("جیرا پیکربندی نشده"))
        val key = issueKey.trim().uppercase()
        val started = JiraService.toJiraStarted(date, timeHHmm)
        service.addWorklog(key, durationMinutes, started, note).map { wl ->
            val rid = wl.id
            if (!rid.isNullOrBlank()) {
                val existing = jiraWorklogDao.getByRemoteId(rid)
                val entity = JiraWorklogCacheEntity(
                    localId = existing?.localId ?: 0,
                    remoteId = rid,
                    issueKey = key,
                    date = date,
                    started = started,
                    durationMinutes = durationMinutes,
                    comment = note,
                    authorName = wl.author?.displayName ?: wl.author?.name,
                    syncStatus = "synced",
                    cachedAt = TimeUtils.nowDateTime()
                )
                if (existing != null) jiraWorklogDao.update(entity) else jiraWorklogDao.upsert(entity)
            }
            rid ?: ""
        }
    }




    // ---- Jira as Tasks (cache) ----

    fun observeJiraIssues() = jiraIssueDao.observeAll()
    suspend fun getDistinctJiraProjects(): List<String> {
        return jiraIssueDao.getAllOnce()
            .flatMap { listOf(it.projectKey, it.projectName) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    fun observeJiraAssigned() = jiraIssueDao.observeAssigned()
    fun observeJiraFavoritesIssues() = jiraIssueDao.observeFavorites()
    fun observeJiraOpen() = jiraIssueDao.observeOpen()
    fun searchJiraIssues(q: String) = jiraIssueDao.search(q)
    fun observeJiraStatuses() = jiraStatusDao.observeAll()
    suspend fun getJiraStatuses() = jiraStatusDao.getAllOnce()
    fun observeJiraWorklogs(issueKey: String) = jiraWorklogDao.observeByIssue(issueKey.trim().uppercase())
    suspend fun getJiraWorklogsOnce(issueKey: String) = jiraWorklogDao.getByIssueOnce(issueKey.trim().uppercase())
    suspend fun jiraWorklogMinutesInRange(start: String, end: String) = jiraWorklogDao.sumMinutesInRange(start, end)

    /** واکشی وضعیت‌های معتبر از API شرکت و ذخیره در کش */
    suspend fun refreshJiraStatuses(): Result<Int> = withContext(Dispatchers.IO) {
        val service = jiraServiceOrNull()
            ?: return@withContext Result.failure(Exception("جیرا پیکربندی نشده"))
        service.fetchStatuses().map { list ->
            val now = TimeUtils.nowDateTime()
            val entities = list.map {
                JiraStatusEntity(
                    id = it.id!!,
                    name = it.name!!,
                    categoryKey = it.statusCategory?.key.orEmpty(),
                    categoryName = it.statusCategory?.name.orEmpty(),
                    cachedAt = now
                )
            }
            jiraStatusDao.clear()
            jiraStatusDao.upsertAll(entities)
            entities.size
        }
    }

    /**
     * همگام‌سازی Issueها از سرور به کش محلی.
     * assigned + favorites keys را پوشش می‌دهد.
     */

    /**
     * همگام‌سازی Issueها از سرور.
     * @param projectKeys اگر خالی نباشد: همه Issueهای این پروژه‌ها (نه فقط اساین)
     * @param append اگر true، به کش اضافه می‌کند؛ وگرنه فقط همان صفحه را upsert می‌کند
     * @return Triple(تعداد دریافت‌شده، startAt بعدی، total سرور) یا failure
     */
    suspend fun refreshJiraIssues(
        openOnly: Boolean = false,
        projectKeys: List<String> = emptyList(),
        textQuery: String? = null,
        startAt: Int = 0,
        pageSize: Int = 50,
        append: Boolean = true,
        assignedToMe: Boolean = false,
        statusNames: List<String> = emptyList()
    ): Result<Triple<Int, Int, Int>> = withContext(Dispatchers.IO) {
        val service = jiraServiceOrNull()
            ?: return@withContext Result.failure(Exception("جیرا پیکربندی نشده"))
        val now = TimeUtils.nowDateTime()
        val favKeys = jiraFavoriteDao.getAllOnce().map { it.issueKey.uppercase() }.toSet()
        val me = service.testConnection().getOrNull()
        val myNames = listOfNotNull(me?.name, me?.displayName, me?.emailAddress)
            .map { n -> n.lowercase() }.toSet()

        val pageResult = service.fetchIssues(
            projectKeys = projectKeys,
            assignedToMe = assignedToMe,
            openOnly = openOnly && statusNames.isEmpty(),
            statusNames = statusNames,
            maxResults = pageSize,
            startAt = startAt,
            textQuery = textQuery
        )
        if (pageResult.isFailure) return@withContext Result.failure(pageResult.exceptionOrNull()!!)
        val page = pageResult.getOrNull()!!

        val entities = page.items.map { issue ->
            val key = issue.key.uppercase()
            val prev = jiraIssueDao.getByKey(key)
            JiraIssueCacheEntity(
                issueKey = key,
                summary = issue.summary,
                description = issue.description,
                projectKey = issue.projectKey,
                projectName = issue.projectName,
                statusId = "",
                statusName = issue.statusName,
                statusCategory = issue.statusCategory,
                priorityName = issue.priorityName,
                issueTypeName = issue.issueTypeName,
                assigneeName = issue.assigneeName,
                labels = issue.labels.joinToString(","),
                requiredMinutes = issue.requiredMinutes,
                remainingMinutes = issue.remainingMinutes,
                timeSpentMinutes = issue.timeSpentMinutes,
                isFavorite = key in favKeys || (prev?.isFavorite == true),
                isAssignedToMe = assignedToMe ||
                    (issue.assigneeName?.lowercase()?.let { it in myNames } == true),
                jiraUpdated = null,
                cachedAt = now
            )
        }
        jiraIssueDao.upsertAll(entities)

        // favorites not in page
        if (startAt == 0) {
            for (fav in jiraFavoriteDao.getAllOnce()) {
                val key = fav.issueKey.uppercase()
                if (jiraIssueDao.getByKey(key) != null) continue
                val remote = service.getIssue(key).getOrNull()
                if (remote != null) {
                    jiraIssueDao.upsert(
                        JiraIssueCacheEntity(
                            issueKey = key,
                            summary = remote.summary,
                            description = remote.description,
                            projectKey = remote.projectKey,
                            projectName = remote.projectName,
                            statusName = remote.statusName,
                            statusCategory = remote.statusCategory,
                            priorityName = remote.priorityName,
                            issueTypeName = remote.issueTypeName,
                            assigneeName = remote.assigneeName,
                            labels = remote.labels.joinToString(","),
                            requiredMinutes = remote.requiredMinutes,
                            remainingMinutes = remote.remainingMinutes,
                            timeSpentMinutes = remote.timeSpentMinutes,
                            isFavorite = true,
                            isAssignedToMe = false,
                            cachedAt = now
                        )
                    )
                }
            }
        }

        val nextStart = startAt + page.items.size
        Result.success(Triple(page.items.size, nextStart, page.total))
    }


    suspend fun refreshJiraProjectsCatalog(): Result<Int> = withContext(Dispatchers.IO) {
        val service = jiraServiceOrNull()
            ?: return@withContext Result.failure(Exception("جیرا پیکربندی نشده"))
        service.fetchProjects().map { list ->
            val keys = list.mapNotNull { it.key?.trim()?.uppercase() }.filter { it.isNotEmpty() }
            val s = getSettings()
            saveSettings(s.copy(jiraProjectCatalog = keys.joinToString(",")))
            keys.size
        }
    }

    suspend fun getJiraProjectCatalog(): List<String> {
        val s = getSettings()
        return s.jiraProjectCatalog.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    suspend fun saveJiraListFilters(projects: Set<String>, statuses: Set<String>) {
        val s = getSettings()
        saveSettings(
            s.copy(
                jiraFilterProjects = projects.joinToString(","),
                jiraFilterStatuses = statuses.joinToString(",")
            )
        )
    }


    /** دریافت Worklogهای یک Issue از سرور و جایگزینی کش (به‌جز pendingهای محلی) */
    suspend fun refreshJiraWorklogs(issueKey: String): Result<Int> = withContext(Dispatchers.IO) {
        val key = issueKey.trim().uppercase()
        val service = jiraServiceOrNull()
            ?: return@withContext Result.failure(Exception("جیرا پیکربندی نشده"))
        service.getWorklogs(key).map { remoteList ->
            val pending = jiraWorklogDao.getByIssueOnce(key).filter { it.syncStatus != "synced" }
            jiraWorklogDao.deleteSyncedForIssue(key)
            val now = TimeUtils.nowDateTime()
            val seenRemote = mutableSetOf<String>()
            val entities = remoteList.mapNotNull { wl ->
                val rid = wl.id?.trim().orEmpty()
                if (rid.isEmpty() || rid in seenRemote) return@mapNotNull null
                seenRemote.add(rid)
                val started = wl.started
                val date = started?.take(10) ?: TimeUtils.today()
                JiraWorklogCacheEntity(
                    remoteId = rid,
                    issueKey = key,
                    date = date,
                    started = started,
                    durationMinutes = ((wl.timeSpentSeconds ?: 0) / 60).coerceAtLeast(0),
                    comment = wl.comment,
                    authorName = wl.author?.displayName ?: wl.author?.name,
                    syncStatus = "synced",
                    cachedAt = now
                )
            }
            // upsert one-by-one by remoteId to avoid duplicates
            entities.forEach { entity ->
                val existing = jiraWorklogDao.getByRemoteId(entity.remoteId!!)
                if (existing != null) {
                    jiraWorklogDao.update(entity.copy(localId = existing.localId))
                } else {
                    jiraWorklogDao.upsert(entity)
                }
            }
            // re-upsert pending local ops (بدون remoteId تکراری)
            pending.forEach { p ->
                if (p.remoteId != null && jiraWorklogDao.getByRemoteId(p.remoteId) != null) return@forEach
                jiraWorklogDao.upsert(p.copy(localId = 0))
            }
            jiraWorklogDao.dedupeByRemoteId()
            entities.size
        }
    }

    /** ثبت لاگ (= Worklog). آنلاین → سرور؛ آفلاین → صف pending_add */
    suspend fun addJiraTaskLog(
        issueKey: String,
        durationMinutes: Int,
        date: String,
        comment: String? = null,
        timeHHmm: String? = null
    ): Result<JiraWorklogCacheEntity> = withContext(Dispatchers.IO) {
        val key = issueKey.trim().uppercase()
        if (durationMinutes <= 0) return@withContext Result.failure(Exception("مدت نامعتبر"))
        val now = TimeUtils.nowDateTime()
        val started = JiraService.toJiraStarted(date, timeHHmm)
        val local = JiraWorklogCacheEntity(
            issueKey = key,
            date = date,
            started = started,
            durationMinutes = durationMinutes,
            comment = comment,
            syncStatus = "pending_add",
            cachedAt = now
        )
        val localId = jiraWorklogDao.upsert(local)
        val saved = jiraWorklogDao.getByLocalId(localId) ?: local.copy(localId = localId)
        val service = jiraServiceOrNull()
        if (service == null) {
            return@withContext Result.success(saved) // offline queue
        }
        val remote = service.addWorklog(key, durationMinutes, started, comment)
        if (remote.isSuccess) {
            val wl = remote.getOrNull()!!
            val synced = saved.copy(
                remoteId = wl.id,
                syncStatus = "synced",
                authorName = wl.author?.displayName ?: wl.author?.name,
                cachedAt = now
            )
            jiraWorklogDao.update(synced)
            Result.success(synced)
        } else {
            // keep pending
            Result.success(saved)
        }
    }

    /** ویرایش Worklog — به سرور هم اعمال می‌شود */
    suspend fun updateJiraTaskLog(
        localId: Long,
        durationMinutes: Int,
        date: String,
        comment: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val row = jiraWorklogDao.getByLocalId(localId)
            ?: return@withContext Result.failure(Exception("لاگ یافت نشد"))
        val updated = row.copy(
            durationMinutes = durationMinutes,
            date = date,
            comment = comment,
            syncStatus = if (row.remoteId != null) "pending_update" else "pending_add",
            cachedAt = TimeUtils.nowDateTime()
        )
        jiraWorklogDao.update(updated)
        val service = jiraServiceOrNull() ?: return@withContext Result.success(Unit)
        if (updated.remoteId != null) {
            service.updateWorklog(updated.issueKey, updated.remoteId, durationMinutes, comment).fold(
                onSuccess = {
                    jiraWorklogDao.update(updated.copy(syncStatus = "synced"))
                    Result.success(Unit)
                },
                onFailure = { Result.success(Unit) } // stays pending
            )
        } else Result.success(Unit)
    }

    /** حذف Worklog — روی سرور هم */
    suspend fun deleteJiraTaskLog(localId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val row = jiraWorklogDao.getByLocalId(localId)
            ?: return@withContext Result.failure(Exception("لاگ یافت نشد"))
        if (row.remoteId == null) {
            jiraWorklogDao.delete(row)
            return@withContext Result.success(Unit)
        }
        jiraWorklogDao.update(row.copy(syncStatus = "pending_delete"))
        val service = jiraServiceOrNull() ?: return@withContext Result.success(Unit)
        service.deleteWorklog(row.issueKey, row.remoteId).fold(
            onSuccess = {
                jiraWorklogDao.delete(row)
                Result.success(Unit)
            },
            onFailure = { Result.success(Unit) }
        )
    }

    /** ارسال همه عملیات pending به سرور */
    suspend fun flushPendingJiraWorklogs(): Result<Int> = withContext(Dispatchers.IO) {
        val service = jiraServiceOrNull()
            ?: return@withContext Result.failure(Exception("جیرا پیکربندی نشده"))
        val pending = jiraWorklogDao.getPending()
        var ok = 0
        for (row in pending) {
            when (row.syncStatus) {
                "pending_add" -> {
                    val r = service.addWorklog(row.issueKey, row.durationMinutes, row.started, row.comment)
                    if (r.isSuccess) {
                        jiraWorklogDao.update(row.copy(remoteId = r.getOrNull()?.id, syncStatus = "synced"))
                        ok++
                    }
                }
                "pending_update" -> {
                    val rid = row.remoteId ?: continue
                    val r = service.updateWorklog(row.issueKey, rid, row.durationMinutes, row.comment)
                    if (r.isSuccess) {
                        jiraWorklogDao.update(row.copy(syncStatus = "synced"))
                        ok++
                    }
                }
                "pending_delete" -> {
                    val rid = row.remoteId
                    if (rid == null) {
                        jiraWorklogDao.delete(row)
                        ok++
                    } else {
                        val r = service.deleteWorklog(row.issueKey, rid)
                        if (r.isSuccess) {
                            jiraWorklogDao.delete(row)
                            ok++
                        }
                    }
                }
            }
        }
        Result.success(ok)
    }

    suspend fun toggleJiraFavorite(issueKey: String, summary: String = "", projectKey: String = "", projectName: String = ""): Boolean =
        withContext(Dispatchers.IO) {
            val key = issueKey.trim().uppercase()
            val exists = jiraFavoriteDao.exists(key) > 0
            if (exists) {
                jiraFavoriteDao.deleteByKey(key)
                jiraIssueDao.setFavorite(key, false)
                false
            } else {
                addJiraFavorite(key, summary, projectKey, projectName)
                val issue = jiraIssueDao.getByKey(key)
                if (issue != null) jiraIssueDao.setFavorite(key, true)
                else {
                    // ensure row exists
                    jiraIssueDao.upsert(
                        JiraIssueCacheEntity(
                            issueKey = key,
                            summary = summary.ifBlank { key },
                            projectKey = projectKey,
                            projectName = projectName,
                            isFavorite = true,
                            cachedAt = TimeUtils.nowDateTime()
                        )
                    )
                }
                true
            }
        }


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
        jiraWorklogDao.dedupeByRemoteId()
        val jiraRows = dedupeWorklogs(
            filterOwnWorklogs(
                jiraWorklogDao.getByRange(start, end).filter { it.syncStatus != "pending_delete" }
            )
        )
        if (jiraRows.isNotEmpty()) {
            val issues = jiraIssueDao.getAllOnce().associateBy { it.issueKey.uppercase() }
            return jiraRows
                .groupBy { it.issueKey.uppercase() }
                .map { (key, list) ->
                    val title = issues[key]?.summary
                        ?: taskDao.getAllOnce().firstOrNull { it.jiraNumber?.uppercase() == key }?.taskTitle
                        ?: ""
                    JiraSum(key, list.sumOf { it.durationMinutes }, title)
                }
                .filter { it.total > 0 }
                .sortedByDescending { it.total }
        }
        // fallback به لاگ محلی قدیمی وقتی کش جیرا خالی است
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

        // زمان تسک از Worklogهای جیرا فقط کاربر فعلی؛ در صورت خالی بودن، fallback به لاگ محلی
        val jiraMins = filterOwnWorklogs(
            jiraWorklogDao.getByRange(start, end).filter { it.syncStatus != "pending_delete" }
        ).sumOf { it.durationMinutes }
        val localMins = taskLogDao.getByRange(start, end).sumOf { it.duration }
        val logMinutes = if (jiraMins > 0) jiraMins else localMins

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
        jiraWorklogDao.dedupeByRemoteId()
        val jiraLogs = filterOwnWorklogs(
            jiraWorklogDao.getByRange(start, end).filter { it.syncStatus != "pending_delete" }
        )
        val localLogs = taskLogDao.getByRange(start, end)
        val issues = jiraIssueDao.getAllOnce().associateBy { it.issueKey.uppercase() }
        val tasks = taskDao.getAllOnce().associateBy { it.id }
        val byDate = (
            days.map { it.date } +
            jiraLogs.map { it.date } +
            localLogs.map { it.date }
        ).toSet().sorted()
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
            // اولویت با Worklogهای جیرا (بدون تکرار)
            val dayJira = dedupeWorklogs(
                jiraLogs.filter { it.date == date && it.syncStatus != "pending_delete" }
            )
            val taskLines = if (dayJira.isNotEmpty()) {
                dayJira.map { wl ->
                    val issue = issues[wl.issueKey.uppercase()]
                    val startedShort = wl.started?.take(16)?.replace("T", " ")
                    val noteParts = listOfNotNull(
                        startedShort?.let { "شروع: $it" },
                        wl.comment?.takeIf { it.isNotBlank() }
                    )
                    TaskLogLine(
                        logId = wl.localId,
                        taskId = 0L,
                        taskTitle = issue?.summary ?: wl.issueKey,
                        jira = wl.issueKey,
                        project = issue?.projectName ?: issue?.projectKey,
                        duration = wl.durationMinutes,
                        note = noteParts.joinToString(" · ").ifBlank { null }
                    )
                }
            } else {
                localLogs.filter { it.date == date }.map { log ->
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


    /**
     * سینک Worklogهای کاربر فعلی برای یک بازه تاریخ از سرور جیرا.
     * با JQL: worklogAuthor = currentUser() AND worklogDate >= start AND worklogDate <= end
     * سپس برای هر Issue، worklogها را می‌گیرد و فقط موارد همان بازه/نویسنده را در کش می‌نویسد.
     */
    suspend fun syncWorklogsForDateRange(start: String, end: String): Result<Int> = withContext(Dispatchers.IO) {
        val service = jiraServiceOrNull()
            ?: return@withContext Result.failure(Exception("جیرا پیکربندی نشده"))
        val me = service.myself().getOrElse {
            return@withContext Result.failure(it)
        }
        val myNames = listOfNotNull(me.name, me.key, me.displayName)
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

        val jql = """worklogAuthor = currentUser() AND worklogDate >= "$start" AND worklogDate <= "$end" ORDER BY updated DESC"""
        val page = service.search(jql, maxResults = 50).getOrElse {
            // fallback بدون worklogDate (بعضی نسخه‌ها)
            val jql2 = "worklogAuthor = currentUser() ORDER BY updated DESC"
            service.search(jql2, maxResults = 50).getOrElse { e ->
                return@withContext Result.failure(e)
            }
        }
        var count = 0
        val now = TimeUtils.nowDateTime()
        for (issue in page.items) {
            val key = issue.key.uppercase()
            val remoteList = service.getWorklogs(key).getOrElse { emptyList() }
            for (wl in remoteList) {
                val rid = wl.id?.trim().orEmpty()
                if (rid.isEmpty()) continue
                val started = wl.started
                val date = started?.take(10) ?: continue
                if (date < start || date > end) continue
                // فقط worklog خود کاربر
                val author = (wl.author?.name ?: wl.author?.displayName ?: "").trim().lowercase()
                if (myNames.isNotEmpty() && author.isNotEmpty() && author !in myNames) {
                    // اگر displayName متفاوت بود، باز هم اگر worklogAuthor JQL آورده احتمالاً مال ماست — نگه می‌داریم وقتی author خالی است
                    val dn = (wl.author?.displayName ?: "").trim().lowercase()
                    if (dn.isNotEmpty() && dn !in myNames && author !in myNames) continue
                }
                val entity = JiraWorklogCacheEntity(
                    remoteId = rid,
                    issueKey = key,
                    date = date,
                    started = started,
                    durationMinutes = ((wl.timeSpentSeconds ?: 0) / 60).coerceAtLeast(0),
                    comment = wl.comment,
                    authorName = wl.author?.displayName ?: wl.author?.name,
                    syncStatus = "synced",
                    cachedAt = now
                )
                val existing = jiraWorklogDao.getByRemoteId(rid)
                if (existing != null) {
                    jiraWorklogDao.update(entity.copy(localId = existing.localId))
                } else {
                    jiraWorklogDao.upsert(entity)
                }
                // کش خلاصه issue
                val prev = jiraIssueDao.getByKey(key)
                if (prev == null) {
                    jiraIssueDao.upsert(
                        JiraIssueCacheEntity(
                            issueKey = key,
                            summary = issue.summary,
                            description = issue.description,
                            projectKey = issue.projectKey,
                            projectName = issue.projectName,
                            statusName = issue.statusName,
                            statusCategory = issue.statusCategory,
                            priorityName = issue.priorityName,
                            issueTypeName = issue.issueTypeName,
                            assigneeName = issue.assigneeName,
                            requiredMinutes = issue.requiredMinutes,
                            remainingMinutes = issue.remainingMinutes,
                            timeSpentMinutes = issue.timeSpentMinutes,
                            cachedAt = now
                        )
                    )
                }
                count++
            }
        }
        jiraWorklogDao.dedupeByRemoteId()
        Result.success(count)
    }

    suspend fun syncWorklogsForDate(date: String): Result<Int> =
        syncWorklogsForDateRange(date, date)


    /** فقط Worklogهای کاربر فعلی (تقویم/گزارش). pending بدون author حفظ می‌شود. */
    private suspend fun filterOwnWorklogs(rows: List<JiraWorklogCacheEntity>): List<JiraWorklogCacheEntity> {
        val me = try {
            jiraServiceOrNull()?.myself()?.getOrNull()
        } catch (_: Exception) { null } ?: return rows
        val myNames = listOfNotNull(me.name, me.key, me.displayName)
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
        if (myNames.isEmpty()) return rows
        return rows.filter { wl ->
            val a = wl.authorName?.trim()?.lowercase().orEmpty()
            a.isEmpty() || a in myNames
        }
    }

    /** Worklogهای یک روز برای تقویم — بدون تکرار بر اساس remoteId */
    suspend fun getJiraWorklogsForDate(date: String): List<JiraWorklogCacheEntity> {
        jiraWorklogDao.dedupeByRemoteId()
        val rows = jiraWorklogDao.getByDateOnce(date).filter { it.syncStatus != "pending_delete" }
        return dedupeWorklogs(filterOwnWorklogs(rows))
    }

    /** حذف تکراری در حافظه: اولویت با remoteId، سپس issueKey+started+duration */
    private fun dedupeWorklogs(rows: List<JiraWorklogCacheEntity>): List<JiraWorklogCacheEntity> {
        val byRemote = linkedMapOf<String, JiraWorklogCacheEntity>()
        val withoutRemote = mutableListOf<JiraWorklogCacheEntity>()
        for (r in rows) {
            val rid = r.remoteId?.trim()
            if (!rid.isNullOrEmpty()) {
                byRemote.putIfAbsent(rid, r)
            } else {
                withoutRemote.add(r)
            }
        }
        val pendingKeys = mutableSetOf<String>()
        val pendingUnique = withoutRemote.filter { p ->
            val k = "${p.issueKey}|${p.started}|${p.durationMinutes}|${p.comment}"
            if (k in pendingKeys) false else { pendingKeys.add(k); true }
        }
        return (byRemote.values + pendingUnique).sortedWith(
            compareByDescending<JiraWorklogCacheEntity> { it.started ?: "" }
                .thenByDescending { it.localId }
        )
    }

    suspend fun transitionJiraIssue(issueKey: String, transitionId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val service = jiraServiceOrNull()
                ?: return@withContext Result.failure(Exception("جیرا پیکربندی نشده"))
            service.transitionIssue(issueKey, transitionId).onSuccess {
                // refresh single issue into cache
                service.getIssue(issueKey).onSuccess { issue ->
                    val prev = jiraIssueDao.getByKey(issue.key.uppercase())
                    jiraIssueDao.upsert(
                        JiraIssueCacheEntity(
                            issueKey = issue.key.uppercase(),
                            summary = issue.summary,
                            description = issue.description,
                            projectKey = issue.projectKey,
                            projectName = issue.projectName,
                            statusName = issue.statusName,
                            statusCategory = issue.statusCategory,
                            priorityName = issue.priorityName,
                            issueTypeName = issue.issueTypeName,
                            assigneeName = issue.assigneeName,
                            labels = issue.labels.joinToString(","),
                            requiredMinutes = issue.requiredMinutes,
                            remainingMinutes = issue.remainingMinutes,
                            timeSpentMinutes = issue.timeSpentMinutes,
                            isFavorite = prev?.isFavorite == true,
                            isAssignedToMe = prev?.isAssignedToMe == true,
                            cachedAt = TimeUtils.nowDateTime()
                        )
                    )
                }
            }
        }

    suspend fun fetchJiraTransitions(issueKey: String) = withContext(Dispatchers.IO) {
        val service = jiraServiceOrNull()
            ?: return@withContext Result.failure(Exception("جیرا پیکربندی نشده"))
        service.fetchTransitions(issueKey)
    }

    suspend fun addJiraComment(issueKey: String, body: String) = withContext(Dispatchers.IO) {
        val service = jiraServiceOrNull()
            ?: return@withContext Result.failure(Exception("جیرا پیکربندی نشده"))
        service.addComment(issueKey, body)
    }

    suspend fun getJiraComments(issueKey: String) = withContext(Dispatchers.IO) {
        val service = jiraServiceOrNull()
            ?: return@withContext Result.failure(Exception("جیرا پیکربندی نشده"))
        service.getComments(issueKey)
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
