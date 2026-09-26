package com.personal.timetracker.jira

/**
 * توضیح فایل: مدل‌های داده (DTO) برای JSON جیرا و ScriptRunner.
 * بسته: com.personal.timetracker.jira
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

// ---------- Auth / User ----------

/**
 * مدل‌های داده (DTO) برای JSON جیرا و ScriptRunner.
 */
data class JiraUser(
    val self: String? = null,
    val key: String? = null,
    val name: String? = null,
    val displayName: String? = null,
    val emailAddress: String? = null,
    val active: Boolean? = null
)

data class JiraServerInfo(
    val baseUrl: String? = null,
    val version: String? = null,
    val versionNumbers: List<Int>? = null,
    val deploymentType: String? = null,
    val serverTitle: String? = null
)

// ---------- Search / Issue ----------

data class JiraSearchResult(
    val expand: String? = null,
    val startAt: Int = 0,
    val maxResults: Int = 0,
    val total: Int = 0,
    val issues: List<JiraIssue> = emptyList()
)

data class JiraIssue(
    val id: String? = null,
    val key: String? = null,
    val self: String? = null,
    val fields: JiraIssueFields? = null
)

data class JiraIssueFields(
    val summary: String? = null,
    val description: String? = null,
    val status: JiraStatus? = null,
    val priority: JiraPriority? = null,
    val issuetype: JiraIssueType? = null,
    val project: JiraProject? = null,
    val assignee: JiraUser? = null,
    val reporter: JiraUser? = null,
    val created: String? = null,
    val updated: String? = null,
    val timetracking: JiraTimeTracking? = null,
    val comment: JiraCommentContainer? = null,
    val labels: List<String>? = null
)

data class JiraStatus(
    val name: String? = null,
    val id: String? = null,
    val statusCategory: JiraStatusCategory? = null
)

data class JiraStatusCategory(
    val key: String? = null,
    val name: String? = null
)

data class JiraPriority(
    val name: String? = null,
    val id: String? = null
)

data class JiraIssueType(
    val name: String? = null,
    val id: String? = null,
    val subtask: Boolean? = null,
    val description: String? = null
)

data class JiraProject(
    val key: String? = null,
    val name: String? = null,
    val id: String? = null
)

data class JiraTimeTracking(
    val originalEstimate: String? = null,
    val remainingEstimate: String? = null,
    val timeSpent: String? = null,
    val originalEstimateSeconds: Int? = null,
    val remainingEstimateSeconds: Int? = null,
    val timeSpentSeconds: Int? = null
)

// ---------- Worklog ----------

data class JiraWorklogRequest(
    val timeSpent: String? = null,
    val timeSpentSeconds: Int? = null,
    val started: String? = null,
    val comment: String? = null
)

data class JiraWorklog(
    val id: String? = null,
    val self: String? = null,
    val author: JiraUser? = null,
    val updateAuthor: JiraUser? = null,
    val comment: String? = null,
    val created: String? = null,
    val updated: String? = null,
    val started: String? = null,
    val timeSpent: String? = null,
    val timeSpentSeconds: Int? = null
)

data class JiraWorklogList(
    val startAt: Int = 0,
    val maxResults: Int = 0,
    val total: Int = 0,
    val worklogs: List<JiraWorklog> = emptyList()
)

// ---------- Comments ----------

data class JiraCommentContainer(
    val comments: List<JiraComment> = emptyList(),
    val total: Int = 0,
    val maxResults: Int = 0,
    val startAt: Int = 0
)

data class JiraComment(
    val id: String? = null,
    val self: String? = null,
    val author: JiraUser? = null,
    val body: String? = null,
    val created: String? = null,
    val updated: String? = null
)

data class JiraCommentRequest(
    val body: String
)

// ---------- Create / Update Issue ----------

data class JiraCreateIssueRequest(
    val fields: JiraCreateFields
)

data class JiraCreateIssueRequestRaw(
    val fields: Map<String, Any?>
)

data class JiraCreateFields(
    val project: JiraProjectKey,
    val summary: String,
    val description: String? = null,
    val issuetype: JiraIssueTypeName,
    val assignee: JiraAssigneeName? = null,
    val labels: List<String>? = null
)

data class JiraUpdateIssueRequest(
    val fields: Map<String, Any?>
)

data class JiraProjectKey(val key: String)
data class JiraIssueTypeName(val name: String = "Task")
data class JiraAssigneeName(val name: String)

data class JiraCreateIssueResponse(
    val id: String? = null,
    val key: String? = null,
    val self: String? = null
)

// ---------- Error ----------

data class JiraError(
    val errorMessages: List<String>? = null,
    val errors: Map<String, String>? = null
)


// ---------- Status list from /rest/api/2/status ----------

