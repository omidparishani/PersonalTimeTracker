package com.personal.timetracker.data.repository

import android.content.Context
import com.personal.timetracker.data.db.AppDatabase
import com.personal.timetracker.data.entity.AttendanceEntity
import com.personal.timetracker.data.entity.SettingsEntity
import com.personal.timetracker.data.entity.TaskEntity
import com.personal.timetracker.data.entity.TaskLogEntity
import com.personal.timetracker.util.NotifHelper
import com.personal.timetracker.util.TimeCalc
import com.personal.timetracker.util.TimeUtils
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek

class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.get(context)
    private val attendanceDao = db.attendanceDao()
    private val taskDao = db.taskDao()
    private val taskLogDao = db.taskLogDao()
    private val settingsDao = db.settingsDao()

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

    suspend fun checkIn(date: String = TimeUtils.today(), entryTime: String = TimeUtils.nowTime()) {
        val active = attendanceDao.getActive()
        if (active != null && active.date == date) return
        attendanceDao.insert(AttendanceEntity(date = date, entryTime = entryTime, status = "active"))
        val settings = getSettings()
        if (settings.notifEnabled && date == TimeUtils.today()) {
            val end = TimeCalc.suggestedEnd(entryTime, settings.minimumWorkMinutes)
            NotifHelper.scheduleWorkEnd(
                appContext, end, settings.notifMinutesBefore,
                settings.notifTitle, settings.notifBody
            )
        }
    }

    suspend fun checkOut(exitTime: String = TimeUtils.nowTime()) {
        val active = attendanceDao.getActive() ?: return
        val settings = getSettings()
        val duration = TimeUtils.minutesBetween(active.entryTime, exitTime)
        val leave = TimeCalc.earlyLeave(active.entryTime, exitTime, settings.minimumWorkMinutes)
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
                leaveDuration = leave + mid,
                status = "completed"
            )
        )
        NotifHelper.cancel(appContext)
    }

    suspend fun addAttendance(date: String, entry: String, exit: String?) {
        val settings = getSettings()
        if (exit == null) {
            attendanceDao.insert(AttendanceEntity(date = date, entryTime = entry, status = "active"))
        } else {
            val dur = TimeUtils.minutesBetween(entry, exit)
            val leave = TimeCalc.earlyLeave(entry, exit, settings.minimumWorkMinutes)
            attendanceDao.insert(
                AttendanceEntity(
                    date = date, entryTime = entry, exitTime = exit,
                    duration = dur, leaveDuration = leave, status = "completed"
                )
            )
        }
    }

    suspend fun updateAttendance(item: AttendanceEntity) {
        val settings = getSettings()
        val updated = if (item.exitTime != null) {
            val dur = TimeUtils.minutesBetween(item.entryTime, item.exitTime)
            val leave = TimeCalc.earlyLeave(item.entryTime, item.exitTime, settings.minimumWorkMinutes)
            item.copy(duration = dur, leaveDuration = leave, status = "completed")
        } else item.copy(status = "active")
        attendanceDao.update(updated)
    }

    suspend fun deleteAttendance(item: AttendanceEntity) = attendanceDao.delete(item)

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

    suspend fun report(start: String, end: String): ReportData {
        val settings = getSettings()
        val days = attendanceDao.getByRange(start, end)
        val byDate = days.groupBy { it.date }

        var worked = 0
        var leave = 0
        var overtime = 0
        var undertime = 0

        byDate.forEach { (date, recs) ->

            var dayWork = 0
            var dayLeave = 0

            recs.forEach { r ->
                if (r.exitTime != null) {
                    dayWork += r.duration
                    dayLeave += r.leaveDuration
                } else if (r.status == "active") {
                    dayWork += TimeUtils.minutesBetween(
                        r.entryTime,
                        TimeUtils.nowTime()
                    ).coerceAtLeast(0)
                }
            }

            worked += dayWork
            leave += dayLeave

            if (TimeUtils.isWeekend(date)) {
                if (dayWork > 0) {
                    overtime += dayWork
                }

            } else {

                val diff = dayWork - settings.minimumWorkMinutes

                if (diff > 0) {
                    overtime += diff
                } else {
                    undertime += -diff
                }
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
        val settings = getSettings()
        val days = attendanceDao.getByRange(start, end)
        val logs = taskLogDao.getByRange(start, end)
        val tasks = taskDao.getAllOnce().associateBy { it.id }
        val byDate = (days.map { it.date } + logs.map { it.date }).toSet().sorted()
        return byDate.map { date ->
            val att = days.filter { it.date == date }
            var dayWork = 0
            var dayLeave = 0
            att.forEach { r ->
                if (r.exitTime != null) {
                    dayWork += r.duration
                    dayLeave += r.leaveDuration
                } else if (r.status == "active") {
                    dayWork += TimeUtils.minutesBetween(r.entryTime, TimeUtils.nowTime()).coerceAtLeast(0)
                }
            }
            val dayLogs = logs.filter { it.date == date }
            val taskLines = dayLogs.map { log ->
                val t = tasks[log.taskId]
                TaskLogLine(
                    taskTitle = t?.taskTitle ?: "تسک #${log.taskId}",
                    jira = t?.jiraNumber,
                    project = t?.projectName,
                    duration = log.duration,
                    note = log.note
                )
            }
            val ot = (dayWork - settings.minimumWorkMinutes).coerceAtLeast(0)
            DayBreakdown(
                date = date,
                attendance = att,
                worked = dayWork,
                leave = dayLeave,
                overtime = ot,
                taskLogs = taskLines
            )
        }
    }
}

data class TaskLogLine(
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
