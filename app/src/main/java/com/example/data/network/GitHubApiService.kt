package com.example.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service interface for the GitHub REST API v3 (`https://api.github.com/`).
 *
 * Provides endpoints for fetching repository metadata, listing GitHub Actions workflows
 * and build runs, inspecting jobs/artifacts/releases, and triggering `workflow_dispatch` runs.
 */
interface GitHubApiService {

  @GET("user")
  suspend fun getAuthenticatedUser(): Response<GitHubUserDto>

  @GET("rate_limit")
  suspend fun getRateLimit(): Response<GitHubRateLimitResponse>

  @GET("users/{username}/repos")
  suspend fun getUserRepos(
    @Path("username") username: String,
    @Query("sort") sort: String = "updated",
    @Query("per_page") perPage: Int = 30,
    @Query("page") page: Int = 1,
  ): Response<List<GitHubRepoDto>>

  @GET("user/repos")
  suspend fun getMyRepos(
    @Query("sort") sort: String = "updated",
    @Query("per_page") perPage: Int = 30,
    @Query("page") page: Int = 1,
  ): Response<List<GitHubRepoDto>>

  @GET("repos/{owner}/{repo}")
  suspend fun getRepo(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
  ): Response<GitHubRepoDto>

  @GET("repos/{owner}/{repo}/branches")
  suspend fun getBranches(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Query("per_page") perPage: Int = 30,
  ): Response<List<GitHubBranchDto>>

  @GET("repos/{owner}/{repo}/actions/workflows")
  suspend fun getWorkflows(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Query("per_page") perPage: Int = 30,
  ): Response<GitHubWorkflowsResponse>

  @GET("repos/{owner}/{repo}/actions/workflows/{workflow_id}")
  suspend fun getWorkflow(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("workflow_id") workflowId: Long,
  ): Response<GitHubWorkflowDto>

  @POST("repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches")
  suspend fun dispatchWorkflow(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("workflow_id") workflowId: Long,
    @Body request: WorkflowDispatchRequest,
  ): Response<Unit>

  @POST("repos/{owner}/{repo}/actions/workflows/{workflow_file}/dispatches")
  suspend fun dispatchWorkflowByFileName(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("workflow_file") workflowFileName: String,
    @Body request: WorkflowDispatchRequest,
  ): Response<Unit>

  @GET("repos/{owner}/{repo}/actions/runs")
  suspend fun getWorkflowRuns(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Query("per_page") perPage: Int = 20,
    @Query("branch") branch: String? = null,
    @Query("status") status: String? = null,
  ): Response<GitHubRunsResponse>

  @GET("repos/{owner}/{repo}/actions/runs/{run_id}")
  suspend fun getWorkflowRun(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("run_id") runId: Long,
  ): Response<GitHubWorkflowRunDto>

  @GET("repos/{owner}/{repo}/actions/runs/{run_id}/jobs")
  suspend fun getWorkflowRunJobs(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("run_id") runId: Long,
  ): Response<GitHubJobsResponse>

  @GET("repos/{owner}/{repo}/actions/runs/{run_id}/artifacts")
  suspend fun getWorkflowRunArtifacts(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("run_id") runId: Long,
  ): Response<GitHubArtifactsResponse>

  @POST("repos/{owner}/{repo}/actions/runs/{run_id}/rerun")
  suspend fun rerunWorkflow(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("run_id") runId: Long,
  ): Response<Unit>

  @POST("repos/{owner}/{repo}/actions/runs/{run_id}/cancel")
  suspend fun cancelWorkflowRun(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("run_id") runId: Long,
  ): Response<Unit>

  @GET("repos/{owner}/{repo}/releases")
  suspend fun getReleases(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Query("per_page") perPage: Int = 10,
  ): Response<List<GitHubReleaseDto>>

  @GET("repos/{owner}/{repo}/contents/{path}")
  suspend fun getFileContent(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path(value = "path", encoded = true) path: String,
    @Query("ref") ref: String? = null,
  ): Response<GitHubFileContentResponse>

  @PUT("repos/{owner}/{repo}/contents/{path}")
  suspend fun putFileContent(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path(value = "path", encoded = true) path: String,
    @Body body: GitHubPutFileRequest,
  ): Response<Unit>
}
