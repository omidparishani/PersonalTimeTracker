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
}
