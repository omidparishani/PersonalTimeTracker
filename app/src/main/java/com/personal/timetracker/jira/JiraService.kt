package com.personal.timetracker.jira

import com.personal.timetracker.data.entity.SettingsEntity
import com.personal.timetracker.data.entity.TaskEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * لایه سطح‌بالای عملیات جیرا برای UI.
 * همه متدها Result برمی‌گردانند تا پیام خطا به فارسی نمایش داده شود.
 */
class JiraService(
    private val baseUrl: String,
    private val token: String
) {
    private val api = JiraClient.create(baseUrl, token)

    /** مدل ساده‌شده برای لیست UI */
    data class IssueItem(
        val key: String,
        val summary: String,
        val description: String?,
        val projectKey: String,
        val projectName: String,
        val statusCategory: String,   // new | indeterminate | done
        val statusName: String,
        val priorityName: String,
        val issueTypeName: String,
        val assigneeName: String?,
        val remainingMinutes: Int,
        val requiredMinutes: Int,
        val timeSpentMinutes: Int,
        val labels: List<String>
    )

    suspend fun myself(): Result<JiraUser> = testConnection()

    suspend fun testConnection(): Result<JiraUser> = runCatching {
        val resp = api.myself()
        if (resp.isSuccessful && resp.body() != null) resp.body()!!
        else throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
    }

    /** Issueهای assign‌شده به کاربر فعلی */
    suspend fun fetchAssigned(
        openOnly: Boolean = true,
        maxResults: Int = 50,
        extraJql: String? = null
    ): Result<List<IssueItem>> = runCatching {
        val base = if (openOnly) {
            "assignee = currentUser() AND statusCategory != Done"
        } else {
            "assignee = currentUser()"
        }
        val jql = buildString {
            append(base)
            if (!extraJql.isNullOrBlank()) append(" AND ($extraJql)")
            append(" ORDER BY priority DESC, updated DESC")
        }
        searchJql(jql, maxResults).items
    }

    /** جستجوی آزاد با JQL یا کلید Issue */
    suspend fun search(
        query: String,
        maxResults: Int = 30
    ): Result<Page> = runCatching {
        val q = query.trim()
        val jql = when {
            q.matches(Regex("[A-Za-z][A-Za-z0-9]+-\\d+")) ->
                "key = $q"
            q.contains("=") || q.contains("AND") || q.contains("OR") ->
                q
            else ->
                """text ~ "$q" OR summary ~ "$q" ORDER BY updated DESC"""
        }
        searchJql(jql, maxResults)
    }

    /** جزئیات یک Issue با کلید */
    suspend fun getIssue(issueKey: String): Result<IssueItem> = runCatching {
        val resp = api.getIssue(issueKey.trim().uppercase())
        if (!resp.isSuccessful || resp.body() == null) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
        resp.body()!!.toItem() ?: throw Exception("Issue یافت نشد")
    }

    data class Page(
        val items: List<IssueItem>,
        val startAt: Int,
        val maxResults: Int,
        val total: Int
    ) {
        val hasMore: Boolean get() = startAt + items.size < total
    }

    private suspend fun searchJql(jql: String, maxResults: Int, startAt: Int = 0): Page {
        val resp = api.search(jql = jql, maxResults = maxResults, startAt = startAt)
        if (!resp.isSuccessful || resp.body() == null) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
        val body = resp.body()!!
        return Page(
            items = body.issues.mapNotNull { it.toItem() },
            startAt = body.startAt,
            maxResults = body.maxResults,
            total = body.total
        )
    }

    /**
     * جستجوی Issue با فیلتر پروژه / اساین / وضعیت / متن (صفحه‌بندی)
     */
    suspend fun fetchIssues(
        projectKeys: List<String> = emptyList(),
        assignedToMe: Boolean = false,
        openOnly: Boolean = false,
        statusNames: List<String> = emptyList(),
        maxResults: Int = 50,
        startAt: Int = 0,
        textQuery: String? = null
    ): Result<Page> = runCatching {
        val jql = buildString {
            val parts = mutableListOf<String>()
            if (projectKeys.isNotEmpty()) {
                val projects = projectKeys.joinToString(", ") { it.trim().uppercase() }
                parts.add("project in ($projects)")
            }
            if (assignedToMe) {
                parts.add("assignee = currentUser()")
            }
            if (openOnly) {
                parts.add("statusCategory != Done")
            }
            if (statusNames.isNotEmpty()) {
                val statuses = statusNames.joinToString(", ") { name ->
                    val safe = name.replace("\"", "")
                    "\"" + safe + "\""
                }
                parts.add("status in ($statuses)")
            }
            val q = textQuery?.trim().orEmpty()
            if (q.isNotEmpty()) {
                if (q.matches(Regex("[A-Za-z][A-Za-z0-9]+-\\d+"))) {
                    parts.add("key = $q")
                } else {
                    parts.add("""(summary ~ "$q" OR description ~ "$q" OR text ~ "$q")""")
                }
            }
            if (parts.isEmpty()) {
                parts.add("assignee = currentUser()")
            }
            append(parts.joinToString(" AND "))
            append(" ORDER BY updated DESC")
        }
        searchJql(jql, maxResults, startAt)
    }

    /** سازگاری با کد قبلی */
    suspend fun fetchByProjects(
        projectKeys: List<String>,
        openOnly: Boolean = false,
        maxResults: Int = 50,
        startAt: Int = 0,
        textQuery: String? = null,
        assignedToMe: Boolean = false,
        statusNames: List<String> = emptyList()
    ): Result<Page> = fetchIssues(
        projectKeys = projectKeys,
        assignedToMe = assignedToMe,
        openOnly = openOnly,
        statusNames = statusNames,
        maxResults = maxResults,
        startAt = startAt,
        textQuery = textQuery
    )


    // ---- Worklogs ----

    suspend fun getWorklogs(issueKey: String): Result<List<JiraWorklog>> = runCatching {
        val resp = api.getWorklogs(issueKey.trim().uppercase())
        if (!resp.isSuccessful || resp.body() == null) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
        resp.body()!!.worklogs.sortedByDescending { it.started ?: it.created ?: "" }
    }

    suspend fun addWorklog(
        issueKey: String,
        durationMinutes: Int,
        startedIso: String? = null,
        comment: String? = null
    ): Result<JiraWorklog> = runCatching {
        if (durationMinutes <= 0) throw Exception("مدت باید بیشتر از صفر باشد")
        val body = JiraWorklogRequest(
            timeSpentSeconds = durationMinutes * 60,
            started = startedIso ?: nowIranIso(),
            comment = comment?.takeIf { it.isNotBlank() }
        )
        val resp = api.addWorklog(issueKey.trim().uppercase(), body)
        if (!resp.isSuccessful || resp.body() == null) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
        resp.body()!!
    }

    suspend fun updateWorklog(
        issueKey: String,
        worklogId: String,
        durationMinutes: Int?,
        comment: String?
    ): Result<JiraWorklog> = runCatching {
        val body = JiraWorklogRequest(
            timeSpentSeconds = durationMinutes?.times(60),
            comment = comment
        )
        val resp = api.updateWorklog(issueKey.trim().uppercase(), worklogId, body)
        if (!resp.isSuccessful || resp.body() == null) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
        resp.body()!!
    }

    suspend fun deleteWorklog(issueKey: String, worklogId: String): Result<Unit> = runCatching {
        val resp = api.deleteWorklog(issueKey.trim().uppercase(), worklogId)
        if (!resp.isSuccessful) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
    }

    // ---- Comments ----

    suspend fun getComments(issueKey: String): Result<List<JiraComment>> = runCatching {
        val resp = api.getComments(issueKey.trim().uppercase())
        if (!resp.isSuccessful || resp.body() == null) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
        resp.body()!!.comments.sortedByDescending { it.created ?: "" }
    }

    suspend fun addComment(issueKey: String, body: String): Result<JiraComment> = runCatching {
        if (body.isBlank()) throw Exception("متن کامنت خالی است")
        val resp = api.addComment(issueKey.trim().uppercase(), JiraCommentRequest(body))
        if (!resp.isSuccessful || resp.body() == null) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
        resp.body()!!
    }

    // ---- Create / Update Issue ----

    suspend fun createIssue(
        projectKey: String,
        summary: String,
        description: String? = null,
        issueType: String = "Task",
        assigneeName: String? = null
    ): Result<JiraCreateIssueResponse> = runCatching {
        if (projectKey.isBlank() || summary.isBlank()) {
            throw Exception("کلید پروژه و عنوان الزامی است")
        }
        val fields = JiraCreateFields(
            project = JiraProjectKey(projectKey.trim().uppercase()),
            summary = summary.trim(),
            description = description?.takeIf { it.isNotBlank() },
            issuetype = JiraIssueTypeName(issueType),
            assignee = assigneeName?.takeIf { it.isNotBlank() }?.let { JiraAssigneeName(it) }
        )
        val resp = api.createIssue(JiraCreateIssueRequest(fields))
        if (!resp.isSuccessful || resp.body() == null) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
        resp.body()!!
    }

    suspend fun updateSummary(
        issueKey: String,
        summary: String
    ): Result<Unit> = runCatching {
        val body = JiraUpdateIssueRequest(fields = mapOf("summary" to summary))
        val resp = api.updateIssue(issueKey.trim().uppercase(), body)
        if (!resp.isSuccessful) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
    }


    suspend fun fetchStatuses(): Result<List<JiraStatusDto>> = runCatching {
        val resp = api.getStatuses()
        if (!resp.isSuccessful || resp.body() == null) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
        resp.body()!!.filter { !it.id.isNullOrBlank() && !it.name.isNullOrBlank() }
    }


    suspend fun fetchTransitions(issueKey: String): Result<List<JiraTransition>> = runCatching {
        val resp = api.getTransitions(issueKey.trim().uppercase())
        if (!resp.isSuccessful || resp.body() == null) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
        resp.body()!!.transitions.filter { !it.id.isNullOrBlank() }
    }

    suspend fun transitionIssue(issueKey: String, transitionId: String): Result<Unit> = runCatching {
        val resp = api.doTransition(
            issueKey.trim().uppercase(),
            JiraDoTransitionRequest(JiraTransitionId(transitionId))
        )
        if (!resp.isSuccessful) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
    }

    suspend fun fetchProjects(): Result<List<JiraProjectDto>> = runCatching {
        val resp = api.getProjects()
        if (!resp.isSuccessful || resp.body() == null) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
        resp.body()!!.filter { !it.key.isNullOrBlank() }
            .sortedBy { it.key }
    }

    companion object {
        fun fromSettings(s: SettingsEntity): JiraService? {
            val url = s.jiraBaseUrl.trim()
            val token = s.jiraToken.trim()
            if (!s.jiraEnabled || url.isBlank() || token.isBlank()) return null
            return JiraService(url, token)
        }

        fun nowIranIso(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXX", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Tehran")
            return sdf.format(Date())
        }

        fun toJiraStarted(dateIso: String, timeHHmm: String? = null): String {
            val time = timeHHmm?.takeIf { it.matches(Regex("\\d{1,2}:\\d{2}")) } ?: "09:00"
            val raw = "${dateIso}T${time}:00.000"
            return try {
                val inFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
                inFmt.timeZone = TimeZone.getTimeZone("Asia/Tehran")
                val date = inFmt.parse(raw) ?: Date()
                val outFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXX", Locale.US)
                outFmt.timeZone = TimeZone.getTimeZone("Asia/Tehran")
                outFmt.format(date)
            } catch (_: Exception) {
                nowIranIso()
            }
        }

        fun mapStatus(category: String): String = when (category) {
            "done" -> "done"
            "indeterminate" -> "in_progress"
            else -> "new"
        }

        fun formatSeconds(sec: Int?): String {
            if (sec == null || sec <= 0) return "—"
            val m = sec / 60
            val h = m / 60
            val rm = m % 60
            return when {
                h > 0 && rm > 0 -> "${h}س ${rm}د"
                h > 0 -> "${h}س"
                else -> "${rm}د"
            }
        }
    }
}

