package com.personal.timetracker.jira

import retrofit2.Response
import retrofit2.http.*

/**
 * Jira REST API v2 (Server / Data Center) — مطابق استاندارد شرکت.
 * Auth: Bearer Personal Access Token (در OkHttp interceptor).
 */
interface JiraApi {

    @GET("rest/api/2/status")
    suspend fun getStatuses(): Response<List<JiraStatusDto>>

    @GET("rest/api/2/project")
    suspend fun getProjects(): Response<List<JiraProjectDto>>

    @GET("rest/api/2/myself")
    suspend fun myself(): Response<JiraUser>

    @GET("rest/api/2/serverInfo")
    suspend fun serverInfo(): Response<JiraServerInfo>

    @GET("rest/api/2/search")
    suspend fun search(
        @Query("jql") jql: String,
        @Query("maxResults") maxResults: Int = 50,
        @Query("startAt") startAt: Int = 0,
        @Query("fields") fields: String = "summary,status,priority,issuetype,created,updated,timetracking,assignee,project,description,labels"
    ): Response<JiraSearchResult>

    @GET("rest/api/2/issue/{issueKey}")
    suspend fun getIssue(
        @Path("issueKey") issueKey: String,
        @Query("fields") fields: String = "summary,status,priority,issuetype,assignee,timetracking,worklog,description,project,comment,labels,reporter",
        @Query("expand") expand: String = "renderedFields"
    ): Response<JiraIssue>

    // ---- Worklog ----

    @POST("rest/api/2/issue/{issueKey}/worklog")
    suspend fun addWorklog(
        @Path("issueKey") issueKey: String,
        @Body body: JiraWorklogRequest
    ): Response<JiraWorklog>

    @GET("rest/api/2/issue/{issueKey}/worklog")
    suspend fun getWorklogs(
        @Path("issueKey") issueKey: String
    ): Response<JiraWorklogList>

    @PUT("rest/api/2/issue/{issueKey}/worklog/{worklogId}")
    suspend fun updateWorklog(
        @Path("issueKey") issueKey: String,
        @Path("worklogId") worklogId: String,
        @Body body: JiraWorklogRequest
    ): Response<JiraWorklog>

    @DELETE("rest/api/2/issue/{issueKey}/worklog/{worklogId}")
    suspend fun deleteWorklog(
        @Path("issueKey") issueKey: String,
        @Path("worklogId") worklogId: String
    ): Response<Unit>

    // ---- Comments ----

    @GET("rest/api/2/issue/{issueKey}/comment")
    suspend fun getComments(
        @Path("issueKey") issueKey: String
    ): Response<JiraCommentContainer>

    @POST("rest/api/2/issue/{issueKey}/comment")
    suspend fun addComment(
        @Path("issueKey") issueKey: String,
        @Body body: JiraCommentRequest
    ): Response<JiraComment>

    // ---- Create / Update ----

    @POST("rest/api/2/issue")
    suspend fun createIssue(
        @Body body: JiraCreateIssueRequest
    ): Response<JiraCreateIssueResponse>

    @POST("rest/api/2/issue")
    suspend fun createIssueRaw(
        @Body body: JiraCreateIssueRequestRaw
    ): Response<JiraCreateIssueResponse>

    @PUT("rest/api/2/issue/{issueKey}")
    suspend fun updateIssue(
        @Path("issueKey") issueKey: String,
        @Body body: JiraUpdateIssueRequest
    ): Response<Unit>

    // ---- Transitions (workflow) ----

    @GET("rest/api/2/issue/{issueKey}/transitions")
    suspend fun getTransitions(
        @Path("issueKey") issueKey: String
    ): Response<JiraTransitionsResponse>

    @POST("rest/api/2/issue/{issueKey}/transitions")
    suspend fun doTransition(
        @Path("issueKey") issueKey: String,
        @Body body: JiraDoTransitionRequest
    ): Response<Unit>

    /** نسخه قدیمی createmeta (ممکن است در بعضی سرورها 404 بدهد) */
    @GET("rest/api/2/issue/createmeta")
    suspend fun getCreateMeta(
        @Query("projectKeys") projectKeys: String,
        @Query("expand") expand: String = "projects.issuetypes.fields"
    ): Response<JiraCreateMetaResponse>