data class JiraStatusDto(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val statusCategory: JiraStatusCategory? = null
)


// ---------- Transitions ----------

data class JiraTransitionsResponse(
    val expand: String? = null,
    val transitions: List<JiraTransition> = emptyList()
)

data class JiraTransition(
    val id: String? = null,
    val name: String? = null,
    val to: JiraStatus? = null
)

data class JiraDoTransitionRequest(
    val transition: JiraTransitionId
)

data class JiraTransitionId(
    val id: String
)


data class JiraProjectDto(
    val id: String? = null,
    val key: String? = null,
    val name: String? = null,
    val projectTypeKey: String? = null
)


// ---------- Create / Edit Meta ----------

data class JiraCreateMetaResponse(
    val projects: List<JiraCreateMetaProject> = emptyList()
)

data class JiraCreateMetaProject(
    val id: String? = null,
    val key: String? = null,
    val name: String? = null,
    val issuetypes: List<JiraCreateMetaIssueType> = emptyList()
)

data class JiraCreateMetaIssueType(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val subtask: Boolean? = null,
    val fields: Map<String, JiraMetaField>? = null
)

data class JiraMetaField(
    val required: Boolean = false,
    val name: String? = null,
    val hasDefaultValue: Boolean = false,
    val schema: JiraFieldSchema? = null,
    val allowedValues: List<JiraAllowedValue>? = null,
    val operations: List<String>? = null
)

data class JiraFieldSchema(
    val type: String? = null,
    val system: String? = null,
    val items: String? = null,
    val custom: String? = null,
    val customId: Int? = null
)

data class JiraAllowedValue(
    val id: String? = null,
    val name: String? = null,
    val value: String? = null,
    val key: String? = null
)

data class JiraEditMetaResponse(
    val fields: Map<String, JiraMetaField> = emptyMap()
)


data class JiraProjectDetailDto(
    val id: String? = null,
    val key: String? = null,
    val name: String? = null,
    val issueTypes: List<JiraIssueType>? = null
)

data class JiraCreateMetaIssueTypesPage(
    val maxResults: Int = 0,
    val startAt: Int = 0,
    val total: Int = 0,
    val values: List<JiraCreateMetaIssueType> = emptyList()
)

data class JiraCreateMetaFieldsPage(
    val maxResults: Int = 0,
    val startAt: Int = 0,
    val total: Int = 0,
    /** بعضی نسخه‌ها آرایه fields می‌دهند */
    val fields: List<JiraMetaFieldWithId>? = null,
    /** بعضی نسخه‌ها map می‌دهند */
    val values: List<JiraMetaFieldWithId>? = null
)

data class JiraMetaFieldWithId(
    val fieldId: String? = null,
    val required: Boolean = false,
    val name: String? = null,
    val hasDefaultValue: Boolean = false,
    val schema: JiraFieldSchema? = null,
    val allowedValues: List<JiraAllowedValue>? = null,
    val operations: List<String>? = null
)


data class JiraIssuePickerResult(
    val sections: List<JiraIssuePickerSection> = emptyList()
)

data class JiraIssuePickerSection(
    val label: String? = null,
    val sub: String? = null,
    val id: String? = null,
    val issues: List<JiraIssuePickerItem> = emptyList()
)

data class JiraIssuePickerItem(
    val key: String? = null,
    val keyHtml: String? = null,
    val img: String? = null,
    val summary: String? = null,
    val summaryText: String? = null
)


/** گزینه ScriptRunner generic-picker */
data class ScriptRunnerPickerItem(
    val id: String,
    val label: String,
    val value: String = id
)

/**
 * شناسه پیکربندی فیلدهای ScriptRunner DB Picker شرکت (از فرم ایجاد Issue).
 * fcsId = field configuration script id در ScriptRunner
 */
data class ScriptRunnerFieldMeta(
    val customFieldId: String,
    val fcsId: String,
    val multiple: Boolean,
    val labelHint: String = ""
)

object DemiscoScriptRunnerFields {
    /** نگاشت فیلدهای شناخته‌شده شرکت — در صورت نبودن در createmeta */
    val known: List<ScriptRunnerFieldMeta> = listOf(
        ScriptRunnerFieldMeta("customfield_12900", "15901", multiple = false, labelHint = "ActivityType"),
        ScriptRunnerFieldMeta("customfield_13600", "16900", multiple = true, labelHint = "DemisCustomer"),
        ScriptRunnerFieldMeta("customfield_13601", "16901", multiple = false, labelHint = "BudgetType")
    )
    fun byFieldId(id: String): ScriptRunnerFieldMeta? =
        known.firstOrNull { it.customFieldId.equals(id, ignoreCase = true) }
}