private fun JiraIssue.toItem(): JiraService.IssueItem? {
    val key = this.key ?: return null
    val f = fields ?: return null
    val cat = f.status?.statusCategory?.key ?: "indeterminate"
    return JiraService.IssueItem(
        key = key,
        summary = f.summary ?: key,
        description = f.description,
        projectKey = f.project?.key ?: "",
        projectName = f.project?.name ?: f.project?.key ?: "",
        statusCategory = cat,
        statusName = f.status?.name ?: "",
        priorityName = f.priority?.name ?: "",
        issueTypeName = f.issuetype?.name ?: "",
        assigneeName = f.assignee?.displayName ?: f.assignee?.name,
        remainingMinutes = ((f.timetracking?.remainingEstimateSeconds ?: 0) / 60).coerceAtLeast(0),
        requiredMinutes = ((f.timetracking?.originalEstimateSeconds ?: 0) / 60).coerceAtLeast(0),
        timeSpentMinutes = ((f.timetracking?.timeSpentSeconds ?: 0) / 60).coerceAtLeast(0),
        labels = f.labels.orEmpty()
    )
}

fun JiraService.IssueItem.toTaskEntity(existingId: Long = 0, createdAt: String): TaskEntity {
    return TaskEntity(
        id = existingId,
        jiraNumber = key,
        projectName = projectName.ifBlank { projectKey },
        taskTitle = summary,
        description = description,
        requiredMinutes = requiredMinutes,
        remainingMinutes = remainingMinutes,
        status = JiraService.mapStatus(statusCategory),
        isRunning = false,
        runStartedAt = null,
        createdAt = createdAt
    )
}