    /** جزئیات پروژه + لیست issue typeها */
    @GET("rest/api/2/project/{projectIdOrKey}")
    suspend fun getProject(
        @Path("projectIdOrKey") projectIdOrKey: String,
        @Query("expand") expand: String = "issueTypes"
    ): Response<JiraProjectDetailDto>

    /** لیست issue typeهای قابل ایجاد در پروژه (API جدیدتر) */
    @GET("rest/api/2/issue/createmeta/{projectIdOrKey}/issuetypes")
    suspend fun getCreateMetaIssueTypes(
        @Path("projectIdOrKey") projectIdOrKey: String
    ): Response<JiraCreateMetaIssueTypesPage>

    /** فیلدهای createmeta برای یک نوع Issue */
    @GET("rest/api/2/issue/createmeta/{projectIdOrKey}/issuetypes/{issueTypeId}")
    suspend fun getCreateMetaFields(
        @Path("projectIdOrKey") projectIdOrKey: String,
        @Path("issueTypeId") issueTypeId: String,
        @Query("startAt") startAt: Int = 0,
        @Query("maxResults") maxResults: Int = 200
    ): Response<JiraCreateMetaFieldsPage>

    @DELETE("rest/api/2/issue/{issueKey}")
    suspend fun deleteIssue(
        @Path("issueKey") issueKey: String,
        @Query("deleteSubtasks") deleteSubtasks: Boolean = true
    ): Response<Unit>

    @GET("rest/api/2/issue/{issueKey}/editmeta")
    suspend fun getEditMeta(
        @Path("issueKey") issueKey: String
    ): Response<JiraEditMetaResponse>

    /** Jira Server/DC: پارامتر username */
    @GET("rest/api/2/user/assignable/search")
    suspend fun searchAssignableUsers(
        @Query("project") project: String,
        @Query("username") username: String = "",
        @Query("maxResults") maxResults: Int = 50
    ): Response<List<JiraUser>>

    /** بعضی نسخه‌ها query می‌پذیرند */
    @GET("rest/api/2/user/assignable/search")
    suspend fun searchAssignableUsersQuery(
        @Query("project") project: String,
        @Query("query") query: String = "",
        @Query("maxResults") maxResults: Int = 50
    ): Response<List<JiraUser>>

    @GET("rest/api/2/issue/createmeta/{projectIdOrKey}/issuetypes/{issueTypeId}")
    suspend fun getCreateMetaFieldsRaw(
        @Path("projectIdOrKey") projectIdOrKey: String,
        @Path("issueTypeId") issueTypeId: String,
        @Query("startAt") startAt: Int = 0,
        @Query("maxResults") maxResults: Int = 200
    ): Response<okhttp3.ResponseBody>

    @GET("rest/api/2/issue/createmeta")
    suspend fun getCreateMetaRaw(
        @Query("projectKeys") projectKeys: String,
        @Query("issuetypeIds") issuetypeIds: String? = null,
        @Query("issuetypeNames") issuetypeNames: String? = null,
        @Query("expand") expand: String = "projects.issuetypes.fields"
    ): Response<okhttp3.ResponseBody>

    @GET("rest/api/2/user/search")
    suspend fun searchUsers(
        @Query("username") username: String,
        @Query("maxResults") maxResults: Int = 20
    ): Response<List<JiraUser>>

    @GET("rest/api/2/issue/picker")
    suspend fun issuePicker(
        @Query("query") query: String,
        @Query("currentProjectId") currentProjectId: String? = null,
        @Query("showSubTasks") showSubTasks: Boolean = false
    ): Response<JiraIssuePickerResult>

    /**
     * ScriptRunner Database / Configurable Object Picker
     * مثال: ActivityType, BudgetType, DemisCustomer در شرکت
     */
    @POST("rest/scriptrunner-jira/latest/generic-picker/search")
    suspend fun scriptRunnerPickerSearch(
        @Query("inputValue") inputValue: String = "",
        @Query("userData") userData: String = "{}",
        @Query("fcsId") fcsId: String,
        @Query("pid") pid: String,
        @Query("issueTypeId") issueTypeId: String,
        @Body body: okhttp3.RequestBody
    ): Response<okhttp3.ResponseBody>

    @GET("rest/api/2/project/{projectIdOrKey}")
    suspend fun getProjectRaw(
        @Path("projectIdOrKey") projectIdOrKey: String,
        @Query("expand") expand: String = "issueTypes"
    ): Response<okhttp3.ResponseBody>
}
