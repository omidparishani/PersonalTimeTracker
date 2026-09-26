package com.personal.timetracker.jira

/**
 * توضیح فایل: سرویس سطح بالا برای عملیات جیرا با Result و مدیریت خطا.
 * بسته: com.personal.timetracker.jira
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

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

    /**
     * اطلاعات کاربر جاری توکن.
     */
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

    /**
     * لیست Worklog یک Issue از سرور.
     */
    suspend fun getWorklogs(issueKey: String): Result<List<JiraWorklog>> = runCatching {
        val resp = api.getWorklogs(issueKey.trim().uppercase())
        if (!resp.isSuccessful || resp.body() == null) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
        resp.body()!!.worklogs.sortedByDescending { it.started ?: it.created ?: "" }
    }

    /**
     * افزودن Worklog روی سرور.
     */
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

    /** ایجاد Issue با فیلدهای خام (از createmeta) */
    suspend fun createIssueWithFields(fields: Map<String, Any?>): Result<JiraCreateIssueResponse> = runCatching {
        val resp = api.createIssueRaw(JiraCreateIssueRequestRaw(fields))
        if (!resp.isSuccessful || resp.body() == null) {
            val parsed = JiraClient.parseError(resp.errorBody()?.string())
            val msg = if (parsed.isBlank() || parsed == "خطای ناشناخته از سرور جیرا") {
                "خطای سرور (${resp.code()}): امکان ایجاد Issue نیست. فیلدهای اجباری یا مقادیر را بررسی کنید."
            } else {
                parsed
            }
            throw Exception(msg)
        }
        resp.body()!!
    }

    /**
     * فقط لیست نوع Issueهای قابل ایجاد در پروژه (بدون فیلد — سریع).
     * فیلدها را با [fetchFieldsForIssueType] بعد از انتخاب نوع بگیرید.
     */
    suspend fun fetchCreateMeta(projectKey: String): Result<JiraCreateMetaProject> = runCatching {
        val key = projectKey.trim().uppercase()

        // 1) API جدید issuetypes
        try {
            val typesResp = api.getCreateMetaIssueTypes(key)
            if (typesResp.isSuccessful && typesResp.body() != null) {
                val types = typesResp.body()!!.values
                    .filter { !it.id.isNullOrBlank() && it.subtask != true }
                if (types.isNotEmpty()) {
                    return@runCatching JiraCreateMetaProject(
                        id = null, key = key, name = key, issuetypes = types
                    )
                }
            }
        } catch (_: Exception) { }

        // 2) از خود پروژه
        try {
            val projResp = api.getProject(key)
            if (projResp.isSuccessful && projResp.body() != null) {
                val proj = projResp.body()!!
                val types = (proj.issueTypes.orEmpty())
                    .filter { !it.id.isNullOrBlank() && it.subtask != true }
                    .map { t ->
                        JiraCreateMetaIssueType(
                            id = t.id,
                            name = t.name,
                            description = t.description,
                            subtask = t.subtask,
                            fields = null
                        )
                    }
                if (types.isNotEmpty()) {
                    return@runCatching JiraCreateMetaProject(
                        id = proj.id,
                        key = proj.key ?: key,
                        name = proj.name,
                        issuetypes = types
                    )
                }
            }
        } catch (_: Exception) { }

        // 3) حداقل
        JiraCreateMetaProject(
            key = key,
            name = key,
            issuetypes = listOf(
                JiraCreateMetaIssueType(id = null, name = "Task"),
                JiraCreateMetaIssueType(id = null, name = "Bug"),
                JiraCreateMetaIssueType(id = null, name = "Story")
            )
        )
    }

    /**
     * واکشی همه فیلدهای createmeta برای یک نوع Issue (شامل allowedValues).
     * با پارس JSON خام تا فیلدهای سفارشی مثل BudgetType / activityType از دست نروند.
     */
    suspend fun fetchFieldsForIssueType(
        projectKey: String,
        issueTypeId: String?,
        issueTypeName: String? = null
    ): Result<Map<String, JiraMetaField>> = runCatching {
        val key = projectKey.trim().uppercase()
        val typeId = issueTypeId?.trim().orEmpty()

        // 1) API جدید — JSON خام
        if (typeId.isNotEmpty()) {
            val parsed = parseCreateMetaFieldsRaw(key, typeId)
            if (parsed.isNotEmpty()) return@runCatching parsed
        }

        // 2) createmeta کلاسیک با issuetypeIds یا Names
        val classic = parseClassicCreateMeta(key, typeId.ifBlank { null }, issueTypeName)
        if (classic.isNotEmpty()) return@runCatching classic

        // 3) typed retrofit fallback
        if (typeId.isNotEmpty()) {
            try {
                val resp = api.getCreateMetaFields(key, typeId, startAt = 0, maxResults = 200)
                if (resp.isSuccessful && resp.body() != null) {
                    val list = resp.body()!!.fields ?: resp.body()!!.values ?: emptyList()
                    val map = list.mapNotNull { f ->
                        val id = f.fieldId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        id to JiraMetaField(
                            required = f.required,
                            name = f.name,
                            hasDefaultValue = f.hasDefaultValue,
                            schema = f.schema,
                            allowedValues = f.allowedValues,
                            operations = f.operations
                        )
                    }.toMap()
                    if (map.isNotEmpty()) return@runCatching map
                }
            } catch (_: Exception) { }
        }

        defaultCreateFields()
    }

    private suspend fun parseCreateMetaFieldsRaw(projectKey: String, issueTypeId: String): Map<String, JiraMetaField> {
        val all = linkedMapOf<String, JiraMetaField>()
        var start = 0
        try {
            repeat(8) {
                val resp = api.getCreateMetaFieldsRaw(projectKey, issueTypeId, startAt = start, maxResults = 100)
                if (!resp.isSuccessful || resp.body() == null) return@repeat
                val json = resp.body()!!.string()
                val root = com.google.gson.JsonParser.parseString(json).asJsonObject
                val arr = when {
                    root.has("fields") && root.get("fields").isJsonArray -> root.getAsJsonArray("fields")
                    root.has("values") && root.get("values").isJsonArray -> root.getAsJsonArray("values")
                    else -> null
                } ?: return@repeat
                if (arr.size() == 0) return@repeat
                arr.forEach { el ->
                    if (!el.isJsonObject) return@forEach
                    val o = el.asJsonObject
                    val fieldId = when {
                        o.has("fieldId") -> o.get("fieldId").asString
                        o.has("key") -> o.get("key").asString
                        else -> return@forEach
                    }
                    all[fieldId] = parseMetaFieldObject(o)
                }
                start += arr.size()
                val total = root.get("total")?.asInt ?: arr.size()
                if (start >= total || arr.size() < 100) return@repeat
            }
        } catch (_: Exception) { }
        return all
    }

    private suspend fun parseClassicCreateMeta(
        projectKey: String,
        issueTypeId: String?,
        issueTypeName: String?
    ): Map<String, JiraMetaField> {
        try {
            val resp = api.getCreateMetaRaw(
                projectKeys = projectKey,
                issuetypeIds = issueTypeId,
                issuetypeNames = if (issueTypeId.isNullOrBlank()) issueTypeName else null
            )
            if (!resp.isSuccessful || resp.body() == null) return emptyMap()
            val json = resp.body()!!.string()
            // اگر سرور "Issue Does Not Exist" برگرداند، خالی
            if (json.contains("Issue Does Not Exist", ignoreCase = true)) return emptyMap()
            val root = com.google.gson.JsonParser.parseString(json).asJsonObject
            val projects = root.getAsJsonArray("projects") ?: return emptyMap()
            for (p in projects) {
                if (!p.isJsonObject) continue
                val po = p.asJsonObject
                val pkey = po.get("key")?.asString
                if (pkey != null && !pkey.equals(projectKey, ignoreCase = true)) continue
                val types = po.getAsJsonArray("issuetypes") ?: continue
                for (t in types) {
                    if (!t.isJsonObject) continue
                    val to = t.asJsonObject
                    val tid = to.get("id")?.asString
                    val tname = to.get("name")?.asString
                    val match = when {
                        !issueTypeId.isNullOrBlank() && tid == issueTypeId -> true
                        !issueTypeName.isNullOrBlank() && tname.equals(issueTypeName, true) -> true
                        issueTypeId.isNullOrBlank() && issueTypeName.isNullOrBlank() -> true
                        else -> false
                    }
                    if (!match) continue
                    val fieldsObj = to.get("fields") ?: continue
                    if (fieldsObj.isJsonObject) {
                        val map = linkedMapOf<String, JiraMetaField>()
                        for ((k, v) in fieldsObj.asJsonObject.entrySet()) {
                            if (v.isJsonObject) map[k] = parseMetaFieldObject(v.asJsonObject)
                        }
                        if (map.isNotEmpty()) return map
                    }
                }
            }
        } catch (_: Exception) { }
        return emptyMap()
    }

    private fun parseMetaFieldObject(o: com.google.gson.JsonObject): JiraMetaField {
        val required = o.get("required")?.asBoolean ?: false
        val name = o.get("name")?.asString
        val hasDefault = o.get("hasDefaultValue")?.asBoolean ?: false
        val schema = o.get("schema")?.takeIf { it.isJsonObject }?.asJsonObject?.let { sc ->
            JiraFieldSchema(
                type = sc.get("type")?.asString,
                system = sc.get("system")?.asString,
                items = sc.get("items")?.asString,
                custom = sc.get("custom")?.asString,
                customId = sc.get("customId")?.asInt
            )
        }
        val allowed = mutableListOf<JiraAllowedValue>()
        val av = o.get("allowedValues")
        if (av != null && av.isJsonArray) {
            av.asJsonArray.forEach { item ->
                if (!item.isJsonObject) return@forEach
                val a = item.asJsonObject
                allowed.add(
                    JiraAllowedValue(
                        id = a.get("id")?.asString,
                        name = a.get("name")?.asString,
                        value = a.get("value")?.asString,
                        key = a.get("key")?.asString
                    )
                )
            }
        }
        val ops = o.get("operations")?.takeIf { it.isJsonArray }?.asJsonArray
            ?.mapNotNull { if (it.isJsonPrimitive) it.asString else null }
        return JiraMetaField(
            required = required,
            name = name,
            hasDefaultValue = hasDefault,
            schema = schema,
            allowedValues = allowed.ifEmpty { null },
            operations = ops
        )
    }

    /** سازگاری با کد قبلی */
    private suspend fun fetchFieldsForType(projectKey: String, issueTypeId: String): Map<String, JiraMetaField> {
        return fetchFieldsForIssueType(projectKey, issueTypeId).getOrElse { defaultCreateFields() }
    }

    private fun defaultCreateFields(): Map<String, JiraMetaField> = mapOf(
        "summary" to JiraMetaField(required = true, name = "Summary", schema = JiraFieldSchema(type = "string", system = "summary")),
        "description" to JiraMetaField(required = false, name = "Description", schema = JiraFieldSchema(type = "string", system = "description"))
    )

    suspend fun fetchEditMeta(issueKey: String): Result<Map<String, JiraMetaField>> = runCatching {
        val resp = api.getEditMeta(issueKey.trim().uppercase())
        if (!resp.isSuccessful || resp.body() == null) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
        resp.body()!!.fields
    }

    /**
     * به‌روزرسانی فیلدهای Issue.
     */
    suspend fun updateIssueFields(
        issueKey: String,
        fields: Map<String, Any?>
    ): Result<Unit> = runCatching {
        val body = JiraUpdateIssueRequest(fields = fields)
        val resp = api.updateIssue(issueKey.trim().uppercase(), body)
        if (!resp.isSuccessful) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
    }

    suspend fun updateSummary(
        issueKey: String,
        summary: String
    ): Result<Unit> = updateIssueFields(issueKey, mapOf("summary" to summary))

    /**
     * حذف Issue از سرور.
     */
    suspend fun deleteIssue(issueKey: String, deleteSubtasks: Boolean = true): Result<Unit> = runCatching {
        val resp = api.deleteIssue(issueKey.trim().uppercase(), deleteSubtasks)
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


    /**
     * جستجوی کاربران قابل‌اساین در پروژه.
     */
    suspend fun searchAssignableUsers(projectKey: String, query: String): Result<List<JiraUser>> = runCatching {
        val pk = projectKey.trim().uppercase()
        val q = query.trim()
        // 1) Server-style username
        try {
            val resp = api.searchAssignableUsers(pk, username = q)
            if (resp.isSuccessful && resp.body() != null) {
                val list = resp.body()!!
                if (list.isNotEmpty() || q.isEmpty()) return@runCatching list
            }
        } catch (_: Exception) { }
        // 2) query param
        try {
            val resp = api.searchAssignableUsersQuery(pk, query = q)
            if (resp.isSuccessful && resp.body() != null) {
                return@runCatching resp.body()!!
            }
        } catch (_: Exception) { }
        // 3) general user search
        val r2 = api.searchUsers(q.ifBlank { "." })
        if (!r2.isSuccessful || r2.body() == null) {
            throw Exception("کاربری یافت نشد")
        }
        r2.body()!!
    }

    suspend fun searchIssuesPicker(query: String, projectKey: String? = null): Result<List<Pair<String, String>>> = runCatching {
        val resp = api.issuePicker(query = query.trim(), currentProjectId = projectKey)
        if (resp.isSuccessful && resp.body() != null) {
            return@runCatching resp.body()!!.sections.flatMap { sec ->
                sec.issues.mapNotNull { item ->
                    val key = item.key ?: return@mapNotNull null
                    val summary = item.summaryText ?: item.summary ?: ""
                    key to summary
                }
            }
        }
        // fallback JQL search
        val jql = if (query.matches(Regex("[A-Za-z][A-Za-z0-9]+-\\d+"))) {
            "key = $query"
        } else {
            """summary ~ "$query" ORDER BY updated DESC"""
        }
        searchJql(jql, 20).items.map { it.key to it.summary }
    }

    suspend fun fetchEditFields(issueKey: String): Result<Map<String, JiraMetaField>> = runCatching {
        val resp = api.getEditMeta(issueKey.trim().uppercase())
        if (!resp.isSuccessful || resp.body() == null) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
        resp.body()!!.fields.ifEmpty { defaultCreateFields() }
    }


    /**
     * جستجوی گزینه‌های ScriptRunner Database Picker
     * (ActivityType / BudgetType / DemisCustomer و مشابه)
     */
    suspend fun searchScriptRunnerPicker(
        fcsId: String,
        projectId: String,
        issueTypeId: String,
        inputValue: String = "",
        issueFieldsBody: Map<String, Any?> = emptyMap()
    ): Result<List<ScriptRunnerPickerItem>> = runCatching {
        val bodyJson = com.google.gson.Gson().toJson(
            mapOf("issueFields" to issueFieldsBody.ifEmpty {
                mapOf(
                    "pid" to listOf(projectId),
                    "issuetype" to listOf(issueTypeId)
                )
            })
        )
        val media = "application/json; charset=utf-8".toMediaType()
        val reqBody = bodyJson.toRequestBody(media)
        val resp = api.scriptRunnerPickerSearch(
            inputValue = inputValue,
            userData = "{}",
            fcsId = fcsId,
            pid = projectId,
            issueTypeId = issueTypeId,
            body = reqBody
        )
        if (!resp.isSuccessful || resp.body() == null) {
            throw Exception(JiraClient.parseError(resp.errorBody()?.string()))
        }
        val raw = resp.body()!!.string()
        parseScriptRunnerPickerResponse(raw)
    }

    private fun parseScriptRunnerPickerResponse(raw: String): List<ScriptRunnerPickerItem> {
        val out = mutableListOf<ScriptRunnerPickerItem>()
        try {
            val el = com.google.gson.JsonParser.parseString(raw)
            val arr = when {
                el.isJsonArray -> el.asJsonArray
                el.isJsonObject && el.asJsonObject.has("items") ->
                    el.asJsonObject.getAsJsonArray("items")
                el.isJsonObject && el.asJsonObject.has("results") ->
                    el.asJsonObject.getAsJsonArray("results")
                el.isJsonObject && el.asJsonObject.has("data") && el.asJsonObject.get("data").isJsonArray ->
                    el.asJsonObject.getAsJsonArray("data")
                else -> null
            } ?: return emptyList()
            arr.forEach { item ->
                if (!item.isJsonObject) {
                    if (item.isJsonPrimitive) {
                        val v = item.asString
                        out.add(ScriptRunnerPickerItem(id = v, label = v, value = v))
                    }
                    return@forEach
                }
                val o = item.asJsonObject
                val id = sequenceOf("id", "value", "key", "objectId")
                    .mapNotNull { k -> o.get(k)?.takeIf { it.isJsonPrimitive }?.asString }
                    .firstOrNull() ?: return@forEach
                val label = sequenceOf("label", "name", "text", "displayName", "html", "value")
                    .mapNotNull { k -> o.get(k)?.takeIf { it.isJsonPrimitive }?.asString }
                    .firstOrNull() ?: id
                // strip simple html tags from label
                val clean = label.replace(Regex("<[^>]+>"), "").trim()
                out.add(ScriptRunnerPickerItem(id = id, label = clean.ifBlank { id }, value = id))
            }
        } catch (_: Exception) { }
        return out.distinctBy { it.id }
    }

    /** شناسه عددی پروژه از کلید */
    suspend fun resolveProjectId(projectKey: String): Result<String> = runCatching {
        val resp = api.getProject(projectKey.trim().uppercase())
        if (resp.isSuccessful && resp.body()?.id != null) {
            return@runCatching resp.body()!!.id!!
        }
        // raw fallback
        val raw = api.getProjectRaw(projectKey.trim().uppercase())
        if (raw.isSuccessful && raw.body() != null) {
            val json = raw.body()!!.string()
            val o = com.google.gson.JsonParser.parseString(json).asJsonObject
            o.get("id")?.asString?.let { return@runCatching it }
        }
        throw Exception("شناسه پروژه یافت نشد")
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

        /**
         * تبدیل تاریخ/ساعت به فرمت datetime مورد قبول جیرا.
         */
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
